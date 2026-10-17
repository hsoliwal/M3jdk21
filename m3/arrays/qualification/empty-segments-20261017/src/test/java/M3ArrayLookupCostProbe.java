/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
import com.m3.arrays.M3Arrays;
import com.m3.arrays.M3IntArrayView;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Finite resource observation; elapsed time is recorded without a speedup claim. */
public final class M3ArrayLookupCostProbe {
    private static volatile long sink;
    private M3ArrayLookupCostProbe() { }

    public static void main(String[] args) throws Exception {
        boolean emptyDescriptors = args.length == 1 && args[0].equals("empty");
        int count = 256;
        M3IntArrayView[] segments = new M3IntArrayView[count];
        for (int i = 0; i < count; i++) {
            int[] values = new int[emptyDescriptors && (i & 3) != 0 ? 0 : 16];
            for (int j = 0; j < values.length; j++) values[j] = i * 16 + j;
            segments[i] = M3Arrays.ints(values);
        }
        M3IntArrayView view = M3Arrays.join(segments);
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!threads.isThreadAllocatedMemorySupported() || !threads.isCurrentThreadCpuTimeSupported()) {
            throw new AssertionError("allocation and CPU counters unavailable");
        }
        threads.setThreadAllocatedMemoryEnabled(true);
        threads.setThreadCpuTimeEnabled(true);
        long id = Thread.currentThread().threadId();
        // Stabilize methods and counter machinery before the recorded cold and warm workloads.
        for (int i = 0; i < 10_000; i++) sink += M3Arrays.join(segments).length();
        for (int i = 0; i < 10; i++) sink = read(view, 100_000);
        M3IntArrayView[] retained = new M3IntArrayView[512];
        long allocated = threads.getThreadAllocatedBytes(id);
        long cpu = threads.getCurrentThreadCpuTime();
        long wall = System.nanoTime();
        for (int i = 0; i < retained.length; i++) retained[i] = M3Arrays.join(segments);
        long coldWall = System.nanoTime() - wall;
        long coldCpu = threads.getCurrentThreadCpuTime() - cpu;
        long coldAllocated = threads.getThreadAllocatedBytes(id) - allocated;
        allocated = threads.getThreadAllocatedBytes(id);
        cpu = threads.getCurrentThreadCpuTime();
        wall = System.nanoTime();
        sink = read(view, 2_000_000);
        long warmWall = System.nanoTime() - wall;
        long warmCpu = threads.getCurrentThreadCpuTime() - cpu;
        long warmAllocated = threads.getThreadAllocatedBytes(id) - allocated;
        Geometry geometry = geometry(view);
        if (warmAllocated != 0) throw new AssertionError("lookup unexpectedly allocated: " + warmAllocated);
        System.out.println("{\"empty_descriptors\":" + emptyDescriptors
                + ",\"descriptor_count\":" + count
                + ",\"logical_length\":" + view.length()
                + ",\"cold_joins\":" + retained.length
                + ",\"cold_allocated_bytes\":" + coldAllocated
                + ",\"cold_cpu_ns\":" + coldCpu + ",\"cold_wall_ns\":" + coldWall
                + ",\"warm_reads\":2000000,\"warm_allocated_bytes\":" + warmAllocated
                + ",\"warm_cpu_ns\":" + warmCpu + ",\"warm_wall_ns\":" + warmWall
                + ",\"retained_primitive_bytes\":" + geometry.primitiveBytes
                + ",\"retained_reference_slots\":" + geometry.referenceSlots
                + ",\"retained_arrays\":" + geometry.arrays
                + ",\"retained_view_objects\":" + geometry.views
                + ",\"native_allocations\":0,\"checksum\":" + sink + "}");
    }

    private static long read(M3IntArrayView view, int count) {
        long result = 0;
        int length = view.length();
        for (int i = 0; i < count; i++) result += view.intAt((i * 31 & Integer.MAX_VALUE) % length);
        return result;
    }

    private static Geometry geometry(Object root) throws Exception {
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Object> pending = new ArrayDeque<>();
        Map<String, Integer> fields = new TreeMap<>();
        pending.add(root);
        long primitiveBytes = 0;
        long references = 0;
        int arrays = 0;
        int views = 0;
        while (!pending.isEmpty()) {
            Object value = pending.removeLast();
            if (seen.put(value, Boolean.TRUE) != null) continue;
            Class<?> type = value.getClass();
            if (type.isArray()) {
                arrays++;
                int length = Array.getLength(value);
                if (type.getComponentType() == int.class) primitiveBytes += (long) length * Integer.BYTES;
                else if (type.getComponentType().isPrimitive()) throw new AssertionError(type);
                else {
                    references += length;
                    for (Object element : (Object[]) value) if (element != null) pending.add(element);
                }
            } else if (type.getName().startsWith("com.m3.arrays.M3Arrays$")) {
                views++;
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    fields.merge(type.getName() + ":" + field.getName() + ":" + field.getType().getName(), 1, Integer::sum);
                    if (!field.getType().isPrimitive()) {
                        if (!field.trySetAccessible()) throw new AssertionError("cannot inspect " + field);
                        Object child = field.get(value);
                        if (child != null) pending.add(child);
                    }
                }
            } else throw new AssertionError("unexpected retained owner " + type);
        }
        if (fields.size() != 4) throw new AssertionError("unexpected owner field topology " + fields);
        return new Geometry(primitiveBytes, references, arrays, views);
    }

    private record Geometry(long primitiveBytes, long referenceSlots, int arrays, int views) { }
}
