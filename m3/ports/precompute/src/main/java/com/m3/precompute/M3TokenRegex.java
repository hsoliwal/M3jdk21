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
