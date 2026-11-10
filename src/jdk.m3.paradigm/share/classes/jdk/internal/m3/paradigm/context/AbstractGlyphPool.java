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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.collections.TokenInterner;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractGlyphPool - Base implementation for glyph pools (Flyweight Pattern).
 *
 * <p>Provides heavy interning for identity stability:
 * <ul>
 *   <li>One instance per semantic concept</li>
 *   <li>Thread-safe via ConcurrentHashMap</li>
 *   <li>Pointer equality works after interning</li>
 *   <li>Cache friendliness through canonical instances</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doIntern(String, Object)} - Custom interning logic</li>
 *   <li>{@link #doGet(String)} - Custom retrieval logic</li>
 *   <li>{@link #shouldIntern(String, Object)} - Interning policy</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractGlyphPool.java}
 * blob {@code c163d4c6fa7e01b26ade2bf6d347a901df42298f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractGlyphPool implements IGlyphPool {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractGlyphPool.class);


    private final Map<String, Object> pool;

    /** Create a glyph pool with default ConcurrentHashMap. */
    protected AbstractGlyphPool() {
        this.pool = new ConcurrentHashMap<>();
    }

    /**
     * Create a glyph pool with custom backing map.
     *
     * @param backingMap the backing map (should be thread-safe)
     */
    protected AbstractGlyphPool(Map<String, Object> backingMap) {
        this.pool = backingMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T intern(String key, T value) {
        if (!shouldIntern(key, value)) {
            return value;
        }
        Optional<T> result = doIntern(key, value);
        if (result.isPresent()) {
            return result.get();
        }
        return (T) pool.computeIfAbsent(key, k -> value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Optional<T> result = doGet(key);
        return result.isPresent() ? result.get() : (T) pool.get(key);
    }

    @Override
    public boolean contains(String key) {
        return pool.containsKey(key);
    }

    @Override
    public int size() {
        return pool.size();
    }

    @Override
    public void clear() {
        pool.clear();
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Custom interning logic.
     *
     * @param key the interning key
     * @param value the value to intern
     * @param <T> value type
     * @return interned value or empty to use default behavior
     */
    protected <T> Optional<T> doIntern(String key, T value) {
        return Optional.empty(); //FLLDCIM: MRL — FM-36 return null -> Optional.empty()
    }

    /**
     * Custom retrieval logic.
     *
     * @param key the key to look up
     * @param <T> value type
     * @return retrieved value or empty to use default behavior
     */
    protected <T> Optional<T> doGet(String key) {
        return Optional.empty(); //FLLDCIM: MRL — FM-36 return null -> Optional.empty()
    }

    /**
     * Determine if a value should be interned.
     *
     * @param key the interning key
     * @param value the value to potentially intern
     * @return true if value should be interned
     */
    protected boolean shouldIntern(String key, Object value) {
        return true; // Default: always intern
    }

    /**
     * Get the underlying pool map.
     *
     * @return the pool map
     */
    protected Map<String, Object> getPool() {
        return pool;
    }
}
