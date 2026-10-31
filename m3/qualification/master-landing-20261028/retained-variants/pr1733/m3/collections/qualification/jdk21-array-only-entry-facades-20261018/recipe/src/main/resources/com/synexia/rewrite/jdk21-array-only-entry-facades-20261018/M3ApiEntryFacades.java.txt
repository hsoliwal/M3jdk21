/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the canonical Synexia array-only facade recipe.
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Optional API-only immutable Entry facade reuse above canonical array storage.
 *
 * <p>The source collection keeps keys/values in its existing arrays and uses
 * primitive indexes/masks for behavior. This helper is used ONLY when an API
 * explicitly asks for a detached {@code Map.Entry}. Its small LFU cache is an
 * evictable *derived* object cache, never the authoritative map representation.
 * A held immutable Entry is never repointed or mutated by cache replacement.
 *
 * <p>The handle must include the owner's slot generation. Call invalidate on
 * structural changes, reusing physical slots, or when stronger invalidation is
 * required. Key and value reference-identity checks prevent returning stale
 * snapshot references after ordinary value replacement. It intentionally does
 * not replace iterator-returned live Entry implementations supporting setValue:
 * those require a separate contract-qualified, fail-fast/live API facade.
 */
public final class M3ApiEntryFacades<K, V> {
    private final M3LFUCache<Map.Entry<K, V>> cache;

    public M3ApiEntryFacades(int maximumFacades) {
        cache = new M3LFUCache<>(maximumFacades);
    }

    /** An explicitly requested detached, immutable API Entry; never canonical storage. */
    public Map.Entry<K, V> snapshot(long stableHandle, K key, V value) {
        Map.Entry<K, V> existing = cache.peek(stableHandle);
        if (existing != null && existing.getKey() == key && existing.getValue() == value) {
            return cache.get(stableHandle);
        }
        Map.Entry<K, V> created = new AbstractMap.SimpleImmutableEntry<>(key, value);
        cache.put(stableHandle, created);
        return created;
    }

    public void invalidate(long handle) { cache.invalidate(handle); }
    public void invalidateAll() { cache.clear(); }
    public void setMaximumFacades(int max) { cache.setMaximumEntries(max); }
    public void trimToLiveFacades() { cache.compactStorage(); }
    public int cachedFacades() { return cache.size(); }
    public long cacheHits() { return cache.hits(); }
    public long cacheEvictions() { return cache.evictions(); }
    public void verifyInvariants() { cache.verifyInvariants(); }
}
