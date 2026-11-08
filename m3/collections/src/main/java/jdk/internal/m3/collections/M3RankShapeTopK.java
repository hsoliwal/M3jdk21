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

import java.util.Objects;
import java.util.function.ObjLongConsumer;

/**
 * Bounded smallest-K admission sharing the priority rank store with maximum polarity.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RankShapeTopK.java}
 * blob {@code 1b57c88c4ad213a03ddf86142f10c19e8b1d8e03} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3RankShapeTopK<E> extends M3ShapeGuard implements M3ShapeTopK<E> {
    private final M3RankStore<E> store;
    M3RankShapeTopK(M3CollectionRequirements required, M3RankStore<E> store) { super(required); this.store = store; }
    @Override public int size() { return store.size; }
    @Override public int capacity() { return required.maximumSize(); }
    @Override public boolean offer(E value, long score) {
        writable(); element(value);
        if (capacity() == 0) { return false; }
        if (size() < capacity()) { store.add(value, score); return true; }
        if (score >= store.boundaryRank()) { return false; }
        store.replaceBoundary(value, score); return true;
    }
    @Override public long cutoff() { return store.boundaryRank(); }
    @Override public E worst() { return store.boundaryValue(); }
    @Override public E removeWorst() { writable(); return store.removeBoundary(); }
    @Override public void clear() { writable(); store.clear(); }
    @Override public M3ShapeStorage storage() { return store.storage(); }
    @Override public void forEach(ObjLongConsumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { store.walk(action); } finally { leave(); }
    }
}
