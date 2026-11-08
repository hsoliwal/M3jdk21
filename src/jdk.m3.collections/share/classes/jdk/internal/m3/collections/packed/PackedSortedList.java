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
package jdk.internal.m3.collections.packed;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Stable sorted list with segmented payloads and primitive AVL relationships.
 * Comparator equality defines contains/remove membership; indexOf uses equals.
 * Indexed add/set are unsupported. No Node extension API is exposed.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/PackedSortedList.java}
 * blob {@code 7ab0cbcc45207ce10c756d0641e81618d20a0fb8} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public class PackedSortedList<E> extends PackedListBase<E> {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("serial") // comparator serializability is the caller's contract
    private final Comparator<? super E> comparator;

    public PackedSortedList(Comparator<? super E> comparator) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    @Override public boolean add(E value) {
        if (value == null) return false;
        int expected = modCount;
        int rank = 0, s = root;
        while (s != 0) {
            int comparison = comparator.compare(value, store.value(s));
            if (expected != modCount) throw new ConcurrentModificationException();
            if (comparison < 0) s = store.left(s);
            else { rank += store.count(store.left(s)) + 1; s = store.right(s); }
        }
        // Comparisons complete before allocating a slot or mutating membership.
        insert(rank, value);
        return true;
    }

    private int firstEqual(E value) {
        int s = root, found = 0, expected = modCount;
        while (s != 0) {
            int comparison = comparator.compare(store.value(s), value);
            if (expected != modCount) throw new ConcurrentModificationException();
            if (comparison < 0) s = store.right(s);
            else { if (comparison == 0) found = s; s = store.left(s); }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    @Override public boolean contains(Object value) {
        return value != null && root != 0 && firstEqual((E) value) != 0;
    }

    @SuppressWarnings("unchecked")
    @Override public boolean remove(Object value) {
        if (value == null || root == 0) return false;
        final int slot;
        try { slot = firstEqual((E) value); }
        catch (ClassCastException incompatible) { return false; }
        if (slot == 0) return false;
        root = store.removeSlot(root, slot);
        modCount++;
        return true;
    }
}
