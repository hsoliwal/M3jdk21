// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package org.HdrHistogram;

import java.util.SplittableRandom;

/**
 * BASE64_TO_JDK v1 speed: nanoseconds per Base64Helper.printBase64Binary and per parseBase64Binary call, best of 7
 * rounds after warm-up, for 16 B to 64 KiB of data. Run once before the overlay (reflection) and once after.
 */
public final class Base64HelperSpeed {
    private static final int[] SIZES = {16, 256, 4096, 65536};
    private static long sink;

    public static void main(String[] args) {
        for (int size : SIZES) {
            byte[] data = new byte[size];
            new SplittableRandom(size).nextBytes(data);
            String text = Base64Helper.printBase64Binary(data);
            long calls = Math.max(2_000, (32L << 20) / size);
            double encode = Double.MAX_VALUE, decode = Double.MAX_VALUE;
            for (int round = 0; round < 10; round++) {
                long start = System.nanoTime();
                for (long i = 0; i < calls; i++) {
                    sink += Base64Helper.printBase64Binary(data).length();
                }
                long middle = System.nanoTime();
                for (long i = 0; i < calls; i++) {
                    sink += Base64Helper.parseBase64Binary(text).length;
                }
                long end = System.nanoTime();
                if (round >= 3) {
                    encode = Math.min(encode, (middle - start) / (double) calls);
                    decode = Math.min(decode, (end - middle) / (double) calls);
                }
            }
            System.out.printf("size=%d encode ns/call=%.1f decode ns/call=%.1f%n", size, encode, decode);
        }
        System.out.println("sink " + Long.toHexString(sink));
    }
}
