// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexDomainTransfer}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexDomainTransfer.java}
 * blob {@code ccad6c22bcfb37f9c15fa9f0ba599a8bab251d2e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 375a05dd58bbb000cc59c5f905bfdf235a5974df227d843610155caa3278aff7}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3DomainTransfer}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member transferId(MIndexSpace<E>, int, MIndexSpace<E>) (parameter source)}</li>
 *   <li>{@code member transferIds(MIndexSpace<E>, int[], MIndexSpace<E>) (parameter source)}</li>
 * </ul>
 */
public interface M3DomainTransfer<E> {
    default boolean identityWhenSameSpace() {
        throw new M3NotYetReceived("M3DomainTransfer#identityWhenSameSpace/0");
    }
}
