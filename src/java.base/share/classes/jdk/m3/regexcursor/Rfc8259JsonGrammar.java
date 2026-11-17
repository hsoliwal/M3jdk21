// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * RFC 8259 JSON grammar backed by one build-prepared M3 grammar image.
 *
 * <p>The source token scanner is deliberately small and exact: it identifies
 * lexical spans but delegates their classification/validation to the common
 * versioned {@link GrammarSpecification} trie and compiled regex programs.
 * No per-token String, Pattern, Matcher or AST node is retained. Syntax uses
 * the same packed LL(1) table as any other language grammar.</p>
 *
 * <p>JSON text is matched exactly, not as a substring. Allowed whitespace is
 * only SP, HTAB, LF or CR; comments and trailing commas are not admitted.
 * This class validates syntax, not application-level schema constraints.</p>
 */
public final class Rfc8259JsonGrammar {
    public static final int OBJECT_OPEN = 1;
    public static final int OBJECT_CLOSE = 2;
    public static final int ARRAY_OPEN = 3;
    public static final int ARRAY_CLOSE = 4;
    public static final int COMMA = 5;
    public static final int COLON = 6;
    public static final int TRUE = 7;
    public static final int FALSE = 8;
    public static final int NULL = 9;
    public static final int STRING = 10;
    public static final int NUMBER = 11;

    private static final String LANGUAGE = "JSON";
    private static final String REVISION = "RFC8259-2017";
    private static final String SPEC = """
            value = object | array | STRING | NUMBER | 'true' | 'false' | 'null';
            object = '{', [STRING, ':', value, {',', STRING, ':', value}], '}';
            array = '[', [value, {',', value}], ']';
            """;
    // The image rfc8259-2017.m3grammar is embedded in Rfc8259JsonGrammarImage
    // (java.base does not copy arbitrary resource files into the module).
    // Supplied by the build-time image generator; hash is not read from the same file.
    private static final String EXPECTED_IMAGE_SHA256 =
            "63538ed114e8969d08ac3073d18b3329ad693cafc40aa90e5a5ad547cd8a57c1";

    private Rfc8259JsonGrammar() { }

    /** Canonical spec compiler, invoked by build tooling rather than on every startup. */
    static GrammarSpecification compileReference() {
        List<StaticGrammarLexicon.Literal> fixed = List.of(
                literal("{", OBJECT_OPEN), literal("}", OBJECT_CLOSE),
                literal("[", ARRAY_OPEN), literal("]", ARRAY_CLOSE),
                literal(",", COMMA), literal(":", COLON),
                literal("true", TRUE), literal("false", FALSE), literal("null", NULL));
        // The escaped quote, backslash and control-class characters below are
        // Java-regex syntax; these two patterns are an exact RFC 8259 lexical
        // description, not a replacement for the JDK Pattern engine.
        // Build regex syntax from named atoms, avoiding accidental double
        // escaping between Java source, regex syntax, and JSON syntax.
        String regexBackslash = "\\\\";
        String unescaped = "[^\\\"" + regexBackslash + "\\x00-\\x1F]";
        String escaped = regexBackslash + "(?:[\\\"" + regexBackslash
                + "/bfnrt]|u[0-9a-fA-F]{4})";
        String jsonStringRegex = "\\\"(?:" + unescaped + "|" + escaped + ")*\\\"";
        List<GrammarSpecification.RegexRule> lexical = List.of(
                new GrammarSpecification.RegexRule(STRING, jsonStringRegex, 0, 0),
                new GrammarSpecification.RegexRule(NUMBER,
                        "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?", 0, 1));
        var grammar = EbnfGrammar.normalize(SPEC, "value", Map.ofEntries(
                entry("STRING", STRING), entry("NUMBER", NUMBER),
                entry("{", OBJECT_OPEN), entry("}", OBJECT_CLOSE),
                entry("[", ARRAY_OPEN), entry("]", ARRAY_CLOSE),
                entry(",", COMMA), entry(":", COLON),
                entry("true", TRUE), entry("false", FALSE), entry("null", NULL)));
        return GrammarSpecification.compile(LANGUAGE, REVISION, fixed, lexical,
                grammar.startSymbol(), grammar.productions());
    }

    private static StaticGrammarLexicon.Literal literal(String word, int id) {
        return new StaticGrammarLexicon.Literal(word, id, false);
    }

    /** Lazy once-per-JVM read of the build-precomputed image. */
    public static GrammarSpecification prepared() {
        return Prepared.SPECIFICATION;
    }

    private static final class Prepared {
        private static final GrammarSpecification SPECIFICATION = load();

        private static GrammarSpecification load() {
            GrammarSpecification specification = GrammarSpecification.fromImage(
                    Rfc8259JsonGrammarImage.bytes(), EXPECTED_IMAGE_SHA256);
            if (!LANGUAGE.equals(specification.language())
                    || !REVISION.equals(specification.revision())) {
                throw new IllegalStateException("incorrect JSON grammar image identity");
            }
            return specification;
        }
    }

    /**
     * Classify source spans into a single primitive token lane. Only the grammar
     * session's matcher arrays persist during this scan, never token objects.
     * Throws on lexical errors; syntax errors return false from {@link #accepts}.
     */
    public static int[] tokens(CharSequence text) {
        Objects.requireNonNull(text, "text");
        CursorTokens scanner = new CursorTokens(text);
        int[] tokens = new int[32];
        int count = 0;
        while (true) {
            int id = scanner.getAsInt();
            if (id == 0) break;
            if (count == tokens.length) {
                if (count > Integer.MAX_VALUE / 2) {
                    throw new IllegalArgumentException("JSON token limit exceeded");
                }
                tokens = Arrays.copyOf(tokens, count * 2);
            }
            tokens[count++] = id;
        }
        return Arrays.copyOf(tokens, count);
    }

    /**
     * Single-pass streaming syntax over RFC 8259 source, retaining no token
     * array regardless of input size. Only grammar state and matchers are kept.
     */
    public static boolean acceptsStreaming(CharSequence source) {
        Objects.requireNonNull(source, "source");
        try {
            return prepared().accepts(new CursorTokens(source));
        } catch (IllegalArgumentException invalidLexeme) {
            return false;
        }
    }

    private static final class CursorTokens implements IntSupplier {
        private final CharSequence source;
        private final GrammarSpecification.Session session;
        private int position;

        private CursorTokens(CharSequence source) {
            this.source = source;
            this.session = prepared().bind(source);
        }

        @Override
        public int getAsInt() {
            int length = source.length();
            while (position < length) {
                char c = source.charAt(position);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') position++;
                else break;
            }
            if (position == length) return 0;
            int from = position;
            char initial = source.charAt(from);
            switch (initial) {
                case '{', '}', '[', ']', ':', ',' -> position++;
                case '"' -> position = scanString(source, position);
                default -> {
                    if (initial == '-' || isDigit(initial)) position = scanNumber(source, position);
                    else if (initial >= 'a' && initial <= 'z')
                        position = scanWord(source, position);
                    else throw new IllegalArgumentException("invalid JSON token at UTF-16 offset "
                            + from);
                }
            }
            int id = session.classify(from, position);
            if (id <= 0) {
                throw new IllegalArgumentException("invalid JSON token at UTF-16 offset " + from);
            }
            return id;
        }
    }

    /** Strict JSON syntax validation without building per-entry objects or strings. */
    public static boolean accepts(CharSequence source) {
        try {
            return prepared().accepts(tokens(source));
        } catch (IllegalArgumentException invalidLexeme) {
            return false;
        }
    }

    private static int scanString(CharSequence source, int offset) {
        int i = offset + 1, length = source.length();
        while (i < length) {
            char unit = source.charAt(i++);
            if (unit == '"') return i;
            if (unit == '\\') {
                if (i == length) break;
                // The precompiled JSON regex validates every escape and every
                // exactly-four-digit Unicode escape; this scan finds span only.
                i++;
            } else if (Character.isHighSurrogate(unit)) {
                if (i == length || !Character.isLowSurrogate(source.charAt(i))) {
                    throw new IllegalArgumentException("unpaired raw high surrogate");
                }
                i++;
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException("unpaired raw low surrogate");
            }
        }
        throw new IllegalArgumentException("unterminated JSON string");
    }

    private static int scanNumber(CharSequence source, int offset) {
        int i = offset, length = source.length();
        while (i < length) {
            char c = source.charAt(i);
            if (!isDigit(c) && c != '.' && c != '-' && c != '+' && c != 'e' && c != 'E') break;
            i++;
        }
        return i;
    }

    private static int scanWord(CharSequence source, int offset) {
        int i = offset, n = source.length();
        while (i < n && source.charAt(i) >= 'a' && source.charAt(i) <= 'z') i++;
        return i;
    }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
}
