// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Counterexamples against every physical FIFO owner, preserving each native contract. */
final class M3QueueNativeBehaviorTest {
    private static List<Supplier<M3LongQueue>> factories() {
        return List.of(() -> new M3LongArrayDeque(7));
    }
    private static M3LongQueue wrap(M3LongQueue owner) {
        return new M3ForwardingLongQueue() {
            @Override protected M3LongQueue delegate() { return owner; }
        };
    }
    private static void fill(M3LongQueue queue, long... values) {
        for (long value : values) assertTrue(queue.offer(value));
    }
    @Test void emptyInvalidAndWrappedPartialBatchesRetainNativeEffects() {
        for (var factory : factories()) {
            M3LongQueue owner = factory.get(), raw = factory.get(), queue = wrap(owner);
            long[] target = {77, 77, 77, 77, 77};
            assertFalse(queue.tryPoll(target, 2));
            assertArrayEquals(new long[] {77, 77, 77, 77, 77}, target);
            assertThrows(IndexOutOfBoundsException.class, () -> queue.drainTo(target, -1, 0));
            assertThrows(NullPointerException.class, () -> queue.offerBatch(null, 0, 0));
            assertTrue(queue.isEmpty());
            fill(queue, 0, 1, 2, 3, 4, 5); fill(raw, 0, 1, 2, 3, 4, 5);
            long[] scratch = new long[4];
            assertEquals(4, queue.drainTo(scratch, 0, 4));
            assertArrayEquals(new long[] {0, 1, 2, 3}, scratch);
            assertEquals(4, raw.drainTo(new long[4], 0, 4));
            fill(queue, 6, 7, 8, 9); fill(raw, 6, 7, 8, 9);
            long[] input = {55, Long.MIN_VALUE, -1, 0, Long.MAX_VALUE, 66};
            int accepted = queue.offerBatch(input, 1, 4);
            assertEquals(raw.offerBatch(input, 1, 4), accepted);
            assertTrue(accepted == 2 || accepted == 4, "bounded prefix or growing owner");
            assertArrayEquals(raw.toArray(), owner.toArray());
            input[1] = 44; assertArrayEquals(raw.toArray(), owner.toArray());
            assertEquals(Long.MIN_VALUE, owner.toArray()[6], "bulk input must be copied");
            long[] before = owner.toArray();
            assertThrows(IndexOutOfBoundsException.class, () -> queue.tryPoll(target, 5));
            assertThrows(IndexOutOfBoundsException.class, () -> queue.drainTo(target, 4, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> queue.offerBatch(input, 4, 3));
            assertArrayEquals(before, owner.toArray());
            assertEquals(raw.pollReserved(), queue.pollReserved());
            assertEquals(3, queue.drainTo(target, 1, 3));
            assertEquals(3, raw.drainTo(new long[3], 0, 3));
            assertArrayEquals(new long[] {77, 5, 6, 7, 77}, target);
            assertArrayEquals(raw.toArray(), owner.toArray());
            queue.clear(); assertTrue(owner.isEmpty());
        }
    }
    @Test void positiveTryPollWritesOnlyItsSlotAndEmptyPollLeavesItIntact() {
        for (var factory : factories()) {
            M3LongQueue owner = factory.get(), queue = wrap(owner);
            fill(owner, Long.MIN_VALUE, Long.MAX_VALUE);
            long[] target = {77, 77, 77};
            assertTrue(queue.tryPoll(target, 1));
            assertArrayEquals(new long[] {77, Long.MIN_VALUE, 77}, target);
            assertArrayEquals(new long[] {Long.MAX_VALUE}, owner.toArray());
            assertThrows(NullPointerException.class, () -> queue.tryPoll(null, 0));
            assertArrayEquals(new long[] {Long.MAX_VALUE}, owner.toArray());
            assertTrue(queue.tryPoll(target, 1));
            assertArrayEquals(new long[] {77, Long.MAX_VALUE, 77}, target);
            assertFalse(queue.tryPoll(target, 1));
            assertArrayEquals(new long[] {77, Long.MAX_VALUE, 77}, target);
        }
    }
    private static String consume(M3LongIterator iterator) {
        try {
            var values = new ArrayList<Long>();
            while (iterator.hasNext()) values.add(iterator.nextLong());
            return values.toString();
        } catch (RuntimeException failure) {
            return failure.getClass().getName();
        }
    }
    @Test void traversalAfterMutationPreservesEachOwnersNativeRules() {
        for (var factory : factories()) {
            M3LongQueue owner = factory.get(), raw = factory.get();
            fill(owner, Long.MIN_VALUE, 0, Long.MAX_VALUE); fill(raw, Long.MIN_VALUE, 0, Long.MAX_VALUE);
            var iterator = wrap(owner).iterator(); var expected = raw.iterator();
            owner.offer(7); raw.offer(7);
            assertEquals(consume(expected), consume(iterator));
            assertArrayEquals(raw.parallelLongStream().toArray(), wrap(owner).parallelLongStream().toArray());
        }
    }
    @Test void readonlyRejectsAllSevenRoutesAcrossEveryOwnerBeforeArrayWrites() {
        for (var factory : factories()) {
            M3LongQueue owner = factory.get(); fill(owner, 1, 2, 3);
            var readonly = new M3ReadOnlyLongQueue(owner);
            long[] target = {77, 77}; long[] before = owner.toArray();
            assertThrows(UnsupportedOperationException.class, () -> readonly.offer(9));
            assertThrows(UnsupportedOperationException.class, readonly::poll);
            assertThrows(UnsupportedOperationException.class, readonly::pollReserved);
            assertThrows(UnsupportedOperationException.class, () -> readonly.tryPoll(target, 0));
            assertThrows(UnsupportedOperationException.class, () -> readonly.offerBatch(new long[0], 0, 0));
            assertThrows(UnsupportedOperationException.class, () -> readonly.drainTo(target, 0, 0));
            assertThrows(UnsupportedOperationException.class, readonly::clear);
            assertArrayEquals(before, owner.toArray()); assertArrayEquals(new long[] {77, 77}, target);
            owner.poll(); owner.offer(4);
            assertArrayEquals(owner.toArray(), readonly.toArray());
        }
    }
}
