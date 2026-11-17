// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static jdk.m3.regexcursor.RegexSyntax.LINE_TERMINATORS;
import static jdk.m3.regexcursor.RegexSyntax.grow;
import static jdk.m3.regexcursor.RegexSyntax.predefined;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Parser of the {@code java.util.regex} subset the cursor engines prove equal to the stock engine
 * (see m3/docs/regex-cursor/DESIGN.md section 4). Input patterns have already compiled with the
 * stock {@code Pattern}; anything outside the subset throws {@link M3RegexRefusal}.
 */
final class RegexSubsetParser {
    static final int MAX_COUNT = 1000;
    private static final String QUOTE = "\\" + "Q";
    private static final String UNQUOTE = "\\" + "E";

    private final String text;
    private final RegexAst ast = new RegexAst(0xFFFF);
    private final boolean dotAll;
    private int pos;

    private RegexSubsetParser(String text, boolean dotAll) {
        this.text = text;
        this.dotAll = dotAll;
    }

    static RegexAst parse(String regex, int flags) {
        if ((flags & ~Pattern.DOTALL) != 0) throw new RegexRefusal("flags");
        RegexSubsetParser parser = new RegexSubsetParser(regex, flags != 0);
        RegexAst ast = parser.ast;
        ast.root = parser.alternation();
        if (parser.pos != regex.length()) throw new RegexRefusal("unbalanced");
        ast.anyClass = ast.addClass(new int[] {0, 0xFFFF}, 1, false);
        return ast;
    }

    private int alternation() {
        int[] items = new int[4];
        int count = 0;
        while (true) {
            items = grow(items, count);
            items[count++] = sequence();
            if (!peek('|')) return ast.list(RegexAst.ALT, items, count);
            pos++;
        }
    }

    private int sequence() {
        int[] items = new int[8];
        int count = 0;
        while (pos < text.length() && !peek('|') && !peek(')')) {
            if (text.startsWith(QUOTE, pos)) {
                int end = text.indexOf(UNQUOTE, pos + 2);
                int stop = end < 0 ? text.length() : end;
                int from = pos + 2;
                pos = end < 0 ? stop : end + 2;
                for (int at = from; at < stop; at++) {
                    items = grow(items, count);
                    int atom = literal(text.charAt(at));
                    items[count++] = at + 1 == stop ? quantifier(atom) : atom;
                }
                continue;
            }
            items = grow(items, count);
            items[count++] = quantifier(atom());
        }
        return ast.list(RegexAst.CONCAT, items, count);
    }

    private int quantifier(int atom) {
        if (pos >= text.length()) return atom;
        char c = text.charAt(pos);
        int lower;
        int upper;
        if (c == '*' || c == '+' || c == '?') {
            pos++;
            lower = c == '+' ? 1 : 0;
            upper = c == '?' ? 1 : -1;
        } else if (c == '{') {
            int[] bounds = braces();
            lower = bounds[0];
            upper = bounds[1];
        } else {
            return atom;
        }
        boolean lazy = peek('?');
        if (lazy) pos++;
        if (peek('+')) throw new RegexRefusal("possessive");
        if (pos < text.length() && "*+?{".indexOf(text.charAt(pos)) >= 0) {
            throw new RegexRefusal("stacked quantifier");
        }
        return repeat(atom, lower, upper, !lazy);
    }

    private int repeat(int atom, int lower, int upper, boolean greedy) {
        if (ast.op[atom] == RegexAst.ASSERT) throw new RegexRefusal("quantified assertion");
        if (upper != 1 && ast.nullable(atom)) throw new RegexRefusal("nullable loop");
        int body = ast.op[atom] == RegexAst.GROUP ? ast.a[atom] : atom;
        if (upper != 1 && ast.hasGroup(body)) {
            // Stock GroupCurly/Curly keep inner captures of failed attempts (no restore).
            throw new RegexRefusal("capture inside loop");
        }
        if (lower == 1 && upper == 1) return atom;
        return ast.repeat(atom, lower, upper, greedy);
    }

    private int[] braces() {
        int close = text.indexOf('}', pos);
        if (close < 0) throw new RegexRefusal("brace");
        String body = text.substring(pos + 1, close);
        pos = close + 1;
        int comma = body.indexOf(',');
        int lower = RegexSyntax.number(comma < 0 ? body : body.substring(0, comma), MAX_COUNT);
        int upper = comma < 0 ? lower
                : comma == body.length() - 1 ? -1 : RegexSyntax.number(body.substring(comma + 1), MAX_COUNT);
        if (upper >= 0 && upper < lower) throw new RegexRefusal("bounds");
        return new int[] {lower, upper};
    }

    private int atom() {
        char c = text.charAt(pos++);
        switch (c) {
            case '(':
                return group();
            case '[':
                return charClass();
            case '.':
                return dotAll
                        ? ast.addClassNode(new int[] {0, 0xFFFF}, false)
                        : ast.addClassNode(LINE_TERMINATORS, true);
            case '^':
                return assertion(RegexAst.BEGIN);
            case '$':
                return assertion(RegexAst.END_Z);
            case '\\':
                return escape();
            case '*':
            case '+':
            case '?':
            case '{':
            case ')':
                throw new RegexRefusal("dangling " + c);
            default:
                return literal(c);
        }
    }

    private int group() {
        int index = 0;
        if (peek('?')) {
            if (!text.startsWith("?:", pos)) throw new RegexRefusal("group kind");
            pos += 2;
        } else {
            index = ++ast.groupCount;
        }
        int body = alternation();
        if (!peek(')')) throw new RegexRefusal("unclosed group");
        pos++;
        if (index != 0) return ast.group(body, index);
        // (?:(x)) stays distinct from (x): stock quantifies them with different capture rules.
        return ast.op[body] == RegexAst.GROUP ? ast.node(RegexAst.CONCAT, body) : body;
    }

    private int assertion(int kind) {
        ast.assertions = true;
        return ast.node(RegexAst.ASSERT, kind);
    }

    private int escape() {
        if (pos >= text.length()) throw new RegexRefusal("trailing backslash");
        char c = text.charAt(pos++);
        switch (c) {
            case 'A':
                return assertion(RegexAst.BEGIN);
            case 'Z':
                return assertion(RegexAst.END_Z);
            case 'z':
                return assertion(RegexAst.END);
            default:
                int[] pairs = predefined(c);
                if (pairs != null) return ast.addClassNode(pairs, Character.isUpperCase(c));
                return literal(escapedUnit(c));
        }
    }

    private int escapedUnit(char c) {
        switch (c) {
            case 't': return '\t';
            case 'n': return '\n';
            case 'r': return '\r';
            case 'f': return '\f';
            case 'a': return 0x07;
            case 'e': return 0x1B;
            case 'x': return hex(2);
            case 'u': return hex(4);
            default:
                if (Character.isLetterOrDigit(c)) throw new RegexRefusal("escape \\" + c);
                return c;
        }
    }

    private int hex(int digits) {
        if (pos + digits > text.length()) throw new RegexRefusal("hex");
        String body = text.substring(pos, pos + digits);
        if (!body.chars().allMatch(ch -> Character.digit(ch, 16) >= 0)) throw new RegexRefusal("hex");
        pos += digits;
        return Integer.parseInt(body, 16);
    }

    private int literal(int unit) {
        if (unit >= Character.MIN_SURROGATE && unit <= Character.MAX_SURROGATE) {
            throw new RegexRefusal("surrogate");
        }
        return ast.addClassNode(new int[] {unit, unit}, false);
    }

    private int charClass() {
        boolean negate = peek('^');
        if (negate) pos++;
        int[] pairs = new int[16];
        int count = 0;
        boolean first = true;
        while (true) {
            if (pos >= text.length()) throw new RegexRefusal("unclosed class");
            char c = text.charAt(pos);
            if (c == ']') {
                if (first) throw new RegexRefusal("leading ]");
                break;
            }
            if (c == '[' || text.startsWith("&&", pos)) throw new RegexRefusal("nested class");
            pairs = Arrays.copyOf(pairs, Math.max(pairs.length, 2 * count + 16));
            count = classItem(pairs, count);
            first = false;
        }
        pos++;
        return ast.node(RegexAst.CLASS, ast.addClass(pairs, count, negate));
    }

    private int classItem(int[] pairs, int count) {
        int[] set = classEscape();
        if (set != null) return union(pairs, count, set);
        int lo = classUnit();
        int hi = lo;
        if (peek('-') && pos + 1 < text.length() && text.charAt(pos + 1) != ']') {
            pos++;
            if (classEscapePeek()) throw new RegexRefusal("range to class");
            hi = classUnit();
            if (hi < lo) throw new RegexRefusal("range order");
        }
        pairs[2 * count] = lo;
        pairs[2 * count + 1] = hi;
        return count + 1;
    }

    private boolean classEscapePeek() {
        return peek('\\') && pos + 1 < text.length() && predefined(text.charAt(pos + 1)) != null;
    }

    private int[] classEscape() {
        if (!classEscapePeek()) return null;
        char c = text.charAt(pos + 1);
        pos += 2;
        int[] base = predefined(c);
        return Character.isUpperCase(c) ? RegexSyntax.complement(base, 0xFFFF) : base;
    }

    private static int union(int[] pairs, int count, int[] set) {
        System.arraycopy(set, 0, pairs, 2 * count, set.length);
        return count + set.length / 2;
    }

    private int classUnit() {
        char c = text.charAt(pos++);
        int unit = c;
        if (c == '\\') {
            if (pos >= text.length()) throw new RegexRefusal("class escape");
            char e = text.charAt(pos++);
            if (e == 'Q') throw new RegexRefusal("quote in class");
            unit = escapedUnit(e);
        }
        if (unit >= Character.MIN_SURROGATE && unit <= Character.MAX_SURROGATE) {
            throw new RegexRefusal("surrogate");
        }
        return unit;
    }

    private boolean peek(char c) {
        return pos < text.length() && text.charAt(pos) == c;
    }
}
