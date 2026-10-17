/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.commons.collections4.multimap.AbstractMultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedLinkedHashMap;
import org.apache.commons.collections4.multimap.LinkedHashSetValuedLinkedHashMap;
import org.junit.jupiter.api.Test;

/** Independent donor differential contracts; no donor test source is copied. */
final class M3ApacheMultivalueTest {
    private static final long[] SEEDS = {0x41525241594cL, 0x464c414753L, 0x4f4646534554L};

    private record Outcome(Object value, Class<?> failure) { }

    private static Outcome outcome(Supplier<?> operation) {
        try {
            Object value = operation.get();
            if (value instanceof Collection<?> collection) {
                value = new ArrayList<>(collection);
            } else if (value instanceof Map<?, ?> map) {
                value = new LinkedHashMap<>(map);
            }
            return new Outcome(value, null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass());
        }
    }

    private static void sameOutcome(Supplier<?> expected, Supplier<?> actual, String context) {
        assertEquals(outcome(expected), outcome(actual), context);
    }

    private static Integer token(Random random) {
        int value = random.nextInt(17);
        return value == 0 ? null : value - 8;
    }

    private static Object field(Class<?> owner, String name, Object instance) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect exact donor storage seam", failure);
        }
    }

    private static void verifyM3Owners(MultiValuedMap<?, ?> map) {
        M3LinkedHashMap<?, ?> owner = assertInstanceOf(M3LinkedHashMap.class,
                field(AbstractMultiValuedMap.class, "map", map));
        owner.verifyInvariants();
        for (Object values : owner.values()) {
            if (map instanceof ListValuedMap<?, ?>) {
                assertInstanceOf(M3LinkedList.class, values).verifyInvariants();
            } else {
                assertInstanceOf(M3LinkedHashSet.class, values).verifyInvariants();
            }
        }
    }

    private static void sameMultimap(MultiValuedMap<Integer, Integer> expected,
            MultiValuedMap<Integer, Integer> actual, String context) {
        assertEquals(expected.size(), actual.size(), context + " pair count");
        assertEquals(expected.isEmpty(), actual.isEmpty(), context + " empty state");
        assertEquals(expected, actual, context + " donor equality");
        assertEquals(actual, expected, context + " symmetric equality");
        assertEquals(expected.hashCode(), actual.hashCode(), context + " hash");
        assertEquals(expected.asMap(), actual.asMap(), context + " asMap");
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()),
                context + " key order");
        assertEquals(new ArrayList<>(expected.entries()), new ArrayList<>(actual.entries()),
                context + " entry order");
        assertEquals(new ArrayList<>(expected.values()), new ArrayList<>(actual.values()),
                context + " value order");
        assertEquals(expected.keys(), actual.keys(), context + " key multiplicity");
        for (Integer key : Arrays.asList(null, -7, -3, 0, 4, 8)) {
            assertEquals(expected.containsKey(key), actual.containsKey(key), context);
            assertEquals(expected.keys().getCount(key), actual.keys().getCount(key), context);
            assertEquals(expected.get(key), actual.get(key), context);
        }
        verifyM3Owners(actual);
    }

    private static void sameBidi(BidiMap<Integer, Integer> expected,
            BidiMap<Integer, Integer> actual, String context) {
        assertEquals(expected, actual, context + " forward");
        assertEquals(actual, expected, context + " equality symmetry");
        assertEquals(expected.hashCode(), actual.hashCode(), context + " hash");
        assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()),
                context + " forward order");
        assertEquals(expected.inverseBidiMap(), actual.inverseBidiMap(), context + " reverse");
        assertEquals(new ArrayList<>(expected.inverseBidiMap().entrySet()),
                new ArrayList<>(actual.inverseBidiMap().entrySet()), context + " reverse order");
        assertSame(actual, actual.inverseBidiMap().inverseBidiMap(), context + " inverse identity");
        assertEquals(actual.size(), actual.inverseBidiMap().size(), context + " cardinality");
        for (Map.Entry<Integer, Integer> entry : actual.entrySet()) {
            assertTrue(actual.inverseBidiMap().containsKey(entry.getValue()), context);
            assertEquals(entry.getKey(), actual.getKey(entry.getValue()), context);
        }
        assertInstanceOf(M3LinkedHashMap.class,
                field(AbstractDualBidiMap.class, "normalMap", actual)).verifyInvariants();
        assertInstanceOf(M3LinkedHashMap.class,
                field(AbstractDualBidiMap.class, "reverseMap", actual)).verifyInvariants();
    }

    @Test void oracleArtifactIsExactlyThePinnedApacheRelease() throws Exception {
        Properties metadata = new Properties();
        try (InputStream input = MultiValuedMap.class.getResourceAsStream(
                "/META-INF/maven/org.apache.commons/commons-collections4/pom.properties")) {
            assertNotNull(input, "The real donor JAR must supply its Maven identity");
            metadata.load(input);
        }
        assertEquals("org.apache.commons", metadata.getProperty("groupId"));
        assertEquals("commons-collections4", metadata.getProperty("artifactId"));
        assertEquals("4.6.0", metadata.getProperty("version"));
    }

    @Test void nullDuplicatesAndPairCountsMatchEachDonorFamily() {
        var expectedList = new ArrayListValuedLinkedHashMap<Integer, Integer>();
        var actualList = new M3ApacheListValuedMap<Integer, Integer>();
        var expectedSet = new LinkedHashSetValuedLinkedHashMap<Integer, Integer>();
        var actualSet = new M3ApacheSetValuedMap<Integer, Integer>();
        for (Integer value : Arrays.asList(null, 1, 1, 2, null)) {
            assertEquals(expectedList.put(null, value), actualList.put(null, value));
            assertEquals(expectedSet.put(null, value), actualSet.put(null, value));
        }
        assertEquals(5, actualList.size());
        assertEquals(3, actualSet.size());
        assertEquals(1, actualList.keySet().size());
        assertEquals(1, actualSet.keySet().size());
        sameMultimap(expectedList, actualList, "list nulls and duplicates");
        sameMultimap(expectedSet, actualSet, "set nulls and duplicates");
    }

    @Test void missingViewsStayLazyAndReattachAfterRemoval() {
        for (boolean set : List.of(false, true)) {
            MultiValuedMap<Integer, Integer> expected = set
                    ? new LinkedHashSetValuedLinkedHashMap<>() : new ArrayListValuedLinkedHashMap<>();
            MultiValuedMap<Integer, Integer> actual = set
                    ? new M3ApacheSetValuedMap<>() : new M3ApacheListValuedMap<>();
            Collection<Integer> expectedView = expected.get(1);
            Collection<Integer> actualView = actual.get(1);
            assertFalse(actual.containsKey(1));
            assertEquals(expectedView.add(2), actualView.add(2));
            Collection<Integer> expectedRemoved = expected.remove(1);
            Collection<Integer> actualRemoved = actual.remove(1);
            assertTrue(actualView.isEmpty());
            expectedView.add(3);
            actualView.add(3);
            expectedRemoved.add(4);
            actualRemoved.add(4);
            assertEquals(List.of(3), new ArrayList<>(actual.get(1)));
            assertEquals(new ArrayList<>(expectedRemoved), new ArrayList<>(actualRemoved));
            if (set) assertInstanceOf(M3LinkedHashSet.class, actualRemoved);
            else assertInstanceOf(M3LinkedList.class, actualRemoved);
            sameMultimap(expected, actual, "reattachment set=" + set);
        }
    }

    @Test void listIteratorInsertionRemovalAndIllegalStatesMatchDonor() {
        var expected = new ArrayListValuedLinkedHashMap<Integer, Integer>();
        var actual = new M3ApacheListValuedMap<Integer, Integer>();
        ListIterator<Integer> donor = expected.get(1).listIterator();
        ListIterator<Integer> candidate = actual.get(1).listIterator();
        sameOutcome(() -> { donor.remove(); return null; },
                () -> { candidate.remove(); return null; }, "remove before next");
        donor.add(null);
        candidate.add(null);
        assertEquals(donor.previous(), candidate.previous());
        donor.set(4);
        candidate.set(4);
        donor.remove();
        candidate.remove();
        assertFalse(actual.containsKey(1));
        sameMultimap(expected, actual, "list iterator removes last value");
        sameOutcome(() -> expected.get(1).listIterator(1), () -> actual.get(1).listIterator(1),
                "invalid missing-list index");
    }

    @Test void rawSublistEmptyMappingBehaviorStaysVersionCompatible() {
        var expected = new ArrayListValuedLinkedHashMap<Integer, Integer>();
        var actual = new M3ApacheListValuedMap<Integer, Integer>();
        expected.putAll(1, List.of(2, 3));
        actual.putAll(1, List.of(2, 3));
        expected.get(1).subList(0, 2).clear();
        actual.get(1).subList(0, 2).clear();
        assertEquals(0, actual.size());
        assertTrue(actual.containsKey(1), "Apache raw sublist retains the empty key mapping");
        sameMultimap(expected, actual, "raw sublist clears all values");
        sameOutcome(() -> expected.get(2).subList(0, 0).add(3),
                () -> actual.get(2).subList(0, 0).add(3), "missing raw sublist");
        expected.get(1).add(4);
        actual.get(1).add(4);
        sameMultimap(expected, actual, "raw sublist mapping reused");
    }

    @Test void multimapUnsupportedMutationsHaveDonorExceptionsAndNoExtraEffects() {
        var expected = new ArrayListValuedLinkedHashMap<Integer, Integer>();
        var actual = new M3ApacheListValuedMap<Integer, Integer>();
        expected.put(1, 2);
        actual.put(1, 2);
        sameOutcome(() -> expected.asMap().put(3, List.of(4)),
                () -> actual.asMap().put(3, List.of(4)), "asMap put");
        sameOutcome(() -> expected.keys().add(3), () -> actual.keys().add(3), "keys add");
        MapIterator<Integer, Integer> donor = expected.mapIterator();
        MapIterator<Integer, Integer> candidate = actual.mapIterator();
        assertEquals(donor.next(), candidate.next());
        sameOutcome(() -> donor.setValue(9), () -> candidate.setValue(9), "mapIterator setValue");
        sameMultimap(expected, actual, "unsupported views");
    }

    @Test void inversionRetainsM3OwnersAndCreatesAnIndependentMap() {
        var expectedList = new ArrayListValuedLinkedHashMap<Integer, Integer>();
        var actualList = new M3ApacheListValuedMap<Integer, Integer>();
        var expectedSet = new LinkedHashSetValuedLinkedHashMap<Integer, Integer>();
        var actualSet = new M3ApacheSetValuedMap<Integer, Integer>();
        for (Integer key : Arrays.asList(null, 1, 2, 1)) {
            expectedList.put(key, 4); actualList.put(key, 4);
            expectedSet.put(key, 4); actualSet.put(key, 4);
        }
        var listInverse = actualList.inverted();
        var setInverse = actualSet.inverted();
        sameMultimap(expectedList.inverted(), listInverse, "list inversion");
        sameMultimap(expectedSet.inverted(), setInverse, "set inversion");
        listInverse.clear();
        setInverse.clear();
        assertEquals(4, actualList.size());
        assertEquals(3, actualSet.size());
    }

    @Test void bimapDuplicateValueNullAndInverseIdentityMatchApache() {
        var expected = new DualLinkedHashBidiMap<Integer, Integer>();
        var actual = new M3ApacheBidiMap<Integer, Integer>();
        assertEquals(expected.put(1, null), actual.put(1, null));
        assertEquals(expected.put(2, null), actual.put(2, null));
        assertFalse(actual.containsKey(1), "Apache put displaces a prior equal-value key");
        assertEquals(expected.put(null, 3), actual.put(null, 3));
        assertEquals(expected.inverseBidiMap().put(4, 2), actual.inverseBidiMap().put(4, 2));
        sameBidi(expected, actual, "duplicate/null/inverse updates");
        assertSame(field(AbstractDualBidiMap.class, "normalMap", actual),
                field(AbstractDualBidiMap.class, "reverseMap", actual.inverseBidiMap()));
        assertSame(field(AbstractDualBidiMap.class, "reverseMap", actual),
                field(AbstractDualBidiMap.class, "normalMap", actual.inverseBidiMap()));
    }

    @Test void bimapEntryAndMapIteratorRejectDuplicateValuesWithoutIndexCorruption() {
        var expected = new DualLinkedHashBidiMap<Integer, Integer>();
        var actual = new M3ApacheBidiMap<Integer, Integer>();
        expected.put(1, 10); actual.put(1, 10);
        expected.put(2, 20); actual.put(2, 20);
        sameOutcome(() -> expected.entrySet().iterator().next().setValue(20),
                () -> actual.entrySet().iterator().next().setValue(20), "entry duplicate");
        MapIterator<Integer, Integer> donor = expected.mapIterator();
        MapIterator<Integer, Integer> candidate = actual.mapIterator();
        sameOutcome(donor::getKey, candidate::getKey, "iterator before first key");
        assertEquals(donor.next(), candidate.next());
        sameOutcome(() -> donor.setValue(20), () -> candidate.setValue(20), "iterator duplicate");
        assertEquals(donor.setValue(30), candidate.setValue(30));
        donor.remove(); candidate.remove();
        sameOutcome(() -> { donor.remove(); return null; },
                () -> { candidate.remove(); return null; }, "double remove");
        sameBidi(expected, actual, "entry and map iterator updates");
    }

    @Test void bimapIteratorDetectsStructuralChangesLikeTheDonor() {
        var expected = new DualLinkedHashBidiMap<Integer, Integer>();
        var actual = new M3ApacheBidiMap<Integer, Integer>();
        expected.put(1, 10); actual.put(1, 10);
        expected.put(2, 20); actual.put(2, 20);
        Iterator<Integer> donor = expected.keySet().iterator();
        Iterator<Integer> candidate = actual.keySet().iterator();
        assertEquals(donor.next(), candidate.next());
        expected.put(3, 30); actual.put(3, 30);
        assertThrows(ConcurrentModificationException.class, donor::next);
        assertThrows(ConcurrentModificationException.class, candidate::next);
        sameBidi(expected, actual, "fail-fast iteration");
    }

    @Test void listRandomizedMutationsAndViewsMatchThePinnedDonor() {
        for (long seed : SEEDS) {
            var expected = new ArrayListValuedLinkedHashMap<Integer, Integer>();
            var actual = new M3ApacheListValuedMap<Integer, Integer>();
            Random random = new Random(seed);
            for (int step = 0; step < 1200; step++) {
                Integer key = token(random), value = token(random), other = token(random);
                int operation = random.nextInt(20), cursor = random.nextInt(31);
                String context = "list seed=" + seed + " step=" + step + " op=" + operation;
                sameOutcome(() -> mutateList(expected, operation, key, value, other, cursor),
                        () -> mutateList(actual, operation, key, value, other, cursor), context);
                sameMultimap(expected, actual, context);
            }
        }
    }

    @Test void setRandomizedMutationsAndViewsMatchThePinnedDonor() {
        for (long seed : SEEDS) {
            var expected = new LinkedHashSetValuedLinkedHashMap<Integer, Integer>();
            var actual = new M3ApacheSetValuedMap<Integer, Integer>();
            Random random = new Random(seed ^ 0x534554L);
            for (int step = 0; step < 1200; step++) {
                Integer key = token(random), value = token(random), other = token(random);
                int operation = random.nextInt(16), cursor = random.nextInt(31);
                String context = "set seed=" + seed + " step=" + step + " op=" + operation;
                sameOutcome(() -> mutateSet(expected, operation, key, value, other, cursor),
                        () -> mutateSet(actual, operation, key, value, other, cursor), context);
                sameMultimap(expected, actual, context);
            }
        }
    }

    @Test void bimapRandomizedForwardInverseAndViewMutationsMatchThePinnedDonor() {
        for (long seed : SEEDS) {
            var expected = new DualLinkedHashBidiMap<Integer, Integer>();
            var actual = new M3ApacheBidiMap<Integer, Integer>();
            Random random = new Random(seed ^ 0x42494449L);
            for (int step = 0; step < 1600; step++) {
                Integer key = token(random), value = token(random), other = token(random);
                int operation = random.nextInt(19), cursor = random.nextInt(31);
                String context = "bidi seed=" + seed + " step=" + step + " op=" + operation;
                sameOutcome(() -> mutateBidi(expected, operation, key, value, other, cursor),
                        () -> mutateBidi(actual, operation, key, value, other, cursor), context);
                sameBidi(expected, actual, context);
            }
        }
    }

    private static Object removeMappingAt(MultiValuedMap<Integer, Integer> map, int cursor) {
        if (map.size() == 0) return false;
        MapIterator<Integer, Integer> iterator = map.mapIterator();
        for (int index = cursor % map.size(); index >= 0; index--) iterator.next();
        var removed = new SimpleImmutableEntry<>(iterator.getKey(), iterator.getValue());
        iterator.remove();
        return removed;
    }

    private static Object mutateList(ListValuedMap<Integer, Integer> map, int operation,
            Integer key, Integer value, Integer other, int cursor) {
        return switch (operation) {
            case 0, 1 -> map.put(key, value);
            case 2 -> map.removeMapping(key, value);
            case 3 -> map.remove(key);
            case 4 -> map.get(key).add(value);
            case 5 -> map.get(key).remove(value);
            case 6 -> map.get(key).addAll(Arrays.asList(value, other));
            case 7 -> map.get(key).retainAll(Arrays.asList(value, other));
            case 8 -> map.get(key).removeAll(Arrays.asList(value, other));
            case 9 -> map.get(key).set(cursor % (map.get(key).size() + 1), value);
            case 10 -> {
                ListIterator<Integer> iterator = map.get(key).listIterator(cursor % (map.get(key).size() + 1));
                iterator.add(value);
                iterator.previous();
                iterator.set(other);
                if ((cursor & 1) == 0) iterator.remove();
                yield true;
            }
            case 11 -> {
                List<Integer> values = map.get(key);
                List<Integer> sublist = values.subList(cursor % (values.size() + 1), values.size());
                List<Integer> removed = new ArrayList<>(sublist);
                sublist.clear();
                yield removed;
            }
            case 12 -> map.values().remove(value);
            case 13 -> map.entries().remove(new SimpleImmutableEntry<>(key, value));
            case 14 -> map.keySet().remove(key);
            case 15 -> removeMappingAt(map, cursor);
            case 16 -> map.asMap().remove(key);
            case 17 -> map.keys().remove(key);
            case 18 -> map.putAll(key, Arrays.asList(value, other));
            default -> {
                if (cursor == 0) { map.clear(); yield null; }
                yield map.put(key, value);
            }
        };
    }

    private static Object mutateSet(SetValuedMap<Integer, Integer> map, int operation,
            Integer key, Integer value, Integer other, int cursor) {
        return switch (operation) {
            case 0, 1 -> map.put(key, value);
            case 2 -> map.removeMapping(key, value);
            case 3 -> map.remove(key);
            case 4 -> map.get(key).add(value);
            case 5 -> map.get(key).remove(value);
            case 6 -> map.get(key).addAll(Arrays.asList(value, other));
            case 7 -> map.get(key).retainAll(Arrays.asList(value, other));
            case 8 -> map.get(key).removeAll(Arrays.asList(value, other));
            case 9 -> map.values().remove(value);
            case 10 -> map.entries().remove(new SimpleImmutableEntry<>(key, value));
            case 11 -> map.keySet().remove(key);
            case 12 -> removeMappingAt(map, cursor);
            case 13 -> map.asMap().remove(key);
            case 14 -> map.putAll(key, Arrays.asList(value, other));
            default -> {
                if (cursor == 0) { map.clear(); yield null; }
                yield map.values().removeAll(Arrays.asList(value, other));
            }
        };
    }

    private static Object mutateBidi(BidiMap<Integer, Integer> map, int operation,
            Integer key, Integer value, Integer other, int cursor) {
        return switch (operation) {
            case 0, 1 -> map.put(key, value);
            case 2 -> map.remove(key);
            case 3 -> map.removeValue(value);
            case 4 -> map.inverseBidiMap().put(value, key);
            case 5 -> map.keySet().remove(key);
            case 6 -> map.values().remove(value);
            case 7 -> map.entrySet().remove(new SimpleImmutableEntry<>(key, value));
            case 8 -> {
                if (map.isEmpty()) yield false;
                Iterator<Map.Entry<Integer, Integer>> iterator = map.entrySet().iterator();
                Map.Entry<Integer, Integer> entry = iterator.next();
                for (int index = cursor % map.size(); index > 0; index--) entry = iterator.next();
                yield entry.setValue(value);
            }
            case 9, 10 -> {
                if (map.isEmpty()) yield false;
                MapIterator<Integer, Integer> iterator = map.mapIterator();
                for (int index = cursor % map.size(); index >= 0; index--) iterator.next();
                if (operation == 10) yield iterator.setValue(value);
                var removed = new SimpleImmutableEntry<>(iterator.getKey(), iterator.getValue());
                iterator.remove();
                yield removed;
            }
            case 11 -> map.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), value));
            case 12 -> map.keySet().retainAll(Arrays.asList(key, other));
            case 13 -> map.values().removeAll(Arrays.asList(value, other));
            case 14 -> map.putIfAbsent(key, value);
            case 15 -> map.compute(key, (ignored, previous) -> value);
            case 16 -> map.merge(key, value == null ? 1 : value, (previous, next) -> other);
            case 17 -> map.inverseBidiMap().values().remove(key);
            default -> {
                if (cursor == 0) { map.clear(); yield null; }
                yield map.replace(key, value);
            }
        };
    }
}
