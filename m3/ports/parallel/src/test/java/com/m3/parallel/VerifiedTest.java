/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.testing.TestKernels;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

final class VerifiedTest {
    private static final int N = 777;

    @Test
    void referenceRunsAreVerifiedAndContentAddressed() {
        Verified first = verifyAdd(Backend.reference(), 1);
        Verified second = verifyAdd(Backend.reference(), 1);
        assertEquals(first, second, "identical inputs give identical receipts");
        assertTrue(first.root().matches("[0-9a-f]{64}"));
        assertEquals("java", first.backend());
        assertEquals(TestKernels.ADD, first.kernel());
        assertNotEquals(first.inputRoot(), first.outputRoot());
        assertEquals(first, new Verified(first.backend(), first.kernel(), first.grid(), first.launches(),
                first.inputRoot(), first.outputRoot(), first.root()));
        assertThrows(IllegalArgumentException.class, () -> new Verified(first.backend(), first.kernel(),
                first.grid(), 2, first.inputRoot(), first.outputRoot(), first.root()));
        assertThrows(IllegalArgumentException.class, () -> new Verified(first.backend(), first.kernel(),
                first.grid(), 1, "ABC", first.outputRoot(), null));
        assertNotEquals(first.root(), verifyAdd(Backend.reference(), 2).root());
    }

    @Test
    void multiLaunchVerificationLeavesVerifiedOutputsInHostBuffers() {
        IntBuffer state = IntBuffer.of(IntStream.range(0, N).toArray());
        IntBuffer config = IntBuffer.of(N, 3);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(TestKernels.ACCUMULATE).grid(new Grid1D(N, 32))
                .bind(state, Transfer.ONCE).bind(config, Transfer.EACH_LAUNCH).build()) {
            Verified receipt = Verified.run(plan, 4);
            assertEquals(4, receipt.launches());
            assertArrayEquals(IntStream.range(0, N).map(i -> i + 12).toArray(), state.toArray());
            assertTrue(plan.isOpen());
            assertEquals(4, plan.session().launches());
            assertThrows(IllegalStateException.class, () -> Verified.run(plan), "plan is no longer fresh");
        }
    }

    @Test
    void divergentBackendIsRejected() {
        IllegalStateException mismatch = assertThrows(IllegalStateException.class,
                () -> verifyAdd(Backend.of("test-faulty"), 1));
        assertTrue(mismatch.getMessage().contains("diverged"), mismatch.getMessage());
    }

    @Test
    void closedPlansAndBadLaunchCountsAreRejected() {
        Plan plan = addPlan(Backend.reference(), IntBuffer.allocate(N));
        assertThrows(IllegalArgumentException.class, () -> Verified.run(plan, 0));
        plan.close();
        assertThrows(IllegalStateException.class, () -> Verified.run(plan));
    }

    private static Verified verifyAdd(Backend backend, int launches) {
        IntBuffer out = IntBuffer.allocate(N);
        try (Plan plan = addPlan(backend, out)) {
            Verified receipt = Verified.run(plan, launches);
            assertArrayEquals(IntStream.range(0, N).map(i -> 3 * i + 1).toArray(), out.toArray());
            return receipt;
        }
    }

    private static Plan addPlan(Backend backend, IntBuffer out) {
        return Plan.builder(backend)
                .kernel(TestKernels.ADD)
                .grid(new Grid1D(N, 64))
                .bind(IntBuffer.of(IntStream.range(0, N).toArray()), Transfer.ONCE)
                .bind(IntBuffer.of(IntStream.range(0, N).map(i -> 2 * i + 1).toArray()), Transfer.ONCE)
                .bind(out, Transfer.ON_DEMAND)
                .bind(IntBuffer.of(N), Transfer.EACH_LAUNCH)
                .build();
    }
}
