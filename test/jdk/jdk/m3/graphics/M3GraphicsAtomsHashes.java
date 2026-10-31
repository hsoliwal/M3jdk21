/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Whether a hashCode can be compared across the two sides of the jdk.m3.graphics atom tests. Enum.hashCode and an
 * Object.hashCode that no class overrides are identity hashes: they differ between any two objects, so a donor
 * hashCode that mixes one in (a value class with an enum field) differs between the M3 object and the oracle object
 * although both compute the same formula. Such hashes are skipped and counted; every other hashCode is compared.
 */
final class M3GraphicsAtomsHashes {
    private static final int MAX_DEPTH = 4;
    private static final String HASH_CODE = "hashCode";
    private static final AtomicInteger SKIPPED = new AtomicInteger();

    private M3GraphicsAtomsHashes() {}

    /**
     * True when the hashCode of a value is reproducible across the two sides: no enum and no identity-hashed object
     * in it. Checked on the oracle value, whose classes are in an unnamed module and open to deep reflection.
     */
    static boolean stable(Object o) {
        return stable(o, 0);
    }

    /** Records one skipped hash comparison. */
    static void skipped() {
        SKIPPED.incrementAndGet();
    }

    /** The number of hash comparisons skipped as identity-dependent since the last call. */
    static int skippedAndReset() {
        return SKIPPED.getAndSet(0);
    }

    private static boolean stable(Object o, int depth) {
        if (o == null || o instanceof Number || o instanceof String || o instanceof Boolean || o instanceof Character) {
            return true;
        }
        if (o instanceof Enum<?> || depth > MAX_DEPTH) {
            return false;
        }
        Class<?> c = o.getClass();
        if (c.isArray()) {
            return c.getComponentType().isPrimitive()
                    || IntStream.range(0, Array.getLength(o)).allMatch(i -> stable(Array.get(o, i), depth + 1));
        }
        if (o instanceof Collection<?> col) {
            return col.stream().allMatch(e -> stable(e, depth + 1));
        }
        if (o instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .allMatch(e -> stable(e.getKey(), depth + 1) && stable(e.getValue(), depth + 1));
        }
        if (!overridesHashCode(c)) {
            return false;
        }
        return !M3GraphicsAtomsOracle.isDonor(c.getName())
                || instanceFields(c).allMatch(f -> f.trySetAccessible() && stable(read(f, o), depth + 1));
    }

    private static boolean overridesHashCode(Class<?> c) {
        try {
            return c.getMethod(HASH_CODE).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Stream<Field> instanceFields(Class<?> c) {
        return Stream.<Class<?>>iterate(c, k -> k != null && M3GraphicsAtomsOracle.isDonor(k.getName()),
                        k -> k.getSuperclass())
                .flatMap(k -> Arrays.stream(k.getDeclaredFields())).filter(f -> !Modifier.isStatic(f.getModifiers()));
    }

    private static Object read(Field f, Object o) {
        try {
            return f.get(o);
        } catch (IllegalAccessException e) {
            return e;
        }
    }
}
