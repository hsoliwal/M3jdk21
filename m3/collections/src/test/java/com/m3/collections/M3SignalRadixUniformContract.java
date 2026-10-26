/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Independent stable-sort oracle for every subset of active key bytes and public projections. */
public final class M3SignalRadixUniformContract {
    private static int checks;
    private static final Method JAVA_SORT = method("sortJava");
    private M3SignalRadixUniformContract() { }
    private static Method method(String name) {
        try {
            Method method = M3SignalOrder.class.getDeclaredMethod(name, long[].class, int[].class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void sort(long[] keys, int[] rows, boolean nativeBackend) {
        if (nativeBackend) { M3SignalOrderNative.sort(keys, rows); return; }
        try { JAVA_SORT.invoke(null, keys, rows); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw new AssertionError(failure.getCause());
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void verify(long[] source, boolean nativeBackend, String fixture) {
        long[] keys = source.clone();
        int[] rows = new int[keys.length];
        Integer[] oracle = new Integer[keys.length];
        for (int i = 0; i < rows.length; i++) { rows[i] = i; oracle[i] = i; }
        Arrays.sort(oracle, (left, right) -> Long.compareUnsigned(source[left], source[right]));
        sort(keys, rows, nativeBackend);
        for (int i = 0; i < keys.length; i++) {
            require(rows[i] == oracle[i], fixture + ": stable row " + i);
            require(keys[i] == source[oracle[i]], fixture + ": associated key " + i);
        }
        int[] fixedRows = rows.clone();
        long[] fixedKeys = keys.clone();
        sort(keys, rows, nativeBackend);
        require(Arrays.equals(rows, fixedRows) && Arrays.equals(keys, fixedKeys), fixture + ": fixed order");
    }
    private static void rawKeys(boolean nativeBackend) {
        Random random = new Random(0x4d334a444b32314cL);
        int[] lengths = {0, 1, 2, 3, 17, 256, 257};
        for (int active = 0; active < 256; active++) {
            for (int length : lengths) {
                long[] keys = new long[length];
                long constant = random.nextLong();
                for (int row = 0; row < length; row++) {
                    long key = constant;
                    for (int digit = 0; digit < 8; digit++) {
                        if ((active & (1 << digit)) != 0) {
                            long value = row < 2 ? row : random.nextInt(256);
                            key = (key & ~(255L << (digit * 8))) | (value << (digit * 8));
                        }
                    }
                    keys[row] = key;
                }
                verify(keys, nativeBackend, "active=" + active + ",n=" + length);
            }
        }
        for (int length : new int[] {4097, 65536}) {
            long[] randomKeys = new long[length];
            for (int i = 0; i < length; i++) randomKeys[i] = random.nextLong();
            verify(randomKeys, nativeBackend, "full-width n=" + length);
        }
    }
    private static void publicOrder(boolean nativeBackend) {
        double[] values = {Double.NaN, 0, -0.0, 7, -7, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.longBitsToDouble(0x7ff0000000000001L),
                7, Double.MIN_VALUE, -Double.MIN_VALUE, 0, -0.0, 42, -42};
        byte[] masks = new byte[values.length];
        int[] states = new int[values.length];
        for (int i = 0; i < values.length; i++) { masks[i] = (byte) i; states[i] = i | ((i & 1) == 0 ? Integer.MIN_VALUE : 0); }
        M3SignalOrder.Backend backend = nativeBackend ? M3SignalOrder.Backend.NATIVE_REQUIRED : M3SignalOrder.Backend.JAVA;
        double[] original = values.clone();
        for (boolean ascending : new boolean[] {true, false}) {
            for (int required = 0; required < 16; required++) {
                for (int excluded = 0; excluded < 16; excluded++) {
                    final int r = required, e = excluded;
                    Integer[] selected = java.util.stream.IntStream.range(0, values.length)
                            .filter(i -> ((masks[i] & 255) & r) == r && ((masks[i] & 255) & e) == 0)
                            .boxed().toArray(Integer[]::new);
                    Comparator<Integer> order = (a, b) -> ascending ? Double.compare(values[a], values[b]) : Double.compare(values[b], values[a]);
                    Arrays.sort(selected, order);
                    M3SignalOrder actual = M3SignalOrder.filtered(values, masks, r, e, ascending, backend);
                    int[] expected = Arrays.stream(selected).mapToInt(Integer::intValue).toArray();
                    require(Arrays.equals(expected, actual.rows()), "byte masks and total double order");
                    require(actual.retainedPrimitiveBytes() == (long) expected.length * Integer.BYTES, "retained row lane");
                    int lo = actual.size() / 3, hi = actual.size() * 2 / 3;
                    require(Arrays.equals(actual.slice(lo, hi).rows(), Arrays.copyOfRange(expected, lo, hi)), "slice coordinates");
                }
            }
            M3SignalOrder signSelected = M3SignalOrder.filtered32(values, states, Integer.MIN_VALUE, 0, ascending, backend);
            Integer[] expected = java.util.stream.IntStream.range(0, values.length).filter(i -> (i & 1) == 0).boxed().toArray(Integer[]::new);
            Arrays.sort(expected, (a, b) -> ascending ? Double.compare(values[a], values[b]) : Double.compare(values[b], values[a]));
            require(Arrays.equals(signSelected.rows(), Arrays.stream(expected).mapToInt(Integer::intValue).toArray()), "full-width sign-state selection");
        }
        require(Arrays.equals(values, original), "source signals remain unchanged");
        Integer[] objects = {3, 1, 3, 2};
        int[] callbacks = {0};
        require(Arrays.equals(M3SignalOrder.ascending(objects, value -> { callbacks[0]++; return value; }).rows(), new int[] {1, 3, 0, 2}), "object stable projection");
        require(callbacks[0] == objects.length, "projection once per occurrence");
    }
    private static void nativeRefusals() {
        expect(NullPointerException.class, () -> M3SignalOrderNative.sort(null, new int[0]));
        expect(NullPointerException.class, () -> M3SignalOrderNative.sort(new long[0], null));
        expect(IllegalArgumentException.class, () -> M3SignalOrderNative.sort(new long[1], new int[0]));
        expect(IllegalArgumentException.class, () -> M3SignalOrderNative.sort(new long[0], new int[1]));
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); throw new AssertionError("missing " + type.getName()); }
        catch (Throwable failure) { require(type.isInstance(failure), "native refusal " + type.getName()); }
    }
    public static int run(boolean nativeBackend) {
        checks = 0;
        if (nativeBackend) require(M3SignalOrderNative.available(), "native owner loaded");
        rawKeys(nativeBackend);
        publicOrder(nativeBackend);
        if (nativeBackend) nativeRefusals();
        return checks;
    }
    public static void main(String[] arguments) {
        boolean nativeBackend = arguments.length > 0 && arguments[0].equals("native");
        System.out.println("PASS radix " + (nativeBackend ? "JNI" : "Java") + " checks=" + run(nativeBackend));
    }
}
