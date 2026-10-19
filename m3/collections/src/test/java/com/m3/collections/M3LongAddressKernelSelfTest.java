// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Standalone deterministic oracle for the direct address atom and batch JNI boundary. */
public final class M3LongAddressKernelSelfTest {
    private M3LongAddressKernelSelfTest() { }

    public static void main(String[] args) {
        run();
    }

    static void run() {
        Random random = new Random(0x4d33414444524553L);
        long checks = 0L;
        for (int trial = 0; trial < 512; trial++) {
            int payloadSize = 1 + random.nextInt(257);
            long[] payload = new long[payloadSize];
            for (int i = 0; i < payload.length; i++) payload[i] = random.nextLong();

            int count = random.nextInt(257);
            int prefix = random.nextInt(9);
            int suffix = random.nextInt(9);
            int[] addresses = new int[prefix + count + suffix];
            byte[] states = new byte[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = random.nextInt(payloadSize);
                states[i] = (byte) random.nextInt(256);
            }

            long[] gathered = M3LongAddressKernel.gather(
                    payload, addresses, prefix, count, M3IndexMaskCompactor.Backend.JAVA);
            if (gathered.length != count) throw new AssertionError("gather length");
            for (int i = 0; i < count; i++) {
                int addressIndex = prefix + i;
                int physical = addresses[addressIndex];
                if (M3LongAddressKernel.physicalIndex(addresses, addressIndex) != physical) {
                    throw new AssertionError("physical index");
                }
                if (M3LongAddressKernel.get(payload, addresses, addressIndex) != payload[physical]) {
                    throw new AssertionError("scalar load");
                }
                if (gathered[i] != payload[physical]) throw new AssertionError("Java gather");
                int required = random.nextInt(256);
                int forbidden = random.nextInt(256) & ~required;
                int state = states[addressIndex] & 0xff;
                boolean expected = (state & required) == required && (state & forbidden) == 0;
                if (M3LongAddressKernel.matches(states, addressIndex, required, forbidden)
                        != expected) {
                    throw new AssertionError("mask");
                }
                checks += 4;
            }

            long[] into = new long[count + 4];
            Arrays.fill(into, Long.MIN_VALUE);
            M3LongAddressKernel.gatherInto(
                    payload, addresses, prefix, count, into, 2,
                    M3IndexMaskCompactor.Backend.JAVA);
            for (int i = 0; i < count; i++) {
                if (into[2 + i] != gathered[i]) throw new AssertionError("gatherInto");
                checks++;
            }

            if (M3LongAddressKernel.nativeAvailable()) {
                long[] nativeValues = M3LongAddressKernel.gather(
                        payload, addresses, prefix, count,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                if (!Arrays.equals(gathered, nativeValues)) {
                    throw new AssertionError("JNI gather parity");
                }
                checks += count;
            } else {
                try {
                    M3LongAddressKernel.gather(
                            payload, addresses, prefix, count,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    throw new AssertionError("native-required fallback");
                } catch (UnsatisfiedLinkError expected) {
                    checks++;
                }
            }
        }

        long[] source = {9, -1, 9, 4, Long.MIN_VALUE, Long.MAX_VALUE};
        M3LongIndexProjection projection = M3LongIndexProjection.snapshot(source)
                .sortedAscending()
                .slice(1, 4);
        long[] materialized = projection.toArray();
        for (int i = 0; i < materialized.length; i++) {
            if (projection.get(i) != materialized[i]) throw new AssertionError("projection kernel");
            checks++;
        }

        try {
            M3LongAddressKernel.gatherInto(
                    source, new int[] {0}, 0, 1, source, 0,
                    M3IndexMaskCompactor.Backend.JAVA);
            throw new AssertionError("alias accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        try {
            M3LongAddressKernel.matches(new byte[1], 0, 1, 1);
            throw new AssertionError("overlapping masks accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }

        System.out.println("PASS M3LongAddressKernelSelfTest checks=" + checks
                + " native=" + M3LongAddressKernel.nativeAvailable());
    }
}
