// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;

import org.eclipse.jgit.util.Base64;

/**
 * JGIT_BASE64_TO_JDK v1 speed: nanoseconds per util.Base64.encodeBytes and per decode(String) call, best of 7 rounds
 * after warm-up, for 16 B to 64 KiB of data; decode also for the same text with a line break every 76 characters
 * (the lenient path). Run once on the master jar and once on the branch jar.
 */
public final class JgitBase64Speed {
    private static final int[] SIZES = {16, 256, 4096, 65536};
    private static long sink;

    public static void main(String[] args) {
        for (int size : SIZES) {
            byte[] data = new byte[size];
            new SplittableRandom(size).nextBytes(data);
            String text = Base64.encodeBytes(data);
            String wrapped = new String(java.util.Base64.getMimeEncoder().encode(data), StandardCharsets.ISO_8859_1);
            long calls = Math.max(2_000, (32L << 20) / size);
            double encode = Double.MAX_VALUE, decode = Double.MAX_VALUE, lenient = Double.MAX_VALUE;
            for (int round = 0; round < 10; round++) {
                long t0 = System.nanoTime();
                for (long i = 0; i < calls; i++) {
                    sink += Base64.encodeBytes(data).length();
                }
                long t1 = System.nanoTime();
                for (long i = 0; i < calls; i++) {
                    sink += Base64.decode(text).length;
                }
                long t2 = System.nanoTime();
                for (long i = 0; i < calls; i++) {
                    sink += Base64.decode(wrapped).length;
                }
                long t3 = System.nanoTime();
                if (round >= 3) {
                    encode = Math.min(encode, (t1 - t0) / (double) calls);
                    decode = Math.min(decode, (t2 - t1) / (double) calls);
                    lenient = Math.min(lenient, (t3 - t2) / (double) calls);
                }
            }
            System.out.printf("size=%d encode ns/call=%.1f decode ns/call=%.1f decode-wrapped ns/call=%.1f%n", size,
                    encode, decode, lenient);
        }
        System.out.println("sink " + Long.toHexString(sink));
    }
}
