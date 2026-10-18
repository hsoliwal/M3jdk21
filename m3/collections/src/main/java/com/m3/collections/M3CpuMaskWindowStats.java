/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: independent M3 CPU window reduction over the existing mask-path selector.
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Bounded, non-owning tumbling-window reductions over selected primitive row IDs.
 *
 * <p>Selection, logical-to-physical offsets, full 32-bit required/forbidden flags,
 * signed-stride ordering, and optional JNI are owned by {@link M3IndexPathMasks}.
 * This class only reduces its ordered output into occurrence-count windows; a
 * duplicate row ID still contributes once per selected occurrence. It allocates
 * a single reusable {@code int[]} scratch buffer of at most 4,096 elements,
 * never boxed per-row objects or a materialized selected result array.
 *
 * <p>Each full window contains {@code width} selected occurrences; the final
 * nonempty partial window is emitted once. Statistics are the signed row-ID sum,
 * minimum, and maximum, not state-mask or payload-value statistics. Row IDs
 * can name canonical M3String atoms in the caller's separate namespace, but
 * this CPU operator neither copies nor materializes text.
 *
 * <p>The arrays and masks are borrowed and must not be mutated during scanning.
 * Input validation and explicit native-backend availability occur before the
 * first callback. Physical offsets in later path segments can fail after earlier
 * callbacks; callbacks may also throw. This is not a transactional operation.
 * The selector may read up to one bounded scratch batch beyond the last emitted
 * window when {@code maxWindows} stops output early.
 */
public final class M3CpuMaskWindowStats {
    private M3CpuMaskWindowStats() { }

    private static final int[] NO_OUTPUT = new int[0];

    @FunctionalInterface
    public interface WindowConsumer {
        /** All positions count selected occurrences, not original physical slots. */
        void accept(long firstSelectedOrdinal, int occurrences, long sum, int min, int max);
    }

    /**
     * Streams at most {@code maxWindows} bounded tumbling windows in encounter order.
     * Windows are formed only from rows accepted by the existing M3 state-path selector.
     *
     * @return number of callbacks delivered; zero budget still validates the selector
     */
    public static int scan(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, int width, int maxWindows,
            M3IndexPathMasks.Backend backend, WindowConsumer consumer) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(consumer, "consumer");
        if (width < 1 || width > M3IndexPathMasks.MAX_WINDOW) {
            throw new IllegalArgumentException("window width must be in [1,4096]");
        }
        if (maxWindows < 0) throw new IllegalArgumentException("negative window budget");

        // Delegate source/JNI geometry admission, even for an empty result budget.
        M3IndexPathMasks.scanWindow(orderedRows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride, 0,
                NO_OUTPUT, 0, 0, backend);
        if (maxWindows == 0 || count == 0) return 0;

        // Bounded scratch sized to the task; never proportional to the full input.
        int[] scratch = new int[Math.min(M3IndexPathMasks.MAX_WINDOW,
                Math.max(64, width))];
        int next = 0;
        int emitted = 0;
        long accepted = 0L;
        int inWindow = 0;
        long sum = 0L;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        while (next < count) {
            long result = M3IndexPathMasks.scanWindow(orderedRows, physicalOffsets,
                    statePlanes, requiredMasks, forbiddenMasks, first, count,
                    stride, next, scratch, 0, scratch.length, backend);
            int following = M3IndexPathMasks.nextOrdinal(result);
            int written = M3IndexPathMasks.written(result);
            if (following <= next || following > count
                    || written < 0 || written > scratch.length) {
                throw new IllegalStateException("mask window made invalid progress");
            }
            for (int i = 0; i < written; i++) {
                int row = scratch[i];
                sum = Math.addExact(sum, (long) row);
                min = Math.min(min, row);
                max = Math.max(max, row);
                inWindow++;
                accepted++;
                if (inWindow == width) {
                    consumer.accept(accepted - inWindow, inWindow, sum, min, max);
                    emitted++;
                    if (emitted == maxWindows) return emitted;
                    inWindow = 0;
                    sum = 0L;
                    min = Integer.MAX_VALUE;
                    max = Integer.MIN_VALUE;
                }
            }
            next = following;
        }
        if (inWindow != 0 && emitted < maxWindows) {
            consumer.accept(accepted - inWindow, inWindow, sum, min, max);
            emitted++;
        }
        return emitted;
    }
}
