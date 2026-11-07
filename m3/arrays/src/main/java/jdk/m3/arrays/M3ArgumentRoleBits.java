// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.ArrayList;
import java.util.List;

/**
 * M3 SDK scaffold of {@code jdk.internal.m3.indexstring.MIndexArgumentRoleBits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}, except: census3 P3-007 (qualification/census3-gaps-20261107) received the atoms of requireKnown, has, hasAny, names and their private helpers from the same donor blob at develop 78cf2ad4c7e5.
 *
 * <p>Lineage: Synexia {@code m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexArgumentRoleBits.java}
 * blob {@code 7dc48bec582fd7768142739cb15a8177dd581288} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code aa7ab0ae883a28c0603b90be166d4d359a79f57dda2c17de1030dc205f518919}.
 * Copy contract {@code jdk.internal.m3.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code jdk.internal.m3.indexstring.M3ArgumentRoleBits}); projection
 * {@code jdk.internal.m3.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
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

    //FLLDCIM: MRL - census3 P3-007, donor MIndexArgumentRoleBits blob 7dc48bec582f: atoms received
    public static void requireKnown(int bits) {
        if ((bits & ~KNOWN_MASK) != 0) {
            throw new IllegalArgumentException("unknown argument role bits: " + bits);
        }
    }

    public static boolean has(int bits, int required) {
        requireKnown(required);
        return (bits & required) == required;
    }

    public static boolean hasAny(int bits, int mask) {
        requireKnown(mask);
        return (bits & mask) != 0;
    }

    public static List<String> names(int bits) {
        requireKnown(bits);
        ArrayList<String> names = new ArrayList<>();
        add(names, bits, CLAIM, "CLAIM");
        add(names, bits, FACT, "FACT");
        add(names, bits, EVIDENCE, "EVIDENCE");
        add(names, bits, REASON, "REASON");
        add(names, bits, RULE, "RULE");
        add(names, bits, CONCLUSION, "CONCLUSION");
        add(names, bits, CONTEXT, "CONTEXT");
        add(names, bits, ASSUMPTION, "ASSUMPTION");
        add(names, bits, OBSERVATION, "OBSERVATION");
        add(names, bits, COUNTEREXAMPLE, "COUNTEREXAMPLE");
        add(names, bits, OTHER, "OTHER");
        return List.copyOf(names);
    }

    //FLLDCIM: MRL - census3 P3-007, donor MIndexArgumentRoleBits blob 7dc48bec582f: restored private helper
    private static void add(List<String> names, int bits, int mask, String name) {
        if ((bits & mask) != 0) names.add(name);
    }
}
