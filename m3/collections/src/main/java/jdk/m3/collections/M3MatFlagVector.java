/*******************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable 64-bit flag lane: one machine word per row.
 *
 * <p>Hot predicates are bit arithmetic only. The builder is transient; frozen state is one
 * contiguous long array.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatFlagVector.java}
 * blob {@code 86e6f724419f8b4b8dee3d43955529a70006f74b} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatFlagVector {
    private final long[] flags;

    public M3MatFlagVector(long[] flags) {
        this.flags = Objects.requireNonNull(flags, "flags").clone();
    }

    public int size() { return flags.length; }
    public long get(int row) { return flags[Objects.checkIndex(row, flags.length)]; }
    public long payloadBytes() { return 8L * flags.length; }
    public long[] toArray() { return flags.clone(); }

    public boolean hasAll(int row, long mask) {
        long value = get(row);
        return (value & mask) == mask;
    }

    public boolean hasAny(int row, long mask) {
        return (get(row) & mask) != 0L;
    }

    public boolean hasNone(int row, long mask) {
        return (get(row) & mask) == 0L;
    }

    public M3MatBits all(long mask) {
        return M3MatPrimitiveKernel.selectFlags(this, mask, 0L, 0L);
    }

    public M3MatBits any(long mask) {
        return M3MatPrimitiveKernel.selectFlags(this, 0L, mask, 0L);
    }

    public M3MatBits none(long mask) {
        return M3MatPrimitiveKernel.selectFlags(this, 0L, 0L, mask);
    }

    public M3MatBits maskedEquals(long mask, long expected) {
        if ((expected & ~mask) != 0L) {
            throw new IllegalArgumentException("expected contains bits outside mask");
        }
        M3MatBits.Builder result = M3MatBits.builder(flags.length);
        for (int row = 0; row < flags.length; row++) {
            if ((flags[row] & mask) == expected) result.set(row);
        }
        return result.build();
    }

    long getUnchecked(int row) { return flags[row]; }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof M3MatFlagVector vector && Arrays.equals(flags, vector.flags);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(flags); }

    public static final class Builder {
        private final long[] flags;

        public Builder(int size) {
            if (size < 0) throw new IllegalArgumentException("negative flag-vector size");
            this.flags = new long[size];
        }

        public Builder set(int row, long value) {
            flags[Objects.checkIndex(row, flags.length)] = value;
            return this;
        }

        public Builder add(int row, long mask) {
            flags[Objects.checkIndex(row, flags.length)] |= mask;
            return this;
        }

        public Builder remove(int row, long mask) {
            flags[Objects.checkIndex(row, flags.length)] &= ~mask;
            return this;
        }

        public M3MatFlagVector build() { return new M3MatFlagVector(flags); }
    }
}
