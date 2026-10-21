/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Derived from the first-party M3ArrayLookupCostProbe at M3jdk21@4b24824.
 * Changes: numeric copy workloads, both primitive widths, exact graph shallow sizes,
 * repeated CPU samples, and before/after process/direct-buffer memory observations.
 */
package com.m3.arrays;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Supplier;

/** Bounded resource observations; no global benchmark or VM-backend admission claim. */
public final class M3ArrayNumericCopyCostProbe {
    private static volatile long sink;
    private static final int DESCRIPTORS = 256;
    private static final int SEGMENT_LENGTH = 32;
    private static final int WARM_COPIES = 512;

    private M3ArrayNumericCopyCostProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !List.of("snapshot", "slice", "join", "nested").contains(args[0])) {
            throw new IllegalArgumentException("snapshot|slice|join|nested");
        }
        if (M3ArrayCopySizeAgent.instrumentation == null) throw new AssertionError("size agent required");
        String shape = args[0];
        int[] integers = new int[DESCRIPTORS * SEGMENT_LENGTH];
        long[] longs = new long[integers.length];
        for (int i = 0; i < integers.length; i++) {
            integers[i] = i * 31 ^ 0x52af011e;
            longs[i] = (long) integers[i] << 32 ^ i;
        }
        M3IntArrayView[] intSegments = new M3IntArrayView[DESCRIPTORS];
        M3LongArrayView[] longSegments = new M3LongArrayView[DESCRIPTORS];
        for (int i = 0; i < DESCRIPTORS; i++) {
            int from = i * SEGMENT_LENGTH;
            int to = from + SEGMENT_LENGTH;
            intSegments[i] = M3Arrays.ints(Arrays.copyOfRange(integers, from, to));
            longSegments[i] = M3Arrays.longs(Arrays.copyOfRange(longs, from, to));
        }
        Supplier<Views> factory = () -> switch (shape) {
            case "snapshot" -> new Views(M3Arrays.ints(integers), M3Arrays.longs(longs));
            case "slice" -> new Views(M3Arrays.ints(integers).slice(3, integers.length - 3),
                    M3Arrays.longs(longs).slice(3, longs.length - 3));
            case "join" -> new Views(M3Arrays.join(intSegments), M3Arrays.join(longSegments));
            case "nested" -> nested(intSegments, longSegments);
            default -> throw new AssertionError(shape);
        };
        Views view = factory.get();
        int[] intTarget = new int[view.ints.length() + 4];
        long[] longTarget = new long[view.longs.length() + 4];
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!threads.isThreadAllocatedMemorySupported() || !threads.isCurrentThreadCpuTimeSupported()) {
            throw new AssertionError("CPU/allocation counters required");
        }
        threads.setThreadAllocatedMemoryEnabled(true);
        threads.setThreadCpuTimeEnabled(true);
        long id = Thread.currentThread().threadId();
        for (int i = 0; i < 2_000; i++) sink += factory.get().ints.length();
        for (int i = 0; i < 4; i++) {
            sink ^= copyInts(view.ints, intTarget, WARM_COPIES);
            sink ^= copyLongs(view.longs, longTarget, WARM_COPIES);
        }
        long rssBefore = rss();
        long directBefore = directMemory();
        Views[] retained = new Views[128];
        long startAllocated = threads.getThreadAllocatedBytes(id);
        long startCpu = threads.getCurrentThreadCpuTime();
        long startWall = System.nanoTime();
        for (int i = 0; i < retained.length; i++) retained[i] = factory.get();
        long coldWall = System.nanoTime() - startWall;
        long coldCpu = threads.getCurrentThreadCpuTime() - startCpu;
        long coldAllocated = threads.getThreadAllocatedBytes(id) - startAllocated;
        long[] intCpu = new long[5];
        long[] longCpu = new long[5];
        long[] intWall = new long[5];
        long[] longWall = new long[5];
        long[] intAllocated = new long[5];
        long[] longAllocated = new long[5];
        for (int round = 0; round < intCpu.length; round++) {
            startAllocated = threads.getThreadAllocatedBytes(id);
            startCpu = threads.getCurrentThreadCpuTime();
            startWall = System.nanoTime();
            sink ^= copyInts(view.ints, intTarget, WARM_COPIES);
            intWall[round] = System.nanoTime() - startWall;
            intCpu[round] = threads.getCurrentThreadCpuTime() - startCpu;
            intAllocated[round] = threads.getThreadAllocatedBytes(id) - startAllocated;
            startAllocated = threads.getThreadAllocatedBytes(id);
            startCpu = threads.getCurrentThreadCpuTime();
            startWall = System.nanoTime();
            sink ^= copyLongs(view.longs, longTarget, WARM_COPIES);
            longWall[round] = System.nanoTime() - startWall;
            longCpu[round] = threads.getCurrentThreadCpuTime() - startCpu;
            longAllocated[round] = threads.getThreadAllocatedBytes(id) - startAllocated;
        }
        if (Arrays.stream(intAllocated).anyMatch(value -> value != 0)
                || Arrays.stream(longAllocated).anyMatch(value -> value != 0)) {
            throw new AssertionError("warm copy unexpectedly allocated");
        }
        Graph intGraph = graph(view.ints);
        Graph longGraph = graph(view.longs);
        // Keep the cold population live through the end of the measurement.
        sink += retained[retained.length - 1].longs.length();
        System.out.println("{\"shape\":\"" + shape + "\",\"logical_length\":" + view.ints.length()
                + ",\"descriptor_count\":" + DESCRIPTORS + ",\"copies_per_sample\":" + WARM_COPIES
                + ",\"elements_per_copy\":" + (view.ints.length() - 32)
                + ",\"cold_constructions\":" + retained.length + ",\"cold_allocated_bytes\":" + coldAllocated
                + ",\"cold_cpu_ns\":" + coldCpu + ",\"cold_wall_ns\":" + coldWall
                + ",\"int_cpu_ns\":" + Arrays.toString(intCpu) + ",\"long_cpu_ns\":" + Arrays.toString(longCpu)
                + ",\"int_wall_ns\":" + Arrays.toString(intWall) + ",\"long_wall_ns\":" + Arrays.toString(longWall)
                + ",\"int_allocated_bytes\":" + Arrays.toString(intAllocated)
                + ",\"long_allocated_bytes\":" + Arrays.toString(longAllocated)
                + ",\"int_graph\":" + intGraph.json() + ",\"long_graph\":" + longGraph.json()
                + ",\"rss_before_bytes\":" + rssBefore + ",\"rss_after_bytes\":" + rss()
                + ",\"direct_buffer_before_bytes\":" + directBefore + ",\"direct_buffer_after_bytes\":" + directMemory()
                + ",\"checksum\":" + sink + "}");
    }

    private static Views nested(M3IntArrayView[] integers, M3LongArrayView[] longs) {
        M3IntArrayView[] intGroups = new M3IntArrayView[16];
        M3LongArrayView[] longGroups = new M3LongArrayView[16];
        for (int i = 0; i < intGroups.length; i++) {
            intGroups[i] = M3Arrays.join(Arrays.copyOfRange(integers, i * 16, i * 16 + 16));
            longGroups[i] = M3Arrays.join(Arrays.copyOfRange(longs, i * 16, i * 16 + 16));
        }
        return new Views(M3Arrays.join(intGroups), M3Arrays.join(longGroups));
    }

    private static long copyInts(M3IntArrayView source, int[] target, int repetitions) {
        int count = source.length() - 32;
        long result = 0;
        for (int i = 0; i < repetitions; i++) {
            source.copyTo(8 + (i & 7), target, 2, count);
            result += target[2] ^ target[count + 1];
        }
        return result;
    }

    private static long copyLongs(M3LongArrayView source, long[] target, int repetitions) {
        int count = source.length() - 32;
        long result = 0;
        for (int i = 0; i < repetitions; i++) {
            source.copyTo(8 + (i & 7), target, 2, count);
            result += target[2] ^ target[count + 1];
        }
        return result;
    }

    private static Graph graph(Object root) throws IllegalAccessException {
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        long shallowBytes = 0;
        long primitiveBytes = 0;
        long references = 0;
        int arrays = 0;
        int views = 0;
        while (!pending.isEmpty()) {
            Object value = pending.removeLast();
            if (seen.put(value, Boolean.TRUE) != null) continue;
            shallowBytes += M3ArrayCopySizeAgent.instrumentation.getObjectSize(value);
            Class<?> type = value.getClass();
            if (type.isArray()) {
                arrays++;
                int length = Array.getLength(value);
                if (type.getComponentType() == int.class) primitiveBytes += (long) length * Integer.BYTES;
                else if (type.getComponentType() == long.class) primitiveBytes += (long) length * Long.BYTES;
                else if (type.getComponentType().isPrimitive()) throw new AssertionError(type);
                else {
                    references += length;
                    for (Object element : (Object[]) value) if (element != null) pending.add(element);
                }
            } else if (type.getName().startsWith("com.m3.arrays.M3Arrays$")) {
                views++;
                for (Class<?> owner = type; owner != Object.class; owner = owner.getSuperclass()) {
                    for (Field field : owner.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                        references++;
                        if (!field.trySetAccessible()) throw new AssertionError("cannot inspect " + field);
                        Object child = field.get(value);
                        if (child != null) pending.add(child);
                    }
                }
            } else throw new AssertionError("unexpected retained owner " + type);
        }
        return new Graph(shallowBytes, primitiveBytes, references, arrays, views);
    }

    private static long rss() throws Exception {
        for (String line : Files.readString(Path.of("/proc/self/status")).lines().toList()) {
            if (line.startsWith("VmRSS:")) return Long.parseLong(line.trim().split("\\s+")[1]) * 1024;
        }
        throw new AssertionError("Linux process RSS unavailable");
    }

    private static long directMemory() {
        long bytes = 0;
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if (pool.getName().equals("direct")) bytes += pool.getMemoryUsed();
        }
        return bytes;
    }

    private record Views(M3IntArrayView ints, M3LongArrayView longs) { }

    private record Graph(long shallowBytes, long primitiveBytes, long referenceSlots, int arrays, int views) {
        String json() {
            return "{\"shallow_bytes\":" + shallowBytes + ",\"primitive_bytes\":" + primitiveBytes
                    + ",\"reference_slots\":" + referenceSlots + ",\"arrays\":" + arrays + ",\"views\":" + views + "}";
        }
    }
}
