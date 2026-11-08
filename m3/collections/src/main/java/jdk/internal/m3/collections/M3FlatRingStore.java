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

/**
 * Generic adaptation of Synexia's packed circular deque, with lazy reference storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/FlatRingStore.java}
 * blob {@code 6745763889051f6f7f2a37d51c60f44063b5a358} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3FlatRingStore<E> extends M3RingStore<E> {
    private static final Object[] EMPTY = new Object[0];
    private Object[] values = EMPTY;
    M3FlatRingStore(int maximum) { super(maximum); }
    @Override int capacity() { return values.length; }
    @Override E readSlot(int slot) { return M3ShapeGuard.read(values, slot); }
    @Override void writeSlot(int slot, E value) { values[slot] = value; }
    @Override void grow() {
        Object[] next = new Object[M3ShapeGuard.grown(values.length, size + 1, maximum)];
        for (int i = 0; i < size; i++) { next[i] = values[slot(i)]; }
        values = next; head = 0;
    }
    @Override void releaseVacated(int slot) { }
    @Override void reset() { values = EMPTY; }
    @Override M3ShapeStorage storage() { return new M3ShapeStorage(values.length, 0, values.length == 0 ? 0 : 1, 0); }
}
