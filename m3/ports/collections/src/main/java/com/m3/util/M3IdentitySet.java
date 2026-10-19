// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.util.function.IntConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collection.MIndexIdentitySet}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/collection/MIndexIdentitySet.java}
 * blob {@code 3f321f3cb80e70cbac6a945cdae9aa3f0695c522} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2f82c630c6456cf652c17ed8bebd235e78d1d799be0c1cf23ebf674e894aaa1d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collection.M3IdentitySet}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S03}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 */
interface M3IdentitySet {
    boolean containsIdentity(int identity);

    void forEachIdentity(IntConsumer action);
}
