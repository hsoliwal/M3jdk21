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
package jdk.internal.m3.collections.packed;

import jdk.m3.arrays.IntWidth;
import jdk.m3.arrays.packed.VInt;
import jdk.m3.collections.ProgressMonitor;
import java.util.Objects;

/**
 * Explicit JNI batch codec for exact little-endian {@link IntWidth} payloads and
 * canonical base-128 unsigned/ZigZag {@link VInt} payloads.
 * The host loads the native library before calling this class. There is no implicit loader,
 * fallback, retained native pointer, or automatic backend selection. Even an empty valid
 * operation calls the native entry point and requires the library to be loaded.
 *
 * <p>Each operation returns a fresh caller-owned array, with no alias to its input and no
 * retained buffers. Callers must not mutate input arrays until the operation returns.
 * Fixed-width encoding validates value ranges in Java before crossing into native.
 * VInt sizing and complete frame validation/counting execute in native code, without a
 * duplicate Java varint decode. Native entry points independently defend their arguments.
 * Each call processes at most 1024 values; VInt readers use a bounded 1024-byte window.
 *
 * <p>A non-null monitor is checked before work, during validation and between native calls.
 * Successful native chunks report one work unit per value. The caller owns the monitor's
 * task lifecycle. Cancellation or failure does not expose a partially produced result.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntNative.java}
 * blob {@code e7e53ce06c00ebd4095caa72464278792fe26941} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections PackedIntNative (U12, second packed lineage JNI codec) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedIntNative {
    private static final int CHUNK_SIZE = 1024;

    private PackedIntNative() { }

    /**
     * Encodes a checked input range to exactly {@code count * width.bytes()} bytes.
     * Unsigned INT64 treats each long as its complete 64-bit pattern.
     *
     * @throws ArithmeticException if an input does not fit, or the byte count overflows int
     * @throws IndexOutOfBoundsException if the input range is invalid
     * @throws NullPointerException if values or width is null
     * @throws UnsatisfiedLinkError if the host has not loaded the native implementation
     */
    public static byte[] encode(long[] values, int offset, int count, IntWidth width,
            boolean signed, ProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(width, "width");
        Objects.checkFromIndexSize(offset, count, values.length);
        int byteCount = Math.multiplyExact(count, width.bytes());
        checkCanceled(monitor);

        for (int checked = 0; checked < count; ) {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - checked);
            for (int i = 0; i < chunk; i++) {
                width.require(values[offset + checked + i], signed);
            }
            checked += chunk;
        }
        checkCanceled(monitor);

        byte[] result = new byte[byteCount];
        if (count == 0) {
            encodeChunk0(values, offset, 0, result, 0, width.bits(), signed);
        }
        for (int completed = 0; completed < count; ) {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - completed);
            encodeChunk0(values, offset + completed, chunk, result,
                    completed * width.bytes(), width.bits(), signed);
            completed += chunk;
            if (monitor != null) {
                monitor.worked(chunk);
            }
        }
        checkCanceled(monitor);
        return result;
    }

    /**
     * Decodes an exact little-endian payload. Signed values are sign-extended to long;
     * unsigned widths below 64 return nonnegative longs and unsigned INT64 returns raw bits.
     *
     * @throws IllegalArgumentException if payload has a partial trailing value
     * @throws NullPointerException if payload or width is null
     * @throws UnsatisfiedLinkError if the host has not loaded the native implementation
     */
    public static long[] decode(byte[] payload, IntWidth width, boolean signed,
            ProgressMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(width, "width");
        if (payload.length % width.bytes() != 0) {
            throw new IllegalArgumentException("Payload length is not a multiple of width");
        }
        checkCanceled(monitor);
        int count = payload.length / width.bytes();
        long[] result = new long[count];
        if (count == 0) {
            decodeChunk0(payload, 0, 0, result, 0, width.bits(), signed);
        }
        for (int completed = 0; completed < count; ) {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - completed);
            decodeChunk0(payload, completed * width.bytes(), chunk, result,
                    completed, width.bits(), signed);
            completed += chunk;
            if (monitor != null) {
                monitor.worked(chunk);
            }
        }
        checkCanceled(monitor);
        return result;
    }

    /**
     * Encodes a selected range as a fresh, exact-size canonical base-128 payload.
     * Signed values use ZigZag; unsigned values preserve every raw long bit pattern.
     * Native sizing completes before output allocation; only the native output pass
     * reports work. Inputs must remain stable through both passes and monitor callbacks.
     *
     * @throws ArithmeticException if the encoded byte count exceeds the int index range
     * @throws IndexOutOfBoundsException if the input range is invalid
     * @throws NullPointerException if values is null
     * @throws UnsatisfiedLinkError if the host has not loaded the native implementation
     */
    public static byte[] encodeVInt(long[] values, int offset, int count,
            boolean signed, ProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, count, values.length);
        checkCanceled(monitor);
        int byteCount = 0;
        int completed = 0;
        do {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - completed);
            int bytes = sizeVIntChunk0(values, offset + completed, chunk, signed);
            if (bytes < chunk || bytes > chunk * VInt.MAX_BYTES) {
                throw new IllegalStateException("Invalid native VInt size result");
            }
            byteCount = Math.addExact(byteCount, bytes);
            completed += chunk;
        } while (completed < count);
        checkCanceled(monitor);

        byte[] result = new byte[byteCount];
        int position = 0;
        completed = 0;
        do {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - completed);
            int next = encodeVIntChunk0(values, offset + completed, chunk,
                    result, position, result.length, signed);
            requireVIntAdvance(position, next, result.length, chunk);
            position = next;
            completed += chunk;
            if (monitor != null && chunk != 0) {
                monitor.worked(chunk);
            }
        } while (completed < count);
        if (position != result.length) {
            throw new IllegalArgumentException("VInt input changed after native sizing");
        }
        checkCanceled(monitor);
        return result;
    }

    /** Decodes a complete canonical VInt payload with caller-supplied signedness. */
    public static long[] decodeVInt(byte[] payload, boolean signed, ProgressMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        return decodeVInt(payload, 0, payload.length, signed, monitor);
    }

    /**
     * Decodes the entire {@code [offset, limit)} byte frame to a fresh exact-size array.
     * Limits are exclusive. Native preflight validates the complete frame before
     * allocating the output; a second native pass materializes its values. Signed
     * selects ZigZag, while false returns complete unsigned 64-bit patterns in longs.
     * No tags, count, signedness metadata or padding are part of the payload.
     *
     * @throws IllegalArgumentException for truncated, overlong or overflowing encodings
     * @throws IndexOutOfBoundsException if the byte frame is invalid
     * @throws NullPointerException if payload is null
     * @throws UnsatisfiedLinkError if the host has not loaded the native implementation
     */
    public static long[] decodeVInt(byte[] payload, int offset, int limit,
            boolean signed, ProgressMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        Objects.checkFromToIndex(offset, limit, payload.length);
        checkCanceled(monitor);
        int position = offset;
        int count = 0;
        do {
            checkCanceled(monitor);
            long scanned = scanVIntChunk0(payload, position, limit);
            int next = (int) (scanned >>> Integer.SIZE);
            int chunk = (int) scanned;
            if (chunk < 0 || chunk > CHUNK_SIZE || (position < limit && chunk == 0)) {
                throw new IllegalStateException("Invalid native VInt count result");
            }
            requireVIntAdvance(position, next, limit, chunk);
            position = next;
            count = Math.addExact(count, chunk);
        } while (position < limit);
        checkCanceled(monitor);

        long[] result = new long[count];
        position = offset;
        int completed = 0;
        do {
            checkCanceled(monitor);
            int chunk = Math.min(CHUNK_SIZE, count - completed);
            int next = decodeVIntChunk0(payload, position, limit, chunk,
                    result, completed, signed);
            requireVIntAdvance(position, next, limit, chunk);
            position = next;
            completed += chunk;
            if (monitor != null && chunk != 0) {
                monitor.worked(chunk);
            }
        } while (completed < count);
        if (position != limit) {
            throw new IllegalArgumentException("VInt frame changed after native preflight");
        }
        checkCanceled(monitor);
        return result;
    }

    /** Defends the Java chunk scheduler against an invalid native position/count result. */
    private static void requireVIntAdvance(int before, int after, int limit, int count) {
        if (after < before || after > limit || after - before < count
                || after - before > count * VInt.MAX_BYTES) {
            throw new IllegalStateException("Invalid native VInt position result");
        }
    }

    private static void checkCanceled(ProgressMonitor monitor) {
        if (monitor != null) {
            monitor.checkCanceled();
        }
    }

    private static native void encodeChunk0(long[] values, int valueOffset, int count,
            byte[] output, int byteOffset, int bits, boolean signed);

    private static native void decodeChunk0(byte[] payload, int byteOffset, int count,
            long[] output, int valueOffset, int bits, boolean signed);

    /** Returns the exact encoded byte count for 0..1024 selected values. */
    private static native int sizeVIntChunk0(long[] values, int valueOffset, int count,
            boolean signed);

    /** Returns the exclusive byte end after validating capacity and staging the whole chunk. */
    private static native int encodeVIntChunk0(long[] values, int valueOffset, int count,
            byte[] output, int byteOffset, int byteLimit, boolean signed);

    /**
     * Validates up to 1024 values. The high unsigned 32 bits hold the exclusive byte end;
     * the low 32 bits hold the count. Only an empty frame returns a zero count.
     */
    private static native long scanVIntChunk0(byte[] payload, int byteOffset, int byteLimit);

    /** Decodes exactly count values and returns their exclusive byte end, possibly below limit. */
    private static native int decodeVIntChunk0(byte[] payload, int byteOffset, int byteLimit,
            int count, long[] output, int valueOffset, boolean signed);
}
