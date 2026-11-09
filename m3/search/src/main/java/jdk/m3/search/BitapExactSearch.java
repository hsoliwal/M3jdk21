// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/BitapExactSearch.java (blob 86c9d2609574) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Candidate-only exact Shift-And byte search for patterns of one through 63 bytes.
 *
 * <p>Package-private and deliberately absent from adaptive dispatch until the existing
 * KMP/Horspool contracts, cold preparation, retained heap and JNI gates qualify it.
 * The pattern is snapshotted once; no caller array or buffer is retained.</p>
 */
final class BitapExactSearch implements CompiledSearch {
    static final int MAX_PATTERN_LENGTH = 63;

    private final byte[] pattern;
    private final long[] masks = new long[256];
    private final long acceptingBit;

    BitapExactSearch(byte[] needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0 || needle.length > MAX_PATTERN_LENGTH) {
            throw new IllegalArgumentException("needle length must be between 1 and 63");
        }
        pattern = Arrays.copyOf(needle, needle.length);
        for (int i = 0; i < pattern.length; i++) {
            masks[Byte.toUnsignedInt(pattern[i])] |= 1L << i;
        }
        acceptingBit = 1L << (pattern.length - 1);
    }

    @Override
    public int indexOf(byte[] haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int start = Math.max(0, fromIndex);
        if (start > haystack.length - pattern.length) return -1;
        long state = 0L;
        for (int i = start; i < haystack.length; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack[i])];
            if ((state & acceptingBit) != 0L) return i - pattern.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int maximum = haystack.length - pattern.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int end = Math.min(fromIndex, maximum) + pattern.length;
        long state = 0L;
        int last = -1;
        for (int i = 0; i < end; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack[i])];
            if ((state & acceptingBit) != 0L) last = i - pattern.length + 1;
        }
        return last;
    }

    /**
     * Count all overlapping occurrences from a zero-clamped byte offset.
     * Reuse this compiled Shift-And mask table: no repeated indexOf, per-hit callback,
     * hit positions or mutable retained state. This candidate does not change dispatch.
     */
    int countOverlapping(byte[] haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int start = Math.max(0, fromIndex);
        if (start > haystack.length - pattern.length) return 0;
        long state = 0L;
        int count = 0;
        for (int i = start; i < haystack.length; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack[i])];
            if ((state & acceptingBit) != 0L) count++;
        }
        return count;
    }

    @Override
    public int indexOf(ByteBuffer haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int start = Math.max(0, fromIndex);
        if (start > length - pattern.length) return -1;
        long state = 0L;
        for (int i = start; i < length; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack.get(base + i))];
            if ((state & acceptingBit) != 0L) return i - pattern.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int maximum = length - pattern.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int end = Math.min(fromIndex, maximum) + pattern.length;
        long state = 0L;
        int last = -1;
        for (int i = 0; i < end; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack.get(base + i))];
            if ((state & acceptingBit) != 0L) last = i - pattern.length + 1;
        }
        return last;
    }

    /** Logical remaining-slice count; absolute reads leave position and limit unchanged. */
    int countOverlapping(ByteBuffer haystack, int fromIndex) {
        Objects.requireNonNull(haystack, "haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int start = Math.max(0, fromIndex);
        if (start > length - pattern.length) return 0;
        long state = 0L;
        int count = 0;
        for (int i = start; i < length; i++) {
            state = ((state << 1) | 1L) & masks[Byte.toUnsignedInt(haystack.get(base + i))];
            if ((state & acceptingBit) != 0L) count++;
        }
        return count;
    }

    @Override
    public int needleLength() { return pattern.length; }

    @Override
    public String strategy() { return "BITAP_EXACT_CANDIDATE"; }

    /**
     * Explicit probe-only JNI symbols. No automatic loading, dispatch or retained native
     * handles; the C17 sidecar is exercised by the separate native parity harness.
     */
    static native int indexOfNative(byte[] needle, byte[] haystack, int fromIndex);
    static native int lastIndexOfNative(byte[] needle, byte[] haystack, int fromIndex);
    /** Explicit optional probe: never replaces the existing JNI occurrence provider. */
    static native int countOverlappingNative(byte[] needle, byte[] haystack, int fromIndex);
}
