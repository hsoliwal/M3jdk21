// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import com.m3.file.M3File;
import com.m3.file.M3FileDigestShadow;
import com.m3.file.M3FileProgress;
import com.m3.file.M3FileShadow;
import com.m3.file.M3FileTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * One offline discovery pass over original M3File entries, with no WatchService.
 *
 * <p>Full content SHA-256 is observed in one bounded input read per selected
 * regular file, through its original M3File source and optional caller shadows.
 * A project-local M3FS SQLite handle owns the checkpoint; this class neither
 * implements SQL nor stores another filesystem. ABI and dependency lanes in
 * the resulting M3BuildState remain explicitly UNKNOWN. The compiler's
 * independently owned attributed /m3-build/state is never overwritten.</p>
 */
public final class M3OfflineProjectScan {
    private static final String FORMAT = "M3_OFFLINE_DISCOVERY_V1";
    private static final String[] EXCLUDED_DIRS = {".git", ".m3"};

    private M3OfflineProjectScan() { }

    /** Independent limits on source count, checkpoint image, and complete file bytes. */
    public record Limits(int maxRows, int maxStateBytes, long maxFileBytes) {
        public Limits {
            if (maxRows < 0 || maxRows > 1_000_000 || maxStateBytes < 84
                    || maxFileBytes < 0) {
                throw new IllegalArgumentException("invalid offline scan budgets");
            }
        }
    }

    /** Extra consumers receive only bytes consumed by the original primary stream. */
    @FunctionalInterface
    public interface ShadowLayers {
        M3FileShadow[] forFile(M3File file) throws IOException;
    }

    /**
     * Candidate only. The publisher must explicitly commit after checking the
     * consuming build/tool policy; a completed scan is not a successful build.
     */
    public record Result(Path database, M3BuildState state, M3BuildState.Plan delta,
            byte[] expectedCommitment, String[] removedPaths, long observedBytes) {
        public Result {
            database = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(delta, "delta");
            expectedCommitment = expectedCommitment == null ? null : expectedCommitment.clone();
            removedPaths = Objects.requireNonNull(removedPaths, "removedPaths").clone();
            if (observedBytes < 0) throw new IllegalArgumentException("negative byte count");
        }
        @Override public byte[] expectedCommitment() {
            return expectedCommitment == null ? null : expectedCommitment.clone();
        }
        @Override public String[] removedPaths() { return removedPaths.clone(); }
        /** Directly modified/added current rows. Removed paths are separate. */
        public IntStream changedRows() {
            return IntStream.range(0, state.size()).filter(delta::changed);
        }
        /** Only source change discovery; absent ABI/edges cannot prove a safe task skip. */
        public boolean needsCompilerImpactAnalysis() {
            return delta.fullRebuild() || delta.affectedCount() != 0 || removedPaths.length != 0;
        }
    }

    /**
     * Convenient complete scan using the existing M3FileTree scanner and one
     * M3File traversal. Native SQLite is supplied by the caller, never recreated.
     */
    public static Result scan(Path project, M3FS database, byte[] toolContextSha256,
            String selectionId, Predicate<String> accept, ShadowLayers shadows,
            Limits limits, M3FileProgress progress) throws IOException {
        Path root = root(project);
        try (M3FileTree tree = M3FileTree.open(root);
                Stream<M3File> files = tree.walk().stream()) {
            return capture(root, database, files, toolContextSha256,
                    selectionId, accept, shadows, limits, progress);
        }
    }

    /**
     * Consumes and closes an externally supplied original M3File stream.
     * Selection is explicit and scoped by the required stable selectionId.
     * The .m3/.git control folders and SQLite's live database/WAL/lock
     * files are excluded so checkpoint writes never invalidate themselves.
     */
    public static Result capture(Path project, M3FS database, Stream<M3File> entries,
            byte[] toolContextSha256, String selectionId, Predicate<String> accept,
            ShadowLayers shadows, Limits limits, M3FileProgress progress) throws IOException {
        Path root = root(project);
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(accept, "accept");
        Objects.requireNonNull(shadows, "shadows");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(progress, "progress");
        if (selectionId == null || selectionId.isBlank() || selectionId.length() > 256) {
            throw new IllegalArgumentException("a stable nonblank selection identity is required");
        }
        if (toolContextSha256 == null || toolContextSha256.length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("32-byte compiler/tool context SHA-256 required");
        }
        byte[] context = context(root, selectionId, toolContextSha256);
        M3BuildStateStore.Version prior = M3BuildStateStore.readOffline(
                database, limits.maxStateBytes(), limits.maxRows()).orElse(null);
        Builder facts = new Builder(limits.maxRows());
        long observed = 0;
        progress.begin("M3 offline project discovery", 0L);
        try (Stream<M3File> selected = entries.sequential()) {
            Iterator<M3File> cursor = selected.iterator();
            while (cursor.hasNext()) {
                progress.checkCanceled();
                M3File file = Objects.requireNonNull(cursor.next(), "M3File");
                Path source = file.path().toAbsolutePath().normalize();
                if (!source.startsWith(root)) throw new IOException("file escaped project root: " + source);
                Path relative = root.relativize(source);
                if (relative.getNameCount() == 0 || relative.toString().isEmpty()) continue;
                String key = relative.toString().replace('\\', '/');
                if (excluded(key, source, database.database()) || !accept.test(key)) continue;
                M3File.Metadata before = file.metadata();
                if (before.directory()) continue;
                if (before.symbolicLink() || !before.regularFile()) {
                    throw new IOException("offline build source is not a regular file: " + key);
                }
                if (before.size() > limits.maxFileBytes()) {
                    throw new IOException("offline source exceeds byte budget: " + key);
                }
                M3FileShadow[] extra = Objects.requireNonNull(shadows.forFile(file), "shadows");
                M3FileDigestShadow sha = M3FileShadow.sha256();
                M3FileShadow[] all = new M3FileShadow[extra.length + 1];
                all[0] = sha;
                for (int i = 0; i < extra.length; i++) {
                    all[i + 1] = Objects.requireNonNull(extra[i], "extra shadow");
                }
                long count = file.observe(limits.maxFileBytes(),
                        progress.split(before.size()), all);
                M3File.Metadata after = file.metadata();
                // The provider's attributes are a stability check, never a
                // substitute for observing full content from zero through EOF.
                boolean sizeKnown = !file.isTreeEntry() || file.size() >= 0;
                if (!after.regularFile() || after.symbolicLink()
                        || (sizeKnown && (before.size() != count || after.size() != count))
                        || !before.lastModified().equals(after.lastModified())) {
                    throw new IOException("file changed during complete observation: " + key);
                }
                facts.add(key, role(key), count, sha.bytes());
                try { observed = Math.addExact(observed, count); }
                catch (ArithmeticException overflow) {
                    throw new IOException("project observation exceeds long byte count", overflow);
                }
            }
        }
        M3BuildState state = facts.seal(context);
        M3BuildState previous = prior == null ? null : prior.state();
        M3BuildState.Plan delta = state.changedFrom(previous);
        String[] removed = removed(state, previous);
        progress.done();
        return new Result(database.database(), state, delta,
                prior == null ? null : prior.commitment(), removed, observed);
    }

    /** Explicit optimistic commit to the same project's existing native SQLite DB. */
    public static M3BuildStateStore.Version publish(M3FS database, Result candidate, Limits limits)
            throws IOException {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(limits, "limits");
        if (!database.database().toAbsolutePath().normalize().equals(candidate.database())) {
            throw new IOException("project database changed since offline scan");
        }
        return M3BuildStateStore.publishOffline(database, candidate.state(),
                candidate.expectedCommitment(), limits.maxStateBytes(), limits.maxRows());
    }

    private static Path root(Path project) throws IOException {
        Path root = Objects.requireNonNull(project, "project").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("offline project root must be an existing directory");
        }
        return root;
    }

    private static boolean excluded(String key, Path file, Path database) {
        for (String dir : EXCLUDED_DIRS) {
            if (key.equals(dir) || key.startsWith(dir + "/")) return true;
        }
        Path db = database.toAbsolutePath().normalize();
        if (file.equals(db)) return true;
        Path folder = file.getParent();
        if (folder == null || !folder.equals(db.getParent())) return false;
        String name = file.getFileName().toString();
        String dbName = db.getFileName().toString();
        return name.equals(dbName + "-wal") || name.equals(dbName + "-shm")
                || name.equals(dbName + "-journal") || name.equals(dbName + ".m3build.lock");
    }

    private static byte role(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.endsWith(".java") || key.endsWith(".kt") || key.endsWith(".scala")
                || key.endsWith(".groovy")) return M3BuildState.SOURCE;
        if (key.endsWith(".class")) return M3BuildState.CLASS;
        if (key.endsWith(".jar") || key.endsWith(".jmod")) return M3BuildState.DEPENDENCY;
        if (key.equals("pom.xml") || key.endsWith(".gradle") || key.endsWith(".gradle.kts")
                || key.endsWith("gradle.properties") || key.endsWith("settings.xml")
                || key.endsWith("maven.config")) return M3BuildState.BUILD_INPUT;
        return M3BuildState.RESOURCE;
    }

    private static byte[] context(Path root, String selectionId, byte[] toolContext) {
        MessageDigest sha = digest();
        sha.update(FORMAT.getBytes(StandardCharsets.UTF_8));
        sha.update((byte) 0);
        sha.update(root.toUri().toASCIIString().getBytes(StandardCharsets.UTF_8));
        sha.update((byte) 0);
        sha.update(selectionId.getBytes(StandardCharsets.UTF_8));
        sha.update((byte) 0);
        sha.update(toolContext);
        return sha.digest();
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static String[] removed(M3BuildState current, M3BuildState previous) {
        if (previous == null) return new String[0];
        String[] buffer = new String[previous.size()];
        int n = 0;
        for (int i = 0; i < previous.size(); i++) {
            if (current.find(previous.path(i), previous.role(i)) < 0) {
                buffer[n++] = previous.path(i);
            }
        }
        return Arrays.copyOf(buffer, n);
    }

    /** One growing set of primitive/string lanes, sorted by a primitive index. */
    private static final class Builder {
        private final int limit;
        private String[] paths;
        private byte[] roles;
        private long[] lengths;
        private byte[] hashes;
        private int size;

        Builder(int limit) {
            this.limit = limit;
            int initial = Math.min(16, limit);
            paths = new String[initial]; roles = new byte[initial];
            lengths = new long[initial]; hashes = new byte[initial * M3BuildState.HASH_BYTES];
        }

        void add(String key, byte role, long length, byte[] hash) throws IOException {
            if (size == limit) throw new IOException("offline file count exceeds budget");
            if (size == paths.length) grow();
            if (hash.length != M3BuildState.HASH_BYTES) throw new IllegalArgumentException("SHA-256");
            paths[size] = key;
            roles[size] = role;
            lengths[size] = length;
            System.arraycopy(hash, 0, hashes, size * M3BuildState.HASH_BYTES, hash.length);
            size++;
        }

        private void grow() {
            int capacity = (int) Math.min(limit, Math.max((long) paths.length + 1,
                    (long) paths.length + (paths.length >>> 1)));
            paths = Arrays.copyOf(paths, capacity);
            roles = Arrays.copyOf(roles, capacity);
            lengths = Arrays.copyOf(lengths, capacity);
            hashes = Arrays.copyOf(hashes, Math.multiplyExact(capacity, M3BuildState.HASH_BYTES));
        }

        M3BuildState seal(byte[] context) throws IOException {
            int[] order = new int[size], scratch = new int[size];
            for (int i = 0; i < size; i++) order[i] = i;
            for (long width = 1; width < size; width *= 2) {
                for (long base = 0; base < size; base += 2 * width) {
                    int a = (int) base, mid = (int) Math.min(size, base + width);
                    int b = mid, end = (int) Math.min(size, base + 2 * width);
                    int out = a;
                    while (a < mid && b < end) {
                        scratch[out++] = paths[order[a]].compareTo(paths[order[b]]) <= 0
                                ? order[a++] : order[b++];
                    }
                    while (a < mid) scratch[out++] = order[a++];
                    while (b < end) scratch[out++] = order[b++];
                }
                int[] swap = order; order = scratch; scratch = swap;
            }
            String[] sorted = new String[size];
            byte[] kinds = new byte[size], contents = new byte[Math.multiplyExact(size, M3BuildState.HASH_BYTES)];
            long[] counts = new long[size];
            for (int row = 0; row < size; row++) {
                int source = order[row];
                if (row > 0 && sorted[row - 1].equals(paths[source])) {
                    throw new IOException("duplicate file identity in offline stream: " + paths[source]);
                }
                sorted[row] = paths[source]; kinds[row] = roles[source]; counts[row] = lengths[source];
                System.arraycopy(hashes, source * M3BuildState.HASH_BYTES,
                        contents, row * M3BuildState.HASH_BYTES, M3BuildState.HASH_BYTES);
            }
            return new M3BuildState(sorted, kinds, counts, contents,
                    new byte[contents.length], new long[(size + 63) >>> 6],
                    new int[size + 1], new int[0], context);
        }
    }
}
