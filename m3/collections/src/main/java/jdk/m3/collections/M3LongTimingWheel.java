/*******************************************************************************
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
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongTimingWheel.java}
 * blob {@code 17ddb833f940a3439c185137d6105480e0def60a} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class M3LongTimingWheel {
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

    public M3LongTimingWheel(int wheelSize, long tickNanos, long startNanos) {
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
        int bucket = (int) tick & mask;
        next[slot] = bucketHead[bucket];
        bucketHead[bucket] = slot;
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
            // An empty wheel has no callbacks and no per-tick work. Preserve
            // the exact cursor that the original tick-by-tick loop produced.
            if (size == 0) {
                advanceEmptyTo(target);
                break;
            }
            long distance = target - currentTick;
            if (distance < 0 || distance >= (long) bucketHead.length * 2L) {
                long nextDue = nextScheduledTick();
                if (nextDue > currentTick) {
                    if (nextDue > target) {
                        advanceEmptyTo(target);
                        break;
                    }
                    currentTick = nextDue;
                }
            }
            int bucket = (int) currentTick & mask;
            int slot = bucketHead[bucket];
            bucketHead[bucket] = -1;
            while (slot >= 0) {
                int following = next[slot];
                if (deadlineTick[slot] <= currentTick) {
                    try {
                        dueConsumer.accept(values[slot]);
                    } catch (RuntimeException | Error failure) {
                        // This bucket was detached before the callback. Restore
                        // the failing node and every unvisited successor before
                        // propagating the original failure. Nodes already
                        // successfully emitted remain released.
                        int tail = slot;
                        while (next[tail] >= 0) tail = next[tail];
                        next[tail] = bucketHead[bucket];
                        bucketHead[bucket] = slot;
                        throw failure;
                    }
                    release(slot);
                    size--;
                    emitted++;
                } else {
                    int targetBucket = (int) deadlineTick[slot] & mask;
                    next[slot] = bucketHead[targetBucket];
                    bucketHead[targetBucket] = slot;
                }
                slot = following;
            }
            // Incrementing Long.MAX_VALUE wraps to Long.MIN_VALUE and
            // would reprocess the wheel for an effectively unbounded time.
            if (currentTick == Long.MAX_VALUE) finalTickConsumed = true;
            else currentTick++;
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
     * Scan only on a long-distance advance: no payload is moved or copied.
     * The earliest due tick cannot be skipped, and the number of live slots
     * must equal the published primitive wheel size before a skip is allowed.
     */
    private long nextScheduledTick() {
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
