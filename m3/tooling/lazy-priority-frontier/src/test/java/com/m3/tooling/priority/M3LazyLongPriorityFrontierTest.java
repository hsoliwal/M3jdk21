// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.priority;

import java.util.Arrays;
import java.util.NoSuchElementException;

/** Dependency-free deterministic regression runner; execute with assertions enabled. */
public final class M3LazyLongPriorityFrontierTest {
    private M3LazyLongPriorityFrontierTest() { }

    public static void main(String[] args) {
        topPeekDoesNotRemoveAndPollRemovesOnlyTop();
        minAndMaxDrainMatchSortWithoutMutatingInput();
        boundedDrainLeavesUnrequestedValues();
        emptyAndInvalidInputsFailClosed();
        System.out.println("M3_LAZY_LONG_PRIORITY_FRONTIER_PASS cases=4");
    }

    private static void topPeekDoesNotRemoveAndPollRemovesOnlyTop() {
        long[] input = {3, 7, 3, 1, 9, 9, 2, Long.MIN_VALUE, Long.MAX_VALUE};
        long[] before = input.clone();
        M3LazyLongPriorityFrontier queue = M3LazyLongPriorityFrontier.maxHeap(input);
        require(Arrays.equals(input, before), "constructor mutated input");
        require(queue.peek() == Long.MAX_VALUE, "wrong max peek");
        require(queue.size() == input.length, "peek removed an item");
        require(queue.poll() == Long.MAX_VALUE, "poll did not remove max");
        require(queue.size() == input.length - 1, "poll removed wrong count");
    }

    private static void minAndMaxDrainMatchSortWithoutMutatingInput() {
        long[] input = new long[4096];
        for (int i = 0; i < input.length; i++) input[i] = ((long) i * 1103515245L + 12345L) & 1023L;
        input[0] = Long.MIN_VALUE;
        input[1] = Long.MAX_VALUE;
        long[] before = input.clone();
        long[] ascending = input.clone();
        Arrays.sort(ascending);
        M3LazyLongPriorityFrontier min = M3LazyLongPriorityFrontier.minHeap(input);
        for (long expected : ascending) require(min.poll() == expected, "min order mismatch");
        long[] descending = ascending.clone();
        for (int left = 0, right = descending.length - 1; left < right; left++, right--) {
            long value = descending[left]; descending[left] = descending[right]; descending[right] = value;
        }
        M3LazyLongPriorityFrontier max = M3LazyLongPriorityFrontier.maxHeap(input);
        for (long expected : descending) require(max.poll() == expected, "max order mismatch");
        require(Arrays.equals(input, before), "drain mutated input");
    }

    private static void boundedDrainLeavesUnrequestedValues() {
        M3LazyLongPriorityFrontier queue = M3LazyLongPriorityFrontier.maxHeap(new long[] {2, 9, 4, 7, 1});
        require(Arrays.equals(queue.pollTop(2), new long[] {9, 7}), "wrong top prefix");
        require(queue.size() == 3, "bounded drain consumed too many entries");
        require(queue.peek() == 4, "wrong remaining top");
        require(queue.pollTop(0).length == 0, "zero limit must not drain");
        require(queue.size() == 3, "zero limit changed state");
    }

    private static void emptyAndInvalidInputsFailClosed() {
        expect(NullPointerException.class, () -> M3LazyLongPriorityFrontier.maxHeap(null));
        expect(NoSuchElementException.class, () -> M3LazyLongPriorityFrontier.maxHeap(new long[0]).peek());
        expect(NoSuchElementException.class, () -> M3LazyLongPriorityFrontier.minHeap(new long[0]).poll());
        expect(IllegalArgumentException.class, () -> M3LazyLongPriorityFrontier.maxHeap(new long[] {1}).pollTop(-1));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("expected " + type.getName() + " but got " + actual, actual);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
