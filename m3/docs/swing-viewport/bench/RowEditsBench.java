/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.Arrays;
import java.util.Random;

import sun.swing.list.M3RowPieces;

/**
 * Rule 21 measurement for leaf 4 (JList row sizes kept between layouts): the bookkeeping cost of
 * following list data events on two int lanes of n row sizes until the next layout, which needs
 * fresh lanes with the kept rows copied and the new rows marked. Measuring the new rows (renderer
 * calls) is the same for every candidate and is not included. Candidates: a piece table edited per
 * event and materialized once per layout ({@code sun.swing.list.M3RowPieces}), and shifting both
 * lanes on every event (System.arraycopy, then one copy for the fresh array BasicListUI needs).
 * Scenarios between two layouts: 1 insert at the front, 40 inserts at random rows, 1,000 appends,
 * 1,000 front inserts, 100 changes, 100 removals. Prints {@code candidate n scenario value unit}.
 * Usage: {@code java --add-exports java.desktop/sun.swing.list=ALL-UNNAMED --patch-module
 * java.desktop=<patch> RowEditsBench [n]}.
 */
public final class RowEditsBench {

    /** One kind of edit sequence between two layouts: kind 0 insert, 1 remove, 2 change. */
    record Scenario(String name, int[] kind, int[] at) {
    }

    static long sink;

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
        Random rnd = new Random(42);
        int[] h = new int[n];
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            h[i] = 16 + rnd.nextInt(25);
            w[i] = 40 + rnd.nextInt(200);
        }
        Scenario[] all = {
            scenario("insert_front_1", 1, 0, k -> 0, rnd, n),
            scenario("insert_random_40", 40, 0, k -> -1, rnd, n),
            scenario("append_1000", 1000, 0, k -> n + k, rnd, n),
            scenario("insert_front_1000", 1000, 0, k -> 0, rnd, n),
            scenario("change_random_100", 100, 2, k -> -1, rnd, n),
            scenario("remove_random_100", 100, 1, k -> -1, rnd, n),
        };
        for (Scenario s : all) {
            check(s, h, n);
        }
        System.out.println("# candidates agree on every scenario");
        for (int round = 0; round < 3; round++) {
            for (Scenario s : all) {
                long pieces = time(() -> pieces(s, h, w, n));
                long shift = timeShift(s, h, w, n);
                if (round == 2) {
                    System.out.printf("piece_table\t%d\t%s\t%.3f\tms%n", n, s.name(), pieces / 1e6);
                    System.out.printf("shift_per_event\t%d\t%s\t%.3f\tms%n", n, s.name(), shift / 1e6);
                }
            }
        }
        System.out.println("# sink " + sink);
    }

    interface Pos {
        int at(int k);
    }

    static Scenario scenario(String name, int count, int kind, Pos pos, Random rnd, int n) {
        int[] kinds = new int[count];
        int[] at = new int[count];
        int size = n;
        for (int k = 0; k < count; k++) {
            kinds[k] = kind;
            int p = pos.at(k);
            at[k] = p >= 0 ? Math.min(p, size) : rnd.nextInt(kind == 0 ? size + 1 : size);
            size += kind == 0 ? 1 : kind == 1 ? -1 : 0;
        }
        return new Scenario(name, kinds, at);
    }

    /** Median of 7 runs, in ns. */
    static long time(Runnable r) {
        long[] t = new long[7];
        for (int i = 0; i < t.length; i++) {
            long t0 = System.nanoTime();
            r.run();
            t[i] = System.nanoTime() - t0;
        }
        Arrays.sort(t);
        return t[3];
    }

    /** Piece table: O(pieces) per event, one O(n) materialization into fresh lanes. */
    static int[][] pieces(Scenario s, int[] h, int[] w, int n) {
        M3RowPieces p = new M3RowPieces();
        p.reset(n);
        for (int k = 0; k < s.kind().length; k++) {
            int a = s.at()[k];
            switch (s.kind()[k]) {
                case 0 -> p.insert(a, 1);
                case 1 -> p.remove(a, a);
                default -> p.change(a, a);
            }
        }
        int m = p.size();
        int[] nh = new int[m];
        int[] nw = new int[m];
        for (int i = 0, row = 0; i < p.count(); row += p.length(i), i++) {
            int st = p.start(i);
            if (st == M3RowPieces.NEW) {
                Arrays.fill(nh, row, row + p.length(i), -1);
            } else {
                System.arraycopy(h, st, nh, row, p.length(i));
                System.arraycopy(w, st, nw, row, p.length(i));
            }
        }
        sink += nh[m / 2] + nw[m / 3];
        return new int[][] {nh, nw};
    }

    /** Median of 7 runs of {@link #shift}, in ns; the lanes it edits are prepared outside the timing. */
    static long timeShift(Scenario s, int[] h0, int[] w0, int n) {
        long[] t = new long[7];
        for (int i = 0; i < t.length; i++) {
            int[] h = Arrays.copyOf(h0, n + s.kind().length);
            int[] w = Arrays.copyOf(w0, n + s.kind().length);
            long t0 = System.nanoTime();
            shift(s, h, w, n);
            t[i] = System.nanoTime() - t0;
        }
        Arrays.sort(t);
        return t[3];
    }

    /**
     * Shifting both kept lanes (capacity n + edits) on every event, then one copy into the fresh
     * array BasicListUI needs.
     */
    static int[][] shift(Scenario s, int[] h, int[] w, int n) {
        int size = n;
        for (int k = 0; k < s.kind().length; k++) {
            int a = s.at()[k];
            switch (s.kind()[k]) {
                case 0 -> {
                    System.arraycopy(h, a, h, a + 1, size - a);
                    System.arraycopy(w, a, w, a + 1, size - a);
                    h[a] = -1;
                    size++;
                }
                case 1 -> {
                    System.arraycopy(h, a + 1, h, a, size - a - 1);
                    System.arraycopy(w, a + 1, w, a, size - a - 1);
                    size--;
                }
                default -> h[a] = -1;
            }
        }
        int[] nh = Arrays.copyOf(h, size);
        sink += nh[size / 2] + w[size / 3];
        return new int[][] {nh, w};
    }

    static void check(Scenario s, int[] h, int n) {
        int[] w = h.clone();
        int[] a = pieces(s, h, w, n)[0];
        int cap = n + s.kind().length;
        int[] b = shift(s, Arrays.copyOf(h, cap), Arrays.copyOf(w, cap), n)[0];
        if (!Arrays.equals(a, b)) {
            throw new AssertionError("candidates disagree on " + s.name());
        }
    }
}
