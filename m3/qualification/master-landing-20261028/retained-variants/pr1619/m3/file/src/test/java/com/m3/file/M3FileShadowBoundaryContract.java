// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/** Standalone Java21 oracle for bounded, consumption-driven shadows and exact progress. */
public final class M3FileShadowBoundaryContract {
    private static int checks;
    private M3FileShadowBoundaryContract() { }

    private static void check(boolean correct, String label) {
        checks++;
        if (!correct) throw new AssertionError(label);
    }

    private static void boundedRead() throws Exception {
        byte[] data = new byte[128];
        for (int index = 0; index < data.length; index++) data[index] = (byte) index;
        final int[] shadowed = {0}, finished = {0}, worked = {0};
        M3FileShadow shadow = new M3FileShadow() {
            @Override public void update(byte[] bytes, int start, int length) {
                shadowed[0] += length;
            }
            @Override public void finish() { finished[0]++; }
        };
        ByteArrayInputStream original = new ByteArrayInputStream(data);
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(original, 5,
                units -> worked[0] += (int) units, shadow)) {
            byte[] output = new byte[128];
            check(input.read(output) == 5, "first bulk read is bounded");
            check(Arrays.equals(Arrays.copyOf(output, 5), Arrays.copyOf(data, 5)),
                    "returned bytes are exact source prefix");
            check(input.count() == 5 && shadowed[0] == 5 && worked[0] == 5,
                    "monitor and shadows account for exact admitted bytes");
            check(input.read(output, 0, 0) == 0, "zero-length read stays zero");
            try {
                input.read(output);
                throw new AssertionError("oversized source incorrectly accepted");
            } catch (M3FileLimitException expected) {
                checks++;
            }
            check(original.available() == data.length - 6, "one-byte overflow probe only");
            try {
                input.read();
                throw new AssertionError("overflow refusal was forgotten");
            } catch (M3FileLimitException expected) {
                checks++;
            }
            check(finished[0] == 0, "incomplete stream must not finalize shadows");
        }
    }

    private static void exactAndEmpty() throws Exception {
        byte[] source = {1, 2, 3, 4, 5};
        final int[] finished = {0};
        M3FileShadow shadow = new M3FileShadow() {
            @Override public void update(byte[] b, int start, int count) { }
            @Override public void finish() { finished[0]++; }
        };
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                new ByteArrayInputStream(source), 5, M3FileProgress.NONE, shadow)) {
            check(input.read(new byte[512]) == 5, "exact-length source accepted");
            check(input.read() == -1 && input.ended(), "EOF proven at byte boundary");
            check(input.read() == -1 && finished[0] == 1, "finalization exactly once");
        }
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                new ByteArrayInputStream(new byte[0]), 0, M3FileProgress.NONE)) {
            check(input.read() == -1, "zero limit accepts empty source");
        }
    }

    private static void unusualProvider() throws Exception {
        byte[] source = {8, 9, 10};
        InputStream provider = new InputStream() {
            private int position;
            @Override public int read() {
                return position < source.length ? source[position++] & 255 : -1;
            }
            @Override public int read(byte[] bytes, int start, int count) {
                return 0;
            }
        };
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                provider, 3, M3FileProgress.NONE)) {
            check(Arrays.equals(input.readAllBytes(), source), "zero bulk read makes progress");
            check(input.ended(), "provider fallback reaches EOF");
        }
    }

    private static void exactProgress() {
        Random random = new Random(20261021);
        for (int sample = 0; sample < 10_000; sample++) {
            long total = 1 + (random.nextLong() & (Long.MAX_VALUE - 1));
            long work = Long.remainderUnsigned(random.nextLong() & Long.MAX_VALUE, total);
            long share = random.nextLong() & Long.MAX_VALUE;
            final long[] reported = {0};
            M3FileSubProgress child = new M3FileSubProgress(
                    units -> reported[0] += units, share);
            child.begin("large progress", total);
            child.worked(work);
            long expected = BigInteger.valueOf(work)
                    .multiply(BigInteger.valueOf(share))
                    .divide(BigInteger.valueOf(total)).longValueExact();
            check(reported[0] == expected, "exact 128/64 quotient at " + sample);
            child.done();
            check(reported[0] == share, "no lost/extra work at " + sample);
        }
    }

    public static void run() throws Exception {
        boundedRead();
        exactAndEmpty();
        unusualProvider();
        exactProgress();
    }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3_SHADOW_BOUNDARY_PASS checks=" + checks);
    }
}
