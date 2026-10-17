/*
 * Copyright (C) 2007 The Guava Authors
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Official test-suite wiring follows google/guava v33.4.8 HashBiMapTest and
 * LinkedHashMultisetTest (f06690fa3e874f65515e8fd338a74d636e2c792f).
 * Donor Git blobs: f4bee7bf9830b47d6519453c093a999c77ebd7f9 and
 * adfbfe65634178c4b7131029c73e47d37dc4327e, respectively.
 * Modified 2026: supply M3 owners, omit unsupported serialization features,
 * execute the unmodified upstream suites through Jupiter, and add source-bound
 * count-plane/inverse-consistency regression and differential tests.
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.MultisetFeature;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.m3.collections.M3ObjectCounts;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.jupiter.api.Test;

/** Exact-version upstream contracts plus focused M3 count/ownership regressions. */
final class M3GuavaContractTest {
    @Test void officialGuavaMultisetContractSuite() {
        TestSuite suite = MultisetTestSuiteBuilder.using(new TestStringMultisetGenerator() {
            @Override protected Multiset<String> create(String[] elements) {
                M3GuavaMultiset<String> result = new M3GuavaMultiset<>();
                Collections.addAll(result, elements);
                return result;
            }

            @Override public List<String> order(List<String> insertionOrder) {
                // A linked multiset visits each distinct key's occurrences together.
                List<String> grouped = new ArrayList<>();
                for (String element : insertionOrder) {
                    int position = grouped.indexOf(element);
                    if (position < 0) grouped.add(element);
                    else grouped.add(position, element);
                }
                return grouped;
            }
        }).named("M3GuavaMultiset / Guava 33.4.8-jre")
                .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
                        CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                        CollectionFeature.ALLOWS_NULL_VALUES, CollectionFeature.GENERAL_PURPOSE,
                        MultisetFeature.ENTRIES_ARE_VIEWS)
                .createTestSuite();
        runOfficial(suite);
    }

    @Test void officialGuavaBiMapContractSuite() {
        TestSuite suite = BiMapTestSuiteBuilder.using(new TestStringBiMapGenerator() {
            @Override protected BiMap<String, String> create(Map.Entry<String, String>[] entries) {
                M3GuavaBiMap<String, String> result = new M3GuavaBiMap<>();
                for (Map.Entry<String, String> entry : entries) result.put(entry.getKey(), entry.getValue());
                return result;
            }
        }).named("M3GuavaBiMap / Guava 33.4.8-jre")
                .withFeatures(CollectionSize.ANY, CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                        CollectionFeature.KNOWN_ORDER, MapFeature.ALLOWS_NULL_KEYS,
                        MapFeature.ALLOWS_NULL_VALUES, MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        MapFeature.GENERAL_PURPOSE)
                .createTestSuite();
        runOfficial(suite);
    }

    private static void runOfficial(TestSuite suite) {
        TestResult result = new TestResult();
        suite.run(result);
        List<TestFailure> failures = new ArrayList<>();
        failures.addAll(Collections.list(result.failures()));
        failures.addAll(Collections.list(result.errors()));
        System.out.println(suite.getName() + ": executed=" + result.runCount()
                + ", failures=" + result.failureCount() + ", errors=" + result.errorCount());
        assertTrue(suite.countTestCases() > 0, "upstream suite must contain tests");
        assertEquals(suite.countTestCases(), result.runCount(), "every generated upstream case executed");
        assertTrue(failures.isEmpty(), () -> failures.stream()
                .map(failure -> failure.failedTest() + "\n" + failure.trace())
                .reduce("", (left, right) -> left + right + "\n"));
    }

    @Test void multisetCountOverflowAndSaturatedCollectionSize() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        M3GuavaMultiset<String> actual = new M3GuavaMultiset<>(owner);
        assertEquals(0, actual.add("a", Integer.MAX_VALUE));
        assertEquals(0, actual.add("b", Integer.MAX_VALUE));
        assertEquals(2L * Integer.MAX_VALUE, owner.totalCount());
        assertEquals(Integer.MAX_VALUE, actual.size());
        assertThrows(IllegalArgumentException.class, () -> actual.add("a", 1));
        assertEquals(Integer.MAX_VALUE, actual.count("a"));
        assertEquals(2L * Integer.MAX_VALUE, owner.totalCount());
        assertEquals(Integer.MAX_VALUE, actual.remove("a", Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, owner.totalCount());
        actual.verifyInvariants();
    }

    @Test void multisetNegativeAndZeroInputsDoNotMutate() {
        M3GuavaMultiset<String> actual = new M3GuavaMultiset<>();
        actual.add(null, 3);
        assertThrows(IllegalArgumentException.class, () -> actual.add(null, -1));
        assertThrows(IllegalArgumentException.class, () -> actual.remove(null, -1));
        assertThrows(IllegalArgumentException.class, () -> actual.setCount(null, -1));
        assertThrows(IllegalArgumentException.class, () -> actual.setCount(null, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> actual.setCount(null, 100, -1));
        assertEquals(3, actual.add(null, 0));
        assertEquals(3, actual.remove(null, 0));
        assertEquals(0, actual.add("absent", 0));
        assertFalse(actual.contains("absent"));
        assertEquals(3, actual.count(null));
        actual.verifyInvariants();
    }

    @Test void multisetLiveEntriesAndViewsSurviveOwnerMutations() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        M3GuavaMultiset<String> first = new M3GuavaMultiset<>(owner);
        M3GuavaMultiset<String> second = new M3GuavaMultiset<>(owner);
        first.add("a", 2);
        owner.setFlags("a", 0x8000_0041);
        Multiset.Entry<String> entry = first.entrySet().iterator().next();
        second.add("a", 3);
        assertEquals(5, entry.getCount());
        assertEquals(0x8000_0041, owner.flags("a"));
        assertTrue(second.elementSet().remove("a"));
        assertEquals(0, entry.getCount());
        assertEquals(0, owner.flags("a"));
        first.add("a", 4);
        assertEquals(4, entry.getCount());
        assertEquals(0, owner.flags("a"), "reused slots must not inherit old element flags");
        assertFalse(first.entrySet().remove(Multisets.immutableEntry("a", 3)));
        assertTrue(first.entrySet().remove(Multisets.immutableEntry("a", 4)));
        assertTrue(second.isEmpty());
        owner.verifyInvariants();
    }

    @Test void multisetIteratorRemovesOneAndDistinctIteratorRemovesAll() {
        M3GuavaMultiset<String> actual = new M3GuavaMultiset<>();
        actual.add(null, 3);
        actual.add("b", 2);
        Iterator<String> repeated = actual.iterator();
        assertThrows(IllegalStateException.class, repeated::remove);
        assertNull(repeated.next());
        repeated.remove();
        assertEquals(2, actual.count(null));
        assertThrows(IllegalStateException.class, repeated::remove);
        assertNull(repeated.next());
        assertNull(repeated.next());
        assertEquals("b", repeated.next());
        repeated.remove();
        assertEquals("b", repeated.next());
        repeated.remove();
        assertFalse(actual.contains("b"));
        assertFalse(repeated.hasNext());
        Iterator<Multiset.Entry<String>> distinct = actual.entrySet().iterator();
        assertNull(distinct.next().getElement());
        distinct.remove();
        assertTrue(actual.isEmpty());
        actual.verifyInvariants();
    }

    @Test void multisetBulkSelfOperationsKeepCountsAndInsertionOrder() {
        M3GuavaMultiset<String> actual = new M3GuavaMultiset<>();
        actual.addAll(Arrays.asList("a", "b", "a", null));
        assertTrue(actual.addAll(actual));
        assertEquals(List.of("a", "a", "a", "a", "b", "b"),
                new ArrayList<>(actual).subList(0, 6));
        assertEquals(2, actual.count(null));
        assertFalse(actual.retainAll(actual));
        assertTrue(actual.removeAll(actual));
        assertTrue(actual.isEmpty());
        actual.verifyInvariants();
    }

    @Test void multisetCollidingKeysAndMutationTraceMatchOfficialLinkedHashMultiset() {
        M3GuavaMultiset<Collision> actual = new M3GuavaMultiset<>();
        LinkedHashMultiset<Collision> expected = LinkedHashMultiset.create();
        Random random = new Random(0x4d33434f554e54L);
        for (int step = 0; step < 3500; step++) {
            Collision key = random.nextInt(9) == 0 ? null : new Collision(random.nextInt(31));
            int amount = random.nextInt(6);
            switch (random.nextInt(9)) {
                case 0 -> assertEquals(expected.add(key, amount), actual.add(key, amount));
                case 1 -> assertEquals(expected.remove(key, amount), actual.remove(key, amount));
                case 2 -> assertEquals(expected.setCount(key, amount), actual.setCount(key, amount));
                case 3 -> assertEquals(expected.setCount(key, amount, 4), actual.setCount(key, amount, 4));
                case 4 -> assertEquals(expected.elementSet().remove(key), actual.elementSet().remove(key));
                case 5 -> assertEquals(expected.entrySet().remove(Multisets.immutableEntry(key, amount)),
                        actual.entrySet().remove(Multisets.immutableEntry(key, amount)));
                case 6 -> {
                    if (!expected.isEmpty()) {
                        Iterator<Collision> ei = expected.iterator(), ai = actual.iterator();
                        assertEquals(ei.next(), ai.next());
                        ei.remove(); ai.remove();
                    }
                }
                case 7 -> assertEquals(expected.removeAll(Arrays.asList(key, key)), actual.removeAll(Arrays.asList(key, key)));
                default -> {
                    Collection<Collision> retain = Arrays.asList(key, null, new Collision(7));
                    assertEquals(expected.retainAll(retain), actual.retainAll(retain));
                }
            }
            assertEquals(expected, actual);
            assertEquals(actual, expected);
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
            actual.verifyInvariants();
        }
    }

    @Test void bimapDuplicateNullAndInverseIdentityContracts() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        assertSame(actual, actual.inverse().inverse());
        actual.put(null, 1);
        actual.put("a", null);
        assertThrows(IllegalArgumentException.class, () -> actual.put("b", 1));
        assertThrows(IllegalArgumentException.class, () -> actual.put("b", null));
        assertNull(actual.forcePut("b", 1));
        assertFalse(actual.containsKey(null));
        assertEquals("b", actual.inverse().get(1));
        assertEquals("a", actual.inverse().remove(null));
        assertFalse(actual.containsKey("a"));
        actual.verifyInvariants();
    }

    @Test void bimapEntriesFromIteratorAndArraysCannotBypassUniqueness() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        actual.put("a", 1); actual.put("b", 2);
        Map.Entry<String, Integer> entry = actual.entrySet().iterator().next();
        assertThrows(IllegalArgumentException.class, () -> entry.setValue(2));
        assertEquals(1, entry.setValue(3));
        assertEquals("a", actual.inverse().get(3));
        Object[] array = actual.entrySet().toArray();
        @SuppressWarnings("unchecked") Map.Entry<String, Integer> fromArray = (Map.Entry<String, Integer>) array[0];
        assertThrows(IllegalArgumentException.class, () -> fromArray.setValue(2));
        assertTrue(actual.entrySet().remove(fromArray));
        assertFalse(actual.containsValue(3));
        assertFalse(actual.inverse().containsKey(3));
        actual.verifyInvariants();
    }

    @Test void bimapAllViewRemovalPathsMaintainInverse() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        for (int i = 0; i < 10; i++) actual.put("k" + i, i);
        Iterator<Integer> values = actual.values().iterator();
        assertEquals(0, values.next()); values.remove();
        assertFalse(actual.inverse().containsKey(0));
        assertTrue(actual.keySet().removeAll(List.of("k1", "k2")));
        assertTrue(actual.values().removeIf(i -> i % 2 == 0));
        assertTrue(actual.entrySet().retainAll(Set.of(new AbstractMap.SimpleImmutableEntry<>("k3", 3))));
        assertEquals(Map.of("k3", 3), actual);
        actual.inverse().keySet().clear();
        assertTrue(actual.isEmpty());
        actual.verifyInvariants();
    }

    @Test void bimapDefaultMapMutatorsMaintainInverseAndRejectConflicts() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        actual.put("a", null); actual.put("b", 2);
        assertEquals(1, actual.computeIfAbsent("a", ignored -> 1));
        assertNull(actual.putIfAbsent("c", 3));
        assertTrue(actual.replace("b", 2, 4));
        assertEquals(6, actual.computeIfPresent("b", (key, value) -> value + 2));
        assertEquals(8, actual.merge("b", 2, Integer::sum));
        assertThrows(IllegalArgumentException.class, () -> actual.compute("c", (key, value) -> 8));
        assertEquals(3, actual.get("c"));
        assertTrue(actual.remove("a", 1));
        assertNull(actual.compute("c", (key, value) -> null));
        assertEquals(Map.of("b", 8), actual);
        actual.verifyInvariants();
    }

    @Test void bimapReplaceAllValidatesNullKeyConflictsAndCallbackFailure() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        actual.put(null, 1); actual.put("a", 2); actual.put("b", 3);
        Map<String, Integer> before = new LinkedHashMap<>(actual);
        assertThrows(IllegalArgumentException.class, () -> actual.replaceAll((key, value) -> 7));
        assertEquals(before, actual);
        assertThrows(IllegalStateException.class, () -> actual.replaceAll((key, value) -> {
            if (Objects.equals(key, "a")) throw new IllegalStateException("fixture callback failure");
            return value + 10;
        }));
        assertEquals(before, actual);
        actual.verifyInvariants();

        Set<String> heldKeys = actual.keySet();
        Set<Integer> heldValues = actual.values();
        M3GuavaBiMap<Integer, String> heldInverse = actual.inverse();
        BiMap<String, Integer> oracle = HashBiMap.create(before);
        oracle.replaceAll((key, value) -> 10 - value);
        actual.replaceAll((key, value) -> 10 - value);
        assertEquals(oracle, actual);
        assertEquals(oracle.keySet(), heldKeys);
        assertEquals(oracle.values(), heldValues);
        assertEquals(oracle.inverse(), heldInverse);
        assertSame(actual, heldInverse.inverse());
        actual.verifyInvariants();
    }

    @Test void bimapReplaceAllKeepsCallbackMutationObservableAndConsistent() {
        M3GuavaBiMap<String, Integer> actual = new M3GuavaBiMap<>();
        actual.put("a", 1);
        assertThrows(ConcurrentModificationException.class, () -> actual.replaceAll((key, value) -> {
            actual.put("b", 2);
            return value + 10;
        }));
        assertEquals(Map.of("a", 1, "b", 2), actual);
        actual.verifyInvariants();
    }

    @Test void bimapCollisionAndNullMutationTraceMatchesOfficialHashBiMap() {
        M3GuavaBiMap<Collision, Collision> actual = new M3GuavaBiMap<>();
        BiMap<Collision, Collision> expected = HashBiMap.create();
        Random random = new Random(0x4d3342494d4150L);
        for (int step = 0; step < 4500; step++) {
            Collision key = random.nextInt(7) == 0 ? null : new Collision(random.nextInt(23));
            Collision value = random.nextInt(7) == 0 ? null : new Collision(random.nextInt(23));
            switch (random.nextInt(8)) {
                case 0 -> sameOutcome(() -> expected.put(key, value), () -> actual.put(key, value));
                case 1 -> sameOutcome(() -> expected.forcePut(key, value), () -> actual.forcePut(key, value));
                case 2 -> sameOutcome(() -> expected.inverse().put(value, key), () -> actual.inverse().put(value, key));
                case 3 -> sameOutcome(() -> expected.inverse().forcePut(value, key), () -> actual.inverse().forcePut(value, key));
                case 4 -> assertEquals(expected.remove(key), actual.remove(key));
                case 5 -> assertEquals(expected.inverse().remove(value), actual.inverse().remove(value));
                case 6 -> assertEquals(expected.keySet().remove(key), actual.keySet().remove(key));
                default -> assertEquals(expected.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(key, value)),
                        actual.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(key, value)));
            }
            assertEquals(expected, actual);
            assertEquals(actual, expected);
            assertEquals(expected.inverse(), actual.inverse());
            assertEquals(expected.hashCode(), actual.hashCode());
            actual.verifyInvariants();
        }
    }

    private static void sameOutcome(Supplier<?> expected, Supplier<?> actual) {
        assertEquals(outcome(expected), outcome(actual));
    }

    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException failure) { return failure.getClass(); }
    }

    private record Collision(int value) {
        @Override public int hashCode() { return 1; }
    }
}
