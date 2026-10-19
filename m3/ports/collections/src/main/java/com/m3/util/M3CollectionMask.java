// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexCollectionMask}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexCollectionMask.java}
 * blob {@code 82ad827480e38d1417f33f0f2011e4c1cf4c102e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code fcf00dea271d0c714cc50913db2e65a320aa4cea4eba0be38624aefa128c8ec4}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3CollectionMask}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3CollectionMask {
    RAW,
    ASCII_UPPER,
    ASCII_LOWER,
    ASCII_TITLE,
    ASCII_CASE_BITMAP,
    ASCII_CASE_INLINE4,
    ASCII_CASE_INLINE8,
    ASCII_CASE_INLINE12,
    RANGE_WHOLE,
    RANGE_SMALL,
    RANGE_MEDIUM,
    RANGE_EXTENDED,
    MASKED_UNKNOWN,
    MIXED;
}
