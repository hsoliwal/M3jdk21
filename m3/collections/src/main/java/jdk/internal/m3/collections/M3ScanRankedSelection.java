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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Sorted dense small-K storage: compare first, then commit one reference shift.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ScanRankedSelection.java}
 * blob {@code 823fdd4037ba87e1bc62d5f279c98e2e93c72912} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3ScanRankedSelection<E> extends M3AbstractRankedSelection<E> {
    private static final Object[] EMPTY = new Object[0];
    private Object[] values = EMPTY;
    private int size;
    M3ScanRankedSelection(M3CollectionRequirements required, Comparator<? super E> comparator) { super(required, comparator); }
    @Override public int size() { return size; }
    @Override boolean admit(E value) {
        if (size == capacity() && comparator.compare(value, read(values, size - 1)) >= 0) { return false; }
        int at = size;
        while (at > 0 && comparator.compare(value, read(values, at - 1)) < 0) { at--; }
        int next = Math.min(size + 1, capacity());
        if (next > values.length) { values = Arrays.copyOf(values, grown(values.length, next, capacity())); }
        System.arraycopy(values, at, values, at + 1, next - at - 1);
        values[at] = value; size = next; return true;
    }
    @Override public E worst() { if (size == 0) { throw new NoSuchElementException(); } return read(values, size - 1); }
    @Override List<E> snapshot() {
        var result = new ArrayList<E>(size);
        for (int i = 0; i < size; i++) { result.add(read(values, i)); } return List.copyOf(result);
    }
    @Override void walk(Consumer<? super E> action) { for (int i = 0; i < size; i++) { action.accept(read(values, i)); } }
    @Override public void clear() { writable(); values = EMPTY; size = 0; }
    @Override public M3ShapeStorage storage() { return new M3ShapeStorage(values.length, 0, values.length == 0 ? 0 : 1, 0); }
}
