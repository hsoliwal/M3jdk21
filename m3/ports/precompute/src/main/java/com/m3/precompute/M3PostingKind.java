// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexPostingKind}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexPostingKind.java}
 * blob {@code 43df6c5d6ed048f08a3e35211b2f84178a30f3ef} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8101559753dbbf1eb01593f6045eeb7bc546a67e75ab98818673cee452711019}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3PostingKind}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3PostingKind {
    ENTITY_TYPE,
    ATTRIBUTE,
    RELATION_FORWARD,
    RELATION_REVERSE,
    FEATURE;

    public int code() {
        throw new M3NotYetReceived("M3PostingKind#code/0");
    }

    public static M3PostingKind fromCode(int code) {
        throw new M3NotYetReceived("M3PostingKind#fromCode/1");
    }
}
