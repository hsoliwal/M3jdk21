// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongPredicate;

/**
 * Live, read-only primitive composition with no retained objects per element.
 * Streams bind on first valid traversal; metadata and trySplit acquire no iterators.
 * Source mutation during traversal remains unsupported. A sorted union still needs
 * one head from every source when traversal starts; it never freezes them at stream creation.
 */
public final class M3LongCompositeViews {
    private M3LongCompositeViews() { }

    public static M3LongCollection concat(M3LongCollection... backings) {
        return new Concatenation(validate(backings));
    }
    public static M3LongSortedSet sortedUnion(M3LongSortedSet... backings) {
        return new Union(validate(backings));
    }
    private static <T> T[] validate(T[] sources) {
        T[] result = Objects.requireNonNull(sources, "sources").clone();
        for (T source : result) Objects.requireNonNull(source, "source");
        return result;
    }
    private static Spliterator.OfLong cursorSpliterator(M3LongCollection source, int flags) {
        return M3LongTraversal.of(source, flags);
    }

    private static final class Concatenation implements M3LongCollection {
        private final M3LongCollection[] inputs;
        Concatenation(M3LongCollection[] inputs) { this.inputs = inputs; }
        public int size() {
            int n = 0;
            for (M3LongCollection c : inputs) n = Math.addExact(n, c.size());
            return n;
        }
        public boolean contains(long value) {
            for (M3LongCollection c : inputs) if (c.contains(value)) return true;
            return false;
        }
        public boolean isEmpty() {
            for (M3LongCollection c : inputs) if (!c.isEmpty()) return false;
            return true;
        }
        public void clear() { throw new UnsupportedOperationException("read-only composite"); }
        public long[] toArray() {
            long[] result = new long[size()];
            int i = 0;
            M3LongIterator cursor = iterator();
            while (cursor.hasNext()) {
                if (i == result.length) throw new IllegalStateException("backing size changed");
                result[i++] = cursor.nextLong();
            }
            if (i != result.length) throw new IllegalStateException("backing size changed");
            return result;
        }
        public M3LongIterator iterator() {
            return new M3LongIterator() {
                private int index;
                private M3LongIterator active;
                public boolean hasNext() {
                    while (active == null || !active.hasNext()) {
                        if (index == inputs.length) return false;
                        active = inputs[index++].iterator();
                    }
                    return true;
                }
                public long nextLong() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return active.nextLong();
                }
            };
        }
        public Spliterator.OfLong longSpliterator() {
            return cursorSpliterator(this, Spliterator.ORDERED | Spliterator.NONNULL);
        }
    }
    private static final class Union implements M3LongSortedSet {
        private final M3LongSortedSet[] inputs;
        Union(M3LongSortedSet[] inputs) { this.inputs = inputs; }
        public boolean add(long v) { throw new UnsupportedOperationException("read-only union"); }
        public boolean remove(long v) { throw new UnsupportedOperationException("read-only union"); }
        public void clear() { throw new UnsupportedOperationException("read-only union"); }
        public int filterInPlace(LongPredicate p) { throw new UnsupportedOperationException("read-only union"); }
        public boolean contains(long v) {
            for (M3LongSortedSet s : inputs) if (s.contains(v)) return true;
            return false;
        }
        public boolean isEmpty() {
            for (M3LongSortedSet s : inputs) if (!s.isEmpty()) return false;
            return true;
        }
        public M3LongIterator iterator() { return new Merge(inputs); }
        public int size() {
            int n = 0; M3LongIterator i = iterator();
            while (i.hasNext()) { i.nextLong(); n = Math.addExact(n, 1); }
            return n;
        }
        public long[] toArray() {
            long[] result = new long[Math.min(8, inputs.length)]; int size = 0;
            M3LongIterator i = iterator();
            while (i.hasNext()) {
                if (size == result.length) result = Arrays.copyOf(result,
                        Math.max(size + 1, Math.max(8, result.length + (result.length >>> 1))));
                result[size++] = i.nextLong();
            }
            return size == result.length ? result : Arrays.copyOf(result, size);
        }
        public Spliterator.OfLong longSpliterator() {
            return cursorSpliterator(this, Spliterator.ORDERED | Spliterator.NONNULL
                    | Spliterator.SORTED | Spliterator.DISTINCT);
        }
        public long first() { M3LongIterator i = iterator(); if (!i.hasNext()) throw new NoSuchElementException(); return i.nextLong(); }
        public long last() {
            boolean found = false; long result = 0;
            for (M3LongSortedSet source : inputs) if (!source.isEmpty()) {
                long x = source.last(); if (!found || x > result) result = x; found = true;
            }
            if (!found) throw new NoSuchElementException();
            return result;
        }
        public long lower(long q, long fallback) {
            boolean found = false; long result = 0;
            for (M3LongSortedSet s : inputs) {
                long x = s.lower(q, q); if (x == q) continue;
                if (!found || x > result) result = x; found = true;
            }
            return found ? result : fallback;
        }
        public long higher(long q, long fallback) {
            boolean found = false; long result = 0;
            for (M3LongSortedSet s : inputs) {
                long x = s.higher(q, q); if (x == q) continue;
                if (!found || x < result) result = x; found = true;
            }
            return found ? result : fallback;
        }
        public long floor(long q, long fallback) { return contains(q) ? q : lower(q, fallback); }
        public long ceiling(long q, long fallback) { return contains(q) ? q : higher(q, fallback); }
    }
    /** One primitive heap head per source; no Map.Entry or per-value nodes. */
    private static final class Merge implements M3LongIterator {
        private final M3LongIterator[] input;
        private final long[] head;
        private final int[] owner;
        private int size;
        private boolean ready, previous;
        private long next, last;
        Merge(M3LongSortedSet[] sources) {
            input = new M3LongIterator[sources.length];
            head = new long[sources.length]; owner = new int[sources.length];
            for (int n = 0; n < sources.length; n++) {
                input[n] = sources[n].iterator();
                if (input[n].hasNext()) {
                    head[size] = input[n].nextLong();
                    owner[size++] = n;
                }
            }
            // Same source callback order and tie-breaker; O(k) bottom-up heap construction.
            for (int at = (size >>> 1) - 1; at >= 0; at--) siftDown(at);
        }
        private boolean before(int a, int b) {
            int c = Long.compare(head[a], head[b]);
            return c < 0 || (c == 0 && owner[a] < owner[b]);
        }
        private void swap(int a, int b) {
            long x = head[a]; head[a] = head[b]; head[b] = x;
            int n = owner[a]; owner[a] = owner[b]; owner[b] = n;
        }
        private void insert(int id, long value) {
            int at = size++; head[at] = value; owner[at] = id;
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (before(parent, at)) break;
                swap(parent, at); at = parent;
            }
        }
        private void pop() {
            --size; if (size == 0) return;
            head[0] = head[size]; owner[0] = owner[size];
            siftDown(0);
        }
        private void siftDown(int at) {
            // Test for an internal node before computing 2*at+1, avoiding int overflow.
            while (at < (size >>> 1)) {
                int child = (at << 1) + 1;
                if (child + 1 < size && before(child + 1, child)) child++;
                if (before(at, child)) break;
                swap(at, child); at = child;
            }
        }
        public boolean hasNext() {
            if (ready) return true;
            while (size > 0) {
                long value = head[0]; int from = owner[0]; pop();
                if (input[from].hasNext()) insert(from, input[from].nextLong());
                if (!previous || value != last) { next = value; ready = true; return true; }
            }
            return false;
        }
        public long nextLong() {
            if (!hasNext()) throw new NoSuchElementException();
            ready = false; previous = true; last = next; return next;
        }
    }
}
