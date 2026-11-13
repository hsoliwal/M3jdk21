// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project

import jdk.m3.tools.cpu.M3ArcConsistency;
import jdk.m3.tools.cpu.M3TwoSat;
import java.util.Arrays;
import java.util.Random;

/** Independent finite exhaustive Boolean/constraint oracle; no JUnit runtime needed. */
public final class M3Wave8ConstraintContract {
    private static long checks;
    private M3Wave8ConstraintContract() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) { throw new AssertionError(why + " at check " + checks); }
    }

    private static boolean lit(int literal, int bits) {
        boolean v = ((bits >>> (Math.abs(literal) - 1)) & 1) != 0;
        return literal > 0 ? v : !v;
    }

    private static boolean bruteSat(int count, int[] left, int[] right) {
        for (int bits = 0; bits < 1 << count; bits++) {
            boolean satisfied = true;
            for (int clause = 0; clause < left.length; clause++) {
                if (!lit(left[clause], bits) && !lit(right[clause], bits)) {
                    satisfied = false;
                    break;
                }
            }
            if (satisfied) { return true; }
        }
        return false;
    }

    private static boolean witness(int[] left, int[] right, boolean[] result) {
        for (int clause = 0; clause < left.length; clause++) {
            int a = left[clause], b = right[clause];
            boolean av = result[Math.abs(a) - 1] == (a > 0);
            boolean bv = result[Math.abs(b) - 1] == (b > 0);
            if (!av && !bv) { return false; }
        }
        return true;
    }

    private static void twoSat() {
        check(M3TwoSat.solve(0, new int[0], new int[0], 0).length == 0, "empty CNF");
        check(M3TwoSat.verify(0, new int[0], new int[0], new boolean[0]), "empty witness");
        check(M3TwoSat.solve(1, new int[]{1,-1}, new int[]{1,-1}, 100) == null,
                "contradictory unit clauses");
        check(M3TwoSat.solve(1, new int[]{-1}, new int[]{-1}, 100)[0] == false,
                "negative unit clause");
        check(M3TwoSat.solve(1, new int[]{1}, new int[]{1}, 100)[0], "positive unit clause");
        Random random = new Random(0x831A2026L);
        for (int t = 0; t < 25000; t++) {
            int variables = 1 + random.nextInt(7);
            int clauses = random.nextInt(17);
            int[] a = new int[clauses], b = new int[clauses];
            for (int i = 0; i < clauses; i++) {
                a[i] = (1 + random.nextInt(variables)) * (random.nextBoolean() ? 1 : -1);
                b[i] = (1 + random.nextInt(variables)) * (random.nextBoolean() ? 1 : -1);
            }
            int[] beforeA = a.clone(), beforeB = b.clone();
            boolean[] answer = M3TwoSat.solve(variables, a, b, 1_000_000);
            boolean feasible = bruteSat(variables, a, b);
            check((answer != null) == feasible, "2-CNF satisfiability oracle");
            if (answer != null) {
                check(answer.length == variables && witness(a, b, answer), "2-CNF witness");
                check(M3TwoSat.verify(variables, a, b, answer), "2-CNF verifier");
                check(Arrays.equals(answer, M3TwoSat.solve(variables, a, b, 1_000_000)),
                        "2-CNF deterministic assignment");
            }
            check(Arrays.equals(a, beforeA) && Arrays.equals(b, beforeB), "2-CNF input immutability");
        }
        boolean budgetRefused = false;
        try { M3TwoSat.solve(2, new int[]{1}, new int[]{2}, 0); }
        catch (IllegalStateException expected) { budgetRefused = true; }
        check(budgetRefused, "2-CNF exhaustion must fail closed");
        boolean invalidRefused = false;
        try { M3TwoSat.solve(1, new int[]{0}, new int[]{1}, 100); }
        catch (IllegalArgumentException expected) { invalidRefused = true; }
        check(invalidRefused, "zero literals refused");
        invalidRefused = false;
        try { M3TwoSat.solve(1, new int[]{Integer.MIN_VALUE}, new int[]{1}, 100); }
        catch (IllegalArgumentException expected) { invalidRefused = true; }
        check(invalidRefused, "min-int literal refused");
    }

    private static int[] bruteSupports(int vars, int values, int[] domains,
            int[] left, int[] right, int[] relations) {
        int[] result = new int[vars];
        int states = 1;
        for (int v = 0; v < vars; v++) { states *= values; }
        int[] assignment = new int[vars];
        for (int mask = 0; mask < states; mask++) {
            int cursor = mask;
            boolean valid = true;
            for (int v = 0; v < vars; v++) {
                assignment[v] = cursor % values;
                cursor /= values;
                if ((domains[v] & (1 << assignment[v])) == 0) { valid = false; break; }
            }
            if (!valid) { continue; }
            for (int edge = 0; edge < left.length; edge++) {
                int a = assignment[left[edge]];
                int b = assignment[right[edge]];
                if ((relations[edge * values + a] & (1 << b)) == 0) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                for (int v = 0; v < vars; v++) { result[v] |= 1 << assignment[v]; }
            }
        }
        return result;
    }

    private static void arcConsistency() {
        check(M3ArcConsistency.filter(0, 1, new int[0], new int[0], new int[0],
                new int[0], 0).length == 0, "zero-variable CSP");
        check(M3ArcConsistency.filter(1, 2, new int[]{3}, new int[]{0}, new int[]{0},
                new int[]{0,2}, 100)[0] == 2, "self-edge diagonal only");
        check(M3ArcConsistency.filter(1, 2, new int[]{1}, new int[]{0}, new int[]{0},
                new int[]{0,2}, 100) == null, "self-edge contradiction");
        Random random = new Random(0xAC312026L);
        for (int t = 0; t < 14000; t++) {
            int vars = random.nextInt(6);
            int values = 1 + random.nextInt(4);
            int edges = vars == 0 ? 0 : random.nextInt(9);
            int universe = (1 << values) - 1;
            int[] domains = new int[vars];
            for (int v = 0; v < vars; v++) {
                domains[v] = 1 + random.nextInt(universe);
            }
            int[] left = new int[edges], right = new int[edges];
            int[] relations = new int[edges * values];
            for (int edge = 0; edge < edges; edge++) {
                left[edge] = random.nextInt(vars);
                right[edge] = random.nextInt(vars);
                for (int v = 0; v < values; v++) {
                    relations[edge * values + v] = random.nextInt(universe + 1);
                }
            }
            int[] before = domains.clone(), originalRelations = relations.clone();
            int[] exact = bruteSupports(vars, values, domains, left, right, relations);
            int[] pruned = M3ArcConsistency.filter(vars, values, domains, left, right,
                    relations, 1_000_000);
            if (pruned == null) {
                check(Arrays.equals(exact, new int[vars]), "CSP inconsistency is sound");
            } else {
                check(pruned.length == vars, "CSP output dimensions");
                for (int v = 0; v < vars; v++) {
                    check((exact[v] & ~pruned[v]) == 0, "CSP never discards supported values");
                    check((pruned[v] & ~domains[v]) == 0 && pruned[v] != 0,
                            "CSP monotone and nonempty");
                }
                int[] replay = M3ArcConsistency.filter(vars, values, pruned, left,
                        right, relations, 1_000_000);
                check(Arrays.equals(pruned, replay), "CSP fixed point");
            }
            check(Arrays.equals(domains, before) && Arrays.equals(relations, originalRelations),
                    "CSP input untouched");
        }
        boolean refused = false;
        try { M3ArcConsistency.filter(2, 2, new int[]{3,3}, new int[]{0},
                new int[]{1}, new int[]{3,3}, 0); }
        catch (IllegalStateException expected) { refused = true; }
        check(refused, "CSP budget refused");
        refused = false;
        try { M3ArcConsistency.filter(1, 2, new int[]{4}, new int[0],
                new int[0], new int[0], 100); }
        catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "CSP invalid domain refused");
    }

    public static void main(String[] args) {
        twoSat();
        arcConsistency();
        System.out.println("M3_WAVE8_CONSTRAINTS_PASS checks=" + checks);
    }
}
