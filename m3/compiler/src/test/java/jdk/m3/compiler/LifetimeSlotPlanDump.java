// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * Differential dump (gate 3 of the relocation, recipe/differential.sh). Replays the oracle inputs of the two
 * self-tests (fixed cases, refusal cases, the 6,000 seeded planCandidates workloads and the 5,000 seeded
 * planCompatibleCandidates workloads, each also in reversed input order) through the planner class named by
 * args[0], reached by reflection so that the donor class and the relocated class run the same driver, and prints
 * one canonical line per plan. Two planners agree when their dumps are byte-identical. The final TOTAL line
 * repeats the oracle figures (cases, interval pairs, workloads) of the self-tests.
 * Usage: java -cp planner-classes;test-classes jdk.m3.compiler.LifetimeSlotPlanDump FQN
 */
public final class LifetimeSlotPlanDump {
    private static final long SEED = 0x4D33444F4F50L;
    private static final int PLANNER_TRIALS = 6_000;
    private static final int COMPATIBLE_TRIALS = 5_000;
    private final Method planCandidates;
    private final Method planCompatibleCandidates;
    private final int maxWindows;
    private final PrintStream out;
    private long cases;
    private long pairs;
    private long compatiblePairs;

    private LifetimeSlotPlanDump(Class<?> planner, PrintStream out) throws ReflectiveOperationException {
        this.planCandidates = planner.getMethod("planCandidates", long[].class, long[].class, long[].class);
        this.planCompatibleCandidates = planner.getMethod("planCompatibleCandidates",
                long[].class, long[].class, long[].class, long[].class);
        this.maxWindows = planner.getField("MAX_WINDOWS").getInt(null);
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        LifetimeSlotPlanDump dump = new LifetimeSlotPlanDump(Class.forName(args[0]), out);
        dump.fixedCases();
        dump.seededCandidates();
        dump.seededCompatibleCandidates();
        out.print("TOTAL cases=" + dump.cases + " pairs=" + dump.pairs + " compatible_pairs=" + dump.compatiblePairs
                + " planner_trials=" + PLANNER_TRIALS + " compatible_workloads=" + COMPATIBLE_TRIALS + "\n");
        out.flush();
    }

    private void fixedCases() {
        long[] none = new long[0];
        long[] over = new long[maxWindows + 1];
        line("F max_windows=" + maxWindows);
        line("F empty " + plan(none, none, none));
        line("F adjacent " + plan(new long[]{0, 5, 10}, new long[]{5, 10, 20}, new long[]{3, 2, 1}));
        line("F parallel " + plan(new long[]{0, 0, 0}, new long[]{7, 7, 7}, new long[]{2, 1, 3}));
        line("F guard " + plan(new long[]{1, 2}, new long[]{3, 4}, new long[]{11, 12}));
        line("F long " + plan(new long[]{Long.MAX_VALUE - 3, Long.MAX_VALUE - 1},
                new long[]{Long.MAX_VALUE - 1, Long.MAX_VALUE}, new long[]{1, 2}));
        line("F refuse_empty_interval " + plan(new long[]{0}, new long[]{0}, new long[]{1}));
        line("F refuse_duplicate_id " + plan(new long[]{0, 1}, new long[]{2, 3}, new long[]{1, 1}));
        line("F refuse_negative_start " + plan(new long[]{-1}, new long[]{3}, new long[]{1}));
        line("F refuse_length " + plan(new long[]{1}, new long[]{3, 4}, new long[]{1}));
        line("F refuse_null_ids " + plan(new long[]{1}, new long[]{3}, null));
        line("F refuse_excess " + plan(over, over, over));
        line("F compatible_empty " + planGroups(none, none, none, none));
        line("F compatible_adjacent " + planGroups(new long[]{0, 5, 0}, new long[]{5, 9, 10},
                new long[]{11, 12, 13}, new long[]{5, 5, 9}));
        line("F compatible_long " + planGroups(new long[]{Long.MAX_VALUE - 5, Long.MAX_VALUE - 2},
                new long[]{Long.MAX_VALUE - 2, Long.MAX_VALUE}, new long[]{1, 2},
                new long[]{Long.MAX_VALUE, Long.MAX_VALUE}));
        line("F compatible_refuse_negative_group " + planGroups(new long[]{1}, new long[]{3}, new long[]{1}, new long[]{-1}));
        line("F compatible_refuse_group_length " + planGroups(new long[]{1}, new long[]{3}, new long[]{1}, none));
        line("F compatible_refuse_duplicate_id " + planGroups(new long[]{1, 2}, new long[]{3, 4},
                new long[]{9, 9}, new long[]{1, 2}));
        line("F compatible_refuse_empty_interval " + planGroups(new long[]{1}, new long[]{1}, new long[]{1}, new long[]{0}));
        line("F compatible_refuse_null_groups " + planGroups(new long[]{1}, new long[]{2}, new long[]{1}, null));
        line("F compatible_refuse_excess " + planGroups(over, over, over, over));
    }

    /** Same generator as DoopLifetimeSlotPlannerSelfTest: seed, trial count, bounds and draw order. */
    private void seededCandidates() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < PLANNER_TRIALS; trial++) {
            int n = random.nextInt(35);
            long[] start = new long[n], end = new long[n], atom = new long[n];
            for (int i = 0; i < n; i++) {
                start[i] = random.nextInt(30);
                end[i] = start[i] + 1 + random.nextInt(20);
                atom[i] = 1_000L + i;
            }
            pairs += (long) n * (n - 1) / 2;
            line("T " + trial + " n=" + n + " " + plan(start, end, atom));
            line("T " + trial + " reversed " + plan(reverse(start), reverse(end), reverse(atom)));
        }
    }

    /** Same generator as DoopCompatibleLifetimeSlotsSelfTest: seed, trial count, bounds and draw order. */
    private void seededCompatibleCandidates() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < COMPATIBLE_TRIALS; trial++) {
            int n = random.nextInt(46);
            long[] start = new long[n], end = new long[n], atom = new long[n], group = new long[n];
            for (int i = 0; i < n; i++) {
                start[i] = random.nextInt(40);
                end[i] = start[i] + 1 + random.nextInt(25);
                atom[i] = 1000L + i;
                group[i] = random.nextInt(6);
            }
            compatiblePairs += (long) n * (n - 1) / 2;
            line("G " + trial + " n=" + n + " " + planGroups(start, end, atom, group));
            line("G " + trial + " reversed " + planGroups(reverse(start), reverse(end), reverse(atom), reverse(group)));
        }
    }

    private String plan(long[] start, long[] end, long[] atom) {
        return invoke(planCandidates, new Object[]{start, end, atom});
    }

    private String planGroups(long[] start, long[] end, long[] atom, long[] group) {
        return invoke(planCompatibleCandidates, new Object[]{start, end, atom, group});
    }

    private String invoke(Method method, Object[] arguments) {
        cases++;
        try {
            Object plan = method.invoke(null, arguments);
            int required = (Integer) plan.getClass().getMethod("requiredSlots").invoke(plan);
            int[] slots = (int[]) plan.getClass().getMethod("slotsByInput").invoke(plan);
            return "P slots=" + required + " " + Arrays.toString(slots);
        } catch (InvocationTargetException refused) {
            Throwable cause = refused.getCause();
            return "R " + cause.getClass().getSimpleName() + " " + cause.getMessage();
        } catch (ReflectiveOperationException broken) {
            throw new IllegalStateException(broken);
        }
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private static long[] reverse(long[] input) {
        long[] result = new long[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[input.length - 1 - i];
        }
        return result;
    }
}
