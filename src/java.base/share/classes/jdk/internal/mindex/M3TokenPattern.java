/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * Compiled token-sequence pattern: a hand-written parser for a subset of the Square T-Rex
 * language (grammar TRex.g4 at c84ddf5a, Apache-2.0, used as the specification only; no T-Rex
 * or ANTLR code is present) and the parsed tree held in primitive arena lanes.
 *
 * <p>Tokens: {@code [body]}, wildcard {@code []}, a bare or quoted word (exact text) and
 * {@code /re/} (regex on the spelling). Body atoms: {@code {key:value}}, {@code {key:/re/}},
 * {@code {key>n}} with {@code > >= < <= == !=}, {@code {!key}} (absent), {@code !atom},
 * {@code (body)}; {@code &} and {@code |} inside a body are left-branching with equal
 * precedence. Sequence: juxtaposition, {@code |} (lowest, right-branching), postfix
 * {@code ? * + {n} {n,} {n,m}} each optionally reluctant, {@code ( )} numbered capture,
 * {@code (?<name> )} named capture, {@code ^} and {@code $}. Keys {@code text word default}
 * mean the spelling; {@code kind lexeme binding flags} read the index lanes; any other key is an
 * attribute column. Refused rather than approximated: pattern-level {@code &}, variables and
 * the legacy {@code (?$name ...)} capture form.
 */
public final class M3TokenPattern {
    /** Exact text predicates compare case-insensitively (regex predicates carry their own flags). */
    public static final int CASE_INSENSITIVE = 1;
    public static final int MAX_PATTERN_CHARS = 4096;
    public static final int MAX_DEPTH = 32;
    public static final int MAX_GROUPS = 24;
    public static final int MAX_REPEAT = 4096;

    public static final byte N_TOKEN = 1, N_ANY = 2, N_SEQ = 3, N_ALT = 4;
    public static final byte N_REPEAT = 5, N_GROUP = 6, N_BOL = 7, N_EOL = 8;
    public static final byte P_ROW = 1, P_NOT = 2, P_AND = 3, P_OR = 4;
    public static final byte K_TEXT = 1, K_KIND = 2, K_LEXEME = 3, K_BINDING = 4, K_FLAGS = 5, K_ATTR = 6;
    public static final byte O_EQ = 1, O_REGEX = 2, O_ABSENT = 3, O_GT = 4, O_GE = 5;
    public static final byte O_LT = 6, O_LE = 7, O_NUM_EQ = 8, O_NE = 9;

    private final String source;
    private final int flags;
    private final String root;
    private final int rootNode;
    private final Lanes lanes;

    private M3TokenPattern(String source, int flags, String root, int rootNode, Lanes lanes) {
        this.source = source;
        this.flags = flags;
        this.root = root;
        this.rootNode = rootNode;
        this.lanes = lanes;
    }

    public static M3TokenPattern compile(String source) {
        return compile(source, 0);
    }

    /** @throws PatternSyntaxException for malformed, unsupported or over-budget patterns */
    public static M3TokenPattern compile(String source, int flags) {
        Objects.requireNonNull(source, "source");
        if ((flags & ~CASE_INSENSITIVE) != 0) {
            throw new IllegalArgumentException("unknown token pattern flags");
        }
        if (source.length() > MAX_PATTERN_CHARS) {
            throw new PatternSyntaxException("pattern exceeds " + MAX_PATTERN_CHARS + " chars", source, 0);
        }
        Parser parser = new Parser(source);
        int top = parser.parsePattern();
        return new M3TokenPattern(source, flags, rootOf(source, flags), top, parser.trim());
    }

    private static String rootOf(String source, int flags) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = ("M3TKRX1\n" + flags + "\n" + source).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 unavailable", missing);
        }
    }

    public String source() { return source; }
    public int flags() { return flags; }
    /** SHA-256 hex of the tagged source and flags: the predicate-cache key component. */
    public String patternRoot() { return root; }
    public int rootNode() { return rootNode; }
    public int nodeCount() { return lanes.nodes; }
    public byte nodeOp(int n) { return lanes.op[Objects.checkIndex(n, lanes.nodes)]; }
    /** TOKEN: expression; SEQ: first child; ALT: left; REPEAT and GROUP: child. */
    public int nodeX(int n) { return lanes.x[Objects.checkIndex(n, lanes.nodes)]; }
    /** SEQ: child count; ALT: right; REPEAT: min; GROUP: group number. */
    public int nodeY(int n) { return lanes.y[Objects.checkIndex(n, lanes.nodes)]; }
    /** REPEAT: max, -1 when unbounded. */
    public int nodeZ(int n) { return lanes.z[Objects.checkIndex(n, lanes.nodes)]; }
    /** Next sibling inside a SEQ, -1 at the end. */
    public int nodeNext(int n) { return lanes.next[Objects.checkIndex(n, lanes.nodes)]; }
    public boolean nodeReluctant(int n) { return lanes.lazy[Objects.checkIndex(n, lanes.nodes)] != 0; }
    public int exprCount() { return lanes.exprs; }
    public byte exprOp(int e) { return lanes.eop[Objects.checkIndex(e, lanes.exprs)]; }
    public int exprA(int e) { return lanes.ea[Objects.checkIndex(e, lanes.exprs)]; }
    public int exprB(int e) { return lanes.eb[Objects.checkIndex(e, lanes.exprs)]; }
    public int rowCount() { return lanes.rows; }
    public byte rowKey(int r) { return lanes.rkey[Objects.checkIndex(r, lanes.rows)]; }
    public byte rowOp(int r) { return lanes.rop[Objects.checkIndex(r, lanes.rows)]; }
    public long rowNumber(int r) { return lanes.rnum[Objects.checkIndex(r, lanes.rows)]; }
    /** Literal value (text, regex source or raw kind/attribute value), or null. */
    public String rowLiteral(int r) { return literal(lanes.rlit[Objects.checkIndex(r, lanes.rows)]); }
    /** Attribute column name for K_ATTR rows, else null. */
    public String rowAttribute(int r) { return literal(lanes.rattr[Objects.checkIndex(r, lanes.rows)]); }
    public int groupCount() { return lanes.groups; }
    /** Name of group g (1-based), or null for an unnamed group. */
    public String groupName(int g) { return lanes.names[Objects.checkIndex(g, lanes.groups + 1)]; }

    /** First group carrying the name, or -1. */
    public int groupIndex(String name) {
        Objects.requireNonNull(name, "name");
        for (int g = 1; g <= lanes.groups; g++) {
            if (name.equals(lanes.names[g])) {
                return g;
            }
        }
        return -1;
    }

    private String literal(int slot) {
        return slot < 0 ? null : lanes.lits[slot];
    }

    /** Canonical rendering of the parsed tree (diagnostics and tests). */
    public String describe() {
        StringBuilder out = new StringBuilder();
        describeNode(rootNode, out);
        return out.toString();
    }

    private void describeNode(int n, StringBuilder out) {
        switch (lanes.op[n]) {
            case N_TOKEN -> { out.append('['); describeExpr(lanes.x[n], out); out.append(']'); }
            case N_ANY -> out.append("[]");
            case N_BOL -> out.append('^');
            case N_EOL -> out.append('$');
            case N_SEQ -> describeSeq(n, out);
            case N_ALT -> describeBinary("alt(", lanes.x[n], lanes.y[n], out);
            case N_REPEAT -> describeRepeat(n, out);
            default -> describeGroup(n, out);
        }
    }

    private void describeSeq(int n, StringBuilder out) {
        out.append("seq(");
        for (int c = lanes.x[n]; c >= 0; c = lanes.next[c]) {
            describeNode(c, out);
            out.append(lanes.next[c] >= 0 ? " " : "");
        }
        out.append(')');
    }

    private void describeBinary(String head, int left, int right, StringBuilder out) {
        out.append(head);
        describeNode(left, out);
        out.append(", ");
        describeNode(right, out);
        out.append(')');
    }

    private void describeRepeat(int n, StringBuilder out) {
        out.append("rep{").append(lanes.y[n]).append(',').append(lanes.z[n]).append('}');
        out.append(lanes.lazy[n] != 0 ? "?(" : "(");
        describeNode(lanes.x[n], out);
        out.append(')');
    }

    private void describeGroup(int n, StringBuilder out) {
        int g = lanes.y[n];
        out.append('g').append(g);
        if (lanes.names[g] != null) {
            out.append('<').append(lanes.names[g]).append('>');
        }
        out.append('(');
        describeNode(lanes.x[n], out);
        out.append(')');
    }

    private void describeExpr(int e, StringBuilder out) {
        switch (lanes.eop[e]) {
            case P_ROW -> describeRow(lanes.ea[e], out);
            case P_NOT -> { out.append('!'); describeExpr(lanes.ea[e], out); }
            default -> {
                out.append('(');
                describeExpr(lanes.ea[e], out);
                out.append(lanes.eop[e] == P_AND ? "&" : "|");
                describeExpr(lanes.eb[e], out);
                out.append(')');
            }
        }
    }

    private static final String[] KEY_NAMES = {"?", "text", "kind", "lexeme", "binding", "flags", "attr"};
    private static final String[] OP_NAMES = {"?", "=", "~", "!", ">", ">=", "<", "<=", "==", "!="};

    private void describeRow(int r, StringBuilder out) {
        out.append('{');
        String key = lanes.rkey[r] == K_ATTR ? lanes.lits[lanes.rattr[r]] : KEY_NAMES[lanes.rkey[r]];
        byte op = lanes.rop[r];
        if (op == O_ABSENT) {
            out.append('!').append(key);
        } else if (op == O_EQ) {
            out.append(key).append("=\"").append(lanes.lits[lanes.rlit[r]]).append('"');
        } else if (op == O_REGEX) {
            out.append(key).append("=/").append(lanes.lits[lanes.rlit[r]]).append('/');
        } else {
            out.append(key).append(OP_NAMES[op]).append(lanes.rnum[r]);
        }
        out.append('}');
    }

    /** Primitive arena lanes; filled by the parser, then trimmed and never mutated. */
    private static final class Lanes {
        byte[] op; int[] x; int[] y; int[] z; int[] next; byte[] lazy; int nodes;
        byte[] eop; int[] ea; int[] eb; int exprs;
        byte[] rkey; byte[] rop; long[] rnum; int[] rlit; int[] rattr; int rows;
        String[] lits; int litCount;
        String[] names; int groups;

        Lanes(int length) {
            int nodeCap = 3 * length + 8;
            op = new byte[nodeCap]; x = new int[nodeCap]; y = new int[nodeCap];
            z = new int[nodeCap]; next = new int[nodeCap]; lazy = new byte[nodeCap];
            eop = new byte[nodeCap]; ea = new int[nodeCap]; eb = new int[nodeCap];
            int rowCap = length + 2;
            rkey = new byte[rowCap]; rop = new byte[rowCap]; rnum = new long[rowCap];
            rlit = new int[rowCap]; rattr = new int[rowCap];
            lits = new String[2 * length + 2];
            names = new String[MAX_GROUPS + 1];
        }

        Lanes trim() {
            op = Arrays.copyOf(op, nodes); x = Arrays.copyOf(x, nodes); y = Arrays.copyOf(y, nodes);
            z = Arrays.copyOf(z, nodes); next = Arrays.copyOf(next, nodes); lazy = Arrays.copyOf(lazy, nodes);
            eop = Arrays.copyOf(eop, exprs); ea = Arrays.copyOf(ea, exprs); eb = Arrays.copyOf(eb, exprs);
            rkey = Arrays.copyOf(rkey, rows); rop = Arrays.copyOf(rop, rows); rnum = Arrays.copyOf(rnum, rows);
            rlit = Arrays.copyOf(rlit, rows); rattr = Arrays.copyOf(rattr, rows);
            lits = Arrays.copyOf(lits, litCount);
            names = Arrays.copyOf(names, groups + 1);
            return this;
        }
    }

    /** Recursive-descent parser over the pattern characters (T-Rex grammar rules, by hand). */
    private static final class Parser {
        private static final String SPECIAL = "[]{}():&|!+*?,$^\"<>=";
        private final String s;
        private final Lanes l;
        private int at;
        private int depth;

        Parser(String source) {
            this.s = source;
            this.l = new Lanes(source.length());
        }

        Lanes trim() {
            return l.trim();
        }

        int parsePattern() {
            skip();
            if (eof()) {
                throw error("empty pattern");
            }
            int top = parseAlt();
            skip();
            if (!eof()) {
                throw error("unexpected '" + s.charAt(at) + "'");
            }
            return top;
        }

        private int parseAlt() {
            int left = parseSeq();
            skip();
            if (peek('&')) {
                throw error("pattern-level & (equal-length conjunction) is not supported");
            }
            if (!peek('|')) {
                return left;
            }
            at++;
            int right = parseAlt();
            return node(N_ALT, left, right, 0);
        }

        private int parseSeq() {
            int first = -1, last = -1, n = 0;
            while (true) {
                skip();
                if (eof() || peek(')') || peek('|') || peek('&')) {
                    break;
                }
                int q = parseQuantified();
                if (last < 0) { first = q; } else { l.next[last] = q; }
                last = q;
                n++;
            }
            if (n == 0) {
                throw error("empty sequence");
            }
            return n == 1 ? first : node(N_SEQ, first, n, 0);
        }

        private int parseQuantified() {
            int atom = parseAtom();
            while (true) {
                skip();
                if (eof()) {
                    return atom;
                }
                char c = s.charAt(at);
                if (c == '?') { at++; atom = repeat(atom, 0, 1); }
                else if (c == '*') { at++; atom = repeat(atom, 0, -1); }
                else if (c == '+') { at++; atom = repeat(atom, 1, -1); }
                else if (c == '{') { atom = parseBounds(atom); }
                else { return atom; }
            }
        }

        private int repeat(int child, int min, int max) {
            int n = node(N_REPEAT, child, min, max);
            if (peek('?')) {
                at++;
                l.lazy[n] = 1;
            }
            return n;
        }

        private int parseBounds(int child) {
            at++;
            int min = number();
            int max = min;
            if (peek(',')) {
                at++;
                max = peek('}') ? -1 : number();
            }
            expect('}');
            if (max >= 0 && max < min) {
                throw error("repeat bounds {" + min + "," + max + "} are reversed");
            }
            return repeat(child, min, max);
        }

        private int number() {
            int begin = at;
            while (!eof() && Character.isDigit(s.charAt(at)) && at - begin < 5) {
                at++;
            }
            if (begin == at) {
                throw error("expected a number");
            }
            int value = Integer.parseInt(s, begin, at, 10);
            if (value > MAX_REPEAT) {
                throw error("repeat bound exceeds " + MAX_REPEAT);
            }
            return value;
        }

        private int parseAtom() {
            skip();
            return switch (s.charAt(at)) {
                case '(' -> parseGroup();
                case '[' -> parseToken();
                case '^' -> { at++; yield node(N_BOL, 0, 0, 0); }
                case '$' -> parseDollar();
                default -> node(N_TOKEN, rowExpr(parseValueRow(K_TEXT, -1)), 0, 0);
            };
        }

        private int parseDollar() {
            at++;
            if (!eof() && isWordChar(s.charAt(at), false)) {
                throw error("variables ($NAME) are not supported");
            }
            return node(N_EOL, 0, 0, 0);
        }

        private int parseGroup() {
            at++;
            String name = null;
            if (s.startsWith("?$", at)) {
                throw error("legacy (?$name ...) capture is not supported; use (?<name> ...)");
            }
            if (s.startsWith("?<", at)) {
                at += 2;
                name = word("group name");
                expect('>');
            }
            if (l.groups == MAX_GROUPS) {
                throw error("more than " + MAX_GROUPS + " capture groups");
            }
            int g = ++l.groups;
            l.names[g] = name;
            enter();
            int inner = parseAlt();
            expect(')');
            depth--;
            return node(N_GROUP, inner, g, 0);
        }

        private int parseToken() {
            at++;
            skip();
            if (peek(']')) {
                at++;
                return node(N_ANY, 0, 0, 0);
            }
            int body = parseBody();
            expect(']');
            return node(N_TOKEN, body, 0, 0);
        }

        private int parseBody() {
            int left = parseBodyAtom();
            while (true) {
                skip();
                byte join = peek('&') ? P_AND : peek('|') ? P_OR : 0;
                if (join == 0) {
                    return left;
                }
                at++;
                left = expr(join, left, parseBodyAtom());
            }
        }

        private int parseBodyAtom() {
            skip();
            if (peek('!')) {
                return parseNegation();
            }
            if (peek('(')) {
                at++;
                enter();
                int inner = parseBody();
                expect(')');
                depth--;
                return inner;
            }
            if (!peek('{')) {
                return rowExpr(parseKeyValue(word("key")));
            }
            at++;
            skip();
            int row = peek('!') ? absentRow() : parseKeyValue(word("key"));
            expect('}');
            return rowExpr(row);
        }

        private int parseNegation() {
            at++;
            skip();
            if (peek('{') || peek('(') || peek('!')) {
                return expr(P_NOT, parseBodyAtom(), 0);
            }
            String key = word("key");
            skip();
            if (peek(':') || peek('<') || peek('>') || peek('=') || peek('!')) {
                return expr(P_NOT, rowExpr(parseKeyValue(key)), 0);
            }
            return rowExpr(row(keyCode(key), O_ABSENT, 0, -1, key));
        }

        private int absentRow() {
            at++;
            String key = word("key");
            return row(keyCode(key), O_ABSENT, 0, -1, key);
        }

        private int parseKeyValue(String key) {
            skip();
            byte code = keyCode(key);
            if (peek(':')) {
                at++;
                return parseValueRow(code, code == K_ATTR ? literal(key) : -1);
            }
            byte op = comparison();
            skip();
            long value = signedNumber();
            return row(code, op, value, -1, key);
        }

        private byte comparison() {
            if (s.startsWith(">=", at)) { at += 2; return O_GE; }
            if (s.startsWith("<=", at)) { at += 2; return O_LE; }
            if (s.startsWith("==", at)) { at += 2; return O_NUM_EQ; }
            if (s.startsWith("!=", at)) { at += 2; return O_NE; }
            if (peek('>')) { at++; return O_GT; }
            if (peek('<')) { at++; return O_LT; }
            if (peek('=')) { at++; return O_NUM_EQ; }
            throw error("expected ':' or a comparison after the key");
        }

        private long signedNumber() {
            int begin = at;
            if (peek('-')) {
                at++;
            }
            while (!eof() && Character.isDigit(s.charAt(at)) && at - begin < 19) {
                at++;
            }
            if (at == begin || (at == begin + 1 && s.charAt(begin) == '-')) {
                throw error("expected an integer");
            }
            return Long.parseLong(s, begin, at, 10);
        }

        private int parseValueRow(byte key, int attrSlot) {
            skip();
            if (eof()) {
                throw error("expected a value");
            }
            char c = s.charAt(at);
            byte op = c == '/' ? O_REGEX : O_EQ;
            String value = c == '"' ? quoted() : c == '/' ? regex() : word("value");
            int r = row(key, op, 0, literal(value), null);
            l.rattr[r] = attrSlot;
            return r;
        }

        private String quoted() {
            StringBuilder out = new StringBuilder();
            at++;
            while (!eof() && s.charAt(at) != '"') {
                char c = s.charAt(at++);
                if (c == '\\' && !eof()) {
                    c = s.charAt(at++);
                }
                out.append(c);
            }
            expect('"');
            if (out.isEmpty()) {
                throw error("empty quoted value");
            }
            return out.toString();
        }

        private String regex() {
            int begin = ++at;
            while (!eof() && s.charAt(at) != '/') {
                at += s.charAt(at) == '\\' && at + 1 < s.length() ? 2 : 1;
            }
            if (eof() || at == begin) {
                throw error(at == begin ? "empty regex" : "unterminated regex");
            }
            return s.substring(begin, at++);
        }

        private String word(String what) {
            int begin = at;
            while (!eof() && isWordChar(s.charAt(at), at > begin)) {
                at++;
            }
            if (at == begin) {
                throw error("expected " + what);
            }
            return s.substring(begin, at);
        }

        private static boolean isWordChar(char c, boolean inside) {
            return !Character.isWhitespace(c) && SPECIAL.indexOf(c) < 0 && (inside || c != '/');
        }

        private byte keyCode(String key) {
            return switch (key) {
                case "text", "word", "default" -> K_TEXT;
                case "kind" -> K_KIND;
                case "lexeme" -> K_LEXEME;
                case "binding" -> K_BINDING;
                case "flags" -> K_FLAGS;
                default -> K_ATTR;
            };
        }

        private int row(byte key, byte op, long number, int lit, String attrName) {
            if (l.rows == l.rkey.length) {
                throw error("too many predicates");
            }
            int r = l.rows++;
            l.rkey[r] = key; l.rop[r] = op; l.rnum[r] = number; l.rlit[r] = lit;
            l.rattr[r] = key == K_ATTR && attrName != null ? literal(attrName) : -1;
            return r;
        }

        private int literal(String value) {
            for (int i = 0; i < l.litCount; i++) {
                if (l.lits[i].equals(value)) {
                    return i;
                }
            }
            l.lits[l.litCount] = value;
            return l.litCount++;
        }

        private int rowExpr(int row) {
            return expr(P_ROW, row, 0);
        }

        private int expr(byte op, int a, int b) {
            if (l.exprs == l.eop.length) {
                throw error("predicate expression too large");
            }
            int e = l.exprs++;
            l.eop[e] = op; l.ea[e] = a; l.eb[e] = b;
            return e;
        }

        private int node(byte op, int x, int y, int z) {
            if (l.nodes == l.op.length) {
                throw error("pattern too complex");
            }
            int n = l.nodes++;
            l.op[n] = op; l.x[n] = x; l.y[n] = y; l.z[n] = z; l.next[n] = -1;
            return n;
        }

        private void enter() {
            if (++depth > MAX_DEPTH) {
                throw error("nesting deeper than " + MAX_DEPTH);
            }
        }

        private void skip() {
            while (!eof()) {
                char c = s.charAt(at);
                if (Character.isWhitespace(c)) {
                    at++;
                } else if (s.startsWith("//", at)) {
                    int eol = s.indexOf('\n', at);
                    at = eol < 0 ? s.length() : eol + 1;
                } else if (s.startsWith("/*", at)) {
                    int close = s.indexOf("*/", at + 2);
                    at = close < 0 ? s.length() : close + 2;
                } else {
                    return;
                }
            }
        }

        private boolean peek(char c) {
            return at < s.length() && s.charAt(at) == c;
        }

        private boolean eof() {
            return at >= s.length();
        }

        private void expect(char c) {
            skip();
            if (!peek(c)) {
                throw error("expected '" + c + "'");
            }
            at++;
        }

        private PatternSyntaxException error(String description) {
            return new PatternSyntaxException(description, s, Math.min(at, s.length()));
        }
    }
}
