// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/**
 * Deterministic Java 21 collection API and required-JNI parity oracle for skewed CSR postings.
 * This is an executable main, separate from the Maven/JUnit wrapper.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingSkewSearchContract.java blob 7ecaf0dc7f8f; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexSkewContract {
    private static long assertions;
    private static long scenarios;

    private IntPostingIndexSkewContract() { }

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void same(int[] expected, int[] actual, String reason) {
        check(Arrays.equals(expected, actual), reason);
    }

    private static int[] oracle(int universe, int[] pairsKey, int[] pairsRow, int[] query) {
        if (query.length == 0) {
            int[] all = new int[universe];
            for (int i = 0; i < universe; i++) all[i] = i;
            return all;
        }
        BitSet selected = new BitSet(universe);
        selected.set(0, universe);
        for (int key : query) {
            BitSet matching = new BitSet(universe);
            for (int i = 0; i < pairsKey.length; i++) {
                if (pairsKey[i] == key) matching.set(pairsRow[i]);
            }
            selected.and(matching);
        }
        return selected.stream().toArray();
    }

    private static void verify(IntPostingIndex index, int[] keys, int[] rows, int[] query) {
        int[] sourceKeys = keys.clone();
        int[] sourceRows = rows.clone();
        int[] requested = query.clone();
        int[] expected = oracle(index.rows(), keys, rows, query);
        same(expected, index.all(query).rowsSelected().toArray(), "Java skew/CSR");
        if (IntPostingIndex.nativeAvailable()) {
            same(expected, index.all(IntPostingIndex.Backend.NATIVE_REQUIRED, query)
                    .rowsSelected().toArray(), "JNI skew/CSR");
        }
        same(sourceKeys, keys, "input key array unchanged");
        same(sourceRows, rows, "input row array unchanged");
        same(requested, query, "query array unchanged");
        scenarios++;
    }

    private static void denseAndSkewed(boolean requireNative) {
        final int universe = 65_536;
        final int sparse = 128;
        int[] keys = new int[universe + sparse + universe / 2 + universe / 2];
        int[] rows = new int[keys.length];
        int at = 0;
        for (int i = 0; i < universe; i++) {
            keys[at] = 0; rows[at++] = i;
        }
        for (int i = 0; i < sparse; i++) {
            keys[at] = -1; rows[at++] = i * 512;
        }
        for (int i = 0; i < universe; i += 2) {
            keys[at] = 1; rows[at++] = i;
        }
        for (int i = 1; i < universe; i += 2) {
            keys[at] = 2; rows[at++] = i;
        }
        check(at == keys.length, "complete skew fixtures");
        IntPostingIndex index = IntPostingIndex.ofPairs(keys, rows, universe);
        check(index.rows() == universe, "row universe unchanged");
        check(index.keyCount() == 4, "distinct signed keys");
        check(index.postingSize(-1) == sparse, "shortest posting");
        check(index.postingSize(0) == universe, "dense posting");
        check(index.contains(-1, 0) && index.contains(-1, 512)
                && !index.contains(-1, 1), "row membership");

        verify(index, keys, rows, new int[] {0, -1});
        verify(index, keys, rows, new int[] {0, -1, -1, 0});
        verify(index, keys, rows, new int[] {0, 1});
        verify(index, keys, rows, new int[] {0, 2});
        verify(index, keys, rows, new int[] {-1, 1});
        verify(index, keys, rows, new int[] {-1, 2});
        verify(index, keys, rows, new int[] {1, 2});
        verify(index, keys, rows, new int[] {-1, 1, 2});
        verify(index, keys, rows, new int[] {-1, 1000});
        verify(index, keys, rows, new int[0]);
        if (requireNative) check(IntPostingIndex.nativeAvailable(), "native required executed");
    }

    private static void randomCases() {
        Random random = new Random(0x4D4154504F535431L);
        int[] pool = {-99, -1, 0, 1, 2, 3, 17, 1024};
        for (int round = 0; round < 1_024; round++) {
            int universe = random.nextInt(257);
            int pairCount = universe == 0 ? 0 : random.nextInt(513);
            int[] keys = new int[pairCount];
            int[] rows = new int[pairCount];
            for (int i = 0; i < pairCount; i++) {
                keys[i] = pool[random.nextInt(pool.length)];
                rows[i] = random.nextInt(universe);
            }
            IntPostingIndex index = IntPostingIndex.ofPairs(keys, rows, universe);
            int[] query = new int[random.nextInt(7)];
            for (int i = 0; i < query.length; i++) {
                query[i] = random.nextInt(7) == 0 ? 5_000 : pool[random.nextInt(pool.length)];
            }
            verify(index, keys, rows, query);
        }
    }

    public static void runAll() {
        boolean required = Boolean.getBoolean("matposting.requireNative");
        String library = System.getProperty("matposting.native.path", "");
        if (required && library.isBlank()) {
            throw new IllegalStateException("MAT_POSTING_REQUIRED_JNI_LIBRARY_MISSING");
        }
        if (!library.isBlank()) {
            IntPostingIndex.loadNative(Path.of(library));
            check(IntPostingIndex.nativeAvailable(), "native library initialized");
        }
        denseAndSkewed(required);
        randomCases();
        System.out.println("MAT_POSTING_SKEW_SCENARIOS=" + scenarios);
        System.out.println("MAT_POSTING_SKEW_ASSERTIONS=" + assertions);
        System.out.println("MAT_POSTING_SKEW_BACKEND="
                + (IntPostingIndex.nativeAvailable() ? "JAVA_AND_JNI" : "JAVA_ONLY"));
        System.out.println("MAT_POSTING_SKEW_RESULT=PASS");
    }

    public static void main(String[] args) { runAll(); }
}
