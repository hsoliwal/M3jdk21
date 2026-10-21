// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

// Modified 2026-10-21: completed native inventories, exact timestamp lanes and v1-compatible persistence.

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Versioned, immutable, array/offset/mask build facts owned by M3JDK21's M3FS.
 * The producer (M3File and M3AST/javac) supplies verified facts. Native inventory capture
 * delegates byte observation to M3FS; this class never parses code or infers ABI compatibility.
 * Dependency offsets contain the row indices on which each row depends.
 */
public final class M3BuildState {
    public static final byte SOURCE = 1, CLASS = 2, BUILD_INPUT = 3, DEPENDENCY = 4, RESOURCE = 5;
    public static final int HASH_BYTES = 32;
    private static final int MAGIC = 0x4d334253; // M3BS
    private static final int VERSION = 2;
    private static final int MAX_KEY_BYTES = 65_535;

    private final String[] paths;
    private final byte[] roles;
    private final long[] lengths;
    private final byte[] contentHashes;
    private final byte[] abiHashes;
    private final long[] abiKnown;
    private final int[] offsets;
    private final int[] dependencies;
    private final byte[] contextHash;
    private final long[] modifiedNanos;
    private final long[] modifiedKnown;
    private final byte[] inventoryScope;

    /** All input arrays are defensively copied. A zero ABI-known bit means NOT PROVEN. */
    public M3BuildState(String[] paths, byte[] roles, long[] lengths, byte[] contentHashes,
            byte[] abiHashes, long[] abiKnown, int[] offsets, int[] dependencies, byte[] contextHash) {
        this(paths, roles, lengths, contentHashes, abiHashes, abiKnown, offsets, dependencies, contextHash,
                new long[Objects.requireNonNull(paths, "paths").length],
                new long[(int) ((paths.length + 63L) >>> 6)], null);
    }

    private M3BuildState(String[] paths, byte[] roles, long[] lengths, byte[] contentHashes,
            byte[] abiHashes, long[] abiKnown, int[] offsets, int[] dependencies, byte[] contextHash,
            long[] modifiedNanos, long[] modifiedKnown, byte[] inventoryScope) {
        this.paths = Objects.requireNonNull(paths, "paths").clone();
        this.roles = Objects.requireNonNull(roles, "roles").clone();
        this.lengths = Objects.requireNonNull(lengths, "lengths").clone();
        this.contentHashes = Objects.requireNonNull(contentHashes, "contentHashes").clone();
        this.abiHashes = Objects.requireNonNull(abiHashes, "abiHashes").clone();
        this.abiKnown = Objects.requireNonNull(abiKnown, "abiKnown").clone();
        this.offsets = Objects.requireNonNull(offsets, "offsets").clone();
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies").clone();
        this.contextHash = Objects.requireNonNull(contextHash, "contextHash").clone();
        this.modifiedNanos = Objects.requireNonNull(modifiedNanos, "modifiedNanos").clone();
        this.modifiedKnown = Objects.requireNonNull(modifiedKnown, "modifiedKnown").clone();
        this.inventoryScope = inventoryScope == null ? null : inventoryScope.clone();
        validate();
    }

    private void validate() {
        int n = paths.length;
        if (roles.length != n || lengths.length != n ||
                contentHashes.length != Math.multiplyExact(n, HASH_BYTES) ||
                abiHashes.length != Math.multiplyExact(n, HASH_BYTES) ||
                abiKnown.length != (n + 63L) / 64L || offsets.length != n + 1 ||
                modifiedNanos.length != n || modifiedKnown.length != (n + 63L) / 64L ||
                inventoryScope != null && inventoryScope.length != HASH_BYTES ||
                contextHash.length != HASH_BYTES || offsets[0] != 0 || offsets[n] != dependencies.length) {
            throw new IllegalArgumentException("build state column geometry");
        }
        if (n % 64 != 0 && n != 0 && (abiKnown[abiKnown.length - 1] >>> (n & 63)) != 0) {
            throw new IllegalArgumentException("noncanonical ABI mask tail");
        }
        if (n != 0 && (n & 63) != 0 && (modifiedKnown[modifiedKnown.length - 1] >>> (n & 63)) != 0) {
            throw new IllegalArgumentException("noncanonical timestamp mask tail");
        }
        for (int i = 0; i < n; i++) {
            if (hasModifiedTime(i) ? modifiedNanos[i] < 0 : modifiedNanos[i] != 0) {
                throw new IllegalArgumentException("invalid known or unknown timestamp");
            }
            String key = paths[i];
            if (key == null || key.isEmpty() || key.indexOf('\0') >= 0 ||
                    key.startsWith("/") || key.indexOf('\\') >= 0 ||
                    !validSegments(key) ||
                    encodedKey(key).length > MAX_KEY_BYTES) {
                throw new IllegalArgumentException("invalid workspace-relative path");
            }
            if (roles[i] < SOURCE || roles[i] > RESOURCE || lengths[i] < 0L ||
                    (i > 0 && compare(paths[i - 1], roles[i - 1], key, roles[i]) >= 0)) {
                throw new IllegalArgumentException("unsorted or invalid build identity");
            }
            if (offsets[i] < 0 || offsets[i] > offsets[i + 1] || offsets[i + 1] > dependencies.length) {
                throw new IllegalArgumentException("invalid dependency offsets");
            }
            if (!abiAt(i) && !zeroHash(abiHashes, i)) {
                throw new IllegalArgumentException("unknown ABI must have zero lane");
            }
            for (int p = offsets[i]; p < offsets[i + 1]; p++) {
                int d = dependencies[p];
                if (d < 0 || d >= n || (p > offsets[i] && d <= dependencies[p - 1])) {
                    throw new IllegalArgumentException("unsorted or invalid dependency edge");
                }
            }
        }
    }

    /** Validate path segments without allocating a retained row-local segment list. */
    private static boolean validSegments(String key) {
        int start = 0;
        while (start <= key.length()) {
            int end = key.indexOf('/', start);
            if (end < 0) end = key.length();
            int length = end - start;
            if (length == 0 || (length == 1 && key.charAt(start) == '.') ||
                    (length == 2 && key.charAt(start) == '.' && key.charAt(start + 1) == '.')) {
                return false;
            }
            if (end == key.length()) return true;
            start = end + 1;
        }
        return false;
    }

    private static byte[] encodedKey(String key) {
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(key));
            byte[] result = new byte[buffer.remaining()]; buffer.get(result); return result;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("non-UTF8 build path", invalid);
        }
    }

    private static int compare(String a, byte roleA, String b, byte roleB) {
        int difference = a.compareTo(b);
        return difference != 0 ? difference : Byte.compare(roleA, roleB);
    }

    public int size() { return paths.length; }
    public String path(int row) { return paths[Objects.checkIndex(row, size())]; }
    public byte role(int row) { return roles[Objects.checkIndex(row, size())]; }
    public long length(int row) { return lengths[Objects.checkIndex(row, size())]; }
    public boolean hasAbi(int row) { Objects.checkIndex(row, size()); return abiAt(row); }
    private boolean abiAt(int row) { return (abiKnown[row >>> 6] & (1L << (row & 63))) != 0L; }
    public byte[] contextSha256() { return contextHash.clone(); }
    public int dependencyCount() { return dependencies.length; }
    public int dependency(int row, int ordinal) {
        Objects.checkIndex(row, size());
        return dependencies[Objects.checkIndex(offsets[row] + ordinal, offsets[row + 1])];
    }
    public int dependencyCount(int row) { Objects.checkIndex(row, size()); return offsets[row + 1] - offsets[row]; }
    public int find(String path, byte role) {
        int low = 0, high = size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1, diff = compare(paths[mid], roles[mid], path, role);
            if (diff == 0) return mid;
            if (diff < 0) low = mid + 1; else high = mid - 1;
        }
        return -1;
    }

    /** Producer-declared complete inventory, retained by the checksum image; not authenticated provenance. */
    public boolean completeInventory() { return inventoryScope != null; }
    public boolean hasModifiedTime(int row) { Objects.checkIndex(row, size()); return bit(modifiedKnown, row); }
    public java.util.OptionalLong modifiedTimeNanos(int row) {
        return hasModifiedTime(row) ? java.util.OptionalLong.of(modifiedNanos[row]) : java.util.OptionalLong.empty();
    }
    public byte[] contentSha256(int row) {
        Objects.checkIndex(row, size()); return Arrays.copyOfRange(contentHashes, row * HASH_BYTES, (row + 1) * HASH_BYTES);
    }

    /**
     * Collects a complete native-container input inventory using the existing M3FS stream owner.
     * Retained state and sorting scratch are O(N) primitive/reference columns, bounded by both
     * maxImageBytes and limits.maxEntries; payload transfer remains one 64-KiB buffer.
     * All rows are BUILD_INPUT with unknown ABI and no claimed dependency graph. The selected
     * root must be separate from /m3-build so publication cannot become its own build input.
     */
    public static M3BuildState captureInventory(M3FS snapshot, String root, byte[] contextHash,
            M3FS.InventoryLimits limits, int maxImageBytes, M3FsProgress progress) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot"); Objects.requireNonNull(root, "root");
        Objects.requireNonNull(limits, "limits"); Objects.requireNonNull(contextHash, "contextHash");
        if (contextHash.length != HASH_BYTES) throw new IllegalArgumentException("contextHash");
        byte[] requestedContext = contextHash.clone();
        if (maxImageBytes < 116) throw new IllegalArgumentException("maxImageBytes");
        if (root.equals("/") || root.equals("/m3-build") || root.startsWith("/m3-build/")) {
            throw new IOException("build inventory root must exclude the M3FS build-state namespace");
        }
        InventoryColumns columns = new InventoryColumns(maxImageBytes);
        M3FS.InventoryReceipt complete = snapshot.scanInventory(root, limits, progress, columns::add);
        if (complete.files() != columns.size) throw new IOException("inventory completion count mismatch");
        String[] sorted = Arrays.copyOf(columns.paths, columns.size); Arrays.sort(sorted);
        byte[] roles = new byte[columns.size]; Arrays.fill(roles, BUILD_INPUT);
        long[] lengths = new long[columns.size], times = new long[columns.size];
        byte[] hashes = new byte[Math.multiplyExact(columns.size, HASH_BYTES)];
        long[] known = new long[(int) ((columns.size + 63L) >>> 6)];
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i - 1].equals(sorted[i])) throw new IOException("duplicate inventory path");
        }
        for (int i = 0; i < columns.size; i++) {
            int row = Arrays.binarySearch(sorted, columns.paths[i]);
            lengths[row] = columns.lengths[i]; times[row] = columns.times[i]; set(known, row);
            System.arraycopy(columns.hashes, i * HASH_BYTES, hashes, row * HASH_BYTES, HASH_BYTES);
        }
        return new M3BuildState(sorted, roles, lengths, hashes, new byte[hashes.length],
                new long[known.length], new int[sorted.length + 1], new int[0], requestedContext,
                times, known, digest(complete.scope().getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Original M3BuildState V2 input receiver for a completed caller-owned
     * M3File traversal over the host provider. The caller has already verified
     * every admitted original file through EOF; this method neither opens
     * paths, reparses source, nor asserts compiler ABI/dependency facts.
     *
     * <p>All columns are caller-owned candidate arrays and are defensively
     * copied by the existing immutable V2 constructor. Scope is the verified
     * host-root and stable selection-policy digest (not an M3FS native inode).
     * Full inventory is a producer attestation, not an OS atomic snapshot.</p>
     */
    static M3BuildState completedHostRows(String[] paths, byte[] roles,
            long[] lengths, byte[] hashes, long[] modifiedNanos,
            long[] modifiedKnown, byte[] contextHash, byte[] scopeHash) {
        int count = Objects.requireNonNull(paths, "paths").length;
        return new M3BuildState(paths, roles, lengths, hashes,
                new byte[Math.multiplyExact(count, HASH_BYTES)],
                new long[(int) ((count + 63L) >>> 6)],
                new int[Math.addExact(count, 1)], new int[0], contextHash,
                modifiedNanos, modifiedKnown, scopeHash);
    }

    private static final class InventoryColumns {
        private final int maxBytes;
        private String[] paths = new String[0];
        private long[] lengths = new long[0], times = new long[0];
        private byte[] hashes = new byte[0];
        private int size;
        private long nameBytes;
        private InventoryColumns(int maxBytes) { this.maxBytes = maxBytes; }
        private void add(String path, M3FS.Stat metadata, MFileCapture capture) throws IOException {
            long names = Math.addExact(nameBytes, encodedKey(path).length);
            long next = (long) size + 1;
            long required = 116L + 89L * next + 16L * ((next + 63L) >>> 6) + names;
            if (required > maxBytes || next > Integer.MAX_VALUE / HASH_BYTES) {
                throw new IOException("build inventory image budget exceeded");
            }
            if (size == paths.length) {
                int capacity = Math.max(size + 1, (int) Math.min((long) Integer.MAX_VALUE / HASH_BYTES,
                        Math.max(8L, (long) paths.length * 2L)));
                // Do not let geometric spare capacity exceed the admitted image budget.
                capacity = Math.min(capacity, Math.max(size + 1, maxBytes / 89));
                paths = Arrays.copyOf(paths, capacity); lengths = Arrays.copyOf(lengths, capacity);
                times = Arrays.copyOf(times, capacity); hashes = Arrays.copyOf(hashes, capacity * HASH_BYTES);
            }
            paths[size] = path; lengths[size] = metadata.size(); times[size] = metadata.modifiedTimeNanos();
            System.arraycopy(capture.manifest().raw().id().bytes(), 0, hashes, size * HASH_BYTES, HASH_BYTES);
            size++; nameBytes = names;
        }
    }

    /**
     * Full file-membership delta for completed inventories of the same source scope and build context.
     * A different scope/context or legacy facts image is explicitly incomparable: no deletion mask
     * is inferred, and the caller must rebuild its full scope. Timestamp-only changes are optional;
     * an unknown timestamp becoming known is never interpreted as a modification by itself.
     */
    public InventoryDelta deltaFrom(M3BuildState previous, boolean includeTimestampChanges) {
        if (!completeInventory()) throw new IllegalStateException("delta requires a completed inventory");
        int oldSize = previous == null ? 0 : previous.size();
        long[] added = new long[(int) ((size() + 63L) >>> 6)];
        long[] modified = new long[added.length], removed = new long[(int) ((oldSize + 63L) >>> 6)];
        if (previous != null && (!previous.completeInventory()
                || !Arrays.equals(contextHash, previous.contextHash)
                || !Arrays.equals(inventoryScope, previous.inventoryScope))) {
            return new InventoryDelta(added, modified, removed, size(), oldSize, false, false);
        }
        int now = 0, old = 0;
        while (now < size() || old < oldSize) {
            int comparison = now == size() ? 1 : old == oldSize ? -1
                    : compare(paths[now], roles[now], previous.paths[old], previous.roles[old]);
            if (comparison < 0) { set(added, now++); }
            else if (comparison > 0) { set(removed, old++); }
            else {
                if (lengths[now] != previous.lengths[old]
                        || !sameHash(contentHashes, now, previous.contentHashes, old)
                        || includeTimestampChanges && hasModifiedTime(now) && previous.hasModifiedTime(old)
                            && modifiedNanos[now] != previous.modifiedNanos[old]) set(modified, now);
                now++; old++;
            }
        }
        return new InventoryDelta(added, modified, removed, size(), oldSize, true, previous == null);
    }

    /** Masks index current rows for additions/modifications and previous rows for removals. */
    public record InventoryDelta(long[] addedMask, long[] modifiedMask, long[] removedMask,
            int size, int previousSize, boolean comparable, boolean initial) {
        public InventoryDelta {
            addedMask = addedMask.clone(); modifiedMask = modifiedMask.clone(); removedMask = removedMask.clone();
            if (size < 0 || previousSize < 0 || addedMask.length != (size + 63L) / 64L
                    || modifiedMask.length != addedMask.length || removedMask.length != (previousSize + 63L) / 64L) {
                throw new IllegalArgumentException("inventory delta geometry");
            }
        }
        @Override public long[] addedMask() { return addedMask.clone(); }
        @Override public long[] modifiedMask() { return modifiedMask.clone(); }
        @Override public long[] removedMask() { return removedMask.clone(); }
        public boolean added(int row) { Objects.checkIndex(row, size); return bit(addedMask, row); }
        public boolean modified(int row) { Objects.checkIndex(row, size); return bit(modifiedMask, row); }
        public boolean removed(int row) { Objects.checkIndex(row, previousSize); return bit(removedMask, row); }
        public long changedCount() {
            long count = 0;
            for (long word : addedMask) count += Long.bitCount(word);
            for (long word : modifiedMask) count += Long.bitCount(word);
            for (long word : removedMask) count += Long.bitCount(word);
            return count;
        }
        /** Whether an impact planner must evaluate this inventory; does not select rebuild scope. */
        public boolean needsPlanning() { return initial || !comparable || changedCount() != 0; }
    }

    private static boolean zeroHash(byte[] values, int row) {
        int base = row * HASH_BYTES;
        for (int i = 0; i < HASH_BYTES; i++) if (values[base + i] != 0) return false;
        return true;
    }

    private static boolean sameHash(byte[] first, int a, byte[] second, int b) {
        return Arrays.equals(first, a * HASH_BYTES, (a + 1) * HASH_BYTES,
                second, b * HASH_BYTES, (b + 1) * HASH_BYTES);
    }

    private boolean sameDependencies(int row, M3BuildState prior, int earlier) {
        int count = dependencyCount(row);
        if (count != prior.dependencyCount(earlier)) return false;
        for (int i = 0; i < count; i++) {
            int current = dependencies[offsets[row] + i];
            int old = prior.dependencies[prior.offsets[earlier] + i];
            if (compare(paths[current], roles[current], prior.paths[old], prior.roles[old]) != 0) return false;
        }
        return true;
    }

    /**
     * Conservative reverse-dependency closure. Equal, *compiler-proven* ABI may
     * suppress downstream invalidation, but never skips the edited node itself.
     * A removed input or changed build context invalidates the entire inventory:
     * a removal can alter resolution even if a current graph edge has disappeared.
     * Results are candidates only; Maven/Gradle still own task execution decisions.
     */
    public Plan changedFrom(M3BuildState previous) {
        int n = size(), words = (int) ((n + 63L) >>> 6);
        long[] changed = new long[words], affected = new long[words];
        if (completeInventory()) {
            InventoryDelta delta = deltaFrom(previous, false);
            if (previous != null && delta.comparable() && delta.changedCount() == 0) {
                return new Plan(changed, affected, false, n);
            }
            Arrays.fill(changed, -1L);
            if (words != 0 && (n & 63) != 0) changed[words - 1] = (1L << (n & 63)) - 1L;
            return new Plan(changed, changed.clone(), true, n);
        }
        if (previous == null || !Arrays.equals(contextHash, previous.contextHash) || removedFrom(previous)) {
            Arrays.fill(changed, -1L);
            if (words != 0 && (n & 63) != 0) changed[words - 1] = (1L << (n & 63)) - 1L;
            return new Plan(changed, changed.clone(), true, n);
        }
        int[] reverseCounts = new int[n];
        for (int d : dependencies) reverseCounts[d]++;
        int[] reverseOffsets = new int[n + 1];
        for (int i = 0; i < n; i++) reverseOffsets[i + 1] = Math.addExact(reverseOffsets[i], reverseCounts[i]);
        int[] reverse = new int[dependencies.length], cursor = reverseOffsets.clone();
        for (int owner = 0; owner < n; owner++) {
            for (int p = offsets[owner]; p < offsets[owner + 1]; p++) {
                reverse[cursor[dependencies[p]]++] = owner;
            }
        }
        int[] queue = new int[n]; long[] enqueued = new long[words];
        int head = 0, tail = 0;
        for (int row = 0; row < n; row++) {
            int old = previous.find(paths[row], roles[row]);
            boolean bytesChanged = old < 0 || lengths[row] != previous.lengths[old] ||
                    !sameHash(contentHashes, row, previous.contentHashes, old);
            boolean abiChanged = old < 0 || abiAt(row) != previous.abiAt(old) ||
                    (abiAt(row) && !sameHash(abiHashes, row, previous.abiHashes, old));
            boolean edgesChanged = old < 0 || !sameDependencies(row, previous, old);
            if (bytesChanged || abiChanged || edgesChanged) {
                set(changed, row); set(affected, row);
                // ABI evidence only applies to source/class changes with the same
                // proven ABI, and an unchanged dependency graph.
                boolean canStop = old >= 0 && !edgesChanged && !abiChanged &&
                        (roles[row] == SOURCE || roles[row] == CLASS) && abiAt(row);
                if (!canStop) { set(enqueued, row); queue[tail++] = row; }
            }
        }
        while (head < tail) {
            int current = queue[head++];
            for (int p = reverseOffsets[current]; p < reverseOffsets[current + 1]; p++) {
                int dependent = reverse[p];
                // Being directly dirty is not the same as having traversed its
                // reverse edges. Track the actual worklist separately.
                if (!bit(enqueued, dependent)) {
                    set(affected, dependent); set(enqueued, dependent);
                    queue[tail++] = dependent;
                }
            }
        }
        return new Plan(changed, affected, false, n);
    }

    private boolean removedFrom(M3BuildState earlier) {
        int current = 0;
        for (int i = 0; i < earlier.size(); i++) {
            while (current < size() && compare(paths[current], roles[current],
                    earlier.paths[i], earlier.roles[i]) < 0) current++;
            if (current == size() || compare(paths[current], roles[current],
                    earlier.paths[i], earlier.roles[i]) != 0) return true;
        }
        return false;
    }

    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void set(long[] words, int row) { words[row >>> 6] |= 1L << (row & 63); }
    private static boolean bit(long[] words, int row) { return (words[row >>> 6] & (1L << (row & 63))) != 0L; }

    public record Plan(long[] changedMask, long[] affectedMask, boolean fullRebuild, int size) {
        public Plan {
            changedMask = changedMask.clone(); affectedMask = affectedMask.clone();
            if (size < 0 || changedMask.length != (size + 63L) / 64L ||
                    affectedMask.length != changedMask.length) throw new IllegalArgumentException("plan mask geometry");
        }
        @Override public long[] changedMask() { return changedMask.clone(); }
        @Override public long[] affectedMask() { return affectedMask.clone(); }
        public boolean changed(int row) { Objects.checkIndex(row, size); return bit(changedMask, row); }
        public boolean affected(int row) { Objects.checkIndex(row, size); return bit(affectedMask, row); }
        public long affectedCount() { long count = 0L; for (long word : affectedMask) count += Long.bitCount(word); return count; }
    }

    /** Checksum-sealed, versioned serialization; the caller supplies an allocation budget. */
    public byte[] encode(int maxBytes) throws IOException {
        if (maxBytes < HASH_BYTES + 48) throw new IllegalArgumentException("maxBytes");
        // Verify exact encoded geometry before allocating a growing output buffer.
        long required = 84L + 81L * size() + 4L * dependencies.length + 8L * abiKnown.length;
        if (completeInventory()) required += HASH_BYTES + 8L * size() + 8L * modifiedKnown.length;
        if (required > maxBytes) throw new IOException("build state image budget exceeded");
        for (String path : paths) {
            required = Math.addExact(required, encodedKey(path).length);
            if (required > maxBytes) throw new IOException("build state image budget exceeded");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) required);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(completeInventory() ? VERSION : 1);
            out.writeInt(size()); out.writeInt(dependencies.length);
            out.write(contextHash);
            for (int i = 0; i < size(); i++) {
                byte[] name = encodedKey(paths[i]);
                out.writeInt(name.length); out.write(name);
                out.writeByte(roles[i]); out.writeLong(lengths[i]);
                out.write(contentHashes, i * HASH_BYTES, HASH_BYTES);
                out.write(abiHashes, i * HASH_BYTES, HASH_BYTES);
            }
            for (long word : abiKnown) out.writeLong(word);
            for (int offset : offsets) out.writeInt(offset);
            for (int dependency : dependencies) out.writeInt(dependency);
            if (completeInventory()) {
                out.write(inventoryScope);
                for (long time : modifiedNanos) out.writeLong(time);
                for (long word : modifiedKnown) out.writeLong(word);
            }
        }
        byte[] payload = bytes.toByteArray();
        if (payload.length > maxBytes - HASH_BYTES) throw new IOException("build state image budget exceeded");
        byte[] image = Arrays.copyOf(payload, payload.length + HASH_BYTES);
        byte[] digest = digest(payload);
        System.arraycopy(digest, 0, image, payload.length, HASH_BYTES);
        return image;
    }

    public static M3BuildState decode(byte[] encoded, int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(encoded, "encoded");
        if (maxBytes < 0 || maxRows < 0 || maxEdges < 0 || encoded.length > maxBytes || encoded.length < 52 + HASH_BYTES)
            throw new IOException("invalid or oversized build state image");
        int payloadEnd = encoded.length - HASH_BYTES;
        if (!MessageDigest.isEqual(digest(Arrays.copyOf(encoded, payloadEnd)),
                Arrays.copyOfRange(encoded, payloadEnd, encoded.length))) {
            throw new IOException("build state checksum mismatch");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded, 0, payloadEnd))) {
            if (in.readInt() != MAGIC) throw new IOException("unknown build state schema");
            int version = in.readInt();
            if (version != 1 && version != VERSION) throw new IOException("unknown build state schema");
            int n = in.readInt(), m = in.readInt();
            if (n < 0 || n > maxRows || m < 0 || m > maxEdges) throw new IOException("invalid image counts");
            // Reject impossible counts before any data-column allocation; a tiny image must
            // never cause large allocations even when the caller sets generous count caps.
            long metadataBytes = version == 1 ? 0 : HASH_BYTES + 8L * n + 8L * ((n + 63L) >>> 6);
            if (52L + 82L * n + 4L * m + 8L * ((n + 63L) >>> 6) + metadataBytes > payloadEnd) {
                throw new IOException("build state counts exceed sealed byte budget");
            }
            byte[] context = new byte[HASH_BYTES]; in.readFully(context);
            String[] paths = new String[n]; byte[] roles = new byte[n]; long[] lengths = new long[n];
            byte[] content = new byte[Math.multiplyExact(n, HASH_BYTES)];
            byte[] abi = new byte[content.length];
            for (int i = 0; i < n; i++) {
                int len = in.readInt();
                if (len < 1 || len > MAX_KEY_BYTES) throw new IOException("invalid path size");
                byte[] pathBytes = new byte[len]; in.readFully(pathBytes);
                try {
                    paths[i] = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(pathBytes)).toString();
                } catch (CharacterCodingException invalid) { throw new IOException("malformed UTF-8 path", invalid); }
                roles[i] = in.readByte(); lengths[i] = in.readLong();
                in.readFully(content, i * HASH_BYTES, HASH_BYTES);
                in.readFully(abi, i * HASH_BYTES, HASH_BYTES);
            }
            long[] abiKnown = new long[(int) ((n + 63L) >>> 6)];
            for (int i = 0; i < abiKnown.length; i++) abiKnown[i] = in.readLong();
            int[] offsets = new int[n + 1], dependencies = new int[m];
            for (int i = 0; i < offsets.length; i++) offsets[i] = in.readInt();
            for (int i = 0; i < m; i++) dependencies[i] = in.readInt();
            long[] modifiedNanos = new long[n], modifiedKnown = new long[abiKnown.length];
            byte[] scope = null;
            if (version == VERSION) {
                scope = new byte[HASH_BYTES]; in.readFully(scope);
                for (int i = 0; i < n; i++) modifiedNanos[i] = in.readLong();
                for (int i = 0; i < modifiedKnown.length; i++) modifiedKnown[i] = in.readLong();
            }
            if (in.read() != -1) throw new IOException("trailing build state payload");
            return new M3BuildState(paths, roles, lengths, content, abi, abiKnown, offsets, dependencies, context,
                    modifiedNanos, modifiedKnown, scope);
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            throw new IOException("invalid build state lanes", invalid);
        }
    }
}
