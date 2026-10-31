// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Invariant 6: the position precompute retains no second spelling store. Its exact blocks
 *          keep only block-relative first-occurrence offsets (byte) and position masks (long);
 *          every retained lane of the cache is primitive and bounded by the published maximum
 * @modules java.base/java.lang:+open
 * @run main M3StringPositionInvariant6Test
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class M3StringPositionInvariant6Test {
    private static long checks;

    public static void main(String[] args) throws Exception {
        Class<?> precompute = Class.forName("java.lang.M3StringPositionPrecompute");
        Class<?> exactBlock = Class.forName("java.lang.M3StringPositionPrecompute$ExactBlock");
        List<String> lanes = new ArrayList<>();
        for (Field field : exactBlock.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            lanes.add(field.getType().getSimpleName() + " " + field.getName());
            Class<?> type = field.getType();
            check(type.isArray() && type.getComponentType().isPrimitive(), "primitive lane: " + field);
            check(type != char[].class && type != String.class && type != byte[].class || field.getName().equals("firstOffsets"),
                    "no spelling lane: " + field);
        }
        check(lanes.size() == 2, "exactly two lanes, got " + lanes);
        check(lanes.contains("byte[] firstOffsets") && lanes.contains("long[] masks"), "lanes " + lanes);
        // Blocks and Entry retain only atomics over primitives/ExactBlock and weak owner keys.
        Class<?> blocks = Class.forName("java.lang.M3StringPositionPrecompute$Blocks");
        for (Field field : blocks.getDeclaredFields()) {
            String name = field.getType().getName();
            check(name.startsWith("java.util.concurrent.atomic.Atomic"), "blocks lane " + field);
        }
        Class<?> entry = Class.forName("java.lang.M3StringPositionPrecompute$Entry");
        for (Field field : entry.getDeclaredFields()) {
            Class<?> type = field.getType();
            check(type.isPrimitive() || type == java.lang.ref.WeakReference.class || type == blocks,
                    "entry lane " + field);
        }
        // The published retained maximum counts one byte plus one long per source unit.
        Method maximum = precompute.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        maximum.setAccessible(true);
        long slots = 64, maxUnits = 32_768, blocks64 = (maxUnits + 63) >>> 6;
        long expected = slots * blocks64 * Long.BYTES + slots * maxUnits * (Byte.BYTES + Long.BYTES);
        check(expected == (long) maximum.invoke(null), "maximumRetainedPrimitiveBytes " + maximum.invoke(null));
        System.out.println("M3StringPositionInvariant6Test checks=" + checks + " lanes=" + lanes);
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
