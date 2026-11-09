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
package jdk.internal.m3.collections.packed;

/**
 * Positional AVL list: O(log n) indexed edits without moving surviving payloads.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/PackedUnsortedList.java}
 * blob {@code a2bdaac2fb46792240d34547e5c7f7e3b7d53319} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedUnsortedList<E> extends PackedListBase<E> {
    private static final long serialVersionUID = 1L;
    public PackedUnsortedList() { }

    @Override public boolean add(E value) { add(size(), value); return true; }

    @Override public void add(int index, E value) {
        nonNull(value);
        insert(index, value);
    }

    public void addToHead(E value) { add(0, value); }

    @Override public E set(int index, E value) {
        nonNull(value);
        return store.value(store.at(root, index), value);
    }

    private static void nonNull(Object value) {
        if (value == null) throw new IllegalArgumentException("Null payload");
    }
}
