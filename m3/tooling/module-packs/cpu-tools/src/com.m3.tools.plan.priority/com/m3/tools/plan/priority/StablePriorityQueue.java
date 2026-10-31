// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: mapped receiver with packed primitive storage; see DONOR_CHANGES.tsv.
package com.m3.tools.plan.priority;

import java.util.Arrays;
import java.util.Objects;

/** Stable score queue; caller values occupy parallel lanes with no per-value structural wrapper. */
public final class StablePriorityQueue<T> {
    private Object[] values = new Object[0];
    private double[] scores = new double[0];
    private long[] sequences = new long[0];
    private int size;
    private long next;

    public StablePriorityQueue() { }

    /** Rejects null/nonfinite scores; equal scores retain offer order, including duplicate values. */
    public void offer(T item, double score) {
        Objects.requireNonNull(item);
        if (!Double.isFinite(score)) throw new IllegalArgumentException("nonfinite score");
        if (next == Long.MAX_VALUE) throw new IllegalStateException("sequence exhausted");
        grow();
        long sequence = next++;
        int at = size++;
        while (at > 0) {
            int parent = (at - 1) >>> 1;
            if (!before(score, sequence, scores[parent], sequences[parent])) break;
            copy(parent, at); at = parent;
        }
        put(at, item, score, sequence);
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0) return null;
        T result = (T) values[0];
        int last = --size;
        Object item = values[last]; double score = scores[last]; long sequence = sequences[last];
        values[last] = null;
        if (size > 0) {
            int at = 0, half = size >>> 1;
            while (at < half) {
                int child = 2 * at + 1;
                if (child + 1 < size && before(scores[child + 1], sequences[child + 1],
                        scores[child], sequences[child])) child++;
                if (!before(scores[child], sequences[child], score, sequence)) break;
                copy(child, at); at = child;
            }
            put(at, item, score, sequence);
        }
        return result;
    }

    public int size() { return size; }

    private static boolean before(double leftScore, long leftSequence, double rightScore, long rightSequence) {
        int order = Double.compare(leftScore, rightScore);
        return order < 0 || (order == 0 && leftSequence < rightSequence);
    }

    private void copy(int from, int to) {
        put(to, values[from], scores[from], sequences[from]);
    }

    private void put(int at, Object value, double score, long sequence) {
        values[at] = value; scores[at] = score; sequences[at] = sequence;
    }

    private void grow() {
        if (size < values.length) return;
        long requested = Math.max(8L, (long) size + (size >>> 1) + 1L);
        if (requested > Integer.MAX_VALUE - 8L) throw new OutOfMemoryError("queue capacity");
        int capacity = (int) requested;
        Object[] newValues = Arrays.copyOf(values, capacity);
        double[] newScores = Arrays.copyOf(scores, capacity);
        long[] newSequences = Arrays.copyOf(sequences, capacity);
        values = newValues; scores = newScores; sequences = newSequences;
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test");
        var queue = new StablePriorityQueue<String>();
        queue.offer("late", 3); queue.offer("first", 1); queue.offer("second", 1);
        if (!"first".equals(queue.poll()) || !"second".equals(queue.poll()) || !"late".equals(queue.poll()))
            throw new AssertionError("stable order");
        System.out.println("M3_JMOD_TEST_PASS plan.priority");
    }
}
