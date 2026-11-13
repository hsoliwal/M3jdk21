// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors

import java.util.Random;

/** Actual extracted before/after Java leaf versus independent graph reachability. */
public final class RegexClosureProof {
    private static long checks;
    private static long pairs;
    private static long digest;

    private RegexClosureProof() {}

    private static void equal(long actual, long expected, int count, long stars, long bits) {
        checks++;
        if (actual != expected) {
            throw new AssertionError("closure mismatch n=" + count + " stars=" + stars
                    + " bits=" + bits + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void pair(int count, long stars, long bits, long[] rows) {
        long expected = RegexGraph.close(bits, stars);
        long before = BeforeClose.apply(bits, rows);
        long after = AfterClose.apply(bits, stars);
        equal(before, expected, count, stars, bits);
        equal(after, expected, count, stars, bits);
        equal(after | bits, after, count, stars, bits);
        equal(AfterClose.apply(after, stars), after, count, stars, bits);
        equal(after & ~((1L << (count + 1)) - 1), 0, count, stars, bits);
        digest = Long.rotateLeft(digest, 7) ^ after ^ stars ^ bits;
        pairs++;
    }

    private static void exhaustive() {
        for (int count = 1; count <= 9; count++) {
            for (long stars = 0; stars < (1L << count); stars++) {
                long[] rows = RegexGraph.closures(RegexGraph.atoms(count, stars, 0));
                for (long bits = 0; bits < (1L << (count + 1)); bits++) {
                    pair(count, stars, bits, rows);
                }
            }
        }
        equal(pairs, 699_048L, 9, 0, 0);
    }

    private static void boundary(long stars) {
        long[] rows = RegexGraph.closures(RegexGraph.atoms(32, stars, 0));
        long all = (1L << 33) - 1;
        long[] seeds = {0, 1, 1L << 31, 1L << 32, all, stars,
                all ^ stars, 0x1aaaa_aaaaL, 0x15555_5555L};
        for (long bits : seeds) pair(32, stars, bits, rows);
        for (int bit = 0; bit <= 32; bit++) {
            pair(32, stars, 1L << bit, rows);
            pair(32, stars, (1L << bit) | (1L << 32), rows);
        }
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no args");
        exhaustive();
        boundary(0);
        boundary(0xffff_ffffL);
        boundary(0xaaaa_aaaaL);
        boundary(0x5555_5555L);
        for (int low = 0; low < 32; low++) {
            for (int high = low; high < 32; high++) {
                long run = ((1L << (high - low + 1)) - 1) << low;
                boundary(run);
                if (high + 2 < 32) boundary(run | (1L << (high + 2)));
            }
        }
        Random random = new Random(0x4d3352434c4f5345L);
        for (int trial = 0; trial < 20_000; trial++) {
            long stars = Integer.toUnsignedLong(random.nextInt());
            long bits = random.nextLong() & ((1L << 33) - 1);
            long[] rows = RegexGraph.closures(RegexGraph.atoms(32, stars, 0));
            pair(32, stars, bits, rows);
            long added = bits | (1L << random.nextInt(33));
            long smaller = AfterClose.apply(bits, stars);
            long larger = AfterClose.apply(added, stars);
            equal(smaller | larger, larger, 32, stars, bits);
        }
        System.out.println("M3_REGEX_CLOSURE_PROOF PASS pairs=" + pairs + " checks=" + checks
                + " digest=" + Long.toUnsignedString(digest, 16)
                + " source_extracted=true simulated_owner=false matched_m3jdk=false");
    }
}
