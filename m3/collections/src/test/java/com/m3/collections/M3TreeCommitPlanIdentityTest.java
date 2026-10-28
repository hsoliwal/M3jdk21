/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Native-handle window plans must commit their exact source-sealed coordinates,
 * physical parent positions and retained handles; invalid commits are atomic.
 * No JNI callbacks, per-row wrapper objects or alternate tree topology.
 */
final class M3TreeCommitPlanIdentityTest {
    private static final int ROOT = -1;

    private static M3TreeWindowPlan committedTwo() {
        M3TreeWindowPlan owner = new M3TreeWindowPlan();
        int[] roots = {ROOT, ROOT};
        int[] children = {0, 1};
        int[] physical = {-1, -1};
        owner.plan(2, roots, children, physical);
        owner.commit(2, roots, children, physical, new long[] {101, 102});
        return owner;
    }

    private static void originalRemains(M3TreeWindowPlan owner) {
        assertEquals(2, owner.size());
        assertEquals(101L, owner.handleOf(ROOT, 0));
        assertEquals(102L, owner.handleOf(ROOT, 1));
        assertEquals(0, owner.entryOf(101));
        assertEquals(1, owner.entryOf(102));
    }

    private static void proposedTwo(M3TreeWindowPlan owner,
                                    int[] parents, int[] children, int[] physical) {
        owner.plan(2, parents, children, physical);
        assertEquals(2, owner.plannedCount());
        assertEquals(-1, owner.previousSibling(0));
        assertEquals(0, owner.previousSibling(1));
    }

    @Test void wrongCoordinateIsRefusedWithoutPublishing() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> owner.commit(2, parents, new int[] {2, 4},
                        physical, new long[] {201, 202}));
        assertEquals("commit differs from planned window", rejected.getMessage());
        originalRemains(owner);
        assertEquals(0L, owner.handleOf(ROOT, 4));
        // A refused publication must not destroy the still-valid pending plan.
        owner.commit(2, parents, children, physical, new long[] {201, 202});
        assertEquals(201L, owner.handleOf(ROOT, 2));
        assertEquals(202L, owner.handleOf(ROOT, 3));
    }

    @Test void wellFormedButDifferentParentTopologyIsRefused() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        // The second row becomes a child of the first; it remains a legal
        // dense preorder, but is not the topology admitted by plan(...).
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> owner.commit(2, parents, children, new int[] {-1, 0},
                        new long[] {201, 202}));
        assertEquals("commit differs from planned window", rejected.getMessage());
        originalRemains(owner);
        owner.commit(2, parents, children, physical, new long[] {201, 202});
        assertEquals(2, owner.size());
    }

    @Test void plannedReusedNativeHandleCannotBeSilentlyReplaced() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {0, 1};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        assertEquals(101L, owner.plannedHandle(0));
        assertEquals(102L, owner.plannedHandle(1));
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> owner.commit(2, parents, children, physical,
                        new long[] {301, 302}));
        assertEquals("retained native handle changed", rejected.getMessage());
        originalRemains(owner);
        assertEquals(-1, owner.entryOf(301));
        owner.commit(2, parents, children, physical, new long[] {101, 102});
        originalRemains(owner);
    }

    @Test void callerArrayMutationCannotChangeThePlannedWindow() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        children[0] = 99;
        assertEquals("commit differs from planned window",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.commit(2, parents, children, physical,
                                new long[] {201, 202})).getMessage());
        originalRemains(owner);
        children[0] = 2;
        owner.commit(2, parents, children, physical, new long[] {201, 202});
        assertEquals(201L, owner.handleOf(ROOT, 2));
    }

    @Test void refusedDuplicateCandidatePlanLeavesPriorPendingSnapshot() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        assertEquals("duplicate tree coordinate",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.plan(2, parents, new int[] {2, 2}, physical)).getMessage());
        assertEquals(2, owner.plannedCount());
        originalRemains(owner);
        owner.commit(2, parents, children, physical, new long[] {201, 202});
        assertEquals(202L, owner.handleOf(ROOT, 3));
    }

    @Test void priorDuplicateAndZeroNativeHandleRefusalsRemain() {
        M3TreeWindowPlan owner = committedTwo();
        int[] parents = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        proposedTwo(owner, parents, children, physical);
        assertEquals("uncommitted native row 1",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.commit(2, parents, children, physical,
                                new long[] {201, 0})).getMessage());
        assertEquals("duplicate native handle",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.commit(2, parents, children, physical,
                                new long[] {201, 201})).getMessage());
        assertEquals("duplicate tree coordinate",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.commit(2, parents, new int[] {2, 2}, physical,
                                new long[] {201, 202})).getMessage());
        originalRemains(owner);
        owner.commit(2, parents, children, physical, new long[] {201, 202});
        assertEquals(2, owner.size());
    }

    @Test void largerDynamicArrayPlanReusesHandlesByLogicalOffset() {
        final int count = 257;
        M3TreeWindowPlan owner = new M3TreeWindowPlan();
        int[] parents = new int[count];
        int[] children = new int[count];
        int[] physical = new int[count];
        long[] handles = new long[count];
        Arrays.fill(parents, ROOT);
        Arrays.fill(physical, -1);
        for (int i = 0; i < count; i++) {
            children[i] = i;
            handles[i] = 1001L + i;
        }
        owner.plan(count, parents, children, physical);
        owner.commit(count, parents, children, physical, handles);
        for (int i = 0; i < count; i++) {
            children[i] = i + 1;
        }
        owner.plan(count, parents, children, physical);
        long[] nextHandles = new long[count];
        for (int i = 0; i < count; i++) {
            nextHandles[i] = owner.plannedHandle(i);
            if (nextHandles[i] == 0) {
                nextHandles[i] = 9000L + i;
            }
        }
        children[count - 1] = 999;
        assertEquals("commit differs from planned window",
                assertThrows(IllegalArgumentException.class,
                        () -> owner.commit(count, parents, children, physical,
                                nextHandles)).getMessage());
        assertEquals(count, owner.size());
        assertEquals(1001L, owner.handleOf(ROOT, 0));
        children[count - 1] = count;
        owner.commit(count, parents, children, physical, nextHandles);
        assertEquals(0L, owner.handleOf(ROOT, 0));
        assertEquals(1002L, owner.handleOf(ROOT, 1));
        assertEquals(nextHandles[count - 1], owner.handleOf(ROOT, count));
    }
}
