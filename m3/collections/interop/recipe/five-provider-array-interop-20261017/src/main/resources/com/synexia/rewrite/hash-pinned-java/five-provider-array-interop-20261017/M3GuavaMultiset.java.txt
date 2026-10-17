/*
 * Copyright (C) 2007 The Guava Authors
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modified 2026: adapt the AbstractMultiset/Multisets skeletal contracts from
 * google/guava@f06690fa3e874f65515e8fd338a74d636e2c792f (v33.4.8) to the
 * existing M3 slot owner and primitive count plane. No Guava package-private
 * APIs or copied package names are used. AbstractMultiset donor Git blob:
 * 55848b24b4969bf37a114e28d64221444d4dade2.
 */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.m3.collections.M3ObjectCounts;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * Guava's mutable multiset contract over M3's shared key slots and primitive counts.
 *
 * <p>Distinct elements retain insertion order; iteration repeats each distinct element
 * by its count. Null is a valid element. Counts are nonnegative {@code int} values;
 * a per-element overflow is rejected before mutation. {@link #size()} saturates at
 * {@link Integer#MAX_VALUE}, while the owner retains an exact long total.
 *
 * <p>Element and entry sets are backed views. An entry reads its current count, including
 * after removal and reinsertion. Removing through the element/entry view removes all
 * occurrences; removing through the repeated-element iterator removes one occurrence.
 * No per-distinct-element count box or retained entry object is introduced.
 * This collection is not thread-safe and does not implement serialization.
 */
public final class M3GuavaMultiset<E> extends AbstractCollection<E> implements Multiset<E> {
    private final M3ObjectCounts<E> counts;
    private Set<Entry<E>> entries;

    /** Creates an independent empty multiset. */
    public M3GuavaMultiset() { this(new M3ObjectCounts<>()); }

    /** Creates a Guava facade sharing the supplied, nonconcurrent M3 count owner. */
    public M3GuavaMultiset(M3ObjectCounts<E> counts) {
        this.counts = Objects.requireNonNull(counts, "counts");
    }

    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, counts.totalCount()); }
    @Override public boolean isEmpty() { return counts.distinctSize() == 0; }
    @Override public int count(Object element) { return counts.count(element); }
    @Override public boolean contains(Object element) { return count(element) != 0; }

    @Override public int add(E element, int occurrences) { return counts.add(element, occurrences); }
    @Override public boolean add(E element) { add(element, 1); return true; }
    @Override public int remove(Object element, int occurrences) { return counts.remove(element, occurrences); }
    @Override public boolean remove(Object element) { return remove(element, 1) != 0; }
    @Override public int setCount(E element, int count) { return counts.setCount(element, count); }

    @Override public boolean setCount(E element, int oldCount, int newCount) {
        if (oldCount < 0 || newCount < 0) throw new IllegalArgumentException("negative count");
        if (count(element) != oldCount) return false;
        counts.setCount(element, newCount);
        return true;
    }

    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) return false;
        if (source instanceof Multiset<? extends E> multiset) {
            for (Entry<? extends E> entry : multiset.entrySet()) {
                add(entry.getElement(), entry.getCount());
            }
        } else {
            for (E element : source) add(element);
        }
        return true;
    }

    @Override public boolean containsAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        return elementSet().containsAll(source instanceof Multiset<?> multiset ? multiset.elementSet() : source);
    }

    @Override public boolean removeAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        return elementSet().removeAll(source instanceof Multiset<?> multiset ? multiset.elementSet() : source);
    }

    @Override public boolean retainAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        return elementSet().retainAll(source instanceof Multiset<?> multiset ? multiset.elementSet() : source);
    }

    @Override public void clear() { counts.clear(); }
    @Override public Set<E> elementSet() { return counts.elementSet(); }
    @Override public void forEachEntry(ObjIntConsumer<? super E> action) { counts.forEachCount(action); }

    @Override public Iterator<E> iterator() {
        Iterator<E> distinct = elementSet().iterator();
        return new Iterator<>() {
            private E element;
            private int remaining;
            private boolean canRemove;

            @Override public boolean hasNext() { return remaining != 0 || distinct.hasNext(); }

            @Override public E next() {
                // Consult the underlying iterator even while repeating a key, so its
                // structural-change check remains the authoritative fail-fast boundary.
                boolean moreDistinct = distinct.hasNext();
                if (remaining == 0) {
                    if (!moreDistinct) throw new NoSuchElementException();
                    element = distinct.next();
                    remaining = count(element);
                }
                remaining--;
                canRemove = true;
                return element;
            }

            @Override public void remove() {
                if (!canRemove) throw new IllegalStateException();
                distinct.hasNext();
                if (count(element) == 1) distinct.remove();
                else counts.remove(element, 1);
                canRemove = false;
            }
        };
    }

    @Override public Set<Entry<E>> entrySet() {
        if (entries == null) entries = new EntrySet();
        return entries;
    }

    private final class EntrySet extends AbstractSet<Entry<E>> {
        @Override public int size() { return counts.distinctSize(); }
        @Override public void clear() { counts.clear(); }

        @Override public boolean contains(Object object) {
            if (!(object instanceof Entry<?> entry) || entry.getCount() <= 0) return false;
            return count(entry.getElement()) == entry.getCount();
        }

        @Override public boolean remove(Object object) {
            if (!contains(object)) return false;
            Entry<?> entry = (Entry<?>) object;
            return counts.elementSet().remove(entry.getElement());
        }

        @Override public Iterator<Entry<E>> iterator() {
            Iterator<E> distinct = elementSet().iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return distinct.hasNext(); }
                @Override public Entry<E> next() { return new CountEntry(distinct.next()); }
                @Override public void remove() { distinct.remove(); }
            };
        }
    }

    private final class CountEntry implements Entry<E> {
        private final E element;
        CountEntry(E element) { this.element = element; }
        @Override public E getElement() { return element; }
        @Override public int getCount() { return count(element); }

        @Override public boolean equals(Object object) {
            return object instanceof Entry<?> entry && getCount() == entry.getCount()
                    && Objects.equals(element, entry.getElement());
        }

        @Override public int hashCode() { return Objects.hashCode(element) ^ getCount(); }

        @Override public String toString() {
            int count = getCount();
            return count == 1 ? String.valueOf(element) : element + " x " + count;
        }
    }

    @Override public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof Multiset<?> other) || size() != other.size()
                || counts.distinctSize() != other.elementSet().size()) return false;
        for (Entry<E> entry : entrySet()) {
            if (other.count(entry.getElement()) != entry.getCount()) return false;
        }
        return true;
    }

    @Override public int hashCode() { return entrySet().hashCode(); }
    @Override public String toString() { return entrySet().toString(); }

    /** Checks the canonical owner without materializing repeated occurrences. */
    public void verifyInvariants() { counts.verifyInvariants(); }
}
