/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Insertion-ordered primitive set.
 *
 * <p>Membership and order share primitive storage: one hash set plus one dense
 * primitive order lane. This spends one extra primitive value per entry instead
 * of a linked-node object and prev/next references.
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
