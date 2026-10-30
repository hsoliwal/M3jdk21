// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Java21 original-owner indexed byte-mask oracle, including raw JNI safety boundaries. */
public final class M3StateMaskHistogramIndexedNativeContract {
    private static int checks;
    private M3StateMaskHistogramIndexedNativeContract() { }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("wrong failure: " + problem, problem);
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private static void compare(int[] offsets, byte[] states, int first,
            int count, int stride) {
        int[] expected = new int[256];
        long ordinal = first;
        for (int row = 0; row < count; row++, ordinal += stride) {
            expected[states[offsets[(int) ordinal]] & 0xff]++;
        }
        var javaView = M3StateMaskHistogram.indexed(
                offsets, states, first, count, stride, M3StateMaskHistogram.Backend.JAVA);
        check(javaView.size() == count, "indexed occurrence count");
        check(Arrays.equals(expected, javaView.exactCounts()), "Java histogram");
        if (M3StateMaskHistogram.nativeAvailable()) {
            var nativeView = M3StateMaskHistogram.indexed(
                    offsets, states, first, count, stride,
                    M3StateMaskHistogram.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(expected, nativeView.exactCounts()),
                    "direct JNI or legacy native ABI preserved");
            for (int state = 0; state < 256; state += 17) {
                check(nativeView.countExactly(state) == expected[state],
                        "native exact-count query");
            }
            check(nativeView.countMatching(0, 0) == count, "native mask algebra");
        }
    }

    private static void exercise() {
        Random random = new Random(20261021L);
        for (int seed = 0; seed < 5000; seed++) {
            int size = random.nextInt(2048);
            byte[] states = new byte[random.nextInt(1024) + 1];
            random.nextBytes(states);
            int[] offsets = new int[size];
            for (int i = 0; i < size; i++) offsets[i] = random.nextInt(states.length);
            int first;
            int count;
            int stride;
            if (size == 0) {
                first = 0; count = 0;
                stride = random.nextBoolean() ? Integer.MIN_VALUE : 1;
            } else {
                first = random.nextInt(size + 1);
                stride = random.nextBoolean()
                        ? 1 + random.nextInt(15) : -(1 + random.nextInt(15));
                int available = stride > 0
                        ? (first < size ? 1 + (size - 1 - first) / stride : 0)
                        : (first < size ? 1 + first / -stride : 0);
                count = random.nextInt(available + 1);
            }
            compare(offsets, states, first, count, stride);
        }
        compare(new int[] {0, 0, 0, 0}, new byte[] {(byte) 0xff}, 3, 4, -1);
        compare(new int[] {1, 2, 3}, new byte[] {1, 2, 3, 4}, 3, 0, Integer.MIN_VALUE);
        compare(new int[] {1, 2, 3}, new byte[] {1, 2, 3, 4}, 0, 1, Integer.MIN_VALUE);
        expect(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.indexed(
                new int[] {9}, new byte[] {1}, 0, 1, 1));
        if (!M3StateMaskHistogram.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () -> M3StateMaskHistogram.indexed(
                    new int[] {0}, new byte[] {1}, 0, 1, 1,
                    M3StateMaskHistogram.Backend.NATIVE_REQUIRED));
        }
    }

    private static Object invoke(Method method, Object... args) {
        try { return method.invoke(null, args); }
        catch (InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException reflection) {
            throw new AssertionError(reflection);
        }
    }

    /** Check direct ABI only when present: older JNI versions remain supported. */
    private static void rawNativeSafety() throws Exception {
        if (!M3StateMaskHistogram.nativeAvailable()) return;
        Method probe = M3StateMaskHistogram.class.getDeclaredMethod("indexedAbiNative");
        probe.setAccessible(true);
        try {
            if (((Integer) invoke(probe)).intValue() != 1) return;
        } catch (UnsatisfiedLinkError oldLibrary) {
            return;
        }
        Method tally = M3StateMaskHistogram.class.getDeclaredMethod("tallyIndexedNative",
                int[].class, byte[].class, int.class, int.class, int.class, int[].class);
        tally.setAccessible(true);
        int[] offsets = {2, 2, 0, 1};
        byte[] states = {1, 2, (byte) 0xff};
        int[] counts = new int[256];
        invoke(tally, offsets, states, 3, 4, -1, counts);
        check(counts[1] == 1 && counts[2] == 1 && counts[255] == 2,
                "raw JNI duplicate occurrence/reverse");
        for (int failure = 0; failure < 6; failure++) {
            int[] output = new int[256];
            Arrays.fill(output, 0x12345678);
            final int index = failure;
            expect(IllegalArgumentException.class, () -> {
                switch (index) {
                    case 0 -> invoke(tally, null, states, 0, 0, 1, output);
                    case 1 -> invoke(tally, offsets, states, -1, 0, 1, output);
                    case 2 -> invoke(tally, offsets, states, 0, 5, 1, output);
                    case 3 -> invoke(tally, offsets, states, 0, 1, 0, output);
                    case 4 -> invoke(tally, new int[] {9}, states, 0, 1, 1, output);
                    default -> invoke(tally, offsets, states, 0, 0, 1, new int[255]);
                }
            });
            for (int bucket : output) check(bucket == 0x12345678,
                    "failed JNI operation leaves output unchanged");
        }
    }

    public static void main(String[] arguments) throws Exception {
        exercise();
        rawNativeSafety();
        System.out.println("M3_INDEXED_NATIVE_HISTOGRAM_PASS checks=" + checks);
    }
}
