// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;

/**
 * L-Hash v1 (LOCKED): five FNV-1a components over a canonical token lane, no AST.
 *
 * <p>Input: {@link M3LHashTokens.Tokens}. Output: {@code long[5]} = H1 exact structural identity,
 * H2 operator multiset, H3 control depth, H4 operator frequency, H5 fold (FNV-1a over the window
 * hashes of {@link #windows}); {@link #windows} gives the H5 lane itself (k = {@value #WINDOW}).
 * Effects: none. Failure: none; an empty lane yields {@link M3LaneFnv1a#OFFSET} components and no
 * windows. Equality of H1 proves equality of the canonical lanes, never semantic equivalence.
 * Byte-for-byte twin of {@code cpu_lhash.py hash_ids}; the differential contract and the
 * discriminators (window off-by-one, swapped prime) keep it so.</p>
 */
//FLLDCIM: LHASH — H1..H5 as in cpu_lhash.py hash_ids; W=8; ids 51..100 are operators
//FLLDCIM: HPCPOP — hot path; int lane in, long[5] out
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3LogicHash.java blob 2eaecc6a5cc6; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3LogicHash {
    public static final int WINDOW = 8;
    public static final String ALGORITHM = "LHASH";
    public static final int VERSION = 1;
    private static final int CONTROL_MAX = 50;
    private static final int OPERATOR_MIN = 51;
    private static final int OPERATOR_MAX = 100;

    private M3LogicHash() { }

    public static long[] of(M3LHashTokens.Tokens tokens) {
        int[] ids = tokens.ids();
        int n = tokens.size();
        long[] windows = windows(tokens);
        return new long[] {M3LaneFnv1a.ofInts(ids, 0, n), operatorMultiset(ids, n), controlDepth(ids, n),
            operatorFrequency(ids, n), M3LaneFnv1a.ofLongs(windows, 0, windows.length)};
    }

    public static long[] of(String source) {
        return of(M3LHashTokens.tokenize(source));
    }

    /** H5 lane: one FNV-1a per window of {@value #WINDOW} ids; one window of all ids when shorter. */
    public static long[] windows(M3LHashTokens.Tokens tokens) {
        int[] ids = tokens.ids();
        int n = tokens.size();
        if (n == 0) return new long[0];
        if (n < WINDOW) return new long[] {M3LaneFnv1a.ofInts(ids, 0, n)};
        long[] windows = new long[n - WINDOW + 1];
        for (int i = 0; i < windows.length; i++) windows[i] = M3LaneFnv1a.ofInts(ids, i, i + WINDOW);
        return windows;
    }

    /** H2: FNV-1a over the sorted operator ids. */
    static long operatorMultiset(int[] ids, int n) {
        int[] operators = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (ids[i] >= OPERATOR_MIN && ids[i] <= OPERATOR_MAX) operators[count++] = ids[i];
        }
        Arrays.sort(operators, 0, count);
        return M3LaneFnv1a.ofInts(operators, 0, count);
    }

    /** H3: FNV-1a over the bracket depth at each control keyword. */
    static long controlDepth(int[] ids, int n) {
        int[] depths = new int[n];
        int count = 0;
        int depth = 0;
        for (int i = 0; i < n; i++) {
            int id = ids[i];
            if (id == 101 || id == 103 || id == 105) depth++;
            else if (id == 102 || id == 104 || id == 106) depth = Math.max(0, depth - 1);
            else if (id >= 1 && id <= CONTROL_MAX) depths[count++] = depth;
        }
        return M3LaneFnv1a.ofInts(depths, 0, count);
    }

    /** H4: FNV-1a over [arith, comp, logic, assign, call] counts. */
    static long operatorFrequency(int[] ids, int n) {
        int[] counts = new int[5];
        for (int i = 0; i < n; i++) {
            int id = ids[i];
            if (id >= 57 && id <= 61) counts[0]++;
            else if (id >= 54 && id <= 56) counts[1]++;
            else if (id >= 51 && id <= 53) counts[2]++;
            else if (id == 62 || id == 63) counts[3]++;
            if (id == M3LHashTokens.IDENT && i + 1 < n && ids[i + 1] == M3LHashTokens.LPAREN) counts[4]++;
        }
        return M3LaneFnv1a.ofInts(counts, 0, 5);
    }
}
