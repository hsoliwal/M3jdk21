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
import java.util.function.Consumer;

/**
 * Guarded deque facade over one selected native ring store.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RingShapeDeque.java}
 * blob {@code baaba425e3e675bca3965bbef537c8ef3fee46b1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3RingShapeDeque<E> extends M3ShapeGuard implements M3ShapeDeque<E> {
    private final M3RingStore<E> ring;
    M3RingShapeDeque(M3CollectionRequirements required, M3RingStore<E> ring) { super(required); this.ring = ring; }
    @Override public int size() { return ring.size; }
    @Override public void addFirst(E value) { writable(); element(value); growing(size()); ring.addFirst(value); }
    @Override public void addLast(E value) { writable(); element(value); growing(size()); ring.addLast(value); }
    @Override public E first() { return ring.first(); }
    @Override public E last() { return ring.last(); }
    @Override public E get(int index) { return ring.get(index); }
    @Override public E removeFirst() { writable(); return ring.removeFirst(); }
    @Override public E removeLast() { writable(); return ring.removeLast(); }
    @Override public void clear() { writable(); ring.clear(); }
    @Override public M3ShapeStorage storage() { return ring.storage(); }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { ring.walk(action); } finally { leave(); }
    }
}
