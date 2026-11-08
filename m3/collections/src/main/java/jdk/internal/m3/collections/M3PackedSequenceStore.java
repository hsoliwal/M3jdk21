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

import java.util.Arrays;

/**
 * Payload lane for any number of independent rank-indexed sequences.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/PackedSequenceStore.java}
 * blob {@code 0d6cfa8aa13bc1fc67152bb5cbc504cc53479853} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PackedSequenceStore<E> extends M3IntAvlForest {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("serial") // element storage; serializability of the values is the caller's contract
    private Object[][] values = new Object[0][];

    @Override void growExtraDirectory(int length) { values = Arrays.copyOf(values, length); }
    @Override void allocateExtraLeaf(int segment) { values[segment] = new Object[LEAF_SIZE]; }
    @Override void clearExtraSlot(int slot) { values[slot >>> SHIFT][slot & MASK] = null; }
    @Override void clearExtraLeaf(int segment) { Arrays.fill(values[segment], null); }

    @SuppressWarnings("unchecked")
    E value(int slot) { return (E) values[slot >>> SHIFT][slot & MASK]; }

    E value(int slot, E value) {
        E previous = value(slot);
        values[slot >>> SHIFT][slot & MASK] = value;
        return previous;
    }

    int insert(int root, int rank, E value) {
        if (rank < 0 || rank > count(root)) throw new IndexOutOfBoundsException("rank=" + rank);
        int slot = allocate();
        values[slot >>> SHIFT][slot & MASK] = value;
        return insertSlot(root, rank, slot);
    }
}
