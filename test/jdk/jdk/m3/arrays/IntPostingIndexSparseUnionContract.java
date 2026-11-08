// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/** Real-product Java 21 + JNI regression for sparse/dense immutable CSR union. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingSparseUnionContract.java blob 8c25149e5543; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexSparseUnionContract {
    private static long assertions;
    private static long scenarios;
    private static Method nativeUnion;
    private static Field offsetsField;
    private static Field ordinalsField;

    private IntPostingIndexSparseUnionContract() { }

    private static void require(boolean value, String context) {
        assertions++;
        if (!value) throw new AssertionError(context);
    }

    private static void same(int[] expected, int[] actual, String context) {
        require(Arrays.equals(expected, actual), context
                + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }

    private static void initReflection() throws Exception {
        nativeUnion = IntPostingIndex.class.getDeclaredMethod("unionNative",
                int[].class, int[].class, int[].class, int.class);
        nativeUnion.setAccessible(true);
        offsetsField = IntPostingIndex.class.getDeclaredField("offsets");
        ordinalsField = IntPostingIndex.class.getDeclaredField("rowOrdinals");
        offsetsField.setAccessible(true);
        ordinalsField.setAccessible(true);
    }

    private static int[] rawUnion(int[] postings, int[] offsets, int[] selected, int universe)
            throws Exception {
        return (int[]) nativeUnion.invoke(null, postings, offsets, selected, universe);
    }

    private static int[] positions(int[] directory, int[] query) {
        int[] buf = new int[query.length];
        int found = 0;
        for (int k : query) {
            int pos = Arrays.binarySearch(directory, k);
            if (pos >= 0) buf[found++] = pos;
        }
        Arrays.sort(buf, 0, found);
        int unique = 0;
        for (int i = 0; i < found; i++) {
            if (unique == 0 || buf[unique - 1] != buf[i]) buf[unique++] = buf[i];
        }
        return Arrays.copyOf(buf, unique);
    }

    private static int[] oracle(int universe, int[] pairKeys, int[] pairRows, int[] query) {
        BitSet selected = new BitSet(universe);
        for (int i = 0; i < pairKeys.length; i++) {
            for (int key : query) {
                if (key == pairKeys[i]) {
                    selected.set(pairRows[i]);
                    break;
                }
            }
        }
        return selected.stream().toArray();
    }

    private static void exercise(int[] keys, int[] rows, int universe, int[] query) throws Exception {
        int[] keysBefore = keys.clone();
        int[] rowsBefore = rows.clone();
        int[] queryBefore = query.clone();
        IntPostingIndex index = IntPostingIndex.ofPairs(keys, rows, universe);
        int[] expected = oracle(universe, keys, rows, query);
        same(expected, index.any(query).rowsSelected().toArray(), "Java union");
        same(expected, index.any(IntPostingIndex.Backend.NATIVE_REQUIRED, query)
                .rowsSelected().toArray(), "native-required Java API");
        int[] postings = ((int[]) ordinalsField.get(index));
        int[] offsets = ((int[]) offsetsField.get(index));
        int[] postingsBefore = postings.clone();
        int[] offsetsBefore = offsets.clone();
        int[] directory = index.keys();
        int[] selected = positions(directory, query);
        int[] selectedBefore = selected.clone();
        int[] direct = rawUnion(postings, offsets, selected, universe);
        same(expected, direct, "raw native union");
        require(direct != postings && direct != selected, "native output detached from inputs");
        same(postingsBefore, postings, "CSR posting immutable");
        same(offsetsBefore, offsets, "CSR offsets immutable");
        same(selectedBefore, selected, "selected lane immutable");
        same(keysBefore, keys, "key input immutable");
        same(rowsBefore, rows, "row input immutable");
        same(queryBefore, query, "query immutable");
        scenarios++;
    }

    private static void failInvalid(int[] postings, int[] offsets, int[] selected, int universe)
            throws Exception {
        try {
            rawUnion(postings, offsets, selected, universe);
            throw new AssertionError("invalid native batch accepted");
        } catch (InvocationTargetException error) {
            require(error.getCause() instanceof IllegalArgumentException,
                    "invalid CSR must refuse with IllegalArgumentException");
        }
    }

    private static void structured() throws Exception {
        final int universe = 1_000_000;
        int[] keys = new int[320];
        int[] rows = new int[320];
        for (int i = 0; i < 320; i++) {
            keys[i] = 100 + (i % 8);
            rows[i] = i * 3011 % universe;
        }
        int[][] cases = {
            {}, {100}, {100, 100}, {101, 102}, {101, 102, 103, 104},
            {100, 101, 102, 103, 104, 105, 106, 107}, {-100}, {100, -100},
            {9999, -100, 101}, {107, 106, 105, 104}, {100, 100, 100, 101}
        };
        for (int[] query : cases) exercise(keys, rows, universe, query);
        // Two-list overlap, duplicate-pair canonicalization and signed keys.
        exercise(new int[] {-100, -100, -99, -99, -99, 3, 3, 3},
                new int[] {1, 1, 1, 2, 5, 2, 3, 5}, 9,
                new int[] {-100, -99, 3, 3});
        // Dense bitmap path; both selected postings cover large fractions of the universe.
        int[] denseKeys = new int[8_192];
        int[] denseRows = new int[denseKeys.length];
        for (int i = 0; i < denseKeys.length; i++) {
            denseKeys[i] = i & 1;
            denseRows[i] = i >>> 1;
        }
        exercise(denseKeys, denseRows, 4_096, new int[] {0, 1});
        exercise(denseKeys, denseRows, 4_096, new int[] {0});
        exercise(denseKeys, denseRows, 4_096, new int[0]);
        // Empty universe valid only with no rows.
        exercise(new int[0], new int[0], 0, new int[] {-1, 1});
        exercise(new int[0], new int[0], 0, new int[0]);
        // Native validates all fields even in its empty and single-key fast paths.
        failInvalid(new int[] {2, 2}, new int[] {0, 2}, new int[] {0}, 10);
        failInvalid(new int[] {0, 2}, new int[] {0, 2}, new int[] {0, 0}, 10);
        failInvalid(new int[] {1}, new int[] {1, 1}, new int[0], 10);
        failInvalid(new int[] {-1}, new int[] {0, 1}, new int[] {0}, 10);
        failInvalid(new int[] {10}, new int[] {0, 1}, new int[] {0}, 10);
        failInvalid(new int[] {1}, new int[] {0, 1}, new int[] {0}, -1);
        failInvalid(new int[] {1}, new int[] {0, 1}, new int[] {1}, 10);
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x524157554E494F4EL);
        for (int round = 0; round < 1_024; round++) {
            int universe = random.nextInt(2_049);
            int count = universe == 0 ? 0 : random.nextInt(385);
            int[] keys = new int[count];
            int[] rows = new int[count];
            for (int i = 0; i < count; i++) {
                keys[i] = random.nextInt(23) - 11;
                rows[i] = random.nextInt(universe);
            }
            int[] query = new int[random.nextInt(16)];
            for (int i = 0; i < query.length; i++) query[i] = random.nextInt(31) - 15;
            exercise(keys, rows, universe, query);
        }
    }

    private static void randomSparseRaw() throws Exception {
        Random random = new Random(0x534F52544544524FL);
        final int universe = 1_000_000;
        for (int trial = 0; trial < 2_048; trial++) {
            int lanes = 2 + random.nextInt(24);
            int[] offsets = new int[lanes + 1];
            List<Integer> postingRows = new ArrayList<>();
            BitSet expected = new BitSet(universe);
            for (int lane = 0; lane < lanes; lane++) {
                offsets[lane] = postingRows.size();
                BitSet unique = new BitSet(universe);
                int cardinality = random.nextInt(65);
                for (int j = 0; j < cardinality; j++) {
                    int row = random.nextInt(5) == 0
                            ? random.nextInt(universe)
                            : random.nextInt(2_048);
                    unique.set(row);
                }
                for (int row = unique.nextSetBit(0); row >= 0;
                        row = unique.nextSetBit(row + 1)) {
                    postingRows.add(row);
                    expected.set(row);
                }
            }
            offsets[lanes] = postingRows.size();
            int[] postings = postingRows.stream().mapToInt(Integer::intValue).toArray();
            int[] selected = new int[lanes];
            for (int i = 0; i < lanes; i++) selected[i] = i;
            int[] before = postings.clone();
            int[] boundaries = offsets.clone();
            int[] positions = selected.clone();
            int[] output = rawUnion(postings, offsets, selected, universe);
            same(expected.stream().toArray(), output, "random sparse multiway heap #" + trial);
            same(before, postings, "sparse source posting immutability");
            same(boundaries, offsets, "sparse CSR geometry immutability");
            same(positions, selected, "sparse key positions immutability");
            scenarios++;
        }
        // Wide 256-way heap: duplicate rows across many independent source lists.
        final int lanes = 256;
        int[] postings = new int[lanes];
        int[] offsets = new int[lanes + 1];
        int[] positions = new int[lanes];
        BitSet unique = new BitSet(universe);
        for (int i = 0; i < lanes; i++) {
            offsets[i] = i;
            positions[i] = i;
            postings[i] = (i * 101) % 1031;
            unique.set(postings[i]);
        }
        offsets[lanes] = lanes;
        same(unique.stream().toArray(), rawUnion(postings, offsets, positions, universe),
                "256-way packed-heap posting union");
    }

    private static void extremeSparseNativeOnly() throws Exception {
        // No Java million/one-billion-row M3IntSelection materialization for this test.
        int universe = 1_000_000_000;
        int[] postings = {1, 5, 100_000_000, 5, 7, 100_000_000, 7, 10, 700_000_000};
        int[] offsets = {0, 3, 6, 9};
        same(new int[] {1, 5, 7, 10, 100_000_000, 700_000_000},
                rawUnion(postings, offsets, new int[] {0, 1, 2}, universe),
                "billion-row sparse heap avoids universe bitmap");
        same(new int[] {1, 5, 100_000_000},
                rawUnion(postings, offsets, new int[] {0}, universe),
                "billion-row single posting direct output");
        same(new int[0], rawUnion(postings, offsets, new int[0], universe),
                "billion-row zero postings empty output");
    }

    public static void runAll() throws Exception {
        String library = System.getProperty("matposting.native.path", "");
        if (library.isBlank()) throw new IllegalStateException("MAT_SPARSE_UNION_REQUIRES_JNI");
        IntPostingIndex.loadNative(Path.of(library));
        require(IntPostingIndex.nativeAvailable(), "source-built native enabled");
        initReflection();
        structured();
        randomized();
        randomSparseRaw();
        if (Boolean.getBoolean("matposting.sparse.extreme")) extremeSparseNativeOnly();
        System.out.println("MAT_SPARSE_UNION_SCENARIOS=" + scenarios);
        System.out.println("MAT_SPARSE_UNION_ASSERTIONS=" + assertions);
        System.out.println("MAT_SPARSE_UNION_RESULT=PASS");
    }

    public static void main(String[] args) throws Exception { runAll(); }
}
