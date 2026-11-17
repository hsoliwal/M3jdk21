/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary StrategyHashMap: equality by a strategy, against HashMap and Eclipse's strategy map
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main StrategyHashMapTest
 */

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.StrategyHashMap;
import jdk.m3.collections.apache.Equator;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;
import jdk.m3.collections.eclipse.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;

public class StrategyHashMapTest {

    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    /** Strings equal ignoring case, as a Commons equator. */
    static final class CaseInsensitive implements Equator<String> {
        @Override
        public boolean equate(String a, String b) {
            return a.equalsIgnoreCase(b);
        }

        @Override
        public int hash(String s) {
            return s.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    /** Integers equal by value, with only eight hashes: long probe runs and many collisions. */
    static final class FewHashes implements HashingStrategy<Integer> {
        private static final long serialVersionUID = 1L;

        @Override
        public int computeHashCode(Integer i) {
            return i & 7;
        }

        @Override
        public boolean equals(Integer a, Integer b) {
            return a.equals(b);
        }
    }

    public static void main(String[] args) {
        againstHashMap(20261117L, 300_000);
        againstHashMap(7L, 50_000);
        againstEclipse(new CaseInsensitive().asHashingStrategy(), 11L, 200_000);
        collisions(13L, 100_000);
        entryView();
        failFast();
        System.out.println("StrategyHashMapTest: " + checks + " checks passed");
    }

    /** The default strategy (equals, hashCode, null-safe) behaves as HashMap, nulls included. */
    static void againstHashMap(long seed, int steps) {
        Random random = new Random(seed);
        StrategyHashMap<Integer, Integer> map = new StrategyHashMap<>();
        HashMap<Integer, Integer> oracle = new HashMap<>();
        for (int i = 0; i < steps; i++) {
            Integer key = random.nextInt(20) == 0 ? null : random.nextInt(5_000);
            Integer value = random.nextInt(10) == 0 ? null : random.nextInt();
            switch (random.nextInt(10)) {
                case 0, 1, 2, 3 ->
                    check(Objects.equals(map.put(key, value), oracle.put(key, value)), "put");
                case 4, 5 -> check(Objects.equals(map.remove(key), oracle.remove(key)), "remove");
                case 6, 7 -> check(Objects.equals(map.get(key), oracle.get(key)), "get");
                case 8 -> check(map.containsKey(key) == oracle.containsKey(key), "containsKey");
                default -> {
                    if (random.nextInt(2_000) == 0) {
                        map.clear();
                        oracle.clear();
                    }
                }
            }
            check(map.size() == oracle.size(), "size at step " + i);
        }
        check(entries(map.entrySet()).equals(entries(oracle.entrySet())),
                "entries after " + steps + " steps");
    }

    /** A custom strategy behaves as Eclipse's strategy map with the same strategy. */
    static void againstEclipse(HashingStrategy<String> strategy, long seed, int steps) {
        Random random = new Random(seed);
        StrategyHashMap<String, Integer> map = new StrategyHashMap<>(strategy);
        UnifiedMapWithHashingStrategy<String, Integer> oracle =
                new UnifiedMapWithHashingStrategy<>(strategy);
        for (int i = 0; i < steps; i++) {
            String key = word(random);
            Integer value = random.nextInt(1_000);
            switch (random.nextInt(6)) {
                case 0, 1 ->
                    check(Objects.equals(map.put(key, value), oracle.put(key, value)), "put");
                case 2 -> check(Objects.equals(map.remove(key), oracle.remove(key)), "remove");
                case 3, 4 -> check(Objects.equals(map.get(key), oracle.get(key)), "get");
                default -> check(map.containsKey(key) == oracle.containsKey(key), "containsKey");
            }
            check(map.size() == oracle.size(), "size at step " + i);
        }
        Set<String> folded = new HashSet<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            check(folded.add(e.getKey().toLowerCase(Locale.ROOT)), "one key per equal class");
            check(Objects.equals(oracle.get(e.getKey()), e.getValue()), "value of " + e.getKey());
        }
        check(folded.size() == oracle.size(), "entry count");
    }

    /** Eight hashes for many keys: probing across deleted slots, reuse and resizes stay correct. */
    static void collisions(long seed, int steps) {
        Random random = new Random(seed);
        StrategyHashMap<Integer, Integer> map = new StrategyHashMap<>(new FewHashes());
        HashMap<Integer, Integer> oracle = new HashMap<>();
        for (int i = 0; i < steps; i++) {
            Integer key = random.nextInt(600);
            if (random.nextInt(3) == 0) {
                check(Objects.equals(map.remove(key), oracle.remove(key)), "remove");
            } else {
                check(Objects.equals(map.put(key, i), oracle.put(key, i)), "put");
            }
            check(Objects.equals(map.get(key), oracle.get(key)), "get after change");
        }
        check(entries(map.entrySet()).equals(entries(oracle.entrySet())),
                "entries with collisions");
    }

    /** Entries write through; the iterator removes. */
    static void entryView() {
        StrategyHashMap<String, Integer> map = new StrategyHashMap<>();
        for (int i = 0; i < 100; i++) {
            map.put("k" + i, i);
        }
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            e.setValue(e.getValue() * 10);
        }
        check(map.get("k7") == 70, "setValue writes through");
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() % 20 == 0) {
                it.remove();
            }
        }
        check(map.size() == 50 && !map.containsKey("k8") && map.containsKey("k9"),
                "iterator remove");
        check(map.entrySet().contains(Map.entry("k9", 90)), "entry equality");
        map.entrySet().clear();
        check(map.isEmpty() && map.get("k9") == null, "view clear");
    }

    /** A change outside the iterator makes it fail. */
    static void failFast() {
        StrategyHashMap<Integer, Integer> map = new StrategyHashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        Iterator<Map.Entry<Integer, Integer>> it = map.entrySet().iterator();
        it.next();
        map.put(3, 3);
        try {
            it.next();
            check(false, "must fail fast");
        } catch (ConcurrentModificationException expected) {
            checks++;
        }
        try {
            new StrategyHashMap<String, String>(null);
            check(false, "null strategy must throw");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    /** The mappings as a set of [key, value] lists, nulls kept. */
    static Set<List<Object>> entries(Iterable<? extends Map.Entry<?, ?>> mappings) {
        Set<List<Object>> out = new HashSet<>();
        for (Map.Entry<?, ?> e : mappings) {
            out.add(Arrays.asList(e.getKey(), e.getValue()));
        }
        return out;
    }

    static String word(Random random) {
        char[] chars = new char[1 + random.nextInt(3)];
        for (int i = 0; i < chars.length; i++) {
            char c = (char) ('a' + random.nextInt(4));
            chars[i] = random.nextBoolean() ? Character.toUpperCase(c) : c;
        }
        return new String(chars);
    }
}
