// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/** Independent whole-text test oracle; production never retains the fixture text. */
public final class M3GrammarContract {
    private static final Pattern BREAKS = Pattern.compile("\\r\\n|[\\r\\n\\x{0085}\\x{2028}\\x{2029}]");
    private static int checks;
    private M3GrammarContract() { }

    public static void main(String[] args) {
        String[] fixtures = {"", "\r\n", "a", "a\n", "a\r\nb", "a\r\n\r\nb",
                "  a \n b \n\t\n  c", "a\u2029b", "a\u2028b", "a\u0085b",
                "\u00a0\n\u2003\r\nx\tq", "\ud83d\ude42\r\n\r\nx\ud800", "a\r\nb\n\nc"};
        for (String text : fixtures) {
            for (int chunk = 1; chunk <= text.length() + 2; chunk++) verify(text, feed(text, chunk));
            if (text.length() <= 12) {
                for (int cuts = 0; cuts < (1 << Math.max(0, text.length() - 1)); cuts++) {
                    verify(text, partition(text, cuts));
                }
            }
        }
        Random random = new Random(0x3a77bL);
        String alphabet = "ab\r\n \t\u0085\u2028\u2029\u00a0\u2003\u03bb\ud83d\ude42";
        for (int trial = 0; trial < 600; trial++) {
            StringBuilder text = new StringBuilder();
            for (int n = random.nextInt(250); n > 0; n--) text.append(alphabet.charAt(random.nextInt(alphabet.length())));
            for (int chunk : new int[]{1, 2, 3, 7, 31, 512}) verify(text.toString(), feed(text.toString(), chunk));
        }
        views();
        lifecycle();
        String large = "first\r\nsecond\n\n".repeat(2048);
        verify(large, feed(large, 4093));
        System.out.println("M3_GRAMMAR_PASS checks=" + checks);
    }

    private static M3Paragraphs feed(String text, int chunk) {
        M3Grammar grammar = M3FileCharShadow.grammar(10000);
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i += chunk) grammar.update(chars, i, Math.min(chunk, chars.length - i));
        grammar.finish();
        M3Paragraphs result = grammar.details();
        grammar.finish();
        check(grammar.details() == result, "idempotent finish");
        return result;
    }

    private static M3Paragraphs partition(String text, int cuts) {
        M3Grammar grammar = M3Grammar.paragraphs(100);
        char[] chars = text.toCharArray();
        int first = 0;
        for (int i = 0; i < chars.length - 1; i++) {
            if ((cuts & (1 << i)) != 0) {
                grammar.update(chars, first, i + 1 - first);
                first = i + 1;
            }
        }
        grammar.update(chars, first, chars.length - first);
        grammar.finish();
        return grammar.details();
    }

    private static void verify(String text, M3Paragraphs actual) {
        List<long[]> expected = oracle(text);
        check(actual.size() == expected.size(), "paragraph count");
        check(actual.observedUtf16Units() == text.length(), "unit count");
        for (int row = 0; row < expected.size(); row++) {
            long[] e = expected.get(row);
            check(actual.start(row) == e[0], "start");
            check(actual.end(row) == e[1], "end");
            check(actual.firstLine(row) == e[2], "first line");
            check(actual.lastLine(row) == e[3], "last line");
            check(actual.whitespaceTokens(row) == e[4], "tokens");
            check(actual.flags(row) == e[5], "flags");
            check(actual.length(row) == e[1] - e[0], "length");
            check(actual.physicalRow(row) == row, "physical row");
        }
    }

    private static List<long[]> oracle(String text) {
        List<long[]> paragraphs = new ArrayList<>();
        var matcher = BREAKS.matcher(text);
        int cursor = 0;
        int line = 1;
        long[] active = null;
        while (matcher.find()) {
            active = line(text, cursor, matcher.start(), line++, active, paragraphs);
            if (text.charAt(matcher.start()) == '\u2029' && active != null) {
                paragraphs.add(active);
                active = null;
            }
            cursor = matcher.end();
        }
        if (cursor < text.length()) active = line(text, cursor, text.length(), line, active, paragraphs);
        if (active != null) paragraphs.add(active);
        for (long[] p : paragraphs) {
            boolean token = false;
            int flags = p[3] > p[2] ? M3Paragraphs.MULTILINE : 0;
            long count = 0;
            for (int i = (int) p[0]; i < p[1]; i++) {
                char c = text.charAt(i);
                if (c > 127) flags |= M3Paragraphs.NON_ASCII;
                if (Character.isSurrogate(c)) flags |= M3Paragraphs.SURROGATE;
                if (c == '\t') flags |= M3Paragraphs.TAB;
                boolean separator = space(c) || c == '\u0085';
                if (!separator && !token) count++;
                token = !separator;
            }
            p[4] = count;
            p[5] = flags;
        }
        return paragraphs;
    }

    private static long[] line(String text, int begin, int end, int number,
            long[] active, List<long[]> paragraphs) {
        boolean blank = true;
        for (int i = begin; i < end; i++) blank &= space(text.charAt(i));
        if (blank) {
            if (active != null) paragraphs.add(active);
            return null;
        }
        if (active == null) return new long[]{begin, end, number, number, 0, 0};
        active[1] = end;
        active[3] = number;
        return active;
    }

    private static boolean space(char c) { return Character.isWhitespace(c) || Character.isSpaceChar(c); }

    private static void views() {
        M3Paragraphs p = feed("aa\n\nX\n\nbb\n\nlonger\n\n\u03bb\n\n\tQ", 2);
        int[] original = p.rowIds().toArray();
        AtomicInteger callbacks = new AtomicInteger();
        M3Paragraphs selected = p.filter(i -> { check(i == callbacks.getAndIncrement(), "filter order"); return i != 1; });
        check(callbacks.get() == p.size(), "once per row");
        check(Arrays.equals(selected.rowIds().toArray(), new int[]{0, 2, 3, 4, 5}), "selection");
        check(p.filter(i -> true) == p, "all selection reuse");
        check(p.filter(i -> false).size() == 0, "empty selection");
        for (boolean reverse : new boolean[]{false, true}) {
            Integer[] expected = selected.rowIds().boxed().toArray(Integer[]::new);
            Comparator<Integer> comparator = Comparator.comparingLong(p::length);
            Arrays.sort(expected, reverse ? comparator.reversed() : comparator);
            M3Paragraphs sorted = selected.sortedByLength(reverse);
            for (int i = 0; i < expected.length; i++) check(sorted.physicalRow(i) == expected[i], "stable sort");
            check(sorted.sortedByLength(reverse) == sorted, "sorted reuse");
            check(sorted.slice(1, 3).size() == 2, "slice");
            check(sorted.slice(1, 3).physicalRow(0) == sorted.physicalRow(1), "slice physical index");
        }
        check(Arrays.equals(original, p.rowIds().toArray()), "no source mutation");
        check(p.filterMask(M3Paragraphs.NON_ASCII, 0).size() == 1, "mask");
        check(p.filterMask(1, 1).size() == 0, "contradictory mask");
        check(p.filterMask(0, 0) == p, "mask no-op");
        expect(IllegalArgumentException.class, () -> p.filterMask(256, 0));
        expect(IndexOutOfBoundsException.class, () -> p.slice(-1, 0));
        RuntimeException sentinel = new RuntimeException("predicate");
        try { p.filter(i -> { throw sentinel; }); throw new AssertionError(); }
        catch (RuntimeException actual) { check(actual == sentinel, "exception identity"); }
    }

    private static void lifecycle() {
        M3Grammar incomplete = M3Grammar.paragraphs(3);
        expect(IllegalStateException.class, incomplete::details);
        incomplete.update(new char[]{'x'}, 0, 1);
        expect(IllegalStateException.class, incomplete::details);
        incomplete.finish();
        expect(IllegalStateException.class, () -> incomplete.update(new char[0], 0, 0));
        M3Grammar limit = M3Grammar.paragraphs(1, 1);
        limit.update(new char[]{'x'}, 0, 1);
        expect(IllegalStateException.class, () -> limit.update(new char[]{'y'}, 0, 1));
        expect(IllegalStateException.class, limit::finish);
        expect(IllegalStateException.class, limit::details);
        M3Grammar noRows = M3Grammar.paragraphs(0);
        noRows.update(new char[]{'x'}, 0, 1);
        expect(IllegalStateException.class, noRows::finish);
        expect(IllegalStateException.class, noRows::details);
        M3Grammar empty = M3Grammar.paragraphs(0, 0);
        empty.finish();
        check(empty.details().size() == 0, "empty zero budget");
        expect(IllegalArgumentException.class, () -> M3Grammar.paragraphs(-1));
        expect(IllegalArgumentException.class, () -> M3Grammar.paragraphs(1, Long.MAX_VALUE));
        M3Grammar left = M3Grammar.paragraphs(5);
        M3Grammar right = M3Grammar.paragraphs(5);
        M3FileCharShadow composed = left.andThen(right);
        composed.update(new char[]{'x', '\n', '\n', 'y'}, 0, 4);
        composed.finish();
        check(left.details().size() == 2 && right.details().size() == 2, "existing shadow composition");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) { check(type.isInstance(failure), "refusal type"); return; }
        throw new AssertionError("expected " + type.getName());
    }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
