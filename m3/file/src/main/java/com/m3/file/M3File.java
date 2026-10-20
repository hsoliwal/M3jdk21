// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The M3 file entry: a thin facade over one {@link M3FileTree} row, and the read boundary over the
 * caller's original NIO Path/provider.
 *
 * <p>Tree entries come only from {@link M3FileTree#file(int)}; they hold {@code (tree, id)} and build
 * their {@link Path} lazily from the name lanes. Identity is the row, not the instance: the tree may
 * drop and recreate facades at will ({@link M3FileResidency}). {@link #of(Path)} keeps the original
 * detached edge: no tree, no interned path table, no retained descriptor. Metadata is a point-in-time
 * observation. Symbolic links are never followed by the file-open operations.</p>
 *
 * <p>Detached read edge adapted from Synexia MIndexResourceAccess file-edge semantics.</p>
 */
public final class M3File {
    private final M3FileTree tree;
    private final int id;
    private Path path;

    private M3File(Path path) {
        this.tree = M3FileTree.detached();
        this.id = -1;
        this.path = Objects.requireNonNull(path, "path");
    }

    M3File(M3FileTree tree, int id) {
        this.tree = tree;
        this.id = id;
    }

    /** Detached edge over a caller-owned Path; lane accessors are unavailable on it. */
    public static M3File of(Path path) { return new M3File(path); }

    /** Original provider-owned Path for a detached file; {@code root.resolve(names)} for a tree entry (memoised). */
    public Path path() {
        Path current = path;
        if (current == null) {
            current = tree.path(id);
            path = current;
        }
        return current;
    }

    /** Same as {@link #path()}; named for tree-entry callers. */
    public Path toPath() { return path(); }

    /** Row id, or {@code -1} for a detached file. */
    public int id() { return id; }

    public boolean isTreeEntry() { return id >= 0; }

    /** Owning tree; {@link M3FileTree#detached()} for a detached file. */
    public M3FileTree tree() { return tree; }

    public String name() { return tree.name(entryId()); }

    public Optional<M3File> parent() {
        int parentId = tree.parent(entryId());
        return parentId < 0 ? Optional.empty() : Optional.of(tree.file(parentId));
    }

    public int depth() { return tree.depth(entryId()); }

    public int childCount() { return tree.childCount(entryId()); }

    public IntStream childIds() { return tree.childIds(entryId()); }

    public Stream<M3File> children() { return childIds().mapToObj(tree::file); }

    public boolean isDirectory() { return tree.flag(M3FileFlag.DIR, entryId()); }

    public boolean isSymbolicLink() { return tree.flag(M3FileFlag.LINK, entryId()); }

    public boolean isRegularFile() { return tree.flag(M3FileFlag.REGULAR, entryId()); }

    public M3FileKind kind() { return tree.kind(entryId()); }

    /** Interned lower-case extension, {@code ""} for none and for directories. */
    public String type() { return tree.type(entryId()); }

    public int typeId() { return tree.typeId(entryId()); }

    /** Walk over this entry and its descendants. */
    public M3FileWalk walk() { return tree.walk(entryId()); }

    public boolean isHidden() { return tree.flag(M3FileFlag.HIDDEN, entryId()); }

    public boolean flag(M3FileFlag flag) { return tree.flag(flag, entryId()); }

    /** Byte length observed at scan time; {@code -1} unknown. */
    public long size() { return tree.size(entryId()); }

    /** Last-modified epoch millis observed at scan time; {@code Long.MIN_VALUE} unknown. */
    public long lastModified() { return tree.mtime(entryId()); }

    /** Every invocation refreshes metadata with NOFOLLOW_LINKS. */
    public Metadata metadata() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                path(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new Metadata(attributes.isRegularFile(), attributes.isDirectory(),
                attributes.isSymbolicLink(), attributes.size(),
                attributes.lastModifiedTime());
    }

    /** Opens a caller-owned channel; the caller MUST close it. */
    public SeekableByteChannel openReadChannel() throws IOException {
        requireRegular();
        return Files.newByteChannel(path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }

    /** Caller owns the returned stream and closes the underlying channel. */
    public InputStream openInputStream() throws IOException {
        return Channels.newInputStream(openReadChannel());
    }

    /**
     * Bounded complete read; a size precheck is only an optimization. The actual
     * stream is checked again to handle concurrent file growth or replacement.
     */
    public byte[] readAllBytes(int maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        if (metadata().size() > maxBytes) {
            throw new IOException("resource exceeds configured read limit");
        }
        try (InputStream input = openInputStream()) {
            byte[] data = input.readNBytes(maxBytes);
            if (input.read() != -1) {
                throw new IOException("resource exceeds configured read limit");
            }
            return data;
        }
    }

    /** {@link #openInputStream(long, M3FileProgress, M3FileShadow...)} without a byte limit. */
    public M3FileShadowInputStream openInputStream(M3FileProgress progress, M3FileShadow... shadows)
            throws IOException {
        return openInputStream(Long.MAX_VALUE, progress, shadows);
    }

    /**
     * Caller-closed input stream with a sub-progress monitor (units = bytes, total = observed size) and
     * shadows that move as it is read; links are not followed, the limit is enforced while streaming.
     */
    public M3FileShadowInputStream openInputStream(long maxBytes, M3FileProgress progress, M3FileShadow... shadows)
            throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        Metadata observed = metadata();
        if (!observed.regularFile()) throw new IOException("resource is not a regular file");
        if (observed.size() > maxBytes) throw new IOException("resource exceeds configured read limit");
        progress.begin(path().toString(), observed.size());
        InputStream input = Channels.newInputStream(
                Files.newByteChannel(path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
        return new M3FileShadowInputStream(input, maxBytes, progress, shadows);
    }

    /**
     * Caller-closed output stream (create or truncate, links not followed) with a sub-progress monitor
     * and shadows that move as it is written. Tree lanes are a scan-time observation and do not change.
     */
    public M3FileShadowOutputStream openOutputStream(M3FileProgress progress, M3FileShadow... shadows)
            throws IOException {
        progress.begin(path().toString(), 0);
        OutputStream output = Files.newOutputStream(path(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        return new M3FileShadowOutputStream(output, Long.MAX_VALUE, progress, shadows);
    }

    /** One read pass that only feeds {@code shadows}; returns the bytes read. */
    public long observe(long maxBytes, M3FileProgress progress, M3FileShadow... shadows) throws IOException {
        try (M3FileShadowInputStream input = openInputStream(maxBytes, progress, shadows)) {
            byte[] buffer = new byte[8192];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                // shadows consume the block
            }
            progress.done();
            return input.count();
        }
    }

    private void requireRegular() throws IOException {
        if (!metadata().regularFile()) {
            throw new IOException("resource is not a regular file");
        }
    }

    private int entryId() {
        if (id < 0) throw new IllegalStateException("detached M3File has no tree row: " + path);
        return id;
    }

    /** Tree entries are equal by {@code (tree, id)}; detached files by Path. */
    @Override
    public boolean equals(Object other) {
        return other instanceof M3File that && that.tree == tree && that.id == id
                && (id >= 0 || that.path().equals(path()));
    }

    @Override
    public int hashCode() {
        return id >= 0 ? 31 * System.identityHashCode(tree) + id : path().hashCode();
    }

    @Override
    public String toString() {
        return id >= 0 ? "M3File[" + id + ":" + name() + "]" : "M3File[" + path + "]";
    }

    public record Metadata(boolean regularFile, boolean directory,
            boolean symbolicLink, long size, FileTime lastModified) {
        public Metadata {
            if (size < 0) throw new IllegalArgumentException("negative file size");
            Objects.requireNonNull(lastModified, "lastModified");
        }
    }
}
