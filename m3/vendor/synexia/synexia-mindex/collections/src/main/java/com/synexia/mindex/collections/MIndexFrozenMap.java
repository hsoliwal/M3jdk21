// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Immutable canonical map backed by sorted key/value primitive pairs. */
public final class MIndexFrozenMap<K, V> implements MIndexCanonicalCollection {
    private final MIndexSpace<K> keySpace;
    private final MIndexSpace<V> valueSpace;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenMap(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.keySpace = Objects.requireNonNull(keySpace, "keySpace");
        this.valueSpace = Objects.requireNonNull(valueSpace, "valueSpace");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <K, V> MIndexFrozenMap<K, V> copyOf(
            MIndexMap<K, V> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return fromSortedLane(
                source.keySpace(),
                source.valueSpace(),
                index,
                source.snapshotSortedLane());
    }

    public static <K, V> MIndexFrozenMap<K, V> ofIds(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            MIndexCompositeIndex index,
            int[] keyValueLane) {
        Objects.requireNonNull(keySpace, "keySpace");
        Objects.requireNonNull(valueSpace, "valueSpace");
        int[] lane = Objects.requireNonNull(keyValueLane, "keyValueLane").clone();
        if ((lane.length & 1) != 0) {
            throw new IllegalArgumentException("map lane must contain key/value pairs");
        }
        int entries = lane.length >>> 1;
        int[] keys = new int[entries];
        int[] values = new int[entries];
        for (int entry = 0; entry < entries; entry++) {
            int keyId = lane[entry << 1];
            int valueId = lane[(entry << 1) + 1];
            keySpace.requireId(keyId);
            valueSpace.requireId(valueId);
            keys[entry] = keyId;
            values[entry] = valueId;
        }
        IdSupport.sortPairsByFirst(keys, values, entries);
        for (int entry = 1; entry < entries; entry++) {
            if (keys[entry - 1] == keys[entry]) {
                throw new IllegalArgumentException("duplicate key ID: " + keys[entry]);
            }
        }
        for (int entry = 0; entry < entries; entry++) {
            lane[entry << 1] = keys[entry];
            lane[(entry << 1) + 1] = values[entry];
        }
        return fromSortedLane(keySpace, valueSpace, index, lane);
    }

    private static <K, V> MIndexFrozenMap<K, V> fromSortedLane(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            MIndexCompositeIndex index,
            int[] lane) {
        Objects.requireNonNull(index, "index");
        int canonicalId = index.intern(
                MIndexCompositeIndex.KIND_MAP,
                keySpace,
                valueSpace,
                lane);
        return new MIndexFrozenMap<>(keySpace, valueSpace, index, canonicalId);
    }

    public MIndexSpace<K> keySpace() {
        return keySpace;
    }

    public MIndexSpace<V> valueSpace() {
        return valueSpace;
    }

    @Override
    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int canonicalId() {
        return canonicalId;
    }

    @Override
    public int kind() {
        return MIndexCompositeIndex.KIND_MAP;
    }

    @Override
    public int size() {
        return index.length(canonicalId) >>> 1;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(K key) {
        int keyId = keySpace.findId(key);
        return keyId >= 0 && findEntry(keyId) >= 0;
    }

    public V get(K key) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return null;
        }
        int entry = findEntry(keyId);
        return entry < 0 ? null : valueSpace.value(valueIdAtEntry(entry));
    }

    public int valueIdOrDefault(int keyId, int fallbackId) {
        int entry = findEntry(keyId);
        return entry < 0 ? fallbackId : valueIdAtEntry(entry);
    }

    public MIndexFrozenMap<K, V> with(K key, V value) {
        return withIds(keySpace.id(key), valueSpace.id(value));
    }

    public MIndexFrozenMap<K, V> withIds(int keyId, int valueId) {
        keySpace.requireId(keyId);
        valueSpace.requireId(valueId);
        int entry = findEntry(keyId);
        int[] source = copyLane();
        if (entry >= 0) {
            int valuePosition = (entry << 1) + 1;
            if (source[valuePosition] == valueId) {
                return this;
            }
            source[valuePosition] = valueId;
            return fromSortedLane(keySpace, valueSpace, index, source);
        }

        int insertion = -entry - 1;
        int[] target = new int[source.length + 2];
        int prefix = insertion << 1;
        System.arraycopy(source, 0, target, 0, prefix);
        target[prefix] = keyId;
        target[prefix + 1] = valueId;
        System.arraycopy(source, prefix, target, prefix + 2, source.length - prefix);
        return fromSortedLane(keySpace, valueSpace, index, target);
    }

    public MIndexFrozenMap<K, V> without(K key) {
        int keyId = keySpace.findId(key);
        return keyId < 0 ? this : withoutKeyId(keyId);
    }

    public MIndexFrozenMap<K, V> withoutKeyId(int keyId) {
        int entry = findEntry(keyId);
        if (entry < 0) {
            return this;
        }
        int[] source = copyLane();
        int[] target = new int[source.length - 2];
        int position = entry << 1;
        System.arraycopy(source, 0, target, 0, position);
        System.arraycopy(source, position + 2, target, position, source.length - position - 2);
        return fromSortedLane(keySpace, valueSpace, index, target);
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        for (int entry = 0; entry < size(); entry++) {
            action.accept(
                    keySpace.value(keyIdAtEntry(entry)),
                    valueSpace.value(valueIdAtEntry(entry)));
        }
    }

    public void forEachId(MIndexMap.IdEntryConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int entry = 0; entry < size(); entry++) {
            action.accept(keyIdAtEntry(entry), valueIdAtEntry(entry));
        }
    }

    public int[] copyLane() {
        return index.copyLane(canonicalId);
    }

    public MIndexMap<K, V> mutableCopy() {
        MIndexMap<K, V> copy = new MIndexMap<>(keySpace, valueSpace, size());
        forEachId(copy::putIds);
        return copy;
    }

    @Override
    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    @Override
    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexFrozenMap<?, ?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    private int keyIdAtEntry(int entry) {
        if (entry < 0 || entry >= size()) {
            throw new IndexOutOfBoundsException(entry);
        }
        return index.valueAt(canonicalId, entry << 1);
    }

    private int valueIdAtEntry(int entry) {
        if (entry < 0 || entry >= size()) {
            throw new IndexOutOfBoundsException(entry);
        }
        return index.valueAt(canonicalId, (entry << 1) + 1);
    }

    private int findEntry(int keyId) {
        int low = 0;
        int high = size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int candidate = index.valueAt(canonicalId, middle << 1);
            if (candidate < keyId) {
                low = middle + 1;
            } else if (candidate > keyId) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, keySpace, valueSpace)) {
            throw new IllegalArgumentException("canonical ID is not a map in these MIndex spaces");
        }
    }
}
