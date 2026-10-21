/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Standalone real-byte Java 21 differential oracle. */
public final class M3SignalOrderContract {
    private static long checks;
    private static final Random RANDOM = new Random(0x4d335349474e414cL);
    private M3SignalOrderContract() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length > 0 && args[0].equals("native");
        M3SignalOrder.Backend backend = nativeRun
                ? M3SignalOrder.Backend.NATIVE_REQUIRED : M3SignalOrder.Backend.JAVA;
        if (nativeRun) check(M3SignalOrderNative.available(), "native library loaded");
        basics(backend);
        masks(backend);
        projection();
        for (int round = 0; round < 256; round++) {
            int n = (round * 29) % 521;
            double[] signals = new double[n];
            byte[] states = new byte[n];
            for (int i = 0; i < n; i++) {
                signals[i] = Double.longBitsToDouble(RANDOM.nextLong());
                if (i % 7 == 0) signals[i] = 0.0;
                if (i % 13 == 0) signals[i] = -0.0;
                if (i % 23 == 0) signals[i] = Double.NaN;
                states[i] = (byte) RANDOM.nextInt(256);
            }
            for (boolean asc : new boolean[] {true, false}) {
                M3SignalOrder order = M3SignalOrder.filtered(signals, states, 0x05, 0x80, asc, backend);
                eq(oracle(signals, states, 0x05, 0x80, asc), order.rows(), "random radix filtered");
                check(order.retainedPrimitiveBytes() == (long) order.size() * Integer.BYTES,
                        "single retained primitive order lane");
                if (order.size() > 1) {
                    int from = order.size() / 3;
                    int to = (order.size() * 2) / 3;
                    eq(Arrays.copyOfRange(order.rows(), from, to), order.slice(from, to).rows(),
                            "zero-copy slice logical parity");
                    check(order.slice(from, to).retainedPrimitiveBytes() == order.retainedPrimitiveBytes(),
                            "slice shares retained primitive backing");
                }
            }
        }
        System.out.println("PASS M3SignalOrderContract backend=" + backend + " checks=" + checks);
    }

    private static void basics(M3SignalOrder.Backend backend) {
        double[] data = {
            Double.NaN, -0.0, +0.0, -1, +1,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.longBitsToDouble(0x7ff8000000000001L), +0.0
        };
        byte[] all = new byte[data.length];
        Arrays.fill(all, (byte) 1);
        eq(oracle(data, null, 0, 0, true), M3SignalOrder.ascending(data).rows(), "IEEE ascending");
        eq(oracle(data, null, 0, 0, false), M3SignalOrder.descending(data).rows(), "IEEE descending");
        eq(oracle(data, all, 1, 0, true),
                M3SignalOrder.filtered(data, all, 1, 0, true, backend).rows(), "IEEE backend");
        check(M3SignalOrder.compare(-0.0, +0.0) < 0, "negative zero ordered first");
        check(Double.isNaN(M3SignalOrder.numericSeparation(Double.NaN, 3)), "NaN distance refused");
        check(M3SignalOrder.numericSeparation(-3, 5) == 8.0, "numeric separation preserved");
        eq(new int[0], M3SignalOrder.ascending(new double[0]).rows(), "empty");
        eq(new int[] {0}, M3SignalOrder.descending(new double[] {7}).rows(), "singleton");
    }

    private static void masks(M3SignalOrder.Backend backend) {
        double[] signals = {4, 4, -1, 0, 2, 4, 1};
        byte[] flags = {1, 3, 1, 3, (byte) 0x83, 2, 3};
        byte[] copy = flags.clone();
        eq(oracle(signals, flags, 1, 2, true),
                M3SignalOrder.filtered(signals, flags, 1, 2, true, backend).rows(), "required/excluded");
        eq(oracle(signals, flags, 1, 0, false),
                M3SignalOrder.filtered(signals, flags, 1, 0, false, backend).rows(), "stable descending");
        check(Arrays.equals(flags, copy), "no mask mutation");
        eq(new int[0], M3SignalOrder.filtered(signals, flags, 1, 1, true, backend).rows(),
                "overlapping masks match none");
        reject(IllegalArgumentException.class,
                () -> M3SignalOrder.filtered(signals, flags, 256, 0, true, backend), "mask range");
        reject(IllegalArgumentException.class,
                () -> M3SignalOrder.filtered(signals, new byte[3], 1, 0, true, backend),
                "unaligned masks");
        reject(IndexOutOfBoundsException.class,
                () -> M3SignalOrder.ascending(signals).slice(3, 8), "slice bounds");
    }

    private static void projection() {
        final class Person {
            double score; int calls;
            Person(double weight) { score = weight; }
            double weight() { calls++; return score; }
        }
        Person[] people = {new Person(3), new Person(1), new Person(3), new Person(-1)};
        eq(new int[] {3, 1, 0, 2}, M3SignalOrder.ascending(people, Person::weight).rows(),
                "domain signal once and stable ties");
        for (Person p : people) check(p.calls == 1, "exactly one domain callback");
        reject(NullPointerException.class,
                () -> M3SignalOrder.ascending(new Person[] {null}, Person::weight),
                "null domain object");
    }

    private static int[] oracle(double[] values, byte[] states,
            int required, int excluded, boolean ascending) {
        Integer[] boxed = new Integer[values.length];
        int size = 0;
        for (int i = 0; i < values.length; i++) {
            int mask = states == null ? 0 : Byte.toUnsignedInt(states[i]);
            if (states == null || ((mask & required) == required && (mask & excluded) == 0)) {
                boxed[size++] = i;
            }
        }
        boxed = Arrays.copyOf(boxed, size);
        Arrays.sort(boxed, (a, b) -> {
            int order = ascending ? Double.compare(values[a], values[b])
                    : Double.compare(values[b], values[a]);
            return order != 0 ? order : Integer.compare(a, b);
        });
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = boxed[i];
        return result;
    }

    private static void eq(int[] expected, int[] actual, String msg) {
        check(Arrays.equals(expected, actual), msg + " expected=" + Arrays.toString(expected)
                + " actual=" + Arrays.toString(actual));
    }

    private static void check(boolean yes, String msg) {
        checks++;
        if (!yes) throw new AssertionError(msg);
    }

    @FunctionalInterface private interface Throwing { void run(); }
    private static void reject(Class<? extends Throwable> type, Throwing action, String msg) {
        try { action.run(); }
        catch (Throwable error) { check(type.isInstance(error), msg + " wrong exception " + error); return; }
        throw new AssertionError(msg + " did not refuse");
    }
}
