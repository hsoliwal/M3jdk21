/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * M3 Commons Collections Bag: first-party implementation against the Apache Bag API.
 */
package com.m3.collections.interop;

import com.m3.collections.M3CountedBag;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.Bag;

/**
 * Real Apache Commons Collections {@link Bag} with one M3 reference-key index and
 * primitive logical multiplicities, not an Apache HashBag or a bag-of-entry objects.
 *
 * <p>Apache Bag intentionally differs from the JDK Collection contract: a
 * one-element add returns true only for a newly distinct key; one-element remove
 * erases every copy; containsAll, removeAll and retainAll honor multiplicity.
 * The backing {@link M3CountedBag} owns GC-traced values and count/index offsets.
 * API views and temporary input-cardinality maps allocate as needed, but no
 * per-occurrence resident Entry or MutableInteger object is retained.
 *
 * <p>Mutable, single-threaded, non-serializable, and not the concrete HashBag type.
 * Iterator reads are fail-fast for all underlying count mutations. JNI callbacks
 * never handle Java values; native indexed-mask selection remains an optional
 * operation of the existing backing M3CountedBag.
 */
public final class M3ApacheBag<E> extends AbstractCollection<E> implements Bag<E> {
    private final M3CountedBag<E> counts;
    private transient Set<E> unique;

    /** Independent M3-backed bag, with lazy backing allocation. */
    public M3ApacheBag() { this(new M3CountedBag<>()); }

    /** Views may intentionally share one M3 owner; there is no retained second store. */
    public M3ApacheBag(M3CountedBag<E> counts) {
        this.counts = Objects.requireNonNull(counts, "counts");
    }

    public M3CountedBag<E> m3Backend() { return counts; }

    @Override public int size() { return counts.size(); }
    @Override public boolean isEmpty() { return counts.isEmpty(); }
    @Override public boolean contains(Object value) { return counts.contains(value); }
    @Override public int getCount(Object value) { return counts.count(value); }
    @Override public Iterator<E> iterator() { return counts.iterator(); }
    @Override public void clear() { counts.clear(); }

    /** Apache reports new distinctness, not successful occurrence insertion. */
    @Override public boolean add(E value) { return add(value, 1); }

    /** Apache HashBag treats zero/negative copy requests as no-ops. */
    @Override public boolean add(E value, int copies) {
        if (copies <= 0) return false;
        return counts.addOccurrences(value, copies) == 0;
    }

    /** Apache Bag.remove(Object) erases all copies, unlike a JDK Collection. */
    @Override public boolean remove(Object value) {
        int old = counts.count(value);
        if (old == 0) return false;
        counts.removeOccurrences(value, old);
        return true;
    }

    /** Removes up to the requested number, retaining other copies. */
    @Override public boolean remove(Object value, int copies) {
        return copies > 0 && counts.removeOccurrences(value, copies) > 0;
    }

    /** A live, unmodifiable distinct-key view; no snapshot copy. */
    @Override public Set<E> uniqueSet() {
        if (unique == null) unique = Collections.unmodifiableSet(counts.distinctElements());
        return unique;
    }

    /** Apache returns true only when at least one *distinct* key was inserted. */
    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        Collection<? extends E> input = source == this ? new ArrayList<>(this) : source;
        boolean newDistinct = false;
        for (E element : input) newDistinct |= add(element);
        return newDistinct;
    }

    /** Cardinality-aware even when the argument is not itself a Bag. */
    @Override public boolean containsAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof Bag<?> bag) {
            for (Object element : bag.uniqueSet()) {
                if (getCount(element) < bag.getCount(element)) return false;
            }
            return true;
        }
        M3CountedBag<Object> requested = tally(source);
        for (Object key : requested.distinctElements()) {
            if (getCount(key) < requested.count(key)) return false;
        }
        return true;
    }

    /** Unlike Collection.removeAll, remove only the requested multiplicities. */
    @Override public boolean removeAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        M3CountedBag<Object> wanted = tally(source);
        boolean changed = false;
        for (Object key : wanted.distinctElements()) {
            changed |= remove(key, wanted.count(key));
        }
        return changed;
    }

    /**
     * Commons Collections 4.5.0 AbstractMapBag.retainAll parity.
     * In that release a request for MORE copies than owned removes the owned key.
     * Preserve its published behavior, including this nonstandard edge case.
     */
    @Override public boolean retainAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        if (source == this) return false;
        M3CountedBag<Object> wanted = tally(source);
        boolean changed = false;
        for (E key : new ArrayList<>(counts.distinctElements())) {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            int owned = counts.count(key);
            int requested = wanted.count(key);
            int excess = requested == 0 || requested > owned
                    ? owned : owned - requested;
=======
            int stored = counts.count(key);
            int requested = wanted.count(key);
            // Apache Commons Collections 4.5.0 AbstractMapBag.retainAll:
            // an over-requested multiplicity removes all incumbent copies.
            int excess = requested >= 1 && requested <= stored ? stored - requested : stored;
>>>>>>> 8ffe1f8c047161076bf91eb537a187efce96f645
            if (excess > 0) changed |= remove(key, excess);
        }
        return changed;
    }

    private static M3CountedBag<Object> tally(Collection<?> source) {
        M3CountedBag<Object> wanted = new M3CountedBag<>();
        if (source instanceof Bag<?> bag) {
            for (Object element : bag.uniqueSet()) {
                wanted.addOccurrences(element, bag.getCount(element));
            }
        } else {
            for (Object element : source) wanted.add(element);
        }
        return wanted;
    }

    /** Count-based equality is intentionally restricted to Apache Bag values. */
    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Bag<?> bag) || bag.size() != size()) return false;
        for (E element : counts.distinctElements()) {
            if (counts.count(element) != bag.getCount(element)) return false;
        }
        return true;
    }

    /** Apache's per-key hash is (elementHash XOR occurrenceCount). */
    @Override public int hashCode() {
        int hash = 0;
        for (E element : counts.distinctElements()) {
            hash += Objects.hashCode(element) ^ counts.count(element);
        }
        return hash;
    }
}
