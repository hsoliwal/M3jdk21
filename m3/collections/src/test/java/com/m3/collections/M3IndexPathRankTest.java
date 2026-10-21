/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** JUnit entry point for the standalone Java21 and existing selector oracles. */
class M3IndexPathRankTest {
    @Test void boundedDeterministicDifferential() {
        M3IndexPathRankSelfTest.main(new String[0]);
    }

    @Test void agreesWithOriginalJavaMaskSelector() {
        int[] rows = {3, 3, -5, 8, 0, 8};
        int[] slots = {0, 1, 2, 3, 4, 2};
        int[][] stages = {{0, 1, -1, 3, 1}, {1, 0, -1, 1, 3}};
        int[] needs = {1, 1};
        int[] excludes = {2, 2};
        int[] expected = M3IndexPathMasks.select(rows, slots, stages, needs, excludes,
                0, rows.length, 1, M3IndexPathMasks.Backend.JAVA);
        M3IndexPathRank result = M3IndexPathRank.snapshot(rows, slots, stages, needs,
                excludes, 0, rows.length, 1);
        assertArrayEquals(expected, result.toArray());
    }
}
