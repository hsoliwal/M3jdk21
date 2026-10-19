// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexJson}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexJson.java}
 * blob {@code 910b06e020889de3599fe339b163b11bf4e070af} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code af176de5479a09f56b103115507723f5738c33b6b95d8ad51bc8f29da8c87e1c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Json}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
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
 *   <li>{@code source supertypes not received: implements SelectiveValueCodec}</li>
 *   <li>{@code member decodeTape(byte[], MIndexUtf8KeyIndex, MIndexUtf8KeyIndex, int, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member decodeTape(byte[], int, int) (return type)}</li>
 *   <li>{@code member decodeFast(byte[], MIndexUtf8KeyIndex, int, int, int, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member decode(byte[], int, int, int) (return type)}</li>
 *   <li>{@code member decode(byte[], MIndexSchemaPlan, int, int, int) (return type)}</li>
 *   <li>{@code member select(byte[], MIndexPath, int, int, int) (return type)}</li>
 *   <li>{@code member encode(MIndexValues, int) (parameter values)}</li>
 * </ul>
 */
public final class M3Json {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    public M3Json() {
        throw new M3NotYetReceived("M3Json#<init>/0");
    }

    public String format() {
        throw new M3NotYetReceived("M3Json#format/0");
    }
}
