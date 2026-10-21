// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexArgumentRoleBits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexArgumentRoleBits.java}
 * blob {@code 7dc48bec582fd7768142739cb15a8177dd581288} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code aa7ab0ae883a28c0603b90be166d4d359a79f57dda2c17de1030dc205f518919}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3ArgumentRoleBits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3ArgumentRoleBits {
    public static final int CLAIM = 1;

    public static final int FACT = 1 << 1;

    public static final int EVIDENCE = 1 << 2;

    public static final int REASON = 1 << 3;

    public static final int RULE = 1 << 4;

    public static final int CONCLUSION = 1 << 5;

    public static final int CONTEXT = 1 << 6;

    public static final int ASSUMPTION = 1 << 7;

    public static final int OBSERVATION = 1 << 8;

    public static final int COUNTEREXAMPLE = 1 << 9;

    public static final int OTHER = 1 << 10;

    public static final int KNOWN_MASK = (1 << 11) - 1;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3ArgumentRoleBits() {
        throw new M3NotYetReceived("M3ArgumentRoleBits#<init>/0");
    }

    public static void requireKnown(int bits) {
        throw new M3NotYetReceived("M3ArgumentRoleBits#requireKnown/1");
    }

    public static boolean has(int bits, int required) {
        throw new M3NotYetReceived("M3ArgumentRoleBits#has/2");
    }

    public static boolean hasAny(int bits, int mask) {
        throw new M3NotYetReceived("M3ArgumentRoleBits#hasAny/2");
    }

    public static List<String> names(int bits) {
        throw new M3NotYetReceived("M3ArgumentRoleBits#names/1");
    }
}
