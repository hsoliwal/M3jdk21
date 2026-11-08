/*******************************************************************************
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

import java.util.function.BiConsumer;

/**
 * Mutable key/value shape. Null and equality policies come from its factory request.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeMap.java}
 * blob {@code 649d0892b860a836e3183b7584f18a0d61e956bf} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeMap<K, V> extends M3ShapeContainer {
    /** Insert or replace; true exactly when a previously absent key is inserted. */
    boolean put(K key, V value);
    /** Test presence independently of stored null values. */
    boolean containsKey(K key);
    /** Return the stored value, including null, or fallback only if absent. */
    V getOrDefault(K key, V fallback);
    /** Remove a key; true exactly when present. */
    boolean remove(K key);
    /** Traverse without per-entry wrappers; callback mutation of this map is rejected. */
    void forEach(BiConsumer<? super K, ? super V> action);
}
