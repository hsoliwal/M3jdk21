// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded, no-payload-copy streaming composition of the original sparse UTF-16
 * DFA's Java/JNI window operations. No second automaton, matcher or JNI symbol.
 *
 * <p>Each window overlaps its predecessor by the maximum literal length minus
 * one UTF-16 code unit, so a match crossing a physical window boundary is seen
 * in the next scan. A single result selects the earliest occurrence per literal.
 * All Java String contracts here are UTF-16 substring/indexOf, not general
 * Pattern/Matcher behavior for surrogate halves, groups, flags or regions.
 * The input and immutable plan must remain unchanged while a call executes.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-literal-batch-precompute-20261022/src/main/java/com/synexia/m3/mastery/M3SparseUtf16Stitched.java blob 8b7b57679739; recipe m3/ports/precompute/recipe/utf16-stitch-20261106/relocate.py v3
final class SparseUtf16Stitched {
    private static final int OVERLAP = LiteralBatchCandidate.MAX_NEEDLE_UNITS - 1;
    private static final int STRIDE = LiteralBatchCandidate.MAX_REGION_UNITS - OVERLAP;
    private static final int BATCH_SIZE = 64;
    private static final int MAX_WINDOWS = 4096;

    private SparseUtf16Stitched() { }

    /** Non-JNI baseline, no copied String payloads or retained entry objects. */
    static int[] firstJava(SparseUtf16LiteralPlan plan, char[] text, int from, int to) {
        return scan(plan, text, from, to, false);
    }

    /** Explicit JNI: no Java fallback when the original native binary is absent. */
    static int[] firstNative(SparseUtf16LiteralPlan plan, char[] text, int from, int to) {
        return scan(plan, text, from, to, true);
    }

    /** The number of physical bounded windows, calculated without overflowing int. */
    static int windowCount(int length) {
        if (length < 0 || (long) length > (long) MAX_WINDOWS * STRIDE) {
            throw new IllegalArgumentException("UTF-16 stitched scan exceeds window budget");
        }
        return length == 0 ? 1 : (int) (((long) length + STRIDE - 1) / STRIDE);
    }

    private static int[] scan(SparseUtf16LiteralPlan plan, char[] text,
            int from, int to, boolean requireNative) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(text, "text");
        Objects.checkFromToIndex(from, to, text.length);
        int windows = windowCount(to - from);
        int patterns = plan.patternCount();
        int[] earliest = new int[patterns];
        Arrays.fill(earliest, -1);
        // Source/index lane fanout is bounded to original first*Batch capacity.
        for (int begin = 0; begin < windows; begin += BATCH_SIZE) {
            int count = Math.min(BATCH_SIZE, windows - begin);
            int[] starts = new int[count];
            int[] ends = new int[count];
            for (int local = 0; local < count; local++) {
                int window = begin + local;
                int cursor = (int) ((long) from + (long) window * STRIDE);
                starts[local] = window == 0 ? from : cursor - OVERLAP;
                ends[local] = (int) Math.min((long) to, (long) cursor + STRIDE);
            }
            int[] candidates = requireNative
                    ? plan.firstNativeBatch(text, starts, ends)
                    : plan.firstJavaBatch(text, starts, ends);
            if (candidates.length != count * patterns) {
                throw new IllegalStateException("incomplete stitched DFA candidate batch");
            }
            for (int local = 0; local < count; local++) {
                int base = local * patterns;
                for (int p = 0; p < patterns; p++) {
                    int at = candidates[base + p];
                    if (at >= 0 && (earliest[p] == -1 || at < earliest[p])) {
                        earliest[p] = at;
                    }
                }
            }
            int nextWindow = begin + count;
            if (nextWindow < windows) {
                // No future window can report a start before its own physical
                // beginning. A result at/before that lower bound is final.
                long nextStart = (long) from + (long) nextWindow * STRIDE - OVERLAP;
                if (settledBefore(earliest, nextStart)) break;
            }
        }
        return earliest;
    }

    /** Pure lower-bound proof, including missing and not-yet-final occurrences. */
    static boolean settledBefore(int[] earliest, long nextStart) {
        for (int first : earliest) {
            if (first < 0 || (long) first > nextStart) return false;
        }
        return true;
    }
}
