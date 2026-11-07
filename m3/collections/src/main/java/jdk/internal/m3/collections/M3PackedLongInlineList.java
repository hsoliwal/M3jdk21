// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.M3LongSequence;
import jdk.m3.collections.LongStack;

/**
 * Small-buffer primitive long sequence adapted from the Apache-2.0 Abseil InlinedVector mechanic.
 *
 * <p>The first four values live in scalar fields, so an empty or tiny list retains no separate
 * primitive payload array. The fifth value spills once into the ordinary primitive-array regime.
 * Existing {@link M3LongArrayList} remains the general one-array owner.</p>
 */
//FLLDCIM: RELOCATE - donor blob c0b02969ffe9 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3PackedLongInlineList implements M3LongSequence, LongStack {
    public static final int INLINE_CAPACITY = 4;

    private long e0;
    private long e1;
    private long e2;
    private long e3;
    private long[] spill;
    private int size;

    public M3PackedLongInlineList() {}

    public M3PackedLongInlineList(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (expectedSize > INLINE_CAPACITY) spill = new long[expectedSize];
    }

    public static M3PackedLongInlineList copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        M3PackedLongInlineList result = new M3PackedLongInlineList(source.length);
        result.size = source.length;
        if (result.spill != null) {
            System.arraycopy(source, 0, result.spill, 0, source.length);
        } else {
            for (int index = 0; index < source.length; index++) result.inlineSet(index, source[index]);
        }
        return result;
    }

    @Override
    public boolean add(long value) {
        int required = M3PackedArrayCapacity.required(size, 1);
        ensureCapacity(required);
        rawSet(size++, value);
        return true;
    }

    @Override
    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) return false;
        add(value);
        return true;
    }

    @Override
    public boolean addAll(long[] values) {
        long[] checked = Objects.requireNonNull(values, "values");
        if (checked.length == 0) return false;
        int required = M3PackedArrayCapacity.required(size, checked.length);
        ensureCapacity(required);
        if (spill != null) {
            System.arraycopy(checked, 0, spill, size, checked.length);
            size = required;
            return true;
        }
        for (long value : checked) inlineSet(size++, value);
        return true;
    }

    @Override
    public void insert(int index, long value) {
        M3PackedSupport.checkPosition(index, size);
        int required = M3PackedArrayCapacity.required(size, 1);
        ensureCapacity(required);
        if (spill != null) {
            System.arraycopy(spill, index, spill, index + 1, size - index);
            spill[index] = value;
        } else {
            for (int cursor = size; cursor > index; cursor--) {
                inlineSet(cursor, inlineGet(cursor - 1));
            }
            inlineSet(index, value);
        }
        size = required;
    }

    @Override
    public long get(int index) {
        M3PackedSupport.checkIndex(index, size);
        return rawGet(index);
    }

    @Override
    public long set(int index, long value) {
        M3PackedSupport.checkIndex(index, size);
        long previous = rawGet(index);
        rawSet(index, value);
        return previous;
    }

    @Override
    public long removeAt(int index) {
        M3PackedSupport.checkIndex(index, size);
        long previous = rawGet(index);
        if (spill != null) {
            int moved = size - index - 1;
            if (moved > 0) System.arraycopy(spill, index + 1, spill, index, moved);
        } else {
            for (int cursor = index; cursor < size - 1; cursor++) {
                inlineSet(cursor, inlineGet(cursor + 1));
            }
        }
        size--;
        return previous;
    }

    @Override
    public boolean removeValue(long value) {
        int index = indexOf(value);
        if (index < 0) return false;
        removeAt(index);
        return true;
    }

    @Override
    public int indexOf(long value) {
        // Only the inline two-to-four-element state gains this specialized atom.
        // The spill path retains its original loop and original search order.
        if (spill == null && size >= 2) {
            if (e0 == value) return 0;
            if (e1 == value) return 1;
            if (size >= 3 && e2 == value) return 2;
            if (size >= 4 && e3 == value) return 3;
            return -1;
        }
        for (int index = 0; index < size; index++) if (rawGet(index) == value) return index;
        return -1;
    }

    @Override
    public int lastIndexOf(long value) {
        for (int index = size - 1; index >= 0; index--) if (rawGet(index) == value) return index;
        return -1;
    }

    @Override
    public boolean contains(long value) { return indexOf(value) >= 0; }

    public void sort() {
        if (spill != null) {
            Arrays.sort(spill, 0, size);
            return;
        }
        for (int index = 1; index < size; index++) {
            long value = inlineGet(index);
            int cursor = index;
            while (cursor > 0 && inlineGet(cursor - 1) > value) {
                inlineSet(cursor, inlineGet(cursor - 1));
                cursor--;
            }
            inlineSet(cursor, value);
        }
    }

    public int binarySearch(long value) {
        if (spill != null) return Arrays.binarySearch(spill, 0, size, value);
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long candidate = inlineGet(middle);
            if (candidate < value) low = middle + 1;
            else if (candidate > value) high = middle - 1;
            else return middle;
        }
        return -low - 1;
    }

    public int deduplicateSorted() {
        if (size < 2) return 0;
        int write = 1;
        for (int read = 1; read < size; read++) {
            long value = rawGet(read);
            if (value != rawGet(write - 1)) rawSet(write++, value);
        }
        int removed = size - write;
        size = write;
        return removed;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int originalSize = size;
        int read = 0;
        int write = 0;
        try {
            for (; read < originalSize; read++) {
                long value = rawGet(read);
                if (predicate.test(value)) rawSet(write++, value);
            }
        } finally {
            for (int cursor = read; cursor < originalSize; cursor++) rawSet(write++, rawGet(cursor));
            size = write;
        }
        return originalSize - size;
    }

    @Override public void push(long value) { add(value); }

    @Override
    public long peek() {
        if (size == 0) throw new NoSuchElementException();
        return rawGet(size - 1);
    }

    @Override
    public long pop() {
        long value = peek();
        size--;
        return value;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) throw new OutOfMemoryError("required array size too large");
        if (spill == null) {
            if (minimumCapacity <= INLINE_CAPACITY) return;
            int capacity = Math.max(8, minimumCapacity);
            long[] grown = new long[capacity];
            for (int index = 0; index < size; index++) grown[index] = inlineGet(index);
            spill = grown;
            return;
        }
        if (minimumCapacity > spill.length) spill = Arrays.copyOf(spill, M3HeapGrowth.capacity(spill.length, minimumCapacity, 8));
    }

    @Override
    public void trimToSize() {
        if (spill == null) return;
        if (size <= INLINE_CAPACITY) {
            long[] old = spill;
            spill = null;
            for (int index = 0; index < size; index++) inlineSet(index, old[index]);
            return;
        }
        if (size != spill.length) spill = Arrays.copyOf(spill, size);
    }

    @Override public int size() { return size; }

    @Override
    public int capacity() { return spill == null ? INLINE_CAPACITY : spill.length; }

    public boolean spilled() { return spill != null; }

    public long retainedSpillBytes() {
        return spill == null ? 0L : Math.multiplyExact((long) spill.length, Long.BYTES);
    }

    @Override public void clear() { size = 0; }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        if (spill != null) {
            System.arraycopy(spill, 0, result, 0, size);
        } else {
            for (int index = 0; index < size; index++) result[index] = inlineGet(index);
        }
        return result;
    }

    @Override public Spliterator.OfLong longSpliterator() {
        return new RangeSpliterator(this, 0, size);
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private int index;
            @Override public boolean hasNext() { return index < size; }
            @Override public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return rawGet(index++);
            }
        };
    }

    private long rawGet(int index) { return spill == null ? inlineGet(index) : spill[index]; }

    private void rawSet(int index, long value) {
        if (spill == null) inlineSet(index, value);
        else spill[index] = value;
    }

    private long inlineGet(int index) {
        return switch (index) {
            case 0 -> e0;
            case 1 -> e1;
            case 2 -> e2;
            case 3 -> e3;
            default -> throw new IndexOutOfBoundsException("inline index: " + index);
        };
    }

    private void inlineSet(int index, long value) {
        switch (index) {
            case 0 -> e0 = value;
            case 1 -> e1 = value;
            case 2 -> e2 = value;
            case 3 -> e3 = value;
            default -> throw new IndexOutOfBoundsException("inline index: " + index);
        }
    }

    private static final class RangeSpliterator implements Spliterator.OfLong {
        private final M3PackedLongInlineList owner;
        private int index;
        private final int fence;

        private RangeSpliterator(M3PackedLongInlineList owner, int index, int fence) {
            this.owner = owner;
            this.index = index;
            this.fence = fence;
        }

        @Override
        public OfLong trySplit() {
            int low = index;
            int middle = (low + fence) >>> 1;
            if (low >= middle) return null;
            index = middle;
            return new RangeSpliterator(owner, low, middle);
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (index >= fence) return false;
            action.accept(owner.rawGet(index++));
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            while (index < fence) action.accept(owner.rawGet(index++));
        }

        @Override public long estimateSize() { return fence - index; }
        @Override public int characteristics() { return ORDERED | SIZED | SUBSIZED | NONNULL; }
    }
}
