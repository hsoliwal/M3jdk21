// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
// Modified 2026-10-21: separate observed and successful checkpoints using existing native atomic publication.

=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
import java.util.HexFormat;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
    private static final String OFFLINE_HEAD = DIRECTORY + "/offline-scan";
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        return readHead(fs, HEAD, maxBytes, maxRows, maxEdges);
    }

    private static Optional<Version> readHead(M3FS fs, String head,
=======
        return readAt(fs, HEAD, "state", maxBytes, maxRows, maxEdges);
    }

    /**
     * Offline file-discovery checkpoint in the SAME project M3FS SQLite database.
     * Compiler-attributed ABI/dependency state under /m3-build/state is untouched.
     */
    public static Optional<Version> readOffline(M3FS fs, int maxBytes, int maxRows)
            throws IOException {
        return readAt(fs, OFFLINE_HEAD, "offline-scan", maxBytes, maxRows, 0);
    }

    /**
     * Read the exact project's named offline input generation.
     * Each 32-byte context seal maps to a distinct key in the SAME SQLite DB:
     * Maven/Gradle and different selection policies cannot replace each other.
     * The unkeyed readOffline remains available for its original legacy clients.
     */
    public static Optional<Version> readOffline(M3FS fs, byte[] contextSha256,
            int maxBytes, int maxRows) throws IOException {
        String head = profileHead(contextSha256);
        String name = head.substring(DIRECTORY.length() + 1);
        Optional<Version> stored = readAt(fs, head, name, maxBytes, maxRows, 0);
        if (stored.isPresent()) {
            M3BuildState candidate = stored.orElseThrow().state();
            if (!Arrays.equals(contextSha256, candidate.contextSha256())
                    || candidate.dependencyCount() != 0) {
                throw new IOException("foreign or attributed offline project checkpoint");
            }
            for (int row = 0; row < candidate.size(); row++) {
                if (candidate.hasAbi(row)) {
                    throw new IOException("offline checkpoint cannot assert compiler ABI");
                }
            }
        }
        return stored;
    }

    /** A fixed-length, collision-resistant SQLite inode name from an exact caller context. */
    private static String profileHead(byte[] contextSha256) {
        if (Objects.requireNonNull(contextSha256, "contextSha256").length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("expected a 32-byte profile SHA-256");
        }
        return DIRECTORY + "/offline-" + HexFormat.of().formatHex(contextSha256);
    }

    private static Optional<Version> readAt(M3FS fs, String head, String name,
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(fs, "fs");
        if (maxBytes < 0 || maxRows < 0 || maxEdges < 0) throw new IllegalArgumentException("limits");
        if (!exists(fs, "/", "m3-build")) return Optional.empty();
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        if (!exists(fs, DIRECTORY, head.substring(DIRECTORY.length() + 1))) return Optional.empty();
=======
        if (!exists(fs, DIRECTORY, name)) return Optional.empty();
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        M3FS.Stat stat = fs.stat(head);
        if (stat.kind() != M3FS.Kind.FILE || stat.size() > maxBytes || stat.size() > Integer.MAX_VALUE) {
            throw new IOException("invalid build state kind or byte budget");
        }
        byte[] data = new byte[Math.toIntExact(stat.size())];
        int read = 0;
        while (read < data.length) {
            int amount = fs.read(head, read, data, read, Math.min(READ_BLOCK, data.length - read));
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
        return publishAt(fs, HEAD, "state", state, expectedCommitment,
                maxBytes, maxRows, maxEdges);
    }

    /** Content-only scan checkpoint; compare-and-swap in the existing native SQLite namespace. */
    public static Version publishOffline(M3FS fs, M3BuildState state,
            byte[] expectedCommitment, int maxBytes, int maxRows) throws IOException {
        if (state.dependencyCount() != 0) {
            throw new IllegalArgumentException("offline discovery must not assert compiler dependency edges");
        }
        for (int row = 0; row < state.size(); row++) {
            if (state.hasAbi(row)) {
                throw new IllegalArgumentException("offline discovery must not assert attributed ABI");
            }
        }
        return publishAt(fs, OFFLINE_HEAD, "offline-scan", state, expectedCommitment,
                maxBytes, maxRows, 0);
    }

    /**
     * Compare-and-swap one independent scan profile. Reuses original per-project
     * M3FS SQLite WAL, serialization, native rename and writer lock.
     */
    public static Version publishOffline(M3FS fs, byte[] contextSha256, M3BuildState state,
            byte[] expectedCommitment, int maxBytes, int maxRows) throws IOException {
        String head = profileHead(contextSha256);
        Objects.requireNonNull(state, "state");
        if (!Arrays.equals(contextSha256, state.contextSha256())) {
            throw new IllegalArgumentException("offline profile/source context mismatch");
        }
        if (state.dependencyCount() != 0) {
            throw new IllegalArgumentException("offline discovery must not assert compiler edges");
        }
        for (int row = 0; row < state.size(); row++) {
            if (state.hasAbi(row)) {
                throw new IllegalArgumentException("offline discovery must not assert compiler ABI");
            }
        }
        return publishAt(fs, head, head.substring(DIRECTORY.length() + 1),
                state, expectedCommitment, maxBytes, maxRows, 0);
    }

    private static Version publishAt(M3FS fs, String head, String name, M3BuildState state,
            byte[] expectedCommitment, int maxBytes, int maxRows, int maxEdges)
            throws IOException {
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        Objects.requireNonNull(fs, "fs");
        Objects.requireNonNull(state, "state");
        if (fs.isReadOnly() || fs.isSnapshot()) throw new IOException("build state publication requires writable M3FS");
        if (expectedCommitment != null && expectedCommitment.length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("expectedCommitment");
        }
        byte[] encoded = state.encode(maxBytes);
        // Ensure the candidate meets exactly the same decoder bounds as a future reader.
        M3BuildState.decode(encoded, maxBytes, maxRows, maxEdges);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        return locked(fs, () -> {
            Optional<Version> previous = read(fs, maxBytes, maxRows, maxEdges);
            byte[] actual = previous.map(Version::commitment).orElse(null);
            if (!Arrays.equals(actual, expectedCommitment)) throw new IOException("stale build-state commitment");
            return writeHead(fs, HEAD, state, encoded);
        });
    }

    /**
     * Reads the observation staged against one exact successful head. A pinned snapshot is required
     * so the two reads share a view. No observation is a successful-build authorization.
     */
    public static Optional<Version> readObserved(M3FS snapshot, byte[] expectedSuccessful,
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        return readObservedAt(snapshot, expectedSuccessful, null, maxBytes, maxRows, maxEdges);
    }

    /**
     * Reads one exact Maven/Gradle/IDE observation profile in the SAME M3FS database.
     * All profiles still share one globally authoritative successful-build head.
     * The existing unprofiled method and checkpoint path remain unchanged.
     */
    public static Optional<Version> readObserved(M3FS snapshot, byte[] expectedSuccessful,
            byte[] profileSha256, int maxBytes, int maxRows, int maxEdges) throws IOException {
        return readObservedAt(snapshot, expectedSuccessful,
                requiredProfile(profileSha256), maxBytes, maxRows, maxEdges);
    }

    private static Optional<Version> readObservedAt(M3FS snapshot, byte[] expectedSuccessful,
            byte[] profile, int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        byte[] expected = commitment(expectedSuccessful, true);
        if (!snapshot.isSnapshot()) throw new IOException("observed checkpoint read requires a pinned snapshot");
        requireCommitment(read(snapshot, maxBytes, maxRows, maxEdges), expected, "successful");
        Optional<Version> observed = readHead(snapshot,
                observedPath(expected, profile), maxBytes, maxRows, maxEdges);
        if (observed.isPresent()) {
            M3BuildState data = observed.orElseThrow().state();
            if (!data.completeInventory()
                    || profile != null && !Arrays.equals(profile, data.contextSha256())) {
                throw new IOException("observed checkpoint has a foreign context or incomplete inventory");
            }
        }
        return observed;
    }

    /**
     * Atomically stages a completed observation without changing /m3-build/state. Both the expected
     * successful head and previous observation are CAS-checked under the existing writer lock.
     * The observation is the existing state payload at a name bound to its successful predecessor;
     * no independent SQL schema or checkpoint envelope is introduced.
     */
    public static Version observe(M3FS fs, M3BuildState completed, byte[] expectedSuccessful,
            byte[] expectedObserved, int maxBytes, int maxRows, int maxEdges) throws IOException {
        return observeAt(fs, completed, expectedSuccessful, expectedObserved, null,
                maxBytes, maxRows, maxEdges);
    }

    /**
     * Stage one context-qualified completed inventory without publishing success.
     * Different profiles have distinct observations but use the same SQLite lock,
     * original CAS, native transaction and single successful build state.
     */
    public static Version observe(M3FS fs, M3BuildState completed, byte[] expectedSuccessful,
            byte[] expectedObserved, byte[] profileSha256,
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        return observeAt(fs, completed, expectedSuccessful, expectedObserved,
                requiredProfile(profileSha256), maxBytes, maxRows, maxEdges);
    }

    private static Version observeAt(M3FS fs, M3BuildState completed, byte[] expectedSuccessful,
            byte[] expectedObserved, byte[] profile,
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(completed, "completed");
        if (!completed.completeInventory()) throw new IOException("observation requires a completed inventory");
        if (profile != null && !Arrays.equals(profile, completed.contextSha256())) {
            throw new IllegalArgumentException("observed source does not belong to profile");
        }
        byte[] successful = commitment(expectedSuccessful, true);
        byte[] observed = commitment(expectedObserved, true);
        byte[] encoded = completed.encode(maxBytes);
        M3BuildState.decode(encoded, maxBytes, maxRows, maxEdges);
        return locked(fs, () -> {
            requireCommitment(read(fs, maxBytes, maxRows, maxEdges), successful, "successful");
            String target = observedPath(successful, profile);
            requireCommitment(readHead(fs, target, maxBytes, maxRows, maxEdges), observed, "observed");
            return writeHead(fs, target, completed, encoded);
        });
    }

    /**
     * Caller attests that execution for this exact observation succeeded. This method runs no compiler.
     * It checks the successful predecessor and observed image under the writer lock, then atomically
     * promotes the existing payload. Failed execution must leave this method uncalled. Stale base or
     * superseded observation fails. Content commitments are not nonces and do not exclude ABA edits;
     * callers own build-output serialization and input/execution identity outside this SQLite store.
     * The exact promoted observation is removed best-effort after commit; a crash or cleanup failure
     * may retain an inert orphan. Cleanup addresses only that exact promoted observation.
     */
    public static Version promoteObserved(M3FS fs, byte[] expectedSuccessful, byte[] expectedObserved,
            int maxBytes, int maxRows, int maxEdges) throws IOException {
        return promoteObservedAt(fs, expectedSuccessful, expectedObserved, null,
                maxBytes, maxRows, maxEdges);
    }

    /** Promote exactly one successful profile; other profiles remain observations, not successes. */
    public static Version promoteObserved(M3FS fs, byte[] expectedSuccessful, byte[] expectedObserved,
            byte[] profileSha256, int maxBytes, int maxRows, int maxEdges) throws IOException {
        return promoteObservedAt(fs, expectedSuccessful, expectedObserved,
                requiredProfile(profileSha256), maxBytes, maxRows, maxEdges);
    }

    private static Version promoteObservedAt(M3FS fs, byte[] expectedSuccessful, byte[] expectedObserved,
            byte[] profile, int maxBytes, int maxRows, int maxEdges) throws IOException {
        byte[] successful = commitment(expectedSuccessful, true);
        byte[] observed = commitment(expectedObserved, false);
        return locked(fs, () -> {
            requireCommitment(read(fs, maxBytes, maxRows, maxEdges), successful, "successful");
            String path = observedPath(successful, profile);
            Optional<Version> pending = readHead(fs, path, maxBytes, maxRows, maxEdges);
            requireCommitment(pending, observed, "observed");
            M3BuildState candidate = pending.orElseThrow(() -> new IOException("missing observation")).state();
            if (!candidate.completeInventory()
                    || profile != null && !Arrays.equals(profile, candidate.contextSha256())) {
                throw new IOException("observation has a foreign context or incomplete inventory");
            }
            Version committed = writeHead(fs, HEAD, candidate, candidate.encode(maxBytes));
            // The head is already committed. Cleanup must not report a failed promotion.
            try { fs.unlink(path); }
            catch (IOException | RuntimeException cleanup) { /* restart may reclaim this exact orphan */ }
            return committed;
        });
    }

    private static byte[] commitment(byte[] value, boolean nullable) {
        if (value == null) {
            if (!nullable) throw new IllegalArgumentException("commitment is required");
            return null;
        }
        if (value.length != M3BuildState.HASH_BYTES) throw new IllegalArgumentException("commitment");
        return value.clone();
    }

    private static String observedPath(byte[] successful) {
        return DIRECTORY + "/observed-" + (successful == null ? "initial" : java.util.HexFormat.of().formatHex(successful));
    }

    /** Profile bytes only name a native SQLite inode; no new storage or pointer owner. */
    private static String observedPath(byte[] successful, byte[] profile) {
        String path = observedPath(successful);
        return profile == null ? path : path + "-" + java.util.HexFormat.of().formatHex(profile);
    }

    private static byte[] requiredProfile(byte[] profile) {
        if (Objects.requireNonNull(profile, "profile").length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("profile must be exactly 32 bytes");
        }
        return profile.clone();
    }

    private static void requireCommitment(Optional<Version> value, byte[] expected, String kind) throws IOException {
        if (!Arrays.equals(value.map(Version::commitment).orElse(null), expected)) {
            throw new IOException("stale " + kind + " build-state commitment");
        }
    }

    @FunctionalInterface
    private interface LockedUpdate { Version run() throws IOException; }

    private static Version locked(M3FS fs, LockedUpdate update) throws IOException {
        Objects.requireNonNull(fs, "fs");
        if (fs.isReadOnly() || fs.isSnapshot()) throw new IOException("build state publication requires writable M3FS");
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        Path database = fs.database();
        Path lockFile = database.resolveSibling(database.getFileName() + ".m3build.lock");
        try (FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException busy) { throw new IOException("build-state writer already active", busy); }
            if (lock == null) throw new IOException("build-state writer already active");
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            try (lock) { return update.run(); }
        }
    }

    private static Version writeHead(M3FS fs, String head, M3BuildState state, byte[] encoded) throws IOException {
        if (!exists(fs, "/", "m3-build")) fs.mkdir(DIRECTORY, DIRECTORY_MODE);
        String temporary = DIRECTORY + "/.state-" + UUID.randomUUID();
        fs.createFile(temporary, FILE_MODE);
        boolean published = false;
        try {
            long size = fs.write(temporary, 0L, encoded);
            if (size != encoded.length) throw new IOException("incomplete staged build state");
            fs.rename(temporary, head); // one native SQLite transaction
            published = true;
        } finally {
            if (!published) {
                try { fs.unlink(temporary); } catch (IOException cleanup) {
                    // The prior head is unchanged; an orphan may be cleaned by its owner later.
                }
            }
        }
        return new Version(state, digest(encoded));
=======
            try (lock) {
                Optional<Version> previous = readAt(fs, head, name, maxBytes, maxRows, maxEdges);
                byte[] actual = previous.map(Version::commitment).orElse(null);
                if (!Arrays.equals(actual, expectedCommitment)) {
                    throw new IOException("stale build-state commitment");
                }
                if (!exists(fs, "/", "m3-build")) fs.mkdir(DIRECTORY, DIRECTORY_MODE);
                String temporary = DIRECTORY + "/." + name + "-" + UUID.randomUUID();
                fs.createFile(temporary, FILE_MODE);
                boolean published = false;
                try {
                    long size = fs.write(temporary, 0L, encoded);
                    if (size != encoded.length) throw new IOException("incomplete staged build state");
                    fs.rename(temporary, head); // one original native SQLite transaction
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
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
