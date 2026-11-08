// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Arrays;
import java.util.Random;

/** Deterministic format and bounds contract probes for both opt-in encoders. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionEncoderSelfTest.java blob 2e81ee7b80ce; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressionEncoderSelfTest {
    private static final int[] SIZES = {0, 1, 2, 3, 4, 5, 11, 12, 15, 16,
            31, 59, 60, 61, 127, 128, 255, 256, 257, 1024, 4096, 65535, 65536, 70000};
    private static int checks;

    private M3CompressionEncoderSelfTest() {}

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        M3SnappyRawEncoder snappy = new M3SnappyRawEncoder(12);
        M3Lz4RawBlockEncoder lz4 = new M3Lz4RawBlockEncoder(12);
        Random random = new Random(0x4d33534e41505059L);
        for (int sample = 0; sample < 2200; sample++) {
            int size = sample < SIZES.length * 8 ? SIZES[sample % SIZES.length]
                    : random.nextInt(16384);
            byte[] input = new byte[size];
            random.nextBytes(input);
            switch (sample % 8) {
                case 0 -> Arrays.fill(input, (byte) 0);
                case 1 -> Arrays.fill(input, (byte) 0x5a);
                case 2 -> {
                    for (int i = 0; i < input.length; i++) input[i] = (byte) i;
                }
                case 3 -> {
                    for (int i = 0; i < input.length; i++) input[i] = (byte) (i % 7);
                }
                case 4 -> {
                    byte[] motif = {'{', '"', 'k', '"', ':', '4', '2', '}'};
                    for (int i = 0; i < input.length; i++) input[i] = motif[i % motif.length];
                }
                case 5 -> {
                    int half = input.length / 2;
                    System.arraycopy(input, 0, input, input.length - half, half);
                }
                default -> { }
            }
            byte[] before = input.clone();
            int cap = Math.max(64, size * 2 + 128);
            byte[] compressedSnappy = snappy.encode(input, cap);
            byte[] compressedLz4 = lz4.encode(input, cap);
            check(Arrays.equals(before, input), "encoders must not alter inputs");
            check(Arrays.equals(before, M3SnappyRawDecoder.decode(compressedSnappy, size)),
                    "Snappy decoder roundtrip");
            check(Arrays.equals(before, M3Lz4RawBlockDecoder.decode(compressedLz4, size, size)),
                    "LZ4 decoder roundtrip");
            check(Arrays.equals(compressedSnappy, snappy.encode(input, compressedSnappy.length)),
                    "Snappy deterministic exact cap");
            check(Arrays.equals(compressedLz4, lz4.encode(input, compressedLz4.length)),
                    "LZ4 deterministic exact cap");
            if (sample % 5 == 0) {
                reject(() -> snappy.encode(input, compressedSnappy.length - 1));
                reject(() -> lz4.encode(input, compressedLz4.length - 1));
            }
        }
        byte[] repeating = new byte[180000];
        Arrays.fill(repeating, (byte) 'A');
        byte[] a = snappy.encode(repeating, repeating.length + 1000);
        byte[] b = lz4.encode(repeating, repeating.length + 1000);
        check(a.length < repeating.length / 8, "Snappy repetitive compression ratio");
        check(b.length < repeating.length / 8, "LZ4 repetitive compression ratio");
        check(Arrays.equals(repeating, M3SnappyRawDecoder.decode(a, repeating.length)), "Snappy long run");
        check(Arrays.equals(repeating, M3Lz4RawBlockDecoder.decode(b, repeating.length, repeating.length)),
                "LZ4 long run");
        reject(() -> new M3SnappyRawEncoder(7));
        reject(() -> new M3Lz4RawBlockEncoder(21));
        reject(() -> snappy.encode(new byte[0], 0));
        reject(() -> lz4.encode(new byte[0], 0));
        reject(() -> snappy.encode(new byte[4], -1));
        reject(() -> lz4.encode(new byte[4], -1));
        rejectNull(() -> snappy.encode(null, 10));
        rejectNull(() -> lz4.encode(null, 10));
        System.out.println("M3 compression encoder oracle checks=" + checks);
    }

    private static void reject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("invalid compression request accepted");
    }

    private static void rejectNull(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }
        throw new AssertionError("null input accepted");
    }

    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label + " #" + checks);
    }
}
