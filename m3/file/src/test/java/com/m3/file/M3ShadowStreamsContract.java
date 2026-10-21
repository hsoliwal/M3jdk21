// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

/** Strict Java21 no-JUnit regression for one-source layered shadow I/O. */
public final class M3ShadowStreamsContract {
    private static int checks;
    private M3ShadowStreamsContract() { }

    public static void main(String[] args) throws Exception {
        byte[] payload = new byte[24_723];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i * 13 + (i >>> 4));
        CountingInput original = new CountingInput(payload);
        AtomicLong sourceProgress = new AtomicLong();
        AtomicLong parserProgress = new AtomicLong();
        try (var main = M3ShadowStreams.input(original, payload.length, sourceProgress::addAndGet)) {
            try (InputStream early = main.shadow(parserProgress::addAndGet)) {
                byte[] head = main.readNBytes(19);
                equal(Arrays.copyOfRange(payload, 0, 19), head, "main head");
                check(main.consumedSourceBytes() > 19, "one shared demand page");
                byte[] replay = early.readNBytes(19);
                equal(head, replay, "shadow matches already consumed bytes");
                main.mark(100);
                byte[] middle = main.readNBytes(200);
                main.reset();
                equal(middle, main.readNBytes(200), "main replay");
                try (InputStream late = main.shadowAtCurrent(M3StreamProgress.none())) {
                    equal(Arrays.copyOfRange(payload, 219, 251), late.readNBytes(32),
                            "cursor at current primary offset");
                }
                byte[] shadowRemainder = early.readAllBytes();
                equal(Arrays.copyOfRange(payload, 19, payload.length), shadowRemainder,
                        "shadow drives producer on demand to EOF");
                byte[] remaining = main.readAllBytes();
                equal(Arrays.copyOfRange(payload, 219, payload.length), remaining,
                        "main independent offset while shadow reads ahead");
                check(main.read() == -1 && early.read() == -1, "one EOF for both cursors");
                check(sourceProgress.get() == payload.length, "physical input progress once");
                check(parserProgress.get() == payload.length, "parser progress per consumed byte");
                check(original.bytesRead == payload.length, "no duplicate physical source reads");
                check(main.retentionBudget() == payload.length, "exact explicit retention cap");
            }
            check(original.closeCount == 0, "closing shadow keeps original live");
        }
        check(original.closeCount == 1, "close original exactly once");

        AtomicLong quota = new AtomicLong();
        M3StreamProgress child = ((M3StreamProgress) quota::addAndGet).subProgress(7);
        child.worked(3);
        child.worked(15);
        child.worked(4);
        check(quota.get() == 7, "bounded nested progress default method");
        rejects(IllegalArgumentException.class, () -> child.worked(-1), "negative child work");
        rejects(IllegalArgumentException.class,
                () -> M3StreamProgress.none().subProgress(-1), "negative sub budget");
        M3StreamProgress cancelled = new M3StreamProgress() {
            @Override public void worked(long amount) { }
            @Override public boolean cancelled() { return true; }
        };
        rejects(CancellationException.class, () -> cancelled.subProgress(2).worked(1),
                "cancel propagates via default child");
        byte[] copy = new byte[53];
        try (InputStream read = M3ShadowStreams.monitoredInput(
                new ByteArrayInputStream(copy), amount -> quota.addAndGet(amount))) {
            check(read.read() == 0, "monitored single read");
            check(read.readNBytes(10).length == 10, "monitored bulk read");
            check(read.skip(42) == 42, "monitored skip");
            check(read.read() == -1, "monitored EOF");
        }
        check(quota.get() == 60, "progress counted only consumed bytes");

        try (var full = M3ShadowStreams.input(new ByteArrayInputStream(payload), 47, M3StreamProgress.none())) {
            try (InputStream shadow = full.shadow(M3StreamProgress.none())) {
                rejects(IOException.class, shadow::readAllBytes, "bounded shadow memory overrun refused");
                check(full.consumedSourceBytes() == 47, "cap enforced without excess retention");
            }
        }
        try (var empty = M3ShadowStreams.input(
                new ByteArrayInputStream(new byte[0]), 0, M3StreamProgress.none())) {
            check(empty.read() == -1, "zero-budget empty input EOF");
        }
        var owned = M3ShadowStreams.input(new ByteArrayInputStream(payload),
                payload.length, M3StreamProgress.none());
        InputStream detached = owned.shadow(M3StreamProgress.none());
        check(detached.readNBytes(3).length == 3, "pre-close read");
        owned.close();
        rejects(IOException.class, detached::read, "closed parent invalidates shadow");
        detached.close();
        rejects(IOException.class, detached::read, "closed shadow rejects read");

        ByteArrayOutputStream mainOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream shadowOutput = new ByteArrayOutputStream();
        AtomicLong outputProgress = new AtomicLong();
        try (OutputStream output = M3ShadowStreams.output(mainOutput, shadowOutput,
                outputProgress::addAndGet)) {
            output.write(42);
            output.write(payload, 100, 125);
            output.flush();
        }
        check(outputProgress.get() == 126, "outbound progress on confirmed mirror work");
        check(mainOutput.size() == 126, "primary output exact bytes");
        equal(mainOutput.toByteArray(), shadowOutput.toByteArray(), "output mirrored exactly");
        rejects(IllegalArgumentException.class,
                () -> M3ShadowStreams.output(mainOutput, mainOutput, M3StreamProgress.none()),
                "shadow cannot alias primary output");
        ByteArrayOutputStream ahead = new ByteArrayOutputStream();
        OutputStream failing = new OutputStream() {
            @Override public void write(int ignored) throws IOException { throw new IOException("shadow failure"); }
            @Override public void write(byte[] data, int off, int len) throws IOException {
                throw new IOException("shadow failure");
            }
        };
        AtomicLong onlyCommitted = new AtomicLong();
        try (OutputStream output = M3ShadowStreams.output(ahead, failing, onlyCommitted::addAndGet)) {
            rejects(IOException.class, () -> output.write(new byte[] {3, 4}),
                    "failing shadow propagates error");
            rejects(IOException.class, () -> output.write(2), "poisoned mirror rejects later writes");
        }
        check(onlyCommitted.get() == 0 && ahead.size() == 2,
                "partial primary output acknowledged but no false confirmed progress");

        // A real M3File would open its existing provider-owned channel; a shadow
        // cursor is a byte-offset view, not a second Path/FS or new parser.
        byte[] utf8 = "{\"α\":123,\"items\":[1,2,3]}".getBytes(StandardCharsets.UTF_8);
        try (var input = M3ShadowStreams.input(new ByteArrayInputStream(utf8),
                utf8.length, M3StreamProgress.none());
             InputStream parserA = input.shadow(M3StreamProgress.none());
             InputStream parserB = input.shadow(M3StreamProgress.none())) {
            equal(parserA.readAllBytes(), utf8, "JSON parser-A layered ingress");
            equal(parserB.readAllBytes(), utf8, "JSON parser-B reuses one page arena");
        }
        Path real = Files.createTempFile("m3-shadow-input-", ".json");
        try {
            Files.write(real, utf8);
            var m3file = M3File.of(real);
            check(m3file.path() == real, "original provider-owned Path retained");
            AtomicLong physical = new AtomicLong();
            try (var stream = m3file.openShadowInputStream(utf8.length, physical::addAndGet);
                 InputStream layerA = stream.shadow(M3StreamProgress.none());
                 InputStream layerB = stream.shadow(M3StreamProgress.none())) {
                equal(layerA.readAllBytes(), utf8, "real M3File first parser domain");
                equal(layerB.readAllBytes(), utf8, "real M3File second parser domain");
            }
            check(physical.get() == utf8.length, "real provider source read once");
            try (InputStream monitored = m3file.openInputStream(M3StreamProgress.none())) {
                equal(monitored.readAllBytes(), utf8, "original monitored file input");
            }
            Path link = real.resolveSibling(real.getFileName() + "-link");
            try {
                Files.createSymbolicLink(link, real);
                try {
                    rejects(IOException.class, () -> M3File.of(link).openShadowInputStream(
                            utf8.length, M3StreamProgress.none()), "M3File refuses symlink");
                } finally { Files.deleteIfExists(link); }
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException ignored) {
                // Symlink tests are host-permission gated; original NIO contract remains authoritative.
            }
        } finally { Files.deleteIfExists(real); }
        System.out.println("M3_SHADOW_STREAM_PASS checks=" + checks);
    }

    private static void equal(byte[] a, byte[] b, String what) {
        check(Arrays.equals(a, b), what);
    }
    private static void check(boolean yes, String what) {
        checks++;
        if (!yes) throw new AssertionError(what);
    }
    private static void rejects(Class<? extends Throwable> kind,
                                ThrowingRunnable action, String what) {
        checks++;
        try { action.run(); } catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError(what, problem);
        }
        throw new AssertionError(what + " not rejected");
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
    private static final class CountingInput extends ByteArrayInputStream {
        private int bytesRead;
        private int closeCount;
        CountingInput(byte[] contents) { super(contents); }
        @Override public synchronized int read() {
            int value = super.read();
            if (value != -1) bytesRead++;
            return value;
        }
        @Override public synchronized int read(byte[] out, int offset, int length) {
            int n = super.read(out, offset, length);
            if (n > 0) bytesRead += n;
            return n;
        }
        @Override public void close() throws IOException { closeCount++; super.close(); }
    }
}
