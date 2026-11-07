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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;
import jdk.internal.m3.collections.M3PrimitiveBits;

/**
 * Fixed-size byte-packed integers, including 24/40/48/56-bit widths.
 * Byte order is explicit; no alignment padding or overread is required.
 * Instances are not safe for concurrent mutation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PackedByteArray.java}
 * blob {@code fbfbfa71cf602908337b9f9017e46aeeb1200e15} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PackedByteArray {
    private static final VarHandle LONG_LE = MethodHandles.byteArrayViewVarHandle(
            long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(
            long[].class, ByteOrder.BIG_ENDIAN);
    private final int bitsPerValue;
    private final int bytesPerValue;
    private final boolean signed;
    private final ByteOrder order;
    private final byte[] bytes;

    public M3PackedByteArray(int bitsPerValue, int size, boolean signed, ByteOrder order) {
        M3PrimitiveBits.mask(bitsPerValue);
        if ((bitsPerValue & 7) != 0) {
            throw new IllegalArgumentException("byte-packed width must be a multiple of 8");
        }
        this.order = Objects.requireNonNull(order, "order");
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.bitsPerValue = bitsPerValue;
        this.bytesPerValue = bitsPerValue >>> 3;
        this.signed = signed;
        long length = (long) size * bytesPerValue;
        if (length > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("byte array too large");
        }
        bytes = new byte[(int) length];
    }

    public int bitsPerValue() { return bitsPerValue; }
    public int size() { return bytes.length / bytesPerValue; }
    public boolean signed() { return signed; }
    public ByteOrder order() { return order; }
    public long payloadBytes() { return bytes.length; }

    public long get(int index) {
        int offset = Objects.checkIndex(index, size()) * bytesPerValue;
        if (bitsPerValue == Long.SIZE) {
            return order == ByteOrder.LITTLE_ENDIAN
                    ? (long) LONG_LE.get(bytes, offset)
                    : (long) LONG_BE.get(bytes, offset);
        }
        long raw = 0L;
        for (int i = 0; i < bytesPerValue; i++) {
            int source = order == ByteOrder.LITTLE_ENDIAN ? i : bytesPerValue - 1 - i;
            raw |= (bytes[offset + source] & 0xffL) << (i * Byte.SIZE);
        }
        return signed ? M3PrimitiveBits.signExtend(raw, bitsPerValue) : raw;
    }

    public void set(int index, long value) {
        int offset = Objects.checkIndex(index, size()) * bytesPerValue;
        M3PrimitiveBits.checkValue(value, bitsPerValue, signed);
        if (bitsPerValue == Long.SIZE) {
            if (order == ByteOrder.LITTLE_ENDIAN) LONG_LE.set(bytes, offset, value);
            else LONG_BE.set(bytes, offset, value);
            return;
        }
        for (int i = 0; i < bytesPerValue; i++) {
            int target = order == ByteOrder.LITTLE_ENDIAN ? i : bytesPerValue - 1 - i;
            bytes[offset + target] = (byte) (value >>> (i * Byte.SIZE));
        }
    }

    public byte[] toByteArray() { return bytes.clone(); }

    /** Imports a defensive snapshot; the byte length must contain whole entries. */
    public static M3PackedByteArray fromBytes(
            byte[] source, int bitsPerValue, boolean signed, ByteOrder order) {
        Objects.requireNonNull(source, "source");
        M3PrimitiveBits.mask(bitsPerValue);
        if ((bitsPerValue & 7) != 0) {
            throw new IllegalArgumentException("byte-packed width must be a multiple of 8");
        }
        int bytesPerValue = bitsPerValue >>> 3;
        if (source.length % bytesPerValue != 0) {
            throw new IllegalArgumentException("incomplete packed value");
        }
        M3PackedByteArray result = new M3PackedByteArray(
                bitsPerValue, source.length / bytesPerValue, signed, order);
        System.arraycopy(source, 0, result.bytes, 0, source.length);
        return result;
    }
}
