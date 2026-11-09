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

import jdk.internal.m3.collections.LongOrderedSet;
import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongIterator;

import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Insertion-ordered primitive long set without linked nodes.
 *
 * <p>Membership is an open-addressed primitive set; order is one dense long lane.
 * The value is duplicated across two primitive arrays to buy O(1) expected membership
 * while avoiding a per-entry linked-node object.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongOrderedSet.java}
 * blob {@code 6156e5d4936ca3178d1ba8857059cb1ae10d1f5c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections receipt-aware batch (v6, RECEIPT_MAP): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongOrderedSet implements LongOrderedSet {
    private final PackedLongHashSet membership;
    private final LongArrayList order;

    public PackedLongOrderedSet() {
        this(8);
    }

    public PackedLongOrderedSet(int expectedSize) {
        membership = new PackedLongHashSet(expectedSize);
        order = new LongArrayList(expectedSize);
    }

    public boolean add(long value) {
        if (!membership.add(value)) {
            return false;
        }
        order.add(value);
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    public boolean remove(long value) {
        if (!membership.remove(value)) {
            return false;
        }
        order.removeValue(value);
        return true;
    }

    @Override
    public boolean contains(long value) {
        return membership.contains(value);
    }

    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removed = order.filterInPlace(predicate);
        if (removed != 0) {
            membership.clear();
            membership.addAll(order.toArray());
        }
        return removed;
    }

    @Override
    public int size() {
        return order.size();
    }

    @Override
    public void clear() {
        membership.clear();
        order.clear();
    }

    @Override
    public long[] toArray() {
        return order.toArray();
    }

    @Override
    public LongIterator iterator() {
        return order.iterator();
    }
}
