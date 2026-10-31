/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Independent encounter-order and JNI contract oracle for the complete existing owners. */
public final class M3PackedFilterProof {
    private static long checks;
    private static long digest = 23;
    private M3PackedFilterProof() { }
    private record Row(int key, int id) { }
    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }
    private static void equal(long expected, long actual) {
        check(expected == actual, "expected=" + expected + " actual=" + actual);
        digest = 31 * digest + actual;
    }
    private static void refuses(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (RuntimeException | LinkageError failure) {
            check(type.isInstance(failure), failure.toString());
            digest = 31 * digest + failure.getClass().getName().hashCode();
            digest = 31 * digest + Objects.hashCode(failure.getMessage());
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static boolean selected(int i, int n, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (i & 1) == 0;
            case 3 -> i == n - 1;
            case 4 -> (i & 63) == 63;
            default -> ((i * 1103515245 + 12345) & 255) < 37;
        };
    }
    private static void compareProjection(M3IndexedListSnapshot<Row> actual,
            List<Integer> model, Row[] payload) throws Exception {
        equal(model.size(), actual.size());
        BitSet expected = new BitSet();
        for (int i = 0; i < model.size(); i++) {
            int physical = model.get(i);
            equal(physical, actual.physicalIndex(i));
            check(payload[physical] == actual.get(i), "reference identity");
            expected.set(physical);
        }
        check(expected.equals(actual.physicalMaskCopy()), "membership");
        for (int i = -1; i <= payload.length; i++) {
            check(actual.includesPhysicalIndex(i) == (i >= 0 && expected.get(i)), "physical membership");
        }
        BitSet escaped = actual.physicalMaskCopy(); escaped.flip(0, payload.length + 1);
        check(expected.equals(actual.physicalMaskCopy()), "escaped membership mutated owner");
        int[] escapedIndexes = actual.physicalIndices();
        if (escapedIndexes.length != 0) escapedIndexes[0] = -1;
        Object[] escapedValues = actual.toArray();
        if (escapedValues.length != 0) escapedValues[0] = new Object();
        if (!model.isEmpty()) {
            equal(model.get(0), actual.physicalIndex(0));
            check(actual.get(0) == payload[model.get(0)], "escaped payload mutated owner");
        }
        refuses(UnsupportedOperationException.class, () -> actual.addAll(List.of()));
        refuses(UnsupportedOperationException.class, () -> actual.removeIf(x -> false));
        refuses(UnsupportedOperationException.class, actual::clear);
    }
    private static void projections(M3IndexMaskCompactor.Backend backend) throws Exception {
        Comparator<Row> order = Comparator.nullsFirst(Comparator.comparingInt(Row::key));
        for (int n : new int[]{0,1,2,31,32,33,63,64,65,127,128,129,255,256,257,511,512,513,1025}) {
            Row[] original = new Row[n + 12];
            Row[] pool = new Row[17];
            for (int i = 0; i < pool.length; i++) pool[i] = new Row(i % 5, i);
            for (int i = 0; i < original.length; i++) original[i] = i % 11 == 0 ? null : pool[(i * 7) % pool.length];
            var root = M3IndexedListSnapshot.copyOf(original);
            Field values = M3IndexedListSnapshot.class.getDeclaredField("values"); values.setAccessible(true);
            for (int sorted = 0; sorted < 2; sorted++) {
                List<Integer> model = new ArrayList<>();
                for (int i = 0; i < original.length; i++) model.add(i);
                var base = root;
                if (sorted != 0) {
                    base = base.sorted(order);
                    model.sort((a,b) -> order.compare(original[a], original[b]));
                }
                base = base.slice(3, n + 6).slice(3, n);
                model = new ArrayList<>(model.subList(6, 6 + n));
                for (int mode = 0; mode < 6; mode++) {
                    final int choice = mode;
                    final var source = base;
                    final var inputModel = model;
                    AtomicInteger ordinal = new AtomicInteger();
                    var filtered = source.filtered(value -> {
                        int i = ordinal.getAndIncrement();
                        check(value == original[inputModel.get(i)], "callback order");
                        return selected(i, n, choice);
                    }, backend);
                    equal(n, ordinal.get());
                    List<Integer> expected = new ArrayList<>();
                    for (int i = 0; i < n; i++) if (selected(i,n,choice)) expected.add(model.get(i));
                    compareProjection(filtered, expected, original);
                    check(values.get(root) == values.get(filtered), "second payload owner");
                    check(filtered != source, "fresh filter wrapper");
                    var reordered = filtered.sorted(order.reversed());
                    expected.sort((a,b) -> order.reversed().compare(original[a],original[b]));
                    compareProjection(reordered, expected, original);
                    int offset = expected.size() / 3;
                    var tail = reordered.subList(offset, reordered.size()).filtered(v -> v != null, backend);
                    expected = new ArrayList<>(expected.subList(offset, expected.size()));
                    expected.removeIf(i -> original[i] == null);
                    compareProjection(tail, expected, original);
                }
                int[] points = n == 0 ? new int[0] : new int[]{0,n/2,n-1};
                for (int point : points) {
                    RuntimeException marker = new RuntimeException("predicate marker");
                    AtomicInteger called = new AtomicInteger();
                    try {
                        base.filtered(v -> { if(called.getAndIncrement() == point) throw marker; return true; }, backend);
                        throw new AssertionError("callback exception lost");
                    } catch (RuntimeException failure) { check(failure == marker, "exception identity"); }
                    equal(point + 1, called.get());
                    compareProjection(base, model, original);
                }
            }
            original[0] = new Row(-999, -999);
            check(root.get(0) == null, "defensive payload snapshot");
        }
    }
    private static int[] reference(int[] rows, int ro, byte[] states, int so, int n, int mask) {
        var expected = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) if (((states[so+i] & 255) & mask) == mask) expected.add(rows[ro+i]);
        return expected.stream().mapToInt(Integer::intValue).toArray();
    }
    private static void ints(int[] expected, int[] actual) {
        equal(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) equal(expected[i], actual[i]);
    }
    private static void nativeMatrix() {
        check(M3IndexMaskCompactor.nativeAvailable(), "native load mandatory");
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        for (int n : new int[]{0,1,63,64,65,127,128,129,255,256,257,511,512,513,1025,4097}) {
            int ro = 7, so = 13;
            int[] rows = new int[n + ro + 5];
            long[] longs = new long[rows.length];
            byte[] states = new byte[n + so + 3];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = (i & 1) == 0 ? Integer.MIN_VALUE + i % 7 : Integer.MAX_VALUE - i % 9;
                longs[i] = (i & 1) == 0 ? Long.MIN_VALUE + i % 7 : Long.MAX_VALUE - i % 9;
            }
            for (int pattern = 0; pattern < 5; pattern++) {
                for (int i = 0; i < states.length; i++) states[i] = (byte) switch (pattern) {
                    case 0 -> 0; case 1 -> 255; case 2 -> (i-so) % 256 == 255 ? 255 : 0;
                    case 3 -> (i-so) % 256 == 0 ? 128 : 0; default -> (i * 37 + 11) & 255;
                };
                int[] rowCopy = rows.clone(); byte[] stateCopy = states.clone(); long[] longCopy = longs.clone();
                for (int mask = 0; mask < 256; mask++) {
                    int[] expected = reference(rows, ro, states, so, n, mask);
                    ints(expected, M3IndexMaskCompactor.selectWindow(rows,ro,states,so,n,mask,nativeBackend));
                    byte[] aligned = Arrays.copyOfRange(states, so, so+n);
                    int[] clipped = Arrays.copyOfRange(rows, ro, ro+n);
                    ints(expected, M3IndexMaskCompactor.select(clipped, aligned, mask, nativeBackend));
                    ints(expected, M3IndexMaskCompactor.selectNativeRange(rows, ro, n, aligned, mask));
                    int[] identity = new int[n]; for(int i=0;i<n;i++) identity[i]=i;
                    ints(reference(identity,0,aligned,0,n,mask),M3IndexMaskCompactor.selectPositions(aligned,mask,nativeBackend));
                    long[] actual = M3IndexMaskCompactor.selectLongWindow(longs,ro,states,so,n,mask,nativeBackend);
                    equal(expected.length, actual.length);
                    int k = 0;
                    for(int i=0;i<n;i++) if(((states[so+i]&255)&mask)==mask)equal(longs[ro+i],actual[k++]);
                }
                check(Arrays.equals(rows,rowCopy) && Arrays.equals(states,stateCopy) && Arrays.equals(longs,longCopy),"JNI input mutated");
            }
        }
        int[] rows = {1,2,3}; byte[] flags = {1,2,3};
        refuses(IllegalArgumentException.class,()->M3IndexMaskCompactor.select(rows,flags,256,nativeBackend));
        refuses(IllegalArgumentException.class,()->M3IndexMaskCompactor.select(rows,new byte[0],1,nativeBackend));
        refuses(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectWindow(rows,Integer.MAX_VALUE,flags,0,1,1,nativeBackend));
        refuses(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectWindow(rows,0,flags,0,-1,1,nativeBackend));
        refuses(NullPointerException.class,()->M3IndexMaskCompactor.select(rows,flags,1,null));
    }
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "java" : args[0];
        if (mode.equals("missing")) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "unexpected library");
            var empty = M3IndexedListSnapshot.copyOf(new Object[0]);
            AtomicInteger calls = new AtomicInteger();
            refuses(UnsatisfiedLinkError.class,()->empty.filtered(v->{calls.incrementAndGet();return true;},M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            equal(0,calls.get());
        } else {
            projections(mode.equals("java") ? M3IndexMaskCompactor.Backend.JAVA : M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            if (!mode.equals("java")) nativeMatrix();
        }
        System.out.println("PACKED_FILTER_PASS mode="+mode+" checks="+checks+" digest="+Long.toUnsignedString(digest));
    }
}
