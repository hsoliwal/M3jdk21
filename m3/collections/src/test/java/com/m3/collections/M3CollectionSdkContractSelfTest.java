/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free Java21 differential oracle. No external provider dependency or fake adapter. */
public final class M3CollectionSdkContractSelfTest {
    private static long checks;
    private M3CollectionSdkContractSelfTest() { }
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static void fails(Class<? extends Throwable> expected, Runnable work) {
        checks++;
        try { work.run(); }
        catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("expected " + expected + " got " + t, t);
        }
        throw new AssertionError("expected failure " + expected);
    }

    private static void names() {
        check(M3CollectionSdkNames.size() >= 100, "at least 100 provider names reviewed");
        for (M3CollectionSdkNames.Provider provider : M3CollectionSdkNames.Provider.values()) {
            List<M3CollectionSdkNames.Family> entries = M3CollectionSdkNames.from(provider);
            check(!entries.isEmpty(), "one or more known API owners per provider");
            for (var family : entries) {
                check(family.provider() == provider, "provider index");
                check(M3CollectionSdkNames.require(family.upstreamFqcn()) == family, "index identity");
                check(!family.vendorContractProven(), "no false third-party/JDK substitutability");
                if (family.disposition() == M3CollectionSdkNames.Disposition.PENDING) {
                    check(family.isProposedOnly(), "proposals must not masquerade as implemented");
                }
            }
        }
        check(M3CollectionSdkNames.require("com.google.common.collect.Multimap").isProposedOnly(),
                "Guava namespace remains explicit");
        check(M3CollectionSdkNames.require("org.springframework.util.MultiValueMap").isProposedOnly(),
                "Spring is a map-of-lists, not Guava Multimap");
        check(M3CollectionSdkNames.require("org.apache.commons.collections4.MultiValuedMap").isProposedOnly(),
                "Apache retains its API identity");
        check(M3CollectionSdkNames.require("org.eclipse.collections.api.multimap.list.MutableListMultimap")
                .isProposedOnly(), "Eclipse list multimap API identity");
        check(M3CollectionSdkNames.require("java.util.ArrayList").hasExistingM3Backend(),
                "JDK owner retains a different backend name");
        fails(IllegalArgumentException.class, () -> M3CollectionSdkNames.require("guava.Multimap"));
        fails(NullPointerException.class, () -> M3CollectionSdkNames.from(null));
    }

    private static void inspect(M3IndexedMultiValueMap<Integer,Integer> actual,
            Map<Integer, List<Integer>> reference, Map<Integer,List<Integer>> referenceStates) {
        int size = 0;
        for (List<Integer> list : reference.values()) size += list.size();
        check(actual.size() == size, "flattened size");
        check(actual.keyCount() == reference.size(), "distinct keys");
        check(actual.keysSnapshot().equals(new ArrayList<>(reference.keySet())), "group insertion order");
        for (Integer k : reference.keySet()) {
            var snap = actual.snapshot(k);
            List<Integer> v = reference.get(k);
            check(snap.equals(v), "per-key encounter order / nulls / duplicates");
            check(actual.valueCount(k) == v.size(), "per-key count");
            check(Objects.equals(actual.getFirst(k), v.get(0)), "first value may be null");
            for (int i = 0; i < v.size(); i++) {
                check(Objects.equals(snap.get(i), v.get(i)), "indexed values");
                check(actual.stateAt(k, i) == referenceStates.get(k).get(i), "unsigned state");
            }
            if (v.size() > 0) {
                check(snap.subList(1, v.size()).equals(v.subList(1, v.size())), "slice zero copy");
                int from = v.size()/2;
                var reversed = snap.sorted(Comparator.nullsLast(Comparator.reverseOrder()));
                ArrayList<Integer> expected = new ArrayList<>(v);
                expected.sort(Comparator.nullsLast(Comparator.reverseOrder()));
                check(reversed.equals(expected), "stable sorted snapshot value order");
                check(snap.subList(from, v.size()).equals(v.subList(from, v.size())), "slice");
            }
        }
        check(actual.valueCount(8192) == 0 && actual.snapshot(8192).isEmpty(), "absent key");
    }

    private static void test(Random random) {
        M3IndexedMultiValueMap<Integer,Integer> target = new M3IndexedMultiValueMap<>();
        LinkedHashMap<Integer,List<Integer>> expected = new LinkedHashMap<>();
        LinkedHashMap<Integer,List<Integer>> flags = new LinkedHashMap<>();
        for (int n = 0; n < 14000; n++) {
            Integer key = random.nextInt(22) == 0 ? null : random.nextInt(17) - 8;
            int action = random.nextInt(13);
            if (action < 7) {
                Integer value = random.nextInt(11) == 0 ? null : random.nextInt(11) - 5;
                int initial = random.nextInt(128) << 1;
                target.add(key, value, initial);
                expected.computeIfAbsent(key, x -> new ArrayList<>()).add(value);
                flags.computeIfAbsent(key, x -> new ArrayList<>()).add(initial | 1);
            } else if (action < 9) {
                int removed = target.removeKey(key);
                List<Integer> deleted = expected.remove(key);
                flags.remove(key);
                check(removed == (deleted == null ? 0 : deleted.size()), "group removal and slot recycling");
            } else if (action < 11) {
                List<Integer> rows = expected.get(key);
                if (rows != null && !rows.isEmpty()) {
                    int index = random.nextInt(rows.size());
                    int set = 2 << random.nextInt(5);
                    int clear = 2 << random.nextInt(5);
                    if ((set & clear) == 0) {
                        int before = target.updateState(key,index,set,clear);
                        int oracle = flags.get(key).get(index);
                        check(before == oracle, "prior state");
                        flags.get(key).set(index, (oracle | set) & ~clear);
                    }
                }
            } else if (action == 11) {
                var immutable = target.snapshot(key);
                ArrayList<Integer> copy = new ArrayList<>(immutable);
                int removed = target.removeKey(key);
                List<Integer> old = expected.remove(key);
                flags.remove(key);
                check(removed == (old == null ? 0 : old.size()), "remove consistency");
                check(immutable.equals(copy), "snapshots detach from reclaimed storage");
            } else if (random.nextInt(5) == 0) {
                target.clear(); expected.clear(); flags.clear();
            }
            if (n % 39 == 0) inspect(target, expected, flags);
            if (n % 71 == 0) {
                Integer probe = key;
                List<Integer> list = expected.getOrDefault(probe, List.of());
                var current = target.snapshot(probe);
                AtomicInteger calls = new AtomicInteger();
                var positive = current.filtered(x -> {
                    calls.incrementAndGet(); return x != null && x >= 0;
                });
                check(calls.get() == list.size(), "filter is called once per snapshot occurrence");
                ArrayList<Integer> oracle = new ArrayList<>();
                for (Integer val : list) if (val != null && val >= 0) oracle.add(val);
                check(positive.equals(oracle), "predicate once filtering");
                positive.sorted(Comparator.naturalOrder()).subList(0, positive.size());
                check(calls.get() == list.size(), "sorting and slicing do not refilter");
                byte[] states = new byte[list.size()];
                for (int i = 0; i < states.length; i++) states[i] = flags.get(probe).get(i).byteValue();
                int required = M3IndexedMultiValueMap.DIRTY;
                var javaSelected = target.selected(probe, required, 0, M3IndexMaskCompactor.Backend.JAVA);
                ArrayList<Integer> byMask = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) if ((states[i] & required) == required) byMask.add(list.get(i));
                check(javaSelected.equals(byMask), "unsigned byte states");
                if (M3IndexMaskCompactor.nativeAvailable()) {
                    var nativeSelected = target.selected(probe, required, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    check(nativeSelected.equals(javaSelected), "real JNI parity");
                } else {
                    fails(UnsatisfiedLinkError.class, () -> target.selected(probe, required, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                }
            }
        }
        inspect(target, expected, flags);
        fails(IllegalArgumentException.class, () -> target.add(0, 1, M3IndexedMultiValueMap.LIVE));
        fails(IllegalArgumentException.class, () -> target.updateState(0,0,1,0));
        fails(IllegalArgumentException.class, () -> target.selected(0,256,0, M3IndexMaskCompactor.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> target.selected(0,2,2, M3IndexMaskCompactor.Backend.JAVA));
        check(target.revision() >= 0, "mutation revision available");
        target.clear();
        check(target.size() == 0 && target.keyCount() == 0, "clear empties");
        target.add(null,null);
        check(target.containsKey(null) && target.getFirst(null) == null, "null key/value accepted");
    }

    public static void main(String[] args) {
        names();
        test(new Random(0x331acb0195L));
        System.out.println("PASS M3CollectionSdkContractSelfTest checks=" + checks
                + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
