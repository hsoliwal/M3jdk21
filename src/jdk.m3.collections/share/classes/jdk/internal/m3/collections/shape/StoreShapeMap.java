/* *****************************************************************************
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
package jdk.internal.m3.collections.shape;

import java.util.function.BiConsumer;

/**
 * Map semantic façade over an admitted key-store implementation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/StoreShapeMap.java}
 * blob {@code b85a8888b0526cc3fc8fcd6fe19f91176ecf6d75} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class StoreShapeMap<K, V> implements ShapeMap<K, V> {
    private final M3KeyStore<K, V> store;
    StoreShapeMap(M3KeyStore<K, V> store) { this.store = store; }
    @Override public boolean put(K key, V value) { return store.put(key, value); }
    @Override public boolean containsKey(K key) { return store.contains(key); }
    @Override public V getOrDefault(K key, V fallback) { return store.get(key, fallback); }
    @Override public boolean remove(K key) { return store.remove(key); }
    @Override public void forEach(BiConsumer<? super K, ? super V> action) { store.forEach(action); }
    @Override public int size() { return store.size; }
    @Override public void clear() { store.clear(); }
    @Override public ShapeStorage storage() { return store.storage(); }
}
