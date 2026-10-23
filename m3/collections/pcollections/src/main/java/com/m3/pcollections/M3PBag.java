/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Reuses the unchanged MIT-licensed PCollections PBag and unmodifiable base.
 * The key/count composition follows the reviewed MapPBag public contract.
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.pcollections.AbstractUnmodifiableCollection;
import org.pcollections.PBag;

/**
 * Persistent unordered occurrence counts over the canonical M3 bitmap-map owner.
 *
 * <p>Size counts occurrences; count returns a member's frequency. Minus removes one
 * occurrence, while minusAll removes every occurrence of each requested member.
 * Checked int arithmetic throws ArithmeticException before a new overflowing value
 * is returned. Null is supported and contributes zero to the occurrence-sum hash.
 *
 * <p>Equality is defined only against PBag instances with equal element frequencies,
 * following MapPBag's bag contract rather than List or Set equality. Ordinary Java
 * equality/hash semantics apply; comparator/identity-specific donor backing maps
 * remain distinct unchanged donor implementations. Iteration repeats each member
 * by its count in unspecified hash order. A serialization proxy stores distinct
 * member/count pairs, so its cost does not expand large frequencies.
 */
public final class M3PBag<E> extends AbstractUnmodifiableCollection<E>
        implements PBag<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static final M3PBag<?> EMPTY = new M3PBag<>(M3PMap.empty(), 0);
    private final M3PMap<E, Integer> counts;
    private final int size;

    private M3PBag(M3PMap<E, Integer> counts, int size) {
        this.counts = counts;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <E> M3PBag<E> empty() {
        return (M3PBag<E>) EMPTY;
    }

    public static <E> M3PBag<E> singleton(E value) {
        return M3PBag.<E>empty().plus(value);
    }

    @SuppressWarnings("unchecked")
    public static <E> M3PBag<E> from(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof M3PBag<?> bag) return (M3PBag<E>) bag;
        return M3PBag.<E>empty().plusAll(source);
    }

    @Override public int size() { return size; }
    @Override public boolean contains(Object value) { return counts.containsKey(value); }

    /** Exact occurrence count, including null; absent members have frequency zero. */
    public int count(Object value) {
        Integer count = counts.get(value);
        return count == null ? 0 : count;
    }

    @Override public M3PBag<E> plus(E value) {
        int newSize = Math.addExact(size, 1);
        int newCount = Math.addExact(count(value), 1);
        return new M3PBag<>(counts.plus(value, newCount), newSize);
    }

    @Override public M3PBag<E> plusAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3PBag<E> result = this;
        for (E value : source) result = result.plus(value);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override public M3PBag<E> minus(Object value) {
        int previous = count(value);
        if (previous == 0) return this;
        if (size == 1) return empty();
        M3PMap<E, Integer> changed = previous == 1
                ? counts.minus(value) : counts.plus((E) value, previous - 1);
        return new M3PBag<>(changed, size - 1);
    }

    @Override public M3PBag<E> minusAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        M3PMap<E, Integer> changed = counts;
        int newSize = size;
        for (Object value : source) {
            Integer previous = changed.get(value);
            if (previous != null) {
                changed = changed.minus(value);
                newSize -= previous;
            }
        }
        if (changed == counts) return this;
        return newSize == 0 ? empty() : new M3PBag<>(changed, newSize);
    }

    @Override public Iterator<E> iterator() {
        Iterator<Map.Entry<E, Integer>> entries = counts.entrySet().iterator();
        return new Iterator<E>() {
            private E current;
            private int remaining;

            @Override public boolean hasNext() { return remaining != 0 || entries.hasNext(); }

            @Override public E next() {
                if (remaining == 0) {
                    if (!entries.hasNext()) throw new NoSuchElementException();
                    Map.Entry<E, Integer> entry = entries.next();
                    current = entry.getKey();
                    remaining = entry.getValue();
                }
                remaining--;
                E result = current;
                if (remaining == 0) current = null;
                return result;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException("immutable M3 persistent bag iterator");
            }
        };
    }

    @Override public int hashCode() {
        int hash = 0;
        for (Map.Entry<E, Integer> entry : counts.entrySet()) {
            hash += Objects.hashCode(entry.getKey()) * entry.getValue();
        }
        return hash;
    }

    @SuppressWarnings("unchecked")
    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof PBag<?> bag) || size != bag.size()) return false;
        if (other instanceof M3PBag<?> m3) return counts.equals(m3.counts);
        M3PMap<E, Integer> remaining = counts;
        for (Object value : bag) {
            Integer count = remaining.get(value);
            if (count == null) return false;
            remaining = count == 1 ? remaining.minus(value) : remaining.plus((E) value, count - 1);
        }
        return remaining.isEmpty();
    }

    @Serial private Object writeReplace() {
        return new SerializationProxy(counts);
    }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final M3PMap<?, Integer> counts;

        private SerializationProxy(M3PMap<?, Integer> counts) { this.counts = counts; }

        @Serial private Object readResolve() throws ObjectStreamException {
            if (counts == null) throw new InvalidObjectException("invalid persistent bag counts");
            M3PMap<Object, Integer> restored = M3PMap.empty();
            int size = 0;
            for (Map.Entry<?, Integer> entry : counts.entrySet()) {
                Integer count = entry.getValue();
                if (count == null || count <= 0) {
                    throw new InvalidObjectException("nonpositive persistent bag frequency");
                }
                try {
                    size = Math.addExact(size, count);
                } catch (ArithmeticException overflow) {
                    InvalidObjectException invalid = new InvalidObjectException("persistent bag size overflow");
                    invalid.initCause(overflow);
                    throw invalid;
                }
                restored = restored.plus(entry.getKey(), count);
            }
            return size == 0 ? M3PBag.empty() : new M3PBag<>(restored, size);
        }
    }
}
