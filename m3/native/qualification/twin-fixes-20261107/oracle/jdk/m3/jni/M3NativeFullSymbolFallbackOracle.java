// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;

/** A real JNI loader/symbol-resolution regression against production M3NativeLeaseScan. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/bee80508c334ec11d9f82ad05106b556f6036546/synexia-openrewrite-recipes/recipe-crates/m3-jni-fullscan-missing-symbol-20261107/oracle/NativeFullSymbolFallbackOracle.java blob 9ce6d6322a4b; recipe m3/native/qualification/twin-fixes-20261107/apply.py v1
public final class M3NativeFullSymbolFallbackOracle {
    private M3NativeFullSymbolFallbackOracle() { }

    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }

    private static void verify(long[] ids, int[] leases, int[] pins, byte[] states) {
        long[] javaOutput = new long[ids.length];
        long[] nativeOutput = new long[ids.length];
        Arrays.fill(javaOutput, -17L);
        Arrays.fill(nativeOutput, -17L);
        int expected = M3NativeLeaseScan.collect(ids, leases, pins, states, ids.length,
                javaOutput, false);
        int actual = M3NativeLeaseScan.collect(ids, leases, pins, states, ids.length,
                nativeOutput, true);
        check(expected == actual, "full scan candidate count changed");
        check(Arrays.equals(javaOutput, nativeOutput), "full scan outputs differ");

        long[] javaWindow = new long[2];
        long[] nativeWindow = new long[2];
        int first = 0;
        int copied = 0;
        while (first < ids.length) {
            Arrays.fill(javaWindow, -55L);
            Arrays.fill(nativeWindow, -55L);
            long j = M3NativeLeaseScan.collectWindow(ids, leases, pins, states, ids.length,
                    first, javaWindow, false);
            long n = M3NativeLeaseScan.collectWindow(ids, leases, pins, states, ids.length,
                    first, nativeWindow, true);
            check(j == n, "window cursors differ");
            check(Arrays.equals(javaWindow, nativeWindow), "window contents differ");
            int next = M3NativeLeaseScan.nextIndex(n);
            check(next > first, "window failed to progress");
            copied += M3NativeLeaseScan.written(n);
            first = next;
        }
        check(copied == actual, "window count differs from full scan");
    }

    public static void main(String[] args) {
        check(M3NativeLeaseScan.nativeAvailable(), "JNI stub must load successfully");
        long[] ids = {1L, 2L, 3L, 4L, 5L, 6L, 7L};
        int[] leases = new int[ids.length];
        int[] pins = new int[ids.length];
        byte[] states = {0, 0, 0, 1, 2, 0, 0};
        for (int i = 0; i < 10_000; i++) {
            leases[2] = i & 1;
            pins[1] = (i >>> 1) & 1;
            pins[6] = (i >>> 2) & 1;
            verify(ids, leases, pins, states);
        }
        check(M3NativeLeaseScan.collect(ids, leases, pins, states, 0,
                new long[0], true) == 0, "zero-length scan changed");
        try {
            M3NativeLeaseScan.collect(ids, leases, pins, states, ids.length,
                    new long[1], true);
            throw new AssertionError("missing output capacity validation");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("bounded"), "unexpected preflight error");
        }
        System.out.println("PASS: 10000 real JNI-loaded missing-symbol full/window scan comparisons");
    }
}
