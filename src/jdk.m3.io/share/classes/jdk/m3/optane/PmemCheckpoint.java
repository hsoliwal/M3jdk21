// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Publish an ordered checkpoint manifest only after every immutable chunk is
 * durable in {@link PmemBlockStore}. The manifest is the sole publication point;
 * interrupted construction leaves unreferenced chunks and the preceding complete
 * checkpoint. There is no second persistence log or implicit model serialization.
 *
 * <p>This independently authored protocol follows the persist-before-publication
 * principle used by PMDK and complete-checkpoint techniques studied by PCcheck.
 * It offers explicit byte transfers, not transparent optimizer/heap capture or
 * hardware power-loss certification. Callers must encode all state needed to
 * resume (including RNG/optimizer/scheduler state where applicable).</p>
 */
public final class PmemCheckpoint {
    private static final byte[] MAGIC = "M3CKPT01".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MANIFEST_DOMAIN = "M3PMEM_CHECKPOINT_MANIFEST_V1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CHUNK_DOMAIN = "M3PMEM_CHECKPOINT_CHUNK_V1".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 64;
    private static final int ENTRY_BYTES = 48;
    private static final int MAX_MANIFEST_BYTES = 16 * 1024 * 1024;

    private PmemCheckpoint() { }

    /** Full checkpoint identity should include its model, schema and application namespace. */
    public record Info(long generation, int chunks, long totalBytes, long manifestOrdinal) { }

    /**
     * Fill the borrowed transfer buffer and return its valid byte count. Consume
     * each source chunk exactly once; do not retain/mutate the buffer after return
     * or append to the same store from this callback.
     */
    @FunctionalInterface
    public interface ChunkSource {
        int readChunk(int index, byte[] transferBuffer) throws IOException;
    }

    /**
     * A transactional receiver. Stage all bytes privately until commit; abort
     * must discard staged state. writeChunk must consume/copy its borrowed buffer
     * before returning. Publication/rollback at the destination remains the
     * receiver's responsibility, including an uncertain destination commit.
     */
    public interface RestoreTarget {
        void begin(long generation, int chunks, long totalBytes) throws IOException;
        void writeChunk(int index, byte[] transferBuffer, int length) throws IOException;
        void commit() throws IOException;
        void abort() throws IOException;
    }

    /**
     * Encode into a bounded borrowed stream on the calling thread. Do not retain
     * the stream, mutate its transfer array, or access the borrowed store from
     * this callback. Closing the stream seals its chunks, but does not publish
     * the checkpoint; publication requires successful callback completion.
     */
    @FunctionalInterface
    public interface StreamWriter {
        void write(OutputStream output) throws IOException;
    }

    /**
     * Decode the entire borrowed stream on the calling thread into private state.
     * The result becomes available to the caller only after complete consumption
     * and successful validation. Do not retain the stream, mutate its transfer
     * array, access the store, or publish partially decoded state from the callback.
     * The decoder owns disposal of resources it creates if decoding fails.
     */
    @FunctionalInterface
    public interface StreamReader<T> {
        T read(InputStream input) throws IOException;
    }

    /**
     * Encode a checkpoint without materializing its entire payload on the heap.
     * The nonempty transfer buffer must fit one store payload. maximumBytes is a
     * hard limit and also reserves ceil(maximumBytes / buffer.length) chunks plus
     * one manifest before invoking the writer. Empty streams use zero chunks.
     * Full chunks persist while writing; flush does not split a partial chunk.
     * close seals a partial chunk but cannot publish before the callback returns.
     *
     * <p>Any stream failure poisons this operation even if the writer catches it.
     * A failed writer can leave unreferenced chunks. An uncertain final append
     * can have committed: reopen and inspect before retrying. The existing v1
     * manifest and chunk format is also readable by {@link #restore}.</p>
     */
    public static Info commitStream(PmemBlockStore store, String checkpointSha256,
                                    long generation, long maximumBytes, byte[] transferBuffer,
                                    StreamWriter writer) throws IOException {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(transferBuffer, "transferBuffer");
        Objects.requireNonNull(writer, "writer");
        PmemBlockStore.parseHash(checkpointSha256);
        if (generation < 0 || maximumBytes < 0 || transferBuffer.length == 0
                || transferBuffer.length > store.maxPayloadBytes() || store.maxPayloadBytes() < HEADER_BYTES)
            throw new IllegalArgumentException("checkpoint stream bounds are invalid");
        long maximumChunks = maximumBytes == 0 ? 0 : 1 + (maximumBytes - 1) / transferBuffer.length;
        if (maximumChunks > maxChunks(store))
            throw new IllegalArgumentException("checkpoint stream exceeds bounded manifest");
        synchronized (store) {
            String key = manifestKey(checkpointSha256);
            long previous = store.findLatest(key);
            if (previous >= 0 && generation <= load(store, checkpointSha256, previous).info().generation())
                throw new IllegalArgumentException("checkpoint generation must increase");
            if (store.remainingCapacity() < maximumChunks + 1)
                throw new IOException("insufficient block capacity for bounded checkpoint");
            CheckpointOutput output = new CheckpointOutput(store, checkpointSha256, generation,
                    maximumBytes, transferBuffer, (int) maximumChunks);
            try {
                writer.write(output);
                output.finish();
                return output.publish(key);
            } finally {
                output.revoke();
            }
        }
    }

    /**
     * Validate the latest checkpoint and decode its complete finite byte stream.
     * maximumBytes and the transfer buffer's ability to hold every stored chunk
     * are checked before the reader runs. Existing chunk checkpoints, including
     * empty chunks, are accepted. Each chunk is revalidated while reading.
     *
     * <p>Closing early does not drain or accept a partial decode. Caught stream
     * failures still refuse the result. The transfer buffer and bounded manifest
     * are the adapter's scratch; allocations explicitly requested by a decoder
     * (for example readAllBytes) and the decoded application's state are separate.</p>
     */
    public static <T> T readStream(PmemBlockStore store, String checkpointSha256,
                                  long maximumBytes, byte[] transferBuffer,
                                  StreamReader<T> reader) throws IOException {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(transferBuffer, "transferBuffer");
        Objects.requireNonNull(reader, "reader");
        if (maximumBytes < 0 || transferBuffer.length == 0)
            throw new IllegalArgumentException("checkpoint stream bounds are invalid");
        synchronized (store) {
            Manifest manifest = latest(store, checkpointSha256);
            if (manifest.info().totalBytes() > maximumBytes)
                throw new IllegalArgumentException("checkpoint exceeds reader byte limit");
            ByteBuffer fields = ByteBuffer.wrap(manifest.bytes());
            for (int index = 0; index < manifest.info().chunks(); index++) {
                if (fields.getInt(HEADER_BYTES + ENTRY_BYTES * index + Long.BYTES) > transferBuffer.length)
                    throw new IllegalArgumentException("checkpoint stream buffer is too small");
            }
            CheckpointInput input = new CheckpointInput(store, transferBuffer, manifest);
            try {
                T result = reader.read(input);
                input.finish();
                return result;
            } finally {
                input.revoke();
            }
        }
    }

    /** Scope checks precede store locking so a foreign callback thread cannot deadlock its owner. */
    private static final class StreamScope {
        private final Thread owner = Thread.currentThread();
        private volatile boolean active = true;
        private volatile Throwable failure;
        private PmemBlockStore store;
        private long expectedCount;

        StreamScope(PmemBlockStore store) {
            this.store = store;
            expectedCount = store.committedCount();
        }

        void check() throws IOException {
            if (Thread.currentThread() != owner) {
                IOException problem = new IOException("checkpoint stream belongs to its callback thread");
                poison(problem);
                throw problem;
            }
            if (!active) throw new IOException("checkpoint stream callback has ended");
            if (failure != null) throw new IOException("checkpoint stream operation failed", failure);
            if (store.requiresReopen() || store.committedCount() != expectedCount) {
                IOException problem = new IOException("checkpoint callback changed the borrowed store");
                poison(problem);
                throw problem;
            }
        }

        synchronized void poison(Throwable problem) {
            if (failure == null) failure = problem;
        }

        long append(String key, byte[] bytes, int length) throws IOException {
            check();
            long ordinal = store.append(key, bytes, 0, length);
            if (ordinal != expectedCount) throw new IOException("checkpoint append ordinal changed");
            expectedCount++;
            return ordinal;
        }

        void revoke() { active = false; store = null; }
    }

    private static final class CheckpointOutput extends OutputStream {
        private final StreamScope scope;
        private final String identity;
        private final long generation, maximum;
        private byte[] buffer;
        private ByteBuffer manifest;
        private long total;
        private int buffered, chunks;
        private boolean closed;

        CheckpointOutput(PmemBlockStore store, String identity, long generation, long maximum,
                         byte[] buffer, int maximumChunks) {
            scope = new StreamScope(store);
            this.identity = identity;
            this.generation = generation;
            this.maximum = maximum;
            this.buffer = buffer;
            manifest = ByteBuffer.allocate(HEADER_BYTES + ENTRY_BYTES * maximumChunks);
            manifest.put(MAGIC).putInt(VERSION).putInt(0).putLong(generation).putLong(0)
                    .put(PmemBlockStore.parseHash(identity));
        }

        private void writable() throws IOException {
            scope.check();
            if (closed) throw new IOException("checkpoint output is closed");
        }

        private void bounded(int length) throws IOException {
            if (length > maximum - total) throw new IOException("checkpoint writer exceeded byte limit");
        }

        @Override public void write(int value) throws IOException {
            try {
                writable();
                bounded(1);
                buffer[buffered++] = (byte) value;
                total++;
                if (buffered == buffer.length) appendChunk();
            } catch (IOException | RuntimeException | Error failure) {
                scope.poison(failure);
                throw failure;
            }
        }

        @Override public void write(byte[] bytes) throws IOException {
            try {
                writable();
                write(bytes, 0, bytes.length);
            } catch (IOException | RuntimeException | Error failure) {
                scope.poison(failure);
                throw failure;
            }
        }

        @Override public void write(byte[] bytes, int offset, int length) throws IOException {
            try {
                writable();
                Objects.checkFromIndexSize(offset, length, bytes.length);
                if (bytes == buffer) throw new IllegalArgumentException("writer must not alias its transfer array");
                bounded(length);
                while (length != 0) {
                    int count = Math.min(length, buffer.length - buffered);
                    System.arraycopy(bytes, offset, buffer, buffered, count);
                    buffered += count;
                    offset += count;
                    length -= count;
                    total += count;
                    if (buffered == buffer.length) appendChunk();
                }
            } catch (IOException | RuntimeException | Error failure) {
                scope.poison(failure);
                throw failure;
            }
        }

        private void appendChunk() throws IOException {
            if (buffered == 0) return;
            MessageDigest digest = PmemBlockStore.digest();
            digest.update(buffer, 0, buffered);
            byte[] hash = digest.digest();
            long ordinal = scope.append(chunkKey(identity, generation, chunks), buffer, buffered);
            manifest.putLong(ordinal).putInt(buffered).putInt(0).put(hash);
            chunks++;
            buffered = 0;
        }

        @Override public void flush() throws IOException {
            try { writable(); }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public void close() throws IOException {
            try {
                scope.check();
                if (!closed) { appendChunk(); closed = true; }
            } catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        void finish() throws IOException { scope.check(); close(); }

        Info publish(String key) throws IOException {
            scope.check();
            manifest.putInt(12, chunks).putLong(24, total);
            long ordinal = scope.append(key, manifest.array(), HEADER_BYTES + ENTRY_BYTES * chunks);
            return new Info(generation, chunks, total, ordinal);
        }

        void revoke() { scope.revoke(); buffer = null; manifest = null; }
    }

    private static final class CheckpointInput extends InputStream {
        private final StreamScope scope;
        private PmemBlockStore store;
        private final Info info;
        private final String manifestHash;
        private byte[] buffer;
        private ByteBuffer fields;
        private int nextChunk, offset, limit;
        private long consumed;
        private boolean closed;

        CheckpointInput(PmemBlockStore store, byte[] buffer, Manifest manifest) {
            scope = new StreamScope(store);
            this.store = store;
            this.buffer = buffer;
            info = manifest.info();
            fields = ByteBuffer.wrap(manifest.bytes());
            manifestHash = PmemBlockStore.sha256(manifest.bytes());
        }

        private void readable() throws IOException {
            scope.check();
            if (closed) throw new IOException("checkpoint input is closed");
        }

        private boolean fill() throws IOException {
            while (offset == limit && nextChunk < info.chunks()) {
                int entry = HEADER_BYTES + ENTRY_BYTES * nextChunk++;
                long ordinal = fields.getLong(entry);
                limit = store.read(ordinal, buffer, 0);
                offset = 0;
                if (limit != fields.getInt(entry + Long.BYTES))
                    throw new IOException("checkpoint chunk length changed during decoding");
                MessageDigest digest = PmemBlockStore.digest();
                digest.update(buffer, 0, limit);
                byte[] expected = new byte[32];
                fields.get(entry + 16, expected);
                if (!MessageDigest.isEqual(expected, digest.digest()))
                    throw new IOException("checkpoint chunk changed during decoding");
            }
            return offset != limit;
        }

        @Override public int read() throws IOException {
            try {
                readable();
                if (!fill()) return -1;
                consumed++;
                return buffer[offset++] & 255;
            } catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public int read(byte[] bytes) throws IOException {
            try {
                readable();
                return read(bytes, 0, bytes.length);
            } catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public int read(byte[] bytes, int start, int length) throws IOException {
            try {
                readable();
                Objects.checkFromIndexSize(start, length, bytes.length);
                if (bytes == buffer) throw new IllegalArgumentException("reader must not alias its transfer array");
                if (length == 0) return 0;
                if (!fill()) return -1;
                int count = Math.min(length, limit - offset);
                System.arraycopy(buffer, offset, bytes, start, count);
                offset += count;
                consumed += count;
                return count;
            } catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public int available() throws IOException {
            try { readable(); return (int) Math.min(Integer.MAX_VALUE, info.totalBytes() - consumed); }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public long skip(long count) throws IOException {
            try {
                readable();
                long skipped = 0;
                while (skipped < count && fill()) {
                    int step = (int) Math.min(count - skipped, limit - offset);
                    offset += step;
                    consumed += step;
                    skipped += step;
                }
                return skipped;
            } catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public byte[] readNBytes(int length) throws IOException {
            try { readable(); return super.readNBytes(length); }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public int readNBytes(byte[] bytes, int start, int length) throws IOException {
            try {
                readable();
                Objects.checkFromIndexSize(start, length, bytes.length);
                if (bytes == buffer) throw new IllegalArgumentException("reader must not alias its transfer array");
                return super.readNBytes(bytes, start, length);
            }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public void skipNBytes(long count) throws IOException {
            try { readable(); super.skipNBytes(count); }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public long transferTo(OutputStream target) throws IOException {
            try { readable(); return super.transferTo(target); }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        @Override public void reset() throws IOException {
            IOException failure = new IOException("checkpoint stream does not support reset");
            try { readable(); }
            catch (IOException | RuntimeException | Error problem) { scope.poison(problem); throw problem; }
            scope.poison(failure);
            throw failure;
        }

        @Override public void close() throws IOException {
            try { scope.check(); closed = true; }
            catch (IOException | RuntimeException | Error failure) { scope.poison(failure); throw failure; }
        }

        void finish() throws IOException {
            scope.check();
            if (consumed != info.totalBytes()) throw new IOException("checkpoint reader did not consume complete stream");
            if (!store.metadata(info.manifestOrdinal()).payloadSha256().equals(manifestHash))
                throw new IOException("checkpoint manifest changed during decoding");
        }

        void revoke() { scope.revoke(); store = null; buffer = null; fields = null; }
    }

    /** Maximum bounded manifest entries supported by this store's fixed payload size. */
    public static int maxChunks(PmemBlockStore store) {
        Objects.requireNonNull(store, "store");
        return Math.max(0, (Math.min(store.maxPayloadBytes(), MAX_MANIFEST_BYTES) - HEADER_BYTES) / ENTRY_BYTES);
    }

    /**
     * Commit a strictly newer, nonnegative generation. A source/persistence failure
     * may consume slots, and an uncertain final append may have committed; reopen
     * and inspect before retrying. The complete n+1-slot capacity is checked before
     * invoking the source, but aborted chunks are not reclaimed.
     */
    public static Info commit(PmemBlockStore store, String checkpointSha256, long generation,
                               int chunkCount, byte[] transferBuffer, ChunkSource source) throws IOException {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(transferBuffer, "transferBuffer");
        Objects.requireNonNull(source, "source");
        PmemBlockStore.parseHash(checkpointSha256);
        if (generation < 0 || chunkCount < 0 || chunkCount > maxChunks(store)
                || store.maxPayloadBytes() < HEADER_BYTES)
            throw new IllegalArgumentException("checkpoint generation or manifest size out of bounds");
        synchronized (store) {
            String manifestKey = manifestKey(checkpointSha256);
            long previous = store.findLatest(manifestKey);
            if (previous >= 0 && generation <= load(store, checkpointSha256, previous).info().generation())
                throw new IllegalArgumentException("checkpoint generation must increase");
            if (store.remainingCapacity() < chunkCount + 1L)
                throw new IOException("insufficient block capacity for complete checkpoint");
            ByteBuffer manifest = ByteBuffer.allocate(HEADER_BYTES + ENTRY_BYTES * chunkCount);
            manifest.put(MAGIC).putInt(VERSION).putInt(chunkCount).putLong(generation).putLong(0)
                    .put(PmemBlockStore.parseHash(checkpointSha256));
            long total = 0;
            for (int index = 0; index < chunkCount; index++) {
                long beforeSource = store.committedCount();
                int length = source.readChunk(index, transferBuffer);
                if (store.committedCount() != beforeSource)
                    throw new IOException("checkpoint source appended to the borrowed store");
                if (length < 0 || length > transferBuffer.length || length > store.maxPayloadBytes())
                    throw new IOException("checkpoint source returned an invalid chunk length");
                MessageDigest digest = PmemBlockStore.digest();
                digest.update(transferBuffer, 0, length);
                byte[] payloadHash = digest.digest();
                long ordinal = store.append(chunkKey(checkpointSha256, generation, index), transferBuffer, 0, length);
                manifest.putLong(ordinal).putInt(length).putInt(0).put(payloadHash);
                total = Math.addExact(total, length);
            }
            manifest.putLong(24, total);
            long ordinal = store.append(manifestKey, manifest.array(), 0, manifest.capacity());
            return new Info(generation, chunkCount, total, ordinal);
        }
    }

    /** Inspect the latest complete manifest and verify every ordered chunk; missing state refuses. */
    public static Info inspect(PmemBlockStore store, String checkpointSha256) throws IOException {
        Objects.requireNonNull(store, "store");
        synchronized (store) { return latest(store, checkpointSha256).info(); }
    }

    /**
     * Validate manifest, order, chunk identities, lengths and hashes before even
     * beginning a restore. Re-read into the caller's bounded buffer, then ask the
     * transactional target to publish. Any transfer/target failure after begin
     * calls abort and preserves that failure (and suppressed abort failures).
     */
    public static Info restore(PmemBlockStore store, String checkpointSha256,
                                byte[] transferBuffer, RestoreTarget target) throws IOException {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(transferBuffer, "transferBuffer");
        Objects.requireNonNull(target, "target");
        synchronized (store) {
            Manifest manifest = latest(store, checkpointSha256);
            ByteBuffer fields = ByteBuffer.wrap(manifest.bytes());
            for (int index = 0; index < manifest.info().chunks(); index++) {
                int length = fields.getInt(HEADER_BYTES + ENTRY_BYTES * index + Long.BYTES);
                if (length > transferBuffer.length)
                    throw new IllegalArgumentException("restore transfer buffer is too small");
            }
            Info info = manifest.info();
            try {
                target.begin(info.generation(), info.chunks(), info.totalBytes());
                for (int index = 0; index < info.chunks(); index++) {
                    int entry = HEADER_BYTES + ENTRY_BYTES * index;
                    long ordinal = fields.getLong(entry);
                    int length = store.read(ordinal, transferBuffer, 0);
                    if (length != fields.getInt(entry + Long.BYTES))
                        throw new IOException("checkpoint chunk changed during restore");
                    target.writeChunk(index, transferBuffer, length);
                }
                target.commit();
            } catch (IOException | RuntimeException | Error failure) {
                try { target.abort(); }
                catch (IOException | RuntimeException | Error abortFailure) {
                    if (abortFailure != failure) failure.addSuppressed(abortFailure);
                }
                throw failure;
            }
            return info;
        }
    }

    private static Manifest latest(PmemBlockStore store, String checkpointSha256) throws IOException {
        String key = manifestKey(checkpointSha256);
        long ordinal = store.findLatest(key);
        if (ordinal < 0) throw new IOException("complete checkpoint is missing");
        return load(store, checkpointSha256, ordinal);
    }

    private static Manifest load(PmemBlockStore store, String checkpointSha256, long ordinal) throws IOException {
        PmemBlockStore.Metadata metadata = store.metadata(ordinal);
        if (metadata.length() < HEADER_BYTES || metadata.length() > MAX_MANIFEST_BYTES)
            throw new IOException("checkpoint manifest length out of bounds");
        byte[] bytes = new byte[metadata.length()];
        store.read(ordinal, bytes, 0);
        ByteBuffer fields = ByteBuffer.wrap(bytes);
        byte[] magic = new byte[MAGIC.length]; fields.get(magic);
        int version = fields.getInt(), chunks = fields.getInt();
        long generation = fields.getLong(), expectedTotal = fields.getLong();
        byte[] identity = new byte[32]; fields.get(identity);
        if (!Arrays.equals(MAGIC, magic) || version != VERSION || generation < 0
                || chunks < 0 || chunks > maxChunks(store) || expectedTotal < 0
                || HEADER_BYTES + ENTRY_BYTES * (long) chunks != bytes.length
                || !MessageDigest.isEqual(identity, PmemBlockStore.parseHash(checkpointSha256)))
            throw new IOException("corrupt checkpoint manifest header");
        long previousOrdinal = -1, total = 0;
        byte[] hash = new byte[32];
        for (int index = 0; index < chunks; index++) {
            long chunkOrdinal = fields.getLong();
            int length = fields.getInt(), reserved = fields.getInt();
            fields.get(hash);
            if (chunkOrdinal <= previousOrdinal || chunkOrdinal >= ordinal || length < 0
                    || length > store.maxPayloadBytes() || reserved != 0)
                throw new IOException("corrupt checkpoint chunk order or length");
            PmemBlockStore.Metadata chunk = store.metadata(chunkOrdinal);
            if (chunk.length() != length || !chunk.payloadSha256().equals(HexFormat.of().formatHex(hash))
                    || !chunk.keySha256().equals(chunkKey(checkpointSha256, generation, index)))
                throw new IOException("corrupt checkpoint chunk identity or checksum");
            total = Math.addExact(total, length);
            previousOrdinal = chunkOrdinal;
        }
        if (total != expectedTotal) throw new IOException("checkpoint total length mismatch");
        return new Manifest(bytes, new Info(generation, chunks, total, ordinal));
    }

    private record Manifest(byte[] bytes, Info info) { }

    private static String manifestKey(String checkpointSha256) {
        MessageDigest digest = PmemBlockStore.digest();
        digest.update(MANIFEST_DOMAIN);
        digest.update(PmemBlockStore.parseHash(checkpointSha256));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String chunkKey(String checkpointSha256, long generation, int index) {
        MessageDigest digest = PmemBlockStore.digest();
        digest.update(CHUNK_DOMAIN);
        digest.update(PmemBlockStore.parseHash(checkpointSha256));
        digest.update(ByteBuffer.allocate(Long.BYTES + Integer.BYTES).putLong(generation).putInt(index).array());
        return HexFormat.of().formatHex(digest.digest());
    }
}
