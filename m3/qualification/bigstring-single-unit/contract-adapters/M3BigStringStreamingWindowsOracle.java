/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3BigString;
import java.io.*;import java.util.*;
/** Pinned streaming test's rope-window subset only; filesystem/shard suite is not invoked. */
public final class M3BigStringStreamingWindowsOracle {
    private static long checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    /** Accepts only the ranged append, and only a non-empty window inside the chunk. */
    private static final class Windows implements Appendable {
        private final StringBuilder text = new StringBuilder();
        private int calls;

        @Override
        public Appendable append(CharSequence chunk, int start, int end) {
            calls++;
            check(0 <= start && start < end && end <= chunk.length(), "non-empty chunk window");
            text.append(chunk, start, end);
            return this;
        }

        @Override
        public Appendable append(CharSequence chunk) {
            throw new AssertionError("unranged append");
        }

        @Override
        public Appendable append(char unit) {
            throw new AssertionError("single-unit append");
        }
    }

    private static void ropeWindows() throws IOException {
        M3BigString base = M3BigString.of("ab", "", "cde", "f", "😀g", "hij");
        String text = base.flatten();
        M3BigString sliced = M3BigString.concat(base.subSequence(1, 6), base.subSequence(7, 10));
        String slicedText = text.substring(1, 6) + text.substring(7, 10);
        M3BigString nested = M3BigString.concat(sliced.subSequence(1, 7), base.subSequence(0, 2));
        String nestedText = slicedText.substring(1, 7) + text.substring(0, 2);
        List<M3BigString> ropes = List.of(base, sliced, nested);
        List<String> oracles = List.of(text, slicedText, nestedText);
        for (int r = 0; r < ropes.size(); r++) {
            M3BigString rope = ropes.get(r);
            String oracle = oracles.get(r);
            check(rope.flatten().equals(oracle), "rope oracle " + r);
            for (int from = 0; from <= oracle.length(); from++) {
                for (int to = from; to <= oracle.length(); to++) {
                    M3BigString range = rope.subSequence(from, to);
                    Windows windows = new Windows();
                    range.appendTo(windows);
                    String expected = oracle.substring(from, to);
                    check(
                            windows.text.toString().equals(expected),
                            "appendTo " + r + ":" + from + "," + to);
                    check(
                            (windows.calls == 0) == expected.isEmpty(),
                            "empty ranges append nothing");
                    check(windows.calls <= expected.length(), "at most one window per unit");
                    StringWriter writer = new StringWriter();
                    range.appendTo(writer);
                    check(writer.toString().equals(expected), "Writer streaming " + r);
                }
            }
        }
    }


    public static void main(String[] args)throws Exception{ropeWindows();System.out.println("M3_BIG_STRING_STREAM_WINDOWS_SUBSET_PASS checks="+checks);}
}
