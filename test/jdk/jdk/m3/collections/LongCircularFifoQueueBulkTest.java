// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Target regression for primitive FIFO bulk-copy geometry, epochs and failed preflights.
 * Private epoch checks require the owning module to open jdk.m3.collections to this test.
 */
public final class LongCircularFifoQueueBulkTest {
    private static final Field ELEMENTS = field("elements");
    private static final Field HEAD = field("head");
    private static final Field SIZE = field("size");
    private static final Field MOD_COUNT = field("modCount");
    private static long checks;
    private static long cases;

    private static Field field(String name) {
        try {
            Field field = LongCircularFifoQueue.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static Set<Integer> distinct(int... values) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int value : values) result.add(value);
        return result;
    }

    private static LongCircularFifoQueue initial(int capacity, int head, int size,
            int epoch) throws Exception {
        LongCircularFifoQueue queue = new LongCircularFifoQueue(capacity);
        if (size != 0) {
            // Reach every selected legal head/size using the actual single-item API.
            int offers = capacity + (head + size) % capacity;
            for (int i = 0; i < offers; i++) queue.offer(i * 0x9e3779b97f4a7c15L);
            for (int i = size; i < capacity; i++) queue.poll();
            check(HEAD.getInt(queue) == head && queue.size() == size,
                    "initial reachable geometry");
        }
        // These epochs are reachable after enough mutations; avoid billions of setup calls.
        MOD_COUNT.setInt(queue, epoch);
        return queue;
    }

    private static void offerCase(int capacity, int head, int size, int length,
            int offset, int epoch) throws Exception {
        LongCircularFifoQueue queue = initial(capacity, head, size, epoch);
        long[] input = new long[offset + length + 4];
        for (int i = 0; i < input.length; i++) {
            input[i] = switch (i & 3) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0L;
                default -> -i;
            };
        }
        long[] original = input.clone();
        long[] physical = ((long[]) ELEMENTS.get(queue)).clone();
        int expectedHead = size == 0 ? 0 : head;
        int expectedSize = size;
        int expectedEpoch = epoch;
        if (length >= capacity) {
            for (int i = 0; i < capacity; i++) physical[i] = input[offset + length - capacity + i];
            expectedHead = 0;
            expectedSize = capacity;
            expectedEpoch++;
        } else {
            // Independent scalar model preserves all physical slots, including cold ones.
            for (int i = 0; i < length; i++) {
                if (expectedSize == capacity) {
                    physical[expectedHead] = input[offset + i];
                    expectedHead = (expectedHead + 1) % capacity;
                } else {
                    physical[(expectedHead + expectedSize) % capacity] = input[offset + i];
                    expectedSize++;
                }
                expectedEpoch++;
            }
        }
        LongIterator old = queue.iterator();
        check(queue.offerBatch(input, offset, length) == length, "accepted count");
        check(Arrays.equals(input, original), "source unchanged");
        check(Arrays.equals((long[]) ELEMENTS.get(queue), physical), "physical slots");
        check(HEAD.getInt(queue) == expectedHead, "head");
        check(SIZE.getInt(queue) == expectedSize, "size");
        check(MOD_COUNT.getInt(queue) == expectedEpoch, "exact epoch including wrap");
        long[] expected = new long[expectedSize];
        for (int i = 0; i < expected.length; i++) expected[i] = physical[(expectedHead + i) % capacity];
        long[] snapshot = queue.toArray();
        check(Arrays.equals(snapshot, expected), "logical order");
        if (snapshot.length > 0) {
            snapshot[0] ^= -1L;
            check(Arrays.equals(queue.toArray(), expected), "detached snapshot");
        }
        if (length == 0) {
            check(old.hasNext() == (size != 0), "no-op preserves iterator");
        } else {
            expect(ConcurrentModificationException.class, old::hasNext);
        }
        cases++;
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable exception) {
            if (type.isInstance(exception)) return;
            throw new AssertionError("expected " + type + " but got " + exception, exception);
        }
        throw new AssertionError("missing exception " + type);
    }

    private static void rejected(LongCircularFifoQueue queue, long[] source, int offset,
            int length, Class<? extends Throwable> type) throws Exception {
        long[] physical = ((long[]) ELEMENTS.get(queue)).clone();
        long[] input = source == null ? null : source.clone();
        int head = HEAD.getInt(queue), size = SIZE.getInt(queue), epoch = MOD_COUNT.getInt(queue);
        LongIterator old = queue.iterator();
        expect(type, () -> queue.offerBatch(source, offset, length));
        check(Arrays.equals(physical, (long[]) ELEMENTS.get(queue)), "rejection retains slots");
        check(HEAD.getInt(queue) == head && SIZE.getInt(queue) == size
                && MOD_COUNT.getInt(queue) == epoch, "rejection retains geometry/epoch");
        check(Arrays.equals(source, input), "rejection retains source");
        check(old.hasNext() == (size != 0), "rejection retains iterator");
        cases++;
    }

    @Test
    public void exhaustiveSmallRingGeometryAndEpochs() throws Exception {
        for (int capacity = 1; capacity <= 17; capacity++) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 0; size <= capacity; size++) {
                    if (size == 0 && head != 0) continue;
                    for (int length = 0; length <= capacity + 1; length++) {
                        for (int offset : new int[]{0, 3}) {
                            for (int epoch : new int[]{0, Integer.MAX_VALUE - 7,
                                    Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                                offerCase(capacity, head, size, length, offset, epoch);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void largerRingThresholdsAndFailedPreflights() throws Exception {
        for (int capacity : new int[]{31, 32, 33, 64, 65, 127, 128}) {
            for (int head : distinct(0, 1, capacity / 2, capacity - 1)) {
                for (int size : distinct(0, 1, capacity / 2, capacity - 1, capacity)) {
                    if (size == 0 && head != 0) continue;
                    for (int length : distinct(0, 1, 7, 8, 9, capacity - 1, capacity, capacity + 1)) {
                        offerCase(capacity, head, size, length, 3, Integer.MAX_VALUE);
                    }
                }
            }
        }
        for (int capacity : new int[]{1, 2, 7, 8, 9, 16, 17, 31, 32, 33, 64, 65, 127, 128}) {
            LongCircularFifoQueue queue = initial(capacity, capacity - 1, capacity,
                    Integer.MAX_VALUE);
            rejected(queue, null, 0, 0, NullPointerException.class);
            rejected(queue, null, -1, Integer.MAX_VALUE, NullPointerException.class);
            rejected(queue, new long[10], -1, 0, IndexOutOfBoundsException.class);
            rejected(queue, new long[10], 0, -1, IndexOutOfBoundsException.class);
            rejected(queue, new long[10], 11, 0, IndexOutOfBoundsException.class);
            rejected(queue, new long[10], 7, 4, IndexOutOfBoundsException.class);
            rejected(queue, new long[10], Integer.MAX_VALUE, 1, IndexOutOfBoundsException.class);
            rejected(queue, new long[10], 1, Integer.MAX_VALUE, IndexOutOfBoundsException.class);
            LongIterator iterator = queue.iterator();
            check(queue.offerBatch(new long[10], 10, 0) == 0, "end-offset empty window");
            check(iterator.hasNext() && MOD_COUNT.getInt(queue) == Integer.MAX_VALUE,
                    "end-offset empty window preserves epoch");
        }
    }

    public static void main(String[] args) throws Exception {
        LongCircularFifoQueueBulkTest test = new LongCircularFifoQueueBulkTest();
        test.exhaustiveSmallRingGeometryAndEpochs();
        test.largerRingThresholdsAndFailedPreflights();
        System.out.println("LONG_FIFO_BULK_TEST_PASS cases=" + cases + " checks=" + checks);
    }
}
