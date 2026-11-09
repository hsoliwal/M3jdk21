/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
/**
 * Index-based lazy loader with soft reference caching.
 *
 * <p>
 * Entries may be reclaimed by GC under memory pressure. Useful for large
 * datasets where not all entries need to stay in memory.
 * </p>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/SoftLazyIndex.java}
 * blob {@code f5c8d7f55fec656eda2803cde42d41257977d1f6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SoftLazyIndex<K, V> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(SoftLazyIndex.class);


    private final Function<K, V> loader;
    private final Map<K, SoftReference<V>> cache = new ConcurrentHashMap<>();

    SoftLazyIndex(Function<K, V> loader) {
        this.loader = Objects.requireNonNull(loader, "Loader must not be null");
    }

    /**
     * Gets value by key (loads and soft-caches on access).
     *
     * @param key
     *            the key
     * @return the value
     */
    public V get(K key) {
        SoftReference<V> ref = cache.get(key);
        V value = ref != null ? ref.get() : null;

        if (value == null) {
            value = loader.apply(key);
            cache.put(key, new SoftReference<>(value));
        }
        return value;
    }

    /**
     * Gets value if cached and not GC'd.
     *
     * @param key
     *            the key
     * @return Optional containing value if present
     */
    public Optional<V> getIfPresent(K key) {
        SoftReference<V> ref = cache.get(key);
        return ref != null ? Optional.ofNullable(ref.get()) : Optional.empty();
    }

    /**
     * Invalidates a cached entry.
     *
     * @param key
     *            the key to invalidate
     */
    public void invalidate(K key) {
        cache.remove(key);
    }

    /** Clears all cached entries. */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Cleans up entries where soft reference was GC'd.
     *
     * @return number of entries cleaned
     */
    public int cleanup() {
        int sizeBefore = cache.size();
        cache.values().removeIf(ref -> ref.get() == null);
        return sizeBefore - cache.size();
    }
}
