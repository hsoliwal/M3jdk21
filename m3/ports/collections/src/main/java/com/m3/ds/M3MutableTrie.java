// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexMutableTrie}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexMutableTrie.java}
 * blob {@code c0d284f21002d030cfd948719a1eadf57f5a45b1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 79af74657c58412fe33643eeeab05ada341b48e3e226e9dc4795b4692a95a48c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3MutableTrie}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
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
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 * </ul>
 */
final class M3MutableTrie {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code edgeByParentAndUnit} of type {@code Map<Long, Integer>}. */
    private int[] edgeByParentAndUnitLane;

    /** primitive lane for source field {@code firstChild} of type {@code int[]}. */
    private int[] firstChild;

    /** primitive lane for source field {@code nextSibling} of type {@code int[]}. */
    private int[] nextSibling;

    /** primitive lane for source field {@code terminalRow} of type {@code int[]}. */
    private int[] terminalRow;

    /** primitive lane for source field {@code nodeCount} of type {@code int}. */
    private int nodeCount;

    private M3MutableTrie() {
        throw new M3NotYetReceived("M3MutableTrie#<init>/0");
    }
}
