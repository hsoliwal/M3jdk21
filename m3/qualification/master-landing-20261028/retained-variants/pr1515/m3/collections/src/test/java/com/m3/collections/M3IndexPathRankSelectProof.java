// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent scalar scan oracle for rank-to-occurrence decoding. */
public final class M3IndexPathRankSelectProof {
    private static long checks;
    private M3IndexPathRankSelectProof() { }

    public static void main(String[] args) {
        int[] rows = new int[256], offsets = new int[256], states = new int[256];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i % 3 == 0 ? Integer.MIN_VALUE : (i % 3 == 1 ? Integer.MAX_VALUE : -7);
            offsets[i] = i;
        }
        for (int shift = 0; shift < 64; shift += 16) {
            for (int mask = 0; mask < 65536; mask++) {
                Arrays.fill(states, 0);
                long bits = ((long) mask) << shift;
                for (int bit = 0; bit < 64; bit++) states[64 + bit] = (int) ((bits >>> bit) & 1);
                check(rows, offsets, states);
            }
        }
        Random random = new Random(0x5e1ec7L);
        for (int trial = 0; trial < 2048; trial++) {
            for (int i = 0; i < states.length; i++) states[i] = random.nextInt(4) == 0 ? 0 : 1;
            check(rows, offsets, states);
        }
        Arrays.fill(states, 1);
        check(rows, offsets, states); // includes selected rank 63 within every full word
        Arrays.fill(states, 0);
        check(rows, offsets, states);
        System.out.println("PASS rank select: exhaustive 16-bit masks at four word offsets, 2048 full-width paths; checks=" + checks);
    }

    private static void check(int[] rows, int[] offsets, int[] states) {
        M3IndexPathRank rank = M3IndexPathRank.snapshot(rows, offsets, new int[][]{states},
                new int[]{1}, new int[]{0}, 0, rows.length, 1);
        int admitted = 0;
        for (int source = 0; source < states.length; source++) {
            if (states[source] == 0) continue;
            equal(source, rank.selectedSourceOrdinal(admitted));
            equal(rows[source], rank.selectedRowId(admitted));
            equal(admitted, rank.rank(source));
            admitted++;
        }
        equal(admitted, rank.size());
        var window = rank.window(63, 193);
        int selected = 0;
        for (int source = 63; source < 193; source++) {
            if (states[source] == 0) continue;
            equal(source - 63, window.selectedSourceOrdinal(selected));
            equal(rows[source], window.selectedRowId(selected));
            selected++;
        }
        equal(selected, window.size());
        if (admitted == 0 || admitted == rows.length) {
            rejects(() -> rank.selectedSourceOrdinal(-1));
            rejects(() -> rank.selectedSourceOrdinal(rank.size()));
            rejects(() -> rank.selectedSourceOrdinal(Integer.MAX_VALUE));
            rejects(() -> window.selectedSourceOrdinal(-1));
            rejects(() -> window.selectedSourceOrdinal(window.size()));
        }
    }

    private static void equal(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError("expected " + expected + " actual " + actual);
    }
    private static void rejects(Runnable action) {
        checks++;
        try { action.run(); } catch (IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("rank bounds accepted");
    }
}
