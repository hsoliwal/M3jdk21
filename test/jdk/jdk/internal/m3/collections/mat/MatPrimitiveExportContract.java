/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve immutable sparse-vector and sorted primitive-map exports.
 * @modules jdk.m3.collections/jdk.internal.m3.collections.mat
 * @compile --patch-module jdk.m3.collections=${test.src} MatPrimitiveExportContract.java
 * @run main/othervm --patch-module jdk.m3.collections=${test.classes} jdk.internal.m3.collections.mat.MatPrimitiveExportContract
 */
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

/**
 * Target contract for immutable primitive outputs.
 *
 * <p>The first oracle retains the 211,200 checks from Synexia PR 17163's
 * MatPrimitiveExportRecipeContract (Git blob e8e229a768024aafafa2bea79e77131676356b7f).
 * Further cases cover public-constructor contracts, direct-map bitmap holes,
 * empty maps, signed extremes and detached snapshots.
 */
public final class MatPrimitiveExportContract {
    private static final Random RANDOM = new Random(0x20261112L);
    private static long checks;

    private MatPrimitiveExportContract() { }

    private static void yes(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("check " + checks);
    }

    public static void main(String[] args) {
        System.out.println("MAT_PRIMITIVE_EXPORT_CONTRACT_PASS checks=" + verify()
                + " retainedOracle=211200");
    }

    /** Runs in both a fresh jtreg VM and the existing Jupiter module suite. */
    public static synchronized long verify() {
        RANDOM.setSeed(0x20261112L);
        checks = 0;
        originalOracle();
        if (checks != 211_200L) {
            throw new AssertionError("retained oracle count changed: " + checks);
        }
        sparseConstructorAndBounds();
        emptyMaps();
        directMapHoles();
        sortedAndHashExtremes();
        return checks;
    }

    private static void originalOracle() {
        int[] defaults = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int size : new int[] {0, 1, 63, 64, 65, 128, 1024, 4096}) {
            for (int defaultValue : defaults) {
                for (int density : new int[] {0, 1, 20, 50, 100}) {
                    int[] dense = new int[size];
                    for (int i = 0; i < size; i++) {
                        dense[i] = RANDOM.nextInt(100) < density ? i ^ defaultValue ^ 63 : defaultValue;
                    }
                    int[] expected = dense.clone();
                    MatSparseIntVector vector = MatSparseIntVector.fromDense(dense, defaultValue);
                    yes(Arrays.equals(expected, vector.toArray()));
                    Arrays.fill(dense, 7);
                    yes(Arrays.equals(expected, vector.toArray()));
                    int[] output = vector.toArray();
                    Arrays.fill(output, 11);
                    yes(Arrays.equals(expected, vector.toArray()));
                    for (int i = 0; i < size; i++) yes(vector.get(i) == expected[i]);
                }
            }
        }
        for (int mode = 0; mode < 3; mode++) {
            for (int round = 0; round < 100; round++) {
                MatCollectionPolicy policy = mode == 2
                        ? MatCollectionPolicy.BALANCED : MatCollectionPolicy.MIN_MEMORY;
                MatAdaptiveIntIntMap.Builder builder = MatAdaptiveIntIntMap.builder(policy);
                TreeMap<Integer, Integer> truth = new TreeMap<>();
                int size = mode == 0 ? 80 : mode == 1 ? 24 : 150;
                for (int i = size - 1; i >= 0; i--) {
                    int key = mode == 0 ? Integer.MIN_VALUE + i
                            : mode == 1 ? -1_000_000_000 + i * 23_000_000
                            : -1_500_000_000 + i * 14_000_000;
                    int value = RANDOM.nextInt();
                    builder.put(key, value);
                    truth.put(key, value);
                    if ((i & 7) == 0) {
                        builder.put(key, value + 1);
                        truth.put(key, value + 1);
                    }
                }
                MatAdaptiveIntIntMap map = builder.build();
                yes(map.encoding().ordinal() == mode);
                int[] keys = map.keys(), values = map.valuesBySortedKey();
                int j = 0;
                for (var entry : truth.entrySet()) {
                    yes(keys[j] == entry.getKey());
                    yes(values[j] == entry.getValue());
                    j++;
                }
                Arrays.fill(keys, 0);
                Arrays.fill(values, 0);
                int[] newKeys = map.keys(), newValues = map.valuesBySortedKey();
                j = 0;
                for (var entry : truth.entrySet()) {
                    yes(newKeys[j] == entry.getKey());
                    yes(newValues[j] == entry.getValue());
                    j++;
                }
            }
        }
    }

    private static void sparseConstructorAndBounds() {
        int[] positions = {1, 64, 65, 129};
        int[] values = {0, Integer.MIN_VALUE, Integer.MAX_VALUE, 4};
        MatSparseIntVector vector = new MatSparseIntVector(130, -7, positions, values);
        int[] expected = new int[130];
        Arrays.fill(expected, -7);
        for (int i = 0; i < positions.length; i++) expected[positions[i]] = values[i];
        yes(vector.size() == 130);
        yes(vector.defaultValue() == -7);
        yes(vector.explicitCount() == 4);
        yes(vector.primitivePayloadBytes() == 4L + 8L * 4);
        Arrays.fill(positions, 0);
        Arrays.fill(values, -7);
        yes(Arrays.equals(expected, vector.toArray()));
        for (int i = 0; i < expected.length; i++) yes(vector.get(i) == expected[i]);
        int[] first = vector.toArray();
        int[] second = vector.toArray();
        yes(first != second);
        Arrays.fill(first, 99);
        yes(Arrays.equals(expected, second));
        yes(Arrays.equals(expected, vector.toArray()));
        expect(IndexOutOfBoundsException.class, null, () -> vector.get(-1));
        expect(IndexOutOfBoundsException.class, null, () -> vector.get(130));
        expect(IndexOutOfBoundsException.class, null, () -> vector.get(Integer.MAX_VALUE));

        MatSparseIntVector empty = new MatSparseIntVector(0, 5, new int[0], new int[0]);
        yes(empty.size() == 0);
        yes(empty.explicitCount() == 0);
        yes(empty.toArray().length == 0);
        yes(empty.toArray() != empty.toArray());
        expect(IndexOutOfBoundsException.class, null, () -> empty.get(0));

        MatSparseIntVector wide = new MatSparseIntVector(Integer.MAX_VALUE, -7,
                new int[] {0, Integer.MAX_VALUE - 1},
                new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
        yes(wide.get(0) == Integer.MIN_VALUE);
        yes(wide.get(Integer.MAX_VALUE - 1) == Integer.MAX_VALUE);
        yes(wide.get(1) == -7);
        yes(wide.get(Integer.MAX_VALUE - 2) == -7);
        yes(wide.size() == Integer.MAX_VALUE);
        expect(IndexOutOfBoundsException.class, null, () -> wide.get(Integer.MAX_VALUE));

        // The original order is size, array lengths, then per-position/value validation.
        expect(IllegalArgumentException.class, "size",
                () -> new MatSparseIntVector(-1, 0, null, null));
        expect(NullPointerException.class, null,
                () -> new MatSparseIntVector(1, 0, null, new int[0]));
        expect(NullPointerException.class, null,
                () -> new MatSparseIntVector(1, 0, new int[0], null));
        expect(IllegalArgumentException.class, "length mismatch",
                () -> new MatSparseIntVector(1, 0, new int[] {-1}, new int[0]));
        expect(IllegalArgumentException.class, "positions must be strict sorted",
                () -> new MatSparseIntVector(1, 0, new int[] {-1}, new int[] {0}));
        expect(IllegalArgumentException.class, "positions must be strict sorted",
                () -> new MatSparseIntVector(1, 0, new int[] {1}, new int[] {3}));
        expect(IllegalArgumentException.class, "positions must be strict sorted",
                () -> new MatSparseIntVector(2, 0, new int[] {1, 0}, new int[] {3, 4}));
        expect(IllegalArgumentException.class, "positions must be strict sorted",
                () -> new MatSparseIntVector(2, 0, new int[] {0, 0}, new int[] {3, 4}));
        expect(IllegalArgumentException.class, "explicit default wastes sparse slot",
                () -> new MatSparseIntVector(1, 7, new int[] {0}, new int[] {7}));
        expect(IllegalArgumentException.class, "explicit default wastes sparse slot",
                () -> new MatSparseIntVector(2, 7, new int[] {0, 1}, new int[] {7, 8}));
        expect(NullPointerException.class, "dense", () -> MatSparseIntVector.fromDense(null, 0));
    }

    private static void emptyMaps() {
        for (MatCollectionPolicy policy : MatCollectionPolicy.values()) {
            MatAdaptiveIntIntMap map = MatAdaptiveIntIntMap.builder(policy).build();
            yes(map.size() == 0);
            yes(map.encoding() == MatAdaptiveIntIntMap.Encoding.SORTED);
            yes(map.keys().length == 0);
            yes(map.valuesBySortedKey().length == 0);
            yes(map.keys() != map.keys());
            yes(map.valuesBySortedKey() != map.valuesBySortedKey());
            yes(!map.containsKey(0));
            yes(map.getOrDefault(Integer.MIN_VALUE, 99) == 99);
            expect(IllegalArgumentException.class, "missing key: 0", () -> map.get(0));
        }
    }

    private static void directMapHoles() {
        int[] offsets = {0, 1, 31, 32, 63, 64, 65, 126, 127, 128, 129};
        for (int base : new int[] {Integer.MIN_VALUE, -65, Integer.MAX_VALUE - 129}) {
            MatAdaptiveIntIntMap.Builder builder =
                    MatAdaptiveIntIntMap.builder(MatCollectionPolicy.FAST_LOOKUP).maxDirectSpan(130);
            TreeMap<Integer, Integer> truth = new TreeMap<>();
            for (int i = offsets.length - 1; i >= 0; i--) {
                int key = base + offsets[i];
                int value = switch (i % 4) {
                    case 0 -> 0;
                    case 1 -> Integer.MIN_VALUE;
                    case 2 -> Integer.MAX_VALUE;
                    default -> -i;
                };
                builder.put(key, value);
                truth.put(key, value);
            }
            builder.put(base + 64, 700);
            truth.put(base + 64, 700);
            MatAdaptiveIntIntMap map = builder.build();
            yes(map.encoding() == MatAdaptiveIntIntMap.Encoding.DIRECT);
            checkMap(map, truth);
            for (int offset = 0; offset < 130; offset++) {
                int key = base + offset;
                yes(map.containsKey(key) == truth.containsKey(key));
                yes(map.getOrDefault(key, 12345) == truth.getOrDefault(key, 12345));
            }
            // Reusing the builder must not mutate an already built immutable map.
            builder.put(base, 45);
            builder.put(base + 2, 67);
            checkMap(map, truth);
            TreeMap<Integer, Integer> changed = new TreeMap<>(truth);
            changed.put(base, 45);
            changed.put(base + 2, 67);
            checkMap(builder.build(), changed);
        }
    }

    private static void sortedAndHashExtremes() {
        int[] keys = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0,
                1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (MatCollectionPolicy policy : new MatCollectionPolicy[] {
                MatCollectionPolicy.MIN_MEMORY, MatCollectionPolicy.FAST_LOOKUP}) {
            MatAdaptiveIntIntMap.Builder builder =
                    MatAdaptiveIntIntMap.builder(policy).maxDirectSpan(0);
            TreeMap<Integer, Integer> truth = new TreeMap<>();
            for (int i = keys.length - 1; i >= 0; i--) {
                builder.put(keys[i], keys[i] ^ 0x5a5a5a5a);
                truth.put(keys[i], keys[i] ^ 0x5a5a5a5a);
            }
            builder.put(-1, 0);
            truth.put(-1, 0);
            MatAdaptiveIntIntMap map = builder.build();
            yes(map.encoding() == (policy == MatCollectionPolicy.MIN_MEMORY
                    ? MatAdaptiveIntIntMap.Encoding.SORTED : MatAdaptiveIntIntMap.Encoding.HASH));
            checkMap(map, truth);
            yes(!map.containsKey(2));
            yes(map.getOrDefault(2, -35) == -35);
            expect(IllegalArgumentException.class, "missing key: 2", () -> map.get(2));
        }
    }

    private static void checkMap(MatAdaptiveIntIntMap map, TreeMap<Integer, Integer> truth) {
        int[] keys = map.keys();
        int[] values = map.valuesBySortedKey();
        yes(map.size() == truth.size());
        yes(keys.length == truth.size());
        yes(values.length == truth.size());
        int index = 0;
        for (var entry : truth.entrySet()) {
            yes(keys[index] == entry.getKey());
            yes(values[index] == entry.getValue());
            yes(map.get(keys[index]) == entry.getValue());
            yes(map.containsKey(keys[index]));
            index++;
        }
        int[] secondKeys = map.keys();
        int[] secondValues = map.valuesBySortedKey();
        yes(keys != secondKeys);
        yes(values != secondValues);
        Arrays.fill(keys, 19);
        Arrays.fill(values, 23);
        yes(Arrays.equals(secondKeys, map.keys()));
        yes(Arrays.equals(secondValues, map.valuesBySortedKey()));
        index = 0;
        for (var entry : truth.entrySet()) {
            yes(secondKeys[index] == entry.getKey());
            yes(secondValues[index] == entry.getValue());
            index++;
        }
    }

    private static void expect(Class<? extends Throwable> type, String message, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            yes(failure.getClass() == type);
            if (message != null) yes(message.equals(failure.getMessage()));
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }
}
