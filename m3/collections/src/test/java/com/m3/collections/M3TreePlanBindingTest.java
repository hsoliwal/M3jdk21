// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Mixed-overload source-identity contract for the original native Tree window owner. */
final class M3TreePlanBindingTest {
    private static M3TreeProjection.Residency roots(int first) {
        M3TreeTopology topology = new M3TreeTopology();
        topology.setChildCount(-1, 32);
        M3TreeProjection.Residency rows = new M3TreeProjection.Residency();
        new M3TreeProjection(topology).residencyWindow(first, 2, rows);
        assertEquals(2, rows.size());
        assertEquals(-1, rows.parentId(0));
        assertEquals(first, rows.childIndex(0));
        return rows;
    }

    private static int[] rootParents() { return new int[] {-1, -1}; }
    private static int[] rootPhysical() { return new int[] {-1, -1}; }

    @Test
    void successfulDirectPlanInvalidatesEarlierResidencyCommitBinding() {
        M3TreeWindowPlan planner = new M3TreeWindowPlan();
        planner.plan(roots(0));
        int[] parents = rootParents(), indices = {8, 9}, physical = rootPhysical();
        planner.plan(2, parents, indices, physical);
        assertThrows(IllegalStateException.class, () -> planner.commit(new long[] {801, 901}),
                "A direct plan cannot commit a stale earlier Residency snapshot");
        assertEquals(0, planner.size(), "refusal must not publish wrong logical rows");
        planner.commit(2, parents, indices, physical, new long[] {801, 901});
        assertEquals(801, planner.handleOf(-1, 8));
        assertEquals(901, planner.handleOf(-1, 9));
        assertEquals(0, planner.handleOf(-1, 0));
    }

    @Test
    void rejectedDirectPlanDoesNotDestroyEarlierValidResidencyBinding() {
        M3TreeWindowPlan planner = new M3TreeWindowPlan();
        planner.plan(roots(2));
        int[] indices = {8, 9};
        assertThrows(IllegalArgumentException.class,
                () -> planner.plan(2, rootParents(), indices, new int[] {-1, 1}),
                "A physical parent cannot be its own entry");
        planner.commit(new long[] {201, 301});
        assertEquals(201, planner.handleOf(-1, 2));
        assertEquals(301, planner.handleOf(-1, 3));
        assertEquals(0, planner.handleOf(-1, 8));
    }

    @Test
    void residencyAfterDirectPlanRebindsItsOwnCoordinateSnapshot() {
        M3TreeWindowPlan planner = new M3TreeWindowPlan();
        planner.plan(2, rootParents(), new int[] {8, 9}, rootPhysical());
        planner.plan(roots(4));
        planner.commit(new long[] {401, 501});
        assertEquals(401, planner.handleOf(-1, 4));
        assertEquals(501, planner.handleOf(-1, 5));
        assertEquals(0, planner.handleOf(-1, 8));
    }

    @Test
    void ordinaryRepeatedResidencyPlansKeepCommittedNativeIdentity() {
        M3TreeWindowPlan planner = new M3TreeWindowPlan();
        planner.plan(roots(0));
        planner.commit(new long[] {101, 102});
        planner.plan(roots(1));
        assertEquals(102, planner.plannedHandle(0), "overlap keeps the actual native handle");
        planner.commit(new long[] {102, 103});
        assertEquals(102, planner.handleOf(-1, 1));
        assertEquals(103, planner.handleOf(-1, 2));
        assertEquals(0, planner.handleOf(-1, 0));
    }

    @Test
    void mixedOverloadsRemainIndependentAcrossRepeatedWindowMoves() {
        M3TreeWindowPlan planner = new M3TreeWindowPlan();
        for (int move = 0; move < 1024; move++) {
            planner.clear();
            planner.plan(roots(move % 25));
            int child = 20 + (move % 7);
            int[] parents = rootParents(), indices = {child, child + 1};
            int[] physical = rootPhysical();
            planner.plan(2, parents, indices, physical);
            assertThrows(IllegalStateException.class, () -> planner.commit(new long[] {7001, 7002}));
            planner.commit(2, parents, indices, physical, new long[] {7001, 7002});
            assertEquals(7001, planner.handleOf(-1, child));
            assertEquals(7002, planner.handleOf(-1, child + 1));
        }
    }
}
