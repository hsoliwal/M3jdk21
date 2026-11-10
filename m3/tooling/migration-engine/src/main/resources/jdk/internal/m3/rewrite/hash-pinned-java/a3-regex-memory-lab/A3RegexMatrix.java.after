// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded precompiled regex x string matrix used as A3 lexical/precompute evidence. */
final class A3RegexMatrix {
    record Signal(
            String id,
            String regex,
            int flags,
            String literalPrefix,
            long literalAsciiMask) {
        Signal {
            id = text(id, "id");
            regex = Objects.requireNonNull(regex, "regex");
            literalPrefix = Objects.requireNonNull(literalPrefix, "literalPrefix");
        }
    }

    record Matrix(
            String root,
            int patternCount,
            int subjectCount,
            int matchedCells,
            List<Signal> signals) {
        Matrix {
            if (root == null || !root.matches("[0-9a-f]{64}")
                    || patternCount < 1
                    || subjectCount < 1
                    || matchedCells < 0
                    || matchedCells > patternCount * subjectCount) {
                throw new IllegalArgumentException("invalid A3 regex matrix");
            }
            signals = List.copyOf(signals);
            if (signals.size() != patternCount) {
                throw new IllegalArgumentException("A3 regex signal count");
            }
        }
    }

    private record Prepared(Signal signal, Pattern pattern) {}

    private static final List<String> FIXED_SUBJECTS =
            List.of(
                    "",
                    "plain text",
                    "return (a+b)*31;",
                    "private static int compute(int a, int b) { return (a+b)*31; }",
                    "if (x) { return y; }",
                    "switch (kind) { case 0 -> 1; default -> 2; }",
                    "record Pair(int left, int right) {}",
                    "Pattern.compile(\"a+b?\")",
                    "M3-IOP: PURE_INT_EXPRESSION",
                    "m3$pureIntAtom",
                    "// return (a+b)*31;",
                    "/* private static int ghost() {} */",
                    "foo foo",
                    "alpha βeta 😀 return",
                    "line1\nline2",
                    "aaab");

    private static final List<Prepared> PREPARED =
            List.of(
                    prepared("literal-return", "return (a+b)*31;", Pattern.LITERAL, "return (a+b)*31;"),
                    prepared("method-shape", "private\\s+static\\s+int\\s+compute\\s*\\(", 0, "private"),
                    prepared("keyword-alt", "\\b(?:return|if|switch|record|class)\\b", 0, ""),
                    prepared("quantifier", "a+b?", 0, "a"),
                    prepared("identifier-call", "[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(", 0, ""),
                    prepared("visibility-line", "(?m)^\\s*(?:public|private|protected)\\b", 0, ""),
                    prepared("pattern-call", "(?s)Pattern\\.compile\\(.*?\\)", 0, "Pattern"),
                    prepared("return-lookahead", "\\breturn(?=\\s*\\()", 0, "return"),
                    prepared("m3-lookbehind", "(?<=M3-IOP:\\s)[A-Z_]+", 0, ""),
                    prepared("word-backref", "\\b([A-Za-z]+)\\s+\\1\\b", 0, ""),
                    prepared("unicode-letters", "\\p{L}+", 0, ""),
                    prepared("balanced-flat-braces", "\\{[^{}]*\\}", 0, ""),
                    prepared("line-comment", "(?m)//[^\\r\\n]*$", 0, "//"),
                    prepared("block-comment", "(?s)/\\*.*?\\*/", 0, "/*"),
                    prepared("line-break", "\\R", 0, ""),
                    prepared("m3-atom-literal", "\\Qm3$pureIntAtom\\E", 0, "m3$pureIntAtom"));

    private A3RegexMatrix() {}

    static Matrix evaluate(String payload) {
        ArrayList<String> subjects = new ArrayList<>(FIXED_SUBJECTS);
        subjects.add(Objects.toString(payload, ""));

        MessageDigest digest = digest();
        frame(digest, "M3-A3-REGEX-MATRIX/1");
        frame(digest, Integer.toString(PREPARED.size()));
        frame(digest, Integer.toString(subjects.size()));

        int matched = 0;
        ArrayList<Signal> signals = new ArrayList<>(PREPARED.size());
        for (Prepared prepared : PREPARED) {
            Signal signal = prepared.signal();
            signals.add(signal);
            frame(digest, signal.id());
            frame(digest, signal.regex());
            frame(digest, Integer.toString(signal.flags()));
            frame(digest, signal.literalPrefix());
            frame(digest, Long.toUnsignedString(signal.literalAsciiMask()));
            for (String subject : subjects) {
                frame(digest, subject);
                Matcher matcher = prepared.pattern().matcher(subject);
                if (matcher.find()) {
                    matched++;
                    frame(digest, "1");
                    frame(digest, Integer.toString(matcher.start()));
                    frame(digest, Integer.toString(matcher.end()));
                } else {
                    frame(digest, "0");
                }
            }
        }

        return new Matrix(
                HexFormat.of().formatHex(digest.digest()),
                PREPARED.size(),
                subjects.size(),
                matched,
                signals);
    }

    static int patternCount() {
        return PREPARED.size();
    }

    static int fixedSubjectCount() {
        return FIXED_SUBJECTS.size();
    }

    private static Prepared prepared(
            String id,
            String regex,
            int flags,
            String literalPrefix) {
        Signal signal =
                new Signal(
                        id,
                        regex,
                        flags,
                        literalPrefix,
                        asciiMask(literalPrefix));
        return new Prepared(signal, Pattern.compile(regex, flags));
    }

    private static long asciiMask(String value) {
        long mask = 0L;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (unit < 128) {
                mask |= 1L << (unit & 63);
            }
        }
        return mask;
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
