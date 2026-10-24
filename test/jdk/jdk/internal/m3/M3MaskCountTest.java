/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Count bounded state-mask rows with Java/native parity and unchanged scan ABI
 * @modules java.base/jdk.internal.m3
 * @compile --patch-module java.base=${test.src} M3MaskCountTest.java
 * @run main/othervm --patch-module java.base=${test.classes} -Xcheck:jni jdk.internal.m3.M3MaskCountTest native
 * @run main/othervm --patch-module java.base=${test.classes} -Xint -Xcheck:jni jdk.internal.m3.M3MaskCountTest native-quick
 */
package jdk.internal.m3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Real-owner tests. Optional absolute native library path is host qualification only. */
public final class M3MaskCountTest {
    private static long checks;
    private static long cases;
    private static long digest = 0x4d33434f554e54L;
    private static final M3IndexMaskWindow.Backend JAVA = M3IndexMaskWindow.Backend.JAVA;
    private static final M3IndexMaskWindow.Backend NATIVE = M3IndexMaskWindow.Backend.NATIVE_REQUIRED;
    private M3MaskCountTest() { }
    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why + " case=" + cases);
    }
    @FunctionalInterface private interface Action { void run() throws Throwable; }
    private static void fails(Class<? extends Throwable> type, Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (type == failure.getClass()) { checks++; return; }
            throw new AssertionError("wanted " + type + ", got " + failure, failure);
        }
        throw new AssertionError("missing " + type);
    }
    private static long reference(byte[] states, int first, int budget, int required, int excluded) {
        int end = first + (int) Math.min((long) budget, (long) states.length - first);
        int count = 0;
        for (int i = first; i < end; i++) {
            int bits = Byte.toUnsignedInt(states[i]);
            boolean matches = true;
            // Independent bit-by-bit oracle, not the candidate's combined-mask expression.
            for (int bit = 0; bit < 8; bit++) {
                boolean present = (bits & (1 << bit)) != 0;
                if (((required & (1 << bit)) != 0 && !present)
                        || ((excluded & (1 << bit)) != 0 && present)) matches = false;
            }
            if (matches) count++;
        }
        return ((long) end << 32) | Integer.toUnsignedLong(count);
    }
    private static void vector(byte[] states, int first, int budget, int required, int excluded,
            M3IndexMaskWindow.Backend backend) {
        byte[] before = states.clone();
        long expected = reference(states, first, budget, required, excluded);
        long actual = M3IndexMaskWindow.countBounded(states, first, budget, required, excluded, backend);
        check(expected == actual, "count/cursor");
        check(Arrays.equals(before, states), "state mutation");
        check(M3IndexMaskWindow.written(actual) <= M3IndexMaskWindow.nextIndex(actual) - first,
                "count bounds");
        digest = Long.rotateLeft(digest, 7) ^ actual;
        digest *= 0x9e3779b97f4a7c15L;
        cases++;
    }
    private static void corpus(M3IndexMaskWindow.Backend backend, boolean quick) {
        byte[] states = new byte[260];
        for (int i = 0; i < 256; i++) states[i + 2] = (byte) i;
        for (int required = 0; required < 256; required += quick ? 17 : 1) {
            for (int excluded = 0; excluded < 256; excluded += quick ? 17 : 1) {
                vector(states, 2, 256, required, excluded, backend);
            }
        }
        int[] sizes = {0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097, 65537};
        int[] masks = {0, 1, 63, 127, 128, 170, 255};
        Random random = new Random(0x4d33434f554e54L);
        for (int size : sizes) {
            if (quick && size > 4097) continue;
            byte[] values = new byte[size]; random.nextBytes(values);
            for (int first : new int[]{0, size / 2, size}) {
                for (int budget : new int[]{0, 1, 127, 128, 129, 4096, Integer.MAX_VALUE}) {
                    for (int req : masks) {
                        for (int exc : masks) vector(values, first, budget, req, exc, backend);
                    }
                }
            }
        }
        for (int trial = 0; trial < (quick ? 128 : 4000); trial++) {
            int size = random.nextInt(2049);
            byte[] values = new byte[size]; random.nextBytes(values);
            int first = random.nextInt(size + 1);
            int budget = trial % 13 == 0 ? Integer.MAX_VALUE : random.nextInt(4097);
            int req = random.nextInt(256), exc = random.nextInt(256);
            vector(values, first, budget, req, exc, backend);
            if (trial < 128) resume(values, req, exc, backend);
        }
    }
    private static void resume(byte[] states, int req, int exc, M3IndexMaskWindow.Backend backend) {
        int cursor = 0, count = 0;
        while (cursor < states.length) {
            long result = M3IndexMaskWindow.countBounded(states, cursor, 17, req, exc, backend);
            check(M3IndexMaskWindow.nextIndex(result) > cursor, "resumption progress");
            cursor = M3IndexMaskWindow.nextIndex(result);
            count += M3IndexMaskWindow.written(result);
        }
        check(count == (int) reference(states, 0, states.length, req, exc), "resumed count");
        // Counts preserve duplicates: two equal row IDs still contribute two state rows.
        if (states.length <= M3IndexMaskWindow.MAX_WINDOW) {
            int[] rows = new int[states.length], out = new int[states.length];
            Arrays.fill(rows, 7);
            long scan = M3IndexMaskWindow.scanBounded(rows, states, 0, states.length,
                    req, exc, out, 0, out.length, backend);
            check(count == M3IndexMaskWindow.written(scan), "count/selection consistency");
        }
    }
    private static void validation(M3IndexMaskWindow.Backend backend) {
        byte[] states = {0, (byte) 128, (byte) 255};
        fails(NullPointerException.class, () -> M3IndexMaskWindow.countBounded(null, -1, -1, -1, -1, null));
        fails(NullPointerException.class, () -> M3IndexMaskWindow.countBounded(states, -1, -1, -1, -1, null));
        for (int mask : new int[]{-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class, () -> M3IndexMaskWindow.countBounded(states, 0, 0, mask, 0, backend));
            fails(IllegalArgumentException.class, () -> M3IndexMaskWindow.countBounded(states, 0, 0, 0, mask, backend));
        }
        for (int first : new int[]{-1, 4, Integer.MAX_VALUE}) {
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.countBounded(states, first, 0, 0, 0, backend));
        }
        fails(IllegalArgumentException.class, () -> M3IndexMaskWindow.countBounded(states, 0, -1, 0, 0, backend));
        // Validation wins over contradictory/zero-work shortcuts and missing native ABI.
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.countBounded(states, -1, -1, 1, 1, backend));
        check(Arrays.equals(states, new byte[]{0, (byte)128, (byte)255}), "invalid call mutated states");
    }
    private static Method nativeMethod() throws ReflectiveOperationException {
        Method method = M3IndexMaskWindow.class.getDeclaredMethod("countBoundedNative", byte[].class,
                int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return method;
    }
    private static long invoke(Method method, byte[] states, int first, int end, int req, int exc) throws Throwable {
        try { return (Long) method.invoke(null, states, first, end, req, exc); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    private static void directNative() throws Throwable {
        Method method = nativeMethod(); byte[] states = {(byte)255, 0, (byte)128};
        fails(IllegalArgumentException.class, () -> invoke(method, null, 0, 0, 0, 0));
        for (int[] range : new int[][]{{-1,0},{0,4},{2,1},{4,4},{0,Integer.MAX_VALUE}}) {
            fails(IllegalArgumentException.class, () -> invoke(method, states, range[0], range[1], 0, 0));
        }
        for (int mask : new int[]{-1,256,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class, () -> invoke(method, states, 0, 0, mask, 0));
            fails(IllegalArgumentException.class, () -> invoke(method, states, 0, 0, 0, mask));
        }
        check(invoke(method, states, 1, 3, 128, 1) == (((long)3<<32)|1L), "direct boundary value");
    }
    private static void unavailable(boolean legacy) {
        check(M3IndexMaskWindow.nativeAvailable() == legacy, "legacy capability changed");
        check(!M3IndexMaskWindow.nativeCountAvailable(), "missing count ABI admitted");
        validation(NATIVE);
        for (int[] masks : new int[][]{{0,0},{1,1},{128,1}}) {
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.countBounded(new byte[0], 0, 0,
                    masks[0], masks[1], NATIVE));
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.countBounded(new byte[]{(byte)255}, 0, 1,
                    masks[0], masks[1], NATIVE));
        }
        if (legacy) {
            int[] out = new int[2];
            long result = M3IndexMaskWindow.scanBounded(new int[]{9,9}, new byte[]{1,1}, 0, 2, 1, 0, out, 0, 2, NATIVE);
            check(result == (((long)2<<32)|2L) && Arrays.equals(out,new int[]{9,9}), "old scans broken");
        }
        check(M3IndexMaskWindow.countBounded(new byte[]{1,2},0,2,1,0,JAVA)==(((long)2<<32)|1L), "Java path missing");
    }
    public static void main(String[] args) throws Throwable {
        String mode = args.length == 0 ? "native" : args[0];
        if (args.length > 1) System.load(args[1]);
        if (mode.equals("missing") || mode.equals("legacy")) {
            unavailable(mode.equals("legacy"));
            System.out.println("COUNT_ABI_PASS mode="+mode+" checks="+checks); return;
        }
        if (mode.equals("bad-result")) {
            check(M3IndexMaskWindow.nativeCountAvailable(), "probe must pass");
            fails(IllegalStateException.class, () -> M3IndexMaskWindow.countBounded(new byte[]{1,2,3},0,3,1,0,NATIVE));
            System.out.println("COUNT_BAD_RESULT_PASS checks="+checks); return;
        }
        M3IndexMaskWindow.Backend backend = mode.equals("java") ? JAVA : NATIVE;
        if (backend == NATIVE) check(M3IndexMaskWindow.nativeCountAvailable(), "count JNI unavailable");
        corpus(backend, mode.endsWith("-quick")); validation(backend);
        if (backend == NATIVE) directNative();
        System.out.println("COUNT_PASS cases="+cases+" checks="+checks+" digest="+Long.toHexString(digest));
    }
}
