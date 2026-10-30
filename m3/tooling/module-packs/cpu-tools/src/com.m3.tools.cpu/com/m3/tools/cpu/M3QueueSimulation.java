// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Bounded deterministic FIFO multi-server service simulator using an indexed heap.
 * Not a distributed job scheduler, wall-clock timer, or general discrete-event model.
 */
public final class M3QueueSimulation {
    private M3QueueSimulation() { }
    public record Result(long[] started, long[] finished, int[] worker, long totalWait,
                         long makespan) {
        public Result {
            started = started.clone(); finished = finished.clone(); worker = worker.clone();
            if (started.length != finished.length || started.length != worker.length)
                throw new IllegalArgumentException("result geometry");
        }
        @Override public long[] started() { return started.clone(); }
        @Override public long[] finished() { return finished.clone(); }
        @Override public int[] worker() { return worker.clone(); }
    }

    /** Arrivals nonnegative and nondecreasing; service durations strictly positive. */
    public static Result fifo(long[] arrivals, long[] duration, int workers) {
        Objects.requireNonNull(arrivals); Objects.requireNonNull(duration);
        if (workers < 1 || workers > 65536 || arrivals.length != duration.length
                || arrivals.length > 1_000_000)
            throw new IllegalArgumentException("queue geometry");
        long[] available = new long[workers];
        int[] heap = new int[workers];
        for (int i = 0; i < workers; i++) heap[i] = i;
        long[] start = new long[arrivals.length], end = new long[arrivals.length];
        int[] selected = new int[arrivals.length];
        long waiting = 0, makespan = 0;
        for (int job = 0; job < arrivals.length; job++) {
            if (arrivals[job] < 0 || duration[job] <= 0
                    || (job > 0 && arrivals[job] < arrivals[job - 1]))
                throw new IllegalArgumentException("unsorted arrivals or invalid duration");
            int worker = heap[0];
            long at = Math.max(arrivals[job], available[worker]);
            long done = Math.addExact(at, duration[job]);
            waiting = Math.addExact(waiting, Math.subtractExact(at, arrivals[job]));
            start[job] = at;
            end[job] = done;
            selected[job] = worker;
            available[worker] = done;
            makespan = Math.max(makespan, done);
            siftDown(heap, available);
        }
        return new Result(start, end, selected, waiting, makespan);
    }

    private static boolean before(int a, int b, long[] available) {
        return available[a] < available[b]
                || (available[a] == available[b] && a < b);
    }
    private static void siftDown(int[] heap, long[] available) {
        int size = heap.length, vertex = heap[0], at = 0;
        while (2 * at + 1 < size) {
            int child = 2 * at + 1;
            if (child + 1 < size && before(heap[child + 1], heap[child], available)) child++;
            if (!before(heap[child], vertex, available)) break;
            heap[at] = heap[child];
            at = child;
        }
        heap[at] = vertex;
    }
}
