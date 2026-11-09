/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/reduce/DocumentDeduplicator.java (blob 114630806595) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Exact SHA-256 dedupe plus four-band SimHash locality filtering for near duplicates.
 *
 * <p>Band payloads are primitive long arrays rather than boxed queue nodes. Striped locks make
 * near-duplicate admission atomic for overlapping bands, preventing two concurrently arriving
 * near-identical documents from both being accepted.
 */
public final class DocumentDeduplicator {
    public enum Decision {
        ACCEPT,
        EXACT_DUPLICATE,
        NEAR_DUPLICATE
    }

    private static final int BAND_COUNT = 4;
    private static final int STRIPE_COUNT = 64;

    private final int maxHammingDistance;
    private final Set<String> exactHashes = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Integer, LongBucket> bands = new ConcurrentHashMap<>();
    private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

    public DocumentDeduplicator(int maxHammingDistance) {
        if (maxHammingDistance < 0 || maxHammingDistance > 16) {
            throw new IllegalArgumentException("maxHammingDistance must be in [0,16]");
        }
        this.maxHammingDistance = maxHammingDistance;
        Arrays.setAll(stripes, ignored -> new ReentrantLock());
    }

    public Decision classify(KnowledgeDocument document) {
        if (!exactHashes.add(document.contentHash())) {
            return Decision.EXACT_DUPLICATE;
        }

        long simHash = SimHash64.of(document.text());
        int[] keys = new int[BAND_COUNT];
        int[] lockIds = new int[BAND_COUNT];
        for (int band = 0; band < BAND_COUNT; band++) {
            keys[band] = bandKey(simHash, band);
            lockIds[band] = stripe(keys[band]);
        }
        Arrays.sort(lockIds);
        int uniqueLocks = compact(lockIds);
        lock(lockIds, uniqueLocks);
        try {
            for (int key : keys) {
                LongBucket bucket = bands.get(key);
                if (bucket != null && bucket.containsNear(simHash, maxHammingDistance)) {
                    exactHashes.remove(document.contentHash());
                    return Decision.NEAR_DUPLICATE;
                }
            }
            for (int key : keys) {
                bands.computeIfAbsent(key, ignored -> new LongBucket()).add(simHash);
            }
            return Decision.ACCEPT;
        } finally {
            unlockReverse(lockIds, uniqueLocks);
        }
    }

    int exactCount() {
        return exactHashes.size();
    }

    long bandValueCount() {
        long total = 0L;
        for (LongBucket bucket : bands.values()) {
            total += bucket.size;
        }
        return total;
    }

    private void lock(int[] ids, int count) {
        for (int i = 0; i < count; i++) {
            stripes[ids[i]].lock();
        }
    }

    private void unlockReverse(int[] ids, int count) {
        for (int i = count - 1; i >= 0; i--) {
            stripes[ids[i]].unlock();
        }
    }

    private static int compact(int[] sorted) {
        if (sorted.length == 0) {
            return 0;
        }
        int write = 1;
        for (int read = 1; read < sorted.length; read++) {
            if (sorted[read] != sorted[write - 1]) {
                sorted[write++] = sorted[read];
            }
        }
        return write;
    }

    private static int stripe(int key) {
        int mixed = key ^ (key >>> 16);
        return mixed & (STRIPE_COUNT - 1);
    }

    private static int bandKey(long hash, int band) {
        return (band << 16) | (int) ((hash >>> (band * 16)) & 0xffffL);
    }

    private static final class LongBucket {
        private long[] values = new long[4];
        private int size;

        private void add(long value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, Math.multiplyExact(values.length, 2));
            }
            values[size++] = value;
        }

        private boolean containsNear(long value, int maxDistance) {
            for (int i = 0; i < size; i++) {
                if (SimHash64.hamming(value, values[i]) <= maxDistance) {
                    return true;
                }
            }
            return false;
        }
    }
}
