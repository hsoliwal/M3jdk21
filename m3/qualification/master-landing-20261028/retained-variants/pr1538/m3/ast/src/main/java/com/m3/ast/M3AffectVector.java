// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.OptionalInt;

/** Sparse immutable signed-Q15 affect/appraisal vector. */
public final class M3AffectVector {
    public static final short MISSING = Short.MIN_VALUE;
    public static final int MIN_Q15 = -32767;
    public static final int MAX_Q15 = 32767;

    private final byte[] axes;
    private final short[] valuesQ15;

    private M3AffectVector(byte[] axes, short[] valuesQ15) {
        this.axes = axes;
        this.valuesQ15 = valuesQ15;
    }

    public static Builder builder() { return new Builder(); }

    public int size() { return axes.length; }

    public M3AffectAxis axisAt(int index) {
        return M3AffectAxis.values()[Byte.toUnsignedInt(axes[Objects.checkIndex(index, axes.length)])];
    }

    public int valueQ15At(int index) {
        return valuesQ15[Objects.checkIndex(index, valuesQ15.length)];
    }

    public OptionalInt valueQ15(M3AffectAxis axis) {
        int wanted = Objects.requireNonNull(axis, "axis").ordinal();
        int low = 0, high = axes.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            int value = Byte.toUnsignedInt(axes[middle]);
            if (value < wanted) low = middle + 1;
            else high = middle;
        }
        return low < axes.length && Byte.toUnsignedInt(axes[low]) == wanted
                ? OptionalInt.of(valuesQ15[low]) : OptionalInt.empty();
    }

    /** Packs the four hottest affect lanes into atom metadata; missing uses 0x8000 sentinel. */
    public long packedCore() {
        return pack(valueOrMissing(M3AffectAxis.VALENCE), 0)
                | pack(valueOrMissing(M3AffectAxis.AROUSAL), 16)
                | pack(valueOrMissing(M3AffectAxis.DOMINANCE), 32)
                | pack(valueOrMissing(M3AffectAxis.POLARITY), 48);
    }

    public static int unpackCore(long packed, M3AffectAxis axis) {
        int shift = switch (axis) {
            case VALENCE -> 0;
            case AROUSAL -> 16;
            case DOMINANCE -> 32;
            case POLARITY -> 48;
            default -> throw new IllegalArgumentException("axis is not packed core: " + axis);
        };
        return (short) (packed >>> shift);
    }

    public M3AffectVector overlay(M3AffectVector specific) {
        Builder out = builder();
        for (int i = 0; i < size(); i++) out.put(axisAt(i), valueQ15At(i));
        for (int i = 0; i < specific.size(); i++) out.put(specific.axisAt(i), specific.valueQ15At(i));
        return out.build();
    }

    private int valueOrMissing(M3AffectAxis axis) {
        return valueQ15(axis).orElse(MISSING);
    }

    private static long pack(int value, int shift) {
        return ((long) value & 0xffffL) << shift;
    }

    public static final class Builder {
        private final EnumMap<M3AffectAxis, Short> values = new EnumMap<>(M3AffectAxis.class);

        public Builder put(M3AffectAxis axis, int valueQ15) {
            Objects.requireNonNull(axis, "axis");
            if (valueQ15 < MIN_Q15 || valueQ15 > MAX_Q15) {
                throw new IllegalArgumentException("affect value outside signed Q15");
            }
            values.put(axis, (short) valueQ15);
            return this;
        }

        public M3AffectVector build() {
            byte[] axes = new byte[values.size()];
            short[] coordinates = new short[values.size()];
            int cursor = 0;
            for (var entry : values.entrySet()) {
                axes[cursor] = (byte) entry.getKey().ordinal();
                coordinates[cursor] = entry.getValue();
                cursor++;
            }
            return new M3AffectVector(
                    Arrays.copyOf(axes, cursor), Arrays.copyOf(coordinates, cursor));
        }
    }
}
