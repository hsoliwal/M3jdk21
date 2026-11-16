/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jdk.internal.m3.collections.range.FrozenLongRangeSet;
import org.junit.jupiter.api.Test;

/** Independent closed-range topology and unbounded-arithmetic domain models. */
final class FrozenLongRangeSetContractTest {
    private static final long MIN = Long.MIN_VALUE;
    private static final long MAX = Long.MAX_VALUE;
    private static final BigInteger BIG_MIN = BigInteger.valueOf(MIN);
    private static final BigInteger BIG_MAX = BigInteger.valueOf(MAX);

    private record Interval(long lower, long upper) { }

    private static List<Interval> model(long[] lowers, long[] uppers) {
        List<Interval> result = new ArrayList<>();
        for (int i = 0; i < lowers.length; i++) result.add(new Interval(lowers[i], uppers[i]));
        result.sort(Comparator.comparingLong(Interval::lower));
        return result;
    }

    private static int index(List<Interval> ranges, long value) {
        for (int i = 0; i < ranges.size(); i++) {
            Interval range = ranges.get(i);
            if (range.lower() <= value && value <= range.upper()) return i;
        }
        return -1;
    }

    private static boolean enclosed(List<Interval> ranges, long lower, long upper) {
        for (Interval range : ranges) {
            if (range.lower() <= lower && upper <= range.upper()) return true;
        }
        return false;
    }

    private static List<Interval> projectedComplement(List<Interval> ranges) {
        List<Interval> result = new ArrayList<>();
        BigInteger cursor = BIG_MIN;
        for (Interval range : ranges) {
            BigInteger end = BigInteger.valueOf(range.lower()).subtract(BigInteger.ONE);
            if (cursor.compareTo(end) <= 0) {
                result.add(new Interval(cursor.longValueExact(), end.longValueExact()));
            }
            cursor = BigInteger.valueOf(range.upper()).add(BigInteger.ONE);
        }
        if (cursor.compareTo(BIG_MAX) <= 0) result.add(new Interval(cursor.longValueExact(), MAX));
        return result;
    }

    private static void ranges(FrozenLongRangeSet actual, List<Interval> expected) {
        assertEquals(expected.size(), actual.rangeCount());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        for (int i = 0; i < expected.size(); i++) {
            Interval range = expected.get(i);
            assertEquals(range.lower(), actual.lowerAt(i));
            assertEquals(range.upper(), actual.upperAt(i));
            assertEquals(i, actual.rangeIndexOf(range.lower()));
            assertEquals(i, actual.rangeIndexOf(range.upper()));
            assertTrue(actual.encloses(range.lower(), range.upper()));
        }
        if (expected.isEmpty()) {
            assertThrows(NoSuchElementException.class, actual::spanLower);
            assertThrows(NoSuchElementException.class, actual::spanUpper);
        } else {
            assertEquals(expected.get(0).lower(), actual.spanLower());
            assertEquals(expected.get(expected.size() - 1).upper(), actual.spanUpper());
        }
    }

    private static void query(FrozenLongRangeSet actual, List<Interval> expected, long point) {
        int expectedIndex = index(expected, point);
        assertEquals(expectedIndex, actual.rangeIndexOf(point), "ordinal at " + point);
        assertEquals(expectedIndex >= 0, actual.contains(point), "contains " + point);
        assertEquals(expectedIndex >= 0, actual.encloses(point, point));
    }

    private static void bounds(FrozenLongRangeSet actual) {
        for (int ordinal : new int[] {Integer.MIN_VALUE, -1, actual.rangeCount(), Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> actual.lowerAt(ordinal));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.upperAt(ordinal));
        }
        assertThrows(IllegalArgumentException.class, () -> actual.encloses(1, 0));
        assertThrows(IllegalArgumentException.class, () -> actual.encloses(MAX, MIN));
    }

    private static FrozenLongRangeSet verify(long[] lowers, long[] uppers, long[] points) {
        long[] beforeLower = lowers.clone();
        long[] beforeUpper = uppers.clone();
        List<Interval> expected = model(lowers, uppers);
        FrozenLongRangeSet actual = FrozenLongRangeSet.copyOf(lowers, uppers);
        assertArrayEquals(beforeLower, lowers);
        assertArrayEquals(beforeUpper, uppers);
        ranges(actual, expected);
        bounds(actual);
        List<Interval> inverse = projectedComplement(expected);
        FrozenLongRangeSet complement = actual.complement();
        assertNotSame(actual, complement);
        assertSame(complement, actual.complement());
        assertSame(actual, complement.complement());
        ranges(complement, inverse);
        bounds(complement);
        for (long point : points) {
            query(actual, expected, point);
            query(complement, inverse, point);
            assertEquals(!actual.contains(point), complement.contains(point));
        }
        for (Interval range : expected) {
            query(actual, expected, range.lower());
            query(actual, expected, range.upper());
            if (range.lower() > MIN) query(actual, expected, range.lower() - 1);
            if (range.upper() < MAX) query(actual, expected, range.upper() + 1);
        }
        return actual;
    }

    private static long[] points() {
        return new long[] {MIN, MIN + 1, MIN + 2, -10, -2, -1, 0, 1, 2, 10, MAX - 2, MAX - 1, MAX};
    }

    @Test
    void emptyOwnersAndFullUniverseHaveClosedComplements() {
        FrozenLongRangeSet empty = new FrozenLongRangeSet();
        ranges(empty, List.of());
        bounds(empty);
        for (long point : points()) query(empty, List.of(), point);
        FrozenLongRangeSet all = empty.complement();
        ranges(all, List.of(new Interval(MIN, MAX)));
        assertTrue(all.encloses(MIN, MAX));
        assertSame(empty, all.complement());
        assertSame(all, empty.complement());
        verify(new long[0], new long[0], points());
        verify(new long[] {MIN}, new long[] {MAX}, points());
    }

    @Test
    void signedSortingPreservesPairsAndSnapshotsCallerLanes() {
        long[] lowers = {MAX - 3, -5, MIN, 6, 0};
        long[] uppers = {MAX, -2, MIN + 4, 9, 2};
        FrozenLongRangeSet actual = verify(lowers, uppers, points());
        List<Interval> expected = model(lowers, uppers);
        Arrays.fill(lowers, MAX);
        Arrays.fill(uppers, MIN);
        ranges(actual, expected);
        ranges(actual.complement(), projectedComplement(expected));
        long[] both = {MAX, 0, MIN, -1, 1};
        FrozenLongRangeSet singletons = verify(both, both, points());
        Arrays.fill(both, 9);
        ranges(singletons, List.of(new Interval(MIN, MIN), new Interval(-1, -1),
                new Interval(0, 0), new Interval(1, 1), new Interval(MAX, MAX)));
    }

    @Test
    void constructionRejectsNullLengthReversalAndEveryClosedOverlap() {
        assertThrows(NullPointerException.class, () -> FrozenLongRangeSet.copyOf(null, new long[0]));
        assertThrows(NullPointerException.class, () -> FrozenLongRangeSet.copyOf(new long[0], null));
        assertThrows(NullPointerException.class, () -> FrozenLongRangeSet.copyOf(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> FrozenLongRangeSet.copyOf(new long[] {0}, new long[0]));
        assertThrows(IllegalArgumentException.class,
                () -> FrozenLongRangeSet.copyOf(new long[0], new long[] {0}));
        List<long[][]> invalid = List.of(
                new long[][] {{1}, {0}}, new long[][] {{MAX}, {MIN}},
                new long[][] {{1, 1}, {2, 2}}, new long[][] {{0, 2}, {2, 4}},
                new long[][] {{4, 0}, {8, 5}}, new long[][] {{MIN, 0}, {MAX, 0}},
                new long[][] {{MAX, MAX}, {MAX, MAX}},
                new long[][] {{MIN, MIN}, {MIN, MIN + 1}},
                new long[][] {{9, -20, -10}, {10, -5, -8}});
        for (long[][] input : invalid) {
            long[] oldLowers = input[0].clone();
            long[] oldUppers = input[1].clone();
            assertThrows(IllegalArgumentException.class, () -> FrozenLongRangeSet.copyOf(input[0], input[1]));
            assertArrayEquals(oldLowers, input[0]);
            assertArrayEquals(oldUppers, input[1]);
        }
    }

    @Test
    void singletonsAndExtremeEndpointsNeverOverflow() {
        for (long point : points()) verify(new long[] {point}, new long[] {point}, points());
        verify(new long[] {MIN, 0}, new long[] {-1, MAX}, points());
        verify(new long[] {MIN, MIN + 1, MAX - 1, MAX},
                new long[] {MIN, MIN + 1, MAX - 1, MAX}, points());
        verify(new long[] {MIN + 1}, new long[] {MAX - 1}, points());
    }

    @Test
    void integerAdjacentRangesRetainBoundarySensitiveEnclosure() {
        FrozenLongRangeSet actual = verify(new long[] {3, 1}, new long[] {4, 2},
                new long[] {0, 1, 2, 3, 4, 5});
        assertEquals(2, actual.rangeCount());
        assertTrue(actual.encloses(1, 2));
        assertTrue(actual.encloses(3, 4));
        assertFalse(actual.encloses(1, 4));
        assertFalse(actual.encloses(2, 3));
        assertEquals(0, actual.rangeIndexOf(2));
        assertEquals(1, actual.rangeIndexOf(3));
        ranges(actual.complement(), List.of(new Interval(MIN, 0), new Interval(5, MAX)));
        assertSame(actual, actual.complement().complement());
        assertEquals(2, actual.complement().complement().rangeCount());
        assertFalse(actual.complement().complement().encloses(1, 4));
    }

    @Test
    void adjacentFullUniverseDecompositionSurvivesDoubleComplement() {
        FrozenLongRangeSet actual = verify(new long[] {0, MIN}, new long[] {MAX, -1}, points());
        assertEquals(2, actual.rangeCount());
        assertFalse(actual.encloses(MIN, MAX));
        assertTrue(actual.complement().isEmpty());
        assertSame(actual, actual.complement().complement());
        assertFalse(actual.complement().complement().encloses(-1, 0));
        for (long point : points()) assertTrue(actual.contains(point));
    }

    @Test
    void openGapsProjectToExactlyTheRemainingClosedLongs() {
        FrozenLongRangeSet actual = verify(new long[] {MIN, MIN + 2, 0, 2, MAX},
                new long[] {MIN, -2, 0, MAX - 2, MAX}, points());
        ranges(actual.complement(), List.of(new Interval(MIN + 1, MIN + 1),
                new Interval(-1, -1), new Interval(1, 1), new Interval(MAX - 1, MAX - 1)));
        assertFalse(actual.complement().encloses(-1, 1));
        verify(new long[] {MIN, MAX}, new long[] {MIN, MAX}, points());
        verify(new long[] {MIN}, new long[] {MAX - 1}, points());
        verify(new long[] {MIN + 1}, new long[] {MAX}, points());
    }

    @Test
    void exhaustiveSmallDomainMembershipAndEveryQueryInterval() {
        long[] probes = new long[13];
        for (int i = 0; i < probes.length; i++) probes[i] = i - 6;
        for (int mask = 0; mask < (1 << 9); mask++) {
            for (boolean retainSingletons : new boolean[] {false, true}) {
                List<Interval> input = new ArrayList<>();
                int at = 0;
                while (at < 9) {
                    if ((mask & (1 << at)) == 0) {
                        at++;
                        continue;
                    }
                    int first = at++;
                    if (!retainSingletons) {
                        while (at < 9 && (mask & (1 << at)) != 0) at++;
                    }
                    input.add(new Interval(first - 4, at - 5));
                }
                Collections.reverse(input);
                long[] lowers = input.stream().mapToLong(Interval::lower).toArray();
                long[] uppers = input.stream().mapToLong(Interval::upper).toArray();
                FrozenLongRangeSet actual = verify(lowers, uppers, probes);
                List<Interval> expected = model(lowers, uppers);
                List<Interval> inverse = projectedComplement(expected);
                for (long lower = -6; lower <= 6; lower++) {
                    for (long upper = lower; upper <= 6; upper++) {
                        assertEquals(enclosed(expected, lower, upper), actual.encloses(lower, upper));
                        assertEquals(enclosed(inverse, lower, upper), actual.complement().encloses(lower, upper));
                    }
                }
            }
        }
    }

    @Test
    void fullWidthRandomPairsAgreeWithIndependentLinearAndBigIntegerModels() {
        Random random = new Random(0x71A9E5A116L);
        for (int round = 0; round < 160; round++) {
            TreeSet<Long> endpoints = new TreeSet<>();
            int endpointCount = 2 + 2 * random.nextInt(60);
            if ((round & 1) == 0) {
                endpoints.add(MIN);
                endpoints.add(MAX);
            }
            while (endpoints.size() < endpointCount) endpoints.add(random.nextLong());
            List<Long> ordered = new ArrayList<>(endpoints);
            List<Interval> input = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i += 2) {
                input.add(new Interval(ordered.get(i), ordered.get(i + 1)));
            }
            Collections.shuffle(input, random);
            long[] lowers = input.stream().mapToLong(Interval::lower).toArray();
            long[] uppers = input.stream().mapToLong(Interval::upper).toArray();
            long[] probes = new long[100];
            for (int i = 0; i < probes.length; i++) probes[i] = random.nextLong();
            FrozenLongRangeSet actual = verify(lowers, uppers, probes);
            List<Interval> expected = model(lowers, uppers);
            List<Interval> inverse = projectedComplement(expected);
            for (int i = 0; i + 1 < probes.length; i++) {
                long lower = Math.min(probes[i], probes[i + 1]);
                long upper = Math.max(probes[i], probes[i + 1]);
                assertEquals(enclosed(expected, lower, upper), actual.encloses(lower, upper));
                assertEquals(enclosed(inverse, lower, upper), actual.complement().encloses(lower, upper));
            }
        }
    }

    @Test
    void immutableMutatorsRejectAllRequestsWithoutChangingQueries() {
        FrozenLongRangeSet[] owners = {
            new FrozenLongRangeSet(),
            FrozenLongRangeSet.copyOf(new long[] {MIN}, new long[] {MAX}),
            FrozenLongRangeSet.copyOf(new long[] {3, 1}, new long[] {4, 2})
        };
        long[][] requests = {{0, 0}, {MIN, MAX}, {MAX, MIN}, {1, 0}, {1, 4}, {5, 6}};
        for (FrozenLongRangeSet owner : owners) {
            for (FrozenLongRangeSet actual : new FrozenLongRangeSet[] {owner, owner.complement()}) {
                List<Interval> before = new ArrayList<>();
                for (int i = 0; i < actual.rangeCount(); i++) {
                    before.add(new Interval(actual.lowerAt(i), actual.upperAt(i)));
                }
                for (long[] request : requests) {
                    assertThrows(UnsupportedOperationException.class, () -> actual.add(request[0], request[1]));
                    assertThrows(UnsupportedOperationException.class, () -> actual.remove(request[0], request[1]));
                }
                ranges(actual, before);
            }
        }
    }

    @Test
    void concurrentFirstComplementPublishesOneReciprocalImmutableOwner() throws Exception {
        FrozenLongRangeSet actual = FrozenLongRangeSet.copyOf(new long[] {MIN, 1, 3, MAX},
                new long[] {MIN, 2, 4, MAX});
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<FrozenLongRangeSet>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    FrozenLongRangeSet inverse = actual.complement();
                    for (int repeat = 0; repeat < 100; repeat++) {
                        assertSame(actual, inverse.complement());
                        assertSame(inverse, actual.complement());
                        assertFalse(inverse.contains(MIN));
                        assertFalse(inverse.contains(MAX));
                        assertTrue(inverse.contains(0));
                    }
                    return inverse;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            FrozenLongRangeSet inverse = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<FrozenLongRangeSet> future : futures) {
                assertSame(inverse, future.get(10, TimeUnit.SECONDS));
            }
            ranges(inverse, List.of(new Interval(MIN + 1, 0), new Interval(5, MAX - 1)));
            assertSame(actual, inverse.complement());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void originalPublicSurfaceAndDescriptorRemainExact() {
        Set<String> expected = Set.of(
                "scaffoldDescriptor():java.lang.String", "copyOf(long[],long[]):jdk.internal.m3.collections.range.FrozenLongRangeSet",
                "rangeCount():int", "isEmpty():boolean", "contains(long):boolean", "encloses(long,long):boolean",
                "rangeIndexOf(long):int", "lowerAt(int):long", "upperAt(int):long", "spanLower():long", "spanUpper():long",
                "complement():jdk.internal.m3.collections.range.FrozenLongRangeSet", "add(long,long):void", "remove(long,long):void");
        Set<String> actual = new HashSet<>();
        for (Method method : FrozenLongRangeSet.class.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                String parameters = String.join(",", Arrays.stream(method.getParameterTypes()).map(Class::getTypeName).toList());
                actual.add(method.getName() + "(" + parameters + "):" + method.getReturnType().getTypeName());
            }
        }
        assertEquals(expected, actual);
        assertEquals(1, FrozenLongRangeSet.class.getConstructors().length);
        assertEquals(0, FrozenLongRangeSet.class.getConstructors()[0].getParameterCount());
        assertTrue(Modifier.isFinal(FrozenLongRangeSet.class.getModifiers()));
        assertEquals("M3FrozenLongRangeSet|FROZEN_LONG_RANGE_SET|LONG|FROZEN|long[] lowerLane;long[] upperLane",
                FrozenLongRangeSet.scaffoldDescriptor());
    }
}
