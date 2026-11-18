/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Failed aligned-lane allocation leaves the set cache reusable and unchanged
 * @modules jdk.m3.collections/jdk.internal.m3.collections:open
 * @run main/othervm -Xmx32m -XX:+UseSerialGC SetAssociativeCacheGrowthFailureTest
 */
import java.lang.reflect.Field;
import jdk.internal.m3.collections.SetAssociativeCache;

public final class SetAssociativeCacheGrowthFailureTest {
    public static void main(String[] arguments) throws Exception {
        // The two payload arrays alone exceed the explicitly bounded heap.
        // First allocation can succeed; a later aligned lane must fail.
        SetAssociativeCache<Object> cache = new SetAssociativeCache<>(1, 1 << 22);
        Object[] before = lanes(cache);
        Object value = new Object();
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                cache.put(0, value);
                throw new AssertionError("bounded allocation unexpectedly succeeded");
            } catch (OutOfMemoryError expected) {
                // The allocator itself supplies the real failure; no injection hook.
            }
            Object[] after = lanes(cache);
            for (int index = 0; index < before.length; index++) {
                if (before[index] != after[index]) {
                    throw new AssertionError("partially published lane " + index);
                }
            }
            if (cache.size() != 0 || cache.containsKey(0) || cache.cachePuts() != 0) {
                throw new AssertionError("failed allocation changed logical state");
            }
        }
        cache.clear();
        if (cache.size() != 0) throw new AssertionError("clear after failure");
        System.out.println("SetAssociativeCacheGrowthFailureTest PASS real OOME retained all lane identities");
    }

    private static Object[] lanes(SetAssociativeCache<?> cache) throws Exception {
        String[] names = {"keyLane", "valueLane", "stateMaskWords"};
        Object[] result = new Object[names.length];
        for (int index = 0; index < names.length; index++) {
            Field field = SetAssociativeCache.class.getDeclaredField(names[index]);
            field.setAccessible(true);
            result[index] = field.get(cache);
        }
        return result;
    }
}
