// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Immutable application source snapshot over the incumbent SDK region and slice
 * owners. Exact original bytes and relative names are retained; decoding belongs
 * at the prompt boundary. The directory and backing must remain externally
 * immutable while open. This class provides no global M3File ownership or
 * portable power-failure transaction guarantee.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/9b4728b743ab12cf0c6eaedeb899af22dad1f983/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSourceImage.java blob 743e5bb54bc6; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class M3PmemSourceImage implements AutoCloseable {
    private static final long MAGIC = 0x4d33535243494d47L; // M3SRCIMG
    private static final int VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int MAX_PATH_BYTES = 4096;
    private static final int MAX_FILES = 1_000_000;
    private static final int MAX_BUFFER_BYTES = 16 * 1024 * 1024;
    private static final String BACKING = "source.bin";
    private static final String MANIFEST = "manifest.bin";

    /** Hard admission budgets, checked before corpus/index allocation or reading. */
    public record Limits(int maxFiles, long maxCorpusBytes, int maxFileBytes, int ioBufferBytes) {
        public Limits {
            if (maxFiles < 0 || maxFiles > MAX_FILES || maxCorpusBytes < 0
                    || maxFileBytes < 0 || maxFileBytes > MAX_BUFFER_BYTES
                    || ioBufferBytes <= 0 || ioBufferBytes > MAX_BUFFER_BYTES)
                throw new IllegalArgumentException("invalid source-image limits (files <= 1000000; file/IO <= 16MiB)");
        }
    }

    private final M3PmemRegion region;
    private final M3PmemSourceSlices slices;
    private final String[] paths;
    private final int[] pathSlots;
    private final long[] offsets;
    private final long corpusBytes;
    private final int ioBufferBytes;
    private final String root;
    private boolean closed;

    private M3PmemSourceImage(M3PmemRegion region, String[] paths, long[] offsets,
                            int[] lengths, byte[] digests, long corpusBytes,
                            String root, Limits limits) {
        this.region = region;
        this.slices = new M3PmemSourceSlices(region, offsets, lengths, digests,
                Math.max(1, limits.maxFileBytes()));
        this.paths = paths;
        this.pathSlots = M3PmemSourcePathIndex.build(paths);
        this.offsets = offsets;
        this.corpusBytes = corpusBytes;
        this.ioBufferBytes = limits.ioBufferBytes();
        this.root = root;
    }

    /**
     * Pack explicitly selected regular files in deterministic Java lexical order.
     * Destination must not exist. Incomplete captures deliberately retain their
     * directory without a committed manifest, so open refuses them. The manifest
     * is installed last after forcing source and manifest bytes. Source changes
     * are detected through exact rehashes and file attributes; callers must
     * coordinate writers for a repository-wide atomic capture.
     */
    public static void pack(Path repositoryRoot, List<String> relativePaths,
                            Path destination, Limits limits) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(relativePaths, "relativePaths");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(limits, "limits");
        if (relativePaths.size() > limits.maxFiles()) throw new IOException("source file count exceeds budget");
        List<String> selected = new ArrayList<>(relativePaths);
        if (selected.size() > limits.maxFiles()) throw new IOException("source file count exceeds budget");
        for (String path : selected) validatePath(path);
        selected.sort(String::compareTo);
        for (int i = 1; i < selected.size(); i++)
            if (selected.get(i - 1).equals(selected.get(i)))
                throw new IllegalArgumentException("duplicate source path: " + selected.get(i));
        Path repository = repositoryRoot.toAbsolutePath().normalize();
        rejectSymlinkComponents(repository);
        if (!Files.isDirectory(repository, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("repository root is not a directory");
        int count = selected.size();
        BasicFileAttributes[] captured = new BasicFileAttributes[count];
        long[] offsets = new long[count];
        int[] lengths = new int[count];
        byte[] digests = new byte[count * DIGEST_BYTES];
        long total = 0;
        for (int i = 0; i < count; i++) {
            BasicFileAttributes attributes = sourceAttributes(repository, selected.get(i));
            long length = attributes.size();
            if (length > limits.maxFileBytes() || length > limits.maxCorpusBytes() - total)
                throw new IOException("source byte budget exceeded at " + selected.get(i));
            captured[i] = attributes;
            offsets[i] = total;
            lengths[i] = (int) length;
            total += length;
        }
        Files.createDirectory(destination);
        byte[] buffer = new byte[limits.ioBufferBytes()];
        try (FileChannel output = FileChannel.open(destination.resolve(BACKING),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            for (int i = 0; i < count; i++) {
                byte[] digest = streamSource(repository, selected.get(i), lengths[i], captured[i], buffer, output);
                System.arraycopy(digest, 0, digests, i * DIGEST_BYTES, DIGEST_BYTES);
            }
            if (total == 0) writeFully(output, ByteBuffer.wrap(new byte[]{0}));
            output.force(true);
        }
        // Recheck the whole selected set after capture; a same-size edit to an
        // earlier file must not silently become part of a different snapshot.
        for (int i = 0; i < count; i++) {
            byte[] actual = streamSource(repository, selected.get(i), lengths[i], captured[i], buffer, null);
            if (!digestEquals(digests, i, actual))
                throw new IOException("source changed during capture: " + selected.get(i));
        }
        Path pending = destination.resolve("manifest.pending");
        try (FileChannel channel = FileChannel.open(pending, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             BufferedOutputStream buffered = new BufferedOutputStream(Channels.newOutputStream(channel), limits.ioBufferBytes());
             DigestOutputStream sealed = new DigestOutputStream(buffered, sha256());
             DataOutputStream output = new DataOutputStream(sealed)) {
            output.writeLong(MAGIC); output.writeInt(VERSION); output.writeInt(count); output.writeLong(total);
            for (int i = 0; i < count; i++) {
                byte[] path = selected.get(i).getBytes(StandardCharsets.UTF_8);
                output.writeInt(path.length); output.write(path);
                output.writeLong(offsets[i]); output.writeInt(lengths[i]);
                output.write(digests, i * DIGEST_BYTES, DIGEST_BYTES);
            }
            byte[] checksum = sealed.getMessageDigest().digest();
            sealed.on(false); output.write(checksum); output.flush(); channel.force(true);
        }
        Files.move(pending, destination.resolve(MANIFEST), StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Verify version, checksum, bounded primitive geometry and every file digest
     * before admitting a read-only region. No corpus-sized heap buffer is used.
     */
    public static M3PmemSourceImage open(Path snapshotDirectory, M3PmemBackend backend,
                                       Limits limits) throws IOException {
        Objects.requireNonNull(snapshotDirectory, "snapshotDirectory");
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(limits, "limits");
        Path directory = snapshotDirectory.toAbsolutePath().normalize();
        rejectSymlinkComponents(directory);
        Path manifest = directory.resolve(MANIFEST), backing = directory.resolve(BACKING);
        BasicFileAttributes manifestAttributes = regularAttributes(manifest);
        BasicFileAttributes backingAttributes = regularAttributes(backing);
        long manifestSize = manifestAttributes.size();
        long maximumManifestBytes = 56L + limits.maxFiles() * (48L + MAX_PATH_BYTES);
        if (manifestSize < 56 || manifestSize > maximumManifestBytes)
            throw new IOException("source manifest size exceeds geometry/budget");
        String[] paths;
        long[] offsets;
        int[] lengths;
        byte[] digests;
        long total;
        String root;
        try (FileChannel channel = FileChannel.open(manifest, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             BufferedInputStream buffered = new BufferedInputStream(Channels.newInputStream(channel), limits.ioBufferBytes());
             DigestInputStream sealed = new DigestInputStream(buffered, sha256());
             DataInputStream input = new DataInputStream(sealed)) {
            if (input.readLong() != MAGIC || input.readInt() != VERSION)
                throw new IOException("unsupported source manifest magic/version");
            int count = input.readInt(); total = input.readLong();
            if (count < 0 || count > limits.maxFiles() || count > (manifestSize - 56) / 49
                    || total < 0 || total > limits.maxCorpusBytes())
                throw new IOException("source manifest count/byte budget exceeded");
            paths = new String[count]; offsets = new long[count]; lengths = new int[count];
            digests = new byte[count * DIGEST_BYTES];
            long expectedOffset = 0;
            long consumed = 24;
            for (int i = 0; i < count; i++) {
                int pathLength = input.readInt();
                if (pathLength <= 0 || pathLength > MAX_PATH_BYTES
                        || pathLength > manifestSize - consumed - 80)
                    throw new IOException("invalid source manifest path length");
                byte[] encoded = new byte[pathLength]; input.readFully(encoded);
                paths[i] = decodePath(encoded);
                try { validatePath(paths[i]); }
                catch (IllegalArgumentException failure) { throw new IOException("unsafe manifest source path", failure); }
                if (i > 0 && paths[i - 1].compareTo(paths[i]) >= 0)
                    throw new IOException("source manifest paths are not unique and sorted");
                offsets[i] = input.readLong(); lengths[i] = input.readInt();
                if (offsets[i] != expectedOffset || lengths[i] < 0 || lengths[i] > limits.maxFileBytes()
                        || lengths[i] > total - expectedOffset)
                    throw new IOException("invalid source extent geometry at " + i);
                expectedOffset += lengths[i];
                input.readFully(digests, i * DIGEST_BYTES, DIGEST_BYTES);
                consumed += 48L + pathLength;
            }
            if (expectedOffset != total || consumed + DIGEST_BYTES != manifestSize)
                throw new IOException("source manifest total/trailing geometry mismatch");
            byte[] actual = sealed.getMessageDigest().digest();
            sealed.on(false);
            byte[] expected = new byte[DIGEST_BYTES]; input.readFully(expected);
            if (!MessageDigest.isEqual(expected, actual) || input.read() != -1)
                throw new IOException("source manifest checksum/trailing data mismatch");
            root = HexFormat.of().formatHex(actual);
        }
        long expectedBackingSize = Math.max(1L, total);
        if (backingAttributes.size() != expectedBackingSize)
            throw new IOException("source backing size mismatch");
        M3PmemRegion region = M3PmemSdk.open(backing, 0, expectedBackingSize, M3PmemAccess.READ_ONLY, backend);
        try {
            byte[] buffer = new byte[limits.ioBufferBytes()];
            if (total == 0) {
                region.read(0, buffer, 0, 1);
                if (buffer[0] != 0) throw new IOException("invalid empty source backing padding");
            }
            for (int i = 0; i < paths.length; i++) {
                MessageDigest digest = sha256();
                long cursor = offsets[i]; int remaining = lengths[i];
                while (remaining > 0) {
                    int amount = Math.min(buffer.length, remaining);
                    region.read(cursor, buffer, 0, amount); digest.update(buffer, 0, amount);
                    cursor += amount; remaining -= amount;
                }
                if (!digestEquals(digests, i, digest.digest()))
                    throw new IOException("source backing SHA-256 drift at " + paths[i]);
            }
            return new M3PmemSourceImage(region, paths, offsets, lengths, digests, total, root, limits);
        } catch (IOException | RuntimeException | Error failure) {
            try { region.close(); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public synchronized int fileCount() { requireOpen(); return slices.fileCount(); }
    public synchronized String relativePathAt(int fileIndex) { requireOpen(); return paths[fileIndex]; }
    public synchronized int lengthAt(int fileIndex) { requireOpen(); return slices.lengthAt(fileIndex); }
    public synchronized String sha256At(int fileIndex) { requireOpen(); return slices.sha256At(fileIndex); }
    /** Content identity of the versioned directory and exact per-file digests. */
    public synchronized String sourceRoot() { requireOpen(); return root; }
    public synchronized byte[] readSealed(int fileIndex) { requireOpen(); return slices.readSealed(fileIndex); }

    /** Source-verified caller-owned destination; preserves original JNI/FILE leases. */
    public synchronized int readSealed(int fileIndex, byte[] target, int arrayOffset) {
        requireOpen();
        return slices.readSealed(fileIndex, target, arrayOffset);
    }

    /**
     * Verify one source file against the immutable image digest using a
     * caller-owned reusable buffer, without allocating a file-sized array.
     * Serial callers may reuse the buffer across the entire source corpus.
     * The caller must keep the backing immutable; this is not a write lock.
     * Even empty files acquire the original region read/close lease.
     */
    public synchronized int verifySealed(int fileIndex, byte[] scratch) {
        requireOpen();
        Objects.requireNonNull(scratch, "scratch");
        if (scratch.length == 0 || scratch.length > MAX_BUFFER_BYTES)
            throw new IllegalArgumentException("verification scratch must be 1..16 MiB");
        int length = slices.lengthAt(fileIndex);
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        long start = offsets[fileIndex];
        if (length == 0) region.read(start, scratch, 0, 0);
        for (int done = 0; done < length;) {
            int amount = Math.min(scratch.length, length - done);
            region.read(start + done, scratch, 0, amount);
            digest.update(scratch, 0, amount);
            done += amount;
        }
        byte[] expected = HexFormat.of().parseHex(slices.sha256At(fileIndex));
        if (!MessageDigest.isEqual(digest.digest(), expected))
            throw new SecurityException("M3File source image SHA-256 drift at " + fileIndex);
        return length;
    }
    public synchronized int fileIndexOf(String relativePath) {
        requireOpen(); Objects.requireNonNull(relativePath, "relativePath");
        return M3PmemSourcePathIndex.find(paths, pathSlots, relativePath);
    }
    /** Search exactly one file extent; result is file-relative, including empty matches. */
    public synchronized long indexOf(int fileIndex, byte[] pattern) {
        requireOpen(); int length = slices.lengthAt(fileIndex);
        long found = M3PmemSearch.indexOf(region, offsets[fileIndex], length, pattern);
        return found < 0 ? -1 : found - offsets[fileIndex];
    }
    /** Touch at most maxBytes of corpus using a bounded buffer; no residency promise. */
    public synchronized long warm(long maxBytes) throws IOException {
        requireOpen();
        if (maxBytes < 0) throw new IllegalArgumentException("negative warm byte budget");
        long end = Math.min(maxBytes, corpusBytes);
        if (end == 0) return 0;
        byte[] buffer = new byte[(int) Math.min(ioBufferBytes, end)];
        long cursor = 0;
        while (cursor < end) {
            int amount = (int) Math.min(buffer.length, end - cursor);
            region.read(cursor, buffer, 0, amount); cursor += amount;
        }
        return cursor;
    }
    @Override public synchronized void close() throws IOException {
        if (!closed) { closed = true; region.close(); }
    }

    private void requireOpen() {
        if (closed || !region.isOpen()) throw new IllegalStateException("source image is closed");
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("JDK SHA-256 unavailable", impossible); }
    }
    private static boolean digestEquals(byte[] lane, int index, byte[] actual) {
        int differences = 0, base = index * DIGEST_BYTES;
        for (int i = 0; i < DIGEST_BYTES; i++) differences |= lane[base + i] ^ actual[i];
        return differences == 0;
    }
    private static void validatePath(String path) {
        Objects.requireNonNull(path, "relative source path");
        if (path.isEmpty() || path.length() > MAX_PATH_BYTES || path.indexOf('\\') >= 0
                || path.indexOf('\0') >= 0 || path.indexOf(':') >= 0 || path.startsWith("/") || Path.of(path).isAbsolute()
                || (path.length() > 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':' && path.charAt(2) == '/'))
            throw new IllegalArgumentException("unsafe relative source path: " + path);
        for (String part : path.split("/", -1))
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("unsafe relative source path: " + path);
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(path));
            if (encoded.remaining() > MAX_PATH_BYTES) throw new IllegalArgumentException("source path exceeds byte budget");
        } catch (CharacterCodingException failure) { throw new IllegalArgumentException("invalid Unicode source path", failure); }
    }
    private static String decodePath(byte[] encoded) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(encoded)).toString();
        } catch (CharacterCodingException failure) { throw new IOException("invalid UTF-8 manifest path", failure); }
    }
    private static void rejectSymlinkComponents(Path absolute) throws IOException {
        Path cursor = absolute.getRoot();
        for (Path part : absolute) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("symbolic links are not admitted: " + cursor);
        }
    }
    private static BasicFileAttributes regularAttributes(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink())
            throw new IOException("not a regular non-symlink file: " + path);
        return attributes;
    }
    private static BasicFileAttributes sourceAttributes(Path repository, String relative) throws IOException {
        Path source = repository.resolve(relative);
        rejectSymlinkComponents(source);
        return regularAttributes(source);
    }
    private static void checkStable(BasicFileAttributes expected, BasicFileAttributes actual, String relative) throws IOException {
        if (expected.size() != actual.size() || !expected.lastModifiedTime().equals(actual.lastModifiedTime())
                || !Objects.equals(expected.fileKey(), actual.fileKey()))
            throw new IOException("source changed during capture: " + relative);
    }
    private static byte[] streamSource(Path repository, String relative, int length,
                                        BasicFileAttributes captured, byte[] buffer,
                                        FileChannel output) throws IOException {
        checkStable(captured, sourceAttributes(repository, relative), relative);
        MessageDigest digest = sha256();
        try (FileChannel input = FileChannel.open(repository.resolve(relative), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int remaining = length;
            while (remaining > 0) {
                int read = input.read(ByteBuffer.wrap(buffer, 0, Math.min(buffer.length, remaining)));
                if (read < 0) throw new IOException("source truncated during capture: " + relative);
                if (read == 0) continue;
                digest.update(buffer, 0, read);
                if (output != null) writeFully(output, ByteBuffer.wrap(buffer, 0, read));
                remaining -= read;
            }
            if (input.read(ByteBuffer.wrap(buffer, 0, 1)) != -1)
                throw new IOException("source grew during capture: " + relative);
        }
        checkStable(captured, sourceAttributes(repository, relative), relative);
        return digest.digest();
    }
    private static void writeFully(FileChannel channel, ByteBuffer bytes) throws IOException {
        while (bytes.hasRemaining()) channel.write(bytes);
    }
}
