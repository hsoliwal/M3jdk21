/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Independent scalar differential and real JNI contract proof for bounded scan work. */
public final class M3BudgetedMaskWindowContract {
    private static final int SENTINEL = 0x74ac013d;
    private static int assertions;
    private M3BudgetedMaskWindowContract() { }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static long expected(int[] rows, byte[] states, int first, int budget,
            int required, int excluded, int[] out, int offset, int limit) {
        int next = first, written = 0;
        long end = Math.min(rows.length, (long) first + budget);
        while (next < end && written < limit) {
            int flags = Byte.toUnsignedInt(states[next]);
            if ((flags & required) == required && (flags & excluded) == 0)
                out[offset + written++] = rows[next];
            next++;
        }
        return ((long) next << 32) | (written & 0xffffffffL);
    }
    private static void one(M3IndexMaskCompactor.Backend backend, int[] rows, byte[] states,
            int first, int budget, int required, int excluded, int limit, int offset) {
        int[] output = new int[offset + limit + 7], oracle = new int[output.length];
        Arrays.fill(output, SENTINEL); Arrays.fill(oracle, SENTINEL);
        int[] original = rows.clone(); byte[] flags = states.clone();
        long want = expected(rows, states, first, budget, required, excluded, oracle, offset, limit);
        long got = M3IndexMaskWindow.scanBounded(rows, states, first, budget, required,
                excluded, output, offset, limit, backend);
        check(want == got, "exact progress/count " + first + "/" + budget + "/" + limit);
        check(Arrays.equals(oracle, output), "output and untouched guards");
        check(Arrays.equals(rows, original) && Arrays.equals(states, flags), "no input mutation");
        check((long) M3IndexMaskWindow.nextIndex(got) - first <= budget, "input work bound");
        if (budget == Integer.MAX_VALUE) {
            int[] legacy = new int[output.length]; Arrays.fill(legacy, SENTINEL);
            long old = M3IndexMaskWindow.scan(rows, states, first, required, excluded,
                    legacy, offset, limit, backend);
            check(old == got && Arrays.equals(legacy, output), "legacy full-scan parity");
        }
    }
    private static void cases(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x171053L);
        int[] sizes = {0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097};
        for (int n : sizes) {
            int[] rows = new int[n]; byte[] states = new byte[n];
            for (int i = 0; i < n; i++) { rows[i] = i % 5 == 0 ? Integer.MIN_VALUE : i - n / 2; states[i] = (byte) i; }
            for (int budget : new int[] {0, 1, 63, 128, 129, 4096, Integer.MAX_VALUE})
                for (int cap : new int[] {0, 1, 7, 128, 4096}) {
                    one(backend, rows, states, 0, budget, 0, 0, cap, 3);
                    one(backend, rows, states, n / 2, budget, 128, 1, cap, 0);
                    one(backend, rows, states, n, budget, 255, 255, cap, 5);
                }
        }
        for (int test = 0; test < 5000; test++) {
            int n = random.nextInt(1600); int[] rows = new int[n]; byte[] states = new byte[n];
            for (int i = 0; i < n; i++) { rows[i] = random.nextInt(); states[i] = (byte) random.nextInt(256); }
            int budget = test % 13 == 0 ? Integer.MAX_VALUE : random.nextInt(1800);
            one(backend, rows, states, random.nextInt(n + 1), budget, random.nextInt(256),
                    random.nextInt(256), random.nextInt(130), random.nextInt(6));
        }
        int n = 1_000_000; int[] rows = new int[n]; byte[] states = new byte[n];
        rows[n - 1] = 42; states[n - 1] = (byte) 128;
        int[] out = new int[2];
        long step = M3IndexMaskWindow.scanBounded(rows, states, 0, 512, 128, 0, out, 0, 2, backend);
        check(M3IndexMaskWindow.nextIndex(step) == 512 && M3IndexMaskWindow.written(step) == 0,
                "sparse million-row source yields after exactly 512 ordinals");
        long legacy = M3IndexMaskWindow.scan(rows, states, 0, 128, 0, out, 0, 2, backend);
        check(M3IndexMaskWindow.nextIndex(legacy) == n && M3IndexMaskWindow.written(legacy) == 1,
                "legacy scan deliberately keeps its whole-remainder contract");
        List<Integer> selected = new ArrayList<>(); int cursor = 0, calls = 0;
        while (cursor < n) {
            step = M3IndexMaskWindow.scanBounded(rows, states, cursor, 512, 128, 0, out, 0, 2, backend);
            int next = M3IndexMaskWindow.nextIndex(step);
            check(next > cursor && next - cursor <= 512, "resume always progresses with positive budgets");
            cursor = next; calls++;
            for (int i = 0; i < M3IndexMaskWindow.written(step); i++) selected.add(out[i]);
        }
        check(selected.equals(List.of(42)) && calls == 1954, "all sparse windows reconcile once");

        // Companion to a real live sorted Map view: only precomputed primitive state crosses JNI.
        TreeMap<Integer, Integer> owner = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (int i = -200; i < 200; i++) owner.put(i, i * i);
        var view = M3MutableFilteredViews.filterEntries(owner, (k, v) -> (v & 1) == 0);
        int[] ordered = new int[owner.size()]; byte[] state = new byte[owner.size()];
        int pos = 0;
        for (var entry : owner.entrySet()) { ordered[pos] = entry.getKey(); state[pos++] = (byte) ((entry.getValue() & 1) == 0 ? 1 : 0); }
        List<Integer> actual = new ArrayList<>(); cursor = 0;
        while (cursor < ordered.length) {
            step = M3IndexMaskWindow.scanBounded(ordered, state, cursor, 7, 1, 0, out, 0, 2, backend);
            cursor = M3IndexMaskWindow.nextIndex(step);
            for (int i = 0; i < M3IndexMaskWindow.written(step); i++) actual.add(out[i]);
        }
        check(actual.equals(new ArrayList<>(view.navigableKeySet())), "live comparator order equals primitive projection");
        check(Arrays.equals(M3IndexMaskCompactor.select(ordered, state, 1, backend),
                actual.stream().mapToInt(Integer::intValue).toArray()), "existing whole-selection JNI peer parity");
    }
    private static void refusal(Class<? extends Throwable> type, Runnable call, int[] output) {
        int[] before = output.clone();
        try { call.run(); }
        catch (RuntimeException | LinkageError error) {
            check(type.isInstance(error), "wrong refusal " + error);
            check(Arrays.equals(before, output), "refused operation changed output");
            return;
        }
        throw new AssertionError("missing refusal " + type.getSimpleName());
    }
    private static void invalid(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {1, 2, 3}, output = {9, 9, 9}; byte[] states = {1, 1, 1};
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, -1, 1, 0, output, 0, 1, backend), output);
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 256, 0, output, 0, 1, backend), output);
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 1, -1, output, 0, 1, backend), output);
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, new byte[1], 0, 1, 1, 0, output, 0, 1, backend), output);
        refusal(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 4, 1, 1, 0, output, 0, 1, backend), output);
        refusal(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 1, 0, output, Integer.MAX_VALUE, 1, backend), output);
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 1, 0, output, 0, 4097, backend), output);
        refusal(NullPointerException.class, () -> M3IndexMaskWindow.scanBounded(null, states, 0, 1, 1, 0, output, 0, 1, backend), output);
        refusal(NullPointerException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 1, 0, output, 0, 1, null), output);
        refusal(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 1, 0, rows, 0, 1, backend), rows);
    }
    private static void nativeRefusals() throws Exception {
        Method method = M3IndexMaskWindow.class.getDeclaredMethod("scanBoundedNative", int[].class, byte[].class,
                int.class, int.class, int.class, int.class, int[].class, int.class, int.class);
        method.setAccessible(true);
        int[] rows = {1, 2, 3}, output = {77, 77, 77}; byte[] flags = {1, 1, 1};
        Object[][] args = {
            {null, flags, 0, 2, 1, 0, output, 0, 2}, {rows, null, 0, 2, 1, 0, output, 0, 2},
            {rows, flags, 0, 2, 1, 0, null, 0, 2}, {rows, flags, -1, 2, 1, 0, output, 0, 2},
            {rows, flags, 0, 4, 1, 0, output, 0, 2}, {rows, flags, 2, 1, 1, 0, output, 0, 2},
            {rows, flags, 0, 2, 256, 0, output, 0, 2}, {rows, flags, 0, 2, 1, -1, output, 0, 2},
            {rows, flags, 0, 2, 1, 0, output, 2, 2}, {rows, flags, 0, 2, 1, 0, output, 0, 4097},
            {rows, flags, 0, 2, 1, 0, rows, 0, 2}, {rows, new byte[1], 0, 2, 1, 0, output, 0, 2}
        };
        for (Object[] arguments : args) {
            int[] a = rows.clone(), b = output.clone();
            try { method.invoke(null, arguments); throw new AssertionError("native boundary did not refuse"); }
            catch (InvocationTargetException e) { check(e.getCause() instanceof IllegalArgumentException, "native refusal type"); }
            check(Arrays.equals(a, rows) && Arrays.equals(b, output), "native boundary no writes");
        }
    }
    public static void main(String[] args) throws Exception {
        boolean nativeMode = args.length != 0 && args[0].equals("--native");
        if (args.length != 0 && (args[0].equals("--missing") || args[0].equals("--old-binary"))) {
            int[] out = {99};
            refusal(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scanBounded(new int[] {1},
                    new byte[] {1}, 0, 1, 1, 0, out, 0, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), out);
            if (args[0].equals("--old-binary")) {
                long old = M3IndexMaskWindow.scan(new int[] {1}, new byte[] {1}, 0, 1, 0,
                        out, 0, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                check(M3IndexMaskWindow.written(old) == 1 && out[0] == 1, "old ABI still callable");
            }
            System.out.println("EXPLICIT_NATIVE_REFUSAL_PASS " + args[0] + " assertions=" + assertions); return;
        }
        var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeMode) check(M3IndexMaskCompactor.nativeAvailable(), "actual native library required");
        cases(backend); invalid(backend);
        if (nativeMode) nativeRefusals();
        System.out.println("BUDGETED_MASK_WINDOW_PASS random=5000 assertions=" + assertions + " backend=" + backend);
    }
}
