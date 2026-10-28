// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** No extra storage owner: one borrowed byte[] across serial provider observations. */
public final class M3FileSharedScratchContract {
    private static int checks;
    private M3FileSharedScratchContract() { }
    private static void check(boolean expected, String why) {
        checks++;
        if (!expected) throw new AssertionError(why);
    }
    private static byte[] digest(M3File.Fingerprint f) {
        return ByteBuffer.allocate(32).putLong(f.digest0()).putLong(f.digest1())
                .putLong(f.digest2()).putLong(f.digest3()).array();
    }
    private static void rejected(byte[] scratch, int max) throws Exception {
        checks++;
        Path temporary = Files.createTempFile("m3-scratch-budget", ".bin");
        try {
            Files.write(temporary, new byte[] {1,2,3});
            try {
                M3File.of(temporary).observe(max, scratch, M3StreamProgress.none(),
                        (b,from,len) -> { });
                throw new AssertionError("invalid scratch/input budget accepted");
            } catch (IllegalArgumentException | IOException expected) {
                // No observation is published on invalid input or over-limit.
            }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static void main(String[] arguments) throws Exception {
        byte[] scratch = new byte[64 * 1024];
        Path directory = Files.createTempDirectory("m3-shared-file-");
        try {
            Random random = new Random(20261022);
            for (int fileId = 0; fileId < 256; fileId++) {
                Path path = directory.resolve("src-" + fileId + ".java");
                byte[] content = new byte[fileId * 521];
                random.nextBytes(content);
                Files.write(path, content);
                var source = M3File.of(path);
                AtomicLong progress = new AtomicLong();
                AtomicInteger chunks = new AtomicInteger();
                ByteArrayOutputStream received = new ByteArrayOutputStream();
                M3File.Observation old = source.observe(content.length);
                M3File.Observation next = source.observe(content.length, scratch, progress::addAndGet,
                        (bytes, offset, length) -> {
                            check(bytes == scratch, "borrow original buffer, no internal copy");
                            check(length > 0 && length <= scratch.length, "bounded source chunk");
                            chunks.incrementAndGet();
                            received.write(bytes, offset, length);
                        });
                check(old.fingerprint().equals(next.fingerprint()), "old hash contract");
                check(Arrays.equals(content, received.toByteArray()), "exact borrowed bytes");
                check(Arrays.equals(digest(next.fingerprint()),
                        MessageDigest.getInstance("SHA-256").digest(content)), "hash parity");
                check(progress.get() == content.length, "progress parity");
                check(content.length == 0 || chunks.get() == (content.length + scratch.length - 1) / scratch.length,
                        "expected chunk count");
            }
            rejected(new byte[0], 3);
            rejected(new byte[(1 << 20) + 1], 3);
            rejected(scratch, 2);
            System.out.println("M3_SHARED_SCRATCH_PASS checks=" + checks);
        } finally {
            try (var stream = Files.list(directory)) {
                for (Path path : stream.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        }
    }
}
