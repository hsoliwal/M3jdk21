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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Set semantic façade over a key-only store; no dummy value lane is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/StoreShapeSet.java}
 * blob {@code c0cbc7c5f64ad44b2beb067223c60f3b4d2b587e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class StoreShapeSet<E> implements ShapeSet<E> {
    private final M3KeyStore<E, Object> store;
    StoreShapeSet(M3KeyStore<E, Object> store) { this.store = store; }
    @Override public boolean add(E value) { return store.put(value, null); }
    @Override public boolean contains(E value) { return store.contains(value); }
    @Override public boolean remove(E value) { return store.remove(value); }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        store.forEach((key, ignored) -> action.accept(key));
    }
    @Override public int size() { return store.size; }
    @Override public void clear() { store.clear(); }
    @Override public ShapeStorage storage() { return store.storage(); }
}
