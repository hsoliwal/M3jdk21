// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0

import jdk.m3.tools.cpu.M3PackedSpatialIndex;
import java.util.Arrays;
import java.util.SplittableRandom;

/** Independent naïve rectangle-intersection oracle. */
public final class M3PackedSpatialIndexContract {
    private static long checks;

    private M3PackedSpatialIndexContract() { }

    private static void check(boolean condition, String failure) {
        checks++;
        if (!condition) { throw new AssertionError(failure); }
    }

    private static void refuses(Class<? extends Throwable> type, Runnable call) {
        try { call.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "wrong refusal: " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getSimpleName() + " refusal");
    }

    private static int[] naive(int[] left, int[] bottom, int[] right, int[] top,
            int a, int b, int c, int d) {
        int[] hits = new int[left.length];
        int used = 0;
        for (int id = 0; id < left.length; id++) {
            if (left[id] < right[id] && bottom[id] < top[id]
                    && a < c && b < d && left[id] < c && right[id] > a
                    && bottom[id] < d && top[id] > b) {
                hits[used++] = id;
            }
        }
        return Arrays.copyOf(hits, used);
    }

    private static void spatialCases() {
        SplittableRandom r = new SplittableRandom(0x92026112L);
        for (int iteration = 0; iteration < 220; iteration++) {
            int n = r.nextInt(300);
            int[] l = new int[n], b = new int[n], u = new int[n], t = new int[n];
            for (int k = 0; k < n; k++) {
                int x = r.nextInt(-50, 51), y = r.nextInt(-50, 51);
                int w = r.nextInt(0, 31), h = r.nextInt(0, 31);
                l[k] = x; b[k] = y; u[k] = x + w; t[k] = y + h;
            }
            M3PackedSpatialIndex.Index index = M3PackedSpatialIndex.build(l, b, u, t);
            check(index.size() == n, "spatial size");
            for (int query = 0; query < 240; query++) {
                int a = r.nextInt(-70, 71), c = a + r.nextInt(0, 40);
                int bottom = r.nextInt(-70, 71), top = bottom + r.nextInt(0, 40);
                int[] expected = naive(l, b, u, t, a, bottom, c, top);
                int[] actual = index.query(a, bottom, c, top, n, 1_000_000L);
                check(Arrays.equals(expected, actual), "spatial mismatch");
                if (actual.length > 0) {
                    int[] clipped = actual.clone();
                    clipped[0] = -2;
                    check(Arrays.equals(expected, index.query(a, bottom, c, top,
                            n, 1_000_000L)), "spatial output alias");
                    refuses(IllegalStateException.class,
                            () -> index.query(a, bottom, c, top, 0, 1_000_000L));
                }
            }
            if (n > 0) {
                l[0] = Integer.MAX_VALUE;
                check(index.size() == n, "immutable index size");
            }
        }
        M3PackedSpatialIndex.Index extremes = M3PackedSpatialIndex.build(
                new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE - 1},
                new int[] {-1, -1},
                new int[] {Integer.MIN_VALUE + 1, Integer.MAX_VALUE},
                new int[] {1, 1});
        check(Arrays.equals(extremes.query(Integer.MIN_VALUE, -1,
                Integer.MIN_VALUE + 1, 1, 2, 10L), new int[] {0}), "spatial int min");
        check(Arrays.equals(extremes.query(Integer.MAX_VALUE - 1, -1,
                Integer.MAX_VALUE, 1, 2, 10L), new int[] {1}), "spatial int max");
        refuses(IllegalStateException.class,
                () -> extremes.query(Integer.MIN_VALUE, -1, Integer.MAX_VALUE, 1,
                        2, 0L));
        refuses(IllegalArgumentException.class,
                () -> extremes.query(5, 0, 4, 1, 2, 10L));
    }

    public static void main(String[] args) {
        spatialCases();
        System.out.println("M3_PACKED_SPATIAL_INDEX_PASS checks=" + checks);
    }
}
