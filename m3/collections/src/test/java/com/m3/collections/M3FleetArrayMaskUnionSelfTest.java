/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Bounded, deterministic original-owner differential checks:
 * primitive in-place map probes and exact detached Java/JNI mask windows.
 * No heap entry objects are stored in either M3 owner.
 */
public final class M3FleetArrayMaskUnionSelfTest {
    private static long checks;

    private M3FleetArrayMaskUnionSelfTest() { }

    private static void require(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what + " at check " + checks);
    }

    private static void maps() {
        Random random = new Random(0x20261019L);
        for (int round = 0; round < 16; round++) {
            M3IntIntHashMap actual = new M3IntIntHashMap(round % 4);
            Map<Integer, Integer> model = new HashMap<>();
            for (int step = 0; step < 4000; step++) {
                int key = switch (step % 37) {
                    case 0 -> 0;
                    case 1 -> Integer.MIN_VALUE;
                    case 2 -> Integer.MAX_VALUE;
                    case 3 -> -1;
                    default -> random.nextInt(801) - 400;
                };
                int value = step % 13 == 0 ? 0 : random.nextInt();
                switch (random.nextInt(8)) {
                    case 0, 1, 2 -> {
                        int expected = model.getOrDefault(key, 0) + value;
                        model.put(key, expected);
                        require(actual.addTo(key, value) == expected, "addTo");
                    }
                    case 3 -> {
                        boolean inserted = !model.containsKey(key);
                        model.put(key, value);
                        require(actual.put(key, value) == inserted, "put/insert");
                    }
                    case 4 -> {
                        boolean removed = model.remove(key) != null;
                        require(actual.remove(key) == removed, "remove/backshift");
                    }
                    case 5 -> {
                        int old = step % 2 == 0
                                ? model.getOrDefault(key, 0) : random.nextInt();
                        boolean present = model.containsKey(key) && model.get(key) == old;
                        if (present) model.put(key, value);
                        require(actual.replace(key, old, value) == present, "replace");
                    }
                    case 6 -> {
                        require(actual.containsKey(key) == model.containsKey(key), "contains");
                    }
                    default -> {
                        if (step % 113 == 0) {
                            model.clear();
                            actual.clear();
                        }
                    }
                }
                require(actual.size() == model.size(), "size");
                require(actual.getOrDefault(key, 73) == model.getOrDefault(key, 73),
                        "getOrDefault");
                if ((step & 127) == 0) {
                    Map<Integer, Integer> captured = new HashMap<>();
                    actual.forEach((k, v) -> captured.put(k, v));
                    require(captured.equals(model), "full primitive-lane projection");
                }
            }
        }
        for (int key : new int[]{0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            M3IntIntHashMap actual = new M3IntIntHashMap();
            require(actual.addTo(key, 0) == 0 && actual.containsKey(key),
                    "zero delta must insert");
            actual.put(key, Integer.MAX_VALUE);
            M3IntIterator stable = actual.keyIterator();
            require(actual.addTo(key, 1) == Integer.MIN_VALUE,
                    "Java int overflow semantics");
            require(stable.hasNext() && stable.nextInt() == key, "overwrite iterator");
            M3IntIterator invalidated = actual.keyIterator();
            actual.addTo(key ^ 0x4a4a4a4a, 0);
            try {
                invalidated.hasNext();
                throw new AssertionError("structural insertion retained stale iterator");
            } catch (ConcurrentModificationException expected) {
                checks++;
            }
        }
    }

    private static void masks(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x515afe20261019L);
        int[] masks = {0, 1, 2, 3, 7, 16, 128, 255};
        for (int length : new int[]{0, 1, 2, 3, 31, 63, 64, 65, 127, 128,
                                    129, 255, 256, 257, 511, 512, 513, 1025}) {
            for (int required : masks) {
                int rowOffset = 4, stateOffset = 3;
                int[] rows = new int[length + 12];
                long[] longs = new long[length + 12];
                byte[] states = new byte[length + 12];
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = i % 9 == 0 ? Integer.MIN_VALUE
                            : i % 11 == 0 ? Integer.MAX_VALUE : random.nextInt(13) - 6;
                    longs[i] = i % 9 == 0 ? Long.MIN_VALUE
                            : i % 11 == 0 ? Long.MAX_VALUE : random.nextLong();
                    states[i] = (byte) random.nextInt(256);
                }
                int[] savedRows = rows.clone();
                long[] savedLongs = longs.clone();
                byte[] savedStates = states.clone();
                int count = 0;
                for (int i = 0; i < length; i++) {
                    if (((states[stateOffset + i] & 255) & required) == required) count++;
                }
                int[] expectedInts = new int[count];
                long[] expectedLongs = new long[count];
                int out = 0;
                for (int i = 0; i < length; i++) {
                    if (((states[stateOffset + i] & 255) & required) == required) {
                        expectedInts[out] = rows[rowOffset + i];
                        expectedLongs[out++] = longs[rowOffset + i];
                    }
                }
                int[] ints = M3IndexMaskCompactor.selectWindow(rows, rowOffset,
                        states, stateOffset, length, required, backend);
                long[] actualLongs = M3IndexMaskCompactor.selectLongWindow(longs,
                        rowOffset, states, stateOffset, length, required, backend);
                require(Arrays.equals(ints, expectedInts), "native/Java int stable selection");
                require(Arrays.equals(actualLongs, expectedLongs),
                        "native/Java long stable selection");
                require(Arrays.equals(rows, savedRows), "borrowed int[] unchanged");
                require(Arrays.equals(longs, savedLongs), "borrowed long[] unchanged");
                require(Arrays.equals(states, savedStates), "borrowed mask unchanged");
                if (ints.length != 0) {
                    ints[0] ^= -1;
                    require(Arrays.equals(rows, savedRows), "int output detached");
                }
                if (actualLongs.length != 0) {
                    actualLongs[0] ^= -1L;
                    require(Arrays.equals(longs, savedLongs), "long output detached");
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--native"))) {
            throw new IllegalArgumentException("optional --native only");
        }
        boolean nativeMode = args.length == 1;
        if (nativeMode && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("native-required JNI library not loaded");
        }
        maps();
        masks(nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                : M3IndexMaskCompactor.Backend.JAVA);
        System.out.println("M3_FLEET_ARRAY_MASK_UNION_PASS backend="
                + (nativeMode ? "jni" : "java") + " checks=" + checks);
    }
}
