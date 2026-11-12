// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
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
