// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Actual JNI regression projection; arbitrary client callbacks remain in Java. */
public final class M3DeepFilterNativeProof {
    private static int checks;
    private M3DeepFilterNativeProof() { }
    private static native long fold(int[] values);

    private static long reference(int[] values) {
        long state = 0xcbf29ce484222325L;
        for (int value : values) {
            state ^= Integer.toUnsignedLong(value);
            state *= 0x100000001b3L;
        }
        return state;
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void parity(int[] values) {
        int[] before = values.clone();
        check(fold(values) == reference(values), "native/reference mismatch");
        check(Arrays.equals(before, values), "input array changed");
    }
    public static void main(String[] args) {
        String library = System.getProperty("m3.deep.native.library");
        if (library == null || !Path.of(library).isAbsolute()) {
            throw new IllegalArgumentException("absolute m3.deep.native.library required");
        }
        System.load(library);
        for (int size : new int[] {0, 1, 2, 3, 255, 256, 257, 1023, 1024}) {
            ArrayList<Integer> owner = new ArrayList<>();
            for (int i = 0; i < size; i++) owner.add(i == 0 ? Integer.MIN_VALUE
                    : i == 1 ? Integer.MAX_VALUE : i - 500);
            Collection<Integer> filtered = M3CollectionViews.filtered(owner, x -> (x & 1) == 0);
            Map<Integer, Integer> mapOwner = new LinkedHashMap<>();
            for (Integer x : owner) mapOwner.put(x, x);
            Map<Integer, Integer> map = M3FilteredMaps.filtered(mapOwner, (k, v) -> (k & 1) == 0);
            for (int depth = 0; depth < 1024; depth++) {
                filtered = M3CollectionViews.filtered(filtered, x -> true);
                map = M3FilteredMaps.filtered(map, (k, v) -> true);
            }
            int[] expected = owner.stream().filter(x -> (x & 1) == 0).mapToInt(Integer::intValue).toArray();
            int[] projected = filtered.stream().mapToInt(Integer::intValue).toArray();
            check(Arrays.equals(expected, projected), "filter projection order");
            parity(projected);
            parity(map.keySet().stream().mapToInt(Integer::intValue).toArray());
            List<Integer> sorted = M3CollectionViews.sortedSnapshot(filtered, Comparator.reverseOrder());
            parity(sorted.stream().mapToInt(Integer::intValue).toArray());
            List<Integer> fused = M3CollectionViews.filteredSortedSnapshot(owner, x -> (x & 1) == 0,
                    Comparator.naturalOrder());
            parity(fused.stream().mapToInt(Integer::intValue).toArray());
            owner.add(1_000_000);
            mapOwner.put(1_000_000, 1_000_000);
            parity(filtered.parallelStream().mapToInt(Integer::intValue).toArray());
            parity(map.keySet().stream().mapToInt(Integer::intValue).toArray());
            check(!sorted.contains(1_000_000), "snapshot changed with owner");
            parity(owner.stream().mapToInt(Integer::intValue).toArray());
        }
        NavigableSet<Integer> setOwner = new TreeSet<>();
        NavigableMap<Integer, Integer> mapOwner = new TreeMap<>();
        for (int i = -32; i <= 32; i++) { setOwner.add(i); mapOwner.put(i, i); }
        NavigableSet<Integer> set = setOwner;
        NavigableMap<Integer, Integer> map = mapOwner;
        for (int i = 0; i < 65_536; i++) {
            set = M3CollectionViews.filtered(set, x -> (x & 1) == 0);
            map = M3FilteredMaps.filtered(map, (k, v) -> (k & 1) == 0);
        }
        int[] descending = set.subSet(-16, true, 16, false).descendingSet()
                .stream().mapToInt(Integer::intValue).toArray();
        int[] keys = map.subMap(-16, true, 16, false).descendingMap().keySet()
                .stream().mapToInt(Integer::intValue).toArray();
        check(Arrays.equals(descending, keys), "range/set/map equality");
        parity(descending); parity(keys);
        set.clear(); map.clear();
        check(setOwner.stream().allMatch(x -> (x & 1) != 0), "set hidden values destroyed");
        check(mapOwner.keySet().stream().allMatch(x -> (x & 1) != 0), "map hidden values destroyed");
        parity(setOwner.stream().mapToInt(Integer::intValue).toArray());
        parity(mapOwner.keySet().stream().mapToInt(Integer::intValue).toArray());
        try { fold(null); throw new AssertionError("null accepted"); }
        catch (NullPointerException expected) { checks++; }
        System.out.println("M3_DEEP_FILTER_JNI_PASS checks=" + checks + " depth=65536");
    }
}
