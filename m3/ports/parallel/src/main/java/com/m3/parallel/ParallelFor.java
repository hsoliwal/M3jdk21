/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * Parallel loop over {@code [0, size)} with dynamic chunking: workers on the common {@link ForkJoinPool} claim
 * fixed-size chunks from a shared counter until the range is exhausted, so faster cores (P-cores) take more chunks
 * than slower ones (E-cores). A failing work-item stops further chunk claims and its exception propagates.
 */
final class ParallelFor {
    /** Target number of chunks per worker; more chunks balance better, fewer chunks claim less often. */
    static final int CHUNKS_PER_WORKER = 8;
    private static final long EXHAUSTED = Long.MAX_VALUE / 2;

    private ParallelFor() {
    }

    static void run(int size, IntConsumer body) {
        if (size <= 0) {
            return;
        }
        int workers = workers(size);
        if (workers == 1) {
            IntStream.range(0, size).forEach(body);
            return;
        }
        int chunk = chunkSize(size, workers);
        AtomicLong next = new AtomicLong();
        IntStream.range(0, workers).parallel().forEach(worker -> drain(next, size, chunk, body));
    }

    static int workers(int size) {
        return Math.max(1, Math.min(size, ForkJoinPool.getCommonPoolParallelism() + 1));
    }

    static int chunkSize(int size, int workers) {
        return Math.max(1, size / (workers * CHUNKS_PER_WORKER));
    }

    private static void drain(AtomicLong next, int size, int chunk, IntConsumer body) {
        try {
            for (long start = next.getAndAdd(chunk); start < size; start = next.getAndAdd(chunk)) {
                IntStream.range((int) start, (int) Math.min(size, start + chunk)).forEach(body);
            }
        } catch (RuntimeException | Error failure) {
            next.set(EXHAUSTED);
            throw failure;
        }
    }
}
