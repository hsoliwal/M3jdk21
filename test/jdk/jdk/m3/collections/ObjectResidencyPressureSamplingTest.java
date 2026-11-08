// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.function.Function;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/**
 * Lazy pressure sampling (residency fold, leaf PS of recipe RESIDENCY_DONOR_ATOMS v3): without PRESSURE_AWARE the
 * budget boundaries (admission, signalMemoryPressure, both reuse budget checks) take no HeapSnapshot. The probe counts
 * the bytes this thread allocates: an operation that took a snapshot allocates one HeapSnapshot record (48 bytes with
 * compressed class pointers) per boundary, so CALLS boundaries with a snapshot each allocate far more than the bound of
 * one byte per call that the measured loops must stay below (the bound absorbs the few hundred bytes the VM may
 * allocate on the thread outside the operation). With PRESSURE_AWARE the boundaries still sample the running JVM heap.
 */
//FLLDCIM: MRL - residency fold leaf PS, crate m3-doop-reuse-20261030 (m3-doop-pressure-sampling-20261030): no snapshot per fixed-budget boundary
final class ObjectResidencyPressureSamplingTest {
    private static final int CALLS = 20_000;
    private static final Object PROJECTION = new Object();
    private static final Function<Object, Boolean> TOUCH = projection -> Boolean.TRUE;
    private static final ObjectResidency.Reinitializer<Object> RESET = (projection, slot) -> { };
    private static final int FIXED = ObjectResidency.LAZY_CREATE;
    private static final int REUSABLE = ObjectResidency.LAZY_CREATE | ObjectResidency.RECREATE
            | ObjectResidency.STRICT_NO_ESCAPE;
    private static volatile ObjectResidency.Decision sink;

    private static com.sun.management.ThreadMXBean meter() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled(),
                "thread allocation accounting unavailable");
        return bean;
    }

    private static ObjectResidency<Object> owner(int slots, int flags, long budget, long reserve) {
        return new ObjectResidency<>(slots, flags, budget, reserve, slot -> PROJECTION, null, null);
    }

    /** Bytes CALLS iterations of the loop allocated beyond an empty measurement (after 16 linking iterations). */
    private static long allocated(com.sun.management.ThreadMXBean bean, IntConsumer loop) {
        loop.accept(16);
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
    void fixedBudgetPressureSignalTakesNoSnapshot() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(4, FIXED, 64L, 0L);
        arena.use(0, 8L, TOUCH);
        arena.signalMemoryPressure();
        long bytes = allocated(bean, n -> {
            for (int i = 0; i < n; i++) arena.signalMemoryPressure();
        });
        assertBelowOneBytePerCall(bytes, "signalMemoryPressure() allocated without PRESSURE_AWARE");
        assertEquals(ObjectResidency.State.RESIDENT, arena.state(0));
    }

    @Test
    void fixedBudgetAdmissionAllocatesOnlyItsDecision() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(CALLS + 17, FIXED, Long.MAX_VALUE, 0L);
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
        assertBelowOneBytePerCall(admissions - decisions,
                "an admission without PRESSURE_AWARE allocated beyond its Decision");
        assertEquals(CALLS + 16L, arena.residentBytes());
        assertEquals("created", arena.lastDecision().reason());
    }

    @Test
    void fixedBudgetRefusedReuseTakesNoSnapshot() {
        com.sun.management.ThreadMXBean bean = meter();
        ObjectResidency<Object> arena = owner(2, REUSABLE, 16L, 0L);
        arena.use(0, 8L, TOUCH);
        assertFalse(arena.reuseUnborrowedProjection(0, 1, 32L, RESET));
        long bytes = allocated(bean, n -> {
            for (int i = 0; i < n; i++) {
                if (arena.reuseUnborrowedProjection(0, 1, 32L, RESET)) throw new AssertionError("over-budget reuse");
            }
        });
        assertBelowOneBytePerCall(bytes, "a budget-refused reuse allocated without PRESSURE_AWARE");
        assertEquals(ObjectResidency.State.RESIDENT, arena.state(0));
        assertEquals(ObjectResidency.State.ABSENT, arena.state(1));
    }

    @Test
    void pressureAwareBoundariesStillSampleTheRunningHeap() {
        int aware = ObjectResidency.PRESSURE_AWARE;
        // Reserve beyond any headroom: the sampled target is zero at every boundary.
        ObjectResidency<Object> starved = owner(2, FIXED | aware, 64L, Long.MAX_VALUE);
        assertThrows(ObjectResidency.BudgetExceededException.class, () -> starved.use(0, 1L, TOUCH));
        assertEquals(ObjectResidency.State.ABSENT, starved.state(0));
        // No reserve: the sampled headroom exceeds the 64-byte maximum, so the maximum governs, as without the flag.
        ObjectResidency<Object> roomy = owner(2, REUSABLE | aware, 64L, 0L);
        assertEquals(Boolean.TRUE, roomy.use(0, 8L, TOUCH));
        assertFalse(roomy.reuseUnborrowedProjection(0, 1, 65L, RESET));
        roomy.signalMemoryPressure();
        assertEquals(ObjectResidency.State.RESIDENT, roomy.state(0));
        assertEquals(8L, roomy.residentBytes());
        ObjectResidency<Object> fixed = owner(2, FIXED, 64L, Long.MAX_VALUE);
        assertEquals(Boolean.TRUE, fixed.use(0, 1L, TOUCH), "the reserve applies only with PRESSURE_AWARE");
    }
}
