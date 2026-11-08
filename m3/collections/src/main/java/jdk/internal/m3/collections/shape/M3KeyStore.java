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
package jdk.internal.m3.collections.shape;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Shared key semantics; physical lookup indices are private to each storage implementation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/KeyStore.java}
 * blob {@code 62c600192d5bc3820e35d25b7ad91dc6410a7ff7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class M3KeyStore<K, V> extends ShapeGuard {
    final boolean withValues;
    int size;
    M3KeyStore(CollectionRequirements required, boolean withValues) {
        super(required); this.withValues = withValues;
    }
    final boolean put(K key, V value) {
        writable(); element(key);
        if (withValues) { element(value); }
        int hash = hash(key);
        int at = find(key, hash);
        if (at >= 0) { if (withValues) { value(at, value); } return false; }
        growing(size); insert(key, value, hash); return true;
    }
    final boolean contains(K key) { element(key); return find(key, hash(key)) >= 0; }
    final V get(K key, V fallback) {
        element(key); int at = find(key, hash(key)); return at < 0 ? fallback : value(at);
    }
    final boolean remove(K key) {
        writable(); element(key); int at = find(key, hash(key));
        if (at < 0) { return false; }
        erase(at); return true;
    }
    final void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { walk(action); } finally { leave(); }
    }
    final void clear() { writable(); reset(); }
    abstract int find(K key, int hash);
    abstract V value(int at);
    abstract void value(int at, V value);
    abstract void insert(K key, V value, int hash);
    abstract void erase(int at);
    abstract void walk(BiConsumer<? super K, ? super V> action);
    abstract void reset();
    abstract ShapeStorage storage();
}
