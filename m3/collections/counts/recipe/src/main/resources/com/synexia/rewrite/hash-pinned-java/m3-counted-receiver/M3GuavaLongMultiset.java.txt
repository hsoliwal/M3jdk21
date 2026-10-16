/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.m3.collections.M3LongBag;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * Guava's mutable Multiset contract for non-null Long keys over M3 primitive count
 * storage. Retains no boxed key/count or entry per distinct value. Boxing and entry
 * projections occur only at the caller-facing interface. It is not a HashMultiset
 * concrete-class, serialization, null-key, or concurrency substitute.
 *
 * <p>Each key's count is bounded by Integer.MAX_VALUE. size() saturates at that
 * value; internal aggregate size is checked long. add/remove return OLD counts,
 * unlike M3LongBag. Iterators retain a primitive snapshot of distinct keys (not
 * occurrences), are best-effort fail-fast, and support removal through the live owner.
 * Entry projections are immutable snapshots. Views themselves stay backed.
 */
public final class M3GuavaLongMultiset extends AbstractCollection<Long> implements Multiset<Long> {
    private final M3LongBag counts = new M3LongBag();
    private long version;
    private Set<Long> elements;
    private Set<Entry<Long>> entries;

    public M3GuavaLongMultiset() { }
    public static M3GuavaLongMultiset create() { return new M3GuavaLongMultiset(); }
    public static M3GuavaLongMultiset create(Iterable<? extends Long> values) {
        Objects.requireNonNull(values, "values");
        M3GuavaLongMultiset result = create();
        for (Long value : values) result.add(value);
        return result;
    }

    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, counts.size()); }
    @Override public boolean isEmpty() { return counts.isEmpty(); }
    @Override public int count(Object value) {
        return value instanceof Long key ? (int) counts.count(key.longValue()) : 0;
    }
    @Override public boolean contains(Object value) { return count(value) != 0; }
    @Override public boolean add(Long value) { add(value, 1); return true; }
    @Override public int add(Long value, int occurrences) {
        nonnegative(occurrences);
        long key = Objects.requireNonNull(value, "value").longValue();
        int previous = (int) counts.count(key);
        if ((long) previous + occurrences > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("per-key count exceeds Integer.MAX_VALUE");
        }
        if (occurrences != 0) { counts.add(key, occurrences); version++; }
        return previous;
    }
    @Override public boolean remove(Object value) { return remove(value, 1) != 0; }
    @Override public int remove(Object value, int occurrences) {
        nonnegative(occurrences);
        int previous = count(value);
        if (previous != 0 && occurrences != 0) {
            counts.remove(((Long) value).longValue(), occurrences); version++;
        }
        return previous;
    }
    @Override public int setCount(Long value, int desired) {
        nonnegative(desired);
        long key = Objects.requireNonNull(value, "value").longValue();
        int previous = (int) counts.setCount(key, desired);
        if (previous != desired) version++;
        return previous;
    }
    @Override public boolean setCount(Long value, int expected, int desired) {
        nonnegative(expected); nonnegative(desired);
        Objects.requireNonNull(value, "value");
        if (count(value) != expected) return false;
        setCount(value, desired);
        return true;
    }
    @Override public void clear() {
        if (!isEmpty()) { counts.clear(); version++; }
    }
    @Override public boolean addAll(Collection<? extends Long> values) {
        Objects.requireNonNull(values, "values");
        if (values == this) {
            if (isEmpty()) return false;
            for (long key : counts.distinctValues()) add(key, count(key));
            return true;
        }
        if (values instanceof Multiset<? extends Long> multiset) {
            boolean changed = false;
            for (Entry<? extends Long> entry : multiset.entrySet()) {
                int n = entry.getCount();
                add(entry.getElement(), n);
                changed |= n != 0;
            }
            return changed;
        }
        return super.addAll(values);
    }
    @Override public boolean removeAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for (long key : counts.distinctValues()) {
            if (values.contains(key)) { setCount(key, 0); changed = true; }
        }
        return changed;
    }
    @Override public boolean retainAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for (long key : counts.distinctValues()) {
            if (!values.contains(key)) { setCount(key, 0); changed = true; }
        }
        return changed;
    }
    @Override public void forEachEntry(ObjIntConsumer<? super Long> action) {
        Objects.requireNonNull(action, "action");
        long expected = version;
        counts.forEachCount((key, amount) -> {
            action.accept(key, (int) amount);
            check(expected);
        });
    }

    @Override public Iterator<Long> iterator() {
        return new Iterator<>() {
            private final long[] keys = counts.distinctValues();
            private long expected = version;
            private int cursor;
            private int remaining;
            private long key;
            private boolean removable;
            @Override public boolean hasNext() {
                check(expected); return remaining != 0 || cursor < keys.length;
            }
            @Override public Long next() {
                if (!hasNext()) throw new NoSuchElementException();
                if (remaining == 0) { key = keys[cursor++]; remaining = count(key); }
                remaining--; removable = true; return key;
            }
            @Override public void remove() {
                check(expected);
                if (!removable) throw new IllegalStateException();
                M3GuavaLongMultiset.this.remove(key, 1);
                expected = version; removable = false;
            }
        };
    }

    private abstract class DistinctIterator<T> implements Iterator<T> {
        private final long[] keys = counts.distinctValues();
        private long expected = version;
        private int cursor;
        private long key;
        private boolean removable;
        abstract T project(long value);
        @Override public boolean hasNext() { check(expected); return cursor < keys.length; }
        @Override public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            key = keys[cursor++]; removable = true; return project(key);
        }
        @Override public void remove() {
            check(expected);
            if (!removable) throw new IllegalStateException();
            setCount(key, 0); expected = version; removable = false;
        }
    }

    @Override public Set<Long> elementSet() {
        if (elements == null) elements = new AbstractSet<>() {
            @Override public int size() { return counts.distinctSize(); }
            @Override public boolean contains(Object value) { return M3GuavaLongMultiset.this.contains(value); }
            @Override public boolean remove(Object value) {
                int n = count(value); return n != 0 && M3GuavaLongMultiset.this.remove(value, n) != 0;
            }
            @Override public void clear() { M3GuavaLongMultiset.this.clear(); }
            @Override public Iterator<Long> iterator() {
                return new DistinctIterator<>() { @Override Long project(long key) { return key; } };
            }
        };
        return elements;
    }
    @Override public Set<Entry<Long>> entrySet() {
        if (entries == null) entries = new AbstractSet<>() {
            @Override public int size() { return counts.distinctSize(); }
            @Override public boolean contains(Object value) {
                return value instanceof Entry<?> entry && entry.getCount() > 0
                        && count(entry.getElement()) == entry.getCount();
            }
            @Override public boolean remove(Object value) {
                if (!contains(value)) return false;
                Entry<?> entry = (Entry<?>) value;
                M3GuavaLongMultiset.this.remove(entry.getElement(), entry.getCount()); return true;
            }
            @Override public void clear() { M3GuavaLongMultiset.this.clear(); }
            @Override public Iterator<Entry<Long>> iterator() {
                return new DistinctIterator<>() {
                    @Override Entry<Long> project(long key) { return Multisets.immutableEntry(key, count(key)); }
                };
            }
        };
        return entries;
    }
    @Override public boolean equals(Object value) {
        if (value == this) return true;
        if (!(value instanceof Multiset<?> other) || other.entrySet().size() != counts.distinctSize()) return false;
        for (long key : counts.distinctValues()) if (count(key) != other.count(key)) return false;
        return true;
    }
    @Override public int hashCode() {
        int[] hash = {0};
        counts.forEachCount((key, amount) -> hash[0] += Long.hashCode(key) ^ (int) amount);
        return hash[0];
    }
    @Override public String toString() { return entrySet().toString(); }

    /** Independent primitive snapshot; mutating it cannot bypass the facade's int-count limit. */
    public M3LongBag toLongBag() { return counts.copy(); }

    private void check(long expected) {
        if (expected != version) throw new ConcurrentModificationException();
    }
    private static void nonnegative(int count) {
        if (count < 0) throw new IllegalArgumentException("negative count");
    }
}
