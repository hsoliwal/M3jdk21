// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.Arrays;
import java.util.Random;

/** Independent type-aware interval lower bound; does not assert JVM object reachability. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3DoopCompatibleLifetimeSlotsSelfTest.java blob 92361abdb03c; recipe m3/compiler/lifetime-slots/recipe/relocate.py v1
public final class DoopCompatibleLifetimeSlotsSelfTest {
    private DoopCompatibleLifetimeSlotsSelfTest() { }

    public static void main(String[] args) {
        var empty = DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[0], new long[0], new long[0], new long[0]);
        check(empty.requiredSlots() == 0 && empty.count() == 0, "empty");

        var adjacent = DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{0, 5, 0}, new long[]{5, 9, 10},
                new long[]{11, 12, 13}, new long[]{5, 5, 9});
        check(adjacent.requiredSlots() == 2, "same-class reuse; cross-class isolation");
        check(adjacent.slotOf(0) == adjacent.slotOf(1), "half-open reuse");
        check(adjacent.slotOf(0) != adjacent.slotOf(2), "different reset classes");
        int[] exported = adjacent.slotsByInput();
        exported[0] = -7;
        check(adjacent.slotOf(0) >= 0, "defensive output");

        check(DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{Long.MAX_VALUE - 5, Long.MAX_VALUE - 2},
                new long[]{Long.MAX_VALUE - 2, Long.MAX_VALUE},
                new long[]{1, 2}, new long[]{Long.MAX_VALUE, Long.MAX_VALUE})
                .requiredSlots() == 1, "long boundaries without overflow");

        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{1}, new long[]{3}, new long[]{1}, new long[]{-1}));
        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{1}, new long[]{3}, new long[]{1}, new long[0]));
        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{1, 2}, new long[]{3, 4}, new long[]{9, 9}, new long[]{1, 2}));
        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{1}, new long[]{1}, new long[]{1}, new long[]{0}));
        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[]{1}, new long[]{2}, new long[]{1}, null));
        refuses(() -> DoopLifetimeSlotPlanner.planCompatibleCandidates(
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1],
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1],
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1],
                new long[DoopLifetimeSlotPlanner.MAX_WINDOWS + 1]));

        Random random = new Random(0x4D33444F4F50L);
        long pairs = 0;
        for (int trial = 0; trial < 5000; trial++) {
            int n = random.nextInt(46);
            long[] start = new long[n], end = new long[n];
            long[] atom = new long[n], group = new long[n];
            for (int i = 0; i < n; i++) {
                start[i] = random.nextInt(40);
                end[i] = start[i] + 1 + random.nextInt(25);
                atom[i] = 1000L + i;
                group[i] = random.nextInt(6);
            }
            var plan = DoopLifetimeSlotPlanner.planCompatibleCandidates(start, end, atom, group);
            check(plan.count() == n, "candidate count");
            check(plan.requiredSlots() == oracle(start, end, group), "per-class minimal slots");
            for (int i = 0; i < n; i++) {
                check(plan.slotOf(i) >= 0 && plan.slotOf(i) < plan.requiredSlots(), "slot range");
                for (int j = i + 1; j < n; j++) {
                    boolean clash = group[i] != group[j]
                            || (start[i] < end[j] && start[j] < end[i]);
                    check(!clash || plan.slotOf(i) != plan.slotOf(j),
                            "incompatible or overlapping shells cannot share");
                    pairs++;
                }
            }
            if (n > 1) {
                long[] rs = reverse(start), re = reverse(end),
                        ra = reverse(atom), rg = reverse(group);
                var reversed = DoopLifetimeSlotPlanner.planCompatibleCandidates(rs, re, ra, rg);
                check(reversed.requiredSlots() == plan.requiredSlots(), "permuted capacity");
                for (int i = 0; i < n; i++) {
                    check(reversed.slotOf(n - i - 1) == plan.slotOf(i),
                            "permuted canonical atom assignment");
                }
            }
        }
        System.out.println("M3_DOOP_COMPATIBLE_LIFETIME_PASS trials=5000 pairs=" + pairs);
    }

    /** Independently sort starts and ends per category to compute sum of peak overlaps. */
    private static int oracle(long[] starts, long[] ends, long[] groups) {
        long[] sortedGroups = groups.clone();
        Arrays.sort(sortedGroups);
        int total = 0;
        for (int i = 0; i < sortedGroups.length; ) {
            long category = sortedGroups[i];
            int k = 0;
            while (i < sortedGroups.length && sortedGroups[i] == category) { i++; k++; }
            long[] s = new long[k], e = new long[k];
            int w = 0;
            for (int j = 0; j < groups.length; j++) {
                if (groups[j] == category) { s[w] = starts[j]; e[w++] = ends[j]; }
            }
            Arrays.sort(s);
            Arrays.sort(e);
            int active = 0, peak = 0, finished = 0;
            for (long t : s) {
                while (finished < e.length && e[finished] <= t) { active--; finished++; }
                active++;
                peak = Math.max(peak, active);
            }
            total += peak;
        }
        return total;
    }

    private static long[] reverse(long[] input) {
        long[] out = input.clone();
        for (int l = 0, r = out.length - 1; l < r; l++, r--) {
            long swap = out[l];
            out[l] = out[r];
            out[r] = swap;
        }
        return out;
    }

    private static void refuses(Runnable candidate) {
        try { candidate.run(); }
        catch (IllegalArgumentException | NullPointerException expected) { return; }
        throw new AssertionError("unverified candidate was admitted");
    }

    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }
}
