// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Real Java -> existing JNI -> SQLite composition; no substituted loader or storage model. */
public final class M3FsHotPathSelfTest {
    private static final int BLOCK = 65_536;
    private static final long SEED = 20260928L;
    private static int checks;

    private M3FsHotPathSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("mindexfs-hotpath-");
        try {
            exercise(directory.resolve("store.sqlite"));
            System.out.println("PASS hotpath-java checks=" + checks
                    + " seed=" + SEED + " mutationCases=120 real-loader-jni-sqlite=YES");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void exercise(Path database) throws Exception {
        Random random = new Random(SEED);
        byte[] original = new byte[16 * BLOCK];
        random.nextBytes(original);
        byte[] expected = original.clone();
        ByteBuffer input = ByteBuffer.allocateDirect(original.length + 32);
        ByteBuffer output = ByteBuffer.allocateDirect(original.length + 32);
        try (var fs = M3FS.create(database)) {
            fs.mkdir("/images", 0700);
            fs.createFile("/images/source", 0600);
            input.position(16).put(original).flip().position(16);
            ByteBuffer readonly = input.asReadOnlyBuffer();
            check(fs.write("/images/source", 0, readonly) == original.length);
            check(readonly.position() == readonly.limit() && input.position() == 16);
            // Mutating the caller's buffer after write must not mutate stored content or bindings.
            for (int i = 16; i < input.limit(); i++) input.put(i, (byte) 0x6b);
            check(Arrays.equals(fs.read("/images/source", 0, original.length), original));
            long unique = fs.stats().uniqueBlocks();
            fs.cloneFile("/images/source", "/images/clone");
            check(fs.stats().uniqueBlocks() == unique);
            check(fs.stat("/images/source").inode() != fs.stat("/images/clone").inode());
            try (var snapshot = M3FS.openSnapshot(database)) {
                for (int sample = 0; sample < 120; sample++) {
                    int length = sample % 3 == 0 ? BLOCK : 1 + random.nextInt(BLOCK + 17);
                    int at = sample % 3 == 0
                            ? random.nextInt(16) * BLOCK
                            : random.nextInt(expected.length - length + 1);
                    byte[] change = new byte[length];
                    random.nextBytes(change);
                    if ((sample & 1) == 0) {
                        input.clear().position(11).put(change).flip().position(11);
                        check(fs.write("/images/clone", at, input) == expected.length);
                        check(input.position() == 11 + length);
                    } else {
                        check(fs.write("/images/clone", at, change) == expected.length);
                    }
                    System.arraycopy(change, 0, expected, at, length);
                    int start = random.nextInt(expected.length + 1);
                    int capacity = random.nextInt(BLOCK * 2);
                    int wanted = Math.min(capacity, expected.length - start);
                    output.clear();
                    output.put(15, (byte) 0x55).put(16 + wanted, (byte) 0x66);
                    output.position(16).limit(16 + capacity);
                    int got = fs.read("/images/clone", start, output);
                    check(got == wanted && output.position() == 16 + wanted);
                    output.clear();
                    check(output.get(15) == 0x55 && output.get(16 + got) == 0x66);
                    for (int i = 0; i < got; i++) check(output.get(16 + i) == expected[start + i]);
                    check(Arrays.equals(snapshot.read("/images/clone", start, capacity),
                            Arrays.copyOfRange(original, start, start + wanted)));
                }
                check(Arrays.equals(fs.read("/images/clone", 0, expected.length), expected));
                fs.unlink("/images/source");
                check(Arrays.equals(snapshot.read("/images/source", 0, original.length), original));
                fs.truncate("/images/clone", BLOCK + 7);
                fs.truncate("/images/clone", expected.length);
                Arrays.fill(expected, BLOCK + 7, expected.length, (byte) 0);
                check(Arrays.equals(fs.read("/images/clone", 0, expected.length), expected));
                check(Arrays.equals(snapshot.read("/images/clone", 0, original.length), original));
                var page = snapshot.listPage("/images", null, 1);
                check(page.hasMore() && page.entries().getFirst().name().equals("clone"));
                fs.rename("/images/clone", "/images/moved");
                var next = snapshot.listPage("/images", page.nextAfter(), 1);
                check(!next.hasMore() && next.entries().getFirst().name().equals("source"));
                snapshot.verify();
            }
            fs.verify();
            fs.checkpoint();
        }
        try (var reopened = M3FS.openReadOnly(database)) {
            check(Arrays.equals(reopened.read("/images/moved", 0, expected.length), expected));
            reopened.verify();
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check=" + checks);
    }
}
