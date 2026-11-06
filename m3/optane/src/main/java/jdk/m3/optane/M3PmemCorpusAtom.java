// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Bounded, candidate-only source atom delivery over the existing JNI mapping owner.
 * Source identity comes from the existing immutable source inventory, not this class.
 * This class exposes no public Java SDK API or native symbol.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemCorpusAtom.java blob f7f504b96418; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class M3PmemCorpusAtom {
    private static final int READ_CHUNK = 64 * 1024;
    private static final int MAX_ATOM_BYTES = 1024 * 1024;
    private static final long MAX_SOURCE_BYTES = 64L * 1024 * 1024;

    private M3PmemCorpusAtom() { }

    record Atom(long sourceLength, String sha256, long start, byte[] bytes) {
        Atom {
            bytes = bytes.clone();
        }
        @Override public byte[] bytes() { return bytes.clone(); }
        long end() { return start + bytes.length; }
        String atomSha256() { return hex(digest().digest(bytes)); }
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException missing) { throw new AssertionError(missing); }
    }

    private static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }

    private static long decimal(String text, String label) {
        if (text == null || !text.matches("[0-9]{1,20}"))
            throw new IllegalArgumentException("invalid decimal " + label);
        try { return Long.parseLong(text); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("decimal overflow " + label, invalid); }
    }

    private static Path source(Path root, String relative) throws IOException {
        Objects.requireNonNull(root, "root");
        if (relative == null || relative.isEmpty() || relative.startsWith("/")
                || relative.indexOf('\\') >= 0 || relative.indexOf(':') >= 0) {
            throw new IllegalArgumentException("noncanonical source path");
        }
        for (String part : relative.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("noncanonical source path");
        }
        for (int i = 0; i < relative.length(); i++) {
            char c = relative.charAt(i);
            if (c < 32 || c == 127) throw new IllegalArgumentException("control in source path");
        }
        for (Path part : root) if (part.toString().equals(".."))
            throw new IllegalArgumentException("parent traversal in root");
        Path absolute = root.toAbsolutePath();
        Path path = absolute.resolve(relative);
        // Refuse symlinks at all path components; never silently resolve aliases.
        for (Path cursor = path; cursor != null; cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink path refused");
        }
        if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("missing real root directory");
        return path;
    }

    private static BasicFileAttributes stat(Path path, long expectedLength) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile() || attrs.size() != expectedLength)
            throw new IOException("nonregular or drifted source length");
        return attrs;
    }

    private static void sameFile(Path path, BasicFileAttributes before, long expectedLength)
            throws IOException {
        for (Path cursor = path; cursor != null; cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) throw new IOException("source path changed to symlink");
        }
        BasicFileAttributes after = stat(path, expectedLength);
        if (!Objects.equals(before.fileKey(), after.fileKey())
                || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("source file identity drift");
    }

    /** Maps one existing file at a time; holds no mapping lease after return. */
    static Atom read(Path root, String relative, long expectedLength, String expectedSha256,
                     long start, int atomBytes) throws IOException {
        if (expectedLength < 0 || expectedLength > MAX_SOURCE_BYTES
                || start < 0 || atomBytes < 0 || atomBytes > MAX_ATOM_BYTES
                || start > expectedLength || atomBytes > expectedLength - start) {
            throw new IllegalArgumentException("source or atom range out of bounds");
        }
        if (expectedSha256 == null || !expectedSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("invalid source SHA-256");
        Path path = source(root, relative);
        BasicFileAttributes before = stat(path, expectedLength);
        MessageDigest sha = digest();
        byte[] atom = new byte[atomBytes];
        // No JNI zero-length map; empty files are still hash-validated.
        if (expectedLength != 0) {
            byte[] scratch = new byte[READ_CHUNK];
            try (M3PmemRegion mapping = M3PmemSdk.open(path, 0, expectedLength,
                    M3PmemAccess.READ_ONLY, M3PmemBackend.POSIX)) {
                for (long offset = 0; offset < expectedLength;) {
                    int count = (int) Math.min((long) scratch.length, expectedLength - offset);
                    mapping.read(offset, scratch, 0, count);
                    sha.update(scratch, 0, count);
                    long left = Math.max(offset, start);
                    long right = Math.min(offset + count, start + atomBytes);
                    if (left < right) {
                        System.arraycopy(scratch, (int) (left - offset), atom,
                                (int) (left - start), (int) (right - left));
                    }
                    offset += count;
                }
            }
        }
        sameFile(path, before, expectedLength);
        String actual = hex(sha.digest());
        if (!MessageDigest.isEqual(HexFormat.of().parseHex(expectedSha256),
                HexFormat.of().parseHex(actual)))
            throw new IOException("source SHA-256 mismatch");
        return new Atom(expectedLength, actual, start, atom);
    }

    private static String quotedPath(String path) {
        // ASCII controls and backslashes were already refused by source().
        return "\"" + path.replace("\"", "\\\"") + "\"";
    }

    /** Exact six-field CLI; output stays candidate-only and never writes sources. */
    public static void main(String[] args) {
        try {
            if (args.length != 6) throw new IllegalArgumentException("six arguments required");
            long expectedLength = decimal(args[2], "source length");
            long start = decimal(args[4], "atom start");
            long width = decimal(args[5], "atom width");
            if (width > MAX_ATOM_BYTES) throw new IllegalArgumentException("atom width exceeds limit");
            Atom atom = read(Path.of(args[0]), args[1], expectedLength, args[3], start, (int) width);
            System.out.println("{\"schema\":\"m3.pmem-corpus-atom.v1\","
                    + "\"status\":\"CANDIDATE_ONLY\","
                    + "\"path\":" + quotedPath(args[1]) + ","
                    + "\"source_sha256\":\"" + atom.sha256() + "\","
                    + "\"source_bytes\":" + atom.sourceLength() + ","
                    + "\"atom_start\":" + atom.start() + ","
                    + "\"atom_end\":" + atom.end() + ","
                    + "\"atom_sha256\":\"" + atom.atomSha256() + "\","
                    + "\"atom_base64\":\"" + Base64.getEncoder().encodeToString(atom.bytes()) + "\"}");
        } catch (IOException | RuntimeException | LinkageError refusal) {
            System.err.println("PMEM_CORPUS_REFUSED:" + refusal.getClass().getSimpleName());
            System.exit(2);
        }
    }
}
