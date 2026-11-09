/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.text;

import java.util.Objects;

/**
 * Immutable double 64-bit rolling hash over an int sequence.
 *
 * <p>Arithmetic intentionally uses Java long overflow (mod 2^64). Two independent
 * odd bases reduce accidental collision probability; equality remains probabilistic.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntPrefixRollingHash.java}
 * blob {@code bcbf44b754db22de063061511b015491baeeafdb} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntPrefixRollingHash {
    public record Hash(long first, long second) {}

    private static final long BASE_A = 0x9e3779b185ebca87L;
    private static final long BASE_B = 0xc2b2ae3d27d4eb4fL;
    private static final long SALT_A = 0x165667b19e3779f9L;
    private static final long SALT_B = 0x85ebca77c2b2ae63L;

    private final int length;
    private final long[] prefixA;
    private final long[] prefixB;
    private final long[] powersA;
    private final long[] powersB;

    public IntPrefixRollingHash(int[] values) {
        Objects.requireNonNull(values, "values");
        length = values.length;
        prefixA = new long[length + 1];
        prefixB = new long[length + 1];
        powersA = new long[length + 1];
        powersB = new long[length + 1];
        powersA[0] = 1L;
        powersB[0] = 1L;
        for (int i = 0; i < length; i++) {
            long symbol = Integer.toUnsignedLong(values[i]);
            prefixA[i + 1] = prefixA[i] * BASE_A + mix(symbol ^ SALT_A);
            prefixB[i + 1] = prefixB[i] * BASE_B + mix(symbol ^ SALT_B);
            powersA[i + 1] = powersA[i] * BASE_A;
            powersB[i + 1] = powersB[i] * BASE_B;
        }
    }

    public int length() { return length; }

    public long payloadBytes() {
        return (long) (prefixA.length + prefixB.length + powersA.length + powersB.length)
                * Long.BYTES;
    }

    public long hashA(int fromInclusive, int toExclusive) {
        checkRange(fromInclusive, toExclusive);
        int count = toExclusive - fromInclusive;
        return prefixA[toExclusive] - prefixA[fromInclusive] * powersA[count];
    }

    public long hashB(int fromInclusive, int toExclusive) {
        checkRange(fromInclusive, toExclusive);
        int count = toExclusive - fromInclusive;
        return prefixB[toExclusive] - prefixB[fromInclusive] * powersB[count];
    }

    public Hash hash(int fromInclusive, int toExclusive) {
        return new Hash(
                hashA(fromInclusive, toExclusive),
                hashB(fromInclusive, toExclusive));
    }

    public boolean equalRanges(
            int leftFrom,
            int leftTo,
            int rightFrom,
            int rightTo) {
        checkRange(leftFrom, leftTo);
        checkRange(rightFrom, rightTo);
        int count = leftTo - leftFrom;
        if (count != rightTo - rightFrom) return false;
        return equalRangesUnchecked(leftFrom, rightFrom, count);
    }

    /** Longest common prefix of suffixes starting at left and right, capped by maxLength. */
    public int longestCommonPrefix(int left, int right, int maxLength) {
        if (left < 0 || right < 0 || left > length || right > length) {
            throw new IndexOutOfBoundsException(left + "," + right);
        }
        if (maxLength < 0) throw new IllegalArgumentException("maxLength < 0");
        int limit = Math.min(maxLength, Math.min(length - left, length - right));
        int lo = 0;
        int hi = limit;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (equalRangesUnchecked(left, right, mid)) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /** Both spans have been checked by their caller. No redundant validation in LCP probes. */
    private boolean equalRangesUnchecked(int leftFrom, int rightFrom, int count) {
        int leftTo = leftFrom + count;
        int rightTo = rightFrom + count;
        return prefixA[leftTo] - prefixA[leftFrom] * powersA[count]
                    == prefixA[rightTo] - prefixA[rightFrom] * powersA[count]
                && prefixB[leftTo] - prefixB[leftFrom] * powersB[count]
                    == prefixB[rightTo] - prefixB[rightFrom] * powersB[count];
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > length) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }

    private static long mix(long value) {
        long z = value;
        z ^= z >>> 30;
        z *= 0xbf58476d1ce4e5b9L;
        z ^= z >>> 27;
        z *= 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
