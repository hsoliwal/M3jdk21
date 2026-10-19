// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.stream.LongStream;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongAtomStreams}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexLongAtomStreams.java}
 * blob {@code bc240519ae4e84dd2907e849af7733b88ba69775} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c4e8923b8baded3abeaa16e39d37064746a93aeecd93c86591a2f712318b32b3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongAtomStreams}); projection
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
 *   <li>{@code member kind(MIndexLongAtomTape, MIndexAtomTape.Kind) (parameter kind)}</li>
 * </ul>
 */
public final class M3LongAtomStreams {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LongAtomStreams() {
        throw new M3NotYetReceived("M3LongAtomStreams#<init>/0");
    }

    public static LongStream all(M3LongAtomTape tape) {
        throw new M3NotYetReceived("M3LongAtomStreams#all/1");
    }

    public static LongStream significant(M3LongAtomTape tape) {
        throw new M3NotYetReceived("M3LongAtomStreams#significant/1");
    }

    public static LongStream known(M3MappedStructureIndex index, int atomId) {
        throw new M3NotYetReceived("M3LongAtomStreams#known/2");
    }
}
