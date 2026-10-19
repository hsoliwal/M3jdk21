// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable artifact custody and serial root publication for existing MIndex images.
 * This is NOT a new dictionary/AST codec, a mounted filesystem, or a global interner.
 * References bind namespace, image format and dictionary identity; raw IDs inside
 * artifacts are untouched. Readers may run in other processes. Exactly one writer
 * owns the directory. The directory must be private to cooperating store processes:
 * arbitrary external writers and hostile ancestor-directory replacement are out of scope.
 *
 * Durability requires atomic same-directory rename and directory fsync. Unsupported
 * filesystems fail closed. After a post-rename fsync error, publication is uncertain;
 * reopen/read the head instead of blindly retrying an increment. Orphans are retained.
 */
public final class M3SnapshotStore implements AutoCloseable {
    public static final String NO_DICTIONARY = "0".repeat(64);
    // Opening then closing a second descriptor can release this JVM's OS-level
    // record locks. Reject duplicate JVM writers BEFORE opening the lock file.
    private static final Object WRITERS = new Object();
    private static Path[] writerPaths = new Path[4];
    private static boolean registerWriter(Path path) {
        for (Path value : writerPaths) if (path.equals(value)) return false;
        for (int i = 0; i < writerPaths.length; i++) if (writerPaths[i] == null) {
            writerPaths[i] = path; return true;
        }
        int at = writerPaths.length;
        writerPaths = Arrays.copyOf(writerPaths, at * 2); writerPaths[at] = path; return true;
    }
    private static void unregisterWriter(Path path) {
        for (int i = 0; i < writerPaths.length; i++) if (path.equals(writerPaths[i])) {
            writerPaths[i] = null; return;
        }
    }
    private final Path writerKey;
    private static final int MAGIC = 0x4d495348;
    private static final int MAX_HEAD_BYTES = 1024;
    private final Path directory;
    private final long namespaceMost;
    private final long namespaceLeast;
    private final int maxBlobBytes;
    private final FileChannel lockChannel;
    private final FileLock writerLock;
    private boolean closed;

    public record BlobRef(UUID namespace, String format, String dictionarySha256,
            String sha256, int length) {
        public BlobRef {
            Objects.requireNonNull(namespace, "namespace");
            format = name(format); dictionarySha256 = digest(dictionarySha256); sha256 = digest(sha256);
            if (length < 0) throw new IllegalArgumentException("length");
        }
    }
    public record Snapshot(String name, long generation, BlobRef root) {
        public Snapshot {
            name = M3SnapshotStore.name(name); Objects.requireNonNull(root, "root");
            if (generation < 1) throw new IllegalArgumentException("generation");
        }
    }

    private M3SnapshotStore(Path path, int limit, boolean writable) throws IOException {
        if (limit < 1) throw new IllegalArgumentException("maxBlobBytes");
        directory = path.toAbsolutePath().normalize(); maxBlobBytes = limit;
        FileChannel channel = null; FileLock lock = null; Path registered = null;
        try {
            if (writable && !Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                requireDirectory(directory.getParent());
                Files.createDirectory(directory); syncDirectory(directory.getParent());
            }
            requireDirectory(directory);
            if (writable) {
                Path key = directory.toRealPath();
                synchronized (WRITERS) {
                    if (!registerWriter(key)) throw new IOException("writer already open in this JVM");
                }
                registered = key;
                channel = FileChannel.open(directory.resolve("writer.lock"), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                try { lock = channel.tryLock(); }
                catch (OverlappingFileLockException busy) { throw new IOException("writer already open", busy); }
                if (lock == null) throw new IOException("writer already open");
                for (String child : new String[] {"objects", "heads"}) {
                    Path dir = directory.resolve(child);
                    if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(dir);
                    requireDirectory(dir);
                }
                Path identity = directory.resolve("namespace");
                if (!Files.exists(identity, LinkOption.NOFOLLOW_LINKS))
                    atomicWrite(identity, (UUID.randomUUID() + "\n").getBytes(StandardCharsets.US_ASCII));
                syncDirectory(directory);
            }
            requireDirectory(directory.resolve("objects")); requireDirectory(directory.resolve("heads"));
            byte[] id = readSmall(directory.resolve("namespace"), 37);
            if (id.length != 37 || id[36] != '\n') throw new IOException("invalid store namespace");
            long most = 0, least = 0;
            int digits = 0;
            for (int i = 0; i < 36; i++) {
                if (i == 8 || i == 13 || i == 18 || i == 23) {
                    if (id[i] != '-') throw new IOException("invalid store namespace");
                    continue;
                }
                int c = Byte.toUnsignedInt(id[i]);
                int nibble = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'f' ? c - 'a' + 10 : -1;
                if (nibble < 0) throw new IOException("invalid store namespace");
                if (digits++ < 16) most = (most << 4) | nibble; else least = (least << 4) | nibble;
            }
            namespaceMost = most; namespaceLeast = least;
            lockChannel = channel; writerLock = lock; writerKey = registered;
        } catch (IOException | RuntimeException failure) {
            if (lock != null) try { lock.release(); } catch (IOException suppressed) { failure.addSuppressed(suppressed); }
            if (channel != null) try { channel.close(); } catch (IOException suppressed) { failure.addSuppressed(suppressed); }
            if (registered != null) synchronized (WRITERS) { unregisterWriter(registered); }
            throw failure;
        }
    }

    public static M3SnapshotStore writer(Path path, int maxBlobBytes) throws IOException {
        return new M3SnapshotStore(Objects.requireNonNull(path), maxBlobBytes, true);
    }
    public static M3SnapshotStore reader(Path path, int maxBlobBytes) throws IOException {
        return new M3SnapshotStore(Objects.requireNonNull(path), maxBlobBytes, false);
    }
    public UUID namespace() { return new UUID(namespaceMost, namespaceLeast); }

    /** Copies input once at admission; equal existing bytes reuse the persistent reference. */
    public synchronized BlobRef put(String format, String dictionarySha256, ByteBuffer input,
            M3FsProgress supplied) throws IOException {
        writable(); name(format); digest(dictionarySha256);
        Objects.requireNonNull(input, "input");
        M3FsProgress monitor = monitor(supplied); monitor.checkCanceled();
        if (input.remaining() > maxBlobBytes) throw new IllegalArgumentException("blob budget exceeded");
        byte[] bytes = new byte[input.remaining()]; input.duplicate().get(bytes);
        String hash = sha256(bytes);
        
        Path shard = directory.resolve("objects").resolve(hash.substring(0, 2));
        if (!Files.exists(shard, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(shard); syncDirectory(directory.resolve("objects"));
        }
        requireDirectory(shard); Path object = shard.resolve(hash + ".blob");
        monitor.checkCanceled();
        if (Files.exists(object, LinkOption.NOFOLLOW_LINKS)) {
            byte[] existing = readSmall(object, maxBlobBytes);
            if (!Arrays.equals(bytes, existing)) throw new IOException("digest collision or corrupted object");
        } else atomicWrite(object, bytes);
        monitor.worked(bytes.length);
        return new BlobRef(namespace(), format, dictionarySha256, hash, bytes.length);
    }

    /** Exact verification precedes exposing an immutable mapped image. Mapping lifetime is JVM-managed. */
    public synchronized ByteBuffer map(BlobRef ref, M3FsProgress supplied) throws IOException {
        open(); validate(ref);
        M3FsProgress monitor = monitor(supplied);
        try (FileChannel file = FileChannel.open(object(ref), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            verify(file, ref, monitor);
            if (ref.length() == 0) return ByteBuffer.allocateDirect(0).asReadOnlyBuffer();
            return file.map(FileChannel.MapMode.READ_ONLY, 0, ref.length()).asReadOnlyBuffer();
        }
    }

    public synchronized Optional<Snapshot> head(String snapshotName) throws IOException {
        open(); Path path = directory.resolve("heads").resolve(name(snapshotName) + ".head");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        byte[] bytes = readSmall(path, MAX_HEAD_BYTES);
        if (bytes.length < 32) throw new IOException("truncated head");
        int payloadLength = bytes.length - 32;
        if (!MessageDigest.isEqual(hash().digest(Arrays.copyOf(bytes, payloadLength)),
                Arrays.copyOfRange(bytes, payloadLength, bytes.length))) throw new IOException("head checksum");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, payloadLength))) {
            if (in.readInt() != MAGIC || in.readInt() != 1) throw new IOException("head format");
            long generation = in.readLong(); UUID scope = new UUID(in.readLong(), in.readLong());
            BlobRef ref = new BlobRef(scope, in.readUTF(), in.readUTF(), in.readUTF(), in.readInt());
            if (in.read() != -1) throw new IOException("trailing head data");
            validate(ref); return Optional.of(new Snapshot(snapshotName, generation, ref));
        } catch (IllegalArgumentException malformed) { throw new IOException("invalid head", malformed); }
    }

    /** Internal CAS admission decodes scalars, never Snapshot/BlobRef/UUID API values. */
    private long currentGeneration(String snapshotName) throws IOException {
        Path path = directory.resolve("heads").resolve(name(snapshotName) + ".head");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return 0;
        byte[] bytes = readSmall(path, MAX_HEAD_BYTES);
        if (bytes.length < 32) throw new IOException("truncated head");
        int length = bytes.length - 32;
        MessageDigest checksum = hash(); checksum.update(bytes, 0, length);
        byte[] actual = checksum.digest();
        if (!Arrays.equals(actual, 0, 32, bytes, length, bytes.length)) throw new IOException("head checksum");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, length))) {
            if (in.readInt() != MAGIC || in.readInt() != 1) throw new IOException("head format");
            long generation = in.readLong();
            if (generation < 1 || in.readLong() != namespaceMost || in.readLong() != namespaceLeast)
                throw new IOException("invalid head generation or namespace");
            name(in.readUTF()); digest(in.readUTF()); digest(in.readUTF());
            int size = in.readInt();
            if (size < 0 || size > maxBlobBytes || in.read() != -1) throw new IOException("invalid head geometry");
            return generation;
        } catch (IllegalArgumentException malformed) { throw new IOException("invalid head", malformed); }
    }

    /** Compare expected generation, verify the image, then publish exactly one new root. */
    public synchronized Snapshot publish(String snapshotName, long expectedGeneration, BlobRef root,
            M3FsProgress supplied) throws IOException {
        writable(); validate(root);
        if (expectedGeneration < 0) throw new IllegalArgumentException("expectedGeneration");
        M3FsProgress monitor = monitor(supplied); monitor.checkCanceled();
        long current = currentGeneration(snapshotName);
        if (current != expectedGeneration) throw new IOException("stale snapshot generation");
        try (FileChannel file = FileChannel.open(object(root), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            verify(file, root, monitor);
        }
        long next = Math.incrementExact(current);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC); out.writeInt(1); out.writeLong(next);
            out.writeLong(namespaceMost); out.writeLong(namespaceLeast);
            out.writeUTF(root.format()); out.writeUTF(root.dictionarySha256()); out.writeUTF(root.sha256());
            out.writeInt(root.length());
        }
        byte[] payload = buffer.toByteArray(); buffer.writeBytes(hash().digest(payload));
        monitor.checkCanceled();
        // Cancellation stops before the commit point; never report cancellation after a committed rename.
        atomicWrite(directory.resolve("heads").resolve(name(snapshotName) + ".head"), buffer.toByteArray());
        return new Snapshot(snapshotName, next, root);
    }

    /** Explicit reference sharing: no payload serialization or ID rewriting. */
    public synchronized Snapshot cloneSnapshot(Snapshot source, String target, long expectedGeneration,
            M3FsProgress monitor) throws IOException {
        return publish(target, expectedGeneration, Objects.requireNonNull(source).root(), monitor);
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return; closed = true;
        if (writerLock != null) {
            try {
                try { writerLock.release(); } finally { lockChannel.close(); }
            } finally { synchronized (WRITERS) { unregisterWriter(writerKey); } }
        }
    }

    private void validate(BlobRef ref) {
        Objects.requireNonNull(ref, "ref");
        if (namespaceMost != ref.namespace().getMostSignificantBits()
                || namespaceLeast != ref.namespace().getLeastSignificantBits())
            throw new IllegalArgumentException("foreign namespace");
        if (ref.length() > maxBlobBytes) throw new IllegalArgumentException("blob budget exceeded");
    }
    private Path object(BlobRef ref) throws IOException {
        Path shard = directory.resolve("objects").resolve(ref.sha256().substring(0, 2));
        requireDirectory(shard); return shard.resolve(ref.sha256() + ".blob");
    }
    private void open() { if (closed) throw new IllegalStateException("store closed"); }
    private void writable() {
        open(); if (writerLock == null) throw new IllegalStateException("read-only store");
    }
    private static M3FsProgress monitor(M3FsProgress value) {
        return value == null ? M3FsProgress.noop() : value;
    }
    private static void verify(FileChannel file, BlobRef ref, M3FsProgress monitor) throws IOException {
        monitor.checkCanceled();
        if (file.size() != ref.length()) throw new IOException("object length mismatch");
        MessageDigest digest = hash(); ByteBuffer block = ByteBuffer.allocate(65536);
        while (file.position() < ref.length()) {
            monitor.checkCanceled(); block.clear();
            int read = file.read(block);
            if (read <= 0) throw new IOException("object read made no progress");
            block.flip(); digest.update(block); monitor.worked(read);
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(ref.sha256())) throw new IOException("object checksum");
        monitor.checkCanceled();
    }
    private static byte[] readSmall(Path path, int limit) throws IOException {
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long length = file.size();
            if (length > limit) throw new IOException("file budget exceeded");
            ByteBuffer data = ByteBuffer.allocate(Math.toIntExact(length));
            while (data.hasRemaining()) if (file.read(data) <= 0) throw new IOException("truncated file");
            if (file.size() != length) throw new IOException("file changed while reading");
            return data.array();
        }
    }
    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), ".mindex-", ".tmp");
        try {
            try (FileChannel file = FileChannel.open(temp, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) if (file.write(buffer) <= 0) throw new IOException("write stalled");
                file.force(true);
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            syncDirectory(target.getParent());
        } finally { Files.deleteIfExists(temp); }
    }
    private static void syncDirectory(Path path) throws IOException {
        try (FileChannel dir = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            dir.force(true);
        }
    }
    private static void requireDirectory(Path path) throws IOException {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("not a real directory: " + path);
    }
    private static String name(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))
            throw new IllegalArgumentException("unsafe or empty name");
        return value;
    }
    private static String digest(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("SHA-256");
        return value;
    }
    private static String sha256(byte[] bytes) { return HexFormat.of().formatHex(hash().digest(bytes)); }
    private static MessageDigest hash() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
