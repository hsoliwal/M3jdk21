// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexStringTypeBits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringTypeBits.java}
 * blob {@code 52545a951b49a049aa75dab2c6f66c5af2194477} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 5c244fade423701d7dbea2cbb5907bdafe4e99c22c1fdb599e78e5ab3f462875}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3StringTypeBits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant NUMERIC (int)}</li>
 * </ul>
 */
public final class M3StringTypeBits {
    public static final int EMPTY = 1;

    public static final int BLANK = 1 << 1;

    public static final int WORD = 1 << 2;

    public static final int IDENTIFIER = 1 << 3;

    public static final int INTEGER = 1 << 4;

    public static final int DECIMAL = 1 << 5;

    public static final int BOOLEAN = 1 << 6;

    public static final int PUNCTUATION = 1 << 7;

    public static final int SYMBOL = 1 << 8;

    public static final int ASCII = 1 << 9;

    public static final int COMPOSITE = 1 << 10;

    public static final int MASKED = 1 << 11;

    public static final int OTHER = 1 << 12;

    public static final int KNOWN_MASK = (1 << 13) - 1;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3StringTypeBits() {
        throw new M3NotYetReceived("M3StringTypeBits#<init>/0");
    }

    public static boolean has(int bits, int required) {
        throw new M3NotYetReceived("M3StringTypeBits#has/2");
    }

    public static boolean hasAny(int bits, int mask) {
        throw new M3NotYetReceived("M3StringTypeBits#hasAny/2");
    }

    public static List<String> names(int bits) {
        throw new M3NotYetReceived("M3StringTypeBits#names/1");
    }
}
