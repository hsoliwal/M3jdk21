// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * PikeVM over a chunk cursor: captures and assertions with leftmost-first priority
 * (regex-automata {@code nfa/thompson/pikevm.rs}, algorithm only).
 *
 * <p>Thread lists are sparse sets in priority order; each listed state owns a block of
 * {@code slots} ints in {@code curSlots}/{@code nextSlots}. The closure runs on an explicit int
 * stack whose negative frames restore a capture slot. The first thread reaching MATCH at a
 * position cuts every lower-priority thread. Nothing is allocated per search.</p>
 */
final class M3CursorPikeVm {
    static final int MATCHED = 1;
    static final int NONE = 0;

    private final M3CursorNfa nfa;
    private final int slots;
    private final boolean quitSurrogates;
    private int[] curDense;
    private int[] curSparse;
    private int[] curSlots;
    private int curSize;
    private int[] nextDense;
    private int[] nextSparse;
    private int[] nextSlots;
    private int nextSize;
    private final int[] stack;
    private final int[] scratch;
    final int[] result;
    private M3ChunkCursor cursor;
    private int regionStart;
    private int regionEnd;

    M3CursorPikeVm(M3CursorNfa nfa, int groupCount, boolean quitSurrogates) {
        this.nfa = nfa;
        this.slots = 2 * (groupCount + 1);
        this.quitSurrogates = quitSurrogates;
        int n = nfa.size;
        curDense = new int[n];
        curSparse = new int[n];
        curSlots = new int[n * slots];
        nextDense = new int[n];
        nextSparse = new int[n];
        nextSlots = new int[n * slots];
        // Each state expands once per closure and pushes at most 3 ints (SAVE): 3n + 1 bound.
        stack = new int[3 * n + 2];
        scratch = new int[slots];
        result = new int[slots];
    }

    /**
     * Searches {@code [from, to]}; assertions see the region {@code [regionStart, regionEnd]}.
     *
     * @return {@link #MATCHED} with {@link #result} filled, {@link #NONE}, or
     *     {@link M3CursorDfa#QUIT_RESULT} when a surrogate was read by a text program
     */
    int search(M3ChunkCursor source, int from, int to, int regionStart, int regionEnd,
            boolean anchored, boolean requireEnd) {
        this.cursor = source;
        this.regionStart = regionStart;
        this.regionEnd = regionEnd;
        boolean found = false;
        curSize = 0;
        for (int pos = from; ; pos++) {
            if (!found && (!anchored || pos == from)) {
                Arrays.fill(scratch, -1);
                add(true, nfa.start, pos);
            }
            if (curSize == 0) break;
            int unit = pos < to ? source.unitAt(pos) : -1;
            if (quitSurrogates && unit >= Character.MIN_SURROGATE && unit <= Character.MAX_SURROGATE) {
                return M3CursorDfa.QUIT_RESULT;
            }
            found |= step(pos, unit, to, requireEnd);
            swap();
            if (pos >= to) break;
        }
        return found ? MATCHED : NONE;
    }

    private boolean step(int pos, int unit, int to, boolean requireEnd) {
        nextSize = 0;
        boolean found = false;
        for (int index = 0; index < curSize; index++) {
            int s = curDense[index];
            int op = nfa.op[s];
            if (op == M3CursorNfa.MATCH) {
                if (requireEnd && pos != to) continue;
                System.arraycopy(curSlots, s * slots, result, 0, slots);
                found = true;
                break;
            }
            if (op == M3CursorNfa.CLASS && unit >= 0 && nfa.admits(nfa.arg[s], unit)) {
                System.arraycopy(curSlots, s * slots, scratch, 0, slots);
                add(false, nfa.out[s], pos + 1);
            }
        }
        return found;
    }

    private void swap() {
        int[] dense = curDense;
        curDense = nextDense;
        nextDense = dense;
        int[] sparse = curSparse;
        curSparse = nextSparse;
        nextSparse = sparse;
        int[] block = curSlots;
        curSlots = nextSlots;
        nextSlots = block;
        curSize = nextSize;
    }

    private void add(boolean current, int root, int pos) {
        int[] dense = current ? curDense : nextDense;
        int[] sparse = current ? curSparse : nextSparse;
        int[] block = current ? curSlots : nextSlots;
        int size = current ? curSize : nextSize;
        int top = 0;
        stack[top++] = root;
        while (top > 0) {
            int frame = stack[--top];
            if (frame < 0) {
                scratch[-frame - 1] = stack[--top];
                continue;
            }
            int at = sparse[frame];
            if (at < size && dense[at] == frame) continue;
            dense[size] = frame;
            sparse[frame] = size++;
            top = expand(frame, pos, block, top);
        }
        if (current) curSize = size; else nextSize = size;
    }

    private int expand(int s, int pos, int[] block, int top) {
        switch (nfa.op[s]) {
            case M3CursorNfa.SPLIT -> {
                stack[top++] = nfa.out2[s];
                stack[top++] = nfa.out[s];
            }
            case M3CursorNfa.SAVE -> {
                int slot = nfa.arg[s];
                stack[top++] = scratch[slot];
                stack[top++] = -slot - 1;
                scratch[slot] = pos;
                stack[top++] = nfa.out[s];
            }
            case M3CursorNfa.ASSERT -> {
                if (holds(nfa.arg[s], pos)) stack[top++] = nfa.out[s];
            }
            default -> System.arraycopy(scratch, 0, block, s * slots, slots);
        }
        return top;
    }

    private boolean holds(int kind, int pos) {
        switch (kind) {
            case M3RegexAst.BEGIN:
                return pos == regionStart;
            case M3RegexAst.END:
                return pos == regionEnd;
            default:
                return dollar(pos);
        }
    }

    /** {@code java.util.regex.Pattern.Dollar(false)} over the cursor (stock L4004-4049). */
    private boolean dollar(int pos) {
        if (pos < regionEnd - 2) return false;
        if (pos == regionEnd - 2 && (cursor.unitAt(pos) != '\r' || cursor.unitAt(pos + 1) != '\n')) {
            return false;
        }
        if (pos < regionEnd) {
            int ch = cursor.unitAt(pos);
            if (ch == '\n') return !(pos > 0 && cursor.unitAt(pos - 1) == '\r');
            return ch == '\r' || ch == 0x85 || (ch | 1) == 0x2029;
        }
        return true;
    }
}
