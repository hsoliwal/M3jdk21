// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * Lazy DFA over a chunk cursor (pattern of regex-cursor {@code engines/hybrid/search.rs};
 * determinization of regex-automata, algorithm only).
 *
 * <p>A DFA state is the priority-ordered list of NFA class states plus a match bit. In
 * leftmost-first mode the closure stops at the first MATCH state, cutting every lower-priority
 * thread; scanning then records the last matching position until the dead state, which is the
 * leftmost-first end. In all-match mode nothing is cut (used for {@code matches()} and for the
 * reverse start scan). Transitions are filled on first use into the {@code trans} lane.</p>
 *
 * <p>Reserved states: {@code 0} dead, {@code 1} quit. Search results: {@code >= 0} position,
 * {@link #NO_MATCH}, {@link #QUIT_RESULT} (a quit symbol was read), {@link #GAVE_UP} (state
 * cache full).</p>
 */
final class M3CursorDfa {
    static final int NO_MATCH = -1;
    static final int QUIT_RESULT = -2;
    static final int GAVE_UP = -3;
    static final int DEFAULT_MAX_STATES = 4096;
    private static final int DEAD = 0;
    private static final int QUIT = 1;
    private static final int UNKNOWN = -1;
    private static final int FULL = -2;

    private final M3CursorNfa nfa;
    private final M3UnitClasses classes;
    private final boolean leftmostFirst;
    private final int maxStates;
    private final int stride;
    private int[] trans;
    private long[] matchBits = new long[1];
    private int[] stateStart = new int[16];
    private int[] stateLength = new int[16];
    private int[] pool = new int[64];
    private int poolSize;
    private int stateCount = 2;
    private int[] table = new int[64];
    private final int[] dense;
    private final int[] sparse;
    private final int[] stack;
    private final int[] list;
    private int setSize;
    private int listSize;
    private boolean matched;
    private int start = UNKNOWN;

    M3CursorDfa(M3CursorNfa nfa, M3UnitClasses classes, boolean leftmostFirst, int maxStates) {
        this.nfa = nfa;
        this.classes = classes;
        this.leftmostFirst = leftmostFirst;
        this.maxStates = maxStates;
        this.stride = classes.count;
        this.trans = new int[16 * stride];
        Arrays.fill(trans, UNKNOWN);
        this.dense = new int[nfa.size];
        this.sparse = new int[nfa.size];
        // Each state expands once per closure and pushes at most 2 ints (SPLIT): 2n + 1 bound.
        this.stack = new int[2 * nfa.size + 2];
        this.list = new int[nfa.size];
    }

    /** Number of materialized DFA states, reserved states included. */
    int stateCount() {
        return stateCount;
    }

    /** Scans {@code [from, to)} forward; returns the last match end or a negative result. */
    int scanForward(M3ChunkCursor cursor, int from, int to) {
        int state = startState();
        if (state < 0) return GAVE_UP;
        int last = isMatch(state) ? from : NO_MATCH;
        if (from >= to || (last >= 0 && stateLength[state] == 0)) return last;
        cursor.seek(from);
        int index = from - cursor.offset();
        while (true) {
            int base = cursor.offset();
            int limit = Math.min(cursor.chunkLength(), to - base);
            for (; index < limit; index++) {
                state = step(state, cursor.unit(index));
                if (state <= QUIT) return state == DEAD ? last : state < 0 ? GAVE_UP : QUIT_RESULT;
                if (isMatch(state)) {
                    last = base + index + 1;
                    if (stateLength[state] == 0) return last;
                }
            }
            if (base + limit >= to || !cursor.advance()) return last;
            index = 0;
        }
    }

    /** Scans {@code [from, end)} backward from {@code end}; returns the last match position. */
    int scanBackward(M3ChunkCursor cursor, int from, int end) {
        int state = startState();
        if (state < 0) return GAVE_UP;
        int last = isMatch(state) ? end : NO_MATCH;
        if (end <= from || (last >= 0 && stateLength[state] == 0)) return last;
        cursor.seek(end - 1);
        int index = end - 1 - cursor.offset();
        while (true) {
            int base = cursor.offset();
            int floor = Math.max(0, from - base);
            for (; index >= floor; index--) {
                state = step(state, cursor.unit(index));
                if (state <= QUIT) return state == DEAD ? last : state < 0 ? GAVE_UP : QUIT_RESULT;
                if (isMatch(state)) {
                    last = base + index;
                    if (stateLength[state] == 0) return last;
                }
            }
            if (base <= from || !cursor.backtrack()) return last;
            index = cursor.chunkLength() - 1;
        }
    }

    private int step(int state, int unit) {
        int c = classes.classOf(unit);
        int next = trans[state * stride + c];
        return next != UNKNOWN ? next : computeTransition(state, c);
    }

    private boolean isMatch(int state) {
        return (matchBits[state >>> 6] & (1L << state)) != 0;
    }

    private int startState() {
        if (start == UNKNOWN) {
            beginSet();
            closure(nfa.start);
            int interned = intern();
            if (interned == FULL) return FULL;
            start = interned;
        }
        return start;
    }

    private int computeTransition(int state, int c) {
        int next;
        if (classes.quit[c]) {
            next = QUIT;
        } else {
            next = follow(state, classes.bounds[c]);
            if (next == FULL) {
                M3CursorProbe.dfaGiveUp();
                return FULL;
            }
        }
        trans[state * stride + c] = next;
        return next;
    }

    private int follow(int state, int unit) {
        beginSet();
        int begin = stateStart[state];
        int end = begin + stateLength[state];
        for (int index = begin; index < end; index++) {
            int nfaState = pool[index];
            if (nfa.admits(nfa.arg[nfaState], unit) && closure(nfa.out[nfaState])) break;
        }
        return listSize == 0 && !matched ? DEAD : intern();
    }

    private void beginSet() {
        setSize = 0;
        listSize = 0;
        matched = false;
    }

    /** Priority-ordered closure; returns {@code true} when leftmost-first cut at a MATCH. */
    private boolean closure(int root) {
        int top = 0;
        stack[top++] = root;
        while (top > 0) {
            int s = stack[--top];
            if (contains(s)) continue;
            dense[setSize] = s;
            sparse[s] = setSize++;
            switch (nfa.op[s]) {
                case M3CursorNfa.CLASS -> list[listSize++] = s;
                case M3CursorNfa.MATCH -> {
                    matched = true;
                    if (leftmostFirst) return true;
                }
                case M3CursorNfa.SPLIT -> {
                    stack[top++] = nfa.out2[s];
                    stack[top++] = nfa.out[s];
                }
                case M3CursorNfa.SAVE -> stack[top++] = nfa.out[s];
                default -> { }
            }
        }
        return false;
    }

    private boolean contains(int s) {
        int at = sparse[s];
        return at < setSize && dense[at] == s;
    }

    private static int[] ensure(int[] array, int needed) {
        return needed <= array.length ? array : Arrays.copyOf(array, Math.max(needed, array.length * 2));
    }

    private int intern() {
        int hash = hash();
        int mask = table.length - 1;
        for (int slot = hash & mask; ; slot = (slot + 1) & mask) {
            int id = table[slot];
            if (id == 0) return add(slot, hash);
            if (same(id)) return id;
        }
    }

    private int hash() {
        int h = matched ? 0x9E3779B9 : 0x7F4A7C15;
        for (int index = 0; index < listSize; index++) {
            h = (h ^ list[index]) * 0x01000193;
        }
        return h ^ (h >>> 16);
    }

    private boolean same(int id) {
        if (stateLength[id] != listSize || isMatch(id) != matched) return false;
        return Arrays.equals(pool, stateStart[id], stateStart[id] + listSize, list, 0, listSize);
    }

    private int add(int slot, int hash) {
        if (stateCount >= maxStates) return FULL;
        int id = stateCount++;
        growStates(id);
        pool = ensure(pool, poolSize + listSize);
        System.arraycopy(list, 0, pool, poolSize, listSize);
        stateStart[id] = poolSize;
        stateLength[id] = listSize;
        poolSize += listSize;
        if (matched) matchBits[id >>> 6] |= 1L << id;
        table[slot] = id;
        if (stateCount * 2 > table.length) rehash();
        return id;
    }

    private void growStates(int id) {
        if (id < stateStart.length) return;
        int capacity = stateStart.length * 2;
        stateStart = Arrays.copyOf(stateStart, capacity);
        stateLength = Arrays.copyOf(stateLength, capacity);
        int old = trans.length;
        trans = Arrays.copyOf(trans, capacity * stride);
        Arrays.fill(trans, old, trans.length, UNKNOWN);
        matchBits = Arrays.copyOf(matchBits, (capacity >>> 6) + 1);
    }

    private void rehash() {
        int[] old = table;
        table = new int[old.length * 2];
        int mask = table.length - 1;
        for (int id : old) {
            if (id == 0) continue;
            int slot = rehashKey(id) & mask;
            while (table[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            table[slot] = id;
        }
    }

    private int rehashKey(int id) {
        int h = isMatch(id) ? 0x9E3779B9 : 0x7F4A7C15;
        for (int index = stateStart[id]; index < stateStart[id] + stateLength[id]; index++) {
            h = (h ^ pool[index]) * 0x01000193;
        }
        return h ^ (h >>> 16);
    }
}
