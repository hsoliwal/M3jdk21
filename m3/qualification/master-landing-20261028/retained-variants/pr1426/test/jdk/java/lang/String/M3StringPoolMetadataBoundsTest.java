/*
 * Copyright (c) 2026, M3 contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Refused local atoms and uncached tuples do not retain empty map buckets
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -Dm3.string.pool.maxBytes=0 -Dm3.string.pool.maxTuples=0 M3StringPoolMetadataBoundsTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class M3StringPoolMetadataBoundsTest {
    private static final List<Object> HOLDS = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        check(field(pool, "MAX_LOCAL_BYTES").getLong(null) == 0, "zero local budget");
        check(field(pool, "MAX_RETAINED_TUPLES").getLong(null) == 0, "zero tuple cap");
        check(!field(m3, "ready").getBoolean(null), "incidental String admissions disabled");
        Method chars = method(pool, "internChars", char[].class, int.class, int.class);
        Method latin1 = method(pool, "internLatin1Bytes", byte[].class, int.class, int.class);
        Method compact = method(pool, "internCompactBytes", byte[].class,
                int.class, int.class, byte.class);
        Method points = method(pool, "internCodePoints", int[].class, int.class, int.class);
        Method unit = method(pool, "internUnit", char.class);
        Method concat = method(pool, "concat", m3, m3);
        Method bytes = method(pool, "localNativeBytes");
        Method tuples = method(pool, "retainedTuples");
        Map<?, ?> locals = (Map<?, ?>) field(pool, "LOCAL").get(null);
        Map<?, ?> tupleMap = (Map<?, ?>) field(pool, "TUPLES").get(null);
        Field holdoff = field(pool, "nextSlowReserveNanos");
        long previous = holdoff.getLong(null);
        try {
            // Refusal is already determined by a zero budget. Hold off its optional
            // reference-processing/GC retry while measuring retained bucket metadata.
            holdoff.setLong(null, System.nanoTime() + 60_000_000_000L);
            int localBaseline = locals.size();
            long byteBaseline = (long) bytes.invoke(null);
            for (int index = 0; index < 1024; index++) {
                char[] value = {(char) (0x100 + index), (char) (0x800 + index)};
                check(chars.invoke(null, value, 0, 2) == null, "char admission refused");
                int[] codePoints = {value[0], value[1]};
                check(points.invoke(null, codePoints, 0, 2) == null, "code-point admission refused");
                byte[] value8 = {(byte) index, (byte) (index >>> 8)};
                check(latin1.invoke(null, value8, 0, 2) == null, "Latin-1 admission refused");
                check(compact.invoke(null, value8, 0, 2, (byte) 0) == null, "compact admission refused");
                check(locals.size() == localBaseline, "refusal retires empty local metadata");
            }
            check((long) bytes.invoke(null) == byteBaseline, "refusal leaves native accounting unchanged");
            int tupleBaseline = tupleMap.size();
            for (int index = 0; index < 512; index++) {
                char leftUnit = (char) (0x2000 + index);
                char rightUnit = (char) (0x4000 + index);
                Object left = unit.invoke(null, leftUnit);
                Object right = unit.invoke(null, rightUnit);
                HOLDS.add(left);
                HOLDS.add(right);
                Object joined = concat.invoke(null, left, right);
                HOLDS.add(joined);
                CharSequence text = (CharSequence) joined;
                check(text.length() == 2 && text.charAt(0) == leftUnit
                        && text.charAt(1) == rightUnit, "uncached tuple remains exact");
                check(tupleMap.size() == tupleBaseline, "cap retires empty tuple metadata");
                check((long) tuples.invoke(null) == 0, "cap leaves retained count at zero");
            }
            System.out.println("M3StringPoolMetadataBoundsTest checks=" + checks);
        } finally {
            holdoff.setLong(null, previous);
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
