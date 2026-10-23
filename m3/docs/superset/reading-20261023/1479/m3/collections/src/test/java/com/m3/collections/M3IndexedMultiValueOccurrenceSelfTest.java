// Modified 2026 by Hitesh Soliwal and contributors: keep the reserved-flag oracle type-correct.
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Independent ordered-multimap oracle for slot unlink, duplicate/null state,
 * recycled physical indexes and detached filtered/sorted snapshots.
 */
public final class M3IndexedMultiValueOccurrenceSelfTest {
    private static long checks;
    private M3IndexedMultiValueOccurrenceSelfTest() { }
    private static void check(boolean pass, String meaning) {
        checks++;
        if (!pass) throw new AssertionError(meaning + " at " + checks);
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong failure", failure);
        }
        throw new AssertionError("expected " + kind);
    }
    private static final class Item {
        Integer value;
        int bits;
        Item(Integer value, int bits) { this.value = value; this.bits = bits; }
    }
    private static void validate(
            M3IndexedMultiValueMap<Integer, Integer> actual,
            Map<Integer, ArrayList<Item>> oracle) {
        int total = 0;
        check(actual.keyCount() == oracle.size(), "distinct key count");
        check(actual.keysSnapshot().equals(new ArrayList<>(oracle.keySet())), "key order");
        for (var entry : oracle.entrySet()) {
            Integer key = entry.getKey();
            var items = entry.getValue();
            total += items.size();
            check(actual.containsKey(key), "present key");
            check(actual.valueCount(key) == items.size(), "group occurrence count");
            check(Objects.equals(actual.getFirst(key), items.get(0).value), "first value");
            var snap = actual.snapshot(key);
            check(snap.size() == items.size(), "snapshot count");
            for (int i = 0; i < items.size(); i++) {
                check(Objects.equals(snap.get(i), items.get(i).value), "occurrence order");
                check(actual.stateAt(key, i) == (items.get(i).bits | M3IndexedMultiValueMap.LIVE),
                        "unaltered bits");
            }
            var selected = actual.selected(key, M3IndexedMultiValueMap.VISIBLE,
                    M3IndexedMultiValueMap.DIRTY, M3IndexMaskCompactor.Backend.JAVA);
            int selectedCount = 0;
            for (var item : items) {
                if ((item.bits & M3IndexedMultiValueMap.VISIBLE) != 0
                        && (item.bits & M3IndexedMultiValueMap.DIRTY) == 0) {
                    check(Objects.equals(selected.get(selectedCount++), item.value),
                            "mask selection occurrence");
                }
            }
            check(selected.size() == selectedCount, "mask selection count");
            if (M3IndexMaskCompactor.nativeAvailable()) {
                var nativeSelected = actual.selected(key, M3IndexedMultiValueMap.VISIBLE,
                        M3IndexedMultiValueMap.DIRTY, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(selected.toArray(), nativeSelected.toArray()), "native parity");
            }
        }
        check(actual.size() == total, "total occurrences");
    }
    public static void main(String[] args) {
        Random random = new Random(0x4d336df019L);
        M3IndexedMultiValueMap<Integer, Integer> map = new M3IndexedMultiValueMap<>();
        LinkedHashMap<Integer, ArrayList<Item>> oracle = new LinkedHashMap<>();
        expect(IndexOutOfBoundsException.class, () -> map.removeAt("missing", 0));
        check(!map.removeFirstValue("missing", null), "absent removal");
        for (int step = 0; step < 3000; step++) {
            Integer key = random.nextInt(13) == 0 ? null : random.nextInt(13);
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(11);
            int action = random.nextInt(8);
            var expected = oracle.get(key);
            long before = map.revision();
            switch (action) {
                case 0, 1, 2 -> {
                    int bits = (random.nextBoolean() ? M3IndexedMultiValueMap.VISIBLE : 0)
                            | (random.nextInt(4) == 0 ? M3IndexedMultiValueMap.DIRTY : 0);
                    map.add(key, value, bits);
                    oracle.computeIfAbsent(key, unused -> new ArrayList<>()).add(new Item(value, bits));
                    check(map.revision() > before, "add revision");
                }
                case 3 -> {
                    if (expected == null) {
                        check(!map.removeFirstValue(key, value), "absent key removal");
                    } else {
                        boolean had = false;
                        for (int i = 0; i < expected.size(); i++) {
                            if (Objects.equals(expected.get(i).value, value)) {
                                expected.remove(i);
                                had = true;
                                break;
                            }
                        }
                        check(map.removeFirstValue(key, value) == had, "first-value removal");
                        if (expected.isEmpty()) oracle.remove(key);
                        if (had) check(map.revision() > before, "remove revision");
                    }
                }
                case 4 -> {
                    if (expected == null) {
                        expect(IndexOutOfBoundsException.class, () -> map.removeAt(key, 0));
                    } else {
                        int ordinal = random.nextInt(expected.size());
                        Integer removed = expected.remove(ordinal).value;
                        check(Objects.equals(map.removeAt(key, ordinal), removed), "removeAt value");
                        if (expected.isEmpty()) oracle.remove(key);
                        check(map.revision() > before, "removeAt revision");
                    }
                }
                case 5 -> {
                    if (expected != null) {
                        int ordinal = random.nextInt(expected.size());
                        Integer old = expected.get(ordinal).value;
                        expected.get(ordinal).value = value;
                        check(Objects.equals(map.replaceAt(key, ordinal, value), old),
                                "replace old value");
                    } else expect(IndexOutOfBoundsException.class, () -> map.replaceAt(key, 0, value));
                }
                case 6 -> {
                    int removed = expected == null ? 0 : expected.size();
                    check(map.removeKey(key) == removed, "removeKey count");
                    oracle.remove(key);
                }
                default -> {
                    if (expected != null) {
                        int ordinal = random.nextInt(expected.size());
                        expected.get(ordinal).bits |= M3IndexedMultiValueMap.VISIBLE;
                        map.updateState(key, ordinal, M3IndexedMultiValueMap.VISIBLE, 0);
                    } else check(map.valueCount(key) == 0, "absent key count");
                }
            }
            validate(map, oracle);
            if ((step % 421) == 0) {
                var detached = map.snapshot(key);
                List<Integer> previous = new ArrayList<>(detached);
                map.add(key, -999);
                check(new ArrayList<>(detached).equals(previous), "snapshot detached on insert");
                map.removeAt(key, map.valueCount(key) - 1);
                validate(map, oracle);
            }
        }
        map.clear();
        oracle.clear();
        validate(map, oracle);
        check(map.capacity() >= 0, "array capacity");
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        // An admitted key type reaches the reserved LIVE-bit validation.
        expect(IllegalArgumentException.class, () -> map.add(0, 1,
=======
        expect(IllegalArgumentException.class, () -> map.add(-1, 1,
>>>>>>> 2e36d73217cc08e4f5f9bdbe37fb5fea3472889f
                M3IndexedMultiValueMap.LIVE));
        System.out.println("PASS M3IndexedMultiValueOccurrenceSelfTest checks=" + checks
                + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
