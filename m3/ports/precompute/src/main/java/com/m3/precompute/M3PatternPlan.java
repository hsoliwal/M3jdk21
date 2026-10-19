// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexPatternPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexPatternPlan.java}
 * blob {@code e03cfb0807bf51a612db55f205cabd65fc7fd370} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 06e484da28856abdb75b9bd42ad05c2cbee70ebaa1e16ac3cb515bf7bb559615}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3PatternPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Strategy, Analysis}</li>
 *   <li>{@code member compileRegex(IndexResolver, int, String) (parameter resolver)}</li>
 *   <li>{@code member compileRegex(IndexResolver, int, String, IProgressMonitor) (parameter resolver)}</li>
 *   <li>{@code member compileRegex(IndexResolver, int, String, int) (parameter resolver)}</li>
 *   <li>{@code member compileRegex(IndexResolver, int, String, int, IProgressMonitor) (parameter resolver)}</li>
 *   <li>{@code member fromAutomatonImage(IndexResolver, int, MIndexRegexProgram.Image) (parameter resolver)}</li>
 *   <li>{@code member compileLiteralAlternatives(IndexResolver, int, List<? extends CharSequence>) (parameter resolver)}</li>
 *   <li>{@code member compileLiteralAlternatives(IndexResolver, int, List<? extends CharSequence>, IProgressMonitor) (parameter resolver)}</li>
 *   <li>{@code member fromLiteralAlternativesImage(IndexResolver, MIndexLiteralSearchTree.Image) (parameter resolver)}</li>
 *   <li>{@code member compileCharacterGlob(IndexResolver, int, CharSequence) (parameter resolver)}</li>
 *   <li>{@code member compileCharacterGlob(IndexResolver, int, CharSequence, IProgressMonitor) (parameter resolver)}</li>
 *   <li>{@code member analyzeCharacterGlob(CharSequence) (return type)}</li>
 *   <li>{@code member compileTokenGlob(IndexResolver, int, CharSequence) (parameter resolver)}</li>
 *   <li>{@code member compileTokenGlob(IndexResolver, int, CharSequence, IProgressMonitor) (parameter resolver)}</li>
 *   <li>{@code member compileTokenAlternatives(MIndexString...) (parameter alternatives)}</li>
 *   <li>{@code member compileTokenAlternatives(IProgressMonitor, MIndexString...) (parameter monitor)}</li>
 *   <li>{@code member analyzeRegex(String) (return type)}</li>
 *   <li>{@code member analyzeRegex(String, int) (return type)}</li>
 *   <li>{@code member resolver() (return type)}</li>
 *   <li>{@code member strategy() (return type)}</li>
 *   <li>{@code member matches(MIndexString) (parameter input)}</li>
 *   <li>{@code member lookingAt(MIndexString) (parameter input)}</li>
 *   <li>{@code member find(MIndexString) (parameter input)}</li>
 *   <li>{@code member matchesTokenRange(MIndexString, int, int) (parameter input)}</li>
 *   <li>and 2 more</li>
 * </ul>
 */
public final class M3PatternPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code resolver} of type {@code IndexResolver}. */
    private int[] resolverLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code strategy} of type {@code Strategy}. */
    private int[] strategyLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code literal} of type {@code MIndexString}. */
    private int[] literalLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code literalTree} of type {@code MIndexLiteralSearchTree}. */
    private int[] literalTreeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code tokenTrie} of type {@code MIndexTokenTrie}. */
    private int[] tokenTrieLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code glob} of type {@code MIndexTokenPattern}. */
    private int[] globLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code automaton} of type {@code MIndexRegexProgram}. */
    private int[] automatonLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code re2j} of type {@code Pattern}. */
    private int[] re2jLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code requiredLiteral} of type {@code MIndexString}. */
    private int[] requiredLiteralLane;

    /** primitive lane for source field {@code requiredSignal} of type {@code long}. */
    private long requiredSignal;

    private M3PatternPlan() {
        throw new M3NotYetReceived("M3PatternPlan#<init>/0");
    }

    public long requiredSignal64() {
        throw new M3NotYetReceived("M3PatternPlan#requiredSignal64/0");
    }

    public boolean usesRe2j() {
        throw new M3NotYetReceived("M3PatternPlan#usesRe2j/0");
    }

    public boolean usesMIndexAutomaton() {
        throw new M3NotYetReceived("M3PatternPlan#usesMIndexAutomaton/0");
    }

    public boolean isTokenNative() {
        throw new M3NotYetReceived("M3PatternPlan#isTokenNative/0");
    }

    public boolean isCharacterDomain() {
        throw new M3NotYetReceived("M3PatternPlan#isCharacterDomain/0");
    }

    public int mindexAutomatonStates() {
        throw new M3NotYetReceived("M3PatternPlan#mindexAutomatonStates/0");
    }

    public boolean hasLiteralPrefilter() {
        throw new M3NotYetReceived("M3PatternPlan#hasLiteralPrefilter/0");
    }

    public static String characterGlobRegex(CharSequence glob) {
        throw new M3NotYetReceived("M3PatternPlan#characterGlobRegex/1");
    }
}
