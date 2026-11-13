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
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An array-owned indexed linked list with reusable integer slots.
 *
 * <p>The linked-list payload is stored in one {@code Object[]} and two
 * {@code int[]} link lanes. Removed slots are chained through the spare
 * {@code next} coordinates and are reused without allocating nodes.
 * A packed {@code long[]} mask records which physical slots are live.
 * Iterators are API boundary objects; individual elements are not.
 *
 * <p>The legacy Apache node-cache size constructor is retained as an
 * allocation hint. There is no node object cache: all removed coordinates
 * are safely reusable until the backing arrays themselves are released.
 * This class is not thread-safe.
 *
 * @param <E> element type
 */
@Deprecated
public final class M3NodeCachingLinkedList<E> extends AbstractSequentialList<E>
        implements Iterable<E>, Collection<E>, List<E>, SequencedCollection<E>, Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private static final int NONE = -1;
    private static final int DEFAULT_CACHE_HINT = 20;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final long[] EMPTY_WORDS = new long[0];

    /** Advisory size of the recycled physical-slot pool. */
    private final int maximumCacheSize;

    private transient Object[] elements = EMPTY_OBJECTS;
    private transient int[] previous = EMPTY_INTS;
    private transient int[] next = EMPTY_INTS;
    private transient long[] markMask = EMPTY_WORDS;
    private transient int first = NONE;
    private transient int last = NONE;
    private transient int free = NONE;
    private transient int used;
    private transient int size;

    /** Constructs an empty list with the default recycled-slot capacity hint. */
    public M3NodeCachingLinkedList() {
        this(DEFAULT_CACHE_HINT);
    }

    /**
     * Constructs an empty list with a physical-slot recycling hint.
     *
     * @param maximumCacheSize advisory recycled-slot capacity
     */
    public M3NodeCachingLinkedList(int maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }

    /**
     * Copies the specified collection into independent array storage.
     *
     * @param source values to copy in their encounter order
     */
    public M3NodeCachingLinkedList(Collection<? extends E> source) {
        this(DEFAULT_CACHE_HINT);
        addAll(source);
    }

    /** Grows every parallel lane before publishing any new backing storage. */
    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        if (required > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("Array-backed linked list too large");
        }
        int oldCapacity = elements.length;
        long growth = oldCapacity == 0
                ? Math.max(8, Math.min(32, Math.max(0, maximumCacheSize)))
                : (long) oldCapacity + (oldCapacity >>> 1) + 1;
        int capacity = (int) Math.min(MAX_ARRAY_SIZE, Math.max(growth, required));
        Object[] newElements = Arrays.copyOf(elements, capacity);
        int[] newPrevious = Arrays.copyOf(previous, capacity);
        int[] newNext = Arrays.copyOf(next, capacity);
        long[] newMask = Arrays.copyOf(markMask, ((capacity - 1) >>> 6) + 1);
        elements = newElements;
        previous = newPrevious;
        next = newNext;
        markMask = newMask;
    }

    /** Allocates only an index into the existing lanes. */
    private int acquire(E element) {
        int slot;
        if (free != NONE) {
            slot = free;
            free = next[slot];
        } else {
            if (used == MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError("Array-backed linked list too large");
            }
            ensureCapacity(used + 1);
            slot = used++;
        }
        elements[slot] = element;
        markMask[slot >>> 6] |= 1L << (slot & 63);
        return slot;
    }

    /** Inserts before an existing physical coordinate, or appends if {@code -1}. */
    private void linkBefore(int successor, E element) {
        int predecessor = successor == NONE ? last : previous[successor];
        int slot = acquire(element);
        previous[slot] = predecessor;
        next[slot] = successor;
        if (predecessor == NONE) {
            first = slot;
        } else {
            next[predecessor] = slot;
        }
        if (successor == NONE) {
            last = slot;
        } else {
            previous[successor] = slot;
        }
        size++;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    private E elementAt(int slot) {
        return (E) elements[slot];
    }

    /** Releases an element reference and chains its integer slot for reuse. */
    private E unlink(int slot) {
        E value = elementAt(slot);
        int predecessor = previous[slot];
        int successor = next[slot];
        if (predecessor == NONE) {
            first = successor;
        } else {
            next[predecessor] = successor;
        }
        if (successor == NONE) {
            last = predecessor;
        } else {
            previous[successor] = predecessor;
        }
        elements[slot] = null;
        markMask[slot >>> 6] &= ~(1L << (slot & 63));
        previous[slot] = NONE;
        next[slot] = free;
        free = slot;
        size--;
        modCount++;
        return value;
    }

    private int coordinate(int index) {
        Objects.checkIndex(index, size);
        int slot;
        if (index < (size >>> 1)) {
            slot = first;
            for (int i = 0; i < index; i++) {
                slot = next[slot];
            }
        } else {
            slot = last;
            for (int i = size - 1; i > index; i--) {
                slot = previous[slot];
            }
        }
        return slot;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean add(E element) {
        addLast(element);
        return true;
    }

    @Override
    public void addFirst(E element) {
        linkBefore(first, element);
    }

    @Override
    public void addLast(E element) {
        linkBefore(NONE, element);
    }

    @Override
    public E getFirst() {
        if (first == NONE) {
            throw new NoSuchElementException();
        }
        return elementAt(first);
    }

    @Override
    public E getLast() {
        if (last == NONE) {
            throw new NoSuchElementException();
        }
        return elementAt(last);
    }

    @Override
    public E removeFirst() {
        if (first == NONE) {
            throw new NoSuchElementException();
        }
        return unlink(first);
    }

    @Override
    public E removeLast() {
        if (last == NONE) {
            throw new NoSuchElementException();
        }
        return unlink(last);
    }

    @Override
    public E get(int index) {
        return elementAt(coordinate(index));
    }

    @Override
    public E set(int index, E element) {
        int slot = coordinate(index);
        E previousValue = elementAt(slot);
        elements[slot] = element;
        return previousValue;
    }

    @Override
    public void add(int index, E element) {
        Objects.checkIndex(index, size + 1);
        linkBefore(index == size ? NONE : coordinate(index), element);
    }

    @Override
    public E remove(int index) {
        return unlink(coordinate(index));
    }

    @Override
    public boolean addAll(Collection<? extends E> source) {
        return addAll(size, source);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> source) {
        Objects.checkIndex(index, size + 1);
        Object[] snapshot = Objects.requireNonNull(source, "source").toArray();
        if (snapshot.length == 0) {
            return false;
        }
        int successor = index == size ? NONE : coordinate(index);
        // Preflight all allocations before making the first structural change.
        if (snapshot.length > MAX_ARRAY_SIZE - size) {
            throw new OutOfMemoryError("Array-backed linked list too large");
        }
        int available = used - size;
        long additional = Math.max(0L, (long) snapshot.length - available);
        long required = used + additional;
        if (required > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("Array-backed linked list too large");
        }
        ensureCapacity((int) required);
        for (Object value : snapshot) {
            @SuppressWarnings("unchecked") E element = (E) value;
            linkBefore(successor, element);
        }
        return true;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(elements, 0, used, null);
        Arrays.fill(markMask, 0L);
        first = NONE;
        last = NONE;
        free = NONE;
        used = 0;
        size = 0;
        modCount++;
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Objects.checkIndex(index, size + 1);
        return new SlotIterator(index);
    }

    private final class SlotIterator implements ListIterator<E> {
        private int nextSlot;
        private int nextIndex;
        private int lastReturned = NONE;
        private int expectedModCount = modCount;

        private SlotIterator(int index) {
            nextIndex = index;
            nextSlot = index == size ? NONE : coordinate(index);
        }

        private void checkForComodification() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public E next() {
            checkForComodification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturned = nextSlot;
            nextSlot = M3NodeCachingLinkedList.this.next[nextSlot];
            nextIndex++;
            return elementAt(lastReturned);
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            nextSlot = nextSlot == NONE ? last : M3NodeCachingLinkedList.this.previous[nextSlot];
            lastReturned = nextSlot;
            nextIndex--;
            return elementAt(lastReturned);
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (lastReturned == NONE) {
                throw new IllegalStateException();
            }
            int nextAfterRemoved = M3NodeCachingLinkedList.this.next[lastReturned];
            if (nextSlot == lastReturned) {
                nextSlot = nextAfterRemoved;
            } else {
                nextIndex--;
            }
            unlink(lastReturned);
            lastReturned = NONE;
            expectedModCount = modCount;
        }

        @Override
        public void set(E element) {
            checkForComodification();
            if (lastReturned == NONE) {
                throw new IllegalStateException();
            }
            elements[lastReturned] = element;
        }

        @Override
        public void add(E element) {
            checkForComodification();
            linkBefore(nextSlot, element);
            nextIndex++;
            lastReturned = NONE;
            expectedModCount = modCount;
        }
    }

    @Override
    public boolean contains(Object value) {
        return super.contains(value);
    }

    @Override
    public boolean containsAll(Collection<?> values) {
        return super.containsAll(values);
    }

    @Override
    public int indexOf(Object value) {
        return super.indexOf(value);
    }

    @Override
    public int lastIndexOf(Object value) {
        return super.lastIndexOf(value);
    }

    @Override
    public boolean remove(Object value) {
        return super.remove(value);
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        return super.removeAll(values);
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        return super.retainAll(values);
    }

    @Override
    public boolean removeIf(Predicate<? super E> predicate) {
        return super.removeIf(predicate);
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
    public List<E> reversed() {
        return super.reversed();
    }

    @Override
    public List<E> subList(int from, int to) {
        return super.subList(from, to);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        super.forEach(action);
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }

    @Override
    public <T> T[] toArray(T[] target) {
        return super.toArray(target);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return super.toArray(generator);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED);
    }

    @Override
    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
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
     * Writes the ordered logical elements without serializing physical free slots.
     *
     * @param out output stream
     * @throws IOException if the stream cannot be written
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size);
        for (E element : this) {
            out.writeObject(element);
        }
    }

    /**
     * Recreates the logical order in new array storage after deserialization.
     *
     * @param in input stream
     * @throws IOException if the stream data is invalid or cannot be read
     * @throws ClassNotFoundException if a serialized element type is unavailable
     */
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        elements = EMPTY_OBJECTS;
        previous = EMPTY_INTS;
        next = EMPTY_INTS;
        markMask = EMPTY_WORDS;
        first = last = free = NONE;
        used = size = 0;
        int count = in.readInt();
        if (count < 0 || count > MAX_ARRAY_SIZE) {
            throw new InvalidObjectException("Invalid array-linked list length: " + count);
        }
        for (int i = 0; i < count; i++) {
            addLast((E) in.readObject());
        }
        modCount = 0;
    }
}
