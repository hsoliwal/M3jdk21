// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in finite performance witnesses, following Synexia's per-atom PMem test pattern.
 * Each atom has a Java baseline and JNI measurement. No machine-dependent speed gate.
 * These are smoke measurements, not JMH or production throughput qualification.
 */
@Tag("performance")
@EnabledIfSystemProperty(named = "m3.arrays.perf", matches = "true")
final class M3ArrayAllocationPerformanceTest {
    private static final int[] LENGTHS = {0, 64, 4096};
    private static final int ELEMENTS_PER_BATCH = 1 << 18;
    private static final int MAX_OPERATIONS = 8192;
    private static final int MIN_OPERATIONS = 32;
    private static final int WARMUP_ROUNDS = 2;
    private static final int SAMPLE_ROUNDS = 5;
    private static volatile Object sink;

    @BeforeAll static void requireActualLibrary() {
        String configured = System.getProperty("m3.arrays.native.path");
        assertNotNull(configured, "performance witnesses require the actual native library");
        assertFalse(configured.isBlank());
        M3ArrayNative.load(Path.of(configured));
        assertTrue(M3ArrayNative.isLoaded());
    }

    @Test void objects() { measure("objects", Object.class, Object[]::new, M3ArrayNative::allocateObjectsRequired); }
    @Test void booleans() { measure("booleans", boolean.class, boolean[]::new, M3ArrayNative::allocateBooleansRequired); }
    @Test void bytes() { measure("bytes", byte.class, byte[]::new, M3ArrayNative::allocateBytesRequired); }
    @Test void chars() { measure("chars", char.class, char[]::new, M3ArrayNative::allocateCharsRequired); }
    @Test void shorts() { measure("shorts", short.class, short[]::new, M3ArrayNative::allocateShortsRequired); }
    @Test void ints() { measure("ints", int.class, int[]::new, M3ArrayNative::allocateIntsRequired); }
    @Test void longs() { measure("longs", long.class, long[]::new, M3ArrayNative::allocateLongsRequired); }
    @Test void floats() { measure("floats", float.class, float[]::new, M3ArrayNative::allocateFloatsRequired); }
    @Test void doubles() { measure("doubles", double.class, double[]::new, M3ArrayNative::allocateDoublesRequired); }

    private static void measure(String atom, Class<?> component, IntFunction<Object> java,
            IntFunction<Object> nativeArray) {
        try {
            for (int length : LENGTHS) {
                // Oracles and reflection are outside both timed loops.
                validate(java, component, length);
                validate(nativeArray, component, length);
                int operations = Math.min(MAX_OPERATIONS,
                        Math.max(MIN_OPERATIONS, ELEMENTS_PER_BATCH / Math.max(1, length)));
                for (int warmup = 0; warmup < WARMUP_ROUNDS; warmup++) {
                    timed(java, length, operations); timed(nativeArray, length, operations);
                }
                long[] javaNanos = new long[SAMPLE_ROUNDS];
                long[] nativeNanos = new long[SAMPLE_ROUNDS];
                for (int sample = 0; sample < SAMPLE_ROUNDS; sample++) {
                    // Alternate order to reduce systematic first/second-lane bias.
                    if ((sample & 1) == 0) {
                        javaNanos[sample] = timed(java, length, operations);
                        nativeNanos[sample] = timed(nativeArray, length, operations);
                    } else {
                        nativeNanos[sample] = timed(nativeArray, length, operations);
                        javaNanos[sample] = timed(java, length, operations);
                    }
                }
                Arrays.sort(javaNanos); Arrays.sort(nativeNanos);
                System.out.printf(Locale.ROOT,
                        "M3_ARRAY_ATOM_PERF atom=%s length=%d operations=%d samples=%d java_median_ns_op=%.3f jni_median_ns_op=%.3f%n",
                        atom, length, operations, SAMPLE_ROUNDS,
                        javaNanos[SAMPLE_ROUNDS / 2] / (double) operations,
                        nativeNanos[SAMPLE_ROUNDS / 2] / (double) operations);
            }
        } finally { sink = null; }
    }

    private static long timed(IntFunction<Object> allocate, int length, int operations) {
        long start = System.nanoTime();
        // Every result escapes: neither lane can elide the allocation as dead code.
        for (int operation = 0; operation < operations; operation++) sink = allocate.apply(length);
        return System.nanoTime() - start;
    }

    private static void validate(IntFunction<Object> allocate, Class<?> component, int length) {
        Object first = allocate.apply(length); Object second = allocate.apply(length);
        assertNotSame(first, second);
        assertEquals(component, first.getClass().getComponentType());
        assertEquals(component, second.getClass().getComponentType());
        assertEquals(length, Array.getLength(first));
        assertEquals(length, Array.getLength(second));
    }
}
