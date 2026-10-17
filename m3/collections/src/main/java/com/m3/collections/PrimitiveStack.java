/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/** LIFO semantics over the shared exact-width primitive list core. */
public final class PrimitiveStack {
    private final PrimitiveArrayList values;

    public PrimitiveStack(PrimitiveKind kind) {
        values = new PrimitiveArrayList(Objects.requireNonNull(kind, "kind"));
    }

    public PrimitiveKind kind() { return values.kind(); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes(); }

    public void pushBits(long bits) { values.addBits(bits); }

    public long peekBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.getBits(values.size() - 1);
    }

    public long popBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.removeAtBits(values.size() - 1);
    }

    public void clear() { values.clear(); }
}
