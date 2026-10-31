/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.testing.TestKernels;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

final class PlanTest {

    @Test
    void launchesPaddedGridAndKernelGuardsLogicalBound() {
        int n = 1000;
        IntBuffer a = IntBuffer.of(IntStream.range(0, n).toArray());
        IntBuffer b = IntBuffer.of(IntStream.range(0, n).map(i -> 3 * i).toArray());
        IntBuffer out = IntBuffer.allocate(n);
        IntBuffer config = IntBuffer.of(n);
        try (Plan plan = Plan.builder(Backend.reference())
                .kernel(TestKernels.ADD)
                .grid(new Grid1D(n, 64))
                .bind(a, Transfer.ONCE)
                .bind(b, Transfer.ONCE)
                .bind(out, Transfer.ON_DEMAND)
                .bind(config, Transfer.EACH_LAUNCH)
                .build()) {
            assertEquals(1024, plan.grid().launchSize());
            Result result = plan.execute();
            assertArrayEquals(new int[n], out.toArray(), "no implicit device-to-host copy");
            result.download(out);
            assertArrayEquals(IntStream.range(0, n).map(i -> 4 * i).toArray(), out.toArray());
            assertEquals(1, result.launch());
            assertSame(plan, result.plan());
        }
    }

    @Test
    void paddedWorkItemsRunExactlyLikeOnADevice() {
        IntBuffer out = IntBuffer.allocate(320);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.TOUCH).grid(new Grid1D(300, 64))
                .bind(out, Transfer.ON_DEMAND).build()) {
            plan.execute().download(out);
        }
        assertArrayEquals(IntStream.rangeClosed(1, 320).toArray(), out.toArray());
    }

    @Test
    void onceIsResidentEachLaunchIsRefreshedAndUploadPatches() {
        IntBuffer state = IntBuffer.of(10, 20, 30, 40);
        IntBuffer config = IntBuffer.of(3, 5);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.ACCUMULATE).grid(new Grid1D(3, 2))
                .bind(state, Transfer.ONCE).bind(config, Transfer.EACH_LAUNCH).build()) {
            plan.execute();
            state.set(0, 1_000);
            config.set(1, 100);
            Result second = plan.execute().download(state);
            assertArrayEquals(new int[] {115, 125, 135, 40}, state.toArray(), "ONCE ignores host writes");

            state.set(3, 7);
            second.upload(state);
            config.set(0, 4);
            config.set(1, 1);
            plan.execute().download(state);
            assertArrayEquals(new int[] {116, 126, 136, 8}, state.toArray());
            assertEquals(3, plan.session().launches());
        }
    }

    @Test
    void onDemandStartsZeroAndIsNeverUploadedImplicitly() {
        IntBuffer state = IntBuffer.of(1, 1);
        IntBuffer config = IntBuffer.of(2, 9);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.ACCUMULATE).grid(new Grid1D(2, 1))
                .bind(state, Transfer.ON_DEMAND).bind(config, Transfer.EACH_LAUNCH).build()) {
            Result result = plan.execute().download(state);
            assertArrayEquals(new int[] {9, 9}, state.toArray());
            result.downloadAll();
            assertArrayEquals(new int[] {2, 9}, config.toArray());
        }
    }

    @Test
    void staleClosedAndUnboundTransfersAreRejected() {
        IntBuffer state = IntBuffer.of(0);
        IntBuffer config = IntBuffer.of(1, 1);
        Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.ACCUMULATE).grid(new Grid1D(1, 1))
                .bind(state, Transfer.ONCE).bind(config, Transfer.EACH_LAUNCH).build();
        Result first = plan.execute();
        Result second = plan.execute();
        assertThrows(IllegalStateException.class, () -> first.download(state));
        assertThrows(IllegalStateException.class, () -> first.upload(state));
        assertThrows(IllegalArgumentException.class, () -> second.download(IntBuffer.allocate(1)));
        assertTrue(plan.isOpen());

        plan.close();
        plan.close();
        assertFalse(plan.isOpen());
        assertThrows(IllegalStateException.class, plan::execute);
        assertThrows(IllegalStateException.class, () -> second.download(state));
    }

    @Test
    void builderRejectsIncompleteDuplicateAndUnsupportedPlans() {
        IntBuffer buffer = IntBuffer.allocate(1);
        assertThrows(IllegalStateException.class, () -> Plan.builder(Backend.reference()).build());
        assertThrows(IllegalStateException.class,
                () -> Plan.builder(Backend.reference()).kernel(TestKernels.TOUCH).grid(new Grid1D(1, 1)).build());
        assertThrows(IllegalArgumentException.class,
                () -> Plan.builder(Backend.reference()).bind(buffer, Transfer.ONCE).bind(buffer, Transfer.ONCE));
        assertThrows(IllegalArgumentException.class, () -> Plan.builder(Backend.reference())
                .kernel(TestKernels.MISSING).grid(new Grid1D(1, 1)).bind(buffer, Transfer.ONCE).build());
    }

    @Test
    void recordRejectsASessionPreparedForAnotherPlan() {
        IntBuffer out = IntBuffer.allocate(1);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.TOUCH).grid(new Grid1D(1, 1))
                .bind(out, Transfer.ON_DEMAND).build()) {
            Plan same = new Plan(plan.backend(), plan.kernel(), plan.grid(), plan.bindings(), plan.session());
            assertEquals(plan, same);
            assertThrows(IllegalArgumentException.class, () -> new Plan(plan.backend(), plan.kernel(),
                    new Grid1D(2, 1), plan.bindings(), plan.session()));
            assertThrows(IllegalArgumentException.class, () -> new Plan(plan.backend(), plan.kernel(), plan.grid(),
                    List.of(new Plan.Binding(IntBuffer.allocate(1), Transfer.ON_DEMAND)), plan.session()));
            assertThrows(UnsupportedOperationException.class, () -> plan.bindings().clear());
        }
    }

    @Test
    void kernelFailurePropagatesFromExecute() {
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.FAIL).grid(new Grid1D(64, 8))
                .bind(IntBuffer.allocate(1), Transfer.ON_DEMAND).build()) {
            assertThrows(IllegalStateException.class, plan::execute);
            assertEquals(0, plan.session().launches());
        }
    }
}
