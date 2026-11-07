// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.function.Function;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/**
 * PRESSURE_AWARE budget boundaries allocate nothing (recipe RESIDENCY_PRESSURE_ALLOC v1,
 * qualification/residency-pressure-alloc-20261107). targetBudget() reads the heap headroom into primitives, so the
 * resident use() hit path, signalMemoryPressure(), a budget-refused reuse and an admission (net of its Decision) take
 * no HeapSnapshot: interpreted, C1 or C2, with or without scalar replacement. A boundary that built the 48-byte record
 * allocates far more than the bound of one byte per call over CALLS calls (the bound absorbs the few hundred bytes the
 * VM may allocate on the thread outside the operation). Reserve 0 keeps the sampled headroom above the 64-byte
 * maximum, so the maximum governs and every loop stays on the same path.
 */
//FLLDCIM: HPCPOP - residency-pressure-alloc-20261107: the PRESSURE_AWARE boundaries are allocation-free
final class M3ObjectResidencyPressureAllocationTest {
    private static final int CALLS = 20_000;
    private static final int WARMUP_CALLS = 16;
    private static final long BUDGET = 64L;
    private static final long PROJECTION_BYTES = 8L;
    private static final long OVER_BUDGET_BYTES = BUDGET + 1L;
    private static final Object PROJECTION = new Object();
    private static final Function<Object, Boolean> TOUCH = projection -> Boolean.TRUE;
    private static final ObjectResidency.Reinitializer<Object> RESET = (projection, slot) -> { };
    private static final int AWARE = ObjectResidency.LAZY_CREATE | ObjectResidency.PRESSURE_AWARE;
    private static final int AWARE_REUSABLE = AWARE | ObjectResidency.RECREATE
            | ObjectResidency.STRICT_NO_ESCAPE;
    private static volatile ObjectResidency.Decision sink;

    private static com.sun.management.ThreadMXBean meter() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled(),
                "thread allocation accounting unavailable");
        return bean;
    }

    private static ObjectResidency<Object> owner(int slots, int flags, long budget) {
        return new ObjectResidency<>(slots, flags, budget, 0L, slot -> PROJECTION, null, null);
    }

    /** Bytes CALLS iterations of the loop allocated beyond an empty measurement (after WARMUP_CALLS iterations). */
    private static long allocated(com.sun.management.ThreadMXBean bean, IntConsumer loop) {
        loop.accept(WARMUP_CALLS);
        long empty = bean.getCurrentThreadAllocatedBytes();
        empty = bean.getCurrentThreadAllocatedBytes() - empty;
        long before = bean.getCurrentThreadAllocatedBytes();
        loop.accept(CALLS);
        return bean.getCurrentThreadAllocatedBytes() - before - empty;
    }

    private static void assertBelowOneBytePerCall(long bytes, String message) {
        if (bytes >= CALLS) throw new AssertionError(message + ": " + bytes + " bytes for " + CALLS + " calls");
    }

    @Test
    void pressureAwareResidentUseTakesNoSnapshot() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(2, AWARE, BUDGET);
        assertEquals(Boolean.TRUE, arena.use(0, PROJECTION_BYTES, TOUCH));
        long bytes = allocated(bean, n -> {
            for (int i = 0; i < n; i++) arena.use(0, PROJECTION_BYTES, TOUCH);
        });
        assertBelowOneBytePerCall(bytes, "a PRESSURE_AWARE resident use() allocated");
        assertEquals(PROJECTION_BYTES, arena.residentBytes());
    }

    @Test
    void pressureAwareSignalTakesNoSnapshot() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(2, AWARE, BUDGET);
        arena.use(0, PROJECTION_BYTES, TOUCH);
        long bytes = allocated(bean, n -> {
            for (int i = 0; i < n; i++) arena.signalMemoryPressure();
        });
        assertBelowOneBytePerCall(bytes, "a PRESSURE_AWARE signalMemoryPressure() allocated");
        assertEquals(ObjectResidency.State.RESIDENT, arena.state(0));
    }

    @Test
    void pressureAwareRefusedReuseTakesNoSnapshot() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(2, AWARE_REUSABLE, BUDGET);
        arena.use(0, PROJECTION_BYTES, TOUCH);
        assertFalse(arena.reuseUnborrowedProjection(0, 1, OVER_BUDGET_BYTES, RESET));
        long bytes = allocated(bean, n -> {
            for (int i = 0; i < n; i++) {
                if (arena.reuseUnborrowedProjection(0, 1, OVER_BUDGET_BYTES, RESET)) {
                    throw new AssertionError("over-budget reuse");
                }
            }
        });
        assertBelowOneBytePerCall(bytes, "a PRESSURE_AWARE budget-refused reuse allocated");
        assertEquals(ObjectResidency.State.ABSENT, arena.state(1));
    }

    @Test
    void pressureAwareAdmissionAllocatesOnlyItsDecision() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(CALLS + WARMUP_CALLS + 1, AWARE, Long.MAX_VALUE);
        int[] next = {0};
        long admissions = allocated(bean, n -> {
            for (int end = next[0] + n; next[0] < end; next[0]++) arena.use(next[0], 1L, TOUCH);
        });
        long decisions = allocated(bean, n -> {
            for (int slot = 0; slot < n; slot++) {
                sink = new ObjectResidency.Decision(slot, ObjectResidency.State.ABSENT,
                        ObjectResidency.State.RESIDENT, slot, Long.MAX_VALUE, "created");
            }
        });
        assertBelowOneBytePerCall(admissions - decisions, "a PRESSURE_AWARE admission allocated beyond its Decision");
        assertEquals((long) CALLS + WARMUP_CALLS, arena.residentBytes());
    }
}
