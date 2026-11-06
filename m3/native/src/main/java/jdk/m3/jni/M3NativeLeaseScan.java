// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Objects;

/**
 * Bounded Java/JNI parity kernel for M3 logical-handle lease snapshots.
 * Inputs are Java-owned arrays; JNI borrows them for a single call and retains no pointers.
 * A zero count is ONLY a candidate residency signal, never a JVM reachability oracle.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3NativeLeaseScan.java blob 056cf09973d4; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3NativeLeaseScan {
    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("m3_scoped_leases");
            loaded = true;
        } catch (UnsatisfiedLinkError ignored) {
            // Default installations do not need a native binary.
        }
        NATIVE_AVAILABLE = loaded;
    }

    private M3NativeLeaseScan() { }

    public static boolean nativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static int collect(long[] ids, int[] leases, int[] pins, byte[] states,
                              int count, long[] output, boolean preferNative) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(leases, "leases");
        Objects.requireNonNull(pins, "pins");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(output, "output");
        if (output == ids) throw new IllegalArgumentException("output must not alias ids");
        if (count < 0 || ids.length < count || leases.length < count
                || pins.length < count || states.length < count || output.length < count) {
            throw new IllegalArgumentException("invalid bounded scan arrays");
        }
        // Guard against silently treating a corrupt/negative count as an unleased resource.
        for (int i = 0; i < count; i++) {
            if (ids[i] <= 0 || leases[i] < 0 || pins[i] < 0
                    || states[i] < M3ScopedLeaseTable.CLEAN
                    || states[i] > M3ScopedLeaseTable.RETIRED) {
                throw new IllegalArgumentException("invalid handle/lease snapshot at " + i);
            }
        }
        if (preferNative && NATIVE_AVAILABLE) {
            return collectNative(ids, leases, pins, states, count, output);
        }
        return collectJava(ids, leases, pins, states, count, output);
    }

    static int collectJava(long[] ids, int[] leases, int[] pins, byte[] states,
                                  int count, long[] output) {
        int outputCount = 0;
        // Public callers use the validated entry point; keep this pure helper package-private.
        for (int i = 0; i < count; i++) {
            if (states[i] == M3ScopedLeaseTable.CLEAN && leases[i] == 0 && pins[i] == 0) {
                output[outputCount++] = ids[i];
            }
        }
        return outputCount;
    }

    /** Upper half: next index to scan; lower half: number of emitted candidates. */
    static int nextIndex(long packed) {
        return (int) (packed >>> 32);
    }

    /** Number of values written to the output prefix. */
    static int written(long packed) {
        return (int) packed;
    }

    static long packWindow(int next, int written) {
        return ((long) next << 32) | (written & 0xffff_ffffL);
    }

    /**
     * Incremental snapshot filter: validates only visited slots, never allocates
     * a table-sized output, and remains valid without an optional native binary.
     */
    static long collectWindow(long[] ids, int[] leases, int[] pins, byte[] states,
                                     int count, int first, long[] output, boolean preferNative) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(leases, "leases");
        Objects.requireNonNull(pins, "pins");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(output, "output");
        if (output == ids) throw new IllegalArgumentException("output must not alias ids");
        if (count < 0 || first < 0 || first > count || output.length == 0
                || ids.length < count || leases.length < count
                || pins.length < count || states.length < count) {
            throw new IllegalArgumentException("invalid bounded window arrays");
        }
        long result;
        if (preferNative && NATIVE_AVAILABLE) {
            try {
                result = collectNativeWindow(ids, leases, pins, states, count, first, output);
            } catch (UnsatisfiedLinkError oldLibraryWithoutWindowSymbol) {
                // Optional native installations predating the window ABI remain supported.
                result = collectJavaWindow(ids, leases, pins, states, count, first, output);
            }
        } else {
            result = collectJavaWindow(ids, leases, pins, states, count, first, output);
        }
        int next = nextIndex(result);
        int written = written(result);
        if (next < first || next > count || written < 0 || written > output.length
                || written > next - first || (next < count && written != output.length)) {
            throw new IllegalStateException("invalid JNI/Java scan result");
        }
        return result;
    }

    static long collectJavaWindow(long[] ids, int[] leases, int[] pins, byte[] states,
                                  int count, int first, long[] output) {
        int next = first;
        int written = 0;
        while (next < count) {
            if (ids[next] <= 0 || leases[next] < 0 || pins[next] < 0
                    || states[next] < M3ScopedLeaseTable.CLEAN
                    || states[next] > M3ScopedLeaseTable.RETIRED) {
                throw new IllegalArgumentException("invalid handle/lease window at " + next);
            }
            if (states[next] == M3ScopedLeaseTable.CLEAN && leases[next] == 0
                    && pins[next] == 0) {
                output[written++] = ids[next];
            }
            next++;
            if (written == output.length) break;
        }
        return packWindow(next, written);
    }

    private static native long collectNativeWindow(long[] ids, int[] leases, int[] pins,
                                                   byte[] states, int count,
                                                   int first, long[] output);

    private static native int collectNative(long[] ids, int[] leases, int[] pins,
                                            byte[] states, int count, long[] output);
}
