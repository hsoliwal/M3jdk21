// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.MIndexObject;
import com.synexia.mindex.MIndexObjects;
import java.util.Arrays;
import java.util.Objects;

/**
 * Strong local integer domain over exact, value-semantic {@link MIndexObject}s.
 *
 * <p>The global MIndex object pool is deliberately weak. A collection cannot
 * safely retain only an integer that points at a weakly held value, so this
 * domain owns one strong canonical handle per admitted value and gives that
 * handle a stable local ID. Multiple collections can share one domain and
 * therefore share the same primitive IDs.
 *
 * <p>Signals only choose probe candidates. Every signal hit is confirmed with
 * {@link MIndexObject#sameContent(MIndexObject)} before an ID is reused.
 */
public final class MIndexObjectSpace<T> implements MIndexSpace<T> {
    private long[] signals;
    private long[] highs;
    private long[] lows;
    private int[] typeIds;
    private Object[] handles;
    private int[] table;
    private int size;

    public MIndexObjectSpace() {
        this(16);
    }

    public MIndexObjectSpace(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expected size");
        }
        int valueCapacity = Math.max(8, expectedSize);
        signals = new long[valueCapacity];
        highs = new long[valueCapacity];
        lows = new long[valueCapacity];
        typeIds = new int[valueCapacity];
        handles = new Object[valueCapacity];
        table = new int[IdSupport.tableCapacity(expectedSize)];
    }

    @Override
    public int id(T value) {
        return id(MIndexObjects.canonicalize(MIndexObjects.index(value)));
    }

    public int id(MIndexObject<T> indexed) {
        MIndexObject<T> candidate = Objects.requireNonNull(indexed, "indexed");
        requireValueSemantics(candidate);
        int existing = findIndexed(candidate);
        if (existing >= 0) {
            return existing;
        }
        if (size == IdSupport.MAX_ID) {
            throw new IllegalStateException("MIndex object space exhausted");
        }
        ensureValueCapacity(size + 1);
        if ((size + 1L) * 3L >= table.length * 2L) {
            rehash(table.length << 1);
        }

        MIndexObject<T> canonical = MIndexObjects.canonicalize(candidate);
        int id = size++;
        signals[id] = canonical.signal64();
        highs[id] = canonical.signalHigh();
        lows[id] = canonical.signalLow();
        typeIds[id] = canonical.typeId();
        handles[id] = canonical;

        int slot = vacant(canonical.signal64(), canonical.signalHigh(), canonical.signalLow(), canonical.typeId());
        table[slot] = id + 1;
        return id;
    }

    @Override
    public int findId(T value) {
        MIndexObject<T> candidate = MIndexObjects.index(value);
        if (!candidate.contentAddressable()) {
            return -1;
        }
        return findIndexed(candidate);
    }

    public int findId(MIndexObject<T> indexed) {
        MIndexObject<T> candidate = Objects.requireNonNull(indexed, "indexed");
        return candidate.contentAddressable() ? findIndexed(candidate) : -1;
    }

    @Override
    public T value(int id) {
        return indexed(id).value();
    }

    public MIndexObject<T> indexed(int id) {
        requireId(id);
        @SuppressWarnings("unchecked")
        MIndexObject<T> value = (MIndexObject<T>) handles[id];
        return value;
    }

    /**
     * Admit the exact value represented by one ID from another object space.
     *
     * <p>The source handle is transferred directly, so the target reuses the
     * existing MIndexObject structural evidence instead of rediscovering the
     * source Java object's shape. The target still performs its normal
     * candidate lookup and exact {@link MIndexObject#sameContent(MIndexObject)}
     * confirmation before reusing an ID.
     */
    public int transferIdFrom(
            MIndexObjectSpace<T> source,
            int sourceId) {
        MIndexObjectSpace<T> actual =
                Objects.requireNonNull(source, "source");
        return id(actual.indexed(sourceId));
    }

    /**
     * Find the target ID for an exact value from another object space without
     * admitting it. Returns {@code -1} when the target does not contain an
     * exactly equivalent value.
     */
    public int findTransferredId(
            MIndexObjectSpace<T> source,
            int sourceId) {
        MIndexObjectSpace<T> actual =
                Objects.requireNonNull(source, "source");
        return findId(actual.indexed(sourceId));
    }

    /**
     * Exact cross-space value comparison.
     *
     * <p>Signals/type IDs remain candidate metadata only; semantic authority is
     * still {@link MIndexObject#sameContent(MIndexObject)}.
     */
    public boolean sameContentId(
            int id,
            MIndexObjectSpace<T> other,
            int otherId) {
        return indexed(id).sameContent(
                Objects.requireNonNull(other, "other").indexed(otherId));
    }

    /**
     * Bulk exact transfer preserving input order. The returned lane belongs to
     * this target space; the caller-owned source array is never modified.
     */
    public int[] transferIdsFrom(
            MIndexObjectSpace<T> source,
            int[] sourceIds) {
        MIndexObjectSpace<T> actual =
                Objects.requireNonNull(source, "source");
        int[] ids = Objects.requireNonNull(sourceIds, "sourceIds");
        int[] result = new int[ids.length];
        int[] remap = new int[actual.size()];
        Arrays.fill(remap, -1);
        for (int index = 0; index < ids.length; index++) {
            int sourceId = ids[index];
            actual.requireId(sourceId);
            int targetId = remap[sourceId];
            if (targetId < 0) {
                targetId = transferIdFrom(actual, sourceId);
                remap[sourceId] = targetId;
            }
            result[index] = targetId;
        }
        return result;
    }

    @Override
    public int size() {
        return size;
    }

    private int findIndexed(MIndexObject<?> candidate) {
        int mask = table.length - 1;
        int slot = slot(candidate.signal64(), candidate.signalHigh(), candidate.signalLow(), candidate.typeId(), mask);
        while (table[slot] != 0) {
            int id = table[slot] - 1;
            if (signals[id] == candidate.signal64()
                    && highs[id] == candidate.signalHigh()
                    && lows[id] == candidate.signalLow()
                    && typeIds[id] == candidate.typeId()) {
                MIndexObject<?> existing = handle(id);
                if (candidate.sameContent(existing)) {
                    return id;
                }
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private int vacant(long signal, long high, long low, int typeId) {
        int mask = table.length - 1;
        int slot = slot(signal, high, low, typeId, mask);
        while (table[slot] != 0) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private MIndexObject<?> handle(int id) {
        return (MIndexObject<?>) handles[id];
    }

    private static void requireValueSemantics(MIndexObject<?> value) {
        if (!value.contentAddressable()) {
            throw new IllegalArgumentException(
                    "MIndexObject is not content-addressable: " + value);
        }
    }

    private void ensureValueCapacity(int needed) {
        if (needed <= handles.length) {
            return;
        }
        int capacity = IdSupport.grown(handles.length, needed);
        signals = Arrays.copyOf(signals, capacity);
        highs = Arrays.copyOf(highs, capacity);
        lows = Arrays.copyOf(lows, capacity);
        typeIds = Arrays.copyOf(typeIds, capacity);
        handles = Arrays.copyOf(handles, capacity);
    }

    private void rehash(int requested) {
        int capacity = 8;
        while (capacity < requested) {
            capacity <<= 1;
        }
        int[] next = new int[capacity];
        int mask = capacity - 1;
        for (int id = 0; id < size; id++) {
            int slot = slot(signals[id], highs[id], lows[id], typeIds[id], mask);
            while (next[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            next[slot] = id + 1;
        }
        table = next;
    }

    private static int slot(long signal, long high, long low, int typeId, int mask) {
        long mixed = IdSupport.mix64(
                signal ^ Long.rotateLeft(high, 21) ^ Long.rotateLeft(low, 43)
                        ^ Integer.toUnsignedLong(typeId));
        return ((int) mixed) & mask;
    }
}
