// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Repeatable bounded-input mutation and independent-worker codec safety oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionAdversarialSelfTest.java blob f69e26c6c321; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressionAdversarialSelfTest {
    private static final int RANDOM_DECODER_CASES = 100_000;
    private static final int MUTATED_BLOCK_CASES = 5_000;
    private static final int WORKERS = 4;
    private static final int CASES_PER_WORKER = 1_000;

    private M3CompressionAdversarialSelfTest() {}

    public static void main(String[] args) throws Exception {
        runAll();
    }

    public static void runAll() throws Exception {
        int checks = randomDecoderBounds() + mutateValidBlocks() + independentWorkers();
        System.out.println("M3 compression adversarial checks=" + checks
                + " (decoder/mutation/worker isolation)");
    }

    private static int randomDecoderBounds() {
        Random random = new Random(0x4d33_4445434f4445L);
        int checks = 0;
        for (int i = 0; i < RANDOM_DECODER_CASES; i++) {
            byte[] packed = new byte[random.nextInt(257)];
            random.nextBytes(packed);
            int expected = random.nextInt(1025);
            try {
                byte[] decoded = M3SnappyRawDecoder.decode(packed, 1024);
                require(decoded.length <= 1024, "Snappy output exceeded cap");
            } catch (IllegalArgumentException malformed) {
                // Correct rejection of an arbitrary invalid raw block.
            }
            checks++;
            try {
                byte[] decoded = M3Lz4RawBlockDecoder.decode(packed, expected, 1024);
                require(decoded.length == expected, "LZ4 output length mismatch");
            } catch (IllegalArgumentException malformed) {
                // Correct rejection of an arbitrary invalid raw block.
            }
            checks++;
        }
        return checks;
    }

    private static int mutateValidBlocks() {
        Random random = new Random(0x4d33_4d5554415445L);
        M3SnappyRawEncoder snappy = new M3SnappyRawEncoder(11);
        M3Lz4RawBlockEncoder lz4 = new M3Lz4RawBlockEncoder(11);
        int checks = 0;
        for (int i = 0; i < MUTATED_BLOCK_CASES; i++) {
            byte[] plain = new byte[random.nextInt(1025)];
            random.nextBytes(plain);
            if (i % 4 == 0) Arrays.fill(plain, (byte) 'Z');
            byte[] a = snappy.encode(plain, plain.length * 2 + 64);
            byte[] b = lz4.encode(plain, plain.length * 2 + 64);
            require(Arrays.equals(plain, M3SnappyRawDecoder.decode(a, plain.length)),
                    "Snappy pre-mutation roundtrip");
            require(Arrays.equals(plain, M3Lz4RawBlockDecoder.decode(b,
                    plain.length, plain.length)), "LZ4 pre-mutation roundtrip");
            checks += 2;
            a[random.nextInt(a.length)] ^= (byte) (1 << random.nextInt(8));
            b[random.nextInt(b.length)] ^= (byte) (1 << random.nextInt(8));
            checkDeterministicSnappy(a, plain.length);
            checkDeterministicLz4(b, plain.length);
            checks += 2;
        }
        return checks;
    }

    private static void checkDeterministicSnappy(byte[] packed, int maximumOutput) {
        byte[] first;
        try {
            first = M3SnappyRawDecoder.decode(packed, maximumOutput);
        } catch (IllegalArgumentException malformed) {
            expectRejection(() -> M3SnappyRawDecoder.decode(packed, maximumOutput));
            return;
        }
        byte[] second = M3SnappyRawDecoder.decode(packed, maximumOutput);
        require(Arrays.equals(first, second) && first.length <= maximumOutput,
                "non-deterministic Snappy corruption result");
    }

    private static void checkDeterministicLz4(byte[] packed, int expectedOutput) {
        byte[] first;
        try {
            first = M3Lz4RawBlockDecoder.decode(packed, expectedOutput, expectedOutput);
        } catch (IllegalArgumentException malformed) {
            expectRejection(() -> M3Lz4RawBlockDecoder.decode(packed,
                    expectedOutput, expectedOutput));
            return;
        }
        byte[] second = M3Lz4RawBlockDecoder.decode(packed, expectedOutput, expectedOutput);
        require(Arrays.equals(first, second) && first.length == expectedOutput,
                "non-deterministic LZ4 corruption result");
    }

    private static int independentWorkers() throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newFixedThreadPool(WORKERS)) {
            List<Future<Integer>> outcomes = new ArrayList<>(WORKERS);
            for (int worker = 0; worker < WORKERS; worker++) {
                final int workerId = worker;
                outcomes.add(executor.submit(() -> workerOracle(workerId)));
            }
            int checks = 0;
            for (Future<Integer> outcome : outcomes) checks += outcome.get();
            return checks;
        }
    }

    private static int workerOracle(int worker) {
        Random random = new Random(0x4d33_574f524b4552L + worker);
        M3SnappyRawEncoder snappy = new M3SnappyRawEncoder(10);
        M3Lz4RawBlockEncoder lz4 = new M3Lz4RawBlockEncoder(10);
        int checks = 0;
        for (int i = 0; i < CASES_PER_WORKER; i++) {
            byte[] source = new byte[random.nextInt(2049)];
            random.nextBytes(source);
            if (i % 3 == 0) Arrays.fill(source, (byte) ('A' + worker));
            byte[] before = source.clone();
            byte[] snappyBytes = snappy.encode(source, source.length * 2 + 64);
            byte[] lz4Bytes = lz4.encode(source, source.length * 2 + 64);
            require(Arrays.equals(before, source), "worker mutated input");
            require(Arrays.equals(source, M3SnappyRawDecoder.decode(snappyBytes, source.length)),
                    "worker Snappy roundtrip");
            require(Arrays.equals(source, M3Lz4RawBlockDecoder.decode(
                    lz4Bytes, source.length, source.length)), "worker LZ4 roundtrip");
            checks += 3;
        }
        return checks;
    }

    private static void expectRejection(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException correct) {
            return;
        }
        throw new AssertionError("corrupt block classification changed between replays");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
