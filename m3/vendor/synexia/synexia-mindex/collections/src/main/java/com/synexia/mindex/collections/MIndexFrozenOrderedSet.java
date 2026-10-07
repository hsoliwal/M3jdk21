// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Immutable canonical insertion-ordered set.
 *
 * <p>Encounter order is semantic identity. Construction preserves the first
 * occurrence of each ID and removes later duplicates, matching mutable
 * insertion-ordered-set admission semantics.
 */
public final class MIndexFrozenOrderedSet<E>
        implements MIndexCanonicalCollection {
    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenOrderedSet(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException(
                    "canonical ID is not an ordered set in this MIndexSpace");
        }
        requireUnique(index.copyLane(canonicalId));
    }

    public static <E> MIndexFrozenOrderedSet<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] ids) {
        MIndexSpace<E> domain =
                Objects.requireNonNull(space, "space");
        MIndexCompositeIndex composites =
                Objects.requireNonNull(index, "index");
        int[] source =
                Objects.requireNonNull(ids, "ids").clone();

        int[] table =
                new int[IdSupport.tableCapacity(
                        Math.max(1, source.length))];
        int mask = table.length - 1;
        int unique = 0;
        for (int id : source) {
            domain.requireId(id);
            int slot = IdSupport.mix(id) & mask;
            boolean duplicate = false;
            while (table[slot] != 0) {
                if (table[slot] - 1 == id) {
                    duplicate = true;
                    break;
                }
                slot = (slot + 1) & mask;
            }
            if (!duplicate) {
                table[slot] = id + 1;
                source[unique++] = id;
            }
        }
        int[] lane = Arrays.copyOf(source, unique);
        int canonicalId = composites.intern(
                MIndexCompositeIndex.KIND_ORDERED_SET,
                domain,
                null,
                lane);
        return new MIndexFrozenOrderedSet<>(
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
        return MIndexCompositeIndex.KIND_ORDERED_SET;
    }

    @Override
    public int size() {
        return index.length(canonicalId);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E get(int position) {
        return space.value(idAt(position));
    }

    public int idAt(int position) {
        return index.valueAt(
                canonicalId,
                Objects.checkIndex(position, size()));
    }

    public boolean contains(E value) {
        int id = space.findId(value);
        return id >= 0 && containsId(id);
    }

    public boolean containsId(int id) {
        return indexOfId(id) >= 0;
    }

    public int indexOfId(int id) {
        for (int position = 0;
                position < size();
                position++) {
            if (idAt(position) == id) {
                return position;
            }
        }
        return -1;
    }

    public MIndexFrozenOrderedSet<E> with(E value) {
        return withId(space.id(value));
    }

    public MIndexFrozenOrderedSet<E> withId(int id) {
        space.requireId(id);
        if (containsId(id)) {
            return this;
        }
        int[] ids = Arrays.copyOf(copyIds(), size() + 1);
        ids[ids.length - 1] = id;
        return ofIds(space, index, ids);
    }

    public MIndexFrozenOrderedSet<E> without(E value) {
        int id = space.findId(value);
        return id < 0 ? this : withoutId(id);
    }

    public MIndexFrozenOrderedSet<E> withoutId(int id) {
        int position = indexOfId(id);
        if (position < 0) {
            return this;
        }
        int[] source = copyIds();
        int[] target = new int[source.length - 1];
        System.arraycopy(
                source, 0, target, 0, position);
        System.arraycopy(
                source,
                position + 1,
                target,
                position,
                source.length - position - 1);
        return ofIds(space, index, target);
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        forEachId(id -> action.accept(space.value(id)));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int position = 0;
                position < size();
                position++) {
            action.accept(idAt(position));
        }
    }

    public int[] copyIds() {
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
                || other instanceof MIndexFrozenOrderedSet<?> that
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
        return "MIndexFrozenOrderedSet[id="
                + canonicalId + ",size=" + size() + ']';
    }

    private static void requireUnique(int[] ids) {
        int[] table =
                new int[IdSupport.tableCapacity(
                        Math.max(1, ids.length))];
        int mask = table.length - 1;
        for (int id : ids) {
            int slot = IdSupport.mix(id) & mask;
            while (table[slot] != 0) {
                if (table[slot] - 1 == id) {
                    throw new IllegalArgumentException(
                            "canonical ordered set contains duplicate ID: "
                                    + id);
                }
                slot = (slot + 1) & mask;
            }
            table[slot] = id + 1;
        }
    }
}
