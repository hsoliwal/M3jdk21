// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexCanonicalCollection}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexCanonicalCollection.java}
 * blob {@code e12e7d9c4c273e37817c3699195a46eef524b1cf} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f53782f870c217d60084ab27414d07bb432324900d16a3606db642a38ef91ce7}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3CanonicalCollection}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member compositeIndex() (return type)}</li>
 *   <li>{@code member precompute(MIndexCollectionPrecompute) (parameter owner)}</li>
 * </ul>
 */
public interface M3CanonicalCollection {
    int canonicalId();

    int kind();

    int size();

    long structuralHash64();

    long signal64();
}
