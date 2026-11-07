/*
 * Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Differential tests use an independent bit-at-a-time oracle, never M3PrimitiveBits. */
class M3PackedNativeTest {
    private static final int[] COUNTS = {0, 1, 2, 3, 7, 8, 9, 15, 16, 17,
            31, 32, 33, 63, 64, 65, 127, 257};

    static Stream<Arguments> widths() {
        return IntStream.rangeClosed(1, 64).boxed().flatMap(width -> Stream.of(
                Arguments.of(width, false), Arguments.of(width, true)));
    }

    @ParameterizedTest(name = "Java width={0}, signed={1}")
    @MethodSource("widths")
    void javaMatchesIndependentOracle(int width, boolean signed) {
        Arrays.stream(COUNTS).forEach(count -> verify(width, signed, count, false));
    }

    @ParameterizedTest(name = "JNI width={0}, signed={1}")
    @MethodSource("widths")
    void nativeMatchesIndependentOracleAndJava(int width, boolean signed) {
        requireNative();
        Arrays.stream(COUNTS).forEach(count -> verify(width, signed, count, true));
    }

    @Test
    void all64BitPatternsAreValidInEitherInterpretation() {
        long[] values = {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L,
                0x0123456789abcdefL, 0xfedcba9876543210L};
        for (boolean signed : new boolean[] {false, true}) {
            for (boolean preferNative : new boolean[] {false, true}) {
                long[] packed = M3PackedNative.pack(values, 64, signed, preferNative);
                assertArrayEquals(values, packed);
                assertNotSame(values, packed);
                assertArrayEquals(values, M3PackedNative.unpack(
                        packed, values.length, 64, signed, preferNative));
            }
        }
    }

    @Test
    void publicValidationIsIndependentOfNativePreference() {
        for (boolean preferNative : new boolean[] {false, true}) {
            for (int width : new int[] {Integer.MIN_VALUE, -1, 0, 65, Integer.MAX_VALUE}) {
                assertThrows(IllegalArgumentException.class,
                        () -> M3PackedNative.pack(new long[0], width, false, preferNative));
                assertThrows(IllegalArgumentException.class,
                        () -> M3PackedNative.unpack(new long[0], 0, width, false, preferNative));
            }
            assertThrows(NullPointerException.class,
                    () -> M3PackedNative.pack(null, 0, false, preferNative));
            assertThrows(NullPointerException.class,
                    () -> M3PackedNative.unpack(null, -1, 0, false, preferNative));
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[0], -1, 1, false, preferNative));
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[0], Integer.MAX_VALUE, 64, false, preferNative));
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[0], Integer.MAX_VALUE, 1, false, preferNative));
        }
    }

    @Test
    void everyNarrowWidthRejectsOutOfRangeValuesWithoutChangingInput() {
        IntStream.rangeClosed(1, 63).forEach(width -> {
            long unsignedOverflow = 1L << width;
            long signedBoundary = 1L << (width - 1);
            for (boolean preferNative : new boolean[] {false, true}) {
                rejectValue(width, false, -1L, preferNative);
                rejectValue(width, false, unsignedOverflow, preferNative);
                rejectValue(width, true, signedBoundary, preferNative);
                rejectValue(width, true, -signedBoundary - 1L, preferNative);
            }
        });
    }

    @Test
    void exactWordCountAndTailAreRequired() {
        for (boolean preferNative : new boolean[] {false, true}) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[] {0}, 0, 1, false, preferNative));
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[0], 1, 1, false, preferNative));
            assertThrows(IllegalArgumentException.class,
                    () -> M3PackedNative.unpack(new long[] {0, 0}, 1, 64, false, preferNative));
            IntStream.rangeClosed(1, 63).forEach(width -> {
                long[] words = {1L << width};
                long[] before = words.clone();
                assertThrows(IllegalArgumentException.class,
                        () -> M3PackedNative.unpack(words, 1, width, false, preferNative));
                assertArrayEquals(before, words);
            });
        }
    }

    @Test
    void nativeEntrypointsDefendAgainstBypassedJavaChecks() {
        requireNative();
        assertThrows(NullPointerException.class, () -> directPack(null, 0, false));
        assertThrows(NullPointerException.class, () -> directUnpack(null, -1, 0, false));
        for (int width : new int[] {Integer.MIN_VALUE, -1, 0, 65, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> directPack(new long[0], width, false));
            assertThrows(IllegalArgumentException.class,
                    () -> directUnpack(new long[0], -1, width, false));
        }
        assertThrows(IllegalArgumentException.class, () -> directUnpack(new long[0], -1, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> directUnpack(new long[0], Integer.MAX_VALUE, 64, false));
        assertThrows(IllegalArgumentException.class,
                () -> directUnpack(new long[] {0}, 0, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> directUnpack(new long[] {0, 0}, 1, 64, false));
        IntStream.rangeClosed(1, 63).forEach(width -> {
            long bound = 1L << (width - 1);
            assertThrows(IllegalArgumentException.class,
                    () -> directPack(new long[] {0, -1L}, width, false));
            assertThrows(IllegalArgumentException.class,
                    () -> directPack(new long[] {0, 1L << width}, width, false));
            assertThrows(IllegalArgumentException.class,
                    () -> directPack(new long[] {0, bound}, width, true));
            assertThrows(IllegalArgumentException.class,
                    () -> directPack(new long[] {0, -bound - 1L}, width, true));
            assertThrows(IllegalArgumentException.class,
                    () -> directUnpack(new long[] {1L << width}, 1, width, false));
        });
        assertArrayEquals(new long[0], directPack(new long[0], 64, false));
        assertArrayEquals(new long[0], directUnpack(new long[0], 0, 64, true));
    }

    @Test
    void independentNativeCallsDoNotShareMutableStorage() throws Exception {
        requireNative();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var jobs = IntStream.range(0, 256).mapToObj(job -> (Callable<Boolean>) () -> {
                int width = 1 + job % 64;
                boolean signed = (job & 1) != 0;
                verify(width, signed, 257, true);
                return true;
            }).toList();
            for (var result : executor.invokeAll(jobs)) {
                assertTrue(result.get());
            }
        }
    }

    @Test
    void missingLibraryFallsBackInAnIndependentJvm() throws Exception {
        String classPath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String absentPath = Path.of(System.getProperty("java.io.tmpdir"),
                "synexia-packed-no-library-20261029").toString();
        Process process = new ProcessBuilder(java, "-Xcheck:jni", "-Djava.library.path=" + absentPath,
                "-cp", classPath, FallbackProbe.class.getName()).redirectErrorStream(true).start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertTrue(finished, "fallback child JVM timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("M3_PACKED_FALLBACK_PASS"), output);
    }

    private static void requireNative() {
        boolean available = M3PackedNative.nativeAvailable();
        if (Boolean.getBoolean("synexia.test.requireNative")) {
            assertTrue(available, "native execution is required for this qualification run");
        }
        Assumptions.assumeTrue(available, "optional JNI library is unavailable");
    }

    private static void verify(int width, boolean signed, int count, boolean preferNative) {
        long[] values = values(width, signed, count);
        long[] before = values.clone();
        long[] expected = independentPack(values, width);
        long[] packed = M3PackedNative.pack(values, width, signed, preferNative);
        assertArrayEquals(expected, packed);
        assertArrayEquals(before, values);
        assertArrayEquals(values, M3PackedNative.unpack(packed, count, width, signed, preferNative));
        assertArrayEquals(values, M3PackedNative.unpack(packed, count, width, signed, false));
        assertArrayEquals(expected, packed);
        assertNotSame(values, packed);
        if (preferNative) {
            assertArrayEquals(expected, M3PackedNative.pack(values, width, signed, false));
            assertArrayEquals(expected, directPack(values, width, signed));
            assertArrayEquals(values, directUnpack(expected, count, width, signed));
        }
    }

    private static long[] values(int width, boolean signed, int count) {
        SplittableRandom random = new SplittableRandom(0x13c7e5a9L ^ width ^ (signed ? 0x4000L : 0L));
        long mask = width == 64 ? -1L : (1L << width) - 1L;
        long[] values = IntStream.range(0, count).mapToLong(index -> {
            long raw = random.nextLong() & mask;
            return signed && width < 64 ? (raw << (64 - width)) >> (64 - width) : raw;
        }).toArray();
        if (count > 0) {
            values[0] = 0L;
        }
        if (count > 1) {
            values[1] = signed ? (width == 64 ? Long.MIN_VALUE : -(1L << (width - 1))) : mask;
        }
        if (count > 2) {
            values[2] = signed ? (width == 64 ? Long.MAX_VALUE : (1L << (width - 1)) - 1L) : 1L;
        }
        return values;
    }

    private static long[] independentPack(long[] values, int width) {
        long[] words = new long[(int) (((long) values.length * width + 63L) / 64L)];
        for (int index = 0; index < values.length; index++) {
            for (int bit = 0; bit < width; bit++) {
                if (((values[index] >>> bit) & 1L) != 0L) {
                    long position = (long) index * width + bit;
                    words[(int) (position / 64L)] |= 1L << (position % 64L);
                }
            }
        }
        return words;
    }

    private static void rejectValue(int width, boolean signed, long value, boolean preferNative) {
        long[] input = {0L, value};
        long[] before = input.clone();
        assertThrows(IllegalArgumentException.class,
                () -> M3PackedNative.pack(input, width, signed, preferNative));
        assertArrayEquals(before, input);
    }

    private static long[] directPack(long[] values, int width, boolean signed) {
        return invoke("packNative", new Class<?>[] {long[].class, int.class, boolean.class},
                values, width, signed);
    }

    private static long[] directUnpack(long[] words, int count, int width, boolean signed) {
        return invoke("unpackNative", new Class<?>[] {long[].class, int.class, int.class, boolean.class},
                words, count, width, signed);
    }

    private static long[] invoke(String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = M3PackedNative.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return (long[]) method.invoke(null, arguments);
        } catch (InvocationTargetException target) {
            if (target.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            if (target.getCause() instanceof Error failure) {
                throw failure;
            }
            throw new AssertionError(target.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    /** A child JVM with an empty library path checks real fallback behavior. */
    public static final class FallbackProbe {
        private FallbackProbe() { }

        public static void main(String[] arguments) {
            assertFalse(M3PackedNative.nativeAvailable());
            long[] values = {0L, (1L << 40) - 1L, 9L};
            long[] words = M3PackedNative.pack(values, 40, false, true);
            assertArrayEquals(values, M3PackedNative.unpack(words, values.length, 40, false, true));
            System.out.println("M3_PACKED_FALLBACK_PASS");
        }
    }
}
