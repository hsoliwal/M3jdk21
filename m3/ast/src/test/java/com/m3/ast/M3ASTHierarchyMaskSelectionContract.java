// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/** Standalone Java 21 contract; no JUnit dependency or production allocations measured. */
public final class M3ASTHierarchyMaskSelectionContract {
    private static int checks;
    private M3ASTHierarchyMaskSelectionContract() { }

    public static void main(String[] args) {
        Random random = new Random(0x5a75a1f019L);
        List<M3ASTHierarchy.FileFact> files = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            int n = 1 + random.nextInt(16);
            int[] atoms = new int[n];
            byte[] states = new byte[n];
            for (int j = 0; j < n; j++) {
                atoms[j] = 1 + random.nextInt(16384);
                states[j] = (byte) random.nextInt(256);
            }
            int family = 1 << (i % 13);
            if (i % 5 == 0) family |= 1 << ((i + 2) % 13);
            files.add(new M3ASTHierarchy.FileFact("p" + (i / 500),
                    "m" + (i / 100), "pkg" + (i / 10),
                    "src/F" + i + ".java", hex(i), hex(i + 37001), family, atoms, states));
        }
        M3ASTHierarchy tree = M3ASTHierarchy.build("collection-source-fixture", files);
        eq(2000, tree.totalFiles());
        int used = tree.totalOccurrences();
        eq(used, tree.level(M3ASTHierarchy.Level.L0_FILE_FACTS).occurrenceCount(0)
                + countOtherRows(tree));
        String before = tree.rootSha256();
        int[] flags = {0, 1, 2, 7, 0x40, 0x80, 0xF0, 0xFF};
        int[] families = {0, 1, 2, 4, 8, 0x10, 0x80, 0x100, 0x1000,
                M3ASTHierarchy.ALL_COLLECTION_FAMILIES, 0x5, 0x180};
        for (int family : families) {
            for (int mask : flags) {
                int[] reference = expected(tree, family, mask);
                int[] selected = M3ASTHierarchyMaskSelection.coordinates(tree, family, mask);
                int[] handles = M3ASTHierarchyMaskSelection.atomHandles(tree, family, mask);
                eq(reference.length, M3ASTHierarchyMaskSelection.count(tree, family, mask));
                eq(reference, selected);
                eq(reference.length, handles.length);
                for (int i = 0; i < selected.length; i++) {
                    eq(tree.atomHandle(selected[i]), handles[i]);
                }
                List<Integer> observed = new ArrayList<>();
                M3ASTHierarchyMaskSelection.forEachCoordinate(tree, family, mask, observed::add);
                eq(reference.length, observed.size());
                for (int i = 0; i < reference.length; i++) eq(reference[i], observed.get(i));
                if (selected.length > 0) {
                    int original = selected[0]; selected[0] = -1;
                    eq(original, M3ASTHierarchyMaskSelection.coordinates(tree, family, mask)[0]);
                }
            }
        }
        eq(before, tree.rootSha256());
        eq(used, M3ASTHierarchyMaskSelection.count(tree, 0, 0));
        eq(0, M3ASTHierarchyMaskSelection.count(tree,
                M3ASTHierarchy.ALL_COLLECTION_FAMILIES, 0));
        expect(IllegalArgumentException.class,
                () -> M3ASTHierarchyMaskSelection.coordinates(tree, -1, 0));
        expect(IllegalArgumentException.class,
                () -> M3ASTHierarchyMaskSelection.count(tree, 0, 256));
        expect(IllegalArgumentException.class,
                () -> M3ASTHierarchyMaskSelection.atomHandles(tree, 0, -1));
        expect(IllegalArgumentException.class,
                () -> M3ASTHierarchyMaskSelection.forEachCoordinate(tree, 1 << 13, 0, index -> {}));
        expect(NullPointerException.class,
                () -> M3ASTHierarchyMaskSelection.count(null, 0, 0));
        expect(NullPointerException.class,
                () -> M3ASTHierarchyMaskSelection.forEachCoordinate(tree, 0, 0, null));
        boolean[] fieldTypeViolation = {false};
        for (var field : M3ASTHierarchyMaskSelection.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) fieldTypeViolation[0] = true;
        }
        ok(!fieldTypeViolation[0]);
        System.out.println("PASS M3ASTHierarchyMaskSelectionContract checks=" + checks
                + " files=" + tree.totalFiles() + " atoms=" + tree.totalOccurrences()
                + " root=" + tree.rootSha256());
    }

    private static int countOtherRows(M3ASTHierarchy tree) {
        var plane = tree.level(M3ASTHierarchy.Level.L0_FILE_FACTS);
        int sum = 0;
        for (int i = 1; i < plane.size(); i++) sum += plane.occurrenceCount(i);
        return sum;
    }
    private static int[] expected(M3ASTHierarchy tree, int family, int mask) {
        var plane = tree.level(M3ASTHierarchy.Level.L0_FILE_FACTS);
        int[] selected = new int[tree.totalOccurrences()]; int written = 0;
        for (int f = 0; f < plane.size(); f++) {
            if ((plane.familyMask(f) & family) != family) continue;
            for (int k = 0; k < plane.childCount(f); k++) {
                int index = plane.childOrdinal(f, k);
                if ((tree.stateMask(index) & mask) == mask) selected[written++] = index;
            }
        }
        return Arrays.copyOf(selected, written);
    }
    private static String hex(int seed) {
        return "%064x".formatted((long) seed + 1);
    }
    private static void eq(long expected, long value) {
        if (expected != value) throw new AssertionError(expected + " != " + value);
        checks++;
    }
    private static void eq(String expected, String value) {
        if (!expected.equals(value)) throw new AssertionError("digest changed");
        checks++;
    }
    private static void eq(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) throw new AssertionError("selected coordinates differ");
        checks++;
    }
    private static void ok(boolean value) {
        if (!value) throw new AssertionError("expected true");
        checks++;
    }
    private static void expect(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) { checks++; return; }
            throw new AssertionError("wrong failure type", failure);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }
}
