/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

public final class M3StringPhase1ResourceProbe {
    private static volatile String[] escape;

    private M3StringPhase1ResourceProbe() {}

    public static void main(String[] args) throws Exception {
        boolean expectedM3 = Boolean.parseBoolean(args[0]);
        int fork = Integer.parseInt(args[1]);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("thread allocation accounting unsupported");
        }
        bean.setThreadAllocatedMemoryEnabled(true);

        Field value = String.class.getDeclaredField("value");
        value.setAccessible(true);
        Field body = String.class.getDeclaredField("m3");
        body.setAccessible(true);
        Class<?> m3Type = Class.forName("java.lang.M3String");
        Field owner = m3Type.getDeclaredField("owner");
        owner.setAccessible(true);
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Method localNativeBytes = pool.getDeclaredMethod("localNativeBytes");
        localNativeBytes.setAccessible(true);

        long nativeBefore = (long) localNativeBytes.invoke(null);
        long thread = Thread.currentThread().threadId();

        long coldAllocatedStart = bean.getThreadAllocatedBytes(thread);
        long coldStart = System.nanoTime();
        String[] cold = joins(128, body);
        long coldNs = System.nanoTime() - coldStart;
        long coldAllocated = bean.getThreadAllocatedBytes(thread) - coldAllocatedStart;
        escape = cold;

        joins(512, body);
        long warmAllocatedStart = bean.getThreadAllocatedBytes(thread);
        long warmStart = System.nanoTime();
        String[] warm = joins(2_000, body);
        long warmNs = System.nanoTime() - warmStart;
        long warmAllocated = bean.getThreadAllocatedBytes(thread) - warmAllocatedStart;
        escape = warm;

        IdentityHashMap<Object, Boolean> owners = new IdentityHashMap<>();
        IdentityHashMap<byte[], Boolean> shadows = new IdentityHashMap<>();
        long compatibilityBytes = 0L;
        for (String string : warm) {
            Object storage = body.get(string);
            if ((storage != null) != expectedM3) {
                throw new AssertionError("representation mismatch");
            }
            if (storage != null) owners.put(owner.get(storage), Boolean.TRUE);
            byte[] shadow = (byte[]) value.get(string);
            if (shadows.put(shadow, Boolean.TRUE) == null) compatibilityBytes += shadow.length;
        }
        if (expectedM3 && owners.size() != 1) {
            throw new AssertionError("equal joins must converge to one tuple owner");
        }
        if (expectedM3 && compatibilityBytes != 0L) {
            throw new AssertionError("M3 wrappers retained compatibility spelling bytes");
        }

        long nativeAfter = (long) localNativeBytes.invoke(null);
        long nativeRetained = Math.max(0L, nativeAfter - nativeBefore);
        System.out.printf(
                "{\"fork\":%d,\"m3\":%s,\"cold_joins\":128,"
                + "\"cold_allocated_bytes\":%d,\"cold_ns\":%d,"
                + "\"warm_joins\":2000,\"warm_allocated_bytes\":%d,"
                + "\"warm_ns\":%d,\"native_before_bytes\":%d,"
                + "\"native_after_bytes\":%d,\"native_retained_delta_bytes\":%d,"
                + "\"canonical_join_owners\":%d,"
                + "\"compatibility_shadow_arrays\":%d,"
                + "\"compatibility_shadow_bytes\":%d}%n",
                fork, expectedM3, coldAllocated, coldNs, warmAllocated, warmNs,
                nativeBefore, nativeAfter, nativeRetained, owners.size(),
                shadows.size(), compatibilityBytes);
    }

    private static String[] joins(int count, Field body) throws Exception {
        char[] leftUnits = new char[8_192];
        char[] rightUnits = new char[8_192];
        java.util.Arrays.fill(leftUnits, 'x');
        java.util.Arrays.fill(rightUnits, '\u0100');
        String left = new String(leftUnits);
        String right = new String(rightUnits);
        String[] output = new String[count];
        for (int i = 0; i < count; i++) {
            String joined = left.concat(right);
            if (joined.length() != 16_384
                    || joined.charAt(8_191) != 'x'
                    || joined.charAt(8_192) != '\u0100') {
                throw new AssertionError("join semantics");
            }
            output[i] = joined;
        }
        return output;
    }
}
