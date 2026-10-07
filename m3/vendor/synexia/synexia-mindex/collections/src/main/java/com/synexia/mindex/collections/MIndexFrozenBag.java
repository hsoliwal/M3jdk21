// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable canonical multiset represented by sorted identity/count pairs.
 *
 * <p>The canonical lane is
 * {@code [id0,count0,id1,count1,...]}. IDs are strictly increasing and counts
 * are positive. Repeated input IDs are combined during construction with
 * overflow checks.
 */
public final class MIndexFrozenBag<E>
        implements MIndexCanonicalCollection {
    @FunctionalInterface
    public interface IdCountConsumer {
        void accept(int id, int count);
    }

    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;
    private final long totalSize;

    private MIndexFrozenBag(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)
                || (index.length(canonicalId) & 1) != 0) {
            throw new IllegalArgumentException(
                    "canonical ID is not a bag in this MIndexSpace");
        }

        long total = 0L;
        int previousId = -1;
        for (int offset = 0;
                offset < index.length(canonicalId);
                offset += 2) {
            int id = index.valueAt(canonicalId, offset);
            int count = index.valueAt(canonicalId, offset + 1);
            space.requireId(id);
            if (id <= previousId || count <= 0) {
                throw new IllegalArgumentException(
                        "invalid canonical bag identity/count lane");
            }
            previousId = id;
            total = Math.addExact(total, count);
        }
        totalSize = total;
    }

    public static <E> MIndexFrozenBag<E> ofIdentityCounts(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] identityCountLane) {
        MIndexSpace<E> domain =
                Objects.requireNonNull(space, "space");
        MIndexCompositeIndex composites =
                Objects.requireNonNull(index, "index");
        int[] input =
                Objects.requireNonNull(
                        identityCountLane, "identityCountLane").clone();
        if ((input.length & 1) != 0) {
            throw new IllegalArgumentException(
                    "bag lane must contain identity/count pairs");
        }

        int entries = input.length >>> 1;
        long[] packed = new long[entries];
        for (int entry = 0; entry < entries; entry++) {
            int id = input[entry << 1];
            int count = input[(entry << 1) + 1];
            domain.requireId(id);
            if (count <= 0) {
                throw new IllegalArgumentException(
                        "bag counts must be positive");
            }
            packed[entry] =
                    (Integer.toUnsignedLong(id) << 32)
                            | Integer.toUnsignedLong(count);
        }
        Arrays.sort(packed);

        int[] normalized = new int[input.length];
        int distinct = 0;
        for (long pair : packed) {
            int id = (int) (pair >>> 32);
            int count = (int) pair;
            if (distinct > 0
                    && normalized[(distinct - 1) << 1] == id) {
                int countOffset =
                        ((distinct - 1) << 1) + 1;
                normalized[countOffset] =
                        Math.addExact(
                                normalized[countOffset], count);
            } else {
                normalized[distinct << 1] = id;
                normalized[(distinct << 1) + 1] = count;
                distinct++;
            }
        }
        normalized = Arrays.copyOf(
                normalized, distinct << 1);

        int canonicalId = composites.intern(
                MIndexCompositeIndex.KIND_BAG,
                domain,
                null,
                normalized);
        return new MIndexFrozenBag<>(
                domain, composites, canonicalId);
    }

    public MIndexSpace<E> space() {
        return space;
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
        return MIndexCompositeIndex.KIND_BAG;
    }

    /** Number of distinct identities. */
    @Override
    public int size() {
        return distinctSize();
    }

    public int distinctSize() {
        return index.length(canonicalId) >>> 1;
    }

    public long totalSize() {
        return totalSize;
    }

    public boolean isEmpty() {
        return distinctSize() == 0;
    }

    public int count(E value) {
        int id = space.findId(value);
        return id < 0 ? 0 : countId(id);
    }

    public int countId(int id) {
        int entry = findId(id);
        return entry < 0
                ? 0
                : index.valueAt(
                        canonicalId, (entry << 1) + 1);
    }

    public E valueAt(int entry) {
        return space.value(idAt(entry));
    }

    public int idAt(int entry) {
        return index.valueAt(
                canonicalId,
                Objects.checkIndex(
                        entry, distinctSize()) << 1);
    }

    public int countAt(int entry) {
        return index.valueAt(
                canonicalId,
                (Objects.checkIndex(
                        entry, distinctSize()) << 1) + 1);
    }

    public MIndexFrozenBag<E> with(
            E value,
            int occurrences) {
        return withId(space.id(value), occurrences);
    }

    public MIndexFrozenBag<E> withId(
            int id,
            int occurrences) {
        space.requireId(id);
        if (occurrences <= 0) {
            throw new IllegalArgumentException(
                    "occurrences must be positive");
        }
        int entry = findId(id);
        int[] lane = copyIdentityCountLane();
        if (entry >= 0) {
            int countOffset = (entry << 1) + 1;
            lane[countOffset] =
                    Math.addExact(
                            lane[countOffset], occurrences);
            return ofIdentityCounts(space, index, lane);
        }

        int insertion = -entry - 1;
        int[] target = new int[lane.length + 2];
        int offset = insertion << 1;
        System.arraycopy(lane, 0, target, 0, offset);
        target[offset] = id;
        target[offset + 1] = occurrences;
        System.arraycopy(
                lane,
                offset,
                target,
                offset + 2,
                lane.length - offset);
        return ofIdentityCounts(space, index, target);
    }

    public MIndexFrozenBag<E> without(
            E value,
            int occurrences) {
        int id = space.findId(value);
        return id < 0
                ? this
                : withoutId(id, occurrences);
    }

    public MIndexFrozenBag<E> withoutId(
            int id,
            int occurrences) {
        if (occurrences <= 0) {
            throw new IllegalArgumentException(
                    "occurrences must be positive");
        }
        int entry = findId(id);
        if (entry < 0) {
            return this;
        }
        int current = countAt(entry);
        int[] lane = copyIdentityCountLane();
        if (occurrences < current) {
            lane[(entry << 1) + 1] =
                    current - occurrences;
            return ofIdentityCounts(space, index, lane);
        }

        int[] target = new int[lane.length - 2];
        int offset = entry << 1;
        System.arraycopy(lane, 0, target, 0, offset);
        System.arraycopy(
                lane,
                offset + 2,
                target,
                offset,
                lane.length - offset - 2);
        return ofIdentityCounts(space, index, target);
    }

    public void forEachIdCount(
            IdCountConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int entry = 0;
                entry < distinctSize();
                entry++) {
            action.accept(idAt(entry), countAt(entry));
        }
    }

    public int[] copyIdentityCountLane() {
        return index.copyLane(canonicalId);
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
                || other instanceof MIndexFrozenBag<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {
        return "MIndexFrozenBag[id="
                + canonicalId
                + ",distinct="
                + distinctSize()
                + ",total="
                + totalSize
                + ']';
    }

    private int findId(int id) {
        int low = 0;
        int high = distinctSize() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int candidate = idAt(middle);
            if (candidate < id) {
                low = middle + 1;
            } else if (candidate > id) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }
}
