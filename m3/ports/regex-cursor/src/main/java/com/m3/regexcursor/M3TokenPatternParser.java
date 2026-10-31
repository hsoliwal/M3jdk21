// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static com.m3.regexcursor.M3RegexSyntax.grow;

import java.util.Arrays;
import java.util.Map;

/**
 * Parser of the token pattern notation into {@link M3RegexAst} over the token-id alphabet
 * {@code [0, Integer.MAX_VALUE]}. Notation: NLTK {@code TokenSearcher} angle brackets with the
 * square/t-rex algebra.
 *
 * <pre>
 * alt   := seq ('|' seq)*            seq  := item*        item := atom quant?
 * atom  := '&lt;' spec '&gt;' | '(' ['?:'] alt ')' | '^' | '$'
 * spec  := '!' spec | '.' | '#' NAME | range (',' range)*      range := INT ['-' INT]
 * quant := ('*' | '+' | '?' | '{' INT [',' [INT]] '}') ['?']
 * </pre>
 *
 * <p>Whitespace is ignored. {@code ^}/{@code $} are the lane start/end. Quantifiers over a
 * pattern that can match no token are rejected (no defined iteration semantics).</p>
 */
final class M3TokenPatternParser {
    private final String text;
    private final Map<String, int[]> classes;
    private final M3RegexAst ast = new M3RegexAst(Integer.MAX_VALUE);
    private int pos;

    private M3TokenPatternParser(String text, Map<String, int[]> classes) {
        this.text = text;
        this.classes = classes;
    }

    static M3RegexAst parse(String notation, Map<String, int[]> classes) {
        M3TokenPatternParser parser = new M3TokenPatternParser(notation, classes);
        M3RegexAst ast = parser.ast;
        ast.root = parser.alternation();
        parser.skipSpace();
        if (parser.pos != notation.length()) throw parser.error("unexpected input");
        ast.anyClass = ast.addClass(new int[] {0, Integer.MAX_VALUE}, 1, false);
        return ast;
    }

    private int alternation() {
        int[] items = new int[4];
        int count = 0;
        while (true) {
            items = grow(items, count);
            items[count++] = sequence();
            if (!take('|')) return ast.list(M3RegexAst.ALT, items, count);
        }
    }

    private int sequence() {
        int[] items = new int[8];
        int count = 0;
        while (!atEnd() && !peek('|') && !peek(')')) {
            items = grow(items, count);
            items[count++] = quantifier(atom());
        }
        return ast.list(M3RegexAst.CONCAT, items, count);
    }

    private int atom() {
        char c = text.charAt(pos++);
        switch (c) {
            case '<':
                int id = ast.addClassNode(spec(), false);
                expect('>');
                return id;
            case '(':
                return group();
            case '^':
                ast.assertions = true;
                return ast.node(M3RegexAst.ASSERT, M3RegexAst.BEGIN);
            case '$':
                ast.assertions = true;
                return ast.node(M3RegexAst.ASSERT, M3RegexAst.END);
            default:
                pos--;
                throw error("token or group expected");
        }
    }

    private int group() {
        int index = 0;
        if (text.startsWith("?:", pos)) {
            pos += 2;
        } else {
            index = ++ast.groupCount;
        }
        int body = alternation();
        expect(')');
        if (index != 0) return ast.group(body, index);
        return ast.op[body] == M3RegexAst.GROUP ? ast.node(M3RegexAst.CONCAT, body) : body;
    }

    private int[] spec() {
        skipSpace();
        if (take('!')) return complement(spec());
        if (take('.')) return new int[] {0, Integer.MAX_VALUE};
        if (take('#')) return named();
        int[] pairs = new int[8];
        int count = 0;
        do {
            pairs = Arrays.copyOf(pairs, Math.max(pairs.length, 2 * count + 2));
            int lo = number();
            int hi = take('-') ? number() : lo;
            if (hi < lo) throw error("range order");
            pairs[2 * count] = lo;
            pairs[2 * count + 1] = hi;
            count++;
        } while (take(','));
        return Arrays.copyOf(pairs, 2 * count);
    }

    private int[] complement(int[] pairs) {
        int[] out = M3RegexSyntax.complement(pairs, Integer.MAX_VALUE);
        if (out.length == 0) throw error("empty token class");
        return out;
    }

    private int[] named() {
        int start = pos;
        while (pos < text.length() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
            pos++;
        }
        int[] ids = classes.get(text.substring(start, pos));
        if (ids == null || ids.length == 0) throw error("unknown token class");
        int[] pairs = new int[2 * ids.length];
        for (int index = 0; index < ids.length; index++) {
            if (ids[index] < 0) throw error("negative token id");
            pairs[2 * index] = ids[index];
            pairs[2 * index + 1] = ids[index];
        }
        return pairs;
    }

    private int number() {
        skipSpace();
        int start = pos;
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
        if (start == pos || pos - start > 10) throw error("token id expected");
        long value = Long.parseLong(text.substring(start, pos));
        if (value > Integer.MAX_VALUE) throw error("token id too large");
        skipSpace();
        return (int) value;
    }

    private int quantifier(int atom) {
        skipSpace();
        if (atEnd()) return atom;
        char c = text.charAt(pos);
        int lower;
        int upper;
        if (c == '*' || c == '+' || c == '?') {
            pos++;
            lower = c == '+' ? 1 : 0;
            upper = c == '?' ? 1 : -1;
        } else if (c == '{') {
            pos++;
            lower = number();
            upper = take(',') ? (peek('}') ? -1 : number()) : lower;
            expect('}');
            if (upper >= 0 && upper < lower) throw error("bounds");
        } else {
            return atom;
        }
        boolean lazy = take('?');
        skipSpace();
        if (!atEnd() && "*+?{".indexOf(text.charAt(pos)) >= 0) throw error("stacked quantifier");
        if (ast.op[atom] == M3RegexAst.ASSERT) throw error("quantified assertion");
        if (upper != 1 && ast.nullable(atom)) throw error("nullable loop");
        if (lower == 1 && upper == 1) return atom;
        return ast.repeat(atom, lower, upper, !lazy);
    }

    private boolean take(char c) {
        skipSpace();
        if (peek(c)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!take(c)) throw error("'" + c + "' expected");
    }

    private boolean peek(char c) {
        skipSpace();
        return pos < text.length() && text.charAt(pos) == c;
    }

    private boolean atEnd() {
        skipSpace();
        return pos >= text.length();
    }

    private void skipSpace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
    }

    private IllegalArgumentException error(String reason) {
        return new IllegalArgumentException(reason + " at " + pos + " in token pattern: " + text);
    }
}
