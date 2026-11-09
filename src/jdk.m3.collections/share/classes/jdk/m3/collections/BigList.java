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

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Long-sized resizable primitive list backed by {@link BigArray}.
 * Mutable and not thread-safe. Values use {@link PrimitiveKind}'s bit-carrier contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBigList.java}
 * blob {@code 78032530f777bc2830cc9bab0299c28a944975a7} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class BigList {
    private final PrimitiveKind kind;
    private final int segmentShift;
    private BigArray elements;
    private long size;
    private long mutationVersion;

    public BigList(PrimitiveKind kind) {
        this(kind, 16L, BigArray.DEFAULT_SEGMENT_SHIFT);
    }

    public BigList(PrimitiveKind kind, long initialCapacity, int segmentShift) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.segmentShift = segmentShift;
        elements = new BigArray(kind, Math.max(1L, initialCapacity), segmentShift);
    }

    public PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public boolean isEmpty() { return size == 0L; }
    public long capacity() { return elements.length(); }
    public long payloadBytes() { return elements.payloadBytes(); }
    public int directoryReferenceSlots() { return elements.directoryReferenceSlots(); }

    /**
     * Returns the addressability bound for the backing segment directory.
     * Available memory and JVM array limits may impose a smaller practical capacity.
     * @return the maximum representable capacity
     */
    public long maxCapacity() { return elements.maxLength(); }

    public void addBits(long bits) {
        long nextSize = expandedSize(1L);
        ensure(nextSize);
        elements.setBits(size, bits);
        size = nextSize;
        mutationVersion++;
    }

    public void addBits(long index, long bits) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(Long.toString(index));
        long nextSize = expandedSize(1L);
        ensure(nextSize);
        if (index < size) elements.copyTo(index, elements, index + 1L, size - index);
        elements.setBits(index, bits);
        size = nextSize;
        mutationVersion++;
    }

    public long getBits(long index) {
        return elements.getBits(check(index));
    }

    public long setBits(long index, long bits) {
        long checked = check(index);
        long old = elements.getBits(checked);
        elements.setBits(checked, bits);
        mutationVersion++;
        return old;
    }

    public long removeAtBits(long index) {
        long checked = check(index);
        long old = elements.getBits(checked);
        long moved = size - checked - 1L;
        if (moved > 0) elements.copyTo(checked + 1L, elements, checked, moved);
        size--;
        elements.setBits(size, 0L);
        mutationVersion++;
        return old;
    }

    public void clear() {
        boolean changed = size != 0L;
        elements.fillBits(0L, size, 0L);
        size = 0L;
        if (changed) mutationVersion++;
    }

    public void ensureCapacity(long required) {
        if (required < 0) throw new IllegalArgumentException("required < 0");
        ensure(required);
    }

    public void trimToSize() {
        elements.resize(Math.max(1L, size));
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (long i = 0; i < size; i++) consumer.accept(elements.getBits(i));
    }

    /**
     * Copies a source range over an existing target range without changing either size.
     * Self-overlap has memmove semantics. Both ranges and the kind are checked before writes.
     * @param sourceIndex first source position
     * @param target destination list of the same primitive kind
     * @param targetIndex first existing destination position
     * @param count number of values to replace
     * @throws NullPointerException if target is null
     * @throws IllegalArgumentException if primitive kinds differ
     * @throws IndexOutOfBoundsException if either offset/count range is invalid
     */
    public void copyTo(long sourceIndex, BigList target, long targetIndex, long count) {
        Objects.requireNonNull(target, "target");
        requireKind(target);
        Objects.checkFromIndexSize(sourceIndex, count, size);
        Objects.checkFromIndexSize(targetIndex, count, target.size);
        if (count == 0L) return;
        elements.copyTo(sourceIndex, target.elements, targetIndex, count);
        target.mutationVersion++;
    }

    /**
     * Appends the selected source range, including a range of this list itself.
     * Validation and capacity growth precede copying and publication of the new size.
     * @param source source list of the same primitive kind
     * @param sourceIndex first source position
     * @param count number of values to append
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if kinds differ or the resulting size is unrepresentable
     * @throws IndexOutOfBoundsException if the source offset/count range is invalid
     */
    public void addAllBits(BigList source, long sourceIndex, long count) {
        Objects.requireNonNull(source, "source");
        requireKind(source);
        Objects.checkFromIndexSize(sourceIndex, count, source.size);
        if (count == 0L) return;
        long nextSize = expandedSize(count);
        ensure(nextSize);
        source.elements.copyTo(sourceIndex, elements, size, count);
        size = nextSize;
        mutationVersion++;
    }

    /**
     * Replaces every value in a half-open logical range with the supplied carrier.
     * @param fromInclusive first position
     * @param toExclusive exclusive end position
     * @param bits value using this list's primitive conversion rules
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void fillBits(long fromInclusive, long toExclusive, long bits) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        if (fromInclusive == toExclusive) return;
        elements.fillBits(fromInclusive, toExclusive, bits);
        mutationVersion++;
    }

    /**
     * Removes a half-open logical range, shifts its suffix left and clears the vacated tail.
     * Capacity is retained. Empty ranges do not invalidate spliterators.
     * @param fromInclusive first removed position
     * @param toExclusive exclusive end of the removed range
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void removeRange(long fromInclusive, long toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        long removed = toExclusive - fromInclusive;
        if (removed == 0L) return;
        elements.copyTo(toExclusive, elements, fromInclusive, size - toExclusive);
        long nextSize = size - removed;
        elements.fillBits(nextSize, size, 0L);
        size = nextSize;
        mutationVersion++;
    }

    /**
     * Finds the first value equal under this primitive kind's equality rules.
     * @param bits value to find
     * @return its global position, or -1 if absent
     */
    public long indexOfBits(long bits) { return indexOfBits(0L, size, bits); }

    /**
     * Finds the first equal value in a half-open range. NaNs compare equal; signed zeros differ.
     * @param fromInclusive first searched position
     * @param toExclusive exclusive end position
     * @param bits value to find
     * @return the global position, or -1 if absent
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public long indexOfBits(long fromInclusive, long toExclusive, long bits) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        for (long index = fromInclusive; index < toExclusive; index++) {
            if (kind.equalBits(elements.getBits(index), bits)) return index;
        }
        return -1L;
    }

    /**
     * Binary-searches this list, which the caller must already have sorted by its primitive kind.
     * @param bits value to find
     * @return a matching global position, or {@code -(insertionPoint + 1)} if absent
     */
    public long binarySearchBits(long bits) { return binarySearchBits(0L, size, bits); }

    /**
     * Binary-searches an already sorted half-open range using the primitive kind's ordering.
     * Sorting is a caller precondition and is not checked by a linear scan. Any matching
     * position may be returned for duplicates. The insertion point is a global index.
     * @param fromInclusive first searched position
     * @param toExclusive exclusive end position
     * @param bits value to find
     * @return a matching global position, or {@code -(insertionPoint + 1)} if absent
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public long binarySearchBits(long fromInclusive, long toExclusive, long bits) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        long low = fromInclusive;
        long high = toExclusive;
        while (low < high) {
            long middle = low + ((high - low) >>> 1);
            int compared = kind.compareBits(elements.getBits(middle), bits);
            if (compared < 0) low = middle + 1L;
            else if (compared > 0) high = middle;
            else return middle;
        }
        return -low - 1L;
    }

    /**
     * Creates an ordered, sized primitive spliterator over the current logical range.
     * The fence and mutation version bind immediately; no payload snapshot is allocated.
     * @return a spliterator of this kind's long bit carriers
     * @see #spliteratorBits(long, long)
     */
    public Spliterator.OfLong spliteratorBits() { return spliteratorBits(0L, size); }

    /**
     * Creates a spliterator over a checked half-open range with long positions and counts.
     * Any successful write to logical elements or size change invalidates it, including
     * same-value replacement; empty range operations and capacity-only changes do not.
     * Traversal, splitting and size queries check for mutation on a best-effort basis.
     * This fail-fast policy does not make this list thread-safe. Callback exceptions propagate.
     * @param fromInclusive first traversed position
     * @param toExclusive exclusive end position
     * @return an ordered, sized, subsized primitive bit-carrier spliterator
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public Spliterator.OfLong spliteratorBits(long fromInclusive, long toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        return new BitsSpliterator(fromInclusive, toExclusive, mutationVersion);
    }

    private final class BitsSpliterator implements Spliterator.OfLong {
        private long cursor;
        private final long fence;
        private final long expectedVersion;

        private BitsSpliterator(long cursor, long fence, long expectedVersion) {
            this.cursor = cursor;
            this.fence = fence;
            this.expectedVersion = expectedVersion;
        }

        @Override public Spliterator.OfLong trySplit() {
            checkVersion();
            long middle = cursor + ((fence - cursor) >>> 1);
            if (middle == cursor) return null;
            Spliterator.OfLong prefix = new BitsSpliterator(cursor, middle, expectedVersion);
            cursor = middle;
            return prefix;
        }

        @Override public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            checkVersion();
            if (cursor == fence) return false;
            long bits = elements.getBits(cursor++);
            action.accept(bits);
            checkVersion();
            return true;
        }

        @Override public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            while (tryAdvance(action)) {
                // The same before/after-callback mutation contract applies to bulk traversal.
            }
        }

        @Override public long estimateSize() {
            checkVersion();
            return fence - cursor;
        }

        @Override public int characteristics() { return ORDERED | SIZED | SUBSIZED | NONNULL; }

        private void checkVersion() {
            if (mutationVersion != expectedVersion) throw new ConcurrentModificationException();
        }
    }

    private void requireKind(BigList other) {
        if (other.kind != kind) throw new IllegalArgumentException("kind mismatch");
    }

    private long expandedSize(long count) {
        if (count < 0L || count > maxCapacity() - size) {
            throw new IllegalArgumentException("list too large");
        }
        return size + count;
    }

    private long check(long index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(Long.toString(index));
        return index;
    }

    private void ensure(long required) {
        if (required <= capacity()) return;
        long current = capacity();
        long grown = grownCapacity(current, required, maxCapacity());
        // Charge only the new directory, replacement tail and newly allocated pages.
        // One heap observation at backing growth; no per-add probe or forced GC.
        elements.resize(RuntimeHeap.chooseSegmentedAmortizedCapacity(
                RuntimeHeap.current(), current, required, grown,
                kind.widthBytes(), segmentShift));
    }

    /** Pure, checked capacity arithmetic; permits boundary proof without giant allocations. */
    static long grownCapacity(long current, long required, long maximum) {
        if (current < 0L || required < 0L || maximum < 0L
                || current > maximum || required > maximum) {
            throw new IllegalArgumentException("capacity outside representable range");
        }
        if (required <= current) return current;
        long growth = Math.min(maximum - current, (current >>> 1) + 1L);
        return Math.max(required, current + growth);
    }
}
