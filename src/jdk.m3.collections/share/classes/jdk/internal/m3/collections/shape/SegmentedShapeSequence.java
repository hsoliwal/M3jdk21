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
 * Indexed reference sequence with stable payload blocks; positional edits still shift elements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SegmentedShapeSequence.java}
 * blob {@code 484fee9a39ded52050d889d5ecaced364f7cf384} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SegmentedShapeSequence<E> extends ShapeGuard implements ShapeSequence<E> {
    private SegmentedObjectLane28<E> values = new SegmentedObjectLane28<>();
    private int size;
    SegmentedShapeSequence(CollectionRequirements required) { super(required); }
    @Override public int size() { return size; }
    @Override public void add(E value) { insert(size, value); }
    @Override public void insert(int index, E value) {
        writable(); position(index, size); element(value); growing(size);
        values.copyWithin(index, index + 1, size - index);
        values.set(index, value); size++;
    }
    @Override public E get(int index) { index(index, size); return values.get(index); }
    @Override public E set(int index, E value) {
        writable(); index(index, size); element(value);
        E old = values.get(index); values.set(index, value); return old;
    }
    @Override public E removeAt(int index) {
        writable(); index(index, size); E old = values.get(index);
        values.copyWithin(index + 1, index, size - index - 1);
        values.set(--size, null); return old;
    }
    @Override public void clear() { writable(); values = new SegmentedObjectLane28<>(); size = 0; }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { for (int i = 0; i < size; i++) { action.accept(values.get(i)); } }
        finally { leave(); }
    }
    @Override public ShapeStorage storage() { return new ShapeStorage(values.referenceSlots(), 0, values.blocks(), 0); }
}
