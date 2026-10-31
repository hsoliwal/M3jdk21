// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Static signal bundle for source-code-looking and regex-looking text.
 *
 * <p>Scores nominate test/candidate lanes only. They are not Java syntax, regex validity, semantic
 * equivalence or promotion authority.</p>
 */
public final class M3CodeTextSignals {
    public static final int HAS_BRACE = 1;
    public static final int HAS_PAREN = 1 << 1;
    public static final int HAS_SEMICOLON = 1 << 2;
    public static final int HAS_BACKSLASH = 1 << 3;
    public static final int HAS_REGEX_META = 1 << 4;
    public static final int HAS_ARROW = 1 << 5;
    public static final int HAS_METHOD_REFERENCE = 1 << 6;
    public static final int HAS_ASSIGN = 1 << 7;
    public static final int HAS_COMPARISON = 1 << 8;
    public static final int HAS_QUANTIFIER = 1 << 9;
    public static final int HAS_CLASS_BRACKET = 1 << 10;
    public static final int HAS_DOT = 1 << 11;
    public static final int HAS_COMMA = 1 << 12;
    public static final int HAS_COLON = 1 << 13;
    public static final int HAS_AMP_PIPE = 1 << 14;
    public static final int HAS_WHITESPACE = 1 << 15;

    private static final List<String> JAVA_WORDS =
            List.of(
                    "class", "interface", "enum", "record", "if", "else", "for", "while", "do",
                    "switch", "case", "try", "catch", "finally", "throw", "throws", "return", "new",
                    "public", "private", "protected", "static", "final", "synchronized", "native",
                    "void", "int", "long", "boolean", "double", "float", "char", "byte", "short");

    private M3CodeTextSignals() {}

    public record Snapshot(
            M3TextSignals text,
            long lexicalPacked,
            int javaKeywordHits,
            int codeScore,
            int regexScore,
            M3RegexShape.Kind regexShape) {

        public Snapshot {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(regexShape, "regexShape");
            if (javaKeywordHits < 0 || codeScore < 0 || regexScore < 0) {
                throw new IllegalArgumentException("negative code-text signal");
            }
        }

        public int lexicalFlags() {
            return (int) lexicalPacked;
        }

        public int semicolonCount() {
            return byteLane(lexicalPacked, 16);
        }

        public int braceCount() {
            return byteLane(lexicalPacked, 24);
        }

        public int parenCount() {
            return byteLane(lexicalPacked, 32);
        }

        public int backslashCount() {
            return byteLane(lexicalPacked, 40);
        }

        public int regexMetaCount() {
            return byteLane(lexicalPacked, 48);
        }

        public int punctuationCount() {
            return byteLane(lexicalPacked, 56);
        }

        public boolean likelyCode() {
            return codeScore >= 8;
        }

        public boolean likelyRegex() {
            return regexScore >= 8;
        }

        public int simHashDistance(Snapshot other) {
            return text.simHashDistance(Objects.requireNonNull(other, "other").text);
        }

        public double estimatedJaccard(Snapshot other) {
            return text.estimatedJaccard(Objects.requireNonNull(other, "other").text);
        }

        private static int byteLane(long value, int shift) {
            return (int) ((value >>> shift) & 0xffL);
        }
    }

    public static Snapshot compile(CharSequence text) {
        Objects.requireNonNull(text, "text");
        char[] units = snapshot(text);
        return finish(text, M3CodeTextSignalBatchJava.pack(units, 0, units.length));
    }

    public static List<Snapshot> compileBatch(
            List<? extends CharSequence> texts,
            M3CodeTextSignalBatch batch,
            M3CodeTextSignalBatch.Limits limits,
            M3Progress monitor) {
        List<? extends CharSequence> input = List.copyOf(Objects.requireNonNull(texts, "texts"));
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(limits, "limits");
        int[] offsets = new int[input.size()];
        int[] lengths = new int[input.size()];
        long total = 0;

        for (int row = 0; row < input.size(); row++) {
            CharSequence text = Objects.requireNonNull(input.get(row), "text");
            if (total > Integer.MAX_VALUE - (long) text.length()) {
                throw new IllegalArgumentException("packed code-text image exceeds JVM geometry");
            }
            offsets[row] = (int) total;
            lengths[row] = text.length();
            total += text.length();
        }
        char[] packed = new char[(int) total];
        for (int row = 0; row < input.size(); row++) {
            CharSequence text = input.get(row);
            int offset = offsets[row];
            for (int index = 0; index < text.length(); index++) {
                packed[offset + index] = text.charAt(index);
            }
        }

        long[] lexical = batch.analyze(packed, offsets, lengths, limits, monitor);
        if (lexical.length != input.size()) {
            throw new IllegalStateException("code-text provider changed row geometry");
        }
        ArrayList<Snapshot> result = new ArrayList<>(input.size());
        for (int row = 0; row < input.size(); row++) {
            result.add(finish(input.get(row), lexical[row]));
        }
        return List.copyOf(result);
    }

    private static Snapshot finish(CharSequence input, long lexicalPacked) {
        String text = input instanceof String ? (String) input : input.toString();
        M3TextSignals base = M3TextSignals.compile(text);
        int keywords = keywordHits(text);
        int flags = (int) lexicalPacked;
        int code =
                saturate(
                        keywords * 4L
                                + lane(lexicalPacked, 16) * 4L
                                + lane(lexicalPacked, 24) * 3L
                                + lane(lexicalPacked, 32)
                                + ((flags & HAS_ARROW) != 0 ? 5 : 0)
                                + ((flags & HAS_METHOD_REFERENCE) != 0 ? 5 : 0)
                                + ((flags & HAS_ASSIGN) != 0 ? 2 : 0)
                                + ((flags & HAS_COMPARISON) != 0 ? 2 : 0));
        int regex =
                saturate(
                        lane(lexicalPacked, 48) * 2L
                                + lane(lexicalPacked, 40) * 2L
                                + ((flags & HAS_CLASS_BRACKET) != 0 ? 4 : 0)
                                + ((flags & HAS_QUANTIFIER) != 0 ? 4 : 0)
                                + ((flags & HAS_REGEX_META) != 0 ? 2 : 0));
        return new Snapshot(
                base,
                lexicalPacked,
                keywords,
                code,
                regex,
                M3RegexShape.analyze(text).kind());
    }

    private static int keywordHits(CharSequence source) {
        int hits = 0;
        StringBuilder token = new StringBuilder();
        for (int index = 0; index <= source.length(); index++) {
            char unit = index == source.length() ? '\0' : source.charAt(index);
            if (index < source.length() && Character.isJavaIdentifierPart(unit)) {
                token.append(unit);
                continue;
            }
            if (!token.isEmpty()) {
                String word = token.toString().toLowerCase(Locale.ROOT);
                if (JAVA_WORDS.contains(word)) hits++;
                token.setLength(0);
            }
        }
        return hits;
    }

    private static char[] snapshot(CharSequence text) {
        char[] result = new char[text.length()];
        for (int index = 0; index < result.length; index++) result[index] = text.charAt(index);
        return result;
    }

    private static int lane(long value, int shift) {
        return (int) ((value >>> shift) & 0xffL);
    }

    private static int saturate(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
