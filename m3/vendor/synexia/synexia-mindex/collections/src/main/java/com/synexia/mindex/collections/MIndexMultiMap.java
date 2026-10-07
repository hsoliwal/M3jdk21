// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * One-to-many map with primitive key IDs and primitive linked value slots.
 *
 * <p>Each distinct key occupies one open-addressed primitive bucket. Values
 * occupy reusable primitive edge slots; there are no per-entry node objects.
 */
public final class MIndexMultiMap<K, V> {
    private final MIndexSpace<K> keySpace;
    private final MIndexSpace<V> valueSpace;
    private int[] keys;
    private int[] heads;
    private int keyCount;

    private int[] valueIds;
    private int[] next;
    private int edgeHighWater;
    private int liveEdges;
    private int free = -1;

    public MIndexMultiMap(MIndexSpace<K> keySpace, MIndexSpace<V> valueSpace) {
        this(keySpace, valueSpace, 16);
    }

    public MIndexMultiMap(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            int expectedKeys) {
        this.keySpace = Objects.requireNonNull(keySpace, "keySpace");
        this.valueSpace = Objects.requireNonNull(valueSpace, "valueSpace");
        int capacity = IdSupport.tableCapacity(expectedKeys);
        keys = new int[capacity];
        heads = new int[capacity];
        Arrays.fill(heads, -1);
        int edgeCapacity = Math.max(8, expectedKeys);
        valueIds = new int[edgeCapacity];
        next = new int[edgeCapacity];
        Arrays.fill(next, -1);
    }

    public MIndexSpace<K> keySpace() {
        return keySpace;
    }

    public MIndexSpace<V> valueSpace() {
        return valueSpace;
    }

    public int keyCount() {
        return keyCount;
    }

    public int size() {
        return liveEdges;
    }

    public boolean isEmpty() {
        return liveEdges == 0;
    }

    public boolean add(K key, V value) {
        return addIds(keySpace.id(key), valueSpace.id(value));
    }

    public boolean addIds(int keyId, int valueId) {
        keySpace.requireId(keyId);
        valueSpace.requireId(valueId);
        ensureKeyCapacity(keyCount + 1);
        int slot = keySlotOrVacant(keyId);
        if (keys[slot] == 0) {
            keys[slot] = keyId + 1;
            heads[slot] = -1;
            keyCount++;
        } else {
            for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
                if (valueIds[edge] == valueId) {
                    return false;
                }
            }
        }
        int edge = allocateEdge();
        valueIds[edge] = valueId;
        next[edge] = heads[slot];
        heads[slot] = edge;
        liveEdges++;
        return true;
    }

    /** Add a validated value slice. Unique pairs retain the existing newest-first order. */
    public int addAllIds(int keyId, int[] source, int offset, int length) {
        keySpace.requireId(keyId);
        IdSupport.requireIds(valueSpace, source, offset, length);
        int previous = liveEdges;
        for (int end = offset + length; offset < end; offset++) addIds(keyId, source[offset]);
        return liveEdges - previous;
    }

    public boolean containsIds(int keyId, int valueId) {
        int slot = findKeySlot(keyId);
        if (slot < 0 || valueId < 0) return false;
        for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
            if (valueIds[edge] == valueId) return true;
        }
        return false;
    }

    public int valueCountId(int keyId) {
        int slot = findKeySlot(keyId);
        if (slot < 0) return 0;
        int count = 0;
        for (int edge = heads[slot]; edge >= 0; edge = next[edge]) count++;
        return count;
    }

    /** Caller-owned export in the same newest-first order as forEachValueId. */
    public int[] snapshotValueIds(int keyId) {
        int[] result = new int[valueCountId(keyId)];
        int slot = findKeySlot(keyId);
        int cursor = 0;
        if (slot >= 0) {
            for (int edge = heads[slot]; edge >= 0; edge = next[edge]) result[cursor++] = valueIds[edge];
        }
        return result;
    }

    /** Direct primitive traversal. Cross-key order is unspecified; do not mutate in callbacks. */
    public void forEachId(MIndexMap.IdEntryConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = liveEdges;
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] == 0) continue;
            int keyId = keys[slot] - 1;
            for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
                action.accept(keyId, valueIds[edge]);
                if (liveEdges != expected) throw new IllegalStateException("collection modified during traversal");
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, 0);
        Arrays.fill(heads, -1);
        Arrays.fill(valueIds, 0, edgeHighWater, 0);
        Arrays.fill(next, 0, edgeHighWater, -1);
        keyCount = 0;
        edgeHighWater = 0;
        liveEdges = 0;
        free = -1;
    }

    /** Release deleted slots and excess probe capacity, preserving each key's value order. */
    public void compact() {
        int capacity = Math.min(keys.length, IdSupport.tableCapacity(keyCount));
        int edgeCapacity = Math.max(8, liveEdges);
        if (capacity >= keys.length && edgeCapacity >= valueIds.length && free < 0) return;
        int[] packedKeys = new int[capacity];
        int[] packedHeads = new int[capacity];
        Arrays.fill(packedHeads, -1);
        int[] packedValues = new int[edgeCapacity];
        int[] packedNext = new int[edgeCapacity];
        Arrays.fill(packedNext, -1);
        int cursor = 0;
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] == 0) continue;
            int target = IdSupport.mix(keys[slot] - 1) & (capacity - 1);
            while (packedKeys[target] != 0) target = (target + 1) & (capacity - 1);
            packedKeys[target] = keys[slot];
            packedHeads[target] = cursor;
            for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
                packedValues[cursor] = valueIds[edge];
                if (next[edge] >= 0) packedNext[cursor] = cursor + 1;
                cursor++;
            }
        }
        if (cursor != liveEdges) throw new AssertionError("multimap compaction size mismatch");
        keys = packedKeys;
        heads = packedHeads;
        valueIds = packedValues;
        next = packedNext;
        edgeHighWater = liveEdges;
        free = -1;
    }

    public boolean containsKey(K key) {
        int id = keySpace.findId(key);
        return id >= 0 && findKeySlot(id) >= 0;
    }

    public boolean containsEntry(K key, V value) {
        int keyId = keySpace.findId(key);
        int valueId = valueSpace.findId(value);
        if (keyId < 0 || valueId < 0) {
            return false;
        }
        int slot = findKeySlot(keyId);
        if (slot < 0) {
            return false;
        }
        for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
            if (valueIds[edge] == valueId) {
                return true;
            }
        }
        return false;
    }

    public int valueCount(K key) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return 0;
        }
        int slot = findKeySlot(keyId);
        if (slot < 0) {
            return 0;
        }
        int count = 0;
        for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
            count++;
        }
        return count;
    }

    public void forEachValue(K key, Consumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return;
        }
        forEachValueId(keyId, id -> action.accept(valueSpace.value(id)));
    }

    public void forEachValueId(int keyId, IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int slot = findKeySlot(keyId);
        if (slot < 0) {
            return;
        }
        int expected = liveEdges;
        for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
            action.accept(valueIds[edge]);
            if (liveEdges != expected) {
                throw new IllegalStateException("collection modified during traversal");
            }
        }
    }

    public int[] snapshotSortedLane() {
        long[] packed = new long[liveEdges];
        int cursor = 0;
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] == 0) {
                continue;
            }
            int keyId = keys[slot] - 1;
            for (int edge = heads[slot]; edge >= 0; edge = next[edge]) {
                packed[cursor++] =
                        (Integer.toUnsignedLong(keyId) << 32)
                                | Integer.toUnsignedLong(valueIds[edge]);
            }
        }
        if (cursor != liveEdges) {
            throw new AssertionError("multimap edge count mismatch");
        }
        Arrays.sort(packed);
        int[] lane = new int[liveEdges << 1];
        for (int entry = 0; entry < packed.length; entry++) {
            lane[entry << 1] = (int) (packed[entry] >>> 32);
            lane[(entry << 1) + 1] = (int) packed[entry];
        }
        return lane;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(
                        MIndexCompositeIndex.KIND_MULTI_MAP,
                        keySpace,
                        valueSpace,
                        snapshotSortedLane());
    }

    public MIndexFrozenMultiMap<K, V> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenMultiMap.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    public boolean remove(K key, V value) {
        int keyId = keySpace.findId(key);
        int valueId = valueSpace.findId(value);
        return keyId >= 0 && valueId >= 0 && removeIds(keyId, valueId);
    }

    public boolean removeIds(int keyId, int valueId) {
        int slot = findKeySlot(keyId);
        if (slot < 0) {
            return false;
        }
        int previous = -1;
        int edge = heads[slot];
        while (edge >= 0) {
            if (valueIds[edge] == valueId) {
                int following = next[edge];
                if (previous < 0) {
                    heads[slot] = following;
                } else {
                    next[previous] = following;
                }
                releaseEdge(edge);
                liveEdges--;
                if (heads[slot] < 0) {
                    deleteKeySlot(slot);
                }
                return true;
            }
            previous = edge;
            edge = next[edge];
        }
        return false;
    }

    public int removeAll(K key) {
        int keyId = keySpace.findId(key);
        return keyId < 0 ? 0 : removeAllId(keyId);
    }

    public int removeAllId(int keyId) {
        int slot = findKeySlot(keyId);
        if (slot < 0) {
            return 0;
        }
        int removed = 0;
        int edge = heads[slot];
        while (edge >= 0) {
            int following = next[edge];
            releaseEdge(edge);
            removed++;
            edge = following;
        }
        liveEdges -= removed;
        deleteKeySlot(slot);
        return removed;
    }

    private int allocateEdge() {
        if (free >= 0) {
            int edge = free;
            free = next[edge];
            next[edge] = -1;
            return edge;
        }
        if (edgeHighWater == valueIds.length) {
            int capacity = IdSupport.grown(valueIds.length, edgeHighWater + 1);
            valueIds = Arrays.copyOf(valueIds, capacity);
            int old = next.length;
            next = Arrays.copyOf(next, capacity);
            Arrays.fill(next, old, capacity, -1);
        }
        return edgeHighWater++;
    }

    private void releaseEdge(int edge) {
        valueIds[edge] = 0;
        next[edge] = free;
        free = edge;
    }

    private int findKeySlot(int keyId) {
        int mask = keys.length - 1;
        int slot = IdSupport.mix(keyId) & mask;
        while (keys[slot] != 0) {
            if (keys[slot] - 1 == keyId) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private int keySlotOrVacant(int keyId) {
        int mask = keys.length - 1;
        int slot = IdSupport.mix(keyId) & mask;
        while (keys[slot] != 0 && keys[slot] - 1 != keyId) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private void deleteKeySlot(int slot) {
        int mask = keys.length - 1;
        keys[slot] = 0;
        heads[slot] = -1;
        keyCount--;
        int scan = (slot + 1) & mask;
        while (keys[scan] != 0) {
            int encodedKey = keys[scan];
            int head = heads[scan];
            keys[scan] = 0;
            heads[scan] = -1;
            keyCount--;
            int destination = keySlotOrVacant(encodedKey - 1);
            keys[destination] = encodedKey;
            heads[destination] = head;
            keyCount++;
            scan = (scan + 1) & mask;
        }
    }

    private void ensureKeyCapacity(int needed) {
        if ((long) needed * 3L < (long) keys.length * 2L) {
            return;
        }
        int[] oldKeys = keys;
        int[] oldHeads = heads;
        keys = new int[oldKeys.length << 1];
        heads = new int[keys.length];
        Arrays.fill(heads, -1);
        int previous = keyCount;
        keyCount = 0;
        for (int slot = 0; slot < oldKeys.length; slot++) {
            if (oldKeys[slot] != 0) {
                int destination = keySlotOrVacant(oldKeys[slot] - 1);
                keys[destination] = oldKeys[slot];
                heads[destination] = oldHeads[slot];
                keyCount++;
            }
        }
        if (keyCount != previous) {
            throw new AssertionError("key rehash size mismatch");
        }
    }
}
