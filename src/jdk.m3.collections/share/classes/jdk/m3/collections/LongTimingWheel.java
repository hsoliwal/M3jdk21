/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Power-of-two timing wheel with primitive bucket heads and struct-of-arrays entries.
 *
 * <p>Future rotations remain in the same modulo bucket and are reconsidered only
 * when that bucket comes around again. No scheduled-entry object is retained.
 *
 * <p>A long-distance advance takes the earliest linked deadline from exact
 * bucket-indexed lanes (bucket minimum, 64-bucket group minimum, occupancy,
 * live and dirty bitsets) that the first clean full scan builds; slot-lane
 * growth never touches them and payloadBytes() reports the same lanes as before.
 *
 * <p>Lineage: absorbed from Synexia {@code
 * m3-primitives/src/main/java/jdk/internal/m3/primitives/LongTimingWheel.java}
 * blob {@code 17ddb833f940a3439c185137d6105480e0def60a} at com.synexia develop {@code
 * ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class LongTimingWheel {
    private final int[] bucketHead;
    private final int mask;
    private final long tickNanos;
    private long currentTick;
    /** There is no signed 64-bit tick after Long.MAX_VALUE. */
    private boolean finalTickConsumed;

    private long[] values;
    private long[] deadlineTick;
    private int[] next;
    private int highWater;
    private int freeHead = -1;
    private int size;

    /** Exact minimum deadline per bucket while indexed; MAX for an empty bucket. */
    private long[] bucketMin;
    /** Per 64 buckets: [2g] group minimum, exact unless dirty; [2g + 1] occupancy. */
    private long[] groupLane;
    /** Per 64 groups: [2w] live-group bits, [2w + 1] dirty-group bits. */
    private long[] groupBits;
    /** Bucket walks holding a detached chain; non-zero while any callback runs. */
    private int walkDepth;
    /** The lanes exist, are exact and are maintained on every link and detach. */
    private boolean indexed;
    /** Re-entrant clear() or an unrestored walk exit: scan until a clean clear(). */
    private boolean tainted;

    public LongTimingWheel(int wheelSize, long tickNanos, long startNanos) {
        if (wheelSize <= 0 || Integer.bitCount(wheelSize) != 1) {
            throw new IllegalArgumentException("wheelSize must be a power of two");
        }
        if (tickNanos <= 0L) throw new IllegalArgumentException("tickNanos <= 0");
        bucketHead = new int[wheelSize];
        Arrays.fill(bucketHead, -1);
        mask = wheelSize - 1;
        this.tickNanos = tickNanos;
        currentTick = Math.floorDiv(startNanos, tickNanos);
        int capacity = Math.max(8, wheelSize);
        values = new long[capacity];
        deadlineTick = new long[capacity];
        next = new int[capacity];
    }

    public int wheelSize() { return bucketHead.length; }
    public long tickNanos() { return tickNanos; }
    public long currentTick() { return currentTick; }
    public int size() { return size; }
    public long payloadBytes() {
        return (long) bucketHead.length * Integer.BYTES
                + (long) values.length * Long.BYTES * 2L
                + (long) next.length * Integer.BYTES;
    }

    public void schedule(long value, long deadlineNanos) {
        if (finalTickConsumed) throw new IllegalStateException("timing wheel exhausted");
        long tick = ceilDiv(deadlineNanos, tickNanos);
        if (tick < currentTick) tick = currentTick;
        int slot = allocate();
        values[slot] = value;
        deadlineTick[slot] = tick;
        relink(slot, (int) tick & mask);
        size++;
    }

    /**
     * Advances through the target tick and emits every due value.
     *
     * @return number of emitted entries
     */
    public int advanceTo(long nowNanos, LongConsumer dueConsumer) {
        Objects.requireNonNull(dueConsumer, "dueConsumer");
        long target = Math.floorDiv(nowNanos, tickNanos);
        int emitted = 0;
        while (!finalTickConsumed && currentTick <= target) {
            // An empty wheel, or one whose earliest tick lies past target, has
            // no per-tick work: preserve the exact cursor of the tick loop.
            if (size == 0 || !seekDue(target)) {
                advanceEmptyTo(target);
                break;
            }
            emitted += walkBucket((int) currentTick & mask, dueConsumer);
            stepTick();
        }
        return emitted;
    }

    /** Empty-span skip is equivalent to completing each intervening tick. */
    private void advanceEmptyTo(long target) {
        if (target == Long.MAX_VALUE) {
            currentTick = Long.MAX_VALUE;
            finalTickConsumed = true;
        } else {
            currentTick = target + 1L;
        }
    }

    /**
     * Long-distance skip. False when the earliest live tick lies past target,
     * which is the same empty span as an empty wheel.
     */
    private boolean seekDue(long target) {
        long distance = target - currentTick;
        if (distance >= 0 && distance < (long) bucketHead.length * 2L) return true;
        long nextDue = nextScheduledTick();
        if (nextDue <= currentTick) return true;
        if (nextDue > target) return false;
        currentTick = nextDue;
        return true;
    }

    /**
     * Incrementing Long.MAX_VALUE wraps to Long.MIN_VALUE and would reprocess
     * the wheel for an effectively unbounded time.
     */
    private void stepTick() {
        if (currentTick == Long.MAX_VALUE) finalTickConsumed = true;
        else currentTick++;
    }

    /** One tick: detach the bucket and walk it with the re-entry depth raised. */
    private int walkBucket(int bucket, LongConsumer dueConsumer) {
        int slot = detach(bucket);
        if (slot < 0) return 0;
        walkDepth++;
        try {
            int emitted = walkChain(slot, bucket, dueConsumer);
            walkDepth--;
            return emitted;
        } catch (Throwable failure) {
            walkDepth--;
            if (!restored(failure)) taint();
            throw failure;
        }
    }

    /** Only a callback RuntimeException or non-VM Error is known to pass restore(). */
    private static boolean restored(Throwable failure) {
        return failure instanceof RuntimeException
                || (failure instanceof Error && !(failure instanceof VirtualMachineError));
    }

    /** The verbatim scan answers every query until a clear() outside all walks. */
    private void taint() {
        tainted = true;
        indexed = false;
    }

    /** LIFO chain walk: emit due nodes, relink later rotations to their bucket. */
    private int walkChain(int slot, int bucket, LongConsumer dueConsumer) {
        int emitted = 0;
        while (slot >= 0) {
            int following = next[slot];
            if (deadlineTick[slot] <= currentTick) {
                emit(slot, bucket, dueConsumer);
                emitted++;
            } else {
                relink(slot, (int) deadlineTick[slot] & mask);
            }
            slot = following;
        }
        return emitted;
    }

    private void emit(int slot, int bucket, LongConsumer dueConsumer) {
        try {
            dueConsumer.accept(values[slot]);
        } catch (RuntimeException | Error failure) {
            // This bucket was detached before the callback. Restore the failing
            // node and every unvisited successor before propagating the original
            // failure. Nodes already successfully emitted remain released.
            restore(slot, bucket);
            throw failure;
        }
        release(slot);
        size--;
    }

    /** Prepend the failing node and its unvisited successors; fold their minimum. */
    private void restore(int slot, int bucket) {
        int tail = slot;
        long first = deadlineTick[slot];
        while (next[tail] >= 0) {
            tail = next[tail];
            if (deadlineTick[tail] < first) first = deadlineTick[tail];
        }
        next[tail] = bucketHead[bucket];
        bucketHead[bucket] = slot;
        noteLinked(bucket, first);
    }

    private int detach(int bucket) {
        int slot = bucketHead[bucket];
        bucketHead[bucket] = -1;
        if (slot >= 0) noteDetached(bucket);
        return slot;
    }

    private void relink(int slot, int bucket) {
        next[slot] = bucketHead[bucket];
        bucketHead[bucket] = slot;
        noteLinked(bucket, deadlineTick[slot]);
    }

    /** MASK: occupancy and live bits set; bucket and group minimum folded. */
    private void noteLinked(int bucket, long tick) {
        if (!indexed) return;
        int pair = (bucket >>> 6) << 1;
        groupLane[pair + 1] |= 1L << bucket;
        if (tick < groupLane[pair]) groupLane[pair] = tick;
        if (tick < bucketMin[bucket]) bucketMin[bucket] = tick;
        groupBits[(bucket >>> 12) << 1] |= 1L << (bucket >>> 6);
    }

    /** MASK: occupancy cleared, bucket minimum retired, group marked dirty. */
    private void noteDetached(int bucket) {
        if (!indexed) return;
        groupLane[((bucket >>> 6) << 1) + 1] &= ~(1L << bucket);
        bucketMin[bucket] = Long.MAX_VALUE;
        groupBits[((bucket >>> 12) << 1) + 1] |= 1L << (bucket >>> 6);
    }

    /**
     * Earliest linked deadline for a long-distance advance. The lanes are exact;
     * a query from inside a callback meets a detached chain and fails exactly as
     * the scan does. Without lanes the scan answers and may build them.
     */
    private long nextScheduledTick() {
        if (!indexed) return scanAndIndex();
        if (walkDepth != 0) {
            throw new IllegalStateException("invalid timing wheel occupancy");
        }
        refreshDirtyGroups();
        return minLinkedTick();
    }

    /** A scan outside every walk of an untainted wheel builds the exact lanes. */
    private long scanAndIndex() {
        long first = scanScheduledTick();
        if (walkDepth == 0 && !tainted) activate();
        return first;
    }

    private void activate() {
        if (bucketMin == null && !allocateIndex()) return;
        resetIndex();
        indexed = true;
        for (int bucket = 0; bucket < bucketHead.length; bucket++) {
            for (int slot = bucketHead[bucket]; slot >= 0; slot = next[slot]) {
                noteLinked(bucket, deadlineTick[slot]);
            }
        }
    }

    /** OFFSET_ARRAY geometry; all lanes publish together or the scan stays owner. */
    private boolean allocateIndex() {
        int groups = (bucketHead.length + 63) >>> 6;
        try {
            long[] pairs = new long[groups << 1];
            long[] words = new long[((groups + 63) >>> 6) << 1];
            long[] minima = new long[bucketHead.length];
            groupLane = pairs;
            groupBits = words;
            bucketMin = minima;
            return true;
        } catch (OutOfMemoryError exhausted) {
            tainted = true;
            return false;
        }
    }

    private void resetIndex() {
        Arrays.fill(bucketMin, Long.MAX_VALUE);
        for (int pair = 0; pair < groupLane.length; pair += 2) {
            groupLane[pair] = Long.MAX_VALUE;
            groupLane[pair + 1] = 0L;
        }
        Arrays.fill(groupBits, 0L);
    }

    /** BITMAP_WALK over dirty-group words: every dirty group becomes exact. */
    private void refreshDirtyGroups() {
        for (int word = 0; word < groupBits.length; word += 2) {
            for (long dirty = groupBits[word + 1]; dirty != 0L; dirty &= dirty - 1L) {
                refreshGroup(((word >>> 1) << 6) + Long.numberOfTrailingZeros(dirty));
            }
            groupBits[word + 1] = 0L;
        }
    }

    /** BITMAP_WALK over one group's occupied buckets; an empty group leaves the live set. */
    private void refreshGroup(int group) {
        long bits = groupLane[(group << 1) + 1];
        long first = Long.MAX_VALUE;
        for (long walk = bits; walk != 0L; walk &= walk - 1L) {
            long tick = bucketMin[(group << 6) + Long.numberOfTrailingZeros(walk)];
            if (tick < first) first = tick;
        }
        groupLane[group << 1] = first;
        if (bits == 0L) groupBits[(group >>> 6) << 1] &= ~(1L << group);
    }

    /** BITMAP_WALK over live groups: the exact earliest linked deadline. */
    private long minLinkedTick() {
        long first = Long.MAX_VALUE;
        for (int word = 0; word < groupBits.length; word += 2) {
            for (long live = groupBits[word]; live != 0L; live &= live - 1L) {
                int group = ((word >>> 1) << 6) + Long.numberOfTrailingZeros(live);
                if (groupLane[group << 1] < first) first = groupLane[group << 1];
            }
        }
        return first;
    }

    /**
     * Scan only on a long-distance advance: no payload is moved or copied.
     * The earliest due tick cannot be skipped, and the number of live slots
     * must equal the published primitive wheel size before a skip is allowed.
     */
    private long scanScheduledTick() {
        long first = Long.MAX_VALUE;
        int visited = 0;
        for (int head : bucketHead) {
            for (int slot = head; slot >= 0; slot = next[slot]) {
                if (visited++ >= size) {
                    throw new IllegalStateException("invalid timing wheel occupancy");
                }
                if (deadlineTick[slot] < first) first = deadlineTick[slot];
            }
        }
        if (visited != size) {
            throw new IllegalStateException("invalid timing wheel occupancy");
        }
        return first;
    }

    public void clear() {
        Arrays.fill(bucketHead, -1);
        Arrays.fill(values, 0L);
        Arrays.fill(deadlineTick, 0L);
        Arrays.fill(next, 0);
        highWater = 0;
        freeHead = -1;
        size = 0;
        tainted = walkDepth != 0;
        if (tainted) indexed = false;
        else if (indexed) resetIndex();
    }

    private int allocate() {
        if (freeHead >= 0) {
            int slot = freeHead;
            freeHead = next[slot];
            return slot;
        }
        if (highWater == values.length) grow();
        return highWater++;
    }

    private void release(int slot) {
        values[slot] = 0L;
        deadlineTick[slot] = 0L;
        next[slot] = freeHead;
        freeHead = slot;
    }

    /** Package-local fault seam for the one original backing-geometry atom. */
    interface ArrayCopier {
        long[] copy(long[] source, int capacity);
        int[] copy(int[] source, int capacity);
    }

    // One class-level copier, never one node/lease/object per scheduled entry.
    private static final ArrayCopier JDK_COPIER = new ArrayCopier() {
        @Override public long[] copy(long[] source, int capacity) {
            return Arrays.copyOf(source, capacity);
        }
        @Override public int[] copy(int[] source, int capacity) {
            return Arrays.copyOf(source, capacity);
        }
    };

    private void grow() {
        grow(JDK_COPIER);
    }

    /**
     * Prepare every replacement array before publishing any lane. A failure
     * on allocation 1, 2, 3 or malformed test-only copy leaves all old lanes,
     * their active payload, free list and high-water geometry untouched.
     */
    void grow(ArrayCopier copier) {
        Objects.requireNonNull(copier, "copier");
        int capacity = values.length + (values.length >>> 1) + 1;
        long[] grownValues = copier.copy(values, capacity);
        long[] grownDeadlines = copier.copy(deadlineTick, capacity);
        int[] grownNext = copier.copy(next, capacity);
        if (grownValues == null || grownDeadlines == null || grownNext == null
                || grownValues.length != capacity || grownDeadlines.length != capacity
                || grownNext.length != capacity) {
            throw new IllegalStateException("invalid timing wheel growth copy");
        }
        values = grownValues;
        deadlineTick = grownDeadlines;
        next = grownNext;
    }

    private static long ceilDiv(long value, long divisor) {
        long quotient = Math.floorDiv(value, divisor);
        long remainder = Math.floorMod(value, divisor);
        return remainder == 0L ? quotient : Math.addExact(quotient, 1L);
    }
}
