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

/**
 * Immutable schema-ordinal primitive context table.
 *
 * <p>Names live once in SlotSchema. Each value table retains one primitive
 * lane plus one presence word. Empty tables share zero-length primitive arrays and
 * allocate a schema-sized lane only on first write.
 *
 * <p>The primitive kind is derived from the lane array type, so no enum/reference
 * discriminator is retained per table.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPrimitiveSlots.java}
 * blob {@code 40c4436c921d468eef6b38500fd3b65fe0856faf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveSlots {
    public enum Kind {
        U8,
        U16,
        I32,
        I64
    }

    @FunctionalInterface
    public interface SlotLongConsumer {
        void accept(int slot, long value);
    }

    private static final byte[] EMPTY_U8 = new byte[0];
    private static final short[] EMPTY_U16 = new short[0];
    private static final int[] EMPTY_I32 = new int[0];
    private static final long[] EMPTY_I64 = new long[0];

    private final SlotSchema schema;
    private final Object lane;
    private final long present;

    private PrimitiveSlots(
            SlotSchema schema,
            Object lane,
            long present) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.lane = Objects.requireNonNull(lane, "lane");
        schema.requireCompatible(present);
        if (present != 0L && laneLength(lane) != schema.size()) {
            throw new IllegalArgumentException(
                    "materialized lane length differs from schema");
        }
        if (present == 0L && laneLength(lane) != 0
                && laneLength(lane) != schema.size()) {
            throw new IllegalArgumentException(
                    "empty lane length differs from schema");
        }
        this.present = present;
    }

    public static PrimitiveSlots u8(
            SlotSchema schema) {
        return new PrimitiveSlots(
                schema, EMPTY_U8, 0L);
    }

    public static PrimitiveSlots u16(
            SlotSchema schema) {
        return new PrimitiveSlots(
                schema, EMPTY_U16, 0L);
    }

    public static PrimitiveSlots i32(
            SlotSchema schema) {
        return new PrimitiveSlots(
                schema, EMPTY_I32, 0L);
    }

    public static PrimitiveSlots i64(
            SlotSchema schema) {
        return new PrimitiveSlots(
                schema, EMPTY_I64, 0L);
    }

    public SlotSchema schema() {
        return schema;
    }

    public Kind kind() {
        if (lane instanceof byte[]) {
            return Kind.U8;
        }
        if (lane instanceof short[]) {
            return Kind.U16;
        }
        if (lane instanceof int[]) {
            return Kind.I32;
        }
        return Kind.I64;
    }

    public int widthBytes() {
        return switch (kind()) {
            case U8 -> Byte.BYTES;
            case U16 -> Short.BYTES;
            case I32 -> Integer.BYTES;
            case I64 -> Long.BYTES;
        };
    }

    public int size() {
        return Long.bitCount(present);
    }

    public boolean isEmpty() {
        return present == 0L;
    }

    public long presentMask() {
        return present;
    }

    public boolean contains(int slot) {
        Objects.checkIndex(slot, schema.size());
        return (present & (1L << slot)) != 0L;
    }

    public long value(int slot) {
        Objects.checkIndex(slot, schema.size());
        if (!contains(slot)) {
            throw new IllegalStateException(
                    "context primitive slot is absent: " + slot);
        }
        return read(slot);
    }

    public long getOrDefault(
            int slot,
            long fallback) {
        Objects.checkIndex(slot, schema.size());
        return contains(slot)
                ? read(slot)
                : fallback;
    }

    public long getOrDefault(
            String exactName,
            long fallback) {
        int slot = schema.slot(exactName);
        return slot < 0
                ? fallback
                : getOrDefault(slot, fallback);
    }

    public PrimitiveSlots with(
            int slot,
            long value) {
        Objects.checkIndex(slot, schema.size());
        requireValue(value);
        long bit = 1L << slot;
        if ((present & bit) != 0L
                && read(slot) == value) {
            return this;
        }
        Object copy = copyOrMaterialize();
        write(copy, slot, value);
        return new PrimitiveSlots(
                schema,
                copy,
                present | bit);
    }

    public PrimitiveSlots with(
            String exactName,
            long value) {
        int slot = schema.slot(exactName);
        if (slot < 0) {
            throw new IllegalArgumentException(
                    "Unknown exact context slot: " + exactName);
        }
        return with(slot, value);
    }

    /**
     * Validated multi-slot update. Slots must be strictly increasing so duplicate
     * writes cannot silently override each other. At most one primitive lane copy
     * is made.
     */
    public PrimitiveSlots withBatch(
            int[] slots,
            long[] values) {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(values, "values");
        if (slots.length != values.length) {
            throw new IllegalArgumentException(
                    "slot/value length mismatch");
        }
        int previous = -1;
        boolean changed = false;
        long nextPresent = present;
        for (int i = 0; i < slots.length; i++) {
            int slot = Objects.checkIndex(
                    slots[i], schema.size());
            if (slot <= previous) {
                throw new IllegalArgumentException(
                        "slots must be strictly increasing");
            }
            previous = slot;
            requireValue(values[i]);
            long bit = 1L << slot;
            changed |= (present & bit) == 0L
                    || read(slot) != values[i];
            nextPresent |= bit;
        }
        if (!changed) {
            return this;
        }
        Object copy = copyOrMaterialize();
        for (int i = 0; i < slots.length; i++) {
            write(copy, slots[i], values[i]);
        }
        return new PrimitiveSlots(
                schema, copy, nextPresent);
    }

    public PrimitiveSlots without(int slot) {
        Objects.checkIndex(slot, schema.size());
        long bit = 1L << slot;
        if ((present & bit) == 0L) {
            return this;
        }
        long nextPresent = present & ~bit;
        return new PrimitiveSlots(
                schema,
                nextPresent == 0L
                        ? emptyLane()
                        : lane,
                nextPresent);
    }

    public void forEach(
            SlotLongConsumer consumer) {
        SlotLongConsumer checked =
                Objects.requireNonNull(consumer, "consumer");
        long remaining = present;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(
                    remaining);
            checked.accept(slot, read(slot));
            remaining &= remaining - 1L;
        }
    }

    private long read(int slot) {
        if (laneLength(lane) == 0) {
            return 0L;
        }
        if (lane instanceof byte[] values) {
            return Byte.toUnsignedLong(values[slot]);
        }
        if (lane instanceof short[] values) {
            return Short.toUnsignedLong(values[slot]);
        }
        if (lane instanceof int[] values) {
            return values[slot];
        }
        return ((long[]) lane)[slot];
    }

    private Object copyOrMaterialize() {
        int slots = schema.size();
        if (lane instanceof byte[] values) {
            return values.length == 0
                    ? new byte[slots]
                    : values.clone();
        }
        if (lane instanceof short[] values) {
            return values.length == 0
                    ? new short[slots]
                    : values.clone();
        }
        if (lane instanceof int[] values) {
            return values.length == 0
                    ? new int[slots]
                    : values.clone();
        }
        long[] values = (long[]) lane;
        return values.length == 0
                ? new long[slots]
                : values.clone();
    }

    private Object emptyLane() {
        if (lane instanceof byte[]) {
            return EMPTY_U8;
        }
        if (lane instanceof short[]) {
            return EMPTY_U16;
        }
        if (lane instanceof int[]) {
            return EMPTY_I32;
        }
        return EMPTY_I64;
    }

    private void requireValue(long value) {
        if (lane instanceof byte[]) {
            if (value < 0L || value > 255L) {
                throw new IllegalArgumentException(
                        "U8 value outside [0,255]");
            }
            return;
        }
        if (lane instanceof short[]) {
            if (value < 0L || value > 65_535L) {
                throw new IllegalArgumentException(
                        "U16 value outside [0,65535]");
            }
            return;
        }
        if (lane instanceof int[]
                && (value < Integer.MIN_VALUE
                || value > Integer.MAX_VALUE)) {
            throw new IllegalArgumentException(
                    "I32 value outside int range");
        }
    }

    private static void write(
            Object target,
            int slot,
            long value) {
        if (target instanceof byte[] values) {
            values[slot] = (byte) value;
        } else if (target instanceof short[] values) {
            values[slot] = (short) value;
        } else if (target instanceof int[] values) {
            values[slot] = (int) value;
        } else {
            ((long[]) target)[slot] = value;
        }
    }

    private static int laneLength(Object values) {
        if (values instanceof byte[] lane) {
            return lane.length;
        }
        if (values instanceof short[] lane) {
            return lane.length;
        }
        if (values instanceof int[] lane) {
            return lane.length;
        }
        if (values instanceof long[] lane) {
            return lane.length;
        }
        throw new IllegalArgumentException(
                "unsupported primitive lane: "
                        + values.getClass().getName());
    }
}
