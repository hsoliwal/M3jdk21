/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent row-matrix oracle and exact saturation-red witness. */
public final class M3CountMinSaturationSelfTest {
    private static long checks;
    private M3CountMinSaturationSelfTest() { }

    private static void check(boolean ok, String issue) {
        checks++;
        if (!ok) throw new AssertionError(issue);
    }

    private static long mix(long value) {
        long z = value + 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static long plus(long a, long b) {
        return Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b;
    }

    static final class IndependentMatrix {
        final long[][] rows;
        final int mask;
        long total;

        IndependentMatrix(int requestedWidth, int depth) {
            int width = 1;
            while (width < requestedWidth) width <<= 1;
            rows = new long[depth][width];
            mask = width - 1;
        }

        int column(long key, int row) {
            long first = mix(key);
            long second = mix(first + 0x9e3779b97f4a7c15L * (row + 1L));
            return (int) second & mask;
        }

        long read(long key) {
            long result = Long.MAX_VALUE;
            for (int row = 0; row < rows.length; row++) {
                result = Math.min(result, rows[row][column(key, row)]);
            }
            return result;
        }

        long add(long key, long delta) {
            if (delta > 0) {
                for (int row = 0; row < rows.length; row++) {
                    int i = column(key, row);
                    rows[row][i] = plus(rows[row][i], delta);
                }
                total = plus(total, delta);
            }
            return read(key);
        }

        void merge(IndependentMatrix other) {
            for (int row = 0; row < rows.length; row++) {
                for (int i = 0; i < rows[row].length; i++) {
                    rows[row][i] = plus(rows[row][i], other.rows[row][i]);
                }
            }
            total = plus(total, other.total);
        }

        void clear() {
            for (long[] row : rows) Arrays.fill(row, 0L);
            total = 0;
        }
    }

    private static long[] keys(Random random) {
        return new long[] {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE,
                42, -42, random.nextInt(11) - 5, random.nextLong()};
    }

    private static void ordinary() {
        for (int width : new int[] {1, 2, 3, 4, 17, 128}) {
            for (int depth : new int[] {1, 2, 4, 8, 16, 32}) {
                M3LongCountMinSketch sketch = new M3LongCountMinSketch(width, depth);
                IndependentMatrix oracle = new IndependentMatrix(width, depth);
                Random random = new Random(0x2179f105a4f01c11L ^ ((long) width << 16) ^ depth);
                check(sketch.width() == oracle.rows[0].length, "normalized width");
                check(sketch.depth() == depth, "depth");
                check(sketch.payloadBytes() == 8L * depth * oracle.rows[0].length,
                        "retained primitive payload");
                for (int step = 0; step < 2400; step++) {
                    long[] values = keys(random);
                    long key = values[random.nextInt(values.length)];
                    int operation = random.nextInt(16);
                    if (operation < 11) {
                        long delta = random.nextInt(16);
                        long expected = oracle.add(key, delta);
                        long got = sketch.add(key, delta);
                        check(got == expected, "increment result width/depth/step="
                                + width + "/" + depth + "/" + step);
                    } else if (operation < 13) {
                        check(sketch.estimate(key) == oracle.read(key), "read estimate");
                    } else if (operation < 15) {
                        M3LongCountMinSketch other = new M3LongCountMinSketch(width, depth);
                        IndependentMatrix reference = new IndependentMatrix(width, depth);
                        for (int i = 0; i < 3; i++) {
                            long anotherKey = values[random.nextInt(values.length)];
                            long delta = random.nextInt(12);
                            check(other.add(anotherKey, delta) == reference.add(anotherKey, delta),
                                    "peer insert");
                        }
                        sketch.merge(other);
                        oracle.merge(reference);
                    } else {
                        sketch.clear();
                        oracle.clear();
                    }
                    check(sketch.totalCount() == oracle.total, "total count");
                    for (int k = 0; k < 3; k++) {
                        long sample = values[random.nextInt(values.length)];
                        check(sketch.estimate(sample) == oracle.read(sample),
                                "matrix read at width/depth/step=" + width + "/" + depth + "/" + step);
                    }
                }
            }
        }
        // Invalid geometry and negative delta retain original validation.
        try { new M3LongCountMinSketch(1, 0); throw new AssertionError("zero depth"); }
        catch (IllegalArgumentException expected) { checks++; }
        try { new M3LongCountMinSketch(0, 1); throw new AssertionError("zero width"); }
        catch (IllegalArgumentException expected) { checks++; }
        M3LongCountMinSketch sketch = new M3LongCountMinSketch(2, 2);
        try { sketch.add(1L, -1L); throw new AssertionError("negative delta"); }
        catch (IllegalArgumentException expected) { checks++; }
        check(sketch.totalCount() == 0L, "reject without mutation");
        System.out.println("PASS ordinary checks=" + checks);
    }

    private static void saturation() {
        for (int width : new int[] {1, 2, 4, 17, 128}) {
            for (int depth : new int[] {1, 2, 4, 8, 16, 32}) {
                M3LongCountMinSketch sketch = new M3LongCountMinSketch(width, depth);
                IndependentMatrix oracle = new IndependentMatrix(width, depth);
                long key = 0xdeadbeefL ^ width ^ ((long) depth << 32);
                // A saturated counter is a real value, not an "unseen" sentinel.
                check(sketch.add(key, Long.MAX_VALUE) == oracle.add(key, Long.MAX_VALUE),
                        "saturating insertion");
                check(sketch.totalCount() == Long.MAX_VALUE, "total saturates");
                check(sketch.estimate(key) == oracle.read(key),
                        "saturated query must not become zero");
                check(sketch.add(key, 0L) == Long.MAX_VALUE, "zero delta preserves max");
                check(sketch.add(key, 1L) == Long.MAX_VALUE, "next add stays saturated");
                M3LongCountMinSketch other = new M3LongCountMinSketch(width, depth);
                IndependentMatrix otherOracle = new IndependentMatrix(width, depth);
                check(other.add(key, Long.MAX_VALUE - 2L)
                        == otherOracle.add(key, Long.MAX_VALUE - 2L), "peer saturation");
                sketch.merge(other);
                oracle.merge(otherOracle);
                check(sketch.estimate(key) == oracle.read(key), "merged saturation");
                sketch.clear();
                oracle.clear();
                check(sketch.estimate(key) == 0L, "clear resets saturated value");
                check(sketch.totalCount() == 0L, "clear resets total");
                check(sketch.add(key, Long.MAX_VALUE - 1L)
                        == oracle.add(key, Long.MAX_VALUE - 1L), "near max insertion");
                check(sketch.add(key, 2L) == Long.MAX_VALUE, "near max increment saturates");
                check(sketch.estimate(key) == Long.MAX_VALUE, "near max query saturates");
            }
        }
        System.out.println("PASS saturation checks=" + checks);
    }

    public static void main(String[] args) {
        if (args.length == 0 || "ordinary".equals(args[0])) ordinary();
        if (args.length == 0 || "saturation".equals(args[0])) saturation();
    }
}
