/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package java.lang;

import java.util.Objects;
import jdk.internal.misc.Unsafe;

/**
 * JDK-internal MIndexString storage engine for ordinary {@link String} values.
 *
 * <p>The absolute representation invariant is:</p>
 *
 * <pre>
 * String wrapper
 *      -> canonical MIndexString storage
 *          -> OS-shared lexicon atom
 *          -> VM-local interned atom
 *          -> immutable tuple/range composition of those atoms
 * </pre>
 *
 * <p>Java arrays never resize. Logical concatenation/slicing/repeat changes only immutable
 * atom/range geometry. A contiguous Compact-String {@code byte[]} is a compatibility projection,
 * allocated only when a legacy Java/VM/JNI boundary requires it. Local scalar atoms can expose
 * their already-canonical Compact-String byte array directly; mapped lexicon atoms and joined
 * values materialize lazily.</p>
 */
final class MIndexString {
    static final byte EMPTY = 0;
    static final byte LOCAL = 1;
    static final byte LEXICON = 2;
    static final byte JOINED = 3;

    private static final int LARGE_LOCAL_ATOM = 4096;
    private static final int MAX_RETENTION_RATIO = 8;
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static volatile boolean ready;

    private static final MIndexString EMPTY_STORAGE =
            new MIndexString(
                    EMPTY,
                    null,
                    null,
                    0L,
                    null,
                    new int[0],
                    new int[0],
                    0,
                    String.LATIN1,
                    0L,
                    0,
                    MIndexStringPool.mix64(0L));

    /*
     * These field names/types are part of the M3JDK VM/JDK private contract.
     * HotSpot resolves them in java_lang_MIndexString.
     */
    private final byte storageKind;
    private final byte[] localValue;
    private final Object mappedOwner;
    private final long mappedAddress;
    private final MIndexString[] segments;
    private final int[] offsets;
    private final int[] ends;
    private final int length;
    private final byte coder;
    private final long canonicalId;
    private final int javaHash;
    private final long structuralHash64;

    /** Compatibility projection for non-local storage only. */
    private volatile byte[] materialized;

    private MIndexString(
            byte storageKind,
            byte[] localValue,
            Object mappedOwner,
            long mappedAddress,
            MIndexString[] segments,
            int[] offsets,
            int[] ends,
            int length,
            byte coder,
            long canonicalId,
            int javaHash,
            long structuralHash64) {
        this.storageKind = storageKind;
        this.localValue = localValue;
        this.mappedOwner = mappedOwner;
        this.mappedAddress = mappedAddress;
        this.segments = segments;
        this.offsets = offsets;
        this.ends = ends;
        this.length = length;
        this.coder = coder;
        this.canonicalId = canonicalId;
        this.javaHash = javaHash;
        this.structuralHash64 = structuralHash64;
    }

    static void activate(String lexiconFile) {
        if (ready) {
            return;
        }
        synchronized (MIndexString.class) {
            if (ready) {
                return;
            }
            MIndexStringPool.initializeLexicon(lexiconFile);
            ready = true;
        }
    }

    static boolean ready() {
        return ready;
    }

    static MIndexString admit(byte[] value, byte coder) {
        Objects.requireNonNull(value, "value");
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        if ((value.length >> coder << coder) != value.length) {
            throw new IllegalArgumentException("misaligned String payload");
        }
        return MIndexStringPool.internScalar(value, coder);
    }

    static MIndexString emptyStorage() {
        return EMPTY_STORAGE;
    }

    static MIndexString localScalar(
            byte[] canonicalValue, byte coder, long canonicalId, long structuralHash64) {
        Objects.requireNonNull(canonicalValue, "canonicalValue");
        int length = canonicalValue.length >> coder;
        int hash =
                coder == String.LATIN1
                        ? StringLatin1.hashCode(canonicalValue)
                        : StringUTF16.hashCode(canonicalValue);
        return new MIndexString(
                LOCAL,
                canonicalValue,
                null,
                0L,
                null,
                null,
                null,
                length,
                coder,
                canonicalId,
                hash,
                structuralHash64);
    }

    static MIndexString lexiconScalar(
            Object mappedOwner,
            long mappedAddress,
            int length,
            int javaHash,
            long canonicalId,
            long structuralHash64) {
        if (mappedAddress == 0L || length < 0) {
            throw new IllegalArgumentException("invalid mapped MIndex atom");
        }
        return new MIndexString(
                LEXICON,
                null,
                Objects.requireNonNull(mappedOwner, "mappedOwner"),
                mappedAddress,
                null,
                null,
                null,
                length,
                String.UTF16,
                canonicalId,
                javaHash,
                structuralHash64);
    }

    static MIndexString joinedCanonical(
            MIndexString[] segments,
            int[] offsets,
            int[] lengths,
            byte coder,
            int logicalLength,
            long canonicalId,
            long structuralHash64) {
        Objects.requireNonNull(segments, "segments");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        if (segments.length != offsets.length || segments.length != lengths.length) {
            throw new IllegalArgumentException("MIndex tuple lane lengths differ");
        }
        int[] ends = new int[segments.length];
        int total = 0;
        for (int index = 0; index < segments.length; index++) {
            MIndexString atom = Objects.requireNonNull(segments[index], "segment");
            if (!atom.isScalar()) {
                throw new IllegalArgumentException("joined MIndex pieces must be scalar atoms");
            }
            Objects.checkFromIndexSize(offsets[index], lengths[index], atom.length);
            if (lengths[index] <= 0) {
                throw new IllegalArgumentException("empty MIndex joined segment");
            }
            total = Math.addExact(total, lengths[index]);
            ends[index] = total;
        }
        if (total != logicalLength) {
            throw new IllegalArgumentException("MIndex joined length mismatch");
        }
        MIndexString provisional =
                new MIndexString(
                        JOINED,
                        null,
                        null,
                        0L,
                        segments,
                        offsets,
                        ends,
                        logicalLength,
                        coder,
                        canonicalId,
                        0,
                        structuralHash64);
        int hash = provisional.computeJavaHash();
        return new MIndexString(
                JOINED,
                null,
                null,
                0L,
                segments,
                offsets,
                ends,
                logicalLength,
                coder,
                canonicalId,
                hash,
                structuralHash64);
    }

    static MIndexString join(String first, String second) {
        return join(new String[] {first, second});
    }

    static MIndexString join(String[] parts) {
        Objects.requireNonNull(parts, "parts");
        int capacity = 0;
        int totalLength = 0;
        byte resultCoder = String.LATIN1;

        for (String part : parts) {
            Objects.requireNonNull(part, "part");
            MIndexString storage = part.mindex();
            if (storage == null || storage.length == 0) {
                continue;
            }
            totalLength = Math.addExact(totalLength, storage.length);
            resultCoder |= storage.coder;
            capacity = Math.addExact(capacity, storage.storageKind == JOINED
                    ? storage.segments.length : 1);
        }

        if (totalLength == 0) {
            return EMPTY_STORAGE;
        }

        MIndexString[] atoms = new MIndexString[capacity];
        int[] atomOffsets = new int[capacity];
        int[] atomLengths = new int[capacity];
        int count = 0;

        for (String part : parts) {
            MIndexString storage = part.mindex();
            if (storage == null || storage.length == 0) {
                continue;
            }
            if (storage.storageKind == JOINED) {
                for (int index = 0; index < storage.segments.length; index++) {
                    int previous = index == 0 ? 0 : storage.ends[index - 1];
                    int take = storage.ends[index] - previous;
                    count = addNormalized(
                            atoms,
                            atomOffsets,
                            atomLengths,
                            count,
                            storage.segments[index],
                            storage.offsets[index],
                            take);
                }
            } else {
                count = addNormalized(
                        atoms,
                        atomOffsets,
                        atomLengths,
                        count,
                        storage,
                        0,
                        storage.length);
            }
        }

        if (count == 1
                && atomOffsets[0] == 0
                && atomLengths[0] == atoms[0].length) {
            return atoms[0];
        }

        if (count != capacity) {
            atoms = java.util.Arrays.copyOf(atoms, count);
            atomOffsets = java.util.Arrays.copyOf(atomOffsets, count);
            atomLengths = java.util.Arrays.copyOf(atomLengths, count);
        }
        return MIndexStringPool.internJoin(
                atoms, atomOffsets, atomLengths, resultCoder, totalLength);
    }

    static MIndexString sliceOf(String source, int beginIndex, int endIndex) {
        Objects.requireNonNull(source, "source");
        MIndexString storage = source.mindex();
        if (storage == null) {
            return null;
        }
        return storage.slice(beginIndex, endIndex);
    }

    MIndexString slice(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length);
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        int newLength = endIndex - beginIndex;
        if (newLength == 0) {
            return EMPTY_STORAGE;
        }

        if (isScalar()) {
            if (storageKind == LOCAL && !mayRetain(length, newLength)) {
                byte[] compact = copyRange(beginIndex, endIndex);
                return MIndexStringPool.internScalar(compact, coder);
            }
            return MIndexStringPool.internJoin(
                    new MIndexString[] {this},
                    new int[] {beginIndex},
                    new int[] {newLength},
                    coder,
                    newLength);
        }

        int first = segmentAt(beginIndex);
        int last = segmentAt(endIndex - 1);
        int capacity = last - first + 1;
        MIndexString[] atoms = new MIndexString[capacity];
        int[] atomOffsets = new int[capacity];
        int[] atomLengths = new int[capacity];
        int count = 0;
        byte resultCoder = String.LATIN1;

        for (int segment = first; segment <= last; segment++) {
            int segmentStart = segment == 0 ? 0 : ends[segment - 1];
            int logicalStart = Math.max(beginIndex, segmentStart);
            int logicalEnd = Math.min(endIndex, ends[segment]);
            int take = logicalEnd - logicalStart;
            MIndexString atom = segments[segment];
            int atomOffset = offsets[segment] + logicalStart - segmentStart;
            if (atom.storageKind == LOCAL && !mayRetain(atom.length, take)) {
                byte[] compact = atom.copyRange(atomOffset, atomOffset + take);
                atom = MIndexStringPool.internScalar(compact, atom.coder);
                atomOffset = 0;
            }
            resultCoder |= atom.coder;
            count = addNormalized(
                    atoms, atomOffsets, atomLengths, count, atom, atomOffset, take);
        }

        if (count == 1
                && atomOffsets[0] == 0
                && atomLengths[0] == atoms[0].length) {
            return atoms[0];
        }
        if (count != capacity) {
            atoms = java.util.Arrays.copyOf(atoms, count);
            atomOffsets = java.util.Arrays.copyOf(atomOffsets, count);
            atomLengths = java.util.Arrays.copyOf(atomLengths, count);
        }
        return MIndexStringPool.internJoin(
                atoms, atomOffsets, atomLengths, resultCoder, newLength);
    }

    MIndexString repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 0 || length == 0) {
            return EMPTY_STORAGE;
        }
        if (count == 1) {
            return this;
        }
        int resultLength = Math.multiplyExact(length, count);
        int leafCount = storageKind == JOINED ? segments.length : 1;
        int capacity = Math.multiplyExact(leafCount, count);
        MIndexString[] atoms = new MIndexString[capacity];
        int[] atomOffsets = new int[capacity];
        int[] atomLengths = new int[capacity];
        int at = 0;
        for (int repetition = 0; repetition < count; repetition++) {
            if (storageKind == JOINED) {
                for (int segment = 0; segment < segments.length; segment++) {
                    int previous = segment == 0 ? 0 : ends[segment - 1];
                    at = addNormalized(
                            atoms,
                            atomOffsets,
                            atomLengths,
                            at,
                            segments[segment],
                            offsets[segment],
                            ends[segment] - previous);
                }
            } else {
                at = addNormalized(atoms, atomOffsets, atomLengths, at, this, 0, length);
            }
        }
        if (at != capacity) {
            atoms = java.util.Arrays.copyOf(atoms, at);
            atomOffsets = java.util.Arrays.copyOf(atomOffsets, at);
            atomLengths = java.util.Arrays.copyOf(atomLengths, at);
        }
        return MIndexStringPool.internJoin(
                atoms, atomOffsets, atomLengths, coder, resultLength);
    }

    int length() {
        return length;
    }

    byte coder() {
        return coder;
    }

    byte storageKind() {
        return storageKind;
    }

    long canonicalId() {
        return canonicalId;
    }

    long structuralHash64() {
        return structuralHash64;
    }

    boolean isScalar() {
        return storageKind != JOINED;
    }

    boolean isContiguousLocal() {
        return storageKind == LOCAL || storageKind == EMPTY;
    }

    byte[] compatibilityValue() {
        return storageKind == LOCAL ? localValue : EMPTY_BYTES;
    }

    char charAt(int index) {
        Objects.checkIndex(index, length);
        return switch (storageKind) {
            case EMPTY -> throw new StringIndexOutOfBoundsException(index);
            case LOCAL -> coder == String.LATIN1
                    ? StringLatin1.charAt(localValue, index)
                    : StringUTF16.charAt(localValue, index);
            case LEXICON -> mappedChar(index);
            case JOINED -> {
                int segment = segmentAt(index);
                int previous = segment == 0 ? 0 : ends[segment - 1];
                yield segments[segment].charAt(offsets[segment] + index - previous);
            }
            default -> throw new InternalError("invalid MIndex storage kind");
        };
    }

    int hashCodeValue() {
        return javaHash;
    }

    boolean contentEquals(String other) {
        Objects.requireNonNull(other, "other");
        MIndexString that = other.mindex();
        if (that == this) {
            return true;
        }
        if (other.length() != length) {
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (charAt(index) != other.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        String.checkBoundsBeginEnd(srcBegin, srcEnd, length);
        Objects.checkFromIndexSize(dstBegin, srcEnd - srcBegin, dst.length);
        for (int source = srcBegin, target = dstBegin; source < srcEnd; source++, target++) {
            dst[target] = charAt(source);
        }
    }

    void getBytes(byte[] dst, int srcPos, int dstBegin, byte dstCoder, int count) {
        Objects.checkFromIndexSize(srcPos, count, length);
        Objects.checkFromIndexSize(dstBegin << dstCoder, count << dstCoder, dst.length);
        if (storageKind == LOCAL && coder == dstCoder) {
            System.arraycopy(
                    localValue,
                    srcPos << coder,
                    dst,
                    dstBegin << dstCoder,
                    count << coder);
            return;
        }
        for (int index = 0; index < count; index++) {
            char value = charAt(srcPos + index);
            if (dstCoder == String.LATIN1) {
                dst[dstBegin + index] = (byte) value;
            } else {
                StringUTF16.putChar(dst, dstBegin + index, value);
            }
        }
    }

    byte[] materialize() {
        if (storageKind == LOCAL) {
            return localValue;
        }
        if (storageKind == EMPTY) {
            return EMPTY_BYTES;
        }
        byte[] cached = materialized;
        if (cached != null) {
            return cached;
        }
        int byteLength = length << coder;
        if ((byteLength >> coder) != length) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        byte[] created = new byte[byteLength];
        getBytes(created, 0, 0, coder, length);
        materialized = created;
        return created;
    }

    boolean localContentEquals(byte[] value, byte valueCoder) {
        if (storageKind != LOCAL
                || coder != valueCoder
                || localValue.length != value.length) {
            return false;
        }
        return java.util.Arrays.equals(localValue, value);
    }

    boolean joinGeometryEquals(
            MIndexString[] atoms, int[] atomOffsets, int[] atomLengths, byte expectedCoder) {
        if (storageKind != JOINED
                || coder != expectedCoder
                || segments.length != atoms.length) {
            return false;
        }
        for (int index = 0; index < segments.length; index++) {
            int previous = index == 0 ? 0 : ends[index - 1];
            if (segments[index] != atoms[index]
                    || offsets[index] != atomOffsets[index]
                    || ends[index] - previous != atomLengths[index]) {
                return false;
            }
        }
        return true;
    }

    private int computeJavaHash() {
        int hash = 0;
        for (int index = 0; index < length; index++) {
            hash = 31 * hash + charAt(index);
        }
        return hash;
    }

    private byte[] copyRange(int beginIndex, int endIndex) {
        int count = endIndex - beginIndex;
        byte[] result = new byte[count << coder];
        getBytes(result, beginIndex, 0, coder, count);
        return result;
    }

    private char mappedChar(int index) {
        long address = mappedAddress + ((long) index << 1);
        int low = UNSAFE.getByte(address) & 0xff;
        int high = UNSAFE.getByte(address + 1L) & 0xff;
        return (char) (low | (high << 8));
    }

    private int segmentAt(int logicalIndex) {
        int low = 0;
        int high = ends.length - 1;
        int key = logicalIndex + 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int end = ends[middle];
            if (end < key) {
                low = middle + 1;
            } else if (middle > 0 && ends[middle - 1] >= key) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        throw new InternalError("invalid MIndexString segment coordinate");
    }

    private static int addNormalized(
            MIndexString[] atoms,
            int[] offsets,
            int[] lengths,
            int count,
            MIndexString atom,
            int offset,
            int length) {
        if (length == 0) {
            return count;
        }
        if (!atom.isScalar()) {
            throw new IllegalArgumentException("MIndex join atom must be scalar");
        }
        Objects.checkFromIndexSize(offset, length, atom.length);
        if (count > 0
                && atoms[count - 1] == atom
                && offsets[count - 1] + lengths[count - 1] == offset) {
            lengths[count - 1] = Math.addExact(lengths[count - 1], length);
            return count;
        }
        atoms[count] = atom;
        offsets[count] = offset;
        lengths[count] = length;
        return count + 1;
    }

    private static boolean mayRetain(int retainedLength, int usedLength) {
        return retainedLength <= LARGE_LOCAL_ATOM
                || usedLength
                        >= (retainedLength + MAX_RETENTION_RATIO - 1) / MAX_RETENTION_RATIO;
    }
}
