// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded regular expressions over token IDs/kind masks, NOT character regex.
 *
 * <p>Syntax: #id (token ID), %mask (all kind bits), !%mask (no kind bits),
 * [id=7,all=0x2,none=0xC], . (any token), groups, |, ?, *, + and outer ^ / $.
 * Spaces separate atoms but are not matched. Captures, lookaround and backrefs
 * are deliberately unsupported. Matching uses compiled follow-position bitsets
 * and two primitive start-offset arrays; it neither builds token objects nor
 * backtracks through the input. The initial implementation is Java-only.</p>
 */
public final class M3TokenRegex {
    private static final int ID = 1, MASK = 2, ANY = 3, EMPTY = 4;
    private static final int ALT = 5, CONCAT = 6, STAR = 7, PLUS = 8, OPTIONAL = 9, ACCEPT = 10;
    private static final int MAX_NODES = 1024;
    private static final int MAX_POSITIONS = 512;

    /** A result is allocated only when a public matching API finds a match. */
    public record Match(int firstToken, int endToken, long startUtf16, long endUtf16) {
        public Match {
            if (firstToken < 0 || endToken < firstToken || startUtf16 < 0 || endUtf16 < startUtf16) {
                throw new IllegalArgumentException("invalid token/source match range");
            }
        }
    }

    private final String expression;
    private final boolean anchoredStart;
    private final boolean anchoredEnd;
    private final int positionCount;
    private final int acceptingPosition;
    private final int[] tokenIds;
    private final int[] requiredMasks;
    private final int[] excludedMasks;
    private final int[] terminalKinds;
    private final long[] startBits;
    private final long[] followBits;
    private final int words;

    private M3TokenRegex(String expression, boolean anchoredStart, boolean anchoredEnd,
            int positionCount, int acceptingPosition, int[] tokenIds, int[] requiredMasks,
            int[] excludedMasks, int[] terminalKinds, long[] startBits, long[] followBits, int words) {
        this.expression = expression;
        this.anchoredStart = anchoredStart;
        this.anchoredEnd = anchoredEnd;
        this.positionCount = positionCount;
        this.acceptingPosition = acceptingPosition;
        this.tokenIds = tokenIds;
        this.requiredMasks = requiredMasks;
        this.excludedMasks = excludedMasks;
        this.terminalKinds = terminalKinds;
        this.startBits = startBits;
        this.followBits = followBits;
        this.words = words;
    }

    public static M3TokenRegex compile(String expression) {
        Objects.requireNonNull(expression, "expression");
        if (expression.length() > 8192) throw new IllegalArgumentException("token pattern too long");
        Parser parser = new Parser(expression);
        return parser.compile();
    }

    public String expression() { return expression; }
    public int compiledPositions() { return positionCount; }
    public long primitiveBytes() {
        return (long) Integer.BYTES * (tokenIds.length + requiredMasks.length
                + excludedMasks.length + terminalKinds.length)
                + Long.BYTES * (long) (startBits.length + followBits.length);
    }

    /** Exact entire-tape token sequence match; no capture or character-regex fallback. */
    public boolean matches(M3TokenTape tape) {
        Objects.requireNonNull(tape, "tape");
        return search(tape, true) != null;
    }

    /** Leftmost-longest token-span match. Returns null when there is no match. */
    public Match find(M3TokenTape tape) {
        Objects.requireNonNull(tape, "tape");
        return search(tape, false);
    }

    private Match search(M3TokenTape tape, boolean full) {
        int[] active = new int[positionCount];
        int[] next = new int[positionCount];
        Arrays.fill(active, -1);
        int first = -1, end = -1;
        final int count = tape.size();
        for (int boundary = 0; boundary <= count; boundary++) {
            if (full ? boundary == 0 : !anchoredStart || boundary == 0) {
                for (int word = 0; word < words; word++) {
                    long bits = startBits[word];
                    while (bits != 0) {
                        int bit = Long.numberOfTrailingZeros(bits);
                        int pos = (word << 6) + bit;
                        if (active[pos] == -1 || boundary < active[pos]) active[pos] = boundary;
                        bits &= bits - 1;
                    }
                }
            }
            int candidate = active[acceptingPosition];
            if (candidate >= 0 && (!full || candidate == 0)
                    && (!full || boundary == count) && (!anchoredEnd || boundary == count)) {
                if (first < 0 || candidate < first || candidate == first && boundary > end) {
                    first = candidate;
                    end = boundary;
                }
            }
            if (boundary == count) break;
            Arrays.fill(next, -1);
            int id = tape.idAt(boundary);
            int mask = tape.kindMaskAt(boundary);
            for (int pos = 0; pos < positionCount; pos++) {
                int start = active[pos];
                if (start < 0 || !accepts(pos, id, mask)) continue;
                final int base = pos * words;
                for (int word = 0; word < words; word++) {
                    long bits = followBits[base + word];
                    while (bits != 0) {
                        int dest = (word << 6) + Long.numberOfTrailingZeros(bits);
                        if (next[dest] < 0 || start < next[dest]) next[dest] = start;
                        bits &= bits - 1;
                    }
                }
            }
            int[] previous = active; active = next; next = previous;
        }
        if (first < 0) return null;
        long utf16Start = tape.boundaryUtf16(first);
        long utf16End = first == end ? utf16Start : tape.endUtf16At(end - 1);
        return new Match(first, end, utf16Start, utf16End);
    }

    private boolean accepts(int position, int id, int mask) {
        return switch (terminalKinds[position]) {
            case ID -> id == tokenIds[position]
                    && (mask & requiredMasks[position]) == requiredMasks[position]
                    && (mask & excludedMasks[position]) == 0;
            case MASK -> (mask & requiredMasks[position]) == requiredMasks[position]
                    && (mask & excludedMasks[position]) == 0;
            case ANY -> true;
            default -> false;
        };
    }

    private static final class Parser {
        private final String source;
        private int cursor, nodeCount, positions;
        private final int[] op = new int[MAX_NODES];
        private final int[] left = new int[MAX_NODES], right = new int[MAX_NODES];
        private final int[] leafPosition = new int[MAX_NODES];
        private final int[] ids = new int[MAX_POSITIONS];
        private final int[] req = new int[MAX_POSITIONS];
        private final int[] exc = new int[MAX_POSITIONS];
        private final int[] kind = new int[MAX_POSITIONS];
        private boolean anchoredStart, anchoredEnd;

        private Parser(String source) { this.source = source; }

        private M3TokenRegex compile() {
            skipSpace();
            if (take('^')) anchoredStart = true;
            int expr = alternation();
            skipSpace();
            if (take('$')) anchoredEnd = true;
            skipSpace();
            if (cursor != source.length()) error("unrecognized or misplaced token-regex operator");
            int accept = leaf(ACCEPT, -1, 0, 0);
            int root = node(CONCAT, expr, accept);
            int words = (positions + Long.SIZE - 1) / Long.SIZE;
            long[] first = new long[nodeCount * words];
            long[] last = new long[nodeCount * words];
            long[] follow = new long[positions * words];
            boolean[] nullable = new boolean[nodeCount];
            for (int n = 0; n < nodeCount; n++) {
                int a = left[n], b = right[n], dst = n * words;
                switch (op[n]) {
                    case ID, MASK, ANY, ACCEPT -> {
                        int pos = leafPosition[n];
                        first[dst + (pos >>> 6)] |= 1L << pos;
                        last[dst + (pos >>> 6)] |= 1L << pos;
                    }
                    case EMPTY -> nullable[n] = true;
                    case ALT -> {
                        nullable[n] = nullable[a] || nullable[b];
                        orRow(first, dst, first, a * words, words);
                        orRow(first, dst, first, b * words, words);
                        orRow(last, dst, last, a * words, words);
                        orRow(last, dst, last, b * words, words);
                    }
                    case CONCAT -> {
                        nullable[n] = nullable[a] && nullable[b];
                        orRow(first, dst, first, a * words, words);
                        if (nullable[a]) orRow(first, dst, first, b * words, words);
                        orRow(last, dst, last, b * words, words);
                        if (nullable[b]) orRow(last, dst, last, a * words, words);
                        connect(last, a * words, first, b * words, follow, positions, words);
                    }
                    case STAR, PLUS, OPTIONAL -> {
                        nullable[n] = op[n] != PLUS || nullable[a];
                        orRow(first, dst, first, a * words, words);
                        orRow(last, dst, last, a * words, words);
                        if (op[n] != OPTIONAL) {
                            connect(last, a * words, first, a * words, follow, positions, words);
                        }
                    }
                    default -> throw new IllegalStateException("invalid token regex node");
                }
            }
            return new M3TokenRegex(source, anchoredStart, anchoredEnd, positions,
                    leafPosition[accept], Arrays.copyOf(ids, positions),
                    Arrays.copyOf(req, positions), Arrays.copyOf(exc, positions),
                    Arrays.copyOf(kind, positions),
                    Arrays.copyOfRange(first, root * words, (root + 1) * words), follow, words);
        }

        private static void orRow(long[] target, int dst, long[] src, int from, int words) {
            for (int w = 0; w < words; w++) target[dst + w] |= src[from + w];
        }

        private static void connect(long[] last, int lastOffset, long[] first, int firstOffset,
                long[] follow, int positions, int words) {
            for (int w = 0; w < words; w++) {
                long bits = last[lastOffset + w];
                while (bits != 0) {
                    int pos = (w << 6) + Long.numberOfTrailingZeros(bits);
                    if (pos < positions) orRow(follow, pos * words, first, firstOffset, words);
                    bits &= bits - 1;
                }
            }
        }

        private int alternation() {
            int result = sequence();
            skipSpace();
            while (take('|')) {
                result = node(ALT, result, sequence());
                skipSpace();
            }
            return result;
        }

        private int sequence() {
            int result = -1;
            skipSpace();
            while (cursor < source.length()) {
                char ch = source.charAt(cursor);
                if (ch == ')' || ch == '|' || ch == '$') break;
                int item = atom();
                result = result < 0 ? item : node(CONCAT, result, item);
                skipSpace();
            }
            return result < 0 ? node(EMPTY, 0, 0) : result;
        }

        private int atom() {
            skipSpace();
            int result;
            if (take('(')) {
                result = alternation();
                skipSpace();
                if (!take(')')) error("missing closing parenthesis");
            } else if (take('.')) {
                result = leaf(ANY, -1, 0, 0);
            } else if (take('#')) {
                int id = number();
                if (id < 0) error("token ID exceeds signed int range");
                result = leaf(ID, id, 0, 0);
            } else if (take('%')) {
                result = leaf(MASK, -1, number(), 0);
            } else if (take('!')) {
                if (!take('%')) error("expected !%kind-mask");
                result = leaf(MASK, -1, 0, number());
            } else if (take('[')) {
                result = bracket();
            } else {
                error("expected token ID, kind mask, wildcard or group");
                throw new AssertionError();
            }
            if (take('*')) result = node(STAR, result, 0);
            else if (take('+')) result = node(PLUS, result, 0);
            else if (take('?')) result = node(OPTIONAL, result, 0);
            return result;
        }

        private int bracket() {
            int id = -1, required = 0, excluded = 0;
            boolean found = false;
            while (true) {
                skipSpace();
                if (take(']')) {
                    if (!found) error("empty token selector");
                    return leaf(id >= 0 ? ID : MASK, id, required, excluded);
                }
                String key = keyword();
                skipSpace();
                if (!take('=')) error("token selector requires '='");
                int value = number();
                switch (key) {
                    case "id" -> {
                        if (id >= 0) error("duplicate id");
                        if (value < 0) error("token ID exceeds signed int range");
                        id = value;
                    }
                    case "all" -> required |= value;
                    case "none" -> excluded |= value;
                    default -> error("unknown selector attribute");
                }
                found = true;
                skipSpace();
                if (!take(',')) {
                    if (!take(']')) error("expected ',' or ']'");
                    return leaf(id >= 0 ? ID : MASK, id, required, excluded);
                }
            }
        }

        private String keyword() {
            int start = cursor;
            while (cursor < source.length() && Character.isLetter(source.charAt(cursor))) cursor++;
            if (start == cursor) error("expected selector attribute name");
            return source.substring(start, cursor);
        }

        private int number() {
            skipSpace();
            int radix = 10;
            if (source.startsWith("0x", cursor) || source.startsWith("0X", cursor)) {
                radix = 16; cursor += 2;
            }
            int start = cursor;
            while (cursor < source.length()
                    && Character.digit(source.charAt(cursor), radix) >= 0) cursor++;
            if (start == cursor) error("missing numeric token/mask value");
            try {
                return radix == 16
                        ? Integer.parseUnsignedInt(source.substring(start, cursor), 16)
                        : Integer.parseUnsignedInt(source.substring(start, cursor), 10);
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("token regex numeric value out of range", invalid);
            }
        }

        private int leaf(int opcode, int tokenId, int required, int excluded) {
            if (positions >= MAX_POSITIONS) error("token-regex position budget exceeded");
            int at = positions++;
            ids[at] = tokenId;
            req[at] = required;
            exc[at] = excluded;
            kind[at] = opcode;
            int n = node(opcode, 0, 0);
            leafPosition[n] = at;
            return n;
        }

        private int node(int opcode, int a, int b) {
            if (nodeCount == MAX_NODES) error("token-regex node budget exceeded");
            int at = nodeCount++;
            op[at] = opcode;
            left[at] = a;
            right[at] = b;
            return at;
        }

        private void skipSpace() {
            while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor)) cursor++;
        }
        private boolean take(char ch) {
            if (cursor < source.length() && source.charAt(cursor) == ch) { cursor++; return true; }
            return false;
        }
        private void error(String message) {
            throw new IllegalArgumentException(message + " at offset " + cursor);
        }
    }
}
