/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import org.pcollections.AbstractUnmodifiableList;
import org.pcollections.PVector;

/**
 * An immutable persistent vector with balanced 32-slot array pages.
 *
 * <p>Leaves retain element references, including null. Branches retain pages and primitive
 * cumulative child sizes. An update copies its search path; disjoint pages remain shared with
 * older versions. Lookup, replacement, insertion, and indexed removal take logarithmic time
 * in the retained backing size, with bounded work per page.
 *
 * <p>A {@link #subList(int, int)} is an immutable {@code (root, offset, length)} view created
 * in constant time. It may retain elements outside its visible range. Materialize only the
 * visible sequence with {@code M3PVector.<E>empty().plusAll(view)} when that retention is
 * undesirable. Serialization always includes only the visible sequence.
 *
 * <p>Persistence protects collection structure, not the mutability of user elements. All
 * destructive Java collection operations throw {@link UnsupportedOperationException}.
 *
 * @param <E> element type
 */
public final class M3PVector<E> extends AbstractUnmodifiableList<E>
        implements PVector<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private static final int PAGE_BITS = 5;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int MIN_SLOTS = PAGE_SIZE >>> 1;
    private static final int COUNT_BITS = PAGE_BITS + 1;
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;
    private static final M3PVector<?> EMPTY = new M3PVector<>(null, 0, 0);

    private final transient Page root;
    private final int offset;
    private final int length;

    private M3PVector(Page root, int offset, int length) {
        this.root = root;
        this.offset = offset;
        this.length = length;
    }

    /** Returns the canonical empty vector. */
    @SuppressWarnings("unchecked")
    public static <E> M3PVector<E> empty() {
        return (M3PVector<E>) EMPTY;
    }

    /** Returns a vector containing one element, which may be null. */
    public static <E> M3PVector<E> singleton(E element) {
        return M3PVector.<E>empty().plus(element);
    }

    /**
     * Copies a collection in encounter order using a linear page builder.
     * An existing M3 vector can be reused, including its retained slice backing.
     *
     * @throws NullPointerException if {@code source} is null
     */
    @SuppressWarnings("unchecked")
    public static <E> M3PVector<E> from(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof M3PVector<?> vector) return (M3PVector<E>) vector;
        Builder builder = new Builder();
        for (E element : source) builder.add(element);
        Page page = builder.finish();
        return page == null ? empty() : new M3PVector<>(page, 0, page.size);
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        Objects.checkIndex(index, length);
        Page page = root;
        int position = offset + index;
        while (page.height() != 0) {
            int child = childIndex(page, position);
            position -= startOfChild(page, child);
            page = page.child(child);
        }
        return (E) page.slots[position];
    }

    @Override
    public M3PVector<E> plus(E element) {
        return plus(length, element);
    }

    @Override
    public M3PVector<E> plus(int index, E element) {
        checkPosition(index);
        if (root == null) {
            Object[] slots = new Object[PAGE_SIZE];
            slots[0] = element;
            return new M3PVector<>(new Page(0, slots, 1), 0, 1);
        }
        if (root.size == Integer.MAX_VALUE) throw capacityExceeded();
        Split inserted = insert(root, offset + index, element);
        Page next = inserted.left;
        if (inserted.right != null) {
            Object[] slots = new Object[PAGE_SIZE];
            slots[0] = inserted.left;
            slots[1] = inserted.right;
            next = new Page(root.height() + 1, slots, 2);
        }
        return new M3PVector<>(next, offset, length + 1);
    }

    @Override
    public M3PVector<E> plusAll(Collection<? extends E> source) {
        return plusAll(length, source);
    }

    /**
     * Inserts the input encounter sequence at {@code index}. Source iteration failures
     * propagate; every previously returned vector remains unchanged.
     */
    @Override
    public M3PVector<E> plusAll(int index, Collection<? extends E> source) {
        checkPosition(index);
        Objects.requireNonNull(source, "source");
        M3PVector<E> result = this;
        int position = index;
        for (E element : source) result = result.plus(position++, element);
        return result;
    }

    /** Replaces the element reference; only identical-reference replacement is a no-op. */
    @Override
    public M3PVector<E> with(int index, E element) {
        Objects.checkIndex(index, length);
        if (get(index) == element) return this;
        return new M3PVector<>(replace(root, offset + index, element), offset, length);
    }

    @Override
    public M3PVector<E> minus(Object element) {
        int index = indexOf(element);
        return index < 0 ? this : minus(index);
    }

    @Override
    public M3PVector<E> minus(int index) {
        Objects.checkIndex(index, length);
        if (length == 1) return empty();
        Page next = remove(root, offset + index);
        while (next.height() != 0 && next.count() == 1) next = next.child(0);
        return new M3PVector<>(next, offset, length - 1);
    }

    /**
     * Removes every visible occurrence accepted by {@code members.contains(element)}.
     * Membership is evaluated once per original element in encounter order. It is not
     * replaced with hashing, so custom collection equality and callback failures retain
     * their meaning. This follows the PCollection interface's remove-all contract.
     */
    @Override
    public M3PVector<E> minusAll(Collection<?> members) {
        Objects.requireNonNull(members, "members");
        M3PVector<E> result = this;
        int position = 0;
        for (E element : this) {
            if (members.contains(element)) result = result.minus(position);
            else position++;
        }
        return result;
    }

    /**
     * Returns an immutable constant-time range view, retaining the backing root.
     *
     * @throws IndexOutOfBoundsException for negative or out-of-size endpoints
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     */
    @Override
    public M3PVector<E> subList(int fromIndex, int toIndex) {
        checkPosition(fromIndex);
        checkPosition(toIndex);
        if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex > toIndex");
        if (fromIndex == 0 && toIndex == length) return this;
        if (fromIndex == toIndex) return empty();
        return new M3PVector<>(root, offset + fromIndex, toIndex - fromIndex);
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        checkPosition(index);
        return new VectorIterator<>(this, index);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(iterator(), length,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /** Returns a read-only reverse-order snapshot view. */
    @Override
    public List<E> reversed() {
        return new ReversedList<>(this);
    }

    /** @deprecated Persistent vectors do not support destructive insertion. */
    @Deprecated
    @Override
    public void addFirst(E element) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Persistent vectors do not support destructive insertion. */
    @Deprecated
    @Override
    public void addLast(E element) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Persistent vectors do not support destructive removal, even when empty. */
    @Deprecated
    @Override
    public E removeFirst() {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Persistent vectors do not support destructive removal, even when empty. */
    @Deprecated
    @Override
    public E removeLast() {
        throw new UnsupportedOperationException();
    }

    private void checkPosition(int index) {
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + length);
        }
    }

    private static OutOfMemoryError capacityExceeded() {
        return new OutOfMemoryError("Persistent vector capacity exceeds Integer.MAX_VALUE");
    }

    private static int startOfChild(Page page, int child) {
        return child == 0 ? 0 : page.prefix[child - 1];
    }

    private static int childIndex(Page page, int index) {
        int low = 0;
        int high = page.count() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (index < page.prefix[middle]) high = middle;
            else low = middle + 1;
        }
        return low;
    }

    private static Page replace(Page page, int index, Object element) {
        Object[] slots = page.slots.clone();
        if (page.height() == 0) {
            slots[index] = element;
        } else {
            int child = childIndex(page, index);
            slots[child] = replace(page.child(child), index - startOfChild(page, child), element);
        }
        return new Page(page.height(), slots, page.count());
    }

    private static Split insert(Page page, int index, Object element) {
        int count = page.count();
        if (page.height() == 0) {
            Object[] slots = new Object[count == PAGE_SIZE ? PAGE_SIZE + 1 : PAGE_SIZE];
            System.arraycopy(page.slots, 0, slots, 0, index);
            slots[index] = element;
            System.arraycopy(page.slots, index, slots, index + 1, count - index);
            return split(page.height(), slots, count + 1);
        }
        int child = childIndex(page, index);
        Split inserted = insert(page.child(child), index - startOfChild(page, child), element);
        int extra = inserted.right == null ? 0 : 1;
        int nextCount = count + extra;
        Object[] slots = new Object[nextCount > PAGE_SIZE ? PAGE_SIZE + 1 : PAGE_SIZE];
        System.arraycopy(page.slots, 0, slots, 0, child);
        slots[child] = inserted.left;
        if (extra != 0) slots[child + 1] = inserted.right;
        System.arraycopy(page.slots, child + 1, slots, child + 1 + extra, count - child - 1);
        return split(page.height(), slots, nextCount);
    }

    private static Split split(int height, Object[] slots, int count) {
        if (count <= PAGE_SIZE) return new Split(new Page(height, slots, count), null);
        Object[] left = new Object[PAGE_SIZE];
        Object[] right = new Object[PAGE_SIZE];
        System.arraycopy(slots, 0, left, 0, MIN_SLOTS);
        System.arraycopy(slots, MIN_SLOTS, right, 0, count - MIN_SLOTS);
        return new Split(new Page(height, left, MIN_SLOTS),
                new Page(height, right, count - MIN_SLOTS));
    }

    private static Page remove(Page page, int index) {
        if (page.height() == 0) return removeSlot(page, index);
        int child = childIndex(page, index);
        Page reduced = remove(page.child(child), index - startOfChild(page, child));
        Object[] slots = page.slots.clone();
        slots[child] = reduced;
        int count = page.count();
        if (reduced.count() < MIN_SLOTS) {
            Page left = child == 0 ? null : (Page) slots[child - 1];
            Page right = child + 1 == count ? null : (Page) slots[child + 1];
            if (left != null && left.count() > MIN_SLOTS) {
                slots[child - 1] = removeSlot(left, left.count() - 1);
                slots[child] = insertSlot(reduced, 0, left.slots[left.count() - 1]);
            } else if (right != null && right.count() > MIN_SLOTS) {
                slots[child] = insertSlot(reduced, reduced.count(), right.slots[0]);
                slots[child + 1] = removeSlot(right, 0);
            } else if (left != null) {
                slots[child - 1] = merge(left, reduced);
                System.arraycopy(slots, child + 1, slots, child, count - child - 1);
                slots[--count] = null;
            } else if (right != null) {
                slots[child] = merge(reduced, right);
                System.arraycopy(slots, child + 2, slots, child + 1, count - child - 2);
                slots[--count] = null;
            }
        }
        return new Page(page.height(), slots, count);
    }

    private static Page removeSlot(Page page, int index) {
        int count = page.count();
        Object[] slots = new Object[PAGE_SIZE];
        System.arraycopy(page.slots, 0, slots, 0, index);
        System.arraycopy(page.slots, index + 1, slots, index, count - index - 1);
        return new Page(page.height(), slots, count - 1);
    }

    private static Page insertSlot(Page page, int index, Object value) {
        int count = page.count();
        Object[] slots = new Object[PAGE_SIZE];
        System.arraycopy(page.slots, 0, slots, 0, index);
        slots[index] = value;
        System.arraycopy(page.slots, index, slots, index + 1, count - index);
        return new Page(page.height(), slots, count + 1);
    }

    private static Page merge(Page left, Page right) {
        Object[] slots = new Object[PAGE_SIZE];
        System.arraycopy(left.slots, 0, slots, 0, left.count());
        System.arraycopy(right.slots, 0, slots, left.count(), right.count());
        return new Page(left.height(), slots, left.count() + right.count());
    }

    /** The single retained structural object represents a whole leaf or branch page. */
    private static final class Page {
        private final Object[] slots;
        private final int[] prefix;
        private final int state;
        private final int size;

        private Page(int height, Object[] slots, int count) {
            this.slots = slots;
            this.state = (height << COUNT_BITS) | count;
            if (height == 0) {
                this.prefix = null;
                this.size = count;
            } else {
                this.prefix = new int[PAGE_SIZE];
                int total = 0;
                for (int child = 0; child < count; child++) {
                    total = Math.addExact(total, ((Page) slots[child]).size);
                    prefix[child] = total;
                }
                this.size = total;
            }
        }

        private int count() { return state & COUNT_MASK; }
        private int height() { return state >>> COUNT_BITS; }
        private Page child(int index) { return (Page) slots[index]; }
    }

    /** Transient recursion result; never retained as per-element collection storage. */
    private static final class Split {
        private final Page left;
        private final Page right;

        private Split(Page left, Page right) {
            this.left = left;
            this.right = right;
        }
    }

    /** Linear ingress builder. Its mutable arrays never escape into an older vector. */
    private static final class Builder {
        private Page[] leaves = new Page[4];
        private int leafCount;
        private Object[] pending = new Object[PAGE_SIZE];
        private int pendingCount;
        private int size;

        private void add(Object value) {
            if (size == Integer.MAX_VALUE) throw capacityExceeded();
            pending[pendingCount++] = value;
            size++;
            if (pendingCount == PAGE_SIZE) {
                appendPage(new Page(0, pending, pendingCount));
                pending = new Object[PAGE_SIZE];
                pendingCount = 0;
            }
        }

        private void appendPage(Page page) {
            if (leafCount == leaves.length) leaves = Arrays.copyOf(leaves, leaves.length << 1);
            leaves[leafCount++] = page;
        }

        private Page finish() {
            if (pendingCount != 0) {
                if (pendingCount < MIN_SLOTS && leafCount != 0) {
                    Page previous = leaves[--leafCount];
                    int leftCount = (previous.count() + pendingCount) >>> 1;
                    Object[] left = new Object[PAGE_SIZE];
                    Object[] right = new Object[PAGE_SIZE];
                    System.arraycopy(previous.slots, 0, left, 0, leftCount);
                    int previousRemainder = previous.count() - leftCount;
                    System.arraycopy(previous.slots, leftCount, right, 0, previousRemainder);
                    System.arraycopy(pending, 0, right, previousRemainder, pendingCount);
                    appendPage(new Page(0, left, leftCount));
                    appendPage(new Page(0, right, previousRemainder + pendingCount));
                } else {
                    appendPage(new Page(0, pending, pendingCount));
                }
            }
            if (leafCount == 0) return null;
            Page[] level = leaves;
            int count = leafCount;
            int height = 1;
            while (count > 1) {
                int groups = (count + PAGE_SIZE - 1) >>> PAGE_BITS;
                Page[] next = new Page[groups];
                int groupSize = count / groups;
                int extra = count % groups;
                int cursor = 0;
                for (int group = 0; group < groups; group++) {
                    int used = groupSize + (group < extra ? 1 : 0);
                    Object[] slots = new Object[PAGE_SIZE];
                    System.arraycopy(level, cursor, slots, 0, used);
                    next[group] = new Page(height, slots, used);
                    cursor += used;
                }
                level = next;
                count = groups;
                height++;
            }
            return level[0];
        }
    }

    /** Cursor path is iterator-local; traversal does not allocate an object per element. */
    private static final class Cursor {
        private final Page root;
        private final Page[] parents;
        private final int[] positions;
        private Page leaf;
        private int leafStart;

        private Cursor(Page root) {
            this.root = root;
            int height = root == null ? 0 : root.height();
            this.parents = new Page[height];
            this.positions = new int[height];
        }

        private Object at(int absoluteIndex) {
            if (leaf == null) {
                seek(absoluteIndex);
            } else if (absoluteIndex == leafStart + leaf.count()) {
                nextLeaf();
            } else if (absoluteIndex == leafStart - 1) {
                previousLeaf();
            } else if (absoluteIndex < leafStart || absoluteIndex >= leafStart + leaf.count()) {
                seek(absoluteIndex);
            }
            return leaf.slots[absoluteIndex - leafStart];
        }

        private void seek(int absoluteIndex) {
            Page page = root;
            int localIndex = absoluteIndex;
            int start = 0;
            int depth = 0;
            while (page.height() != 0) {
                int child = childIndex(page, localIndex);
                parents[depth] = page;
                positions[depth++] = child;
                int childStart = startOfChild(page, child);
                localIndex -= childStart;
                start += childStart;
                page = page.child(child);
            }
            leaf = page;
            leafStart = start;
        }

        private void nextLeaf() {
            for (int depth = parents.length - 1; depth >= 0; depth--) {
                Page parent = parents[depth];
                int next = positions[depth] + 1;
                if (next < parent.count()) {
                    positions[depth] = next;
                    Page page = parent.child(next);
                    int lower = depth + 1;
                    while (page.height() != 0) {
                        parents[lower] = page;
                        positions[lower++] = 0;
                        page = page.child(0);
                    }
                    leafStart += leaf.count();
                    leaf = page;
                    return;
                }
            }
            throw new NoSuchElementException();
        }

        private void previousLeaf() {
            for (int depth = parents.length - 1; depth >= 0; depth--) {
                Page parent = parents[depth];
                int previous = positions[depth] - 1;
                if (previous >= 0) {
                    positions[depth] = previous;
                    Page page = parent.child(previous);
                    int lower = depth + 1;
                    while (page.height() != 0) {
                        int child = page.count() - 1;
                        parents[lower] = page;
                        positions[lower++] = child;
                        page = page.child(child);
                    }
                    leafStart -= page.count();
                    leaf = page;
                    return;
                }
            }
            throw new NoSuchElementException();
        }
    }

    private static final class VectorIterator<E> implements ListIterator<E> {
        private final M3PVector<E> vector;
        private final Cursor cursor;
        private final boolean reverse;
        private int next;

        private VectorIterator(M3PVector<E> vector, int index) {
            this(vector, index, false);
        }

        private VectorIterator(M3PVector<E> vector, int index, boolean reverse) {
            this.vector = vector;
            this.cursor = new Cursor(vector.root);
            this.reverse = reverse;
            this.next = index;
        }

        @Override public boolean hasNext() { return next < vector.length; }
        @Override public boolean hasPrevious() { return next > 0; }
        @Override public int nextIndex() { return next; }
        @Override public int previousIndex() { return next - 1; }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (!hasNext()) throw new NoSuchElementException();
            int index = next++;
            return (E) cursor.at(vector.offset + (reverse ? vector.length - 1 - index : index));
        }

        @Override
        @SuppressWarnings("unchecked")
        public E previous() {
            if (!hasPrevious()) throw new NoSuchElementException();
            int index = --next;
            return (E) cursor.at(vector.offset + (reverse ? vector.length - 1 - index : index));
        }

        @Override public void remove() { throw new UnsupportedOperationException(); }
        @Override public void set(E value) { throw new UnsupportedOperationException(); }
        @Override public void add(E value) { throw new UnsupportedOperationException(); }
    }

    /** Uses the same page cursor in reverse; Java 21 destructive defaults remain strict. */
    private static final class ReversedList<E> extends AbstractUnmodifiableList<E> {
        private final M3PVector<E> vector;

        private ReversedList(M3PVector<E> vector) {
            this.vector = vector;
        }

        @Override public int size() { return vector.length; }

        @Override
        public E get(int index) {
            Objects.checkIndex(index, vector.length);
            return vector.get(vector.length - 1 - index);
        }

        @Override public Iterator<E> iterator() { return listIterator(0); }

        @Override
        public ListIterator<E> listIterator(int index) {
            vector.checkPosition(index);
            return new VectorIterator<>(vector, index, true);
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(iterator(), size(),
                    Spliterator.ORDERED | Spliterator.IMMUTABLE);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            vector.checkPosition(fromIndex);
            vector.checkPosition(toIndex);
            if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex > toIndex");
            return vector.subList(size() - toIndex, size() - fromIndex).reversed();
        }

        @Override public List<E> reversed() { return vector; }
        @Deprecated @Override public void addFirst(E element) { throw new UnsupportedOperationException(); }
        @Deprecated @Override public void addLast(E element) { throw new UnsupportedOperationException(); }
        @Deprecated @Override public E removeFirst() { throw new UnsupportedOperationException(); }
        @Deprecated @Override public E removeLast() { throw new UnsupportedOperationException(); }
    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy(toArray());
    }

    @Serial
    private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // ObjectOutputStream checks the serializability of each visible element.
        @SuppressWarnings("serial")
        private final Object[] elements;

        private SerializationProxy(Object[] elements) {
            this.elements = elements;
        }

        @Serial
        private Object readResolve() throws InvalidObjectException {
            if (elements == null) throw new InvalidObjectException("Missing vector elements");
            Builder builder = new Builder();
            for (Object element : elements) builder.add(element);
            Page page = builder.finish();
            return page == null ? M3PVector.empty() : new M3PVector<>(page, 0, page.size);
        }
    }

    // Proof-only package surface. These methods never expose mutable storage or affect updates.

    void verifyInvariants() {
        if (root == null) {
            requireInvariant(offset == 0 && length == 0, "empty vector state");
            return;
        }
        requireInvariant(offset >= 0 && length > 0 && (long) offset + length <= root.size,
                "visible range");
        verifyPage(root, true);
    }

    boolean sharesRoot(M3PVector<?> other) {
        return root == Objects.requireNonNull(other, "other").root;
    }

    int sharedPageCount(M3PVector<?> other) {
        Objects.requireNonNull(other, "other");
        IdentityHashMap<Page, Boolean> pages = new IdentityHashMap<>();
        collectPages(root, pages);
        return countShared(other.root, pages);
    }

    int pageCount() { return countPages(root); }
    int treeHeight() { return root == null ? 0 : root.height() + 1; }
    int retainedSize() { return root == null ? 0 : root.size; }

    private static void verifyPage(Page page, boolean isRoot) {
        int count = page.count();
        requireInvariant(page.slots.length == PAGE_SIZE, "page capacity");
        requireInvariant(count <= PAGE_SIZE && count >= (isRoot ? 1 : MIN_SLOTS), "page occupancy");
        for (int index = count; index < PAGE_SIZE; index++) {
            requireInvariant(page.slots[index] == null, "unused object lane");
        }
        if (page.height() == 0) {
            requireInvariant(page.prefix == null && page.size == count, "leaf metadata");
            return;
        }
        requireInvariant(!isRoot || count >= 2, "collapsed root");
        requireInvariant(page.prefix != null && page.prefix.length == PAGE_SIZE, "branch size lanes");
        long total = 0;
        for (int index = 0; index < count; index++) {
            requireInvariant(page.slots[index] instanceof Page, "branch page reference");
            Page child = page.child(index);
            requireInvariant(child.height() + 1 == page.height(), "balanced page height");
            verifyPage(child, false);
            total += child.size;
            requireInvariant(total <= Integer.MAX_VALUE && page.prefix[index] == total, "prefix offset");
        }
        requireInvariant(total == page.size, "subtree size");
        for (int index = count; index < PAGE_SIZE; index++) {
            requireInvariant(page.prefix[index] == 0, "unused size lane");
        }
    }

    private static void requireInvariant(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("M3PVector invariant: " + message);
    }

    private static int countPages(Page page) {
        if (page == null) return 0;
        int result = 1;
        if (page.height() != 0) {
            for (int child = 0; child < page.count(); child++) result += countPages(page.child(child));
        }
        return result;
    }

    private static void collectPages(Page page, IdentityHashMap<Page, Boolean> pages) {
        if (page == null || pages.put(page, Boolean.TRUE) != null) return;
        if (page.height() != 0) {
            for (int child = 0; child < page.count(); child++) collectPages(page.child(child), pages);
        }
    }

    private static int countShared(Page page, IdentityHashMap<Page, Boolean> pages) {
        if (page == null) return 0;
        int result = pages.remove(page) == null ? 0 : 1;
        if (page.height() != 0) {
            for (int child = 0; child < page.count(); child++) result += countShared(page.child(child), pages);
        }
        return result;
    }
}
