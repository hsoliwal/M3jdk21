// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * Thompson NFA in int lanes, built from {@link M3RegexAst} by continuation passing.
 *
 * <p>Lanes: {@code op}, {@code out}, {@code out2} (second split target), {@code arg} (class id,
 * capture slot or assertion kind). SPLIT prefers {@code out}: greedy repeats prefer the body,
 * lazy repeats prefer the exit, alternation prefers the left branch. The unanchored form adds
 * the lazy any-symbol prefix loop at the lowest priority (regex-automata's unanchored start).
 * The reverse form reverses every concatenation and drops capture slots.</p>
 */
final class M3CursorNfa {
    static final int CLASS = 1;
    static final int SPLIT = 2;
    static final int EPS = 3;
    static final int SAVE = 4;
    static final int MATCH = 5;
    static final int ASSERT = 6;
    static final int MAX_STATES = 50_000;

    final M3RegexAst ast;
    final boolean reverse;
    int[] op = new int[32];
    int[] out = new int[32];
    int[] out2 = new int[32];
    int[] arg = new int[32];
    int size;
    int start;

    private M3CursorNfa(M3RegexAst ast, boolean reverse) {
        this.ast = ast;
        this.reverse = reverse;
    }

    /** Forward NFA with capture slots; {@code unanchored} adds the lazy prefix loop. */
    static M3CursorNfa forward(M3RegexAst ast, boolean unanchored) {
        M3CursorNfa nfa = new M3CursorNfa(ast, false);
        int match = nfa.state(MATCH, -1, -1, 0);
        int body = nfa.state(SAVE, nfa.compile(ast.root, nfa.state(SAVE, match, -1, 1)), -1, 0);
        nfa.start = unanchored ? nfa.prefixLoop(body) : body;
        return nfa;
    }

    /** Reverse anchored NFA without capture slots. */
    static M3CursorNfa reverse(M3RegexAst ast) {
        M3CursorNfa nfa = new M3CursorNfa(ast, true);
        nfa.start = nfa.compile(ast.root, nfa.state(MATCH, -1, -1, 0));
        return nfa;
    }

    private int prefixLoop(int body) {
        int loop = state(SPLIT, body, -1, 0);
        out2[loop] = state(CLASS, loop, -1, ast.anyClass);
        return loop;
    }

    private int state(int kind, int target, int target2, int argument) {
        if (size == op.length) {
            if (size >= MAX_STATES) throw new M3RegexRefusal("nfa size");
            int capacity = size * 2;
            op = Arrays.copyOf(op, capacity);
            out = Arrays.copyOf(out, capacity);
            out2 = Arrays.copyOf(out2, capacity);
            arg = Arrays.copyOf(arg, capacity);
        }
        op[size] = kind;
        out[size] = target;
        out2[size] = target2;
        arg[size] = argument;
        return size++;
    }

    private int compile(int node, int next) {
        switch (ast.op[node]) {
            case M3RegexAst.CLASS:
                return state(CLASS, next, -1, ast.a[node]);
            case M3RegexAst.CONCAT:
                return reverse ? concatReverse(ast.a[node], next) : concatForward(ast.a[node], next);
            case M3RegexAst.ALT:
                return alternation(ast.a[node], next);
            case M3RegexAst.REPEAT:
                return repeat(node, next);
            case M3RegexAst.GROUP:
                return group(node, next);
            case M3RegexAst.ASSERT:
                return state(ASSERT, next, -1, ast.a[node]);
            default:
                return next;
        }
    }

    private int concatForward(int child, int next) {
        if (child < 0) return next;
        return compile(child, concatForward(ast.next[child], next));
    }

    private int concatReverse(int first, int next) {
        int current = next;
        for (int child = first; child >= 0; child = ast.next[child]) {
            current = compile(child, current);
        }
        return current;
    }

    private int alternation(int child, int next) {
        int branch = compile(child, next);
        if (ast.next[child] < 0) return branch;
        return state(SPLIT, branch, alternation(ast.next[child], next), 0);
    }

    private int group(int node, int next) {
        if (reverse) return compile(ast.a[node], next);
        int index = ast.min[node];
        int close = state(SAVE, next, -1, 2 * index + 1);
        return state(SAVE, compile(ast.a[node], close), -1, 2 * index);
    }

    private int repeat(int node, int next) {
        int child = ast.a[node];
        int lower = ast.min[node];
        int upper = ast.max[node];
        boolean greedy = ast.greedy[node];
        int tail = upper < 0 ? loop(child, next, greedy) : optional(child, upper - lower, next, greedy);
        for (int copy = 0; copy < lower; copy++) {
            tail = compile(child, tail);
        }
        return tail;
    }

    private int loop(int child, int next, boolean greedy) {
        int split = state(SPLIT, -1, -1, 0);
        int body = compile(child, split);
        out[split] = greedy ? body : next;
        out2[split] = greedy ? next : body;
        return split;
    }

    private int optional(int child, int count, int next, boolean greedy) {
        int current = next;
        for (int copy = 0; copy < count; copy++) {
            int body = compile(child, current);
            current = greedy ? state(SPLIT, body, next, 0) : state(SPLIT, next, body, 0);
        }
        return current;
    }

    /** Whether class {@code id} admits {@code unit}. */
    boolean admits(int id, int unit) {
        return ast.contains(id, unit);
    }
}
