// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Differential Java21 oracle; contains no third-party algorithm source. */
public final class LongOrderStatisticArrayGrowthTest {
    private static long checks;
    private LongOrderStatisticArrayGrowthTest() { }
    private static void check(boolean good, String what) {
        if (!good) throw new AssertionError(what);
        checks++;
    }
    private static long rank(TreeMap<Long, Long> reference, long key) {
        long total = 0;
        for (long n : reference.headMap(key).values()) total += n;
        return total;
    }
    private static long select(TreeMap<Long, Long> reference, long ordinal) {
        for (Map.Entry<Long, Long> e : reference.entrySet()) {
            if (ordinal < e.getValue()) return e.getKey();
            ordinal -= e.getValue();
        }
        throw new AssertionError("bad reference ordinal");
    }
    private static void growthAndReuse() throws Exception {
        var actual = new LongOrderStatisticMultiset();
        for (int i = 0; i < 17; i++) actual.add(i);
        check(actual.size() == 17 && actual.distinctSize() == 17, "growth cardinality");
        check(actual.count(16) == 1 && actual.rank(16) == 16, "new child reachable");
        check(actual.select(16) == 16, "new child selectable");
        for (int i = 17; i < 4096; i++) actual.add(i);
        Field keys = actual.getClass().getDeclaredField("keys");
        keys.setAccessible(true);
        Object storage = keys.get(actual);
        long payload = actual.payloadBytes();
        actual.clear(); actual.clear();
        check(actual.isEmpty() && actual.distinctSize() == 0, "logical clear");
        check(keys.get(actual) == storage && actual.payloadBytes() == payload,
                "primitive storage retained for reuse");
        for (int i = 0; i < 4096; i++) actual.add(-i, 2);
        check(actual.size() == 8192 && actual.select(0) == -4095 &&
                actual.select(8191) == 0, "recycled capacity remains correct");
        actual.clear();
        actual.add(3, Long.MAX_VALUE);
        try { actual.add(4); throw new AssertionError("overflow accepted"); }
        catch (ArithmeticException expected) { checks++; }
        check(actual.size() == Long.MAX_VALUE && actual.count(3) == Long.MAX_VALUE,
                "overflow failure is atomic");
    }
    private static void randomized() {
        var actual = new LongOrderStatisticMultiset();
        var reference = new TreeMap<Long, Long>();
        Random rng = new Random(0x4d335f41564c5f31L);
        for (int step = 0; step < 12_000; step++) {
            long key = rng.nextInt(256) - 128;
            int action = rng.nextInt(100);
            if (action < 48) {
                int n = 1 + rng.nextInt(4);
                actual.add(key, n);
                reference.merge(key, (long) n, Long::sum);
            } else if (action < 82) {
                int n = 1 + rng.nextInt(4);
                long old = reference.getOrDefault(key, 0L);
                long removed = Math.min(n, old);
                check(actual.remove(key, n) == removed, "remove count");
                if (old == removed) reference.remove(key);
                else reference.put(key, old - removed);
            } else if (action < 90) {
                actual.clear(); reference.clear();
            } else {
                actual.add(Long.MIN_VALUE);
                reference.merge(Long.MIN_VALUE, 1L, Long::sum);
            }
            if (step % 11 == 0) {
                long cardinality = 0;
                for (long n : reference.values()) cardinality += n;
                check(actual.size() == cardinality && actual.distinctSize() == reference.size(),
                        "cardinality and distinct count");
                check(actual.rank(key) == rank(reference, key), "rank");
                if (!reference.isEmpty()) {
                    long pos = rng.nextLong(cardinality);
                    check(actual.select(pos) == select(reference, pos), "select");
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        growthAndReuse();
        randomized();
        System.out.println("PASS M3 LongOrderStatisticMultiset: " + checks + " checks");
    }
}
