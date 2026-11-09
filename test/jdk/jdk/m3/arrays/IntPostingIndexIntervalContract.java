// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/**
 * Executable Java 21/JNI contract for singleton and disjoint-range CSR intersections.
 * Accepts only an explicit source-built native library; all oracles are independent
 * of the native intersection algorithm and retain the original Java API.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingIntervalIntersectionContract.java blob 5b3473b6ab6a; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexIntervalContract {
    private static long assertions;
    private static long scenarios;
    private static Method nativeIntersection;
    private static Field rowOrdinalsField;
    private static Field offsetsField;

    private IntPostingIndexIntervalContract() { }

    private static void require(boolean condition, String description) {
        assertions++;
        if (!condition) throw new AssertionError(description);
    }

    private static void equal(int[] expected, int[] actual, String description) {
        require(Arrays.equals(expected, actual), description + " expected="
                + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }

    private static void initialize() throws ReflectiveOperationException {
        nativeIntersection = IntPostingIndex.class.getDeclaredMethod("intersectionNative",
                int[].class, int[].class, int[].class, int.class);
        nativeIntersection.setAccessible(true);
        rowOrdinalsField = IntPostingIndex.class.getDeclaredField("rowOrdinals");
        rowOrdinalsField.setAccessible(true);
        offsetsField = IntPostingIndex.class.getDeclaredField("offsets");
        offsetsField.setAccessible(true);
    }

    private static int[] nativeIntersection(int[] postings, int[] offsets,
                                            int[] positions, int universe) throws Exception {
        return (int[]) nativeIntersection.invoke(null, postings, offsets, positions, universe);
    }

    private static int[] uniquePositions(int[] directory, int[] query) {
        int[] found = new int[query.length];
        int count = 0;
        for (int key : query) {
            int slot = Arrays.binarySearch(directory, key);
            if (slot < 0) return null;
            found[count++] = slot;
        }
        Arrays.sort(found, 0, count);
        int unique = 0;
        for (int i = 0; i < count; i++) {
            if (unique == 0 || found[unique - 1] != found[i]) found[unique++] = found[i];
        }
        return Arrays.copyOf(found, unique);
    }

    private static int[] oracle(int universe, int[] pairKeys,
                                int[] pairRows, int[] query) {
        BitSet remaining = new BitSet(universe);
        remaining.set(0, universe);
        for (int key : query) {
            BitSet next = new BitSet(universe);
            for (int i = 0; i < pairKeys.length; i++) {
                if (pairKeys[i] == key) next.set(pairRows[i]);
            }
            remaining.and(next);
        }
        return remaining.stream().toArray();
    }

    private static void verifyApi(int[] pairKeys, int[] pairRows,
                                  int universe, int[] query) throws Exception {
        IntPostingIndex index = IntPostingIndex.ofPairs(pairKeys, pairRows, universe);
        int[] expected = oracle(universe, pairKeys, pairRows, query);
        int[] pairKeysBefore = pairKeys.clone();
        int[] pairRowsBefore = pairRows.clone();
        int[] queryBefore = query.clone();
        equal(expected, index.all(query).rowsSelected().toArray(), "Java AND API");
        equal(expected, index.all(IntPostingIndex.Backend.NATIVE_REQUIRED, query)
                .rowsSelected().toArray(), "JNI AND API");
        int[] directory = index.keys();
        int[] positions = uniquePositions(directory, query);
        if (positions != null) {
            int[] postings = (int[]) rowOrdinalsField.get(index);
            int[] offsets = (int[]) offsetsField.get(index);
            int[] originalPostings = postings.clone();
            int[] originalOffsets = offsets.clone();
            int[] originalPositions = positions.clone();
            int[] nativeRows = nativeIntersection(postings, offsets, positions, universe);
            equal(expected, nativeRows, "raw JNI AND");
            require(nativeRows != postings && nativeRows != positions,
                    "JNI output must be detached");
            equal(originalPostings, postings, "postings are immutable");
            equal(originalOffsets, offsets, "offsets are immutable");
            equal(originalPositions, positions, "query positions are immutable");
        }
        equal(pairKeysBefore, pairKeys, "pair keys not mutated");
        equal(pairRowsBefore, pairRows, "pair rows not mutated");
        equal(queryBefore, query, "requested keys not mutated");
        scenarios++;
    }

    private static void refuse(int[] postings, int[] offsets,
                               int[] positions, int universe) throws Exception {
        try {
            nativeIntersection(postings, offsets, positions, universe);
            throw new AssertionError("invalid CSR must refuse before interval fast path");
        } catch (InvocationTargetException error) {
            require(error.getCause() instanceof IllegalArgumentException,
                    "invalid CSR must raise IllegalArgumentException");
        }
    }

    private static void deterministic() throws Exception {
        verifyApi(new int[] {1, 1, 1, 1, 2, 2, 2, 2},
                new int[] {0, 1, 2, 3, 11, 12, 13, 14}, 100,
                new int[] {1, 2});
        verifyApi(new int[] {1, 1, 2, 2, 3, 3},
                new int[] {0, 2, 1, 3, 2, 3}, 100, new int[] {1, 2, 3});
        verifyApi(new int[] {1, 1, 2, 2, 3, 3},
                new int[] {0, 2, 1, 3, 2, 3}, 100, new int[] {1, 1, 1});
        verifyApi(new int[] {-1, -1, 3, 3, 3},
                new int[] {0, 5, 0, 6, 6}, 100, new int[] {-1, 3});
        verifyApi(new int[] {-1, -1, 3, 3, 3},
                new int[] {0, 5, 0, 6, 6}, 100, new int[] {-1});
        verifyApi(new int[] {-1, -1, 3, 3, 3},
                new int[] {0, 5, 0, 6, 6}, 100, new int[] {-1, 500});
        verifyApi(new int[0], new int[0], 0, new int[0]);
        verifyApi(new int[0], new int[0], 16, new int[0]);
        verifyApi(new int[0], new int[0], 16, new int[] {1});
        verifyApi(new int[] {0, 1, 2}, new int[] {0, 1, 2}, 3, new int[0]);
        verifyApi(new int[] {0, 1, 2}, new int[] {0, 1, 2}, 3, new int[] {0});
        verifyApi(new int[] {0, 1, 2}, new int[] {0, 1, 2}, 3, new int[] {0, 0});
        // Explicit empty selected lane is legal at raw-JNI boundary.
        equal(new int[0], nativeIntersection(new int[] {4, 5},
                new int[] {0, 0, 2}, new int[] {0, 1}, 20), "empty selected posting");
        // One billion rows must not trigger a billion-row Java selection allocation.
        equal(new int[] {Integer.MAX_VALUE - 2},
                nativeIntersection(new int[] {Integer.MAX_VALUE - 2},
                        new int[] {0, 1}, new int[] {0}, Integer.MAX_VALUE),
                "single-row high-universe JNI");
        equal(new int[0], nativeIntersection(new int[] {0, Integer.MAX_VALUE - 2},
                new int[] {0, 1, 2}, new int[] {0, 1}, Integer.MAX_VALUE),
                "disjoint high-universe JNI");
        equal(new int[] {0, 1, 2, 3},
                nativeIntersection(new int[0], new int[] {0},
                        new int[0], 4), "zero query is full universe");

        refuse(new int[] {2, 1, 100, 101}, new int[] {0, 2, 4},
                new int[] {0, 1}, 200);
        refuse(new int[] {0, 1, 101, 100}, new int[] {0, 2, 4},
                new int[] {0, 1}, 200);
        refuse(new int[] {0, 1, 100, 101}, new int[] {0, 2, 4},
                new int[] {0, 0}, 200);
        refuse(new int[] {0, 1, 100, 101}, new int[] {1, 2, 4},
                new int[] {0, 1}, 200);
        refuse(new int[] {0, 1, 100, 101}, new int[] {0, 2, 4},
                new int[] {0, 2}, 200);
        refuse(new int[] {0, 1, 100, 101}, new int[] {0, 2, 4},
                new int[] {0, 1}, -1);
        refuse(new int[] {-1, 1, 100, 101}, new int[] {0, 2, 4},
                new int[] {0, 1}, 200);
        refuse(new int[] {0, 1, 100, 201}, new int[] {0, 2, 4},
                new int[] {0, 1}, 200);
        refuse(null, new int[] {0}, new int[0], 20);
    }

    private static void skewed() throws Exception {
        final int universe = 65_536;
        int[] pairKeys = new int[32_768 + 128 + 64];
        int[] pairRows = new int[pairKeys.length];
        for (int i = 0; i < 32_768; i++) {
            pairKeys[i] = 1;
            pairRows[i] = i * 2;
        }
        for (int i = 0; i < 128; i++) {
            pairKeys[32_768 + i] = 2;
            pairRows[32_768 + i] = i * 4;
        }
        for (int i = 0; i < 64; i++) {
            pairKeys[32_896 + i] = 3;
            pairRows[32_896 + i] = 16_384 + i * 4;
        }
        verifyApi(pairKeys, pairRows, universe, new int[] {1, 2});
        verifyApi(pairKeys, pairRows, universe, new int[] {1, 3});
        verifyApi(pairKeys, pairRows, universe, new int[] {1, 2, 3});
        verifyApi(pairKeys, pairRows, universe, new int[] {1});
        verifyApi(pairKeys, pairRows, universe, new int[] {2});
    }

    private static void randomized() throws Exception {
        Random rng = new Random(0x4D33504F53544A21L);
        int[] keys = {-7, -2, -1, 0, 1, 2, 10, 100};
        for (int iteration = 0; iteration < 2_048; iteration++) {
            int universe = rng.nextInt(257);
            int count = universe == 0 ? 0 : rng.nextInt(513);
            int[] pairKeys = new int[count];
            int[] pairRows = new int[count];
            for (int i = 0; i < count; i++) {
                pairKeys[i] = keys[rng.nextInt(keys.length)];
                pairRows[i] = rng.nextInt(universe);
            }
            int[] query = new int[rng.nextInt(8)];
            for (int i = 0; i < query.length; i++) {
                query[i] = rng.nextInt(13) == 0 ? 5_000 : keys[rng.nextInt(keys.length)];
            }
            verifyApi(pairKeys, pairRows, universe, query);
        }
    }

    public static void runAll() throws Exception {
        String library = System.getProperty("matposting.native.path", "");
        if (library.isBlank()) {
            throw new IllegalStateException("M3_INTERVAL_JNI_LIBRARY_REQUIRED");
        }
        IntPostingIndex.loadNative(Path.of(library));
        require(IntPostingIndex.nativeAvailable(), "native backend required");
        initialize();
        deterministic();
        skewed();
        randomized();
        System.out.println("MAT_INTERVAL_SCENARIOS=" + scenarios);
        System.out.println("MAT_INTERVAL_ASSERTIONS=" + assertions);
        System.out.println("MAT_INTERVAL_RESULT=PASS");
    }

    public static void main(String[] args) throws Exception { runAll(); }
}
