// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative immutable facts for one regular-expression / String pair.
 *
 * <p>Only the plain-literal subset owns executable precompute facts. General regex syntax,
 * matching state, captures and all JDK regex semantics remain owned by {@link Pattern}.</p>
 */
public final class M3RegexStringSignals {
    private static final String REGEX_META = "\\.^$|?*+()[]{}";

    public enum Feature {
        ESCAPE, DOT, CHAR_CLASS, GROUP, ALTERNATION, QUANTIFIER, ANCHOR,
        LOOKAROUND, BACKREFERENCE, UNICODE_CLASS, INLINE_FLAG, QUOTED_REGION
    }

    public record Pair(String pattern, String subject) {
        public Pair {
            pattern = Objects.requireNonNull(pattern, "pattern");
            subject = Objects.requireNonNull(subject, "subject");
        }
    }

    public record Fact(
            String patternSha256,
            String subjectSha256,
            int patternUtf16Units,
            int patternCodePoints,
            int subjectUtf16Units,
            int subjectCodePoints,
            boolean patternAscii,
            boolean subjectAscii,
            boolean subjectHasLineTerminator,
            List<Feature> features,
            boolean plainLiteral,
            int literalFindIndex,
            boolean literalFullMatch,
            String rootSha256) {
        public Fact {
            patternSha256 = requireSha(patternSha256, "patternSha256");
            subjectSha256 = requireSha(subjectSha256, "subjectSha256");
            if (patternUtf16Units < 0 || patternCodePoints < 0
                    || subjectUtf16Units < 0 || subjectCodePoints < 0) {
                throw new IllegalArgumentException("negative regex/string length");
            }
            features = List.copyOf(Objects.requireNonNull(features, "features"));
            if (!plainLiteral && literalFindIndex != Integer.MIN_VALUE) {
                throw new IllegalArgumentException("non-literal find index must use sentinel");
            }
            rootSha256 = requireSha(rootSha256, "rootSha256");
        }

        public boolean semanticAuthority() { return false; }
        public boolean mutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public record Snapshot(List<Fact> facts, int plainLiteralFacts, String rootSha256) {
        public Snapshot {
            facts = List.copyOf(Objects.requireNonNull(facts, "facts"));
            if (facts.isEmpty()) throw new IllegalArgumentException("empty regex/string facts");
            if (plainLiteralFacts < 0 || plainLiteralFacts > facts.size()) {
                throw new IllegalArgumentException("plainLiteralFacts");
            }
            rootSha256 = requireSha(rootSha256, "rootSha256");
        }

        public boolean semanticAuthority() { return false; }
        public boolean mutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public record LiteralParity(
            boolean applicable,
            boolean findMatched,
            int findStart,
            int expectedFindStart,
            boolean fullMatched,
            boolean expectedFullMatched,
            String rootSha256) {
        public LiteralParity {
            if (!applicable
                    && (findStart != Integer.MIN_VALUE
                        || expectedFindStart != Integer.MIN_VALUE)) {
                throw new IllegalArgumentException("non-applicable parity sentinel");
            }
            rootSha256 = requireSha(rootSha256, "rootSha256");
        }

        public boolean passed() {
            return !applicable
                    || (findMatched == (expectedFindStart >= 0)
                        && findStart == expectedFindStart
                        && fullMatched == expectedFullMatched);
        }
    }

    private M3RegexStringSignals() {}

    public static Fact analyze(String pattern, String subject) {
        Pair pair = new Pair(pattern, subject);
        EnumSet<Feature> found = features(pair.pattern());
        boolean literal = plainLiteral(pair.pattern());
        int findIndex = literal ? pair.subject().indexOf(pair.pattern()) : Integer.MIN_VALUE;
        boolean fullMatch = literal && pair.subject().equals(pair.pattern());
        List<Feature> ordered = List.copyOf(found);
        String patternHash = hashUtf16(pair.pattern());
        String subjectHash = hashUtf16(pair.subject());
        String root = root(
                "M3_REGEX_STRING_FACT_V2",
                patternHash,
                subjectHash,
                Integer.toString(pair.pattern().length()),
                Integer.toString(pair.pattern().codePointCount(0, pair.pattern().length())),
                Integer.toString(pair.subject().length()),
                Integer.toString(pair.subject().codePointCount(0, pair.subject().length())),
                Boolean.toString(ascii(pair.pattern())),
                Boolean.toString(ascii(pair.subject())),
                Boolean.toString(hasLineTerminator(pair.subject())),
                ordered.stream().map(Enum::name).reduce("", (left, right) -> left + right + "\u001f"),
                Boolean.toString(literal),
                Integer.toString(findIndex),
                Boolean.toString(fullMatch));
        return new Fact(
                patternHash,
                subjectHash,
                pair.pattern().length(),
                pair.pattern().codePointCount(0, pair.pattern().length()),
                pair.subject().length(),
                pair.subject().codePointCount(0, pair.subject().length()),
                ascii(pair.pattern()),
                ascii(pair.subject()),
                hasLineTerminator(pair.subject()),
                ordered,
                literal,
                findIndex,
                fullMatch,
                root);
    }

    public static Snapshot analyze(List<Pair> pairs, M3Progress monitor) {
        List<Pair> checked = List.copyOf(Objects.requireNonNull(pairs, "pairs"));
        if (checked.isEmpty() || checked.size() > 100_000) {
            throw new IllegalArgumentException("regex/string pair count");
        }
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        progress.begin("M3 regex/string static facts", checked.size());
        try {
            ArrayList<Fact> facts = new ArrayList<>(checked.size());
            int literals = 0;
            MessageDigest digest = digest();
            frame(digest, "M3_REGEX_STRING_SNAPSHOT_V2");
            frame(digest, Integer.toString(checked.size()));
            for (int index = 0; index < checked.size(); index++) {
                if ((index & 255) == 0) progress.checkCanceled();
                Pair pair = checked.get(index);
                Fact fact = analyze(pair.pattern(), pair.subject());
                facts.add(fact);
                if (fact.plainLiteral()) literals++;
                frame(digest, fact.rootSha256());
                progress.worked(1);
            }
            progress.checkCanceled();
            return new Snapshot(facts, literals, HexFormat.of().formatHex(digest.digest()));
        } finally {
            progress.done();
        }
    }

    /** JDK Pattern remains the exact oracle for the only executable precompute subset. */
    public static LiteralParity verifyPlainLiteralAgainstJdk(String pattern, String subject) {
        Fact fact = analyze(pattern, subject);
        if (!fact.plainLiteral()) {
            return new LiteralParity(
                    false, false, Integer.MIN_VALUE, Integer.MIN_VALUE, false, false,
                    root("M3_REGEX_STRING_LITERAL_PARITY_V2", fact.rootSha256(), "NOT_APPLICABLE"));
        }
        Matcher matcher = Pattern.compile(pattern).matcher(subject);
        boolean found = matcher.find();
        int start = found ? matcher.start() : -1;
        boolean matches = Pattern.compile(pattern).matcher(subject).matches();
        LiteralParity parity = new LiteralParity(
                true,
                found,
                start,
                fact.literalFindIndex(),
                matches,
                fact.literalFullMatch(),
                root(
                        "M3_REGEX_STRING_LITERAL_PARITY_V2",
                        fact.rootSha256(),
                        Boolean.toString(found),
                        Integer.toString(start),
                        Integer.toString(fact.literalFindIndex()),
                        Boolean.toString(matches),
                        Boolean.toString(fact.literalFullMatch())));
        if (!parity.passed()) {
            throw new IllegalStateException("M3 plain-literal signal disagrees with JDK Pattern");
        }
        return parity;
    }

    static EnumSet<Feature> features(String pattern) {
        EnumSet<Feature> result = EnumSet.noneOf(Feature.class);
        for (int index = 0; index < pattern.length(); index++) {
            char value = pattern.charAt(index);
            if (value == '\\') {
                result.add(Feature.ESCAPE);
                if (index + 1 < pattern.length()) {
                    char next = pattern.charAt(++index);
                    if (next >= '1' && next <= '9') result.add(Feature.BACKREFERENCE);
                    if (next == 'p' || next == 'P') result.add(Feature.UNICODE_CLASS);
                    if (next == 'Q') result.add(Feature.QUOTED_REGION);
                }
                continue;
            }
            switch (value) {
                case '.' -> result.add(Feature.DOT);
                case '[' -> result.add(Feature.CHAR_CLASS);
                case '(' -> {
                    result.add(Feature.GROUP);
                    if (index + 2 < pattern.length() && pattern.charAt(index + 1) == '?') {
                        char kind = pattern.charAt(index + 2);
                        if (kind == '=' || kind == '!') result.add(Feature.LOOKAROUND);
                        else if (kind == '<'
                                && index + 3 < pattern.length()
                                && (pattern.charAt(index + 3) == '='
                                    || pattern.charAt(index + 3) == '!')) {
                            result.add(Feature.LOOKAROUND);
                        } else if (Character.isLetter(kind) || kind == '-') {
                            result.add(Feature.INLINE_FLAG);
                        }
                    }
                }
                case '|' -> result.add(Feature.ALTERNATION);
                case '*', '+', '?', '{' -> result.add(Feature.QUANTIFIER);
                case '^', '$' -> result.add(Feature.ANCHOR);
                default -> { }
            }
        }
        return result;
    }

    static boolean plainLiteral(String pattern) {
        for (int index = 0; index < pattern.length(); index++) {
            if (REGEX_META.indexOf(pattern.charAt(index)) >= 0) return false;
        }
        return true;
    }

    private static boolean ascii(String value) {
        return value.codePoints().allMatch(codePoint -> codePoint <= 0x7f);
    }

    private static boolean hasLineTerminator(String value) {
        return value.codePoints().anyMatch(codePoint ->
                codePoint == '\n' || codePoint == '\r' || codePoint == 0x85
                        || codePoint == 0x2028 || codePoint == 0x2029);
    }

    private static String hashUtf16(String value) {
        MessageDigest digest = digest();
        updateUtf16(digest, value);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String root(String domain, String... values) {
        MessageDigest digest = digest();
        frame(digest, domain);
        for (String value : values) frame(digest, Objects.requireNonNull(value, "root value"));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static void updateUtf16(MessageDigest digest, String value) {
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
    }

    private static String requireSha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
