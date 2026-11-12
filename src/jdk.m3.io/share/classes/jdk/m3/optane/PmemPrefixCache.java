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
