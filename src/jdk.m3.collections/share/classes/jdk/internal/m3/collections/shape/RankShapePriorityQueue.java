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
import java.util.function.ObjLongConsumer;

/**
 * Guarded least-priority facade over a selected rank store.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RankShapePriorityQueue.java}
 * blob {@code d4294e8fea1fb4ff40ceb1e386260f5fb2b05a92} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class RankShapePriorityQueue<E> extends ShapeGuard implements ShapePriorityQueue<E> {
    private final RankStore<E> store;
    RankShapePriorityQueue(CollectionRequirements required, RankStore<E> store) { super(required); this.store = store; }
    @Override public int size() { return store.size; }
    @Override public void add(E value, long priority) { writable(); element(value); growing(size()); store.add(value, priority); }
    @Override public long firstPriority() { return store.boundaryRank(); }
    @Override public E first() { return store.boundaryValue(); }
    @Override public E removeFirst() { writable(); return store.removeBoundary(); }
    @Override public void clear() { writable(); store.clear(); }
    @Override public ShapeStorage storage() { return store.storage(); }
    @Override public void forEach(ObjLongConsumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { store.walk(action); } finally { leave(); }
    }
}
