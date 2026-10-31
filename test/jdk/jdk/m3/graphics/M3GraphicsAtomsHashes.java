/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * How a hashCode compares across the two sides of the jdk.m3.graphics atom tests. Enum.hashCode and an Object.hashCode
 * that no class overrides are identity hashes: they differ between any two objects, so a donor hashCode that mixes one
 * in (a value class with an enum field) differs between the M3 object and the oracle object although both compute the
 * same formula. The generated tests run with {@code -XX:hashCode=2}, which makes every identity hash 1 on both sides
 * ({@link #pinned()}); then every hashCode compares exactly. Without pinned identity hashes, a hash compares exactly
 * when no enum or identity-hashed part is in the value ({@link #stable}), else by partition: two cases with equal
 * hashes on one side must have equal hashes on the other ({@link #partition}). A partition catches a hashCode that
 * drops a field, not a changed constant (it shifts every hash alike). Exact and partition comparisons are counted.
 */
final class M3GraphicsAtomsHashes {
    private static final int MAX_DEPTH = 4;
    private static final int PROBES = 3;
    private static final int M3 = 0;
    private static final int ORACLE = 1;
    private static final String HASH_CODE = "hashCode";
    private static final String PARTITION = "hashCode partition: cases %d and %d equal on %s only"
            + " (M3 %d %d, oracle %d %d)";
    private static final String M3_SIDE = "M3";
    private static final String ORACLE_SIDE = "the oracle";
    private static final boolean PINNED = IntStream.range(0, PROBES).map(i -> System.identityHashCode(new Object()))
            .distinct().count() == 1;
    private static final AtomicInteger EXACT = new AtomicInteger();
    private static final AtomicInteger PARTITIONED = new AtomicInteger();
    private static final AtomicInteger SKIPPED = new AtomicInteger();

    private M3GraphicsAtomsHashes() {}

    /** True when identity hashes are pinned (-XX:hashCode=2): every identity hash is the same value. */
    static boolean pinned() {
        return PINNED;
    }

    /** True when a hash of this oracle value compares exactly: pinned identity hashes, or a stable value. */
    static boolean exact(Object oracleValue) {
        return PINNED || stable(oracleValue);
    }

    /**
     * True when the hashCode of a value is reproducible across the two sides: no enum and no identity-hashed object
     * in it. Checked on the oracle value, whose classes are in an unnamed module and open to deep reflection.
     */
    static boolean stable(Object o) {
        return stable(o, 0);
    }

    /**
     * The partition check of one member's hashes, each {M3 hash, oracle hash} per case: for every pair of cases, equal
     * on one side if and only if equal on the other. Empty when consistent.
     */
    static Optional<String> partition(List<int[]> hashes) {
        PARTITIONED.addAndGet(hashes.size());
        return IntStream.range(0, hashes.size()).boxed()
                .flatMap(i -> IntStream.range(i + 1, hashes.size()).mapToObj(j -> new int[] {i, j}))
                .filter(p -> (hashes.get(p[0])[M3] == hashes.get(p[1])[M3])
                        != (hashes.get(p[0])[ORACLE] == hashes.get(p[1])[ORACLE]))
                .findFirst().map(p -> String.format(Locale.ROOT, PARTITION, p[0], p[1],
                        hashes.get(p[0])[M3] == hashes.get(p[1])[M3] ? M3_SIDE : ORACLE_SIDE, hashes.get(p[0])[M3],
                        hashes.get(p[1])[M3], hashes.get(p[0])[ORACLE], hashes.get(p[1])[ORACLE]));
    }

    /** Records one exact hash comparison. */
    static void exactCompared() {
        EXACT.incrementAndGet();
    }

    /** Records one hash compared by partition outside {@link #partition} (perturbation pairs). */
    static void partitionCompared() {
        PARTITIONED.incrementAndGet();
    }

    /** Records one nested hash that was neither exact nor partitioned (an identity-dependent field value). */
    static void skipped() {
        SKIPPED.incrementAndGet();
    }

    /** {exact, partition, skipped} comparisons since the last call. */
    static int[] countsAndReset() {
        return new int[] {EXACT.getAndSet(0), PARTITIONED.getAndSet(0), SKIPPED.getAndSet(0)};
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
