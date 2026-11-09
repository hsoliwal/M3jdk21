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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lazy flat reference sequence; shifts for positional edits and copies when growing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/FlatShapeSequence.java}
 * blob {@code 027c9daf5a9d74adcd815b0058092966eda3a675} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class FlatShapeSequence<E> extends ShapeGuard implements ShapeSequence<E> {
    private static final Object[] EMPTY = new Object[0];
    private Object[] values = EMPTY;
    private int size;
    FlatShapeSequence(CollectionRequirements required) { super(required); }
    @Override public int size() { return size; }
    @Override public void add(E value) { insert(size, value); }
    @Override public void insert(int index, E value) {
        writable(); position(index, size); element(value); growing(size);
        if (size == values.length) { values = Arrays.copyOf(values, grown(values.length, size + 1, required.maximumSize())); }
        System.arraycopy(values, index, values, index + 1, size - index);
        values[index] = value; size++;
    }
    @Override public E get(int index) { index(index, size); return read(values, index); }
    @Override public E set(int index, E value) {
        writable(); index(index, size); element(value);
        E old = read(values, index); values[index] = value; return old;
    }
    @Override public E removeAt(int index) {
        writable(); index(index, size); E old = read(values, index);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        values[--size] = null; return old;
    }
    @Override public void clear() { writable(); values = EMPTY; size = 0; }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { for (int i = 0; i < size; i++) { action.accept(read(values, i)); } }
        finally { leave(); }
    }
    @Override public ShapeStorage storage() { return new ShapeStorage(values.length, 0, values.length == 0 ? 0 : 1, 0); }
}
