// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded deterministic Boolean CNF solver over packed primitive clause offsets.
 * Literals are signed 1-based variable indexes, clauses occupy half-open ranges
 * of {@code literals}, and no per-literal/per-clause objects are retained.
 * This is Boolean SAT only: not an SMT theory solver, proof generator or ILP engine.
 */
public final class M3CnfSat {
    public static final int MAX_VARIABLES = 64;
    private final int variables;
    private final int[] clauseOffsets;
    private final int[] literals;

    private M3CnfSat(int variables, int[] clauseOffsets, int[] literals) {
        this.variables = variables;
        this.clauseOffsets = clauseOffsets;
        this.literals = literals;
    }

    /** Snapshot a CNF, allowing zero clauses and empty clauses (always unsatisfiable). */
    public static M3CnfSat compile(int variables, int[] offsets, int[] literals) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(literals, "literals");
        if (variables < 0 || variables > MAX_VARIABLES || offsets.length == 0
                || offsets[0] != 0 || offsets[offsets.length - 1] != literals.length) {
            throw new IllegalArgumentException("invalid CNF geometry");
        }
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1] || offsets[i] > literals.length) {
                throw new IllegalArgumentException("nonmonotonic clause offsets");
            }
        }
        for (int literal : literals) {
            if (literal == 0 || literal == Integer.MIN_VALUE || Math.abs(literal) > variables) {
                throw new IllegalArgumentException("invalid literal index");
            }
        }
        return new M3CnfSat(variables, offsets.clone(), literals.clone());
    }

    /** Assignment values -1=false, +1=true; result arrays cannot mutate the compiled plan. */
    public record Result(boolean satisfiable, byte[] assignment, long exploredNodes) {
        public Result {
            assignment = Objects.requireNonNull(assignment, "assignment").clone();
            if (exploredNodes < 1) throw new IllegalArgumentException("exploration count");
        }
        @Override public byte[] assignment() { return assignment.clone(); }
    }

    /**
     * A complete result within the node budget. Exhaustion throws instead of
     * returning an unsound UNSAT verdict. No time or total-heap bound is claimed.
     */
    public Result solve(long maxNodes) {
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        byte[] assignment = new byte[variables + 1];
        long[] budget = {maxNodes, 0};
        boolean satisfiable = search(assignment, budget);
        return new Result(satisfiable, satisfiable ? assignment : new byte[0], budget[1]);
    }

    /** Exact independent witness check; a satisfiable verdict can never omit a clause. */
    public boolean verifies(byte[] assignment) {
        Objects.requireNonNull(assignment, "assignment");
        if (assignment.length != variables + 1 || assignment[0] != 0) return false;
        for (int i = 1; i <= variables; i++) if (assignment[i] != -1 && assignment[i] != 1) return false;
        for (int c = 0; c + 1 < clauseOffsets.length; c++) {
            boolean covered = false;
            for (int j = clauseOffsets[c]; j < clauseOffsets[c + 1]; j++) {
                int literal = literals[j];
                if (literal > 0 ? assignment[literal] == 1 : assignment[-literal] == -1) {
                    covered = true;
                    break;
                }
            }
            if (!covered) return false;
        }
        return true;
    }

    private boolean search(byte[] assignment, long[] budget) {
        if (budget[1] == budget[0]) throw new IllegalStateException("SAT node budget exhausted");
        budget[1]++;
        // Unit propagation to a fixed point. Values are primitive signed bytes.
        while (true) {
            boolean changed = false;
            boolean unresolved = false;
            for (int c = 0; c + 1 < clauseOffsets.length; c++) {
                int missing = 0, last = 0;
                boolean satisfied = false;
                for (int j = clauseOffsets[c]; j < clauseOffsets[c + 1]; j++) {
                    int literal = literals[j];
                    byte value = assignment[Math.abs(literal)];
                    if (value == 0) { missing++; last = literal; }
                    else if (literal > 0 ? value > 0 : value < 0) { satisfied = true; break; }
                }
                if (satisfied) continue;
                if (missing == 0) return false;
                unresolved = true;
                if (missing == 1) {
                    int variable = Math.abs(last);
                    byte value = (byte) (last > 0 ? 1 : -1);
                    if (assignment[variable] != 0 && assignment[variable] != value) return false;
                    if (assignment[variable] == 0) { assignment[variable] = value; changed = true; }
                }
            }
            if (!changed) {
                if (!unresolved) {
                    for (int i = 1; i <= variables; i++) if (assignment[i] == 0) assignment[i] = -1;
                    return true;
                }
                break;
            }
        }
        // Choose the smallest unresolved physical variable. Branch ordering is deterministic.
        int pivot = 0;
        for (int i = 1; i <= variables; i++) if (assignment[i] == 0) { pivot = i; break; }
        if (pivot == 0) return false;
        byte[] positive = assignment.clone();
        positive[pivot] = 1;
        if (search(positive, budget)) {
            System.arraycopy(positive, 0, assignment, 0, assignment.length);
            return true;
        }
        byte[] negative = assignment.clone();
        negative[pivot] = -1;
        if (search(negative, budget)) {
            System.arraycopy(negative, 0, assignment, 0, assignment.length);
            return true;
        }
        return false;
    }
}
