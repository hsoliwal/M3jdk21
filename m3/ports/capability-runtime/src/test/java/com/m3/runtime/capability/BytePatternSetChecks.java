// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Independent naive byte-containment oracle, including all 64 unsigned mask positions. */
public final class BytePatternSetChecks {
    private static long checks;
    private BytePatternSetChecks() { }

    private static void yes(boolean passes, String why) {
        checks++;
        if (!passes) throw new AssertionError(why);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("wrong exception for " + kind + ": " + problem, problem);
        }
        throw new AssertionError("expected exception " + kind);
    }
    private static long brute(byte[] all, int start, int length, List<byte[]> patterns) {
        long mask = 0;
        for (int p = 0; p < patterns.size(); p++) {
            byte[] needle = patterns.get(p);
            for (int at = 0; at + needle.length <= length; at++) {
                boolean same = true;
                for (int k = 0; k < needle.length; k++) {
                    if (all[start + at + k] != needle[k]) { same = false; break; }
                }
                if (same) { mask |= 1L << p; break; }
            }
        }
        return mask;
    }
    private static void sample(byte[] data, int offset, int length, List<byte[]> originals,
                               boolean nativeMode, String id) {
        byte[] input = data.clone();
        List<byte[]> frozen = originals.stream().map(byte[]::clone).toList();
        var set = BytePatternSet.compile(originals);
        var source = SourceArtifact.copyOf(id, data);
        var span = source.span(offset, length);
        Arrays.fill(data, (byte) 0x72);
        for (byte[] value : originals) Arrays.fill(value, (byte) 0xff);
        long expected = brute(input, offset, length, frozen);
        yes(set.patternCount() == originals.size(), "pattern count " + id);
        yes(set.stateCount() >= 1 && set.retainedPrimitiveBytes() <= BytePatternSet.MAX_METADATA_BYTES,
                "primitive metadata bounded " + id);
        yes(set.scan(span) == expected, "independent Java oracle " + id);
        yes(set.scan(span) == expected, "deterministic replay " + id);
        if (nativeMode) {
            yes(set.scanNative(span) == expected, "C11 JNI independent containment " + id);
            yes(set.scanNative(span) == set.scan(span), "JNI/Java parity " + id);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("usage: [native library SHA-256]");
        }
        boolean nativeMode = args.length == 2;
        if (nativeMode) LfCount.install(Path.of(args[0]), args[1]);

        sample("AAABABA\nABABA\r\né🚀".getBytes(StandardCharsets.UTF_8), 0,
                "AAABABA\nABABA\r\né🚀".getBytes(StandardCharsets.UTF_8).length,
                new ArrayList<>(List.of(new byte[]{'A'}, new byte[]{'A','B','A'},
                        new byte[]{'B','A'}, new byte[]{'\r','\n'},
                        new byte[]{(byte)0xc3,(byte)0xa9}, new byte[]{0,0})),
                nativeMode, "overlap-seam-unicode");
        sample(new byte[]{0, 10, 0, -1, 10, 0}, 1, 4,
                new ArrayList<>(List.of(new byte[]{10,0},new byte[]{-1,10},new byte[]{0,10},
                        new byte[]{0}, new byte[]{0})), nativeMode, "binary-subspan-duplicates");
        sample(new byte[0], 0, 0, List.of(), nativeMode, "zero-patterns-empty-payload");
        var many = new ArrayList<byte[]>();
        for (int i = 0; i < 64; i++) many.add(new byte[]{(byte)i});
        sample(new byte[]{63}, 0, 1, many, nativeMode, "highest-bit-mask");
        var overlapping = new ArrayList<byte[]>();
        for (int i = 1; i <= 24; i++) {
            byte[] item = new byte[i]; Arrays.fill(item, (byte)'a'); overlapping.add(item);
        }
        sample("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes(StandardCharsets.UTF_8), 0, 30,
                overlapping, nativeMode, "suffix-output-failure-chains");

        Random random = new Random(0x4D3341484F323032L);
        for (int iteration = 0; iteration < 2_000; iteration++) {
            byte[] data = new byte[random.nextInt(64)];
            for (int i = 0; i < data.length; i++) data[i] = (byte)(random.nextInt(10) < 2
                    ? '\n' : random.nextInt(7));
            int start = random.nextInt(data.length + 1);
            int length = random.nextInt(data.length - start + 1);
            int count = random.nextInt(17);
            var patterns = new ArrayList<byte[]>();
            for (int j = 0; j < count; j++) {
                byte[] candidate = new byte[1 + random.nextInt(9)];
                if (candidate.length <= length && random.nextBoolean()) {
                    int at = start + random.nextInt(length - candidate.length + 1);
                    System.arraycopy(data, at, candidate, 0, candidate.length);
                } else {
                    for (int k = 0; k < candidate.length; k++) {
                        candidate[k] = (byte) random.nextInt(9);
                    }
                }
                patterns.add(candidate);
            }
            sample(data, start, length, patterns, nativeMode, "random" + iteration);
        }
        rejects(NullPointerException.class, () -> BytePatternSet.compile(null));
        rejects(NullPointerException.class, () -> BytePatternSet.compile(Arrays.asList((byte[])null)));
        rejects(IllegalArgumentException.class, () -> BytePatternSet.compile(List.of(new byte[0])));
        rejects(IllegalArgumentException.class, () -> BytePatternSet.compile(List.of(new byte[4097])));
        var over = new ArrayList<byte[]>();
        for (int i = 0; i < 65; i++) over.add(new byte[]{1});
        rejects(IllegalArgumentException.class, () -> BytePatternSet.compile(over));
        var enormous = new ArrayList<byte[]>();
        for (int i = 0; i < 5; i++) enormous.add(new byte[4096]);
        rejects(IllegalArgumentException.class, () -> BytePatternSet.compile(enormous));
        SourceArtifact shortOwner = SourceArtifact.copyOf("short", new byte[]{1,2,3});
        BytePatternSet current = BytePatternSet.compile(List.of(new byte[]{1}));
        if (!nativeMode) rejects(IllegalStateException.class,
                () -> current.scanNative(shortOwner.whole()));
        Thread.currentThread().interrupt();
        try {
            rejects(CancellationException.class, () -> current.scan(shortOwner.whole()));
            rejects(CancellationException.class, () -> BytePatternSet.compile(List.of(new byte[]{1})));
            if (nativeMode) rejects(CancellationException.class,
                    () -> current.scanNative(shortOwner.whole()));
        } finally {
            Thread.interrupted();
        }
        yes(!Thread.currentThread().isInterrupted(), "interruption released");
        System.out.println("M3_BYTE_PATTERN_SET_PASS assertions=" + checks
                + " kernel=" + (nativeMode ? "JNI_SPARSE_AHO_C11" : "JAVA_SPARSE_AHO"));
    }
}
