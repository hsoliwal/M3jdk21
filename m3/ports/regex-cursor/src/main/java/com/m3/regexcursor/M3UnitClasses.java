// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * Symbol equivalence classes of one pattern (the UTF-16/token analogue of regex-automata byte
 * classes): two symbols share a class when every pattern class treats them alike.
 *
 * <p>Lanes: {@code bounds[c]} is the first symbol of class {@code c} (also its representative);
 * {@code low[u]} maps symbols below 256 directly; {@code quit[c]} marks quit classes (the UTF-16
 * surrogate range for text programs).</p>
 */
final class M3UnitClasses {
    final int[] bounds;
    final int[] low = new int[256];
    final boolean[] quit;
    final int count;

    private M3UnitClasses(int[] bounds, int quitLo, int quitHi) {
        this.bounds = bounds;
        this.count = bounds.length;
        this.quit = new boolean[count];
        for (int c = 0; c < count; c++) {
            quit[c] = quitLo >= 0 && bounds[c] >= quitLo && bounds[c] <= quitHi;
        }
        for (int unit = 0; unit < 256; unit++) {
            low[unit] = search(unit);
        }
    }

    /** Partition of {@code [0, alphabetMax]} by every class boundary of {@code ast}. */
    static M3UnitClasses of(M3RegexAst ast, int quitLo, int quitHi) {
        int[] cuts = new int[ast.rangeSize + 3];
        int count = 0;
        cuts[count++] = 0;
        for (int index = 0; index < ast.rangeSize; index += 2) {
            cuts[count++] = ast.ranges[index];
            if (ast.ranges[index + 1] < ast.alphabetMax) {
                cuts = Arrays.copyOf(cuts, Math.max(cuts.length, count + 4));
                cuts[count++] = ast.ranges[index + 1] + 1;
            }
        }
        if (quitLo >= 0) {
            cuts = Arrays.copyOf(cuts, count + 2);
            cuts[count++] = quitLo;
            cuts[count++] = quitHi + 1;
        }
        return new M3UnitClasses(Arrays.stream(cuts, 0, count).sorted().distinct().toArray(), quitLo, quitHi);
    }

    /** Class of {@code unit}. */
    int classOf(int unit) {
        return unit < 256 ? low[unit] : search(unit);
    }

    private int search(int unit) {
        int found = Arrays.binarySearch(bounds, unit);
        return found >= 0 ? found : -found - 2;
    }
}
