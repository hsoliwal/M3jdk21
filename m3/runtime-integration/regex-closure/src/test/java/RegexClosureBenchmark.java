// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

/** Isolated actual closure leaf; source preparation is outside warm operation samples. */
public final class RegexClosureBenchmark {
    private static volatile long sink;

    private RegexClosureBenchmark() {}

    private static long run(boolean after, long[] seeds, long stars, long[] rows, int rounds) {
        long result = 0;
        if (after) {
            for (int round = 0; round < rounds; round++) {
                for (long seed : seeds) result += AfterClose.apply(seed, stars);
            }
        } else {
            for (int round = 0; round < rounds; round++) {
                for (long seed : seeds) result += BeforeClose.apply(seed, rows);
            }
        }
        sink = result;
        return result;
    }

    private static long allocated() {
        var bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean allocation
                && allocation.isThreadAllocatedMemorySupported()
                && allocation.isThreadAllocatedMemoryEnabled()) {
            return allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());
        }
        return -1;
    }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("before") || args[0].equals("after"))) {
            throw new IllegalArgumentException("before or after");
        }
        boolean after = args[0].equals("after");
        Random random = new Random(0x4d3342454e4348L);
        for (String scenario : new String[]{"zero", "single", "sparse", "dense", "mixed"}) {
            long stars = scenario.equals("mixed") ? 0x6f7fff3fL : 0xffff_ffffL;
            long[] rows = RegexGraph.closures(RegexGraph.atoms(32, stars, 0));
            long[] seeds = new long[1024];
            for (int at = 0; at < seeds.length; at++) {
                long first = 1L << random.nextInt(33);
                seeds[at] = switch (scenario) {
                    case "zero" -> 0;
                    case "single" -> first;
                    case "sparse" -> first | (1L << random.nextInt(33));
                    default -> random.nextLong() & ((1L << 33) - 1);
                };
            }
            long begin = System.nanoTime();
            run(after, seeds, stars, rows, 1);
            long first = System.nanoTime() - begin;
            run(after, seeds, stars, rows, 500);
            long[] samples = new long[7];
            long allocationBefore = allocated();
            for (int sample = 0; sample < samples.length; sample++) {
                begin = System.nanoTime();
                run(after, seeds, stars, rows, 1000);
                samples[sample] = System.nanoTime() - begin;
            }
            long allocationAfter = allocated();
            Arrays.sort(samples);
            double operations = 1000.0 * seeds.length;
            System.out.println("M3_REGEX_CLOSURE_BENCH variant=" + args[0] + " scenario=" + scenario
                    + " first_batch_ns=" + first + " median_ns_per_op=" + samples[3] / operations
                    + " min_ns_per_op=" + samples[0] / operations
                    + " sampled_allocated_bytes=" + (allocationBefore < 0 || allocationAfter < 0
                            ? -1 : allocationAfter - allocationBefore)
                    + " sink=" + sink + " preparation_excluded=true");
        }
    }
}
