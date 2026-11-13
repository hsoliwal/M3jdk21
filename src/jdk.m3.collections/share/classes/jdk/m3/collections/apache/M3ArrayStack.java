/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.SequencedCollection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Array-owned, unsynchronized indexed stack retaining Apache ArrayStack's
 * public stack operations and Java 21 {@link List} contract.
 *
 * <p>Logical positions in {@code [0, size)} are the occupied coordinates
 * in the one mutable {@code Object[]} storage lane. No Node, Entry or retained
 * per-element Java object is allocated. The size coordinate itself is the
 * occupancy state; a separate bitmap would duplicate that information.
 * Positional appends and top-of-stack operations are amortized O(1), while
 * insertion and removal at arbitrary indexes shift one contiguous span.
 *
 * @param <E> type of each element
 */
@Deprecated
public final class M3ArrayStack<E> extends AbstractList<E>
        implements RandomAccess, Cloneable, Serializable, SequencedCollection<E> {

    @Serial
    private static final long serialVersionUID = 0L;

    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final Object[] EMPTY = new Object[0];

    private transient Object[] elements;
    private transient int size;

    /** Constructs an empty stack without allocating a backing array. */
    public M3ArrayStack() {
        elements = EMPTY;
    }

    /** Constructs an empty stack with a nonnegative capacity hint. */
    public M3ArrayStack(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative initialCapacity");
        }
        elements = initialCapacity == 0 ? EMPTY : new Object[initialCapacity];
    }

    /**
     * Expands the single payload array if necessary; the capacity is not
     * part of the logical List size.
     */
    public void ensureCapacity(int minCapacity) {
        if (minCapacity <= elements.length) {
            return;
        }
        if (minCapacity < 0 || minCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("M3 array stack exceeds array limit");
        }
        long growth = elements.length == 0 ? DEFAULT_CAPACITY
                : (long) elements.length + (elements.length >>> 1) + 1;
        int next = (int) Math.min(MAX_ARRAY_SIZE, Math.max(growth, minCapacity));
        elements = Arrays.copyOf(elements, next);
        modCount++;
    }

    /** Drops unused physical capacity without changing logical element order. */
    public void trimToSize() {
        if (elements.length != size) {
            elements = size == 0 ? EMPTY : Arrays.copyOf(elements, size);
            modCount++;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** Apache-compatible stack alias for {@link #isEmpty()}. */
    public boolean empty() {
        return isEmpty();
    }

    @SuppressWarnings("unchecked")
    private E elementAt(int index) {
        return (E) elements[index];
    }

    @Override
    public E get(int index) {
        return elementAt(Objects.checkIndex(index, size));
    }

    @Override
    public E set(int index, E value) {
        int position = Objects.checkIndex(index, size);
        E previous = elementAt(position);
        elements[position] = value;
        return previous;
    }

    @Override
    public boolean add(E value) {
        int next = Math.addExact(size, 1);
        ensureCapacity(next);
        elements[size] = value;
        size = next;
        modCount++;
        return true;
    }

    @Override
    public void add(int index, E value) {
        Objects.checkIndex(index, size + 1);
        int next = Math.addExact(size, 1);
        ensureCapacity(next);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size = next;
        modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return addAll(size, collection);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        Objects.checkIndex(index, size + 1);
        Object[] incoming = Objects.requireNonNull(collection, "collection").toArray();
        if (incoming.length == 0) {
            return false;
        }
        int next = Math.addExact(size, incoming.length);
        ensureCapacity(next);
        System.arraycopy(elements, index, elements, index + incoming.length, size - index);
        System.arraycopy(incoming, 0, elements, index, incoming.length);
        size = next;
        modCount++;
        return true;
    }

    @Override
    public E remove(int index) {
        int position = Objects.checkIndex(index, size);
        E previous = elementAt(position);
        int moved = size - position - 1;
        if (moved > 0) {
            System.arraycopy(elements, position + 1, elements, position, moved);
        }
        elements[--size] = null;
        modCount++;
        return previous;
    }

    @Override
    public boolean remove(Object value) {
        int index = indexOf(value);
        if (index == -1) {
            return false;
        }
        remove(index);
        return true;
    }

    @Override
    public void clear() {
        if (size > 0) {
            Arrays.fill(elements, 0, size, null);
            size = 0;
            modCount++;
        }
    }

    @Override
    public int indexOf(Object value) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(value, elements[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object value) {
        for (int i = size - 1; i >= 0; i--) {
            if (Objects.equals(value, elements[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object value) {
        return indexOf(value) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> values) {
        return super.containsAll(values);
    }

    /**
     * Captures predicate decisions in a compact primitive bitmap, then
     * compacts contiguous survivor runs only after callbacks have succeeded.
     * A predicate exception leaves this operation's array mutation uncommitted.
     * The bitmap is allocated only on the first rejection.
     */
    private boolean removeSelected(Predicate<? super E> predicate) {
        int expected = modCount;
        int oldSize = size;
        long[] rejected = null;
        int firstRejection = oldSize;
        for (int i = 0; i < oldSize; i++) {
            boolean remove = predicate.test(elementAt(i));
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
            if (remove) {
                if (rejected == null) {
                    rejected = new long[((oldSize - 1) >>> 6) + 1];
                    firstRejection = i;
                }
                rejected[i >>> 6] |= 1L << (i & 63);
            }
        }
        if (rejected == null) {
            return false;
        }
        int write = firstRejection;
        for (int read = firstRejection; read < oldSize;) {
            if ((rejected[read >>> 6] & (1L << (read & 63))) != 0L) {
                read++;
                continue;
            }
            int start = read;
            while (read < oldSize
                    && (rejected[read >>> 6] & (1L << (read & 63))) == 0L) {
                read++;
            }
            System.arraycopy(elements, start, elements, write, read - start);
            write += read - start;
        }
        Arrays.fill(elements, write, oldSize, null);
        size = write;
        modCount++;
        return true;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return removeSelected(Objects.requireNonNull(filter, "filter"));
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        return removeSelected(values::contains);
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        return removeSelected(element -> !values.contains(element));
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        super.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super E> comparator) {
        super.sort(comparator);
    }

    @Override
    public void addFirst(E value) {
        add(0, value);
    }

    @Override
    public void addLast(E value) {
        add(value);
    }

    @Override
    public E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elementAt(0);
    }

    @Override
    public E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elementAt(size - 1);
    }

    @Override
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(0);
    }

    @Override
    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(size - 1);
    }

    /** Pushes an element to the top and returns the same reference. */
    public E push(E value) {
        add(value);
        return value;
    }

    /** Returns the current top without removal. */
    public E peek() throws EmptyStackException {
        if (size == 0) {
            throw new EmptyStackException();
        }
        return elementAt(size - 1);
    }

    /**
     * Returns an element at the zero-based depth from the top.
     *
     * <p>Preserves the Apache implementation's exceptional behavior for
     * negative depth: a positive out-of-range physical index is delegated
     * to the indexed getter.
     */
    public E peek(int depth) throws EmptyStackException {
        int index = size - depth - 1;
        if (index < 0) {
            throw new EmptyStackException();
        }
        return get(index);
    }

    /** Removes and returns the stack's top element. */
    public E pop() throws EmptyStackException {
        if (size == 0) {
            throw new EmptyStackException();
        }
        return remove(size - 1);
    }

    /** Returns the 1-based depth of the nearest matching value from the top. */
    public int search(Object value) {
        for (int i = size - 1; i >= 0; i--) {
            Object item = elements[i];
            if (value == null ? item == null : value.equals(item)) {
                return size - i;
            }
        }
        return -1;
    }

    @Override
    public List<E> reversed() {
        return super.reversed();
    }

    @Override
    public List<E> subList(int from, int to) {
        return super.subList(from, to);
    }

    @Override
    public Iterator<E> iterator() {
        return super.iterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        return super.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return super.listIterator(index);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        super.forEach(action);
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] target) {
        Objects.requireNonNull(target, "target");
        if (target.length < size) {
            return (T[]) Arrays.copyOf(elements, size, target.getClass());
        }
        System.arraycopy(elements, 0, target, 0, size);
        if (target.length > size) {
            target[size] = null;
        }
        return target;
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return super.toArray(generator);
    }

    @Override
    public Spliterator<E> spliterator() {
        return super.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return super.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return super.parallelStream();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Shallow, structurally independent clone: values are shared references
     * but the mutable storage array is always detached.
     */
    @Override
    public Object clone() {
        try {
            @SuppressWarnings("unchecked")
            M3ArrayStack<E> clone = (M3ArrayStack<E>) super.clone();
            clone.elements = size == 0 ? EMPTY : Arrays.copyOf(elements, elements.length);
            clone.modCount = 0;
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeObject(elements[i]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        elements = EMPTY;
        size = 0;
        int count = in.readInt();
        if (count < 0 || count > MAX_ARRAY_SIZE) {
            throw new InvalidObjectException("Invalid array stack length: " + count);
        }
        ensureCapacity(count);
        for (int i = 0; i < count; i++) {
            elements[size++] = in.readObject();
        }
        modCount = 0;
    }
}
