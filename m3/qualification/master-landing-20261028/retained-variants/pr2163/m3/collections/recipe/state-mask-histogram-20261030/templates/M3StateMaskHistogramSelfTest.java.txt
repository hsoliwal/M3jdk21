/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Deterministic differential oracle; callable without Maven or JUnit. */
public final class M3StateMaskHistogramSelfTest {
    private static long checks;
    private M3StateMaskHistogramSelfTest() { }

    private static void equal(int expected, int actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": " + expected + " != " + actual);
    }

    private static void yes(boolean actual, String label) {
        equal(1, actual ? 1 : 0, label);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action, String label) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError(label + ": unexpected " + error, error);
        }
        throw new AssertionError(label + ": expected " + type.getName());
    }

    private static void verify(byte[] states, int[] offsets, int first,
            int count, int stride, boolean indexed) {
        int[] expected = new int[256];
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            expected[states[indexed ? offsets[(int) at] : (int) at] & 0xff]++;
        }
        M3StateMaskHistogram javaResult = indexed
                ? M3StateMaskHistogram.indexed(offsets, states, first, count, stride)
                : M3StateMaskHistogram.aligned(states, first, count, stride,
                        M3StateMaskHistogram.Backend.JAVA);
        equal(count, javaResult.size(), "cardinality");
        equal(2 * 256 * Integer.BYTES, javaResult.retainedPrimitiveBytes(), "retained primitive bytes");
        int distinct = 0;
        for (int state = 0; state < 256; state++) {
            equal(expected[state], javaResult.countExactly(state), "exact state " + state);
            if (expected[state] != 0) distinct++;
        }
        equal(distinct, javaResult.distinctStates(), "distinct state count");
        int[] exposed = javaResult.exactCounts();
        yes(Arrays.equals(expected, exposed), "defensive histogram");
        exposed[0]++;
        equal(expected[0], javaResult.countExactly(0), "no leaked histogram ownership");
        Random random = new Random(0x53544154454cL + count * 31L + first);
        for (int q = 0; q < 90; q++) {
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            int matches = 0;
            for (int state = 0; state < 256; state++) {
                if ((state & required) == required && (state & excluded) == 0) {
                    matches += expected[state];
                }
            }
            equal(matches, javaResult.countMatching(required, excluded), "mask algebra");
        }
        equal(count, javaResult.countMatching(0, 0), "unconstrained mask");
        equal(0, javaResult.countMatching(1, 1), "overlapping mask selects zero");
        if (M3StateMaskHistogram.nativeAvailable()) {
            M3StateMaskHistogram nativeResult = indexed
                    ? M3StateMaskHistogram.indexed(offsets, states, first, count, stride,
                            M3StateMaskHistogram.Backend.NATIVE_REQUIRED)
                    : M3StateMaskHistogram.aligned(states, first, count, stride,
                            M3StateMaskHistogram.Backend.NATIVE_REQUIRED);
            yes(Arrays.equals(expected, nativeResult.exactCounts()), "Java/C17/JNI parity");
            for (int q = 0; q < 12; q++) {
                int mask = random.nextInt(256);
                equal(javaResult.countMatching(mask, 0), nativeResult.countMatching(mask, 0),
                        "JNI inclusion counts");
            }
        }
    }

    private static void edges() {
        byte[] states = {0, 1, (byte) 0xff, 1, (byte) 0x80, 0};
        int[] offsets = {5, 2, 2, 4, 0, 3, 1};
        verify(states, offsets, 0, 6, 1, false);
        verify(states, offsets, 5, 6, -1, false);
        verify(states, offsets, 1, 3, 2, true);
        verify(states, offsets, 6, 4, -2, true);
        verify(states, offsets, 6, 0, -1, false);
        verify(states, offsets, 7, 0, 1, true);
        int[] empty = {};
        verify(new byte[0], empty, 0, 0, 1, false);
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.aligned(
                states, 6, 1, 1, M3StateMaskHistogram.Backend.JAVA), "past end");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.aligned(
                states, 5, 2, 1, M3StateMaskHistogram.Backend.JAVA), "overflow positive");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.aligned(
                states, 0, 2, -1, M3StateMaskHistogram.Backend.JAVA), "overflow negative");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.aligned(
                states, 0, 1, 0, M3StateMaskHistogram.Backend.JAVA), "zero stride");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.aligned(
                states, 0, 2, Integer.MAX_VALUE, M3StateMaskHistogram.Backend.JAVA), "wide overflow");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.indexed(
                new int[] {0, -1}, states, 0, 2, 1), "invalid physical offset");
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskHistogram.indexed(
                new int[] {0, 6}, states, 0, 2, 1), "physical equals length");
        fails(IllegalArgumentException.class, () -> M3StateMaskHistogram.indexed(
                offsets, states, 0, 2, 1).countExactly(-1), "negative state value");
        fails(IllegalArgumentException.class, () -> M3StateMaskHistogram.indexed(
                offsets, states, 0, 2, 1).countMatching(0, 256), "too wide mask");
        fails(NullPointerException.class, () -> M3StateMaskHistogram.aligned(
                null, 0, 0, 1, M3StateMaskHistogram.Backend.JAVA), "null metadata");
        fails(NullPointerException.class, () -> M3StateMaskHistogram.indexed(
                null, states, 0, 0, 1), "null offset plane");
        if (!M3StateMaskHistogram.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> M3StateMaskHistogram.aligned(
                    states, states.length, 0, 1, M3StateMaskHistogram.Backend.NATIVE_REQUIRED),
                    "explicit native unavailable even for empty window");
            fails(UnsatisfiedLinkError.class, () -> M3StateMaskHistogram.indexed(
                    offsets, states, 0, 1, 1, M3StateMaskHistogram.Backend.NATIVE_REQUIRED),
                    "indexed native unavailable");
        }
    }

    private static void stridedWindows() {
        byte[] states = new byte[8193];
        for (int i = 0; i < states.length; i++) states[i] = (byte) (i * 73 + i / 251);
        int[] unusedOffsets = {};
        int[] steps = {2, 3, 7, 16, 31, 63, 127, 128, 129, 255, 256, 257, 1024, 4096};
        for (int step : steps) {
            int first = 17;
            int available = 1 + (states.length - 1 - first) / step;
            int batch = 1 + 255 / step;
            int[] counts = {0, 1, Math.min(available, batch - 1), Math.min(available, batch),
                    Math.min(available, batch + 1), available};
            for (int count : counts) {
                verify(states, unusedOffsets, first, count, step, false);
                verify(states, unusedOffsets, states.length - 1 - first, count, -step, false);
            }
        }
        verify(states, unusedOffsets, states.length, 0, Integer.MIN_VALUE, false);
        verify(states, unusedOffsets, states.length, 0, Integer.MAX_VALUE, false);
        verify(states, unusedOffsets, 0, 1, Integer.MIN_VALUE, false);
        verify(states, unusedOffsets, states.length - 1, 1, Integer.MAX_VALUE, false);
        verify(states, unusedOffsets, 0, 4097, 2, false);
        verify(states, unusedOffsets, states.length - 1, 4097, -2, false);
    }

    private static void invokeNative(Method tally, byte[] source, int first,
                                     int count, int stride, int[] result) {
        try {
            tally.invoke(null, source, first, count, stride, result);
        } catch (InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError("unexpected checked JNI exception", cause);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("cannot exercise existing JNI boundary", error);
        }
    }

    private static void rejectedNative(Method tally, byte[] source, int first,
            int count, int stride, int outputLength, String label) {
        int[] result = new int[outputLength];
        Arrays.fill(result, 0x12345678);
        fails(IllegalArgumentException.class,
                () -> invokeNative(tally, source, first, count, stride, result), label);
        for (int bucket : result) equal(0x12345678, bucket, "rejected JNI leaves output untouched");
    }

    private static void nativeBoundaries() {
        if (!M3StateMaskHistogram.nativeAvailable()) return;
        Method tally;
        try {
            tally = M3StateMaskHistogram.class.getDeclaredMethod("tallyNative",
                    byte[].class, int.class, int.class, int.class, int[].class);
            tally.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("existing JNI descriptor changed", error);
        }
        byte[] states = {0, 1, (byte) 0xff, (byte) 0x80};
        rejectedNative(tally, null, 0, 0, 1, 256, "null native input");
        rejectedNative(tally, states, -1, 0, 1, 256, "negative native first");
        rejectedNative(tally, states, 5, 0, 1, 256, "native first after end");
        rejectedNative(tally, states, 0, -1, 1, 256, "negative native count");
        rejectedNative(tally, states, 0, 1, 0, 256, "zero native stride");
        rejectedNative(tally, states, 4, 1, 1, 256, "nonempty native end");
        rejectedNative(tally, states, 0, 2, -1, 256, "negative native endpoint");
        rejectedNative(tally, states, 1, 2, Integer.MAX_VALUE, 256, "wide native endpoint");
        rejectedNative(tally, states, 0, 2, Integer.MIN_VALUE, 256, "minimum native stride");
        rejectedNative(tally, states, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 256,
                "maximum native path product");
        rejectedNative(tally, states, 0, 0, 1, 255, "short native output");
        rejectedNative(tally, states, 0, 0, 1, 257, "long native output");
        fails(IllegalArgumentException.class,
                () -> invokeNative(tally, states, 0, 0, 1, null), "null native output");
        int[] result = new int[256];
        Arrays.fill(result, 19);
        invokeNative(tally, states, states.length, 0, Integer.MIN_VALUE, result);
        for (int bucket : result) equal(0, bucket, "valid empty native publishes zero histogram");
        invokeNative(tally, states, 3, 1, Integer.MIN_VALUE, result);
        for (int state = 0; state < 256; state++) {
            equal(state == 128 ? 1 : 0, result[state], "valid huge-stride native singleton");
        }
    }

    private static void randomCases() {
        Random random = new Random(0x4d335354415445L);
        for (int t = 0; t < 240; t++) {
            int length = random.nextInt(180);
            byte[] state = new byte[length];
            int[] offsets = new int[length];
            for (int i = 0; i < length; i++) {
                state[i] = (byte) random.nextInt(256);
                offsets[i] = length == 0 ? 0 : random.nextInt(length);
            }
            int start = random.nextInt(length + 1);
            int max = length - start;
            int forwardCount = random.nextInt(max + 1);
            verify(state, offsets, start, forwardCount, 1, false);
            verify(state, offsets, start, forwardCount, 1, true);
            int reverseFirst = length == 0 ? 0 : random.nextInt(length);
            int reverseCount = length == 0 ? 0 : random.nextInt(reverseFirst + 2);
            verify(state, offsets, reverseFirst, reverseCount, -1, false);
            verify(state, offsets, reverseFirst, reverseCount, -1, true);
        }
        int n = 1 << 16;
        byte[] large = new byte[n];
        int[] offsets = new int[n];
        for (int i = 0; i < n; i++) {
            large[i] = (byte) random.nextInt(256);
            offsets[i] = n - i - 1;
        }
        verify(large, offsets, 0, n, 1, false);
        verify(large, offsets, n - 1, n, -1, true);
    }

    public static void main(String[] args) {
        edges();
        stridedWindows();
        nativeBoundaries();
        randomCases();
        System.out.println("M3StateMaskHistogramSelfTest PASS checks=" + checks
                + " native=" + M3StateMaskHistogram.nativeAvailable());
    }
}
