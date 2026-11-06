/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * TEMPLATE of m3/collections/qualification/flat3map-single-owner-20261106/apply.py (census leaf L075); the
 * public surface still mirrors org.apache.commons.collections4.map.Flat3Map from Apache Commons Collections
 * 4.5.0 (Apache-2.0) as the five-provider scaffold declared it; no provider implementation code is copied.
 */
package jdk.m3.collections.apache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.collections4.Get;
import org.apache.commons.collections4.IterableGet;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.Put;

/**
 * Commons-compatible facade of {@code org.apache.commons.collections4.map.Flat3Map} over the single M3
 * implementation {@link jdk.m3.collections.M3Flat3Map}: every operation delegates to one private core
 * instance, and this class keeps no keys, values or slot states of its own.
 *
 * <p>{@link #entrySet()}, {@link #keySet()}, {@link #values()} and {@link #mapIterator()} are cursors over
 * the core's live slots and write through; an iterator's {@code remove} keeps its cursor valid. Iteration
 * order is the core's slot order (insertion order up to three entries, table order after); Commons does not
 * specify one. The {@code java.util.Map} default methods are the JDK defaults, as in Commons.
 *
 * <p>Serialized form: this class's name, {@code serialVersionUID} 0 and no serializable fields (unchanged
 * from the scaffold), followed by the Commons data layout: the entry count, then each key and its value.
 */
//FLLDCIM: MRL - L075 TEMPLATE: scaffold bodies replaced by delegation to the single owner jdk.m3.collections.M3Flat3Map
public final class M3Flat3Map<K, V>
        implements Cloneable,
                java.io.Serializable,
                Map<K, V>,
                Get<K, V>,
                IterableGet<K, V>,
                IterableMap<K, V>,
                Put<K, V> {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final String EMPTY = "{}";
    private static final String THIS_MAP = "(this Map)";

    /** The single implementation; assigned only by the constructor, clone and readObject. */
    private transient jdk.m3.collections.M3Flat3Map<K, V> core;

    public M3Flat3Map() {
        core = new jdk.m3.collections.M3Flat3Map<>();
    }

    public M3Flat3Map(Map<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    public void clear() { core.clear(); }

    public M3Flat3Map<K, V> clone() {
        try {
            @SuppressWarnings("unchecked")
            M3Flat3Map<K, V> cloned = (M3Flat3Map<K, V>) super.clone();
            cloned.core = core.copy();
            return cloned;
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
        return IterableMap.super.compute(key, remapping);
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mapping) {
        return IterableMap.super.computeIfAbsent(key, mapping);
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
        return IterableMap.super.computeIfPresent(key, remapping);
    }

    public boolean containsKey(Object key) { return core.containsKey(key); }

    public boolean containsValue(Object value) { return core.containsValue(value); }

    public Set<Map.Entry<K, V>> entrySet() { return new EntryView(); }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Map<?, ?> map) || map.size() != core.size()) return false;
        try {
            for (int slot = core.nextSlot(0); slot >= 0; slot = core.nextSlot(slot + 1)) {
                Object key = core.slotKey(slot);
                if (!map.containsKey(key) || !Objects.equals(core.slotValue(slot), map.get(key))) return false;
            }
        } catch (ClassCastException | NullPointerException incomparable) {
            return false;
        }
        return true;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) { IterableMap.super.forEach(action); }

    public V get(Object key) { return core.get(key); }

    public V getOrDefault(Object key, V defaultValue) { return IterableMap.super.getOrDefault(key, defaultValue); }

    public int hashCode() {
        int hash = 0;
        for (int slot = core.nextSlot(0); slot >= 0; slot = core.nextSlot(slot + 1)) {
            hash += Objects.hashCode(core.slotKey(slot)) ^ Objects.hashCode(core.slotValue(slot));
        }
        return hash;
    }

    public boolean isEmpty() { return core.isEmpty(); }

    public Set<K> keySet() { return new KeyView(); }

    public MapIterator<K, V> mapIterator() { return new KeyCursor(); }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remapping) {
        return IterableMap.super.merge(key, value, remapping);
    }

    public V put(K key, V value) { return core.put(key, value); }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) core.put(entry.getKey(), entry.getValue());
    }

    public V putIfAbsent(K key, V value) { return IterableMap.super.putIfAbsent(key, value); }

    public V remove(Object key) { return core.remove(key); }

    public boolean remove(Object key, Object value) { return IterableMap.super.remove(key, value); }

    public V replace(K key, V value) { return IterableMap.super.replace(key, value); }

    public boolean replace(K key, V oldValue, V newValue) { return IterableMap.super.replace(key, oldValue, newValue); }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        IterableMap.super.replaceAll(function);
    }

    public int size() { return core.size(); }

    public String toString() {
        if (core.isEmpty()) return EMPTY;
        StringBuilder text = new StringBuilder().append('{');
        for (int slot = core.nextSlot(0); slot >= 0; slot = core.nextSlot(slot + 1)) {
            if (text.length() > 1) text.append(',').append(' ');
            text.append(self(core.slotKey(slot))).append('=').append(self(core.slotValue(slot)));
        }
        return text.append('}').toString();
    }

    public Collection<V> values() { return new ValueView(); }

    private Object self(Object item) { return item == this ? THIS_MAP : item; }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(core.size());
        for (int slot = core.nextSlot(0); slot >= 0; slot = core.nextSlot(slot + 1)) {
            out.writeObject(core.slotKey(slot));
            out.writeObject(core.slotValue(slot));
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        core = new jdk.m3.collections.M3Flat3Map<>();
        for (int count = in.readInt(); count > 0; count--) core.put((K) in.readObject(), (V) in.readObject());
    }

    /** Cursor over the core's live slots: {@code current} is the slot of the last {@code next()}, else -1. */
    private abstract class SlotCursor {
        private int resume;
        private int current = -1;

        public final boolean hasNext() { return core.nextSlot(resume) >= 0; }

        final int advance() {
            int slot = core.nextSlot(resume);
            if (slot < 0) throw new NoSuchElementException();
            current = slot;
            resume = slot + 1;
            return slot;
        }

        final int current() {
            if (current < 0) throw new IllegalStateException();
            return current;
        }

        public final void remove() {
            resume = core.removeSlot(current());
            current = -1;
            removed();
        }

        void removed() { }
    }

    private final class KeyCursor extends SlotCursor implements MapIterator<K, V> {
        public K next() { return core.slotKey(advance()); }

        public K getKey() { return core.slotKey(current()); }

        public V getValue() { return core.slotValue(current()); }

        public V setValue(V value) { return core.setSlotValue(current(), value); }
    }

    private final class ValueCursor extends SlotCursor implements Iterator<V> {
        public V next() { return core.slotValue(advance()); }
    }

    private final class EntryCursor extends SlotCursor implements Iterator<Map.Entry<K, V>> {
        private SlotEntry last;

        public Map.Entry<K, V> next() { return last = new SlotEntry(advance()); }

        @Override
        void removed() { last.slot = -1; }
    }

    /** Boundary flyweight of one live slot, created only by entry iteration; writes through to the core. */
    private final class SlotEntry implements Map.Entry<K, V> {
        private int slot;

        SlotEntry(int slot) { this.slot = slot; }

        public K getKey() { return core.slotKey(slot); }

        public V getValue() { return core.slotValue(slot); }

        public V setValue(V value) { return core.setSlotValue(slot, value); }

        public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> entry
                    && Objects.equals(getKey(), entry.getKey()) && Objects.equals(getValue(), entry.getValue());
        }

        public int hashCode() { return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue()); }

        public String toString() { return String.valueOf(getKey()) + '=' + getValue(); }
    }

    private final class EntryView extends AbstractSet<Map.Entry<K, V>> {
        public Iterator<Map.Entry<K, V>> iterator() { return new EntryCursor(); }

        public int size() { return core.size(); }

        public void clear() { core.clear(); }

        public boolean contains(Object other) {
            return other instanceof Map.Entry<?, ?> entry && core.containsKey(entry.getKey())
                    && Objects.equals(core.get(entry.getKey()), entry.getValue());
        }

        public boolean remove(Object other) {
            if (!contains(other)) return false;
            core.remove(((Map.Entry<?, ?>) other).getKey());
            return true;
        }
    }

    private final class KeyView extends AbstractSet<K> {
        public Iterator<K> iterator() { return new KeyCursor(); }

        public int size() { return core.size(); }

        public void clear() { core.clear(); }

        public boolean contains(Object key) { return core.containsKey(key); }

        public boolean remove(Object key) {
            boolean present = core.containsKey(key);
            core.remove(key);
            return present;
        }
    }

    private final class ValueView extends AbstractCollection<V> {
        public Iterator<V> iterator() { return new ValueCursor(); }

        public int size() { return core.size(); }

        public void clear() { core.clear(); }

        public boolean contains(Object value) { return core.containsValue(value); }
    }
}
