// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexImageLimits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexImageLimits.java}
 * blob {@code f12e2e42a7841fc01aa1e0a66c77a61bf7dc2154} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2cbbda13d58ceb71626b460244560166f8725607bd92ed1b8dfeb72c2fec7d53}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3ImageLimits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S22}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member dagLimits() (return type)}</li>
 * </ul>
 */
public record M3ImageLimits(int maxNodes, int maxEdges, int maxTextUnits, int maxImageBytes) {
    public static M3ImageLimits defaults() {
        throw new M3NotYetReceived("M3ImageLimits#defaults/0");
    }
}
