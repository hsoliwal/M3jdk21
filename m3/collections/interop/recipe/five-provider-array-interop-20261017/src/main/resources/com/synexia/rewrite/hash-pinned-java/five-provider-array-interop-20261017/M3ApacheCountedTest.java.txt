/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.junit.jupiter.api.Test;

/** Oracle tests deliberately name the deprecated Bag only in its compatibility lane. */
@SuppressWarnings("deprecation")
final class M3ApacheCountedTest {
    private record Outcome(Object value, Class<?> failure) { }

    private record CountEntry<E>(E getElement, int getCount) implements MultiSet.Entry<E> {
        @Override public boolean equals(Object other) {
            return other instanceof MultiSet.Entry<?> entry && getCount == entry.getCount()
                    && Objects.equals(getElement, entry.getElement());
        }
        @Override public int hashCode() { return Objects.hashCode(getElement) ^ getCount; }
    }

    private static Outcome outcome(Supplier<?> operation) {
        try { return new Outcome(operation.get(), null); }
        catch (RuntimeException failure) { return new Outcome(null, failure.getClass()); }
    }

    private static void sameOutcome(Supplier<?> expected, Supplier<?> actual, String context) {
        assertEquals(outcome(expected), outcome(actual), context);
    }

    private static Integer token(Random random) {
        int value = random.nextInt(13);
        return value == 0 ? null : value - 6;
    }

    private static Map<Object, Integer> occurrences(Collection<?> values) {
        Map<Object, Integer> counts = new HashMap<>();
        for (Object value : values) counts.merge(value, 1, Integer::sum);
        return counts;
    }

    private static void sameMultiSet(MultiSet<Integer> expected, M3ApacheMultiSet<Integer> actual,
            M3ObjectCounts<Integer> owner, String context) {
        assertEquals(expected.size(), actual.size(), context + " size");
        assertEquals(expected.isEmpty(), actual.isEmpty(), context + " empty");
        assertEquals(expected, actual, context + " donor equality");
        assertEquals(actual, expected, context + " symmetric equality");
        assertEquals(expected.hashCode(), actual.hashCode(), context + " hash");
        assertEquals(expected.uniqueSet(), actual.uniqueSet(), context + " unique elements");
        assertEquals(expected.entrySet(), actual.entrySet(), context + " entries");
        assertEquals(occurrences(expected), occurrences(actual), context + " iteration");
        assertEquals(occurrences(Arrays.asList(expected.toArray())),
                occurrences(Arrays.asList(actual.toArray())), context + " arrays");
        assertEquals(expected.size(), owner.totalCount(), context + " primitive total");
        for (Integer value : Arrays.asList(null, -5, -1, 0, 3, 6)) {
            assertEquals(expected.getCount(value), actual.getCount(value), context);
            assertEquals(expected.contains(value), actual.contains(value), context);
        }
        owner.verifyInvariants();
    }

    private static void sameBag(Bag<Integer> expected, M3ApacheBag<Integer> actual,
            M3ObjectCounts<Integer> owner, String context) {
        assertEquals(expected.size(), actual.size(), context + " size");
        assertEquals(expected.isEmpty(), actual.isEmpty(), context + " empty");
        assertEquals(expected, actual, context + " donor equality");
        assertEquals(actual, expected, context + " symmetric equality");
        assertEquals(expected.hashCode(), actual.hashCode(), context + " hash");
        assertEquals(expected.uniqueSet(), actual.uniqueSet(), context + " unique elements");
        assertEquals(occurrences(expected), occurrences(actual), context + " iteration");
        assertEquals(occurrences(Arrays.asList(expected.toArray())),
                occurrences(Arrays.asList(actual.toArray())), context + " arrays");
        assertEquals(expected.size(), owner.totalCount(), context + " primitive total");
        for (Integer value : Arrays.asList(null, -5, -1, 0, 3, 6)) {
            assertEquals(expected.getCount(value), actual.getCount(value), context);
            assertEquals(expected.contains(value), actual.contains(value), context);
        }
        owner.verifyInvariants();
    }

    @Test void providerReturnValuesAndRemovalCountsRemainDistinct() {
        var multi = new M3ApacheMultiSet<String>();
        var bag = new M3ApacheBag<String>();
        assertTrue(multi.add("x"));
        assertTrue(multi.add("x"));
        assertTrue(bag.add("x"));
        assertFalse(bag.add("x"));
        assertEquals(2, multi.add("x", 3));
        assertFalse(bag.add("x", 3));
        assertEquals(5, multi.getCount("x"));
        assertEquals(5, bag.getCount("x"));
        assertTrue(multi.remove("x"));
        assertTrue(bag.remove("x"));
        assertEquals(4, multi.getCount("x"));
        assertEquals(0, bag.getCount("x"));
        assertFalse(multi.equals(bag));
        assertFalse(bag.equals(multi));
    }

    @Test void explicitOwnerIsSharedAcrossFacesAndPreservesFlagsUntilDeletion() {
        var owner = new M3ObjectCounts<String>();
        var multi = new M3ApacheMultiSet<>(owner);
        var bag = new M3ApacheBag<>(owner);
        assertEquals(0, multi.add("a", 2));
        owner.setFlags("a", 0x80000005);
        assertFalse(bag.add("a", 3));
        assertEquals(5, multi.getCount("a"));
        assertEquals(5L, owner.totalCount());
        assertEquals(0x80000005, owner.flags("a"));
        assertEquals(List.of("a"), owner.flaggedElements(0x80000001, 2,
                M3IndexPathMasks.Backend.JAVA));
        assertEquals(5, multi.setCount("a", 1));
        assertEquals(1, bag.getCount("a"));
        assertTrue(bag.remove("a"));
        assertEquals(0, owner.flags("a"));
        multi.add("a");
        assertEquals(0, owner.flags("a"));
        owner.verifyInvariants();
        assertThrows(NullPointerException.class, () -> new M3ApacheMultiSet<>(null));
        assertThrows(NullPointerException.class, () -> new M3ApacheBag<>(null));
    }

    @Test void sharedCountChangesInvalidateIteratorsWhileFlagChangesDoNot() {
        var owner = new M3ObjectCounts<Integer>();
        var multi = new M3ApacheMultiSet<>(owner);
        var bag = new M3ApacheBag<>(owner);
        multi.add(1, 3);
        Iterator<Integer> iterator = multi.iterator();
        assertEquals(1, iterator.next());
        owner.setFlags(1, 7);
        assertEquals(1, iterator.next());
        bag.add(1);
        assertThrows(ConcurrentModificationException.class, iterator::next);
        owner.verifyInvariants();
    }

    @Test void apachePerElementSaturationRetainsExactLongTotal() {
        var expected = new HashMultiSet<String>();
        var owner = new M3ObjectCounts<String>();
        var actual = new M3ApacheMultiSet<>(owner);
        assertEquals(expected.add("a", Integer.MAX_VALUE), actual.add("a", Integer.MAX_VALUE));
        assertEquals(expected.add("a", 17), actual.add("a", 17));
        assertEquals(expected.add("b", 2), actual.add("b", 2));
        assertEquals(Integer.MAX_VALUE, actual.getCount("a"));
        assertEquals(Integer.MAX_VALUE, actual.size());
        assertEquals((long) Integer.MAX_VALUE + 2, owner.totalCount());
        assertEquals(expected.remove("a", Integer.MAX_VALUE), actual.remove("a", Integer.MAX_VALUE));
        assertEquals(2, actual.size());
        assertEquals(expected, actual);
        owner.verifyInvariants();
    }

    @Test void legacyBagSaturationMatchesDonorAndReleasesExactCounts() {
        var expected = new HashBag<String>();
        var owner = new M3ObjectCounts<String>();
        var actual = new M3ApacheBag<>(owner);
        assertEquals(expected.add("a", Integer.MAX_VALUE), actual.add("a", Integer.MAX_VALUE));
        assertEquals(expected.add("a", 100), actual.add("a", 100));
        assertEquals(expected.add(null, 4), actual.add(null, 4));
        assertEquals((long) Integer.MAX_VALUE + 4, owner.totalCount());
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.remove("a"), actual.remove("a"));
        assertEquals(4, actual.size());
        assertEquals(expected, actual);
        owner.verifyInvariants();
    }

    @Test void saturatedPositiveAdditionStillInvalidatesDonorOccurrenceIterator() {
        var expected = new HashMultiSet<Integer>();
        var actual = new M3ApacheMultiSet<Integer>();
        expected.add(1, Integer.MAX_VALUE); actual.add(1, Integer.MAX_VALUE);
        Iterator<Integer> donor = expected.iterator();
        Iterator<Integer> candidate = actual.iterator();
        assertEquals(donor.next(), candidate.next());
        expected.add(1, 1); actual.add(1, 1);
        assertThrows(ConcurrentModificationException.class, donor::next);
        assertThrows(ConcurrentModificationException.class, candidate::next);
    }

    @Test void zeroAndNegativeCountPoliciesMatchEachExactDonor() {
        var expectedMulti = new HashMultiSet<Integer>();
        var actualMulti = new M3ApacheMultiSet<Integer>();
        var expectedBag = new HashBag<Integer>();
        var actualBag = new M3ApacheBag<Integer>();
        for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 1}) {
            sameOutcome(() -> expectedMulti.add(null, count), () -> actualMulti.add(null, count), "multiset add");
            sameOutcome(() -> expectedMulti.remove(null, count), () -> actualMulti.remove(null, count), "multiset remove");
            sameOutcome(() -> expectedMulti.setCount(null, count), () -> actualMulti.setCount(null, count), "multiset setCount");
            sameOutcome(() -> expectedBag.add(null, count), () -> actualBag.add(null, count), "bag add");
            sameOutcome(() -> expectedBag.remove(null, count), () -> actualBag.remove(null, count), "bag remove");
        }
        assertEquals(expectedMulti, actualMulti);
        assertEquals(expectedBag, actualBag);
    }

    @Test void bagBulkOperationsRespectCardinalityAndDistinctAddReturn() {
        var expected = new HashBag<Integer>();
        var owner = new M3ObjectCounts<Integer>();
        var actual = new M3ApacheBag<>(owner);
        expected.add(1, 5); actual.add(1, 5);
        assertFalse(actual.addAll(List.of(1, 1)));
        assertFalse(expected.addAll(List.of(1, 1)));
        assertEquals(expected.containsAll(List.of(1, 1, 1)), actual.containsAll(List.of(1, 1, 1)));
        assertEquals(expected.removeAll(List.of(1, 1)), actual.removeAll(List.of(1, 1)));
        assertEquals(5, actual.getCount(1));
        assertEquals(expected.retainAll(List.of(1, 1)), actual.retainAll(List.of(1, 1)));
        assertEquals(2, actual.getCount(1));
        assertFalse(actual.containsAll(List.of(1, 1, 1)));
        assertEquals(expected.removeAll(null), actual.removeAll(null));
        sameOutcome(() -> expected.containsAll(null), () -> actual.containsAll(null), "bag containsAll null");
        sameOutcome(() -> expected.retainAll(null), () -> actual.retainAll(null), "bag retainAll null");
        sameBag(expected, actual, owner, "legacy bulk");
    }

    @Test void multisetBulkMembershipIgnoresDuplicateRequestsAndRemovalErasesAllCopies() {
        var expected = new HashMultiSet<Integer>();
        var owner = new M3ObjectCounts<Integer>();
        var actual = new M3ApacheMultiSet<>(owner);
        expected.add(1, 2); actual.add(1, 2);
        assertTrue(actual.containsAll(List.of(1, 1, 1)));
        assertEquals(expected.removeAll(List.of(1)), actual.removeAll(List.of(1)));
        assertEquals(0, actual.getCount(1));
        sameMultiSet(expected, actual, owner, "multiset bulk");
    }

    @Test void bagUniqueSetIsUnmodifiableAndMultisetUniqueSetRemovesAllCopies() {
        var expectedBag = new HashBag<Integer>();
        var actualBag = new M3ApacheBag<Integer>();
        expectedBag.add(1, 3); actualBag.add(1, 3);
        sameOutcome(() -> expectedBag.uniqueSet().remove(1), () -> actualBag.uniqueSet().remove(1), "bag unique remove");
        Iterator<Integer> donor = expectedBag.uniqueSet().iterator();
        Iterator<Integer> candidate = actualBag.uniqueSet().iterator();
        assertEquals(donor.next(), candidate.next());
        sameOutcome(() -> { donor.remove(); return null; }, () -> { candidate.remove(); return null; }, "bag unique iterator");
        var owner = new M3ObjectCounts<Integer>();
        var multi = new M3ApacheMultiSet<>(owner);
        multi.add(1, 3);
        Set<Integer> live = multi.uniqueSet();
        assertTrue(live.remove(1));
        assertEquals(0L, owner.totalCount());
        multi.add(2, 4);
        assertEquals(Set.of(2), live);
        owner.verifyInvariants();
    }

    @Test void occurrenceIteratorRemovesOneAndCanContinueAcrossDistinctElements() {
        for (boolean bag : List.of(false, true)) {
            Collection<Integer> expected = bag ? new HashBag<>() : new HashMultiSet<>();
            Collection<Integer> actual = bag ? new M3ApacheBag<>() : new M3ApacheMultiSet<>();
            expected.addAll(List.of(1, 1, 2, 3)); actual.addAll(List.of(1, 1, 2, 3));
            Iterator<Integer> donor = expected.iterator();
            Iterator<Integer> candidate = actual.iterator();
            sameOutcome(() -> { donor.remove(); return null; }, () -> { candidate.remove(); return null; }, "before next");
            while (donor.hasNext()) {
                assertTrue(candidate.hasNext());
                assertEquals(donor.next(), candidate.next());
                donor.remove(); candidate.remove();
                sameOutcome(() -> { donor.remove(); return null; }, () -> { candidate.remove(); return null; }, "double remove");
            }
            assertFalse(candidate.hasNext());
            sameOutcome(donor::next, candidate::next, "exhaustion");
            assertTrue(actual.isEmpty());
        }
    }

    @Test void entryViewsTrackLiveCountsAndIteratorRemovalPreservesDetachedEntry() {
        var expected = new HashMultiSet<Integer>();
        var owner = new M3ObjectCounts<Integer>();
        var actual = new M3ApacheMultiSet<>(owner);
        expected.add(1, 2); actual.add(1, 2);
        Iterator<MultiSet.Entry<Integer>> donor = expected.entrySet().iterator();
        Iterator<MultiSet.Entry<Integer>> candidate = actual.entrySet().iterator();
        MultiSet.Entry<Integer> expectedEntry = donor.next(), actualEntry = candidate.next();
        expected.add(1, 2); actual.add(1, 2);
        assertEquals(expectedEntry.getCount(), actualEntry.getCount());
        donor.remove(); candidate.remove();
        assertEquals(expectedEntry.getCount(), actualEntry.getCount());
        assertEquals(4, actualEntry.getCount());
        assertTrue(actual.isEmpty());
        assertEquals(expected.entrySet().contains(new CountEntry<>(9, 0)),
                actual.entrySet().contains(new CountEntry<>(9, 0)), "inherited zero-count membership");
        sameMultiSet(expected, actual, owner, "entry projections");
    }

    @Test void typedArrayConversionsRetainDonorTailNullingAndArrayStoreFailures() {
        for (boolean bag : List.of(false, true)) {
            Collection<Integer> expected = bag ? new HashBag<>() : new HashMultiSet<>();
            Collection<Integer> actual = bag ? new M3ApacheBag<>() : new M3ApacheMultiSet<>();
            expected.addAll(List.of(3, 3)); actual.addAll(List.of(3, 3));
            Integer[] expectedArray = {7, 7, 7, 7, 7}, actualArray = {7, 7, 7, 7, 7};
            assertSame(expectedArray, expected.toArray(expectedArray));
            assertSame(actualArray, actual.toArray(actualArray));
            assertArrayEquals(expectedArray, actualArray);
            assertArrayEquals(new Integer[] {3, 3, null, null, null}, actualArray);
            assertArrayEquals(expected.toArray(new Integer[0]), actual.toArray(new Integer[0]));
            sameOutcome(() -> expected.toArray(new String[3]), () -> actual.toArray(new String[3]), "array store");
            sameOutcome(() -> expected.toArray((Object[]) null), () -> actual.toArray((Object[]) null), "null array");
        }
    }

    @Test void multisetRandomizedCountsAndMutableViewsMatchApache() {
        for (long seed : new long[] {17, 2909, 0x434f554e54L}) {
            var expected = new HashMultiSet<Integer>();
            var owner = new M3ObjectCounts<Integer>();
            var actual = new M3ApacheMultiSet<>(owner);
            Random random = new Random(seed);
            for (int step = 0; step < 1200; step++) {
                Integer element = token(random), other = token(random);
                int operation = random.nextInt(16), count = random.nextInt(7) - 1;
                String context = "multiset seed=" + seed + " step=" + step + " op=" + operation;
                sameOutcome(() -> mutateMultiSet(expected, operation, element, other, count),
                        () -> mutateMultiSet(actual, operation, element, other, count), context);
                sameMultiSet(expected, actual, owner, context);
            }
        }
    }

    @Test void legacyBagRandomizedCountsAndCardinalityOperationsMatchApache() {
        for (long seed : new long[] {31, 1543, 0x424147L}) {
            var expected = new HashBag<Integer>();
            var owner = new M3ObjectCounts<Integer>();
            var actual = new M3ApacheBag<>(owner);
            Random random = new Random(seed);
            for (int step = 0; step < 1200; step++) {
                Integer element = token(random), other = token(random);
                int operation = random.nextInt(12), count = random.nextInt(7) - 1;
                String context = "bag seed=" + seed + " step=" + step + " op=" + operation;
                sameOutcome(() -> mutateBag(expected, operation, element, other, count),
                        () -> mutateBag(actual, operation, element, other, count), context);
                sameBag(expected, actual, owner, context);
            }
        }
    }

    private static boolean removeOccurrence(Collection<Integer> values, Integer target) {
        Iterator<Integer> iterator = values.iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), target)) { iterator.remove(); return true; }
        }
        return false;
    }

    private static boolean removeUnique(MultiSet<Integer> values, Integer target) {
        Iterator<Integer> iterator = values.uniqueSet().iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), target)) { iterator.remove(); return true; }
        }
        return false;
    }

    private static boolean removeEntry(MultiSet<Integer> values, Integer target) {
        Iterator<MultiSet.Entry<Integer>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next().getElement(), target)) { iterator.remove(); return true; }
        }
        return false;
    }

    private static Object mutateMultiSet(MultiSet<Integer> values, int operation,
            Integer element, Integer other, int count) {
        List<Integer> input = Arrays.asList(element, other, element);
        return switch (operation) {
            case 0 -> values.add(element);
            case 1 -> values.add(element, count);
            case 2 -> values.remove(element);
            case 3 -> values.remove(element, count);
            case 4 -> values.setCount(element, count);
            case 5 -> values.addAll(input);
            case 6 -> values.removeAll(input);
            case 7 -> values.retainAll(input);
            case 8 -> values.containsAll(input);
            case 9 -> values.uniqueSet().remove(element);
            case 10 -> removeOccurrence(values, element);
            case 11 -> removeUnique(values, element);
            case 12 -> removeEntry(values, element);
            case 13 -> values.entrySet().remove(new CountEntry<>(element, count));
            case 14 -> values.removeIf(value -> Objects.equals(value, element));
            default -> {
                if (count == 0) { values.clear(); yield null; }
                yield values.add(element, 0);
            }
        };
    }

    private static Object mutateBag(Bag<Integer> values, int operation,
            Integer element, Integer other, int count) {
        List<Integer> input = Arrays.asList(element, other, element);
        return switch (operation) {
            case 0 -> values.add(element);
            case 1 -> values.add(element, count);
            case 2 -> values.remove(element);
            case 3 -> values.remove(element, count);
            case 4 -> values.addAll(input);
            case 5 -> values.removeAll(input);
            case 6 -> values.retainAll(input);
            case 7 -> values.containsAll(input);
            case 8 -> removeOccurrence(values, element);
            case 9 -> values.uniqueSet().remove(element);
            case 10 -> values.removeIf(value -> Objects.equals(value, element));
            default -> {
                if (count == 0) { values.clear(); yield null; }
                yield values.add(element, 0);
            }
        };
    }
}
