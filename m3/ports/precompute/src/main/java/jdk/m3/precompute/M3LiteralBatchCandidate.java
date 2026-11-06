// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Test-only compiler-precompute candidate for 1..24 exact ASCII literal needles.
 * Every first-hit position is a UTF-16 coordinate, just like String.indexOf and
 * Matcher.start(). It is NOT a regex parser, Matcher replacement, or M3JDK runtime.
 *
 * <p>Immutable bit-parallel per-needle Shift-And masks are precomputed once.
 * Java does not retain source text. The native lane takes bounded copies of
 * primitive masks and UTF-16 units, never objects, predicates or comparators.
 * No case-folding, canonical equivalence, zero-width, empty or non-ASCII literal
 * is admitted; those queries stay with java.util.regex.Pattern/Matcher.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-literal-batch-precompute-20261022/src/main/java/com/synexia/m3/mastery/M3LiteralBatchCandidate.java blob c5be09d9289a; recipe m3/ports/precompute/recipe/utf16-stitch-20261106/relocate.py v3
public final class M3LiteralBatchCandidate {
    public static final int MAX_NEEDLES = 24;
    public static final int MAX_NEEDLE_UNITS = 63;
    public static final int MAX_REGION_UNITS = 4096;
    private static final int ASCII = 128;

    private M3LiteralBatchCandidate() { }

    public static Plan prepare(List<String> needles, int regexFlags) {
        Objects.requireNonNull(needles, "needles");
        if (regexFlags != 0) {
            throw new IllegalArgumentException("only zero-flags literal probes admitted");
        }
        if (needles.isEmpty() || needles.size() > MAX_NEEDLES) {
            throw new IllegalArgumentException("invalid literal batch size");
        }
        String[] words = new String[needles.size()];
        int[] lengths = new int[words.length];
        long[] masks = new long[Math.multiplyExact(words.length, ASCII)];
        for (int n = 0; n < words.length; n++) {
            String word = Objects.requireNonNull(needles.get(n), "literal needle");
            int length = word.length();
            if (length == 0 || length > MAX_NEEDLE_UNITS) {
                throw new IllegalArgumentException("literal unit count not in [1, 63]");
            }
            for (int j = 0; j < length; j++) {
                char unit = word.charAt(j);
                if (unit >= ASCII) {
                    throw new IllegalArgumentException("non-ASCII literal needs original JDK matcher");
                }
                masks[n * ASCII + unit] |= 1L << j;
            }
            lengths[n] = length;
            words[n] = word;
        }
        return new Plan(words, lengths, masks);
    }

    public static final class Plan {
        private final String[] needles;
        private final int[] lengths;
        private final long[] masks;

        private Plan(String[] needles, int[] lengths, long[] masks) {
            this.needles = needles.clone();
            this.lengths = lengths.clone();
            this.masks = masks.clone();
        }

        public int size() { return lengths.length; }
        public List<String> literals() { return List.of(needles.clone()); }

        /** First UTF-16 occurrence of each literal wholly within [from, to). */
        public int[] firstJava(char[] input, int from, int to) {
            bounds(input, from, to);
            int[] first = new int[lengths.length];
            Arrays.fill(first, -1);
            long[] active = new long[lengths.length];
            for (int offset = from; offset < to; offset++) {
                int c = input[offset];
                for (int n = 0; n < lengths.length; n++) {
                    if (first[n] >= 0) continue;
                    long admitted = c < ASCII ? masks[n * ASCII + c] : 0L;
                    active[n] = ((active[n] << 1) | 1L) & admitted;
                    if ((active[n] & (1L << (lengths[n] - 1))) != 0L) {
                        first[n] = offset - lengths[n] + 1;
                    }
                }
            }
            return first;
        }

        /** Explicit native lane, never an automatic fallback on an unavailable JNI binary. */
        public int[] firstNative(char[] input, int from, int to) {
            bounds(input, from, to);
            if (!NativeAvailability.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_literal_batch_probe native library unavailable");
            }
            int[] found = nativeFirstPositions(input, from, to - from, masks, lengths);
            if (found == null || found.length != lengths.length) {
                throw new IllegalStateException("invalid native batch result shape");
            }
            for (int n = 0; n < found.length; n++) {
                int at = found[n];
                if (at < -1 || (at >= 0 && (at < from || at > to - lengths[n]))) {
                    throw new IllegalStateException("invalid native literal position");
                }
            }
            return found;
        }

        private static void bounds(char[] input, int from, int to) {
            Objects.requireNonNull(input, "input");
            Objects.checkFromToIndex(from, to, input.length);
            if (to - from > MAX_REGION_UNITS) {
                throw new IllegalArgumentException("literal probe region exceeds bounded budget");
            }
        }
    }

    public static boolean nativeAvailable() { return NativeAvailability.AVAILABLE; }

    private static final class NativeAvailability {
        private static final boolean AVAILABLE = load();
        private static boolean load() {
            try {
                System.loadLibrary("m3_literal_batch_probe");
                return true;
            } catch (UnsatisfiedLinkError unavailable) {
                return false;
            }
        }
    }

    /** JNI fault/refusal harness uses exact primitive shape; no public product ABI. */
    static native int[] nativeFirstPositions(char[] input, int from, int length,
            long[] asciiMasks, int[] lengths);
}
