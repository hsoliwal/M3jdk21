// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * One-to-one relation over the existing MIndexMap primitive ID lanes.
 *
 * <p>The forward map owns key-to-value IDs; the reverse map is its lookup index.
 * No entry, node or payload reference is retained per mapping. Both domains
 * retain their existing identity authority. Instances and live inverse views
 * require external synchronization. An inverse view adds one facade object,
 * never a copied payload or one object per mapping.
 */
public final class MIndexBiMap<K, V> {
    private final MIndexMap<K, V> forward;
    private final MIndexMap<V, K> reverse;

    public MIndexBiMap(MIndexSpace<K> keys, MIndexSpace<V> values) {
        this(keys, values, 16);
    }

    public MIndexBiMap(MIndexSpace<K> keys, MIndexSpace<V> values, int expectedSize) {
        forward = new MIndexMap<>(keys, values, expectedSize);
        reverse = new MIndexMap<>(values, keys, expectedSize);
    }

    private MIndexBiMap(MIndexMap<K, V> forward, MIndexMap<V, K> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public MIndexSpace<K> keySpace() { return forward.keySpace(); }
    public MIndexSpace<V> valueSpace() { return forward.valueSpace(); }
    public int size() { return forward.size(); }
    public boolean isEmpty() { return forward.isEmpty(); }
    public boolean containsKey(K key) { return forward.containsKey(key); }
    public boolean containsKeyId(int keyId) { return forward.containsKeyId(keyId); }
    public boolean containsValue(V value) { return reverse.containsKey(value); }
    public boolean containsValueId(int valueId) { return reverse.containsKeyId(valueId); }
    public V get(K key) { return forward.get(key); }
    public K keyForValue(V value) { return reverse.get(value); }
    public int valueIdOrDefault(int keyId, int fallback) { return forward.valueIdOrDefault(keyId, fallback); }
    public int keyIdOrDefault(int valueId, int fallback) { return reverse.valueIdOrDefault(valueId, fallback); }

    /**
     * Replace a key's value, rejecting a value owned by another key before mutation.
     * Returns true only for a newly inserted key, like MIndexMap.put.
     * Domain admission performed by the value overload is not rolled back on rejection.
     */
    public boolean put(K key, V value) {
        return putIds(keySpace().id(key), valueSpace().id(value));
    }

    public boolean putIds(int keyId, int valueId) {
        return putIds(keyId, valueId, false);
    }

    /** Explicitly evict a conflicting key, then insert/replace this key's mapping. */
    public boolean forcePut(K key, V value) {
        return forcePutIds(keySpace().id(key), valueSpace().id(value));
    }

    public boolean forcePutIds(int keyId, int valueId) {
        return putIds(keyId, valueId, true);
    }

    private boolean putIds(int keyId, int valueId, boolean force) {
        keySpace().requireId(keyId);
        valueSpace().requireId(valueId);
        int previous = forward.valueIdOrDefault(keyId, -1);
        if (previous == valueId) return false;
        int owner = reverse.valueIdOrDefault(valueId, -1);
        if (owner >= 0 && owner != keyId && !force) {
            throw new IllegalArgumentException("value already belongs to another key");
        }
        if (owner >= 0 && owner != keyId) {
            forward.removeKeyId(owner);
            reverse.removeKeyId(valueId);
        }
        if (previous >= 0) reverse.removeKeyId(previous);
        forward.putIds(keyId, valueId);
        reverse.putIds(valueId, keyId);
        return previous < 0;
    }

    /** An absent key never matches an expected sentinel. No mutation occurs on a mismatch. */
    public boolean replaceIds(int keyId, int expectedValueId, int newValueId) {
        valueSpace().requireId(newValueId);
        int previous = forward.valueIdOrDefault(keyId, -1);
        if (previous < 0 || previous != expectedValueId) return false;
        putIds(keyId, newValueId);
        return true;
    }

    public boolean remove(K key) {
        return removeKeyId(keySpace().findId(key));
    }

    public boolean removeKeyId(int keyId) {
        int valueId = forward.valueIdOrDefault(keyId, -1);
        if (valueId < 0) return false;
        forward.removeKeyId(keyId);
        reverse.removeKeyId(valueId);
        return true;
    }

    public boolean removeIds(int keyId, int expectedValueId) {
        int valueId = forward.valueIdOrDefault(keyId, -1);
        return valueId >= 0 && valueId == expectedValueId && removeKeyId(keyId);
    }

    /** Live inverse sharing both maps; view reference identity is not promised. */
    public MIndexBiMap<V, K> inverse() {
        return new MIndexBiMap<>(reverse, forward);
    }

    public void forEachId(MIndexMap.IdEntryConsumer action) {
        forward.forEachId(Objects.requireNonNull(action, "action"));
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        forward.forEach(Objects.requireNonNull(action, "action"));
    }

    public int[] snapshotSortedLane() { return forward.snapshotSortedLane(); }

    /** Freeze the forward relation with existing map identity, without inventing a new kind. */
    public MIndexFrozenMap<K, V> freeze(MIndexCompositeIndex index) {
        return forward.freeze(index);
    }

    public void clear() { forward.clear(); reverse.clear(); }
    public void compact() { forward.compact(); reverse.compact(); }
}
