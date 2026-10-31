// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Direct, original-owner safety and Java/JNI strict-refusal regression. */
public final class M3LongAddressKernelPreflightContract {
    private M3LongAddressKernelPreflightContract() { }
    private static long checks;

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError("check=" + checks + " " + reason);
    }

    private static <T extends Throwable> void expect(Class<T> kind, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("check=" + checks + " expected " + kind.getName()
                    + " but got " + failure, failure);
        }
        throw new AssertionError("check=" + checks + " expected " + kind.getName());
    }

    private static void strictPreflight() {
        long[] values = {11, 22, 33};
        int[] indices = {0, 2, 1};
        var javaBackend = M3IndexMaskCompactor.Backend.JAVA;
        expect(NullPointerException.class, () -> M3LongAddressKernel.gather(
                null, indices, 0, Integer.MAX_VALUE, javaBackend));
        expect(NullPointerException.class, () -> M3LongAddressKernel.gather(
                values, null, 0, Integer.MAX_VALUE, javaBackend));
        expect(NullPointerException.class, () -> M3LongAddressKernel.gather(
                values, indices, 0, Integer.MAX_VALUE, null));
        expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gather(
                values, indices, 0, Integer.MAX_VALUE, javaBackend));
        expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gather(
                values, indices, -1, Integer.MAX_VALUE, javaBackend));
        expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gather(
                values, indices, 0, -1, javaBackend));
        expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gather(
                values, indices, 3, 1, javaBackend));
        check(M3LongAddressKernel.gather(values, indices, 3, 0, javaBackend).length == 0,
                "empty suffix is valid");
        if (!M3LongAddressKernel.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () -> M3LongAddressKernel.gather(
                    values, indices, 0, indices.length,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            expect(UnsatisfiedLinkError.class, () -> M3LongAddressKernel.gather(
                    values, indices, 3, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        check(Arrays.equals(M3LongAddressKernel.gather(values, indices, 0, 3, javaBackend),
                        new long[] {11, 33, 22}), "ordered indirect gather");
        long[] out = {-9, -9, -9};
        expect(ArrayIndexOutOfBoundsException.class, () -> M3LongAddressKernel.gatherInto(
                values, new int[] {0, 999, 2}, 0, 3, out, 0, javaBackend));
        check(Arrays.equals(out, new long[] {11, -9, -9}),
                "physical offset failure leaves the same valid prefix");
        if (M3LongAddressKernel.nativeAvailable()) {
            long[] nativeOutput = {-9, -9, -9};
            expect(ArrayIndexOutOfBoundsException.class, () -> M3LongAddressKernel.gatherInto(
                    values, new int[] {0, 999, 2}, 0, 3, nativeOutput, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            check(Arrays.equals(nativeOutput, out), "JNI invalid-address prefix parity");
        }
        check(Arrays.equals(values, new long[] {11, 22, 33}), "payload ownership");
    }

    private static void randomizedParity() {
        Random random = new Random(0x4d33414444524553L);
        for (int trial = 0; trial < 8_192; trial++) {
            long[] values = new long[1 + random.nextInt(150)];
            for (int i = 0; i < values.length; i++) values[i] = random.nextLong();
            int size = random.nextInt(101);
            int prefix = random.nextInt(8);
            int[] index = new int[prefix + size + random.nextInt(8)];
            for (int i = 0; i < index.length; i++) index[i] = random.nextInt(values.length);
            long[] answer = M3LongAddressKernel.gather(values, index, prefix, size,
                    M3IndexMaskCompactor.Backend.JAVA);
            check(answer.length == size, "result size");
            long[] into = new long[size + 4];
            Arrays.fill(into, Long.MIN_VALUE);
            M3LongAddressKernel.gatherInto(values, index, prefix, size, into, 2,
                    M3IndexMaskCompactor.Backend.JAVA);
            check(into[0] == Long.MIN_VALUE && into[1] == Long.MIN_VALUE,
                    "gatherInto leading canary");
            check(into[into.length - 1] == Long.MIN_VALUE, "trailing canary");
            for (int i = 0; i < size; i++) {
                check(answer[i] == values[index[prefix + i]], "indirect value");
                check(into[i + 2] == answer[i], "existing gatherInto parity");
            }
            if (M3LongAddressKernel.nativeAvailable() && trial % 257 == 0) {
                long[] nativeResult = M3LongAddressKernel.gather(values, index, prefix, size,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(nativeResult, answer), "bounded JNI gather parity");
            }
        }
    }

    public static void main(String[] args) {
        strictPreflight();
        randomizedParity();
        System.out.println("M3LongAddressKernelPreflightContract PASS checks=" + checks
                + " native=" + M3LongAddressKernel.nativeAvailable());
    }
}
