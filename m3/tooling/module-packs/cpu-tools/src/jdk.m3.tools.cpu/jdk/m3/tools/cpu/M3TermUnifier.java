// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** First-order, occurs-checked unification over an acyclic primitive-array term DAG. */
public final class M3TermUnifier {
    public static final int VARIABLE = 0;
    public static final int ATOM = 1;
    public static final int APPLICATION = 2;
    private static final int MAX_NODES = 4096;
    private static final int MAX_EDGES = 65536;
    private static final int MAX_WORK = 2_000_000;

    private M3TermUnifier() { }

    /**
     * Unify two roots of the same immutable input term DAG.
     * <p>Nodes [0, variableCount) are unique variables with symbol == index.
     * Every other node is an atom of arity zero or a nonempty application.
     * CSR children must have indices strictly below the referring parent node;
     * this permits shared subterms but not source cycles. A variable substitution
     * points to an existing input node, or -1 for an unbound variable. The
     * returned array is detached from input and has one entry per variable.
     * </p>
     *
     * @return canonical substitutions, or null for a contradictory equation
     * @throws IllegalArgumentException on malformed input or invalid budget
     * @throws IllegalStateException when maxWork is exhausted
     */
    public static int[] unify(int[] kind, int[] symbol, int[] offsets, int[] children,
            int variableCount, int leftRoot, int rightRoot, int maxWork) {
        validate(kind, symbol, offsets, children, variableCount, leftRoot, rightRoot, maxWork);
        int[] binding = new int[variableCount];
        Arrays.fill(binding, -1);
        int[] budget = {maxWork};
        int[] a = new int[8];
        int[] b = new int[8];
        int used = 1;
        a[0] = leftRoot;
        b[0] = rightRoot;
        int[] visitedAt = new int[kind.length];
        int epoch = 0;
        int[] scan = new int[children.length + 1];
        while (used != 0) {
            consume(budget);
            int left = resolve(a[--used], kind, binding, budget);
            int right = resolve(b[used], kind, binding, budget);
            if (left == right) { continue; }
            if (left < variableCount && right < variableCount) {
                if (left < right) { binding[right] = left; }
                else { binding[left] = right; }
                continue;
            }
            if (left < variableCount || right < variableCount) {
                int variable = left < variableCount ? left : right;
                int term = left < variableCount ? right : left;
                if (occurs(variable, term, kind, offsets, children, binding, budget,
                        visitedAt, ++epoch, scan)) {
                    return null;
                }
                binding[variable] = term;
                continue;
            }
            int lstart = offsets[left];
            int rstart = offsets[right];
            int size = offsets[left + 1] - lstart;
            if (kind[left] != kind[right] || symbol[left] != symbol[right]
                    || size != offsets[right + 1] - rstart) {
                return null;
            }
            if (size > maxWork - used || size > budget[0]) {
                throw new IllegalStateException("unification work budget exhausted");
            }
            int required = used + size;
            if (required > a.length) {
                int capacity = a.length;
                while (capacity < required) { capacity = Math.min(maxWork, capacity * 2); }
                a = Arrays.copyOf(a, capacity);
                b = Arrays.copyOf(b, capacity);
            }
            // Pop child zero first, keeping equation order deterministic.
            for (int i = size - 1; i >= 0; i--) {
                a[used] = children[lstart + i];
                b[used] = children[rstart + i];
                used++;
            }
        }
        int[] answer = new int[variableCount];
        for (int i = 0; i < variableCount; i++) {
            int value = resolve(i, kind, binding, budget);
            answer[i] = value == i ? -1 : value;
        }
        return answer;
    }

    private static int resolve(int node, int[] kind, int[] binding, int[] budget) {
        while (node < binding.length && kind[node] == VARIABLE && binding[node] != -1) {
            consume(budget);
            node = binding[node];
        }
        return node;
    }

    private static boolean occurs(int needle, int root, int[] kind, int[] offsets,
            int[] children, int[] binding, int[] budget, int[] visitedAt,
            int epoch, int[] scan) {
        int size = 1;
        scan[0] = root;
        while (size > 0) {
            consume(budget);
            int node = resolve(scan[--size], kind, binding, budget);
            if (node == needle) { return true; }
            if (visitedAt[node] == epoch) { continue; }
            visitedAt[node] = epoch;
            int start = offsets[node];
            int end = offsets[node + 1];
            for (int i = start; i < end; i++) { scan[size++] = children[i]; }
        }
        return false;
    }

    private static void consume(int[] remaining) {
        if (remaining[0] == 0) { throw new IllegalStateException("unification work budget exhausted"); }
        remaining[0]--;
    }

    private static void validate(int[] kind, int[] symbol, int[] offsets, int[] children,
            int variableCount, int left, int right, int maxWork) {
        if (kind == null || symbol == null || offsets == null || children == null
                || kind.length < 1 || kind.length > MAX_NODES || symbol.length != kind.length
                || offsets.length != kind.length + 1 || children.length > MAX_EDGES
                || variableCount < 0 || variableCount > kind.length
                || left < 0 || left >= kind.length || right < 0 || right >= kind.length
                || maxWork < 1 || maxWork > MAX_WORK) {
            throw new IllegalArgumentException("invalid term DAG or budget");
        }
        if (offsets[0] != 0 || offsets[kind.length] != children.length) {
            throw new IllegalArgumentException("invalid CSR endpoints");
        }
        for (int node = 0; node < kind.length; node++) {
            int start = offsets[node];
            int end = offsets[node + 1];
            if (start < 0 || end < start || end > children.length) {
                throw new IllegalArgumentException("invalid CSR order");
            }
            int arity = end - start;
            if (node < variableCount) {
                if (kind[node] != VARIABLE || symbol[node] != node || arity != 0) {
                    throw new IllegalArgumentException("variable nodes must form canonical prefix");
                }
            } else if (!((kind[node] == ATOM && arity == 0)
                    || (kind[node] == APPLICATION && arity > 0))) {
                throw new IllegalArgumentException("invalid atom/application arity");
            }
            for (int edge = start; edge < end; edge++) {
                if (children[edge] < 0 || children[edge] >= node) {
                    throw new IllegalArgumentException("term DAG must be topologically ordered");
                }
            }
        }
    }
}
