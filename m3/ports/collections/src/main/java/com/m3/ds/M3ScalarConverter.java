// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexScalarConverter}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexScalarConverter.java}
 * blob {@code e26cd05d52553cd816e81f6783d1df3efb0fa6e9} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d87bf22bdd40d1c0353579c6b9f2ae0ff2f077101ee4b489c1ccfd460c824500}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3ScalarConverter}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member forKind(MIndexValues.Kind) (parameter kind)}</li>
 *   <li>{@code member kind() (return type)}</li>
 *   <li>{@code member admit(MIndexValues.Builder, MIndexString) (parameter builder)}</li>
 *   <li>{@code member admit(MIndexValues.Builder, CharSequence) (parameter builder)}</li>
 * </ul>
 */
public final class M3ScalarConverter {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code kind} of type {@code MIndexValues.Kind}. */
    private int[] kindLane;

    private M3ScalarConverter() {
        throw new M3NotYetReceived("M3ScalarConverter#<init>/0");
    }
}
