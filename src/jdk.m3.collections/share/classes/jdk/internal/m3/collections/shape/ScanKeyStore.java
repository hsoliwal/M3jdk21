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

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Lazy dense scan store; insertion order, no hash metadata, suited by policy to tiny collections.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ScanKeyStore.java}
 * blob {@code 97f35b18439e3df4d040ae70cb542f0c98ab7171} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class ScanKeyStore<K, V> extends M3KeyStore<K, V> {
    private static final Object[] EMPTY = new Object[0];
    private Object[] keys = EMPTY;
    private Object[] values = EMPTY;
    ScanKeyStore(CollectionRequirements required, boolean values) { super(required, values); }
    @Override int find(K key, int hash) {
        for (int i = 0; i < size; i++) { if (equivalent(key, keys[i])) { return i; } }
        return -1;
    }
    @Override V value(int at) { return withValues ? read(values, at) : null; }
    @Override void value(int at, V value) { values[at] = value; }
    @Override void insert(K key, V value, int hash) {
        if (size == keys.length) {
            int capacity = grown(keys.length, size + 1, required.maximumSize());
            Object[] newKeys = Arrays.copyOf(keys, capacity);
            Object[] newValues = withValues ? Arrays.copyOf(values, capacity) : EMPTY;
            keys = newKeys; values = newValues;
        }
        keys[size] = key;
        if (withValues) { values[size] = value; }
        size++;
    }
    @Override void erase(int at) {
        int moved = size - at - 1;
        System.arraycopy(keys, at + 1, keys, at, moved);
        if (withValues) { System.arraycopy(values, at + 1, values, at, moved); }
        keys[--size] = null;
        if (withValues) { values[size] = null; }
    }
    @Override void walk(BiConsumer<? super K, ? super V> action) {
        for (int i = 0; i < size; i++) { action.accept(read(keys, i), value(i)); }
    }
    @Override void reset() { keys = EMPTY; values = EMPTY; size = 0; }
    @Override ShapeStorage storage() { return new ShapeStorage((long) keys.length + values.length, 0, keys.length == 0 ? 0 : (withValues ? 2 : 1), 0); }
}
