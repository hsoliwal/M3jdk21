/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Native M3 generic reference-keyed range map over parallel growing arrays.
 * This is not a Guava RangeMap, an Apache decorator, or a library adapter.
 *
 * <p>The only authoritative payload is {@code Object[]} for each endpoint and
 * value, plus one aligned {@code int[]} word per span for full-width state masks.
 * Half-open ranges are comparator-ordered, disjoint, and nonempty; neighboring
 * equal-valued spans are intentionally not coalesced. No Span/Entry/Node object
 * is retained. An explicit outward callback may construct API objects itself.
 *
 * <p>Contains/get are O(log r) via binary search; range overwrite/removal is
 * O(r) in the worst case due to aligned array shifts, with O(1) retained
 * per-span metadata and amortized capacity growth. This owner is single-threaded,
 * relies on a stable comparator and keys, and does not support reentrant mutation
 * from comparator callbacks. It has no serialization/concurrency contract.
 * Native selection is restricted to detached primitive indexes and int state
 * planes through the existing M3IndexPathMasks Java/C17 JNI owner.</p>
 */
public final class M3ObjectRangeMap<K, V> {
    public static final int LIVE = 1;
    public static final int VISIBLE = 2;
    public static final int DIRTY = 4;
    public static final int PIN_REQUEST = 8; // metadata, not a GC pin

    private final Comparator<? super K> comparator;
    private Object[] starts = M3SlotArrays.EMPTY_OBJECTS;
    private Object[] ends = M3SlotArrays.EMPTY_OBJECTS;
    private Object[] values = M3SlotArrays.EMPTY_OBJECTS;
    private int[] states = M3SlotArrays.EMPTY_INTS;
    private int size;
    private long revision;

    public M3ObjectRangeMap(Comparator<? super K> comparator) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    public Comparator<? super K> comparator() { return comparator; }
    public int spanCount() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return values.length; }
    public long revision() { return revision; }

    @SuppressWarnings("unchecked")
    private K startAt(int index) { return (K) starts[index]; }
    @SuppressWarnings("unchecked")
    private K endAt(int index) { return (K) ends[index]; }
    @SuppressWarnings("unchecked")
    private V valueAt(int index) { return (V) values[index]; }

    private int compare(K left, K right) { return comparator.compare(left, right); }
    private int checkRange(K from, K to) {
        int cmp = compare(from, to);
        if (cmp > 0) throw new IllegalArgumentException("inverted comparator range");
        return cmp;
    }
    private int firstEndingAfter(K point) {
        int low = 0, high = size;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (compare(endAt(middle), point) <= 0) low = middle + 1;
            else high = middle;
        }
        return low;
    }
    private int firstStartingAtOrAfter(K point) {
        int low = 0, high = size;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (compare(startAt(middle), point) < 0) low = middle + 1;
            else high = middle;
        }
        return low;
    }
    private int physicalAt(K point) {
        int index = firstEndingAfter(point);
        return index < size && compare(startAt(index), point) <= 0 ? index : -1;
    }

    public boolean containsPoint(K point) { return physicalAt(point) >= 0; }
    public V get(K point) {
        int slot = physicalAt(point);
        return slot < 0 ? null : valueAt(slot);
    }
    public V getOrDefault(K point, V fallback) {
        int slot = physicalAt(point);
        return slot < 0 ? fallback : valueAt(slot);
    }
    public int stateAtPoint(K point) {
        int slot = physicalAt(point);
        return slot < 0 ? 0 : states[slot];
    }
    public boolean intersects(K from, K to) {
        if (checkRange(from, to) == 0) return false;
        int first = firstEndingAfter(from);
        return first < size && compare(startAt(first), to) < 0;
    }

    private void ensureCapacity(int minimum) {
        if (minimum <= starts.length) return;
        int capacity = starts.length;
        while (capacity < minimum) capacity = M3SlotArrays.grow(capacity);
        // Construct all candidate arrays before publishing any replacement lane.
        Object[] s = Arrays.copyOf(starts, capacity);
        Object[] e = Arrays.copyOf(ends, capacity);
        Object[] v = Arrays.copyOf(values, capacity);
        int[] m = Arrays.copyOf(states, capacity);
        starts = s; ends = e; values = v; states = m;
    }

    private static void requireFlags(int flags) {
        if ((flags & LIVE) != 0) throw new IllegalArgumentException("LIVE bit reserved");
    }
    public void put(K from, K to, V value) { put(from, to, value, 0); }
    public void put(K from, K to, V value, int stateBits) {
        requireFlags(stateBits);
        edit(from, to, value, stateBits | LIVE, true);
    }
    public boolean remove(K from, K to) { return edit(from, to, null, 0, false); }

    private boolean edit(K from, K to, V value, int state, boolean inserting) {
        if (checkRange(from, to) == 0) return false;
        int first = firstEndingAfter(from);
        int end = firstStartingAtOrAfter(to);
        if (!inserting && first == end) return false;
        boolean left = first < end && compare(startAt(first), from) < 0;
        boolean right = first < end && compare(endAt(end - 1), to) > 0;
        int inserted = (left ? 1 : 0) + (inserting ? 1 : 0) + (right ? 1 : 0);
        int resulting = Math.addExact(size - (end - first), inserted);
        K originalLeft = left ? startAt(first) : null;
        K originalRight = right ? endAt(end - 1) : null;
        V leftValue = left ? valueAt(first) : null;
        V rightValue = right ? valueAt(end - 1) : null;
        int leftState = left ? states[first] : 0;
        int rightState = right ? states[end - 1] : 0;
        ensureCapacity(resulting);
        int trailing = size - end;
        System.arraycopy(starts, end, starts, first + inserted, trailing);
        System.arraycopy(ends, end, ends, first + inserted, trailing);
        System.arraycopy(values, end, values, first + inserted, trailing);
        System.arraycopy(states, end, states, first + inserted, trailing);
        int write = first;
        if (left) {
            starts[write] = originalLeft; ends[write] = from;
            values[write] = leftValue; states[write++] = leftState;
        }
        if (inserting) {
            starts[write] = from; ends[write] = to;
            values[write] = value; states[write++] = state;
        }
        if (right) {
            starts[write] = to; ends[write] = originalRight;
            values[write] = rightValue; states[write++] = rightState;
        }
        if (resulting < size) {
            Arrays.fill(starts, resulting, size, null);
            Arrays.fill(ends, resulting, size, null);
            Arrays.fill(values, resulting, size, null);
            Arrays.fill(states, resulting, size, 0);
        }
        size = resulting;
        revision++;
        return true;
    }

    /** Change state without replacing a value or touching source interval geometry. */
    public int updateState(K point, int setBits, int clearBits) {
        requireFlags(setBits); requireFlags(clearBits);
        if ((setBits & clearBits) != 0) throw new IllegalArgumentException("overlapping masks");
        int slot = physicalAt(point);
        if (slot < 0) throw new IllegalArgumentException("absent point");
        int old = states[slot], next = (old | setBits) & ~clearBits;
        if (next != old) { states[slot] = next; revision++; }
        return old;
    }
    public void clear() {
        if (size == 0) return;
        Arrays.fill(starts, 0, size, null);
        Arrays.fill(ends, 0, size, null);
        Arrays.fill(values, 0, size, null);
        Arrays.fill(states, 0, size, 0);
        size = 0; revision++;
    }

    @FunctionalInterface
    public interface SpanConsumer<K, V> {
        void accept(K from, K to, V value, int flags);
    }
    /** Avoid retained Span/Entry objects; caller controls callbacks. */
    public void forEachSpan(SpanConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        long expected = revision;
        for (int i = 0; i < size; i++) {
            action.accept(startAt(i), endAt(i), valueAt(i), states[i]);
            if (revision != expected) throw new ConcurrentModificationException("range map mutated");
        }
    }

    /** Stable order index of every span; values remain in their authoritative array. */
    public IndexView spans() {
        int[] ordinals = new int[size];
        for (int i = 0; i < size; i++) ordinals[i] = i;
        return new IndexView(ordinals, 0, ordinals.length, revision);
    }
    /** View of intersecting source spans, without clipping their endpoint keys. */
    public IndexView intersecting(K from, K to) {
        if (checkRange(from, to) == 0) return new IndexView(new int[0], 0, 0, revision);
        int first = firstEndingAfter(from), last = firstStartingAtOrAfter(to);
        int[] ordinals = new int[Math.max(0, last - first)];
        for (int i = 0; i < ordinals.length; i++) ordinals[i] = first + i;
        return new IndexView(ordinals, 0, ordinals.length, revision);
    }

    public final class IndexView {
        private final int[] ordinals;
        private final int from, to;
        private final long generation;
        private IndexView(int[] ordinals, int from, int to, long generation) {
            this.ordinals = ordinals; this.from = from; this.to = to;
            this.generation = generation;
        }
        private void valid() {
            if (generation != revision) throw new ConcurrentModificationException("stale range index");
        }
        public int size() { valid(); return to - from; }
        public int physicalAt(int index) {
            valid(); return ordinals[from + Objects.checkIndex(index, to - from)];
        }
        public K startAt(int index) { return M3ObjectRangeMap.this.startAt(physicalAt(index)); }
        public K endAt(int index) { return M3ObjectRangeMap.this.endAt(physicalAt(index)); }
        public V valueAt(int index) { return M3ObjectRangeMap.this.valueAt(physicalAt(index)); }
        public int stateAt(int index) { return states[physicalAt(index)]; }
        public int[] physicalOrdinals() { valid(); return Arrays.copyOfRange(ordinals, from, to); }
        public IndexView slice(int begin, int end) {
            valid(); Objects.checkFromToIndex(begin, end, to - from);
            return new IndexView(ordinals, from + begin, from + end, generation);
        }
        public IndexView reversed() {
            valid(); int[] result = physicalOrdinals();
            for (int a = 0, b = result.length - 1; a < b; a++, b--) {
                int temp = result[a]; result[a] = result[b]; result[b] = temp;
            }
            return new IndexView(result, 0, result.length, generation);
        }
        /** Stable bottom-up primitive-ordinal mergesort; no payload movement. */
        public IndexView sortedByValue(Comparator<? super V> order) {
            Objects.requireNonNull(order, "order"); valid();
            int[] sorted = physicalOrdinals(), scratch = new int[sorted.length];
            for (long width = 1; width < sorted.length; width <<= 1) {
                for (long base = 0; base < sorted.length; base += width << 1) {
                    int a = (int) base;
                    int middle = (int) Math.min(sorted.length, base + width);
                    int end = (int) Math.min(sorted.length, base + (width << 1));
                    int b = middle, write = a;
                    while (a < middle && b < end) {
                        int compared = order.compare(M3ObjectRangeMap.this.valueAt(sorted[a]),
                                M3ObjectRangeMap.this.valueAt(sorted[b]));
                        valid();
                        scratch[write++] = compared <= 0 ? sorted[a++] : sorted[b++];
                    }
                    while (a < middle) scratch[write++] = sorted[a++];
                    while (b < end) scratch[write++] = sorted[b++];
                }
                int[] swap = sorted; sorted = scratch; scratch = swap;
            }
            return new IndexView(sorted, 0, sorted.length, generation);
        }
        /** One user callback per logical span; never retained in a native plan. */
        public IndexView filtered(Predicate<? super V> predicate) {
            Objects.requireNonNull(predicate, "predicate"); valid();
            int[] kept = new int[to - from]; int used = 0;
            for (int i = from; i < to; i++) {
                int ordinal = ordinals[i];
                boolean chosen = predicate.test(M3ObjectRangeMap.this.valueAt(ordinal));
                valid();
                if (chosen) kept[used++] = ordinal;
            }
            return new IndexView(Arrays.copyOf(kept, used), 0, used, generation);
        }
        /** Full 32-bit required/forbidden masks via existing Java/JNI owner. */
        public IndexView selected(int required, int forbidden, M3IndexPathMasks.Backend backend) {
            Objects.requireNonNull(backend, "backend"); valid();
            if ((required & forbidden) != 0) {
                throw new IllegalArgumentException("contradictory state masks");
            }
            int[] source = physicalOrdinals();
            int[] selected = M3IndexPathMasks.select(source, source, new int[][] {states},
                    new int[] {required}, new int[] {forbidden}, 0, source.length, 1, backend);
            valid();
            return new IndexView(selected, 0, selected.length, generation);
        }
    }
}
