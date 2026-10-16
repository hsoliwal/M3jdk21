// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent byte-by-byte exhaustive oracle, not an invocation of the KMP algorithm. */
public final class BytePatternSearchChecks {
    private static int checks;

    private BytePatternSearchChecks() { }

    private static void yes(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static void same(int[] expected, int[] actual, String context) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(context + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable cause) {
            if (expected.isInstance(cause)) return;
            throw new AssertionError("unexpected exception " + cause, cause);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static int[] oracle(byte[] data, int offset, int count, byte[] pattern) {
        int[] hits = new int[Math.max(0, count - pattern.length + 1)];
        int found = 0;
        for (int i = 0; i + pattern.length <= count; i++) {
            boolean equal = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[offset + i + j] != pattern[j]) {
                    equal = false;
                    break;
                }
            }
            if (equal) hits[found++] = i;
        }
        return Arrays.copyOf(hits, found);
    }

    private static void verify(byte[] source, int start, int count, byte[] pattern,
                               int maxMatches, boolean nativeAvailable, String caseName) {
        byte[] original = source.clone();
        var artifact = SourceArtifact.copyOf("source-" + caseName, source);
        var span = artifact.span(start, count);
        var compiled = BytePatternSearch.compile(pattern);
        int[] expected = oracle(original, start, count, pattern);
        yes(compiled.patternLength() == pattern.length, "pattern length " + caseName);
        yes(compiled.retainedPrimitiveBytes() == (long) pattern.length * 5,
                "primitive memory " + caseName);
        boolean overflow = expected.length > maxMatches;
        if (overflow) {
            throwsType(IllegalArgumentException.class, () -> compiled.findAll(span, maxMatches));
            if (nativeAvailable) {
                throwsType(IllegalArgumentException.class,
                        () -> compiled.findAllNative(span, maxMatches));
            }
        } else {
            same(expected, compiled.findAll(span, maxMatches), "Java " + caseName);
            if (nativeAvailable) {
                same(expected, compiled.findAllNative(span, maxMatches), "JNI " + caseName);
            }
        }
        yes(artifact.root().equals(SourceArtifact.copyOf("another-id", original).root()),
                "source identity across different labels " + caseName);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("usage: [absolute library SHA-256]");
        }
        boolean nativeAvailable = args.length == 2;
        if (nativeAvailable) LfCount.install(Path.of(args[0]), args[1]);

        byte[] sample = "xxABA\nABA\r\nABABA\u00e9\ud83d\ude80".getBytes(StandardCharsets.UTF_8);
        byte[] needle = "ABA".getBytes(StandardCharsets.UTF_8);
        var sampleOwner = SourceArtifact.copyOf("sample", sample);
        var sampleSpan = sampleOwner.whole();
        var plan = BytePatternSearch.compile(needle);
        byte[] sampleSnapshot = sample.clone();
        needle[0] = '?';
        sample[0] = '?';
        same(oracle(sampleSnapshot, 0, sampleSnapshot.length, "ABA".getBytes(StandardCharsets.UTF_8)),
                plan.findAll(sampleSpan, 100), "immutable snapshot and compiled needle");
        if (nativeAvailable) same(plan.findAll(sampleSpan, 100),
                plan.findAllNative(sampleSpan, 100), "snapshot native parity");
        var lineIndex = ByteLineIndex.javaIndex(sampleSpan);
        int[] positions = plan.findAll(sampleSpan, 100);
        for (int hit : positions) {
            int line = lineIndex.lineAtOffset(hit);
            yes(hit >= lineIndex.startOffset(line) && hit < lineIndex.endOffset(line),
                    "hit and line-source coordinate agree");
        }
        verify("AAAAA".getBytes(StandardCharsets.US_ASCII), 0, 5,
                "AAA".getBytes(StandardCharsets.US_ASCII), 3, nativeAvailable, "overlapping");
        verify(new byte[]{0, -1, 1, 0, -1, 1, 0}, 1, 5,
                new byte[]{-1, 1}, 3, nativeAvailable, "signed-bytes-with-zero");
        verify(new byte[]{1, 2, 3, 2, 3, 4}, 2, 3,
                new byte[]{2, 3}, 1, nativeAvailable, "subrange-relative");
        verify(new byte[0], 0, 0, new byte[]{1}, 0, nativeAvailable, "empty-source");
        verify(new byte[]{1}, 0, 1, new byte[]{2}, 0, nativeAvailable, "no-matches-zero-budget");
        verify(new byte[]{1}, 0, 1, new byte[]{1}, 0, nativeAvailable, "hit-zero-budget");
        verify(new byte[]{1, 2, 3, 1, 2, 3}, 0, 6,
                new byte[]{1, 2, 3}, 1, nativeAvailable, "budget-refusal");

        throwsType(NullPointerException.class, () -> BytePatternSearch.compile(null));
        throwsType(IllegalArgumentException.class, () -> BytePatternSearch.compile(new byte[0]));
        throwsType(IllegalArgumentException.class,
                () -> BytePatternSearch.compile(new byte[BytePatternSearch.MAX_PATTERN_BYTES + 1]));
        throwsType(IllegalArgumentException.class, () -> plan.findAll(sampleSpan, -1));
        throwsType(IllegalArgumentException.class,
                () -> plan.findAll(sampleSpan, BytePatternSearch.MAX_MATCHES + 1));
        throwsType(NullPointerException.class, () -> plan.findAll(null, 1));

        byte[] longPattern = new byte[BytePatternSearch.MAX_PATTERN_BYTES];
        Arrays.fill(longPattern, (byte) 7);
        verify(longPattern, 0, longPattern.length, longPattern.clone(),
                1, nativeAvailable, "maximum-needle");
        byte[] highHit = new byte[4096];
        Arrays.fill(highHit, (byte) 19);
        verify(highHit, 0, highHit.length, new byte[]{19},
                4095, nativeAvailable, "dense-refusal");
        verify(highHit, 0, highHit.length, new byte[]{19},
                4096, nativeAvailable, "dense-exact");

        Random random = new Random(0x4350554c4dL);
        for (int n = 0; n < 6000; n++) {
            int length = random.nextInt(128);
            byte[] data = new byte[length + 7];
            // Generate repetitive data alongside binary-range bytes and zeros.
            for (int i = 0; i < data.length; i++) {
                int selector = random.nextInt(5);
                data[i] = (byte) (selector < 3 ? selector : random.nextInt(256));
            }
            int offset = random.nextInt(8);
            int count = random.nextInt(length + 8 - offset);
            int width = 1 + random.nextInt(18);
            byte[] pattern = new byte[width];
            if (random.nextBoolean() && count >= width) {
                int at = offset + random.nextInt(count - width + 1);
                System.arraycopy(data, at, pattern, 0, width);
            } else {
                for (int i = 0; i < width; i++) pattern[i] = (byte) random.nextInt(5);
            }
            int max = random.nextInt(32);
            verify(data, offset, count, pattern, max, nativeAvailable, "random-" + n);
        }

        // A single immutable plan is safe across independent Java/JNI calls.
        try (var workers = Executors.newFixedThreadPool(4)) {
            java.util.List<Future<int[]>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(workers.submit(() -> nativeAvailable
                        ? plan.findAllNative(sampleSpan, 100) : plan.findAll(sampleSpan, 100)));
            }
            for (Future<int[]> future : results) same(positions, future.get(), "concurrency");
        }
        Thread.currentThread().interrupt();
        try {
            throwsType(CancellationException.class, () -> plan.findAll(sampleSpan, 100));
            throwsType(CancellationException.class, () -> BytePatternSearch.compile(new byte[]{1}));
            if (nativeAvailable) {
                throwsType(CancellationException.class, () -> plan.findAllNative(sampleSpan, 100));
            }
        } finally {
            Thread.interrupted();
        }
        yes(!Thread.currentThread().isInterrupted(), "interruption state restored");
        System.out.println("M3_BYTE_PATTERN_SEARCH_PASS assertions=" + checks + " kernel="
                + (nativeAvailable ? "JNI_DIRECT_C11" : "JAVA_KMP"));
    }
}
