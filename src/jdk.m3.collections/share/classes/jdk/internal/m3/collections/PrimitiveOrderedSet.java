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
import java.util.function.LongConsumer;
import jdk.m3.collections.PrimitiveArrayList;
import jdk.m3.collections.PrimitiveHashSet;
import jdk.m3.collections.PrimitiveKind;

/**
 * Insertion-ordered primitive set.
 *
 * <p>Membership and order share primitive storage: one hash set plus one dense
 * primitive order lane. This spends one extra primitive value per entry instead
 * of a linked-node object and prev/next references.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveOrderedSet.java}
 * blob {@code fdc8ba546a538fd34a97c4b67955a860e3a7cdb9} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveOrderedSet {
    private final PrimitiveKind kind;
    private final PrimitiveHashSet membership;
    private final PrimitiveArrayList order;

    public PrimitiveOrderedSet(PrimitiveKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
        membership = new PrimitiveHashSet(kind);
        order = new PrimitiveArrayList(kind);
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return order.size(); }
    public boolean isEmpty() { return order.isEmpty(); }
    public long payloadBytes() { return membership.payloadBytes() + order.payloadBytes(); }

    public boolean addBits(long bits) {
        if (!membership.addBits(bits)) return false;
        order.addBits(bits);
        return true;
    }

    public boolean containsBits(long bits) { return membership.containsBits(bits); }

    public boolean removeBits(long bits) {
        if (!membership.removeBits(bits)) return false;
        boolean removed = order.removeBits(bits);
        if (!removed) throw new IllegalStateException("order/membership invariant");
        return true;
    }

    public long getBits(int index) { return order.getBits(index); }

    public void forEachBits(LongConsumer consumer) {
        order.forEachBits(Objects.requireNonNull(consumer, "consumer"));
    }

    public void clear() {
        membership.clear();
        order.clear();
    }
}
