// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.function.IntConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.algorithm.MIndexPreparedHorspoolPattern}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/algorithm/src/main/java/com/synexia/mindex/algorithm/MIndexPreparedHorspoolPattern.java}
 * blob {@code 1bc4c8b3636f25923ed91911a80314bf17d10e5d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 19e80fc4bbdf83c5ca91743ef566f203575d78719de82eceb94593082b7323cf}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.algorithm.M3PreparedHorspoolPattern}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Lane, NativeSupport}</li>
 *   <li>{@code member compile(CharSequence, boolean, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member lane() (return type)}</li>
 *   <li>{@code member firstIndexIn(CharSequence, int, int, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member scan(CharSequence, int, int, IntConsumer, MIndexProgressMonitor) (parameter monitor)}</li>
 * </ul>
 */
public final class M3PreparedHorspoolPattern {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code pattern} of type {@code char[]}. */
    private char[] pattern;

    /** primitive lane for source field {@code latin1Shift} of type {@code int[]}. */
    private int[] latin1Shift;

    /** primitive lane for source field {@code unicodeKeys} of type {@code char[]}. */
    private char[] unicodeKeys;

    /** primitive lane for source field {@code unicodeShift} of type {@code int[]}. */
    private int[] unicodeShift;

    /** int handle lane (offsets into parent-owned storage) for source field {@code foldedFallback} of type {@code MIndexPreparedKmpPattern}. */
    private int[] foldedFallbackLane;

    /** primitive lane for source field {@code caseSensitive} of type {@code boolean}. */
    private boolean caseSensitive;

    /** primitive lane for source field {@code length} of type {@code int}. */
    private int length;

    private M3PreparedHorspoolPattern() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#<init>/0");
    }

    public static M3PreparedHorspoolPattern compile(CharSequence pattern, boolean caseSensitive) {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#compile/2");
    }

    public int length() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#length/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#isEmpty/0");
    }

    public boolean caseSensitive() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#caseSensitive/0");
    }

    public int firstIndexIn(CharSequence text) {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#firstIndexIn/1");
    }

    public boolean matchesPrefix(CharSequence text) {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#matchesPrefix/1");
    }

    public boolean matches(char textUnit, int patternIndex) {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#matches/2");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#retainedPrimitiveBytes/0");
    }

    public static boolean nativeSearchAvailable() {
        throw new M3NotYetReceived("M3PreparedHorspoolPattern#nativeSearchAvailable/0");
    }
}
