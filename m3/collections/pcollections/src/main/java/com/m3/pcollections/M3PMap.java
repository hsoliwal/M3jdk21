/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Reuses the unchanged MIT-licensed PCollections PMap and unmodifiable base.
 * The bitmap-array storage below is an independent Synexia implementation.
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.pcollections.AbstractUnmodifiableMap;
import org.pcollections.PMap;

/**
 * Persistent equality map with bitmap-ranked directories and packed key/value arrays.
 *
 * <p>Five hash bits choose a directory position; population count converts its bit
 * into an array offset. Inline mappings occupy adjacent key/value slots. Child
 * directories follow those slots in the same privately owned array. Only a changed
 * path is copied. Equal full hashes use a packed collision array and exact equality,
 * never one retained Entry or link object per mapping. Lookup and update traverse at
 * most seven bitmap levels plus an exact collision group; a collision group of c
 * mappings requires O(c) equality work and O(c) array copying for a changed update.
 *
 * <p>Null keys and values are supported. User keys must keep stable hash/equality
 * behavior while any containing version remains in use. An equal-key update retains
 * the incoming key and value references; only identical key and value references
 * make that update a no-op. Hash iteration order is deliberately unspecified.
 * Persistence protects collection state, not mutable objects stored by callers.
 *
 * <p>Every destructive Map operation and view mutation refuses, including no-op
 * conditional defaults. Entry projections are fresh and immutable. Serialization
 * exports mappings through a proxy and rebuilds private immutable directories.
 */
public final class M3PMap<K, V> extends AbstractUnmodifiableMap<K, V>
        implements PMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static final M3PMap<?, ?> EMPTY = new M3PMap<>(null, 0);
    private static final Object ABSENT = new Object();
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    // Proxy-only serialization prevents a stream from publishing aliased node arrays.
    private final transient Node root;
    private final int size;

    private M3PMap(Node root, int size) {
        this.root = root;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> M3PMap<K, V> empty() {
        return (M3PMap<K, V>) EMPTY;
    }

    public static <K, V> M3PMap<K, V> singleton(K key, V value) {
        return M3PMap.<K, V>empty().plus(key, value);
    }

    /** Capture the input mappings; an already immutable M3 map can share its value. */
    @SuppressWarnings("unchecked")
    public static <K, V> M3PMap<K, V> from(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof M3PMap<?, ?> map) return (M3PMap<K, V>) map;
        return M3PMap.<K, V>empty().plusAll(source);
    }

    @Override public int size() { return size; }

    @Override public boolean containsKey(Object key) {
        return find(key) != ABSENT;
    }

    @SuppressWarnings("unchecked")
    @Override public V get(Object key) {
        Object value = find(key);
        return value == ABSENT ? null : (V) value;
    }

    private Object find(Object key) {
        return root == null ? ABSENT : root.find(key, hash(key), 0);
    }

    @Override public boolean containsValue(Object value) {
        Iterator<Object> iterator = new Cursor<>(root, VALUES);
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), value)) return true;
        }
        return false;
    }

    @Override public M3PMap<K, V> plus(K key, V value) {
        int hash = hash(key);
        boolean present = root != null && root.find(key, hash, 0) != ABSENT;
        int newSize = present ? size : Math.addExact(size, 1);
        Node changed = root == null ? Node.entry(key, value, hash, 0)
                : root.put(key, value, hash, 0);
        if (changed == root) return this;
        return new M3PMap<>(changed, newSize);
    }

    @Override public M3PMap<K, V> plusAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        M3PMap<K, V> result = this;
        for (Entry<? extends K, ? extends V> entry : source.entrySet()) {
            result = result.plus(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override public M3PMap<K, V> minus(Object key) {
        if (root == null) return this;
        Node changed = root.remove(key, hash(key), 0);
        if (changed == root) return this;
        return changed == null ? empty() : new M3PMap<>(changed, size - 1);
    }

    @Override public M3PMap<K, V> minusAll(Collection<?> keys) {
        Objects.requireNonNull(keys, "keys");
        M3PMap<K, V> result = this;
        for (Object key : keys) result = result.minus(key);
        return result;
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        if (root != null) root.forEach(action);
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(new AbstractSet<Entry<K, V>>() {
            @Override public int size() { return size; }

            @Override public Iterator<Entry<K, V>> iterator() {
                return new Cursor<>(root, ENTRIES);
            }

            @Override public boolean contains(Object candidate) {
                if (!(candidate instanceof Map.Entry<?, ?> entry)) return false;
                Object actual = find(entry.getKey());
                return actual != ABSENT && Objects.equals(actual, entry.getValue());
            }
        });
    }

    @Override public Set<K> keySet() {
        return Collections.unmodifiableSet(new AbstractSet<K>() {
            @Override public int size() { return size; }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public Iterator<K> iterator() { return new Cursor<>(root, KEYS); }
        });
    }

    @Override public Collection<V> values() {
        return Collections.unmodifiableCollection(new AbstractCollection<V>() {
            @Override public int size() { return size; }
            @Override public boolean contains(Object value) { return containsValue(value); }
            @Override public Iterator<V> iterator() { return new Cursor<>(root, VALUES); }
        });
    }

    /** @deprecated Persistent maps reject every destructive conditional operation. */
    @Deprecated
    @Override public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("immutable M3 persistent map");
    }

    /** @deprecated Persistent maps reject every destructive conditional operation. */
    @Deprecated
    @Override public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException("immutable M3 persistent map");
    }

    // This existing M3 hash-spreading atom is bijective over 32-bit hashes: equal
    // hash codes remain equal and no additional full-hash collisions are created.
    private static int hash(Object key) {
        int value = Objects.hashCode(key);
        return value ^ (value >>> 16);
    }

    private static int bit(int hash, int shift) {
        return 1 << ((hash >>> shift) & 31);
    }

    private static int rank(int bitmap, int bit) {
        return Integer.bitCount(bitmap & (bit - 1));
    }

    /** One immutable directory or full-hash collision group, never an entry node. */
    private static final class Node {
        private static final int COLLISION = 1;
        private final int flags;
        private final int dataMap;
        private final int nodeMap;
        private final int collisionHash;
        private final Object[] slots;

        private Node(int dataMap, int nodeMap, Object[] slots) {
            this.flags = 0;
            this.dataMap = dataMap;
            this.nodeMap = nodeMap;
            this.collisionHash = 0;
            this.slots = slots;
        }

        private Node(int hash, Object[] collisionPairs) {
            this.flags = COLLISION;
            this.dataMap = 0;
            this.nodeMap = 0;
            this.collisionHash = hash;
            this.slots = collisionPairs;
        }

        private boolean collision() { return (flags & COLLISION) != 0; }
        private int dataLength() { return collision() ? slots.length : 2 * Integer.bitCount(dataMap); }
        private boolean singleton() { return slots.length == 2 && (collision() || nodeMap == 0); }

        private static Node entry(Object key, Object value, int hash, int shift) {
            return new Node(bit(hash, shift), 0, new Object[] {key, value});
        }

        private Object find(Object key, int hash, int shift) {
            if (collision()) {
                if (hash != collisionHash) return ABSENT;
                for (int i = 0; i < slots.length; i += 2) {
                    if (Objects.equals(slots[i], key)) return slots[i + 1];
                }
                return ABSENT;
            }
            int bit = bit(hash, shift);
            if ((dataMap & bit) != 0) {
                int offset = 2 * rank(dataMap, bit);
                return Objects.equals(slots[offset], key) ? slots[offset + 1] : ABSENT;
            }
            if ((nodeMap & bit) == 0) return ABSENT;
            Node child = (Node) slots[dataLength() + rank(nodeMap, bit)];
            return child.find(key, hash, shift + 5);
        }

        private Node put(Object key, Object value, int hash, int shift) {
            if (collision()) {
                if (hash != collisionHash) return mergeCollision(this, key, value, hash, shift);
                for (int i = 0; i < slots.length; i += 2) {
                    if (Objects.equals(slots[i], key)) {
                        if (slots[i] == key && slots[i + 1] == value) return this;
                        Object[] copy = slots.clone();
                        copy[i] = key;
                        copy[i + 1] = value;
                        return new Node(hash, copy);
                    }
                }
                Object[] copy = Arrays.copyOf(slots, Math.addExact(slots.length, 2));
                copy[slots.length] = key;
                copy[slots.length + 1] = value;
                return new Node(hash, copy);
            }
            int bit = bit(hash, shift);
            if ((dataMap & bit) != 0) {
                int offset = 2 * rank(dataMap, bit);
                Object oldKey = slots[offset];
                Object oldValue = slots[offset + 1];
                if (Objects.equals(oldKey, key)) {
                    if (oldKey == key && oldValue == value) return this;
                    Object[] copy = slots.clone();
                    copy[offset] = key;
                    copy[offset + 1] = value;
                    return new Node(dataMap, nodeMap, copy);
                }
                Node child = mergeEntries(oldKey, oldValue, hash(oldKey), key, value, hash, shift + 5);
                return pairToChild(bit, offset, child);
            }
            if ((nodeMap & bit) != 0) {
                int offset = dataLength() + rank(nodeMap, bit);
                Node child = (Node) slots[offset];
                Node changed = child.put(key, value, hash, shift + 5);
                if (changed == child) return this;
                Object[] copy = slots.clone();
                copy[offset] = changed;
                return new Node(dataMap, nodeMap, copy);
            }
            int offset = 2 * rank(dataMap, bit);
            Object[] copy = new Object[Math.addExact(slots.length, 2)];
            System.arraycopy(slots, 0, copy, 0, offset);
            copy[offset] = key;
            copy[offset + 1] = value;
            System.arraycopy(slots, offset, copy, offset + 2, slots.length - offset);
            return new Node(dataMap | bit, nodeMap, copy);
        }

        /** Replace two inline slots with one child slot, preserving both rank orders. */
        private Node pairToChild(int bit, int offset, Node child) {
            int dataLength = dataLength();
            int childRank = rank(nodeMap, bit);
            int newDataLength = dataLength - 2;
            Object[] copy = new Object[slots.length - 1];
            System.arraycopy(slots, 0, copy, 0, offset);
            System.arraycopy(slots, offset + 2, copy, offset, dataLength - offset - 2);
            System.arraycopy(slots, dataLength, copy, newDataLength, childRank);
            copy[newDataLength + childRank] = child;
            System.arraycopy(slots, dataLength + childRank, copy, newDataLength + childRank + 1,
                    slots.length - dataLength - childRank);
            return new Node(dataMap ^ bit, nodeMap | bit, copy);
        }

        private Node remove(Object key, int hash, int shift) {
            if (collision()) {
                if (hash != collisionHash) return this;
                for (int i = 0; i < slots.length; i += 2) {
                    if (Objects.equals(slots[i], key)) {
                        return slots.length == 2 ? null : new Node(hash, without(slots, i, 2));
                    }
                }
                return this;
            }
            int bit = bit(hash, shift);
            if ((dataMap & bit) != 0) {
                int offset = 2 * rank(dataMap, bit);
                if (!Objects.equals(slots[offset], key)) return this;
                if (slots.length == 2) return null;
                return new Node(dataMap ^ bit, nodeMap, without(slots, offset, 2));
            }
            if ((nodeMap & bit) == 0) return this;
            int offset = dataLength() + rank(nodeMap, bit);
            Node child = (Node) slots[offset];
            Node changed = child.remove(key, hash, shift + 5);
            if (changed == child) return this;
            if (changed == null) {
                if (slots.length == 1) return null;
                return new Node(dataMap, nodeMap ^ bit, without(slots, offset, 1));
            }
            if (changed.singleton()) return childToPair(bit, offset, changed);
            Object[] copy = slots.clone();
            copy[offset] = changed;
            return new Node(dataMap, nodeMap, copy);
        }

        /** A sole surviving mapping can return to its parent's inline data plane. */
        private Node childToPair(int bit, int childOffset, Node child) {
            int dataLength = dataLength();
            int pairOffset = 2 * rank(dataMap, bit);
            int beforeChild = childOffset - dataLength;
            Object[] copy = new Object[slots.length + 1];
            System.arraycopy(slots, 0, copy, 0, pairOffset);
            copy[pairOffset] = child.slots[0];
            copy[pairOffset + 1] = child.slots[1];
            System.arraycopy(slots, pairOffset, copy, pairOffset + 2, dataLength - pairOffset);
            System.arraycopy(slots, dataLength, copy, dataLength + 2, beforeChild);
            System.arraycopy(slots, childOffset + 1, copy, dataLength + 2 + beforeChild,
                    slots.length - childOffset - 1);
            return new Node(dataMap | bit, nodeMap ^ bit, copy);
        }

        private static Object[] without(Object[] source, int offset, int count) {
            Object[] copy = new Object[source.length - count];
            System.arraycopy(source, 0, copy, 0, offset);
            System.arraycopy(source, offset + count, copy, offset, source.length - offset - count);
            return copy;
        }

        private static Node mergeEntries(Object key1, Object value1, int hash1,
                Object key2, Object value2, int hash2, int shift) {
            if (hash1 == hash2) return new Node(hash1, new Object[] {key1, value1, key2, value2});
            int bit1 = bit(hash1, shift);
            int bit2 = bit(hash2, shift);
            if (bit1 != bit2) {
                Object[] pairs = Integer.numberOfTrailingZeros(bit1) < Integer.numberOfTrailingZeros(bit2)
                        ? new Object[] {key1, value1, key2, value2}
                        : new Object[] {key2, value2, key1, value1};
                return new Node(bit1 | bit2, 0, pairs);
            }
            Node child = mergeEntries(key1, value1, hash1, key2, value2, hash2, shift + 5);
            return new Node(0, bit1, new Object[] {child});
        }

        /** A different hash branches above an already packed equal-hash group. */
        private static Node mergeCollision(Node collision, Object key, Object value, int hash, int shift) {
            int previousBit = bit(collision.collisionHash, shift);
            int incomingBit = bit(hash, shift);
            if (previousBit != incomingBit) {
                return new Node(incomingBit, previousBit, new Object[] {key, value, collision});
            }
            Node child = mergeCollision(collision, key, value, hash, shift + 5);
            return new Node(0, previousBit, new Object[] {child});
        }

        @SuppressWarnings("unchecked")
        private <K, V> void forEach(BiConsumer<? super K, ? super V> action) {
            int dataLength = dataLength();
            for (int i = 0; i < dataLength; i += 2) action.accept((K) slots[i], (V) slots[i + 1]);
            for (int i = dataLength; i < slots.length; i++) ((Node) slots[i]).forEach(action);
        }
    }

    /** One bounded cursor stack; only the entry mode creates caller Entry projections. */
    private static final class Cursor<T> implements Iterator<T> {
        private final Node[] nodes = new Node[8];
        private final int[] offsets = new int[8];
        private final int mode;
        private int depth;
        private boolean ready;
        private Object key;
        private Object value;

        private Cursor(Node root, int mode) {
            this.mode = mode;
            if (root != null) {
                nodes[0] = root;
                depth = 1;
            }
        }

        @Override public boolean hasNext() {
            if (ready) return true;
            while (depth != 0) {
                int level = depth - 1;
                Node node = nodes[level];
                int offset = offsets[level];
                int dataLength = node.dataLength();
                if (offset < dataLength) {
                    key = node.slots[offset];
                    value = node.slots[offset + 1];
                    offsets[level] = offset + 2;
                    ready = true;
                    return true;
                }
                if (offset < node.slots.length) {
                    offsets[level] = offset + 1;
                    if (depth == nodes.length) throw new IllegalStateException("invalid hash directory depth");
                    nodes[depth] = (Node) node.slots[offset];
                    offsets[depth] = 0;
                    depth++;
                } else {
                    nodes[level] = null;
                    depth--;
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            Object result = switch (mode) {
                case KEYS -> key;
                case VALUES -> value;
                default -> new AbstractMap.SimpleImmutableEntry<>(key, value);
            };
            ready = false;
            key = null;
            value = null;
            return (T) result;
        }

        @Override public void remove() {
            throw new UnsupportedOperationException("immutable M3 persistent map iterator");
        }
    }

    @Serial private Object writeReplace() {
        return new SerializationProxy(this);
    }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // Payload serializability is checked by ObjectOutputStream.
        @SuppressWarnings("serial")
        private final Object[] pairs;

        private SerializationProxy(M3PMap<?, ?> map) {
            pairs = new Object[Math.multiplyExact(map.size, 2)];
            int[] offset = {0};
            map.forEach((key, value) -> {
                pairs[offset[0]++] = key;
                pairs[offset[0]++] = value;
            });
        }

        @Serial private Object readResolve() throws ObjectStreamException {
            if (pairs == null || (pairs.length & 1) != 0) {
                throw new InvalidObjectException("invalid persistent map pairs");
            }
            M3PMap<Object, Object> map = M3PMap.empty();
            for (int i = 0; i < pairs.length; i += 2) {
                M3PMap<Object, Object> changed = map.plus(pairs[i], pairs[i + 1]);
                if (changed.size != map.size + 1) {
                    throw new InvalidObjectException("duplicate persistent map key");
                }
                map = changed;
            }
            return map;
        }
    }
}
