// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Objects;
import jdk.m3.arrays.WorkMonitor;

/**
 * Sequential, canonical base-128 encoding of full-width 64-bit integer arrays.
 *
 * <p>Unsigned operations interpret each {@code long} as an unsigned bit carrier;
 * negative carriers therefore occupy ten bytes. ZigZag operations map signed
 * values {@code 0, -1, 1, -2} to unsigned values {@code 0, 1, 2, 3} first.
 * Each byte carries seven payload bits, least-significant group first, with
 * its high bit indicating continuation. A ten-byte value ends in {@code 0x01}.
 *
 * <p>Every operation returns a fresh array and leaves its input unchanged.
 * Callers must not mutate an input until the operation returns. No native
 * library or batch ceiling is required; Java array and available-memory limits
 * apply. This codec supplies sequential serialization, not random access.
 * Encoding computes the complete output size before allocation. Decoding
 * validates the complete format before allocating or populating the result.
 */
public final class VInt {
    private VInt() { }

    /**
     * Encodes every unsigned 64-bit carrier in array order.
     *
     * @param values unsigned values represented by their {@code long} bits
     * @return a fresh array of minimal base-128 encodings
     * @throws NullPointerException if {@code values} is null
     * @throws IllegalArgumentException if the encoded length exceeds
     *         {@link Integer#MAX_VALUE}
     */
    public static byte[] encodeUnsigned64(long[] values) {
        return encode(values, false);
    }

    /**
     * Decodes an entire sequence of canonical unsigned 64-bit values.
     *
     * @param bytes the complete encoded sequence
     * @return a fresh array of unsigned {@code long} bit carriers
     * @throws NullPointerException if {@code bytes} is null
     * @throws IllegalArgumentException for truncation, a nonminimal encoding,
     *         overflow beyond 64 bits or continuation beyond ten bytes
     */
    public static long[] decodeUnsigned64(byte[] bytes) {
        return decode(bytes, false);
    }

    /**
     * ZigZag-maps every signed value and emits its minimal base-128 encoding.
     *
     * @param values signed 64-bit values in array order
     * @return a fresh array of canonical ZigZag encodings
     * @throws NullPointerException if {@code values} is null
     * @throws IllegalArgumentException if the encoded length exceeds
     *         {@link Integer#MAX_VALUE}
     */
    public static byte[] encodeZigZag64(long[] values) {
        return encode(values, true);
    }

    /**
     * Decodes a complete canonical base-128 sequence and reverses ZigZag.
     *
     * @param bytes the complete encoded sequence
     * @return a fresh array of signed 64-bit values
     * @throws NullPointerException if {@code bytes} is null
     * @throws IllegalArgumentException for truncation, a nonminimal encoding,
     *         overflow beyond 64 bits or continuation beyond ten bytes
     */
    public static long[] decodeZigZag64(byte[] bytes) {
        return decode(bytes, true);
    }

    private static byte[] encode(long[] values, boolean zigZag) {
        Objects.requireNonNull(values, "values");
        long length = 0L;
        for (long value : values) {
            long bits = zigZag ? (value << 1) ^ (value >> 63) : value;
            // Preserve the one-byte fast path; otherwise count seven-bit groups from bit length.
            length += (bits & ~0x7fL) == 0L ? 1
                    : (Long.SIZE - Long.numberOfLeadingZeros(bits) + 6) / 7;
        }
        // At most ten bytes per input element: this long count cannot overflow.
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Encoded byte count exceeds Java array limit");
        }
        byte[] result = new byte[(int) length];
        int output = 0;
        for (long value : values) {
            long bits = zigZag ? (value << 1) ^ (value >> 63) : value;
            while ((bits & ~0x7fL) != 0L) {
                result[output++] = (byte) ((bits & 0x7fL) | 0x80L);
                bits >>>= 7;
            }
            result[output++] = (byte) bits;
        }
        return result;
    }

    private static long[] decode(byte[] bytes, boolean zigZag) {
        Objects.requireNonNull(bytes, "bytes");
        long[] result = new long[validatedValueCount(bytes)];
        long value = 0L;
        int shift = 0;
        int output = 0;
        for (byte octet : bytes) {
            int unsigned = octet & 0xff;
            // The validation pass limits shift to 63 and the tenth payload to 1.
            value |= (long) (unsigned & 0x7f) << shift;
            if ((unsigned & 0x80) == 0) {
                result[output++] = zigZag ? (value >>> 1) ^ -(value & 1L) : value;
                value = 0L;
                shift = 0;
            } else {
                shift += 7;
            }
        }
        return result;
    }

    /** Validates all bytes without producing any decoded output. */
    private static int validatedValueCount(byte[] bytes) {
        int values = 0;
        int width = 0;
        for (int index = 0; index < bytes.length; index++) {
            int unsigned = bytes[index] & 0xff;
            width++;
            if (width == 10 && unsigned != 1) {
                throw new IllegalArgumentException("Invalid tenth byte at offset " + index);
            }
            if ((unsigned & 0x80) == 0) {
                if (width > 1 && unsigned == 0) {
                    throw new IllegalArgumentException("Nonminimal integer at offset " + index);
                }
                values++;
                width = 0;
            }
        }
        if (width != 0) {
            throw new IllegalArgumentException("Truncated integer at end of input");
        }
        return values;
    }

    // PACKED_UNION U10 (2026-11-09): the Synexia common-collections VInt API (same encoding), ported unchanged;
    // the monitor is a WorkMonitor.

    public static final int MAX_BYTES = 10;
    private static final int CHECKPOINT_VALUES = 1024;
    private static final byte[] BYTES_BY_LEADING_ZEROS = bytesByLeadingZeros();


    /** Number of bytes for the complete unsigned 64-bit pattern, including negative longs. */
    public static int unsignedSize(long value) {
        return BYTES_BY_LEADING_ZEROS[Long.numberOfLeadingZeros(value)];
    }

    /** The 65 possible leading-zero counts classify every bit pattern; zero uses one byte. */
    private static byte[] bytesByLeadingZeros() {
        byte[] sizes = new byte[Long.SIZE + 1];
        for (int leadingZeros = 0; leadingZeros <= Long.SIZE; leadingZeros++) {
            sizes[leadingZeros] = (byte) Math.max(1, (Long.SIZE - leadingZeros + 6) / 7);
        }
        return sizes;
    }

    /** Number of bytes after mapping a signed long through ZigZag. */
    public static int signedSize(long value) {
        return unsignedSize(zigZag(value));
    }

    /**
     * Writes one raw unsigned 64-bit pattern to {@code [offset, limit)}.
     * Preflights the complete capacity before changing any byte.
     *
     * @return the exclusive end offset
     * @throws IndexOutOfBoundsException if bounds or remaining capacity are invalid
     * @throws NullPointerException if destination is null
     */
    public static int writeUnsigned(long value, byte[] destination, int offset, int limit) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(offset, limit, destination.length);
        int size = unsignedSize(value);
        if (size > limit - offset) {
            throw new IndexOutOfBoundsException("insufficient capacity for varint");
        }
        return writeRaw(value, destination, offset);
    }

    /** Writes one signed long using ZigZag; bounds, ownership and return match writeUnsigned. */
    public static int writeSigned(long value, byte[] destination, int offset, int limit) {
        return writeUnsigned(zigZag(value), destination, offset, limit);
    }

    /**
     * Reads one canonical unsigned value into {@code destination[destinationIndex]}.
     * Values above {@code Long.MAX_VALUE} use their raw long bit patterns.
     * The destination changes only after the complete value has been validated.
     *
     * @return the exclusive end offset of this value, which can be below limit
     * @throws IllegalArgumentException if the value is truncated, overlong or exceeds 64 bits
     * @throws IndexOutOfBoundsException if source bounds or destination index are invalid
     * @throws NullPointerException if either array is null
     */
    public static int readUnsigned(byte[] source, int offset, int limit,
            long[] destination, int destinationIndex) {
        return read(source, offset, limit, destination, destinationIndex, false);
    }

    /** Reads one ZigZag signed value; bounds, ownership and return match readUnsigned. */
    public static int readSigned(byte[] source, int offset, int limit,
            long[] destination, int destinationIndex) {
        return read(source, offset, limit, destination, destinationIndex, true);
    }

    /**
     * Returns a fresh exact-size encoding of {@code count} values starting at offset.
     * Signed selects ZigZag; false selects raw unsigned 64-bit patterns.
     *
     * @throws ArithmeticException if the encoded byte count exceeds the int index range
     * @throws IndexOutOfBoundsException if the value range is invalid
     * @throws NullPointerException if values is null
     */
    public static byte[] encode(long[] values, int offset, int count,
            boolean signed, WorkMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, count, values.length);
        checkCanceled(monitor);
        int byteCount = 0;
        for (int i = 0; i < count; i++) {
            long value = values[offset + i];
            byteCount = Math.addExact(byteCount, unsignedSize(signed ? zigZag(value) : value));
            if ((i + 1) % CHECKPOINT_VALUES == 0) {
                checkCanceled(monitor);
            }
        }
        checkCanceled(monitor);

        byte[] result = new byte[byteCount];
        int position = 0;
        int pending = 0;
        for (int i = 0; i < count; i++) {
            long value = values[offset + i];
            position = writeRaw(signed ? zigZag(value) : value, result, position);
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finish(monitor, pending);
        return result;
    }

    /** Decodes a complete headerless payload, with signedness supplied by the caller. */
    public static long[] decode(byte[] payload, boolean signed, WorkMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        return decode(payload, 0, payload.length, signed, monitor);
    }

    /**
     * Validates and decodes the entire {@code [offset, limit)} frame to a fresh array.
     * No byte outside that frame is read, and no incomplete trailing value is accepted.
     * The result count is determined by a complete validation pass before allocation.
     *
     * @throws IllegalArgumentException for truncated, overlong or overflowing values
     * @throws IndexOutOfBoundsException if the byte frame is invalid
     * @throws NullPointerException if payload is null
     */
    public static long[] decode(byte[] payload, int offset, int limit,
            boolean signed, WorkMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        Objects.checkFromToIndex(offset, limit, payload.length);
        checkCanceled(monitor);
        int count = 0;
        for (int position = offset; position < limit; ) {
            long value = readRaw(payload, position, limit);
            position += unsignedSize(value);
            if (++count % CHECKPOINT_VALUES == 0) {
                checkCanceled(monitor);
            }
        }
        checkCanceled(monitor);

        long[] result = new long[count];
        int position = offset;
        int pending = 0;
        for (int i = 0; i < count; i++) {
            long value = readRaw(payload, position, limit);
            position += unsignedSize(value);
            result[i] = signed ? undoZigZag(value) : value;
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finish(monitor, pending);
        return result;
    }

    private static int read(byte[] source, int offset, int limit,
            long[] destination, int destinationIndex, boolean signed) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(offset, limit, source.length);
        Objects.checkIndex(destinationIndex, destination.length);
        long value = readRaw(source, offset, limit);
        destination[destinationIndex] = signed ? undoZigZag(value) : value;
        return offset + unsignedSize(value);
    }

    /** Called only after bounds/capacity or bulk allocation has been checked. */
    private static int writeRaw(long value, byte[] destination, int offset) {
        while ((value & ~0x7fL) != 0) {
            destination[offset++] = (byte) ((value & 0x7fL) | 0x80L);
            value >>>= 7;
        }
        destination[offset++] = (byte) value;
        return offset;
    }

    /** Canonical encodings make unsignedSize(value) the exact consumed byte count. */
    private static long readRaw(byte[] source, int offset, int limit) {
        long value = 0L;
        for (int group = 0; group < MAX_BYTES; group++) {
            if (offset == limit) {
                throw new IllegalArgumentException("truncated varint");
            }
            int part = source[offset++] & 0xff;
            if (group == MAX_BYTES - 1) {
                // Only bit 63 remains. Zero is overlong; every byte above one overflows.
                if (part != 1) {
                    throw new IllegalArgumentException("invalid tenth varint byte");
                }
                return value | Long.MIN_VALUE;
            }
            value |= (part & 0x7fL) << (group * 7);
            if ((part & 0x80) == 0) {
                if (group != 0 && part == 0) {
                    throw new IllegalArgumentException("noncanonical varint");
                }
                return value;
            }
        }
        throw new AssertionError("unreachable varint group");
    }

    private static long zigZag(long value) {
        return (value << 1) ^ (value >> 63);
    }

    private static long undoZigZag(long value) {
        return (value >>> 1) ^ -(value & 1L);
    }

    private static void checkCanceled(WorkMonitor monitor) {
        if (monitor != null) {
            monitor.checkCanceled();
        }
    }

    private static void checkpoint(WorkMonitor monitor, int completed) {
        if (monitor != null) {
            monitor.worked(completed);
            monitor.checkCanceled();
        }
    }

    private static void finish(WorkMonitor monitor, int pending) {
        if (pending != 0) {
            checkpoint(monitor, pending);
        }
        checkCanceled(monitor);
    }
}
