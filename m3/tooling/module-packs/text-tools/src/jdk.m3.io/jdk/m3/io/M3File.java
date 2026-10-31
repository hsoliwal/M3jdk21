// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;

/**
 * An immutable, bounded, strictly decoded file snapshot for M3 SDK tools.
 *
 * <p>This target adapter follows MIndexFileStreamIndexer's one-stream admission:
 * raw bytes update SHA-256 as a strict UTF-8 decoder consumes them, followed by
 * a before/after metadata and byte-count check. It retains one ordinary String,
 * whose representation remains owned by the running JDK's String/M3String
 * implementation. It retains no raw-byte copy, token dictionary or postings.
 * It is not the complete donor MIndexFile token/occurrence plane.</p>
 *
 * <p>Path components are checked without following symbolic links, and the
 * final file is opened with NOFOLLOW_LINKS. The metadata check is not an atomic
 * filesystem snapshot or protection against concurrent ancestor replacement or
 * writes that restore metadata. The digests identify exactly the bytes and
 * characters observed. No file handle remains open after admission.</p>
 */
public final class M3File implements CharSequence {
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;

    private final Path path;
    private final URI uri;
    private final String text;
    private final String rawSha256;
    private final String utf16Sha256;
    private final Stamp stamp;

    private M3File(Path path, String text, String rawSha256, String utf16Sha256, Stamp stamp) {
        this.path = path;
        this.uri = path.toUri();
        this.text = text;
        this.rawSha256 = rawSha256;
        this.utf16Sha256 = utf16Sha256;
        this.stamp = stamp;
    }

    public static M3File load(Path path) throws IOException {
        return load(path, MAX_BYTES, ProgressMonitors.current());
    }

    public static M3File load(Path path, ProgressMonitor monitor) throws IOException {
        return load(path, MAX_BYTES, monitor);
    }

    public static M3File load(Path path, int maxBytes) throws IOException {
        return load(path, maxBytes, ProgressMonitors.current());
    }

    /** Decode once; malformed UTF-8, observed source changes and excess input refuse publication. */
    public static M3File load(Path path, int maxBytes, ProgressMonitor monitor) throws IOException {
        if (maxBytes < 0 || maxBytes > MAX_BYTES) {
            throw new IllegalArgumentException("file byte budget must be in [0," + MAX_BYTES + "]");
        }
        ProgressMonitor progress = progress(monitor);
        progress.checkCancelled();
        Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Stamp before = stamp(source);
        if (before.size() > maxBytes) throw new IOException("file exceeds byte budget: " + source);
        progress.begin("Read UTF-8 source", before.size());
        try {
            MessageDigest rawDigest = digest();
            MessageDigest characterDigest = digest();
            StringBuilder decoded = new StringBuilder((int) Math.min(before.size(), BUFFER_SIZE));
            ObservedInputStream bytes = new ObservedInputStream(
                    Files.newInputStream(source, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS),
                    rawDigest, maxBytes, progress);
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try (Reader reader = new InputStreamReader(bytes, decoder)) {
                char[] buffer = new char[BUFFER_SIZE];
                for (int count; (count = reader.read(buffer)) != -1;) {
                    progress.checkCancelled();
                    decoded.append(buffer, 0, count);
                    for (int i = 0; i < count; i++) updateUtf16(characterDigest, buffer[i]);
                }
            }
            Stamp after = stamp(source);
            requireStable(before, after, bytes.count);
            progress.checkCancelled();
            return new M3File(source, decoded.toString(), hex(rawDigest), hex(characterDigest), after);
        } finally {
            progress.done();
        }
    }

    public Path path() { return path; }
    public URI uri() { return uri; }
    public long byteLength() { return stamp.size(); }
    public String rawSha256() { return rawSha256; }
    /** SHA-256 of exact big-endian UTF-16 code units, distinct from the raw file digest. */
    public String utf16Sha256() { return utf16Sha256; }
    /** The same canonical public String reference; this accessor does not decode or copy. */
    public String text() { return text; }
    @Override public int length() { return text.length(); }
    @Override public char charAt(int index) { return text.charAt(index); }
    @Override public String toString() { return text; }
    @Override public Range subSequence(int start, int end) { return range(start, end); }

    public Range range(int start, int end) {
        return range(start, end, ProgressMonitors.current());
    }

    public Range range(int start, int end, ProgressMonitor monitor) {
        progress(monitor).checkCancelled();
        return new Range(this, start, end);
    }

    public void verifyUnchanged() throws IOException {
        verifyUnchanged(ProgressMonitors.current());
    }

    /** Explicitly reread and compare a file; the immutable snapshot itself never changes. */
    public void verifyUnchanged(ProgressMonitor monitor) throws IOException {
        ProgressMonitor progress = progress(monitor);
        progress.checkCancelled();
        Stamp before = stamp(path);
        if (!stamp.equals(before)) throw new IOException("file metadata differs from snapshot: " + path);
        progress.begin("Verify source bytes", before.size());
        try {
            MessageDigest rawDigest = digest();
            long count;
            try (ObservedInputStream bytes = new ObservedInputStream(
                    Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS),
                    rawDigest, (int) stamp.size(), progress)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (bytes.read(buffer, 0, buffer.length) != -1) progress.checkCancelled();
                count = bytes.count;
            }
            requireStable(before, stamp(path), count);
            if (!rawSha256.equals(hex(rawDigest))) {
                throw new IOException("file bytes differ from snapshot: " + path);
            }
            progress.checkCancelled();
        } finally {
            progress.done();
        }
    }

    /** A UTF-16 range shares the snapshot owner and does not retain another spelling store. */
    public record Range(M3File source, int start, int end) implements CharSequence {
        public Range {
            Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length());
        }

        @Override public int length() { return end - start; }
        @Override public char charAt(int index) {
            return source.charAt(start + Objects.checkIndex(index, length()));
        }
        @Override public Range subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length());
            return new Range(source, start + from, start + to);
        }
        /** Explicit String projection; ordinary range traversal does not call this method. */
        @Override public String toString() { return source.text.substring(start, end); }
        public String utf16Sha256() { return utf16Sha256(ProgressMonitors.current()); }
        public String utf16Sha256(ProgressMonitor monitor) {
            ProgressMonitor progress = progress(monitor);
            progress.checkCancelled();
            progress.begin("Hash UTF-16 range", length());
            try {
                MessageDigest digest = digest();
                for (int i = start; i < end; i++) {
                    if ((i - start) % BUFFER_SIZE == 0) progress.checkCancelled();
                    updateUtf16(digest, source.text.charAt(i));
                }
                progress.worked(length());
                progress.checkCancelled();
                return hex(digest);
            } finally {
                progress.done();
            }
        }
    }

    private record Stamp(long size, FileTime modified, FileTime created, Object fileKey) { }

    private static Stamp stamp(Path path) throws IOException {
        Path current = path.getRoot();
        if (current == null) throw new IOException("absolute file path required");
        for (int i = 0; i < path.getNameCount(); i++) {
            current = current.resolve(path.getName(i));
            BasicFileAttributes attributes = Files.readAttributes(
                    current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()) throw new IOException("symbolic source component: " + current);
            boolean leaf = i + 1 == path.getNameCount();
            if (!leaf && !attributes.isDirectory()) throw new IOException("non-directory source component: " + current);
            if (leaf) {
                if (!attributes.isRegularFile()) throw new IOException("source is not a regular file: " + path);
                return new Stamp(attributes.size(), attributes.lastModifiedTime(),
                        attributes.creationTime(), attributes.fileKey());
            }
        }
        throw new IOException("source is not a regular file: " + path);
    }

    private static void requireStable(Stamp before, Stamp after, long count) throws IOException {
        if (!before.equals(after) || count != before.size() || count != after.size()) {
            throw new IOException("source changed during one-stream admission");
        }
    }

    private static ProgressMonitor progress(ProgressMonitor monitor) {
        return monitor == null ? ProgressMonitors.current() : monitor;
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void updateUtf16(MessageDigest digest, char value) {
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static String hex(MessageDigest digest) { return HexFormat.of().formatHex(digest.digest()); }

    private static final class ObservedInputStream extends InputStream {
        private final InputStream input;
        private final MessageDigest digest;
        private final int budget;
        private final ProgressMonitor progress;
        private long count;

        ObservedInputStream(InputStream input, MessageDigest digest, int budget, ProgressMonitor progress) {
            this.input = input;
            this.digest = digest;
            this.budget = budget;
            this.progress = progress;
        }

        @Override public int read() throws IOException {
            progress.checkCancelled();
            int value = input.read();
            if (value >= 0) {
                count++;
                if (count > budget) throw new IOException("file grew beyond byte budget");
                digest.update((byte) value);
                progress.worked(1);
                progress.checkCancelled();
            }
            return value;
        }

        @Override public int read(byte[] buffer, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, buffer.length);
            if (length == 0) return 0;
            progress.checkCancelled();
            int allowed = (int) Math.min(length, (long) budget - count + 1L);
            int read = input.read(buffer, offset, allowed);
            if (read > 0) {
                count += read;
                if (count > budget) throw new IOException("file grew beyond byte budget");
                digest.update(buffer, offset, read);
                progress.worked(read);
                progress.checkCancelled();
            }
            return read;
        }

        @Override public void close() throws IOException { input.close(); }
    }
}
