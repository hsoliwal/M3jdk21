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
 * A positional list stored as an implicit order-statistic tree in flat arrays.
 *
 * <p>Physical references occupy one {@code Object[]}; child coordinates,
 * subtree cardinalities and balancing priorities occupy primitive {@code int[]}
 * lanes. No per-element tree nodes or mapping-entry objects are allocated.
 * The tree's in-order rank is the logical list index: random access and
 * middle insertions/removals require expected O(log n) coordinate steps.
 *
 * <p>Removed coordinates are reused through a free index chain, and an
 * occupancy bitmap tracks which slots are live. Iterators and standard JDK
 * list views may allocate API objects, but none is retained per element.
 * This class is not synchronized.
 *
 * @param <E> element type
 */
public final class M3TreeList<E> extends AbstractList<E>
        implements Iterable<E>, Collection<E>, List<E>, SequencedCollection<E>, Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private static final int NONE = -1;
    private static final int INITIAL_CAPACITY = 8;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final long[] EMPTY_WORDS = new long[0];

    private transient Object[] elements = EMPTY_OBJECTS;
    private transient int[] left = EMPTY_INTS;
    private transient int[] right = EMPTY_INTS;
    private transient int[] weight = EMPTY_INTS;
    private transient int[] priority = EMPTY_INTS;
    private transient long[] markMask = EMPTY_WORDS;

    private transient int root = NONE;
    private transient int free = NONE;
    private transient int used;
    private transient int size;
    private transient long sequence;

    /** Constructs an empty list, deferring all payload allocation. */
    public M3TreeList() {
    }

    /**
     * Copies the encounter order into a separate array-owned tree.
     *
     * @param source source values in encounter order
     */
    public M3TreeList(Collection<? extends E> source) {
        addAll(source);
    }

    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        if (required > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("Array-backed order-statistic tree too large");
        }
        int oldCapacity = elements.length;
        long growth = oldCapacity == 0 ? INITIAL_CAPACITY
                : (long) oldCapacity + (oldCapacity >>> 1) + 1;
        int capacity = (int) Math.min(MAX_ARRAY_SIZE, Math.max(growth, required));
        Object[] nextElements = Arrays.copyOf(elements, capacity);
        int[] nextLeft = Arrays.copyOf(left, capacity);
        int[] nextRight = Arrays.copyOf(right, capacity);
        int[] nextWeight = Arrays.copyOf(weight, capacity);
        int[] nextPriority = Arrays.copyOf(priority, capacity);
        long[] nextMask = Arrays.copyOf(markMask, ((capacity - 1) >>> 6) + 1);
        elements = nextElements;
        left = nextLeft;
        right = nextRight;
        weight = nextWeight;
        priority = nextPriority;
        markMask = nextMask;
    }

    /** SplitMix-derived priority, independent of key equality or element hash codes. */
    private int nextPriority() {
        long bits = (sequence += 0x9e3779b97f4a7c15L);
        bits = (bits ^ (bits >>> 30)) * 0xbf58476d1ce4e5b9L;
        bits = (bits ^ (bits >>> 27)) * 0x94d049bb133111ebL;
        return (int) (bits ^ (bits >>> 31));
    }

    private int acquire(E element) {
        int slot;
        if (free != NONE) {
            slot = free;
            free = right[slot];
        } else {
            if (used >= MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError("Array-backed order-statistic tree too large");
            }
            ensureCapacity(used + 1);
            slot = used++;
        }
        elements[slot] = element;
        left[slot] = NONE;
        right[slot] = NONE;
        weight[slot] = 1;
        priority[slot] = nextPriority();
        markMask[slot >>> 6] |= 1L << (slot & 63);
        return slot;
    }

    private void release(int slot) {
        elements[slot] = null;
        left[slot] = NONE;
        weight[slot] = 0;
        markMask[slot >>> 6] &= ~(1L << (slot & 63));
        right[slot] = free;
        free = slot;
    }

    private int count(int coordinate) {
        return coordinate == NONE ? 0 : weight[coordinate];
    }

    private void refresh(int coordinate) {
        weight[coordinate] = 1 + count(left[coordinate]) + count(right[coordinate]);
    }

    private int merge(int before, int after) {
        if (before == NONE) {
            return after;
        }
        if (after == NONE) {
            return before;
        }
        if (Integer.compareUnsigned(priority[before], priority[after]) >= 0) {
            right[before] = merge(right[before], after);
            refresh(before);
            return before;
        }
        left[after] = merge(before, left[after]);
        refresh(after);
        return after;
    }

    /** Packs two nullable physical offsets into one primitive return value. */
    private static long pair(int first, int second) {
        return ((long) (first + 1) << 32) | ((second + 1L) & 0xffffffffL);
    }

    private static int first(long pair) {
        return (int) (pair >>> 32) - 1;
    }

    private static int second(long pair) {
        return (int) pair - 1;
    }

    /** Separates the first {@code rank} in-order entries without allocating nodes. */
    private long split(int coordinate, int rank) {
        if (coordinate == NONE) {
            return pair(NONE, NONE);
        }
        int leftCount = count(left[coordinate]);
        if (rank <= leftCount) {
            long halves = split(left[coordinate], rank);
            left[coordinate] = second(halves);
            refresh(coordinate);
            return pair(first(halves), coordinate);
        }
        long halves = split(right[coordinate], rank - leftCount - 1);
        right[coordinate] = first(halves);
        refresh(coordinate);
        return pair(coordinate, second(halves));
    }

    /** Selects one physical coordinate by the rank of its subtree. */
    private int coordinate(int index) {
        Objects.checkIndex(index, size);
        int slot = root;
        int offset = index;
        while (slot != NONE) {
            int leftCount = count(left[slot]);
            if (offset < leftCount) {
                slot = left[slot];
            } else if (offset == leftCount) {
                return slot;
            } else {
                offset -= leftCount + 1;
                slot = right[slot];
            }
        }
        throw new IllegalStateException("Inconsistent tree rank");
    }

    @SuppressWarnings("unchecked")
    private E elementAt(int coordinate) {
        return (E) elements[coordinate];
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
    public boolean add(E element) {
        add(size, element);
        return true;
    }

    @Override
    public void add(int index, E element) {
        Objects.checkIndex(index, size + 1);
        int slot = acquire(element);
        long halves = split(root, index);
        root = merge(merge(first(halves), slot), second(halves));
        size++;
        modCount++;
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
        if (snapshot.length > MAX_ARRAY_SIZE - size) {
            throw new OutOfMemoryError("Array-backed order-statistic tree too large");
        }
        int available = used - size;
        long additional = Math.max(0L, (long) snapshot.length - available);
        long required = used + additional;
        if (required > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("Array-backed order-statistic tree too large");
        }
        ensureCapacity((int) required);
        int inserted = NONE;
        for (Object value : snapshot) {
            @SuppressWarnings("unchecked") E element = (E) value;
            inserted = merge(inserted, acquire(element));
        }
        long halves = split(root, index);
        root = merge(merge(first(halves), inserted), second(halves));
        size += snapshot.length;
        modCount++;
        return true;
    }

    @Override
    public E remove(int index) {
        Objects.checkIndex(index, size);
        long prefixAndTail = split(root, index);
        long removedAndSuffix = split(second(prefixAndTail), 1);
        int removed = first(removedAndSuffix);
        E value = elementAt(removed);
        root = merge(first(prefixAndTail), second(removedAndSuffix));
        release(removed);
        size--;
        modCount++;
        return value;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(elements, 0, used, null);
        Arrays.fill(markMask, 0L);
        root = NONE;
        free = NONE;
        used = 0;
        size = 0;
        modCount++;
    }

    @Override
    public void addFirst(E element) {
        add(0, element);
    }

    @Override
    public void addLast(E element) {
        add(size, element);
    }

    @Override
    public E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(0);
    }

    @Override
    public E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(size - 1);
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
    public ListIterator<E> listIterator() {
        return size <= INITIAL_CAPACITY ? super.listIterator() : new TreeListIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return size <= INITIAL_CAPACITY ? super.listIterator(index) : new TreeListIterator(index);
    }

    @Override
    public Iterator<E> iterator() {
        return size <= INITIAL_CAPACITY ? super.iterator() : new TreeIterator(0);
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
     * A cursor over physical coordinates. The ancestor stack belongs to this
     * iterator, not to each element; it is allocated only on the first descent.
     */
    private class TreeIterator implements Iterator<E> {
        int cursor;
        int lastReturned = NONE;
        int expectedModCount = modCount;

        private int[] path = EMPTY_INTS;
        private int depth;
        private int current = NONE;
        private int pathIndex = NONE;

        TreeIterator(int index) {
            cursor = index;
        }

        @Override
        public boolean hasNext() {
            return cursor != size;
        }

        @Override
        public E next() {
            checkForComodification();
            try {
                int index = cursor;
                Objects.checkIndex(index, size);
                E value = elementAt(select(index));
                lastReturned = index;
                cursor = index + 1;
                return value;
            } catch (IndexOutOfBoundsException exception) {
                checkForComodification();
                throw new NoSuchElementException(exception);
            }
        }

        @Override
        public void remove() {
            if (lastReturned < 0) {
                throw new IllegalStateException();
            }
            checkForComodification();
            try {
                M3TreeList.this.remove(lastReturned);
                if (lastReturned < cursor) {
                    cursor--;
                }
                lastReturned = NONE;
                expectedModCount = modCount;
                invalidatePath();
            } catch (IndexOutOfBoundsException exception) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        final void invalidatePath() {
            pathIndex = NONE;
            current = NONE;
            depth = 0;
        }

        /** Selects an adjacent coordinate without another root-to-rank walk. */
        final int select(int index) {
            if (pathIndex == index) {
                return current;
            }
            int previousIndex = pathIndex;
            // A failed stack growth must leave the logical cursor retryable.
            pathIndex = NONE;
            if (previousIndex != NONE && index == previousIndex + 1) {
                successor();
            } else if (previousIndex != NONE && index == previousIndex - 1) {
                predecessor();
            } else {
                seek(index);
            }
            pathIndex = index;
            return current;
        }

        private void push(int coordinate) {
            if (depth == path.length) {
                long growth = path.length == 0 ? INITIAL_CAPACITY
                        : (long) path.length + (path.length >>> 1) + 1;
                int capacity = (int) Math.min(MAX_ARRAY_SIZE, growth);
                path = Arrays.copyOf(path, capacity);
            }
            path[depth++] = coordinate;
        }

        private void seek(int index) {
            depth = 0;
            int slot = root;
            int offset = index;
            while (slot != NONE) {
                int leftCount = count(left[slot]);
                if (offset == leftCount) {
                    current = slot;
                    return;
                }
                push(slot);
                if (offset < leftCount) {
                    slot = left[slot];
                } else {
                    offset -= leftCount + 1;
                    slot = right[slot];
                }
            }
            throw new IllegalStateException("Inconsistent tree rank");
        }

        private void successor() {
            int slot = right[current];
            if (slot != NONE) {
                push(current);
                while (left[slot] != NONE) {
                    push(slot);
                    slot = left[slot];
                }
                current = slot;
                return;
            }
            int child = current;
            while (depth != 0) {
                int parent = path[--depth];
                if (left[parent] == child) {
                    current = parent;
                    return;
                }
                child = parent;
            }
            throw new IllegalStateException("Inconsistent tree cursor");
        }

        private void predecessor() {
            int slot = left[current];
            if (slot != NONE) {
                push(current);
                while (right[slot] != NONE) {
                    push(slot);
                    slot = right[slot];
                }
                current = slot;
                return;
            }
            int child = current;
            while (depth != 0) {
                int parent = path[--depth];
                if (right[parent] == child) {
                    current = parent;
                    return;
                }
                child = parent;
            }
            throw new IllegalStateException("Inconsistent tree cursor");
        }
    }

    /** Bidirectional traversal shares the same physical path and invalidations. */
    private final class TreeListIterator extends TreeIterator implements ListIterator<E> {
        TreeListIterator(int index) {
            super(index);
        }

        @Override
        public boolean hasPrevious() {
            return cursor != 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            try {
                int index = cursor - 1;
                Objects.checkIndex(index, size);
                E value = elementAt(select(index));
                cursor = index;
                lastReturned = index;
                return value;
            } catch (IndexOutOfBoundsException exception) {
                checkForComodification();
                throw new NoSuchElementException(exception);
            }
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void set(E element) {
            if (lastReturned < 0) {
                throw new IllegalStateException();
            }
            checkForComodification();
            try {
                Objects.checkIndex(lastReturned, size);
                elements[select(lastReturned)] = element;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException exception) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(E element) {
            checkForComodification();
            try {
                int index = cursor;
                M3TreeList.this.add(index, element);
                lastReturned = NONE;
                cursor = index + 1;
                expectedModCount = modCount;
                invalidatePath();
            } catch (IndexOutOfBoundsException exception) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Serializes the logical sequence independently of the tree priorities.
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
     * Rebuilds the order-statistic index from serialized logical elements.
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
        left = right = weight = priority = EMPTY_INTS;
        markMask = EMPTY_WORDS;
        root = free = NONE;
        used = size = 0;
        sequence = 0L;
        int count = in.readInt();
        if (count < 0 || count > MAX_ARRAY_SIZE) {
            throw new InvalidObjectException("Invalid tree list length: " + count);
        }
        for (int i = 0; i < count; i++) {
            add((E) in.readObject());
        }
        modCount = 0;
    }
}
