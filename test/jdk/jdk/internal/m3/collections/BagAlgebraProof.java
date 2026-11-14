// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.m3.collections.packed.PackedLongBag;
import jdk.internal.m3.collections.packed.PackedLongLongHashMap;

/** Finite differential proof of the actual primitive owners; boxed maps are test-only. */
//FLLDCIM: RELOCATE - donor blob 99fddd7065bc at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public final class BagAlgebraProof {
    private static long checks;
    private static long operations;
    private BagAlgebraProof() { }

    /** @param args optional positive seeds and steps */
    public static void main(String[] args) {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 32;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 8000;
        if (seeds < 1 || steps < 1) throw new IllegalArgumentException("positive budgets");
        for (int seed = 0; seed < seeds; seed++) {
            bag(seed, steps);
            map(seed, steps);
        }
        limits();
        System.out.println("PASS bags/maps operations=" + operations + " checks=" + checks
                + " seeds=" + seeds + " steps=" + steps);
    }

    private static long key(Random random) {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0;
            default -> random.nextInt(65) - 32;
        };
    }

    private static void bag(int seed, int steps) {
        Random random = new Random(20261005L + seed);
        PackedLongBag actual = new PackedLongBag(seed % 17);
        Map<Long, Long> expected = new HashMap<>();
        for (int i = 0; i < steps; i++) {
            operations++;
            long value = key(random), amount = random.nextInt(21) + 1;
            long previous = expected.getOrDefault(value, 0L);
            switch (random.nextInt(8)) {
                case 0 -> {
                    eq(previous + amount, actual.add(value, amount));
                    assign(expected, value, previous + amount);
                }
                case 1 -> {
                    long next = Math.max(0, previous - amount);
                    eq(next, actual.remove(value, amount)); assign(expected, value, next);
                }
                case 2 -> {
                    eq(previous, actual.setCount(value, amount)); assign(expected, value, amount);
                }
                case 3 -> {
                    long wanted = random.nextBoolean() ? previous : previous + 1;
                    eq(wanted == previous, actual.setCount(value, wanted, amount));
                    if (wanted == previous) assign(expected, value, amount);
                }
                case 4 -> { eq(previous, actual.removeAll(value)); expected.remove(value); }
                case 5 -> { eq(previous, actual.setCount(value, 0)); expected.remove(value); }
                case 6 -> actual.trimToSize();
                default -> {
                    if (random.nextInt(31) == 0) { actual.clear(); expected.clear(); }
                    else eq(previous, actual.count(value));
                }
            }
            eq(total(expected), actual.size());
            eq(expected.size(), actual.distinctSize());
            if ((i & 63) == 0) {
                verify(expected, actual);
                PackedLongBag other = new PackedLongBag(); Map<Long, Long> right = new HashMap<>();
                for (int j = 0; j < 12; j++) {
                    long k = key(random), n = random.nextInt(10);
                    other.setCount(k, n); assign(right, k, n);
                }
                algebra(expected, actual, right, other);
            }
        }
        verify(expected, actual);
    }

    private static void algebra(Map<Long, Long> left, PackedLongBag a, Map<Long, Long> right, PackedLongBag b) {
        verify(combine(left, right, 0), a.sum(b));
        verify(combine(left, right, 1), a.union(b));
        verify(combine(left, right, 2), a.intersection(b));
        verify(combine(left, right, 3), a.difference(b));
        boolean contained = right.entrySet().stream()
                .allMatch(e -> left.getOrDefault(e.getKey(), 0L) >= e.getValue());
        eq(contained, a.containsOccurrences(b));
        verify(left, a); verify(right, b);
        PackedLongBag copy = a.copy(); copy.clear(); verify(left, a);
        verify(left, a.union(a)); verify(left, a.intersection(a));
        verify(Map.of(), a.difference(a));
        PackedLongBag detached = a.union(b);
        Map<Long, Long> snapshot = combine(left, right, 1);
        b.clear(); verify(snapshot, detached);
        right.forEach(b::setCount);
    }

    private static Map<Long, Long> combine(Map<Long, Long> a, Map<Long, Long> b, int op) {
        Map<Long, Long> result = new HashMap<>(); TreeSet<Long> keys = new TreeSet<>(a.keySet()); keys.addAll(b.keySet());
        for (long key : keys) {
            long x = a.getOrDefault(key, 0L), y = b.getOrDefault(key, 0L);
            long n = switch (op) {
                case 0 -> Math.addExact(x, y);
                case 1 -> Math.max(x, y);
                case 2 -> Math.min(x, y);
                default -> Math.max(0, x - y);
            };
            assign(result, key, n);
        }
        return result;
    }

    private static void map(int seed, int steps) {
        Random random = new Random(78451L + seed);
        PackedLongLongHashMap actual = new PackedLongLongHashMap(seed % 19);
        Map<Long, Long> expected = new HashMap<>();
        for (int i = 0; i < steps; i++) {
            operations++;
            long k = key(random), v = random.nextLong();
            switch (random.nextInt(7)) {
                case 0 -> { boolean absent = !expected.containsKey(k); expected.put(k, v); eq(absent, actual.put(k, v)); }
                case 1 -> { eq(expected.remove(k) != null, actual.remove(k)); }
                case 2 -> {
                    long old = expected.getOrDefault(k, v);
                    boolean match = random.nextBoolean(); long wanted = match ? old : ~old;
                    boolean present = expected.containsKey(k) && expected.get(k) == wanted;
                    eq(present, actual.replace(k, wanted, v)); if (present) expected.put(k, v);
                }
                case 3 -> {
                    long old = expected.getOrDefault(k, 7L);
                    expected.put(k, old + v); eq(old + v, actual.addTo(k, v, 7L));
                }
                case 4 -> actual.trimToSize();
                case 5 -> {
                    int before = expected.size(); expected.entrySet().removeIf(e -> (e.getKey() & 7) == 0);
                    eq(before - expected.size(), actual.filterInPlace((x, y) -> (x & 7) != 0));
                }
                default -> { eq(expected.getOrDefault(k, 97L).longValue(), actual.getOrDefault(k, 97L)); }
            }
            eq(expected.size(), actual.size());
            if ((i & 127) == 0) {
                Map<Long, Long> seen = new HashMap<>(); actual.forEach(seen::put);
                eq(expected, seen);
                long[] keys = actual.keysToArray(), values = actual.valuesToArray(); eq(keys.length, values.length);
                for (int j = 0; j < keys.length; j++) eq(expected.get(keys[j]).longValue(), values[j]);
            }
        }
    }

    private static void limits() {
        PackedLongBag bag = new PackedLongBag();
        eq(0L, bag.setCount(0, 0)); eq(true, bag.setCount(0, 0, 0));
        eq(0L, bag.setCount(Long.MIN_VALUE, Long.MAX_VALUE));
        expect(ArithmeticException.class, () -> bag.add(Long.MIN_VALUE));
        expect(ArithmeticException.class, () -> bag.add(Long.MAX_VALUE));
        expect(ArithmeticException.class, () -> bag.setCount(1, 1));
        eq(false, bag.setCount(1, 2, Long.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> bag.setCount(1, -1, 0));
        expect(IllegalArgumentException.class, () -> bag.setCount(1, 2, -1));
        expect(IllegalArgumentException.class, () -> bag.add(0, 0));
        expect(IllegalArgumentException.class, () -> bag.remove(0, 0));
        verify(Map.of(Long.MIN_VALUE, Long.MAX_VALUE), bag);
        PackedLongBag other = new PackedLongBag(); other.add(Long.MAX_VALUE);
        expect(ArithmeticException.class, () -> bag.union(other));
        expect(ArithmeticException.class, () -> bag.sum(other));
        expect(ArithmeticException.class, () -> bag.sum(bag));
        verify(Map.of(Long.MIN_VALUE, Long.MAX_VALUE), bag);
        verify(Map.of(Long.MAX_VALUE, 1L), other);
        verify(Map.of(), bag.intersection(other));
        verify(Map.of(Long.MIN_VALUE, Long.MAX_VALUE), bag.difference(other));
        expect(NullPointerException.class, () -> bag.union(null));
        expect(NullPointerException.class, () -> bag.sum(null));
        expect(NullPointerException.class, () -> bag.intersection(null));
        expect(NullPointerException.class, () -> bag.difference(null));
        expect(NullPointerException.class, () -> bag.containsOccurrences(null));
        eq(Long.MAX_VALUE, bag.setCount(Long.MIN_VALUE, 0));
        bag.trimToSize(); verify(Map.of(), bag);
        bag.setCount(9, Long.MAX_VALUE); eq(Long.MAX_VALUE, bag.removeAll(9));
    }

    private static void verify(Map<Long, Long> expected, PackedLongBag actual) {
        Map<Long, Long> seen = new HashMap<>();
        actual.forEachCount((k, n) -> { if (n <= 0 || seen.put(k, n) != null) throw new AssertionError("invalid count lane"); });
        eq(expected, seen); eq(total(expected), actual.size());
        eq(expected.isEmpty(), actual.isEmpty()); eq(expected.size(), actual.distinctSize());
    }
    private static long total(Map<Long, Long> counts) { long result = 0; for (long n : counts.values()) result = Math.addExact(result, n); return result; }
    private static void assign(Map<Long, Long> map, long key, long count) { if (count == 0) map.remove(key); else map.put(key, count); }
    private static void eq(long expected, long actual) { checks++; if (expected != actual) throw new AssertionError(expected + " != " + actual); }
    private static void eq(Object expected, Object actual) { checks++; if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual); }
    private static void expect(Class<? extends RuntimeException> type, Runnable run) {
        checks++;
        try { run.run(); } catch (RuntimeException e) { if (e.getClass() == type) return; throw e; }
        throw new AssertionError("missing " + type.getName());
    }
}
