// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent O(n*m) byte oracle and byte-by-byte line-coordinate oracle. */
public final class ByteEvidenceQueryChecks {
    private static long assertions;

    private ByteEvidenceQueryChecks() { }

    private static void yes(boolean value, String label) {
        assertions++;
        if (!value) throw new AssertionError(label);
    }

    private static void expected(Class<? extends Throwable> kind, Runnable call) {
        assertions++;
        try {
            call.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong failure: " + failure, failure);
        }
        throw new AssertionError("missing " + kind.getSimpleName());
    }

    private static int[] naive(byte[] source, int offset, int length, byte[] needle) {
        int[] result = new int[Math.max(0, length - needle.length + 1)];
        int hits = 0;
        for (int i = 0; i + needle.length <= length; i++) {
            boolean matched = true;
            for (int j = 0; j < needle.length; j++) {
                if (source[offset + i + j] != needle[j]) { matched = false; break; }
            }
            if (matched) result[hits++] = i;
        }
        return Arrays.copyOf(result, hits);
    }

    private static int lineOf(byte[] source, int offset, int endExclusive) {
        int line = 0;
        for (int j = offset; j < endExclusive; j++) if (source[j] == (byte) '\n') line++;
        return line;
    }

    private static int columnOf(byte[] source, int offset, int position) {
        int lastStart = offset;
        for (int j = offset; j < position; j++) {
            if (source[j] == (byte) '\n') lastStart = j + 1;
        }
        return position - lastStart;
    }

    private static void check(byte[] sourceBytes, int start, int length, byte[] pattern,
                              int maxMatches, boolean nativeReady, String name) {
        byte[] frozen = sourceBytes.clone();
        byte[] frozenPattern = pattern.clone();
        SourceArtifact source = SourceArtifact.copyOf("sample-" + name, sourceBytes);
        SourceArtifact.Span range = source.span(start, length);
        ByteEvidenceQuery query = ByteEvidenceQuery.compile(range, pattern);
        Arrays.fill(sourceBytes, (byte) 0x40);
        Arrays.fill(pattern, (byte) 0x3f);
        yes(query.sourceRoot().equals(source.root()), "source root: " + name);
        yes(query.rangeRoot().equals(range.sha256()), "range SHA: " + name);
        yes(query.patternRoot().equals(SourceArtifact.digest(frozenPattern, 0, frozenPattern.length)),
                "pattern SHA: " + name);
        yes(query.patternLength() == frozenPattern.length, "pattern width: " + name);
        int[] expectedOffsets = naive(frozen, start, length, frozenPattern);
        yes(query.retainedPrimitiveBytes() >= 5L * frozenPattern.length + Integer.BYTES,
                "bounded retained primitive index: " + name);
        if (expectedOffsets.length > maxMatches) {
            expected(IllegalArgumentException.class, () -> query.execute(maxMatches));
            if (nativeReady) expected(IllegalArgumentException.class,
                    () -> query.executeNative(maxMatches));
            return;
        }
        var result = query.execute(maxMatches);
        yes(result.count() == expectedOffsets.length, "hit count: " + name);
        yes(result.retainedPrimitiveBytes() == 4L * result.count(), "primitive hits: " + name);
        yes(result.sourceId().equals(source.id()), "source ID: " + name);
        yes(result.sourceRoot().equals(source.root()), "source identity: " + name);
        yes(result.rangeRoot().equals(query.rangeRoot()), "range identity: " + name);
        yes(result.patternRoot().equals(query.patternRoot()), "pattern identity: " + name);
        yes(result.maxMatches() == maxMatches, "max match receipt: " + name);
        yes(result.receipt().matches("[0-9a-f]{64}"), "sha receipt: " + name);
        query.verify(result);
        var replay = query.execute(maxMatches);
        yes(result.receipt().equals(replay.receipt()), "deterministic receipt: " + name);
        for (int i = 0; i < expectedOffsets.length; i++) {
            int hit = expectedOffsets[i];
            int end = hit + frozenPattern.length;
            yes(result.start(i) == hit && result.endExclusive(i) == end, "range: " + name);
            yes(result.absoluteStart(i) == start + hit
                    && result.absoluteEndExclusive(i) == start + end, "absolute: " + name);
            yes(result.firstLine(i) == lineOf(frozen, start, start + hit), "first line: " + name);
            yes(result.lastLine(i) == lineOf(frozen, start, start + end - 1), "last line: " + name);
            yes(result.firstColumnByte(i) == columnOf(frozen, start, start + hit),
                    "first byte column: " + name);
            yes(result.lastColumnByteExclusive(i) == columnOf(frozen, start, start + end - 1) + 1,
                    "last byte column: " + name);
            yes(Arrays.equals(result.evidence(i).copy(), frozenPattern), "evidence bytes: " + name);
            yes(result.evidence(i).sha256().equals(query.patternRoot()), "span SHA: " + name);
        }
        expected(IndexOutOfBoundsException.class, () -> result.start(-1));
        expected(IndexOutOfBoundsException.class, () -> result.evidence(result.count()));
        if (nativeReady) {
            var nativeResult = query.executeNative(maxMatches);
            yes(result.receipt().equals(nativeResult.receipt()), "native receipt parity: " + name);
            yes(result.count() == nativeResult.count(), "native result count: " + name);
            query.verify(nativeResult);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("usage: [absolute native library SHA256]");
        }
        boolean nativeReady = args.length == 2;
        if (nativeReady) LfCount.install(Path.of(args[0]), args[1]);

        byte[] multiline = "a\r\nABA\nABABAé🚀\n".getBytes(StandardCharsets.UTF_8);
        check(multiline.clone(), 0, multiline.length, new byte[]{'A', 'B', 'A'},
                64, nativeReady, "overlap-newlines-unicode");
        check(multiline.clone(), 0, multiline.length, new byte[]{'\n', 'A'},
                64, nativeReady, "line-seam");
        check(multiline.clone(), 1, multiline.length - 1, new byte[]{'\r', '\n'},
                64, nativeReady, "subrange-crlf");
        check(new byte[0], 0, 0, new byte[]{'a'}, 0, nativeReady, "empty");
        check(new byte[]{0, 0, 0, 0}, 0, 4, new byte[]{0, 0}, 3, nativeReady, "binary-overlap");
        check(new byte[]{0, 0, 0, 0}, 0, 4, new byte[]{0, 0}, 2, nativeReady, "budget-exceeded");
        check(new byte[]{(byte) 0xff, (byte) 0xf0, (byte) 0x9f}, 0, 3,
                new byte[]{(byte) 0xff}, 3, nativeReady, "malformed-utf8-no-decode");

        SourceArtifact owned = SourceArtifact.copyOf("one", new byte[]{10, 42, 10});
        ByteEvidenceQuery query = ByteEvidenceQuery.compile(owned.whole(), new byte[]{10});
        ByteEvidenceQuery other = ByteEvidenceQuery.compile(owned.whole(), new byte[]{10});
        expected(IllegalArgumentException.class, () -> other.verify(query.execute(2)));
        expected(NullPointerException.class, () -> ByteEvidenceQuery.compile(null, new byte[]{1}));
        expected(NullPointerException.class, () -> ByteEvidenceQuery.compile(owned.whole(), null));
        expected(IllegalArgumentException.class, () -> ByteEvidenceQuery.compile(owned.whole(), new byte[0]));
        expected(IllegalArgumentException.class, () -> query.execute(-1));
        expected(IllegalArgumentException.class, () -> query.execute(BytePatternSearch.MAX_MATCHES + 1));
        expected(NullPointerException.class, () -> query.verify(null));

        var receiptOne = query.execute(2);
        var receiptAgain = query.execute(3);
        yes(!receiptOne.receipt().equals(receiptAgain.receipt()), "limit affects receipt");
        yes(query.patternRoot().equals(other.patternRoot())
                && query.sourceRoot().equals(other.sourceRoot()), "same immutable content identities");
        yes(query.execute(2).receipt().equals(other.execute(2).receipt()),
                "deterministic equivalent query receipt");

        try (var workers = Executors.newFixedThreadPool(4)) {
            java.util.List<Future<String>> receipts = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                receipts.add(workers.submit(() -> {
                    ByteEvidenceQuery.Results r = nativeReady ? query.executeNative(2) : query.execute(2);
                    query.verify(r);
                    return r.receipt();
                }));
            }
            for (var work : receipts) {
                yes(work.get().equals(receiptOne.receipt()), "concurrent query reuse");
            }
        }

        Random random = new Random(0x456964656e63654cL);
        for (int iteration = 0; iteration < 3_500; iteration++) {
            int n = random.nextInt(72);
            byte[] source = new byte[n + 7];
            for (int j = 0; j < source.length; j++) {
                int selector = random.nextInt(5);
                source[j] = (byte) (selector == 0 ? '\n' : selector == 1 ? 0 : random.nextInt(8));
            }
            int start = random.nextInt(8);
            int length = random.nextInt(n + 8 - start);
            byte[] pattern = new byte[1 + random.nextInt(9)];
            if (pattern.length <= length && random.nextBoolean()) {
                int pos = start + random.nextInt(length - pattern.length + 1);
                System.arraycopy(source, pos, pattern, 0, pattern.length);
            } else {
                for (int i = 0; i < pattern.length; i++) pattern[i] = (byte) random.nextInt(6);
            }
            int limit = random.nextInt(12);
            check(source, start, length, pattern, limit, nativeReady, "random-" + iteration);
        }

        Thread.currentThread().interrupt();
        try {
            expected(CancellationException.class, () -> query.execute(2));
            expected(CancellationException.class, () -> ByteEvidenceQuery.compile(owned.whole(), new byte[]{10}));
            if (nativeReady) expected(CancellationException.class, () -> query.executeNative(2));
        } finally {
            Thread.interrupted();
        }
        yes(!Thread.currentThread().isInterrupted(), "interruption cleared");
        System.out.println("M3_BYTE_EVIDENCE_QUERY_PASS assertions=" + assertions
                + " kernel=" + (nativeReady ? "JAVA_JNI_KMP" : "JAVA_SOURCE_PROOF"));
    }
}
