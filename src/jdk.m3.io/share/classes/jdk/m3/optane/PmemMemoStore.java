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
 * Exact source/AI memo values over the existing {@link PmemBlockStore} owner.
 * Namespace invalidation is an ordinary committed block in that same store.
 * Each operation checks its captured namespace generation under the store's
 * monitor, including across multiple facades sharing that store. Computations
 * begun before invalidation cannot publish using their old snapshot.
 *
 * <p>The caller retains exclusive ownership of the underlying store reservation
 * and region. Do not open a second writable store over the same bytes. Namespace
 * invalidation is logical: old immutable records still consume fixed capacity.
 * No mapping, second log, per-entry heap index or background worker is created.</p>
 *
 * <p>Candidate and externally validated results have separate identities. The
 * validated API checks supplied validation identities and the payload digest;
 * it does not execute the validator, authenticate its evidence, establish a
 * factual claim or authorize source promotion. Callers must obtain that identity
 * from their existing trusted validation authority.</p>
 */
public final class PmemMemoStore {
    public static final int MAX_VALUE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_NAMESPACE_BYTES = 1024;
    private static final int VERSION = 1;
    private static final int EPOCH_HEADER_BYTES = 24;
    private static final int HEADER_BYTES = 136;
    private static final int CANDIDATE = 1;
    private static final int VALIDATED = 2;
    private static final byte[] EPOCH_MAGIC = "M3MEPOCH".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENTRY_MAGIC = "M3MEMO01".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EPOCH_DOMAIN =
            "M3PMEM_MEMO_EPOCH_V1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENTRY_DOMAIN =
            "M3PMEM_MEMO_ENTRY_V1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NO_VALIDATION = new byte[96];
    private final PmemBlockStore store;
    private final String namespace;
    private final byte[] namespaceBytes;
    private final String epochKey;

    /** An immutable generation captured before starting a reusable computation. */
    public record Snapshot(String namespace, long generation) {
        public Snapshot {
            PmemMemoKey.utf8(namespace, MAX_NAMESPACE_BYTES, false);
            if (generation < 0) throw new IllegalArgumentException("negative namespace generation");
        }
    }

    /**
     * Caller-supplied identity of validator version/contract, immutable evidence
     * receipt and exact result bytes. The evidence must bind the complete memo
     * key and result in the caller's validation system. A digest is not a proof
     * that validation actually ran, nor is it an authenticated signature.
     */
    public record Validation(String validatorSha256, String evidenceSha256, String payloadSha256) {
        public Validation {
            PmemBlockStore.parseHash(validatorSha256);
            PmemBlockStore.parseHash(evidenceSha256);
            PmemBlockStore.parseHash(payloadSha256);
        }

        private byte[] bytes() {
            return ByteBuffer.allocate(96).put(PmemBlockStore.parseHash(validatorSha256))
                    .put(PmemBlockStore.parseHash(evidenceSha256))
                    .put(PmemBlockStore.parseHash(payloadSha256)).array();
        }
    }

    /** Borrow the existing store. Construction does not create or reset a namespace. */
    public PmemMemoStore(PmemBlockStore store, String namespace) {
        this.store = Objects.requireNonNull(store, "store");
        namespaceBytes = PmemMemoKey.utf8(namespace, MAX_NAMESPACE_BYTES, false);
        this.namespace = namespace;
        if (EPOCH_HEADER_BYTES + namespaceBytes.length > store.maxPayloadBytes())
            throw new IllegalArgumentException("namespace record exceeds block payload capacity");
        MessageDigest digest = PmemBlockStore.digest();
        digest.update(EPOCH_DOMAIN);
        digest.update(integer(namespaceBytes.length));
        digest.update(namespaceBytes);
        epochKey = HexFormat.of().formatHex(digest.digest());
    }

    /** Read the durable generation. A namespace with no epoch record begins at zero. */
    public Snapshot snapshot() throws IOException {
        synchronized (store) { return new Snapshot(namespace, currentGeneration()); }
    }

    /**
     * Durably advance exactly the expected generation. Stale snapshots and
     * overflow refuse before append. An uncertain persist requires reopening the
     * borrowed block store and inspecting the generation before retrying.
     */
    public Snapshot invalidate(Snapshot expected) throws IOException {
        synchronized (store) {
            checkCurrent(expected);
            if (expected.generation() == Long.MAX_VALUE)
                throw new IllegalStateException("namespace generation exhausted");
            long next = expected.generation() + 1;
            byte[] frame = ByteBuffer.allocate(EPOCH_HEADER_BYTES + namespaceBytes.length)
                    .put(EPOCH_MAGIC).putInt(VERSION).putInt(namespaceBytes.length)
                    .putLong(next).put(namespaceBytes).array();
            store.append(epochKey, frame, 0, frame.length);
            return new Snapshot(namespace, next);
        }
    }

    /** Persist a complete candidate. Caller source bytes must remain stable until return. */
    public long putCandidate(Snapshot expected, PmemMemoKey key,
                              byte[] payload, int offset, int length) throws IOException {
        return put(expected, key, null, payload, offset, length);
    }

    /** Persist a copied result only when its owned bytes match the supplied validation identity. */
    public long putValidated(Snapshot expected, PmemMemoKey key, Validation validation,
                              byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(validation, "validation");
        return put(expected, key, validation, payload, offset, length);
    }

    /** Read candidate bytes, or -1 for a miss. Failure leaves the caller target unchanged. */
    public int getCandidate(Snapshot expected, PmemMemoKey key,
                             byte[] target, int offset) throws IOException {
        return get(expected, key, null, target, offset);
    }

    /** Read only the result matching all supplied validation identities, or -1 for a miss. */
    public int getValidated(Snapshot expected, PmemMemoKey key, Validation validation,
                             byte[] target, int offset) throws IOException {
        Objects.requireNonNull(validation, "validation");
        return get(expected, key, validation, target, offset);
    }

    public int candidateLength(Snapshot expected, PmemMemoKey key) throws IOException {
        return length(expected, key, null);
    }

    public int validatedLength(Snapshot expected, PmemMemoKey key, Validation validation) throws
        IOException {
        Objects.requireNonNull(validation, "validation");
        return length(expected, key, validation);
    }

    private long put(Snapshot expected, PmemMemoKey key, Validation validation,
                      byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(payload, "payload");
        Objects.checkFromIndexSize(offset, length, payload.length);
        byte[] canonical = key.canonicalBytes();
        long frameLength = HEADER_BYTES + (long) namespaceBytes.length + canonical.length + length;
        if (length > MAX_VALUE_BYTES || frameLength > store.maxPayloadBytes())
            throw new IllegalArgumentException(
                "memo envelope exceeds bounded block payload capacity");
        synchronized (store) {
            checkCurrent(expected);
            int status = validation == null ? CANDIDATE : VALIDATED;
            byte[] validationBytes = validation == null ? NO_VALIDATION : validation.bytes();
            byte[] frame = ByteBuffer.allocate((int) frameLength)
                    .put(ENTRY_MAGIC).putInt(VERSION).putInt(namespaceBytes.length)
                    .putInt(canonical.length).putInt(length).putLong(expected.generation())
                    .putInt(status).putInt(0).put(validationBytes)
                    .put(namespaceBytes).put(canonical).put(payload, offset, length).array();
            if (validation != null) {
                MessageDigest digest = PmemBlockStore.digest();
                digest.update(frame, frame.length - length, length);
                if (!validation.payloadSha256().equals(HexFormat.of().formatHex(digest.digest())))
                    throw new IllegalArgumentException(
                "validation result digest does not match payload");
            }
            return store.append(entryKey(expected.generation(), canonical, status, validationBytes),
                    frame, 0, frame.length);
        }
    }

    private int get(Snapshot expected, PmemMemoKey key, Validation validation,
                     byte[] target, int offset) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(offset, target.length + 1L);
        synchronized (store) {
            byte[] frame = load(expected, key, validation);
            if (frame == null) return -1;
            int length = ByteBuffer.wrap(frame).getInt(20);
            Objects.checkFromIndexSize(offset, length, target.length);
            System.arraycopy(frame, frame.length - length, target, offset, length);
            return length;
        }
    }

    private int length(Snapshot expected, PmemMemoKey key, Validation validation) throws IOException
        {
        synchronized (store) {
            byte[] frame = load(expected, key, validation);
            return frame == null ? -1 : ByteBuffer.wrap(frame).getInt(20);
        }
    }

    /** Fully validate a bounded private transfer buffer before exposing any result bytes. */
    private byte[] load(Snapshot expected, PmemMemoKey key, Validation validation) throws
        IOException {
        Objects.requireNonNull(key, "key");
        checkCurrent(expected);
        byte[] canonical = key.canonicalBytes();
        int status = validation == null ? CANDIDATE : VALIDATED;
        byte[] validationBytes = validation == null ? NO_VALIDATION : validation.bytes();
        String entryKey = entryKey(expected.generation(), canonical, status, validationBytes);
        long ordinal = store.findLatest(entryKey);
        if (ordinal < 0) return null;
        PmemBlockStore.Metadata metadata = store.metadata(ordinal);
        int fixedBytes = HEADER_BYTES + namespaceBytes.length + canonical.length;
        if (metadata.length() < fixedBytes || metadata.length() > fixedBytes + MAX_VALUE_BYTES)
            throw new IOException("memo envelope length exceeds expected geometry");
        byte[] frame = new byte[metadata.length()];
        store.read(ordinal, frame, 0);
        ByteBuffer fields = ByteBuffer.wrap(frame);
        byte[] magic = new byte[ENTRY_MAGIC.length];
        fields.get(magic);
        int version = fields.getInt(), namespaceLength = fields.getInt();
        int keyLength = fields.getInt(), valueLength = fields.getInt();
        long generation = fields.getLong();
        int savedStatus = fields.getInt(), reserved = fields.getInt();
        byte[] savedValidation = new byte[96];
        fields.get(savedValidation);
        if (!Arrays.equals(ENTRY_MAGIC, magic) || version != VERSION
                || namespaceLength != namespaceBytes.length || keyLength != canonical.length
                || valueLength < 0 || valueLength > MAX_VALUE_BYTES
                || fixedBytes + (long) valueLength != frame.length
                || generation != expected.generation() || savedStatus != status || reserved != 0
                || !MessageDigest.isEqual(savedValidation, validationBytes)
                || !Arrays.equals(namespaceBytes, 0, namespaceBytes.length,
                        frame, HEADER_BYTES, HEADER_BYTES + namespaceBytes.length)
                || !Arrays.equals(canonical, 0, canonical.length,
                        frame, HEADER_BYTES + namespaceBytes.length, fixedBytes))
            throw new IOException("memo envelope identity or geometry mismatch");
        if (validation != null) {
            MessageDigest digest = PmemBlockStore.digest();
            digest.update(frame, fixedBytes, valueLength);
            if (!validation.payloadSha256().equals(HexFormat.of().formatHex(digest.digest())))
                throw new IOException("memo validation result digest mismatch");
        }
        return frame;
    }

    private void checkCurrent(Snapshot expected) throws IOException {
        Objects.requireNonNull(expected, "snapshot");
        if (!namespace.equals(expected.namespace()))
            throw new IllegalArgumentException("snapshot belongs to a different namespace");
        if (currentGeneration() != expected.generation())
            throw new IllegalStateException("stale namespace generation");
    }

    private long currentGeneration() throws IOException {
        long ordinal = store.findLatest(epochKey);
        if (ordinal < 0) return 0;
        if (store.metadata(ordinal).length() != EPOCH_HEADER_BYTES + namespaceBytes.length)
            throw new IOException("namespace generation record length mismatch");
        byte[] frame = new byte[EPOCH_HEADER_BYTES + namespaceBytes.length];
        store.read(ordinal, frame, 0);
        ByteBuffer fields = ByteBuffer.wrap(frame);
        byte[] magic = new byte[EPOCH_MAGIC.length];
        fields.get(magic);
        int version = fields.getInt(), namespaceLength = fields.getInt();
        long generation = fields.getLong();
        if (!Arrays.equals(EPOCH_MAGIC, magic) || version != VERSION
                || namespaceLength != namespaceBytes.length || generation <= 0
                || !Arrays.equals(namespaceBytes, 0, namespaceBytes.length,
                        frame, EPOCH_HEADER_BYTES, frame.length))
            throw new IOException("namespace generation record identity mismatch");
        return generation;
    }

    private String entryKey(long generation, byte[] canonical, int status, byte[] validation) {
        MessageDigest digest = PmemBlockStore.digest();
        digest.update(ENTRY_DOMAIN);
        digest.update(integer(namespaceBytes.length));
        digest.update(namespaceBytes);
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(generation).array());
        digest.update(integer(canonical.length));
        digest.update(canonical);
        digest.update(integer(status));
        digest.update(validation);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static byte[] integer(int value) { return ByteBuffer.allocate(Integer.BYTES).putInt(
        value).array(); }
}
