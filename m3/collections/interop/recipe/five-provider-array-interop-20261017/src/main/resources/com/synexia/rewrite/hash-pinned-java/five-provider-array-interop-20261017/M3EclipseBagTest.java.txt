/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Differential oracle is the pinned Eclipse Collections 11.1.0 binary, never a local replacement. */
final class M3EclipseBagTest {
    @Test void occurrenceReturnValuesAndNullsMatchPinnedProvider() {
        M3EclipseBag<String> actual = new M3EclipseBag<>();
        MutableBag<String> expected = HashBag.newBag();
        assertEquals(expected.addOccurrences("a", 3), actual.addOccurrences("a", 3));
        assertEquals(expected.addOccurrences("a", 2), actual.addOccurrences("a", 2));
        assertEquals(5, actual.addOccurrences("a", 0));
        assertEquals(expected.add(null), actual.add(null));
        assertEquals(expected.addOccurrences(null, 4), actual.addOccurrences(null, 4));
        assertEquals(expected.removeOccurrences("a", 0), actual.removeOccurrences("a", 0));
        assertEquals(expected.remove("a"), actual.remove("a"));
        assertEquals(expected.removeOccurrences("a", 2), actual.removeOccurrences("a", 2));
        assertEquals(expected.removeOccurrences("a", 99), actual.removeOccurrences("a", 99));
        assertEquals(expected.remove("absent"), actual.remove("absent"));
        assertEquals(expected.setOccurrences(null, 3), actual.setOccurrences(null, 3));
        assertEquals(expected.setOccurrences(null, 3), actual.setOccurrences(null, 3));
        assertEquals(expected.setOccurrences("absent", 0), actual.setOccurrences("absent", 0));
        assertSameBag(expected, actual);
        assertEquals(expected.setOccurrences(null, 0), actual.setOccurrences(null, 0));
        assertSameBag(expected, actual);
    }

    @Test void negativeAndZeroCountsRejectOrPreserveExactly() {
        M3EclipseBag<String> actual = bag("a", "a", null);
        MutableBag<String> expected = HashBag.newBagWith("a", "a", null);
        for (String key : Arrays.asList("a", "absent", null)) {
            assertEquals(failure(() -> expected.addOccurrences(key, -1)),
                    failure(() -> actual.addOccurrences(key, -1)));
            assertEquals(failure(() -> expected.removeOccurrences(key, -1)),
                    failure(() -> actual.removeOccurrences(key, -1)));
            assertEquals(failure(() -> expected.setOccurrences(key, -1)),
                    failure(() -> actual.setOccurrences(key, -1)));
            assertEquals(expected.addOccurrences(key, 0), actual.addOccurrences(key, 0));
            assertEquals(expected.removeOccurrences(key, 0), actual.removeOccurrences(key, 0));
        }
        assertSameBag(expected, actual);
        assertThrows(NullPointerException.class, () -> new M3EclipseBag<String>(null));
    }

    @Test void randomizedCollisionTracesMatchPinnedHashBag() {
        for (long seed : new long[] {11L, 73L, 20261017L, 0x5eedL}) {
            Random random = new Random(seed);
            List<Object> universe = new ArrayList<>();
            universe.add(null);
            for (int index = 0; index < 17; index++) universe.add(new Collision(index));
            MutableBag<Object> expected = HashBag.newBag();
            M3EclipseBag<Object> actual = new M3EclipseBag<>();
            for (int step = 0; step < 1800; step++) {
                Object key = universe.get(random.nextInt(universe.size()));
                int number = random.nextInt(7);
                List<Object> source = new ArrayList<>();
                for (int index = 0; index < number; index++)
                    source.add(universe.get(random.nextInt(universe.size())));
                switch (random.nextInt(11)) {
                    case 0 -> assertEquals(expected.add(key), actual.add(key));
                    case 1 -> assertEquals(expected.remove(key), actual.remove(key));
                    case 2 -> assertEquals(expected.addOccurrences(key, number), actual.addOccurrences(key, number));
                    case 3 -> assertEquals(expected.removeOccurrences(key, number), actual.removeOccurrences(key, number));
                    case 4 -> assertEquals(expected.setOccurrences(key, number), actual.setOccurrences(key, number));
                    case 5 -> assertEquals(expected.addAllIterable(source), actual.addAllIterable(source));
                    case 6 -> assertEquals(expected.removeAllIterable(source), actual.removeAllIterable(source));
                    case 7 -> assertEquals(expected.retainAllIterable(source), actual.retainAllIterable(source));
                    case 8 -> assertEquals(expected.removeIfWith(M3EclipseBagTest::matches, number),
                            actual.removeIfWith(M3EclipseBagTest::matches, number));
                    case 9 -> {
                        MutableBag<Object> sourceBag = HashBag.newBag(source);
                        assertEquals(expected.addAllIterable(sourceBag), actual.addAllIterable(sourceBag));
                    }
                    case 10 -> { if (number == 0) { expected.clear(); actual.clear(); } }
                    default -> throw new AssertionError();
                }
                assertSameBag(expected, actual);
            }
        }
    }

    @Test void richObjectBagTransformsKeepM3OwnersAndProviderSemantics() {
        M3EclipseBag<String> actual = bag("red", "red", "blue", "r", null, null);
        MutableBag<String> expected = HashBag.newBag(actual);
        Function<String, Integer> length = value -> value == null ? 0 : value.length();
        assertM3Result(expected.select(value -> value == null || value.length() > 1),
                actual.select(value -> value == null || value.length() > 1));
        assertM3Result(expected.reject(value -> value == null || value.length() > 1),
                actual.reject(value -> value == null || value.length() > 1));
        assertM3Result(expected.selectWith((value, size) -> value != null && value.length() == size, 3),
                actual.selectWith((value, size) -> value != null && value.length() == size, 3));
        assertM3Result(expected.rejectWith((value, size) -> value != null && value.length() == size, 3),
                actual.rejectWith((value, size) -> value != null && value.length() == size, 3));
        assertM3Result(expected.selectByOccurrences(count -> count > 1), actual.selectByOccurrences(count -> count > 1));
        assertM3Result(expected.collect(length), actual.collect(length));
        assertM3Result(expected.collectWith((value, extra) -> length.valueOf(value) + extra, 2),
                actual.collectWith((value, extra) -> length.valueOf(value) + extra, 2));
        assertM3Result(expected.collectIf(value -> value != null, String::length),
                actual.collectIf(value -> value != null, String::length));
        assertM3Result(expected.collectWithOccurrences((value, count) -> String.valueOf(value) + count),
                actual.collectWithOccurrences((value, count) -> String.valueOf(value) + count));
        assertM3Result(expected.flatCollect(value -> Arrays.asList(value, value)),
                actual.flatCollect(value -> Arrays.asList(value, value)));
        assertM3Result(expected.countBy(length), actual.countBy(length));
        assertM3Result(expected.countByWith((value, extra) -> length.valueOf(value) + extra, 1),
                actual.countByWith((value, extra) -> length.valueOf(value) + extra, 1));
        assertM3Result(expected.countByEach(value -> Arrays.asList(value, value)),
                actual.countByEach(value -> Arrays.asList(value, value)));
        assertM3Result(expected.toBag(), actual.toBag());
        assertM3Result(HashBag.newBag(), actual.newEmpty());
        M3EclipseBag<Object> mixed = new M3EclipseBag<>();
        mixed.addAllIterable(Arrays.asList("a", "a", 1, null));
        assertM3Result(HashBag.newBag(mixed).selectInstancesOf(String.class), mixed.selectInstancesOf(String.class));
        actual.toBag().clear();
        assertSameBag(expected, actual);
    }

    @Test void bagAlgorithmsInvokeFunctionsOncePerDistinctItem() {
        M3EclipseBag<String> actual = bag("a", "a", "a", "b", "b", null);
        AtomicInteger calls = new AtomicInteger();
        Map<String, Integer> visited = new HashMap<>();
        actual.forEachWithOccurrences((item, count) -> { calls.incrementAndGet(); visited.put(item, count); });
        assertEquals(3, calls.get());
        assertEquals(actual.toMapOfItemToCount(), visited);
        calls.set(0);
        actual.each(item -> calls.incrementAndGet());
        assertEquals(6, calls.get());
        calls.set(0);
        assertM3Result(actual, actual.collect(item -> { calls.incrementAndGet(); return item; }));
        assertEquals(3, calls.get());
        calls.set(0);
        assertM3Result(actual, actual.select(item -> { calls.incrementAndGet(); return true; }));
        assertEquals(3, calls.get());
        calls.set(0);
        assertTrue(actual.removeIf((Predicate<String>) item -> { calls.incrementAndGet(); return true; }));
        assertEquals(3, calls.get());
        assertTrue(actual.isEmpty());
        actual.verifyInvariants();
    }

    @Test void occurrencePredicatesMatchPinnedProvider() {
        M3EclipseBag<String> actual = bag("a", "a", "a", "b", "b", null);
        MutableBag<String> expected = HashBag.newBag(actual);
        for (int count = 0; count < 5; count++) {
            int threshold = count;
            assertEquals(expected.anySatisfyWithOccurrences((item, n) -> n > threshold),
                    actual.anySatisfyWithOccurrences((item, n) -> n > threshold));
            assertEquals(expected.allSatisfyWithOccurrences((item, n) -> n > threshold),
                    actual.allSatisfyWithOccurrences((item, n) -> n > threshold));
            assertEquals(expected.noneSatisfyWithOccurrences((item, n) -> n > threshold),
                    actual.noneSatisfyWithOccurrences((item, n) -> n > threshold));
        }
        assertEquals(expected.detectWithOccurrences((item, count) -> count == 3),
                actual.detectWithOccurrences((item, count) -> count == 3));
        assertNull(actual.detectWithOccurrences((item, count) -> count == 99));
        assertNull(actual.detectWithOccurrences((item, count) -> item == null));
        assertThrows(ConcurrentModificationException.class,
                () -> actual.anySatisfyWithOccurrences((item, count) -> { actual.add(item); return true; }));
        actual.verifyInvariants();
    }

    @Test void bulkRemovalUsesMembershipAndHandlesAliasedEclipseViews() {
        M3EclipseBag<String> actual = bag("a", "a", "a", "b", "b", null);
        MutableBag<String> expected = HashBag.newBag(actual);
        assertEquals(expected.retainAllIterable(Arrays.asList("a", null)),
                actual.retainAllIterable(Arrays.asList("a", null)));
        assertEquals(3, actual.occurrencesOf("a"));
        assertEquals(expected.addAllIterable(expected), actual.addAllIterable(actual));
        assertSameBag(expected, actual);
        assertEquals(expected.addAllIterable(expected.asUnmodifiable()),
                actual.addAllIterable(actual.asUnmodifiable()));
        assertSameBag(expected, actual);
        assertFalse(actual.retainAllIterable(actual.asUnmodifiable()));
        assertEquals(expected.removeAllIterable(HashBag.newBagWith("a")),
                actual.removeAllIterable(HashBag.newBagWith("a")));
        assertEquals(0, actual.occurrencesOf("a"));
        assertSameBag(expected, actual);
        assertEquals(expected.removeAllIterable(expected.asUnmodifiable()),
                actual.removeAllIterable(actual.asUnmodifiable()));
        assertSameBag(expected, actual);
        assertFalse(actual.addAllIterable(actual));
        assertFalse(actual.removeAllIterable(actual));
    }

    @Test void collectionBulkMethodsDispatchToCountAwareHooks() {
        M3EclipseBag<String> bag = bag("a", "a", "a", "b", "b", null);
        Collection<String> collection = bag;
        assertTrue(collection.removeAll(List.of("a")));
        assertEquals(0, bag.occurrencesOf("a"));
        assertTrue(collection.retainAll(List.of("b")));
        assertEquals(2, bag.occurrencesOf("b"));
        assertTrue(collection.addAll(List.of("b", "b")));
        assertEquals(4, bag.occurrencesOf("b"));
        bag.verifyInvariants();
    }

    @Test void javaPredicateRemovalRemovesOnlyEachAcceptedOccurrence() {
        M3EclipseBag<String> actual = bag("a", "a", "a", "a");
        AtomicInteger calls = new AtomicInteger();
        java.util.function.Predicate<String> predicate = ignored -> calls.incrementAndGet() == 4;
        assertTrue(((Collection<String>) actual).removeIf(predicate));
        assertEquals(4, calls.get());
        assertEquals(3, actual.size());
        assertEquals(3, actual.occurrencesOf("a"));
        actual.verifyInvariants();
    }

    @Test void iteratorRemovesOneOccurrenceAndHasStandardStateChecks() {
        M3EclipseBag<String> actual = bag("a", "a", "a", "b", "b", null);
        Iterator<String> iterator = actual.iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        while (iterator.hasNext()) {
            String key = iterator.next();
            int previous = actual.occurrencesOf(key);
            iterator.remove();
            assertEquals(previous - 1, actual.occurrencesOf(key));
            assertThrows(IllegalStateException.class, iterator::remove);
            actual.verifyInvariants();
        }
        assertTrue(actual.isEmpty());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test void repairsPinnedDonorLastOccurrenceRemovalCounterexample() {
        MutableBag<String> donor = HashBag.newBagWith("a", "a", "a");
        Iterator<String> donorIterator = donor.iterator();
        donorIterator.next(); donorIterator.next(); donorIterator.next(); donorIterator.remove();
        // Exact 11.1.0 donor counterexample: size and the retained count disagree.
        assertEquals(2, donor.size());
        assertEquals(0, donor.occurrencesOf("a"));
        assertEquals(0, donor.sizeDistinct());
        M3EclipseBag<String> actual = bag("a", "a", "a");
        Iterator<String> iterator = actual.iterator();
        iterator.next(); iterator.next(); iterator.next(); iterator.remove();
        assertEquals(2, actual.size());
        assertEquals(2, actual.occurrencesOf("a"));
        assertEquals(1, actual.sizeDistinct());
        assertFalse(iterator.hasNext());
        assertEquals(List.of("a", "a"), actual.toList());
        actual.verifyInvariants();
    }

    @Test void unmodifiableAndSynchronizedWrappersStayLive() {
        M3EclipseBag<String> actual = bag("a", "a", null);
        MutableBag<String> readOnly = actual.asUnmodifiable();
        MutableBag<String> synchronizedBag = actual.asSynchronized();
        RichIterable<String> distinct = actual.distinctView();
        Set<?> distinctSet = assertInstanceOf(Set.class, distinct);
        assertEquals(2, distinct.size());
        synchronizedBag.addOccurrences("b", 3);
        assertEquals(3, readOnly.occurrencesOf("b"));
        assertTrue(distinct.contains("b"));
        assertThrows(UnsupportedOperationException.class, () -> readOnly.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> readOnly.removeOccurrences("a", 1));
        assertThrows(UnsupportedOperationException.class, () -> readOnly.setOccurrences("a", 0));
        assertThrows(UnsupportedOperationException.class, () -> distinctSet.remove("a"));
        Iterator<String> readOnlyIterator = readOnly.iterator();
        readOnlyIterator.next();
        assertThrows(UnsupportedOperationException.class, readOnlyIterator::remove);
        Iterator<?> distinctIterator = distinctSet.iterator();
        distinctIterator.next();
        assertThrows(UnsupportedOperationException.class, distinctIterator::remove);
        assertThrows(UnsupportedOperationException.class, readOnly::clear);
        synchronizedBag.remove("a");
        assertEquals(1, readOnly.occurrencesOf("a"));
        actual.clear();
        assertTrue(readOnly.isEmpty());
        assertTrue(distinct.isEmpty());
        assertTrue(synchronizedBag.isEmpty());
        actual.verifyInvariants();
    }

    @Test void explicitOwnerSharesCountsAndFlagsWithGuavaWithoutInvalidatingFlagOnlyIteration() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        M3EclipseBag<String> eclipse = new M3EclipseBag<>(owner);
        M3GuavaMultiset<String> guava = new M3GuavaMultiset<>(owner);
        assertEquals(3, eclipse.addOccurrences("red", 3));
        assertEquals(3, guava.count("red"));
        Iterator<String> iterator = eclipse.iterator();
        owner.setFlags("red", 0x80000001);
        assertEquals("red", iterator.next());
        owner.setFlags("red", 1);
        iterator.remove();
        assertEquals(2, guava.count("red"));
        assertEquals(1, owner.flags("red"));
        guava.add("blue", 2);
        owner.setFlags("blue", 2);
        assertEquals(List.of("red"), owner.flaggedElements(1, 2, M3IndexPathMasks.Backend.JAVA));
        assertEquals(2, eclipse.occurrencesOf("blue"));
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertEquals(2, guava.remove("red", 2));
        assertEquals(0, owner.flags("red"));
        eclipse.addOccurrences("reused", 2);
        assertEquals(0, owner.flags("reused"));
        eclipse.verifyInvariants();
        guava.verifyInvariants();
    }

    @Test void countOnlyChangesFromAnotherFacadeInvalidateOccurrenceIterators() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        M3EclipseBag<String> first = new M3EclipseBag<>(owner);
        M3EclipseBag<String> second = new M3EclipseBag<>(owner);
        first.addOccurrences("a", 3);
        Iterator<String> iterator = first.iterator();
        assertEquals("a", iterator.next());
        second.add("a");
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertThrows(ConcurrentModificationException.class, iterator::remove);
        Iterator<String> noChange = first.iterator();
        assertFalse(second.setOccurrences("a", 4));
        second.addOccurrences("a", 0);
        assertEquals("a", noChange.next());
        first.verifyInvariants();
    }

    @Test void overflowRejectsBeforeMutationAndLargeTotalsStayExact() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        M3EclipseBag<String> actual = new M3EclipseBag<>(owner);
        assertEquals(Integer.MAX_VALUE, actual.addOccurrences("a", Integer.MAX_VALUE));
        owner.setFlags("a", 7);
        assertThrows(IllegalArgumentException.class, () -> actual.add("a"));
        assertThrows(IllegalArgumentException.class, () -> actual.addOccurrences("a", 1));
        assertEquals(Integer.MAX_VALUE, actual.occurrencesOf("a"));
        assertEquals(7, owner.flags("a"));
        actual.addOccurrences("b", Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, owner.totalCount());
        assertEquals(Integer.MAX_VALUE, actual.size());
        assertTrue(actual.removeAllIterable(List.of("a")));
        assertEquals(Integer.MAX_VALUE, actual.size());
        assertEquals((long) Integer.MAX_VALUE, owner.totalCount());
        assertTrue(actual.addAllIterable(List.of("c")));
        assertTrue(actual.removeIf((Predicate<String>) "c"::equals));
        assertEquals(Integer.MAX_VALUE, actual.size());
        actual.verifyInvariants();
        actual.clear();
        assertEquals(0L, owner.totalCount());
        actual.verifyInvariants();
    }

    @Test void pinnedDonorOverflowIsRecordedAsAnExcludedCorruptState() {
        MutableBag<String> donor = HashBag.newBag();
        donor.addOccurrences("a", Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, donor.addOccurrences("a", 1));
        assertEquals(Integer.MIN_VALUE, donor.size());
        assertEquals(Integer.MIN_VALUE, donor.occurrencesOf("a"));
    }

    @Test void emptyAndNonemptyNullCallbackBehaviorMatchesPinnedProvider() {
        M3EclipseBag<String> actual = new M3EclipseBag<>();
        MutableBag<String> expected = HashBag.newBag();
        for (int pass = 0; pass < 2; pass++) {
            assertEquals(failure(() -> expected.addAllIterable(null)), failure(() -> actual.addAllIterable(null)));
            assertEquals(failure(() -> expected.removeAllIterable(null)), failure(() -> actual.removeAllIterable(null)));
            assertEquals(failure(() -> expected.retainAllIterable(null)), failure(() -> actual.retainAllIterable(null)));
            assertEquals(failure(() -> expected.each(null)), failure(() -> actual.each(null)));
            assertEquals(failure(() -> expected.forEachWithOccurrences(null)),
                    failure(() -> actual.forEachWithOccurrences(null)));
            assertEquals(failure(() -> expected.selectByOccurrences(null)), failure(() -> actual.selectByOccurrences(null)));
            assertEquals(failure(() -> expected.collect((Function<String, String>) null)),
                    failure(() -> actual.collect((Function<String, String>) null)));
            assertEquals(failure(() -> expected.anySatisfyWithOccurrences(null)),
                    failure(() -> actual.anySatisfyWithOccurrences(null)));
            assertEquals(failure(() -> expected.allSatisfyWithOccurrences(null)),
                    failure(() -> actual.allSatisfyWithOccurrences(null)));
            assertEquals(failure(() -> expected.noneSatisfyWithOccurrences(null)),
                    failure(() -> actual.noneSatisfyWithOccurrences(null)));
            assertEquals(failure(() -> expected.detectWithOccurrences(null)),
                    failure(() -> actual.detectWithOccurrences(null)));
            assertEquals(failure(() -> expected.removeIf((Predicate<String>) null)),
                    failure(() -> actual.removeIf((Predicate<String>) null)));
            expected.add("a"); actual.add("a");
        }
        assertSameBag(expected, actual);
    }

    @Test void exportedItemCountMapAndBagCopiesAreDetached() {
        M3EclipseBag<String> actual = bag("a", "a", "b", null);
        MutableMap<String, Integer> map = actual.toMapOfItemToCount();
        assertEquals(2, map.get("a"));
        assertEquals(1, map.get(null));
        map.put("a", 9);
        map.remove(null);
        assertEquals(2, actual.occurrencesOf("a"));
        assertEquals(1, actual.occurrencesOf(null));
        actual.addOccurrences("c", 3);
        assertFalse(map.containsKey("c"));
        assertSameBag(HashBag.newBag(actual), actual.toBag());
        actual.verifyInvariants();
    }

    @Test void inheritedDetachedResultFamiliesMatchPinnedProvider() {
        M3EclipseBag<String> actual = bag("a", "a", "bb", "ccc", null);
        MutableBag<String> expected = HashBag.newBag(actual);
        Function<String, Integer> length = value -> value == null ? 0 : value.length();
        assertEquals(expected.groupBy(length), actual.groupBy(length));
        assertEquals(expected.groupByEach(value -> List.of(length.valueOf(value), 99)),
                actual.groupByEach(value -> List.of(length.valueOf(value), 99)));
        assertEquals(expected.partition(value -> value == null || value.length() > 1).getSelected(),
                actual.partition(value -> value == null || value.length() > 1).getSelected());
        assertEquals(expected.partitionWith((value, n) -> value == null || value.length() > n, 1).getRejected(),
                actual.partitionWith((value, n) -> value == null || value.length() > n, 1).getRejected());
        assertEquals(expected.collectInt(length::valueOf), actual.collectInt(length::valueOf));
        ImmutableBag<String> immutable = actual.toImmutable();
        assertEquals(expected.toImmutable(), immutable);
        actual.add("later");
        assertEquals(0, immutable.occurrencesOf("later"));
        actual.verifyInvariants();
    }

    @Test void equalityIsTheEclipseBagContractAndIgnoresEncounterOrder() {
        M3EclipseBag<String> first = bag("a", "a", "b", null);
        M3EclipseBag<String> second = bag(null, "b", "a", "a");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, HashBag.newBag(second));
        assertNotEquals(first, Arrays.asList("a", "a", "b", null));
        assertNotEquals(first, new HashSet<>(Arrays.asList("a", "b", null)));
        second.remove("a");
        assertNotEquals(first, second);
        assertNotEquals(first, null);
        assertEquals(first, first);
        first.verifyInvariants();
    }

    private static boolean matches(Object item, Integer threshold) {
        return item == null ? threshold == 0 : ((Collision) item).id() % 7 == threshold;
    }

    private static M3EclipseBag<String> bag(String... values) {
        M3EclipseBag<String> result = new M3EclipseBag<>();
        result.addAllIterable(Arrays.asList(values));
        return result;
    }

    private static Class<? extends RuntimeException> failure(Runnable action) {
        try { action.run(); return null; }
        catch (RuntimeException exception) { return exception.getClass(); }
    }

    private static void assertM3Result(Bag<?> expected, Bag<?> actual) {
        M3EclipseBag<?> m3 = assertInstanceOf(M3EclipseBag.class, actual);
        assertSameBag(expected, m3);
    }

    private static void assertSameBag(Bag<?> expected, M3EclipseBag<?> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.sizeDistinct(), actual.sizeDistinct());
        assertEquals(expected.toMapOfItemToCount(), actual.toMapOfItemToCount());
        assertEquals(expected, actual);
        assertEquals(actual, expected);
        assertEquals(expected.hashCode(), actual.hashCode());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        Map<Object, Integer> iterated = new HashMap<>();
        for (Object item : actual) iterated.merge(item, 1, Integer::sum);
        assertEquals(expected.toMapOfItemToCount(), iterated);
        actual.verifyInvariants();
    }

    private record Collision(int id) {
        @Override public int hashCode() { return 1; }
    }
}
