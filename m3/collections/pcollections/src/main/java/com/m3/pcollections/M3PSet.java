/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Reuses the unchanged MIT-licensed PCollections PSet and unmodifiable base.
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import org.pcollections.AbstractUnmodifiableSet;
import org.pcollections.PSet;

/**
 * Persistent equality set composed over the canonical M3 bitmap-map owner.
 *
 * <p>Null is allowed. Duplicate additions retain the first stored representative,
 * including during plusAll. A key's presence is stored in the map with one shared
 * Boolean marker; no per-element wrapper or parallel membership index is retained.
 * Iteration has the map's unspecified hash order and observes this immutable version.
 */
public final class M3PSet<E> extends AbstractUnmodifiableSet<E>
        implements PSet<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static final M3PSet<?> EMPTY = new M3PSet<>(M3PMap.empty());
    private final M3PMap<E, Boolean> map;

    private M3PSet(M3PMap<E, Boolean> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public static <E> M3PSet<E> empty() {
        return (M3PSet<E>) EMPTY;
    }

    public static <E> M3PSet<E> singleton(E value) {
        return M3PSet.<E>empty().plus(value);
    }

    /** Capture equality-distinct values, preserving each first representative. */
    @SuppressWarnings("unchecked")
    public static <E> M3PSet<E> from(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof M3PSet<?> set) return (M3PSet<E>) set;
        return M3PSet.<E>empty().plusAll(source);
    }

    @Override public int size() { return map.size(); }
    @Override public boolean contains(Object value) { return map.containsKey(value); }
    @Override public Iterator<E> iterator() { return map.keySet().iterator(); }

    @Override public M3PSet<E> plus(E value) {
        if (map.containsKey(value)) return this;
        return new M3PSet<>(map.plus(value, Boolean.TRUE));
    }

    @Override public M3PSet<E> plusAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3PSet<E> result = this;
        for (E value : source) result = result.plus(value);
        return result;
    }

    @Override public M3PSet<E> minus(Object value) {
        M3PMap<E, Boolean> changed = map.minus(value);
        if (changed == map) return this;
        return changed.isEmpty() ? empty() : new M3PSet<>(changed);
    }

    @Override public M3PSet<E> minusAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        M3PMap<E, Boolean> changed = map.minusAll(source);
        if (changed == map) return this;
        return changed.isEmpty() ? empty() : new M3PSet<>(changed);
    }

    /** Membership uses ordinary equality and retains this set's representatives. */
    @Override public M3PSet<E> intersect(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3PSet<E> candidates = M3PSet.from(source);
        M3PSet<E> result = this;
        for (E value : this) {
            if (!candidates.contains(value)) result = result.minus(value);
        }
        return result;
    }

    @Serial private Object writeReplace() {
        return new SerializationProxy(toArray());
    }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // Payload serializability is checked by ObjectOutputStream.
        @SuppressWarnings("serial")
        private final Object[] values;

        private SerializationProxy(Object[] values) { this.values = values; }

        @Serial private Object readResolve() throws ObjectStreamException {
            if (values == null) throw new InvalidObjectException("invalid persistent set values");
            M3PSet<Object> result = M3PSet.empty();
            for (Object value : values) {
                M3PSet<Object> changed = result.plus(value);
                if (changed.size() != result.size() + 1) {
                    throw new InvalidObjectException("duplicate persistent set member");
                }
                result = changed;
            }
            return result;
        }
    }
}
