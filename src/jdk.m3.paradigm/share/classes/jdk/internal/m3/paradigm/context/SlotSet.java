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
 * One-word finite context-slot set. All 64 bits are usable.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotSet.java}
 * blob {@code 1d22f22dcd6e8af88a10e902cec5ef61a0f1ae6b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotSet {
    public static final SlotSet EMPTY = new SlotSet(0L);

    private final long bits;

    private SlotSet(long bits) {
        this.bits = bits;
    }

    public static SlotSet ofBits(long bits) {
        return bits == 0L ? EMPTY : new SlotSet(bits);
    }

    public long bits() {
        return bits;
    }

    public int size() {
        return Long.bitCount(bits);
    }

    public boolean isEmpty() {
        return bits == 0L;
    }

    public boolean contains(int slot) {
        return (bits & bit(slot)) != 0L;
    }

    public SlotSet with(int slot) {
        long next = bits | bit(slot);
        return next == bits ? this : ofBits(next);
    }

    public SlotSet without(int slot) {
        long next = bits & ~bit(slot);
        return next == bits ? this : ofBits(next);
    }

    public SlotSet union(SlotSet other) {
        Objects.requireNonNull(other, "other");
        return ofBits(bits | other.bits);
    }

    public SlotSet intersect(SlotSet other) {
        Objects.requireNonNull(other, "other");
        return ofBits(bits & other.bits);
    }

    public void forEach(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long remaining = bits;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(remaining);
            consumer.accept(slot);
            remaining &= remaining - 1L;
        }
    }

    private static long bit(int slot) {
        if (slot < 0 || slot >= Long.SIZE) {
            throw new IndexOutOfBoundsException("slot must be in [0,63]");
        }
        return 1L << slot;
    }
}
