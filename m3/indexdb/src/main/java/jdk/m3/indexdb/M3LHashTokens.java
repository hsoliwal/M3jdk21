// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * L-Hash v1 canonical token stream as a packed int lane: the Java twin of {@code cpu_lhash.py
 * tokens()} (LOCKED spec, no AST).
 *
 * <p>Input: source text (any language). Output: {@link Tokens}, an {@code int[]} lane of token ids
 * (control 1..50, operators 51..100, delimiters 101..109, literals 151..154, IDENT 201) and its
 * size. Effects: none. Failure: none; unknown characters are dropped exactly as the oracle's
 * {@code other} group drops them. The scanner reproduces the oracle's ordered regex alternation
 * (ws, line comment, block comment, string, number, identifier, operator, delimiter, other), so
 * identifiers, literal values and generics never reach the lane: rename-, literal- and
 * generics-invariant by construction, JSX brackets kept as structure.</p>
 */
//FLLDCIM: LHASH — tokenizer == cpu_lhash.py _TOKEN_RE; the Python is the oracle
//FLLDCIM: HPCPOP — one pass, int lane, no token objects
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3LHashTokens.java blob 9dd696f17d3f; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3LHashTokens {
    public static final int IDENT = 201;
    public static final int STRING = 151;
    public static final int NUMBER = 152;
    public static final int BOOL = 153;
    public static final int NULL = 154;
    public static final int LPAREN = 103;
    private static final Map<String, Integer> CONTROL = Map.ofEntries(
            Map.entry("if", 1), Map.entry("elif", 1), Map.entry("else", 2), Map.entry("for", 3),
            Map.entry("foreach", 3), Map.entry("while", 4), Map.entry("do", 5),
            Map.entry("switch", 6), Map.entry("match", 6), Map.entry("case", 7),
            Map.entry("when", 7), Map.entry("try", 8), Map.entry("catch", 9),
            Map.entry("except", 9), Map.entry("rescue", 9), Map.entry("finally", 10),
            Map.entry("ensure", 10), Map.entry("return", 11), Map.entry("break", 12),
            Map.entry("continue", 13), Map.entry("next", 13), Map.entry("throw", 14),
            Map.entry("raise", 14), Map.entry("def", 15), Map.entry("function", 15),
            Map.entry("func", 15), Map.entry("fn", 15), Map.entry("fun", 15),
            Map.entry("class", 16), Map.entry("struct", 16), Map.entry("interface", 16),
            Map.entry("trait", 16), Map.entry("enum", 16), Map.entry("new", 17),
            Map.entry("true", BOOL), Map.entry("false", BOOL), Map.entry("True", BOOL),
            Map.entry("False", BOOL), Map.entry("null", NULL), Map.entry("None", NULL),
            Map.entry("nil", NULL), Map.entry("undefined", NULL), Map.entry("NULL", NULL));
    /** Operator texts in the oracle's alternation order; ids follow {@code _OPMAP}. */
    private static final String[] OPERATORS = {"===", "!==", "==", "!=", "<=", ">=", "&&", "||",
        "=>", "->", "+=", "-=", "*=", "/=", "%=", "++", "--", "<", ">", "!", "+", "-", "*", "/",
        "%", "=", ".", "?"};
    private static final int[] OPERATOR_IDS = {54, 55, 54, 55, 56, 56, 51, 52, 64, 64, 63, 63, 63,
        63, 63, 57, 58, 56, 56, 53, 57, 58, 59, 60, 61, 62, 65, 66};
    private static final String DELIMITERS = "{}()[];,:";

    /** Packed token lane; {@code ids[0, size)} is the stream. */
    public record Tokens(int[] ids, int size) { }

    private M3LHashTokens() { }

    /** Decodes UTF-8 dropping malformed input, as the oracle's {@code errors="ignore"} does. */
    public static String decode(byte[] utf8) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.IGNORE).decode(ByteBuffer.wrap(utf8))
                    .toString();
        } catch (CharacterCodingException impossible) {
            throw new IllegalStateException("UTF-8 IGNORE decoder reported", impossible);
        }
    }

    public static Tokens tokenize(String source) {
        int[] ids = new int[Math.max(16, source.length() / 2)];
        int size = 0;
        int pos = 0;
        int n = source.length();
        while (pos < n) {
            int cp = source.codePointAt(pos);
            int next = scan(source, pos, cp);
            if (next < 0) {
                if (size == ids.length) ids = java.util.Arrays.copyOf(ids, size * 2);
                ids[size++] = -next;
                next = pos + Character.charCount(cp);
                next = advance(source, pos, cp, next);
            }
            pos = next;
        }
        return new Tokens(ids, size);
    }

    /** Returns the position after a skipped region, or {@code -id} when a token starts here. */
    private static int scan(String s, int pos, int cp) {
        int n = s.length();
        if (cp == '/' && pos + 1 < n && s.charAt(pos + 1) == '/') return lineEnd(s, pos);
        if (cp == '#') return lineEnd(s, pos);
        if (cp == '/' && pos + 1 < n && s.charAt(pos + 1) == '*') {
            int end = s.indexOf("*/", pos + 2);
            if (end >= 0) return end + 2;
        }
        if (cp == '"' || cp == '\'' || cp == '`') return stringEnd(s, pos, cp) < 0 ? pos + 1 : -STRING;
        if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) return -NUMBER;
        if (isIdentStart(cp)) return -CONTROL.getOrDefault(identifier(s, pos), IDENT);
        int operator = operator(s, pos);
        if (operator >= 0) return -OPERATOR_IDS[operator];
        int delimiter = DELIMITERS.indexOf(cp);
        if (delimiter >= 0 && cp < 128) return -(101 + delimiter);
        return pos + Character.charCount(cp);
    }

    /** Position after the token that starts at {@code pos}. */
    private static int advance(String s, int pos, int cp, int fallback) {
        if (cp == '"' || cp == '\'' || cp == '`') return stringEnd(s, pos, cp);
        if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) return numberEnd(s, pos);
        if (isIdentStart(cp)) return pos + identifier(s, pos).length();
        int operator = operator(s, pos);
        if (operator >= 0) return pos + OPERATORS[operator].length();
        return fallback;
    }

    private static int lineEnd(String s, int pos) {
        int end = s.indexOf('\n', pos);
        return end < 0 ? s.length() : end;
    }

    /** End of {@code quote(?:[^quote\\]|\\.)*quote} from {@code pos}, or -1 when unterminated. */
    private static int stringEnd(String s, int pos, int quote) {
        int i = pos + 1;
        int n = s.length();
        while (i < n) {
            int c = s.codePointAt(i);
            if (c == quote) return i + 1;
            i += Character.charCount(c);
            if (c == '\\') {
                if (i >= n) return -1;
                i += Character.charCount(s.codePointAt(i));
            }
        }
        return -1;
    }

    /** End of {@code 0[xX][0-9a-fA-F]+ | \d+\.?\d*(?:[eE][+-]?\d+)?} from {@code pos}. */
    private static int numberEnd(String s, int pos) {
        int n = s.length();
        if (s.charAt(pos) == '0' && pos + 2 < n && (s.charAt(pos + 1) | 0x20) == 'x'
                && isHex(s.charAt(pos + 2))) {
            int i = pos + 3;
            while (i < n && isHex(s.charAt(i))) i++;
            return i;
        }
        int i = digitsEnd(s, pos);
        if (i < n && s.charAt(i) == '.') i = digitsEnd(s, i + 1);
        if (i < n && (s.charAt(i) | 0x20) == 'e') {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
            int k = digitsEnd(s, j);
            if (k > j) return k;
        }
        return i;
    }

    private static int digitsEnd(String s, int from) {
        int i = from;
        int n = s.length();
        while (i < n) {
            int c = s.codePointAt(i);
            if (Character.getType(c) != Character.DECIMAL_DIGIT_NUMBER) break;
            i += Character.charCount(c);
        }
        return i;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || ((c | 0x20) >= 'a' && (c | 0x20) <= 'f');
    }

    private static boolean isIdentStart(int c) {
        return c == '_' || c == '$' || (c < 128 && Character.isLetter(c));
    }

    private static String identifier(String s, int pos) {
        int i = pos + 1;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (!(isIdentStart(c) || (c >= '0' && c <= '9'))) break;
            i++;
        }
        return s.substring(pos, i);
    }

    private static int operator(String s, int pos) {
        for (int i = 0; i < OPERATORS.length; i++) {
            if (s.startsWith(OPERATORS[i], pos)) return i;
        }
        return -1;
    }
}
