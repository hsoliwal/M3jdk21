// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import com.m3.file.M3File;
import com.m3.file.M3StreamProgress;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Completed host-project input generation from ONE caller-owned stream of original
 * M3File edges. It does not walk directories, retain another file system, parse
 * Java, or implement SQLite. The caller owns the completeness of its supplied
 * enumeration and the stable identity of its selection predicate.
 *
 * <p>All selected regular files are read once through M3File.observe; the same
 * exact consumed bytes feed SHA-256, progress and a borrowed parser/shadow
 * callback. A completed M3BuildState V2 inventory is emitted only after the
 * stream finishes without error. No source hash proves ABI, a successful build,
 * atomic mutable filesystem revision or safe plugin task reuse.</p>
 */
public final class M3HostFileInventory {
    private static final byte[] SCOPE_VERSION =
            "M3JDK21_HOST_INPUT_STREAM_V1".getBytes(StandardCharsets.US_ASCII);
    private static final String[] EXCLUDED_PREFIXES = {".git", ".m3"};

    private M3HostFileInventory() { }

    public record Limits(int maxRows, int maxStateBytes,
            long maxFileBytes, long maxTotalBytes) {
        public Limits {
            if (maxRows < 0 || maxRows > 1_000_000 || maxStateBytes < 116
                    || maxFileBytes < 0 || maxTotalBytes < 0) {
                throw new IllegalArgumentException("invalid host-inventory budgets");
            }
        }
    }

    /** The buffer belongs to M3File and is read-only until this callback returns. */
    @FunctionalInterface
    public interface ByteLayer {
        void onBytes(M3File source, byte[] bytes, int offset, int count) throws IOException;

        /** Completion occurs only after the entire file and checked metadata agree. */
        default void completed(M3File source, M3File.Observation observation) throws IOException { }
    }

    public record Result(M3BuildState inventory,
            M3BuildState.InventoryDelta delta, String[] removedPaths,
            long observedBytes) {
        public Result {
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(delta, "delta");
            removedPaths = Objects.requireNonNull(removedPaths, "removedPaths").clone();
            if (!inventory.completeInventory() || observedBytes < 0) {
                throw new IllegalArgumentException("incomplete host inventory");
            }
        }
        @Override public String[] removedPaths() { return removedPaths.clone(); }
        public IntStream addedRows() {
            return IntStream.range(0, inventory.size()).filter(delta::added);
        }
        public IntStream modifiedRows() {
            return IntStream.range(0, inventory.size()).filter(delta::modified);
        }
        public boolean needsImpactAnalysis() { return delta.needsPlanning(); }
    }

    /**
     * Consume the supplied complete original M3File entry stream exactly once.
     * The caller may create that stream through its existing file traversal,
     * Maven source roots or the original provider; this method never rescans.
     *
     * @param previous previous completed V2 generation for exactly this scope
     * @param selectionId stable identity of include/exclude rules, not source code
     */
    public static Result capture(Path project, Stream<M3File> entries,
            String selectionId, Predicate<String> include,
            byte[] toolContextSha256, M3BuildState previous,
            Limits limits, M3StreamProgress progress, ByteLayer layer) throws IOException {
        Path root = Objects.requireNonNull(project, "project").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new IOException("host project root must be a non-symlink directory");
        }
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(include, "include");
        Objects.requireNonNull(limits, "limits");
        M3StreamProgress.require(progress);
        Objects.requireNonNull(layer, "layer");
        if (selectionId == null || selectionId.isBlank()
                || selectionId.length() > 256 || selectionId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("stable selection ID required");
        }
        if (toolContextSha256 == null || toolContextSha256.length != M3BuildState.HASH_BYTES) {
            throw new IllegalArgumentException("exact 32-byte tool context required");
        }
        byte[] scope = scope(root, selectionId);
        MessageDigest combined = sha256();
        combined.update(scope);
        combined.update(toolContextSha256);
        byte[] context = combined.digest();
        Rows rows = new Rows(limits);
        long total = 0;
        try (Stream<M3File> walk = entries.sequential()) {
            Iterator<M3File> cursor = walk.iterator();
            while (cursor.hasNext()) {
                progress.checkCancelled();
                M3File entry = Objects.requireNonNull(cursor.next(), "M3File");
                Path path = entry.path().toAbsolutePath().normalize();
                if (!path.startsWith(root)) throw new IOException("input outside project: " + path);
                Path relative = root.relativize(path);
                if (relative.toString().isEmpty()) continue;
                String key = relative.toString();
                // A literal backslash on Unix is not the Windows separator.
                if (!"\\".equals(root.getFileSystem().getSeparator()) && key.indexOf('\\') >= 0) {
                    throw new IOException("ambiguous source path: " + key);
                }
                key = key.replace('\\', '/');
                if (excluded(key) || !include.test(key)) continue;
                rejectLinks(root, relative);
                M3File.Metadata metadata = entry.metadata();
                if (metadata.directory()) continue;
                if (metadata.symbolicLink() || !metadata.regularFile()) {
                    throw new IOException("not a regular host input: " + key);
                }
                if (metadata.size() > limits.maxFileBytes()
                        || metadata.size() > limits.maxTotalBytes() - total) {
                    throw new IOException("host project input budget exceeded: " + key);
                }
                M3StreamProgress child = progress.subProgress(metadata.size());
                M3File.Observation observed = entry.observe(
                        Math.min(limits.maxFileBytes(), limits.maxTotalBytes() - total),
                        child, (bytes, from, length) -> layer.onBytes(entry, bytes, from, length));
                layer.completed(entry, observed);
                long count = observed.fingerprint().size();
                total = Math.addExact(total, count);
                long nanos = observed.lastModified().to(TimeUnit.NANOSECONDS);
                // A pre-epoch or unrepresentable time is unknown; raw SHA remains authoritative.
                rows.add(key, role(key), observed.fingerprint(), nanos < 0 ? -1 : nanos);
            }
        }
        M3BuildState state = rows.seal(context, scope);
        M3BuildState.InventoryDelta delta = state.deltaFrom(previous, false);
        return new Result(state, delta, removed(state, previous, delta), total);
    }

    /**
     * Stage, but never promote, this completed inventory in the ORIGINAL native
     * M3FS SQLite observation namespace. Compilation/plugin success is an
     * independent caller decision before M3BuildStateStore.promoteObserved.
     */
    public static M3BuildStateStore.Version stage(M3FS database, Result result,
            byte[] expectedSuccessful, byte[] expectedObserved,
            int maxStateBytes, int maxRows) throws IOException {
        Objects.requireNonNull(result, "result");
        return M3BuildStateStore.observe(
                Objects.requireNonNull(database, "database"), result.inventory(),
                expectedSuccessful, expectedObserved,
                result.inventory().contextSha256(), maxStateBytes, maxRows, 0);
    }

    private static void rejectLinks(Path root, Path relative) throws IOException {
        Path candidate = root;
        for (Path segment : relative) {
            candidate = candidate.resolve(segment);
            if (Files.isSymbolicLink(candidate)) {
                throw new IOException("symbolic link in source path: " + candidate);
            }
        }
    }

    private static boolean excluded(String key) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (key.equals(prefix) || key.startsWith(prefix + "/")) return true;
        }
        return false;
    }

    private static byte role(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java") || lower.endsWith(".kt")
                || lower.endsWith(".scala") || lower.endsWith(".groovy"))
            return M3BuildState.SOURCE;
        if (lower.endsWith(".class")) return M3BuildState.CLASS;
        if (lower.equals("pom.xml") || lower.endsWith("/pom.xml")
                || lower.endsWith(".gradle") || lower.endsWith(".gradle.kts")
                || lower.endsWith("gradle.properties") || lower.endsWith("maven.config"))
            return M3BuildState.BUILD_INPUT;
        if (lower.endsWith(".jar") || lower.endsWith(".jmod"))
            return M3BuildState.DEPENDENCY;
        return M3BuildState.RESOURCE;
    }

    private static byte[] scope(Path root, String selectionId) {
        MessageDigest digest = sha256();
        digest.update(SCOPE_VERSION);
        digest.update((byte) 0);
        digest.update(root.toUri().toASCIIString().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(selectionId.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static String[] removed(M3BuildState state, M3BuildState previous,
            M3BuildState.InventoryDelta delta) {
        if (!delta.comparable() || previous == null) return new String[0];
        String[] paths = new String[previous.size()];
        int used = 0;
        for (int row = 0; row < previous.size(); row++) {
            if (delta.removed(row)) paths[used++] = previous.path(row);
        }
        return Arrays.copyOf(paths, used);
    }

    /** One growable array owner, one primitive index order; no per-file retained objects. */
    private static final class Rows {
        private final Limits limits;
        private String[] names;
        private byte[] roles;
        private long[] lengths, times;
        private byte[] hashes;
        private long nameBytes;
        private int count;

        Rows(Limits limits) {
            this.limits = limits;
            int initial = Math.min(16, limits.maxRows());
            names = new String[initial];
            roles = new byte[initial];
            lengths = new long[initial];
            times = new long[initial];
            hashes = new byte[initial * M3BuildState.HASH_BYTES];
        }

        void add(String name, byte role, M3File.Fingerprint bytes, long nanoTime)
                throws IOException {
            if (count >= limits.maxRows()) throw new IOException("file row budget exceeded");
            long newNames = Math.addExact(nameBytes, name.getBytes(StandardCharsets.UTF_8).length);
            int n = count + 1;
            long words = (n + 63L) >>> 6;
            long encoded = 116L + 89L * n + 16L * words + newNames;
            if (encoded > limits.maxStateBytes()) throw new IOException("inventory image budget exceeded");
            if (count == names.length) grow();
            names[count] = name;
            roles[count] = role;
            lengths[count] = bytes.size();
            times[count] = nanoTime;
            int base = count * M3BuildState.HASH_BYTES;
            putLong(base, bytes.digest0()); putLong(base + 8, bytes.digest1());
            putLong(base + 16, bytes.digest2()); putLong(base + 24, bytes.digest3());
            count++;
            nameBytes = newNames;
        }

        private void putLong(int at, long value) {
            for (int i = 7; i >= 0; i--) {
                hashes[at + i] = (byte) value;
                value >>>= 8;
            }
        }

        private void grow() {
            int cap = (int) Math.min(limits.maxRows(),
                    Math.max((long) names.length + 1, (long) names.length * 2));
            names = Arrays.copyOf(names, cap);
            roles = Arrays.copyOf(roles, cap);
            lengths = Arrays.copyOf(lengths, cap);
            times = Arrays.copyOf(times, cap);
            hashes = Arrays.copyOf(hashes, Math.multiplyExact(cap, M3BuildState.HASH_BYTES));
        }

        M3BuildState seal(byte[] context, byte[] scope) throws IOException {
            int[] order = new int[count], scratch = new int[count];
            for (int i = 0; i < count; i++) order[i] = i;
            // Stable primitive-index mergesort, not boxed Integer nodes.
            for (long width = 1; width < count; width <<= 1) {
                for (long begin = 0; begin < count; begin += width * 2) {
                    int left = (int) begin, center = (int) Math.min(count, begin + width);
                    int right = center, end = (int) Math.min(count, begin + width * 2);
                    int write = left;
                    while (left < center && right < end) {
                        scratch[write++] = names[order[left]].compareTo(names[order[right]]) <= 0
                                ? order[left++] : order[right++];
                    }
                    while (left < center) scratch[write++] = order[left++];
                    while (right < end) scratch[write++] = order[right++];
                }
                int[] swap = order; order = scratch; scratch = swap;
            }
            String[] sorted = new String[count];
            byte[] sortedRoles = new byte[count];
            long[] sortedLengths = new long[count], sortedTimes = new long[count];
            long[] known = new long[(int) ((count + 63L) >>> 6)];
            byte[] sortedHashes = new byte[Math.multiplyExact(count, M3BuildState.HASH_BYTES)];
            for (int row = 0; row < count; row++) {
                int input = order[row];
                if (row != 0 && names[input].equals(sorted[row - 1])) {
                    throw new IOException("duplicate file in complete input stream: " + names[input]);
                }
                sorted[row] = names[input];
                sortedRoles[row] = roles[input];
                sortedLengths[row] = lengths[input];
                if (times[input] >= 0) {
                    sortedTimes[row] = times[input];
                    known[row >>> 6] |= 1L << (row & 63);
                }
                System.arraycopy(hashes, input * M3BuildState.HASH_BYTES,
                        sortedHashes, row * M3BuildState.HASH_BYTES, M3BuildState.HASH_BYTES);
            }
            return M3BuildState.completedHostRows(sorted, sortedRoles, sortedLengths,
                    sortedHashes, sortedTimes, known, context, scope);
        }
    }
}
