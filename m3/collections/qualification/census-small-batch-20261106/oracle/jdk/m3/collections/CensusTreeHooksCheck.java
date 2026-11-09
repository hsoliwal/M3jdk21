// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Census leaves L076/L077 oracle, hook part (edited classes only; the hooks do not exist on master): over seeded
 * maps and their full/sub/head/tail/descending views, previousIndexedSlot walked from lastSlot must give the reverse
 * of the nextSlot walk, nearIndexedSlot(key, -2/-1/1/2) must give lowerKey/floorKey/ceilingKey/higherKey of the view
 * (view order), and M3TreeSet.indexedOwner() must be the backing view map (same keys, same order).
 * usage: java -cp EDITED_CLASSES:ORACLE jdk.m3.collections.CensusTreeHooksCheck OUT ROUNDS
 */
public final class CensusTreeHooksCheck {
    static final long SEED = 0x4C0764C0774C076L; // named seed "census-small-L076-L077"

    public static void main(String[] args) throws IOException {
        int rounds = Integer.parseInt(args[1]);
        SplittableRandom rnd = new SplittableRandom(SEED);
        long checks = 0, failures = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (int r = 0; r < rounds; r++) {
                M3TreeMap<Integer, Integer> map = new M3TreeMap<>();
                M3TreeSet<Integer> set = new M3TreeSet<>();
                TreeMap<Integer, Integer> oracle = new TreeMap<>();
                int n = rnd.nextInt(64);
                for (int i = 0; i < n; i++) { int k = rnd.nextInt(128); map.put(k, i); set.add(k); oracle.put(k, i); }
                int lo = rnd.nextInt(128), hi = lo + rnd.nextInt(129 - lo);
                boolean li = rnd.nextBoolean(), hiInc = rnd.nextBoolean();
                List<M3TreeMap<Integer, Integer>> views = List.of(map, map.descendingMap(), map.subMap(lo, li, hi, hiInc),
                        map.headMap(hi, hiInc), map.tailMap(lo, li), map.subMap(lo, li, hi, hiInc).descendingMap());
                List<NavigableMap<Integer, Integer>> want = List.of(oracle, oracle.descendingMap(),
                        oracle.subMap(lo, li, hi, hiInc), oracle.headMap(hi, hiInc), oracle.tailMap(lo, li),
                        oracle.subMap(lo, li, hi, hiInc).descendingMap());
                for (int v = 0; v < views.size(); v++) {
                    M3TreeMap<Integer, Integer> view = views.get(v);
                    NavigableMap<Integer, Integer> o = want.get(v);
                    List<Integer> forward = new ArrayList<>(), backward = new ArrayList<>();
                    for (int s = view.firstSlot(); s >= 0; s = view.nextSlot(s)) forward.add(view.keyAt(s));
                    for (int s = view.lastSlot(); s >= 0; s = view.previousIndexedSlot(s)) backward.add(0, view.keyAt(s));
                    checks++;
                    if (!forward.equals(backward) || !forward.equals(new ArrayList<>(o.keySet()))) {
                        failures++;
                        out.write("FAIL walk r=" + r + " v=" + v + " fwd=" + forward + " back=" + backward + "\n");
                    }
                    for (int q = 0; q < 8; q++) {
                        int k = rnd.nextInt(128);
                        if (!o.isEmpty() && !inRange(view, k)) continue;
                        Integer[] expect = {o.lowerKey(k), o.floorKey(k), o.ceilingKey(k), o.higherKey(k)};
                        int[] rel = {-2, -1, 1, 2};
                        for (int j = 0; j < 4; j++) {
                            int s = view.nearIndexedSlot(k, rel[j]);
                            Integer got = s < 0 ? null : view.keyAt(s);
                            checks++;
                            if (!java.util.Objects.equals(got, expect[j])) {
                                failures++;
                                out.write("FAIL near r=" + r + " v=" + v + " k=" + k + " rel=" + rel[j] + " got=" + got
                                        + " want=" + expect[j] + "\n");
                            }
                        }
                    }
                }
                List<M3TreeSet<Integer>> sets = List.of(set, set.descendingSet(), set.subSet(lo, li, hi, hiInc),
                        set.headSet(hi, hiInc), set.tailSet(lo, li), set.subSet(lo, li, hi, hiInc).descendingSet(),
                        (M3TreeSet<Integer>) map.navigableKeySet());
                for (M3TreeSet<Integer> s : sets) {
                    checks++;
                    if (!new ArrayList<>(s.indexedOwner().keySet()).equals(new ArrayList<>((NavigableSet<Integer>) s))) {
                        failures++;
                        out.write("FAIL owner r=" + r + " set=" + s + " owner=" + s.indexedOwner().keySet() + "\n");
                    }
                }
                checks++;
                if (((M3TreeSet<Integer>) map.navigableKeySet()).indexedOwner() != map) {
                    failures++;
                    out.write("FAIL owner identity r=" + r + "\n");
                }
            }
            out.write("TOTAL rounds=" + rounds + " checks=" + checks + " failures=" + failures + "\n");
        }
    }

    /** Keys outside a bounded view throw from navigation; probe only admitted keys. */
    private static boolean inRange(M3TreeMap<Integer, Integer> view, int k) {
        try { view.floorKey(k); return true; } catch (IllegalArgumentException outside) { return false; }
    }
}
