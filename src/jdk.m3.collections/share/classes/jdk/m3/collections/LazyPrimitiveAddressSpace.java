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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-geometry, lazily allocated primitive address space.
 *
 * <p>All directory and payload arrays have fixed lengths. Missing leaves read
 * as zero. The original three-level geometry remains supported; {@link #paged}
 * uses bounded 256-way directories to address a positive signed-long range
 * without allocating a directory proportional to the logical capacity.
 * This owner is not thread-safe. Exposed primitive leaves remain owner-backed
 * while allocated; releasing or fully clearing a leaf detaches its old array.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LazyPrimitiveAddressSpace.java}
 * blob {@code a2da751d7411e2e001c142d2e3028c806c2fad82} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LazyPrimitiveAddressSpace {
    public static final int DEFAULT_ROOT_BITS = 8;
    public static final int DEFAULT_DIRECTORY_BITS = 8;
    public static final int DEFAULT_LEAF_BITS = 12;

    @FunctionalInterface
    public interface LeafConsumer {
        void accept(long baseIndex, Object primitiveLeaf);
    }

    private final PrimitiveKind kind;
    private final int rootBits;
    private final int directoryBits;
    private final int leafBits;
    private final int middleSize;
    private final int leafSize;
    private final int leafMask;
    private final int[] shifts;
    private final long logicalCapacity;
    private final Object[] root;
    private long allocatedMiddleDirectories;
    private long allocatedLeaves;

    public LazyPrimitiveAddressSpace(PrimitiveKind kind) {
        this(kind, DEFAULT_ROOT_BITS, DEFAULT_DIRECTORY_BITS, DEFAULT_LEAF_BITS);
    }

    public LazyPrimitiveAddressSpace(PrimitiveKind kind, int rootBits,
                                     int directoryBits, int leafBits) {
        this(Objects.requireNonNull(kind, "kind"), rootBits, directoryBits, leafBits, 2,
                checkedCapacity(rootBits, directoryBits, leafBits));
    }

    private LazyPrimitiveAddressSpace(PrimitiveKind kind, int rootBits,
                                      int directoryBits, int leafBits,
                                      int levels, long logicalCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.rootBits = rootBits;
        this.directoryBits = directoryBits;
        this.leafBits = leafBits;
        this.logicalCapacity = logicalCapacity;
        middleSize = 1 << directoryBits;
        leafSize = 1 << leafBits;
        leafMask = leafSize - 1;
        shifts = new int[levels];
        for (int depth = 0; depth < levels; depth++) {
            shifts[depth] = leafBits + (levels - depth - 1) * directoryBits;
        }
        root = new Object[1 << rootBits];
    }

    /**
     * Creates a sparse fixed-page store with the exact exclusive capacity.
     * Capacity is a logical address bound, not an eager memory reservation.
     * Even Long.MAX_VALUE is representable; actual residency remains bounded
     * by the available heap and checked byte-accounting arithmetic.
     */
    public static LazyPrimitiveAddressSpace paged(PrimitiveKind kind,
                                                  long capacity, int leafBits) {
        Objects.requireNonNull(kind, "kind");
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        if (leafBits < 6 || leafBits > 26) {
            throw new IllegalArgumentException("leafBits must be 6..26");
        }
        int addressBits = capacity <= 1 ? 1 : 64 - Long.numberOfLeadingZeros(capacity - 1);
        int directoryAddressBits = Math.max(1, addressBits - leafBits);
        int levels = (directoryAddressBits + 7) / 8;
        int rootBits = directoryAddressBits - (levels - 1) * 8;
        return new LazyPrimitiveAddressSpace(kind, rootBits, 8, leafBits, levels, capacity);
    }

    public PrimitiveKind kind() { return kind; }
    public int rootBits() { return rootBits; }
    public int directoryBits() { return directoryBits; }
    public int leafBits() { return leafBits; }
    public int leafSize() { return leafSize; }
    public long logicalCapacity() { return logicalCapacity; }
    public int allocatedMiddleDirectoryCount() { return Math.toIntExact(allocatedMiddleDirectories); }
    public int allocatedLeafCount() { return Math.toIntExact(allocatedLeaves); }
    public long allocatedMiddleDirectoryCount64() { return allocatedMiddleDirectories; }
    public long allocatedLeafCount64() { return allocatedLeaves; }

    public long allocatedPayloadBytes() {
        return Math.multiplyExact(Math.multiplyExact(allocatedLeaves, leafSize), kind.widthBytes());
    }

    public long directoryReferenceSlots() {
        return Math.addExact(root.length, Math.multiplyExact(allocatedMiddleDirectories, middleSize));
    }

    public boolean isLeafAllocated(long index) { return leaf(index, false) != null; }

    public long getBits(long index) {
        Object page = leaf(index, false);
        return page == null ? 0L : kind.readBits(page, (int) index & leafMask);
    }

    public void setBits(long index, long bits) {
        checkIndex(index);
        Object page = leaf(index, kind.storageBits(bits) != 0L);
        if (page != null) kind.writeBits(page, (int) index & leafMask, bits);
    }

    public void clearBits(long index) { setBits(index, 0L); }

    /** Drops a complete leaf only when the caller no longer needs its values. */
    public boolean releaseLeaf(long index) {
        checkIndex(index);
        return release(root, 0, index);
    }

    /** Visits only resident leaves in increasing global-address order. */
    public void forEachAllocatedLeaf(LeafConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        visit(root, 0, 0L, consumer);
    }

    /**
     * Visits only resident leaves intersecting the requested half-open range.
     * Uses the existing lexicographically ordered directory; does not change
     * ownership, allocate leaves, or materialize missing pages.
     */
    void forEachAllocatedLeaf(long fromInclusive, long toExclusive, LeafConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        Objects.checkFromToIndex(fromInclusive, toExclusive, logicalCapacity);
        if (fromInclusive != toExclusive) {
            visitRange(root, 0, 0L, fromInclusive, toExclusive, consumer);
        }
    }

    /** Clears a long range in work proportional to resident directories/leaves. */
    public void clearRange(long fromInclusive, long toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, logicalCapacity);
        if (fromInclusive != toExclusive) clearRange(root, 0, 0L, fromInclusive, toExclusive, true);
    }

    // Copy preflight has already prepared every destination page. Keep those
    // arrays resident so publishing payload never allocates after clearing it.
    void clearRangePreservingLeaves(long fromInclusive, long toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, logicalCapacity);
        if (fromInclusive != toExclusive) clearRange(root, 0, 0L, fromInclusive, toExclusive, false);
    }

    public void clear() {
        Arrays.fill(root, null);
        allocatedMiddleDirectories = 0;
        allocatedLeaves = 0;
    }

    // The existing big-array owner uses these package-local atoms. No second page store.
    Object existingLeaf(long index) { return leaf(index, false); }
    Object ensureLeaf(long index) { return leaf(index, true); }

    private Object leaf(long index, boolean allocate) {
        checkIndex(index);
        Object[] directory = root;
        for (int depth = 0; depth < shifts.length; depth++) {
            int slot = slot(index, depth, directory.length);
            Object child = directory[slot];
            if (child == null) {
                if (!allocate) return null;
                // Complete the missing branch off to the side. Failed allocation
                // publishes neither directories, counters, nor a partially owned leaf.
                Object page = kind.newArray(leafSize);
                Object branch = page;
                for (int d = shifts.length - 1; d > depth; d--) {
                    Object[] next = new Object[middleSize];
                    next[slot(index, d, middleSize)] = branch;
                    branch = next;
                }
                long nextDirectories = Math.addExact(allocatedMiddleDirectories,
                        shifts.length - depth - 1L);
                long nextLeaves = Math.addExact(allocatedLeaves, 1L);
                directory[slot] = branch;
                allocatedMiddleDirectories = nextDirectories;
                allocatedLeaves = nextLeaves;
                return page;
            }
            if (depth == shifts.length - 1) return child;
            directory = (Object[]) child;
        }
        throw new AssertionError("empty directory geometry");
    }

    private boolean release(Object[] directory, int depth, long index) {
        int slot = slot(index, depth, directory.length);
        Object child = directory[slot];
        if (child == null) return false;
        if (depth == shifts.length - 1) {
            directory[slot] = null;
            allocatedLeaves--;
            return true;
        }
        Object[] next = (Object[]) child;
        boolean removed = release(next, depth + 1, index);
        if (removed && isEmpty(next)) {
            directory[slot] = null;
            allocatedMiddleDirectories--;
        }
        return removed;
    }

    private void visit(Object[] directory, int depth, long base, LeafConsumer consumer) {
        for (int slot = 0; slot < directory.length; slot++) {
            Object child = directory[slot];
            if (child == null) continue;
            long childBase = base | ((long) slot << shifts[depth]);
            if (depth == shifts.length - 1) consumer.accept(childBase, child);
            else visit((Object[]) child, depth + 1, childBase, consumer);
        }
    }

    /** Branch-bounded, allocation-free visitor for sparse range queries. */
    private void visitRange(Object[] directory, int depth, long base, long from, long to,
                            LeafConsumer consumer) {
        if (to <= base) return;
        int shift = shifts[depth];
        int first = from <= base ? 0 : (int) Math.min(directory.length, (from - base) >>> shift);
        int last = (int) Math.min(directory.length - 1L, (to - 1 - base) >>> shift);
        for (int slot = first; slot <= last; slot++) {
            Object child = directory[slot];
            if (child == null) continue;
            long childBase = base | ((long) slot << shift);
            if (depth == shifts.length - 1) {
                consumer.accept(childBase, child);
            } else {
                visitRange((Object[]) child, depth + 1, childBase, from, to, consumer);
            }
        }
    }

    private void clearRange(Object[] directory, int depth, long base, long from, long to,
                            boolean release) {
        if (to <= base) return;
        int shift = shifts[depth];
        long span = 1L << shift;
        int firstSlot = from <= base ? 0
                : (int) Math.min(directory.length, (from - base) >>> shift);
        int lastSlot = (int) Math.min(directory.length - 1L, (to - 1L - base) >>> shift);
        for (int slot = firstSlot; slot <= lastSlot; slot++) {
            Object child = directory[slot];
            if (child == null) continue;
            long childBase = base | ((long) slot << shifts[depth]);
            long childEnd = span > logicalCapacity - childBase
                    ? logicalCapacity : childBase + span;
            if (childBase >= to || childEnd <= from) continue;
            if (depth == shifts.length - 1) {
                if (release && from <= childBase && to >= childEnd) {
                    directory[slot] = null;
                    allocatedLeaves--;
                } else {
                    int first = (int) Math.max(0L, from - childBase);
                    int last = (int) Math.min((long) leafSize, to - childBase);
                    kind.clear(child, first, last);
                }
            } else {
                Object[] next = (Object[]) child;
                clearRange(next, depth + 1, childBase, from, to, release);
                if (release && isEmpty(next)) {
                    directory[slot] = null;
                    allocatedMiddleDirectories--;
                }
            }
        }
    }

    private int slot(long index, int depth, int size) {
        return (int) (index >>> shifts[depth]) & (size - 1);
    }

    private void checkIndex(long index) {
        if (index < 0L || index >= logicalCapacity) {
            throw new IndexOutOfBoundsException(Long.toString(index));
        }
    }

    private static boolean isEmpty(Object[] directory) {
        for (Object value : directory) if (value != null) return false;
        return true;
    }

    private static long checkedCapacity(int rootBits, int directoryBits, int leafBits) {
        validateBits(rootBits, directoryBits, leafBits);
        return 1L << (rootBits + directoryBits + leafBits);
    }

    private static void validateBits(int rootBits, int directoryBits, int leafBits) {
        if (rootBits < 1 || rootBits > 20) throw new IllegalArgumentException("rootBits must be 1..20");
        if (directoryBits < 1 || directoryBits > 20) {
            throw new IllegalArgumentException("directoryBits must be 1..20");
        }
        if (leafBits < 6 || leafBits > 26) throw new IllegalArgumentException("leafBits must be 6..26");
        if (rootBits + directoryBits + leafBits > 62) {
            throw new IllegalArgumentException("logical address width must be <= 62 bits");
        }
    }
}
