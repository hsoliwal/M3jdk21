// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Checked JNI regression for a strictly call-scoped aligned native LZ workspace. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzNativeArenaSelfTest.java blob 8c744cf5e963; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzNativeArenaSelfTest {
    private M3LzNativeArenaSelfTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected the built native library path");
        }
        M3LzNative nativeKernel = M3LzNative.load(Path.of(args[0]));
        int checks = smallAndFullBatches(nativeKernel);
        checks += deterministicCorpus(nativeKernel);
        checks += concurrentReplays(nativeKernel);
        System.out.println("M3 LZ JNI arena parity checks=" + checks);
    }

    private static int smallAndFullBatches(M3LzNative backend) {
        int checks = 0;
        for (int length = 0; length <= 19; length++) {
            byte[] block = new byte[length];
            Arrays.fill(block, (byte) 'a');
            int[] start = {0, length};
            int[] end = {length, 0};
            int[] limits = {0, 0};
            assertEquals(M3LzByteKernels.commonPrefixes(block, start, end, limits),
                    backend.commonPrefixes(block, start, end, limits));
            checks += start.length;
        }
        assertEquals(new int[0], backend.commonPrefixes(new byte[0], new int[0],
                new int[0], new int[0]));
        checks++;
        byte[] repeated = new byte[4096];
        Arrays.fill(repeated, (byte) 'x');
        final int count = 65_536;
        int[] left = new int[count];
        int[] right = new int[count];
        int[] maximum = new int[count];
        for (int i = 0; i < count; i++) {
            left[i] = i & 2047;
            right[i] = (i * 31) & 2047;
            maximum[i] = i & 15;
        }
        assertEquals(M3LzByteKernels.commonPrefixes(repeated, left, right, maximum),
                backend.commonPrefixes(repeated, left, right, maximum));
        checks += count;
        reject(() -> backend.commonPrefixes(new byte[1],
                new int[] {0}, new int[0], new int[] {0}));
        reject(() -> backend.commonPrefixes(new byte[1],
                new int[] {0}, new int[] {0}, new int[] {2}));
        reject(() -> backend.commonPrefixes(new byte[0],
                new int[count + 1], new int[count + 1], new int[count + 1]));
        checks += 3;
        return checks;
    }

    private static int deterministicCorpus(M3LzNative backend) {
        Random rng = new Random(0x4d334172_656e614cL);
        int checks = 0;
        for (int trial = 0; trial < 600; trial++) {
            int length = rng.nextInt(8192);
            byte[] bytes = new byte[length];
            rng.nextBytes(bytes);
            if (trial % 3 == 0) Arrays.fill(bytes, (byte) (trial & 255));
            if (trial % 7 == 0) {
                for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i & 15);
            }
            byte[] original = bytes.clone();
            int lanes = rng.nextInt(1025);
            int[] left = new int[lanes];
            int[] right = new int[lanes];
            int[] limit = new int[lanes];
            for (int i = 0; i < lanes; i++) {
                left[i] = rng.nextInt(length + 1);
                right[i] = rng.nextInt(length + 1);
                limit[i] = rng.nextInt(Math.min(length - left[i], length - right[i]) + 1);
            }
            int[] expect = M3LzByteKernels.commonPrefixes(bytes, left, right, limit);
            assertEquals(expect, backend.commonPrefixes(bytes, left, right, limit));
            if (!Arrays.equals(bytes, original)) throw new AssertionError("JNI modified block");
            checks += lanes + 1;
        }
        return checks;
    }

    private static int concurrentReplays(M3LzNative backend) throws InterruptedException {
        final int workers = 6;
        final int rounds = 48;
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread[] threads = new Thread[workers];
        for (int worker = 0; worker < workers; worker++) {
            int id = worker;
            threads[worker] = new Thread(() -> {
                try {
                    go.await();
                    byte[] bytes = new byte[96 + id];
                    Arrays.fill(bytes, (byte) id);
                    for (int round = 0; round < rounds; round++) {
                        int[] left = {0, 1, 7, bytes.length};
                        int[] right = {0, 2, 8, bytes.length};
                        int[] maximum = {64, 16, 32, 0};
                        assertEquals(M3LzByteKernels.commonPrefixes(bytes, left, right, maximum),
                                backend.commonPrefixes(bytes, left, right, maximum));
                    }
                } catch (Throwable failure) {
                    error.compareAndSet(null, failure);
                }
            }, "m3-native-arena-" + worker);
            threads[worker].start();
        }
        go.countDown();
        for (Thread thread : threads) thread.join();
        if (error.get() != null) throw new AssertionError("concurrent JNI parity", error.get());
        return workers * rounds * 4;
    }

    private static void reject(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid JNI lane request was accepted");
    }

    private static void assertEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("native prefix differs from Java oracle");
        }
    }
}
