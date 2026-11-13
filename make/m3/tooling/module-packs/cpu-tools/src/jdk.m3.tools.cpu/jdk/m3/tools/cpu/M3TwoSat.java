// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** Bounded exact 2-CNF satisfiability with primitive CSR implication graphs. */
public final class M3TwoSat {
    private static final int MAX_VARIABLES = 8192;
    private static final int MAX_CLAUSES = 131072;
    private static final long MAX_STEPS = 50_000_000L;

    private M3TwoSat() { }

    /**
     * Solve conjunction of (left[i] OR right[i]) with signed 1-based literals.
     * Returns a detached satisfying assignment, or null for proven UNSAT.
     * An exhausted traversal budget is an exception, never a negative verdict.
     */
    public static boolean[] solve(int variables, int[] left, int[] right, long maxSteps) {
        validate(variables, left, right, maxSteps);
        int vertices = variables * 2;
        int edges = left.length * 2;
        int[] outgoing = new int[vertices + 1];
        int[] incoming = new int[vertices + 1];
        int[] source = new int[edges];
        int[] target = new int[edges];
        for (int clause = 0; clause < left.length; clause++) {
            int a = literalVertex(left[clause]);
            int b = literalVertex(right[clause]);
            int e = clause * 2;
            source[e] = a ^ 1;
            target[e] = b;
            source[e + 1] = b ^ 1;
            target[e + 1] = a;
            outgoing[source[e] + 1]++;
            outgoing[source[e + 1] + 1]++;
            incoming[target[e] + 1]++;
            incoming[target[e + 1] + 1]++;
        }
        for (int v = 0; v < vertices; v++) {
            outgoing[v + 1] += outgoing[v];
            incoming[v + 1] += incoming[v];
        }
        int[] outCursor = Arrays.copyOf(outgoing, vertices);
        int[] inCursor = Arrays.copyOf(incoming, vertices);
        int[] outEdges = new int[edges];
        int[] inEdges = new int[edges];
        for (int edge = 0; edge < edges; edge++) {
            outEdges[outCursor[source[edge]]++] = target[edge];
            inEdges[inCursor[target[edge]]++] = source[edge];
        }
        long[] remaining = {maxSteps};
        boolean[] seen = new boolean[vertices];
        int[] cursor = Arrays.copyOf(outgoing, vertices);
        int[] stack = new int[vertices];
        int[] finish = new int[vertices];
        int used = 0;
        // Explicit DFS stack: no JVM call-stack dependence or per-edge objects.
        for (int root = 0; root < vertices; root++) {
            if (seen[root]) { continue; }
            charge(remaining);
            seen[root] = true;
            int top = 0;
            stack[top] = root;
            while (top >= 0) {
                int node = stack[top];
                if (cursor[node] < outgoing[node + 1]) {
                    charge(remaining);
                    int next = outEdges[cursor[node]++];
                    if (!seen[next]) {
                        charge(remaining);
                        seen[next] = true;
                        stack[++top] = next;
                    }
                } else {
                    finish[used++] = node;
                    top--;
                }
            }
        }
        int[] component = new int[vertices];
        Arrays.fill(component, -1);
        int componentCount = 0;
        for (int i = used - 1; i >= 0; i--) {
            int root = finish[i];
            if (component[root] >= 0) { continue; }
            charge(remaining);
            component[root] = componentCount;
            int count = 1;
            stack[0] = root;
            while (count != 0) {
                int node = stack[--count];
                for (int edge = incoming[node]; edge < incoming[node + 1]; edge++) {
                    charge(remaining);
                    int predecessor = inEdges[edge];
                    if (component[predecessor] < 0) {
                        charge(remaining);
                        component[predecessor] = componentCount;
                        stack[count++] = predecessor;
                    }
                }
            }
            componentCount++;
        }
        boolean[] assignment = new boolean[variables];
        for (int variable = 0; variable < variables; variable++) {
            int positive = variable * 2;
            int negative = positive + 1;
            if (component[positive] == component[negative]) { return null; }
            assignment[variable] = component[positive] > component[negative];
        }
        return assignment;
    }

    /** Independently check a proposed witness; invalid clauses still fail closed. */
    public static boolean verify(int variables, int[] left, int[] right, boolean[] assignment) {
        validate(variables, left, right, MAX_STEPS);
        if (assignment == null || assignment.length != variables) { return false; }
        for (int i = 0; i < left.length; i++) {
            if (!value(left[i], assignment) && !value(right[i], assignment)) { return false; }
        }
        return true;
    }

    private static boolean value(int literal, boolean[] assignment) {
        boolean positive = assignment[Math.abs(literal) - 1];
        return literal > 0 ? positive : !positive;
    }

    private static int literalVertex(int literal) {
        return (Math.abs(literal) - 1) * 2 + (literal < 0 ? 1 : 0);
    }

    private static void charge(long[] remaining) {
        if (remaining[0] == 0L) {
            throw new IllegalStateException("2-CNF traversal budget exhausted");
        }
        remaining[0]--;
    }

    private static void validate(int variables, int[] left, int[] right, long maxSteps) {
        if (variables < 0 || variables > MAX_VARIABLES || left == null || right == null
                || left.length > MAX_CLAUSES || right.length != left.length
                || maxSteps < 0 || maxSteps > MAX_STEPS) {
            throw new IllegalArgumentException("invalid bounded 2-CNF input or budget");
        }
        for (int i = 0; i < left.length; i++) {
            if (left[i] == 0 || right[i] == 0 || left[i] < -variables
                    || left[i] > variables || right[i] < -variables || right[i] > variables) {
                throw new IllegalArgumentException("2-CNF literal outside variable range");
            }
        }
    }
}
