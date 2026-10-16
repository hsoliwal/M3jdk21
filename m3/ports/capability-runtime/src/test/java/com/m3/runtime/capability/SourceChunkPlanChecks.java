// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent reverse-scan LF oracle for bounded source range chunk geometry. */
public final class SourceChunkPlanChecks {
    private static int assertions;
    private SourceChunkPlanChecks() { }
    private static void yes(boolean valid, String context) {
        assertions++;
        if (!valid) throw new AssertionError(context);
    }
    private static void same(int[] expected, int[] actual, String context) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(context + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }
    private static void sameBytes(byte[] expected, byte[] actual, String context) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(context + " immutable source-byte mismatch");
        }
    }
    private static void expect(Class<? extends Throwable> expected, Runnable call) {
        assertions++;
        try { call.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("unexpected " + error, error);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    /** Independent O(n * maxChunk) geometry oracle; deliberately not binary search. */
    private static int[] oracle(byte[] bytes, int offset, int length,
                                int minimum, int maximum) {
        List<Integer> cuts = new ArrayList<>();
        for (int cursor = 0; cursor < length;) {
            int limit = Math.min(length, cursor + maximum);
            int candidate = limit;
            if (limit != length) {
                for (int end = limit; end >= cursor + minimum; end--) {
                    if (bytes[offset + end - 1] == (byte) '\n') {
                        candidate = end;
                        break;
                    }
                }
            }
            cuts.add(candidate);
            cursor = candidate;
        }
        return cuts.stream().mapToInt(Integer::intValue).toArray();
    }
    private static int[] endings(SourceChunkPlan plan) {
        int[] cuts = new int[plan.chunkCount()];
        for (int i = 0; i < cuts.length; i++) cuts[i] = plan.endOffset(i);
        return cuts;
    }

    private static void verify(byte[] raw, int offset, int length, int min, int max,
                               boolean nativeEnabled, String context) {
        byte[] original = raw.clone();
        var artifact = SourceArtifact.copyOf(context, raw);
        var selected = artifact.span(offset, length);
        SourceChunkPlan javaPlan = SourceChunkPlan.javaPlan(selected, min, max);
        int[] expected = oracle(original, offset, length, min, max);
        same(expected, endings(javaPlan), "naive LF policy " + context);
        yes(javaPlan.chunkCount() == expected.length, "chunk count " + context);
        yes(javaPlan.retainedPrimitiveBytes() == (long) expected.length * 4,
                "primitive capacity " + context);
        yes(javaPlan.sourceLength() == length && javaPlan.minimumBytes() == min
                && javaPlan.maximumBytes() == max, "geometry " + context);
        yes(javaPlan.sourceRoot().equals(artifact.root())
                && javaPlan.rangeRoot().equals(selected.sha256()), "roots " + context);
        int total = 0;
        int previous = 0;
        for (int i = 0; i < javaPlan.chunkCount(); i++) {
            int start = javaPlan.startOffset(i), end = javaPlan.endOffset(i);
            var slice = javaPlan.chunk(i);
            yes(start == previous && end > start && end <= length,
                    "contiguous source " + context);
            yes(end - start <= max && (end - start >= min || end == length),
                    "hard chunk maximum, minimum except final tail " + context);
            yes(slice.source() == artifact && slice.start() == offset + start
                    && slice.length() == end - start, "preserved owner/coordinates " + context);
            sameBytes(Arrays.copyOfRange(original, offset + start, offset + end), slice.copy(),
                    "lossless bytes, including CRLF/UTF8 " + context);
            yes(javaPlan.chunkAtOffset(start) == i
                    && javaPlan.chunkAtOffset(end) == i + 1, "boundaries " + context);
            previous = end;
            total += slice.length();
        }
        yes(total == length && javaPlan.chunkAtOffset(length) == javaPlan.chunkCount(),
                "source recovery and EOF cursor " + context);
        if (length > 0) yes(javaPlan.chunkAtOffset(length - 1) == javaPlan.chunkCount() - 1,
                "last byte coordinate " + context);
        if (nativeEnabled) {
            SourceChunkPlan nativePlan = SourceChunkPlan.nativePlan(selected, min, max);
            same(expected, endings(nativePlan), "native LF-index parity " + context);
            yes(nativePlan.retainedPrimitiveBytes() == javaPlan.retainedPrimitiveBytes()
                    && nativePlan.rangeRoot().equals(javaPlan.rangeRoot()),
                    "native source proof " + context);
        }
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0 && arguments.length != 2) {
            throw new IllegalArgumentException("usage: [absolute-library sha256]");
        }
        boolean nativeEnabled = arguments.length == 2;
        if (nativeEnabled) LfCount.install(Path.of(arguments[0]), arguments[1]);
        verify(new byte[0], 0, 0, 1, 64, nativeEnabled, "empty");
        verify(new byte[]{42}, 0, 1, 3, 32, nativeEnabled, "short-final");
        verify(new byte[]{10, 10, 10, 10, 10, 10}, 0, 6, 2, 3,
                nativeEnabled, "dense-LF");
        verify(new byte[]{65, 13, 10, 66, 13, 10, 67, 13, 10}, 0, 9, 2, 4,
                nativeEnabled, "CRLF-alignment");
        byte[] unicode = "hello\né🚀\ntrailing".getBytes(StandardCharsets.UTF_8);
        verify(unicode, 1, unicode.length - 2, 3, 6, nativeEnabled, "UTF8-byte-cut");
        verify(new byte[65], 0, 65, 8, 8, nativeEnabled, "long-line-forced-split");
        verify(new byte[]{1, 2, 10, 3, 4, 10, 5, 6}, 1, 6, 2, 4,
                nativeEnabled, "subspan-coordinate");

        var x = SourceArtifact.copyOf("invalid", new byte[5]).whole();
        expect(NullPointerException.class, () -> SourceChunkPlan.javaPlan(null, 1, 2));
        expect(IllegalArgumentException.class, () -> SourceChunkPlan.javaPlan(x, 0, 2));
        expect(IllegalArgumentException.class, () -> SourceChunkPlan.javaPlan(x, 4, 3));
        expect(IllegalArgumentException.class, () -> SourceChunkPlan.javaPlan(x, 1,
                SourceArtifact.MAX_BYTES + 1));
        SourceChunkPlan empty = SourceChunkPlan.javaPlan(
                SourceArtifact.copyOf("e", new byte[0]).whole(), 1, 100);
        expect(IndexOutOfBoundsException.class, () -> empty.chunk(0));
        expect(IndexOutOfBoundsException.class, () -> empty.chunkAtOffset(1));
        expect(IndexOutOfBoundsException.class, () -> SourceChunkPlan.javaPlan(x, 1, 2).chunk(-1));

        Random random = new Random(0x504152544954494fL);
        for (int sample = 0; sample < 2200; sample++) {
            int length = random.nextInt(170);
            byte[] bytes = new byte[length + 11];
            for (int i = 0; i < bytes.length; i++) {
                int value = random.nextInt(9);
                bytes[i] = (byte)(value < 4 ? '\n' : value == 4 ? '\r' : random.nextInt(256));
            }
            int off = random.nextInt(12);
            int size = random.nextInt(bytes.length - off + 1);
            int min = 1 + random.nextInt(30);
            int max = min + random.nextInt(42);
            verify(bytes, off, size, min, max, nativeEnabled, "random-" + sample);
        }

        // Maximal admitted source; refuse millions of retained cut offsets rather
        // than allowing a small chunk size to exhaust the Java heap.
        var large = SourceArtifact.copyOf("pressure", new byte[1_000_002]).whole();
        expect(IllegalArgumentException.class, () -> SourceChunkPlan.javaPlan(large, 1, 1));
        if (nativeEnabled) {
            expect(IllegalArgumentException.class, () -> SourceChunkPlan.nativePlan(large, 1, 1));
        }
        var shared = SourceArtifact.copyOf("parallel", unicode).whole();
        SourceChunkPlan plan = SourceChunkPlan.javaPlan(shared, 2, 5);
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<int[]>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(pool.submit(() -> endings(plan)));
            }
            int[] expected = endings(plan);
            for (Future<int[]> future : tasks) same(expected, future.get(), "immutable concurrent cursors");
        }
        Thread.currentThread().interrupt();
        try {
            expect(CancellationException.class, () -> SourceChunkPlan.javaPlan(shared, 1, 4));
            if (nativeEnabled) {
                expect(CancellationException.class,
                        () -> SourceChunkPlan.nativePlan(shared, 1, 4));
            }
        } finally {
            Thread.interrupted();
        }
        yes(!Thread.currentThread().isInterrupted(), "interruption cleared by test");
        System.out.println("M3_SOURCE_CHUNK_PLAN_PASS assertions=" + assertions
                + " kernel=" + (nativeEnabled ? "JNI_LF_INDEX_C11" : "JAVA_LF_INDEX"));
    }
}
