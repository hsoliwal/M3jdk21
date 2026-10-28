// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21 by the Synexia m3-project-delta recipe: checked timestamp observations.
package com.m3.file;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Thin, stateless read boundary over the caller's original NIO Path/provider.
 *
 * <p>Adapted from Synexia MIndexResourceAccess file-edge semantics. No new
 * filesystem, directory tree, interned path table or retained file descriptor is
 * created. Metadata is a point-in-time observation and is not an immutable view
 * of an externally changing filesystem. Symbolic links are never followed by
 * the file-open operations.</p>
 */
public final class M3File {
    /** A previously absent logical file now has an observed fingerprint. */
    public static final int ADDED = 1;
    /** A previously observed logical file is now known to be absent. */
    public static final int REMOVED = 2;
    /** Both observations exist and their byte count or raw SHA-256 differs. */
    public static final int CONTENT_CHANGED = 4;

    private final Path path;

    private M3File(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static M3File of(Path path) { return new M3File(path); }

    /** Original provider-owned Path, not an invented global file identifier. */
    public Path path() { return path; }

    /** Every invocation refreshes metadata with NOFOLLOW_LINKS. */
    public Metadata metadata() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new Metadata(attributes.isRegularFile(), attributes.isDirectory(),
                attributes.isSymbolicLink(), attributes.size(),
                attributes.lastModifiedTime());
    }

    /** Opens a caller-owned channel; the caller MUST close it. */
    public SeekableByteChannel openReadChannel() throws IOException {
        requireRegular();
        return Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }

    /** Caller owns the returned stream and closes the underlying channel. */
    public InputStream openInputStream() throws IOException {
        return Channels.newInputStream(openReadChannel());
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

    /**
     * Materialize exact, verified source bytes and their original observation
     * in a single provider read. This is for a compiler/IDE that needs payload,
     * unlike fingerprint-only build consumers. The limit bounds payload size;
     * no second source stream or duplicate SHA-256 pass is created.
     *
     * <p>Returned bytes are private to this immutable capture. Use a read-only
     * ByteBuffer for low-copy access or request a deliberate byte[] copy.
     * The observation still reflects metadata bracketing, not an atomic
     * external filesystem snapshot.</p>
     */
    public CapturedBytes captureBytes(int maxBytes, M3StreamProgress progress) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        M3StreamProgress.require(progress);
        ByteAccumulator output = new ByteAccumulator(maxBytes);
        Observation observed = observe(maxBytes, progress, output::append);
        return new CapturedBytes(output.freeze(), observed);
    }

    /** Materialize once without a caller progress monitor. */
    public CapturedBytes captureBytes(int maxBytes) throws IOException {
        return captureBytes(maxBytes, M3StreamProgress.none());
    }

    /**
     * Original M3File read with caller-owned reusable bounded scratch. The
     * buffer is borrowed for the entire call, not retained. Its caller must
     * prevent concurrent reuse and mutation by byte observers. It may be reused
     * serially across every input in one Maven/IDE project inventory.
     *
     * <p>No new source handle is opened beyond the existing checked channel;
     * the original hash, file-key/mtime/size, cancellation and EOF contracts
     * are identical to {@link #observe(long, M3StreamProgress, ByteObserver)}.
     * The scratch length is deliberately bounded at 1 MiB.</p>
     */
    public Observation observe(long maxBytes, byte[] scratch, M3StreamProgress progress,
            ByteObserver observer) throws IOException {
        Objects.requireNonNull(scratch, "scratch");
        if (scratch.length < 1 || scratch.length > (1 << 20)) {
            throw new IllegalArgumentException("scratch length must be 1..1048576");
        }
        return observe(maxBytes, scratch, false,
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

    /**
     * Owned immutable source content plus exactly the observation made while
     * consuming those bytes. No exposed mutable array reference and no retained
     * file channel, native pointer or filesystem owner.
     */
    public static final class CapturedBytes {
        private final byte[] content;
        private final Observation observation;

        private CapturedBytes(byte[] content, Observation observation) {
            this.content = Objects.requireNonNull(content, "content");
            this.observation = Objects.requireNonNull(observation, "observation");
            if (observation.fingerprint().size() != content.length) {
                throw new IllegalArgumentException("observed source length mismatch");
            }
        }

        public int length() { return content.length; }
        public Observation observation() { return observation; }

        /** Fresh cursor backed by private content, never a mutable ByteBuffer. */
        public ByteBuffer readOnlyBuffer() {
            return ByteBuffer.wrap(content).asReadOnlyBuffer();
        }

        /** A separate caller-owned array when a legacy parser explicitly needs byte[]. */
        public byte[] copyBytes() { return content.clone(); }
    }

    /** One growing primitive byte[] buffer without per-chunk objects. */
    private static final class ByteAccumulator {
        private final int limit;
        private byte[] bytes;
        private int size;

        ByteAccumulator(int maxBytes) {
            limit = maxBytes;
            bytes = new byte[Math.min(maxBytes, 8192)];
        }

        void append(byte[] source, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, source.length);
            if (length > limit - size) throw new IOException("observed content exceeds byte budget");
            int required = size + length;
            if (required > bytes.length) {
                long wanted = Math.max((long) required,
                        (long) bytes.length + (bytes.length >>> 1) + 1L);
                int capacity = (int) Math.min((long) limit, wanted);
                bytes = Arrays.copyOf(bytes, capacity);
            }
            System.arraycopy(source, offset, bytes, size, length);
            size = required;
        }

        byte[] freeze() {
            return size == bytes.length ? bytes : Arrays.copyOf(bytes, size);
        }
    }

    /** Immutable content plus checked metadata; timestamp equality does not certify unchanged bytes. */
    public record Observation(Fingerprint fingerprint, FileTime lastModified) {
        public Observation {
            Objects.requireNonNull(fingerprint, "fingerprint");
            Objects.requireNonNull(lastModified, "lastModified");
        }
    }

    private void requireRegular() throws IOException {
        if (!metadata().regularFile()) {
            throw new IOException("resource is not a regular file");
        }
    }

    public record Metadata(boolean regularFile, boolean directory,
            boolean symbolicLink, long size, FileTime lastModified) {
        public Metadata {
            if (size < 0) throw new IllegalArgumentException("negative file size");
            Objects.requireNonNull(lastModified, "lastModified");
        }
    }
}
