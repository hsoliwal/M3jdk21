/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Comparator;

/** Standalone deterministic differential oracle, including null and aliasing. */
public final class M3NativeFiveLibrarySelfTest {
    private static long checks;
    private M3NativeFiveLibrarySelfTest() { }
    private static void check(boolean p, String reason) {
        checks++;
        if (!p) throw new AssertionError(reason + " at check=" + checks);
    }
    private static void reject(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + kind);
    }
    private static void bimap() {
        M3IndexedBiMap<Integer, Integer> actual = new M3IndexedBiMap<>();
        Map<Integer, Integer> expected = new LinkedHashMap<>();
        Random r = new Random(0x23ae8901L);
        check(actual.inverse().inverse() == actual, "inverse identity");
        for (int step = 0; step < 14000; step++) {
            Integer key = r.nextInt(8) == 0 ? null : r.nextInt(42);
            Integer value = r.nextInt(9) == 0 ? null : r.nextInt(45);
            int op = r.nextInt(8);
            switch (op) {
                case 0 -> {
                    if (expected.containsValue(value)
                            && (!expected.containsKey(key) || !Objects.equals(expected.get(key), value))) {
                        reject(IllegalArgumentException.class, () -> actual.put(key, value));
                    } else {
                        check(Objects.equals(expected.put(key, value), actual.put(key, value)), "put result");
                    }
                }
                case 1 -> {
                    expected.entrySet().removeIf(e -> Objects.equals(e.getValue(), value)
                            && !Objects.equals(e.getKey(), key));
                    check(Objects.equals(expected.put(key, value), actual.forcePut(key, value)), "forcePut result");
                }
                case 2 -> check(Objects.equals(expected.remove(key), actual.remove(key)), "remove result");
                case 3 -> {
                    if (!expected.containsValue(value) && !expected.containsKey(key)) {
                        expected.put(key, value);
                        actual.inverse().put(value, key);
                        check(Objects.equals(value, actual.get(key)), "inverse put");
                    } else if (!expected.containsValue(value) && expected.containsKey(key)) {
                        reject(IllegalArgumentException.class, () -> actual.inverse().put(value, key));
                    }
                }
                case 4 -> {
                    if (expected.containsKey(key)) {
                        var it = actual.entrySet().iterator();
                        while (it.hasNext()) if (Objects.equals(it.next().getKey(), key)) { it.remove(); break; }
                        expected.remove(key);
                    }
                }
                case 5 -> {
                    if (expected.containsKey(key)) {
                        Iterator<Map.Entry<Integer,Integer>> entries = actual.entrySet().iterator();
                        while (entries.hasNext()) {
                            var e = entries.next();
                            if (Objects.equals(e.getKey(), key)) {
                                if (expected.containsValue(value)
                                        && !Objects.equals(expected.get(key), value)) {
                                    reject(IllegalArgumentException.class, () -> e.setValue(value));
                                } else {
                                    e.setValue(value);
                                    expected.put(key, value);
                                }
                                break;
                            }
                        }
                    }
                }
                case 6 -> {
                    if (expected.containsValue(value)) {
                        Integer owner = null;
                        for (var e : expected.entrySet()) if (Objects.equals(e.getValue(), value)) { owner = e.getKey(); break; }
                        expected.remove(owner);
                        check(Objects.equals(owner, actual.inverse().remove(value)), "inverse remove");
                    }
                }
                default -> {
                    check(Objects.equals(expected.get(key), actual.get(key)), "get result");
                    check(expected.containsKey(key) == actual.containsKey(key), "contains key");
                }
            }
            check(expected.equals(actual) && actual.equals(expected), "forward equality step=" + step);
            check(actual.size() == expected.size(), "size");
            HashMap<Integer,Integer> reversed = new HashMap<>();
            expected.forEach((k,v) -> check(reversed.put(v,k) == null || v == null,
                    "oracle unique inverted values"));
            check(reversed.equals(actual.inverse()), "inverse map equality");
            check(actual.inverse().inverse() == actual, "inverse back reference");
        }
        M3IndexedBiMap<String,Integer> shortMap = new M3IndexedBiMap<>();
        shortMap.put("one", 1);
        var iter = shortMap.entrySet().iterator();
        var e = iter.next();
        e.setValue(2);
        iter.remove();
        check(shortMap.isEmpty() && shortMap.inverse().isEmpty(), "current reverse value removed");
        reject(IllegalStateException.class, iter::remove);
    }

    private static void mapOfLists() {
        M3ListMultiValueMap<String, Integer> actual = new M3ListMultiValueMap<>();
        Map<String, List<Integer>> oracle = new LinkedHashMap<>();
        Random r = new Random(0x789abc45L);
        List<Integer> supplied = new ArrayList<>(Arrays.asList(1, null, 2));
        actual.put("supplied", supplied);
        oracle.put("supplied", supplied);
        actual.add("supplied", 3);
        check(supplied.size() == 4, "caller-list alias preserved");
        for (int step = 0; step < 15000; step++) {
            String key = r.nextInt(11) == 0 ? null : "k" + r.nextInt(35);
            Integer value = r.nextInt(11) == 0 ? null : r.nextInt(85);
            switch (r.nextInt(6)) {
                case 0 -> {
                    actual.add(key, value);
                    oracle.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
                case 1 -> {
                    List<Integer> a = actual.get(key);
                    if (a == null) { a = new ArrayList<>(); actual.put(key, a); }
                    a.add(value);
                    oracle.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
                case 2 -> {
                    actual.set(key, value);
                    oracle.put(key, new ArrayList<>(Arrays.asList(value)));
                }
                case 3 -> check(Objects.equals(actual.remove(key), oracle.remove(key)), "map remove");
                case 4 -> {
                    actual.addAll(key, List.of());
                    oracle.computeIfAbsent(key, k -> new ArrayList<>());
                }
                default -> {
                    actual.asSingleValueMap().put(key, value);
                    oracle.put(key, new ArrayList<>(Arrays.asList(value)));
                }
            }
            check(actual.equals(oracle), "map-of-lists forward equality");
            check(actual.size() == oracle.size(), "map-of-lists key count");
            Integer expectedFirst = oracle.get(key) == null || oracle.get(key).isEmpty()
                    ? null : oracle.get(key).get(0);
            check(Objects.equals(expectedFirst, actual.getFirst(key)), "first mapped item");
            var detached = actual.toSingleValueMap();
            var live = actual.asSingleValueMap();
            for (var entry : oracle.entrySet()) {
                Integer first = entry.getValue() == null || entry.getValue().isEmpty() ? null
                        : entry.getValue().get(0);
                check(live.containsKey(entry.getKey()) && Objects.equals(live.get(entry.getKey()), first),
                        "live map projection");
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    check(Objects.equals(first, detached.get(entry.getKey())), "detached first");
                }
            }
        }
        actual.put("nullList", null);
        check(actual.containsKey("nullList") && actual.getFirst("nullList") == null, "mapped null");
        actual.add("nullList", null);
        check(actual.get("nullList").size() == 1, "new M3 list on null mapping");
        actual.put("empty", new M3ArrayList<>());
        check(actual.containsKey("empty") && actual.get("empty").isEmpty(), "empty differs absent");
        var live = actual.asSingleValueMap();
        check(live.containsKey("empty") && live.get("empty") == null, "single map live empty");
        live.put("empty", 52);
        check(actual.getFirst("empty") == 52, "live writes through");
        var entries = live.entrySet().iterator();
        String firstKey = entries.next().getKey();
        entries.remove();
        check(!actual.containsKey(firstKey), "iterator remove writes through");
        actual.clear();
        check(actual.isEmpty(), "cleared");
    }


    private static void setMultimap() {
        M3IndexedSetMultiMap<Integer, Integer> actual = new M3IndexedSetMultiMap<>();
        Map<Integer, LinkedHashSet<Integer>> expected = new LinkedHashMap<>();
        Random random = new Random(0x615d271eL);
        for (int step = 0; step < 7000; step++) {
            Integer key = random.nextInt(9) == 0 ? null : random.nextInt(37);
            Integer value = random.nextInt(9) == 0 ? null : random.nextInt(46);
            switch (random.nextInt(6)) {
                case 0,1 -> {
                    boolean oracle = expected.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value);
                    check(actual.put(key, value) == oracle, "set-valued put");
                }
                case 2 -> {
                    Set<Integer> previous = expected.get(key);
                    boolean removed = previous != null && previous.remove(value);
                    if (previous != null && previous.isEmpty()) expected.remove(key);
                    check(actual.remove(key, value) == removed, "set-valued remove");
                }
                case 3 -> {
                    Set<Integer> previous = expected.remove(key);
                    check(new LinkedHashSet<>(actual.removeAll(key)).equals(previous == null ? Set.of() : previous), "removed set snapshot");
                }
                case 4 -> check(actual.get(key).add(value)
                        == expected.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value),
                        "live add");
                default -> {
                    var v = expected.get(key);
                    check(actual.get(key).size() == (v == null ? 0 : v.size()), "live size");
                }
            }
            long count = expected.values().stream().mapToLong(Set::size).sum();
            check(actual.size() == count, "total pair count");
            check(actual.keyCount() == expected.size(), "total distinct keys");
            for (var e : expected.entrySet()) {
                check(new LinkedHashSet<>(actual.snapshot(e.getKey())).equals(e.getValue()), "value set snapshot");
                check(new LinkedHashSet<>(actual.get(e.getKey())).equals(e.getValue()), "value set view");
            }
            if (step % 17 == 0) {
                var first = actual.snapshot(key);
                var filtered = actual.snapshotFiltered(key, v -> v != null && v > 20,
                        M3IndexMaskCompactor.Backend.JAVA);
                long match = first.stream().filter(v -> v != null && v > 20).count();
                check(filtered.size() == match, "prepared selection");
                if (M3IndexMaskCompactor.nativeAvailable()) {
                    var nativeResult = actual.snapshotFiltered(key, v -> v != null && v > 20,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    check(nativeResult.equals(filtered), "existing JNI native mask parity");
                } else {
                    reject(UnsatisfiedLinkError.class, () -> actual.snapshotFiltered(key,
                            v -> v != null && v > 20,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                }
                var sorted = filtered.sorted(Comparator.naturalOrder());
                for (int i = 1; i < sorted.size(); i++)
                    check(sorted.get(i-1) <= sorted.get(i), "prepared index-only sort");
            }
        }
        actual.clear();
        check(actual.size() == 0 && actual.keyCount() == 0, "set relation clear");
    }

    private static void nativeSdkFactories() {
        check(M3NativeFiveLibrarySdk.list().isEmpty(), "M3 list factory");
        check(M3NativeFiveLibrarySdk.set().isEmpty(), "M3 set factory");
        check(M3NativeFiveLibrarySdk.map().isEmpty(), "M3 map factory");
        check(M3NativeFiveLibrarySdk.sortedMap(Comparator.<Integer>naturalOrder()).isEmpty(),
                "M3 sorted map factory");
        check(M3NativeFiveLibrarySdk.bag() != null, "M3 bag factory");
        check(M3NativeFiveLibrarySdk.biMap().inverse().inverse() != null, "M3 bimap factory");
        check(M3NativeFiveLibrarySdk.listMultimap() != null, "M3 list multimap factory");
        check(M3NativeFiveLibrarySdk.setMultimap().size() == 0, "M3 set multimap factory");
        check(M3NativeFiveLibrarySdk.mapOfLists().size() == 0, "M3 map-of-lists factory");
        check(M3NativeFiveLibrarySdk.table() != null, "M3 table factory");
        check(M3NativeFiveLibrarySdk.snapshot(List.of(1,2,3)).size() == 3,
                "M3 immutable indexed snapshot factory");
        check(M3NativeFiveLibrarySdk.longProjection(new long[0]) != null,
                "M3 long projection factory");
    }
    public static void main(String[] args) {
        nativeSdkFactories();
        bimap();
        mapOfLists();
        setMultimap();
        System.out.println("PASS M3NativeFiveLibrarySelfTest checks=" + checks);
    }
}
