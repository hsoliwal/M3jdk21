/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.bits;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;
import jdk.m3.collections.LazyPrimitiveAddressSpace;

/**
 * Sparse hierarchical bit address space using lazily allocated long[] leaf bitmaps.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LazyBitAddressSpace.java}
 * blob {@code fc2cda6b80168c7387679a693a23c58e2fa10be5} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LazyBitAddressSpace {
    private final int rootBits;
    private final int directoryBits;
    private final int leafBits;
    private final int middleSize;
    private final int leafSize;
    private final int leafWords;
    private final int middleMask;
    private final int leafMask;
    private final int middleShift;
    private final long logicalCapacity;
    private final Object[] root;

    private int allocatedMiddleDirectories;
    private int allocatedLeaves;
    private long cardinality;

    public LazyBitAddressSpace() {
        this(
                LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS);
    }

    public LazyBitAddressSpace(int rootBits, int directoryBits, int leafBits) {
        if (rootBits < 1 || rootBits > 20
                || directoryBits < 1 || directoryBits > 20
                || leafBits < 6 || leafBits > 26
                || rootBits + directoryBits + leafBits > 62) {
            throw new IllegalArgumentException("invalid address geometry");
        }
        this.rootBits = rootBits;
        this.directoryBits = directoryBits;
        this.leafBits = leafBits;
        middleSize = 1 << directoryBits;
        leafSize = 1 << leafBits;
        leafWords = leafSize >>> 6;
        middleMask = middleSize - 1;
        leafMask = leafSize - 1;
        middleShift = directoryBits + leafBits;
        logicalCapacity = 1L << (rootBits + directoryBits + leafBits);
        root = new Object[1 << rootBits];
    }

    public long logicalCapacity() { return logicalCapacity; }
    public long cardinality() { return cardinality; }
    public int allocatedMiddleDirectoryCount() { return allocatedMiddleDirectories; }
    public int allocatedLeafCount() { return allocatedLeaves; }
    public long allocatedPayloadBytes() {
        return (long) allocatedLeaves * leafWords * Long.BYTES;
    }
    public long directoryReferenceSlots() {
        return root.length + (long) allocatedMiddleDirectories * middleSize;
    }

    public boolean get(long index) {
        checkIndex(index);
        Object[] middle = (Object[]) root[rootIndex(index)];
        if (middle == null) return false;
        long[] leaf = (long[]) middle[middleIndex(index)];
        if (leaf == null) return false;
        int offset = leafOffset(index);
        return (leaf[offset >>> 6] & (1L << (offset & 63))) != 0L;
    }

    /** Returns true when the bit changed from clear to set. */
    public boolean set(long index) {
        checkIndex(index);
        int rootIndex = rootIndex(index);
        Object[] middle = (Object[]) root[rootIndex];
        if (middle == null) {
            middle = new Object[middleSize];
            root[rootIndex] = middle;
            allocatedMiddleDirectories++;
        }
        int middleIndex = middleIndex(index);
        long[] leaf = (long[]) middle[middleIndex];
        if (leaf == null) {
            leaf = new long[leafWords];
            middle[middleIndex] = leaf;
            allocatedLeaves++;
        }
        int offset = leafOffset(index);
        int word = offset >>> 6;
        long bit = 1L << (offset & 63);
        if ((leaf[word] & bit) != 0L) return false;
        leaf[word] |= bit;
        cardinality++;
        return true;
    }

    /** Returns true when the bit changed from set to clear. */
    public boolean clear(long index) {
        checkIndex(index);
        Object[] middle = (Object[]) root[rootIndex(index)];
        if (middle == null) return false;
        long[] leaf = (long[]) middle[middleIndex(index)];
        if (leaf == null) return false;
        int offset = leafOffset(index);
        int word = offset >>> 6;
        long bit = 1L << (offset & 63);
        if ((leaf[word] & bit) == 0L) return false;
        leaf[word] &= ~bit;
        cardinality--;
        return true;
    }

    public long nextSetBit(long fromInclusive) {
        if (fromInclusive < 0L || fromInclusive > logicalCapacity) {
            throw new IndexOutOfBoundsException(Long.toString(fromInclusive));
        }
        if (fromInclusive == logicalCapacity) return -1L;

        int startRoot = rootIndex(fromInclusive);
        for (int rootIndex = startRoot; rootIndex < root.length; rootIndex++) {
            Object[] middle = (Object[]) root[rootIndex];
            if (middle == null) continue;
            int startMiddle = rootIndex == startRoot ? middleIndex(fromInclusive) : 0;
            for (int middleIndex = startMiddle; middleIndex < middle.length; middleIndex++) {
                long[] leaf = (long[]) middle[middleIndex];
                if (leaf == null) continue;
                int startOffset =
                        rootIndex == startRoot && middleIndex == startMiddle
                                ? leafOffset(fromInclusive)
                                : 0;
                int wordIndex = startOffset >>> 6;
                long word = leaf[wordIndex] & (-1L << (startOffset & 63));
                while (true) {
                    if (word != 0L) {
                        int offset = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                        return ((long) rootIndex << middleShift)
                                | ((long) middleIndex << leafBits)
                                | offset;
                    }
                    if (++wordIndex >= leaf.length) break;
                    word = leaf[wordIndex];
                }
            }
        }
        return -1L;
    }

    public void forEachSetBit(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (long index = nextSetBit(0L); index >= 0L; index = nextSetBit(index + 1L)) {
            consumer.accept(index);
        }
    }

    public void clear() {
        Arrays.fill(root, null);
        allocatedMiddleDirectories = 0;
        allocatedLeaves = 0;
        cardinality = 0L;
    }

    private int rootIndex(long index) {
        return (int) (index >>> middleShift);
    }

    private int middleIndex(long index) {
        return (int) (index >>> leafBits) & middleMask;
    }

    private int leafOffset(long index) {
        return (int) index & leafMask;
    }

    private void checkIndex(long index) {
        if (index < 0L || index >= logicalCapacity) {
            throw new IndexOutOfBoundsException(Long.toString(index));
        }
    }
}
