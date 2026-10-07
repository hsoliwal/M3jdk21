// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;
import java.util.function.Consumer;

/** Immutable canonical one-to-many relation set over sorted key/value ID pairs. */
public final class MIndexFrozenMultiMap<K, V> implements MIndexCanonicalCollection {
    private final MIndexSpace<K> keySpace;
    private final MIndexSpace<V> valueSpace;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenMultiMap(
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

    public static <K, V> MIndexFrozenMultiMap<K, V> copyOf(
            MIndexMultiMap<K, V> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return fromSortedLane(
                source.keySpace(),
                source.valueSpace(),
                index,
                source.snapshotSortedLane());
    }

    public static <K, V> MIndexFrozenMultiMap<K, V> ofIds(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            MIndexCompositeIndex index,
            int[] keyValueLane) {
        Objects.requireNonNull(keySpace, "keySpace");
        Objects.requireNonNull(valueSpace, "valueSpace");
        int[] lane = Objects.requireNonNull(keyValueLane, "keyValueLane").clone();
        if ((lane.length & 1) != 0) {
            throw new IllegalArgumentException("multimap lane must contain key/value pairs");
        }
        long[] packed = new long[lane.length >>> 1];
        for (int entry = 0; entry < packed.length; entry++) {
            int keyId = lane[entry << 1];
            int valueId = lane[(entry << 1) + 1];
            keySpace.requireId(keyId);
            valueSpace.requireId(valueId);
            packed[entry] = pack(keyId, valueId);
        }
        java.util.Arrays.sort(packed);
        int unique = 0;
        for (long pair : packed) {
            if (unique == 0 || packed[unique - 1] != pair) {
                packed[unique++] = pair;
            }
        }
        int[] normalized = new int[unique << 1];
        for (int entry = 0; entry < unique; entry++) {
            normalized[entry << 1] = (int) (packed[entry] >>> 32);
            normalized[(entry << 1) + 1] = (int) packed[entry];
        }
        return fromSortedLane(keySpace, valueSpace, index, normalized);
    }

    private static <K, V> MIndexFrozenMultiMap<K, V> fromSortedLane(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            MIndexCompositeIndex index,
            int[] lane) {
        Objects.requireNonNull(index, "index");
        int id = index.intern(
                MIndexCompositeIndex.KIND_MULTI_MAP,
                keySpace,
                valueSpace,
                lane);
        return new MIndexFrozenMultiMap<>(keySpace, valueSpace, index, id);
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
        return MIndexCompositeIndex.KIND_MULTI_MAP;
    }

    @Override
    public int size() {
        return index.length(canonicalId) >>> 1;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int keyCount() {
        int count = 0;
        int previous = -1;
        for (int entry = 0; entry < size(); entry++) {
            int key = keyIdAt(entry);
            if (entry == 0 || key != previous) {
                count++;
                previous = key;
            }
        }
        return count;
    }

    public boolean containsKey(K key) {
        int keyId = keySpace.findId(key);
        return keyId >= 0 && lowerBound(keyId, 0) < upperBound(keyId);
    }

    public boolean containsEntry(K key, V value) {
        int keyId = keySpace.findId(key);
        int valueId = valueSpace.findId(value);
        return keyId >= 0 && valueId >= 0 && findPair(keyId, valueId) >= 0;
    }

    public int valueCount(K key) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return 0;
        }
        return upperBound(keyId) - lowerBound(keyId, 0);
    }

    public MIndexFrozenMultiMap<K, V> with(K key, V value) {
        return withIds(keySpace.id(key), valueSpace.id(value));
    }

    public MIndexFrozenMultiMap<K, V> withIds(int keyId, int valueId) {
        keySpace.requireId(keyId);
        valueSpace.requireId(valueId);
        int position = findPair(keyId, valueId);
        if (position >= 0) {
            return this;
        }
        int insertion = insertionPoint(keyId, valueId);
        int[] source = copyLane();
        int[] target = new int[source.length + 2];
        int offset = insertion << 1;
        System.arraycopy(source, 0, target, 0, offset);
        target[offset] = keyId;
        target[offset + 1] = valueId;
        System.arraycopy(source, offset, target, offset + 2, source.length - offset);
        return fromSortedLane(keySpace, valueSpace, index, target);
    }

    public MIndexFrozenMultiMap<K, V> without(K key, V value) {
        int keyId = keySpace.findId(key);
        int valueId = valueSpace.findId(value);
        if (keyId < 0 || valueId < 0) {
            return this;
        }
        return withoutIds(keyId, valueId);
    }

    public MIndexFrozenMultiMap<K, V> withoutIds(int keyId, int valueId) {
        int position = findPair(keyId, valueId);
        if (position < 0) {
            return this;
        }
        int[] source = copyLane();
        int[] target = new int[source.length - 2];
        int offset = position << 1;
        System.arraycopy(source, 0, target, 0, offset);
        System.arraycopy(source, offset + 2, target, offset, source.length - offset - 2);
        return fromSortedLane(keySpace, valueSpace, index, target);
    }

    public MIndexFrozenMultiMap<K, V> withoutKey(K key) {
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return this;
        }
        int start = lowerBound(keyId, 0);
        int end = upperBound(keyId);
        if (start == end) {
            return this;
        }
        int[] source = copyLane();
        int removedInts = (end - start) << 1;
        int[] target = new int[source.length - removedInts];
        int offset = start << 1;
        System.arraycopy(source, 0, target, 0, offset);
        System.arraycopy(source, offset + removedInts, target, offset, source.length - offset - removedInts);
        return fromSortedLane(keySpace, valueSpace, index, target);
    }

    public void forEachValue(K key, Consumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        int keyId = keySpace.findId(key);
        if (keyId < 0) {
            return;
        }
        for (int entry = lowerBound(keyId, 0), end = upperBound(keyId); entry < end; entry++) {
            action.accept(valueSpace.value(valueIdAt(entry)));
        }
    }

    public void forEachId(MIndexMap.IdEntryConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int entry = 0; entry < size(); entry++) {
            action.accept(keyIdAt(entry), valueIdAt(entry));
        }
    }

    public int[] copyLane() {
        return index.copyLane(canonicalId);
    }

    public MIndexMultiMap<K, V> mutableCopy() {
        MIndexMultiMap<K, V> map = new MIndexMultiMap<>(keySpace, valueSpace, keyCount());
        forEachId(map::addIds);
        return map;
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
                || other instanceof MIndexFrozenMultiMap<?, ?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    private int keyIdAt(int entry) {
        return index.valueAt(canonicalId, entry << 1);
    }

    private int valueIdAt(int entry) {
        return index.valueAt(canonicalId, (entry << 1) + 1);
    }

    private int findPair(int keyId, int valueId) {
        int low = lowerBound(keyId, valueId);
        if (low < size()
                && keyIdAt(low) == keyId
                && valueIdAt(low) == valueId) {
            return low;
        }
        return -1;
    }

    private int insertionPoint(int keyId, int valueId) {
        return lowerBound(keyId, valueId);
    }

    private int lowerBound(int keyId, int valueId) {
        int low = 0;
        int high = size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            int key = keyIdAt(middle);
            int value = valueIdAt(middle);
            if (key < keyId || key == keyId && value < valueId) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private int upperBound(int keyId) {
        int low = 0;
        int high = size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (keyIdAt(middle) <= keyId) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static long pack(int keyId, int valueId) {
        return (Integer.toUnsignedLong(keyId) << 32) | Integer.toUnsignedLong(valueId);
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, keySpace, valueSpace)
                || (index.length(canonicalId) & 1) != 0) {
            throw new IllegalArgumentException("canonical ID is not a multimap in these MIndex spaces");
        }
    }
}
