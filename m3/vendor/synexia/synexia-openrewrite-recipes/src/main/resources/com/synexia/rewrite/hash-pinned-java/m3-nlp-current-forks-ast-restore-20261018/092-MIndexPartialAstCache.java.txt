// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded scan-resistant cache specialized for immutable partial-AST projections.
 *
 * <p>New entries enter probation. A hit promotes to protected. Eviction consumes probation LRU
 * before protected LRU, so one-time repository scans do not immediately destroy hot editor/compiler
 * fragments. Storage is fixed arrays plus an integer open-addressed index; no per-access entry
 * wrapper is allocated.</p>
 */
public final class MIndexPartialAstCache {
    private static final byte NONE = 0;
    private static final byte PROBATION = 1;
    private static final byte PROTECTED = 2;
    private static final int EMPTY_SLOT = 0;
    private static final int TOMBSTONE = -1;

    public record Stats(
            int entries,
            int probationEntries,
            int protectedEntries,
            long retainedWeight,
            long hits,
            long misses,
            long promotions,
            long demotions,
            long evictions,
            long admissionRejections,
            MIndexPartialAstPressure pressure) { }

    private final int maxEntries;
    private final long maxWeight;
    private final int protectedMaxEntries;
    private final long protectedMaxWeight;

    private final MIndexPartialAstKey[] keys;
    private final MIndexPartialAstProjection[] values;
    private final long[] weights;
    private final byte[] segments;
    private final int[] previous;
    private final int[] next;
    private final int[] entrySlots;
    private final int[] freeEntries;
    private final int[] slots;

    private int freeCount;
    private int size;
    private int probationEntries;
    private int protectedEntries;
    private long retainedWeight;
    private long protectedWeight;
    private int probationHead = -1;
    private int probationTail = -1;
    private int protectedHead = -1;
    private int protectedTail = -1;
    private long hits;
    private long misses;
    private long promotions;
    private long demotions;
    private long evictions;
    private long admissionRejections;
    private MIndexPartialAstPressure pressure = MIndexPartialAstPressure.NORMAL;

    public MIndexPartialAstCache(int maxEntries, long maxWeight, int protectedPercent) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be positive");
        if (maxWeight < 1) throw new IllegalArgumentException("maxWeight must be positive");
        if (protectedPercent < 1 || protectedPercent > 99) {
            throw new IllegalArgumentException("protectedPercent must be 1..99");
        }
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        protectedMaxEntries = Math.max(1, (int) percentage(maxEntries, protectedPercent));
        protectedMaxWeight = Math.max(1L, percentage(maxWeight, protectedPercent));

        keys = new MIndexPartialAstKey[maxEntries];
        values = new MIndexPartialAstProjection[maxEntries];
        weights = new long[maxEntries];
        segments = new byte[maxEntries];
        previous = new int[maxEntries];
        next = new int[maxEntries];
        entrySlots = new int[maxEntries];
        freeEntries = new int[maxEntries];
        Arrays.fill(previous, -1);
        Arrays.fill(next, -1);
        Arrays.fill(entrySlots, -1);
        for (int i = 0; i < maxEntries; i++) freeEntries[i] = maxEntries - 1 - i;
        freeCount = maxEntries;

        int slotCapacity = 2;
        while (slotCapacity < Math.multiplyExact(maxEntries, 4)) {
            slotCapacity = Math.multiplyExact(slotCapacity, 2);
        }
        slots = new int[slotCapacity];
    }

    public synchronized MIndexPartialAstProjection get(MIndexPartialAstKey key) {
        Objects.requireNonNull(key, "key");
        int entry = lookup(key);
        if (entry < 0) {
            misses++;
            return null;
        }
        hits++;
        if (segments[entry] == PROBATION) {
            unlink(entry, PROBATION);
            probationEntries--;
            segments[entry] = PROTECTED;
            protectedEntries++;
            protectedWeight = Math.addExact(protectedWeight, weights[entry]);
            linkHead(entry, PROTECTED);
            promotions++;
            rebalanceProtected();
        } else {
            unlink(entry, PROTECTED);
            linkHead(entry, PROTECTED);
        }
        return values[entry];
    }

    /**
     * @return true when the projection was retained; false when pressure/weight rejects admission.
     */
    public synchronized boolean put(MIndexPartialAstProjection projection) {
        Objects.requireNonNull(projection, "projection");
        long weight = projection.retainedBytes();
        long target = currentWeightTarget();
        if (!pressure.admit() || weight > target) {
            admissionRejections++;
            return false;
        }

        int existing = lookup(projection.key());
        if (existing >= 0) {
            while (retainedWeight - weights[existing] > target - weight) evictOneExcept(existing);
            retainedWeight -= weights[existing];
            if (segments[existing] == PROTECTED) protectedWeight -= weights[existing];
            values[existing] = projection;
            weights[existing] = weight;
            retainedWeight = Math.addExact(retainedWeight, weight);
            if (segments[existing] == PROTECTED) protectedWeight = Math.addExact(protectedWeight, weight);
            touch(existing);
            rebalanceProtected();
            trimTo(currentWeightTarget());
            return lookup(projection.key()) >= 0;
        }

        trimTo(target - weight);
        if (freeCount == 0) evictOne();
        int entry = freeEntries[--freeCount];
        int slot = insertionSlot(projection.key());
        keys[entry] = projection.key();
        values[entry] = projection;
        weights[entry] = weight;
        segments[entry] = PROBATION;
        entrySlots[entry] = slot;
        slots[slot] = entry + 1;
        size++;
        probationEntries++;
        retainedWeight = Math.addExact(retainedWeight, weight);
        linkHead(entry, PROBATION);
        trimTo(currentWeightTarget());
        return keys[entry] != null;
    }

    public synchronized void invalidateSource(String sourceSha256) {
        Objects.requireNonNull(sourceSha256, "sourceSha256");
        for (int entry = 0; entry < maxEntries; entry++) {
            MIndexPartialAstKey key = keys[entry];
            if (key != null && key.sourceSha256().equalsIgnoreCase(sourceSha256)) {
                remove(entry, false);
            }
        }
    }

    public synchronized void clear() {
        for (int entry = 0; entry < maxEntries; entry++) {
            if (keys[entry] != null) remove(entry, false);
        }
        Arrays.fill(slots, EMPTY_SLOT);
    }

    public synchronized void setPressure(MIndexPartialAstPressure nextPressure) {
        pressure = Objects.requireNonNull(nextPressure, "nextPressure");
        trimTo(currentWeightTarget());
        int targetEntries = (int) percentage(maxEntries, pressure.retainedPercent());
        while (size > targetEntries) evictOne();
    }

    public synchronized Stats stats() {
        return new Stats(
                size, probationEntries, protectedEntries, retainedWeight,
                hits, misses, promotions, demotions, evictions, admissionRejections, pressure);
    }

    private long currentWeightTarget() {
        return percentage(maxWeight, pressure.retainedPercent());
    }

    /** Exact nonnegative percentage without overflowing an otherwise valid long budget. */
    private static long percentage(long value, int percent) {
        return (value / 100L) * percent + (value % 100L) * percent / 100L;
    }

    private void trimTo(long targetWeight) {
        while (retainedWeight > targetWeight && size > 0) evictOne();
    }

    private void touch(int entry) {
        byte segment = segments[entry];
        unlink(entry, segment);
        linkHead(entry, segment);
    }

    private void rebalanceProtected() {
        while (protectedEntries > protectedMaxEntries || protectedWeight > protectedMaxWeight) {
            int entry = protectedTail;
            if (entry < 0) break;
            unlink(entry, PROTECTED);
            protectedEntries--;
            protectedWeight -= weights[entry];
            segments[entry] = PROBATION;
            probationEntries++;
            linkHead(entry, PROBATION);
            demotions++;
        }
    }

    /** Replacement preserves its existing segment/slot while making room without addition overflow. */
    private void evictOneExcept(int retained) {
        int victim = probationTail;
        if (victim == retained) victim = previous[victim];
        if (victim < 0) {
            victim = protectedTail;
            if (victim == retained) victim = previous[victim];
        }
        if (victim < 0) throw new IllegalStateException("partial AST replacement capacity invariant");
        remove(victim, true);
    }

    private void evictOne() {
        int entry = probationTail >= 0 ? probationTail : protectedTail;
        if (entry < 0) return;
        remove(entry, true);
    }

    private void remove(int entry, boolean eviction) {
        if (keys[entry] == null) return;
        byte segment = segments[entry];
        unlink(entry, segment);
        if (segment == PROBATION) {
            probationEntries--;
        } else if (segment == PROTECTED) {
            protectedEntries--;
            protectedWeight -= weights[entry];
        }
        retainedWeight -= weights[entry];
        size--;

        int slot = entrySlots[entry];
        if (slot >= 0) slots[slot] = TOMBSTONE;
        keys[entry] = null;
        values[entry] = null;
        weights[entry] = 0L;
        segments[entry] = NONE;
        previous[entry] = -1;
        next[entry] = -1;
        entrySlots[entry] = -1;
        freeEntries[freeCount++] = entry;
        if (eviction) evictions++;
    }

    private int lookup(MIndexPartialAstKey key) {
        int mask = slots.length - 1;
        int slot = spread(key.hashCode()) & mask;
        for (int probes = 0; probes < slots.length; probes++) {
            int stored = slots[slot];
            if (stored == EMPTY_SLOT) return -1;
            if (stored > 0) {
                int entry = stored - 1;
                if (key.equals(keys[entry])) return entry;
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private int insertionSlot(MIndexPartialAstKey key) {
        int mask = slots.length - 1;
        int slot = spread(key.hashCode()) & mask;
        int firstTombstone = -1;
        for (int probes = 0; probes < slots.length; probes++) {
            int stored = slots[slot];
            if (stored == EMPTY_SLOT) return firstTombstone >= 0 ? firstTombstone : slot;
            if (stored == TOMBSTONE) {
                if (firstTombstone < 0) firstTombstone = slot;
            } else {
                int entry = stored - 1;
                if (key.equals(keys[entry])) return slot;
            }
            slot = (slot + 1) & mask;
        }
        if (firstTombstone >= 0) return firstTombstone;
        throw new IllegalStateException("partial AST cache index is full");
    }

    private void unlink(int entry, byte segment) {
        int before = previous[entry];
        int after = next[entry];
        if (before >= 0) next[before] = after;
        if (after >= 0) previous[after] = before;

        if (segment == PROBATION) {
            if (probationHead == entry) probationHead = after;
            if (probationTail == entry) probationTail = before;
        } else if (segment == PROTECTED) {
            if (protectedHead == entry) protectedHead = after;
            if (protectedTail == entry) protectedTail = before;
        }
        previous[entry] = -1;
        next[entry] = -1;
    }

    private void linkHead(int entry, byte segment) {
        int head = segment == PROBATION ? probationHead : protectedHead;
        previous[entry] = -1;
        next[entry] = head;
        if (head >= 0) previous[head] = entry;
        if (segment == PROBATION) {
            probationHead = entry;
            if (probationTail < 0) probationTail = entry;
        } else {
            protectedHead = entry;
            if (protectedTail < 0) protectedTail = entry;
        }
    }

    private static int spread(int value) {
        int x = value ^ (value >>> 16);
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        return x ^ (x >>> 16);
    }
}
