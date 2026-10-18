// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Executable Java 21 differential and hostile-input oracle (no JUnit dependency). */
public final class ByteSlidingWindowsChecks {
    private ByteSlidingWindowsChecks() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static List<String> reference(SourceArtifact.Span span, int width, int step) {
        List<String> result = new ArrayList<>();
        for (int from = 0; from + width <= span.length(); from += step) {
            int[] counts = new int[256];
            for (int i = from; i < from + width; i++) {
                counts[Byte.toUnsignedInt(span.byteAt(i))]++;
            }
            int distinct = 0, modal = -1, high = 0;
            for (int v = 0; v < counts.length; v++) {
                if (counts[v] != 0) distinct++;
                if (counts[v] > high) { high = counts[v]; modal = v; }
            }
            result.add(from + ":" + (from + width) + ":" + distinct + ":" + modal);
        }
        return result;
    }

    private static void refuse(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected bounded-window refusal");
        } catch (IllegalArgumentException expected) {
            // Caller-controlled maximum refuses before output.
        }
    }

    public static void main(String[] args) {
        Random random = new Random(18473L);
        int checks = 0;
        for (int len = 0; len < 130; len++) {
            byte[] bytes = new byte[len];
            random.nextBytes(bytes);
            SourceArtifact source = SourceArtifact.copyOf("window-" + len, bytes);
            SourceArtifact.Span whole = source.whole();
            for (int width = 1; width <= 17; width++) {
                for (int step : new int[]{1, 2, 3, 8, 19, 137}) {
                    List<String> expected = reference(whole, width, step);
                    List<String> actual = new ArrayList<>();
                    int count = ByteSlidingWindows.scan(whole, width, step,
                            expected.size(), (from, to, distinct, mode) ->
                                actual.add(from + ":" + to + ":" + distinct + ":" + mode));
                    check(count == expected.size() && actual.equals(expected),
                            "differential: length=" + len + " width=" + width + " step=" + step);
                    if (!expected.isEmpty()) {
                        List<String> rejected = new ArrayList<>();
                        refuse(() -> ByteSlidingWindows.scan(whole, width, step,
                                expected.size() - 1, (a,b,c,d) -> rejected.add("unexpected")));
                        check(rejected.isEmpty(), "validate output budget before callbacks");
                    }
                    checks++;
                }
            }
        }
        SourceArtifact empty = SourceArtifact.copyOf("empty", new byte[0]);
        check(ByteSlidingWindows.scan(empty.whole(), 1, 1, 0,
                (a,b,c,d) -> { throw new AssertionError("empty callback"); }) == 0, "empty source");
        refuse(() -> ByteSlidingWindows.scan(empty.whole(), 0, 1, 1, (a,b,c,d) -> { }));
        refuse(() -> ByteSlidingWindows.scan(empty.whole(), 1, 0, 1, (a,b,c,d) -> { }));
        refuse(() -> ByteSlidingWindows.scan(empty.whole(), 1, 1,
                ByteSlidingWindows.MAX_WINDOWS + 1, (a,b,c,d) -> { }));
        System.out.println("M3_CPU_BYTE_SLIDING_WINDOWS_PASS differential_cases=" + checks);
    }
}
