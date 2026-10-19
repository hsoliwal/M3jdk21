// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Append-only UTF-8 pool with an exact sidecar for unpaired UTF-16 surrogates. */
final class MIndexStringPool {
    static final MIndexStringPool GLOBAL = new MIndexStringPool();
    private static final long STORAGE_GENERATION = 0L;
    private static final double LOAD_FACTOR = 0.65d;
    private static final int UTF16_CHECKPOINT_STRIDE = 64;
    private static final int UTF16_CHECKPOINT_MIN_LENGTH = 256;

    private final long regionId = MIndexRegionIds.next();
    private byte[] arena = new byte[1024];
    private int arenaSize;
    private int[] offsets = new int[65];
    private int[] utf16Lengths = new int[64];
    private int[] javaHashes = new int[64];
    private String[] exceptionalUtf16 = new String[64];
    /** Sparse (UTF-16 boundary, byte offset) pairs; allocated only after a distant read. */
    private int[][] utf16Checkpoints = new int[64][];
    private long retainedUtf16CheckpointBytes;
    private long[] hashes = new long[64];
    private int[] slots = new int[128];
    /**
     * Secondary exact-String lookup table keyed by Java hash + UTF-16 length.
     *
     * <p>This lets String/MIndex bridges discover an already-admitted spelling without allocating
     * a temporary UTF-8 byte array merely to probe the canonical pool. Hash/length are candidate
     * coordinates only; exact UTF-16 content is always verified against canonical storage.</p>
     */
    private int[] javaSlots = new int[128];
    @SuppressWarnings({"rawtypes", "unchecked"})
    private WeakReference<MIndexString>[] canonical = new WeakReference[64];
    private int size;

    synchronized int intern(String value) {
        Objects.requireNonNull(value, "value");
        int existing = findString(value);
        if (existing >= 0) return existing;

        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        long hash = MIndexDictionary.hash64(utf8);
        String exceptional = hasUnpairedSurrogate(value) ? value : null;

        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(Math.addExact(arenaSize, utf8.length));
        ensureTableCapacity(size + 1);

        int id = size;
        offsets[id] = arenaSize;
        System.arraycopy(utf8, 0, arena, arenaSize, utf8.length);
        arenaSize += utf8.length;
        offsets[id + 1] = arenaSize;
        utf16Lengths[id] = value.length();
        javaHashes[id] = value.hashCode();
        exceptionalUtf16[id] = exceptional;
        hashes[id] = hash;
        size++;
        insert(id);
        return id;
    }

    /**
     * Intern a strict UTF-8 slice directly without materializing a temporary String.
     *
     * <p>UTF-16 length and java.lang.String-compatible hashCode are derived while validating the
     * byte slice, so downstream String compatibility remains exact.</p>
     */
    synchronized int internUtf8(byte[] utf8, int offset, int length) {
        Objects.requireNonNull(utf8, "utf8");
        Objects.checkFromIndexSize(offset, length, utf8.length);
        Utf8Metadata metadata = validateUtf8(utf8, offset, length);
        long hash = metadata.contentHash64();
        int existing = find(utf8, offset, length, hash, null);
        if (existing >= 0) return existing;

        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(Math.addExact(arenaSize, length));
        ensureTableCapacity(size + 1);

        int id = size;
        offsets[id] = arenaSize;
        System.arraycopy(utf8, offset, arena, arenaSize, length);
        arenaSize += length;
        offsets[id + 1] = arenaSize;
        utf16Lengths[id] = metadata.utf16Length();
        javaHashes[id] = metadata.javaHash();
        exceptionalUtf16[id] = null;
        hashes[id] = hash;
        size++;
        insert(id);
        return id;
    }

    /** Exact strict UTF-8 lookup without admitting an unknown spelling. */
    synchronized int findExistingUtf8(byte[] utf8, int offset, int length) {
        Objects.requireNonNull(utf8, "utf8");
        Objects.checkFromIndexSize(offset, length, utf8.length);
        Utf8Metadata metadata = validateUtf8(utf8, offset, length);
        return find(
                utf8,
                offset,
                length,
                metadata.contentHash64(),
                null);
    }

    /**
     * Exact allocation-free String lookup without admitting an unknown spelling.
     *
     * <p>The bridge-facing path uses the JDK String's already-available hash and UTF-16 length to
     * select candidates, then verifies exact content directly against the canonical UTF-8 arena or
     * exceptional UTF-16 sidecar. No temporary UTF-8 array is allocated.</p>
     */
    synchronized int findExisting(String value) {
        return findString(Objects.requireNonNull(value, "value"));
    }

    synchronized String materialize(int id) {
        checkId(id);
        if (exceptionalUtf16[id] != null) return exceptionalUtf16[id];
        return new String(arena, offsets[id], offsets[id + 1] - offsets[id], StandardCharsets.UTF_8);
    }

    synchronized byte[] utf8(int id) {
        checkId(id);
        return Arrays.copyOfRange(arena, offsets[id], offsets[id + 1]);
    }

    /**
     * Read-only zero-copy view of one immutable pooled UTF-8 slice.
     *
     * <p>The pool is append-only: admitted bytes are never modified. Arena growth may replace the
     * active backing array, but an existing view safely retains the old immutable byte array.</p>
     */
    synchronized ByteBuffer utf8View(int id) {
        checkId(id);
        return ByteBuffer.wrap(arena, offsets[id], offsets[id + 1] - offsets[id])
                .slice()
                .asReadOnlyBuffer();
    }

    /**
     * Stable logical coordinate for an admitted immutable UTF-8 spelling.
     *
     * <p>Arena growth may replace the Java byte[] object, but append-only logical offsets do not
     * move. Therefore this coordinate is stable for the lifetime of this pool without exposing a
     * GC-sensitive Java heap address.</p>
     */
    synchronized MIndexAddress storageAddress(int id) {
        checkId(id);
        return new MIndexAddress(
                regionId,
                offsets[id],
                offsets[id + 1] - offsets[id],
                STORAGE_GENERATION);
    }

    synchronized MIndexRef ref(int id) {
        return new MIndexRef(id, storageAddress(id));
    }

    synchronized int length(int id) { checkId(id); return utf16Lengths[id]; }
    synchronized int utf8Length(int id) {
        checkId(id);
        return offsets[id + 1] - offsets[id];
    }
    synchronized int javaHash(int id) { checkId(id); return javaHashes[id]; }
    synchronized long hash64(int id) { checkId(id); return hashes[id]; }
    synchronized boolean hasExceptionalUtf16(int id) {
        checkId(id);
        return exceptionalUtf16[id] != null;
    }

    /** Allocation-free UTF-16 code-unit access for ordinary pooled values. */
    synchronized char charAt(int id, int index) {
        checkId(id);
        if (index < 0 || index >= utf16Lengths[id]) {
            return materialize(id).charAt(index);
        }
        String exceptional = exceptionalUtf16[id];
        if (exceptional != null) return exceptional.charAt(index);

        int start = offsets[id];
        int end = offsets[id + 1];
        if (end - start == utf16Lengths[id]) {
            return (char) Byte.toUnsignedInt(arena[start + index]);
        }
        int utf16 = 0;
        int cursor = start;
        int[] checkpoints = checkpointsFor(id, index);
        if (checkpoints != null) {
            int entry = (index / UTF16_CHECKPOINT_STRIDE - 1) * 2;
            utf16 = checkpoints[entry];
            cursor += checkpoints[entry + 1];
        }
        for (; cursor < end; ) {
            int head = Byte.toUnsignedInt(arena[cursor]);
            int width = utf8Width(head);
            int codePoint = decodeUtf8(arena, cursor, width);
            if (width == 4) {
                int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                char high = (char) (Character.MIN_HIGH_SURROGATE + (plane >>> 10));
                char low = (char) (Character.MIN_LOW_SURROGATE + (plane & 0x3ff));
                if (utf16 == index) return high;
                if (utf16 + 1 == index) return low;
                utf16 += 2;
            } else {
                if (utf16 == index) return (char) codePoint;
                utf16++;
            }
            cursor += width;
        }
        throw new AssertionError("pooled UTF-16 coordinate mismatch");
    }

    /**
     * Admit one String-compatible substring directly from the immutable UTF-8 arena when both
     * boundaries are code-point boundaries. Surrogate-splitting and exceptional UTF-16 inputs use
     * the existing exact String path.
     */
    synchronized int substring(int id, int beginIndex, int endIndex) {
        checkId(id);
        if (beginIndex < 0 || endIndex > utf16Lengths[id] || beginIndex > endIndex) {
            return intern(materialize(id).substring(beginIndex, endIndex));
        }
        if (beginIndex == 0 && endIndex == utf16Lengths[id]) return id;
        if (exceptionalUtf16[id] != null) {
            return intern(materialize(id).substring(beginIndex, endIndex));
        }

        int byteStart = utf8BoundaryForUtf16Index(id, beginIndex);
        int byteEnd = byteStart < 0 ? -1
                : utf8BoundaryForUtf16Index(id, endIndex, byteStart, beginIndex);
        if (byteStart < 0 || byteEnd < 0) {
            return intern(materialize(id).substring(beginIndex, endIndex));
        }
        return internUtf8(arena, byteStart, byteEnd - byteStart);
    }

    /**
     * String.indexOf(String) over the immutable UTF-8 arena without admitting the query.
     * Exceptional UTF-16 values retain the JDK path.
     */
    synchronized int indexOf(int id, String needle) {
        checkId(id);
        if (needle == null) return materialize(id).indexOf(needle);
        if (needle.isEmpty()) return 0;
        if (exceptionalUtf16[id] != null || hasUnpairedSurrogate(needle)) {
            return materialize(id).indexOf(needle);
        }

        byte[] query = needle.getBytes(StandardCharsets.UTF_8);
        int sourceStart = offsets[id];
        int sourceEnd = offsets[id + 1];
        if (query.length > sourceEnd - sourceStart) return -1;
        int byteMatch = indexOfUtf8(sourceStart, sourceEnd, query);
        return byteMatch < 0 ? -1 : utf16Length(sourceStart, byteMatch);
    }

    /** String.indexOf(int) over pooled UTF-8 while retaining UTF-16 result coordinates. */
    /** String.indexOf(String) between two pooled values without materialization. */
    synchronized int indexOfPooled(int valueId, int needleId) {
        checkId(valueId);
        checkId(needleId);
        if (exceptionalUtf16[valueId] != null || exceptionalUtf16[needleId] != null) {
            return materialize(valueId).indexOf(materialize(needleId));
        }

        int valueStart = offsets[valueId];
        int valueEnd = offsets[valueId + 1];
        int needleStart = offsets[needleId];
        int needleEnd = offsets[needleId + 1];
        int needleLength = needleEnd - needleStart;
        if (needleLength == 0) return 0;
        if (needleLength > valueEnd - valueStart) return -1;

        int byteOffset =
                valueEnd - valueStart >= 256 && needleLength >= 4
                        ? MIndexNativeOps.indexOfBytes(
                                arena,
                                valueStart,
                                valueEnd,
                                arena,
                                needleStart,
                                needleEnd)
                        : MIndexNativeOps.referenceIndexOfBytes(
                                arena,
                                valueStart,
                                valueEnd,
                                arena,
                                needleStart,
                                needleEnd);
        return byteOffset < 0 ? -1 : utf16Length(valueStart, valueStart + byteOffset);
    }

    synchronized int indexOf(int id, int value) {
        checkId(id);
        if (exceptionalUtf16[id] != null) return materialize(id).indexOf(value);
        if (!Character.isValidCodePoint(value)) return -1;

        int utf16 = 0;
        int end = offsets[id + 1];
        for (int cursor = offsets[id]; cursor < end; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            if (width == 4) {
                int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                int high = Character.MIN_HIGH_SURROGATE + (plane >>> 10);
                int low = Character.MIN_LOW_SURROGATE + (plane & 0x3ff);
                if (value == codePoint || value == high) return utf16;
                if (value == low) return utf16 + 1;
                utf16 += 2;
            } else {
                if (value == codePoint) return utf16;
                utf16++;
            }
            cursor += width;
        }
        return -1;
    }

    /** Java String isBlank semantics over the existing strict UTF-8 representation. */
    synchronized boolean isBlank(int id) {
        checkId(id);
        if (exceptionalUtf16[id] != null) return materialize(id).isBlank();

        int end = offsets[id + 1];
        for (int cursor = offsets[id]; cursor < end; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            if (!Character.isWhitespace(codePoint)) return false;
            cursor += width;
        }
        return true;
    }

    /** Java String trim semantics; ordinary trim units are single-byte UTF-8 values <= U+0020. */
    synchronized int trim(int id) {
        checkId(id);
        if (exceptionalUtf16[id] != null) return intern(materialize(id).trim());

        int start = offsets[id];
        int end = offsets[id + 1];
        while (start < end && Byte.toUnsignedInt(arena[start]) <= 0x20) start++;
        while (end > start && Byte.toUnsignedInt(arena[end - 1]) <= 0x20) end--;
        if (start == offsets[id] && end == offsets[id + 1]) return id;
        return internUtf8(arena, start, end - start);
    }

    /** Java String strip semantics using Character.isWhitespace over decoded code points. */
    synchronized int strip(int id) {
        checkId(id);
        if (exceptionalUtf16[id] != null) return intern(materialize(id).strip());

        int sourceStart = offsets[id];
        int sourceEnd = offsets[id + 1];
        int first = -1;
        int lastEnd = -1;
        for (int cursor = sourceStart; cursor < sourceEnd; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            int next = cursor + width;
            if (!Character.isWhitespace(codePoint)) {
                if (first < 0) first = cursor;
                lastEnd = next;
            }
            cursor = next;
        }
        if (first < 0) return internUtf8(arena, sourceEnd, 0);
        if (first == sourceStart && lastEnd == sourceEnd) return id;
        return internUtf8(arena, first, lastEnd - first);
    }

    /**
     * Java String lexicographic comparison over exact UTF-16 code units without materializing
     * ordinary pooled values.
     */
    synchronized int compare(int leftId, int rightId) {
        checkId(leftId);
        checkId(rightId);
        if (leftId == rightId) return 0;
        if (exceptionalUtf16[leftId] != null || exceptionalUtf16[rightId] != null) {
            return materialize(leftId).compareTo(materialize(rightId));
        }

        int leftCursor = offsets[leftId];
        int leftEnd = offsets[leftId + 1];
        int rightCursor = offsets[rightId];
        int rightEnd = offsets[rightId + 1];
        int leftPending = -1;
        int rightPending = -1;

        while (leftPending >= 0 || leftCursor < leftEnd
                || rightPending >= 0 || rightCursor < rightEnd) {
            boolean leftAvailable = leftPending >= 0 || leftCursor < leftEnd;
            boolean rightAvailable = rightPending >= 0 || rightCursor < rightEnd;
            if (!leftAvailable || !rightAvailable) {
                return utf16Lengths[leftId] - utf16Lengths[rightId];
            }

            int leftUnit;
            if (leftPending >= 0) {
                leftUnit = leftPending;
                leftPending = -1;
            } else {
                int width = utf8Width(Byte.toUnsignedInt(arena[leftCursor]));
                int codePoint = decodeUtf8(arena, leftCursor, width);
                leftCursor += width;
                if (codePoint > Character.MAX_VALUE) {
                    int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                    leftUnit = Character.MIN_HIGH_SURROGATE + (plane >>> 10);
                    leftPending = Character.MIN_LOW_SURROGATE + (plane & 0x3ff);
                } else {
                    leftUnit = codePoint;
                }
            }

            int rightUnit;
            if (rightPending >= 0) {
                rightUnit = rightPending;
                rightPending = -1;
            } else {
                int width = utf8Width(Byte.toUnsignedInt(arena[rightCursor]));
                int codePoint = decodeUtf8(arena, rightCursor, width);
                rightCursor += width;
                if (codePoint > Character.MAX_VALUE) {
                    int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                    rightUnit = Character.MIN_HIGH_SURROGATE + (plane >>> 10);
                    rightPending = Character.MIN_LOW_SURROGATE + (plane & 0x3ff);
                } else {
                    rightUnit = codePoint;
                }
            }

            if (leftUnit != rightUnit) return leftUnit - rightUnit;
        }
        return 0;
    }

    /**
     * Java String char replacement directly over the immutable UTF-8 slice when both code units
     * are non-surrogate BMP values.
     */
    synchronized int replaceChar(int id, char oldChar, char newChar) {
        checkId(id);
        if (oldChar == newChar) return id;
        if (exceptionalUtf16[id] != null
                || Character.isSurrogate(oldChar)
                || Character.isSurrogate(newChar)) {
            return intern(materialize(id).replace(oldChar, newChar));
        }

        int sourceStart = offsets[id];
        int sourceEnd = offsets[id + 1];
        int replacementWidth = utf8Length(newChar);
        int matches = 0;
        long targetLength = sourceEnd - sourceStart;
        for (int cursor = sourceStart; cursor < sourceEnd; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            if (codePoint == oldChar) {
                matches++;
                targetLength += replacementWidth - width;
            }
            cursor += width;
        }
        if (matches == 0) return id;
        if (targetLength > Integer.MAX_VALUE) {
            return intern(materialize(id).replace(oldChar, newChar));
        }

        byte[] replaced = new byte[(int) targetLength];
        int out = 0;
        for (int cursor = sourceStart; cursor < sourceEnd; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            if (codePoint == oldChar) {
                out += encodeBmpUtf8(newChar, replaced, out);
            } else {
                System.arraycopy(arena, cursor, replaced, out, width);
                out += width;
            }
            cursor += width;
        }
        if (out != replaced.length) throw new AssertionError("pooled replacement length mismatch");
        return internUtf8(replaced, 0, replaced.length);
    }

    /** Java String repeat semantics over ordinary immutable UTF-8 bytes. */
    synchronized int repeat(int id, int count) {
        checkId(id);
        if (count < 0) throw new IllegalArgumentException("count is negative: " + count);
        if (count == 1) return id;
        if (count == 0 || utf16Lengths[id] == 0) {
            return internUtf8(arena, offsets[id], 0);
        }
        if (exceptionalUtf16[id] != null) {
            return intern(materialize(id).repeat(count));
        }

        int sourceStart = offsets[id];
        int sourceLength = offsets[id + 1] - sourceStart;
        long targetLength = (long) sourceLength * count;
        if (targetLength > Integer.MAX_VALUE) {
            return intern(materialize(id).repeat(count));
        }

        byte[] repeated = new byte[(int) targetLength];
        System.arraycopy(arena, sourceStart, repeated, 0, sourceLength);
        int copied = sourceLength;
        while (copied < repeated.length) {
            int amount = Math.min(copied, repeated.length - copied);
            System.arraycopy(repeated, 0, repeated, copied, amount);
            copied += amount;
        }
        return internUtf8(repeated, 0, repeated.length);
    }

    /**
     * Join two ordinary pooled values directly into canonical storage.
     *
     * <p>Both source ranges are already strict immutable UTF-8, so the join can hash and compare
     * the logical ranges in place. A miss copies each source range exactly once into the canonical
     * arena; no temporary flattened byte array is created. Exceptional UTF-16 retains the exact
     * String fallback because a surrogate pair may form across the concatenation boundary.</p>
     */
    synchronized int concat(int leftId, int rightId) {
        checkId(leftId);
        checkId(rightId);
        if (utf16Lengths[leftId] == 0) return rightId;
        if (utf16Lengths[rightId] == 0) return leftId;
        if (exceptionalUtf16[leftId] != null || exceptionalUtf16[rightId] != null) {
            return intern(materialize(leftId).concat(materialize(rightId)));
        }
        return internJoinedUtf8(leftId, rightId);
    }

    private int internJoinedUtf8(int leftId, int rightId) {
        int leftStart = offsets[leftId];
        int leftLength = offsets[leftId + 1] - leftStart;
        int rightStart = offsets[rightId];
        int rightLength = offsets[rightId + 1] - rightStart;
        int byteLength = Math.addExact(leftLength, rightLength);

        long hash = MIndexDictionary.beginHash64(byteLength);
        for (int index = leftStart, end = leftStart + leftLength; index < end; index++) {
            hash = MIndexDictionary.updateHash64(hash, arena[index]);
        }
        for (int index = rightStart, end = rightStart + rightLength; index < end; index++) {
            hash = MIndexDictionary.updateHash64(hash, arena[index]);
        }
        hash = MIndexDictionary.finishHash64(hash);

        int existing =
                findJoinedUtf8(leftStart, leftLength, rightStart, rightLength, byteLength, hash);
        if (existing >= 0) return existing;

        int utf16Length = Math.addExact(utf16Lengths[leftId], utf16Lengths[rightId]);
        int javaHash =
                TextMetadata.concatHash(
                        javaHashes[leftId], javaHashes[rightId], utf16Lengths[rightId]);

        /*
         * Arena growth replaces the active byte array. Retain the old immutable payload so source
         * coordinates remain valid while copying into a grown arena.
         */
        byte[] sourceArena = arena;
        int required = Math.addExact(arenaSize, byteLength);
        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(required);
        ensureTableCapacity(size + 1);

        int id = size;
        offsets[id] = arenaSize;
        System.arraycopy(sourceArena, leftStart, arena, arenaSize, leftLength);
        arenaSize += leftLength;
        System.arraycopy(sourceArena, rightStart, arena, arenaSize, rightLength);
        arenaSize += rightLength;
        offsets[id + 1] = arenaSize;
        utf16Lengths[id] = utf16Length;
        javaHashes[id] = javaHash;
        exceptionalUtf16[id] = null;
        hashes[id] = hash;
        size++;
        insert(id);
        return id;
    }

    /**
     * Join an existing vector of pooled values directly into one canonical entry.
     *
     * <p>The caller retains the id vector; this method never mutates it. Ordinary UTF-8 ranges are
     * hashed, exact-compared and copied directly from the append-only arena. Exceptional UTF-16
     * uses the existing exact String path because surrogate pairing can change at segment
     * boundaries.</p>
     */
    synchronized int concat(int[] ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.length == 0) return intern("");

        int byteLength = 0;
        int utf16Length = 0;
        int javaHash = 0;
        boolean exceptional = false;
        for (int id : ids) {
            checkId(id);
            byteLength = Math.addExact(byteLength, offsets[id + 1] - offsets[id]);
            utf16Length = Math.addExact(utf16Length, utf16Lengths[id]);
            javaHash = TextMetadata.concatHash(javaHash, javaHashes[id], utf16Lengths[id]);
            exceptional |= exceptionalUtf16[id] != null;
        }
        if (utf16Length == 0) return ids[0];
        if (ids.length == 1) return ids[0];

        if (exceptional) {
            StringBuilder joined = new StringBuilder(utf16Length);
            for (int id : ids) joined.append(materialize(id));
            return intern(joined.toString());
        }
        return internJoinedUtf8(ids, byteLength, utf16Length, javaHash);
    }

    private int internJoinedUtf8(
            int[] ids, int byteLength, int utf16Length, int javaHash) {
        long hash = MIndexDictionary.beginHash64(byteLength);
        for (int id : ids) {
            int start = offsets[id];
            int end = offsets[id + 1];
            for (int index = start; index < end; index++) {
                hash = MIndexDictionary.updateHash64(hash, arena[index]);
            }
        }
        hash = MIndexDictionary.finishHash64(hash);

        int existing = findJoinedUtf8(ids, byteLength, hash);
        if (existing >= 0) return existing;

        byte[] sourceArena = arena;
        int required = Math.addExact(arenaSize, byteLength);
        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(required);
        ensureTableCapacity(size + 1);

        int id = size;
        offsets[id] = arenaSize;
        for (int sourceId : ids) {
            int sourceStart = offsets[sourceId];
            int sourceLength = offsets[sourceId + 1] - sourceStart;
            System.arraycopy(sourceArena, sourceStart, arena, arenaSize, sourceLength);
            arenaSize += sourceLength;
        }
        offsets[id + 1] = arenaSize;
        utf16Lengths[id] = utf16Length;
        javaHashes[id] = javaHash;
        exceptionalUtf16[id] = null;
        hashes[id] = hash;
        size++;
        insert(id);
        return id;
    }

    /** Join one pooled value with arbitrary text while avoiding source materialization when safe. */
    synchronized int concat(int leftId, String right) {
        checkId(leftId);
        Objects.requireNonNull(right, "right");
        if (right.isEmpty()) return leftId;
        if (exceptionalUtf16[leftId] != null || hasUnpairedSurrogate(right)) {
            return intern(materialize(leftId).concat(right));
        }

        byte[] suffix = right.getBytes(StandardCharsets.UTF_8);
        int leftStart = offsets[leftId];
        int leftLength = offsets[leftId + 1] - leftStart;
        byte[] joined = new byte[Math.addExact(leftLength, suffix.length)];
        System.arraycopy(arena, leftStart, joined, 0, leftLength);
        System.arraycopy(suffix, 0, joined, leftLength, suffix.length);
        return internUtf8(joined, 0, joined.length);
    }

    synchronized boolean startsWith(int valueId, String prefix) {
        checkId(valueId);
        Objects.requireNonNull(prefix, "prefix");
        if (exceptionalUtf16[valueId] != null || hasUnpairedSurrogate(prefix)) {
            return materialize(valueId).startsWith(prefix);
        }
        byte[] query = prefix.getBytes(StandardCharsets.UTF_8);
        int valueStart = offsets[valueId];
        int valueLength = offsets[valueId + 1] - valueStart;
        if (query.length > valueLength) return false;
        for (int index = 0; index < query.length; index++) {
            if (arena[valueStart + index] != query[index]) return false;
        }
        return true;
    }

    synchronized boolean endsWith(int valueId, String suffix) {
        checkId(valueId);
        Objects.requireNonNull(suffix, "suffix");
        if (exceptionalUtf16[valueId] != null || hasUnpairedSurrogate(suffix)) {
            return materialize(valueId).endsWith(suffix);
        }
        byte[] query = suffix.getBytes(StandardCharsets.UTF_8);
        int valueStart = offsets[valueId];
        int valueLength = offsets[valueId + 1] - valueStart;
        if (query.length > valueLength) return false;
        int candidate = valueStart + valueLength - query.length;
        for (int index = 0; index < query.length; index++) {
            if (arena[candidate + index] != query[index]) return false;
        }
        return true;
    }

    synchronized boolean startsWith(int valueId, int prefixId) {
        checkId(valueId);
        checkId(prefixId);
        if (exceptionalUtf16[valueId] != null || exceptionalUtf16[prefixId] != null) {
            return materialize(valueId).startsWith(materialize(prefixId));
        }
        int valueStart = offsets[valueId];
        int valueLength = offsets[valueId + 1] - valueStart;
        int prefixStart = offsets[prefixId];
        int prefixLength = offsets[prefixId + 1] - prefixStart;
        return prefixLength <= valueLength && bytesEqual(valueStart, prefixStart, prefixLength);
    }

    synchronized boolean endsWith(int valueId, int suffixId) {
        checkId(valueId);
        checkId(suffixId);
        if (exceptionalUtf16[valueId] != null || exceptionalUtf16[suffixId] != null) {
            return materialize(valueId).endsWith(materialize(suffixId));
        }
        int valueStart = offsets[valueId];
        int valueLength = offsets[valueId + 1] - valueStart;
        int suffixStart = offsets[suffixId];
        int suffixLength = offsets[suffixId + 1] - suffixStart;
        return suffixLength <= valueLength
                && bytesEqual(valueStart + valueLength - suffixLength, suffixStart, suffixLength);
    }

    synchronized boolean contains(int valueId, int needleId) {
        checkId(valueId);
        checkId(needleId);
        if (exceptionalUtf16[valueId] != null || exceptionalUtf16[needleId] != null) {
            return materialize(valueId).contains(materialize(needleId));
        }
        int valueStart = offsets[valueId];
        int valueLength = offsets[valueId + 1] - valueStart;
        int needleStart = offsets[needleId];
        int needleLength = offsets[needleId + 1] - needleStart;
        if (needleLength == 0) return true;
        if (needleLength > valueLength) return false;
        int last = valueStart + valueLength - needleLength;
        for (int candidate = valueStart; candidate <= last; candidate++) {
            if (arena[candidate] == arena[needleStart]
                    && bytesEqual(candidate, needleStart, needleLength)) {
                return true;
            }
        }
        return false;
    }

    synchronized MIndexString canonical(int id) {
        checkId(id);
        WeakReference<MIndexString> reference = canonical[id];
        MIndexString value = reference == null ? null : reference.get();
        if (value == null) {
            value = MIndexString.wrap(this, id);
            canonical[id] = new WeakReference<>(value);
        }
        return value;
    }

    synchronized int size() { return size; }
    synchronized long retainedUtf8Bytes() { return arenaSize; }
    /** Bytes in checkpoint int-array payloads, excluding Java array headers and references. */
    synchronized long retainedUtf16CheckpointBytes() { return retainedUtf16CheckpointBytes; }

    private boolean bytesEqual(int leftStart, int rightStart, int length) {
        for (int index = 0; index < length; index++) {
            if (arena[leftStart + index] != arena[rightStart + index]) return false;
        }
        return true;
    }

    private int utf8BoundaryForUtf16Index(int id, int target) {
        return utf8BoundaryForUtf16Index(id, target, offsets[id], 0);
    }

    /** Continue from a verified code-point boundary; ordinary ASCII uses exact stored lengths. */
    private int utf8BoundaryForUtf16Index(int id, int target, int byteStart, int utf16) {
        if (target == 0) return offsets[id];
        int end = offsets[id + 1];
        if (target == utf16Lengths[id]) return end;
        if (end - offsets[id] == utf16Lengths[id]) return offsets[id] + target;

        if (byteStart == offsets[id] && utf16 == 0) {
            int[] checkpoints = checkpointsFor(id, target);
            if (checkpoints != null) {
                int entry = (target / UTF16_CHECKPOINT_STRIDE - 1) * 2;
                utf16 = checkpoints[entry];
                byteStart += checkpoints[entry + 1];
            }
        }

        for (int cursor = byteStart; cursor < end; ) {
            if (utf16 == target) return cursor;
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int units = width == 4 ? 2 : 1;
            if (target > utf16 && target < utf16 + units) return -1;
            utf16 += units;
            cursor += width;
        }
        return utf16 == target ? end : -1;
    }

    /**
     * Each pair gives the last code-point boundary at or before a multiple of 64 UTF-16 units.
     * A supplementary code point can straddle that multiple, so both coordinates are stored.
     * The arena is append-only and entry offsets never move; growing the arena keeps these
     * relative offsets valid. Callers hold the pool monitor while building and reading the table.
     */
    private int[] checkpointsFor(int id, int target) {
        int length = utf16Lengths[id];
        if (target < UTF16_CHECKPOINT_STRIDE
                || length < UTF16_CHECKPOINT_MIN_LENGTH
                || length == offsets[id + 1] - offsets[id]) {
            return null;
        }
        int[] checkpoints = utf16Checkpoints[id];
        if (checkpoints != null) return checkpoints;

        int count = (length - 1) / UTF16_CHECKPOINT_STRIDE;
        checkpoints = new int[count * 2];
        int next = UTF16_CHECKPOINT_STRIDE;
        int entry = 0;
        int start = offsets[id];
        int end = offsets[id + 1];
        int utf16 = 0;
        for (int cursor = start; cursor < end; ) {
            int priorCursor = cursor;
            int priorUtf16 = utf16;
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            cursor += width;
            utf16 += width == 4 ? 2 : 1;
            if (entry < count && utf16 >= next) {
                boolean exact = utf16 == next;
                checkpoints[entry * 2] = exact ? utf16 : priorUtf16;
                checkpoints[entry * 2 + 1] = (exact ? cursor : priorCursor) - start;
                entry++;
                next += UTF16_CHECKPOINT_STRIDE;
            }
        }
        if (entry != count || utf16 != length) {
            throw new AssertionError("pooled UTF-16 checkpoint mismatch");
        }
        utf16Checkpoints[id] = checkpoints;
        retainedUtf16CheckpointBytes += (long) checkpoints.length * Integer.BYTES;
        return checkpoints;
    }

    private int utf16Length(int byteStart, int byteEnd) {
        int length = 0;
        for (int cursor = byteStart; cursor < byteEnd; ) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            length = Math.addExact(length, width == 4 ? 2 : 1);
            cursor += width;
        }
        return length;
    }

    private int indexOfUtf8(int sourceStart, int sourceEnd, byte[] query) {
        if (query.length == 1) {
            for (int index = sourceStart; index < sourceEnd; index++) {
                if (arena[index] == query[0]) return index;
            }
            return -1;
        }

        int[] failure = new int[query.length];
        for (int index = 1, prefix = 0; index < query.length; index++) {
            while (prefix > 0 && query[index] != query[prefix]) {
                prefix = failure[prefix - 1];
            }
            if (query[index] == query[prefix]) prefix++;
            failure[index] = prefix;
        }

        for (int index = sourceStart, matched = 0; index < sourceEnd; index++) {
            while (matched > 0 && arena[index] != query[matched]) {
                matched = failure[matched - 1];
            }
            if (arena[index] == query[matched]) matched++;
            if (matched == query.length) return index - query.length + 1;
        }
        return -1;
    }

    private static int utf8Width(int head) {
        if (head < 0x80) return 1;
        if ((head & 0xe0) == 0xc0) return 2;
        if ((head & 0xf0) == 0xe0) return 3;
        return 4;
    }

    private static int utf8Length(char value) {
        if (value <= 0x7f) return 1;
        if (value <= 0x7ff) return 2;
        return 3;
    }

    private static int encodeBmpUtf8(char value, byte[] target, int offset) {
        if (value <= 0x7f) {
            target[offset] = (byte) value;
            return 1;
        }
        if (value <= 0x7ff) {
            target[offset] = (byte) (0xc0 | (value >>> 6));
            target[offset + 1] = (byte) (0x80 | (value & 0x3f));
            return 2;
        }
        target[offset] = (byte) (0xe0 | (value >>> 12));
        target[offset + 1] = (byte) (0x80 | ((value >>> 6) & 0x3f));
        target[offset + 2] = (byte) (0x80 | (value & 0x3f));
        return 3;
    }

    private static int decodeUtf8(byte[] bytes, int offset, int width) {
        int head = Byte.toUnsignedInt(bytes[offset]);
        if (width == 1) return head;
        int codePoint = width == 2 ? head & 0x1f : width == 3 ? head & 0x0f : head & 0x07;
        for (int index = 1; index < width; index++) {
            codePoint = (codePoint << 6) | (Byte.toUnsignedInt(bytes[offset + index]) & 0x3f);
        }
        return codePoint;
    }

    private int findJoinedUtf8(int[] ids, int queryLength, long hash) {
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hash) & mask;
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            int candidateStart = offsets[id];
            int candidateLength = offsets[id + 1] - candidateStart;
            if (hashes[id] == hash
                    && candidateLength == queryLength
                    && exceptionalUtf16[id] == null
                    && bytesEqualJoined(candidateStart, ids)) {
                return id;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean bytesEqualJoined(int candidateStart, int[] ids) {
        int candidate = candidateStart;
        for (int id : ids) {
            int start = offsets[id];
            int end = offsets[id + 1];
            for (int index = start; index < end; index++) {
                if (arena[candidate++] != arena[index]) return false;
            }
        }
        return true;
    }

    private int findJoinedUtf8(
            int leftStart,
            int leftLength,
            int rightStart,
            int rightLength,
            int queryLength,
            long hash) {
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hash) & mask;
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            int candidateStart = offsets[id];
            int candidateLength = offsets[id + 1] - candidateStart;
            if (hashes[id] == hash
                    && candidateLength == queryLength
                    && exceptionalUtf16[id] == null
                    && bytesEqualJoined(
                            candidateStart, leftStart, leftLength, rightStart, rightLength)) {
                return id;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean bytesEqualJoined(
            int candidateStart,
            int leftStart,
            int leftLength,
            int rightStart,
            int rightLength) {
        int candidate = candidateStart;
        for (int index = leftStart, end = leftStart + leftLength; index < end; index++) {
            if (arena[candidate++] != arena[index]) return false;
        }
        for (int index = rightStart, end = rightStart + rightLength; index < end; index++) {
            if (arena[candidate++] != arena[index]) return false;
        }
        return true;
    }

    private int findString(String query) {
        int javaHash = query.hashCode();
        int utf16Length = query.length();
        int mask = javaSlots.length - 1;
        int slot = javaLookupHash(javaHash, utf16Length) & mask;
        while (true) {
            int stored = javaSlots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            if (javaHashes[id] == javaHash
                    && utf16Lengths[id] == utf16Length
                    && stringContentEquals(id, query)) {
                return id;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean stringContentEquals(int id, String query) {
        String exceptional = exceptionalUtf16[id];
        if (exceptional != null) return exceptional.equals(query);

        int cursor = offsets[id];
        int end = offsets[id + 1];

        // UTF-8 byte length equals UTF-16 length only for ASCII canonical payloads.
        if (end - cursor == query.length()) {
            for (int index = 0; index < query.length(); index++) {
                if (Byte.toUnsignedInt(arena[cursor + index]) != query.charAt(index)) return false;
            }
            return true;
        }

        int utf16 = 0;
        while (cursor < end) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            cursor += width;

            if (codePoint > Character.MAX_VALUE) {
                if (utf16 + 1 >= query.length()) return false;
                int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                char high =
                        (char) (Character.MIN_HIGH_SURROGATE + (plane >>> 10));
                char low =
                        (char) (Character.MIN_LOW_SURROGATE + (plane & 0x3ff));
                if (query.charAt(utf16) != high || query.charAt(utf16 + 1) != low) return false;
                utf16 += 2;
            } else {
                if (utf16 >= query.length() || query.charAt(utf16) != (char) codePoint) return false;
                utf16++;
            }
        }
        return utf16 == query.length();
    }

    private static int javaLookupHash(int javaHash, int utf16Length) {
        long key = ((long) javaHash << 32) ^ Integer.toUnsignedLong(utf16Length);
        return MIndexDictionary.mix(key);
    }

    private int find(byte[] query, long hash, String exceptional) {
        return find(query, 0, query.length, hash, exceptional);
    }

    private int find(
            byte[] query,
            int queryOffset,
            int queryLength,
            long hash,
            String exceptional) {
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hash) & mask;
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            int start = offsets[id];
            int length = offsets[id + 1] - start;
            if (hashes[id] == hash
                    && length == queryLength
                    && Objects.equals(exceptionalUtf16[id], exceptional)) {
                boolean same = true;
                for (int i = 0; i < queryLength; i++) {
                    if (arena[start + i] != query[queryOffset + i]) {
                        same = false;
                        break;
                    }
                }
                if (same) return id;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void insert(int id) {
        int mask = slots.length - 1;

        int slot = MIndexDictionary.mix(hashes[id]) & mask;
        while (slots[slot] != 0) slot = (slot + 1) & mask;
        slots[slot] = id + 1;

        int javaSlot = javaLookupHash(javaHashes[id], utf16Lengths[id]) & mask;
        while (javaSlots[javaSlot] != 0) javaSlot = (javaSlot + 1) & mask;
        javaSlots[javaSlot] = id + 1;
    }

    private void ensureTableCapacity(int required) {
        if (required <= (int) (slots.length * LOAD_FACTOR)) return;
        int capacity = Math.multiplyExact(slots.length, 2);
        slots = new int[capacity];
        javaSlots = new int[capacity];
        for (int id = 0; id < size; id++) insert(id);
    }

    private void ensureEntryCapacity(int required) {
        if (required <= hashes.length) return;
        int capacity = hashes.length;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        hashes = Arrays.copyOf(hashes, capacity);
        utf16Lengths = Arrays.copyOf(utf16Lengths, capacity);
        javaHashes = Arrays.copyOf(javaHashes, capacity);
        exceptionalUtf16 = Arrays.copyOf(exceptionalUtf16, capacity);
        utf16Checkpoints = Arrays.copyOf(utf16Checkpoints, capacity);
        offsets = Arrays.copyOf(offsets, capacity + 1);
        canonical = Arrays.copyOf(canonical, capacity);
    }

    private void ensureArenaCapacity(int required) {
        if (required <= arena.length) return;
        int capacity = arena.length;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        arena = Arrays.copyOf(arena, capacity);
    }

    private void checkId(int id) { Objects.checkIndex(id, size); }

    private static Utf8Metadata validateUtf8(byte[] bytes, int offset, int length) {
        int end = offset + length;
        int cursor = offset;
        int utf16Length = 0;
        int javaHash = 0;
        long contentHash = MIndexDictionary.beginHash64(length);

        while (cursor < end) {
            int at = cursor;
            int head = Byte.toUnsignedInt(bytes[cursor++]);
            contentHash = MIndexDictionary.updateHash64(contentHash, head);
            int codePoint;
            int continuation;
            int minimum;

            if (head < 0x80) {
                codePoint = head;
                continuation = 0;
                minimum = 0;
            } else if (head >= 0xc2 && head <= 0xdf) {
                codePoint = head & 0x1f;
                continuation = 1;
                minimum = 0x80;
            } else if (head >= 0xe0 && head <= 0xef) {
                codePoint = head & 0x0f;
                continuation = 2;
                minimum = 0x800;
            } else if (head >= 0xf0 && head <= 0xf4) {
                codePoint = head & 0x07;
                continuation = 3;
                minimum = 0x10000;
            } else {
                throw malformedUtf8(at - offset);
            }

            if (continuation > end - cursor) throw malformedUtf8(at - offset);
            for (int i = 0; i < continuation; i++) {
                int tail = Byte.toUnsignedInt(bytes[cursor++]);
                contentHash = MIndexDictionary.updateHash64(contentHash, tail);
                if ((tail & 0xc0) != 0x80) throw malformedUtf8(at - offset);
                codePoint = (codePoint << 6) | (tail & 0x3f);
            }

            if (codePoint < minimum
                    || codePoint > Character.MAX_CODE_POINT
                    || codePoint >= Character.MIN_SURROGATE
                    && codePoint <= Character.MAX_SURROGATE) {
                throw malformedUtf8(at - offset);
            }

            if (codePoint <= Character.MAX_VALUE) {
                javaHash = 31 * javaHash + codePoint;
                utf16Length++;
            } else {
                int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                char high = (char) (Character.MIN_HIGH_SURROGATE + (plane >>> 10));
                char low = (char) (Character.MIN_LOW_SURROGATE + (plane & 0x3ff));
                javaHash = 31 * javaHash + high;
                javaHash = 31 * javaHash + low;
                utf16Length = Math.addExact(utf16Length, 2);
            }
        }
        return new Utf8Metadata(
                utf16Length,
                javaHash,
                MIndexDictionary.finishHash64(contentHash));
    }

    private static IllegalArgumentException malformedUtf8(int relativeOffset) {
        return new IllegalArgumentException(
                "malformed UTF-8 at relative byte offset " + relativeOffset);
    }

    private record Utf8Metadata(
            int utf16Length,
            int javaHash,
            long contentHash64) {}

    /** UTF-8 replacement bytes are not an exact identity for malformed UTF-16. */
    private static boolean hasUnpairedSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 == value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) return true;
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }

    synchronized void copyChars(
            int id, int sourceStart, char[] target, int targetStart, int count) {
        checkId(id);
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceStart, count, utf16Lengths[id]);
        Objects.checkFromIndexSize(targetStart, count, target.length);
        if (count == 0) return;

        String exceptional = exceptionalUtf16[id];
        if (exceptional != null) {
            exceptional.getChars(sourceStart, sourceStart + count, target, targetStart);
            return;
        }

        int byteStart = offsets[id];
        int byteEnd = offsets[id + 1];
        if (byteEnd - byteStart == utf16Lengths[id]) {
            for (int index = 0; index < count; index++) {
                target[targetStart + index] =
                        (char) Byte.toUnsignedInt(arena[byteStart + sourceStart + index]);
            }
            return;
        }

        int utf16 = 0;
        int cursor = byteStart;
        int[] checkpoints = checkpointsFor(id, sourceStart);
        if (checkpoints != null) {
            int entry = (sourceStart / UTF16_CHECKPOINT_STRIDE - 1) * 2;
            utf16 = checkpoints[entry];
            cursor += checkpoints[entry + 1];
        }
        int sourceEnd = sourceStart + count;
        int written = 0;
        while (utf16 < sourceEnd && cursor < byteEnd) {
            int width = utf8Width(Byte.toUnsignedInt(arena[cursor]));
            int codePoint = decodeUtf8(arena, cursor, width);
            if (width == 4) {
                int plane = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
                char high = (char) (Character.MIN_HIGH_SURROGATE + (plane >>> 10));
                char low = (char) (Character.MIN_LOW_SURROGATE + (plane & 0x3ff));
                if (utf16 >= sourceStart) target[targetStart + written++] = high;
                if (utf16 + 1 >= sourceStart && utf16 + 1 < sourceEnd) {
                    target[targetStart + written++] = low;
                }
                utf16 += 2;
            } else {
                if (utf16 >= sourceStart) target[targetStart + written++] = (char) codePoint;
                utf16++;
            }
            cursor += width;
        }
        if (written != count) throw new AssertionError("pooled UTF-16 range mismatch");
    }
}
