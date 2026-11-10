/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Exact-name boolean context slots represented by one long.
 *
 * <p>The shared schema owns names. A value instance retains only schema reference
 * plus one primitive word; all 64 bits are usable.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextBitSlots.java}
 * blob {@code 43b837b961b5daceb4daa0964ea5d6871d485c26} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BitSlots {
    private final SlotSchema schema;
    private final long bits;

    private BitSlots(
            SlotSchema schema,
            long bits) {
        this.schema = Objects.requireNonNull(schema, "schema");
        schema.requireCompatible(bits);
        this.bits = bits;
    }

    public static BitSlots empty(
            SlotSchema schema) {
        return new BitSlots(schema, 0L);
    }

    public SlotSchema schema() {
        return schema;
    }

    public long bits() {
        return bits;
    }

    public int size() {
        return Long.bitCount(bits);
    }

    public boolean contains(int slot) {
        Objects.checkIndex(slot, schema.size());
        return (bits & (1L << slot)) != 0L;
    }

    public boolean contains(String exactName) {
        int slot = schema.slot(exactName);
        return slot >= 0 && contains(slot);
    }

    public BitSlots with(
            int slot,
            boolean value) {
        Objects.checkIndex(slot, schema.size());
        long bit = 1L << slot;
        long next = value
                ? bits | bit
                : bits & ~bit;
        return next == bits
                ? this
                : new BitSlots(schema, next);
    }

    public BitSlots with(
            String exactName,
            boolean value) {
        int slot = schema.slot(exactName);
        if (slot < 0) {
            throw new IllegalArgumentException(
                    "Unknown exact context slot: " + exactName);
        }
        return with(slot, value);
    }

    public void forEachSet(IntConsumer consumer) {
        IntConsumer checked =
                Objects.requireNonNull(consumer, "consumer");
        long remaining = bits;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(
                    remaining);
            checked.accept(slot);
            remaining &= remaining - 1L;
        }
    }
}
