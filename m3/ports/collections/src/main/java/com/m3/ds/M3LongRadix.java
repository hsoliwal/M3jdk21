// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongRadix}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/data-structure/src/main/java/com/synexia/mindex/structure/MIndexLongRadix.java}
 * blob {@code c8456e642c775d8b9a0c736cafba020efdce4784} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 3718e2592f54f16d312a8e20b7848d7a7abc5a92b36edd422892dc2dd5ff4410}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongRadix}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S06}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3LongRadix {
    public static final int RADIX = 256;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LongRadix() {
        throw new M3NotYetReceived("M3LongRadix#<init>/0");
    }

    public static void sortSigned(long[] values) {
        throw new M3NotYetReceived("M3LongRadix#sortSigned/1");
    }

    public static void sortSigned(long[] values, M3LongRadixWorkspace workspace) {
        throw new M3NotYetReceived("M3LongRadix#sortSigned/2");
    }

    public static void sortUnsigned(long[] values) {
        throw new M3NotYetReceived("M3LongRadix#sortUnsigned/1");
    }

    public static void sortUnsigned(long[] values, M3LongRadixWorkspace workspace) {
        throw new M3NotYetReceived("M3LongRadix#sortUnsigned/2");
    }

    public static void sortSigned(long[] values, long[] scratch, int[] counts) {
        throw new M3NotYetReceived("M3LongRadix#sortSigned/3");
    }

    public static void sortUnsigned(long[] values, long[] scratch, int[] counts) {
        throw new M3NotYetReceived("M3LongRadix#sortUnsigned/3");
    }

    public static void sortPairsSigned(long[] keys, long[] payloads, long[] keyScratch, long[] payloadScratch, int[] counts) {
        throw new M3NotYetReceived("M3LongRadix#sortPairsSigned/5");
    }
}
