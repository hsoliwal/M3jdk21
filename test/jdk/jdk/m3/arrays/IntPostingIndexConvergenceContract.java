// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Deterministic semantics/JNI oracle for the original MAT posting-index owner.
 * Run as a plain Java 21 main, or through the JUnit wrapper in the Maven reactor.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingIndexConvergenceContract.java blob 9aed4954133d; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexConvergenceContract {
    private static final int[] KEY_POOL = {
        Integer.MIN_VALUE, -97, -1, 0, 1, 2, 3, 97, Integer.MAX_VALUE
    };
    private static int checks;
    private static int scenarios;

    private IntPostingIndexConvergenceContract() { }

    public static void runAll() {
        int[] queries = { Integer.MIN_VALUE, Integer.MAX_VALUE, 0 };
        int[] empty = {};
        IntPostingIndex zero = IntPostingIndex.ofPairs(empty, empty, 0);
        verify(zero, empty);
        verify(zero, queries);
        IntPostingIndex sparse = IntPostingIndex.ofPairs(
                new int[] {-1, -1, 0, 0, 5, 5, 5, Integer.MIN_VALUE},
                new int[] {0, 0, 1, 2, 2, 3, 3, 4}, 8);
        verify(sparse, new int[] {-1, 0});
        verify(sparse, new int[] {5, 5, Integer.MIN_VALUE, -1, 42});
        verify(sparse, empty);

        boolean requiredRefuses = false;
        if (!IntPostingIndex.nativeAvailable()) {
            try { sparse.any(IntPostingIndex.Backend.NATIVE_REQUIRED, 5); }
            catch (UnsatisfiedLinkError expected) { requiredRefuses = true; }
            require(requiredRefuses, "required JNI must refuse an absent library");
        }

        String nativePath = System.getProperty("matposting.native.path");
        if (nativePath != null && !nativePath.isBlank()) {
            IntPostingIndex.loadNative(Path.of(nativePath));
            require(IntPostingIndex.nativeAvailable(), "native library loaded");
        }

        int rows = 16_384;
        int[] values = new int[rows * 2 + 8];
        int[] ids = new int[values.length];
        for (int i = 0; i < rows; i++) {
            values[i] = 0;
            ids[i] = i;
            values[rows + i] = 1 + (i % 7);
            ids[rows + i] = i;
        }
        for (int i = rows * 2; i < values.length; i++) {
            values[i] = 99;
            ids[i] = (i - rows * 2) * 2048;
        }
        IntPostingIndex dense = IntPostingIndex.ofPairs(values, ids, rows);
        verify(dense, new int[] {99, 0});
        verify(dense, new int[] {0, 3, 3, 3});
        verify(dense, new int[] {99, 0, 2});
        verify(dense, new int[] {0});
        verify(dense, new int[] {0, 1, 2, 3, 4, 5, 6, 7});

        Random random = new Random(0x51A6_7EED_2026L);
        for (int round = 0; round < 1024; round++) {
            int universe = random.nextInt(257);
            int pairs = universe == 0 ? 0 : random.nextInt(1025);
            int[] keys = new int[pairs];
            int[] ordinal = new int[pairs];
            for (int i = 0; i < pairs; i++) {
                keys[i] = KEY_POOL[random.nextInt(KEY_POOL.length)];
                ordinal[i] = random.nextInt(universe);
            }
            int[] originalKeys = keys.clone();
            int[] originalRows = ordinal.clone();
            IntPostingIndex index = IntPostingIndex.ofPairs(keys, ordinal, universe);
            require(Arrays.equals(keys, originalKeys), "key input unchanged");
            require(Arrays.equals(ordinal, originalRows), "row input unchanged");
            for (int q = 0; q < 7; q++) {
                int[] selected = new int[random.nextInt(12)];
                for (int i = 0; i < selected.length; i++) {
                    int slot = random.nextInt(KEY_POOL.length + 3);
                    selected[i] = slot < KEY_POOL.length
                            ? KEY_POOL[slot] : 0xdead_0000 + slot;
                }
                verify(index, selected);
            }
        }
        System.out.println("MAT_POSTING_SCENARIOS=" + scenarios);
        System.out.println("MAT_POSTING_ASSERTIONS=" + checks);
        System.out.println("MAT_POSTING_BACKEND=" + (IntPostingIndex.nativeAvailable() ? "JNI_PARITY" : "JAVA"));
        System.out.println("MAT_POSTING_RESULT=PASS");
    }

    private static void verify(IntPostingIndex index, int[] query) {
        IntSelection expectedAny = IntSelection.builder(index.rows()).build();
        for (int key : query) expectedAny = expectedAny.or(index.equalSelection(key));
        IntSelection expectedAll;
        if (query.length == 0) {
            IntSelection.Builder builder = IntSelection.builder(index.rows());
            for (int i = 0; i < index.rows(); i++) builder.add(i);
            expectedAll = builder.build();
        } else {
            expectedAll = index.equalSelection(query[0]);
            for (int i = 1; i < query.length; i++) {
                expectedAll = expectedAll.and(index.equalSelection(query[i]));
            }
        }
        checkSame(expectedAny, index.any(query), "JAVA ANY", query);
        checkSame(expectedAll, index.all(query), "JAVA ALL", query);
        if (IntPostingIndex.nativeAvailable()) {
            checkSame(expectedAny, index.any(IntPostingIndex.Backend.NATIVE_REQUIRED, query),
                    "JNI ANY", query);
            checkSame(expectedAll, index.all(IntPostingIndex.Backend.NATIVE_REQUIRED, query),
                    "JNI ALL", query);
        }
        scenarios++;
    }

    private static void checkSame(IntSelection expected, IntSelection actual,
            String operation, int[] query) {
        require(expected.rows() == actual.rows(), operation + " universe");
        require(expected.size() == actual.size(), operation + " count " + Arrays.toString(query));
        for (int row = 0; row < expected.rows(); row++) {
            require(expected.contains(row) == actual.contains(row),
                    operation + " row=" + row + " keys=" + Arrays.toString(query));
        }
    }

    private static void require(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    public static void main(String[] args) { runAll(); }
}
