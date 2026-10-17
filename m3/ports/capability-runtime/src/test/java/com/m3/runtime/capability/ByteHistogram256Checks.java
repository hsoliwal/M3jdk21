// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual JDK21 differential oracle for byte histograms, entropy and source RLE runs. */
public final class ByteHistogram256Checks {
    private static long checks;
    private ByteHistogram256Checks() { }

    private static void check(boolean valid, String reason) {
        checks++;
        if (!valid) throw new AssertionError(reason);
    }

    private static void refusal(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("unexpected exception: " + error, error);
        }
        throw new AssertionError("expected exception: " + kind.getSimpleName());
    }

    private static long[] oracle(byte[] bytes, int from, int length) {
        long[] values = new long[256];
        for (int i = from; i < from + length; i++) values[bytes[i] & 0xff]++;
        return values;
    }

    private static List<String> oracleRuns(byte[] bytes, int from, int length) {
        List<String> result = new ArrayList<>();
        for (int i = from, end = from + length; i < end;) {
            int current = bytes[i] & 0xff;
            int finish = i + 1;
            while (finish < end && (bytes[finish] & 0xff) == current) finish++;
            result.add(current + ":" + (i - from) + ":" + (finish - from));
            i = finish;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("[absolute-native-library sha256]");
        }
        boolean nativeEnabled = args.length == 2;
        if (nativeEnabled) LfCount.install(Path.of(args[0]), args[1]);
        Random random = new Random(39173811L);
        for (int iteration = 0; iteration < 2400; iteration++) {
            byte[] bytes = new byte[random.nextInt(8192)];
            random.nextBytes(bytes);
            for (int i = 0; i < bytes.length; i += 17) bytes[i] = (byte) 255;
            int start = random.nextInt(bytes.length + 1);
            int length = random.nextInt(bytes.length - start + 1);
            SourceArtifact.Span slice = SourceArtifact.copyOf("hist-" + iteration, bytes).span(start, length);
            long[] expected = oracle(bytes, start, length);
            check(Arrays.equals(expected, ByteHistogram256.javaCount(slice)), "source histogram " + iteration);
            ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length).put(bytes);
            direct.flip();
            ByteBuffer readOnly = direct.asReadOnlyBuffer();
            ByteBuffer heap = ByteBuffer.wrap(bytes);
            check(Arrays.equals(expected, ByteHistogram256.javaCount(heap, start, length)), "heap histogram");
            check(Arrays.equals(expected, ByteHistogram256.javaCount(readOnly, start, length)), "read-only histogram");
            int pos = direct.position(), limit = direct.limit();
            check(Arrays.equals(expected, ByteHistogram256.javaCount(direct, start, length)), "direct histogram");
            check(pos == direct.position() && limit == direct.limit(), "buffer geometry preserved");
            if (nativeEnabled) {
                check(Arrays.equals(expected, ByteHistogram256.nativeCount(direct, start, length)), "native direct parity");
                check(Arrays.equals(expected, ByteHistogram256.nativeCount(readOnly, start, length)), "native read-only parity");
                check(Arrays.equals(expected, ByteHistogram256.nativeCount(slice)), "native source parity");
                refusal(IllegalArgumentException.class,
                        () -> ByteHistogram256.nativeCount(heap, start, length));
            } else if (iteration == 0) {
                refusal(IllegalStateException.class,
                        () -> ByteHistogram256.nativeCount(direct, start, length));
            }
            List<String> expectedRuns = oracleRuns(bytes, start, length);
            List<String> actual = new ArrayList<>();
            int runs = ByteHistogram256.scanRuns(slice, expectedRuns.size(),
                    (unit, from, to) -> actual.add(unit + ":" + from + ":" + to));
            check(runs == expectedRuns.size() && actual.equals(expectedRuns), "source run algebra");
            if (!expectedRuns.isEmpty()) {
                AtomicInteger delivered = new AtomicInteger();
                refusal(IllegalArgumentException.class, () -> ByteHistogram256.scanRuns(slice,
                        expectedRuns.size() - 1, (unit, from, to) -> delivered.incrementAndGet()));
                check(delivered.get() == 0, "budget refusal before callbacks");
            }
            long sum = 0;
            for (long count : expected) sum += count;
            check(sum == length, "histogram conservation");
        }
        SourceArtifact.Span zeros = SourceArtifact.copyOf("empty", new byte[0]).whole();
        check(ByteHistogram256.scanRuns(zeros, 0, (unit, start, end) -> {
            throw new AssertionError("empty source emitted run");
        }) == 0, "empty source");
        check(ByteHistogram256.entropy(new long[256]) == 0.0, "entropy empty");
        long[] mono = new long[256]; mono[128] = 200;
        check(ByteHistogram256.entropy(mono) == 0.0, "entropy constant");
        long[] balanced = new long[256]; balanced[0] = 32; balanced[255] = 32;
        check(Math.abs(ByteHistogram256.entropy(balanced) - 1.0) < 1e-12, "entropy 1 bit");
        refusal(IllegalArgumentException.class, () -> ByteHistogram256.entropy(new long[2]));
        balanced[255] = -1;
        refusal(IllegalArgumentException.class, () -> ByteHistogram256.entropy(balanced));
        refusal(IndexOutOfBoundsException.class, () -> ByteHistogram256.javaCount(ByteBuffer.allocate(10), 7, 4));
        refusal(IllegalArgumentException.class, () -> ByteHistogram256.scanRuns(zeros, -1, (a,b,c) -> {}));
        refusal(IllegalArgumentException.class, () -> ByteHistogram256.scanRuns(zeros, ByteHistogram256.MAX_RUNS + 1, (a,b,c) -> {}));
        System.out.println("M3_BYTE_HISTOGRAM_PASS assertions=" + checks
                + " kernel=" + (nativeEnabled ? "JNI_DIRECT_C11" : "JAVA_SOURCE_RANGE"));
    }
}
