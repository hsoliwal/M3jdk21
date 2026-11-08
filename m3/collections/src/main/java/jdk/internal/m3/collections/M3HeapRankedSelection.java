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

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Guarded, lazily created adapter over the existing stable M3DenseTopK owner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/HeapRankedSelection.java}
 * blob {@code a85d2b9647ced8341b88e17fa6cf27b372a275d8} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3HeapRankedSelection<E> extends M3AbstractRankedSelection<E> {
    private M3DenseTopK<E> heap;
    M3HeapRankedSelection(M3CollectionRequirements required, Comparator<? super E> comparator) { super(required, comparator); }
    @Override public int size() { return heap == null ? 0 : heap.size(); }
    @Override boolean admit(E value) {
        if (heap == null) { heap = new M3DenseTopK<>(capacity(), comparator); }
        return heap.offer(value);
    }
    @Override public E worst() {
        if (isEmpty()) { throw new NoSuchElementException(); } return heap.peekWorst();
    }
    @Override List<E> snapshot() { return heap == null ? List.of() : heap.snapshotBestFirst(); }
    @Override void walk(Consumer<? super E> action) { if (heap != null) { heap.forEachRetained(action); } }
    @Override public void clear() { writable(); heap = null; }
    @Override public M3ShapeStorage storage() {
        return heap == null ? new M3ShapeStorage(0, 0, 0, 0) : new M3ShapeStorage(capacity(), 8L * capacity(), 2, 0);
    }
}
