/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exact original owner plus deterministic allocation-failure atomicity proof. */
public final class M3LongTimingWheelAtomicGrowthSelfTest {
    private static long checks;
    private M3LongTimingWheelAtomicGrowthSelfTest() { }

    private static void check(boolean yes, String where) {
        checks++;
        if (!yes) throw new AssertionError(where);
    }

    private static Object field(M3LongTimingWheel wheel, String name) {
        try {
            Field f = M3LongTimingWheel.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(wheel);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static M3LongTimingWheel full() {
        M3LongTimingWheel wheel = new M3LongTimingWheel(2, 1L, 0L);
        for (int i = 0; i < 8; i++) {
            wheel.schedule(1000L + i, 20L + i % 5);
        }
        check(wheel.size() == 8, "pre-growth size");
        check(((long[]) field(wheel, "values")).length == 8, "pre-growth value length");
        return wheel;
    }

    private static void proveFailure(int phase, boolean malformed, int iteration) {
        M3LongTimingWheel wheel = full();
        long[] values = (long[]) field(wheel, "values");
        long[] deadlines = (long[]) field(wheel, "deadlineTick");
        int[] next = (int[]) field(wheel, "next");
        int[] heads = (int[]) field(wheel, "bucketHead");
        long[] oldValues = values.clone();
        long[] oldDeadlines = deadlines.clone();
        int[] oldNext = next.clone();
        int[] oldHeads = heads.clone();
        long beforeBytes = wheel.payloadBytes();
        int highWater = (int) field(wheel, "highWater");
        int freeHead = (int) field(wheel, "freeHead");
        OutOfMemoryError oom = new OutOfMemoryError("synthetic lane " + phase);
        int[] copies = {0};
        M3LongTimingWheel.ArrayCopier copier = new M3LongTimingWheel.ArrayCopier() {
            @Override public long[] copy(long[] source, int capacity) {
                int at = ++copies[0];
                if (!malformed && at == phase) throw oom;
                if (malformed && at == phase) return iteration % 2 == 0 ? null
                        : Arrays.copyOf(source, capacity - 1);
                return Arrays.copyOf(source, capacity);
            }
            @Override public int[] copy(int[] source, int capacity) {
                int at = ++copies[0];
                if (!malformed && at == phase) throw oom;
                if (malformed && at == phase) return iteration % 2 == 0 ? null
                        : Arrays.copyOf(source, capacity - 1);
                return Arrays.copyOf(source, capacity);
            }
        };
        boolean rejected = false;
        try { wheel.grow(copier); }
        catch (OutOfMemoryError error) {
            check(!malformed && error == oom, "wrong OOME identity");
            rejected = true;
        } catch (IllegalStateException malformedCopy) {
            check(malformed, "unexpected invalid copy");
            check("invalid timing wheel growth copy".equals(malformedCopy.getMessage()),
                    "wrong invalid-copy diagnostic");
            rejected = true;
        }
        check(rejected, "growth failure was not rejected");
        check(copies[0] >= phase, "failure stage not reached");
        check(field(wheel, "values") == values, "values lane published prematurely");
        check(field(wheel, "deadlineTick") == deadlines, "deadline lane published prematurely");
        check(field(wheel, "next") == next, "free-index lane published prematurely");
        check(Arrays.equals(oldValues, values), "old values mutated");
        check(Arrays.equals(oldDeadlines, deadlines), "old deadlines mutated");
        check(Arrays.equals(oldNext, next), "old links mutated");
        check(Arrays.equals(oldHeads, heads), "old buckets mutated");
        check(wheel.size() == 8, "size modified after failed grow");
        check((int) field(wheel, "highWater") == highWater, "high-water consumed");
        check((int) field(wheel, "freeHead") == freeHead, "free list modified");
        check(wheel.payloadBytes() == beforeBytes, "published capacity/size mismatch");
        // The ninth public schedule MUST retry the real grow() and recover.
        wheel.schedule(1900L, 29L);
        check(wheel.size() == 9, "schedule retry failed");
        int capacity = ((long[]) field(wheel, "values")).length;
        check(capacity == 13, "original growth policy changed");
        check(((long[]) field(wheel, "deadlineTick")).length == capacity,
                "deadline geometry drift");
        check(((int[]) field(wheel, "next")).length == capacity, "link geometry drift");
        Set<Long> expected = new HashSet<>();
        for (int i = 0; i < 8; i++) expected.add(1000L + i);
        expected.add(1900L);
        List<Long> found = new ArrayList<>();
        check(wheel.advanceTo(30L, found::add) == 9, "drain lost scheduled entry");
        check(new HashSet<>(found).equals(expected), "wrong recovered ID set");
        check(found.size() == expected.size(), "duplicates after recovery");
        check(wheel.size() == 0, "live nodes after recovery");
    }

    private static void smoke() {
        M3LongTimingWheel wheel = full();
        // No OOME or malformed copier: the test-only seam must preserve the
        // incumbent allocation capacity and all due times in the normal case.
        wheel.grow(new M3LongTimingWheel.ArrayCopier() {
            @Override public long[] copy(long[] source, int capacity) {
                return Arrays.copyOf(source, capacity);
            }
            @Override public int[] copy(int[] source, int capacity) {
                return Arrays.copyOf(source, capacity);
            }
        });
        check(((long[]) field(wheel, "values")).length == 13, "normal copy capacity");
        check(wheel.size() == 8, "normal copy size");
        check(wheel.advanceTo(30L, value -> { }) == 8, "normal copy changed deadlines");
        check(wheel.size() == 0, "normal copy failed to drain");
        try {
            wheel.grow(null);
            throw new AssertionError("null copier unexpectedly accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    public static void main(String[] args) {
        // Keep a meaningful many-cycle oracle without huge memory/copy churn.
        for (int i = 0; i < 1200; i++) {
            for (int phase = 1; phase <= 3; phase++) {
                proveFailure(phase, false, i);
                proveFailure(phase, true, i);
            }
        }
        smoke();
        System.out.println("PASS M3 timing-wheel triple-lane atomic growth checks=" + checks);
    }
}
