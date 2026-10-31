/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import java.util.Arrays;
import java.util.Random;

/** Dependency-free Java 21 differential and metamorphic oracle. */
public final class M3ASTLeafIndexSelfTest {
    private static long checks;
    private M3ASTLeafIndexSelfTest() { }
    private static void ok(boolean holds, String why) {
        checks++;
        if (!holds) throw new AssertionError(why);
    }
    private static void fails(Class<? extends Throwable> wanted, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable result) {
            if (wanted.isInstance(result)) return;
            throw new AssertionError("wrong exception", result);
        }
        throw new AssertionError("expected " + wanted.getName());
    }
    private static String sha(char ch) { return String.valueOf(ch).repeat(64); }
    private static M3ASTLeafIndex.FileFacts fact(int project, int module, int pack,
            String path, int[] ids, long[] flags) {
        return new M3ASTLeafIndex.FileFacts("P" + project, "M" + module,
                "pkg" + pack, path, sha('a'), ids, flags);
    }
    private static void deterministic() {
        int[] source = {9, 9};
        long[] state = {M3ASTLeafIndex.LIVE | M3ASTLeafIndex.DIRTY, M3ASTLeafIndex.LIVE};
        var a = fact(1, 1, 1, "src/A.java", source, state);
        var b = fact(1, 1, 1, "src/B.java", new int[0], new long[0]);
        var c = fact(1, 1, 2, "src/C.java", new int[] {8},
                new long[] {M3ASTLeafIndex.LIVE | M3ASTLeafIndex.TOMBSTONE});
        var d = fact(2, 2, 1, "src/D.java", new int[] {4}, new long[] {0});
        source[0] = 42; state[0] = -1;
        int[] leaked = a.atomIds(); leaked[1] = 81;
        var x = M3ASTLeafIndex.build("repo", d, b, c, a);
        var y = M3ASTLeafIndex.build("repo", a, b, c, d);
        int[] expected = {4, 4, 3, 2, 2, 1};
        int count = 0;
        for (int l = 0; l < expected.length; l++) {
            ok(x.levelCount(l) == expected[l], "level count " + l);
            count += expected[l];
        }
        ok(x.rowCount() == count, "row count");
        ok(x.sha256(x.repositoryRow()).equals(y.sha256(y.repositoryRow())), "input permutation");
        ok(x.atomCount(x.repositoryRow()) == 4, "total atoms");
        ok(x.fileCount(x.repositoryRow()) == 4, "total files");
        ok(x.stateUnion(x.repositoryRow()) == (M3ASTLeafIndex.LIVE
                | M3ASTLeafIndex.DIRTY | M3ASTLeafIndex.TOMBSTONE), "union");
        ok(x.stateIntersection(x.repositoryRow()) == 0, "intersection");
        ok(x.atomIdAtPhysical(0) == 9 && x.atomIdAtPhysical(1) == 9, "snapshots/duplicates");
        ok(Arrays.equals(x.selectOffsets(x.repositoryRow(), M3ASTLeafIndex.LIVE,
                M3ASTLeafIndex.TOMBSTONE), new int[] {0, 1}), "state selection");
        ok(Arrays.equals(x.selectOffsets(x.repositoryRow(), 0, 0), new int[] {0, 1, 2, 3}),
                "unfiltered identity");
        for (int row = 0; row < x.rowCount(); row++) {
            ok(x.sha256(row).matches("[0-9a-f]{64}"), "digest");
            if (row != x.repositoryRow()) {
                ok(x.parent(row) > row, "leaf-first parent");
                ok(x.level(x.parent(row)) == x.level(row) + 1, "adjacent parent");
            } else ok(x.parent(row) == -1, "root parent");
            for (int i = 0; i < x.childCount(row); i++) {
                int child = x.childAt(row, i);
                ok(x.parent(child) == row, "backlink");
            }
            for (int i = 0; i < x.atomCount(row); i++) {
                int physical = x.physicalOffset(row, i);
                ok(physical >= 0 && physical < 4, "physical bounds");
            }
        }
        fails(IllegalArgumentException.class,
                () -> M3ASTLeafIndex.build("repo", a, a));
        fails(IllegalArgumentException.class,
                () -> x.selectOffsets(x.repositoryRow(), 1, 1));
        fails(IndexOutOfBoundsException.class, () -> x.childAt(x.rowAt(0, 0), 0));
        fails(IndexOutOfBoundsException.class, () -> x.physicalOffset(x.repositoryRow(), 4));
        fails(IllegalArgumentException.class,
                () -> fact(1, 1, 1, "../bad.java", new int[0], new long[0]));
        fails(IllegalArgumentException.class,
                () -> fact(1, 1, 1, "valid.java", new int[] {-1}, new long[] {1}));
        fails(IllegalArgumentException.class,
                () -> fact(1, 1, 1, "valid.java", new int[] {1}, new long[0]));
        var empty = M3ASTLeafIndex.build("empty");
        ok(empty.rowCount() == 1 && empty.repositoryRow() == 0, "empty hierarchy");
        ok(empty.atomCount(0) == 0 && empty.fileCount(0) == 0, "empty counts");
        ok(empty.selectOffsets(0, 0, 0).length == 0, "empty mask");
    }
    private static void randomized() {
        Random random = new Random(0x3300abcefL);
        for (int round = 0; round < 400; round++) {
            int n = random.nextInt(32);
            var files = new M3ASTLeafIndex.FileFacts[n];
            for (int f = 0; f < n; f++) {
                int atoms = random.nextInt(10);
                int[] ids = new int[atoms]; long[] flags = new long[atoms];
                for (int i = 0; i < atoms; i++) {
                    ids[i] = random.nextInt(7); flags[i] = random.nextLong();
                }
                files[f] = fact(random.nextInt(3), random.nextInt(4), random.nextInt(5),
                        "src/F" + f + ".java", ids, flags);
            }
            var fixed = M3ASTLeafIndex.build("replay", files);
            String before = fixed.sha256(fixed.repositoryRow());
            for (int i = files.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                var swap = files[i]; files[i] = files[j]; files[j] = swap;
            }
            var replay = M3ASTLeafIndex.build("replay", files);
            ok(before.equals(replay.sha256(replay.repositoryRow())), "permutation fixed point");
            for (int row = 0; row < replay.rowCount(); row++) {
                long required = (round & 1) == 0 ? 1L : Long.MIN_VALUE;
                long forbidden = (round & 1) == 0 ? 2L : 0x100L;
                int[] selected = replay.selectOffsets(row, required, forbidden);
                int matches = 0;
                for (int i = 0; i < replay.atomCount(row); i++) {
                    int p = replay.physicalOffset(row, i);
                    long word = replay.stateAtPhysical(p);
                    if ((word & required) == required && (word & forbidden) == 0) {
                        ok(matches < selected.length && selected[matches] == p, "mask order");
                        matches++;
                    }
                }
                ok(matches == selected.length, "mask count");
                if (row != replay.repositoryRow()) {
                    ok(replay.parent(row) > row, "topological order");
                }
            }
        }
    }
    public static void main(String[] args) {
        deterministic(); randomized();
        System.out.println("PASS M3ASTLeafIndexSelfTest checks=" + checks);
    }
}
