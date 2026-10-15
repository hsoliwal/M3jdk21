// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** Independent byte-level oracle for Java and optional direct-buffer C11 JNI line indexing. */
public final class ByteLineIndexChecks {
    private static final AtomicLong assertions = new AtomicLong();
    private ByteLineIndexChecks() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("usage: [absolute-native-library sha256]");
        }
        boolean nativeEnabled = args.length == 2;
        if (nativeEnabled) LfCount.install(Path.of(args[0]), args[1]);

        for (String sample : List.of("", "a", "\n", "\n\n", "a\n", "a\r\n", "a\rb", "a\u00e9\ud83d\ude80\nlast\n")) {
            byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
            checkAll(SourceArtifact.copyOf("sample", bytes).whole(), nativeEnabled, "sample");
        }
        // The byte range begins/ends in the middle of a larger owner; offsets
        // must remain relative to the precise range, not to the original owner.
        byte[] original = "prefix\na\r\nb\nend\nsuffix".getBytes(StandardCharsets.UTF_8);
        SourceArtifact owner = SourceArtifact.copyOf("parent", original);
        checkAll(owner.span(7, 8), nativeEnabled, "subrange");
        checkAll(owner.span(0, 0), nativeEnabled, "empty subrange");

        Random random = new Random(4211548L);
        for (int iteration = 0; iteration < 1400; iteration++) {
            byte[] bytes = new byte[random.nextInt(3072)];
            random.nextBytes(bytes);
            for (int i = 0; i < bytes.length; i += 31) bytes[i] = (byte)'\n';
            SourceArtifact sample = SourceArtifact.copyOf("random-" + iteration, bytes);
            int begin = random.nextInt(bytes.length + 1);
            int size = random.nextInt(bytes.length - begin + 1);
            checkAll(sample.span(begin, size), nativeEnabled, "random " + iteration);
        }
        // Explicit, bounded refusal: no accidental 4M-element retention from a
        // newline-saturated but otherwise admissible SourceArtifact.
        byte[] saturated = new byte[ByteLineIndex.MAX_LINE_BREAKS + 1];
        Arrays.fill(saturated, (byte)'\n');
        var crowded = SourceArtifact.copyOf("saturated", saturated).whole();
        refuse(IllegalArgumentException.class, () -> ByteLineIndex.javaIndex(crowded));
        if (nativeEnabled) {
            refuse(IllegalArgumentException.class, () -> ByteLineIndex.nativeIndex(crowded));
        } else {
            refuse(IllegalStateException.class,
                    () -> ByteLineIndex.nativeIndex(owner.whole()));
        }
        ByteBuffer heap = ByteBuffer.wrap(new byte[]{10, 13, 10});
        refuse(IllegalArgumentException.class,
                () -> LfCount.nativeLineEnds(heap, 0, 3, 2));
        refuse(IndexOutOfBoundsException.class,
                () -> LfCount.nativeLineEnds(ByteBuffer.allocateDirect(4), -1, 1, 2));
        refuse(IndexOutOfBoundsException.class,
                () -> LfCount.nativeLineEnds(ByteBuffer.allocateDirect(4), 3, 2, 2));

        // The native extension uses coordinates relative to the specified
        // logical subrange even when a direct buffer has a nonzero position.
        ByteBuffer shifted = ByteBuffer.allocateDirect(12);
        shifted.put(new byte[]{'x', 'y', 'z', 'a', '\n', 'b', '\n', 'q', 'r'}).flip();
        shifted.position(2);
        if (nativeEnabled) {
            check(Arrays.equals(new int[]{2, 4},
                    LfCount.nativeLineEnds(shifted, 3, 4, 2)),
                    "JNI subrange line end offsets");
            check(Arrays.equals(new int[]{2, 4},
                    LfCount.nativeLineEnds(shifted.asReadOnlyBuffer(), 3, 4, 2)),
                    "JNI read-only line end offsets");
            refuse(IllegalArgumentException.class,
                    () -> LfCount.nativeLineEnds(shifted, 3, 4, 1));
        } else {
            refuse(IllegalStateException.class,
                    () -> LfCount.nativeLineEnds(shifted, 3, 4, 2));
        }
        equal(2, shifted.position(), "JNI did not modify direct position");
        equal(9, shifted.limit(), "JNI did not modify direct limit");

        // Independent virtual threads may read the same immutable source and
        // compute identical retained metadata without synchronized global caches.
        byte[] sharedBytes = new byte[16_384];
        for (int i = 0; i < sharedBytes.length; i++) sharedBytes[i] = (byte)(i % 9 == 0 ? '\n' : 'x');
        var shared = SourceArtifact.copyOf("shared", sharedBytes).whole();
        var expected = ByteLineIndex.javaIndex(shared);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> jobs = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                jobs.add(threads.submit(() -> {
                    ByteLineIndex actual = ByteLineIndex.javaIndex(shared);
                    equal(expected.lineCount(), actual.lineCount(), "parallel same line count");
                    for (int j = 0; j < actual.lineCount(); j += 16) {
                        equal(expected.startOffset(j), actual.startOffset(j), "parallel offset");
                    }
                }));
            }
            for (Future<?> job : jobs) job.get();
        }
        System.out.println("M3_BYTE_LINE_INDEX_PASS assertions=" + assertions.get()
                + " kernel=" + (nativeEnabled ? "JNI_DIRECT_C11" : "JAVA_SOURCE_RANGE"));
    }

    private static void checkAll(SourceArtifact.Span source, boolean nativeEnabled, String tag) {
        byte[] bytes = source.copy();
        int[] starts = independentStarts(bytes);
        ByteLineIndex index = ByteLineIndex.javaIndex(source);
        equal(starts.length, index.lineCount(), tag + " count");
        equal(source.length(), index.sourceLength(), tag + " source length");
        equal(4L * starts.length, index.retainedPrimitiveBytes(), tag + " retained int bytes");
        check(index.rangeRoot().equals(source.sha256()), tag + " exact range SHA");
        check(index.sourceRoot().equals(source.source().root()), tag + " owner SHA");
        for (int i = 0; i < starts.length; i++) {
            int end = i + 1 < starts.length ? starts[i + 1] : bytes.length;
            equal(starts[i], index.startOffset(i), tag + " start");
            equal(end, index.endOffset(i), tag + " end");
            check(Arrays.equals(Arrays.copyOfRange(bytes, starts[i], end), index.line(i).copy()),
                    tag + " exact LF-including byte view");
            int trimmed = end > starts[i] && bytes[end - 1] == (byte)'\n' ? end - 1 : end;
            check(Arrays.equals(Arrays.copyOfRange(bytes, starts[i], trimmed),
                    index.content(i).copy()), tag + " LF-only content view");
        }
        // EOF is a valid cursor; all other out-of-range requests refuse.
        for (int p = 0; p <= bytes.length; p += Math.max(1, bytes.length / 29)) {
            int expected = 0;
            while (expected + 1 < starts.length && starts[expected + 1] <= p) expected++;
            equal(expected, index.lineAtOffset(p), tag + " byte offset lookup");
        }
        equal(starts.length - 1, index.lineAtOffset(bytes.length), tag + " final cursor");
        refuse(IndexOutOfBoundsException.class, () -> index.startOffset(-1));
        refuse(IndexOutOfBoundsException.class, () -> index.endOffset(starts.length));
        refuse(IndexOutOfBoundsException.class, () -> index.lineAtOffset(-1));
        refuse(IndexOutOfBoundsException.class, () -> index.lineAtOffset(bytes.length + 1));

        if (nativeEnabled) {
            ByteLineIndex accelerated = ByteLineIndex.nativeIndex(source);
            equal(starts.length, accelerated.lineCount(), tag + " JNI count");
            for (int i = 0; i < starts.length; i++) {
                equal(starts[i], accelerated.startOffset(i), tag + " JNI boundary");
                check(Arrays.equals(index.line(i).copy(), accelerated.line(i).copy()),
                        tag + " JNI exact bytes");
            }
            check(accelerated.rangeRoot().equals(index.rangeRoot()), tag + " JNI source identity");
        }
    }

    private static int[] independentStarts(byte[] bytes) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < bytes.length; i++) if (bytes[i] == (byte)'\n') starts.add(i + 1);
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }
    private static void equal(long a, long b, String reason) { check(a == b, reason + " " + a + " != " + b); }
    private static void check(boolean yes, String why) {
        assertions.incrementAndGet();
        if (!yes) throw new AssertionError(why);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable action) {
        assertions.incrementAndGet();
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong refusal " + error, error);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
}
