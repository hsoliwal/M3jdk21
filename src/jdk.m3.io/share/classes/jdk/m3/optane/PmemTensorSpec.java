// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable description of a contiguous row-major tensor, without retaining its payload.
 * Dimensions and total byte count use long coordinates. Rank zero denotes one scalar;
 * any zero-sized axis makes the tensor empty. Strided and sub-byte layouts require an
 * explicit application codec and are not represented by this descriptor.
 *
 * <p>Model, attention, engine and quantization-scale identities still belong in the
 * caller's {@link PmemPrefixKey.Context}. This descriptor validates byte geometry,
 * not whether those declarations describe the actual model.</p>
 */
public final class PmemTensorSpec {
    public static final int MAX_RANK = 16;
    private static final long MAGIC = 0x4d3354454e535031L;
    private static final int VERSION = 1;
    private final Scalar scalar;
    private final ByteOrder byteOrder;
    private final long[] shape;
    private final long elements;
    private final long bytes;
    private final byte[] encoding;

    /** Stable format IDs are explicit and never derived from enum ordering. */
    public enum Scalar {
        UINT8(1, 1), INT8(2, 1), INT16(3, 2), INT32(4, 4), INT64(5, 8),
        FLOAT16(6, 2), BFLOAT16(7, 2), FLOAT32(8, 4), FLOAT64(9, 8);

        private final int id;
        private final int bytes;

        Scalar(int id, int bytes) {
            this.id = id;
            this.bytes = bytes;
        }

        public int byteWidth() {
            return bytes;
        }
    }

    /** Snapshot the shape, checking every dimension and the complete byte count. */
    public PmemTensorSpec(Scalar scalar, ByteOrder byteOrder, long... shape) {
        this.scalar = Objects.requireNonNull(scalar, "scalar");
        this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder");
        Objects.requireNonNull(shape, "shape");
        if (shape.length > MAX_RANK) {
            throw new IllegalArgumentException("tensor rank exceeds " + MAX_RANK);
        }
        this.shape = shape.clone();
        boolean empty = false;
        for (long dimension : this.shape) {
            if (dimension < 0) {
                throw new IllegalArgumentException("negative tensor dimension");
            }
            empty |= dimension == 0;
        }
        long count = empty ? 0 : 1;
        try {
            if (!empty) {
                for (long dimension : this.shape) {
                    count = Math.multiplyExact(count, dimension);
                }
            }
            elements = count;
            bytes = Math.multiplyExact(count, scalar.bytes);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("tensor byte geometry overflows", overflow);
        }
        ByteBuffer fields = ByteBuffer.allocate(24 + Long.BYTES * this.shape.length);
        fields.putLong(MAGIC).putInt(VERSION).putInt(scalar.id);
        fields.putInt(byteOrder == ByteOrder.LITTLE_ENDIAN ? 1 : 2).putInt(this.shape.length);
        for (long dimension : this.shape) {
            fields.putLong(dimension);
        }
        encoding = fields.array();
    }

    public Scalar scalar() {
        return scalar;
    }

    public ByteOrder byteOrder() {
        return byteOrder;
    }

    public long[] shape() {
        return shape.clone();
    }

    public int rank() {
        return shape.length;
    }

    public long elements() {
        return elements;
    }

    public long byteCount() {
        return bytes;
    }

    /** Domain/version-bearing deterministic descriptor identity for application manifests. */
    public String sha256() {
        return PmemBlockStore.sha256(encoding);
    }

    byte[] encoding() {
        return encoding.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PmemTensorSpec spec && Arrays.equals(encoding, spec.encoding);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(encoding);
    }

    @Override
    public String toString() {
        return scalar + " " + byteOrder + " " + Arrays.toString(shape);
    }
}
