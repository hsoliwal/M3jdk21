// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexHybridFuzzyMatches}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexHybridFuzzyMatches.java}
 * blob {@code d1be5049abfae4e59034129ae4998cb2b186477e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 29709262f1cf28719bb44a25afbf49b2049b3905b0f89240ac0a0ab2edc7ec83}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3HybridFuzzyMatches}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3HybridFuzzyMatches(M3Rows values, int[] distances, int[] jaroWinklerQ16, int[] trigramJaccardQ16) {
    public int[] distances() {
        throw new M3NotYetReceived("M3HybridFuzzyMatches#distances/0");
    }

    public int[] jaroWinklerQ16() {
        throw new M3NotYetReceived("M3HybridFuzzyMatches#jaroWinklerQ16/0");
    }

    public int[] trigramJaccardQ16() {
        throw new M3NotYetReceived("M3HybridFuzzyMatches#trigramJaccardQ16/0");
    }

    public double jaroWinkler(int index) {
        throw new M3NotYetReceived("M3HybridFuzzyMatches#jaroWinkler/1");
    }

    public double trigramJaccard(int index) {
        throw new M3NotYetReceived("M3HybridFuzzyMatches#trigramJaccard/1");
    }
}
