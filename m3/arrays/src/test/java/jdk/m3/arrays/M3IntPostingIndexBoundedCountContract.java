// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import jdk.m3.arrays.M3IntPostingIndex.Backend;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;

/** Actual product/real JNI; BitSet membership is the independent count oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingBoundedCountContract.java blob 80d72e87cc9b; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class M3IntPostingIndexBoundedCountContract {
    private static long checks;
    private static int scenarios;
    private static boolean nativeMode;
    private M3IntPostingIndexBoundedCountContract() { }
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable call) {
        try { call.run(); }
        catch (Throwable error) { check(type.isInstance(error), "wrong exception: " + error); return; }
        throw new AssertionError("missing " + type.getName());
    }
    private static void verify(M3IntPostingIndex index, int[] keys, int[] rows, int[] query) {
        BitSet any = new BitSet(), all = new BitSet();
        all.set(0, index.rows());
        for (int key : query) {
            BitSet hit = new BitSet();
            for (int i = 0; i < keys.length; i++) if (keys[i] == key) hit.set(rows[i]);
            any.or(hit); all.and(hit);
        }
        int union = any.cardinality(), intersection = all.cardinality();
        int[] copy = query.clone();
        check(index.countAny(query) == union, "exact OR regression");
        check(index.countAll(query) == intersection, "exact AND regression");
        int[] limits = {0, 1, 2, 3, intersection, intersection + 1, union, union + 1, Integer.MAX_VALUE};
        for (int limit : limits) {
            check(index.countAnyUpTo(limit, query) == Math.min(limit, union), "bounded OR");
            check(index.countAllUpTo(limit, query) == Math.min(limit, intersection), "bounded AND");
            if (nativeMode) {
                check(index.countAnyUpTo(Backend.NATIVE_REQUIRED, limit, query) == Math.min(limit, union), "JNI OR");
                check(index.countAllUpTo(Backend.NATIVE_REQUIRED, limit, query) == Math.min(limit, intersection), "JNI AND");
            }
        }
        check(Arrays.equals(copy, query), "query mutated");
        scenarios++;
    }
    private static void exhaustive() {
        int[] keyValues = {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        for (int bits = 0; bits < 4096; bits++) {
            int[] keys = new int[12], rows = new int[12]; int n = 0;
            for (int key = 0; key < 3; key++) for (int row = 0; row < 4; row++) {
                if ((bits & (1 << (key * 4 + row))) != 0) { keys[n] = keyValues[key]; rows[n++] = row; }
            }
            keys = Arrays.copyOf(keys, n); rows = Arrays.copyOf(rows, n);
            M3IntPostingIndex index = M3IntPostingIndex.ofPairs(keys, rows, 4);
            for (int mask = 0; mask < 8; mask++) {
                int[] query = new int[Integer.bitCount(mask) * 2]; int at = 0;
                for (int k = 0; k < 3; k++) if ((mask & (1 << k)) != 0) {
                    query[at++] = keyValues[k]; query[at++] = keyValues[k];
                }
                verify(index, keys, rows, query);
            }
        }
    }
    private static void random() {
        Random r = new Random(0x4d33424f554e44L);
        int[] pool = {Integer.MIN_VALUE, -7, 0, 3, 19, Integer.MAX_VALUE};
        for (int round = 0; round < 600; round++) {
            int universe = r.nextInt(257), n = universe == 0 ? 0 : r.nextInt(700);
            int[] keys = new int[n], rows = new int[n];
            for (int i = 0; i < n; i++) { keys[i] = pool[r.nextInt(pool.length)]; rows[i] = r.nextInt(universe); }
            int[] query = new int[r.nextInt(10)];
            for (int i = 0; i < query.length; i++) query[i] = r.nextInt(5) == 0 ? 13 : pool[r.nextInt(pool.length)];
            M3IntPostingIndex index = M3IntPostingIndex.ofPairs(keys, rows, universe);
            verify(index, keys, rows, query);
            int before = index.countAnyUpTo(5, query);
            Arrays.fill(keys, 13); Arrays.fill(rows, 0);
            check(before == index.countAnyUpTo(5, query), "constructor snapshot changed");
        }
    }
    private static void guards() {
        M3IntPostingIndex index = M3IntPostingIndex.ofKeys(new int[] {1, 2, 1});
        expect(IllegalArgumentException.class, () -> index.countAnyUpTo(-1, 1));
        expect(IllegalArgumentException.class, () -> index.countAllUpTo(-1));
        expect(NullPointerException.class, () -> index.countAnyUpTo(null, 0, 1));
        expect(NullPointerException.class, () -> index.countAllUpTo(null, 0, 1));
        expect(NullPointerException.class, () -> index.countAnyUpTo(0, (int[]) null));
        expect(NullPointerException.class, () -> index.countAllUpTo(0, (int[]) null));
        if (!nativeMode) {
            expect(UnsatisfiedLinkError.class, () -> index.countAnyUpTo(Backend.NATIVE_REQUIRED, 0));
            expect(UnsatisfiedLinkError.class, () -> index.countAllUpTo(Backend.NATIVE_REQUIRED, 0, 999));
        }
    }
    private static int raw(String name, int[] rows, int[] offsets, int[] positions, int universe, int limit) {
        try {
            Method m = M3IntPostingIndex.class.getDeclaredMethod(name, int[].class, int[].class, int[].class, int.class, int.class);
            m.setAccessible(true);
            return (Integer) m.invoke(null, rows, offsets, positions, universe, limit);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException e) throw e;
            if (cause instanceof Error e) throw e;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void rawValidation() {
        for (String name : new String[] {"countUnionUpToNative", "countIntersectionUpToNative"}) {
            for (int cap : new int[] {0, 1, Integer.MAX_VALUE}) {
                // A first match must NOT hide malformed late rows, ordering, or offsets.
                expect(IllegalArgumentException.class, () -> raw(name, new int[]{0, 2, 0, 8}, new int[]{0, 2, 4}, new int[]{0, 1}, 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[]{0, 2, 0, 0}, new int[]{0, 2, 4}, new int[]{0, 1}, 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[]{0, 2}, new int[]{0, 2}, new int[]{0, 0}, 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[]{0, 2}, new int[]{0, 3}, new int[]{0}, 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, null, new int[]{0}, new int[0], 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[0], null, new int[0], 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[0], new int[]{0}, null, 4, cap));
                expect(IllegalArgumentException.class, () -> raw(name, new int[0], new int[]{0}, new int[0], -1, cap));
                check(raw(name, new int[0], new int[]{0, 0}, new int[]{0}, 4, cap) == 0, "empty selected posting");
            }
            expect(IllegalArgumentException.class, () -> raw(name, new int[0], new int[]{0}, new int[0], 4, -1));
        }
    }
    private static void huge() {
        M3IntPostingIndex index = M3IntPostingIndex.ofPairs(new int[]{1, 1, 2, 2},
                new int[]{0, Integer.MAX_VALUE - 1, 1, Integer.MAX_VALUE - 1}, Integer.MAX_VALUE);
        for (Backend b : nativeMode ? Backend.values() : new Backend[]{Backend.JAVA}) {
            for (int limit : new int[]{0, 1, 2, 3, 4, Integer.MAX_VALUE}) {
                check(index.countAnyUpTo(b, limit, 1, 2) == Math.min(limit, 3), "huge OR");
                check(index.countAllUpTo(b, limit, 1, 2) == Math.min(limit, 1), "huge AND");
                check(index.countAllUpTo(b, limit) == limit, "huge empty AND");
                check(index.countAnyUpTo(b, limit) == 0, "huge empty OR");
            }
        }
    }
    private static void legacy() {
        M3IntPostingIndex index = M3IntPostingIndex.ofKeys(new int[]{1, 1, 2});
        for (int limit : new int[]{0, 1}) {
            expect(UnsatisfiedLinkError.class, () -> index.countAnyUpTo(Backend.NATIVE_REQUIRED, limit, 1));
            expect(UnsatisfiedLinkError.class, () -> index.countAllUpTo(Backend.NATIVE_REQUIRED, limit, 1));
            check(index.countAny(Backend.NATIVE_REQUIRED, 1, 2) == 3, "old native OR disabled");
            check(index.countAll(Backend.NATIVE_REQUIRED, 1) == 2, "old native AND disabled");
        }
        check(M3IntPostingIndex.nativeAvailable(), "library disabled after missing optional symbol");
    }
    private static void fault(boolean allocation) {
        M3IntPostingIndex index = M3IntPostingIndex.ofPairs(new int[]{1, 1, 2, 2}, new int[]{0, 1, 0, 1}, 2);
        if (allocation) {
            // Zero-limit paths validate input but allocate no count scratch.
            check(index.countAnyUpTo(Backend.NATIVE_REQUIRED, 0, 1, 2) == 0, "zero OR allocates");
            check(index.countAllUpTo(Backend.NATIVE_REQUIRED, 0, 1, 2) == 0, "zero AND allocates");
            expect(OutOfMemoryError.class, () -> index.countAnyUpTo(Backend.NATIVE_REQUIRED, 1, 1, 2));
        } else {
            expect(IllegalStateException.class, () -> index.countAnyUpTo(Backend.NATIVE_REQUIRED, 1, 1, 2));
            expect(IllegalStateException.class, () -> index.countAllUpTo(Backend.NATIVE_REQUIRED, 1, 1, 2));
        }
    }
    private static void allocationIntersection() {
        M3IntPostingIndex index = M3IntPostingIndex.ofPairs(new int[]{1, 1, 2, 2}, new int[]{0, 1, 0, 1}, 2);
        check(index.countAllUpTo(Backend.NATIVE_REQUIRED, 0, 1, 2) == 0, "zero AND allocates");
        expect(OutOfMemoryError.class, () -> index.countAllUpTo(Backend.NATIVE_REQUIRED, 1, 1, 2));
    }
    private static void concurrent() throws Exception {
        M3IntPostingIndex index = M3IntPostingIndex.ofKeys(new int[]{1, 2, 1, 2, 1});
        Backend b = nativeMode ? Backend.NATIVE_REQUIRED : Backend.JAVA;
        try (var pool = Executors.newFixedThreadPool(4)) {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 16; i++) futures.add(pool.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    if (index.countAnyUpTo(b, 1, 1, 2) != 1 || index.countAllUpTo(b, 2, 1) != 2) return false;
                }
                return true;
            }));
            for (var f : futures) check(f.get(), "concurrent immutable query");
        }
    }
    public static void main(String[] args) throws Exception {
        String path = System.getProperty("matposting.native.path", "");
        if (!path.isBlank()) M3IntPostingIndex.loadNative(Path.of(path));
        nativeMode = M3IntPostingIndex.nativeAvailable();
        String mode = args.length == 0 ? "normal" : args[0];
        switch (mode) {
            case "normal" -> { exhaustive(); random(); guards(); if (nativeMode) rawValidation(); concurrent(); }
            case "huge" -> huge();
            case "legacy" -> legacy();
            case "corrupt" -> fault(false);
            case "allocation" -> fault(true);
            case "allocation-and" -> allocationIntersection();
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("BOUNDED_COUNT_PASS mode=" + mode + " native=" + nativeMode + " scenarios=" + scenarios + " checks=" + checks);
    }
}
