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
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.M3PrimitiveBigArray;
import jdk.m3.collections.M3PrimitiveKind;

/**
 * Long-indexed, six-byte integer lanes over the existing BYTE storage owners.
 *
 * <p>The physical encoding is little endian and has no reserved sentinel. Unsigned
 * access covers 0..2^48-1; signed access interprets the same bits as two's complement.
 * Views alias their supplied owner and retain no separate payload. They are not
 * thread-safe; callers must not resize a dense owner below the admitted view range.
 * Sparse views inherit the owner's absent-page-as-zero and lazy allocation contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/Packed48Array.java}
 * blob {@code 6dc7a909f026a2a9bcff55f8db07962509443a23} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3Packed48Array {
    public static final int BYTES_PER_VALUE = 6;
    public static final long MAX_UNSIGNED = (1L << 48) - 1;
    public static final long MIN_SIGNED = -(1L << 47);
    public static final long MAX_SIGNED = (1L << 47) - 1;

    private final M3PrimitiveBigArray dense;
    private final M3LazyPrimitiveAddressSpace sparse;
    private final long byteOffset;
    private final long length;

    /** Allocates exactly six payload bytes per logical value, excluding array metadata. */
    public M3Packed48Array(long length) {
        this(length, M3PrimitiveBigArray.DEFAULT_SEGMENT_SHIFT);
    }

    public M3Packed48Array(long length, int segmentShift) {
        this(new M3PrimitiveBigArray(M3PrimitiveKind.BYTE, bytesFor(length), segmentShift),
                null, 0L, length);
    }

    private M3Packed48Array(M3PrimitiveBigArray dense, M3LazyPrimitiveAddressSpace sparse,
                          long byteOffset, long length) {
        this.dense = dense;
        this.sparse = sparse;
        this.byteOffset = byteOffset;
        this.length = length;
    }

    public static M3Packed48Array view(M3PrimitiveBigArray bytes, long byteOffset, long length) {
        Objects.requireNonNull(bytes, "bytes");
        requireByteKind(bytes.kind());
        Objects.checkFromIndexSize(byteOffset, bytesFor(length), bytes.length());
        return new M3Packed48Array(bytes, null, byteOffset, length);
    }

    public static M3Packed48Array view(M3LazyPrimitiveAddressSpace bytes,
                                     long byteOffset, long length) {
        Objects.requireNonNull(bytes, "bytes");
        requireByteKind(bytes.kind());
        Objects.checkFromIndexSize(byteOffset, bytesFor(length), bytes.logicalCapacity());
        return new M3Packed48Array(null, bytes, byteOffset, length);
    }

    public long length() { return length; }

    /** Logical payload extent; a sparse owner may have fewer resident bytes. */
    public long payloadBytes() { return bytesFor(length); }

    public long getUnsigned(long index) {
        long firstByte = address(index);
        long value = 0L;
        for (int byteIndex = 0; byteIndex < BYTES_PER_VALUE; byteIndex++) {
            value |= (readByte(firstByte + byteIndex) & 0xffL) << (byteIndex << 3);
        }
        return value;
    }

    public long getSigned(long index) {
        return (getUnsigned(index) << 16) >> 16;
    }

    public void setUnsigned(long index, long value) {
        long firstByte = address(index);
        if (value < 0 || value > MAX_UNSIGNED) {
            throw new IllegalArgumentException("value exceeds unsigned 48-bit range");
        }
        writeValue(firstByte, value);
    }

    public void setSigned(long index, long value) {
        long firstByte = address(index);
        if (value < MIN_SIGNED || value > MAX_SIGNED) {
            throw new IllegalArgumentException("value exceeds signed 48-bit range");
        }
        writeValue(firstByte, value & MAX_UNSIGNED);
    }

    private void writeValue(long firstByte, long value) {
        for (int byteIndex = 0; byteIndex < BYTES_PER_VALUE; byteIndex++) {
            writeByte(firstByte + byteIndex, (value >>> (byteIndex << 3)) & 0xffL);
        }
    }

    private long address(long index) {
        Objects.checkIndex(index, length);
        return byteOffset + index * BYTES_PER_VALUE;
    }

    private long readByte(long index) {
        return dense == null ? sparse.getBits(index) : dense.getBits(index);
    }

    private void writeByte(long index, long value) {
        if (dense == null) sparse.setBits(index, value);
        else dense.setBits(index, value);
    }

    private static long bytesFor(long length) {
        if (length < 0 || length > Long.MAX_VALUE / BYTES_PER_VALUE) {
            throw new IllegalArgumentException("48-bit lane length out of range");
        }
        return length * BYTES_PER_VALUE;
    }

    private static void requireByteKind(M3PrimitiveKind kind) {
        if (kind != M3PrimitiveKind.BYTE) throw new IllegalArgumentException("BYTE owner required");
    }
}
