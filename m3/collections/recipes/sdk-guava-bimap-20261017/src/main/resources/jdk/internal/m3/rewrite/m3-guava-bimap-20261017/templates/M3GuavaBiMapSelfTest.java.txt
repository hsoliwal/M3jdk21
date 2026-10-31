/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** JDK-only deterministic behavior oracle; real Guava differential is a separate JUnit test. */
public final class M3GuavaBiMapSelfTest {
    private static long checks;
    private M3GuavaBiMapSelfTest() { }
    private static void check(boolean truth, String note) {
        checks++;
        if (!truth) throw new AssertionError(note + " at check " + checks);
    }
    private static void reject(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("expected " + type + " got " + problem, problem);
        }
        throw new AssertionError("expected " + type + " but accepted");
    }
    private static <K,V> void verify(BiMap<K,V> map, Map<K,V> oracle) {
        check(map.size() == oracle.size(), "size");
        check(map.equals(oracle) && oracle.equals(map), "equals forward");
        check(map.hashCode() == oracle.hashCode(), "hash forward");
        check(map.inverse().inverse() == map, "inverse identity");
        check(map.values() instanceof Set<?>, "values is set");
        check(map.values().size() == map.size(), "unique values");
        Map<V,K> reversed = new HashMap<>();
        for (Map.Entry<K,V> e : oracle.entrySet()) {
            reversed.put(e.getValue(), e.getKey());
        }
        check(map.inverse().equals(reversed), "inverse equality");
        check(map.inverse().hashCode() == reversed.hashCode(), "inverse hash");
        check(new HashSet<>(map.values()).equals(new HashSet<>(oracle.values())), "value members");
        check(new HashSet<>(map.keySet()).equals(oracle.keySet()), "key members");
        for (K key : oracle.keySet()) {
            check(map.containsKey(key), "contains key");
            check(Objects.equals(map.get(key), oracle.get(key)), "get key");
        }
        for (V value : oracle.values()) {
            check(map.containsValue(value), "contains value");
            check(Objects.equals(map.inverse().get(value), reversed.get(value)), "reverse lookup");
        }
    }
    private static void run() {
        BiMap<Integer,Integer> map = new M3GuavaBiMap<>();
        Map<Integer,Integer> oracle = new HashMap<>();
        Random random = new Random(202610171844L);
        verify(map, oracle);
        for (int round = 0; round < 6000; round++) {
            Integer key = random.nextInt(13) == 0 ? null : random.nextInt(74) - 35;
            Integer value = random.nextInt(13) == 0 ? null : random.nextInt(74) - 35;
            switch (random.nextInt(8)) {
                case 0, 1 -> {
                    boolean collision = oracle.containsValue(value) &&
                        !(oracle.containsKey(key) && Objects.equals(oracle.get(key), value));
                    if (collision) {
                        reject(IllegalArgumentException.class, () -> map.put(key, value));
                    } else {
                        check(Objects.equals(map.put(key, value), oracle.put(key, value)), "put result");
                    }
                }
                case 2 -> {
                    Integer former = oracle.put(key, value);
                    oracle.entrySet().removeIf(e -> !Objects.equals(e.getKey(), key)
                            && Objects.equals(e.getValue(), value));
                    check(Objects.equals(map.forcePut(key, value), former), "force result");
                }
                case 3 -> check(Objects.equals(map.remove(key), oracle.remove(key)), "remove result");
                case 4 -> {
                    if (oracle.containsKey(key)) {
                        check(map.keySet().remove(key), "keySet remove");
                        oracle.remove(key);
                    } else check(!map.keySet().remove(key), "keySet absent");
                }
                case 5 -> {
                    if (oracle.containsValue(value)) {
                        check(map.values().remove(value), "values remove");
                        oracle.entrySet().removeIf(e -> Objects.equals(e.getValue(), value));
                    } else check(!map.values().remove(value), "values absent");
                }
                case 6 -> {
                    if (!map.isEmpty()) {
                        var it = map.entrySet().iterator();
                        var e = it.next();
                        Integer toRemove = e.getKey();
                        it.remove();
                        oracle.remove(toRemove);
                    }
                }
                default -> {
                    if (round % 17 == 0) { map.clear(); oracle.clear(); }
                    else if (!map.isEmpty()) {
                        Integer currentKey = map.keySet().iterator().next();
                        Integer old = oracle.get(currentKey);
                        if (!Objects.equals(old, value) && oracle.containsValue(value)) {
                            reject(IllegalArgumentException.class,
                                    () -> map.entrySet().iterator().next().setValue(value));
                        } else {
                            var entry = map.entrySet().iterator().next();
                            Integer actualKey = entry.getKey();
                            check(Objects.equals(entry.setValue(value), oracle.put(actualKey, value)), "entry setValue");
                        }
                    }
                }
            }
            verify(map, oracle);
        }
        BiMap<String,String> named = new M3GuavaBiMap<>();
        named.put(null,null);
        check(named.inverse().containsKey(null), "inverse null");
        reject(IllegalArgumentException.class, () -> named.put("other", null));
        check(named.size() == 1, "duplicate null value refusal leaves state intact");
        named.inverse().put("next", "v");
        check(Objects.equals(named.get("v"), "next"), "inverse put propagates");
        check(named.values().remove("next"), "values backed removal");
        check(!named.containsKey("v"), "removed through inverse keySet");
        named.put("a", "1");
        var entry = named.entrySet().iterator().next();
        named.remove(entry.getKey());
        reject(IllegalStateException.class, entry::getValue);
        check(named.inverse().inverse() == named, "back reference");
        System.out.println("PASS M3GuavaBiMapSelfTest checks=" + checks);
    }
    public static void main(String[] args) { run(); }
}
