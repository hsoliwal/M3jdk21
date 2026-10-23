// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21 by the Synexia m3-project-delta recipe: checked timestamp observations.
package com.m3.file;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
=======
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * observation. Symbolic links are never followed by the file-open operations. Reads go through the
 * tree's {@link M3FileSource}; for archive entries {@link #path()} is a logical path below the archive
 * file (use the read methods here, not {@code Files}).</p>
 *
 * <p>Detached read edge adapted from Synexia MIndexResourceAccess file-edge semantics.</p>
 */
public final class M3File {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** A previously absent logical file now has an observed fingerprint. */
    public static final int ADDED = 1;
    /** A previously observed logical file is now known to be absent. */
    public static final int REMOVED = 2;
    /** Both observations exist and their byte count or raw SHA-256 differs. */
    public static final int CONTENT_CHANGED = 4;

    private final Path path;
=======
    private final M3FileTree tree;
    private final int id;
    private Path path;
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5

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

    /** Why the last observation of this entry failed; {@link M3FileError#NONE} otherwise. */
    public M3FileError error() { return tree.error(entryId()); }

    /** Byte length observed at scan time; {@code -1} unknown. */
    public long size() { return tree.size(entryId()); }

    /** Last-modified epoch millis observed at scan time; {@code Long.MIN_VALUE} unknown. */
    public long lastModified() { return tree.mtime(entryId()); }

    /**
     * Metadata from the tree's {@link M3FileSource}: for the file system every invocation refreshes
     * with NOFOLLOW_LINKS; archive entries report their central-directory observation.
     */
    public Metadata metadata() throws IOException {
        return tree.source().metadata(this);
    }

    /** Opens a caller-owned channel; the caller MUST close it. Archive entries have none. */
    public SeekableByteChannel openReadChannel() throws IOException {
        requireRegular();
        return tree.source().openChannel(this);
    }

    /** Caller owns the returned stream and closes the underlying channel or entry stream. */
    public InputStream openInputStream() throws IOException {
        requireRegular();
        return tree.source().openContent(this);
    }

    /** Monitor read/skip byte work without buffering or duplicating source data. */
    public InputStream openInputStream(M3StreamProgress progress) throws IOException {
        M3StreamProgress.require(progress);
        return M3ShadowStreams.monitoredInput(openInputStream(), progress);
    }

    /**
     * Open one original provider channel with a bounded, shared shadow-page arena.
     * All layered consumers obtain cursors using {@code shared.shadow(child)};
     * the owning caller closes this stream. No filesystem scan or new source owner.
     */
    public M3ShadowStreams.SharedInput openShadowInputStream(
            int maxRetainedBytes, M3StreamProgress progress) throws IOException {
        if (maxRetainedBytes < 0) throw new IllegalArgumentException("negative shadow capacity");
        M3StreamProgress.require(progress);
        return M3ShadowStreams.input(openInputStream(), maxRetainedBytes, progress);
    }

    /**
     * Bounded complete read; a size precheck is only an optimization. The actual
     * stream is checked again to handle concurrent file growth or replacement.
     */
    public byte[] readAllBytes(int maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        if (metadata().size() > maxBytes) {
            throw new M3FileLimitException("resource exceeds configured read limit");
        }
        try (InputStream input = openInputStream()) {
            byte[] data = input.readNBytes(maxBytes);
            if (input.read() != -1) {
                throw new M3FileLimitException("resource exceeds configured read limit");
            }
            return data;
        }
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Hash already-known, ordered, non-overlapping byte spans through one NIO
     * channel. This is intended for FileIndex record/chunk reuse after a caller
     * has established which spans require work; it does not discover changes.
     *
     * <p>Offsets are half-open physical byte coordinates, never UTF-16 indexes.
     * maxEnd bounds the largest source coordinate, including gaps. Provider
     * metadata is checked before and after reading but does not establish an
     * atomic snapshot or prove unchanged bytes in gaps. No complete file digest,
     * compiler ABI result or persistent cache is inferred from range hashes.
     *
     * <p>Inputs are copied before any provider I/O so progress callbacks cannot
     * change selected ranges. Result storage is three primitive long[] lanes:
     * starts, lengths, and four SHA-256 digest words per physical range.
     */
    public RangeFingerprints fingerprintRanges(long[] offsets, long[] lengths,
                                               long maxEnd) throws IOException {
        return fingerprintRanges(offsets, lengths, maxEnd, M3StreamProgress.none());
    }

    /** Bounded selection with a borrowed progress monitor; one opened channel. */
    public RangeFingerprints fingerprintRanges(long[] offsets, long[] lengths,
                                               long maxEnd, M3StreamProgress progress)
            throws IOException {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        M3StreamProgress.require(progress);
        if (maxEnd < 0 || offsets.length != lengths.length
                || offsets.length > Integer.MAX_VALUE / 4) {
            throw new IllegalArgumentException("invalid range column geometry");
        }
        long[] selected = offsets.clone();
        long[] extents = lengths.clone();
        long previousEnd = 0;
        for (int row = 0; row < selected.length; row++) {
            long start = selected[row], extent = extents[row];
            if (start < previousEnd || extent < 0 || start > maxEnd
                    || extent > maxEnd - start) {
                throw new IllegalArgumentException("ranges must be ordered, disjoint and bounded");
            }
            previousEnd = start + extent;
        }
        long[] digestWords = new long[selected.length * 4];
        progress.checkCancelled();
        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile()) throw new IOException("resource is not a regular file");
        for (int row = 0; row < selected.length; row++) {
            if (selected[row] > before.size()
                    || extents[row] > before.size() - selected[row]) {
                throw new EOFException("range exceeds the observed file size");
            }
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("required SHA-256 provider unavailable", impossible);
        }
        byte[] bytes = new byte[8192];
        ByteBuffer page = ByteBuffer.wrap(bytes);
        if (selected.length != 0) {
            try (SeekableByteChannel channel = openReadChannel()) {
                for (int row = 0; row < selected.length; row++) {
                    progress.checkCancelled();
                    channel.position(selected[row]);
                    long remaining = extents[row];
                    digest.reset();
                    while (remaining > 0) {
                        progress.checkCancelled();
                        page.clear().limit((int) Math.min(bytes.length, remaining));
                        int read = readProgressing(channel, page);
                        if (read < 0) throw new EOFException("range shortened during observation");
                        digest.update(bytes, 0, read);
                        remaining -= read;
                        progress.worked(read);
                    }
                    ByteBuffer completed = ByteBuffer.wrap(digest.digest());
                    int at = row * 4;
                    digestWords[at] = completed.getLong();
                    digestWords[at + 1] = completed.getLong();
                    digestWords[at + 2] = completed.getLong();
                    digestWords[at + 3] = completed.getLong();
                }
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("resource changed during range observation");
        }
        return new RangeFingerprints(selected, extents, digestWords, after.lastModifiedTime());
    }

    /**
     * Observe raw bytes once with bounded scratch space, for source or binary files.
     *
     * <p>The returned fingerprint retains only a byte count and four SHA-256 words.
     * Reuse it for an already admitted immutable generation instead of reading and
     * hashing again for each consumer. This method does not parse source or classes,
     * create a cache, or establish compilation/ABI equivalence.</p>
     *
     * <p>The byte limit is enforced during reading, including file growth. Detected
     * size, modification-time or file-key changes are refused. These checks do not
     * make an externally mutable file atomic: an undetected concurrent overwrite
     * can still produce a stream observation rather than one filesystem generation.
     * Build owners must establish stable input admission before reusing outputs.</p>
     *
     * @param maxBytes maximum accepted stream length, including zero
     * @throws IllegalArgumentException if maxBytes is negative
     * @throws IOException if reading fails, the file is not regular, the budget is
     *         exceeded, or a concurrent change is detected; errors never mean absence
     */
    public Fingerprint fingerprint(long maxBytes) throws IOException {
        return observe(maxBytes).fingerprint();
    }

    /**
     * Observe content and its checked modification time in the same bounded read.
     * The timestamp is metadata, not part of {@link Fingerprint} equality or
     * {@link #contentDelta(Fingerprint, Fingerprint)}. Concurrent-change and byte
     * limit checks are identical to {@link #fingerprint(long)}; this is not an
     * atomic filesystem snapshot. Errors never produce an absent observation.
     */
    public Observation observe(long maxBytes) throws IOException {
        return observe(maxBytes, new byte[8192], false, M3StreamProgress.none(), null);
    }

    /**
     * A synchronous, borrowed view of bytes already read by this original M3File
     * observation. The callback must not mutate the scratch bytes or retain
     * their array: source, SHA-256, progress and parser layers advance together.
     * An exception cancels publication; no complete fingerprint is returned.
     */
    @FunctionalInterface
    public interface ByteObserver {
        void onBytes(byte[] buffer, int offset, int length) throws IOException;
    }

    /** One bounded source read: content SHA-256 plus progress and optional layers. */
    public Observation observe(long maxBytes, M3StreamProgress progress,
            ByteObserver observer) throws IOException {
        return observe(maxBytes, new byte[8192], false,
                M3StreamProgress.require(progress), Objects.requireNonNull(observer, "observer"));
    }

    /** Ledger-only admission: only absence at the first metadata read returns null. */
    Observation observeIfExists(byte[] scratch) throws IOException {
        return observe(Long.MAX_VALUE, scratch, true, M3StreamProgress.none(), null);
    }

    private Observation observe(long maxBytes, byte[] buffer, boolean initialAbsenceAllowed,
            M3StreamProgress progress, ByteObserver observer) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        Objects.requireNonNull(buffer, "scratch");
        if (buffer.length == 0) throw new IllegalArgumentException("empty scratch buffer");
        BasicFileAttributes before;
        try {
            before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            if (initialAbsenceAllowed) return null;
            throw missing;
        }
        if (!before.isRegularFile()) throw new IOException("resource is not a regular file");
        if (before.size() > maxBytes) {
            throw new IOException("resource exceeds configured read limit");
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("required SHA-256 provider unavailable", impossible);
        }
        long count = 0;
        ByteBuffer scratch = ByteBuffer.wrap(buffer);
        try (SeekableByteChannel channel = openReadChannel()) {
            while (count < maxBytes) {
                progress.checkCancelled();
                scratch.clear().limit((int) Math.min(buffer.length, maxBytes - count));
                int read = readProgressing(channel, scratch);
                if (read < 0) break;
                digest.update(buffer, 0, read);
                progress.worked(read);
                if (observer != null) observer.onBytes(buffer, 0, read);
                count += read;
            }
            if (count == maxBytes) {
                progress.checkCancelled();
                scratch.clear().limit(1);
                if (readProgressing(channel, scratch) >= 0) {
                    throw new IOException("resource exceeds configured read limit");
                }
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.size() != count || after.size() != count
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("resource changed during fingerprint observation");
        }
        ByteBuffer hash = ByteBuffer.wrap(digest.digest());
        Fingerprint content = new Fingerprint(count, hash.getLong(), hash.getLong(), hash.getLong(), hash.getLong());
        return new Observation(content, after.lastModifiedTime());
    }

    /** A stalled provider must fail rather than hang a ledger scan or invent a zero byte. */
    private static int readProgressing(SeekableByteChannel channel, ByteBuffer scratch) throws IOException {
        for (int attempt = 0; attempt <= 16; attempt++) {
            int read = channel.read(scratch);
            if (read != 0) return read;
        }
        throw new IOException("M3File read made no progress");
    }

    /**
     * Compare two admitted content observations of the same logical file without I/O.
     *
     * <p>Null means caller-established absence, never unread/unknown/inaccessible.
     * Zero means equal fingerprints or two known absences; a nonzero result is one
     * of {@link #ADDED}, {@link #REMOVED}, or {@link #CONTENT_CHANGED}. Locations,
     * timestamps, ABI, compiler context and output validity are not compared.
     * SHA-256 equality is a content-addressing signal, not collision-free byte
     * equality proof or authorization to skip compiler, processor, test or plugin work.</p>
     */
    public static int contentDelta(Fingerprint previous, Fingerprint current) {
        if (previous == null) return current == null ? 0 : ADDED;
        if (current == null) return REMOVED;
        return previous.equals(current) ? 0 : CONTENT_CHANGED;
    }

    /**
     * Detached, immutable columnar range fingerprints. Returned Fingerprint
     * records materialize on demand; this owner retains no per-range objects.
     */
    public static final class RangeFingerprints {
        private final long[] offsets;
        private final long[] lengths;
        private final long[] hashWords;
        private final FileTime lastModified;

        private RangeFingerprints(long[] offsets, long[] lengths,
                                  long[] hashWords, FileTime lastModified) {
            this.offsets = offsets;
            this.lengths = lengths;
            this.hashWords = hashWords;
            this.lastModified = lastModified;
        }

        public int size() { return offsets.length; }
        public long offset(int row) { return offsets[Objects.checkIndex(row, size())]; }
        public long length(int row) { return lengths[Objects.checkIndex(row, size())]; }
        public FileTime lastModified() { return lastModified; }

        public Fingerprint fingerprint(int row) {
            int at = Objects.checkIndex(row, size()) * 4;
            return new Fingerprint(lengths[row], hashWords[at], hashWords[at + 1],
                    hashWords[at + 2], hashWords[at + 3]);
        }
    }

    /**
     * Compact immutable raw-byte observation; digest words are in big-endian order.
     * This format is distinct from decoded UTF-16 and framed source-fact digests.
     * A caller importing existing digest words remains responsible for their provenance.
     */
    public record Fingerprint(long size, long digest0, long digest1, long digest2, long digest3) {
        public Fingerprint {
            if (size < 0) throw new IllegalArgumentException("negative fingerprint size");
        }
    }

    /** Immutable content plus checked metadata; timestamp equality does not certify unchanged bytes. */
    public record Observation(Fingerprint fingerprint, FileTime lastModified) {
        public Observation {
            Objects.requireNonNull(fingerprint, "fingerprint");
            Objects.requireNonNull(lastModified, "lastModified");
=======
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
        if (observed.size() > maxBytes) throw new M3FileLimitException("resource exceeds configured read limit");
        progress.begin(path().toString(), observed.size());
        return new M3FileShadowInputStream(tree.source().openContent(this), maxBytes, progress, shadows);
    }

    /**
     * Caller-closed output stream (create or truncate, links not followed) with a sub-progress monitor
     * and shadows that move as it is written; read-only sources refuse. Tree lanes are a scan-time
     * observation and do not change.
     */
    public M3FileShadowOutputStream openOutputStream(M3FileProgress progress, M3FileShadow... shadows)
            throws IOException {
        OutputStream output = tree.source().openOutput(this);
        progress.begin(path().toString(), 0);
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
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
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
