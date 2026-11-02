// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.util.Objects;

/**
 * Candidate-only machine-pattern hints over already manufactured javac fact columns.
 * No new AST traversal or source interpretation occurs in this class. The compiler
 * fact schema, source evidence owner, and subsequent lowering contract remain separate.
 * Three states are preserved: unknown, known-but-not-a-candidate, candidate-for-review.
 * Nothing here grants permission to transform Java, bypass source checks or invoke JNI.
 */
public final class M3MachineHintRules {
    public static final int FIELD_METHOD_RELATION = 0;
    public static final int INTERFACE_INVOCATION = 1;
    public static final int ITERATIVE_INVOCATION = 2;
    public static final int ALLOCATION_FACTORY_REVIEW = 3;
    public static final int CONDITIONAL_CONTROL = 4;
    public static final int SYNCHRONIZATION = 5;
    public static final int LOOP_BRANCH_REVIEW = 6;
    public static final int COUNT = 7;
    public static final int FACT_COUNT = 22;

    private static final String[] NAMES = {
        "FIELD_METHOD_RELATION", "INTERFACE_INVOCATION", "ITERATIVE_INVOCATION",
        "ALLOCATION_FACTORY_REVIEW", "CONDITIONAL_CONTROL", "SYNCHRONIZATION",
        "LOOP_BRANCH_REVIEW"
    };
    // Source contract: M3CompilerFacts indexes, not another AST or runtime oracle.
    private static final int FIELDS = 0;
    private static final int METHODS = 1;
    private static final int IMPLEMENTED_INTERFACES = 3;
    private static final int METHOD_INVOCATIONS = 7;
    private static final int IF_BRANCHES = 9;
    private static final int SWITCHES = 10;
    private static final int LOOPS = 11;
    private static final int CONDITIONAL_EXPRESSIONS = 12;
    private static final int NEW_EXPRESSIONS = 14;
    private static final int SYNCHRONIZED_BLOCKS = 15;
    private static final long FACT_BITS = (1L << FACT_COUNT) - 1L;
    private static final long HINT_BITS = (1L << COUNT) - 1L;

    public enum State { UNKNOWN, NOT_OBSERVED, CANDIDATE }

    private final long known;
    private final long candidates;

    /** Construct a validated primitive-only projection; no caller buffer is retained. */
    public M3MachineHintRules(long known, long candidates) {
        if (((known | candidates) & ~HINT_BITS) != 0L || (candidates & ~known) != 0L) {
            throw new IllegalArgumentException("invalid machine-hint geometry");
        }
        this.known = known;
        this.candidates = candidates;
    }

    /**
     * Derive review hints using exactly the existing 22 source-time fact coordinates.
     * Missing AST facts remain unknown, never converted into absent-pattern evidence.
     */
    public static M3MachineHintRules fromFacts(int[] counts, long[] validity) {
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(validity, "validity");
        if (counts.length != FACT_COUNT || validity.length != 1
                || (validity[0] & ~FACT_BITS) != 0L) {
            throw new IllegalArgumentException("compiler fact schema mismatch");
        }
        long factsKnown = validity[0];
        for (int i = 0; i < counts.length; i++) {
            if ((factsKnown & (1L << i)) != 0L && counts[i] < 0) {
                throw new IllegalArgumentException("negative known compiler fact");
            }
        }
        long known = 0L;
        long candidates = 0L;
        for (int hint = 0; hint < COUNT; hint++) {
            long required = requiredFacts(hint);
            if ((factsKnown & required) != required) continue;
            long bit = 1L << hint;
            known |= bit;
            boolean candidate = switch (hint) {
                case FIELD_METHOD_RELATION -> counts[FIELDS] > 0 && counts[METHODS] > 0;
                case INTERFACE_INVOCATION -> counts[IMPLEMENTED_INTERFACES] > 0
                        && counts[METHOD_INVOCATIONS] > 0;
                case ITERATIVE_INVOCATION -> counts[LOOPS] > 0 && counts[METHOD_INVOCATIONS] > 0;
                case ALLOCATION_FACTORY_REVIEW -> counts[NEW_EXPRESSIONS] > 0
                        && counts[METHODS] > 0;
                case CONDITIONAL_CONTROL -> branching(counts);
                case SYNCHRONIZATION -> counts[SYNCHRONIZED_BLOCKS] > 0;
                case LOOP_BRANCH_REVIEW -> counts[LOOPS] > 0 && branching(counts);
                default -> throw new AssertionError("unknown machine hint");
            };
            if (candidate) candidates |= bit;
        }
        return new M3MachineHintRules(known, candidates);
    }

    private static boolean branching(int[] values) {
        return values[IF_BRANCHES] > 0 || values[SWITCHES] > 0
                || values[CONDITIONAL_EXPRESSIONS] > 0;
    }

    private static long requiredFacts(int hint) {
        return switch (hint) {
            case FIELD_METHOD_RELATION -> bits(FIELDS, METHODS);
            case INTERFACE_INVOCATION -> bits(IMPLEMENTED_INTERFACES, METHOD_INVOCATIONS);
            case ITERATIVE_INVOCATION -> bits(LOOPS, METHOD_INVOCATIONS);
            case ALLOCATION_FACTORY_REVIEW -> bits(NEW_EXPRESSIONS, METHODS);
            case CONDITIONAL_CONTROL -> bits(IF_BRANCHES, SWITCHES, CONDITIONAL_EXPRESSIONS);
            case SYNCHRONIZATION -> bits(SYNCHRONIZED_BLOCKS);
            case LOOP_BRANCH_REVIEW -> bits(LOOPS, IF_BRANCHES, SWITCHES, CONDITIONAL_EXPRESSIONS);
            default -> throw new IndexOutOfBoundsException("invalid machine hint: " + hint);
        };
    }

    private static long bits(int... facts) {
        long mask = 0L;
        for (int fact : facts) mask |= 1L << fact;
        return mask;
    }

    public static String name(int hint) { return NAMES[Objects.checkIndex(hint, COUNT)]; }

    public State state(int hint) {
        long bit = 1L << Objects.checkIndex(hint, COUNT);
        if ((known & bit) == 0L) return State.UNKNOWN;
        return (candidates & bit) != 0L ? State.CANDIDATE : State.NOT_OBSERVED;
    }

    public long knownMask() { return known; }
    public long candidateMask() { return candidates; }
    public long[] masks() { return new long[]{known, candidates}; }

    /** Hints cannot certify a GoF design pattern or grant a Java/JIT/JNI transformation. */
    public boolean promotionAuthority() { return false; }
    public boolean sourceMutationAuthority() { return false; }
    public boolean nativeLoweringAuthority() { return false; }
}
