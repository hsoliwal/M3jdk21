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
 * Sparse immutable unsigned-byte values with a separate one-word presence mask.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotBytes.java}
 * blob {@code 0b18667f4a94c8b94603d56018fafc347f18ea69} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotBytes {
    public static final SlotBytes EMPTY = new SlotBytes(0L, new byte[0]);

    private final long present;
    private final byte[] values;

    private SlotBytes(long present, byte[] values) {
        this.present = present;
        this.values = values;
    }

    public long presentMask() {
        return present;
    }

    public int size() {
        return values.length;
    }

    public boolean contains(int slot) {
        return (present & bit(slot)) != 0L;
    }

    public int getUnsignedOrDefault(int slot, int fallback) {
        long bit = bit(slot);
        return (present & bit) == 0L ? fallback : Byte.toUnsignedInt(values[rank(present, bit)]);
    }

    public int requireUnsigned(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) {
            throw new IllegalArgumentException("context byte slot is absent: " + slot);
        }
        return Byte.toUnsignedInt(values[rank(present, bit)]);
    }

    public double requireUnit(int slot) {
        return requireUnsigned(slot) / 255.0d;
    }

    public SlotBytes withUnsigned(int slot, int unsigned) {
        if ((unsigned & ~0xff) != 0) {
            throw new IllegalArgumentException("unsigned byte must be in [0,255]");
        }
        long bit = bit(slot);
        int at = rank(present, bit);
        byte encoded = (byte) unsigned;
        if ((present & bit) != 0L) {
            if (values[at] == encoded) {
                return this;
            }
            byte[] next = values.clone();
            next[at] = encoded;
            return new SlotBytes(present, next);
        }
        byte[] next = new byte[values.length + 1];
        System.arraycopy(values, 0, next, 0, at);
        next[at] = encoded;
        System.arraycopy(values, at, next, at + 1, values.length - at);
        return new SlotBytes(present | bit, next);
    }

    public SlotBytes withUnit(int slot, double unit) {
        if (!Double.isFinite(unit) || unit < 0.0d || unit > 1.0d) {
            throw new IllegalArgumentException("unit value must be finite and in [0,1]");
        }
        return withUnsigned(slot, (int) Math.round(unit * 255.0d));
    }

    public SlotBytes without(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) {
            return this;
        }
        int at = rank(present, bit);
        if (values.length == 1) {
            return EMPTY;
        }
        byte[] next = new byte[values.length - 1];
        System.arraycopy(values, 0, next, 0, at);
        System.arraycopy(values, at + 1, next, at, values.length - at - 1);
        return new SlotBytes(present & ~bit, next);
    }

    public int sumUnsigned() {
        int sum = 0;
        for (byte value : values) sum += Byte.toUnsignedInt(value);
        return sum;
    }

    public long selectAtLeast(int threshold) {
        if ((threshold & ~0xff) != 0) throw new IllegalArgumentException("threshold must be in [0,255]");
        long selected = 0L;
        long remaining = present;
        int index = 0;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(remaining);
            if (Byte.toUnsignedInt(values[index++]) >= threshold) selected |= 1L << slot;
            remaining &= remaining - 1L;
        }
        return selected;
    }

    public long dot(SlotBytes other) {
        java.util.Objects.requireNonNull(other, "other");
        long common = present & other.present;
        long sum = 0L;
        while (common != 0L) {
            int slot = Long.numberOfTrailingZeros(common);
            sum += (long) requireUnsigned(slot) * other.requireUnsigned(slot);
            common &= common - 1L;
        }
        return sum;
    }

    public int l1(SlotBytes other) {
        java.util.Objects.requireNonNull(other, "other");
        long union = present | other.present;
        int sum = 0;
        while (union != 0L) {
            int slot = Long.numberOfTrailingZeros(union);
            int left = getUnsignedOrDefault(slot, 0);
            int right = other.getUnsignedOrDefault(slot, 0);
            sum = Math.addExact(sum, Math.abs(left - right));
            union &= union - 1L;
        }
        return sum;
    }

    public void forEach(SlotConsumer consumer) {
        java.util.Objects.requireNonNull(consumer, "consumer");
        long remaining = present;
        int index = 0;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(remaining);
            consumer.accept(slot, Byte.toUnsignedInt(values[index++]));
            remaining &= remaining - 1L;
        }
    }

    @FunctionalInterface
    public interface SlotConsumer {
        void accept(int slot, int unsignedValue);
    }

    public byte[] packedValuesCopy() {
        return values.clone();
    }

    /** Logical semantic payload only; excludes object/array headers and alignment. */
    public int logicalPayloadBytes() {
        return Long.BYTES + values.length;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SlotBytes slots
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
        if (slot < 0 || slot >= Long.SIZE) {
            throw new IndexOutOfBoundsException("slot must be in [0,63]");
        }
        return 1L << slot;
    }
}
