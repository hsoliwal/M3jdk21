/*******************************************************************************
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
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * Real M3JDK21 Java21 array/index/state-mask oracle for both exact-byte and
 * inclusion/exclusion histogram algorithms. No second production owner.
 */
public final class M3StateMaskAdaptiveCountContract {
    private static long checks;
    private M3StateMaskAdaptiveCountContract() { }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static int brute(byte[] states, int[] offsets, int first, int count,
                             int stride, int required, int forbidden) {
        int matches = 0;
        int at = first;
        for (int i = 0; i < count; i++, at += stride) {
            int flags = states[offsets == null ? at : offsets[at]] & 0xff;
            if ((flags & required) == required && (flags & forbidden) == 0) matches++;
        }
        return matches;
    }

    private static void invalidMask(M3StateMaskHistogram histogram, int required, int forbidden) {
        checks++;
        try {
            histogram.countMatching(required, forbidden);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid mask admitted: " + required + "," + forbidden);
    }

    private static void exhaustive() {
        byte[] every = new byte[256];
        for (int value = 0; value < 256; value++) every[value] = (byte) value;
        var hist = M3StateMaskHistogram.aligned(every, 0, 256, 1,
                M3StateMaskHistogram.Backend.JAVA);
        check(hist.size() == 256 && hist.distinctStates() == 256, "all unsigned states");
        check(hist.retainedPrimitiveBytes() == 2 * 256 * Integer.BYTES,
                "unchanged exact and supermask primitive lanes");
        for (int required = 0; required < 256; required++) {
            for (int forbidden = 0; forbidden < 256; forbidden++) {
                int expected = brute(every, null, 0, 256, 1, required, forbidden);
                int actual = hist.countMatching(required, forbidden);
                check(expected == actual, "all-state/mask parity r=" + required + " f=" + forbidden);
                if ((required & forbidden) == 0 && forbidden != 0) {
                    int free = (~(required | forbidden)) & 255;
                    int probes = 1 << Math.min(Integer.bitCount(free),
                            Integer.bitCount(forbidden));
                    check(probes <= 16, "maximum sixteen histogram bucket probes");
                }
                // Existing source owner's aligned position selector is a
                // second independent M3 mask implementation, not a fake array.
                if (((required * 257 + forbidden) & 127) == 0) {
                    int[] selected = M3IndexMaskCompactor.selectPositions(every,
                            required, forbidden, M3IndexMaskCompactor.Backend.JAVA);
                    check(selected.length == actual, "original selected physical occurrences");
                    for (int ordinal : selected) {
                        int flags = every[ordinal] & 255;
                        check((flags & required) == required && (flags & forbidden) == 0,
                                "selected occurrence satisfies original mask");
                    }
                }
            }
        }
        int[] copy = hist.exactCounts();
        copy[255] = -999;
        check(hist.countExactly(255) == 1, "original exact count remains private");
        check(hist.countMatching(0, 255) == 1, "all forbidden bits is exact[0]");
        check(hist.countMatching(255, 0) == 1, "all required bits already fast");
        check(hist.countMatching(0, 0) == 256, "identity state count");
        check(hist.countMatching(127, 128) == 1, "no-free-bit exact leaf");
        invalidMask(hist, 256, 0);
        invalidMask(hist, 0, -1);
        invalidMask(hist, -1, 255);
    }

    private static void stridedAndDuplicated() {
        Random random = new Random(0x2130_1021L);
        for (int iteration = 0; iteration < 12; iteration++) {
            byte[] states = new byte[512];
            for (int i = 0; i < states.length; i++) states[i] = (byte) random.nextInt(256);
            int[] positions = new int[256];
            int[] rowIds = new int[positions.length];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = random.nextInt(states.length);
                rowIds[i] = random.nextInt(23) - 11;
            }
            boolean reverse = (iteration & 1) != 0;
            int first = reverse ? positions.length - 1 : 0;
            int stride = reverse ? -1 : 1;
            var original = M3StateMaskHistogram.indexed(positions, states,
                    first, positions.length, stride, M3StateMaskHistogram.Backend.JAVA);
            check(original.size() == positions.length, "capture original duplicate physical offsets");
            for (int attempt = 0; attempt < 1024; attempt++) {
                int required = random.nextInt(256), forbidden = random.nextInt(256);
                int expected = brute(states, positions, first, positions.length, stride,
                        required, forbidden);
                int actual = original.countMatching(required, forbidden);
                check(actual == expected, "repeat/signed/strided indexed histogram parity");
                if ((attempt & 31) == 0) {
                    int[] selected = M3IndexPathMasks.select(rowIds, positions,
                            new int[][] {asWords(states)}, new int[] {required},
                            new int[] {forbidden}, first, positions.length, stride,
                            M3IndexPathMasks.Backend.JAVA);
                    check(selected.length == actual, "original 32-bit path mask selects same occurrences");
                }
            }
            if (M3StateMaskHistogram.nativeAvailable()) {
                var nativeHistogram = M3StateMaskHistogram.indexed(positions, states,
                        first, positions.length, stride,
                        M3StateMaskHistogram.Backend.NATIVE_REQUIRED);
                for (int required = 0; required < 256; required += 7) {
                    int forbidden = (required * 11) & 255;
                    check(nativeHistogram.countMatching(required, forbidden)
                                    == original.countMatching(required, forbidden),
                            "actual original JNI 256-bucket state tally parity");
                }
            } else {
                checks++;
                try {
                    M3StateMaskHistogram.indexed(positions, states, first, positions.length,
                            stride, M3StateMaskHistogram.Backend.NATIVE_REQUIRED);
                    throw new AssertionError("native required silently fell back to Java");
                } catch (UnsatisfiedLinkError expected) {
                    // The original explicit backend refusal must remain in place.
                }
            }
        }
    }

    private static int[] asWords(byte[] bytes) {
        int[] words = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) words[i] = bytes[i] & 255;
        return words;
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        exhaustive();
        stridedAndDuplicated();
        byte[] empty = new byte[0];
        var none = M3StateMaskHistogram.aligned(empty, 0, 0, 1,
                M3StateMaskHistogram.Backend.JAVA);
        check(none.countMatching(0, 0) == 0
                && none.countMatching(0, 255) == 0, "empty histogram");
        System.out.println("M3_STATEHIST_ADAPTIVE_PASS checks=" + checks);
    }
}
