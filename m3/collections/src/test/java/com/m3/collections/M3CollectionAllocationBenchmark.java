/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the source-sealed Synexia M3 collection allocation recipe.
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.function.Predicate;

/** Local workload probe, not a substitute for JMH or target-platform acceptance. */
public final class M3CollectionAllocationBenchmark {
    private static volatile Object observed;
    private static volatile long consumed;
    private M3CollectionAllocationBenchmark() { }

    public static void main(String[] args) {
        String workload = args[0];
        int n = Integer.parseInt(args[1]);
        int rounds = Integer.parseInt(args[2]);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter required");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        M3ArrayList<Integer> source = new M3ArrayList<>(n);
        Object[] values = new Object[n];
        int[] slots = new int[n];
        byte[] flags = new byte[n];
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        for (int i = 0; i < n; i++) {
            source.add(i); values[i] = i; slots[i] = i; bag.addOccurrences(i, 1);
        }
        var snapshot = M3ObjectIndexSnapshot.<Integer>capture(values, slots, flags);
        var counts = bag.sortedCounts(null);
        String selection = workload.substring(workload.indexOf('-') + 1);
        Predicate<Integer> reject = switch (selection) {
            case "none" -> value -> false;
            case "first" -> value -> value == 0;
            case "tail" -> value -> value == n - 1;
            case "alternate" -> value -> (value & 1) == 0;
            case "all" -> value -> true;
            default -> value -> false;
        };
        Predicate<Integer> keep = reject.negate();
        int batchSize = 128;
        for (int round = -80; round < rounds; round++) {
            @SuppressWarnings("unchecked")
            M3ArrayList<Integer>[] owners = (M3ArrayList<Integer>[]) new M3ArrayList<?>[batchSize];
            if (workload.startsWith("remove-")) {
                for (int i = 0; i < batchSize; i++) owners[i] = new M3ArrayList<>(source);
            } else if (workload.equals("bulk-add")) {
                for (int i = 0; i < batchSize; i++) {
                    owners[i] = new M3ArrayList<>(n + 2); owners[i].add(-1);
                }
            }
            long bytes = bean.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            long sum = 0;
            for (int i = 0; i < batchSize; i++) {
                if (workload.startsWith("remove-")) {
                    owners[i].removeIf(reject); observed = owners[i]; sum += owners[i].size();
                } else if (workload.startsWith("snapshot-")) {
                    var result = snapshot.filtered(keep); observed = result; sum += result.size();
                } else if (workload.equals("counted-none")) {
                    var result = counts.filtered(keep); observed = result; sum += result.distinctSize();
                } else if (workload.equals("bulk-copy")) {
                    var result = new M3ArrayList<>(source); observed = result; sum += result.size();
                } else if (workload.equals("bulk-add")) {
                    owners[i].addAll(1, source); observed = owners[i]; sum += owners[i].size();
                } else throw new IllegalArgumentException(workload);
            }
            long elapsed = System.nanoTime() - start;
            long allocated = bean.getThreadAllocatedBytes(thread) - bytes;
            consumed = sum;
            if (round >= 0) System.out.printf(Locale.ROOT, "%s,%d,%d,%.3f,%.3f,%d%n",
                    workload, n, round, (double) elapsed / batchSize, (double) allocated / batchSize, sum);
        }
    }
}
