/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.apache.M3CircularFifoQueue;
import java.util.ArrayList;
import java.util.List;

public final class CircularFifoIndexWrapProof {
    private static long checks;
    private CircularFifoIndexWrapProof() { }

    public static void main(String[] args) {
        arithmetic();
        wrappedQueues();
        System.out.println("M3_CIRCULAR_FIFO_INDEX_WRAP_PASS checks=" + checks);
    }

    private static void arithmetic() {
        for (int capacity = 1; capacity <= 257; capacity++) {
            for (int start = 0; start < capacity; start++) {
                for (int index = 0; index < capacity; index++) {
                    equal(actual(start, index, capacity), expected(start, index, capacity),
                            "small coordinate");
                }
            }
        }
        int capacity = Integer.MAX_VALUE;
        for (int start : new int[] {0, 1, capacity / 2, capacity - 2, capacity - 1}) {
            for (int index : new int[] {0, 1, capacity / 2, capacity - 2, capacity - 1}) {
                equal(actual(start, index, capacity), expected(start, index, capacity),
                        "overflow boundary");
            }
        }
    }

    private static void wrappedQueues() {
        for (int capacity : new int[] {1, 2, 3, 5, 8, 17, 32}) {
            M3CircularFifoQueue<Integer> queue = new M3CircularFifoQueue<>(capacity);
            List<Integer> model = new ArrayList<>();
            for (int value = 0; value < capacity * 7 + 5; value++) {
                queue.add(value);
                if (model.size() == capacity) model.removeFirst();
                model.add(value);
                equal(queue.size(), model.size(), "size");
                for (int index = 0; index < model.size(); index++) {
                    equal(queue.get(index), model.get(index), "wrapped get");
                }
            }
        }
    }

    private static int actual(int start, int index, int capacity) {
        int remaining = capacity - start;
        return index < remaining ? start + index : index - remaining;
    }

    private static int expected(int start, int index, int capacity) {
        return (int) (((long) start + index) % capacity);
    }

    private static void equal(Object actual, Object expected, String label) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": " + actual + " != " + expected);
        }
    }
}
