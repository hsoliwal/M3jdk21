// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexQGramIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexQGramIndex.java}
 * blob {@code 67e71a94cd810e4be2ced0ebc52889261f1baf2e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d8286091c49de3ae631c43f2bf60f19cc1fc0ec6b97fbd8e42d9b31cf4bb9db1}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3QGramIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
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
 *   <li>{@code nested types not received: Candidates, SparseCounts}</li>
 *   <li>{@code member compile(List<MIndexString>, int) (parameter values)}</li>
 *   <li>{@code member candidates(CharSequence, int) (return type)}</li>
 * </ul>
 */
public final class M3QGramIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code q} of type {@code int}. */
    private int q;

    /** primitive lane for source field {@code valueCount} of type {@code int}. */
    private int valueCount;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code valueIds} of type {@code int[]}. */
    private int[] valueIds;

    private M3QGramIndex() {
        throw new M3NotYetReceived("M3QGramIndex#<init>/0");
    }

    public int q() {
        throw new M3NotYetReceived("M3QGramIndex#q/0");
    }

    public int valueCount() {
        throw new M3NotYetReceived("M3QGramIndex#valueCount/0");
    }

    public int postingCount() {
        throw new M3NotYetReceived("M3QGramIndex#postingCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3QGramIndex#primitivePayloadBytes/0");
    }
}
