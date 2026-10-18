// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

/** Dependency-free differential oracle; also invoked by M3TokenRegexTest. */
public final class M3TokenRegexSelfTest {
    private M3TokenRegexSelfTest() {}

    public static void main(String[] args) {
        geometryAndSelectors();
        grammarAndOffsets();
        randomizedJavaRegexDifferential();
        invalidGrammarAndCaptureRefusal();
        largeTape();
        System.out.println("M3TokenRegexSelfTest PASS");
    }

    private static void geometryAndSelectors() {
        int[] ids = {7, 8, 8, 9};
        int[] kinds = {2, 1, 4, 16};
        long[] starts = {0, 3, 100, 3_000_000_000L};
        long[] ends = {2, 4, 105, 3_000_000_001L};
        M3TokenTape tape = M3TokenTape.capture(ids, kinds, starts, ends);
        ids[0] = 99;
        kinds[1] = 0;
        starts[0] = 19;
        check(tape.idAt(0) == 7 && tape.kindMaskAt(1) == 1 && tape.startUtf16At(0) == 0,
                "defensive array ownership");
        M3TokenTape slice = tape.slice(1, 3);
        check(slice.idAt(0) == 8 && slice.startUtf16At(0) == 3
                && slice.endUtf16At(1) == 105, "offset-only slice");
        check(Arrays.equals(slice.idsCopy(), new int[] {8, 8}), "detached ID array copy");
        check(M3TokenRegex.compile("#8").find(slice).equals(
                new M3TokenRegex.Match(0, 1, 3, 4)), "first token ID span");
        check(M3TokenRegex.compile("[id=8,all=1,none=0x0c]").find(slice)
                .equals(new M3TokenRegex.Match(0, 1, 3, 4)), "combined selector");
        check(M3TokenRegex.compile("[id=8,all=4]").find(slice)
                .equals(new M3TokenRegex.Match(1, 2, 100, 105)), "kind selects occurrence");
        check(M3TokenRegex.compile("%16").find(tape)
                .equals(new M3TokenRegex.Match(3, 4, 3_000_000_000L, 3_000_000_001L)),
                "long UTF-16 coordinate");
        check(M3TokenRegex.compile("#8 [id=8,none=4]").find(tape) == null,
                "explicit token mask does not bypass adjacent mismatches");
        expect(IllegalArgumentException.class,
                () -> M3TokenTape.capture(new int[] {1, 2}, new int[] {0, 0},
                        new long[] {2, 1}, new long[] {3, 4}));
        expect(IndexOutOfBoundsException.class, () -> tape.idAt(4));
    }

    private static void grammarAndOffsets() {
        M3TokenTape tape = make(new int[] {99, 1, 2, 3, 3, 4, 1, 2});
        M3TokenRegex regex = M3TokenRegex.compile("#1 (#2|#3)+");
        check(regex.find(tape).equals(new M3TokenRegex.Match(1, 5, 10, 50)),
                "alternation and plus leftmost-longest");
        check(M3TokenRegex.compile("^#99 #1 (#2|#3)* #4 #1 #2$").matches(tape),
                "anchors and exact full tape");
        check(M3TokenRegex.compile("#1 #2$").find(tape)
                .equals(new M3TokenRegex.Match(6, 8, 60, 80)), "end anchor");
        check(M3TokenRegex.compile("^#1").find(tape) == null, "start anchor");
        check(M3TokenRegex.compile("#9*").find(tape)
                .equals(new M3TokenRegex.Match(0, 0, 0, 0)), "zero-width empty match");
        check(M3TokenRegex.compile("#1? #99").find(tape)
                .equals(new M3TokenRegex.Match(0, 1, 0, 10)), "optional token");
        check(M3TokenRegex.compile("#3+ #4").find(tape)
                .equals(new M3TokenRegex.Match(3, 6, 30, 60)), "repetition and matching range");
        check(!M3TokenRegex.compile("#1 #2").matches(tape), "matches differs from find");
        check(M3TokenRegex.compile("#1 #2").find(tape)
                .equals(new M3TokenRegex.Match(1, 3, 10, 30)), "find substring");
        check(M3TokenRegex.compile(".").compiledPositions() == 2, "one atom plus accept");
        check(M3TokenRegex.compile("#1").primitiveBytes() > 0, "flat payload footprint");
    }

    private static void randomizedJavaRegexDifferential() {
        String[] m3 = {
                "#1 (#2|#3)* #3", "#1? #2+", "(#1|#2)* #3",
                "#1 . #3", "(#1 #2|#3)+", "#1* #2? #3+", "(#1|#2|#3)*"};
        String[] oracle = {
                "a(b|c)*c", "a?b+", "(a|b)*c", "a.c", "(ab|c)+",
                "a*b?c+", "(a|b|c)*"};
        Random random = new Random(0x4d334d33544f4b45L);
        for (int p = 0; p < m3.length; p++) {
            M3TokenRegex compiled = M3TokenRegex.compile(m3[p]);
            Pattern jdk = Pattern.compile(oracle[p]);
            for (int sample = 0; sample < 3500; sample++) {
                int length = random.nextInt(10);
                int[] ids = new int[length];
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    ids[i] = random.nextInt(3) + 1;
                    text.append((char) ('a' + ids[i] - 1));
                }
                M3TokenTape tape = make(ids);
                check(compiled.matches(tape) == jdk.matcher(text).matches(),
                        "full-match differential pattern=" + p + " input=" + text);
                M3TokenRegex.Match found = compiled.find(tape);
                int first = -1, last = -1;
                for (int begin = 0; begin <= length; begin++) {
                    for (int end = length; end >= begin; end--) {
                        if (jdk.matcher(text.substring(begin, end)).matches()) {
                            first = begin;
                            last = end;
                            break;
                        }
                    }
                    if (first >= 0) break;
                }
                check((found == null) == (first < 0), "find oracle presence");
                if (found != null) check(found.firstToken() == first && found.endToken() == last,
                        "leftmost-longest differential pattern=" + p + " input=" + text);
            }
        }
    }

    private static void invalidGrammarAndCaptureRefusal() {
        for (String pattern : new String[] {"#", "!#1", "(?<name>#1)", "#1{2}",
                "#1**", "#1|#2$#3", "#1)", "(#1", "[id=1,unknown=2]", "[]",
                "[id=1,all=bad]", "#4294967295", "#1$|#2", "\\w"}) {
            expect(IllegalArgumentException.class, () -> M3TokenRegex.compile(pattern));
        }
    }

    private static void largeTape() {
        final int length = 150_000;
        int[] ids = new int[length];
        int[] masks = new int[length];
        long[] starts = new long[length], ends = new long[length];
        Arrays.fill(ids, 1);
        for (int i = 0; i < length; i++) { starts[i] = (long) i * 2; ends[i] = starts[i] + 1; }
        ids[length - 1] = 2;
        M3TokenTape tape = M3TokenTape.capture(ids, masks, starts, ends);
        check(M3TokenRegex.compile("^#1* #2$").matches(tape), "long full token scan");
        check(M3TokenRegex.compile("#2").find(tape).firstToken() == length - 1,
                "last token match without per-token objects");
    }

    private static M3TokenTape make(int[] ids) {
        int[] kinds = new int[ids.length];
        long[] starts = new long[ids.length], ends = new long[ids.length];
        for (int i = 0; i < ids.length; i++) { starts[i] = i * 10L; ends[i] = starts[i] + 10; }
        return M3TokenTape.capture(ids, kinds, starts, ends);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable runnable) {
        try { runnable.run(); } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("unexpected failure", error);
        }
        throw new AssertionError("missing expected " + expected.getSimpleName());
    }
}
