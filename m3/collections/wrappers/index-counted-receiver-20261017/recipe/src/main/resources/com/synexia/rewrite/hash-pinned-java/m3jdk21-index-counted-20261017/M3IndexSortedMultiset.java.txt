// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Comparator-ordered occurrence collection over canonical IDs and primitive counts.
 *
 * <p>Reuses the existing ordered ID topology and range owner: its second int lane
 * holds positive occurrence counts, not value-domain IDs. No boxed count, retained
 * entry/node or per-occurrence storage is added. All views share those same lanes.
 * Counts are at most Integer.MAX_VALUE per comparator-equivalent element; longSize
 * reports the exact total, while Collection.size saturates. Lookups/count updates
 * cost O(log d), new-key insertion/removal O(d), and total/rank/select O(d), where
 * d is distinct cardinality and comparator/domain calls are assumed O(1).
 *
 * <p>Unlike the numeric-identity-ordered MIndexFrozenBag, this is a mutable
 * comparator-ordered Collection facade. It is not a Guava binary adapter and has
 * value equality only with other M3IndexSortedMultiset instances. Stable comparator
 * order consistent with equals and stable exact-reference domain projection are
 * required. Equal occurrences retain the first admitted representative; null uses
 * ID -1 and requires a null-accepting comparator. Queries never admit identities.
 *
 * <p>Occurrence iterators fail fast on count and structural edits. Iterator.remove
 * removes one occurrence; elementSet and entrySet removal remove whole groups.
 * Entry snapshots are allocated only at explicit API boundaries. Bulk/predicate
 * callbacks run per occurrence, with completed effects retained on failure. Domains
 * own admitted references after removal; their admissions are not rolled back.
 * Not concurrent, persistent, weak, blocking or serializable. Heap growth uses the
 * existing advisory admission owner; paging/native residency and OOME prevention
 * are not provided by this facade.
 */
public final class M3IndexSortedMultiset<E> extends AbstractCollection<E> {
    private final M3IndexRange<E> range;

    /** Immutable count snapshot. The live collection retains no Entry objects. */
    public record Entry<E>(E element, int count) {
        public Entry {
            if (count <= 0) throw new IllegalArgumentException("entry count must be positive");
        }
        @Override public int hashCode() { return Objects.hashCode(element) ^ count; }
    }

    @FunctionalInterface public interface IdCountConsumer { void accept(int id, int count); }

    public M3IndexSortedMultiset(MIndexSpace<E> space, Comparator<? super E> comparator) {
        this(new M3IndexRange<>(new M3IndexOrderedStore<>(space, comparator, true)));
    }
    public M3IndexSortedMultiset(MIndexSpace<E> space) { this(space, null); }
    private M3IndexSortedMultiset(M3IndexRange<E> range) { this.range = range; }

    /** Package-local mutation stamp for derived, count-aware filtered cursors. */
    long mutationEpoch() { return range.store.mutation; }
    public MIndexSpace<E> space() { return range.store.space; }
    /** Null denotes natural order, following the existing M3 sorted facades. */
    public Comparator<? super E> comparator() { return range.comparator(); }
    public int distinctSize() { return range.size(); }
    @Override public boolean isEmpty() { return range.first() < 0; }
    public long longSize() { return sum(range.start(), range.end()); }
    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, longSize()); }

    private long sum(int start, int end) {
        // Equal excluded endpoints can have start > end; the logical view is empty.
        return M3IndexCountKernel.sum(range.store.values, start, Math.max(start, end));
    }
    public int count(Object element) {
        int index = range.find(cast(element));
        return index < 0 ? 0 : range.store.values[index];
    }
    @Override public boolean contains(Object element) { return count(element) != 0; }
    @Override public boolean add(E element) { add(element, 1); return true; }

    /** Return previous count; zero occurrences perform a non-admitting query. */
    public int add(E element, int occurrences) {
        requireCount(occurrences);
        if (occurrences == 0) return count(element);
        range.requireKey(element);
        M3IndexOrderedStore<E> store = range.store;
        int index = store.bound(element, false);
        if (index < store.size && store.compare(store.key(index), element) == 0) {
            int previous = store.values[index];
            if (occurrences > Integer.MAX_VALUE - previous)
                throw new IllegalArgumentException("element count overflow");
            store.replaceValue(index, previous + occurrences);
            return previous;
        }
        int id = store.admit(store.space, element);
        store.insert(index, id, occurrences);
        return 0;
    }
    @Override public boolean remove(Object element) { return remove(element, 1) != 0; }
    /** Return previous count, removing at most the requested occurrences. */
    public int remove(Object element, int occurrences) {
        requireCount(occurrences);
        int index = range.find(cast(element));
        if (index < 0) return 0;
        int previous = range.store.values[index];
        if (occurrences >= previous) range.store.remove(index);
        else if (occurrences != 0) range.store.replaceValue(index, previous - occurrences);
        return previous;
    }
    public int setCount(E element, int count) {
        requireCount(count);
        if (count == 0) return remove(element, Integer.MAX_VALUE);
        range.requireKey(element);
        M3IndexOrderedStore<E> store = range.store;
        int index = store.bound(element, false);
        if (index < store.size && store.compare(store.key(index), element) == 0) {
            int previous = store.values[index];
            if (previous != count) store.replaceValue(index, count);
            return previous;
        }
        int id = store.admit(store.space, element);
        store.insert(index, id, count);
        return 0;
    }
    /** Conditional count update is single-threaded, not an atomic concurrency API. */
    public boolean setCount(E element, int expected, int count) {
        requireCount(expected); requireCount(count);
        if (count(element) != expected) return false;
        setCount(element, count); return true;
    }
    private static void requireCount(int count) {
        if (count < 0) throw new IllegalArgumentException("negative occurrence count");
    }
    @Override public void clear() { range.clear(); }

    public M3IndexSortedMultiset<E> headMultiset(E to, boolean inclusive) {
        return new M3IndexSortedMultiset<>(range.head(to, inclusive));
    }
    public M3IndexSortedMultiset<E> tailMultiset(E from, boolean inclusive) {
        return new M3IndexSortedMultiset<>(range.tail(from, inclusive));
    }
    public M3IndexSortedMultiset<E> subMultiset(E from, boolean fi, E to, boolean ti) {
        return new M3IndexSortedMultiset<>(range.sub(from, fi, to, ti));
    }
    public M3IndexSortedMultiset<E> descendingMultiset() {
        return new M3IndexSortedMultiset<>(range.reverse());
    }
    public M3IndexNavigableSet<E> elementSet() { return new M3IndexNavigableSet<>(range, false); }
    private Entry<E> snapshot(int index) {
        return index < 0 ? null : new Entry<>(range.store.key(index), range.store.values[index]);
    }
    public Entry<E> firstEntry() { return snapshot(range.first()); }
    public Entry<E> lastEntry() { return snapshot(range.last()); }
    private Entry<E> poll(int index) {
        Entry<E> entry = snapshot(index);
        if (index >= 0) range.store.remove(index);
        return entry;
    }
    public Entry<E> pollFirstEntry() { return poll(range.first()); }
    public Entry<E> pollLastEntry() { return poll(range.last()); }
    public Set<Entry<E>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return distinctSize(); }
            @Override public Iterator<Entry<E>> iterator() { return range.iterator(M3IndexSortedMultiset.this::snapshot); }
            @Override public void clear() { M3IndexSortedMultiset.this.clear(); }
            @Override public boolean contains(Object other) {
                return other instanceof Entry<?> entry && count(entry.element()) == entry.count();
            }
            @Override public boolean remove(Object other) {
                if (!(other instanceof Entry<?> entry)) return false;
                int index = range.find(cast(entry.element()));
                if (index < 0 || range.store.values[index] != entry.count()) return false;
                range.store.remove(index); return true;
            }
            @Override public Spliterator<Entry<E>> spliterator() {
                return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.DISTINCT);
            }
        };
    }

    @Override public Iterator<E> iterator() {
        int start = range.start(), end = range.end();
        return new Iterator<>() {
            private int cursor = range.descending ? end - 1 : start;
            private int rows = Math.max(0, end - start);
            private int occurrences;
            private int last = -1;
            private long expected = range.store.mutation;
            @Override public boolean hasNext() {
                range.store.checkMutation(expected); return occurrences != 0 || rows != 0;
            }
            @Override public E next() {
                range.store.checkMutation(expected);
                if (occurrences == 0) {
                    if (rows == 0) throw new NoSuchElementException();
                    occurrences = range.store.values[cursor]; rows--;
                }
                E result = range.store.key(cursor);
                range.store.checkMutation(expected);
                last = cursor;
                if (--occurrences == 0) cursor += range.descending ? -1 : 1;
                return result;
            }
            @Override public void remove() {
                range.store.checkMutation(expected);
                if (last < 0) throw new IllegalStateException();
                int count = range.store.values[last];
                if (count == 1) {
                    range.store.remove(last);
                    if (!range.descending) cursor--;
                } else range.store.replaceValue(last, count - 1);
                expected = range.store.mutation; last = -1;
            }
        };
    }

    /** Stateful predicates observe each occurrence, not just distinct elements. */
    @Override public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter, "filter");
        boolean changed = false;
        for (Iterator<E> iterator = iterator(); iterator.hasNext();) {
            E value = iterator.next(); long expected = range.store.mutation;
            boolean remove = filter.test(value); range.store.checkMutation(expected);
            if (remove) { iterator.remove(); changed = true; }
        }
        return changed;
    }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        for (Iterator<E> iterator = iterator(); iterator.hasNext();) {
            E value = iterator.next(); long expected = range.store.mutation;
            action.accept(value); range.store.checkMutation(expected);
        }
    }
    public void forEachIdCount(IdCountConsumer action) {
        Objects.requireNonNull(action, "action");
        int start = range.start(), end = range.end(); long expected = range.store.mutation;
        for (int i = range.descending ? end - 1 : start;
                i >= start && i < end; i += range.descending ? -1 : 1) {
            action.accept(range.store.keys[i], range.store.values[i]);
            range.store.checkMutation(expected);
        }
    }
    /** Occurrences strictly before element in view order, or including its group. */
    public long rankOf(E element, boolean inclusive) {
        int start = range.start(), end = range.end();
        int bound = range.store.bound(element, range.descending ? !inclusive : inclusive);
        bound = Math.max(start, Math.min(end, bound));
        return range.descending ? sum(bound, end) : sum(start, bound);
    }
    /** Zero-based occurrence selection without expanding the count lane. */
    public E elementAtOccurrence(long ordinal) {
        if (ordinal < 0) throw new IndexOutOfBoundsException("negative occurrence");
        int start = range.start(), end = Math.max(start, range.end());
        long expected = range.store.mutation;
        long cursor = M3IndexCountKernel.locate(range.store.values, start, end, ordinal, range.descending);
        range.store.checkMutation(expected);
        return range.store.key((int) (cursor >>> 32));
    }

    /** Early-bound primitive coordinate traversal; splitting never projects or copies occurrences. */
    @Override public Spliterator<E> spliterator() {
        int start = range.start(), end = Math.max(start, range.end());
        long expected = range.store.mutation;
        long total = sum(start, end);
        range.store.checkMutation(expected);
        return new CountedSpliterator(start, end, range.descending ? end - 1 : start, 0, total, expected);
    }

    /** One cursor per split: row and intra-row occurrence offset, not an expanded value batch. */
    private final class CountedSpliterator implements Spliterator<E> {
        private final int start;
        private final int end;
        private final long expected;
        private int row;
        private int offset;
        private long remaining;

        CountedSpliterator(int start, int end, int row, int offset, long remaining, long expected) {
            this.start = start; this.end = end; this.row = row; this.offset = offset;
            this.remaining = remaining; this.expected = expected;
        }

        @Override public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action, "action");
            range.store.checkMutation(expected);
            if (remaining == 0) return false;
            E element = range.store.key(row);
            range.store.checkMutation(expected);
            if (++offset == range.store.values[row]) { row += range.descending ? -1 : 1; offset = 0; }
            remaining--;
            // Delivery consumes exactly this occurrence even when the client action throws.
            action.accept(element);
            range.store.checkMutation(expected);
            return true;
        }

        @Override public Spliterator<E> trySplit() {
            range.store.checkMutation(expected);
            if (remaining < 2) return null;
            long half = remaining >>> 1;
            long available = (long) range.store.values[row] - offset;
            int nextRow;
            int nextOffset;
            if (half < available) {
                nextRow = row; nextOffset = offset + (int) half;
            } else if (half == available) {
                nextRow = row + (range.descending ? -1 : 1); nextOffset = 0;
            } else {
                long cursor = range.descending
                        ? M3IndexCountKernel.locate(range.store.values, start, row, half - available, true)
                        : M3IndexCountKernel.locate(range.store.values, row + 1, end, half - available, false);
                nextRow = (int) (cursor >>> 32); nextOffset = (int) cursor;
            }
            range.store.checkMutation(expected);
            // Allocate the child before publishing the original cursor's new position.
            CountedSpliterator prefix = new CountedSpliterator(start, end, row, offset, half, expected);
            row = nextRow; offset = nextOffset; remaining -= half;
            return prefix;
        }

        @Override public long estimateSize() { range.store.checkMutation(expected); return remaining; }
        @Override public int characteristics() { return ORDERED | SORTED | SIZED | SUBSIZED; }
        @Override public Comparator<? super E> getComparator() { return comparator(); }
    }
    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof M3IndexSortedMultiset<?> bag)) return false;
        long expected = range.store.mutation;
        if (longSize() != bag.longSize() || distinctSize() != bag.distinctSize()) return false;
        int start = range.start(), end = range.end();
        try {
            for (int i = start; i < end; i++) {
                boolean equal = bag.count(range.store.key(i)) == range.store.values[i];
                range.store.checkMutation(expected); if (!equal) return false;
            }
        } catch (ClassCastException | NullPointerException incompatible) { return false; }
        return true;
    }
    @Override public int hashCode() {
        int hash = 0, start = range.start(), end = range.end(); long expected = range.store.mutation;
        for (int i = start; i < end; i++) {
            int elementHash = Objects.hashCode(range.store.key(i)); range.store.checkMutation(expected);
            hash += elementHash ^ range.store.values[i];
        }
        return hash;
    }
    @Override public String toString() { return entrySet().toString(); }
    @SuppressWarnings("unchecked") private E cast(Object element) { return (E) element; }
}
