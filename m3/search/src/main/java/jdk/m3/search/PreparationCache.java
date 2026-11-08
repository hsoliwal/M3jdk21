/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/PreparationCache.java (blob 1634b9e6b47f) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entry-count and key-length bounded LRU. Compilation happens outside the monitor.
 * Concurrent misses may compile the same key, but publish only one retained value.
 * Failed and oversized preparations are never retained. No result IDs are cached.
 */
final class PreparationCache<V> implements AutoCloseable {
    @FunctionalInterface
    interface Loader<V> {
        V load(String key) throws IOException;
    }

    private final int capacity;
    private final int maxKeyCharacters;
    private final Map<String, V> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long hits;
    private long misses;
    private long evictions;
    private boolean closed;

    PreparationCache(int capacity, int maxKeyCharacters) {
        if (capacity < 0 || maxKeyCharacters < 0) {
            throw new IllegalArgumentException("cache bounds must be non-negative");
        }
        this.capacity = capacity;
        this.maxKeyCharacters = maxKeyCharacters;
    }

    V get(String key, Loader<V> loader) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(loader, "loader");
        boolean cacheable = capacity != 0 && key.length() <= maxKeyCharacters;
        synchronized (this) {
            ensureOpen();
            if (cacheable) {
                V found = entries.get(key);
                if (found != null) {
                    hits++;
                    return found;
                }
            }
            misses++;
        }
        V loaded = Objects.requireNonNull(loader.load(key), "prepared value");
        synchronized (this) {
            ensureOpen();
            if (!cacheable) {
                return loaded;
            }
            V published = entries.get(key);
            if (published != null) {
                return published;
            }
            if (entries.size() == capacity) {
                var iterator = entries.entrySet().iterator();
                iterator.next();
                iterator.remove();
                evictions++;
            }
            entries.put(key, loaded);
            return loaded;
        }
    }

    synchronized QueryPreparationStats stats() {
        return new QueryPreparationStats(hits, misses, evictions, entries.size());
    }

    @Override
    public synchronized void close() {
        closed = true;
        entries.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("query preparation cache is closed");
        }
    }
}
