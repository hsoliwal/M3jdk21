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
import java.util.function.Consumer;

/**
 * Guarded latest-N admission over the same ring engine as the deque.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RingShapeWindow.java}
 * blob {@code 101f23ec1b659311e88d8d0c57198503591dcd72} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class RingShapeWindow<E> extends ShapeGuard implements ShapeWindow<E> {
    private final RingStore<E> ring;
    RingShapeWindow(CollectionRequirements required, RingStore<E> ring) { super(required); this.ring = ring; }
    @Override public int size() { return ring.size; }
    @Override public int capacity() { return required.maximumSize(); }
    @Override public boolean append(E value) { writable(); element(value); return ring.appendWindow(value); }
    @Override public E get(int index) { return ring.get(index); }
    @Override public void clear() { writable(); ring.clear(); }
    @Override public ShapeStorage storage() { return ring.storage(); }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { ring.walk(action); } finally { leave(); }
    }
}
