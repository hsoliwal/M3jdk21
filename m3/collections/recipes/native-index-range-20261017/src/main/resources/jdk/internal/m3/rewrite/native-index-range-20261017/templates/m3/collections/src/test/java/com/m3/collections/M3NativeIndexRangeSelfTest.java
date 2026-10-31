/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import static com.m3.collections.M3IndexMaskCompactor.Backend.*;

/** Tests the actual product classes and real JNI; reference boxing is test-only. */
public final class M3NativeIndexRangeSelfTest {
    private static long checks;
    private static volatile Object sink;
    private static void check(boolean result) {
        checks++;
        if (!result) throw new AssertionError("range check " + checks);
    }
    private static void same(int[] expected, int[] actual) {
        check(Arrays.equals(expected, actual));
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) { check(type.isInstance(failure)); return; }
        throw new AssertionError("expected " + type.getName());
    }
    private static Object field(Object value, String name) throws ReflectiveOperationException {
        Field f = value.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(value);
    }
    private static Object call(Method method, Object... arguments) {
        try { return method.invoke(null, arguments); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw new AssertionError(failure.getCause());
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void compare(M3LongIndexProjection view, int[] expected, long[] values) {
        same(expected, view.physicalIndices());
        check(expected.length == view.size() && view.isEmpty() == (expected.length == 0));
        for (int i = 0; i < expected.length; i++) {
            check(view.physicalIndex(i) == expected[i]);
            check(view.get(i) == values[expected[i]]);
        }
        int[] leak = view.physicalIndices();
        if (leak.length > 0) { int saved = view.physicalIndex(0); leak[0] = -123; check(saved == view.physicalIndex(0)); }
    }
    private static void directRange() throws Exception {
        Method range = M3IndexMaskCompactor.class.getDeclaredMethod("selectRangeNative",
                int[].class, byte[].class, int.class, int.class, int.class);
        range.setAccessible(true);
        int[] rows = {101, 102, -1, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 103};
        byte[] states = {(byte)128, 0, (byte)255, (byte)129};
        same(new int[]{-1, -1, Integer.MIN_VALUE}, (int[]) call(range, rows, states, 2, 4, 128));
        same(new int[]{}, (int[]) call(range, rows, new byte[0], rows.length, 0, 255));
        for (Object[] args : new Object[][] {
                {null, states, 0, 4, 1}, {rows, null, 0, 0, 1},
                {rows, states, -1, 4, 1}, {rows, states, 0, -1, 1},
                {rows, states, Integer.MAX_VALUE, 4, 1}, {rows, states, 2, Integer.MAX_VALUE, 1},
                {rows, states, 5, 4, 1}, {rows, states, 2, 3, 1},
                {rows, states, 2, 4, -1}, {rows, states, 2, 4, 256}}) {
            fails(IllegalArgumentException.class, () -> call(range, args));
        }
        same(new int[]{-1, Integer.MAX_VALUE, -1, Integer.MIN_VALUE}, (int[]) call(range, rows, states, 2, 4, 0));
        same(new int[]{101, 102, -1, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 103}, rows);
        // Public legacy symbol and new symbol both remain functional after exception paths.
        same(rows, M3IndexMaskCompactor.select(rows, new byte[rows.length], 0, NATIVE_REQUIRED));
    }
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "none" : args[0];
        boolean nativeRun = !mode.equals("none");
        check(M3IndexMaskCompactor.nativeAvailable() == nativeRun);
        if (mode.equals("sentinel")) {
            try {
                M3LongIndexProjection.snapshot(new long[]{5,2,1}).slice(1,2)
                    .filtered(new byte[]{1,1},1,NATIVE_REQUIRED);
                throw new AssertionError("native-required was replaced by Java");
            } catch (UnsatisfiedLinkError expected) {
                check(expected.getMessage().contains("legacy-native-witness"));
            }
            System.out.println("LegacyNativeDispatchWitness PASS checks="+checks); return;
        }
        Random random = new Random(2026101711594L);
        int[] boundaries = {0,1,2,63,64,65,127,128,129,255,256,257,511,512,513,1023,1024,1025};
        for (int trial = 0; trial < 2048; trial++) {
            int n = trial < boundaries.length ? boundaries[trial] : random.nextInt(1026);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) values[i] = switch (random.nextInt(12)) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; default -> random.nextInt(17)-8;
            };
            long[] original = values.clone();
            M3LongIndexProjection base = M3LongIndexProjection.snapshot(values);
            if (n > 0) values[0] ^= 17; // Must not affect the immutable payload.
            Integer[] order = new Integer[n]; Arrays.setAll(order,i -> i);
            Comparator<Integer> cmp = Comparator.comparingLong(i -> original[i]);
            switch (trial % 3) {
                case 1 -> { base = base.sortedAscending(); Arrays.sort(order, cmp); }
                case 2 -> { base = base.sortedDescending(); Arrays.sort(order, cmp.reversed()); }
                default -> { }
            }
            int offset = random.nextInt(n+1), length = random.nextInt(n-offset+1);
            M3LongIndexProjection slice = base.slice(offset,length);
            int inner = random.nextInt(length+1), size = length-inner;
            slice = slice.slice(inner,size);
            byte[] states = new byte[size]; random.nextBytes(states);
            int mask = trial%4==0 ? 0 : trial%4==1 ? 128 : trial%4==2 ? 255 : random.nextInt(256);
            byte[] savedStates = states.clone();
            int[] expected = new int[size]; int k=0;
            for (int i=0;i<size;i++) if (((states[i]&255)&mask)==mask) expected[k++]=order[offset+inner+i];
            expected=Arrays.copyOf(expected,k);
            M3LongIndexProjection javaView = slice.filtered(states,mask,JAVA);
            compare(javaView,expected,original); check(javaView.order()==slice.order());
            if (nativeRun) {
                M3LongIndexProjection nativeView=slice.filtered(states,mask,NATIVE_REQUIRED);
                compare(nativeView,expected,original); check(nativeView.order()==slice.order());
                check(Arrays.equals(javaView.toArray(), nativeView.toArray()));
            }
            check(Arrays.equals(states,savedStates));
            M3LongIndexProjection asc=javaView.sortedAscending();
            Integer[] sorted=Arrays.stream(expected).boxed().toArray(Integer[]::new);
            Arrays.sort(sorted,cmp); compare(asc,Arrays.stream(sorted).mapToInt(Integer::intValue).toArray(),original);
            compare(asc.sortedAscending(),asc.physicalIndices(),original);
            // Descending must be stable, not a naive reversal of equal-key occurrences.
            Arrays.sort(sorted,cmp.reversed());
            M3LongIndexProjection desc=asc.sortedDescending();
            compare(desc,Arrays.stream(sorted).mapToInt(Integer::intValue).toArray(),original);
            compare(desc.sortedDescending(),desc.physicalIndices(),original);
        }
        var view=M3LongIndexProjection.snapshot(new long[]{7,1,1,9,-2}).sortedAscending().slice(1,3);
        fails(NullPointerException.class, () -> view.filtered(null,1,JAVA));
        fails(NullPointerException.class, () -> view.filtered(new byte[3],1,null));
        fails(IllegalArgumentException.class, () -> view.filtered(new byte[2],1,JAVA));
        fails(IllegalArgumentException.class, () -> view.filtered(new byte[3],256,JAVA));
        fails(IllegalArgumentException.class, () -> view.filtered(new byte[3],-1,NATIVE_REQUIRED));
        if (!nativeRun) {
            fails(UnsatisfiedLinkError.class, () -> view.filtered(new byte[3],0,NATIVE_REQUIRED));
            fails(UnsatisfiedLinkError.class, () -> view.slice(0,0).filtered(new byte[0],0,NATIVE_REQUIRED));
        }
        if (mode.equals("range")) directRange();
        if (args.length > 1 && args[1].equals("sharing")) {
            var repeated=view.sortedAscending();
            check(repeated!=view); check(field(view,"indices")==field(repeated,"indices"));
            check(field(view,"values")==field(repeated,"values"));
            check(field(view,"from").equals(field(repeated,"from")));
            check(field(view,"to").equals(field(repeated,"to")));
            var filtered=view.filtered(new byte[]{1,0,1},1,JAVA);
            check(field(filtered,"indices")==field(filtered.sortedAscending(),"indices"));
            var empty=view.slice(0,0); check(field(empty,"indices")==field(empty.sortedDescending(),"indices"));
        }
        System.out.println("M3NativeIndexRangeSelfTest mode="+mode+" checks="+checks+" seededCases=2048");
    }
}
