/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.indexstring.MIndexPrefixZ at
 * 73978088621dc70f5021befafa443f7e622b517e (Apache-2.0).
 * See m3/docs/name-mapping.json and m3/migration/docs/RESUME.md.
 */
package com.m3.algorithm;

import com.m3.text.M3StringPiece;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Exact UTF-16 prefix facts projected from an existing immutable M3 view.
 * This class is not a text owner, interner, regex engine or String replacement.
 * It does not flatten, copy, cache or retain the input's character payload.
 *
 * <p>The Z-box algorithm performs O(n) character comparisons and uses one
 * retained int[n] lane. Reads delegate to the existing owner; with its flat
 * directory of s segments, the worst-case time is O(n log(s + 1)).
 * The budget counts the int lane's payload only: 4*n bytes. It does NOT
 * count JVM headers/alignment, this result object, or existing input storage.
 * No claims of throughput improvement or allocation-free execution are made.
 *
 * <p>Code units, including NUL and unpaired or cross-segment surrogate halves,
 * are compared without normalization. All fields are immutable; there are no
 * global caches. The input must be an existing sealed M3StringPiece value.
 */
public final class M3PrefixZ {
    private final int[] prefixes;
    private final long similaritySum;
    private final int longestProperBorderLength;

    private M3PrefixZ(int[] prefixes, long similaritySum, int border) {
        this.prefixes = prefixes;
        this.similaritySum = similaritySum;
        this.longestProperBorderLength = border;
    }

    /** Uses an unbounded primitive-payload budget; interruption still cancels. */
    public static M3PrefixZ analyze(M3StringPiece value) {
        return analyze(value, Long.MAX_VALUE, () -> false);
    }

    /**
     * Fail before allocating the fact lane when its primitive payload exceeds
     * the budget. Cancellation is checked before allocation, after work, and
     * at most every 1024 comparisons/outer iterations, including inside a long
     * equal-prefix scan. Callback runtime exceptions propagate unchanged.
     * Interruption causes CancellationException without clearing interrupt status.
     */
    public static M3PrefixZ analyze(M3StringPiece value, long maxMetadataPayloadBytes,
                                    BooleanSupplier canceled) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(canceled, "canceled");
        if (maxMetadataPayloadBytes < 0) throw new IllegalArgumentException("negative metadata budget");
        int n = value.length();
        if (requiredMetadataPayloadBytes(n) > maxMetadataPayloadBytes) {
            throw new IllegalArgumentException("prefix fact payload exceeds metadata budget");
        }
        checkCanceled(canceled);
        int[] z = new int[n];
        if (n == 0) return new M3PrefixZ(z, 0, 0);
        z[0] = n;
        long sum = n;
        int border = 0;
        int left = 0;
        int right = -1;
        int comparisons = 0;
        for (int i = 1; i < n; i++) {
            if ((i & 1023) == 0) checkCanceled(canceled);
            if (i <= right) z[i] = Math.min(right - i + 1, z[i - left]);
            while (z[i] < n - i) {
                if ((comparisons++ & 1023) == 0) checkCanceled(canceled);
                if (value.charAt(z[i]) != value.charAt(i + z[i])) break;
                z[i]++;
            }
            if (z[i] > 0 && i + z[i] - 1 > right) {
                left = i;
                right = i + z[i] - 1;
            }
            if (z[i] == n - i) border = Math.max(border, z[i]);
            sum += z[i];
        }
        checkCanceled(canceled);
        return new M3PrefixZ(z, sum, border);
    }

    private static void checkCanceled(BooleanSupplier canceled) {
        if (Thread.currentThread().isInterrupted() || canceled.getAsBoolean()) {
            throw new CancellationException("M3 prefix analysis canceled");
        }
    }

    /** Primitive array payload, excluding JVM object headers and alignment. */
    public static long requiredMetadataPayloadBytes(int codeUnits) {
        if (codeUnits < 0) throw new IllegalArgumentException("negative length");
        return Integer.BYTES * (long) codeUnits;
    }

    public int length() { return prefixes.length; }

    /** Includes the full self-suffix at zero. Empty input has no valid offset. */
    public int prefixLengthAt(int offset) {
        return prefixes[Objects.checkIndex(offset, prefixes.length)];
    }

    /** The sum includes the self-suffix and may exceed Integer.MAX_VALUE. */
    public long similaritySum() { return similaritySum; }

    /** The whole value is excluded. Zero denotes no nonempty proper border. */
    public int longestProperBorderLength() { return longestProperBorderLength; }

    public long metadataPayloadBytes() { return requiredMetadataPayloadBytes(prefixes.length); }
}
