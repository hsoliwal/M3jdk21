// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Single primitive address atom for prepared M3 long projections.
 *
 * <p>Repeated scalar reads are intentionally just {@code payload[addresses[addressBase + logical]]}
 * after the owning view has checked its logical ordinal. Filtering, ordering and masks are
 * precomputed elsewhere. JNI is available only for explicit batch gather.</p>
 */
public final class M3LongAddressKernel {
    private M3LongAddressKernel() { }

    /** Safe standalone address lookup. */
    public static int physicalIndex(int[] addresses, int addressIndex) {
        Objects.requireNonNull(addresses, "addresses");
        return addresses[Objects.checkIndex(addressIndex, addresses.length)];
    }

    /** Safe standalone payload lookup through one prepared address. */
    public static long get(long[] payload, int[] addresses, int addressIndex) {
        Objects.requireNonNull(payload, "payload");
        int physical = physicalIndex(addresses, addressIndex);
        return payload[Objects.checkIndex(physical, payload.length)];
    }

    /** Safe standalone aligned state-mask lookup. */
    public static boolean matches(byte[] states, int stateIndex,
                                  int requiredBits, int forbiddenBits) {
        Objects.requireNonNull(states, "states");
        checkMasks(requiredBits, forbiddenBits);
        int checked = Objects.checkIndex(stateIndex, states.length);
        return matchesUnchecked(states, checked, requiredBits, forbiddenBits);
    }

    /** Returns whether the optional batch JNI library can be loaded. */
    public static boolean nativeAvailable() {
        return NativeLibrary.AVAILABLE;
    }

    /**
     * Detached gather of a half-open prepared address window.
     * JAVA is the default scalar loop; NATIVE_REQUIRED never falls back.
     * Validate before allocating the result so invalid large requests cannot
     * turn the intended input/native refusal into an OutOfMemoryError.
     */
    public static long[] gather(long[] payload, int[] addresses, int addressFrom, int length,
                                M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(addresses, "addresses");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(addressFrom, length, addresses.length);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED && !NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3 address gather JNI library unavailable");
        }
        long[] result = new long[length];
        gatherInto(payload, addresses, addressFrom, length, result, 0, backend);
        return result;
    }

    /**
     * Gathers prepared addresses into caller-owned output. Output may not alias payload so Java and
     * JNI have identical semantics even for arbitrary address permutations.
     */
    public static void gatherInto(long[] payload, int[] addresses, int addressFrom, int length,
                                  long[] output, int outputFrom,
                                  M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(addresses, "addresses");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(addressFrom, length, addresses.length);
        Objects.checkFromIndexSize(outputFrom, length, output.length);
        if (output == payload) {
            throw new IllegalArgumentException("output must not alias payload");
        }
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3 address gather JNI library unavailable");
            }
            gatherNative(payload, addresses, addressFrom, length, output, outputFrom);
            return;
        }
        gatherJavaUnchecked(payload, addresses, addressFrom, output, outputFrom, length);
    }

    /** Package-local hot atom; owning views validate logical bounds once. */
    static int addressUnchecked(int[] addresses, int addressBase, int logicalOrdinal) {
        return addresses[addressBase + logicalOrdinal];
    }

    /** Package-local hot atom: one int[] load followed by one long[] load. */
    static long loadUnchecked(long[] payload, int[] addresses,
                              int addressBase, int logicalOrdinal) {
        return payload[addresses[addressBase + logicalOrdinal]];
    }

    /** Package-local physical payload load used by precomputed ordering kernels. */
    static long loadPhysicalUnchecked(long[] payload, int physicalIndex) {
        return payload[physicalIndex];
    }

    /** Package-local batch atom for an already-validated address/output window. */
    static void gatherJavaUnchecked(long[] payload, int[] addresses, int addressFrom,
                                    long[] output, int outputFrom, int length) {
        for (int i = 0; i < length; i++) {
            output[outputFrom + i] = payload[addresses[addressFrom + i]];
        }
    }

    static boolean matchesUnchecked(byte[] states, int stateIndex,
                                    int requiredBits, int forbiddenBits) {
        int state = states[stateIndex] & 0xff;
        return (state & requiredBits) == requiredBits && (state & forbiddenBits) == 0;
    }

    private static void checkMasks(int requiredBits, int forbiddenBits) {
        if ((requiredBits & ~0xff) != 0 || (forbiddenBits & ~0xff) != 0
                || (requiredBits & forbiddenBits) != 0) {
            throw new IllegalArgumentException("mask must be 8-bit and disjoint");
        }
    }

    private static final class NativeLibrary {
        private static final boolean AVAILABLE = load();

        private static boolean load() {
            try {
                System.loadLibrary("m3_collections_address");
                return true;
            } catch (UnsatisfiedLinkError | SecurityException unavailable) {
                return false;
            }
        }
    }

    private static native void gatherNative(long[] payload, int[] addresses,
                                            int addressFrom, int length,
                                            long[] output, int outputFrom);
}
