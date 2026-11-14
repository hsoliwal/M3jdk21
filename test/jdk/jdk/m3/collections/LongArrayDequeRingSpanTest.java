/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Source-exact primitive-ring contract: bulk slot shifts and clears must
 * reproduce the previous scalar algorithm's entire physical state.
 */
final class LongArrayDequeRingSpanTest {
    private static final Field ELEMENTS = field("elements");
    private static final Field HEAD = field("head");
    private static final Field SIZE = field("size");
    private static final Field EPOCH = field("modCount");

    private static Field field(String name) {
        try {
            Field result = LongArrayDeque.class.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static int integer(Field field, LongArrayDeque deque) throws Exception {
        return field.getInt(deque);
    }

    private static long[] lane(LongArrayDeque deque) throws Exception {
        return ((long[]) ELEMENTS.get(deque)).clone();
    }

    private static long value(int index) {
        return switch (index) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> 0x9e3779b97f4a7c15L * index;
        };
    }

    private static LongArrayDeque ring(int capacity, int head, int count) throws Exception {
        LongArrayDeque deque = new LongArrayDeque(capacity - 1);
        assertEquals(capacity, deque.capacity());
        for (int i = 0; i < head; i++) {
            deque.addLast(-77L);
            deque.removeFirstLong();
        }
        for (int i = 0; i < count; i++) {
            deque.addLast(value(i));
        }
        assertEquals(head, integer(HEAD, deque));
        return deque;
    }

    private static final class ScalarReference {
        final long[] slots;
        int head;
        int size;

        ScalarReference(LongArrayDeque deque) throws Exception {
            slots = lane(deque);
            head = integer(HEAD, deque);
            size = integer(SIZE, deque);
        }

        private int at(int logical) {
            return (head + logical) & (slots.length - 1);
        }

        void remove(int logical) {
            if (logical < (size >> 1)) {
                for (int i = logical; i > 0; i--) {
                    slots[at(i)] = slots[at(i - 1)];
                }
                slots[head] = 0L;
                head = (head + 1) & (slots.length - 1);
            } else {
                for (int i = logical; i < size - 1; i++) {
                    slots[at(i)] = slots[at(i + 1)];
                }
                slots[at(size - 1)] = 0L;
            }
            size--;
        }

        void clear() {
            if (size == 0) {
                return;
            }
            for (int i = 0; i < size; i++) {
                slots[at(i)] = 0L;
            }
            head = 0;
            size = 0;
        }

        long[] logical() {
            long[] result = new long[size];
            for (int i = 0; i < size; i++) {
                result[i] = slots[at(i)];
            }
            return result;
        }
    }

    private static void same(ScalarReference expected, LongArrayDeque actual,
            int expectedEpoch, String label) throws Exception {
        assertEquals(expected.head, integer(HEAD, actual), label + " head");
        assertEquals(expected.size, integer(SIZE, actual), label + " size");
        assertEquals(expectedEpoch, integer(EPOCH, actual), label + " epoch");
        assertArrayEquals(expected.slots, lane(actual), label + " physical slots");
        assertArrayEquals(expected.logical(), actual.toArray(), label + " encounter order");
    }

    @Test
    void occurrenceRemovalRetainsExactPhysicalRingAndIteratorEpoch() throws Exception {
        long trials = 0;
        for (int capacity : new int[]{8, 16, 32, 64}) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 1; size <= capacity; size++) {
                    for (int requested = 0; requested < size; requested += size <= 12 ? 1 : Math.max(1, size / 7)) {
                        for (boolean fromFront : new boolean[]{true, false}) {
                            LongArrayDeque deque = ring(capacity, head, size);
                            ScalarReference baseline = new ScalarReference(deque);
                            int beforeEpoch = integer(EPOCH, deque);
                            LongIterator ascending = deque.iterator();
                            LongIterator descending = deque.descendingIterator();
                            long key = value(requested);
                            assertTrue(fromFront ? deque.removeFirstOccurrence(key)
                                    : deque.removeLastOccurrence(key));
                            baseline.remove(requested);
                            same(baseline, deque, beforeEpoch + 1, "remove " + capacity + "/" + head
                                    + "/" + size + "/" + requested + "/" + fromFront);
                            assertThrows(ConcurrentModificationException.class, ascending::hasNext);
                            assertThrows(ConcurrentModificationException.class, descending::nextLong);
                            trials++;
                        }
                    }
                }
            }
        }
        assertTrue(trials >= 40_000, "large ring geometry and both removal directions");
    }

    @Test
    void clearUsesTwoSpansButMatchesOldSlotZeroing() throws Exception {
        long trials = 0;
        for (int capacity : new int[]{8, 16, 32, 64, 128}) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 0; size <= capacity; size++) {
                    LongArrayDeque deque = ring(capacity, head, size);
                    ScalarReference baseline = new ScalarReference(deque);
                    int originalEpoch = integer(EPOCH, deque);
                    LongIterator cursor = deque.iterator();
                    baseline.clear();
                    deque.clear();
                    same(baseline, deque, originalEpoch + (size == 0 ? 0 : 1),
                            "clear " + capacity + "/" + head + "/" + size);
                    if (size == 0) {
                        assertFalse(cursor.hasNext());
                    } else {
                        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
                    }
                    trials++;
                }
            }
        }
        assertTrue(trials >= 15_000, "empty, full and wrapped capacity spectrum");
    }

    private static LongArrayDeque repeatedRing(int capacity, int head, int count)
            throws Exception {
        LongArrayDeque deque = new LongArrayDeque(capacity - 1);
        for (int i = 0; i < head; i++) {
            deque.addLast(-77L);
            deque.removeFirstLong();
        }
        for (int i = 0; i < count; i++) {
            long repeated = switch (i & 7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0L;
                default -> i % 7 - 3L;
            };
            deque.addLast(repeated);
        }
        return deque;
    }

    private static int scalarMatch(ScalarReference ref, long value, boolean last) {
        if (last) {
            for (int i = ref.size - 1; i >= 0; i--) {
                if (ref.slots[ref.at(i)] == value) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < ref.size; i++) {
                if (ref.slots[ref.at(i)] == value) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Test
    void twoSpanSearchAndDuplicateRemovalPreserveFirstLastAndAbsentSemantics()
            throws Exception {
        long cases = 0;
        long[] targets = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -4L, -1L, 1L, 4L, 999L};
        for (int capacity : new int[]{8, 16, 32, 64}) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 0; size <= capacity; size++) {
                    for (long target : targets) {
                        for (boolean last : new boolean[]{false, true}) {
                            LongArrayDeque deque = repeatedRing(capacity, head, size);
                            ScalarReference baseline = new ScalarReference(deque);
                            int beforeEpoch = integer(EPOCH, deque);
                            int match = scalarMatch(baseline, target, last);
                            assertEquals(match >= 0, deque.contains(target));
                            assertEquals(beforeEpoch, integer(EPOCH, deque),
                                    "contains must never mutate");
                            assertEquals(match >= 0, last
                                    ? deque.removeLastOccurrence(target)
                                    : deque.removeFirstOccurrence(target));
                            if (match >= 0) {
                                baseline.remove(match);
                            }
                            same(baseline, deque, beforeEpoch + (match >= 0 ? 1 : 0),
                                    "duplicate scan " + capacity + "/" + head + "/" + size);
                            cases++;
                        }
                    }
                }
            }
        }
        assertTrue(cases >= 80_000, "all wrapped logical match directions");
    }

    @Test
    void randomizedMixedOperationsMatchArrayDequeAndPreserveNoOpEpochs() throws Exception {
        Random random = new Random(0x4D334C4F4E47L);
        LongArrayDeque actual = new LongArrayDeque();
        ArrayDeque<Long> expected = new ArrayDeque<>();
        for (int step = 0; step < 30_000; step++) {
            long candidate = switch (step & 7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0;
                default -> random.nextInt(61) - 30L;
            };
            int kind = random.nextInt(8);
            if (expected.size() > 128) {
                kind = 7;
            }
            switch (kind) {
                case 0 -> { actual.addFirst(candidate); expected.addFirst(candidate); }
                case 1 -> { actual.addLast(candidate); expected.addLast(candidate); }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.removeFirst().longValue(), actual.removeFirstLong());
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.removeLast().longValue(), actual.removeLastLong());
                    }
                }
                case 4, 5 -> {
                    int epoch = integer(EPOCH, actual);
                    boolean real = kind == 4
                            ? actual.removeFirstOccurrence(candidate)
                            : actual.removeLastOccurrence(candidate);
                    boolean model = kind == 4
                            ? expected.removeFirstOccurrence(candidate)
                            : expected.removeLastOccurrence(candidate);
                    assertEquals(model, real);
                    assertEquals(epoch + (model ? 1 : 0), integer(EPOCH, actual));
                }
                case 6 -> {
                    if (!expected.isEmpty()) {
                        long member = expected.toArray(new Long[0])[random.nextInt(expected.size())];
                        assertEquals(expected.removeLastOccurrence(member),
                                actual.removeLastOccurrence(member));
                    }
                }
                default -> { actual.clear(); expected.clear(); }
            }
            long[] oracle = expected.stream().mapToLong(Long::longValue).toArray();
            assertArrayEquals(oracle, actual.toArray(), "random step " + step);
        }
    }
}
