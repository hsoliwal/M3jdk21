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
 * Sparse immutable primitive-short values keyed by shared context slots.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotShorts.java}
 * blob {@code fc7957a4ce8da778828733ccb0aaa7f2e8b69289} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotShorts {
    public static final SlotShorts EMPTY = new SlotShorts(0L, new short[0]);

    private final long present;
    private final short[] values;

    private SlotShorts(long present, short[] values) {
        this.present = present;
        this.values = values;
    }

    public long presentMask() { return present; }
    public int size() { return values.length; }
    public boolean isEmpty() { return present == 0L; }
    public boolean contains(int slot) { return (present & bit(slot)) != 0L; }

    public short getOrDefault(int slot, short fallback) {
        long bit = bit(slot);
        return (present & bit) == 0L ? fallback : values[rank(present, bit)];
    }

    public int getUnsignedOrDefault(int slot, int fallback) {
        long bit = bit(slot);
        return (present & bit) == 0L ? fallback : Short.toUnsignedInt(values[rank(present, bit)]);
    }

    public short require(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) throw new IllegalArgumentException("context short slot is absent: " + slot);
        return values[rank(present, bit)];
    }

    public SlotShorts with(int slot, short value) {
        long bit = bit(slot);
        int at = rank(present, bit);
        if ((present & bit) != 0L) {
            if (values[at] == value) return this;
            short[] next = values.clone();
            next[at] = value;
            return new SlotShorts(present, next);
        }
        short[] next = new short[values.length + 1];
        System.arraycopy(values, 0, next, 0, at);
        next[at] = value;
        System.arraycopy(values, at, next, at + 1, values.length - at);
        return new SlotShorts(present | bit, next);
    }

    public SlotShorts withUnsigned(int slot, int value) {
        if ((value & ~0xffff) != 0) throw new IllegalArgumentException("unsigned short must be in [0,65535]");
        return with(slot, (short) value);
    }

    public SlotShorts without(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) return this;
        int at = rank(present, bit);
        if (values.length == 1) return EMPTY;
        short[] next = new short[values.length - 1];
        System.arraycopy(values, 0, next, 0, at);
        System.arraycopy(values, at + 1, next, at, values.length - at - 1);
        return new SlotShorts(present & ~bit, next);
    }

    public short[] packedValuesCopy() { return values.clone(); }

    public long logicalPayloadBytes() {
        return Long.BYTES + (long) values.length * Short.BYTES;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SlotShorts slots
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
