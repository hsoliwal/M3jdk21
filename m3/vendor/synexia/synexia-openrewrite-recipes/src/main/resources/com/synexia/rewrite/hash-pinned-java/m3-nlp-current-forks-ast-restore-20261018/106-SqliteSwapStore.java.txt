// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.swap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Optional direct SQLite JNI store; no JDBC driver or eager native initialization in other runtime
 * classes. The shared Synexia JNI loader resolves libsynexia_mindex_swap through java.library.path,
 * system properties, or loadLibrary(Path). Each instance owns one connection and one immutable
 * namespace/codec binding.
 */
public final class SqliteSwapStore implements SwapStore {
    private static final int HEADER = 48, MAGIC = 0x53585031;
    public enum Journal { SAFE_AUTO, WAL, DELETE }
    public record Options(int recordBytes, int batchBytes, int batchRecords, int busyMillis,
                          int pageCacheKiB, Journal journal) {
        public Options(int recordBytes, int batchBytes, int batchRecords, int busyMillis, int pageCacheKiB) {
            this(recordBytes, batchBytes, batchRecords, busyMillis, pageCacheKiB, Journal.SAFE_AUTO);
        }
        public Options {
            Objects.requireNonNull(journal);
            if (recordBytes < 1 || recordBytes > 268_435_456 || batchBytes < recordBytes
                    || batchBytes > 1_073_741_824 || batchRecords < 1 || batchRecords > 4096
                    || busyMillis < 0 || busyMillis > 600_000
                    || pageCacheKiB < 64 || pageCacheKiB > 1_048_576) {
                throw new IllegalArgumentException("invalid SQLite resource limits");
            }
        }
        public static Options defaults() { return new Options(16 << 20, 64 << 20, 256, 5000, 2048); }
    }
    public static final class StalePublicationException extends IOException {
        private static final long serialVersionUID = 1L;
        StalePublicationException() { super("stale head revision; transaction rolled back"); }
    }
    private final Options options;
    private long handle;

    public static void loadLibrary(Path absoluteLibrary) {
        SwapNativeLibrary.load(absoluteLibrary);
    }
    private static void ensureLibrary() {
        SwapNativeLibrary.load();
    }
    public static SqliteSwapStore open(Path database, String namespace, String codec, Options options)
            throws IOException {
        Objects.requireNonNull(database); Objects.requireNonNull(options);
        SwapStore.requireName(namespace); SwapStore.requireName(codec); ensureLibrary();
        byte[] path = utf8(database.toAbsolutePath().normalize().toString());
        long handle = nOpen(path, ascii(namespace), ascii(codec), options.recordBytes() + HEADER,
                options.batchBytes() + HEADER * options.batchRecords(), options.batchRecords(),
                options.busyMillis(), options.pageCacheKiB(), options.journal().ordinal());
        if (handle <= 0) throw new IOException("native open returned invalid handle");
        return new SqliteSwapStore(handle, options);
    }
    private SqliteSwapStore(long handle, Options options) { this.handle = handle; this.options = options; }
    @Override public int maxRecordBytes() { return options.recordBytes(); }
    @Override public int maxBatchBytes() { return options.batchBytes(); }
    @Override public int maxBatchRecords() { return options.batchRecords(); }
    @Override public synchronized byte[] read(long id) throws IOException {
        checkOpen(); SwapStore.requireId(id);
        byte[] frame = nRead(handle, id);
        return frame == null ? null : unframe(id, frame, options.recordBytes());
    }
    @Override public synchronized void write(Map<Long, byte[]> records, Publication publication)
            throws IOException {
        checkOpen(); Objects.requireNonNull(records);
        if (records.size() > options.batchRecords()) throw new IOException("batch record limit");
        TreeMap<Long, byte[]> sorted = new TreeMap<>();
        long total = 0;
        for (var e : records.entrySet()) {
            long id = Objects.requireNonNull(e.getKey()); SwapStore.requireId(id);
            byte[] value = Objects.requireNonNull(e.getValue());
            if (value.length > options.recordBytes()) throw new IOException("record limit");
            total += value.length;
            if (total > options.batchBytes()) throw new IOException("batch byte limit");
            sorted.put(id, frame(id, value));
        }
        long[] ids = new long[sorted.size()];
        byte[][] frames = new byte[sorted.size()][];
        int index = 0;
        for (var e : sorted.entrySet()) { ids[index] = e.getKey(); frames[index++] = e.getValue(); }
        byte[] headName = publication == null ? null : ascii(publication.name());
        long revision = nWrite(handle, ids, frames, headName,
                publication == null ? 0 : publication.expectedRevision(),
                publication == null ? 0 : publication.recordId());
        if (revision < 0) throw new StalePublicationException();
    }
    @Override public synchronized Head head(String name) throws IOException {
        checkOpen(); long[] h = nHead(handle, ascii(SwapStore.requireName(name)));
        if (h == null || h.length != 2) throw new IOException("invalid native head");
        try { return new Head(h[0], h[1]); }
        catch (IllegalArgumentException e) { throw new IOException("corrupt head", e); }
    }
    @Override public synchronized long[] ids(long afterExclusive, int limit) throws IOException {
        checkOpen();
        if (afterExclusive < -1 || limit < 1 || limit > options.batchRecords()) {
            throw new IllegalArgumentException("invalid enumeration bounds");
        }
        return nIds(handle, afterExclusive, limit);
    }
    @Override public synchronized long count() throws IOException { checkOpen(); return nCount(handle); }
    @Override public boolean supportsUnreferencedPrune() { return true; }
    @Override public synchronized int pruneUnreferencedBefore(long beforeExclusive, int maxDeletes)
            throws IOException {
        checkOpen();
        if (beforeExclusive < 0L || maxDeletes < 1 || maxDeletes > options.batchRecords()) {
            throw new IllegalArgumentException("invalid prune bounds");
        }
        return nPrune(handle, beforeExclusive, maxDeletes);
    }
    /** PASSIVE checkpoint; returns SQLite's busy flag, log pages and checkpointed pages. */
    public synchronized int[] checkpoint() throws IOException { checkOpen(); return nCheckpoint(handle); }
    public static String sqliteVersion() { ensureLibrary(); return nVersion(); }
    /** False for runtimes lacking the upstream 2026 WAL-reset fix (including unknown backports). */
    public static boolean walSafeRuntime() { ensureLibrary(); return nWalSafe(); }
    public synchronized String journalMode() throws IOException { checkOpen(); return nJournal(handle); }
    private void checkOpen() { if (handle == 0) throw new IllegalStateException("closed SQLite store"); }
    @Override public synchronized void close() throws IOException {
        if (handle != 0) { nClose(handle); handle = 0; }
    }
    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    private static byte[] utf8(String s) throws IOException {
        try {
            ByteBuffer b = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(s));
            byte[] bytes = new byte[b.remaining()]; b.get(bytes); return bytes;
        } catch (CharacterCodingException e) { throw new IOException("invalid UTF-8 path", e); }
    }
    static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static byte[] frame(long id, byte[] value) {
        byte[] result = new byte[Math.addExact(HEADER, value.length)];
        ByteBuffer.wrap(result).putInt(MAGIC).putLong(id).putInt(value.length);
        System.arraycopy(value, 0, result, HEADER, value.length);
        MessageDigest digest = sha256(); digest.update(result, 0, 16);
        digest.update(result, HEADER, value.length);
        System.arraycopy(digest.digest(), 0, result, 16, 32); return result;
    }
    static byte[] unframe(long id, byte[] frame, int maxBytes) throws IOException {
        if (frame.length < HEADER || (long) frame.length > (long) maxBytes + HEADER) {
            throw new IOException("corrupt frame length");
        }
        ByteBuffer b = ByteBuffer.wrap(frame);
        if (b.getInt() != MAGIC || b.getLong() != id || b.getInt() != frame.length - HEADER) {
            throw new IOException("corrupt frame identity/version/length");
        }
        MessageDigest digest = sha256(); digest.update(frame, 0, 16);
        digest.update(frame, HEADER, frame.length - HEADER);
        if (!MessageDigest.isEqual(digest.digest(), Arrays.copyOfRange(frame, 16, HEADER))) {
            throw new IOException("record checksum mismatch");
        }
        return Arrays.copyOfRange(frame, HEADER, frame.length);
    }
    private static native long nOpen(byte[] path, byte[] namespace, byte[] codec, int recordBytes,
            int batchBytes, int batchRecords, int busyMillis, int pageCacheKiB, int journalPolicy) throws IOException;
    private static native byte[] nRead(long handle, long id) throws IOException;
    private static native long nWrite(long handle, long[] ids, byte[][] frames, byte[] headName,
            long expectedRevision, long recordId) throws IOException;
    private static native long[] nHead(long handle, byte[] name) throws IOException;
    private static native long[] nIds(long handle, long afterExclusive, int limit) throws IOException;
    private static native long nCount(long handle) throws IOException;
    private static native int nPrune(long handle, long beforeExclusive, int maxDeletes)
            throws IOException;
    private static native int[] nCheckpoint(long handle) throws IOException;
    private static native void nClose(long handle) throws IOException;
    private static native String nVersion();
    private static native boolean nWalSafe();
    private static native String nJournal(long handle) throws IOException;
}
