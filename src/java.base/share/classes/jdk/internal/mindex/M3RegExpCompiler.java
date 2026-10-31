// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
//
// Lineage (TARGET_OWNED_PORT, leaf R6-AUTOMATON, run 20261017-synexia-to-public):
//   Structure from Apache Lucene 9.10.0 org.apache.lucene.util.automaton.RegExp (git blob
//   5ba8b957c326b62ef0fdd798bedf4f2d14ccf660; recursive descent union -> concatenation -> repetition -> simple
//   expression, leaf-to-automaton reduction with minimization after every composite node, repetition work guard) and
//   the code-point -> code-unit range splitting idea of UTF32ToUTF8 (git blob fac6b7e636fe05118968d046886cb993360ad2e8,
//   pattern only: UTF-16 target instead of UTF-8), both vendored unchanged in M3jdk21 at m3/vendor/third_party/lucene-core
//   (BLOB_EQUAL to upstream tag 695c0ac84508438302cd346a812cfa2fdc5a10df), Apache License, Version 2.0.
//   Modifications: the Lucene syntax (~ & # @ <..> "...") is replaced by the regular subset of java.util.regex.Pattern
//   syntax (the semantic authority); no RegExp node tree is retained; anchors are admitted only where they are
//   no-ops for matches; everything else is refused so the regex ladder descends.
//
//   RegExp derives from dk.brics.automaton and carries its notice:
//
//   dk.brics.automaton
//   Copyright (c) 2001-2009 Anders Moeller
//   All rights reserved.
//
//   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//   following conditions are met:
//   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
//      following disclaimer.
//   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//      following disclaimer in the documentation and/or other materials provided with the distribution.
//   3. The name of the author may not be used to endorse or promote products derived from this software without
//      specific prior written permission.
//
//   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//   THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
//   POSSIBILITY OF SUCH DAMAGE.
package jdk.internal.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compiles the regular subset of java.util.regex syntax (flags == 0) to minimized UTF-16 unit DFAs. Admitted: literals
 * and escapes, {@code .}, classes with ranges and negation, ASCII predefined and POSIX classes, alternation, capturing and
 * non-capturing groups, greedy and lazy quantifiers, a leading {@code ^} and a trailing {@code $}. Refused (ladder
 * descends): backreferences, lookaround, possessive/atomic constructs, inline flags, word boundaries, nested classes and
 * intersections, Unicode categories, lone surrogates, compile flags. The stock engine stays the authority; results are
 * candidates. Hook point (not wired here): java.util.regex.Pattern.compile beside M3TQ / M3PatternQuery.
 */
public final class M3RegExpCompiler {
    private static final int MAX_CODE_POINT = Character.MAX_CODE_POINT;
    private static final int MAX_DEPTH = 64;
    private static final int[] DOT = {0, 9, 11, 12, 14, 0x84, 0x86, 0x2027, 0x202A, MAX_CODE_POINT};
    private static final int[] DIGIT = {'0', '9'};
    private static final int[] SPACE = {9, 13, ' ', ' '};
    private static final int[] WORD = {'0', '9', 'A', 'Z', '_', '_', 'a', 'z'};
    private static final int[] PUNCT = {'!', '/', ':', '@', '[', '`', '{', '~'};
    private static final int[] ALPHA = {'A', 'Z', 'a', 'z'};
    private static final int[] UPPER = {'A', 'Z'};
    private static final int[] LOWER = {'a', 'z'};
    private static final int[] ALNUM = {'0', '9', 'A', 'Z', 'a', 'z'};

    private final String p;
    private final int workLimit;
    private int pos;
    private int depth;
    private int startAnchorDepth = -1;
    private boolean anchorStart;
    private boolean anchorEnd;
    private boolean consumedAny;
    private final boolean[] altSeen = new boolean[MAX_DEPTH + 1];

    private M3RegExpCompiler(String pattern, int workLimit) {
        this.p = pattern;
        this.workLimit = workLimit;
    }

    /** Refusal of a construct outside the admitted subset; {@link #getMessage()} is the reason code. */
    public static final class Unsupported extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Unsupported(String reason) {
            super(reason, null, false, false);
        }
    }

    /** Compiles with the default work limit; never null (refusals are carried by {@link Compiled#refusal()}). */
    public static Compiled compile(String pattern, int flags) {
        return compile(pattern, flags, M3AutomatonOps.DEFAULT_WORK_LIMIT);
    }

    /** Compiles with an explicit determinization work limit. */
    public static Compiled compile(String pattern, int flags, int workLimit) {
        if (flags != 0) {
            return new Compiled(pattern, "FLAGS", null, null, null, false, false);
        }
        try {
            M3RegExpCompiler c = new M3RegExpCompiler(pattern, workLimit);
            M3Automaton a = c.parseUnion();
            if (c.pos < pattern.length()) {
                throw new Unsupported("UNBALANCED");
            }
            M3RunTable match = M3RunTable.of(a, workLimit);
            M3RunTable find = c.anchorStart ? null : optionalTable(
                    M3AutomatonOps.concatenate(M3AutomatonOps.makeAnyString(), a), workLimit);
            M3RunTable reverse = optionalTable(M3AutomatonOps.reverse(a), workLimit);
            return new Compiled(pattern, null, match, find, reverse, c.anchorStart, c.anchorEnd);
        } catch (Unsupported e) {
            return new Compiled(pattern, e.getMessage(), null, null, null, false, false);
        } catch (M3AutomatonOps.TooComplex e) {
            return new Compiled(pattern, "TOO_COMPLEX", null, null, null, false, false);
        } catch (M3RunTable.StateCap e) {
            return new Compiled(pattern, "STATE_CAP", null, null, null, false, false);
        }
    }

    private static M3RunTable optionalTable(M3Automaton a, int workLimit) {
        try {
            return M3RunTable.of(a, workLimit);
        } catch (M3AutomatonOps.TooComplex | M3RunTable.StateCap e) {
            return null;
        }
    }

    /** Result of a compilation: admitted tables or a refusal reason. */
    public static final class Compiled {
        private final String pattern;
        private final String refusal;
        private final M3RunTable match;
        private final M3RunTable find;
        private final M3RunTable reverse;
        private final boolean anchorStart;
        private final boolean anchorEnd;

        Compiled(String pattern, String refusal, M3RunTable match, M3RunTable find, M3RunTable reverse,
                boolean anchorStart, boolean anchorEnd) {
            this.pattern = pattern;
            this.refusal = refusal;
            this.match = match;
            this.find = find;
            this.reverse = reverse;
            this.anchorStart = anchorStart;
            this.anchorEnd = anchorEnd;
        }

        /** The source pattern. */
        public String pattern() {
            return pattern;
        }

        /** True when tables exist. */
        public boolean admitted() {
            return match != null;
        }

        /** Refusal reason code, or null when admitted. */
        public String refusal() {
            return refusal;
        }

        /** Whole-pattern table (state 0 initial). */
        public M3RunTable matchTable() {
            return match;
        }

        /** Unanchored-prefix table ({@code .*p}), or null. */
        public M3RunTable findTable() {
            return find;
        }

        /** Table of the reversed language, or null. */
        public M3RunTable reverseTable() {
            return reverse;
        }

        /** True when the pattern starts with an admitted {@code ^}. */
        public boolean anchorStart() {
            return anchorStart;
        }

        /** True when the pattern ends with an admitted {@code $}. */
        public boolean anchorEnd() {
            return anchorEnd;
        }

        /** Candidate for {@code Matcher.matches()}. */
        public int matches(CharSequence text) {
            return match == null ? M3RunTable.UNSUPPORTED : match.matches(text);
        }

        /** Candidate for {@code Matcher.lookingAt()}. */
        public int lookingAt(CharSequence text) {
            return match == null ? M3RunTable.UNSUPPORTED : endsInProfile(match.acceptProfile(text), text);
        }

        /** Candidate for the first {@code Matcher.find()} (existence only). */
        public int find(CharSequence text) {
            if (match == null) {
                return M3RunTable.UNSUPPORTED;
            }
            if (anchorStart) {
                return lookingAt(text);
            }
            if (find == null || (M3RunTable.surrogateFlags(text, 0, text.length()) & M3RunTable.HAS_SURROGATE) != 0) {
                return M3RunTable.UNSUPPORTED;
            }
            return endsInProfile(find.acceptProfile(text), text);
        }

        /**
         * Leftmost match window {@code [start, end)} for the stock engine's capture pass (forward DFA end, reversed DFA
         * start, RE2 style; R6_DESIGN section 5). Not received yet.
         */
        public long leftmostWindow(M3ChunkCursor cursor) {
            throw new M3NotYetReceived("M3RegExpCompiler.Compiled.leftmostWindow R6_DESIGN section 5");
        }

        private int endsInProfile(long[] profile, CharSequence text) {
            if (profile == null) {
                return M3RunTable.UNSUPPORTED;
            }
            int n = text.length();
            if (!anchorEnd) {
                for (long word : profile) {
                    if (word != 0) {
                        return M3RunTable.TRUE;
                    }
                }
                return M3RunTable.FALSE;
            }
            if (bit(profile, n)) {
                return M3RunTable.TRUE;
            }
            if (n >= 1 && bit(profile, n - 1) && isLineTerminator(text.charAt(n - 1))
                    && !(text.charAt(n - 1) == '\n' && n >= 2 && text.charAt(n - 2) == '\r')) {
                return M3RunTable.TRUE;
            }
            if (n >= 2 && bit(profile, n - 2) && text.charAt(n - 2) == '\r' && text.charAt(n - 1) == '\n') {
                return M3RunTable.TRUE;
            }
            return M3RunTable.FALSE;
        }

        private static boolean bit(long[] profile, int k) {
            return (profile[k >>> 6] & (1L << k)) != 0;
        }

        private static boolean isLineTerminator(char c) {
            return c == '\n' || c == '\r' || c == '\u0085' || (c | 1) == '\u2029';
        }
    }

    // ---- recursive descent (Lucene RegExp structure, java.util.regex syntax) ----

    private M3Automaton parseUnion() {
        List<M3Automaton> branches = new ArrayList<>();
        branches.add(parseConcat());
        while (more() && peekUnit() == '|') {
            pos++;
            altSeen[depth] = true;
            if (startAnchorDepth >= 0 && depth <= startAnchorDepth) {
                throw new Unsupported("ANCHOR_ALTERNATION");
            }
            branches.add(parseConcat());
        }
        if (branches.size() == 1) {
            return branches.get(0);
        }
        return minimize(M3AutomatonOps.union(branches.toArray(new M3Automaton[0])));
    }

    private M3Automaton parseConcat() {
        List<M3Automaton> parts = new ArrayList<>();
        while (more() && peekUnit() != '|' && peekUnit() != ')') {
            M3Automaton part = parseRepeat();
            if (part != null) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return M3AutomatonOps.makeEmptyString();
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return minimize(M3AutomatonOps.concatenate(parts.toArray(new M3Automaton[0])));
    }

    private M3Automaton parseRepeat() {
        int anchorBefore = startAnchorDepth;
        int depthBefore = depth;
        M3Automaton a = parseAtom();
        boolean quantified = more() && isQuantifierStart(peekUnit());
        if (a == null) {
            if (quantified) {
                throw new Unsupported("ANCHOR_QUANTIFIED");
            }
            return null;
        }
        if (!quantified) {
            return a;
        }
        if (startAnchorDepth >= 0 && (anchorBefore < 0 || startAnchorDepth > depthBefore)) {
            throw new Unsupported("ANCHOR_QUANTIFIED");
        }
        a = quantifier(a);
        if (more() && peekUnit() == '?') {
            pos++;
        } else if (more() && peekUnit() == '+') {
            throw new Unsupported("POSSESSIVE");
        }
        if (more() && isQuantifierStart(peekUnit())) {
            throw new Unsupported("DOUBLE_QUANTIFIER");
        }
        return a;
    }

    private M3Automaton quantifier(M3Automaton a) {
        char q = p.charAt(pos++);
        switch (q) {
            case '?':
                return minimize(M3AutomatonOps.optional(a));
            case '*':
                return minimize(M3AutomatonOps.repeat(a));
            case '+':
                return minimize(M3AutomatonOps.repeat(a, 1));
            default:
                break;
        }
        int min = number();
        int max = min;
        if (more() && peekUnit() == ',') {
            pos++;
            max = (more() && isDigit(peekUnit())) ? number() : -1;
        }
        if (!more() || p.charAt(pos++) != '}') {
            throw new Unsupported("BAD_REPETITION");
        }
        if (max != -1 && min > max) {
            throw new Unsupported("BAD_REPETITION");
        }
        long guard = (long) Math.max(0, a.getNumStates() - 1) * Math.max(min, max);
        if (guard > workLimit) {
            throw new M3AutomatonOps.TooComplex(a.getNumStates(), a.getNumTransitions(), workLimit);
        }
        if (max == -1) {
            return minimize(M3AutomatonOps.repeat(a, min));
        }
        return minimize(M3AutomatonOps.repeat(a, min, max));
    }

    private int number() {
        int start = pos;
        while (more() && isDigit(peekUnit())) {
            pos++;
        }
        if (start == pos || pos - start > 6) {
            throw new Unsupported("BAD_REPETITION");
        }
        return Integer.parseInt(p, start, pos, 10);
    }

    /** Returns the atom's automaton, or null for a zero-width anchor. */
    private M3Automaton parseAtom() {
        int cp = p.codePointAt(pos);
        pos += Character.charCount(cp);
        switch (cp) {
            case '(':
                return group();
            case ')':
                throw new Unsupported("UNBALANCED");
            case '[':
                consumedAny = true;
                return set(parseClass());
            case '.':
                consumedAny = true;
                return set(DOT);
            case '^':
                startAnchor();
                return null;
            case '$':
                endAnchor();
                return null;
            case '\\':
                consumedAny = true;
                return escapeAtom();
            case '*':
            case '+':
            case '?':
            case '{':
                throw new Unsupported("DANGLING");
            default:
                consumedAny = true;
                return literal(cp);
        }
    }

    private M3Automaton group() {
        if (more() && peekUnit() == '?') {
            if (pos + 1 < p.length() && p.charAt(pos + 1) == ':') {
                pos += 2;
            } else {
                throw new Unsupported("GROUP_KIND");
            }
        }
        if (++depth > MAX_DEPTH) {
            throw new Unsupported("DEPTH");
        }
        altSeen[depth] = false;
        M3Automaton a = parseUnion();
        if (!more() || p.charAt(pos) != ')') {
            throw new Unsupported("UNCLOSED_GROUP");
        }
        pos++;
        depth--;
        if (startAnchorDepth > depth) {
            startAnchorDepth = depth;
        }
        return a;
    }

    private void startAnchor() {
        if (consumedAny) {
            throw new Unsupported("ANCHOR_POSITION");
        }
        for (int d = 0; d <= depth; d++) {
            if (altSeen[d]) {
                throw new Unsupported("ANCHOR_ALTERNATION");
            }
        }
        startAnchorDepth = startAnchorDepth < 0 ? depth : Math.min(startAnchorDepth, depth);
        anchorStart = true;
    }

    private void endAnchor() {
        for (int i = pos; i < p.length(); i++) {
            if (p.charAt(i) != ')') {
                throw new Unsupported("ANCHOR_POSITION");
            }
        }
        for (int d = 0; d <= depth; d++) {
            if (altSeen[d]) {
                throw new Unsupported("ANCHOR_ALTERNATION");
            }
        }
        anchorEnd = true;
    }

    private M3Automaton escapeAtom() {
        if (!more()) {
            throw new Unsupported("TRAILING_ESCAPE");
        }
        int e = p.codePointAt(pos);
        if (e == 'Q') {
            pos++;
            int end = p.indexOf("\\E", pos);
            String quoted = end < 0 ? p.substring(pos) : p.substring(pos, end);
            pos = end < 0 ? p.length() : end + 2;
            return literalString(quoted);
        }
        int[] predefined = predefinedSet();
        if (predefined != null) {
            return set(predefined);
        }
        return literal(escapeCodePoint());
    }

    /** Parses \d \D \s \S \w \W \p{..} \P{..} at pos (after the backslash) or returns null without moving. */
    private int[] predefinedSet() {
        char e = p.charAt(pos);
        int[] base;
        switch (e) {
            case 'd': case 'D': base = DIGIT; break;
            case 's': case 'S': base = SPACE; break;
            case 'w': case 'W': base = WORD; break;
            case 'p': case 'P': base = null; break;
            default: return null;
        }
        pos++;
        if (base == null) {
            base = posixClass();
        }
        boolean negate = Character.isUpperCase(e);
        return negate ? complement(base) : base;
    }

    private int[] posixClass() {
        if (!more() || p.charAt(pos) != '{') {
            throw new Unsupported("UNICODE_CATEGORY");
        }
        int end = p.indexOf('}', pos);
        if (end < 0) {
            throw new Unsupported("BAD_PROPERTY");
        }
        String name = p.substring(pos + 1, end);
        pos = end + 1;
        switch (name) {
            case "Punct": return PUNCT;
            case "Alpha": return ALPHA;
            case "Digit": return DIGIT;
            case "Upper": return UPPER;
            case "Lower": return LOWER;
            case "Alnum": return ALNUM;
            case "Space": return SPACE;
            default: throw new Unsupported("UNICODE_CATEGORY");
        }
    }

    /** Parses a single-code-point escape at pos (after the backslash). */
    private int escapeCodePoint() {
        int e = p.codePointAt(pos);
        pos += Character.charCount(e);
        switch (e) {
            case 't': return '\t';
            case 'n': return '\n';
            case 'r': return '\r';
            case 'f': return '\f';
            case 'a': return 7;
            case 'e': return 27;
            case '0': return octal();
            case 'x': return hexEscape();
            case 'u': return unicodeEscape();
            case 'c':
                if (!more()) {
                    throw new Unsupported("BAD_CONTROL");
                }
                return p.charAt(pos++) ^ 64;
            default:
                break;
        }
        if ((e >= 'a' && e <= 'z') || (e >= 'A' && e <= 'Z') || (e >= '0' && e <= '9')) {
            throw new Unsupported("ESCAPE");
        }
        return e;
    }

    private int octal() {
        int value = 0;
        int digits = 0;
        while (more() && digits < 3 && p.charAt(pos) >= '0' && p.charAt(pos) <= '7') {
            int next = value * 8 + (p.charAt(pos) - '0');
            if (next > 0377) {
                break;
            }
            value = next;
            pos++;
            digits++;
        }
        if (digits == 0) {
            throw new Unsupported("BAD_OCTAL");
        }
        return value;
    }

    private int hexEscape() {
        if (more() && p.charAt(pos) == '{') {
            int end = p.indexOf('}', pos);
            if (end < 0 || end == pos + 1 || end - pos > 9) {
                throw new Unsupported("BAD_HEX");
            }
            int value = hex(pos + 1, end);
            pos = end + 1;
            if (value > MAX_CODE_POINT) {
                throw new Unsupported("BAD_HEX");
            }
            return value;
        }
        if (pos + 2 > p.length()) {
            throw new Unsupported("BAD_HEX");
        }
        int value = hex(pos, pos + 2);
        pos += 2;
        return value;
    }

    private int unicodeEscape() {
        if (pos + 4 > p.length()) {
            throw new Unsupported("BAD_UNICODE");
        }
        int value = hex(pos, pos + 4);
        pos += 4;
        if (Character.isHighSurrogate((char) value) && pos + 6 <= p.length()
                && p.charAt(pos) == '\\' && p.charAt(pos + 1) == 'u') {
            int low = hex(pos + 2, pos + 6);
            if (Character.isLowSurrogate((char) low)) {
                pos += 6;
                return Character.toCodePoint((char) value, (char) low);
            }
        }
        return value;
    }

    private int hex(int from, int to) {
        int value = 0;
        for (int i = from; i < to; i++) {
            int d = Character.digit(p.charAt(i), 16);
            if (d < 0) {
                throw new Unsupported("BAD_HEX");
            }
            value = value * 16 + d;
        }
        return value;
    }

    /** Parses a class body after '[' through the closing ']'; returns sorted disjoint code-point ranges. */
    private int[] parseClass() {
        boolean negate = false;
        if (more() && p.charAt(pos) == '^') {
            negate = true;
            pos++;
        }
        if (more() && p.charAt(pos) == ']') {
            throw new Unsupported("CLASS_LEADING_BRACKET");
        }
        int[] acc = new int[0];
        while (true) {
            if (!more()) {
                throw new Unsupported("UNCLOSED_CLASS");
            }
            char c = p.charAt(pos);
            if (c == ']') {
                pos++;
                break;
            }
            if (c == '[') {
                throw new Unsupported("NESTED_CLASS");
            }
            if (c == '&' && pos + 1 < p.length() && p.charAt(pos + 1) == '&') {
                throw new Unsupported("CLASS_INTERSECTION");
            }
            if (c == '\\') {
                pos++;
                if (!more()) {
                    throw new Unsupported("TRAILING_ESCAPE");
                }
                if (p.charAt(pos) == 'Q') {
                    throw new Unsupported("CLASS_QUOTE");
                }
                int[] predefined = predefinedSet();
                if (predefined != null) {
                    acc = union(acc, predefined);
                    continue;
                }
                acc = union(acc, rangeAfter(escapeCodePoint()));
                continue;
            }
            int cp = p.codePointAt(pos);
            pos += Character.charCount(cp);
            acc = union(acc, rangeAfter(cp));
        }
        return negate ? complement(acc) : acc;
    }

    /** Completes a class item starting with {@code lo}: either a range lo-hi or the single code point. */
    private int[] rangeAfter(int lo) {
        if (pos + 1 < p.length() && p.charAt(pos) == '-' && p.charAt(pos + 1) != ']') {
            pos++;
            int hi;
            char c = p.charAt(pos);
            if (c == '[') {
                throw new Unsupported("NESTED_CLASS");
            }
            if (c == '\\') {
                pos++;
                if (!more()) {
                    throw new Unsupported("TRAILING_ESCAPE");
                }
                char e = p.charAt(pos);
                if (e == 'd' || e == 'D' || e == 's' || e == 'S' || e == 'w' || e == 'W' || e == 'p' || e == 'P'
                        || e == 'Q') {
                    throw new Unsupported("BAD_RANGE");
                }
                hi = escapeCodePoint();
            } else {
                hi = p.codePointAt(pos);
                pos += Character.charCount(hi);
            }
            if (hi < lo) {
                throw new Unsupported("BAD_RANGE");
            }
            return new int[] {lo, hi};
        }
        return new int[] {lo, lo};
    }

    // ---- code-point sets -> UTF-16 unit automata (UTF32ToUTF8 idea, UTF-16 target) ----

    private M3Automaton literal(int cp) {
        if (cp >= Character.MIN_SURROGATE && cp <= Character.MAX_SURROGATE) {
            throw new Unsupported("SURROGATE_LITERAL");
        }
        return M3AutomatonOps.makeUnits(new String(Character.toChars(cp)));
    }

    private M3Automaton literalString(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                throw new Unsupported("SURROGATE_LITERAL");
            }
        }
        return M3AutomatonOps.makeUnits(s);
    }

    /** Unit automaton of a code-point set; lone surrogates are never members. */
    private M3Automaton set(int[] ranges) {
        int[] bmp = intersect(ranges, new int[] {0, Character.MIN_SURROGATE - 1, Character.MAX_SURROGATE + 1, 0xFFFF});
        int[] sup = intersect(ranges, new int[] {0x10000, MAX_CODE_POINT});
        List<int[]> groups = new ArrayList<>();
        for (int i = 0; i < sup.length; i += 2) {
            splitSupplementary(sup[i], sup[i + 1], groups);
        }
        if (bmp.length == 0 && groups.isEmpty()) {
            return M3AutomatonOps.makeEmpty();
        }
        M3Automaton a = new M3Automaton();
        int start = a.createState();
        int accept = a.createState();
        a.setAccept(accept, true);
        int[] mids = new int[groups.size()];
        for (int g = 0; g < mids.length; g++) {
            mids[g] = a.createState();
        }
        for (int i = 0; i < bmp.length; i += 2) {
            a.addTransition(start, accept, bmp[i], bmp[i + 1]);
        }
        for (int g = 0; g < mids.length; g++) {
            int[] group = groups.get(g);
            a.addTransition(start, mids[g], group[0], group[1]);
        }
        for (int g = 0; g < mids.length; g++) {
            int[] group = groups.get(g);
            a.addTransition(mids[g], accept, group[2], group[3]);
        }
        a.finishState();
        return minimize(a);
    }

    /** Splits supplementary [lo, hi] into (high range, low range) groups. */
    private static void splitSupplementary(int lo, int hi, List<int[]> groups) {
        int hLo = Character.highSurrogate(lo);
        int hHi = Character.highSurrogate(hi);
        int lLo = Character.lowSurrogate(lo);
        int lHi = Character.lowSurrogate(hi);
        if (hLo == hHi) {
            groups.add(new int[] {hLo, hLo, lLo, lHi});
            return;
        }
        groups.add(new int[] {hLo, hLo, lLo, Character.MAX_LOW_SURROGATE});
        if (hLo + 1 <= hHi - 1) {
            groups.add(new int[] {hLo + 1, hHi - 1, Character.MIN_LOW_SURROGATE, Character.MAX_LOW_SURROGATE});
        }
        groups.add(new int[] {hHi, hHi, Character.MIN_LOW_SURROGATE, lHi});
    }

    static int[] union(int[] a, int[] b) {
        int[] all = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        int pairs = all.length / 2;
        long[] packed = new long[pairs];
        for (int i = 0; i < pairs; i++) {
            packed[i] = ((long) all[2 * i] << 32) | (all[2 * i + 1] & 0xFFFFFFFFL);
        }
        Arrays.sort(packed);
        int[] out = new int[all.length];
        int n = 0;
        for (long range : packed) {
            int lo = (int) (range >>> 32);
            int hi = (int) range;
            if (n > 0 && lo <= out[n - 1] + 1) {
                out[n - 1] = Math.max(out[n - 1], hi);
            } else {
                out[n++] = lo;
                out[n++] = hi;
            }
        }
        return Arrays.copyOf(out, n);
    }

    static int[] complement(int[] ranges) {
        int[] norm = union(ranges, new int[0]);
        int[] out = new int[norm.length + 2];
        int n = 0;
        int next = 0;
        for (int i = 0; i < norm.length; i += 2) {
            if (norm[i] > next) {
                out[n++] = next;
                out[n++] = norm[i] - 1;
            }
            next = norm[i + 1] + 1;
        }
        if (next <= MAX_CODE_POINT) {
            out[n++] = next;
            out[n++] = MAX_CODE_POINT;
        }
        return Arrays.copyOf(out, n);
    }

    static int[] intersect(int[] a, int[] b) {
        int[] x = union(a, new int[0]);
        int[] y = union(b, new int[0]);
        int[] out = new int[x.length + y.length];
        int n = 0;
        int i = 0;
        int j = 0;
        while (i < x.length && j < y.length) {
            int lo = Math.max(x[i], y[j]);
            int hi = Math.min(x[i + 1], y[j + 1]);
            if (lo <= hi) {
                out[n++] = lo;
                out[n++] = hi;
            }
            if (x[i + 1] < y[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return Arrays.copyOf(out, n);
    }

    private M3Automaton minimize(M3Automaton a) {
        return M3AutomatonOps.minimize(a, workLimit);
    }

    private boolean more() {
        return pos < p.length();
    }

    private char peekUnit() {
        return p.charAt(pos);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isQuantifierStart(char c) {
        return c == '?' || c == '*' || c == '+' || c == '{';
    }
}
