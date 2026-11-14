/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3BigString;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exported-API contract of the long-indexed rope: boundary-slice reuse in {@code concat}, slices of
 * slices, Writer streaming, KMP literal search edges, regex/CharSequence facades and summary-record
 * refusals. Every result is compared with the equivalent {@link String} oracle.
 */
public final class M3BigStringContractTest {
    private static long checks;

    private M3BigStringContractTest() {}

    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void same(Object expected, Object actual, String message) {
        check(
                expected.equals(actual),
                message + ": expected <" + expected + "> but was <" + actual + ">");
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(
                    type.isInstance(thrown)
                            && (reason == null || reason.equals(thrown.getMessage())),
                    "wrong refusal " + thrown);
            return;
        }
        throw new AssertionError("accepted: " + reason);
    }

    /** Characters at every long index agree with the String oracle. */
    private static void sameText(String oracle, M3BigString rope, String message) {
        same((long) oracle.length(), rope.length(), message + " length");
        same(oracle, rope.flatten(), message + " flatten");
        for (int i = 0; i < oracle.length(); i++) {
            check(oracle.charAt(i) == rope.charAt(i), message + " charAt " + i);
        }
        M3BigString.Summary summary = rope.summary();
        same(oracle.hashCode(), summary.javaStringHash(), message + " hash");
        same(
                (long) oracle.codePointCount(0, oracle.length()),
                summary.codePointCount(),
                message + " code points");
        same(oracle.chars().allMatch(c -> c <= 0x7f), summary.ascii(), message + " ascii");
    }

    private static String streamed(M3BigString rope) throws IOException {
        StringWriter writer = new StringWriter();
        rope.appendTo(writer);
        return writer.toString();
    }

    private static void boundarySlices() throws IOException {
        String text = "hello" + "wörld" + "😀!";
        M3BigString rope = M3BigString.of("hello", "wörld", "😀!");
        // Partial first/last chunks become boundary slices with their own summaries.
        M3BigString joined = M3BigString.concat(rope.subSequence(2, 8), rope.subSequence(0, 3));
        String oracle = text.substring(2, 8) + text.substring(0, 3);
        sameText(oracle, joined, "partial-range concat");
        // Slices of rebuilt slices keep one flat source window.
        M3BigString nested = M3BigString.concat(joined.subSequence(1, 7), rope.subSequence(10, 13));
        sameText(oracle.substring(1, 7) + text.substring(10, 13), nested, "slice of slice");
        M3BigString twice = M3BigString.concat(nested.subSequence(2, 4));
        sameText(oracle.substring(3, 5), twice, "slice of nested slice");
        // Writer.append(CharSequence, int, int) materializes through subSequence/toString.
        same(nested.flatten(), streamed(nested), "Writer streaming of slices");
        same(oracle.substring(2, 5), streamed(joined.subSequence(2, 5)), "Writer range streaming");
        StringBuilder builder = new StringBuilder();
        twice.appendTo(builder);
        same(twice.flatten(), builder.toString(), "StringBuilder streaming of slices");
        // Ranges ending before the last chunk, at the last chunk, and empty ropes.
        sameText("hel", M3BigString.concat(rope.subSequence(0, 3)), "range before last chunk");
        sameText(
                text,
                M3BigString.concat(M3BigString.of(), rope, rope.subSequence(4, 4)),
                "empty neighbours");
        check(
                M3BigString.concat(M3BigString.of(), rope.subSequence(3, 3)).isEmpty(),
                "only empty inputs");
        check(M3BigString.concat().isEmpty(), "no inputs");
        refused(NullPointerException.class, "rope", () -> M3BigString.concat(rope, null));
        refused(
                IllegalArgumentException.class,
                "chunks must be immutable String or M3StringPiece values",
                () -> M3BigString.of(new StringBuilder("mutable")));
    }

    private static void literalSearch() {
        M3BigString rope = M3BigString.of("xaa", "ab", "aab");
        String text = "xaaabaab";
        // "aab" needs the KMP failure function both while planning and while scanning.
        same((long) text.indexOf("aab"), rope.indexOf("aab", 0), "KMP restart across chunks");
        same((long) text.indexOf("aab", 3), rope.indexOf("aab", 3), "later occurrence");
        same(-1L, rope.indexOf("aab", 6), "suffix too short");
        same(
                -1L,
                M3BigString.of("ab", "ca").indexOf("ac", 0),
                "bloom candidate scanned to the end");
        M3BigString window = M3BigString.of("abc", "def", "ghi").subSequence(0, 4);
        same(-1L, window.indexOf("da", 0), "scan stops at the range end");
        same(3L, window.indexOf("d", -5), "negative start clamps to zero");
        same(2L, window.indexOf("", 2), "empty needle at start");
        same(4L, window.indexOf("", 9), "empty needle clamps to the end");
        same(-1L, window.indexOf("a", 5), "start beyond the range");
        same(-1L, window.indexOf("zz", 0), "absent code units");
        check(window.mayContainLiteral(""), "empty literal is always a candidate");
        check(window.mayContainLiteral("dab"), "present code units are candidates");
        refused(NullPointerException.class, "needle", () -> window.indexOf(null, 0));
        refused(NullPointerException.class, "literal", () -> window.mayContainLiteral(null));
        singleUnitSearch();
    }

    /**
     * One-unit String needles take the single-unit scan. Every unit and every start of String- and
     * slice-backed views is compared with String.indexOf: a hit at the start, the last index, chunks
     * the bloom filter skips, first chunks with only earlier hits, later-chunk hits and misses,
     * scans cut by the range end and scans past the last chunk.
     */
    private static void singleUnitSearch() {
        M3BigString rope = M3BigString.of("abca", "dd", "xbz", "aq", "bb");
        // Partial chunk ranges are copied by concat as slice chunks, not Strings.
        M3BigString sliced = M3BigString.concat(rope.subSequence(1, 6), rope.subSequence(7, 12));
        M3BigString[] views = {
            rope, rope.subSequence(0, 5), rope.subSequence(2, 9), sliced, sliced.subSequence(1, 8)
        };
        for (M3BigString view : views) {
            String oracle = view.flatten();
            for (char unit : "abcdqxyz".toCharArray()) {
                for (int from = -1; from <= oracle.length() + 1; from++) {
                    same(
                            (long) oracle.indexOf(unit, Math.max(0, from)),
                            view.indexOf(String.valueOf(unit), from),
                            "unit " + unit + " from " + from + " in " + oracle);
                }
            }
        }
    }

    private static void facades() {
        M3BigString rope = M3BigString.of("one ", "two ", "three");
        Matcher matcher = rope.matcher(Pattern.compile("t\\w+"));
        check(matcher.find() && matcher.group().equals("two"), "regex over the rope facade");
        check(matcher.find() && matcher.group().equals("three"), "regex across chunks");
        refused(NullPointerException.class, "pattern", () -> rope.matcher(null));
        CharSequence view = rope.asCharSequence();
        same("one two three", view.toString(), "CharSequence toString");
        CharSequence middle = view.subSequence(2, 9);
        same("e two t", middle.toString(), "CharSequence subSequence");
        same(7, middle.length(), "sub view length");
        check(middle.charAt(6) == 't', "sub view charAt");
        refused(IndexOutOfBoundsException.class, null, () -> view.subSequence(5, 4));
    }

    private static void summaryRecord() {
        M3BigString.Summary valid =
                new M3BigString.Summary(
                        0, 0, 0, 0, 0, true, false, false, false, false, false, false, false,
                        false);
        same(valid, M3BigString.of().summary(), "empty summary value");
        refused(
                IllegalArgumentException.class,
                "negative text summary",
                () ->
                        new M3BigString.Summary(
                                -1, 0, 0, 0, 0, true, false, false, false, false, false, false,
                                false, false));
        refused(
                IllegalArgumentException.class,
                "negative text summary",
                () ->
                        new M3BigString.Summary(
                                1, -1, 0, 0, 0, true, false, false, false, false, false, false,
                                false, false));
        refused(
                IllegalArgumentException.class,
                "negative text summary",
                () ->
                        new M3BigString.Summary(
                                1, 1, -1, 0, 0, true, false, false, false, false, false, false,
                                false, false));
    }

    /**
     * Concatenation refuses a chunk directory beyond Java array limits before allocating it:
     * 32768 references to one 65536-chunk rope need 2^31 directory slots.
     */
    private static void chunkDirectoryLimit() {
        String[] chunks = new String[1 << 16];
        Arrays.fill(chunks, "a");
        M3BigString wide = M3BigString.of(chunks);
        int copies = (int) ((Integer.MAX_VALUE - 8L) / chunks.length) + 1;
        M3BigString[] repeated = new M3BigString[copies];
        Arrays.fill(repeated, wide);
        refused(
                IllegalArgumentException.class,
                "chunk directory exceeds Java array limits",
                () -> M3BigString.concat(repeated));
        same(1L << 17, M3BigString.concat(wide, wide).length(), "the same rope below the limit");
    }

    public static void main(String[] args) throws Exception {
        boundarySlices();
        literalSearch();
        facades();
        summaryRecord();
        chunkDirectoryLimit();
        System.out.println("M3_BIG_STRING_CONTRACT_PASS checks=" + checks);
    }
}
