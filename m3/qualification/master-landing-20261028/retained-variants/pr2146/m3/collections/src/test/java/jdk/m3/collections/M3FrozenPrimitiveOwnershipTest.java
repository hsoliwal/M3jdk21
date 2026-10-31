/*
 * Copyright 2026 Hitesh Soliwal and contributors
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;

/**
 * Independent ownership and observable-contract checks for the received primitive owners.
 * The range oracle is the actual Java 21 List.copyOf implementation. Guava's different
 * invalid-range exception behavior is deliberately not inferred from the JDK result.
 * Historical frozen-set equality, hash, rendering and state tests remain separate.
 */
final class M3FrozenPrimitiveOwnershipTest {
    @Test
    void emptyPublicConstructorsProvideUsableImmutableOwners() {
        M3FrozenIntList ints = new M3FrozenIntList();
        M3FrozenLongList longs = new M3FrozenLongList();
        M3FrozenLongSet set = new M3FrozenLongSet();
        assertEquals(0, ints.size());
        assertEquals(0, longs.size());
        assertEquals(0, set.size());
        assertTrue(longs.isEmpty());
        assertTrue(set.isEmpty());
        assertArrayEquals(new int[0], ints.toArray());
        assertArrayEquals(new long[0], longs.toArray());
        assertArrayEquals(new long[0], set.toArray());
        assertEquals(-1, ints.indexOf(Integer.MIN_VALUE));
        assertEquals(-1, longs.indexOf(Long.MIN_VALUE));
        assertFalse(longs.contains(0));
        assertFalse(set.contains(0));
        assertEquals(-1, set.rank(0));
        assertArrayEquals(new long[0], longs.subList(0, 0).toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> ints.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> longs.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(0));
        assertThrows(NoSuchElementException.class, set::first);
        assertThrows(NoSuchElementException.class, set::last);
        assertThrows(UnsupportedOperationException.class, () -> ints.add(1));
        assertThrows(UnsupportedOperationException.class, () -> longs.add(1));
        assertThrows(UnsupportedOperationException.class, () -> longs.set(-1, 1));
        assertThrows(UnsupportedOperationException.class, () -> set.add(1));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(1));
        assertIntCursor(ints.iterator(), new int[0]);
        assertLongCursor(longs.iterator(), new long[0]);
        assertLongCursor(set.iterator(), new long[0]);
    }

    @Test
    void everyOwnerDetachesInputAndEveryExportedMutableArray() {
        int[] intSource = {Integer.MIN_VALUE, 4, 4, -1, Integer.MAX_VALUE};
        long[] longSource = {Long.MIN_VALUE, 4, 4, -1, Long.MAX_VALUE};
        int[] expectedInts = intSource.clone();
        long[] expectedLongs = longSource.clone();
        long[] expectedSet = {Long.MIN_VALUE, -1, 4, Long.MAX_VALUE};
        M3FrozenIntList ints = M3FrozenIntList.copyOf(intSource);
        M3FrozenLongList longs = M3FrozenLongList.copyOf(longSource);
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(longSource);
        Arrays.fill(intSource, 81);
        Arrays.fill(longSource, 81);
        int[] intExport = ints.toArray();
        long[] longExport = longs.toArray();
        long[] setExport = set.toArray();
        assertNotSame(intSource, intExport);
        assertNotSame(longSource, longExport);
        assertNotSame(longSource, setExport);
        assertNotSame(intExport, ints.toArray());
        assertNotSame(longExport, longs.toArray());
        assertNotSame(setExport, set.toArray());
        Arrays.fill(intExport, 92);
        Arrays.fill(longExport, 92);
        Arrays.fill(setExport, 92);
        assertArrayEquals(expectedInts, ints.toArray());
        assertArrayEquals(expectedLongs, longs.toArray());
        assertArrayEquals(expectedSet, set.toArray());
        assertEquals(Integer.MIN_VALUE, ints.get(0));
        assertEquals(Long.MIN_VALUE, longs.get(0));
        assertEquals(Long.MIN_VALUE, set.first());
        assertThrows(NullPointerException.class, () -> M3FrozenIntList.copyOf(null));
        assertThrows(NullPointerException.class, () -> M3FrozenLongList.copyOf(null));
        assertThrows(NullPointerException.class, () -> M3FrozenLongSet.copyOf(null));
    }

    @Test
    void nestedLongViewsShareNonemptyPayloadAndDetachEmptyProperSlices() throws Exception {
        long[] source = new long[4096];
        for (int i = 0; i < source.length; i++) source[i] = i - 2048L;
        M3FrozenLongList owner = M3FrozenLongList.copyOf(source);
        M3FrozenLongList view = owner.subList(500, 3500);
        M3FrozenLongList nested = view.subList(120, 1000);
        assertSame(payload(owner), payload(view));
        assertSame(payload(owner), payload(nested));
        assertArrayEquals(Arrays.copyOfRange(source, 620, 1500), nested.toArray());
        long[] exported = nested.toArray();
        Arrays.fill(exported, Long.MAX_VALUE);
        assertEquals(source[620], nested.get(0));
        assertEquals(source[1499], nested.get(nested.size() - 1));
        assertArrayEquals(source, owner.toArray());
        for (int at : new int[]{0, nested.size() / 2, nested.size()}) {
            M3FrozenLongList empty = nested.subList(at, at);
            assertEquals(0, empty.size());
            assertEquals(0, payload(empty).length);
            assertNotSame(payload(owner), payload(empty));
            assertArrayEquals(new long[0], empty.subList(0, 0).toArray());
        }
        // An empty owner's full-range view may retain its already-empty owner.
        M3FrozenLongList emptyOwner = new M3FrozenLongList();
        assertEquals(0, payload(emptyOwner.subList(0, 0)).length);
    }

    @Test
    void allInvalidSubListRangesMatchActualJdkImmutableListExceptionPrecedence() {
        for (int size : new int[]{0, 1, 2, 5}) {
            List<Long> values = new ArrayList<>();
            for (int i = 0; i < size; i++) values.add(i - 2L);
            List<Long> oracle = List.copyOf(values);
            M3FrozenLongList owner = M3FrozenLongList.copyOf(
                    oracle.stream().mapToLong(Long::longValue).toArray());
            assertRangeMatrix(oracle, owner);
            if (size > 1) {
                assertRangeMatrix(oracle.subList(1, size), owner.subList(1, size));
            }
        }
    }

    @Test
    void listsPreserveFirstOccurrencePositionsIncludingSignedExtremesAndSlices() {
        int[] values = {Integer.MIN_VALUE, 0, -1, 0, Integer.MAX_VALUE,
                Integer.MIN_VALUE, -1, Integer.MAX_VALUE};
        long[] longValues = {Long.MIN_VALUE, 0, -1, 0, Long.MAX_VALUE,
                Long.MIN_VALUE, -1, Long.MAX_VALUE};
        List<Integer> intOracle = List.copyOf(Arrays.stream(values).boxed().toList());
        List<Long> longOracle = List.copyOf(Arrays.stream(longValues).boxed().toList());
        M3FrozenIntList ints = M3FrozenIntList.copyOf(values);
        M3FrozenLongList longs = M3FrozenLongList.copyOf(longValues);
        for (int query : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 17}) {
            assertEquals(intOracle.indexOf(query), ints.indexOf(query));
        }
        for (long query : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1, 17}) {
            assertEquals(longOracle.indexOf(query), longs.indexOf(query));
            assertEquals(longOracle.contains(query), longs.contains(query));
            assertEquals(longOracle.subList(1, 7).indexOf(query),
                    longs.subList(1, 7).indexOf(query));
        }
    }

    @Test
    void primitiveAndBoxedIterationPreserveOrderAndRejectExhaustedReads() {
        int[] ints = {Integer.MIN_VALUE, 7, 7, Integer.MAX_VALUE};
        long[] longs = {Long.MIN_VALUE, 7, 7, Long.MAX_VALUE};
        M3FrozenIntList intOwner = M3FrozenIntList.copyOf(ints);
        M3FrozenLongList longOwner = M3FrozenLongList.copyOf(longs);
        M3FrozenLongSet setOwner = M3FrozenLongSet.copyOf(longs);
        assertIntCursor(intOwner.iterator(), ints);
        assertLongCursor(longOwner.iterator(), longs);
        assertLongCursor(longOwner.subList(1, 3).iterator(), new long[]{7, 7});
        assertLongCursor(setOwner.iterator(), new long[]{Long.MIN_VALUE, 7, Long.MAX_VALUE});
        List<Integer> boxedInts = new ArrayList<>();
        List<Long> boxedLongs = new ArrayList<>();
        List<Long> boxedSet = new ArrayList<>();
        intOwner.iterator().forEachRemaining((Consumer<Integer>) boxedInts::add);
        longOwner.iterator().forEachRemaining((Consumer<Long>) boxedLongs::add);
        setOwner.iterator().forEachRemaining((Consumer<Long>) boxedSet::add);
        assertEquals(Arrays.stream(ints).boxed().toList(), boxedInts);
        assertEquals(Arrays.stream(longs).boxed().toList(), boxedLongs);
        assertEquals(List.of(Long.MIN_VALUE, 7L, Long.MAX_VALUE), boxedSet);
    }

    @Test
    void throwingRemainingActionsPreserveThrowableIdentityAndConsumeTheOffendingValue() {
        RuntimeException failure = new IllegalStateException("frozen callback witness");
        PrimitiveIterator.OfInt ints = M3FrozenIntList.copyOf(new int[]{11, 22, 33}).iterator();
        List<Integer> intSeen = new ArrayList<>();
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> ints.forEachRemaining((int value) -> {
                    intSeen.add(value);
                    if (value == 22) throw failure;
                })));
        assertEquals(List.of(11, 22), intSeen);
        assertEquals(33, ints.nextInt());
        assertFalse(ints.hasNext());
        assertLongFailureProgress(
                M3FrozenLongList.copyOf(new long[]{-9, 0, 5}).iterator(), failure);
        assertLongFailureProgress(
                M3FrozenLongSet.copyOf(new long[]{5, -9, 0, 0}).iterator(), failure);
        PrimitiveIterator.OfLong boxed = M3FrozenLongList.copyOf(new long[]{-9, 0, 5}).iterator();
        List<Long> boxedSeen = new ArrayList<>();
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> boxed.forEachRemaining((Consumer<Long>) value -> {
                    boxedSeen.add(value);
                    if (value == 0) throw failure;
                })));
        assertEquals(List.of(-9L, 0L), boxedSeen);
        assertEquals(5, boxed.nextLong());
        assertFalse(boxed.hasNext());
    }

    @Test
    void ownerCallbacksPreserveExceptionIdentityOrderAndImmutableContents() {
        RuntimeException failure = new IllegalArgumentException("frozen owner callback witness");
        M3FrozenIntList ints = M3FrozenIntList.copyOf(new int[]{11, 22, 33});
        M3FrozenLongList longs = M3FrozenLongList.copyOf(new long[]{11, 22, 33});
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(new long[]{33, 11, 22, 22});
        List<Integer> intSeen = new ArrayList<>();
        List<Long> longSeen = new ArrayList<>();
        List<Long> setSeen = new ArrayList<>();
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> ints.forEach(value -> {
                    intSeen.add(value);
                    if (value == 22) throw failure;
                })));
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> longs.forEach(value -> {
                    longSeen.add(value);
                    if (value == 22) throw failure;
                })));
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> set.forEach(value -> {
                    setSeen.add(value);
                    if (value == 22) throw failure;
                })));
        assertEquals(List.of(11, 22), intSeen);
        assertEquals(List.of(11L, 22L), longSeen);
        assertEquals(List.of(11L, 22L), setSeen);
        assertArrayEquals(new int[]{11, 22, 33}, ints.toArray());
        assertArrayEquals(new long[]{11, 22, 33}, longs.toArray());
        assertArrayEquals(new long[]{11, 22, 33}, set.toArray());
    }

    @Test
    void nullActionsAreRejectedForEmptyPopulatedAndExhaustedTraversals() {
        for (int[] values : new int[][]{new int[0], new int[]{7}}) {
            M3FrozenIntList owner = M3FrozenIntList.copyOf(values);
            assertThrows(NullPointerException.class, () -> owner.forEach(null));
            PrimitiveIterator.OfInt cursor = owner.iterator();
            assertIntNullActions(cursor);
            if (values.length != 0) assertEquals(7, cursor.nextInt());
            assertFalse(cursor.hasNext());
            assertIntNullActions(cursor);
        }
        for (long[] values : new long[][]{new long[0], new long[]{7}}) {
            M3FrozenLongList list = M3FrozenLongList.copyOf(values);
            M3FrozenLongSet set = M3FrozenLongSet.copyOf(values);
            assertThrows(NullPointerException.class, () -> list.forEach(null));
            assertThrows(NullPointerException.class, () -> set.forEach(null));
            for (PrimitiveIterator.OfLong cursor : List.of(list.iterator(), set.iterator())) {
                assertLongNullActions(cursor);
                if (values.length != 0) assertEquals(7, cursor.nextLong());
                assertFalse(cursor.hasNext());
                assertLongNullActions(cursor);
            }
        }
    }

    @Test
    void sortedSetHandlesSignedExtremesAndUsesAbsentIndexRatherThanInsertionRank() {
        long[] source = {Long.MAX_VALUE, -1, Long.MIN_VALUE, 0, Long.MAX_VALUE, 0};
        TreeSet<Long> oracle = new TreeSet<>();
        for (long value : source) oracle.add(value);
        List<Long> ordered = List.copyOf(oracle);
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(source);
        assertArrayEquals(ordered.stream().mapToLong(Long::longValue).toArray(), set.toArray());
        assertEquals(oracle.first().longValue(), set.first());
        assertEquals(oracle.last().longValue(), set.last());
        for (long query : new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, -2, -1,
                0, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE}) {
            assertEquals(oracle.contains(query), set.contains(query));
            assertEquals(ordered.indexOf(query), set.rank(query));
        }
    }

    @Test
    void concurrentReadersObserveStableOwnersAndIndependentExportedSnapshots() throws Exception {
        int[] inputInts = new int[512];
        long[] inputLongs = new long[512];
        for (int i = 0; i < inputInts.length; i++) {
            inputInts[i] = i % 19 - 9;
            inputLongs[i] = i % 19 - 9L;
        }
        inputInts[0] = Integer.MIN_VALUE;
        inputInts[1] = Integer.MAX_VALUE;
        inputLongs[0] = Long.MIN_VALUE;
        inputLongs[1] = Long.MAX_VALUE;
        int[] expectedInts = inputInts.clone();
        long[] expectedLongs = inputLongs.clone();
        TreeSet<Long> setOracle = new TreeSet<>();
        for (long value : inputLongs) setOracle.add(value);
        long[] expectedSet = setOracle.stream().mapToLong(Long::longValue).toArray();
        M3FrozenIntList ints = M3FrozenIntList.copyOf(inputInts);
        M3FrozenLongList longs = M3FrozenLongList.copyOf(inputLongs);
        M3FrozenLongList view = longs.subList(17, 401);
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(inputLongs);
        long[] expectedView = Arrays.copyOfRange(expectedLongs, 17, 401);
        Arrays.fill(inputInts, 71);
        Arrays.fill(inputLongs, 71);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> readers = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            for (int worker = 0; worker < 4; worker++) {
                final int identity = worker;
                readers.add(executor.submit(() -> {
                    start.await();
                    for (int iteration = 0; iteration < 64; iteration++) {
                        int[] intSnapshot = ints.toArray();
                        long[] longSnapshot = longs.toArray();
                        long[] setSnapshot = set.toArray();
                        long[] viewSnapshot = view.toArray();
                        assertArrayEquals(expectedInts, intSnapshot);
                        assertArrayEquals(expectedLongs, longSnapshot);
                        assertArrayEquals(expectedSet, setSnapshot);
                        assertArrayEquals(expectedView, viewSnapshot);
                        intSnapshot[identity] = 1000 + iteration;
                        longSnapshot[identity] = 1000 + iteration;
                        setSnapshot[identity] = 1000 + iteration;
                        viewSnapshot[identity] = 1000 + iteration;
                        assertEquals(expectedInts[identity], ints.get(identity));
                        assertEquals(expectedLongs[identity], longs.get(identity));
                        assertEquals(expectedSet[identity], set.get(identity));
                        assertEquals(expectedView[identity], view.get(identity));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> reader : readers) reader.get(20, TimeUnit.SECONDS);
        }
        assertArrayEquals(expectedInts, ints.toArray());
        assertArrayEquals(expectedLongs, longs.toArray());
        assertArrayEquals(expectedSet, set.toArray());
        assertArrayEquals(expectedView, view.toArray());
    }

    private static void assertRangeMatrix(List<Long> oracle, M3FrozenLongList owner) {
        int size = oracle.size();
        int[] positions = {Integer.MIN_VALUE, -2, -1, 0, 1, size - 1,
                size, size + 1, Integer.MAX_VALUE};
        for (int from : positions) {
            for (int to : positions) {
                String context = "size=" + size + ", from=" + from + ", to=" + to;
                RuntimeException expectedFailure = null;
                List<Long> expected = null;
                try {
                    expected = oracle.subList(from, to);
                } catch (RuntimeException failure) {
                    expectedFailure = failure;
                }
                if (expectedFailure != null) {
                    RuntimeException observed = assertThrows(expectedFailure.getClass(),
                            () -> owner.subList(from, to), context);
                    assertEquals(expectedFailure.getClass(), observed.getClass(), context);
                } else {
                    assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(),
                            owner.subList(from, to).toArray(), context);
                }
            }
        }
    }

    private static long[] payload(M3FrozenLongList owner) throws Exception {
        Field field = M3FrozenLongList.class.getDeclaredField("valueLane");
        field.setAccessible(true);
        return (long[]) field.get(owner);
    }

    private static void assertIntCursor(PrimitiveIterator.OfInt cursor, int[] expected) {
        assertThrows(UnsupportedOperationException.class, cursor::remove);
        for (int value : expected) {
            assertTrue(cursor.hasNext());
            assertEquals(value, cursor.nextInt());
        }
        assertFalse(cursor.hasNext());
        assertThrows(NoSuchElementException.class, cursor::nextInt);
        assertThrows(NoSuchElementException.class, cursor::next);
        assertFalse(cursor.hasNext());
        assertThrows(UnsupportedOperationException.class, cursor::remove);
    }

    private static void assertLongCursor(PrimitiveIterator.OfLong cursor, long[] expected) {
        assertThrows(UnsupportedOperationException.class, cursor::remove);
        for (long value : expected) {
            assertTrue(cursor.hasNext());
            assertEquals(value, cursor.nextLong());
        }
        assertFalse(cursor.hasNext());
        assertThrows(NoSuchElementException.class, cursor::nextLong);
        assertThrows(NoSuchElementException.class, cursor::next);
        assertFalse(cursor.hasNext());
        assertThrows(UnsupportedOperationException.class, cursor::remove);
    }

    private static void assertLongFailureProgress(
            PrimitiveIterator.OfLong cursor, RuntimeException failure) {
        List<Long> seen = new ArrayList<>();
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> cursor.forEachRemaining((long value) -> {
                    seen.add(value);
                    if (value == 0) throw failure;
                })));
        assertEquals(List.of(-9L, 0L), seen);
        assertEquals(5, cursor.nextLong());
        assertFalse(cursor.hasNext());
    }

    private static void assertIntNullActions(PrimitiveIterator.OfInt cursor) {
        assertThrows(NullPointerException.class,
                () -> cursor.forEachRemaining((IntConsumer) null));
        assertThrows(NullPointerException.class,
                () -> cursor.forEachRemaining((Consumer<Integer>) null));
    }

    private static void assertLongNullActions(PrimitiveIterator.OfLong cursor) {
        assertThrows(NullPointerException.class,
                () -> cursor.forEachRemaining((LongConsumer) null));
        assertThrows(NullPointerException.class,
                () -> cursor.forEachRemaining((Consumer<Long>) null));
    }
}
