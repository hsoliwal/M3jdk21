/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/** Actual-owner public trace differential plus an optional private census invariant. */
public final class M3ViewportSignCensusProof {
    private static long checks, digest;
    private static Field census;
    private static final int[] HEIGHTS = {Integer.MIN_VALUE, -21, -2, -1, 0, 9, 14,
            Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
    private static final RuntimeException FAILURE = new RuntimeException("frozen row callback");
    private M3ViewportSignCensusProof() { }
    private static void record(long value) { digest = digest * 31 + value; }
    private static void equal(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual + " @" + checks);
        record(actual);
    }
    private static void check(boolean value) { equal(1, value ? 1 : 0); }

    private static final class Row implements M3RowTreeItem {
        final int id;
        final M3RowTree tree;
        int height, slot = -1, last = -1, fail;
        boolean visible;
        Runnable entered;
        Row(M3RowTree tree, int id, int height, boolean visible) {
            this.tree = tree; this.id = id; this.height = height; this.visible = visible;
        }
        private void event(int kind) {
            record(id); record(kind); snapshot(tree);
            if (kind == fail) throw FAILURE;
        }
        @Override public int getRowIndex() { return last; }
        @Override public int rowTreeSlot() { return slot; }
        @Override public boolean isVisible() { event(1); return visible; }
        @Override public int getHeight() { event(2); return height; }
        @Override public void enteredRowTree(int value) {
            slot = value; event(3);
            if (entered != null) entered.run();
        }
        @Override public void leftRowTree(int value) { slot = -1; last = value; event(4); }
    }

    private static void snapshot(M3RowTree tree) {
        int size = tree.size(), positives = 0, negatives = 0;
        long total = 0;
        for (int i = 0; i < size; i++) {
            int weight = tree.extentAt(i);
            equal(total, tree.extentBefore(i)); equal(positives, tree.rankOf(i));
            if (weight > 0) { equal(i, tree.positionOfRank(positives)); positives++; }
            if (weight < 0) negatives++;
            total += weight; record(((Row) tree.get(i)).id);
        }
        equal(total, tree.totalExtent()); equal(positives, tree.visibleCount());
        if (census != null) {
            try {
                int actual = census.getInt(tree);
                if (actual != negatives) throw new AssertionError("negative census " + actual + " != " + negatives);
            } catch (IllegalAccessException failure) { throw new AssertionError(failure); }
        }
        for (long offset : new long[]{Long.MIN_VALUE, -1, 0, 5, total / 2, total - 1, total, total + 1, Long.MAX_VALUE}) {
            int actual = tree.rankAtExtent(offset);
            if (offset < 0) equal(-1, actual);
            else if (negatives == 0) {
                long prefix = 0; int expected = 0;
                for (int i = 0; i < size; i++) {
                    int weight = tree.extentAt(i);
                    if (weight == 0) continue;
                    if (prefix + weight > offset) break;
                    prefix += weight; expected++;
                }
                equal(expected, actual);
            } else record(actual); // Signed legacy descent is compared between actual source lanes.
        }
    }

    private static void mutations() {
        for (int seed = 0; seed < 9; seed++) {
            var tree = new M3RowTree(); int id = seed * 10000;
            for (int step = 0; step < 360; step++) {
                int size = tree.size();
                int at = size == 0 ? 0 : (step * 17 + seed) % size;
                int height = HEIGHTS[(step * 7 + seed) % HEIGHTS.length];
                switch (step % 6) {
                    case 0 -> tree.add(size == 0 ? 0 : at, new Row(tree, id++, height, (step & 4) == 0));
                    case 1 -> {
                        if (size > 0) {
                            Row row = (Row) tree.get(at); row.height = height; row.visible = !row.visible;
                            tree.update(row); tree.update(row); // Equal-weight reweigh must not double count.
                        }
                    }
                    case 2 -> {
                        if (size > 0) tree.set(at, new Row(tree, id++, height, true));
                    }
                    case 3 -> {
                        if (size > 0 && step % 18 == 3) tree.remove(at);
                        else tree.add(new Row(tree, id++, height, true));
                    }
                    case 4 -> {
                        if (size > 0) {
                            Row row = (Row) tree.get(at); row.visible = false; tree.update(row);
                            row.visible = true; row.height = height; tree.update(row);
                        }
                    }
                    default -> {
                        if (step % 90 == 89) tree.clear();
                        else if (size > 0) {
                            var cursor = tree.iterator();
                            tree.add(new Row(tree, id++, 0, false));
                            try { cursor.next(); throw new AssertionError("lost fail-fast"); }
                            catch (ConcurrentModificationException expected) { checks++; record(17); }
                        }
                    }
                }
                snapshot(tree);
            }
            tree.clear(); snapshot(tree);
            tree.add(new Row(tree, id++, Integer.MAX_VALUE, true)); snapshot(tree);
            tree.clear(); tree.add(new Row(tree, id, 0, false)); snapshot(tree);
        }
    }

    private static M3RowTree trap() {
        var tree = new M3RowTree();
        tree.add(new Row(tree, 1, 9, true));
        tree.add(new Row(tree, 2, -21, true));
        tree.add(new Row(tree, 3, 14, true));
        equal(5, tree.totalExtent()); equal(0, tree.rankAtExtent(5));
        return tree;
    }
    private static void callbacks() {
        for (int scenario = 0; scenario < 11; scenario++) {
            var tree = trap(); Row fresh = new Row(tree, 100 + scenario, Integer.MAX_VALUE, true);
            Row old = (Row) tree.get(1);
            Runnable operation;
            switch (scenario) {
                case 0, 1, 2 -> { fresh.fail = scenario + 1; operation = () -> tree.add(1, fresh); }
                case 3 -> { old.fail = 4; operation = () -> tree.set(1, fresh); }
                case 4 -> { fresh.fail = 3; operation = () -> tree.set(1, fresh); }
                case 5 -> { fresh.fail = 2; operation = () -> tree.set(1, fresh); }
                case 6, 7 -> { old.fail = scenario - 5; old.height = 0; operation = () -> tree.update(old); }
                case 8 -> { old.fail = 4; operation = () -> tree.remove(1); }
                case 9 -> { old.fail = 4; operation = tree::clear; }
                default -> { fresh.fail = 2; operation = () -> tree.add(fresh); }
            }
            try { operation.run(); throw new AssertionError("callback did not fail"); }
            catch (RuntimeException actual) { check(actual == FAILURE); }
            snapshot(tree);
            fresh.fail = 0; old.fail = 0;
            for (int i = 0; i < tree.size(); i++) ((Row) tree.get(i)).fail = 0;
            tree.clear(); snapshot(tree);
            tree.add(new Row(tree, 500, 0, false)); snapshot(tree);
        }
        var tree = trap();
        Row fresh = new Row(tree, 777, Integer.MAX_VALUE, true);
        fresh.entered = () -> { tree.update(fresh); snapshot(tree); fresh.height = 0; tree.update(fresh); };
        tree.set(1, fresh); snapshot(tree);
        tree.add(0, new Row(tree, 888, -2, true)); snapshot(tree);
        tree.remove(0); snapshot(tree);
    }
    private static void signSpectrum() {
        for (int height : HEIGHTS) for (int next : HEIGHTS) for (boolean visible : new boolean[]{false, true}) {
            var tree = new M3RowTree(); var row = new Row(tree, 19, height, visible);
            tree.add(row); snapshot(tree);
            row.height = next; tree.update(row); snapshot(tree);
            row.visible = !visible; tree.update(row); snapshot(tree);
            tree.remove(0); snapshot(tree);
        }
        var hidden = new M3RowTree();
        for (int i = 0; i < 129; i++) hidden.add(new Row(hidden, i, Integer.MAX_VALUE, false));
        equal(0, hidden.rankAtExtent(0)); equal(0, hidden.rankAtExtent(Long.MAX_VALUE));
        hidden.clear(); snapshot(hidden);
    }
    public static void main(String[] args) {
        checks = 0; digest = 59; census = null;
        try { census = M3RowTree.class.getDeclaredField("negativeWeights"); census.setAccessible(true); }
        catch (NoSuchFieldException absent) {
            if (args.length == 0 || !args[0].equals("baseline")) throw new AssertionError("candidate census absent", absent);
        }
        signSpectrum(); mutations(); callbacks();
        System.out.println("VIEWPORT_SIGN_PASS checks=" + checks + " digest=" + Long.toUnsignedString(digest));
    }
}
