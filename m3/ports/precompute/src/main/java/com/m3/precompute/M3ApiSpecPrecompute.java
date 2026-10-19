// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Optional;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.spec.MIndexApiSpecPrecompute}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/spec/MIndexApiSpecPrecompute.java}
 * blob {@code 537364d42d948c37d5f2bfb2bb129912fe3f6e8d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code fc7db981599ea10c7dad238b4bd5386e579ad9d6753fc3de378f1f013738f379}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.spec.M3ApiSpecPrecompute}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
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
 *   <li>{@code member vocabularyOnly(MIndexApiSpecFamily) (parameter family)}</li>
 *   <li>{@code member withGrammar(MIndexApiSpecFamily, MIndexLang) (parameter family)}</li>
 *   <li>{@code member family() (return type)}</li>
 *   <li>{@code member vocabulary() (return type)}</li>
 *   <li>{@code member lang() (return type)}</li>
 *   <li>{@code member astSpec() (return type)}</li>
 *   <li>{@code member parseAst(CharSequence) (return type)}</li>
 *   <li>{@code member compilePattern(MIndexPrecomputeLimits, IProgressMonitor, String...) (return type)}</li>
 * </ul>
 */
public final class M3ApiSpecPrecompute {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code family} of type {@code MIndexApiSpecFamily}. */
    private int[] familyLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code precompute} of type {@code MIndexSpecPrecompute}. */
    private int[] precomputeLane;

    private M3ApiSpecPrecompute() {
        throw new M3NotYetReceived("M3ApiSpecPrecompute#<init>/0");
    }

    public int termId(CharSequence term) {
        throw new M3NotYetReceived("M3ApiSpecPrecompute#termId/1");
    }

    public long fingerprint() {
        throw new M3NotYetReceived("M3ApiSpecPrecompute#fingerprint/0");
    }

    public long retainedVocabularyBytes() {
        throw new M3NotYetReceived("M3ApiSpecPrecompute#retainedVocabularyBytes/0");
    }
}
