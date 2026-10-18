/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch;

import com.m3.runtime.capability.SourceArtifact;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

/** Actual SourceArtifact-backed cursor checks; no substitute payload owner or model. */
public final class M3SourceInputStreamChecks {
    private static int checks;

    private M3SourceInputStreamChecks() { }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }

    private static void expect(Class<? extends Throwable> type, Action action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void unsignedAndIndependentCursors() throws Exception {
        byte[] original = new byte[256];
        for (int index = 0; index < original.length; index++) original[index] = (byte) index;
        SourceArtifact owner = SourceArtifact.copyOf("all-byte-values", original);
        String root = owner.root();
        Arrays.fill(original, (byte) 0);
        var left = M3SourceInputStream.open(owner.whole());
        try (var right = M3SourceInputStream.open(owner.span(128, 128))) {
            check(left.markSupported(), "mark supported");
            check(right.read() == 128, "span uses its absolute source offset");
            for (int index = 0; index < 256; index++) {
                check(left.read() == index, "unsigned source byte " + index);
            }
            check(left.read() == -1, "whole-source EOF");
            check(right.read() == 129, "independent cursors");
            left.close();
            check(right.read() == 130, "closing another cursor leaves this one open");
            check(owner.whole().byteAt(255) == (byte) 255, "owner remains unchanged");
        } finally {
            left.close();
        }
        check(root.equals(owner.root()), "source identity remains unchanged");
    }

    private static void everySmallRangeAgainstJdkCursor() throws Exception {
        byte[] bytes = {0, 1, 127, (byte) 128, (byte) 255, 10, 13, 0,
                (byte) 0xc3, (byte) 0xa9, (byte) 0xff, 32, 65, 66, 67};
        SourceArtifact owner = SourceArtifact.copyOf("binary-ranges", bytes);
        for (int start = 0; start <= bytes.length; start++) {
            for (int length = 0; length <= bytes.length - start; length++) {
                SourceArtifact.Span span = owner.whole().slice(start, length);
                try (var actual = M3SourceInputStream.open(span);
                        var expected = new ByteArrayInputStream(bytes, start, length)) {
                    check(actual.available() == expected.available(), "initial remaining");
                    check(actual.skip(-1) == expected.skip(-1), "negative skip");
                    check(actual.read() == expected.read(), "single read");
                    actual.reset();
                    expected.reset();
                    check(actual.available() == length, "initial mark is range start");
                    check(actual.skip(2) == expected.skip(2), "bounded prefix skip");
                    actual.mark(-7);
                    expected.mark(-7);
                    byte[] got = new byte[11];
                    byte[] wanted = new byte[11];
                    Arrays.fill(got, (byte) 79);
                    Arrays.fill(wanted, (byte) 79);
                    check(actual.read(got, 3, 5) == expected.read(wanted, 3, 5), "bulk count");
                    check(Arrays.equals(got, wanted), "bulk output and outside-range sentinels");
                    actual.reset();
                    expected.reset();
                    check(actual.available() == expected.available(), "mark/reset remaining");
                    check(actual.skip(Long.MAX_VALUE) == expected.skip(Long.MAX_VALUE), "long skip");
                    check(actual.available() == 0, "skip stops at selected span end");
                    check(actual.read(got, 0, 0) == 0, "zero-length read at EOF");
                    check(actual.read(got, 0, 1) == -1, "bulk EOF");
                    actual.reset();
                    expected.reset();
                    check(Arrays.equals(actual.readAllBytes(), expected.readAllBytes()), "full tail");
                    actual.reset();
                    expected.reset();
                    var output = new ByteArrayOutputStream();
                    byte[] tail = expected.readAllBytes();
                    check(actual.transferTo(output) == tail.length, "transfer length");
                    check(Arrays.equals(output.toByteArray(), tail), "transfer bytes");
                }
            }
        }
    }

    private static void refusalAndClose() throws Exception {
        SourceArtifact owner = SourceArtifact.copyOf("refusals", new byte[] {1, 2, 3});
        expect(NullPointerException.class, () -> M3SourceInputStream.open(null));
        var stream = M3SourceInputStream.open(owner.whole());
        try {
            expect(NullPointerException.class, () -> stream.read(null, 0, 0));
            expect(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], -1, 1));
            expect(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], 1, 2));
            expect(IndexOutOfBoundsException.class,
                    () -> stream.read(new byte[2], Integer.MAX_VALUE, 1));
            check(stream.available() == 3, "invalid geometry leaves cursor unchanged");
            check(Arrays.equals(stream.readNBytes(2), new byte[] {1, 2}), "readNBytes");
            stream.mark(0);
            expect(EOFException.class, () -> stream.skipNBytes(2));
            stream.reset();
            check(stream.read() == 3, "mark remains valid beyond advisory limit");
            stream.close();
            stream.close();
            expect(IOException.class, stream::read);
            expect(IOException.class, () -> stream.read(new byte[0], 0, 0));
            expect(IOException.class, () -> stream.skip(0));
            expect(IOException.class, stream::available);
            expect(IOException.class, stream::reset);
            expect(IllegalStateException.class, () -> stream.mark(0));
            check(stream.markSupported(), "capability query remains valid after close");
        } finally {
            stream.close();
        }
        try (var fresh = M3SourceInputStream.open(owner.whole())) {
            check(Arrays.equals(fresh.readAllBytes(), new byte[] {1, 2, 3}), "owner can reopen");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments expected");
        unsignedAndIndependentCursors();
        everySmallRangeAgainstJdkCursor();
        refusalAndClose();
        System.out.println("M3_SOURCE_INPUT_STREAM_PASS checks=" + checks);
    }
}
