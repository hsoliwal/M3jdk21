/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Frozen differential oracle over the actual existing Java owners and optional JNI exports. */
public final class M3MaskWindowKernelSelfTest {
    private static long checks;
    private static final MessageDigest TRACE = digest();
    private M3MaskWindowKernelSelfTest() { }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }
    private static void check(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError(label);
    }
    private static void trace(long value) {
        for (int shift = 56; shift >= 0; shift -= 8) TRACE.update((byte) (value >>> shift));
    }
    private static void ints(int[] expected, int[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
        trace(actual.length);
        for (int value : actual) trace(value);
    }
    private static void longs(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
        trace(actual.length);
        for (long value : actual) trace(value);
    }
    @FunctionalInterface private interface Attempt { void run() throws Throwable; }
    private static void refuses(Class<? extends Throwable> type, String message, Attempt attempt) {
        try { attempt.run(); }
        catch (Throwable ex) {
            check(ex.getClass() == type, "exception type: " + ex);
            if (message != null) check(message.equals(ex.getMessage()), "exception message: " + ex);
            TRACE.update(ex.getClass().getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        throw new AssertionError("missing refusal " + type.getName());
    }
    private static int[] expected(int[] rows, int ro, byte[] flags, int so, int n, int mask) {
        int[] scratch = new int[n];
        int kept = 0;
        for (int i = 0; i < n; i++) {
            int value = Byte.toUnsignedInt(flags[so + i]);
            boolean accept = true;
            // Bit-by-bit oracle is independent of the production bitwise equality expression.
            for (int bit = 0; bit < 8; bit++) {
                if ((mask >>> bit) % 2 != 0 && (value >>> bit) % 2 == 0) accept = false;
            }
            if (accept) scratch[kept++] = rows[ro + i];
        }
        return Arrays.copyOf(scratch, kept);
    }
    private static long[] expected(long[] rows, int ro, byte[] flags, int so, int n, int mask) {
        int[] ordinals = new int[n];
        for (int i = 0; i < n; i++) ordinals[i] = i;
        int[] selected = expected(ordinals, 0, flags, so, n, mask);
        long[] result = new long[selected.length];
        for (int i = 0; i < selected.length; i++) result[i] = rows[ro + selected[i]];
        return result;
    }
    private static void fixture(int n, int ro, int so, int mask, int pattern,
            M3IndexMaskCompactor.Backend backend) {
        int[] rows = new int[ro + n + 19];
        long[] wide = new long[rows.length];
        byte[] flags = new byte[so + n + 23];
        Arrays.fill(rows, 0x13579bdf);
        Arrays.fill(wide, 0x0123456789abcdefL);
        Arrays.fill(flags, (byte) 0x55);
        for (int i = 0; i < n; i++) {
            rows[ro + i] = switch (i % 11) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                default -> (i % 17) - 8;
            };
            wide[ro + i] = switch (i % 13) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                default -> ((long) rows[ro + i] << 32) ^ i % 19;
            };
            flags[so + i] = (byte) switch (pattern) {
                case 0 -> i;
                case 1 -> 0;
                case 2 -> 255;
                case 3 -> (i / 256 == 2 ? 255 : 0);
                case 4 -> (i % 257 == 256 ? 128 : 0);
                default -> (i * 73 + n * 11 + mask) & 255;
            };
        }
        int[] untouched = rows.clone(); long[] untouchedWide = wide.clone(); byte[] untouchedFlags = flags.clone();
        int[] want = expected(rows, ro, flags, so, n, mask);
        long[] wantLong = expected(wide, ro, flags, so, n, mask);
        int[] got = M3IndexMaskCompactor.selectWindow(rows, ro, flags, so, n, mask, backend);
        long[] gotLong = M3IndexMaskCompactor.selectLongWindow(wide, ro, flags, so, n, mask, backend);
        ints(want, got, "int independent offsets"); longs(wantLong, gotLong, "long independent offsets");
        check(got != rows && gotLong != wide, "detached outputs including empty and all-pass");
        int[] whole = Arrays.copyOfRange(rows, ro, ro + n);
        long[] wholeLong = Arrays.copyOfRange(wide, ro, ro + n);
        byte[] aligned = Arrays.copyOfRange(flags, so, so + n);
        ints(want, M3IndexMaskCompactor.select(whole, aligned, mask, backend), "whole int");
        longs(wantLong, M3IndexMaskCompactor.selectLong(wholeLong, aligned, mask), "whole long Java");
        check(M3IndexMaskCompactor.selectedCount(aligned, mask) == want.length, "count");
        int[] ordinals = new int[n];
        for (int i = 0; i < n; i++) ordinals[i] = i;
        ints(expected(ordinals, 0, aligned, 0, n, mask),
                M3IndexMaskCompactor.selectPositions(aligned, mask, backend),
                "new master position API retained");
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            ints(want, M3IndexMaskCompactor.selectNativeRange(rows, ro, n, aligned, mask), "private offset range");
        }
        if (got.length != 0) got[0] ^= 17;
        if (gotLong.length != 0) gotLong[0] ^= 17;
        check(Arrays.equals(rows, untouched), "row input unchanged");
        check(Arrays.equals(wide, untouchedWide), "long input unchanged");
        check(Arrays.equals(flags, untouchedFlags), "flag input unchanged");
    }
    private static void validation(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {3}; long[] wide = {Long.MIN_VALUE}; byte[] flags = {(byte) 255};
        refuses(NullPointerException.class, "orderedRows", () -> M3IndexMaskCompactor.select(null, null, -1, null));
        refuses(NullPointerException.class, "rowStates", () -> M3IndexMaskCompactor.select(rows, null, -1, null));
        refuses(NullPointerException.class, "backend", () -> M3IndexMaskCompactor.select(rows, flags, -1, null));
        refuses(IllegalArgumentException.class, "index and aligned state lengths differ",
                () -> M3IndexMaskCompactor.select(rows, new byte[0], -1, backend));
        refuses(IllegalArgumentException.class, "required mask must fit an unsigned byte",
                () -> M3IndexMaskCompactor.select(rows, flags, 256, backend));
        refuses(NullPointerException.class, "states",
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, null, -1, -1, -1, null));
        refuses(NullPointerException.class, "backend",
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, flags, -1, -1, -1, null));
        for (int[] geometry : new int[][]{{-1,0,1},{0,-1,1},{0,0,-1},{1,0,1},{0,1,1},
                {Integer.MAX_VALUE,0,1},{0,Integer.MAX_VALUE,1},{0,0,Integer.MAX_VALUE}}) {
            refuses(IndexOutOfBoundsException.class, null,
                    () -> M3IndexMaskCompactor.selectWindow(rows, geometry[0], flags, geometry[1], geometry[2], 256, backend));
            refuses(IndexOutOfBoundsException.class, null,
                    () -> M3IndexMaskCompactor.selectLongWindow(wide, geometry[0], flags, geometry[1], geometry[2], 256, backend));
        }
        for (int mask : new int[]{-1,256,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            refuses(IllegalArgumentException.class, "required mask must fit an unsigned byte",
                    () -> M3IndexMaskCompactor.selectLongWindow(wide,0,flags,0,1,mask,backend));
            refuses(IllegalArgumentException.class, "required mask must fit an unsigned byte",
                    () -> M3IndexMaskCompactor.selectedCount(flags,mask));
        }
    }
    private static void projections(M3IndexMaskCompactor.Backend backend) {
        long[] input = new long[1300];
        for (int i = 0; i < input.length; i++) input[i] = i % 37 - 18;
        input[0] = Long.MIN_VALUE; input[1] = Long.MAX_VALUE;
        var root = M3LongIndexProjection.snapshot(input);
        Arrays.fill(input, 7);
        for (boolean descending : new boolean[]{false,true}) {
            var sorted = descending ? root.sortedDescending() : root.sortedAscending();
            var slice = sorted.slice(17,1025).slice(7,1001);
            byte[] flags = new byte[slice.size()];
            for (int i = 0; i < flags.length; i++) flags[i] = (byte)(i / 256 == 2 ? 128 : 0);
            int[] want = expected(slice.physicalIndices(),0,flags,0,flags.length,128);
            var filtered = slice.filtered(flags,128,backend);
            ints(want,filtered.physicalIndices(),"sorted/sliced physical occurrence identity");
            check(filtered.order() == slice.order(),"sort metadata retained");
            for (int i = 0; i < filtered.size(); i++) {
                check(filtered.get(i) == root.get(want[i]),"payload owner preserved");
                if (i != 0 && filtered.get(i) == filtered.get(i-1))
                    check(filtered.physicalIndex(i) > filtered.physicalIndex(i-1),"stable tie order");
            }
            var again = descending ? filtered.sortedDescending() : filtered.sortedAscending();
            ints(want,again.physicalIndices(),"repeat sorted view");
            check(again != filtered,"distinct immutable descriptor");
            long[] copy = again.toArray(); if (copy.length != 0) copy[0] ^= 0x5a;
            ints(want,again.physicalIndices(),"materialization does not mutate index");
        }
    }
    private static void invoke(Method method, Object... arguments) throws Throwable {
        try { method.invoke(null,arguments); }
        catch (InvocationTargetException exception) { throw exception.getCause(); }
    }
    private static void nativeValidation() throws Exception {
        for (Class<?> rowType : new Class<?>[]{int[].class,long[].class}) {
            String name = rowType == int[].class ? "selectWindowNative" : "selectLongWindowNative";
            Method method = M3IndexMaskCompactor.class.getDeclaredMethod(name,rowType,int.class,byte[].class,
                    int.class,int.class,int.class);
            method.setAccessible(true);
            Object rows = rowType == int[].class ? new int[2] : new long[2];
            for (int[] g : new int[][]{{-1,0,0,0},{0,-1,0,0},{0,0,-1,0},{2,0,1,0},
                    {0,2,1,0},{Integer.MAX_VALUE,0,1,0},{0,0,0,256},{0,0,0,-1}}) {
                refuses(IllegalArgumentException.class,"invalid indexed window or unsigned-byte mask",
                        () -> invoke(method,rows,g[0],new byte[2],g[1],g[2],g[3]));
            }
            refuses(IllegalArgumentException.class,"invalid indexed window or unsigned-byte mask",
                    () -> invoke(method,null,0,new byte[0],0,0,0));
        }
    }
    public static void main(String[] args) throws Exception {
        boolean nativeMode = args.length == 1 && args[0].equals("--native");
        if (args.length == 1 && args[0].equals("--missing")) {
            check(!M3IndexMaskCompactor.nativeAvailable(),"native library must be absent for this proof");
            for (int n : new int[]{0,1,257}) {
                refuses(UnsatisfiedLinkError.class,"m3_index_mask JNI library unavailable",
                        () -> M3IndexMaskCompactor.selectPositions(new byte[n],0,
                                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
                refuses(UnsatisfiedLinkError.class,"m3_index_mask JNI library unavailable",
                        () -> M3IndexMaskCompactor.select(new int[n],new byte[n],0,nativeBackend));
                refuses(UnsatisfiedLinkError.class,"m3_index_mask JNI library unavailable",
                        () -> M3IndexMaskCompactor.selectWindow(new int[n],0,new byte[n],0,n,0,nativeBackend));
                refuses(UnsatisfiedLinkError.class,"m3_index_mask JNI library unavailable",
                        () -> M3IndexMaskCompactor.selectLongWindow(new long[n],0,new byte[n],0,n,0,nativeBackend));
            }
        } else {
            if (nativeMode) check(M3IndexMaskCompactor.nativeAvailable(),"native requested, no fallback");
            var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
            for (int mask = 0; mask < 256; mask++) fixture(256,17,31,mask,0,backend);
            for (int n : new int[]{0,1,2,63,64,65,127,128,129,255,256,257,511,512,513,1023,1024,1025,4097})
                for (int mask : new int[]{0,1,3,15,127,128,129,255})
                    for (int pattern=0;pattern<6;pattern++) fixture(n,n%19,n%31,mask,pattern,backend);
            for (int trial=0;trial<600;trial++) fixture(trial*37%1537,trial%29,trial%47,trial*53&255,5,backend);
            validation(backend); projections(backend);
            if (nativeMode) nativeValidation();
        }
        System.out.println("M3_MASK_WINDOW_PASS checks="+checks+" trace="+HexFormat.of().formatHex(TRACE.digest()));
    }
}
