/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

/** Narrow allocation/retention probe; setup and retention inspection are untimed. */
public class JoinAllocation {
    static volatile String[] sink;
    public static void main(String[] args) throws Exception {
        int length = Integer.parseInt(args[0]);
        boolean wide = Boolean.parseBoolean(args[1]);
        int count = 100000;
        long setupStart = System.nanoTime();
        char[] chars = new char[length]; java.util.Arrays.fill(chars, wide ? '\u0100' : 'x');
        String source = new String(chars);
        String[] elements = {source}; String[] results = new String[count];
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        Field field = String.class.getDeclaredField("value"); field.setAccessible(true);
        long setupNs = System.nanoTime() - setupStart;
        long warmStart = System.nanoTime();
        for (int i = 0; i < count; i++) results[i] = String.join(",", elements);
        sink = results;
        long warmNs = System.nanoTime() - warmStart;
        long allocBefore = bean.getThreadAllocatedBytes(thread);
        long cpuBefore = bean.getCurrentThreadCpuTime();
        long operationStart = System.nanoTime();
        for (int i = 0; i < count; i++) results[i] = String.join(",", elements);
        long operationNs = System.nanoTime() - operationStart;
        long cpuNs = bean.getCurrentThreadCpuTime() - cpuBefore;
        long allocated = bean.getThreadAllocatedBytes(thread) - allocBefore;
        sink = results;
        IdentityHashMap<Object, Boolean> arrays = new IdentityHashMap<>();
        long payload = 0;
        for (String result : results) {
            if (result == source || !result.equals(source)) throw new AssertionError("result semantics");
            byte[] bytes = (byte[]) field.get(result);
            if (arrays.put(bytes, true) == null) payload += bytes.length;
        }
        System.out.printf("{\"length\":%d,\"wide\":%s,\"count\":%d,\"setup_ns\":%d,\"warm_ns\":%d,\"operation_ns\":%d,\"cpu_ns\":%d,\"allocated_bytes\":%d,\"retained_arrays\":%d,\"retained_payload_bytes\":%d}%n",
                length, wide, count, setupNs, warmNs, operationNs, cpuNs, allocated, arrays.size(), payload);
    }
}
