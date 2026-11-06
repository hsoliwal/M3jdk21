// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

/**
 * FNV-1a 64 over packed lanes: the one hash primitive shared by every atom of the family.
 *
 * <p>Inputs: a lane slice ({@code int[]}, {@code long[]}) or UTF-16 code units. Output: one
 * {@code long}. Effects: none (pure, allocation-free). Failure: none; an empty slice hashes to
 * {@link #OFFSET}. The constants and the xor-then-multiply order are the LOCKED L-Hash v1 spec
 * ({@code cpu_lhash.py _fnv}) and the cognix-rag SimHash word hash; changing either breaks the
 * oracle differential, which is what {@code discriminators.sh} proves.</p>
 */
//FLLDCIM: HPCPOP — hot path; loops over primitive lanes, no allocation
//FLLDCIM: SFA — constants hoisted; OFFSET/PRIME are the spec, never derived
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3Fnv1a.java blob 5a43302b939b; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3LaneFnv1a {
    public static final long OFFSET = 0xcbf29ce484222325L;
    public static final long PRIME = 0x100000001b3L;

    private M3LaneFnv1a() { }

    /** FNV-1a over {@code values[from, to)}; each int is widened to a long before the xor. */
    public static long ofInts(int[] values, int from, int to) {
        long hash = OFFSET;
        for (int i = from; i < to; i++) {
            hash ^= values[i];
            hash *= PRIME;
        }
        return hash;
    }

    /** FNV-1a over {@code values[from, to)} (64-bit lanes, e.g. H5 windows or band rows). */
    public static long ofLongs(long[] values, int from, int to) {
        long hash = OFFSET;
        for (int i = from; i < to; i++) {
            hash ^= values[i];
            hash *= PRIME;
        }
        return hash;
    }

    /** FNV-1a over UTF-16 code units, stable across JVMs (cognix-rag SimHash.accumulate). */
    public static long ofChars(CharSequence text) {
        long hash = OFFSET;
        for (int i = 0, n = text.length(); i < n; i++) {
            hash ^= text.charAt(i);
            hash *= PRIME;
        }
        return hash;
    }

    /** FNV-1a continued from {@code seed} over one more value (window and band keys). */
    public static long step(long seed, long value) {
        return (seed ^ value) * PRIME;
    }
}
