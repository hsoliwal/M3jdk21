// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Optional;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.spec.MIndexSpecPrecompute}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/spec/MIndexSpecPrecompute.java}
 * blob {@code 8f996f0cab68aeb56a4a044550470bd35051890b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e875c0bc4a005ff7e5e67bfdcf17830bc932e7ba416661955ff3737f66019477}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.spec.M3SpecPrecompute}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
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
 *   <li>{@code member vocabularyOnly(MIndexSpecVocabulary) (parameter vocabulary)}</li>
 *   <li>{@code member withGrammar(MIndexSpecVocabulary, MIndexLang) (parameter vocabulary)}</li>
 *   <li>{@code member vocabulary() (return type)}</li>
 *   <li>{@code member lang() (return type)}</li>
 *   <li>{@code member astSpec() (return type)}</li>
 *   <li>{@code member parseAst(CharSequence) (return type)}</li>
 *   <li>{@code member compilePattern(MIndexPrecomputeLimits, IProgressMonitor, String...) (return type)}</li>
 * </ul>
 */
public final class M3SpecPrecompute {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code vocabulary} of type {@code MIndexSpecVocabulary}. */
    private int[] vocabularyLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code lang} of type {@code MIndexLang}. */
    private int[] langLane;

    /** primitive lane for source field {@code fingerprint} of type {@code long}. */
    private long fingerprint;

    private M3SpecPrecompute() {
        throw new M3NotYetReceived("M3SpecPrecompute#<init>/0");
    }

    public int termId(CharSequence term) {
        throw new M3NotYetReceived("M3SpecPrecompute#termId/1");
    }

    public long fingerprint() {
        throw new M3NotYetReceived("M3SpecPrecompute#fingerprint/0");
    }

    public long retainedVocabularyBytes() {
        throw new M3NotYetReceived("M3SpecPrecompute#retainedVocabularyBytes/0");
    }
}
