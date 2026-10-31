// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding set spliterator.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SequencedSet;
import java.util.Spliterator;

/** Key-only hash slots with insertion order and primitive encounter links. */
public final class M3LinkedHashSet<E> extends AbstractSet<E> implements SequencedSet<E> {
    private final M3LinkedHashMap<E, Object> map;
    /** Empty insertion-order set, allowing null. */
    public M3LinkedHashSet() { this(new M3LinkedHashMap<>(false, true)); }
    private M3LinkedHashSet(M3LinkedHashMap<E, Object> map) { this.map = map; }
    @Override public int size() { return map.size(); }
    @Override public boolean contains(Object value) { return map.containsKey(value); }
    @Override public boolean add(E value) { if (contains(value)) { return false; } map.put(value, null); return true; }
    @Override public boolean remove(Object value) { int s = map.findSlot(value); if (s < 0) { return false; } map.eraseAt(s); return true; }
    @Override public void clear() { map.clear(); }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.ordered(this, true); }
    @Override public Iterator<E> iterator() { return map.slots(0); }
    @Override public E getFirst() { return endpoint(false, false); }
    @Override public E getLast() { return endpoint(true, false); }
    @Override public E removeFirst() { return endpoint(false, true); }
    @Override public E removeLast() { return endpoint(true, true); }
    private E endpoint(boolean last, boolean remove) {
        int s = last ? map.lastSlot() : map.firstSlot(); if (s < 0) { throw new NoSuchElementException(); }
        E key = map.keyAt(s); if (remove) { map.eraseAt(s); } return key;
    }
    @Override public void addFirst(E value) { map.putFirst(value, null); }
    @Override public void addLast(E value) { map.putLast(value, null); }
    @Override public M3LinkedHashSet<E> reversed() { return new M3LinkedHashSet<>(map.reversed()); }
    /** Validate slot/index/link ownership. */
    public void verifyInvariants() { map.verifyInvariants(); }
}
