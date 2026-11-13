// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, reusable inactive/prefix blocks backed by an existing block store.
 * This is an explicit byte-transfer API, not a replacement for the transformer's
 * active heap KV or a decoder/model integration. The caller owns payload buffers
 * and the borrowed store/region. Blocks consume append-only capacity; replacing a
 * key does not reclaim its earlier slots.
 *
 * <p>A bounded optional index retains only primitive digest/ordinal lanes. Full
 * digest comparison and store validation make collisions harmless. Release it
 * under memory pressure with {@link #releaseIndex()}; lookups then scan committed
 * receipts. The independently authored lifecycle takes design inspiration from
 * LMCache's explicit reusable-block reservation/release separation.</p>
 */
public final class PmemPrefixCache {
    public static final int MAX_INDEX_ENTRIES = 1 << 20;
    private static final long TENSOR_MAGIC = 0x4d3354454e534f31L;
    private static final int TENSOR_HEADER_BYTES = 48;
    private final PmemBlockStore store;
    private long[] digestLanes;
    private long[] ordinalLane;
    private long indexedCount;

    /** An entry uses 40 bytes in primitive arrays, excluding array/object headers. */
    public PmemPrefixCache(PmemBlockStore store, int indexEntries) {
        this.store = Objects.requireNonNull(store, "store");
        if (indexEntries < 0 || indexEntries > MAX_INDEX_ENTRIES)
            throw new IllegalArgumentException("prefix index capacity out of bounds");
        if (indexEntries != 0) {
            digestLanes = new long[indexEntries * 4];
            ordinalLane = new long[indexEntries];
            Arrays.fill(ordinalLane, -1);
        }
        indexedCount = store.committedCount();
    }

    /** Publish a complete reusable block; the caller's source must remain stable until return. */
    public long put(PmemPrefixKey key, byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(key, "key");
        synchronized (store) {
            refresh();
            long ordinal = store.append(key.sha256(), payload, offset, length);
            indexedCount = store.committedCount();
            remember(key, ordinal);
            return ordinal;
        }
    }

    /**
     * Read the latest block into the caller's array, returning its length or -1 for
     * a miss. A failed read must be discarded, as specified by {@link PmemBlockStore#read}.
     */
    public int get(PmemPrefixKey key, byte[] target, int offset) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(offset, target.length + 1L);
        synchronized (store) {
            long ordinal = find(key);
            return ordinal < 0 ? -1 : store.read(ordinal, target, offset);
        }
    }

    /**
     * Publish one complete contiguous tensor under a separate typed-key domain.
     * The scalar format, endian order and shape join the prefix identity. The
     * payload length must exactly match the descriptor before any store write.
     * Caller bytes must remain unchanged until return. No dtype conversion occurs.
     */
    public long put(PmemPrefixKey key, PmemTensorSpec spec,
                    byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(payload, "payload");
        Objects.checkFromIndexSize(offset, length, payload.length);
        if (spec.byteCount() != length) {
            throw new IllegalArgumentException("tensor payload length differs from descriptor");
        }
        byte[] descriptor = spec.encoding();
        int frameLength = tensorFrameLength(spec, descriptor);
        ByteBuffer frame = ByteBuffer.allocate(frameLength);
        frame.putLong(TENSOR_MAGIC).putInt(descriptor.length).putInt(length);
        frame.put(PmemBlockStore.parseHash(key.sha256())).put(descriptor);
        frame.put(payload, offset, length);
        return put(PmemPrefixKey.tensor(key, descriptor), frame.array(), 0, frameLength);
    }

    /**
     * Restore a tensor only after its complete envelope and exact descriptor pass
     * validation. A miss returns -1; a miss or rejected frame leaves target intact.
     * A shape, dtype or endian mismatch is a different key and therefore a miss.
     * Target capacity for the declared tensor is checked even when its key is absent.
     * Scratch memory is bounded by this store's maximum block payload.
     */
    public int get(PmemPrefixKey key, PmemTensorSpec spec,
                   byte[] target, int offset) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(target, "target");
        byte[] descriptor = spec.encoding();
        int frameLength = tensorFrameLength(spec, descriptor);
        int count = (int) spec.byteCount();
        Objects.checkFromIndexSize(offset, count, target.length);
        synchronized (store) {
            long ordinal = find(PmemPrefixKey.tensor(key, descriptor));
            if (ordinal < 0) {
                return -1;
            }
            if (store.metadata(ordinal).length() != frameLength) {
                throw new IOException("typed tensor frame length mismatch");
            }
            byte[] frame = new byte[frameLength];
            store.read(ordinal, frame, 0);
            ByteBuffer fields = ByteBuffer.wrap(frame);
            if (fields.getLong() != TENSOR_MAGIC || fields.getInt() != descriptor.length
                    || fields.getInt() != count) {
                throw new IOException("typed tensor frame header mismatch");
            }
            byte[] prefix = PmemBlockStore.parseHash(key.sha256());
            if (!Arrays.equals(frame, 16, TENSOR_HEADER_BYTES, prefix, 0, prefix.length)
                    || !Arrays.equals(frame, TENSOR_HEADER_BYTES,
                            TENSOR_HEADER_BYTES + descriptor.length,
                            descriptor, 0, descriptor.length)) {
                throw new IOException("typed tensor identity mismatch");
            }
            System.arraycopy(frame, TENSOR_HEADER_BYTES + descriptor.length, target, offset, count);
            return count;
        }
    }

    private int tensorFrameLength(PmemTensorSpec spec, byte[] descriptor) {
        long overhead = TENSOR_HEADER_BYTES + (long) descriptor.length;
        if (spec.byteCount() > store.maxPayloadBytes() - overhead) {
            throw new IllegalArgumentException("typed tensor exceeds fixed block payload");
        }
        return (int) (overhead + spec.byteCount());
    }

    /** On-demand validated payload length, or -1 for a miss; no metadata is retained. */
    public int payloadLength(PmemPrefixKey key) throws IOException {
        Objects.requireNonNull(key, "key");
        synchronized (store) {
            long ordinal = find(key);
            return ordinal < 0 ? -1 : store.metadata(ordinal).length();
        }
    }

    /** Drop optional heap index storage; the borrowed store and committed payloads remain usable. */
    public void releaseIndex() {
        synchronized (store) { digestLanes = null; ordinalLane = null; }
    }

    public long indexBytes() {
        synchronized (store) { return ordinalLane == null ? 0 : 40L * ordinalLane.length; }
    }

    private long find(PmemPrefixKey key) throws IOException {
        refresh();
        if (ordinalLane != null) {
            ByteBuffer hash = ByteBuffer.wrap(PmemBlockStore.parseHash(key.sha256()));
            long first = hash.getLong();
            int slot = slot(first);
            int at = slot * 4;
            if (ordinalLane[slot] >= 0 && digestLanes[at] == first
                    && digestLanes[at + 1] == hash.getLong()
                    && digestLanes[at + 2] == hash.getLong()
                    && digestLanes[at + 3] == hash.getLong()) {
                return ordinalLane[slot];
            }
        }
        long ordinal = store.findLatest(key.sha256());
        if (ordinal >= 0) remember(key, ordinal);
        return ordinal;
    }

    private void refresh() {
        long current = store.committedCount();
        if (current != indexedCount) {
            if (ordinalLane != null) Arrays.fill(ordinalLane, -1);
            indexedCount = current;
        }
    }

    private void remember(PmemPrefixKey key, long ordinal) {
        if (ordinalLane == null) return;
        ByteBuffer hash = ByteBuffer.wrap(PmemBlockStore.parseHash(key.sha256()));
        long first = hash.getLong();
        int slot = slot(first), at = slot * 4;
        digestLanes[at] = first;
        for (int i = 1; i < 4; i++) digestLanes[at + i] = hash.getLong();
        ordinalLane[slot] = ordinal;
    }

    private int slot(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        return (int) Long.remainderUnsigned(value, ordinalLane.length);
    }
}
