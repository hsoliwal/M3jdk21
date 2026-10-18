// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;
import java.util.regex.Matcher;

/**
 * Thread-confined matcher of one {@link M3CursorRegex} over one {@link M3ChunkCursor}.
 *
 * <p>Mirrors the {@code java.util.regex.Matcher} contract for {@code find()},
 * {@code find(int)}, {@code matches()}, {@code lookingAt()}, {@code start/end(group)} and the
 * empty-match iteration rule. Group spans are int coordinates; no group text is built. Lazy DFA
 * caches and PikeVM lanes are created on first use and reused, so warmed searches allocate
 * nothing.</p>
 */
public final class M3CursorMatcher {
    public static final int ENGINE_NONE = 0;
    public static final int ENGINE_GATE = 1;
    public static final int ENGINE_LITERAL = 2;
    public static final int ENGINE_DFA = 3;
    public static final int ENGINE_PIKEVM = 4;
    public static final int ENGINE_STOCK = 5;

    private final M3CursorRegex regex;
    private M3ChunkCursor cursor;
    private int total;
    private final int[] groups;
    private final long[] operations = new long[6];
    private boolean matched;
    private boolean resolved;
    private int first = -1;
    private int last;
    private int engine;
    private int gateFrom = -1;
    private int gateHit = -1;
    private final M3CursorDfa[] dfas = new M3CursorDfa[4];
    private M3CursorPikeVm pike;
    private Matcher stockMatcher;

    M3CursorMatcher(M3CursorRegex regex, M3ChunkCursor cursor) {
        this.regex = regex;
        this.cursor = cursor;
        this.total = cursor.totalLength();
        this.groups = new int[2 * (regex.groupCount + 1)];
        Arrays.fill(groups, -1);
    }

    /** Next match, with the stock empty-match advance rule. */
    public boolean find() {
        int next = last == first ? last + 1 : last;
        if (next > total) return miss(ENGINE_NONE);
        return search(next);
    }

    /** Resets, then finds the first match starting at or after {@code from}. */
    public boolean find(int from) {
        if (from < 0 || from > total) throw new IndexOutOfBoundsException("Illegal start index");
        reset();
        return search(from);
    }

    /** Whether the whole input matches. */
    public boolean matches() {
        switch (regex.strategy) {
            case M3CursorRegex.STRATEGY_LITERAL:
                return regex.literal.length() == total && literalAt(0) ? hit(0, total, ENGINE_LITERAL) : miss(ENGINE_LITERAL);
            case M3CursorRegex.STRATEGY_DFA:
                int end = dfa(2).scanForward(cursor, 0, total);
                if (end < M3CursorDfa.NO_MATCH) return pikeOrStock(0, true, true);
                return end == total ? hit(0, total, ENGINE_DFA) : miss(ENGINE_DFA);
            case M3CursorRegex.STRATEGY_PIKEVM:
                return pikeOrStock(0, true, true);
            default:
                return stock(0, 2);
        }
    }

    /** Whether a prefix of the input matches. */
    public boolean lookingAt() {
        switch (regex.strategy) {
            case M3CursorRegex.STRATEGY_LITERAL:
                int n = regex.literal.length();
                return n <= total && literalAt(0) ? hit(0, n, ENGINE_LITERAL) : miss(ENGINE_LITERAL);
            case M3CursorRegex.STRATEGY_DFA:
                int end = dfa(1).scanForward(cursor, 0, total);
                if (end < M3CursorDfa.NO_MATCH) return pikeOrStock(0, true, false);
                return end >= 0 ? hit(0, end, ENGINE_DFA) : miss(ENGINE_DFA);
            case M3CursorRegex.STRATEGY_PIKEVM:
                return pikeOrStock(0, true, false);
            default:
                return stock(0, 1);
        }
    }

    private boolean literalAt(int position) {
        int n = regex.literal.length();
        return n == 0 || (n <= total - position && regex.literal.find(cursor, position, position + n) == position);
    }

    private boolean search(int from) {
        if (gateRejects(from)) {
            M3CursorProbe.gate();
            return miss(ENGINE_GATE);
        }
        switch (regex.strategy) {
            case M3CursorRegex.STRATEGY_LITERAL:
                int at = regex.literal.find(cursor, from, total);
                return at >= 0 ? hit(at, at + regex.literal.length(), ENGINE_LITERAL) : miss(ENGINE_LITERAL);
            case M3CursorRegex.STRATEGY_DFA:
                return regex.prefix != null ? prefixSearch(from) : dfaSearch(from);
            case M3CursorRegex.STRATEGY_PIKEVM:
                return pikeOrStock(from, false, false);
            default:
                return stock(from, 0);
        }
    }

    private boolean dfaSearch(int from) {
        int end = dfa(0).scanForward(cursor, from, total);
        if (end < M3CursorDfa.NO_MATCH) return pikeOrStock(from, false, false);
        if (end == M3CursorDfa.NO_MATCH) return miss(ENGINE_DFA);
        int start = dfa(3).scanBackward(cursor, from, end);
        // A forward end always has a reverse start; any negative result (cache full) re-runs on the PikeVM.
        if (start < 0) return pikeOrStock(from, false, false);
        return hit(start, end, ENGINE_DFA);
    }

    private boolean prefixSearch(int from) {
        int position = from;
        while (true) {
            int start = regex.prefix.find(cursor, position, total);
            if (start < 0) return miss(ENGINE_DFA);
            int end = dfa(1).scanForward(cursor, start, total);
            if (end < M3CursorDfa.NO_MATCH) return pikeOrStock(from, false, false);
            if (end >= 0) return hit(start, end, ENGINE_DFA);
            position = start + 1;
        }
    }

    private boolean gateRejects(int from) {
        if (regex.required == null) return false;
        if (cursor instanceof M3CursorFacts facts
                && (facts.unitPresenceMask() & regex.requiredMask) != regex.requiredMask) {
            return true;
        }
        if (regex.strategy == M3CursorRegex.STRATEGY_LITERAL) return false;
        if (gateFrom >= 0 && from >= gateFrom && (gateHit < 0 || gateHit >= from)) return gateHit < 0;
        gateFrom = from;
        gateHit = regex.required.find(cursor, from, total);
        return gateHit < 0;
    }

    /** Lazy DFA {@code kind}: 0 unanchored first-match, 1 anchored first-match, 2 anchored all, 3 reverse all. */
    private M3CursorDfa dfa(int kind) {
        M3CursorDfa dfa = dfas[kind];
        if (dfa == null) {
            M3CursorNfa nfa = kind == 0 ? regex.unanchored : kind == 3 ? regex.reverse : regex.anchored;
            dfa = new M3CursorDfa(nfa, regex.classes, kind < 2, regex.maxDfaStates);
            dfas[kind] = dfa;
        }
        return dfa;
    }

    private M3CursorPikeVm pike() {
        if (pike == null) pike = new M3CursorPikeVm(regex.anchored, regex.groupCount, regex.quitSurrogates);
        return pike;
    }

    private boolean pikeOrStock(int from, boolean anchored, boolean requireEnd) {
        int result = pike().search(cursor, from, total, 0, total, anchored, requireEnd);
        if (result == M3CursorDfa.QUIT_RESULT) return stock(from, anchored ? (requireEnd ? 2 : 1) : 0);
        if (result != M3CursorPikeVm.MATCHED) return miss(ENGINE_PIKEVM);
        System.arraycopy(pike.result, 0, groups, 0, groups.length);
        resolved = true;
        return hit(groups[0], groups[1], ENGINE_PIKEVM);
    }

    private boolean stock(int from, int mode) {
        M3CursorProbe.stock();
        if (stockMatcher == null) stockMatcher = regex.stock.matcher(new M3CursorCharSequence(cursor));
        Matcher m = stockMatcher;
        boolean found = mode == 0 ? m.find(from) : mode == 1 ? m.reset().lookingAt() : m.reset().matches();
        if (!found) return miss(ENGINE_STOCK);
        for (int group = 0; group <= regex.groupCount; group++) {
            groups[2 * group] = m.start(group);
            groups[2 * group + 1] = m.end(group);
        }
        resolved = true;
        return hit(groups[0], groups[1], ENGINE_STOCK);
    }

    /** Operations this matcher answered with {@code engine} ({@code ENGINE_*}). */
    public long operations(int engine) {
        return operations[engine];
    }

    private boolean hit(int start, int end, int used) {
        operations[used]++;
        if (used != ENGINE_PIKEVM && used != ENGINE_STOCK) resolved = regex.groupCount == 0;
        groups[0] = start;
        groups[1] = end;
        matched = true;
        first = start;
        last = end;
        engine = used;
        return true;
    }

    private boolean miss(int used) {
        operations[used]++;
        matched = false;
        first = -1;
        engine = used;
        return false;
    }

    private void resolveGroups() {
        if (resolved) return;
        int start = groups[0];
        int end = groups[1];
        int result = pike().search(cursor, start, end, 0, total, true, true);
        if (result != M3CursorPikeVm.MATCHED || pike.result[0] != start || pike.result[1] != end) {
            throw new IllegalStateException("group replay diverged " + regex);
        }
        System.arraycopy(pike.result, 0, groups, 0, groups.length);
        resolved = true;
    }

    /** Start of the last match. */
    public int start() {
        return start(0);
    }

    /** End of the last match. */
    public int end() {
        return end(0);
    }

    /** Start of {@code group} in the last match, {@code -1} when it did not participate. */
    public int start(int group) {
        check(group);
        return groups[2 * group];
    }

    /** End of {@code group} in the last match, {@code -1} when it did not participate. */
    public int end(int group) {
        check(group);
        return groups[2 * group + 1];
    }

    private void check(int group) {
        if (!matched) throw new IllegalStateException("No match available");
        if (group < 0 || group > regex.groupCount) throw new IndexOutOfBoundsException("No group " + group);
        if (group > 0) resolveGroups();
    }

    /** Number of capturing groups. */
    public int groupCount() {
        return regex.groupCount;
    }

    /** Engine that decided the last operation ({@code ENGINE_*}). */
    public int engine() {
        return engine;
    }

    /**
     * Rebinds to {@code input} (the {@code Matcher.reset(CharSequence)} analogue). Lazy DFA and
     * PikeVM lanes depend only on the pattern and are kept warm.
     */
    public M3CursorMatcher reset(M3ChunkCursor input) {
        cursor = java.util.Objects.requireNonNull(input, "input");
        total = input.totalLength();
        stockMatcher = null;
        gateFrom = -1;
        gateHit = -1;
        return reset();
    }

    /** Clears match state and the find position; caches are kept. */
    public M3CursorMatcher reset() {
        matched = false;
        first = -1;
        last = 0;
        Arrays.fill(groups, -1);
        return this;
    }
}
