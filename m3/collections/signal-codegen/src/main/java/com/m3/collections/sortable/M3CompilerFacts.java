// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.util.Objects;

/**
 * Immutable source/compile-time facts, held as primitive count and validity columns.
 *
 * <p>These are syntactic/compiler-known facts, never inferred object equality,
 * purity, execution cost, or canonical domain ordering. A missing binding or a
 * runtime-only fact is <em>unknown</em>, not zero. No per-member Entry object,
 * runtime reflection or JNI object pointer is retained.</p>
 */
public final class M3CompilerFacts {
    public static final int FIELDS = 0;
    public static final int METHODS = 1;
    public static final int CONSTRUCTORS = 2;
    public static final int IMPLEMENTED_INTERFACES = 3;
    public static final int DECLARED_TYPE_TOKENS = 4;
    public static final int METHOD_PARAMETERS = 5;
    public static final int DECLARED_THROWS = 6;
    public static final int METHOD_INVOCATIONS = 7;
    public static final int DISTINCT_RESOLVED_CALLEE_TOKENS = 8;
    public static final int IF_BRANCHES = 9;
    public static final int SWITCHES = 10;
    public static final int LOOPS = 11;
    public static final int CONDITIONAL_EXPRESSIONS = 12;
    public static final int LAMBDAS = 13;
    public static final int NEW_EXPRESSIONS = 14;
    public static final int SYNCHRONIZED_BLOCKS = 15;
    public static final int THROW_STATEMENTS = 16;
    public static final int TRY_BLOCKS = 17;
    public static final int CATCH_BLOCKS = 18;
    public static final int RETURN_STATEMENTS = 19;
    public static final int INCOMING_CALLS = 20;
    public static final int RUNTIME_INVOCATIONS = 21;
    public static final int COUNT = 22;
    public static final int MODEL_FACTS = 7;
    public static final int AST_FACTS_END = 20;

    public enum Family { STRUCTURAL, TYPE, RELATION, CONTROL_FLOW, BEHAVIORAL, RUNTIME }
    public enum Origin { COMPILER_MODEL, COMPILER_AST, UNKNOWN }

    private static final String[] NAMES = {
        "fields", "methods", "constructors", "implementedInterfaces", "declaredTypeTokens",
        "methodParameters", "declaredThrows", "methodInvocations", "resolvedCalleeTokens",
        "ifBranches", "switches", "loops", "conditionalExpressions", "lambdas",
        "newExpressions", "synchronizedBlocks", "throwStatements", "tryBlocks",
        "catchBlocks", "returnStatements", "incomingCalls", "runtimeInvocations"
    };

    private final int[] counts;
    private final long[] valid;

    public M3CompilerFacts(int[] counts, long[] valid) {
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(valid, "valid");
        if (counts.length != COUNT || valid.length != (COUNT + 63) / 64) {
            throw new IllegalArgumentException("compiler fact/validity lane geometry");
        }
        long admissible = -1L >>> (64 - COUNT);
        if ((valid[0] & ~admissible) != 0L) {
            throw new IllegalArgumentException("validity includes unnamed facts");
        }
        for (int i = 0; i < COUNT; i++) {
            if ((valid[0] & (1L << i)) != 0 && counts[i] < 0) {
                throw new IllegalArgumentException("negative known compiler fact: " + NAMES[i]);
            }
        }
        this.counts = counts.clone();
        this.valid = valid.clone();
    }

    public static String name(int fact) { return NAMES[Objects.checkIndex(fact, COUNT)]; }

    public static Family family(int fact) {
        return switch (Objects.checkIndex(fact, COUNT)) {
            case FIELDS, METHODS, CONSTRUCTORS -> Family.STRUCTURAL;
            case IMPLEMENTED_INTERFACES, DECLARED_TYPE_TOKENS,
                    METHOD_PARAMETERS, DECLARED_THROWS -> Family.TYPE;
            case METHOD_INVOCATIONS, DISTINCT_RESOLVED_CALLEE_TOKENS,
                    INCOMING_CALLS -> Family.RELATION;
            case IF_BRANCHES, SWITCHES, LOOPS, CONDITIONAL_EXPRESSIONS,
                    TRY_BLOCKS, CATCH_BLOCKS, RETURN_STATEMENTS -> Family.CONTROL_FLOW;
            case LAMBDAS, NEW_EXPRESSIONS, SYNCHRONIZED_BLOCKS,
                    THROW_STATEMENTS -> Family.BEHAVIORAL;
            case RUNTIME_INVOCATIONS -> Family.RUNTIME;
            default -> throw new AssertionError("unmapped fact");
        };
    }

    public boolean known(int fact) {
        int checked = Objects.checkIndex(fact, COUNT);
        return (valid[checked >>> 6] & (1L << (checked & 63))) != 0;
    }

    public Origin origin(int fact) {
        int checked = Objects.checkIndex(fact, COUNT);
        if (!known(checked)) return Origin.UNKNOWN;
        return checked < MODEL_FACTS ? Origin.COMPILER_MODEL : Origin.COMPILER_AST;
    }

    public int count(int fact) {
        int checked = Objects.checkIndex(fact, COUNT);
        if (!known(checked)) throw new IllegalStateException("unknown fact: " + NAMES[checked]);
        return counts[checked];
    }

    public int[] counts() { return counts.clone(); }
    public long[] validityMask() { return valid.clone(); }

    /**
     * Domain-defined heuristic projection. Unknown weighted coordinates return
     * NaN (not a fabricated zero). A scalar never certifies exact object order.
     */
    public double project(double[] weights) {
        Objects.requireNonNull(weights, "weights");
        if (weights.length != COUNT) throw new IllegalArgumentException("weight lane geometry");
        double value = 0.0;
        boolean incomplete = false;
        for (int i = 0; i < COUNT; i++) {
            double weight = weights[i];
            if (!Double.isFinite(weight)) throw new IllegalArgumentException("nonfinite weight");
            if (weight == 0.0) continue;
            if (!known(i)) incomplete = true;
            else value = Math.fma(weight, counts[i], value);
        }
        return !incomplete && Double.isFinite(value) ? value : Double.NaN;
    }

    /**
     * Compare only explicitly selected exact integer coordinates. The caller
     * defines the policy; a zero result does not assert object equality.
     */
    public int compareBy(M3CompilerFacts other, int[] priorities, boolean[] descending) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(priorities, "priorities");
        Objects.requireNonNull(descending, "descending");
        if (priorities.length == 0 || priorities.length != descending.length) {
            throw new IllegalArgumentException("priority/direction geometry");
        }
        long seen = 0L;
        for (int fact : priorities) {
            Objects.checkIndex(fact, COUNT);
            long bit = 1L << fact;
            if ((seen & bit) != 0L) throw new IllegalArgumentException("repeated fact priority");
            seen |= bit;
            if (!known(fact) || !other.known(fact)) {
                throw new IllegalStateException("ordering needs known coordinate: " + NAMES[fact]);
            }
        }
        for (int i = 0; i < priorities.length; i++) {
            int fact = priorities[i];
            int sign = Integer.compare(counts[fact], other.counts[fact]);
            if (sign != 0) return descending[i] ? -sign : sign;
        }
        return 0;
    }
}
