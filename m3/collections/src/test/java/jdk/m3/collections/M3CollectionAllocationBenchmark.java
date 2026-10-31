/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the source-sealed Synexia M3 collection allocation recipe.
 */
package jdk.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import jdk.m3.arrays.M3ArrayNative;

/**
 * Local workload probe, not a substitute for JMH or target-platform acceptance.
 * Storage rows report one whole Java operation and its Java thread-allocation bytes; native/process memory is not
 * measured. Deserialization includes stream construction, reading and closing. The native-cold mode observes the
 * first list construction and required provider loading in a fresh JVM, not an isolated native allocator call.
 */
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
        if (workload.equals("native-cold")) {
            if (n < 0 || rounds != 1) {
                throw new IllegalArgumentException("native-cold requires nonnegative capacity and one fresh-JVM round");
            }
            if (M3ArrayNative.isLoaded()) throw new IllegalStateException("native-cold requires an unloaded provider");
            long bytes = bean.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            M3ArrayList<Integer> result = new M3ArrayList<>(n);
            long elapsed = System.nanoTime() - start;
            long allocated = bean.getThreadAllocatedBytes(thread) - bytes;
            if (bytes < 0 || allocated < 0) throw new IllegalStateException("valid allocation counters required");
            if (!M3ArrayNative.isLoaded()) throw new IllegalStateException("native-cold did not load the required provider");
            observed = result;
            consumed = result.capacity();
            System.out.printf(Locale.ROOT, "%s,%d,%d,%.3f,%.3f,%d%n",
                    workload, n, 0, (double) elapsed, (double) allocated, consumed);
            return;
        }
        if (Set.of("allocate", "allocate-empty", "bulk-copy", "grow", "reserve-noop", "trim", "trim-noop",
                "clone", "object-toArray", "deserialize").contains(workload)) {
            storageWorkload(workload, n, rounds, bean, thread);
            return;
        }
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

    /** Existing round/batch protocol, with fresh mutable owners prepared before each measured round. */
    private static void storageWorkload(String workload, int n, int rounds,
            com.sun.management.ThreadMXBean bean, long thread) {
        if (n < 0 || n > Integer.MAX_VALUE - 17 || rounds <= 0) {
            throw new IllegalArgumentException("storage workloads require bounded nonnegative size and positive rounds");
        }
        if (workload.equals("allocate-empty") && n != 0) {
            throw new IllegalArgumentException("allocate-empty measures the zero-capacity default constructor");
        }
        M3ArrayList<Integer> source = new M3ArrayList<>(n);
        for (int value = 0; value < n; value++) source.add(value);
        Integer appended = Integer.valueOf(-1);
        byte[] serialized = workload.equals("deserialize") ? serializedSource(source) : null;
        int batchSize = 128;
        for (int round = -80; round < rounds; round++) {
            @SuppressWarnings("unchecked")
            M3ArrayList<Integer>[] owners = (M3ArrayList<Integer>[]) new M3ArrayList<?>[batchSize];
            if (workload.equals("grow") || workload.equals("trim-noop")) {
                for (int i = 0; i < batchSize; i++) owners[i] = new M3ArrayList<>(source);
            } else if (workload.equals("trim")) {
                for (int i = 0; i < batchSize; i++) {
                    owners[i] = new M3ArrayList<>(n + 17);
                    owners[i].addAll(source);
                }
            }
            long bytes = bean.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            long sum = 0;
            for (int i = 0; i < batchSize; i++) {
                if (workload.equals("allocate")) {
                    M3ArrayList<Integer> result = new M3ArrayList<>(n);
                    observed = result;
                    sum += result.capacity();
                } else if (workload.equals("allocate-empty")) {
                    M3ArrayList<Integer> result = new M3ArrayList<>();
                    observed = result;
                    sum += result.capacity();
                } else if (workload.equals("bulk-copy")) {
                    M3ArrayList<Integer> result = new M3ArrayList<>(source);
                    observed = result;
                    sum += result.size();
                } else if (workload.equals("grow")) {
                    owners[i].add(appended);
                    observed = owners[i];
                    sum += owners[i].size();
                } else if (workload.equals("reserve-noop")) {
                    source.ensureCapacity(n);
                    observed = source;
                    sum += source.size();
                } else if (workload.equals("trim") || workload.equals("trim-noop")) {
                    owners[i].trimToSize();
                    observed = owners[i];
                    sum += owners[i].capacity();
                } else if (workload.equals("clone")) {
                    M3ArrayList<Integer> result = source.clone();
                    observed = result;
                    sum += result.size();
                } else if (workload.equals("object-toArray")) {
                    Object[] result = source.toArray();
                    observed = result;
                    sum += result.length;
                } else if (workload.equals("deserialize")) {
                    // This is the complete deserialization operation, including stream setup and close.
                    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
                        Object result = input.readObject();
                        if (!(result instanceof M3ArrayList<?> list)) {
                            throw new IllegalStateException("serialized owner type changed");
                        }
                        observed = list;
                        sum += list.size();
                    } catch (IOException | ClassNotFoundException failure) {
                        throw new IllegalStateException("cannot deserialize the prepared owner", failure);
                    }
                } else {
                    throw new IllegalArgumentException(workload);
                }
            }
            long elapsed = System.nanoTime() - start;
            long allocated = bean.getThreadAllocatedBytes(thread) - bytes;
            if (bytes < 0 || allocated < 0) throw new IllegalStateException("valid allocation counters required");
            consumed = sum;
            if (round >= 0) System.out.printf(Locale.ROOT, "%s,%d,%d,%.3f,%.3f,%d%n",
                    workload, n, round, (double) elapsed / batchSize, (double) allocated / batchSize, sum);
        }
    }

    private static byte[] serializedSource(M3ArrayList<?> source) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(source);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot prepare the serialized owner", failure);
        }
        return bytes.toByteArray();
    }
}
