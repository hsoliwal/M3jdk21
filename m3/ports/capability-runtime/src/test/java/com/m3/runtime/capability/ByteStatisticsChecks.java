// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;

/** Differential Java 21 oracle over immutable source-byte ranges and primitive masks. */
public final class ByteStatisticsChecks {
    private static final java.util.concurrent.atomic.AtomicLong checks =
            new java.util.concurrent.atomic.AtomicLong();
    private ByteStatisticsChecks() { }

    private static void check(boolean accepted, String detail) {
        checks.incrementAndGet();
        if (!accepted) throw new AssertionError(detail);
    }

    private static void closeTo(double expected, double actual, String detail) {
        check(Double.isFinite(actual) && Math.abs(expected - actual) <= 2e-12,
              detail + ": expected=" + expected + ", actual=" + actual);
    }

    private static void refuse(Class<? extends Throwable> expected, Runnable action) {
        checks.incrementAndGet();
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError("wrong refusal: " + thrown, thrown);
        }
        throw new AssertionError("no refusal of " + expected.getSimpleName());
    }

    private static void sample(byte[] original, int offset, int length) {
        byte[] snapshot = original.clone();
        SourceArtifact source = SourceArtifact.copyOf("statistics", original);
        SourceArtifact.Span span = source.span(offset, length);
        Arrays.fill(original, (byte) 0x55);
        ByteStatistics facts = ByteStatistics.scan(span);
        long[] expected = new long[256];
        for (int i = offset; i < offset + length; i++) expected[Byte.toUnsignedInt(snapshot[i])]++;
        long[] returned = facts.histogramCopy();
        check(Arrays.equals(expected, returned), "256-bin differential histogram");
        returned[0] = -1;
        check(facts.count(0) == expected[0], "histogram not caller-mutable");
        check(facts.byteLength() == length, "range byte length");
        check(facts.retainedPrimitiveBytes() == 2_080, "fixed primitive byte accounting");
        check(facts.sourceRoot().equals(source.root()), "canonical source owner");
        check(facts.rangeRoot().equals(span.sha256()), "range source root");
        int distinct = 0, modal = -1;
        long largest = 0, high = 0;
        double entropy = 0;
        for (int i = 0; i < 256; i++) {
            check(expected[i] == facts.count(i), "per-byte exact count");
            if (expected[i] > 0) {
                distinct++;
                if (expected[i] > largest) { largest = expected[i]; modal = i; }
                double p = (double) expected[i] / length;
                entropy -= p * StrictMath.log(p) / StrictMath.log(2.0);
                if (i >= 128) high += expected[i];
            }
            check(((facts.presenceMaskWord(i >>> 6) >>> (i & 63)) & 1L)
                    == (expected[i] > 0 ? 1L : 0L), "presence mask");
        }
        check(facts.distinctByteCount() == distinct, "distinct count");
        check(facts.modalUnsignedByte() == modal, "mode tie-lowest, empty=-1");
        check(facts.nonAsciiBytes() == high, "high-bit histogram mass");
        closeTo(entropy, facts.shannonEntropyBitsPerByte(), "entropy oracle");
        if (length > 0) {
            closeTo(0.0, facts.jensenShannonBits(facts), "identical JS distributions");
        } else {
            refuse(IllegalArgumentException.class, () -> facts.jensenShannonBits(facts));
        }
        List<String> expectedRuns = new ArrayList<>();
        int i = offset;
        while (i < offset + length) {
            final int code = Byte.toUnsignedInt(snapshot[i]);
            int j = i + 1;
            while (j < offset + length && Byte.toUnsignedInt(snapshot[j]) == code) j++;
            expectedRuns.add(code + ":" + (i - offset) + ":" + (j - offset));
            i = j;
        }
        List<String> actual = new ArrayList<>();
        int emitted = ByteStatistics.scanRuns(span, ByteStatistics.MAX_RUNS,
                (value, from, to) -> actual.add(value + ":" + from + ":" + to));
        check(emitted == expectedRuns.size() && expectedRuns.equals(actual), "source-range run oracle");
        actual.clear();
        int cap = Math.min(3, expectedRuns.size());
        check(ByteStatistics.scanRuns(span, cap,
              (value, from, to) -> actual.add(value + ":" + from + ":" + to)) == cap,
              "run cap count");
        check(actual.equals(expectedRuns.subList(0, cap)), "run cap prefix");
    }

    public static void main(String[] ignored) throws Exception {
        sample(new byte[0], 0, 0);
        sample(new byte[]{(byte) 255, (byte) 255, 0, 0, 1, (byte) 128}, 1, 4);
        sample("A\r\n🚀\nAA".getBytes(StandardCharsets.UTF_8), 0,
               "A\r\n🚀\nAA".getBytes(StandardCharsets.UTF_8).length);
        Random random = new Random(0x4D33B73L);
        for (int trial = 0; trial < 1_200; trial++) {
            byte[] data = new byte[random.nextInt(2_049)];
            random.nextBytes(data);
            int start = random.nextInt(data.length + 1);
            int len = random.nextInt(data.length - start + 1);
            sample(data, start, len);
        }
        ByteStatistics a = ByteStatistics.scan(SourceArtifact.copyOf("a", new byte[]{0, 0}).whole());
        ByteStatistics b = ByteStatistics.scan(SourceArtifact.copyOf("b", new byte[]{1, 1}).whole());
        closeTo(1.0, a.jensenShannonBits(b), "disjoint distributions are one bit apart");
        closeTo(a.jensenShannonBits(b), b.jensenShannonBits(a), "JS symmetric");
        ByteStatistics c = ByteStatistics.scan(SourceArtifact.copyOf("c", new byte[]{0, 1}).whole());
        check(a.jensenShannonBits(c) > 0 && a.jensenShannonBits(c) < 1,
              "mixed distribution divergence bounded");
        refuse(IndexOutOfBoundsException.class, () -> a.count(-1));
        refuse(IndexOutOfBoundsException.class, () -> a.count(256));
        refuse(IndexOutOfBoundsException.class, () -> a.presenceMaskWord(4));
        SourceArtifact.Span one = SourceArtifact.copyOf("runs", new byte[]{1, 1, 2}).whole();
        refuse(IllegalArgumentException.class,
                () -> ByteStatistics.scanRuns(one, -1, (v, from, to) -> { }));
        refuse(IllegalArgumentException.class,
                () -> ByteStatistics.scanRuns(one, ByteStatistics.MAX_RUNS + 1,
                        (v, from, to) -> { }));
        check(ByteStatistics.scanRuns(one, 0,
                (v, from, to) -> { throw new AssertionError("zero run cap callback"); }) == 0,
                "zero run cap");
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 16; i++) futures.add(pool.submit(() -> {
                for (int k = 0; k < 12; k++) {
                    check(ByteStatistics.scan(one).count(1) == 2,
                          "concurrent immutable span scan");
                }
            }));
            for (var future : futures) future.get();
        }
        Thread.currentThread().interrupt();
        try {
            refuse(CancellationException.class, () -> ByteStatistics.scan(one));
            refuse(CancellationException.class,
                   () -> ByteStatistics.scanRuns(one, 0, (v, from, to) -> { }));
        } finally {
            Thread.interrupted();
        }
        System.out.println("M3_BYTE_STATISTICS_PASS assertions=" + checks.get()
                + " kernel=JAVA_256_BIN_MASK_RLE");
    }
}
