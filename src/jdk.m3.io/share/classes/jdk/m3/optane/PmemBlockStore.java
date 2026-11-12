// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable, fixed-slot binary payloads committed by the existing {@link PmemTaskLog}.
 * The caller exclusively reserves the complete range and owns the region's lifetime.
 * Payloads and receipts must not be changed through another owner, process, or mapping.
 *
 * <p>The first task receipt seals the format, schema and geometry. Later receipts
 * bind a key, slot ordinal and entire payload frame. Payload persistence completes
 * before its task receipt is appended. An uncertain write or persist poisons this
 * handle until reopening; reopening accepts only committed, intact frames. A
 * corrupt committed frame refuses the store rather than silently rolling it back.
 * Uncommitted tail bytes are never exposed and can be replaced on the next append.
 * No second log, memory-mapping engine, or retained per-entry object is introduced.</p>
 *
 * <p>This independently authored design follows persist-before-publication and
 * immutable-block principles studied by PMDK and checkpoint systems. A successful
 * backend persist is not a power-failure or physical-PMem qualification.</p>
 */
public final class PmemBlockStore {
    public static final int HEADER_BYTES = 128;
    private static final int VERSION = 1;
    private static final byte[] MAGIC = "M3BLOCK1".getBytes(StandardCharsets.US_ASCII);
    private static final HexFormat HEX = HexFormat.of();
    private static final String CONFIG_TASK = sha256("M3PMEM_BLOCK_CONFIG_V1".getBytes(StandardCharsets.US_ASCII));
    private final PmemRegion region;
    private final PmemTaskLog log;
    private final long origin;
    private final long payloadOrigin;
    private final long capacity;
    private final int maxPayload;
    private final long slotBytes;
    private final String schema;
    private final String configuration;
    private long committed;
    private boolean recoveredTail;
    private boolean reopenRequired;

    /** A validated, on-demand projection; the store retains no metadata objects per slot. */
    public record Metadata(long ordinal, String keySha256, int length, String payloadSha256) { }

    private PmemBlockStore(PmemRegion region, PmemTaskLog log, long origin,
                           long capacity, int maxPayload, String schema, String configuration) {
        this.region = region;
        this.log = log;
        this.origin = origin;
        this.payloadOrigin = origin + (capacity + 1) * PmemTaskLog.RECORD_BYTES;
        this.capacity = capacity;
        this.maxPayload = maxPayload;
        this.slotBytes = HEADER_BYTES + (long) maxPayload;
        this.schema = schema;
        this.configuration = configuration;
    }

    /** Exact reserved bytes, including the configuration receipt and payload headers. */
    public static long requiredBytes(long capacity, int maxPayloadBytes) {
        if (capacity <= 0 || maxPayloadBytes <= 0) throw new IllegalArgumentException("positive capacity and payload size required");
        try {
            return Math.addExact(Math.multiplyExact(Math.addExact(capacity, 1), PmemTaskLog.RECORD_BYTES),
                    Math.multiplyExact(capacity, Math.addExact((long) HEADER_BYTES, maxPayloadBytes)));
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("block-store geometry overflows", overflow);
        }
    }

    /**
     * Initialize an uncommitted task-log range. Previously committed stores refuse.
     * After an uncertain creation, try {@link #open} first: creation may have committed.
     * Payload slots need not be initialized; only receipts make their bytes observable.
     */
    public static PmemBlockStore create(PmemRegion region, long origin, long capacity,
                                        int maxPayloadBytes, String schemaSha256) throws IOException {
        PmemBlockStore store = prepare(region, origin, capacity, maxPayloadBytes, schemaSha256);
        if (store.log.committedCount() != 0) throw new IOException("block store already initialized");
        store.log.append(PmemTaskLog.Status.CANDIDATE, CONFIG_TASK, schemaSha256, store.configuration);
        return store;
    }

    /**
     * Reopen and validate the complete committed prefix, including every payload
     * checksum. Re-persist validated payloads before committed receipts: a failed
     * earlier persist may have left a valid marker visible without proving it was
     * durable. Open returns only after this recovery barrier also succeeds.
     */
    public static PmemBlockStore open(PmemRegion region, long origin, long capacity,
                                      int maxPayloadBytes, String schemaSha256) throws IOException {
        PmemBlockStore store = prepare(region, origin, capacity, maxPayloadBytes, schemaSha256);
        if (store.log.committedCount() == 0) throw new IOException("block-store configuration is not committed");
        PmemTaskLog.Receipt config = store.log.read(0);
        if (config.status() != PmemTaskLog.Status.CANDIDATE || !CONFIG_TASK.equals(config.taskSha256())
                || !schemaSha256.equals(config.sourceSha256()) || !store.configuration.equals(config.resultSha256())) {
            throw new IOException("block-store schema or geometry mismatch");
        }
        store.committed = store.log.committedCount() - 1;
        for (long ordinal = 0; ordinal < store.committed; ordinal++) {
            Metadata metadata = store.validate(ordinal);
            region.persist(store.slotOffset(ordinal), HEADER_BYTES + (long) metadata.length());
        }
        region.persist(store.origin, (store.committed + 1) * PmemTaskLog.RECORD_BYTES);
        store.recoveredTail = store.log.recoveredIncompleteTail()
                || store.committed < capacity && store.nonzeroSlot(store.committed);
        return store;
    }

    private static PmemBlockStore prepare(PmemRegion region, long origin, long capacity,
                                          int maxPayload, String schema) throws IOException {
        Objects.requireNonNull(region, "region");
        byte[] schemaBytes = parseHash(schema);
        long bytes = requiredBytes(capacity, maxPayload);
        if (!region.isOpen()) throw new IllegalStateException("region is closed");
        if (region.access() != PmemAccess.READ_WRITE) throw new IllegalArgumentException("block store requires a writable region");
        if (origin < 0 || origin > region.length() || bytes > region.length() - origin)
            throw new IndexOutOfBoundsException("block-store reservation exceeds region");
        ByteBuffer identity = ByteBuffer.allocate(64);
        identity.putInt(VERSION).putInt(HEADER_BYTES).putLong(origin).putLong(capacity).putInt(maxPayload)
                .putInt(PmemTaskLog.RECORD_BYTES).put(schemaBytes);
        String configuration = sha256(identity.array());
        return new PmemBlockStore(region, PmemTaskLog.open(region, origin, capacity + 1, null),
                origin, capacity, maxPayload, schema, configuration);
    }

    public synchronized long committedCount() { return committed; }
    public long capacity() { return capacity; }
    public int maxPayloadBytes() { return maxPayload; }
    public String schemaSha256() { return schema; }
    public String configurationSha256() { return configuration; }
    public synchronized long remainingCapacity() { return capacity - committed; }
    public synchronized boolean requiresReopen() { return reopenRequired; }
    public synchronized boolean recoveredIncompleteTail() { return recoveredTail; }

    /**
     * Append using the caller-owned source array, which must remain unchanged until return.
     * Empty payloads are valid. Failure after writing begins requires a fresh open.
     */
    public synchronized long append(String keySha256, byte[] payload, int offset, int length) throws IOException {
        byte[] key = parseHash(keySha256);
        Objects.requireNonNull(payload, "payload");
        Objects.checkFromIndexSize(offset, length, payload.length);
        if (length > maxPayload) throw new IllegalArgumentException("payload exceeds fixed slot");
        usable();
        if (committed == capacity) throw new IOException("block-store capacity exhausted");
        long ordinal = committed;
        MessageDigest payloadDigest = digest();
        payloadDigest.update(payload, offset, length);
        byte[] payloadHash = payloadDigest.digest();
        byte[] header = ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).putInt(VERSION).putInt(length)
                .putLong(ordinal).put(key).put(payloadHash).put(parseHash(configuration)).array();
        MessageDigest full = digest();
        full.update(header);
        full.update(payload, offset, length);
        String frameHash = HEX.formatHex(full.digest());
        long at = slotOffset(ordinal);
        try {
            region.write(at, header, 0, header.length);
            if (length != 0) region.write(at + HEADER_BYTES, payload, offset, length);
            region.persist(at, HEADER_BYTES + (long) length);
            PmemTaskLog.Receipt receipt = log.append(PmemTaskLog.Status.CANDIDATE,
                    configuration, keySha256, frameHash);
            if (receipt.ordinal() != ordinal + 1) throw new IOException("block/log ordinal mismatch");
        } catch (IOException | RuntimeException | Error failure) {
            reopenRequired = true;
            throw failure;
        }
        committed++;
        recoveredTail = false;
        return ordinal;
    }

    /** Latest committed ordinal for a key, or -1; uncommitted slots are excluded. */
    public synchronized long findLatest(String keySha256) throws IOException {
        parseHash(keySha256);
        usable();
        for (long ordinal = committed - 1; ordinal >= 0; ordinal--) {
            PmemTaskLog.Receipt receipt = receipt(ordinal);
            if (keySha256.equals(receipt.sourceSha256())) {
                validate(ordinal);
                return ordinal;
            }
        }
        return -1;
    }

    /** Metadata is returned only after complete frame validation. */
    public synchronized Metadata metadata(long ordinal) throws IOException {
        usable();
        return validate(ordinal);
    }

    /**
     * Validate before copying into the caller's array, returning the payload length.
     * Preflight corruption leaves the target untouched. A subsequent I/O failure
     * may partially fill it; the caller must discard output from a failed read.
     */
    public synchronized int read(long ordinal, byte[] target, int offset) throws IOException {
        Objects.requireNonNull(target, "target");
        usable();
        Metadata metadata = validate(ordinal);
        Objects.checkFromIndexSize(offset, metadata.length(), target.length);
        if (metadata.length() != 0)
            region.read(slotOffset(ordinal) + HEADER_BYTES, target, offset, metadata.length());
        MessageDigest verify = digest();
        verify.update(target, offset, metadata.length());
        if (!metadata.payloadSha256().equals(HEX.formatHex(verify.digest())))
            throw corrupt("payload changed during block read");
        return metadata.length();
    }

    private Metadata validate(long ordinal) throws IOException {
        if (ordinal < 0 || ordinal >= committed) throw new IndexOutOfBoundsException("uncommitted block ordinal");
        PmemTaskLog.Receipt receipt = receipt(ordinal);
        byte[] header = new byte[HEADER_BYTES];
        region.read(slotOffset(ordinal), header, 0, header.length);
        ByteBuffer fields = ByteBuffer.wrap(header);
        byte[] magic = new byte[MAGIC.length]; fields.get(magic);
        int version = fields.getInt(), length = fields.getInt();
        long savedOrdinal = fields.getLong();
        byte[] key = new byte[32], payloadHash = new byte[32], config = new byte[32];
        fields.get(key).get(payloadHash).get(config);
        if (!Arrays.equals(MAGIC, magic) || version != VERSION || savedOrdinal != ordinal
                || length < 0 || length > maxPayload || fields.getLong() != 0
                || !configuration.equals(HEX.formatHex(config))
                || receipt.status() != PmemTaskLog.Status.CANDIDATE
                || !configuration.equals(receipt.taskSha256())
                || !receipt.sourceSha256().equals(HEX.formatHex(key))) {
            throw corrupt("corrupt committed block header or receipt identity");
        }
        MessageDigest full = digest(), payload = digest();
        full.update(header);
        byte[] buffer = new byte[Math.min(65536, Math.max(1, length))];
        int done = 0;
        while (done < length) {
            int count = Math.min(buffer.length, length - done);
            region.read(slotOffset(ordinal) + HEADER_BYTES + done, buffer, 0, count);
            payload.update(buffer, 0, count); full.update(buffer, 0, count); done += count;
        }
        if (!MessageDigest.isEqual(payloadHash, payload.digest())
                || !receipt.resultSha256().equals(HEX.formatHex(full.digest()))) {
            throw corrupt("corrupt committed block payload checksum");
        }
        return new Metadata(ordinal, receipt.sourceSha256(), length, HEX.formatHex(payloadHash));
    }

    private boolean nonzeroSlot(long ordinal) {
        byte[] buffer = new byte[(int) Math.min(65536, slotBytes)];
        for (long done = 0; done < slotBytes;) {
            int count = (int) Math.min(buffer.length, slotBytes - done);
            region.read(slotOffset(ordinal) + done, buffer, 0, count);
            for (int i = 0; i < count; i++) if (buffer[i] != 0) return true;
            done += count;
        }
        return false;
    }

    private long slotOffset(long ordinal) { return payloadOrigin + ordinal * slotBytes; }

    private PmemTaskLog.Receipt receipt(long ordinal) throws IOException {
        try { return log.read(ordinal + 1); }
        catch (IOException corruptReceipt) {
            reopenRequired = true;
            throw corruptReceipt;
        }
    }

    private IOException corrupt(String message) {
        reopenRequired = true;
        return new IOException(message);
    }

    private void usable() {
        if (reopenRequired) throw new IllegalStateException("block append outcome uncertain: reopen required");
        if (!region.isOpen()) throw new IllegalStateException("region is closed");
    }

    static byte[] parseHash(String hash) {
        Objects.requireNonNull(hash, "sha256");
        if (hash.length() != 64) throw new IllegalArgumentException("expected lowercase SHA-256");
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f'))
                throw new IllegalArgumentException("expected lowercase SHA-256");
        }
        return HEX.parseHex(hash);
    }

    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
    static String sha256(byte[] bytes) { return HEX.formatHex(digest().digest(bytes)); }
}
