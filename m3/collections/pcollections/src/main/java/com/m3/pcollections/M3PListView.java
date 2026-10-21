/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import org.pcollections.AbstractUnmodifiableList;

/**
 * Immutable filtered/sorted projection of one persistent vector version. Payload
 * stays in the source owner; a primitive index directory, slice offset and reverse
 * flag compose views. Subviews may retain unseen source payload. Predicates and
 * comparators are evaluated in Java and never retained or passed through JNI.
 * Serialization includes only visible values, rebuilding a dense source owner.
 */
public final class M3PListView<E> extends AbstractUnmodifiableList<E>
        implements RandomAccess, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3PVector<E> owner;
    private final int[] positions;
    private final int offset;
    private final int length;
    private final boolean reverse;

    private M3PListView(M3PVector<E> owner, int[] positions, int offset,
                       int length, boolean reverse) {
        this.owner = owner;
        this.positions = positions;
        this.offset = offset;
        this.length = length;
        this.reverse = reverse;
    }

    /** Captures exactly this immutable vector version without copying its payload. */
    public static <E> M3PListView<E> of(M3PVector<E> source) {
        Objects.requireNonNull(source, "source");
        int[] positions = new int[source.size()];
        Arrays.setAll(positions, index -> index);
        return new M3PListView<>(source, positions, 0, positions.length, false);
    }

    /** Materializes a mutable/external collection once before constructing a view. */
    public static <E> M3PListView<E> copyOf(Collection<? extends E> source) {
        return of(M3PVector.from(Objects.requireNonNull(source, "source")));
    }

    @Override public int size() { return length; }
    @Override public E get(int index) { return owner.get(sourceIndex(index)); }
    @Override public Iterator<E> iterator() { return listIterator(0); }
    @Override public ListIterator<E> listIterator() { return listIterator(0); }
    @Override public ListIterator<E> listIterator(int start) {
        if (start < 0 || start > length) throw new IndexOutOfBoundsException(start);
        return new ListIterator<>() {
            private int cursor = start;
            @Override public boolean hasNext() { return cursor < length; }
            @Override public boolean hasPrevious() { return cursor > 0; }
            @Override public int nextIndex() { return cursor; }
            @Override public int previousIndex() { return cursor - 1; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return get(cursor++);
            }
            @Override public E previous() {
                if (!hasPrevious()) throw new NoSuchElementException();
                return get(--cursor);
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
            @Override public void set(E value) { throw new UnsupportedOperationException(); }
            @Override public void add(E value) { throw new UnsupportedOperationException(); }
        };
    }

    // Package-only derived-coordinate probes: not part of the serialized contract.
    int sourceIndex(int index) {
        Objects.checkIndex(index, length);
        return positions[offset + (reverse ? length - 1 - index : index)];
    }

    int[] sourceIndices() {
        int[] result = new int[length];
        for (int index = 0; index < length; index++) result[index] = sourceIndex(index);
        return result;
    }

    /** Uses the deterministic Java selector by default. */
    public M3PListView<E> filter(Predicate<? super E> predicate) {
        return filter(predicate, Backend.JAVA);
    }

    /**
     * Calls the predicate exactly once per visible occurrence, in encounter order,
     * stopping at the first exception. Native availability is checked before any
     * predicate call. Only the resulting primitive byte state lane crosses JNI.
     */
    public M3PListView<E> filter(Predicate<? super E> predicate, Backend backend) {
        Objects.requireNonNull(predicate, "predicate");
        requireBackend(backend);
        byte[] states = new byte[length];
        for (int index = 0; index < length; index++) {
            if (predicate.test(get(index))) states[index] = 1;
        }
        return selected(M3IndexMaskCompactor.selectPositions(states, 1, backend));
    }

    /**
     * Selects current logical positions whose state contains all unsigned-byte mask
     * bits. State length must equal this view's size. The input is defensively
     * copied; later caller mutation cannot affect this view or its result.
     */
    public M3PListView<E> select(byte[] states, int requiredMask, Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        if (states.length != length) throw new IllegalArgumentException("State/view lengths differ");
        if ((requiredMask & ~0xff) != 0) throw new IllegalArgumentException("Mask must fit an unsigned byte");
        requireBackend(backend);
        return selected(M3IndexMaskCompactor.selectPositions(states.clone(), requiredMask, backend));
    }

    private static void requireBackend(Backend backend) {
        Objects.requireNonNull(backend, "backend");
        if (backend == Backend.NATIVE_REQUIRED && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
        }
    }

    private M3PListView<E> selected(int[] logicalPositions) {
        for (int index = 0; index < logicalPositions.length; index++) {
            logicalPositions[index] = sourceIndex(logicalPositions[index]);
        }
        return new M3PListView<>(owner, logicalPositions, 0, logicalPositions.length, false);
    }

    /** Stable natural-order projection. Null/non-comparable members follow Comparable. */
    @SuppressWarnings("unchecked")
    public M3PListView<E> sorted() {
        return sorted((left, right) -> ((Comparable<? super E>) left).compareTo(right));
    }

    /**
     * Stable bottom-up merge of primitive positions. Equal comparator keys retain
     * the current encounter order, including duplicate occurrences. The source
     * and all previous views remain unchanged if comparison throws. Two primitive
     * arrays are scratch/output; no boxed index or per-element structural object
     * is retained. Comparator cost excludes the vector's O(log retained-size) get.
     */
    public M3PListView<E> sorted(Comparator<? super E> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        int[] current = sourceIndices();
        if (length < 2) return new M3PListView<>(owner, current, 0, length, false);
        int[] scratch = new int[length];
        for (long width = 1; width < length; width *= 2) {
            for (long start = 0; start < length; start += width * 2) {
                int left = (int) start;
                int middle = (int) Math.min(start + width, length);
                int end = (int) Math.min(start + 2 * width, length);
                int a = left;
                int b = middle;
                for (int out = left; out < end; out++) {
                    if (b == end || (a < middle && comparator.compare(
                            owner.get(current[a]), owner.get(current[b])) <= 0)) {
                        scratch[out] = current[a++];
                    } else {
                        scratch[out] = current[b++];
                    }
                }
            }
            int[] swap = current;
            current = scratch;
            scratch = swap;
        }
        return new M3PListView<>(owner, current, 0, length, false);
    }

    @Override public M3PListView<E> subList(int from, int to) {
        if (from < 0 || from > length || to < 0 || to > length) throw new IndexOutOfBoundsException();
        if (from > to) throw new IllegalArgumentException("from > to");
        if (from == 0 && to == length) return this;
        return new M3PListView<>(owner, positions,
                offset + (reverse ? length - to : from), to - from, reverse);
    }

    @Override public M3PListView<E> reversed() {
        return new M3PListView<>(owner, positions, offset, length, !reverse);
    }

    /** Materializes visible values, releasing hidden owner/index retention in the result. */
    public M3PVector<E> toVector() { return M3PVector.<E>empty().plusAll(this); }

    @Override public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public void addFirst(E value) { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public void addLast(E value) { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public E removeFirst() { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public E removeLast() { throw new UnsupportedOperationException(); }

    @Serial private Object writeReplace() { return new SerializationProxy(toArray()); }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // ObjectOutputStream checks serializability of each visible element.
        @SuppressWarnings("serial") private final Object[] elements;
        SerializationProxy(Object[] elements) { this.elements = elements; }
        @Serial private Object readResolve() throws InvalidObjectException {
            if (elements == null) throw new InvalidObjectException("Missing view elements");
            return M3PListView.copyOf(Arrays.asList(elements));
        }
    }
}
