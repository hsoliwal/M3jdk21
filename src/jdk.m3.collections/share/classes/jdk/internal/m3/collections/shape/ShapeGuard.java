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

/**
 * Shared request guards and reentrant traversal exclusion for native mutable shapes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeGuard.java}
 * blob {@code 341e070003b6ddac195b5fe96533704934c780b5} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class ShapeGuard {
    final CollectionRequirements required;
    private int traversals;
    ShapeGuard(CollectionRequirements required) { this.required = Objects.requireNonNull(required, "required"); }
    final void writable() {
        if (traversals != 0) { throw new IllegalStateException("mutation during shape traversal"); }
    }
    final void element(Object value) {
        if (!required.allowNull()) { Objects.requireNonNull(value, "null rejected by collection request"); }
    }
    final void growing(int size) {
        if (size >= required.maximumSize()) { throw new IllegalStateException("maximum live size reached"); }
    }
    final void enter() { traversals = Math.addExact(traversals, 1); }
    final void leave() { traversals--; }
    final boolean equivalent(Object a, Object b) {
        return required.equality() == CollectionRequirements.Equality.IDENTITY ? a == b : Objects.equals(a, b);
    }
    final int hash(Object key) {
        int h = required.equality() == CollectionRequirements.Equality.IDENTITY
                ? System.identityHashCode(key) : Objects.hashCode(key);
        return h ^ (h >>> 16);
    }
    static void index(int index, int size) {
        if (index < 0 || index >= size) { throw new IndexOutOfBoundsException("index=" + index + ", size=" + size); }
    }
    static void position(int index, int size) {
        if (index < 0 || index > size) { throw new IndexOutOfBoundsException("position=" + index + ", size=" + size); }
    }
    static int grown(int old, int minimum, int maximum) {
        return (int) Math.min(maximum, Math.max(minimum, Math.max(4L, old + (long) old / 2)));
    }
    @SuppressWarnings("unchecked")
    static <T> T read(Object[] array, int index) { return (T) array[index]; }
}
