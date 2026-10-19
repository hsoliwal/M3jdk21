// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructuredProjection}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructuredProjection.java}
 * blob {@code 46de181391940608f564d1b6d4d130d8c9d0cb56} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 11753bef2420713ba4ce7dd7a0ed37bc0961f330f04e6ca304e7da5ff13110dd}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3StructuredProjection}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
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
 *   <li>{@code nested types not received: Relation, Limits, Compiled, Reachable, AstCompiler}</li>
 *   <li>{@code member compile(MIndexDocument, long, long, long) (return type)}</li>
 *   <li>{@code member compile(MIndexDocument, long, long, long, Limits) (return type)}</li>
 *   <li>{@code member relation(long, Relation) (parameter relation)}</li>
 * </ul>
 */
public final class M3StructuredProjection {
    public static final long GRAMMAR_STRUCTURED = 0x5354_5255_4354_0001L;

    public static final long FLAG_SCALAR = 1L;

    public static final long FLAG_COMPOSITION = 1L << 1;

    public static final long FLAG_REFERENCE = 1L << 2;

    public static final long FLAG_EDGE = 1L << 3;

    public static final long FLAG_EVENT = 1L << 4;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3StructuredProjection() {
        throw new M3NotYetReceived("M3StructuredProjection#<init>/0");
    }
}
