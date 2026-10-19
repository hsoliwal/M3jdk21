// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexCollection}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexCollection.java}
 * blob {@code e0389ed33fc60c34ae4269ae7e1643dc36634561} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 450e7eac8038f135638962f2bb74d9d5e1d6859cd565888807d0374e2184a81d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3Collection}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: extends M3Collection}</li>
 * </ul>
 */
public interface M3Collection {
    M3CollectionCatalog catalog();

    M3CollectionUseCase useCase();
}
