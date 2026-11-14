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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
/**
 * Index-based lazy loader with caching.
 *
 * <p>
 * Values are loaded on-demand by key and cached. Ideal for pointer-based file
 * access where keys map to byte offsets.
 * </p>
 *
 * <h2>Integration with IndexedLineReader</h2>
 *
 * <pre>{@code
 * Map<String, Long> offsets = ...; // uid -> byte offset
 * LazyIndex<String, String> rows = LazyOps.lazyIndex(uid -> reader.readAt(offsets.get(uid)));
 * }</pre>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/LazyIndex.java}
 * blob {@code 8e7322ebf80d54c654a933a507bbfc92e997df29} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LazyIndex<K, V> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LazyIndex.class);


    private final Function<K, V> loader;
    private final Map<K, V> cache = new ConcurrentHashMap<>();

    LazyIndex(Function<K, V> loader) {
        this.loader = Objects.requireNonNull(loader, "Loader must not be null");
    }

    /**
     * Gets value by key (loads and caches on first access).
     *
     * @param key
     *            the key
     * @return the value (may be null if loader returns null)
     */
    public V get(K key) {
        return cache.computeIfAbsent(key, loader);
    }

    /**
     * Gets value if already cached.
     *
     * @param key
     *            the key
     * @return Optional containing value if cached
     */
    public Optional<V> getIfPresent(K key) {
        return Optional.ofNullable(cache.get(key));
    }

    /**
     * Checks if key is cached.
     *
     * @param key
     *            the key
     * @return true if value is cached
     */
    public boolean isLoaded(K key) {
        return cache.containsKey(key);
    }

    /**
     * Invalidates a cached entry (next get() will reload).
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
     * Returns the number of cached entries.
     *
     * @return cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Preloads values for given keys.
     *
     * @param keys
     *            keys to preload
     */
    public void preload(Iterable<K> keys) {
        keys.forEach(this::get);
    }
}
