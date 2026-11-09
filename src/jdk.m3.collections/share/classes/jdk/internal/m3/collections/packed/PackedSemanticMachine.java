/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Allocation-free-at-transition semantic state machine over primitive bit/scalar storage.
 *
 * <p>Configuration compiles meaning into implication, exclusion and requirement matrices
 * plus primitive compound rules. Runtime transition work is AND/OR/XOR/AND-NOT, bit scans,
 * byte comparisons and array access. This class is single-threaded because it reuses primitive
 * scratch arrays; use one machine instance per concurrent worker.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedSemanticMachine.java}
 * blob {@code 47a793e91df5e9736f43c1f747e24fe04a91c9b8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedSemanticMachine {
    private final int factCount;
    private final int scalarCount;
    private final int wordCount;

    private final PackedRelationMatrix implies;
    private final PackedRelationMatrix excludes;
    private final PackedRelationMatrix requires;
    private final PackedRelationMatrix guards;
    private final PackedRuleProgram rules;
    private final long[] externallyMutable;

    private final long[] before;
    private final long[] setScratch;
    private final long[] clearScratch;
    private final long[] requestSetScratch;
    private final long[] requestClearScratch;
    private final long[] frontier;
    private final long[] nextFrontier;
    private final long[] lastChanged;
    private final long[] lastViolations;

    private boolean compiled;

    public PackedSemanticMachine(int factCount, int scalarCount) {
        this(factCount, scalarCount, 8);
    }

    public PackedSemanticMachine(int factCount, int scalarCount, int expectedRules) {
        if (factCount <= 0) {
            throw new IllegalArgumentException("factCount");
        }
        if (scalarCount < 0) {
            throw new IllegalArgumentException("scalarCount");
        }

        this.factCount = factCount;
        this.scalarCount = scalarCount;
        this.wordCount = PackedSemanticMasks.wordCount(factCount);

        this.implies = new PackedRelationMatrix(factCount);
        this.excludes = new PackedRelationMatrix(factCount);
        this.requires = new PackedRelationMatrix(factCount);
        this.guards = new PackedRelationMatrix(factCount);
        this.rules = new PackedRuleProgram(factCount, scalarCount, expectedRules);

        this.externallyMutable = new long[wordCount];
        Arrays.fill(externallyMutable, -1L);
        PackedSemanticMasks.clearUnusedHighBits(externallyMutable, factCount);

        this.before = new long[wordCount];
        this.setScratch = new long[wordCount];
        this.clearScratch = new long[wordCount];
        this.requestSetScratch = new long[wordCount];
        this.requestClearScratch = new long[wordCount];
        this.frontier = new long[wordCount];
        this.nextFrontier = new long[wordCount];
        this.lastChanged = new long[wordCount];
        this.lastViolations = new long[wordCount];
    }

    public int factCount() {
        return factCount;
    }

    public int scalarCount() {
        return scalarCount;
    }

    public int wordCount() {
        return wordCount;
    }

    public int ruleCount() {
        return rules.ruleCount();
    }

    /**
     * Creates an empty state with dimensions exactly matching this machine.
     *
     * @return new compatible mutable state
     */
    public PackedSemanticState newState() {
        return new PackedSemanticState(factCount, scalarCount);
    }

    public boolean compiled() {
        return compiled;
    }

    public long relationPayloadBytes() {
        return implies.payloadBytes()
                + excludes.payloadBytes()
                + requires.payloadBytes()
                + guards.payloadBytes();
    }

    public long rulePayloadBytes() {
        return rules.payloadBytes();
    }

    /**
     * Returns approximate primitive payload bytes retained by this machine.
     *
     * <p>The value includes relation matrices, rule lanes, external-mutability mask and
     * transition scratch/diagnostic word arrays. Java object/array headers are excluded.
     *
     * @return primitive machine payload bytes
     */
    public long machinePayloadBytes() {
        long wordArrays = 10L * wordCount * Long.BYTES;
        return relationPayloadBytes() + rulePayloadBytes() + wordArrays;
    }

    public void imply(int from, int to) {
        checkConfigurable();
        implies.relate(from, to);
    }

    public void exclude(int from, int to) {
        checkConfigurable();
        excludes.relate(from, to);
    }

    public void excludeMutual(int left, int right) {
        checkConfigurable();
        excludes.relateMutual(left, right);
    }

    public void require(int fact, int requiredFact) {
        checkConfigurable();
        requires.relate(fact, requiredFact);
    }

    /**
     * Adds a transition precondition: activating {@code fact} requires
     * {@code prerequisite} to be active at transition time. Unlike require(), the
     * prerequisite need not remain active forever after the transition.
     */
    public void guard(int fact, int prerequisite) {
        checkConfigurable();
        guards.relate(fact, prerequisite);
    }

    /**
     * Tests whether callers may directly activate/deactivate a fact.
     *
     * @param fact fact index
     * @return true when external mutation is permitted
     * @throws IndexOutOfBoundsException if the fact index is invalid
     */
    public boolean externallyMutable(int fact) {
        checkFact(fact);
        return PackedSemanticMasks.get(externallyMutable, fact);
    }

    public void externallyMutable(int fact, boolean mutable) {
        checkConfigurable();
        checkFact(fact);
        if (mutable) {
            PackedSemanticMasks.set(externallyMutable, fact);
        } else {
            PackedSemanticMasks.clear(externallyMutable, fact);
        }
    }

    public int addRule(
            long[] requireAllOn,
            long[] requireAnyOn,
            long[] requireAllOff,
            long[] set,
            long[] clear) {
        checkConfigurable();
        return rules.addRule(requireAllOn, requireAnyOn, requireAllOff, set, clear);
    }

    public int addScalarRule(
            long[] requireAllOn,
            long[] requireAnyOn,
            long[] requireAllOff,
            int scalar,
            int minInclusive,
            int maxInclusive,
            long[] set,
            long[] clear) {
        checkConfigurable();
        return rules.addScalarRule(
                requireAllOn,
                requireAnyOn,
                requireAllOff,
                scalar,
                minInclusive,
                maxInclusive,
                set,
                clear);
    }

    /**
     * Freezes configuration and precomputes transitive implication/requirement closure.
     */
    public void compile() {
        if (compiled) {
            return;
        }
        implies.transitiveClosureInPlace();
        requires.transitiveClosureInPlace();
        rules.trimToSize();
        compiled = true;
    }

    public int activate(PackedSemanticState state, int fact) {
        requireState(state);
        checkFact(fact);
        clearRequests();
        PackedSemanticMasks.set(requestSetScratch, fact);
        return transition(state, requestSetScratch, requestClearScratch);
    }

    public int deactivate(PackedSemanticState state, int fact) {
        requireState(state);
        checkFact(fact);
        clearRequests();
        PackedSemanticMasks.set(requestClearScratch, fact);
        return transition(state, requestSetScratch, requestClearScratch);
    }

    /** Re-runs semantic propagation after only scalar values changed. */
    public int recompute(PackedSemanticState state) {
        requireState(state);
        clearRequests();
        return transition(state, requestSetScratch, requestClearScratch);
    }

    /**
     * Atomically changes one unsigned-byte scalar and recomputes all derived bit semantics.
     * The scalar is restored if semantic propagation cannot converge or violates invariants.
     */
    public int scalar(PackedSemanticState state, int scalar, int value) {
        requireState(state);
        if (scalar < 0 || scalar >= scalarCount) {
            throw new IndexOutOfBoundsException("scalar=" + scalar);
        }
        PackedU8.require(value);
        int beforeScalar = state.scalarUnsigned(scalar);
        state.scalarUnsigned(scalar, value);
        try {
            return recompute(state);
        } catch (RuntimeException failure) {
            state.scalarUnsigned(scalar, beforeScalar);
            throw failure;
        }
    }

    /**
     * Applies requested external changes, propagates all relationships/rules to a fixed point,
     * validates invariants, and commits atomically. On failure the fact state is restored.
     *
     * @return number of facts whose final active state differs from the pre-transition state
     */
    public int transition(
            PackedSemanticState state,
            long[] requestedSet,
            long[] requestedClear) {
        requireCompiled();
        requireState(state);
        requireWords(requestedSet);
        requireWords(requestedClear);
        ensureNoRequestConflict(requestedSet, requestedClear);
        ensureExternallyLegal(requestedSet, requestedClear);

        long[] active = state.activeWords();
        Arrays.fill(lastChanged, 0L);
        Arrays.fill(lastViolations, 0L);
        ensureGuards(active, requestedSet, requestedClear);

        System.arraycopy(active, 0, before, 0, wordCount);

        for (int word = 0; word < wordCount; word++) {
            active[word] = (active[word] & ~requestedClear[word]) | requestedSet[word];
            frontier[word] = active[word] & ~before[word];
        }
        PackedSemanticMasks.clearUnusedHighBits(active, factCount);
        PackedSemanticMasks.clearUnusedHighBits(frontier, factCount);

        int maxPasses = Math.max(8, factCount * 2 + rules.ruleCount() * 2 + 1);
        boolean converged = false;

        for (int pass = 0; pass < maxPasses; pass++) {
            Arrays.fill(setScratch, 0L);
            Arrays.fill(clearScratch, 0L);

            // Event relations fire only from facts activated since the previous pass.
            for (int word = 0; word < wordCount; word++) {
                long remaining = frontier[word];
                while (remaining != 0L) {
                    int bit = Long.numberOfTrailingZeros(remaining);
                    int fact = (word << 6) + bit;
                    if (fact < factCount) {
                        implies.orRowInto(fact, setScratch);
                        excludes.orRowInto(fact, clearScratch);
                    }
                    remaining &= remaining - 1L;
                }
            }

            // Compound rules are state laws and are evaluated against the full current state.
            rules.evaluate(active, state.scalarBytes(), setScratch, clearScratch);

            boolean changed = false;
            Arrays.fill(nextFrontier, 0L);
            for (int word = 0; word < wordCount; word++) {
                // Safety rule: clear wins when simultaneous laws disagree.
                setScratch[word] &= ~clearScratch[word];
                long previous = active[word];
                long next = (previous | setScratch[word]) & ~clearScratch[word];
                nextFrontier[word] = next & ~previous;
                if (next != previous) {
                    active[word] = next;
                    changed = true;
                }
            }
            PackedSemanticMasks.clearUnusedHighBits(active, factCount);
            PackedSemanticMasks.clearUnusedHighBits(nextFrontier, factCount);

            if (!changed) {
                converged = true;
                break;
            }
            System.arraycopy(nextFrontier, 0, frontier, 0, wordCount);
        }

        if (!converged) {
            System.arraycopy(before, 0, active, 0, wordCount);
            throw new IllegalStateException("semantic propagation did not converge");
        }

        computeViolations(active, lastViolations);
        if (PackedSemanticMasks.any(lastViolations)) {
            System.arraycopy(before, 0, active, 0, wordCount);
            throw new IllegalStateException("semantic invariant violation");
        }

        int changedCount = 0;
        for (int word = 0; word < wordCount; word++) {
            lastChanged[word] = before[word] ^ active[word];
            changedCount += Long.bitCount(lastChanged[word]);
        }
        return changedCount;
    }

    public boolean active(PackedSemanticState state, int fact) {
        requireState(state);
        checkFact(fact);
        return state.active(fact);
    }

    public void copyLastChangedInto(long[] target) {
        requireWords(target);
        System.arraycopy(lastChanged, 0, target, 0, wordCount);
    }

    /**
     * Returns a boundary copy of the last changed-fact mask.
     *
     * @return copied changed-fact words
     */
    public long[] lastChangedCopy() {
        return Arrays.copyOf(lastChanged, wordCount);
    }

    public void copyLastViolationsInto(long[] target) {
        requireWords(target);
        System.arraycopy(lastViolations, 0, target, 0, wordCount);
    }

    /**
     * Returns a boundary copy of the last recorded violation mask.
     *
     * @return copied violation words
     */
    public long[] lastViolationsCopy() {
        return Arrays.copyOf(lastViolations, wordCount);
    }

    public int violationCount(PackedSemanticState state) {
        requireState(state);
        Arrays.fill(lastViolations, 0L);
        computeViolations(state.activeWords(), lastViolations);
        return PackedSemanticMasks.cardinality(lastViolations);
    }

    /**
     * Validates a state without mutating it.
     *
     * @param state compatible state
     * @return number of semantic invariant violations
     */
    public int validate(PackedSemanticState state) {
        return violationCount(state);
    }

    public long[] newMask(int... facts) {
        return PackedSemanticMasks.mask(factCount, facts);
    }

    private void computeViolations(long[] active, long[] violationsOut) {
        Arrays.fill(violationsOut, 0L);
        for (int activeWord = 0; activeWord < wordCount; activeWord++) {
            long remaining = active[activeWord];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int fact = (activeWord << 6) + bit;
                if (fact < factCount) {
                    for (int word = 0; word < wordCount; word++) {
                        violationsOut[word] |= requires.wordAt(fact, word) & ~active[word];
                        violationsOut[word] |= excludes.wordAt(fact, word) & active[word];
                    }
                }
                remaining &= remaining - 1L;
            }
        }
        PackedSemanticMasks.clearUnusedHighBits(violationsOut, factCount);
    }

    private void ensureGuards(long[] active, long[] requestedSet, long[] requestedClear) {
        for (int requestWord = 0; requestWord < wordCount; requestWord++) {
            long remaining = requestedSet[requestWord] & ~active[requestWord];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int fact = (requestWord << 6) + bit;
                if (fact < factCount) {
                    for (int word = 0; word < wordCount; word++) {
                        long available =
                                (active[word] & ~requestedClear[word]) | requestedSet[word];
                        lastViolations[word] |= guards.wordAt(fact, word) & ~available;
                    }
                }
                remaining &= remaining - 1L;
            }
        }
        PackedSemanticMasks.clearUnusedHighBits(lastViolations, factCount);
        if (PackedSemanticMasks.any(lastViolations)) {
            throw new IllegalStateException("semantic transition guard violation");
        }
    }

    private void ensureNoRequestConflict(long[] requestedSet, long[] requestedClear) {
        for (int word = 0; word < wordCount; word++) {
            if ((requestedSet[word] & requestedClear[word]) != 0L) {
                throw new IllegalArgumentException(
                        "same semantic fact requested in both set and clear masks");
            }
        }
    }

    private void ensureExternallyLegal(long[] requestedSet, long[] requestedClear) {
        for (int word = 0; word < wordCount; word++) {
            long illegal = (requestedSet[word] | requestedClear[word]) & ~externallyMutable[word];
            if (illegal != 0L) {
                throw new IllegalArgumentException(
                        "attempt to mutate derived/immutable semantic fact");
            }
        }
    }

    private void clearRequests() {
        Arrays.fill(requestSetScratch, 0L);
        Arrays.fill(requestClearScratch, 0L);
    }

    private void requireCompiled() {
        if (!compiled) {
            throw new IllegalStateException("compile() semantic machine before execution");
        }
    }

    private void requireState(PackedSemanticState state) {
        if (state.factCount() != factCount || state.scalarCount() != scalarCount) {
            throw new IllegalArgumentException("semantic state shape mismatch");
        }
    }

    private void requireWords(long[] words) {
        if (words.length != wordCount) {
            throw new IllegalArgumentException(
                    "word-length mismatch: " + words.length + " != " + wordCount);
        }
    }

    private void checkConfigurable() {
        if (compiled) {
            throw new IllegalStateException("semantic machine is compiled/frozen");
        }
    }

    private void checkFact(int fact) {
        if (fact < 0 || fact >= factCount) {
            throw new IndexOutOfBoundsException("fact=" + fact + ", factCount=" + factCount);
        }
    }
}
