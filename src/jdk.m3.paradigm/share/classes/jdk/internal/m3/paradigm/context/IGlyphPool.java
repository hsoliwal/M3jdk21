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

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Glyph Pool - Heavy Interning for Identity Stability (Flyweight Pattern).
 *
 * <h2>Why Interning Is Mandatory</h2>
 * Without interning:
 * <ul>
 *   <li>Same intent/emotion objects multiply</li>
 *   <li>Memory explodes</li>
 *   <li>Equality becomes expensive</li>
 * </ul>
 *
 * <h2>With Interning</h2>
 * <ul>
 *   <li>One instance per semantic concept</li>
 *   <li>Pointer equality works</li>
 *   <li>Cache friendliness skyrockets</li>
 * </ul>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Flyweight Pattern - heavy interning for identity stability and pointer equality",
    constraints = {
        "One instance per semantic concept",
        "Thread-safe via ConcurrentHashMap",
        "Interned values are immutable"
    },
    invariants = {
        "intern() returns same instance for same key",
        "size() returns current pool size",
        "contains() accurately reflects membership"
    },
    examples = {
        "IGlyphPool.INSTANCE.intern(key, value)",
        "pool.internIntent(\"query\")",
        "pool.internEmotion(\"joy\")"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IGlyphPool.java}
 * blob {@code 8692eda036ee1698b23f6c25cf10f5133614e58c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IGlyphPool {

    /** Global singleton instance. */
    IGlyphPool INSTANCE = new DefaultGlyphPool();

    <T> T intern(String key, T value);
    <T> T get(String key);
    boolean contains(String key);
    int size();
    void clear();

    default String internIntent(String intent) {
        return intern("INTENT:" + intent, intent);
    }

    default String internEmotion(String emotion) {
        return intern("EMOTION:" + emotion, emotion);
    }

    default String internDirection(String direction) {
        return intern("DIRECTION:" + direction, direction);
    }

    /** Default implementation using ConcurrentHashMap. */
    final class DefaultGlyphPool implements IGlyphPool {
        private final Map<String, Object> pool = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T intern(String key, T value) {
            return (T) pool.computeIfAbsent(key, k -> value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key) { return (T) pool.get(key); }

        @Override
        public boolean contains(String key) { return pool.containsKey(key); }

        @Override
        public int size() { return pool.size(); }

        @Override
        public void clear() { pool.clear(); }
    }
}
