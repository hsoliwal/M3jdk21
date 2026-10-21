// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Synexia contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.apache;

import com.m3.collections.M3LinkedHashSet;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.SequencedSet;
import java.util.Spliterator;

/** Serializable detached-group shape around the unchanged M3 array set owner. */
public final class M3LinkedHashSetGroup<E> extends AbstractSet<E>
        implements SequencedSet<E>, Serializable, Cloneable {
    @Serial private static final long serialVersionUID = 1L;
    private transient M3LinkedHashSet<E> owner;
    public M3LinkedHashSetGroup(final int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("negative initialCapacity");
        owner = new M3LinkedHashSet<>();
    }
    @Override public int size() { return owner.size(); }
    @Override public boolean contains(final Object value) { return owner.contains(value); }
    @Override public boolean add(final E value) { return owner.add(value); }
    @Override public boolean remove(final Object value) { return owner.remove(value); }
    @Override public void clear() { owner.clear(); }
    @Override public Iterator<E> iterator() { return owner.iterator(); }
    @Override public Spliterator<E> spliterator() { return owner.spliterator(); }
    @Override public E getFirst() { return owner.getFirst(); }
    @Override public E getLast() { return owner.getLast(); }
    @Override public E removeFirst() { return owner.removeFirst(); }
    @Override public E removeLast() { return owner.removeLast(); }
    @Override public void addFirst(final E value) { owner.addFirst(value); }
    @Override public void addLast(final E value) { owner.addLast(value); }
    @Override public SequencedSet<E> reversed() { return new Reverse<>(owner.reversed(), this); }
    @Override public M3LinkedHashSetGroup<E> clone() {
        final M3LinkedHashSetGroup<E> result = new M3LinkedHashSetGroup<>(0);
        result.addAll(this);
        return result;
    }
    @Serial private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); out.writeInt(size());
        for (final E value : this) out.writeObject(value);
    }
    @Serial private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); final int count = in.readInt();
        if (count < 0) throw new InvalidObjectException("negative group count");
        owner = new M3LinkedHashSet<>();
        for (int i=0;i<count;i++) {
            @SuppressWarnings("unchecked") final E value = (E) in.readObject();
            owner.add(value);
        }
    }
    private static final class Reverse<E> extends AbstractSet<E> implements SequencedSet<E> {
        private final M3LinkedHashSet<E> source;
        private final M3LinkedHashSetGroup<E> forward;
        Reverse(final M3LinkedHashSet<E> source, final M3LinkedHashSetGroup<E> forward) {
            this.source=source; this.forward=forward;
        }
        @Override public int size() { return source.size(); }
        @Override public boolean contains(final Object value) { return source.contains(value); }
        @Override public boolean add(final E value) { return source.add(value); }
        @Override public boolean remove(final Object value) { return source.remove(value); }
        @Override public void clear() { source.clear(); }
        @Override public Iterator<E> iterator() { return source.iterator(); }
        @Override public Spliterator<E> spliterator() { return source.spliterator(); }
        @Override public E getFirst() { return source.getFirst(); }
        @Override public E getLast() { return source.getLast(); }
        @Override public E removeFirst() { return source.removeFirst(); }
        @Override public E removeLast() { return source.removeLast(); }
        @Override public void addFirst(final E value) { source.addFirst(value); }
        @Override public void addLast(final E value) { source.addLast(value); }
        @Override public SequencedSet<E> reversed() { return forward; }
    }
}
