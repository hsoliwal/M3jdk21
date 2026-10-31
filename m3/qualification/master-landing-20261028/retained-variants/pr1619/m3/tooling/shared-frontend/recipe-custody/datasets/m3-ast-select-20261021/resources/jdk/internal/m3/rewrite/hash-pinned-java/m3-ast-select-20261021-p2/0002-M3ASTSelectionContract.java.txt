// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/**
 * Independent, deterministic source/AST/bit-mask selection oracle.
 * Builds actual source-owned M3ASTIndex rows and never invokes a parser.
 */
public final class M3ASTSelectionContract {
    private static long assertions;
    private M3ASTSelectionContract() { }

    private static void check(boolean value, String reason) {
        assertions++;
        if (!value) throw new AssertionError(reason);
    }

    private static void same(int[] expected, int[] actual, String reason) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(reason + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    @FunctionalInterface private interface Probe { void run(); }
    private static void rejects(Class<? extends Throwable> type, Probe probe, String reason) {
        assertions++;
        try { probe.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError(reason + " unexpected " + problem, problem);
        }
        throw new AssertionError(reason + " missing " + type.getName());
    }

    private static int[] expected(M3ASTIndex index, String name, int required, int forbidden) {
        int[] rows = new int[index.nodeCount()];
        int count = 0;
        for (int i = 0; i < index.nodeCount(); i++) {
            if ((name == null || name.equals(index.kind(i)))
                    && (index.flags(i) & required) == required
                    && (index.flags(i) & forbidden) == 0) rows[count++] = i;
        }
        return Arrays.copyOf(rows, count);
    }

    private static void visitChildren(List<List<Integer>> children, int root,
            List<Integer> rows) {
        rows.add(root);
        for (int child : children.get(root)) visitChildren(children, child, rows);
    }

    private static int[] treeExpected(M3ASTIndex index, List<List<Integer>> children,
            int root, boolean includeRoot, String kind, int required, int forbidden) {
        List<Integer> preorder = new ArrayList<>();
        if (includeRoot) visitChildren(children, root, preorder);
        else for (int child : children.get(root)) visitChildren(children, child, preorder);
        return preorder.stream().filter(row ->
                (kind == null || kind.equals(index.kind(row)))
                && (index.flags(row) & required) == required
                && (index.flags(row) & forbidden) == 0).mapToInt(Integer::intValue).toArray();
    }

    private static void directed() {
        M3File file = M3File.of(URI.create("memory:///A.java"), "0123456789abcdef0123456789");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC, 8);
        int root = builder.add(-1, "UNIT", 0, file.length(), M3ASTIndex.EXACT_RANGE);
        int left = builder.add(root, "CLASS", 0, 10,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.FINAL);
        int right = builder.add(root, "CLASS", 11, file.length(), M3ASTIndex.EXACT_RANGE);
        int method = builder.add(left, "METHOD", 2, 5,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.STATIC);
        int rightMethod = builder.add(right, "METHOD", 13, 16, M3ASTIndex.EXACT_RANGE);
        int variable = builder.add(left, "VARIABLE", 5, 8,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.FINAL);
        M3ASTIndex index = builder.seal();
        check(index.source() == file, "same canonical M3File instance");
        same(new int[]{root, left, method, variable, right, rightMethod},
                index.selectedSubtree(root, true, null, 0, 0).toArray(),
                "original children differ from numeric row order");
        same(new int[]{left, method, variable},
                index.selectedSubtree(left, true, null, 0, 0).toArray(),
                "selected child subtree excludes interleaved sibling");
        same(new int[]{method, variable},
                index.selectedSubtree(left, false, null, 0, 0).toArray(),
                "exclude only selected root");
        same(new int[]{variable}, index.selectedSubtree(left, false, null,
                M3ASTIndex.FINAL, M3ASTIndex.STATIC).toArray(),
                "full flags filtered without duplicating nodes");
        same(new int[]{method, rightMethod}, index.selectedNodes("METHOD",
                0, 0).toArray(), "global physical source row order");
        same(new int[]{method}, index.selectedNodes("METHOD", M3ASTIndex.STATIC,
                0).toArray(), "positive flag match");
        same(new int[]{rightMethod}, index.selectedNodes("METHOD", 0,
                M3ASTIndex.STATIC).toArray(), "negative flag mask");
        same(new int[]{}, index.selectedNodes("UNKNOWN", 0, 0).toArray(),
                "missing kind is empty, not a new parser task");
        same(new int[]{}, index.selectedSubtree(left, false, "UNKNOWN",
                0, 0).toArray(), "missing child kind is empty");
        check(index.primitiveBytes() >= index.nodeCount() * 8L * Integer.BYTES,
                "source-index primitive backing retained");
        rejects(IllegalArgumentException.class,
                () -> index.selectedNodes(null, M3ASTIndex.STATIC,
                        M3ASTIndex.STATIC).count(), "reject contradictory masks");
        rejects(IllegalArgumentException.class,
                () -> index.selectedSubtree(root, true, null,
                        M3ASTIndex.FINAL, M3ASTIndex.FINAL).count(), "subtree contradiction");
        rejects(IndexOutOfBoundsException.class,
                () -> index.selectedSubtree(index.nodeCount(), true, null, 0, 0),
                "invalid physical source handle");
        rejects(NullPointerException.class,
                () -> index.forEachSelectedNode(null, 0, 0, null), "missing callback");
        RuntimeException own = new IllegalStateException("original consumer failed");
        rejects(IllegalStateException.class,
                () -> index.forEachSelectedNode(null, 0, 0, row -> { throw own; }),
                "callback failure is never hidden");
    }

    private static void randomized() {
        Random random = new Random(0x4d3341535453454cL);
        for (int round = 0; round < 260; round++) {
            int length = 1 + random.nextInt(325);
            M3File file = M3File.of(URI.create("memory:///D" + round + ".java"),
                    "source generation " + round);
            M3ASTIndex.Builder builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC,
                    length);
            List<List<Integer>> children = new ArrayList<>();
            String[] kinds = {"UNIT", "CLASS", "METHOD", "BLOCK", "LITERAL"};
            int[] choices = {0, M3ASTIndex.STATIC, M3ASTIndex.FINAL,
                    M3ASTIndex.STATIC | M3ASTIndex.FINAL, M3ASTIndex.RECOVERED,
                    M3ASTIndex.INTERFACE};
            for (int row = 0; row < length; row++) {
                int parent = row == 0 ? -1 : random.nextInt(row);
                String kind = row == 0 ? "UNIT" : kinds[random.nextInt(kinds.length)];
                int flags = choices[random.nextInt(choices.length)];
                int actual = builder.add(parent, kind, -1, -1, flags);
                check(actual == row, "original physical atom handle");
                children.add(new ArrayList<>());
                if (parent >= 0) children.get(parent).add(row);
            }
            M3ASTIndex index = builder.seal();
            long bytes = index.primitiveBytes();
            String generation = file.utf16Sha256();
            for (int pass = 0; pass < 9; pass++) {
                String kind = pass % 3 == 0 ? null : pass % 3 == 1 ? "METHOD" : "UNKNOWN";
                int required = pass % 2 == 0 ? M3ASTIndex.STATIC : 0;
                int forbidden = pass % 2 == 0 ? M3ASTIndex.FINAL : M3ASTIndex.RECOVERED;
                int[] expected = expected(index, kind, required, forbidden);
                same(expected, index.selectedNodes(kind, required, forbidden).toArray(),
                        "global indexed match on round " + round);
                ArrayList<Integer> callbacks = new ArrayList<>();
                int count = index.forEachSelectedNode(kind, required, forbidden,
                        callbacks::add);
                check(count == expected.length, "callback count equals selected nodes");
                same(expected, callbacks.stream().mapToInt(Integer::intValue).toArray(),
                        "original order callbacks");
                int root = random.nextInt(length);
                for (boolean include : new boolean[]{true, false}) {
                    int[] oracle = treeExpected(index, children, root, include,
                            kind, required, forbidden);
                    same(oracle, index.selectedSubtree(root, include,
                            kind, required, forbidden).toArray(),
                            "true parent/sibling DFS source traversal");
                }
            }
            check(bytes == index.primitiveBytes(),
                    "filtered/sorted views retain no extra index or duplicated source");
            check(file.utf16Sha256().equals(generation) && index.source() == file,
                    "replay doesn't rewrite the source generation");
        }
    }

    private static void wideWithoutRecursion() {
        M3File file = M3File.of(URI.create("memory:///wide.java"), "wide");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC, 50_005);
        int root = builder.add(-1, "ROOT", 0, file.length(), M3ASTIndex.EXACT_RANGE);
        for (int i = 1; i <= 50_000; i++) {
            int node = builder.add(root, "ITEM", -1, -1,
                    i % 3 == 0 ? M3ASTIndex.FINAL : 0);
            check(node == i, "stable physical row handle");
        }
        M3ASTIndex index = builder.seal();
        check(index.selectedSubtree(root, true, "ITEM", 0, 0).count() == 50_000,
                "no recursive traversal of wide source grammar");
        check(index.selectedSubtree(root, false, "ITEM", M3ASTIndex.FINAL, 0).count()
                        == 50_000 / 3,
                "stable high-cardinality state-mask selection");
    }

    public static void main(String[] args) {
        directed();
        randomized();
        wideWithoutRecursion();
        System.out.println("M3_AST_SELECTION_PASS assertions=" + assertions);
    }
}
