/*******************************************************************************
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
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/** Live, read-only primitive composition with no retained objects per element. */
public final class M3LongCompositeViews {
    private M3LongCompositeViews() { }

    public static M3LongCollection concat(M3LongCollection... backings) {
        return new Concatenation(validate(backings));
    }
    public static M3LongSortedSet sortedUnion(M3LongSortedSet... backings) {
        return new Union(validate(backings));
    }

    /** Live read-only intersection; sorted set occurrences are unique. */
    public static M3LongSortedSet sortedIntersection(
            M3LongSortedSet left, M3LongSortedSet right) {
        return new BinarySetAlgebra(left, right, SetOperation.INTERSECTION);
    }

    /** Live read-only left-minus-right, without copying backing values. */
    public static M3LongSortedSet sortedDifference(
            M3LongSortedSet left, M3LongSortedSet right) {
        return new BinarySetAlgebra(left, right, SetOperation.DIFFERENCE);
    }

    /** Live read-only symmetric set difference, not bag disjunction. */
    public static M3LongSortedSet sortedSymmetricDifference(
            M3LongSortedSet left, M3LongSortedSet right) {
        return new BinarySetAlgebra(left, right, SetOperation.SYMMETRIC_DIFFERENCE);
    }

    private static <T> T[] validate(T[] sources) {
        T[] result = Objects.requireNonNull(sources, "sources").clone();
        for (T source : result) Objects.requireNonNull(source, "source");
        return result;
    }
    private static Spliterator.OfLong cursorSpliterator(M3LongCollection source, int flags) {
        return new Spliterator.OfLong() {
            private M3LongIterator cursor;
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) cursor = source.iterator();
                if (!cursor.hasNext()) return false;
                action.accept(cursor.nextLong());
                return true;
            }
            public OfLong trySplit() { return null; }
            public long estimateSize() { return Long.MAX_VALUE; }
            public int characteristics() { return flags; }
            public java.util.Comparator<? super Long> getComparator() {
                if ((flags & SORTED) == 0) throw new IllegalStateException();
                return null;
            }
        };
    }
    /**
     * Nested compositions are paths through existing child directories, not a
     * reason to recursively wrap every child or materialize primitive payloads.
     */
    private static final class Concatenation implements M3LongCollection {
        private final M3LongCollection[] inputs;
        Concatenation(M3LongCollection[] inputs) { this.inputs = inputs; }

        /** One reference-array frame and one int offset per nesting level. */
        private static final class PathCursor {
            private M3LongCollection[][] frames = new M3LongCollection[8][];
            private int[] offsets = new int[8];
            private int depth = 1;
            private M3LongCollection ready;

            PathCursor(M3LongCollection[] roots) { frames[0] = roots; }

            boolean hasNext() {
                if (ready != null) return true;
                while (depth != 0) {
                    int frame = depth - 1;
                    if (offsets[frame] == frames[frame].length) {
                        frames[frame] = null;
                        depth--;
                        continue;
                    }
                    M3LongCollection child = frames[frame][offsets[frame]++];
                    if (child instanceof Concatenation nested) {
                        if (depth == frames.length) {
                            int capacity = Math.multiplyExact(frames.length, 2);
                            frames = Arrays.copyOf(frames, capacity);
                            offsets = Arrays.copyOf(offsets, capacity);
                        }
                        frames[depth] = nested.inputs;
                        offsets[depth++] = 0;
                    } else {
                        ready = child;
                        return true;
                    }
                }
                return false;
            }

            M3LongCollection next() {
                if (!hasNext()) throw new NoSuchElementException();
                M3LongCollection leaf = ready;
                ready = null;
                return leaf;
            }
        }

        public int size() {
            int count = 0;
            PathCursor path = new PathCursor(inputs);
            while (path.hasNext()) count = Math.addExact(count, path.next().size());
            return count;
        }
        public boolean contains(long value) {
            PathCursor path = new PathCursor(inputs);
            while (path.hasNext()) if (path.next().contains(value)) return true;
            return false;
        }
        public boolean isEmpty() {
            PathCursor path = new PathCursor(inputs);
            while (path.hasNext()) if (!path.next().isEmpty()) return false;
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
                private PathCursor path;
                private M3LongIterator active;
                public boolean hasNext() {
                    if (path == null) path = new PathCursor(inputs);
                    while (active == null || !active.hasNext()) {
                        if (!path.hasNext()) return false;
                        active = path.next().iterator();
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
    private enum SetOperation { INTERSECTION, DIFFERENCE, SYMMETRIC_DIFFERENCE }

    /**
     * Parent-owned live descriptor. Neither an entry node nor an element payload
     * array is created or retained. Contract: ascending signed-long sorted sets.
     * The comparator cannot be arbitrary because M3LongSortedSet has no comparator.
     */
    private static final class BinarySetAlgebra implements M3LongSortedSet {
        private final M3LongSortedSet left;
        private final M3LongSortedSet right;
        private final SetOperation operation;

        BinarySetAlgebra(M3LongSortedSet left, M3LongSortedSet right,
                         SetOperation operation) {
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
            this.operation = operation;
        }

        @Override public boolean contains(long value) {
            return switch (operation) {
                case INTERSECTION -> left.contains(value) && right.contains(value);
                case DIFFERENCE -> left.contains(value) && !right.contains(value);
                case SYMMETRIC_DIFFERENCE -> left.contains(value) != right.contains(value);
            };
        }

        @Override public boolean isEmpty() { return !iterator().hasNext(); }

        @Override public int size() {
            int count = 0;
            M3LongIterator cursor = iterator();
            while (cursor.hasNext()) {
                cursor.nextLong();
                count = Math.addExact(count, 1);
            }
            return count;
        }

        /** Exactly one returned primitive array, with no intermediate entry list. */
        @Override public long[] toArray() {
            long[] values = new long[size()];
            M3LongIterator cursor = iterator();
            int count = 0;
            while (cursor.hasNext()) {
                if (count == values.length) {
                    throw new java.util.ConcurrentModificationException(
                            "sorted algebra source grew during materialization");
                }
                values[count++] = cursor.nextLong();
            }
            if (count != values.length) {
                throw new java.util.ConcurrentModificationException(
                        "sorted algebra source changed during materialization");
            }
            return values;
        }

        @Override public M3LongIterator iterator() {
            return new BinarySetCursor(left.iterator(), right.iterator(), operation);
        }

        @Override public Spliterator.OfLong longSpliterator() {
            return cursorSpliterator(this, Spliterator.ORDERED
                    | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL);
        }

        @Override public long first() {
            M3LongIterator cursor = iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.nextLong();
        }

        @Override public long last() {
            M3LongIterator cursor = iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            long last;
            do { last = cursor.nextLong(); } while (cursor.hasNext());
            return last;
        }

        /**
         * Only the final, first-party M3LongTreeSet supplies proven, signed-long
         * indexed neighbors. Other M3LongSortedSet implementations retain the
         * original cursor traversal, including order/callback validation.
         * For a STRICT neighbor the query itself is a safe miss marker:
         * an existing predecessor or successor cannot equal that query.
         */
        private long indexedSeek(long query, long fallback,
                boolean forward, boolean inclusive) {
            if (inclusive && contains(query)) return query;
            long a = forward ? left.higher(query, query) : left.lower(query, query);
            long b = forward ? right.higher(query, query) : right.lower(query, query);
            boolean haveA = a != query;
            boolean haveB = b != query;
            switch (operation) {
                case INTERSECTION -> {
                    while (haveA && haveB) {
                        int comparison = Long.compare(a, b);
                        if (comparison == 0) return a;
                        if (forward ? comparison < 0 : comparison > 0) {
                            long next = neighbor(left, a, forward);
                            haveA = next != a;
                            a = next;
                        } else {
                            long next = neighbor(right, b, forward);
                            haveB = next != b;
                            b = next;
                        }
                    }
                }
                case DIFFERENCE -> {
                    while (haveA) {
                        if (!right.contains(a)) return a;
                        long next = neighbor(left, a, forward);
                        haveA = next != a;
                        a = next;
                    }
                }
                case SYMMETRIC_DIFFERENCE -> {
                    while (haveA || haveB) {
                        if (!haveA) return b;
                        if (!haveB) return a;
                        int comparison = Long.compare(a, b);
                        if (comparison != 0) {
                            return (forward ? comparison < 0 : comparison > 0) ? a : b;
                        }
                        long nextA = neighbor(left, a, forward);
                        long nextB = neighbor(right, b, forward);
                        haveA = nextA != a;
                        haveB = nextB != b;
                        a = nextA;
                        b = nextB;
                    }
                }
            }
            return fallback;
        }

        private static long neighbor(M3LongSortedSet owner, long pivot,
                boolean forward) {
            return forward ? owner.higher(pivot, pivot) : owner.lower(pivot, pivot);
        }

        private long seek(long query, long fallback, boolean forward, boolean inclusive) {
            if (left instanceof M3LongTreeSet && right instanceof M3LongTreeSet) {
                return indexedSeek(query, fallback, forward, inclusive);
            }
            M3LongIterator cursor = iterator();
            boolean found = false;
            long last = 0L;
            while (cursor.hasNext()) {
                long candidate = cursor.nextLong();
                int compared = Long.compare(candidate, query);
                if (forward) {
                    if (compared > 0 || (inclusive && compared == 0)) return candidate;
                } else if (compared < 0 || (inclusive && compared == 0)) {
                    found = true;
                    last = candidate;
                } else {
                    break;
                }
            }
            return found ? last : fallback;
        }

        @Override public long lower(long query, long fallback) {
            return seek(query, fallback, false, false);
        }
        @Override public long floor(long query, long fallback) {
            return seek(query, fallback, false, true);
        }
        @Override public long higher(long query, long fallback) {
            return seek(query, fallback, true, false);
        }
        @Override public long ceiling(long query, long fallback) {
            return seek(query, fallback, true, true);
        }

        @Override public boolean add(long value) {
            throw new UnsupportedOperationException("read-only primitive set algebra");
        }
        @Override public boolean remove(long value) {
            throw new UnsupportedOperationException("read-only primitive set algebra");
        }
        @Override public int filterInPlace(LongPredicate keep) {
            throw new UnsupportedOperationException("read-only primitive set algebra");
        }
        @Override public void clear() {
            throw new UnsupportedOperationException("read-only primitive set algebra");
        }
    }

    /**
     * Exactly two input control cursors and signed-long scalar coordinates.
     * Advances never allocate a value wrapper or per-element intermediate node.
     * Rejects incorrect source ordering without sentinel MIN_VALUE/MAX_VALUE.
     */
    private static final class BinarySetCursor implements M3LongIterator {
        private final M3LongIterator left;
        private final M3LongIterator right;
        private final SetOperation operation;
        private long leftValue;
        private long rightValue;
        private long previousLeft;
        private long previousRight;
        private long selected;
        private boolean started;
        private boolean leftReady;
        private boolean rightReady;
        private boolean seenLeft;
        private boolean seenRight;
        private boolean outputReady;
        private boolean finished;

        BinarySetCursor(M3LongIterator left, M3LongIterator right, SetOperation operation) {
            this.left = left;
            this.right = right;
            this.operation = operation;
        }

        private void advanceLeft() {
            if (!left.hasNext()) {
                leftReady = false;
                return;
            }
            long current = left.nextLong();
            if (seenLeft && current <= previousLeft) {
                throw new IllegalStateException("left source not strictly ascending signed-long");
            }
            previousLeft = current;
            leftValue = current;
            seenLeft = true;
            leftReady = true;
        }

        private void advanceRight() {
            if (!right.hasNext()) {
                rightReady = false;
                return;
            }
            long current = right.nextLong();
            if (seenRight && current <= previousRight) {
                throw new IllegalStateException("right source not strictly ascending signed-long");
            }
            previousRight = current;
            rightValue = current;
            seenRight = true;
            rightReady = true;
        }

        private boolean found(long value) {
            selected = value;
            outputReady = true;
            return true;
        }

        @Override public boolean hasNext() {
            if (outputReady) return true;
            if (finished) return false;
            if (!started) {
                started = true;
                advanceLeft();
                advanceRight();
            }
            switch (operation) {
                case INTERSECTION -> {
                    while (leftReady && rightReady) {
                        if (leftValue < rightValue) {
                            advanceLeft();
                        } else if (leftValue > rightValue) {
                            advanceRight();
                        } else {
                            long value = leftValue;
                            advanceLeft();
                            advanceRight();
                            return found(value);
                        }
                    }
                }
                case DIFFERENCE -> {
                    while (leftReady) {
                        if (!rightReady || leftValue < rightValue) {
                            long value = leftValue;
                            advanceLeft();
                            return found(value);
                        }
                        if (leftValue == rightValue) {
                            advanceLeft();
                            advanceRight();
                        } else {
                            advanceRight();
                        }
                    }
                }
                case SYMMETRIC_DIFFERENCE -> {
                    while (leftReady || rightReady) {
                        if (!leftReady || (rightReady && rightValue < leftValue)) {
                            long value = rightValue;
                            advanceRight();
                            return found(value);
                        }
                        if (!rightReady || leftValue < rightValue) {
                            long value = leftValue;
                            advanceLeft();
                            return found(value);
                        }
                        advanceLeft();
                        advanceRight();
                    }
                }
            }
            finished = true;
            return false;
        }

        @Override public long nextLong() {
            if (!hasNext()) throw new NoSuchElementException();
            outputReady = false;
            return selected;
        }
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
                if (input[n].hasNext()) insert(n, input[n].nextLong());
            }
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
            int at = 0;
            while (true) {
                int child = (at << 1) + 1; if (child >= size) break;
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
