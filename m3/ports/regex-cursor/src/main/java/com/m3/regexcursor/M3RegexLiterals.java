// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * Literal facts of a parsed pattern: the gate rungs that run before any automaton.
 *
 * <p>Rules (same shape as {@code M3RegexRequirement} in m3/ports/compiletime and the Synexia
 * {@code MIndexRegexProgram} planner): the top-level sequence is flattened through CONCAT and
 * GROUP only; a run of consecutive one-symbol classes is required in every match; the leading run
 * is a prefix of every match; a pattern that is exactly one run without groups or assertions is a
 * pure literal.</p>
 */
final class M3RegexLiterals {
    final int[] pure;
    final int[] prefix;
    final int[] required;

    private M3RegexLiterals(int[] pure, int[] prefix, int[] required) {
        this.pure = pure;
        this.prefix = prefix;
        this.required = required;
    }

    static M3RegexLiterals of(M3RegexAst ast) {
        int[] items = new int[ast.size + 1];
        int count = flatten(ast, ast.root, items, 0);
        int[] units = new int[count];
        boolean allSingles = true;
        for (int index = 0; index < count; index++) {
            units[index] = ast.singleton(items[index]);
            allSingles &= units[index] >= 0;
        }
        int[] pure = allSingles && ast.groupCount == 0 && !ast.assertions ? units : null;
        return new M3RegexLiterals(pure, prefix(units), longestRun(units));
    }

    private static int flatten(M3RegexAst ast, int node, int[] out, int count) {
        int op = ast.op[node];
        if (op == M3RegexAst.GROUP) return flatten(ast, ast.a[node], out, count);
        if (op == M3RegexAst.EMPTY) return count;
        if (op != M3RegexAst.CONCAT) {
            out[count] = node;
            return count + 1;
        }
        int total = count;
        for (int child = ast.a[node]; child >= 0; child = ast.next[child]) {
            total = flatten(ast, child, out, total);
        }
        return total;
    }

    private static int[] prefix(int[] units) {
        int length = 0;
        while (length < units.length && units[length] >= 0) {
            length++;
        }
        return length == 0 ? null : Arrays.copyOf(units, length);
    }

    private static int[] longestRun(int[] units) {
        int bestStart = 0;
        int bestLength = 0;
        int start = 0;
        for (int index = 0; index <= units.length; index++) {
            if (index == units.length || units[index] < 0) {
                if (index - start > bestLength) {
                    bestStart = start;
                    bestLength = index - start;
                }
                start = index + 1;
            }
        }
        return bestLength == 0 ? null : Arrays.copyOfRange(units, bestStart, bestStart + bestLength);
    }

    /** Presence-bit mask of every unit of {@code literal}; 0 for {@code null}. */
    static long mask(int[] literal) {
        long mask = 0L;
        if (literal != null) {
            for (int unit : literal) {
                mask |= M3CursorFacts.bit(unit);
            }
        }
        return mask;
    }
}
