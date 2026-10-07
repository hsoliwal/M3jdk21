// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Key/value collection retaining only primitive MIndex IDs.
 *
 * <p>Sparse mode uses parallel open-addressed key/value lanes. Dense mode uses
 * direct key-ID addressing plus one presence bit per possible key and
 * transparently promotes to sparse mode if a key exceeds the declared bound.
 */
public final class MIndexMap<K, V> {
    @FunctionalInterface
    public interface IdEntryConsumer {
        void accept(int keyId, int valueId);
    }

    private final MIndexSpace<K> keySpace;
    private final MIndexSpace<V> valueSpace;
    private int[] keys;
    private int[] values;
    private long[] densePresence;
    private int denseUpperBound;
    private int size;

    public MIndexMap(MIndexSpace<K> keySpace, MIndexSpace<V> valueSpace) {
        this(keySpace, valueSpace, 16);
    }

    public MIndexMap(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            int expectedSize) {
        this(keySpace, valueSpace, expectedSize, 0);
    }

    MIndexMap(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            int expectedSize,
            int denseUpperBound) {
        this.keySpace = Objects.requireNonNull(keySpace, "keySpace");
        this.valueSpace = Objects.requireNonNull(valueSpace, "valueSpace");
        if (expectedSize < 0 || denseUpperBound < 0) {
            throw new IllegalArgumentException("negative size/bound");
        }
        if (denseUpperBound > 0) {
            this.denseUpperBound = denseUpperBound;
            keys = new int[0];
            values = new int[denseUpperBound];
            int words = (int) ((denseUpperBound + 63L) >>> 6);
            densePresence = new long[words];
        } else {
            int capacity = IdSupport.tableCapacity(expectedSize);
            keys = new int[capacity];
            values = new int[capacity];
        }
    }

    public static <K, V> MIndexMap<K, V> dense(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            int upperKeyIdExclusive) {
        if (upperKeyIdExclusive <= 0) {
            throw new IllegalArgumentException(
                    "upperKeyIdExclusive must be positive");
        }
        return new MIndexMap<>(
                keySpace, valueSpace, 0, upperKeyIdExclusive);
    }

    public MIndexSpace<K> keySpace() {
        return keySpace;
    }

    public MIndexSpace<V> valueSpace() {
        return valueSpace;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isDense() {
        return densePresence != null;
    }

    public boolean put(K key, V value) {
        return putIds(keySpace.id(key), valueSpace.id(value));
    }

    public boolean putIds(int keyId, int valueId) {
        keySpace.requireId(keyId);
        valueSpace.requireId(valueId);
        if (densePresence != null) {
            if (keyId < denseUpperBound) {
                int word = keyId >>> 6;
                long bit = 1L << (keyId & 63);
                boolean absent = (densePresence[word] & bit) == 0L;
                values[keyId] = valueId;
                if (absent) {
                    densePresence[word] |= bit;
                    size++;
                }
                return absent;
            }
            promoteToSparse();
        }
        ensureSparseCapacity(size + 1);
        return putIdsNoGrow(keyId, valueId);
    }

    /** Primitive analogue of putIfAbsent. Returns the previous ID or -1 if inserted. */
    public int putIfAbsentIds(int keyId, int valueId) {
        keySpace.requireId(keyId);
        valueSpace.requireId(valueId);
        int previous = valueIdOrDefault(keyId, -1);
        if (previous < 0) putIds(keyId, valueId);
        return previous;
    }

    /** Conditional ID replacement; an absent key never matches a sentinel expected ID. */
    public boolean replaceIds(int keyId, int expectedValueId, int newValueId) {
        valueSpace.requireId(newValueId);
        int previous = valueIdOrDefault(keyId, -1);
        if (previous < 0 || previous != expectedValueId) return false;
        putIds(keyId, newValueId);
        return true;
    }

    public boolean removeIds(int keyId, int expectedValueId) {
        int previous = valueIdOrDefault(keyId, -1);
        return previous >= 0 && previous == expectedValueId && removeKeyId(keyId);
    }

    /** Parallel caller-owned slices; repeated keys use the last value. Returns new key count. */
    public int putAllIds(int[] keyIds, int keyOffset, int[] valueIds, int valueOffset, int length) {
        IdSupport.requireIds(keySpace, keyIds, keyOffset, length);
        IdSupport.requireIds(valueSpace, valueIds, valueOffset, length);
        int previous = size;
        for (int index = 0; index < length; index++) {
            putIds(keyIds[keyOffset + index], valueIds[valueOffset + index]);
        }
        return size - previous;
    }

    /** Direct lane scan: no Map.Entry, payload projection or temporary collection. */
    public boolean containsValueId(int valueId) {
        if (valueId < 0) return false;
        if (densePresence != null) {
            for (int wordIndex = 0; wordIndex < densePresence.length; wordIndex++) {
                long word = densePresence[wordIndex];
                while (word != 0L) {
                    int keyId = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                    if (values[keyId] == valueId) return true;
                    word &= word - 1L;
                }
            }
        } else {
            for (int slot = 0; slot < keys.length; slot++) {
                if (keys[slot] != 0 && values[slot] == valueId) return true;
            }
        }
        return false;
    }

    public boolean containsKey(K key) {
        int id = keySpace.findId(key);
        return id >= 0 && containsKeyId(id);
    }

    public boolean containsKeyId(int keyId) {
        if (keyId < 0) {
            return false;
        }
        if (densePresence != null) {
            return keyId < denseUpperBound && denseContains(keyId);
        }
        return findSparseSlot(keyId) >= 0;
    }

    public V getOrDefault(K key, V fallback) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return fallback;
        }
        int valueId = valueIdOrDefault(keyId, -1);
        return valueId < 0 ? fallback : valueSpace.value(valueId);
    }

    public int valueIdOrDefault(int keyId, int fallbackId) {
        if (keyId < 0) {
            return fallbackId;
        }
        if (densePresence != null) {
            return keyId < denseUpperBound && denseContains(keyId)
                    ? values[keyId]
                    : fallbackId;
        }
        int slot = findSparseSlot(keyId);
        return slot < 0 ? fallbackId : values[slot];
    }

    public V get(K key) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return null;
        }
        int valueId = valueIdOrDefault(keyId, -1);
        return valueId < 0 ? null : valueSpace.value(valueId);
    }

    public boolean remove(K key) {
        int keyId = keySpace.findId(key);
        return keyId >= 0 && removeKeyId(keyId);
    }

    public boolean removeKeyId(int keyId) {
        if (keyId < 0) {
            return false;
        }
        if (densePresence != null) {
            if (keyId >= denseUpperBound || !denseContains(keyId)) {
                return false;
            }
            int word = keyId >>> 6;
            densePresence[word] &= ~(1L << (keyId & 63));
            values[keyId] = 0;
            size--;
            return true;
        }
        int slot = findSparseSlot(keyId);
        if (slot < 0) {
            return false;
        }
        deleteSparseSlot(slot);
        return true;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        forEachId((keyId, valueId) ->
                action.accept(
                        keySpace.value(keyId),
                        valueSpace.value(valueId)));
    }

    public void forEachId(IdEntryConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = size;
        if (densePresence != null) {
            for (int wordIndex = 0;
                    wordIndex < densePresence.length;
                    wordIndex++) {
                long word = densePresence[wordIndex];
                while (word != 0L) {
                    int bit = Long.numberOfTrailingZeros(word);
                    int keyId = (wordIndex << 6) + bit;
                    action.accept(keyId, values[keyId]);
                    if (size != expected) {
                        throw new IllegalStateException(
                                "collection modified during traversal");
                    }
                    word &= word - 1L;
                }
            }
            return;
        }
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] != 0) {
                action.accept(keys[slot] - 1, values[slot]);
                if (size != expected) {
                    throw new IllegalStateException(
                            "collection modified during traversal");
                }
            }
        }
    }

    public int[] snapshotSortedLane() {
        if (densePresence != null) {
            int[] lane = new int[size << 1];
            int[] cursor = {0};
            forEachId((keyId, valueId) -> {
                int offset = cursor[0] << 1;
                lane[offset] = keyId;
                lane[offset + 1] = valueId;
                cursor[0]++;
            });
            return lane;
        }

        int[] keyLane = new int[size];
        int[] valueLane = new int[size];
        int cursor = 0;
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] != 0) {
                keyLane[cursor] = keys[slot] - 1;
                valueLane[cursor] = values[slot];
                cursor++;
            }
        }
        IdSupport.sortPairsByFirst(keyLane, valueLane, cursor);
        int[] lane = new int[cursor << 1];
        for (int index = 0; index < cursor; index++) {
            lane[index << 1] = keyLane[index];
            lane[(index << 1) + 1] = valueLane[index];
        }
        return lane;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(
                        MIndexCompositeIndex.KIND_MAP,
                        keySpace,
                        valueSpace,
                        snapshotSortedLane());
    }

    public MIndexFrozenMap<K, V> freeze(
            MIndexCompositeIndex index) {
        return MIndexFrozenMap.copyOf(
                this,
                Objects.requireNonNull(index, "index"));
    }

    public void clear() {
        if (densePresence != null) {
            Arrays.fill(densePresence, 0L);
            Arrays.fill(values, 0);
        } else {
            Arrays.fill(keys, 0);
            Arrays.fill(values, 0);
        }
        size = 0;
    }

    /** Shrink sparse probe storage after a long-lived workload has contracted. */
    public void compact() {
        if (densePresence == null) {
            int target = IdSupport.tableCapacity(size);
            if (target < keys.length) {
                rehashSparse(target);
            }
        }
    }

    int sparseCapacity() {
        return densePresence == null ? keys.length : 0;
    }

    private boolean denseContains(int keyId) {
        return (densePresence[keyId >>> 6]
                & (1L << (keyId & 63))) != 0L;
    }

    private int findSparseSlot(int keyId) {
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

    private boolean putIdsNoGrow(int keyId, int valueId) {
        int mask = keys.length - 1;
        int slot = IdSupport.mix(keyId) & mask;
        while (keys[slot] != 0) {
            if (keys[slot] - 1 == keyId) {
                values[slot] = valueId;
                return false;
            }
            slot = (slot + 1) & mask;
        }
        keys[slot] = keyId + 1;
        values[slot] = valueId;
        size++;
        return true;
    }

    private void deleteSparseSlot(int slot) {
        shiftSparseKeys(slot);
        size--;
    }

    private void shiftSparseKeys(int position) {
        int mask = keys.length - 1;
        int last;
        int encoded;
        while (true) {
            position = ((last = position) + 1) & mask;
            while ((encoded = keys[position]) != 0) {
                int home = IdSupport.mix(encoded - 1) & mask;
                if (last <= position
                        ? last >= home || home > position
                        : last >= home && home > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (encoded == 0) {
                keys[last] = 0;
                values[last] = 0;
                return;
            }
            keys[last] = encoded;
            values[last] = values[position];
        }
    }

    private void promoteToSparse() {
        long[] previousPresence = densePresence;
        int[] previousValues = values;
        int previousSize = size;

        keys = new int[IdSupport.tableCapacity(
                Math.max(8, previousSize))];
        values = new int[keys.length];
        densePresence = null;
        denseUpperBound = 0;
        size = 0;

        for (int wordIndex = 0;
                wordIndex < previousPresence.length;
                wordIndex++) {
            long word = previousPresence[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                int keyId = (wordIndex << 6) + bit;
                putIdsNoGrow(keyId, previousValues[keyId]);
                word &= word - 1L;
            }
        }
        if (size != previousSize) {
            throw new AssertionError("dense promotion size mismatch");
        }
    }

    private void ensureSparseCapacity(int needed) {
        if ((long) needed * 3L < (long) keys.length * 2L) {
            return;
        }
        rehashSparse(keys.length << 1);
    }

    private void rehashSparse(int requested) {
        int[] oldKeys = keys;
        int[] oldValues = values;
        int capacity = 8;
        while (capacity < requested) {
            capacity <<= 1;
        }
        keys = new int[capacity];
        values = new int[capacity];
        int previousSize = size;
        size = 0;
        for (int slot = 0; slot < oldKeys.length; slot++) {
            if (oldKeys[slot] != 0) {
                putIdsNoGrow(
                        oldKeys[slot] - 1,
                        oldValues[slot]);
            }
        }
        if (size != previousSize) {
            throw new AssertionError("rehash size mismatch");
        }
    }
}
