// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

/** One-read materialization and SHA-256/metadata admission from the original M3File owner. */
public final class M3FileCapturedBytesContract {
    private static int checks;

    private M3FileCapturedBytesContract() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }

    private static void refuses(Class<? extends Throwable> expected, Throwing action, String why)
            throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) return;
            throw new AssertionError(why + ": unexpected " + problem, problem);
        }
        throw new AssertionError(why + ": missing " + expected.getName());
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }

    private static byte[] sha(M3File.Fingerprint fingerprint) {
        return ByteBuffer.allocate(32).putLong(fingerprint.digest0())
                .putLong(fingerprint.digest1()).putLong(fingerprint.digest2())
                .putLong(fingerprint.digest3()).array();
    }

    public static void main(String[] arguments) throws Exception {
        Path directory = Files.createTempDirectory("m3-file-captured-");
        try {
            Random random = new Random(20261022L);
            Path source = directory.resolve("Input.java");
            for (int i = 0; i < 330; i++) {
                int length = switch (i % 9) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> 8191;
                    case 3 -> 8192;
                    case 4 -> 8193;
                    case 5 -> 16384;
                    default -> random.nextInt(50000);
                };
                byte[] expected = new byte[length];
                random.nextBytes(expected);
                Files.write(source, expected);
                M3File file = M3File.of(source);
                AtomicLong progress = new AtomicLong();
                M3File.CapturedBytes captured = file.captureBytes(length, progress::addAndGet);
                check(captured.length() == length, "exact observed byte count");
                check(progress.get() == length, "single-stream progress");
                check(captured.observation().fingerprint().size() == length, "observation length");
                check(Arrays.equals(sha(captured.observation().fingerprint()),
                        MessageDigest.getInstance("SHA-256").digest(expected)), "original digest");
                ByteBuffer view = captured.readOnlyBuffer();
                check(view.isReadOnly() && !view.hasArray(), "private payload not exposed");
                check(view.remaining() == length, "view matches admitted bytes");
                byte[] read = new byte[length];
                view.get(read);
                check(Arrays.equals(read, expected), "whole raw source materialized once");
                check(captured.readOnlyBuffer().position() == 0, "new read-only cursor each time");
                byte[] copy = captured.copyBytes();
                check(Arrays.equals(copy, expected), "explicit caller copy");
                if (length != 0) {
                    copy[0] ^= (byte) 0xff;
                    check(captured.readOnlyBuffer().get(0) == expected[0],
                            "caller copy cannot modify stored hash identity");
                    refuses(ReadOnlyBufferException.class,
                            () -> captured.readOnlyBuffer().put(0, (byte) 0), "view immutable");
                    refuses(IOException.class, () -> file.captureBytes(length - 1),
                            "short admission limit fails instead of partial capture");
                }
            }

            M3File file = M3File.of(source);
            byte[] first = "first".getBytes(StandardCharsets.UTF_8);
            byte[] second = "other".getBytes(StandardCharsets.UTF_8);
            Files.write(source, first);
            FileTime stamp = Files.getLastModifiedTime(source);
            M3File.CapturedBytes previous = file.captureBytes(64);
            Files.write(source, second);
            Files.setLastModifiedTime(source, stamp);
            M3File.CapturedBytes current = file.captureBytes(64);
            check(M3File.contentDelta(previous.observation().fingerprint(),
                    current.observation().fingerprint()) == M3File.CONTENT_CHANGED,
                    "same-size same-mtime edit changes content fingerprint");
            check(Arrays.equals(previous.copyBytes(), first),
                    "old capture stays immutable across provider writes");
            check(Arrays.equals(current.copyBytes(), second), "new content admitted");
            refuses(IllegalArgumentException.class, () -> file.captureBytes(-1),
                    "negative bound validated before opening source");
            M3StreamProgress stopped = new M3StreamProgress() {
                @Override public void worked(long amount) { }
                @Override public boolean cancelled() { return true; }
            };
            refuses(CancellationException.class, () -> file.captureBytes(64, stopped),
                    "cancelled read cannot return complete bytes");
            Path linked = directory.resolve("symbolic");
            try {
                Files.createSymbolicLink(linked, source.getFileName());
                refuses(IOException.class, () -> M3File.of(linked).captureBytes(64),
                        "symbolic file must not be followed");
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                // Some providers deny creating symlinks; content proofs still execute.
            } finally {
                Files.deleteIfExists(linked);
            }
            System.out.println("M3_FILE_CAPTURE_PASS checks=" + checks);
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        }
    }
}
