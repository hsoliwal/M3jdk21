/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.tree.TreePath;

import sun.swing.tree.M3ExpandedState;

/**
 * Rule 21 measurement for leaf 5 (the JTree expansion table): the stock loops of
 * {@code getExpandedDescendants} (before {@code isVisible}) and {@code removeDescendantToggledPaths}
 * against {@code sun.swing.tree.M3ExpandedState} with its fallback included. The table holds every
 * inner node of a tree of fan-out 10 and depth {@code d} as an expanded key (10^(d-1) + ... + 1 keys).
 * Queries: a fresh expansion (a deepest inner key, no keys below), a re-expansion with a fresh path
 * instance (the key itself and its subtree), a subtree removal (re-inserted after each run). Prints
 * {@code candidate keys scenario value unit note}. Usage: {@code java --patch-module
 * java.desktop=<patch> --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED ExpansionTableBench}.
 */
public final class ExpansionTableBench {

    static long sink;

    public static void main(String[] args) {
        for (int depth = 4; depth <= 6; depth++) {
            run(depth);
        }
        System.out.println("# sink " + sink);
    }

    static void run(int depth) {
        List<TreePath> inner = new ArrayList<>();
        build(new TreePath("r"), 1, depth, inner);
        Hashtable<TreePath, Boolean> stock = new Hashtable<>();
        M3ExpandedState m3 = new M3ExpandedState();
        for (TreePath p : inner) {
            stock.put(p, Boolean.TRUE);
            m3.put(p, Boolean.TRUE);
        }
        int n = inner.size();
        TreePath leafKey = inner.get(n - 1);
        TreePath mid = inner.get(1);
        TreePath midFresh = new TreePath(mid.getPath());
        List<TreePath> sub = new ArrayList<>();
        for (TreePath p : inner) {
            if (mid.isDescendant(p)) {
                sub.add(p);
            }
        }
        int[] ties = new int[1];
        for (int round = 0; round < 3; round++) {
            boolean report = round == 2;
            row(report, "stock_loop", n, "fresh_expansion", time(() -> stockBelow(stock, leafKey).size()), "");
            row(report, "m3_index", n, "fresh_expansion", time(() -> m3Below(m3, stock, leafKey, ties)), "");
            row(report, "stock_loop", n, "re_expansion_" + sub.size(), time(() -> stockBelow(stock, midFresh).size()), "");
            row(report, "m3_index", n, "re_expansion_" + sub.size(), time(() -> m3Below(m3, m3, midFresh, ties)),
                    "the stock loop runs when two keys share a bucket");
            row(report, "stock_loop", n, "remove_subtree_" + sub.size(), time(() -> {
                stockRemove(stock, mid);
                for (TreePath p : sub) {
                    stock.put(p, Boolean.TRUE);
                }
                return stock.size();
            }), "includes re-inserting the subtree");
            row(report, "m3_index", n, "remove_subtree_" + sub.size(), time(() -> {
                m3.removeDescendants(mid);
                for (TreePath p : sub) {
                    m3.put(p, Boolean.TRUE);
                }
                return m3.size();
            }), "includes re-inserting the subtree");
        }
        if (!Arrays.equals(stockBelow(stock, midFresh).toArray(), m3.expandedKeysBelow(midFresh) == null
                ? stockBelow(m3, midFresh).toArray() : m3.expandedKeysBelow(midFresh))) {
            throw new AssertionError("candidates disagree");
        }
        System.out.println("# keys " + n + ": re-expansion fell back to the stock loop " + ties[0] + " times");
        long base = used();
        Hashtable<TreePath, Boolean> s2 = new Hashtable<>();
        for (TreePath p : inner) {
            s2.put(p, Boolean.TRUE);
        }
        long stockBytes = used() - base;
        base = used();
        M3ExpandedState m2 = new M3ExpandedState();
        for (TreePath p : inner) {
            m2.put(p, Boolean.TRUE);
        }
        long m3Bytes = used() - base;
        sink += s2.size() + m2.size();
        System.out.printf("stock_loop\t%d\theap_table\t%.2f\tMB\tHashtable entries (paths shared)%n", n, stockBytes / 1e6);
        System.out.printf("m3_index\t%d\theap_table\t%.2f\tMB\tHashtable entries + trie (paths shared)%n", n, m3Bytes / 1e6);
    }

    static long used() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    static void build(TreePath p, int level, int depth, List<TreePath> inner) {
        if (level == depth) {
            return;
        }
        inner.add(p);
        for (int i = 0; i < 10; i++) {
            build(p.pathByAddingChild("n" + level + "." + i), level + 1, depth, inner);
        }
    }

    static int m3Below(M3ExpandedState m3, Hashtable<TreePath, Boolean> fallback, TreePath parent, int[] ties) {
        TreePath[] keys = m3.expandedKeysBelow(parent);
        if (keys == null) {
            ties[0]++;
            return stockBelow(fallback, parent).size();
        }
        return keys.length;
    }

    static List<TreePath> stockBelow(Hashtable<TreePath, Boolean> t, TreePath parent) {
        List<TreePath> out = new ArrayList<>();
        for (Enumeration<TreePath> e = t.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            Boolean v = t.get(p);
            if (p != parent && v != null && v && parent.isDescendant(p)) {
                out.add(p);
            }
        }
        return out;
    }

    static void stockRemove(Hashtable<TreePath, Boolean> t, TreePath path) {
        List<TreePath> d = new ArrayList<>();
        for (Enumeration<TreePath> e = t.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            if (path.isDescendant(p)) {
                d.add(p);
            }
        }
        for (TreePath p : d) {
            t.remove(p);
        }
    }

    interface Op {
        int run();
    }

    /** Median of 15 runs, in microseconds. */
    static double time(Op op) {
        long[] t = new long[15];
        for (int i = 0; i < t.length; i++) {
            long t0 = System.nanoTime();
            sink += op.run();
            t[i] = System.nanoTime() - t0;
        }
        Arrays.sort(t);
        return t[7] / 1e3;
    }

    static void row(boolean report, String candidate, int keys, String scenario, double us, String note) {
        if (report) {
            System.out.printf("%s\t%d\t%s\t%.2f\tus\t%s%n", candidate, keys, scenario, us, note);
        }
    }
}
