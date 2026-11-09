// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;

/**
 * Banded LSH over MinHash signatures: bucket keys per band and candidate pairs.
 *
 * <p>Input: signatures of equal length {@code k = bands * rows}. Output: {@link #keys} gives one
 * FNV-1a bucket key per band (band index folded in); {@link #candidatePairs} gives every pair
 * {@code (i < j)} sharing a key in at least one band, packed as {@code i << 32 | j}, sorted and
 * distinct. Effects: none. Failure: {@code bands * rows != k} is an
 * {@link IllegalArgumentException}. Candidate probability is {@code 1 - (1 - J^rows)^bands};
 * the recall contract plants near-duplicates and requires at least 0.95 of them back.</p>
 */
//FLLDCIM: MINHASH — LSH buckets = FNV over band rows; pairs as packed long lane
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3MinHashLsh.java blob f1004343232a; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3MinHashLsh {
    public static final String ALGORITHM = "MINHASH_LSH";
    public static final int VERSION = 1;
    public static final int DEFAULT_BANDS = 32;
    public static final int DEFAULT_ROWS = 4;

    private M3MinHashLsh() { }

    public static long[] keys(long[] signature, int bands, int rows) {
        if (bands * rows != signature.length) throw new IllegalArgumentException("bands*rows != k");
        long[] keys = new long[bands];
        for (int band = 0; band < bands; band++) {
            keys[band] = M3LaneFnv1a.step(M3LaneFnv1a.ofLongs(signature, band * rows, (band + 1) * rows), band);
        }
        return keys;
    }

    public static long[] candidatePairs(long[][] signatures, int bands, int rows) {
        long[][] keys = new long[signatures.length][];
        for (int i = 0; i < signatures.length; i++) keys[i] = keys(signatures[i], bands, rows);
        long[] pairs = new long[16];
        int count = 0;
        for (int band = 0; band < bands; band++) {
            long[] packed = packed(keys, band);
            for (int start = 0; start < packed.length;) {
                int end = groupEnd(packed, start);
                for (int x = start; x < end; x++) {
                    for (int y = x + 1; y < end; y++) {
                        int i = (int) packed[x];
                        int j = (int) packed[y];
                        if (keys[i][band] != keys[j][band]) continue;
                        pairs = M3HashLanes.grow(pairs, count + 1);
                        pairs[count++] = ((long) Math.min(i, j) << 32) | Math.max(i, j);
                    }
                }
                start = end;
            }
        }
        return M3HashLanes.sortedDistinct(pairs, count);
    }

    /** {@code (top 32 bits of key) << 32 | index}, sorted: equal prefixes become adjacent. */
    private static long[] packed(long[][] keys, int band) {
        long[] packed = new long[keys.length];
        for (int i = 0; i < keys.length; i++) packed[i] = (keys[i][band] & 0xffffffff00000000L) | i;
        Arrays.sort(packed);
        return packed;
    }

    private static int groupEnd(long[] packed, int start) {
        long prefix = packed[start] >>> 32;
        int end = start + 1;
        while (end < packed.length && (packed[end] >>> 32) == prefix) end++;
        return end;
    }
}
