// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

/*
 * @test
 * @summary M3ChunkCursor contract (pattern donor helix-editor/regex-cursor fd4c51c7, MIT OR Apache-2.0, pattern only):
 *          every chunk boundary, forward/backward walks against the flat oracle, surrogate pairs split across chunks,
 *          and M3RunTable forward/backward runs plus per-chunk transfer composition against java.util.regex.Pattern.
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xmx128m M3ChunkCursorTest
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3ChunkCursor;
import jdk.internal.mindex.M3RegExpCompiler;
import jdk.internal.mindex.M3RunTable;

/**
 * The chunk adapter below is the analogue of regex-cursor's test_rope.rs (an adapter over a chunked text for the test
 * only). The production implementor is java.lang.M3String.CodeUnitCursor, extended in the next leaf.
 */
public final class M3ChunkCursorTest {
    private static final String EMOJI = new String(Character.toChars(0x1F600));
    private static final String HIGH = String.valueOf((char) 0xD83D);
    private static final String LOW = String.valueOf((char) 0xDE00);
    private static final List<String> TEXTS = List.of("", "a", "ab", "abc,def", EMOJI, "a" + EMOJI + "b",
            EMOJI + EMOJI, "x" + EMOJI + "y" + EMOJI + "z", "\u00e9t\u00e9 " + EMOJI, "lone" + HIGH + "high", "lone" + LOW + "low",
            LOW + "start", "end" + HIGH);
    private static final List<String> PATTERNS = List.of(".", "..", "...", "a.b", ".*", "a.*b", "[^a]+",
            "\\x{1F600}", "\\x{1F600}+", "[\\x{1F600}-\\x{1F64F}]*", "x.y.z", "(?:" + EMOJI + "|a)+", "\\w+",
            "abc,def", "[a-z]+,[a-z]+", "\\D*", "\\S+ \\S+");
    private static final Map<String, M3RegExpCompiler.Compiled> COMPILED = new HashMap<>();
    private static long checks;

    public static void main(String[] args) {
        for (String text : TEXTS) {
            for (int[] cuts : partitions(text.length())) {
                walk(text, cuts);
                for (String regex : PATTERNS) {
                    run(regex, text, cuts);
                }
            }
        }
        System.out.println("checks=" + checks);
        require(checks > 0, "no checks");
        System.out.println("PASS M3ChunkCursorTest");
    }

    /** Every single cut and every pair of cuts (chunks never empty unless the text is empty). */
    private static List<int[]> partitions(int n) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[] {0, n});
        for (int a = 1; a < n; a++) {
            out.add(new int[] {0, a, n});
            for (int b = a + 1; b < n; b++) {
                out.add(new int[] {0, a, b, n});
            }
        }
        return out;
    }

    private static void walk(String text, int[] cuts) {
        Chunks c = new Chunks(text, cuts);
        StringBuilder forward = new StringBuilder();
        long expectedOffset = 0;
        do {
            require(c.chunkOffset() == expectedOffset, "offset");
            require(c.chunkLength() > 0 || text.isEmpty(), "empty chunk");
            for (int i = 0; i < c.chunkLength(); i++) {
                forward.append(c.unit(i));
            }
            expectedOffset += c.chunkLength();
        } while (c.advance());
        int lastLength = c.chunkLength();
        require(!c.advance() && c.chunkLength() == lastLength, "advance at end moved");
        require(forward.toString().equals(text), "forward walk");
        StringBuilder backward = new StringBuilder();
        do {
            for (int i = c.chunkLength() - 1; i >= 0; i--) {
                backward.append(c.unit(i));
            }
        } while (c.backtrack());
        require(c.chunkOffset() == 0 && !c.backtrack() && c.chunkOffset() == 0, "backtrack at start moved");
        require(backward.length() == text.length(), "backward walk length");
        for (int k = 0; k < text.length(); k++) {
            // unit order, not StringBuilder.reverse(), which re-pairs surrogates
            require(backward.charAt(k) == text.charAt(text.length() - 1 - k), "backward walk");
        }
        require(c.totalLength() == text.length(), "total length");
        checks += 3;
    }

    private static void run(String regex, String text, int[] cuts) {
        M3RegExpCompiler.Compiled compiled = COMPILED.computeIfAbsent(regex, r -> M3RegExpCompiler.compile(r, 0));
        require(compiled.admitted(), "not admitted: " + regex + " " + compiled.refusal());
        boolean expected = Pattern.matches(regex, text);
        boolean wellFormed = wellFormed(text);
        M3RunTable table = compiled.matchTable();
        int flat = table.matches(text);
        int forward = table.matches(new Chunks(text, cuts));
        verdict(regex, text, "flat", wellFormed, expected, flat);
        verdict(regex, text, "forward", wellFormed, expected, forward);
        M3RunTable reverse = compiled.reverseTable();
        if (reverse != null) {
            Chunks c = new Chunks(text, cuts);
            while (c.advance()) {
                // position at the last chunk
            }
            verdict(regex, text, "backward", wellFormed, expected, reverse.matchesBackward(c));
        }
        Chunks c = new Chunks(text, cuts);
        M3RunTable.Element e = table.transfer(c);
        while (c.advance()) {
            e = table.compose(e, table.transfer(c));
        }
        verdict(regex, text, "composed", wellFormed, expected, table.verdict(e));
    }

    private static void verdict(String regex, String text, String lane, boolean wellFormed, boolean expected,
            int candidate) {
        checks++;
        if (!wellFormed) {
            require(candidate == M3RunTable.UNSUPPORTED, lane + " must refuse unpaired surrogates: /" + regex + "/");
            return;
        }
        require(candidate != M3RunTable.UNSUPPORTED, lane + " refused well-formed text: /" + regex + "/");
        require(expected == (candidate == M3RunTable.TRUE),
                lane + " /" + regex + "/ java=" + expected + " m3=" + candidate + " text=" + text.codePoints().count());
    }

    private static boolean wellFormed(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                return false;
            }
        }
        return true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Test-only chunk adapter over a String split at the given cuts (cut[0] = 0, cut[last] = length). */
    private static final class Chunks implements M3ChunkCursor {
        private final String text;
        private final int[] cuts;
        private int chunk;

        Chunks(String text, int[] cuts) {
            this.text = text;
            this.cuts = cuts;
        }

        @Override public int chunkLength() { return cuts[chunk + 1] - cuts[chunk]; }

        @Override public char unit(int index) {
            if (index < 0 || index >= chunkLength()) {
                throw new IndexOutOfBoundsException(index);
            }
            return text.charAt(cuts[chunk] + index);
        }

        @Override public long chunkOffset() { return cuts[chunk]; }

        @Override public long totalLength() { return text.length(); }

        @Override public boolean advance() {
            if (chunk + 2 >= cuts.length) {
                return false;
            }
            chunk++;
            return true;
        }

        @Override public boolean backtrack() {
            if (chunk == 0) {
                return false;
            }
            chunk--;
            return true;
        }

        @Override public boolean surrogateAware() {
            for (int i = 1; i + 1 < cuts.length; i++) {
                if (Character.isHighSurrogate(text.charAt(cuts[i] - 1))) {
                    return false;
                }
            }
            return true;
        }
    }
}
