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
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Sorted, duplicate-preserving array sequence. Not a positional-mutation List.
 * Existing equal-ranked payloads precede subsequently admitted equal-ranked payloads.
 * Nulls are rejected. Single-threaded; comparator and predicates must be pure.
 *
 * @param <E> element type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseSortedBuffer.java}
 * blob {@code e73943262ddfc531a6751828b11ad64299c1329f} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseSortedBuffer<E> extends AbstractCollection<E> {
    private Object[] elements = new Object[0];
    private final Comparator<? super E> comparator;
    private int size;
    private int modifications;

    public DenseSortedBuffer() { this(null); }

    public DenseSortedBuffer(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int compare(Object left, Object right) {
        return comparator == null ? ((Comparable<? super E>) left).compareTo((E) right)
                : comparator.compare((E) left, (E) right);
    }

    private void checkElement(E element) {
        Objects.requireNonNull(element, "element");
        compare(element, element);
    }

    @Override public int size() { return size; }
    public int capacity() { return elements.length; }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        Objects.checkIndex(index, size);
        return (E) elements[index];
    }

    public int lowerBound(E element) { return bound(element, false); }
    public int upperBound(E element) { return bound(element, true); }

    private int bound(E element, boolean upper) {
        checkElement(element);
        int left = 0;
        int right = size;
        while (left < right) {
            int middle = (left + right) >>> 1;
            int comparison = compare(elements[middle], element);
            if (comparison < 0 || upper && comparison == 0) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    @Override public boolean add(E element) {
        int index = upperBound(element);
        int required = DenseArrayOrder.sum(size, 1);
        if (required > elements.length) {
            elements = Arrays.copyOf(elements, DenseArrayOrder.capacity(elements.length, required));
        }
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = element;
        size++;
        modifications++;
        return true;
    }

    /** Predicate rejection is explicit offer behavior, not changed Collection.add behavior. */
    public boolean offer(E element, Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        checkElement(element);
        return predicate.test(element) && add(element);
    }

    @Override public boolean addAll(Collection<? extends E> source) {
        return addAllFiltered(source, element -> true) > 0;
    }

    /** Predicate runs once per candidate; comparisons finish before the batch is published. */
    @SuppressWarnings("unchecked")
    public int addAllFiltered(Collection<? extends E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        int expected = modifications;
        Object[] incoming = source.toArray();
        int count = 0;
        for (Object candidate : incoming) {
            E element = (E) candidate;
            checkElement(element);
            if (predicate.test(element)) {
                incoming[count++] = element;
            }
        }
        Arrays.fill(incoming, count, incoming.length, null);
        Arrays.sort(incoming, 0, count, this::compare);
        Object[] merged = new Object[DenseArrayOrder.sum(size, count)];
        int old = 0;
        int added = 0;
        int out = 0;
        while (old < size && added < count) {
            merged[out++] = compare(elements[old], incoming[added]) <= 0
                    ? elements[old++] : incoming[added++];
        }
        System.arraycopy(elements, old, merged, out, size - old);
        out += size - old;
        System.arraycopy(incoming, added, merged, out, count - added);
        if (expected != modifications) { throw new ConcurrentModificationException(); }
        if (count != 0) {
            elements = merged;
            size += count;
            modifications++;
        }
        return count;
    }

    public E removeAt(int index) {
        E previous = get(index);
        System.arraycopy(elements, index + 1, elements, index, size - index - 1);
        elements[--size] = null;
        modifications++;
        return previous;
    }

    @Override public void clear() {
        if (size != 0) {
            Arrays.fill(elements, 0, size, null);
            size = 0;
            modifications++;
        }
    }

    /** Evaluate first, then compact; predicate failure cannot partially remove this batch. */
    @Override public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int expected = modifications;
        long[] removed = new long[(int) (((long) size + 63) >>> 6)];
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (predicate.test(get(i))) {
                removed[i >>> 6] |= 1L << (i & 63);
                count++;
            }
            if (expected != modifications) { throw new ConcurrentModificationException(); }
        }
        if (count == 0) { return false; }
        int out = 0;
        for (int i = 0; i < size; i++) {
            if ((removed[i >>> 6] & (1L << (i & 63))) == 0) {
                elements[out++] = elements[i];
            }
        }
        Arrays.fill(elements, out, size, null);
        size = out;
        modifications++;
        return true;
    }

    /** Shallow, immutable boundary copy; subsequent mutations do not affect it. */
    @SuppressWarnings("unchecked")
    public List<E> snapshot() {
        return Collections.unmodifiableList(Arrays.asList((E[]) Arrays.copyOf(elements, size)));
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int expected = modifications;
            private int next;
            private int last = -1;
            private void check() {
                if (expected != modifications) { throw new ConcurrentModificationException(); }
            }
            @Override public boolean hasNext() { check(); return next < size; }
            @Override public E next() {
                if (!hasNext()) { throw new NoSuchElementException(); }
                last = next;
                return get(next++);
            }
            @Override public void remove() {
                check();
                if (last < 0) { throw new IllegalStateException(); }
                removeAt(last);
                next = last;
                last = -1;
                expected = modifications;
            }
        };
    }
}
