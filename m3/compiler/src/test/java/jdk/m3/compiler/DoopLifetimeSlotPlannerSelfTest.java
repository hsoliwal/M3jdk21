// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.Arrays;
import java.util.Random;

/** Independent interval-overlap oracle for the candidate planner; no JNI or GC claim. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3DoopLifetimeSlotPlannerSelfTest.java blob 63ecd47ae267; recipe m3/compiler/lifetime-slots/recipe/relocate.py v1
public final class DoopLifetimeSlotPlannerSelfTest {
    private DoopLifetimeSlotPlannerSelfTest() { }

    public static void main(String[] args) {
        var empty = DoopLifetimeSlotPlanner.planCandidates(new long[0], new long[0], new long[0]);
        require(empty.requiredSlots() == 0 && empty.count() == 0, "empty");
        var adjacent = DoopLifetimeSlotPlanner.planCandidates(
                new long[]{0, 5, 10}, new long[]{5, 10, 20}, new long[]{3, 2, 1});
        require(adjacent.requiredSlots() == 1, "adjacent endpoints");
        var parallel = DoopLifetimeSlotPlanner.planCandidates(
                new long[]{0, 0, 0}, new long[]{7, 7, 7}, new long[]{2, 1, 3});
        require(parallel.requiredSlots() == 3, "overlap");
        int[] exposed = adjacent.slotsByInput();
        exposed[0] = -1;
        require(adjacent.slotOf(0) == 0, "defensive result");
        long[] starts = {1, 2};
        long[] ends = {3, 4};
        long[] ids = {11, 12};
        var inputGuard = DoopLifetimeSlotPlanner.planCandidates(starts, ends, ids);
        starts[0] = 50; ends[0] = 60; ids[0] = 7;
        require(inputGuard.requiredSlots() == 2, "defensive inputs");
        require(DoopLifetimeSlotPlanner.planCandidates(
                new long[]{Long.MAX_VALUE - 3, Long.MAX_VALUE - 1},
                new long[]{Long.MAX_VALUE - 1, Long.MAX_VALUE},
                new long[]{1, 2}).requiredSlots() == 1, "long endpoints");
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[]{0}, new long[]{0}, new long[]{1}));
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[]{0, 1}, new long[]{2, 3}, new long[]{1, 1}));
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[]{-1}, new long[]{3}, new long[]{1}));
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[]{1}, new long[]{3, 4}, new long[]{1}));
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[]{1}, new long[]{3}, null));
        refuses(() -> DoopLifetimeSlotPlanner.planCandidates(
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1],
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1],
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1]));

        Random random = new Random(0x4D33444F4F50L);
        long checked = 0;
        for (int trial = 0; trial < 6_000; trial++) {
            int n = random.nextInt(35);
            long[] start = new long[n], end = new long[n], atom = new long[n];
            for (int i = 0; i < n; i++) {
                start[i] = random.nextInt(30);
                end[i] = start[i] + 1 + random.nextInt(20);
                atom[i] = 1_000L + i;
            }
            var plan = DoopLifetimeSlotPlanner.planCandidates(start, end, atom);
            require(plan.count() == n, "count");
            require(plan.requiredSlots() == oracleMinSlots(start, end), "minimal slots");
            for (int i = 0; i < n; i++) {
                require(plan.slotOf(i) >= 0 && plan.slotOf(i) < plan.requiredSlots(), "slot range");
                for (int j = i + 1; j < n; j++) {
                    boolean overlap = start[i] < end[j] && start[j] < end[i];
                    require(!overlap || plan.slotOf(i) != plan.slotOf(j), "overlap collision");
                    checked++;
                }
            }
            if (n > 1) {
                long[] reversedStart = new long[n], reversedEnd = new long[n], reversedIds = new long[n];
                for (int i = 0; i < n; i++) {
                    reversedStart[i] = start[n - 1 - i];
                    reversedEnd[i] = end[n - 1 - i];
                    reversedIds[i] = atom[n - 1 - i];
                }
                var reversed = DoopLifetimeSlotPlanner.planCandidates(
                        reversedStart, reversedEnd, reversedIds);
                for (int i = 0; i < n; i++) {
                    require(plan.slotOf(i) == reversed.slotOf(n - 1 - i), "permutation invariance");
                }
            }
        }
        System.out.println("M3_DOOP_LIFETIME_SLOT_ORACLE_PASS trials=6000 pairs=" + checked);
    }

    private static int oracleMinSlots(long[] start, long[] end) {
        long[] sortedStart = start.clone();
        long[] sortedEnd = end.clone();
        Arrays.sort(sortedStart);
        Arrays.sort(sortedEnd);
        int active = 0, peak = 0, finished = 0;
        for (long t : sortedStart) {
            while (finished < sortedEnd.length && sortedEnd[finished] <= t) {
                active--; finished++;
            }
            active++;
            peak = Math.max(peak, active);
        }
        return peak;
    }

    private static void refuses(Runnable callback) {
        try { callback.run(); }
        catch (IllegalArgumentException | NullPointerException expected) { return; }
        throw new AssertionError("invalid candidate admitted");
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
