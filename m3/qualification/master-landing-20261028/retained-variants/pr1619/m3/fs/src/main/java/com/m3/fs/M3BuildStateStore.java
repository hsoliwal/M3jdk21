// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable build-fact custody INSIDE the existing M3FS native SQLite namespace.
 * One parent-owned image is atomically renamed over its predecessor. Cooperating
 * writers use an adjacent OS file lock and verify the previous image commitment;
 * readers should use an M3FS.openSnapshot() handle for pinned multi-call reads.
 * Neither this class nor M3FS automatically suppresses build tool tasks.
 */
public final class M3BuildStateStore {
    private static final String DIRECTORY = "/m3-build";
    private static final String HEAD = DIRECTORY + "/state";
    private static final int FILE_MODE = 0600;
    private static final int DIRECTORY_MODE = 0700;
    private static final int READ_BLOCK = 64 * 1024;

    private M3BuildStateStore() { }

    public record Version(M3BuildState state, byte[] commitment) {
        public Version {
            state = Objects.requireNonNull(state, "state");
            commitment = Objects.requireNonNull(commitment, "commitment").clone();
            if (commitment.length != M3BuildState.HASH_BYTES) throw new IllegalArgumentException("commitment");
        }
        @Override public byte[] commitment() { return commitment.clone(); }
    }

    /** Empty if no build state has ever been published. Never repairs corrupt state silently. */
    public static Optional<Version> read(M3FS fs, int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(fs, "fs");
        if (maxBytes < 0 || maxRows < 0 || maxEdges < 0) throw new IllegalArgumentException("limits");
        if (!exists(fs, "/", "m3-build")) return Optional.empty();
        if (!exists(fs, DIRECTORY, "state")) return Optional.empty();
        M3FS.Stat stat = fs.stat(HEAD);
        if (stat.kind() != M3FS.Kind.FILE || stat.size() > maxBytes || stat.size() > Integer.MAX_VALUE) {
            throw new IOException("invalid build state kind or byte budget");
        }
        byte[] data = new byte[Math.toIntExact(stat.size())];
        int read = 0;
        while (read < data.length) {
            int amount = fs.read(HEAD, read, data, read, Math.min(READ_BLOCK, data.length - read));
            if (amount <= 0) throw new IOException("truncated build state");
            read += amount;
        }
        return Optional.of(new Version(M3BuildState.decode(data, maxBytes, maxRows, maxEdges), digest(data)));
    }

    /**
     * Replace the head only if its exact former SHA-256 commitment is still present.
     * Pass null only when publishing the first image; a stale expected commitment fails.
     * The lock is scoped to the owning M3FS database path, not to a Maven execution.
     */
    public static Version publish(M3FS fs, M3BuildState state, byte[] expectedCommitment,
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(fs, "fs");
        Objects.requireNonNull(state, "state");
        if (fs.isReadOnly() || fs.isSnapshot()) throw new IOException("build state publication requires writable M3FS");
        if (expectedCommitment != null && expectedCommitment.length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("expectedCommitment");
        }
        byte[] encoded = state.encode(maxBytes);
        // Ensure the candidate meets exactly the same decoder bounds as a future reader.
        M3BuildState.decode(encoded, maxBytes, maxRows, maxEdges);
        Path database = fs.database();
        Path lockFile = database.resolveSibling(database.getFileName() + ".m3build.lock");
        try (FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException busy) { throw new IOException("build-state writer already active", busy); }
            if (lock == null) throw new IOException("build-state writer already active");
            try (lock) {
                Optional<Version> previous = read(fs, maxBytes, maxRows, maxEdges);
                byte[] actual = previous.map(Version::commitment).orElse(null);
                if (!Arrays.equals(actual, expectedCommitment)) {
                    throw new IOException("stale build-state commitment");
                }
                if (!exists(fs, "/", "m3-build")) fs.mkdir(DIRECTORY, DIRECTORY_MODE);
                String temporary = DIRECTORY + "/.state-" + UUID.randomUUID();
                fs.createFile(temporary, FILE_MODE);
                boolean published = false;
                try {
                    long size = fs.write(temporary, 0L, encoded);
                    if (size != encoded.length) throw new IOException("incomplete staged build state");
                    fs.rename(temporary, HEAD); // one native SQLite transaction
                    published = true;
                } finally {
                    if (!published) {
                        try { fs.unlink(temporary); } catch (IOException cleanup) {
                            // A failed write must not change the old head; orphaned temp may be cleaned later.
                        }
                    }
                }
                return new Version(state, digest(encoded));
            }
        }
    }

    private static boolean exists(M3FS fs, String parent, String name) throws IOException {
        for (M3FS.DirEntry entry : fs.list(parent)) if (name.equals(entry.name())) return true;
        return false;
    }

    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
