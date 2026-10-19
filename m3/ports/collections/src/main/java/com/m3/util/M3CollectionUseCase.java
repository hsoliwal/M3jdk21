// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexCollectionUseCase}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexCollectionUseCase.java}
 * blob {@code c91f56ff94a01f90c251c77dab3055ba082a8d97} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6770934395b30fb21475e27aea891266e4208448d3f1867784a33b6b88c81cf2}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3CollectionUseCase}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3CollectionUseCase {
    SEQUENCE,
    MEMBERSHIP,
    VALUES,
    FREQUENCY,
    DEQUE,
    ORDERED,
    PREFIX,
    SUFFIX,
    SUBSTRING,
    METADATA,
    FUZZY,
    SEMANTIC_GROUPS,
    TOKEN_SEQUENCE,
    CORPUS_RANKING,
    NUMERIC,
    UNICODE,
    LEXICON,
    STATEMENT,
    DAG;
}
