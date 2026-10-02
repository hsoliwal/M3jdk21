// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * M3-owned native content-addressed artifact store.
 *
 * <p>Payloads are stored once by SHA-256 under {@code blobs/}; named artifacts are tiny immutable
 * refs under {@code refs/}. This is the first inlined M3IndexDB storage atom. Coordinate-table mmap
 * storage is a later donor-inline pass and does not require changing this API.
 */
public final class M3IndexDB implements AutoCloseable {
    private static final String REF_MAGIC = "M3INDEXDB-REF-1";

    private final Path root;
    private final Path blobs;
    private final Path refs;
    private boolean closed;

    private M3IndexDB(Path root) throws IOException {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.blobs = this.root.resolve("blobs");
        this.refs = this.root.resolve("refs");
        Files.createDirectories(blobs);
        Files.createDirectories(refs);
    }

    public static M3IndexDB open(Path root) throws IOException {
        return new M3IndexDB(root);
    }

    public Path root() {
        ensureOpen();
        return root;
    }

    public synchronized M3IndexDbArtifact putArtifact(
            String name,
            String kind,
            int formatVersion,
            byte[] payload) throws IOException {
        ensureOpen();
        M3IndexDbArtifact artifact =
                M3IndexDbArtifact.create(name, kind, formatVersion, payload);
        Path blob = blobPath(artifact.contentSha256());
        if (!Files.isRegularFile(blob)) {
            publish(blob, artifact.payload());
        } else {
            byte[] existing = Files.readAllBytes(blob);
            if (!artifact.contentSha256().equals(sha256(existing))) {
                throw new IOException("M3IndexDB blob integrity failure: " + artifact.contentSha256());
            }
        }

        String ref = String.join(
                "\n",
                REF_MAGIC,
                artifact.name(),
                artifact.kind(),
                Integer.toString(artifact.formatVersion()),
                artifact.contentSha256(),
                Long.toString(artifact.payloadLength()))
                + "\n";
        publish(refPath(artifact.name()), ref.getBytes(StandardCharsets.UTF_8));
        return artifact;
    }

    public synchronized boolean containsArtifact(String name) {
        ensureOpen();
        return Files.isRegularFile(refPath(name));
    }

    public synchronized M3IndexDbArtifact requireArtifact(String name) throws IOException {
        ensureOpen();
        Path ref = refPath(name);
        if (!Files.isRegularFile(ref)) {
            throw new IOException("Missing M3IndexDB artifact: " + name);
        }
        List<String> lines = Files.readAllLines(ref, StandardCharsets.UTF_8);
        if (lines.size() != 6 || !REF_MAGIC.equals(lines.get(0)) || !name.equals(lines.get(1))) {
            throw new IOException("M3IndexDB ref integrity failure: " + name);
        }
        int formatVersion;
        long length;
        try {
            formatVersion = Integer.parseInt(lines.get(3));
            length = Long.parseLong(lines.get(5));
        } catch (NumberFormatException malformed) {
            throw new IOException("M3IndexDB ref numeric field failure: " + name, malformed);
        }
        String hash = lines.get(4);
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IOException("M3IndexDB ref hash failure: " + name);
        }

        Path blob = blobPath(hash);
        if (!Files.isRegularFile(blob)) {
            throw new IOException("M3IndexDB blob missing: " + hash);
        }
        byte[] payload = Files.readAllBytes(blob);
        if (payload.length != length || !hash.equals(sha256(payload))) {
            throw new IOException("M3IndexDB blob content failure: " + name);
        }
        try {
            return new M3IndexDbArtifact(name, lines.get(2), formatVersion, hash, payload);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("M3IndexDB artifact metadata failure: " + name, invalid);
        }
    }

    public synchronized long blobBytes() throws IOException {
        ensureOpen();
        try (var stream = Files.list(blobs)) {
            return stream.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException failure) {
                    throw new SizeFailure(failure);
                }
            }).sum();
        } catch (SizeFailure wrapped) {
            throw wrapped.cause;
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private Path refPath(String name) {
        return refs.resolve(sha256(name.getBytes(StandardCharsets.UTF_8)) + ".ref");
    }

    private Path blobPath(String sha256) {
        return blobs.resolve(sha256 + ".blob");
    }

    private static void publish(Path target, byte[] bytes) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean published = false;
        try {
            Files.write(
                    temporary,
                    bytes,
                    StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            published = true;
        } finally {
            if (!published) Files.deleteIfExists(temporary);
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("M3IndexDB is closed");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static final class SizeFailure extends RuntimeException {
        private final IOException cause;

        private SizeFailure(IOException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
