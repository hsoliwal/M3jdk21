/* Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskWindow;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Real JNI and Java parity with stable ordered arrays, masks, offsets and resume cursors. */
public final class MaskWindowConvergence {
    private static long checks;
    private MaskWindowConvergence() { }
    private static void check(boolean b, String message) { checks++; if (!b) throw new AssertionError(message); }
    private static long reference(int[] rows, byte[] states, int from, int end, int must, int not,
            int[] out, int offset, int limit) {
        int next = from, count = 0;
        if (limit == 0) return ((long) from << 32);
        if ((must & not) != 0) return ((long) end << 32);
        while (next < end && count < limit) {
            int flags = states[next] & 255;
            if ((flags & must) == must && (flags & not) == 0) out[offset + count++] = rows[next];
            next++;
        }
        return ((long) next << 32) | count;
    }
    private static void runCase(int[] rows, byte[] states, int first, int max, int must, int not, int limit) {
        int[] expected = new int[limit + 7]; Arrays.fill(expected, 0x57575757);
        int[] java = expected.clone(), nativeOut = expected.clone();
        int end = first + Math.min(max, rows.length - first);
        long truth = reference(rows, states, first, end, must, not, expected, 3, limit);
        long a = M3IndexMaskWindow.scanBounded(rows, states, first, max, must, not, java, 3, limit,
                M3IndexMaskCompactor.Backend.JAVA);
        long b = M3IndexMaskWindow.scanBounded(rows, states, first, max, must, not, nativeOut, 3, limit,
                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        check(a == truth && b == truth, "bounded cursor");
        check(Arrays.equals(expected, java), "Java window contents/guard");
        check(Arrays.equals(expected, nativeOut), "JNI window contents/guard");
        Arrays.fill(expected, 0x57575757); Arrays.fill(nativeOut, 0x57575757);
        truth = reference(rows, states, first, rows.length, must, not, expected, 3, limit);
        b = M3IndexMaskWindow.scan(rows, states, first, must, not, nativeOut, 3, limit,
                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        check(truth == b && Arrays.equals(expected, nativeOut), "legacy full scan parity");
    }
    private static void invalid(Method nativeMethod, Object[] args, int[] output) throws Exception {
        int[] before = output.clone();
        try { nativeMethod.invoke(null, args); throw new AssertionError("missing native refusal"); }
        catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IllegalArgumentException, "native refusal type");
        }
        check(Arrays.equals(before, output), "native validation before writes");
    }
    private static void refusals() throws Exception {
        Method method = M3IndexMaskWindow.class.getDeclaredMethod("scanBoundedNative", int[].class, byte[].class,
                int.class, int.class, int.class, int.class, int[].class, int.class, int.class);
        method.setAccessible(true);
        int[] rows = {7, 7, Integer.MIN_VALUE, Integer.MAX_VALUE}; byte[] flags = {1, 1, 1, 1};
        int[] output = {66, 66, 66, 66};
        Object[] valid = {rows, flags, 0, 4, 1, 0, output, 0, 4};
        for (Object[] change : new Object[][]{{0, null}, {1, null}, {1, new byte[3]}, {2, -1},
                {3, 5}, {4, 256}, {5, -1}, {7, -1}, {8, 5}, {8, 4097}, {8, -1}}) {
            Object[] args = valid.clone(); args[(int) change[0]] = change[1]; invalid(method, args, output);
        }
        Object[] alias = valid.clone(); alias[6] = rows; invalid(method, alias, rows);
        // Even a zero limit cannot bypass geometry/mask checks.
        Object[] invalidZero = valid.clone(); invalidZero[4] = -1; invalidZero[8] = 0;
        invalid(method, invalidZero, output);
    }
    private static void resumes() {
        int[] rows = new int[300]; byte[] states = new byte[300];
        for (int i = 0; i < rows.length; i++) rows[i] = i % 7;
        states[127] = states[128] = states[299] = 1;
        int position = 0, total = 0; int[] collected = new int[3];
        while (position < rows.length) {
            int[] out = new int[1];
            long value = M3IndexMaskWindow.scanBounded(rows, states, position, 17, 1, 0, out, 0, 1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            int next = M3IndexMaskWindow.nextIndex(value), count = M3IndexMaskWindow.written(value);
            check(next > position && next - position <= 17, "progress/budget");
            if (count > 0) collected[total++] = out[0];
            position = next;
        }
        check(total == 3 && Arrays.equals(collected, new int[]{127 % 7, 128 % 7, 299 % 7}), "resume order/duplicates");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "missing library stays missing");
            int[] untouched = {99};
            try {
                M3IndexMaskWindow.scan(new int[]{7}, new byte[]{1}, 0, 1, 0,
                        untouched, 0, 0, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                throw new AssertionError("missing library silently accepted");
            } catch (UnsatisfiedLinkError expected) { checks++; }
            check(untouched[0] == 99, "missing library no writes");
            System.out.println("MASK_WINDOW_MISSING_NATIVE_PASS checks=" + checks);
            return;
        }
        check(M3IndexMaskCompactor.nativeAvailable(), "required native library");
        Random r = new Random(0x4d41534b57494eL);
        // Exhaust every pair of byte masks over every possible state value.
        byte[] everyState = new byte[256]; int[] everyRow = new int[256];
        for (int i = 0; i < 256; i++) { everyState[i] = (byte) i; everyRow[i] = 255 - i; }
        for (int must = 0; must < 256; must++) for (int not = 0; not < 256; not++) {
            runCase(everyRow, everyState, 0, 256, must, not, 256);
        }
        for (int n : new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257, 4096, 8193}) {
            int[] rows = new int[n]; byte[] states = new byte[n];
            for (int i = 0; i < n; i++) { rows[i] = i % 4 == 0 ? Integer.MIN_VALUE : i % 4 == 1 ? Integer.MAX_VALUE : r.nextInt(8); states[i] = (byte) r.nextInt(256); }
            int[] frozenRows = rows.clone(); byte[] frozenStates = states.clone();
            for (int trial = 0; trial < 150; trial++) runCase(rows, states, r.nextInt(n + 1),
                    trial % 3 == 0 ? Integer.MAX_VALUE : r.nextInt(300), r.nextInt(256), r.nextInt(256), r.nextInt(130));
            check(Arrays.equals(rows, frozenRows) && Arrays.equals(states, frozenStates), "input nonmutation");
        }
        resumes(); refusals();
        System.out.println("MASK_WINDOW_CONVERGENCE_PASS checks=" + checks);
    }
}
