// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Content-exact, source/class-aware change evidence for Maven, Gradle and javac consumers.
 *
 * <p>The retained index consists of parallel paths, role bytes, sizes, and packed SHA-256
 * lanes; no per-file entry objects survive snapshot construction. The caller supplies the
 * complete file inventory and a context digest covering the build tool, compiler options,
 * annotation processors, dependencies and applicable configuration. A class ABI digest is
 * optional, caller-produced compiler evidence, not a class-file parser or a skip decision.
 *
 * <p>This is a <em>read-only</em> comparison primitive. It does not schedule recompilation,
 * certify API/ABI equivalence, decide test invalidation, modify files, or replace Maven/Gradle
 * configuration/build caches. JDK/tool owners must validate their own full task inputs and
 * output receipts before reusing any work. All file captures use the existing M3File NIO
 * boundary, verify bytes rather than timestamps, and refuse link-containing paths.
 * A simultaneous rename remains a filesystem race unless the host holds a stable generation.
 */
public final class M3FileDelta {
    public static final int MAX_ENTRIES = 100_000;
    public static final int MAX_CAPTURE_BYTES = 64 * 1024 * 1024;
    public static final String POLICY = "M3-FILE-DELTA/1";
    private static final HexFormat HEX = HexFormat.of();
    private static final int HASH_BYTES = 32;

    private M3FileDelta() { }

    /** Roles are explicitly supplied, never inferred from names or extensions. */
    public enum Kind { SOURCE, CLASS, BUILD_INPUT, DEPENDENCY, RESOURCE }
    public enum Change { ADDED, REMOVED, MODIFIED, ATTESTATION_CHANGED }
    public enum AbiChange { NOT_CLASS, UNKNOWN, SAME_BYTES, SAME_ATTESTED, DIFFERENT_ATTESTED }

    /** Ephemeral admission record; a Snapshot stores only its scalar/array lanes. */
    public record Entry(Kind kind, String path, long length, String sha256, String abiSha256) {
        public Entry {
            kind = Objects.requireNonNull(kind, "kind");
            checkPath(path);
            if (length < 0) throw new IllegalArgumentException("negative entry length");
            checkHash(sha256, "file sha256");
            if (abiSha256 != null) {
                if (kind != Kind.CLASS) throw new IllegalArgumentException("ABI requires CLASS role");
                checkHash(abiSha256, "class ABI sha256");
            }
        }
    }

    public record Delta(Kind kind, String path, Change change,
                        String previousSha256, String currentSha256, AbiChange abi) {
        public Delta {
            Objects.requireNonNull(kind, "kind");
            checkPath(path);
            Objects.requireNonNull(change, "change");
            Objects.requireNonNull(abi, "abi");
        }
    }

    public record Comparison(String previousRoot, String currentRoot,
                             boolean contextChanged, List<Delta> changes) {
        public Comparison {
            checkHash(previousRoot, "previous snapshot root");
            checkHash(currentRoot, "current snapshot root");
            changes = List.copyOf(changes);
        }
        /** Exact snapshot stability only; never permission to skip a build-tool task. */
        public boolean sameEvidence() {
            return !contextChanged && changes.isEmpty() && previousRoot.equals(currentRoot);
        }
    }

    /**
     * Immutable, packed index of a complete host-provided inventory. Each source or class
     * path keeps one path reference, one role byte, one length, and 32 digest bytes.
     * Optional compiler ABI digests occupy a separate packed 32-byte lane and flag byte.
     */
    public static final class Snapshot {
        private final String[] paths;
        private final byte[] kinds;
        private final long[] lengths;
        private final byte[] hashes;
        private final byte[] abiHashes;
        private final byte[] abiPresent;
        private final String contextSha256;
        private final String rootSha256;

        private Snapshot(List<Entry> entries, String contextSha256) {
            checkHash(contextSha256, "build context sha256");
            Objects.requireNonNull(entries, "entries");
            if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("entry budget");
            List<Entry> ordered = new ArrayList<>(entries);
            ordered.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::path));
            int count = ordered.size();
            this.paths = new String[count];
            this.kinds = new byte[count];
            this.lengths = new long[count];
            this.hashes = new byte[count * HASH_BYTES];
            this.abiHashes = new byte[count * HASH_BYTES];
            this.abiPresent = new byte[count];
            this.contextSha256 = contextSha256;
            Entry previous = null;
            for (int i = 0; i < count; i++) {
                Entry entry = Objects.requireNonNull(ordered.get(i), "entry");
                if (previous != null && sameKey(previous, entry)) {
                    throw new IllegalArgumentException("duplicate typed file path");
                }
                paths[i] = entry.path();
                kinds[i] = (byte) entry.kind().ordinal();
                lengths[i] = entry.length();
                putHash(entry.sha256(), hashes, i * HASH_BYTES);
                if (entry.abiSha256() != null) {
                    putHash(entry.abiSha256(), abiHashes, i * HASH_BYTES);
                    abiPresent[i] = 1;
                }
                previous = entry;
            }
            MessageDigest digest = digest();
            addString(digest, POLICY);
            addString(digest, contextSha256);
            addInt(digest, count);
            for (int i = 0; i < count; i++) {
                digest.update(kinds[i]);
                addString(digest, paths[i]);
                addLong(digest, lengths[i]);
                digest.update(hashes, i * HASH_BYTES, HASH_BYTES);
                digest.update(abiPresent[i]);
                if (abiPresent[i] != 0) {
                    digest.update(abiHashes, i * HASH_BYTES, HASH_BYTES);
                }
            }
            this.rootSha256 = HEX.formatHex(digest.digest());
        }

        public int size() { return paths.length; }
        public String contextSha256() { return contextSha256; }
        public String rootSha256() { return rootSha256; }

        /** Materialize one detached projection, not one retained object per file. */
        public Entry entry(int ordinal) {
            Objects.checkIndex(ordinal, size());
            return new Entry(Kind.values()[kinds[ordinal]], paths[ordinal], lengths[ordinal],
                    HEX.formatHex(hashes, ordinal * HASH_BYTES, (ordinal + 1) * HASH_BYTES),
                    abiPresent[ordinal] == 0 ? null :
                            HEX.formatHex(abiHashes, ordinal * HASH_BYTES,
                                    (ordinal + 1) * HASH_BYTES));
        }
    }

    public static Snapshot snapshot(List<Entry> entries, String buildContextSha256) {
        return new Snapshot(entries, buildContextSha256);
    }

    /** Domain-separated SHA-256 of supplied exact bytes, including binary class files. */
    public static Entry fromBytes(Kind kind, String path, byte[] exactBytes, String abiSha256) {
        Objects.requireNonNull(exactBytes, "exactBytes");
        return new Entry(kind, path, exactBytes.length,
                HEX.formatHex(digest().digest(exactBytes)), abiSha256);
    }

    /**
     * Read with the existing M3File bound and NOFOLLOW_LINKS policy. The caller supplies a
     * stable project root and enumerated paths; no implicit directory tree scan occurs.
     * Ancestor checks reject apparent links, but are not a substitute for a transactional
     * filesystem snapshot against concurrent namespace mutation.
     */
    public static Entry capture(Path root, Kind kind, String relativePath,
                                int maxBytes, String abiSha256) throws IOException {
        Objects.requireNonNull(root, "root");
        checkPath(relativePath);
        if (maxBytes < 0 || maxBytes > MAX_CAPTURE_BYTES) {
            throw new IllegalArgumentException("capture size budget");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path file = normalizedRoot.resolve(relativePath).normalize();
        if (!file.startsWith(normalizedRoot)) throw new IllegalArgumentException("escaped root");
        // Reject symbolic links in both the supplied root and the selected path.
        Path component = normalizedRoot.getRoot();
        for (Path element : normalizedRoot) {
            component = component.resolve(element);
            if (Files.isSymbolicLink(component)) throw new IOException("symbolic link in root");
        }
        component = normalizedRoot;
        for (Path element : normalizedRoot.relativize(file)) {
            component = component.resolve(element);
            if (Files.isSymbolicLink(component)) throw new IOException("symbolic link in path");
        }
        byte[] bytes = M3File.of(file).readAllBytes(maxBytes);
        return fromBytes(kind, relativePath, bytes, abiSha256);
    }

    /** Deterministic full merge; creates objects only for changed entries. */
    public static Comparison compare(Snapshot previous, Snapshot current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        List<Delta> changes = new ArrayList<>();
        int left = 0, right = 0;
        while (left < previous.size() || right < current.size()) {
            int cmp = left == previous.size() ? 1
                    : right == current.size() ? -1 : compareKey(previous, left, current, right);
            if (cmp < 0) {
                changes.add(change(previous, left, null, -1, Change.REMOVED));
                left++;
            } else if (cmp > 0) {
                changes.add(change(null, -1, current, right, Change.ADDED));
                right++;
            } else {
                if (!sameBytes(previous, left, current, right)) {
                    changes.add(change(previous, left, current, right, Change.MODIFIED));
                } else if (!sameAbiEvidence(previous, left, current, right)) {
                    changes.add(change(previous, left, current, right, Change.ATTESTATION_CHANGED));
                }
                left++;
                right++;
            }
        }
        return new Comparison(previous.rootSha256(), current.rootSha256(),
                !previous.contextSha256().equals(current.contextSha256()), changes);
    }

    private static Delta change(Snapshot old, int oi, Snapshot now, int ni, Change change) {
        Snapshot source = old == null ? now : old;
        int index = old == null ? ni : oi;
        Kind kind = Kind.values()[source.kinds[index]];
        AbiChange abi = AbiChange.NOT_CLASS;
        if (kind == Kind.CLASS) {
            if (old != null && now != null && sameBytes(old, oi, now, ni)) {
                abi = AbiChange.SAME_BYTES;
            } else if (old == null || now == null || old.abiPresent[oi] == 0
                    || now.abiPresent[ni] == 0) {
                abi = AbiChange.UNKNOWN;
            } else {
                abi = sameDigest(old.abiHashes, oi, now.abiHashes, ni)
                        ? AbiChange.SAME_ATTESTED : AbiChange.DIFFERENT_ATTESTED;
            }
        }
        return new Delta(kind, source.paths[index], change,
                old == null ? null : HEX.formatHex(old.hashes, oi * HASH_BYTES, (oi + 1) * HASH_BYTES),
                now == null ? null : HEX.formatHex(now.hashes, ni * HASH_BYTES, (ni + 1) * HASH_BYTES),
                abi);
    }

    private static int compareKey(Snapshot a, int i, Snapshot b, int j) {
        int byRole = Byte.compare(a.kinds[i], b.kinds[j]);
        return byRole == 0 ? a.paths[i].compareTo(b.paths[j]) : byRole;
    }

    private static boolean sameKey(Entry a, Entry b) {
        return a.kind() == b.kind() && a.path().equals(b.path());
    }

    private static boolean sameBytes(Snapshot a, int i, Snapshot b, int j) {
        return a.lengths[i] == b.lengths[j]
                && sameDigest(a.hashes, i, b.hashes, j);
    }

    private static boolean sameDigest(byte[] a, int i, byte[] b, int j) {
        int baseA = i * HASH_BYTES, baseB = j * HASH_BYTES, unequal = 0;
        for (int k = 0; k < HASH_BYTES; k++) unequal |= a[baseA + k] ^ b[baseB + k];
        return unequal == 0;
    }

    private static boolean sameAbiEvidence(Snapshot a, int i, Snapshot b, int j) {
        return a.abiPresent[i] == b.abiPresent[j]
                && (a.abiPresent[i] == 0 || sameDigest(a.abiHashes, i, b.abiHashes, j));
    }

    /** Decode validated lowercase hex directly into flat lanes without temporary arrays. */
    private static void putHash(String hash, byte[] dest, int offset) {
        for (int i = 0; i < HASH_BYTES; i++) {
            int high = Character.digit(hash.charAt(2 * i), 16);
            int low = Character.digit(hash.charAt(2 * i + 1), 16);
            dest[offset + i] = (byte) ((high << 4) | low);
        }
    }

    private static void checkPath(String path) {
        if (path == null || path.isBlank() || path.length() > 4096
                || path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf(':') >= 0) {
            throw new IllegalArgumentException("invalid relative path");
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")
                    || part.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("invalid relative path segment");
            }
        }
    }

    private static void checkHash(String hash, String label) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid " + label);
        }
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }

    private static void addString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        addInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void addInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void addLong(MessageDigest digest, long value) {
        for (int bits = 56; bits >= 0; bits -= 8) digest.update((byte) (value >>> bits));
    }
}
