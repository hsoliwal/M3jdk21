// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Real Java21 module contract for NIO/provider-preserving range fingerprints. */
public final class M3FileRangeContract {
    private static int checks;
    private M3FileRangeContract() { }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void check(boolean success) {
        checks++;
        if (!success) throw new AssertionError("M3 file-range check " + checks);
    }
    private static void expect(Class<? extends Throwable> kind, Checked action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + failure, failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }
    private static M3File.Fingerprint reference(byte[] bytes, int first, int length)
            throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                Arrays.copyOfRange(bytes, first, first + length));
        ByteBuffer words = ByteBuffer.wrap(hash);
        return new M3File.Fingerprint(length, words.getLong(), words.getLong(),
                words.getLong(), words.getLong());
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-range-");
        try {
            Path target = root.resolve("input.bin");
            Random random = new Random(0x4D334652L);
            for (int trial = 0; trial < 360; trial++) {
                int n = trial < 9
                        ? new int[]{0, 1, 2, 63, 64, 65, 127, 128, 8192}[trial]
                        : random.nextInt(16000);
                byte[] bytes = new byte[n];
                random.nextBytes(bytes);
                Files.write(target, bytes);
                int count = random.nextInt(18);
                long[] offsets = new long[count], lengths = new long[count];
                int position = 0;
                for (int index = 0; index < count; index++) {
                    position += random.nextInt(n - position + 1);
                    int length = random.nextInt(n - position + 1);
                    offsets[index] = position;
                    lengths[index] = length;
                    position += length;
                }
                long[] original = offsets.clone();
                AtomicLong work = new AtomicLong();
                M3File.RangeFingerprints observed = M3File.of(target).fingerprintRanges(
                        offsets, lengths, n, work::addAndGet);
                check(observed.size() == count);
                long sum = 0;
                for (int row = 0; row < count; row++) {
                    check(observed.offset(row) == original[row]);
                    check(observed.length(row) == lengths[row]);
                    check(reference(bytes, (int) original[row], (int) lengths[row])
                            .equals(observed.fingerprint(row)));
                    sum += lengths[row];
                }
                check(work.get() == sum);
                if (count != 0) {
                    offsets[0] = Long.MAX_VALUE;
                    lengths[0] = 1;
                    check(observed.offset(0) == original[0]);
                    check(observed.fingerprint(0).size() != Long.MAX_VALUE);
                }
            }
            Files.write(target, new byte[]{4, 5, 6, 7});
            M3File file = M3File.of(target);
            long[] mutableOffsets = {0, 2};
            long[] mutableLengths = {1, 2};
            AtomicBoolean reshaped = new AtomicBoolean();
            M3File.RangeFingerprints pinned = file.fingerprintRanges(
                    mutableOffsets, mutableLengths, 4, bytes -> {
                        if (reshaped.compareAndSet(false, true)) {
                            mutableOffsets[1] = Long.MAX_VALUE;
                            mutableLengths[1] = Long.MAX_VALUE;
                        }
                    });
            check(pinned.offset(1) == 2 && pinned.length(1) == 2);
            check(pinned.fingerprint(1).equals(reference(new byte[]{4, 5, 6, 7}, 2, 2)));
            expect(IllegalArgumentException.class, () ->
                    file.fingerprintRanges(new long[]{2, 1}, new long[]{0, 0}, 4));
            expect(IllegalArgumentException.class, () ->
                    file.fingerprintRanges(new long[]{0, 0}, new long[]{2}, 4));
            expect(IllegalArgumentException.class, () ->
                    file.fingerprintRanges(new long[]{Long.MAX_VALUE}, new long[]{1}, Long.MAX_VALUE));
            expect(EOFException.class, () ->
                    file.fingerprintRanges(new long[]{4}, new long[]{1}, 5));
            expect(NullPointerException.class, () ->
                    file.fingerprintRanges(null, new long[0], 5));
            expect(CancellationException.class, () ->
                    file.fingerprintRanges(new long[]{0}, new long[]{1}, 5,
                            new M3StreamProgress() {
                                @Override public void worked(long count) { }
                                @Override public boolean cancelled() { return true; }
                            }));
            AtomicBoolean firstWrite = new AtomicBoolean();
            AtomicBoolean mutationSucceeded = new AtomicBoolean();
            boolean returned = false;
            try {
                file.fingerprintRanges(new long[]{0}, new long[]{4}, 4, bytes -> {
                    if (firstWrite.compareAndSet(false, true)) {
                        try {
                            Files.setLastModifiedTime(target,
                                    FileTime.fromMillis(System.currentTimeMillis() + 90000));
                            mutationSucceeded.set(true);
                        } catch (IOException cannotSetTime) {
                            // Some NIO providers disallow changing an open file's timestamp.
                        }
                    }
                });
                returned = true;
            } catch (IOException observedChange) {
                if (!mutationSucceeded.get()) throw observedChange;
            }
            if (mutationSucceeded.get()) check(!returned);
            Path link = root.resolve("link.bin");
            boolean linkCreated = false;
            try {
                Files.createSymbolicLink(link, target.getFileName());
                linkCreated = true;
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException refused) {
                // Windows/embedded providers may not grant symbolic-link creation.
            }
            if (linkCreated) {
                expect(IOException.class, () -> M3File.of(link)
                        .fingerprintRanges(new long[]{0}, new long[]{0}, 0));
            }
            System.out.println("M3_FILE_RANGES_PASS checks=" + checks + " trials=360");
        } finally {
            try (var walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
