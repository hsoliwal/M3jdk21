/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

//FLLDCIM: MRL - fold-close audit leaf L0166: ADD from PR #1084 head 6b16a20dcedf blob 6d1e3be27b43, ancestry-only landing b42ab7605b86 (tree == first parent); recipe m3/collections/qualification/foldaudit-collections-20261107/apply.py v1
//FLLDCIM: MRL - MAPPED: the origin counts/ module has no pom on master; the file joins the real module tree
final class M3CountLanesTest {
    //FLLDCIM: MRL - the origin refused to run without the count library; on master the Java path runs always, the native path when the property names the library, and -Dm3.test.native.required=true restores the refusal
    private static boolean nativeLoaded;
    @BeforeAll static void loadActualNativeLibrary() {
        String path = System.getProperty("m3.counted.native");
        if (path == null && Boolean.getBoolean("m3.test.native.required")) fail("native proof must not silently skip");
        nativeLoaded = path != null;
        if (nativeLoaded) System.load(Path.of(path).toAbsolutePath().toString());
    }
    static long[] oracle(long[] a, int ao, long[] b, int bo, int n, int op) {
        Objects.requireNonNull(a); Objects.requireNonNull(b);
        Objects.checkFromIndexSize(ao, n, a.length); Objects.checkFromIndexSize(bo, n, b.length);
        if (op < 0 || op > 3) throw new IllegalArgumentException();
        long[] out = new long[n]; BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            if (a[ao + i] < 0 || b[bo + i] < 0) throw new IllegalArgumentException();
            BigInteger x = BigInteger.valueOf(a[ao + i]), y = BigInteger.valueOf(b[bo + i]);
            BigInteger v = switch (op) { case 0 -> x.add(y); case 1 -> x.max(y); case 2 -> x.min(y); default -> x.subtract(y).max(BigInteger.ZERO); };
            out[i] = v.longValueExact(); total = total.add(v); total.longValueExact();
        }
        return out;
    }
    static void check(long[] a, int ao, long[] b, int bo, int n, int op) {
        long[] ac = a == null ? null : a.clone(), bc = b == null ? null : b.clone();
        long[] expected = null; Class<? extends RuntimeException> failure = null;
        try { expected = oracle(a, ao, b, bo, n, op); } catch (RuntimeException e) { failure = e.getClass(); }
        if (failure == null) {
            assertArrayEquals(expected, M3CountLanes.combine(a, ao, b, bo, n, op));
            if (nativeLoaded) assertArrayEquals(expected, M3CountLanes.combineNative(a, ao, b, bo, n, op));
        } else {
            assertThrows(failure, () -> M3CountLanes.combine(a, ao, b, bo, n, op));
            if (nativeLoaded) assertThrows(failure, () -> M3CountLanes.combineNative(a, ao, b, bo, n, op));
        }
        assertArrayEquals(ac, a); assertArrayEquals(bc, b);
    }
    @Test void randomSlicesAcrossChunkBoundariesAgainstBigInteger() {
        Random random = new Random(970);
        for (int n : new int[]{0, 1, 2, 1023, 1024, 1025, 2049, 16387}) {
            for (int seed = 0; seed < 12; seed++) {
                long[] a = new long[n + 6], b = new long[n + 9];
                for (int i = 0; i < a.length; i++) a[i] = random.nextInt(1000000);
                for (int i = 0; i < b.length; i++) b[i] = random.nextInt(1000000);
                for (int op = 0; op < 4; op++) { check(a, 2, b, 5, n, op); check(a, 0, a, 3, n, op); }
            }
        }
    }
    @Test void overflowNegativesAndArgumentAdmission() {
        for (int op = 0; op < 4; op++) {
            check(new long[]{Long.MAX_VALUE, 1}, 0, new long[]{1, 0}, 0, 2, op);
            check(new long[]{0, -1}, 0, new long[]{0, 0}, 0, 2, op);
            check(new long[]{Long.MAX_VALUE}, 0, new long[]{Long.MAX_VALUE}, 0, 1, op);
            check(null, 0, new long[0], 0, 0, op);
            check(new long[0], 0, null, 0, 0, op);
            check(new long[1], -1, new long[1], 0, 1, op);
            check(new long[1], 0, new long[1], Integer.MAX_VALUE, 1, op);
            check(new long[1], 0, new long[1], 0, -1, op);
            check(new long[1], 0, new long[1], 0, Integer.MAX_VALUE, op);
            long[] a = new long[1025]; a[1024] = -1; check(a, 0, new long[1025], 0, 1025, op);
        }
        check(new long[0], 0, new long[0], 0, 0, -1); check(new long[0], 0, new long[0], 0, 0, 4);
    }
    @Test void aggregateOverflowCrossesNativeChunkBoundary() {
        long[] a = new long[1025], b = new long[1025]; a[0] = Long.MAX_VALUE; a[1024] = 1;
        for (int op = 0; op < 4; op++) check(a, 0, b, 0, a.length, op);
    }
}
