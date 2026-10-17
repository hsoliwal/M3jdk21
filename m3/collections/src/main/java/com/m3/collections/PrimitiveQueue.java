/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/** FIFO semantics over the shared exact-width primitive deque core. */
public final class PrimitiveQueue {
    private final PrimitiveArrayDeque values;

    public PrimitiveQueue(PrimitiveKind kind) {
        values = new PrimitiveArrayDeque(Objects.requireNonNull(kind, "kind"));
    }

    public PrimitiveKind kind() { return values.kind(); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes(); }

    public void offerBits(long bits) { values.addLastBits(bits); }

    public long peekBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.peekFirstBits();
    }

    public long pollBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.removeFirstBits();
    }

    public void clear() { values.clear(); }
}
