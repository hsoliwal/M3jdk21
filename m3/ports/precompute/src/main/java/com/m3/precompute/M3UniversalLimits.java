// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexUniversalLimits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexUniversalLimits.java}
 * blob {@code 885e5a3bbed9426e7ab0f54c0564221c8612974a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2e8d6b658b948aca5ce1344abfb895c683699079cc44939cf783daa645880e9d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3UniversalLimits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant DEFAULT (M3UniversalLimits)}</li>
 * </ul>
 */
public record M3UniversalLimits(int maxEntities, int maxDictionaryEntries, long maxDictionaryUtf16Units, int maxAttributes, int maxRelations, int maxFeatures, int maxApiRows, int maxApiArguments, int maxApiPayloadValues, int maxPostingValues, int maxBlockSize) {
    public void entities(int count) {
        throw new M3NotYetReceived("M3UniversalLimits#entities/1");
    }

    public void dictionary(int entries, long utf16Units) {
        throw new M3NotYetReceived("M3UniversalLimits#dictionary/2");
    }

    public void attributes(int count) {
        throw new M3NotYetReceived("M3UniversalLimits#attributes/1");
    }

    public void relations(int count) {
        throw new M3NotYetReceived("M3UniversalLimits#relations/1");
    }

    public void features(int count) {
        throw new M3NotYetReceived("M3UniversalLimits#features/1");
    }

    public void api(int rows, int arguments, int payloadValues) {
        throw new M3NotYetReceived("M3UniversalLimits#api/3");
    }

    public void postings(int values) {
        throw new M3NotYetReceived("M3UniversalLimits#postings/1");
    }
}
