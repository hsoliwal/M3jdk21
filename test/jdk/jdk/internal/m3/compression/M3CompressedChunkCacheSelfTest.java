// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** JDK-only deterministic corpus using standard Deflater/Inflater oracles. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressedChunkCacheSelfTest.java blob 75a57329bf3e; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressedChunkCacheSelfTest {
    private static int checks;

    private M3CompressedChunkCacheSelfTest() {}

    public static void main(String[] args) throws Exception {
        runAll();
    }

    public static void runAll() throws Exception {
        reject(() -> new M3CompressedChunkCache(0, 100));
        reject(() -> new M3CompressedChunkCache(13, 100));
        reject(() -> new M3CompressedChunkCache(4, 0));
        M3CompressedChunkCache cache = new M3CompressedChunkCache(6, 1 << 20);
        byte[] input = new byte[8192];
        Arrays.fill(input, (byte) 0x5a);
        byte[] original = input.clone();
        AtomicInteger encodedCalls = new AtomicInteger();
        M3CompressedChunkCache.DeterministicEncoder codec = value -> {
            encodedCalls.incrementAndGet();
            return deflate(value);
        };
        String profile = "deflate/raw:level=1:dictionary=none:version=java21";
        byte[] first = cache.encode(input, 0, input.length, profile, codec);
        check(Arrays.equals(first, deflate(original)), "exact encoded oracle");
        byte[] second = cache.encode(original, 0, original.length, profile, codec);
        check(encodedCalls.get() == 1, "second use bypasses encoder");
        check(Arrays.equals(first, second), "cache bytes stable");
        check(cache.cacheHits() == 1 && cache.cacheMisses() == 1, "cache accounting");
        first[0] ^= 0x55;
        byte[] third = cache.encode(original, 0, original.length, profile, codec);
        check(Arrays.equals(second, third), "returned byte array never aliases cache");
        input[0] ^= 1;
        byte[] changed = cache.encode(input, 0, input.length, profile, codec);
        check(Arrays.equals(changed, deflate(input)), "mutation invalidates exact content");
        check(Arrays.equals(inflate(changed, input.length), input), "valid independent decoding");
        cache.encode(original, 0, original.length, profile + ":variant", codec);
        check(encodedCalls.get() == 3, "configuration identity separates variants");
        byte[] padded = new byte[original.length + 10];
        System.arraycopy(original, 0, padded, 4, original.length);
        check(Arrays.equals(cache.encode(padded, 4, original.length, profile, codec), second),
                "window offset preserves exact bytes");
        reject(() -> cache.encode(padded, -1, 1, profile, codec));
        reject(() -> cache.encode(padded, 0, padded.length + 1, profile, codec));
        reject(() -> cache.encode(padded, 0, 1, "  ", codec));
        reject(() -> cache.encode(padded, 0, 1, "x".repeat(513), codec));
        rejectNull(() -> cache.encode(null, 0, 0, profile, codec));
        rejectNull(() -> cache.encode(padded, 0, 1, null, codec));
        rejectNull(() -> cache.encode(padded, 0, 1, profile, null));
        rejectNull(() -> cache.encode(padded, 0, 1, "returns-null", value -> null));
        M3CompressedChunkCache tiny = new M3CompressedChunkCache(1, 1);
        byte[] uncached = tiny.encode(original, 0, original.length, profile, codec);
        check(Arrays.equals(uncached, deflate(original)), "oversized chunk still encodes");
        check(tiny.retainedEntries() == 0 && tiny.retainedPayloadBytes() == 0,
                "oversized chunk is not retained");
        rejectState(() -> cache.encode(original, 0, original.length, "mutator", value -> {
            value[0] ^= 1;
            return deflate(value);
        }));

        Random random = new Random(0x4d33434f_4d505245L);
        M3CompressedChunkCache collided = new M3CompressedChunkCache(1, 1300);
        for (int iteration = 0; iteration < 500; iteration++) {
            int length = random.nextInt(900);
            byte[] data = new byte[length];
            random.nextBytes(data);
            if (iteration % 3 == 0) Arrays.fill(data, (byte) (iteration & 15));
            String currentProfile = iteration % 2 == 0 ? profile : profile + ":b";
            byte[] got = collided.encode(data, 0, data.length, currentProfile,
                    M3CompressedChunkCacheSelfTest::deflate);
            check(Arrays.equals(got, deflate(data)), "collision never returns wrong bytes");
            check(Arrays.equals(inflate(got, data.length), data), "standard DEFLATE decoder");
            check(collided.retainedPayloadBytes() <= 1300, "hard payload budget");
            check(collided.retainedEntries() <= 2, "hard slot bound");
        }
        collided.trim();
        check(collided.retainedPayloadBytes() == 0 && collided.retainedEntries() == 0,
                "trim clears retained content");
        AtomicInteger afterTrim = new AtomicInteger();
        collided.encode(original, 0, original.length, profile, value -> {
            afterTrim.incrementAndGet();
            return deflate(value);
        });
        check(afterTrim.get() == 1, "trim cannot return stale bytes");
        threadSafety(profile);
        inFlightTrim(profile);
        System.out.println("M3 compressed chunk cache checks=" + checks);
    }

    private static void threadSafety(String profile) throws Exception {
        final int workers = 6;
        final int iterations = 100;
        M3CompressedChunkCache concurrent = new M3CompressedChunkCache(4, 1 << 16);
        Throwable[] failures = new Throwable[workers];
        Thread[] threads = new Thread[workers];
        CountDownLatch gate = new CountDownLatch(1);
        for (int w = 0; w < workers; w++) {
            final int worker = w;
            threads[w] = new Thread(() -> {
                try {
                    gate.await();
                    for (int i = 0; i < iterations; i++) {
                        byte[] raw = new byte[100 + (i % 13)];
                        Arrays.fill(raw, (byte) (i % 7));
                        byte[] compressed = concurrent.encode(raw, 0, raw.length,
                                profile, M3CompressedChunkCacheSelfTest::deflate);
                        if (!Arrays.equals(raw, inflate(compressed, raw.length))) {
                            throw new AssertionError("concurrent chunk mismatch");
                        }
                    }
                } catch (Throwable error) {
                    failures[worker] = error;
                }
            });
            threads[w].start();
        }
        gate.countDown();
        for (Thread thread : threads) thread.join();
        for (Throwable failure : failures) {
            if (failure != null) throw new AssertionError("concurrent cache failure", failure);
        }
        check(concurrent.retainedPayloadBytes() <= 1 << 16, "concurrent payload bound");
    }


    /** A memory-pressure trim must revoke every older in-flight cache insertion. */
    private static void inFlightTrim(String profile) throws Exception {
        M3CompressedChunkCache reentrant = new M3CompressedChunkCache(2, 4096);
        byte[] raw = new byte[1024];
        Arrays.fill(raw, (byte) 42);
        byte[] first = reentrant.encode(raw, 0, raw.length, profile, bytes -> {
            reentrant.trim();
            return deflate(bytes);
        });
        check(Arrays.equals(inflate(first, raw.length), raw), "reentrant trim keeps output");
        check(reentrant.retainedEntries() == 0, "reentrant trim prevents stale installation");

        M3CompressedChunkCache concurrent = new M3CompressedChunkCache(2, 4096);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicReference<byte[]> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                result.set(concurrent.encode(raw, 0, raw.length, profile, bytes -> {
                    entered.countDown();
                    try {
                        if (!resume.await(20, TimeUnit.SECONDS)) {
                            throw new AssertionError("compression resume timed out");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("compression interrupted", interrupted);
                    }
                    return deflate(bytes);
                }));
            } catch (Throwable failure) {
                error.set(failure);
            }
        }, "m3-compression-inflight-trim");
        worker.setDaemon(true);
        worker.start();
        try {
            check(entered.await(20, TimeUnit.SECONDS), "encoder entered outside monitor");
            concurrent.trim();
        } finally {
            resume.countDown();
        }
        worker.join(20_000L);
        check(!worker.isAlive(), "in-flight compression completed");
        if (error.get() != null) throw new AssertionError("in-flight trim", error.get());
        check(Arrays.equals(inflate(result.get(), raw.length), raw),
                "in-flight output stays decompressible");
        check(concurrent.retainedEntries() == 0
                && concurrent.retainedPayloadBytes() == 0,
                "in-flight completion cannot repopulate a trimmed cache");
    }

    private static byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(1, true);
        try {
            deflater.setInput(source);
            deflater.finish();
            byte[] scratch = new byte[256];
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (!deflater.finished()) {
                int amount = deflater.deflate(scratch);
                if (amount == 0 && !deflater.finished()) {
                    throw new AssertionError("DEFLATE encoder stalled");
                }
                output.write(scratch, 0, amount);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] encoded, int length) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(encoded);
            byte[] result = new byte[Math.max(1, length)];
            int produced = 0;
            while (!inflater.finished()) {
                int count = inflater.inflate(result, produced, result.length - produced);
                produced += count;
                if (count == 0 && !inflater.finished()) {
                    throw new AssertionError("DEFLATE decoder stalled");
                }
            }
            if (produced != length || inflater.getRemaining() != 0) {
                throw new AssertionError("DEFLATE decoded length mismatch");
            }
            return Arrays.copyOf(result, length);
        } finally {
            inflater.end();
        }
    }

    private static void reject(Runnable request) {
        try {
            request.run();
        } catch (IllegalArgumentException accepted) {
            checks++;
            return;
        }
        throw new AssertionError("invalid cache request accepted");
    }

    private static void rejectNull(Runnable request) {
        try {
            request.run();
        } catch (NullPointerException accepted) {
            checks++;
            return;
        }
        throw new AssertionError("null input accepted");
    }

    private static void rejectState(Runnable request) {
        try {
            request.run();
        } catch (IllegalStateException accepted) {
            checks++;
            return;
        }
        throw new AssertionError("mutating encoder accepted");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at check=" + checks);
    }
}
