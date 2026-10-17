/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

/** Independent occurrence oracle; no product prefix or mask-packing algorithm is duplicated. */
public final class MaskProjectionProbe {
    private static long checks, cases;
    private static volatile Object sink;
    private static MessageDigest tape;
    private MaskProjectionProbe() { }
    private static void check(boolean truth, String why) {
        checks++; if (!truth) throw new AssertionError(why);
    }
    private static void record(int[] values) {
        ByteBuffer bytes = ByteBuffer.allocate(4 * values.length + 4).putInt(values.length);
        for (int v : values) bytes.putInt(v);
        tape.update(bytes.array());
    }
    private static void expect(Class<? extends Throwable> type, Runnable call) {
        try { call.run(); } catch (Throwable e) {
            check(type.isInstance(e), "wrong refusal: " + e); return;
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
    private static int[] oracle(int[] rows, byte[] states, int required) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            boolean match = true;
            for (int bit = 0; bit < 8; bit++) {
                if ((required >>> bit & 1) != 0 && (states[i] >>> bit & 1) == 0) match = false;
            }
            if (match) result.add(rows[i]);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    private static void masks(boolean nativeMode) {
        var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeMode) check(M3IndexMaskCompactor.nativeAvailable(), "native required, not skip");
        for (int n : new int[] {0,1,63,64,65,127,128,129,255,256,257,511,512,513,1025}) {
            int[] rows = new int[n]; byte[] states = new byte[n];
            for (int shape = 0; shape < 4; shape++) {
                for (int i = 0; i < n; i++) {
                    rows[i] = switch (i % 5) { case 0 -> Integer.MIN_VALUE; case 1 -> Integer.MAX_VALUE; default -> i % 11 - 5; };
                    states[i] = (byte) switch (shape) { case 0 -> 255; case 1 -> 0; case 2 -> i; default -> (i / 256 % 2) * 255; };
                }
                int[] frozen = rows.clone(); byte[] frozenStates = states.clone();
                for (int required = 0; required < 256; required++) {
                    int[] expected = oracle(rows, states, required);
                    int[] actual = M3IndexMaskCompactor.select(rows, states, required, backend);
                    check(Arrays.equals(actual, expected), "byte-state/required-mask oracle");
                    check(actual != rows && Arrays.equals(rows, frozen) && Arrays.equals(states, frozenStates), "input/output isolation");
                    check(M3IndexMaskCompactor.selectedCount(states, required) == actual.length, "count parity");
                    record(actual); cases++;
                }
            }
        }
    }
    private static void projections(boolean nativeMode) throws Exception {
        var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        long[] atoms = {Long.MIN_VALUE,Long.MAX_VALUE,0,-1,1,1};
        for (int n = 0; n < 85; n++) {
            long[] source = new long[n]; for (int i = 0; i < n; i++) source[i] = atoms[(i * 5 + n) % atoms.length];
            var base = M3LongIndexProjection.snapshot(source);
            for (int start : new int[] {0,n / 2,n}) {
                var view = base.slice(start,n-start); var expected = new ArrayList<Integer>();
                for (int i = start; i < n; i++) expected.add(i);
                // Keep the original offset here: sorting first materializes a full index
                // and would hide an unsafe unconditional borrow of the parent's index.
                byte[] sliceStates = new byte[view.size()];
                for (int i = 0; i < sliceStates.length; i++) sliceStates[i] = (byte) ((i & 1) == 0 ? 128 : 0);
                int[] selectedSlice = view.filtered(sliceStates,128,backend).physicalIndices();
                check(Arrays.equals(selectedSlice,oracle(view.physicalIndices(),sliceStates,128)), "direct offset filtering");
                record(selectedSlice); cases++;
                for (int step = 0; step < 4; step++) {
                    boolean desc = step % 3 == 0;
                    Comparator<Integer> comparator = Comparator.comparingLong(i -> source[i]);
                    if (desc) comparator = comparator.reversed();
                    expected.sort(comparator);
                    var next = desc ? view.sortedDescending() : view.sortedAscending();
                    check(next != view, "distinct wrapper required"); view = next;
                    check(Arrays.equals(view.physicalIndices(), expected.stream().mapToInt(Integer::intValue).toArray()), "stable opposite/repeated ordering");
                    byte[] flags = new byte[view.size()];
                    for (int i = 0; i < flags.length; i++) flags[i] = (byte) ((i & 1) == 0 ? 0x81 : 1);
                    int[] wanted = oracle(view.physicalIndices(),flags,0x80);
                    var selected = view.filtered(flags,0x80,backend);
                    check(Arrays.equals(selected.physicalIndices(),wanted), "sorted mask and offset order");
                    check(selected.order() == view.order(), "order certificate retained");
                    check(Arrays.equals(view.physicalIndices(),expected.stream().mapToInt(Integer::intValue).toArray()), "owner not mutated");
                    int[] escaped = selected.physicalIndices(); if (escaped.length > 0) escaped[0] = -1;
                    check(selected.isEmpty() || selected.physicalIndex(0) >= 0, "defensive materialization");
                    record(selected.physicalIndices()); cases++;
                }
            }
        }
        var p = M3LongIndexProjection.snapshot(new long[] {3,1,1});
        expect(NullPointerException.class, () -> p.filtered(null,0,backend));
        expect(NullPointerException.class, () -> p.filtered(new byte[3],0,null));
        expect(IllegalArgumentException.class, () -> p.filtered(new byte[2],0,backend));
        expect(IllegalArgumentException.class, () -> p.filtered(new byte[3],256,backend));
        expect(IndexOutOfBoundsException.class, () -> p.slice(Integer.MAX_VALUE,1));
    }
    private static void sharing() throws Exception {
        var field = M3LongIndexProjection.class.getDeclaredField("indices");field.setAccessible(true);
        var base = M3LongIndexProjection.snapshot(new long[] {9,1,2,2,3,0}).sortedAscending();
        var next = base.sortedAscending();
        check(base != next && field.get(base) == field.get(next), "certified order must reuse immutable index");
        var slice = base.slice(1,4); var compact = slice.sortedAscending();
        check(slice != compact && field.get(slice) != field.get(compact), "partial sorted index must compact");
        check(((int[])field.get(compact)).length == compact.size(), "partial result retains no excess index capacity");
        check(Arrays.equals(slice.physicalIndices(),compact.physicalIndices()), "offset geometry unchanged");
        var wrong = base.sortedDescending();
        check(field.get(base) != field.get(wrong), "opposite order cannot reuse index");
        System.out.println("CERTIFIED_ORDER_INDEX_REUSE=PASS");
    }
    private static void surface() {
        List<String> entries = new ArrayList<>();
        for (Class<?> type : List.of(M3LongIndexProjection.class,M3LongIndexProjection.Order.class,M3IndexMaskCompactor.class,M3IndexMaskCompactor.Backend.class)) {
            entries.add(type.toGenericString());
            for (var f : type.getDeclaredFields()) entries.add(f.toGenericString());
            for (var m : type.getDeclaredMethods()) if (Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) entries.add(m.toGenericString());
        }
        entries.sort(String::compareTo); entries.forEach(System.out::println);
    }
    private static void measure(String name, Supplier<?> call) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation measurement unavailable");
        bean.setThreadAllocatedMemoryEnabled(true); long id = Thread.currentThread().threadId();
        for (int i = 0; i < 15; i++) sink = call.get();
        long[] bytes = new long[9], nanos = new long[9];
        for (int i = 0; i < bytes.length; i++) {
            long startBytes = bean.getThreadAllocatedBytes(id), start = System.nanoTime();
            sink = call.get(); nanos[i] = System.nanoTime() - start;
            bytes[i] = bean.getThreadAllocatedBytes(id) - startBytes;
        }
        Arrays.sort(bytes); Arrays.sort(nanos);
        System.out.println(name + "\t" + bytes[4] + "\t" + nanos[4]);
    }
    private static void retention() throws Exception {
        var index = M3LongIndexProjection.class.getDeclaredField("indices"); index.setAccessible(true);
        var payload = M3LongIndexProjection.class.getDeclaredField("values"); payload.setAccessible(true);
        var tiny = M3LongIndexProjection.snapshot(new long[100000]).sortedAscending().slice(5,10).sortedAscending();
        System.out.println("LOGICAL_ITEMS="+tiny.size());
        System.out.println("RETAINED_INDEX_INTS="+((int[])index.get(tiny)).length);
        System.out.println("RETAINED_PAYLOAD_LONGS="+((long[])payload.get(tiny)).length);
    }
    private static void cost() {
        long[] v = new long[100000]; byte[] s = new byte[v.length];
        for (int i = 0; i < v.length; i++) { v[i] = i % 31; s[i] = (byte) ((i / 256 % 4) == 0 ? 0x80 : 0); }
        var base = M3LongIndexProjection.snapshot(v).sortedAscending();
        var range = base.slice(100,base.size()-200); byte[] rangeStates = Arrays.copyOfRange(s,100,s.length-100);
        System.out.println("operation\tallocated_bytes_median\tnanos_median");
        measure("same-order-full",base::sortedAscending);
        measure("same-order-slice",range::sortedAscending);
        measure("opposite-order-full",base::sortedDescending);
        measure("filter-java-full",() -> base.filtered(s,0x80,M3IndexMaskCompactor.Backend.JAVA));
        measure("filter-java-slice",() -> range.filtered(rangeStates,0x80,M3IndexMaskCompactor.Backend.JAVA));
        measure("filter-native-full",() -> base.filtered(s,0x80,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        int[] rows = base.physicalIndices();
        byte[] dense = new byte[rows.length], none = new byte[rows.length], distributed = new byte[rows.length];
        Arrays.fill(dense,(byte)0x80);
        for (int i = 0; i < rows.length; i++) distributed[i] = (byte) (i % 17 == 0 ? 0x80 : 0);
        measure("jni-only-dense",() -> M3IndexMaskCompactor.select(rows,dense,128,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        measure("jni-only-none",() -> M3IndexMaskCompactor.select(rows,none,128,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        measure("jni-only-distributed",() -> M3IndexMaskCompactor.select(rows,distributed,128,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        measure("jni-only-clustered",() -> M3IndexMaskCompactor.select(rows,s,128,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
    }
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "java" : args[0];
        if (mode.equals("surface")) { surface();return; }
        if (mode.equals("sharing")) { sharing();return; }
        if (mode.equals("cost")) { cost();return; }
        if (mode.equals("retention")) { retention();return; }
        if (mode.equals("missing")) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "must run without provider");
            for (int n : new int[] {0,1,257}) {
                var p = M3LongIndexProjection.snapshot(new long[n]);
                expect(UnsatisfiedLinkError.class,() -> p.filtered(new byte[n],0,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
            System.out.println("MISSING_NATIVE_REFUSALS=3");return;
        }
        tape = MessageDigest.getInstance("SHA-256");
        boolean nativeMode = mode.equals("native"); masks(nativeMode); projections(nativeMode);
        System.out.println("CASES="+cases);System.out.println("CHECKS="+checks);
        System.out.println("TAPE="+HexFormat.of().formatHex(tape.digest()));
    }
}
