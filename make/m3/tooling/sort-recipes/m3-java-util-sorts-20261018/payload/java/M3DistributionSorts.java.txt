/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

/** Numeric distribution of primitive indices over immutable invocation keys. */
final class M3DistributionSorts {
    private static final int RADIX = 256;

    private M3DistributionSorts() { }

    static void sort(int[] order, long[] keys, int base,
                     SortAlgorithm algorithm, M3SortContext context) {
        if (order.length < 2) return;
        switch (algorithm) {
            case COUNTING -> counting(order, keys, base, context);
            case PIGEONHOLE -> pigeonhole(order, keys, base, context);
            case BUCKET -> bucket(order, keys, base, context);
            case LSD_RADIX -> lsd(order, keys, base, context);
            case MSD_RADIX -> msd(order, new int[order.length], keys, base,
                                 0, order.length, 56, context);
            case AMERICAN_FLAG -> americanFlag(order, keys, base,
                                               0, order.length, 56, context);
            default -> throw new IllegalArgumentException("Not a numeric algorithm: " + algorithm);
        }
    }

    private static long minimum(long[] keys, M3SortContext context) {
        long min = keys[0];
        for (long key : keys) {
            if (key < min) min = key;
            context.step();
        }
        return min;
    }

    private static long maximum(long[] keys, M3SortContext context) {
        long max = keys[0];
        for (long key : keys) {
            if (key > max) max = key;
            context.step();
        }
        return max;
    }

    private static int histogramSpan(long min, long max) {
        long distance = max - min;
        if (distance < 0 || distance >= Sorts.MAX_HISTOGRAM_SPAN) {
            throw new IllegalArgumentException("Numeric key span exceeds "
                    + Sorts.MAX_HISTOGRAM_SPAN + "; choose a radix or comparison algorithm");
        }
        return (int) distance + 1;
    }

    private static void counting(int[] order, long[] keys, int base,
                                 M3SortContext context) {
        long min = minimum(keys, context);
        int[] counts = new int[histogramSpan(min, maximum(keys, context))];
        for (int index : order) {
            counts[(int) (keys[index - base] - min)]++;
            context.step();
        }
        int sum = 0;
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            counts[i] = sum;
            sum += count;
            context.step();
        }
        int[] sorted = new int[order.length];
        for (int index : order) {
            sorted[counts[(int) (keys[index - base] - min)]++] = index;
            context.step();
        }
        System.arraycopy(sorted, 0, order, 0, order.length);
    }

    private static void pigeonhole(int[] order, long[] keys, int base,
                                   M3SortContext context) {
        long min = minimum(keys, context);
        int span = histogramSpan(min, maximum(keys, context));
        int[] heads = new int[span];
        int[] tails = new int[span];
        int[] next = new int[order.length];
        Arrays.fill(heads, -1);
        Arrays.fill(next, -1);
        int[] input = order.clone();
        for (int position = 0; position < input.length; position++) {
            int hole = (int) (keys[input[position] - base] - min);
            if (heads[hole] < 0) heads[hole] = position;
            else next[tails[hole]] = position;
            tails[hole] = position;
            context.step();
        }
        int out = 0;
        for (int head : heads) {
            for (int position = head; position >= 0; position = next[position]) {
                order[out++] = input[position];
                context.step();
            }
            context.step();
        }
    }

    private static void bucket(int[] order, long[] keys, int base,
                               M3SortContext context) {
        long min = minimum(keys, context);
        long distance = maximum(keys, context) - min;
        // Unsigned subtraction correctly represents even the full signed-long span.
        int shift = Math.max(0, 64 - Long.numberOfLeadingZeros(distance) - 8);
        int[] counts = new int[RADIX];
        for (int index : order) {
            counts[(int) ((keys[index - base] - min) >>> shift)]++;
            context.step();
        }
        int[] starts = boundaries(counts, 0, context);
        int[] next = Arrays.copyOf(starts, RADIX);
        int[] sorted = new int[order.length];
        for (int index : order) {
            sorted[next[(int) ((keys[index - base] - min) >>> shift)]++] = index;
            context.step();
        }
        for (int b = 0; b < RADIX; b++) {
            for (int i = starts[b] + 1; i < starts[b + 1]; i++) {
                int value = sorted[i];
                int j = i;
                while (j > starts[b] && context.compare(value, sorted[j - 1]) < 0) {
                    sorted[j] = sorted[j - 1];
                    j--;
                }
                sorted[j] = value;
            }
            context.step();
        }
        System.arraycopy(sorted, 0, order, 0, order.length);
    }

    private static int digit(int index, long[] keys, int base, int shift) {
        return (int) (((keys[index - base] ^ Long.MIN_VALUE) >>> shift) & 255L);
    }

    private static int[] boundaries(int[] counts, int from, M3SortContext context) {
        int[] starts = new int[RADIX + 1];
        starts[0] = from;
        for (int b = 0; b < RADIX; b++) {
            starts[b + 1] = starts[b] + counts[b];
            context.step();
        }
        return starts;
    }

    private static int[] histogram(int[] order, long[] keys, int base,
                                    int from, int to, int shift, M3SortContext context) {
        int[] counts = new int[RADIX];
        for (int i = from; i < to; i++) {
            counts[digit(order[i], keys, base, shift)]++;
            context.step();
        }
        return counts;
    }

    private static void lsd(int[] order, long[] keys, int base, M3SortContext context) {
        int[] source = order;
        int[] destination = new int[order.length];
        for (int shift = 0; shift < 64; shift += 8) {
            int[] counts = histogram(source, keys, base, 0, source.length, shift, context);
            int[] next = boundaries(counts, 0, context);
            for (int index : source) {
                destination[next[digit(index, keys, base, shift)]++] = index;
                context.step();
            }
            int[] swap = source;
            source = destination;
            destination = swap;
        }
        // Eight passes finish in order; keep this guard if the digit width evolves.
        if (source != order) System.arraycopy(source, 0, order, 0, order.length);
    }

    private static void msd(int[] order, int[] scratch, long[] keys, int base,
                            int from, int to, int shift, M3SortContext context) {
        int[] starts = boundaries(histogram(order, keys, base, from, to, shift, context),
                                  from, context);
        int[] next = Arrays.copyOf(starts, RADIX);
        for (int i = from; i < to; i++) {
            int index = order[i];
            scratch[next[digit(index, keys, base, shift)]++] = index;
            context.step();
        }
        System.arraycopy(scratch, from, order, from, to - from);
        if (shift == 0) return;
        for (int b = 0; b < RADIX; b++) {
            if (starts[b + 1] - starts[b] > 1) {
                msd(order, scratch, keys, base, starts[b], starts[b + 1], shift - 8, context);
            }
            context.step();
        }
    }

    private static void americanFlag(int[] order, long[] keys, int base,
                                     int from, int to, int shift, M3SortContext context) {
        int[] starts = boundaries(histogram(order, keys, base, from, to, shift, context),
                                  from, context);
        int[] next = Arrays.copyOf(starts, RADIX);
        for (int bucket = 0; bucket < RADIX; bucket++) {
            while (next[bucket] < starts[bucket + 1]) {
                int position = next[bucket];
                int target = digit(order[position], keys, base, shift);
                if (target == bucket) {
                    next[bucket]++;
                } else {
                    int displaced = order[next[target]];
                    order[next[target]++] = order[position];
                    order[position] = displaced;
                }
                context.step();
            }
        }
        if (shift == 0) return;
        for (int b = 0; b < RADIX; b++) {
            if (starts[b + 1] - starts[b] > 1) {
                americanFlag(order, keys, base, starts[b], starts[b + 1], shift - 8, context);
            }
            context.step();
        }
    }
}
