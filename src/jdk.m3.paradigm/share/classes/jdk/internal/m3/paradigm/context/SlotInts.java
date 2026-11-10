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

import java.util.Arrays;

/**
 * Sparse immutable primitive-int values keyed by shared context slots.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotInts.java}
 * blob {@code 38164c5ec3d51ef6b3470721a754a390c60dc4ec} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotInts {
    public static final SlotInts EMPTY = new SlotInts(0L, new int[0]);

    private final long present;
    private final int[] values;

    private SlotInts(long present, int[] values) {
        this.present = present;
        this.values = values;
    }

    public long presentMask() { return present; }
    public int size() { return values.length; }
    public boolean isEmpty() { return present == 0L; }
    public boolean contains(int slot) { return (present & bit(slot)) != 0L; }

    public int getOrDefault(int slot, int fallback) {
        long bit = bit(slot);
        return (present & bit) == 0L ? fallback : values[rank(present, bit)];
    }

    public int require(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) throw new IllegalArgumentException("context int slot is absent: " + slot);
        return values[rank(present, bit)];
    }

    public SlotInts with(int slot, int value) {
        long bit = bit(slot);
        int at = rank(present, bit);
        if ((present & bit) != 0L) {
            if (values[at] == value) return this;
            int[] next = values.clone();
            next[at] = value;
            return new SlotInts(present, next);
        }
        int[] next = new int[values.length + 1];
        System.arraycopy(values, 0, next, 0, at);
        next[at] = value;
        System.arraycopy(values, at, next, at + 1, values.length - at);
        return new SlotInts(present | bit, next);
    }

    public SlotInts without(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) return this;
        int at = rank(present, bit);
        if (values.length == 1) return EMPTY;
        int[] next = new int[values.length - 1];
        System.arraycopy(values, 0, next, 0, at);
        System.arraycopy(values, at + 1, next, at, values.length - at - 1);
        return new SlotInts(present & ~bit, next);
    }

    public int[] packedValuesCopy() { return values.clone(); }

    public long logicalPayloadBytes() {
        return Long.BYTES + (long) values.length * Integer.BYTES;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SlotInts slots
                && present == slots.present
                && Arrays.equals(values, slots.values);
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(present) + Arrays.hashCode(values);
    }

    private static int rank(long mask, long bit) {
        return Long.bitCount(mask & (bit - 1L));
    }

    private static long bit(int slot) {
        if (slot < 0 || slot >= Long.SIZE) throw new IndexOutOfBoundsException("slot must be in [0,63]");
        return 1L << slot;
    }
}
