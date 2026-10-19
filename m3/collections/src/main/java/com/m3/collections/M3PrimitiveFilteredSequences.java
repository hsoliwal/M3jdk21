/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Live filtered adapters over the existing primitive list, deque, queue and stack owners.
 * No value copies, per-element entries or node objects are retained by any view.
 * The backing owner keeps physical offsets, generations, occupancy and modification epochs.
 * Predicates must be stable, deterministic, and must not mutate the backing collection.
 * Neither the views nor the underlying owners become concurrent or serializable.
 *
 * <p>Read-only views reject all mutation, even a no-op. Writable views reject any
 * proposed value excluded by the filter before modifying the backing owner. Hidden
 * values are not removed by clear, filterInPlace or occurrence removal. Insertion
 * at the end of a filtered list appends after the entire backing list, including
 * any hidden suffix. Deque removal uses the owner's primitive occurrence-removal
 * methods; value-stable predicates ensure equal primitive values have equal visibility.
 *
 * <p>Views hold only a source, predicate and mutation-mode bit. One primitive
 * cursor object is made per traversal, not per element. A list sort explicitly
 * allocates primitive temporary coordinate and value lanes for its accepted
 * elements; all other view traversal is streamed without retained element arrays.
 */
public final class M3PrimitiveFilteredSequences {
    private M3PrimitiveFilteredSequences() { }

    public static M3LongList list(M3LongList source, LongPredicate predicate) {
        return new ListView(source, predicate, true);
    }

    public static M3LongList readOnlyList(M3LongList source, LongPredicate predicate) {
        return new ListView(source, predicate, false);
    }

    public static M3LongDeque deque(M3LongDeque source, LongPredicate predicate) {
        return new DequeView(source, predicate, true);
    }

    public static M3LongDeque readOnlyDeque(M3LongDeque source, LongPredicate predicate) {
        return new DequeView(source, predicate, false);
    }

    /** The resulting deque also implements the primitive queue contract. */
    public static M3LongQueue queue(M3LongDeque source, LongPredicate predicate) {
        return deque(source, predicate);
    }

    public static M3LongQueue readOnlyQueue(M3LongDeque source, LongPredicate predicate) {
        return readOnlyDeque(source, predicate);
    }

    /** The existing indexed list owns the stack's physical order and tail removal. */
    public static M3LongStack stack(M3LongList source, LongPredicate predicate) {
        return new StackView(source, predicate, true);
    }

    public static M3LongStack readOnlyStack(M3LongList source, LongPredicate predicate) {
        return new StackView(source, predicate, false);
    }

    private static void admit(LongPredicate predicate, long value) {
        if (!predicate.test(value)) {
            throw new IllegalArgumentException("value rejected by filtered collection");
        }
    }

    private static UnsupportedOperationException readonly() {
        return new UnsupportedOperationException("read-only filtered collection");
    }

    private static final class Cursor implements M3LongIterator {
        private final M3LongIterator source;
        private final LongPredicate predicate;
        private boolean ready;
        private boolean ended;
        private long next;

        Cursor(M3LongIterator source, LongPredicate predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override public boolean hasNext() {
            if (ready) return true;
            if (ended) return false;
            while (source.hasNext()) {
                long candidate = source.nextLong();
                if (predicate.test(candidate)) {
                    next = candidate;
                    ready = true;
                    return true;
                }
            }
            ended = true;
            return false;
        }

        @Override public long nextLong() {
            if (!hasNext()) throw new NoSuchElementException();
            ready = false;
            return next;
        }
    }

    private static int count(M3LongIterator it) {
        int result = 0;
        while (it.hasNext()) {
            it.nextLong();
            result++;
        }
        return result;
    }

    /** One-pass primitive materialization; no source.size() assumption or boxing. */
    private static long[] copy(M3LongIterator it) {
        long[] values = new long[0];
        int used = 0;
        while (it.hasNext()) {
            if (used == values.length) {
                if (used == Integer.MAX_VALUE - 8) throw new OutOfMemoryError("filtered sequence too large");
                int expanded = used == 0 ? 8 : (int) Math.min(Integer.MAX_VALUE - 8L,
                        Math.max((long) used + 1L, (long) used + (used >>> 1) + 1L));
                values = Arrays.copyOf(values, expanded);
            }
            values[used++] = it.nextLong();
        }
        return used == values.length ? values : Arrays.copyOf(values, used);
    }

    private abstract static class View {
        final LongPredicate predicate;
        final boolean writable;

        View(LongPredicate predicate, boolean writable) {
            this.predicate = Objects.requireNonNull(predicate, "predicate");
            this.writable = writable;
        }

        final void requireWritable() {
            if (!writable) throw readonly();
        }

        final void requireAccepted(long value) {
            requireWritable();
            admit(predicate, value);
        }

        final M3LongIterator filter(M3LongIterator source) {
            return new Cursor(source, predicate);
        }
    }

    private static final class ListView extends View implements M3LongList {
        private final M3LongList source;

        ListView(M3LongList source, LongPredicate predicate, boolean writable) {
            super(predicate, writable);
            this.source = Objects.requireNonNull(source, "source");
        }

        /** Index of the visible occurrence; end maps to the physical array end. */
        private int physical(int visibleIndex, boolean allowEnd) {
            if (visibleIndex < 0) throw new IndexOutOfBoundsException(visibleIndex);
            int seen = 0;
            int limit = source.size();
            for (int i = 0; i < limit; i++) {
                if (predicate.test(source.get(i))) {
                    if (seen == visibleIndex) return i;
                    seen++;
                }
            }
            if (allowEnd && visibleIndex == seen) return limit;
            throw new IndexOutOfBoundsException(visibleIndex);
        }

        @Override public int size() { return count(iterator()); }
        @Override public boolean contains(long value) {
            return source.contains(value) && predicate.test(value);
        }
        @Override public M3LongIterator iterator() { return filter(source.iterator()); }
        @Override public long[] toArray() { return copy(iterator()); }
        @Override public int capacity() { return source.capacity(); }
        @Override public long get(int index) { return source.get(physical(index, false)); }

        @Override public boolean add(long value) {
            requireAccepted(value);
            return source.add(value);
        }
        @Override public void add(int index, long value) {
            requireAccepted(value);
            source.add(physical(index, true), value);
        }
        @Override public boolean addIf(long value, LongPredicate condition) {
            requireWritable();
            Objects.requireNonNull(condition, "condition");
            if (!condition.test(value)) return false;
            requireAccepted(value);
            return source.add(value);
        }
        @Override public boolean addAll(long[] values) {
            requireWritable();
            Objects.requireNonNull(values, "values");
            for (long value : values) admit(predicate, value);
            return source.addAll(values);
        }
        @Override public long set(int index, long value) {
            requireAccepted(value);
            return source.set(physical(index, false), value);
        }
        @Override public long removeAt(int index) {
            requireWritable();
            return source.removeAt(physical(index, false));
        }
        @Override public boolean remove(long value) {
            requireWritable();
            return predicate.test(value) && source.remove(value);
        }
        @Override public int indexOf(long value) {
            if (!predicate.test(value)) return -1;
            int visible = 0;
            M3LongIterator it = iterator();
            while (it.hasNext()) {
                if (it.nextLong() == value) return visible;
                visible++;
            }
            return -1;
        }
        @Override public int lastIndexOf(long value) {
            if (!predicate.test(value)) return -1;
            int visible = 0;
            int last = -1;
            M3LongIterator it = iterator();
            while (it.hasNext()) {
                if (it.nextLong() == value) last = visible;
                visible++;
            }
            return last;
        }
        @Override public void clear() {
            requireWritable();
            source.filterInPlace(v -> !predicate.test(v));
        }
        @Override public int filterInPlace(LongPredicate condition) {
            requireWritable();
            Objects.requireNonNull(condition, "condition");
            return source.filterInPlace(v -> !predicate.test(v) || condition.test(v));
        }
        @Override public void sort() {
            requireWritable();
            int logical = source.size();
            int accepted = size();
            if (accepted < 2) return;
            long[] values = new long[accepted];
            int[] positions = new int[accepted];
            int written = 0;
            for (int i = 0; i < logical; i++) {
                long v = source.get(i);
                if (predicate.test(v)) {
                    if (written == accepted) throw new ConcurrentModificationException();
                    values[written] = v;
                    positions[written++] = i;
                }
            }
            if (written != accepted) throw new ConcurrentModificationException();
            Arrays.sort(values);
            for (int i = 0; i < accepted; i++) source.set(positions[i], values[i]);
        }
        /** Sorted visible values required; linear lower bound avoids a scratch array. */
        @Override public int binarySearch(long target) {
            int index = 0;
            M3LongIterator it = iterator();
            while (it.hasNext()) {
                long current = it.nextLong();
                if (current >= target) return current == target ? index : -(index + 1);
                index++;
            }
            return -(index + 1);
        }
        @Override public int deduplicateSorted() {
            requireWritable();
            final boolean[] seen = {false};
            final long[] previous = {0L};
            return source.filterInPlace(v -> {
                if (!predicate.test(v)) return true;
                if (seen[0] && previous[0] == v) return false;
                seen[0] = true;
                previous[0] = v;
                return true;
            });
        }
        @Override public void ensureCapacity(int minimumCapacity) {
            requireWritable();
            source.ensureCapacity(minimumCapacity);
        }
        @Override public void trimToSize() {
            requireWritable();
            source.trimToSize();
        }
    }

    private static final class DequeView extends View implements M3LongDeque {
        private final M3LongDeque source;

        DequeView(M3LongDeque source, LongPredicate predicate, boolean writable) {
            super(predicate, writable);
            this.source = Objects.requireNonNull(source, "source");
        }

        @Override public int size() { return count(iterator()); }
        @Override public int capacity() { return source.capacity(); }
        @Override public boolean contains(long value) {
            return source.contains(value) && predicate.test(value);
        }
        @Override public M3LongIterator iterator() { return filter(source.iterator()); }
        @Override public M3LongIterator descendingIterator() {
            return filter(source.descendingIterator());
        }
        @Override public long[] toArray() { return copy(iterator()); }
        @Override public long get(int index) {
            if (index < 0) throw new IndexOutOfBoundsException(index);
            int cursor = 0;
            M3LongIterator it = iterator();
            while (it.hasNext()) {
                long value = it.nextLong();
                if (cursor++ == index) return value;
            }
            throw new IndexOutOfBoundsException(index);
        }
        @Override public long getFirst() {
            M3LongIterator it = iterator();
            if (!it.hasNext()) throw new NoSuchElementException();
            return it.nextLong();
        }
        @Override public long getLast() {
            M3LongIterator it = descendingIterator();
            if (!it.hasNext()) throw new NoSuchElementException();
            return it.nextLong();
        }
        @Override public boolean offerFirst(long value) {
            requireAccepted(value);
            return source.offerFirst(value);
        }
        @Override public boolean offerLast(long value) {
            requireAccepted(value);
            return source.offerLast(value);
        }
        @Override public void addFirst(long value) {
            requireAccepted(value);
            source.addFirst(value);
        }
        @Override public void addLast(long value) {
            requireAccepted(value);
            source.addLast(value);
        }
        @Override public boolean offer(long value) { return offerLast(value); }
        @Override public boolean addLastIf(long value, LongPredicate condition) {
            requireWritable();
            Objects.requireNonNull(condition, "condition");
            if (!condition.test(value)) return false;
            return offerLast(value);
        }
        @Override public boolean removeFirstOccurrence(long value) {
            requireWritable();
            return predicate.test(value) && source.removeFirstOccurrence(value);
        }
        @Override public boolean removeLastOccurrence(long value) {
            requireWritable();
            return predicate.test(value) && source.removeLastOccurrence(value);
        }
        @Override public long removeFirstLong() {
            requireWritable();
            long value = getFirst();
            if (!source.removeFirstOccurrence(value)) throw new ConcurrentModificationException();
            return value;
        }
        @Override public long removeLastLong() {
            requireWritable();
            long value = getLast();
            if (!source.removeLastOccurrence(value)) throw new ConcurrentModificationException();
            return value;
        }
        @Override public long peek() { return getFirst(); }
        @Override public long poll() { return removeFirstLong(); }
        @Override public void clear() {
            requireWritable();
            source.filterInPlace(v -> !predicate.test(v));
        }
        @Override public int filterInPlace(LongPredicate condition) {
            requireWritable();
            Objects.requireNonNull(condition, "condition");
            return source.filterInPlace(v -> !predicate.test(v) || condition.test(v));
        }
    }

    private static final class StackView extends View implements M3LongStack {
        private final M3LongList source;

        StackView(M3LongList source, LongPredicate predicate, boolean writable) {
            super(predicate, writable);
            this.source = Objects.requireNonNull(source, "source");
        }

        @Override public int size() { return count(iterator()); }
        @Override public boolean contains(long value) {
            return source.contains(value) && predicate.test(value);
        }
        @Override public M3LongIterator iterator() { return filter(source.iterator()); }
        @Override public long[] toArray() { return copy(iterator()); }
        private int top() {
            for (int i = source.size() - 1; i >= 0; i--) {
                if (predicate.test(source.get(i))) return i;
            }
            throw new NoSuchElementException();
        }
        @Override public void push(long value) {
            requireAccepted(value);
            source.add(value);
        }
        @Override public long peek() { return source.get(top()); }
        @Override public long pop() {
            requireWritable();
            return source.removeAt(top());
        }
        @Override public void clear() {
            requireWritable();
            source.filterInPlace(v -> !predicate.test(v));
        }
    }
}
