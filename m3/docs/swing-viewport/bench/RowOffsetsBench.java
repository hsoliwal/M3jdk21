/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.Random;

import javax.swing.SizeSequence;

/**
 * Rule 21 measurement for leaf 3 (JList row offsets over BasicListUI.cellHeights): one dense lane
 * of n row heights, built once per layout and then queried by getCellBounds (start of a row) and
 * convertLocationToRow (row at an offset). Candidates: the stock loop, an int prefix lane, the
 * in-repo javax.swing.SizeSequence, a Fenwick tree and sqrt blocks. Prints one TSV row per
 * candidate and metric: {@code candidate n metric value unit note}.
 * Usage: {@code java -XX:+UseSerialGC RowOffsetsBench [n] [queries]}.
 */
public final class RowOffsetsBench {

    interface Offsets {
        int startOf(int row);

        int rowAt(int offset);
    }

    static int[] heights;
    static long sink;

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
        int q = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;
        Random rnd = new Random(42);
        heights = new int[n];
        for (int i = 0; i < n; i++) {
            heights[i] = 16 + rnd.nextInt(25);
        }
        int[] rows = new int[q];
        int[] ys = new int[q];
        long total = 0;
        for (int h : heights) {
            total += h;
        }
        for (int i = 0; i < q; i++) {
            rows[i] = rnd.nextInt(n);
            ys[i] = (int) (rnd.nextDouble() * total);
        }
        Offsets[] all = {prefix(heights), sizeSequence(heights), fenwick(heights), blocks(heights, 1024)};
        for (int i = 0; i < Math.min(q, 20_000); i++) {
            for (Offsets o : all) {
                if (o.startOf(rows[i]) != all[0].startOf(rows[i]) || o.rowAt(ys[i]) != all[0].rowAt(ys[i])) {
                    throw new AssertionError("candidates disagree at query " + i);
                }
            }
        }
        System.out.println("# candidates agree on " + Math.min(q, 20_000) + " start and row queries");
        for (int round = 0; round < 3; round++) {
            boolean report = round == 2;
            measure("prefix_int_lane", n, rows, ys, report, () -> prefix(heights), 4L * (n + 1));
            measure("size_sequence", n, rows, ys, report, () -> sizeSequence(heights), 4L * n);
            measure("fenwick", n, rows, ys, report, () -> fenwick(heights), 4L * (n + 1));
            measure("sqrt_blocks_1024", n, rows, ys, report, () -> blocks(heights, 1024), 4L * (n / 1024 + 2));
        }
        stockLoop(n, report(n));
        System.out.println("# sink " + sink);
    }

    static int report(int n) {
        return Math.min(n, 2000);
    }

    interface Builder {
        Offsets build();
    }

    static void measure(String name, int n, int[] rows, int[] ys, boolean report, Builder b, long bytes) {
        long t0 = System.nanoTime();
        Offsets o = null;
        for (int i = 0; i < 5; i++) {
            o = b.build();
        }
        double buildMs = (System.nanoTime() - t0) / 5 / 1e6;
        t0 = System.nanoTime();
        long s = 0;
        for (int r : rows) {
            s += o.startOf(r);
        }
        double startNs = (System.nanoTime() - t0) / (double) rows.length;
        t0 = System.nanoTime();
        for (int y : ys) {
            s += o.rowAt(y);
        }
        double rowAtNs = (System.nanoTime() - t0) / (double) ys.length;
        t0 = System.nanoTime();
        for (int i = 0; i < rows.length; i++) {
            s += o.startOf(n - 1);
        }
        double endNs = (System.nanoTime() - t0) / (double) rows.length;
        sink += s;
        if (report) {
            row(name, n, "build", buildMs, "ms", "once per layout (BasicListUI replaces cellHeights)");
            row(name, n, "start_of_random", startNs, "ns/op", "getCellBounds y");
            row(name, n, "start_of_end", endNs, "ns/op", "getCellBounds y of the last row");
            row(name, n, "row_at_random", rowAtNs, "ns/op", "convertLocationToRow");
            row(name, n, "extra_heap", bytes / 1e6, "MB", "besides cellHeights");
        }
    }

    static void stockLoop(int n, int reps) {
        long t0 = System.nanoTime();
        long s = 0;
        for (int k = 0; k < reps; k++) {
            int y = 0;
            for (int i = 0; i < n - 1; i++) {
                y += heights[i];
            }
            s += y;
        }
        sink += s;
        row("stock_loop", n, "start_of_end", (System.nanoTime() - t0) / (double) reps, "ns/op",
                "BasicListUI.getCellBounds loop (O(row))");
    }

    static void row(String name, int n, String metric, double v, String unit, String note) {
        System.out.printf("%s\t%d\t%s\t%.3f\t%s\t%s%n", name, n, metric, v, unit, note);
    }

    // ---- candidates (same algorithms as the production class and the alternatives) ----------

    static Offsets prefix(int[] h) {
        int[] start = new int[h.length + 1];
        int sum = 0;
        for (int i = 0; i < h.length; i++) {
            sum += h[i];
            start[i + 1] = sum;
        }
        return new Offsets() {
            public int startOf(int row) {
                return start[row];
            }

            public int rowAt(int offset) {
                int lo = 1;
                int hi = start.length - 1;
                int res = start.length - 1;
                while (lo <= hi) {
                    int mid = (lo + hi) >>> 1;
                    if (start[mid] > offset) {
                        res = mid - 1;
                        hi = mid - 1;
                    } else {
                        lo = mid + 1;
                    }
                }
                return res;
            }
        };
    }

    static Offsets sizeSequence(int[] h) {
        SizeSequence s = new SizeSequence(h);
        return new Offsets() {
            public int startOf(int row) {
                return s.getPosition(row);
            }

            public int rowAt(int offset) {
                return s.getIndex(offset);
            }
        };
    }

    static Offsets fenwick(int[] h) {
        int n = h.length;
        int[] t = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            t[i] += h[i - 1];
            int j = i + (i & -i);
            if (j <= n) {
                t[j] += t[i];
            }
        }
        int top = Integer.highestOneBit(Math.max(1, n));
        return new Offsets() {
            public int startOf(int row) {
                int s = 0;
                for (int i = row; i > 0; i -= i & -i) {
                    s += t[i];
                }
                return s;
            }

            public int rowAt(int offset) {
                int pos = 0;
                int rem = offset;
                for (int step = top; step > 0; step >>= 1) {
                    int next = pos + step;
                    if (next <= n && t[next] <= rem) {
                        pos = next;
                        rem -= t[next];
                    }
                }
                return pos;
            }
        };
    }

    static Offsets blocks(int[] h, int b) {
        int nb = (h.length + b - 1) / b;
        int[] blockStart = new int[nb + 1];
        for (int k = 0; k < nb; k++) {
            int s = 0;
            for (int i = k * b, end = Math.min(h.length, i + b); i < end; i++) {
                s += h[i];
            }
            blockStart[k + 1] = blockStart[k] + s;
        }
        return new Offsets() {
            public int startOf(int row) {
                int k = row / b;
                int s = blockStart[k];
                for (int i = k * b; i < row; i++) {
                    s += h[i];
                }
                return s;
            }

            public int rowAt(int offset) {
                int lo = 0;
                int hi = nb - 1;
                while (lo < hi) {
                    int mid = (lo + hi + 1) >>> 1;
                    if (blockStart[mid] <= offset) {
                        lo = mid;
                    } else {
                        hi = mid - 1;
                    }
                }
                int y = blockStart[lo];
                int i = lo * b;
                while (i < h.length && y + h[i] <= offset) {
                    y += h[i++];
                }
                return i;
            }
        };
    }
}
