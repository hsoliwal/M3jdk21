// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** JDK-only deterministic oracle shared by the Maven/JUnit and native replay lanes. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzSelfTest.java blob 2b54ef269881; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class LzSelfTest {
    private static int checks;

    private LzSelfTest() {}

    public static void main(String[] args) throws Exception {
        runAll();
    }

    public static void runAll() throws Exception {
        Random random = new Random(0x4d33_4c5a_2026L);
        for (int t = 0; t < 2000; t++) {
            byte[] bytes = new byte[random.nextInt(2048)];
            random.nextBytes(bytes);
            if (t % 3 == 0) Arrays.fill(bytes, (byte) 0x5a);
            int left = random.nextInt(bytes.length + 1);
            int right = random.nextInt(bytes.length + 1);
            int limit = random.nextInt(Math.min(bytes.length - left, bytes.length - right) + 1);
            int expected = 0;
            while (expected < limit && bytes[left + expected] == bytes[right + expected]) expected++;
            check(LzByteKernels.commonPrefix(bytes, left, right, limit) == expected, "Java prefix");
            if (bytes.length > 1) {
                int dst = 1 + random.nextInt(bytes.length - 1);
                int distance = 1 + random.nextInt(dst);
                int count = random.nextInt(bytes.length - dst + 1);
                byte[] oracle = bytes.clone();
                byte[] actual = bytes.clone();
                for (int i = 0; i < count; i++) oracle[dst + i] = oracle[dst + i - distance];
                LzByteKernels.copyMatch(actual, dst, distance, count);
                check(Arrays.equals(oracle, actual), "overlap copy");
            }
            byte[] literal = encodeLiteral(bytes);
            check(Arrays.equals(bytes, SnappyRawDecoder.decode(literal, bytes.length)), "Snappy literals");
        }
        check(Arrays.equals("aaaaaa".getBytes(StandardCharsets.US_ASCII),
                SnappyRawDecoder.decode(new byte[] {6, 0, 'a', 5, 1}, 6)), "copy1");
        check(Arrays.equals("aaa".getBytes(StandardCharsets.US_ASCII),
                SnappyRawDecoder.decode(new byte[] {3, 0, 'a', 6, 1, 0}, 3)), "copy2");
        check(Arrays.equals("aa".getBytes(StandardCharsets.US_ASCII),
                SnappyRawDecoder.decode(new byte[] {2, 0, 'a', 3, 1, 0, 0, 0}, 2)), "copy4");
        check(Arrays.equals(new byte[0], SnappyRawDecoder.decode(new byte[] {0}, 0)), "empty");

        reject(() -> SnappyRawDecoder.decode(new byte[0], 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {(byte) 0x80}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {2, 0, 'a'}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {1, 1, 0}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {1, 1}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {1, 2, 0, 0}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {1, 3, 0, 0, 0, 0}, 10));
        reject(() -> SnappyRawDecoder.decode(new byte[] {2, 0, 'a', 0, 'b'}, 1));
        reject(() -> LzByteKernels.commonPrefix(new byte[1], -1, 0, 0));
        reject(() -> LzByteKernels.commonPrefix(new byte[1], 0, 1, 1));
        reject(() -> LzByteKernels.copyMatch(new byte[1], 0, 1, 1));
        reject(() -> LzByteKernels.copyMatch(new byte[1], 1, 1, Integer.MAX_VALUE));
        reject(() -> LzByteKernels.commonPrefixes(new byte[0], new int[1], new int[0], new int[1]));

        String library = System.getProperty("m3.lz.library", "");
        if (!library.isBlank()) {
            LzNative backend = LzNative.load(Path.of(library));
            byte[] input = new byte[5000];
            random.nextBytes(input);
            int[] a = new int[4096];
            int[] b = new int[4096];
            int[] c = new int[4096];
            for (int i = 0; i < a.length; i++) {
                a[i] = random.nextInt(input.length + 1);
                b[i] = random.nextInt(input.length + 1);
                c[i] = random.nextInt(Math.min(input.length - a[i], input.length - b[i]) + 1);
            }
            byte[] before = input.clone();
            int[] expected = LzByteKernels.commonPrefixes(input, a, b, c);
            check(Arrays.equals(expected, backend.commonPrefixes(input, a, b, c)), "JNI parity");
            check(Arrays.equals(before, input), "JNI read-only");
            check(Arrays.equals(new int[0], backend.commonPrefixes(new byte[0],
                    new int[0], new int[0], new int[0])), "JNI empty");
        }
        System.out.println("M3 LZ Java/JNI oracle checks=" + checks
                + "; JNI=" + (!library.isBlank() ? "executed" : "not requested"));
    }

    private static byte[] encodeLiteral(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int n = data.length;
        while ((n & ~0x7f) != 0) {
            out.write((n & 0x7f) | 0x80);
            n >>>= 7;
        }
        out.write(n);
        if (data.length != 0) {
            int lengthMinusOne = data.length - 1;
            if (data.length <= 60) {
                out.write(lengthMinusOne << 2);
            } else {
                out.write((lengthMinusOne <= 255 ? 60 : 61) << 2);
                out.write(lengthMinusOne & 0xff);
                if (lengthMinusOne > 255) out.write((lengthMinusOne >>> 8) & 0xff);
            }
            out.writeBytes(data);
        }
        return out.toByteArray();
    }

    private static void reject(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("invalid input was accepted");
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message + " at check=" + checks);
    }
}
