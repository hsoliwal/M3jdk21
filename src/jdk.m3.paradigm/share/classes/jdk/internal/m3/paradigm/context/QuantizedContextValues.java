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
import java.util.Objects;

/**
 * Immutable payload for CompactContext: 64 flag bits and one byte per strength.
 * Slot meanings belong to a shared caller-owned schema, not a per-value map.
 * All 256 byte patterns are values; missing/unknown is not encoded as zero.
 *
 * <p>No stored floating-point values or boxed scalar collections. Decoding is
 * computed at the boundary. Publish replacements through the existing context
 * cell CAS to obtain one coherent flags/strengths revision. Array updates are
 * copy-on-write, not allocation-free. Existing exact scalar contracts are not
 * automatically quantized by this opt-in representation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/QuantizedContextValues.java}
 * blob {@code 69f530b56f48ae32ba8d8ecdee87f2ca11c1772b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedContextValues {
    public static final int MAX_STRENGTH = 255;
    private final long flags;
    private final byte[] strengths;

    private QuantizedContextValues(long flags, byte[] owned) {
        this.flags = flags;
        this.strengths = owned;
    }

    /** All strengths initially zero; the caller chooses the bounded slot count. */
    public static QuantizedContextValues zeros(int count) {
        if (count < 0) throw new IllegalArgumentException("negative slot count");
        return new QuantizedContextValues(0L, new byte[count]);
    }

    /** Copies the input; every byte bit pattern is a valid unsigned strength. */
    public static QuantizedContextValues copyOf(long flags, byte[] strengths) {
        return new QuantizedContextValues(flags, Objects.requireNonNull(strengths, "strengths").clone());
    }

    public long flags() { return flags; }
    public int size() { return strengths.length; }

    /** Logical packed data bytes only, excluding JVM headers/references/snapshots. */
    public long payloadBytes() { return Long.BYTES + (long) strengths.length; }

    /** Validates rather than allowing Java's shift-distance wrapping. */
    public static long flagMask(int bit) {
        Objects.checkIndex(bit, Long.SIZE);
        return 1L << bit;
    }

    public boolean flag(int bit) { return (flags & flagMask(bit)) != 0L; }

    /** Empty required mask matches; all 64 bits are meaningful, including bit 63. */
    public boolean hasAll(long required) { return (flags & required) == required; }

    public int strengthU8(int slot) { return strengths[slot] & 0xff; }

    /** A computed binary floating-point view, not stored decimal state. */
    public double strength(int slot) { return strengthU8(slot) / 255.0d; }

    /** No caller receives the internal mutable array. */
    public byte[] strengthsCopy() { return strengths.clone(); }

    /** Flags-only replacements safely share the private immutable byte array. */
    public QuantizedContextValues withFlags(long replacement) {
        return replacement == flags ? this : new QuantizedContextValues(replacement, strengths);
    }

    public QuantizedContextValues withFlag(int bit, boolean enabled) {
        long mask = flagMask(bit);
        return withFlags(enabled ? flags | mask : flags & ~mask);
    }

    /** Exact U8 update; rejects overflow instead of silently wrapping or clipping. */
    public QuantizedContextValues withStrength(int slot, int unsigned) {
        requireU8(unsigned);
        Objects.checkIndex(slot, strengths.length);
        if (strengthU8(slot) == unsigned) return this;
        byte[] replacement = strengths.clone();
        replacement[slot] = (byte) unsigned;
        return new QuantizedContextValues(flags, replacement);
    }

    /**
     * Validated batch replacement: one resident-array clone for all changed slots.
     * Slots must be strictly increasing. Borrowed input arrays are not retained;
     * the caller must not mutate them concurrently with this call.
     * A flags-only change shares immutable storage; a complete no-op returns this.
     */
    public QuantizedContextValues withBatch(long replacementFlags, int[] slots, byte[] values) {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(values, "values");
        if (slots.length != values.length) throw new IllegalArgumentException("batch length mismatch");
        int previous = -1;
        boolean changed = false;
        for (int i = 0; i < slots.length; i++) {
            int slot = Objects.checkIndex(slots[i], strengths.length);
            if (slot <= previous) throw new IllegalArgumentException("slots must be strictly increasing");
            previous = slot;
            changed |= strengths[slot] != values[i];
        }
        if (!changed) return withFlags(replacementFlags);
        byte[] replacement = strengths.clone();
        for (int i = 0; i < slots.length; i++) replacement[slots[i]] = values[i];
        return new QuantizedContextValues(replacementFlags, replacement);
    }

    /** Explicit lossy conversion. Finite [0,1] inputs only; nearest, positive ties up. */
    public QuantizedContextValues withNormalizedStrength(int slot, double normalized) {
        return withStrength(slot, encode(normalized) & 0xff);
    }

    public static byte encode(double normalized) {
        if (!Double.isFinite(normalized) || normalized < 0.0d || normalized > 1.0d) {
            throw new IllegalArgumentException("strength must be finite and in [0,1]");
        }
        return (byte) Math.round(normalized * MAX_STRENGTH);
    }

    public static double decode(byte stored) { return (stored & 0xff) / 255.0d; }

    /** Nearest U8 product of two normalized strengths, without floating point. */
    public static int multiplyU8(int left, int right) {
        requireU8(left);
        requireU8(right);
        return (left * right + 127) / MAX_STRENGTH;
    }

    /** Explicit saturating addition; not an authority or context merge policy. */
    public static int addSaturatingU8(int left, int right) {
        requireU8(left);
        requireU8(right);
        return Math.min(MAX_STRENGTH, left + right);
    }

    private static void requireU8(int value) {
        if (value < 0 || value > MAX_STRENGTH) throw new IllegalArgumentException("U8 must be in [0,255]");
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof QuantizedContextValues value
                && flags == value.flags && Arrays.equals(strengths, value.strengths);
    }

    @Override
    public int hashCode() { return 31 * Long.hashCode(flags) + Arrays.hashCode(strengths); }
}
