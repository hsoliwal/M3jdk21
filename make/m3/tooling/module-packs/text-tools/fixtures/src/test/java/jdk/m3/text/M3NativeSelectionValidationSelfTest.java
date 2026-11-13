// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Real JNI invalid-geometry and source-nonmutation oracle; test-only reflection
 * reaches the existing private C17 selection symbols without changing their ABI.
 */
public final class M3NativeSelectionValidationSelfTest {
    private static int checks;
    private M3NativeSelectionValidationSelfTest() { }

    private static final Method PACKED = method("select0", byte[].class, int[].class,
            long[].class, byte[].class, int.class, int.class);
    private static final Method DIRECT = method("selectDirect0", ByteBuffer.class, int[].class,
            long[].class, byte[].class, int.class, int.class);

    private static Method method(String name, Class<?>... args) {
        try {
            Method m = NativeLineBatch.class.getDeclaredMethod(name, args);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("JNI signature changed: " + name, e);
        }
    }

    private static long[] call(Method method, Object source, int[] offsets, long[] mask,
                               byte[] needle, int op, int count) {
        try {
            return (long[]) method.invoke(null, source, offsets, mask, needle, op, count);
        } catch (InvocationTargetException result) {
            Throwable ex = result.getCause();
            if (ex instanceof RuntimeException rt) throw rt;
            if (ex instanceof Error err) throw err;
            throw new AssertionError("unexpected checked JNI exception", ex);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("JNI reflection failed", e);
        }
    }

    private static void same(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual))
            throw new AssertionError("native mask mismatch expected="+Arrays.toString(expected)
                    +" actual="+Arrays.toString(actual));
    }

    private static void reject(Method m, Object data, int[] offsets, long[] mask,
                               byte[] needle, int op, int count) {
        byte[] beforeData = data instanceof byte[] b ? b.clone() : null;
        int[] beforeOffsets = offsets == null ? null : offsets.clone();
        long[] beforeMask = mask == null ? null : mask.clone();
        byte[] beforeNeedle = needle == null ? null : needle.clone();
        try {
            call(m, data, offsets, mask, needle, op, count);
        } catch (IllegalArgumentException | NullPointerException expected) {
            checks++;
            if (beforeData != null && !Arrays.equals(beforeData, (byte[]) data))
                throw new AssertionError("JNI mutated input bytes");
            if (!Arrays.equals(beforeOffsets, offsets) || !Arrays.equals(beforeMask, mask)
                    || !Arrays.equals(beforeNeedle, needle))
                throw new AssertionError("JNI mutated geometry or needle after refusal");
            return;
        }
        throw new AssertionError("malformed raw JNI request was accepted");
    }

    private static ByteBuffer direct(byte[] bytes) {
        ByteBuffer b = ByteBuffer.allocateDirect(bytes.length);
        b.put(bytes).flip();
        return b.asReadOnlyBuffer();
    }

    private static void geometryRefusal() {
        byte[] data = "abcd".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] offsets = {0, 2, 4};
        long[] selected = {3L};
        byte[] needle = {'b'};
        for (Method m : List.of(PACKED, DIRECT)) {
            Object input = m == PACKED ? data : direct(data);
            same(new long[]{1L}, call(m, input, offsets, selected, needle, 1, 0));
            same(new long[]{1L}, call(m, input, offsets, selected, needle, 1, 0));
            same(new long[]{2L}, call(m, input, offsets, selected, "cd".getBytes(java.nio.charset.StandardCharsets.UTF_8), 1, 0));
            same(new long[]{1L}, call(m, input, offsets, selected, needle, 3, 1));
            same(new long[]{2L}, call(m, input, offsets, selected, needle, 4, 1));
            reject(m, input, new int[]{0, 3, 2}, selected, needle, 1, 0);
            reject(m, input, new int[]{-1, 2, 4}, selected, needle, 1, 0);
            reject(m, input, new int[]{0, 5, 4}, selected, needle, 1, 0);
            reject(m, input, new int[]{0, 2, 3}, selected, needle, 1, 0);
            reject(m, input, new int[]{0, 2, 4}, new long[]{1L, 0}, needle, 1, 0);
            reject(m, input, new int[]{0, 2, 4}, new long[0], needle, 1, 0);
            reject(m, input, offsets, new long[]{1L << 63}, needle, 1, 0);
            reject(m, input, offsets, new long[]{-1L}, needle, 1, 0);
            reject(m, input, offsets, selected, needle, 99, 0);
            reject(m, input, offsets, selected, needle, 4, -1);
            reject(m, input, offsets, selected, null, 1, 0);
            reject(m, input, null, selected, needle, 1, 0);
            reject(m, input, offsets, null, needle, 1, 0);
            reject(m, input, new int[0], new long[0], needle, 1, 0);
        }
        reject(PACKED, null, offsets, selected, needle, 1, 0);
        reject(DIRECT, null, offsets, selected, needle, 1, 0);
        reject(DIRECT, ByteBuffer.wrap(data), offsets, selected, needle, 1, 0);
    }

    private static void randomizedMasks() {
        Random random = new Random(0x4d334a4e49564cL);
        for (int step = 0; step < 5000; step++) {
            int lines = random.nextInt(133);
            int[] offsets = new int[lines + 1];
            byte[] source = new byte[lines * 3];
            long[] mask = new long[(lines + 63) >>> 6];
            for (int i = 0; i < lines; i++) {
                source[3*i] = (byte)('a'+random.nextInt(2));
                source[3*i+1]=(byte)('b'+random.nextInt(2));
                source[3*i+2]=(byte)('c'+random.nextInt(2));
                offsets[i+1] = 3*(i+1);
                if (random.nextBoolean()) mask[i>>>6] |= 1L << (i&63);
            }
            long[] expected = new long[mask.length];
            int kept = 0;
            for (int i = 0; i < lines; i++) {
                if ((mask[i>>>6] & (1L << (i&63))) == 0L) continue;
                if (source[3*i]=='a') { expected[i>>>6] |= 1L << (i&63); kept++; }
            }
            same(expected, call(PACKED, source, offsets, mask, new byte[]{'a'}, 1, 0));
            same(expected, call(DIRECT, direct(source), offsets, mask, new byte[]{'a'}, 1, 0));
            if (lines > 0 && (lines & 63) != 0) {
                long[] invalid = mask.clone();
                invalid[invalid.length-1] |= 1L << (lines & 63);
                reject(PACKED, source, offsets, invalid, new byte[]{'a'}, 1, 0);
            }
        }
    }

    public static void main(String[] ignored) {
        if (NativeLineBatch.nativeAbi() != 0x00010002) throw new AssertionError("JNI ABI drift");
        geometryRefusal();
        randomizedMasks();
        System.out.println("M3_UNIX4J_NATIVE_GEOMETRY_PASS checks="+checks);
    }
}
