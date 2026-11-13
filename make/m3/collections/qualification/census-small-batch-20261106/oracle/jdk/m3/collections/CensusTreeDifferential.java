// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Census leaves L076/L077 oracle, public API part (census-small-batch-20261106): seeded ops over M3TreeMap and
 * M3TreeSet and their sub/head/tail/descending views, one line per op, checked against java.util.TreeMap.
 * Compiled once and run against the master and the edited classes; the lines must be byte-identical (the leaves
 * add package-private hooks only). usage: java -cp CLASSES:ORACLE jdk.m3.collections.CensusTreeDifferential OUT OPS
 */
public final class CensusTreeDifferential {
    static final long SEED = 0x4C0764C0774C076L; // named seed "census-small-L076-L077"
    static final int KEYS = 256;

    public static void main(String[] args) throws IOException {
        long ops = Long.parseLong(args[1]);
        SplittableRandom rnd = new SplittableRandom(SEED);
        M3TreeMap<Integer, Integer> map = new M3TreeMap<>();
        M3TreeSet<Integer> set = new M3TreeSet<>();
        TreeMap<Integer, Integer> oracle = new TreeMap<>();
        long mismatches = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (long i = 0; i < ops; i++) {
                if (i % 50_000 == 0) { map.clear(); set.clear(); oracle.clear(); }
                int k = rnd.nextInt(KEYS);
                int lo = rnd.nextInt(KEYS);
                int hi = lo + rnd.nextInt(KEYS - lo + 1);
                int op = rnd.nextInt(12);
                String got;
                String want;
                final int fk = k, flo = lo, fhi = hi;
                final boolean fin = rnd.nextBoolean();
                final long fi = i;
                switch (op) {
                    case 0, 1, 2 -> { boolean absent = !oracle.containsKey(k); got = safe(() -> "put " + map.put(fk, (int) fi) + set.add(fk)); want = "put " + oracle.put(k, (int) i) + absent; }
                    case 3 -> { boolean present = oracle.containsKey(k); got = safe(() -> "rm " + map.remove(fk) + set.remove(fk)); want = "rm " + oracle.remove(k) + present; }
                    case 4 -> { got = safe(() -> nav(map, fk)); want = safe(() -> nav(oracle, fk)); }
                    case 5 -> { got = safe(() -> nav(map.descendingMap(), fk)); want = safe(() -> nav(oracle.descendingMap(), fk)); }
                    case 6 -> { got = safe(() -> nav(map.subMap(flo, true, fhi, fin), fk)); want = safe(() -> nav(oracle.subMap(flo, true, fhi, fin), fk)); }
                    case 7 -> { got = safe(() -> keys(map.subMap(flo, true, fhi, false))); want = safe(() -> keys(oracle.subMap(flo, true, fhi, false))); }
                    case 8 -> { got = safe(() -> keys(map.headMap(fhi, true).descendingMap())); want = safe(() -> keys(oracle.headMap(fhi, true).descendingMap())); }
                    case 9 -> { got = safe(() -> keys(set.tailSet(flo, false))); want = safe(() -> keys(oracle.navigableKeySet().tailSet(flo, false))); }
                    case 10 -> { got = safe(() -> keys(set.descendingSet().subSet(fhi, true, flo, true))); want = safe(() -> keys(oracle.navigableKeySet().descendingSet().subSet(fhi, true, flo, true))); }
                    default -> { got = "sz " + map.size() + "/" + set.size() + " " + map.firstEntry() + " " + map.lastEntry(); want = "sz " + oracle.size() + "/" + oracle.size() + " " + oracle.firstEntry() + " " + oracle.lastEntry(); }
                }
                boolean ok = got.equals(want);
                if (!ok) mismatches++;
                out.write(i + " " + op + " " + got + (ok ? "" : " ORACLE_MISMATCH " + want) + "\n");
            }
            out.write("TOTAL ops=" + ops + " oracleMismatches=" + mismatches + "\n");
        }
    }

    static String safe(java.util.function.Supplier<String> body) {
        try { return body.get(); } catch (RuntimeException failure) { return "threw " + failure.getClass().getSimpleName(); }
    }

    static String nav(NavigableMap<Integer, Integer> m, int k) {
        return "nav " + m.lowerKey(k) + "," + m.floorKey(k) + "," + m.ceilingKey(k) + "," + m.higherKey(k) + "," + m.get(k);
    }

    static String keys(Map<Integer, ?> m) {
        return keys(m.keySet());
    }

    static String keys(Iterable<Integer> keys) {
        StringBuilder s = new StringBuilder("keys");
        int n = 0;
        long h = 1;
        for (Integer key : keys) { n++; h = h * 31 + key; }
        return s.append(' ').append(n).append(' ').append(Long.toHexString(h)).toString();
    }

    static String keys(NavigableSet<Integer> keys) {
        return keys((Iterable<Integer>) keys);
    }
}
