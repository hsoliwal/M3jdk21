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

import jdk.m3.arrays.IntWidth;
import jdk.m3.arrays.packed.PackedIntArray;
import jdk.m3.collections.ProgressMonitor;
import java.util.Objects;

/**
 * Fixed-length integer storage whose blocks independently widen from 32 through 40, 48, 56,
 * and 64 bits. Each block owns one packed payload; no object is retained per logical value.
 *
 * <p>Writes never shrink a block. A widening write prepares a replacement before publishing it.
 * {@link #compact(ProgressMonitor)} publishes all narrowed blocks together after successful
 * preparation. Cancellation observed before publication leaves the array unchanged. Publication
 * itself is noninterruptible, and no callback runs afterward. A null monitor is accepted.
 *
 * <p>Not thread-safe. Reentrant mutation during monitored operations is rejected, including
 * mutation attempted while serializing. Callers must not concurrently change serialized input.
 * Monitor progress counts prepared values and may exceed the logical length during multipass
 * work; operations do not begin or finish the caller's task lifecycle.
 * Callers must not mutate bulk input/output arrays during a call, including from callbacks.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/AdaptiveIntArray.java}
 * blob {@code fd4b2d1ae9f20566f3b6f8995dfec3599edc072f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections AdaptiveIntArray on the U9 PackedIntArray union (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AdaptiveIntArray {
    private static final int DEFAULT_BLOCK_SIZE = 1024;
    private static final int CHECKPOINT_VALUES = 1024;
    private static final int HEADER_BYTES = 24;
    private static final byte[] MAGIC = {'M', '3', 'I', 'A', 'R', 'R', 'A', 'Y'};

    private final int length;
    private final int blockSize;
    private final boolean signed;
    private PackedIntArray[] blocks;
    private int protectedOperations;

    public AdaptiveIntArray(int length, int blockSize, boolean signed) {
        if (length < 0 || blockSize <= 0) {
            throw new IllegalArgumentException("length must be nonnegative and blockSize positive");
        }
        this.length = length;
        this.blockSize = blockSize;
        this.signed = signed;
        int count = blockCount(length, blockSize);
        this.blocks = new PackedIntArray[count];
        for (int b = 0; b < count; b++) {
            blocks[b] = PackedIntArray.of(blockLength(length, blockSize, b), IntWidth.INT32,
                    signed);
        }
    }

    public AdaptiveIntArray(int length, boolean signed) {
        this(length, DEFAULT_BLOCK_SIZE, signed);
    }

    private AdaptiveIntArray(int length, int blockSize, boolean signed, PackedIntArray[] blocks) {
        this.length = length;
        this.blockSize = blockSize;
        this.signed = signed;
        this.blocks = blocks;
    }

    public int length() {
        return length;
    }

    public int blockCount() {
        return blocks.length;
    }

    public int blockSize() {
        return blockSize;
    }

    public boolean signed() {
        return signed;
    }

    /** Retained element payload only; excludes object, reference-array, and wire headers. */
    public long payloadBytes() {
        long bytes = 0L;
        for (PackedIntArray block : blocks) {
            bytes += block.payloadBytes();
        }
        return bytes;
    }

    public IntWidth blockWidth(int block) {
        return blocks[Objects.checkIndex(block, blocks.length)].width();
    }

    public long get(int index) {
        return get(index, null);
    }

    public long get(int index, ProgressMonitor monitor) {
        Objects.checkIndex(index, length);
        protectedOperations++;
        try {
            checkCanceled(monitor);
            return blocks[index / blockSize].get(index % blockSize);
        } finally {
            protectedOperations--;
        }
    }

    public void set(int index, long value) {
        set(index, value, null);
    }

    /** A widening write changes only the block containing {@code index}. */
    public void set(int index, long value, ProgressMonitor monitor) {
        writable();
        Objects.checkIndex(index, length);
        protectedOperations++;
        try {
            checkCanceled(monitor);
            int blockIndex = index / blockSize;
            int slot = index % blockSize;
            PackedIntArray current = blocks[blockIndex];
            if (current.width().fits(value, signed)) {
                current.set(slot, value);
                return;
            }
            IntWidth wider = IntWidth.smallest(value, signed);
            PackedIntArray replacement = PackedIntArray.of(current.length(), wider, signed);
            int pending = 0;
            for (int i = 0; i < current.length(); i++) {
                replacement.set(i, i == slot ? value : current.get(i));
                if (++pending == CHECKPOINT_VALUES) {
                    checkpoint(monitor, pending);
                    pending = 0;
                }
            }
            finishPreparation(monitor, pending);
            blocks[blockIndex] = replacement;
        } finally {
            protectedOperations--;
        }
    }

    /**
     * Copies a checked source range into this array without publishing a partial update.
     * Each affected block's required width is selected before payload preparation; a block
     * is prepared once at the greater of its current and incoming widths. Existing widths
     * never shrink. Unaffected blocks retain their payloads and identities.
     *
     * <p>Work is linear in {@code count} plus the total lengths of the affected blocks.
     * Temporary storage holds those replacement blocks and two reference arrays sized only
     * to the affected block count. This avoids repeated whole-block widening when a batch
     * contains progressively larger values. Small same-width writes may use less scratch
     * through {@link #set(int, long, ProgressMonitor)} when range atomicity is not required.
     *
     * @throws NullPointerException if source is null
     * @throws IndexOutOfBoundsException if either selected range is invalid
     * @throws IllegalArgumentException if a required block payload exceeds the Java array limit
     */
    public void copyFrom(long[] source, int sourceOffset, int destinationOffset, int count,
            ProgressMonitor monitor) {
        writable();
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(sourceOffset, count, source.length);
        Objects.checkFromIndexSize(destinationOffset, count, length);
        replaceRange(source, sourceOffset, destinationOffset, count, 0L, monitor);
    }

    /**
     * Fills {@code [from, to)} through one prepared replacement per affected block, widening
     * where required and preserving all other values. Cancellation or a callback exception
     * before publication leaves the entire array unchanged. Empty ranges never widen a block.
     *
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if a required block payload exceeds the Java array limit
     */
    public void fill(int from, int to, long value, ProgressMonitor monitor) {
        writable();
        Objects.checkFromToIndex(from, to, length);
        replaceRange(null, 0, from, to - from, value, monitor);
    }

    /**
     * Decodes a checked range into the caller-owned destination. A supplied monitor selects
     * staged publication: cancellation or a callback exception leaves the destination
     * unchanged. With a null monitor, complete range validation precedes direct block-range
     * decoding and no temporary value array is allocated. The source is unchanged in both paths.
     *
     * @throws NullPointerException if destination is null
     * @throws IndexOutOfBoundsException if either selected range is invalid
     */
    public void copyTo(int sourceOffset, long[] destination, int destinationOffset, int count,
            ProgressMonitor monitor) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(sourceOffset, count, length);
        Objects.checkFromIndexSize(destinationOffset, count, destination.length);
        protectedOperations++;
        try {
            checkCanceled(monitor);
            if (count == 0) {
                return;
            }
            long[] output = monitor == null ? destination : new long[count];
            int outputOffset = monitor == null ? destinationOffset : 0;
            int block = sourceOffset / blockSize;
            int slot = sourceOffset % blockSize;
            int completed = 0;
            int pending = 0;
            while (completed < count) {
                PackedIntArray current = blocks[block++];
                int end = slot + Math.min(current.length() - slot, count - completed);
                for (int i = slot; i < end; i++) {
                    output[outputOffset + completed++] = current.get(i);
                    if (++pending == CHECKPOINT_VALUES) {
                        checkpoint(monitor, pending);
                        pending = 0;
                    }
                }
                slot = 0;
            }
            finishPreparation(monitor, pending);
            if (monitor != null) {
                System.arraycopy(output, 0, destination, destinationOffset, count);
            }
        } finally {
            protectedOperations--;
        }
    }

    /** Null source denotes a constant fill; public entry points preflight every array range. */
    private void replaceRange(long[] source, int sourceOffset, int destinationOffset, int count,
            long fillValue, ProgressMonitor monitor) {
        protectedOperations++;
        try {
            checkCanceled(monitor);
            if (count == 0) {
                return;
            }
            int firstBlock = destinationOffset / blockSize;
            int rangeEnd = destinationOffset + count;
            int affected = (rangeEnd - 1) / blockSize - firstBlock + 1;
            IntWidth[] widths = new IntWidth[affected];
            IntWidth fillWidth = source == null ? IntWidth.smallest(fillValue, signed) : null;
            int input = sourceOffset;
            int pending = 0;
            for (int localBlock = 0; localBlock < affected; localBlock++) {
                int block = firstBlock + localBlock;
                PackedIntArray current = blocks[block];
                IntWidth width = current.width();
                if (source == null) {
                    if (fillWidth.bits() > width.bits()) {
                        width = fillWidth;
                    }
                    if ((localBlock & (CHECKPOINT_VALUES - 1)) == 0) {
                        checkCanceled(monitor);
                    }
                } else {
                    int blockStart = block * blockSize;
                    int from = Math.max(0, destinationOffset - blockStart);
                    int to = Math.min(current.length(), rangeEnd - blockStart);
                    for (int i = from; i < to; i++) {
                        long value = source[input++];
                        if (!width.fits(value, signed)) {
                            width = IntWidth.smallest(value, signed);
                        }
                        if (++pending == CHECKPOINT_VALUES) {
                            checkpoint(monitor, pending);
                            pending = 0;
                        }
                    }
                }
                if ((long) current.length() * width.bytes() > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("packed payload exceeds Java array length");
                }
                widths[localBlock] = width;
            }
            finishPreparation(monitor, pending);

            PackedIntArray[] replacements = new PackedIntArray[affected];
            for (int localBlock = 0; localBlock < affected; localBlock++) {
                int block = firstBlock + localBlock;
                int blockStart = block * blockSize;
                PackedIntArray current = blocks[block];
                int from = Math.max(0, destinationOffset - blockStart);
                int to = Math.min(current.length(), rangeEnd - blockStart);
                int inputStart = sourceOffset + Math.max(blockStart - destinationOffset, 0);
                replacements[localBlock] = prepareBlock(current, widths[localBlock], from, to,
                        source, inputStart, fillValue, monitor);
            }
            checkCanceled(monitor);
            System.arraycopy(replacements, 0, blocks, firstBlock, affected);
        } finally {
            protectedOperations--;
        }
    }

    private PackedIntArray prepareBlock(PackedIntArray current, IntWidth width, int from, int to,
            long[] source, int sourceOffset, long fillValue, ProgressMonitor monitor) {
        PackedIntArray replacement = PackedIntArray.of(current.length(), width, signed);
        int pending = 0;
        for (int i = 0; i < current.length(); i++) {
            long value = i < from || i >= to ? current.get(i)
                    : source == null ? fillValue : source[sourceOffset + i - from];
            replacement.set(i, value);
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finishPreparation(monitor, pending);
        return replacement;
    }

    /**
     * Narrows each block to the smallest supported width fitting every retained value.
     * All replacements become visible together after the final cancellation check.
     */
    public void compact(ProgressMonitor monitor) {
        writable();
        protectedOperations++;
        try {
            checkCanceled(monitor);
            PackedIntArray[] replacements = blocks.clone();
            for (int b = 0; b < blocks.length; b++) {
                PackedIntArray current = blocks[b];
                IntWidth narrowest = IntWidth.INT32;
                int pending = 0;
                for (int i = 0; i < current.length(); i++) {
                    IntWidth required = IntWidth.smallest(current.get(i), signed);
                    if (required.bits() > narrowest.bits()) {
                        narrowest = required;
                    }
                    if (++pending == CHECKPOINT_VALUES) {
                        checkpoint(monitor, pending);
                        pending = 0;
                    }
                }
                finishPreparation(monitor, pending);
                if (narrowest != current.width()) {
                    PackedIntArray replacement = PackedIntArray.of(current.length(), narrowest,
                            signed);
                    pending = 0;
                    for (int i = 0; i < current.length(); i++) {
                        replacement.set(i, current.get(i));
                        if (++pending == CHECKPOINT_VALUES) {
                            checkpoint(monitor, pending);
                            pending = 0;
                        }
                    }
                    finishPreparation(monitor, pending);
                    replacements[b] = replacement;
                }
            }
            checkCanceled(monitor);
            blocks = replacements;
        } finally {
            protectedOperations--;
        }
    }

    /**
     * Returns an independent deterministic wire image. The 24-byte header contains ASCII
     * {@code M3IARRAY}, version 1, signed flag 0/1, two zero reserved bytes, and little-endian
     * 32-bit length, block size, and block count. Each block then contributes one bit-width byte
     * followed by its exact little-endian payload. Current block widths are preserved.
     */
    public byte[] toByteArray(ProgressMonitor monitor) {
        protectedOperations++;
        try {
            checkCanceled(monitor);
            PackedIntArray[] snapshot = blocks;
            long bytes = HEADER_BYTES + (long) snapshot.length;
            for (int b = 0; b < snapshot.length; b++) {
                bytes += snapshot[b].payloadBytes();
                if ((b & (CHECKPOINT_VALUES - 1)) == 0) {
                    checkCanceled(monitor);
                }
            }
            if (bytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("wire image exceeds Java array length");
            }
            byte[] result = new byte[(int) bytes];
            System.arraycopy(MAGIC, 0, result, 0, MAGIC.length);
            result[8] = 1;
            result[9] = (byte) (signed ? 1 : 0);
            putInt(result, 12, length);
            putInt(result, 16, blockSize);
            putInt(result, 20, snapshot.length);
            int offset = HEADER_BYTES;
            for (PackedIntArray block : snapshot) {
                result[offset++] = (byte) block.width().bits();
                byte[] payload = block.toByteArray(monitor);
                for (int position = 0; position < block.length();) {
                    int count = Math.min(CHECKPOINT_VALUES, block.length() - position);
                    int sourceOffset = position * block.width().bytes();
                    int byteCount = count * block.width().bytes();
                    System.arraycopy(payload, sourceOffset, result, offset, byteCount);
                    offset += byteCount;
                    position += count;
                    checkCanceled(monitor);
                }
            }
            checkCanceled(monitor);
            return result;
        } finally {
            protectedOperations--;
        }
    }

    /**
     * Validates the complete wire image before allocating any value blocks, then imports an
     * independent copy. Unknown widths, invalid flags/reserved bytes, inconsistent counts,
     * truncation, arithmetic-overflow claims, and trailing bytes are rejected.
     *
     * @throws NullPointerException if {@code bytes} is null
     * @throws IllegalArgumentException if the nonnull wire image is malformed
     */
    public static AdaptiveIntArray fromByteArray(byte[] bytes, ProgressMonitor monitor) {
        Objects.requireNonNull(bytes, "bytes");
        checkCanceled(monitor);
        validate(bytes, monitor);
        byte[] snapshot = new byte[bytes.length];
        for (int offset = 0; offset < bytes.length;) {
            int count = Math.min(CHECKPOINT_VALUES * IntWidth.INT32.bytes(), bytes.length - offset);
            System.arraycopy(bytes, offset, snapshot, offset, count);
            offset += count;
            checkCanceled(monitor);
        }
        Layout layout = validate(snapshot, monitor);
        PackedIntArray[] decoded = new PackedIntArray[layout.count];
        int offset = HEADER_BYTES;
        for (int b = 0; b < decoded.length; b++) {
            IntWidth width = readWidth(snapshot[offset++] & 0xff);
            int values = blockLength(layout.length, layout.blockSize, b);
            int byteCount = (int) ((long) values * width.bytes());
            byte[] payload = new byte[byteCount];
            for (int position = 0; position < values;) {
                int count = Math.min(CHECKPOINT_VALUES, values - position);
                int chunkBytes = count * width.bytes();
                System.arraycopy(snapshot, offset, payload, position * width.bytes(), chunkBytes);
                offset += chunkBytes;
                position += count;
                checkCanceled(monitor);
            }
            decoded[b] = PackedIntArray.fromByteArray(payload, width, layout.signed, monitor);
        }
        checkCanceled(monitor);
        return new AdaptiveIntArray(layout.length, layout.blockSize, layout.signed, decoded);
    }

    private static Layout validate(byte[] bytes, ProgressMonitor monitor) {
        if (bytes.length < HEADER_BYTES) {
            throw new IllegalArgumentException("truncated adaptive integer header");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (bytes[i] != MAGIC[i]) {
                throw new IllegalArgumentException("invalid adaptive integer magic");
            }
        }
        if (bytes[8] != 1 || (bytes[9] != 0 && bytes[9] != 1)
                || bytes[10] != 0 || bytes[11] != 0) {
            throw new IllegalArgumentException("unsupported version, flags, or reserved bytes");
        }
        int length = readInt(bytes, 12);
        int blockSize = readInt(bytes, 16);
        int count = readInt(bytes, 20);
        if (length < 0 || blockSize <= 0 || count < 0
                || count != blockCount(length, blockSize)) {
            throw new IllegalArgumentException("invalid adaptive integer dimensions");
        }
        long minimumBytes = HEADER_BYTES + (long) count + (long) length * IntWidth.INT32.bytes();
        if (minimumBytes > bytes.length) {
            throw new IllegalArgumentException("truncated adaptive integer payload");
        }
        long offset = HEADER_BYTES;
        for (int b = 0; b < count; b++) {
            if (offset >= bytes.length) {
                throw new IllegalArgumentException("missing block width");
            }
            IntWidth width = readWidth(bytes[(int) offset++] & 0xff);
            long byteCount = (long) blockLength(length, blockSize, b) * width.bytes();
            offset += byteCount;
            if (byteCount > Integer.MAX_VALUE || offset > bytes.length) {
                throw new IllegalArgumentException("truncated or oversized block payload");
            }
            if ((b & (CHECKPOINT_VALUES - 1)) == 0) {
                checkCanceled(monitor);
            }
        }
        if (offset != bytes.length) {
            throw new IllegalArgumentException("trailing bytes after adaptive integer payload");
        }
        checkCanceled(monitor);
        return new Layout(length, blockSize, count, bytes[9] == 1);
    }

    private static IntWidth readWidth(int bits) {
        return switch (bits) {
            case 32 -> IntWidth.INT32;
            case 40 -> IntWidth.INT40;
            case 48 -> IntWidth.INT48;
            case 56 -> IntWidth.INT56;
            case 64 -> IntWidth.INT64;
            default -> throw new IllegalArgumentException("unsupported block width: " + bits);
        };
    }

    private static int blockCount(int length, int blockSize) {
        return (int) (((long) length + blockSize - 1L) / blockSize);
    }

    private static int blockLength(int length, int blockSize, int block) {
        return (int) Math.min(blockSize, (long) length - (long) block * blockSize);
    }

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16) | ((bytes[offset + 3] & 0xff) << 24);
    }

    private static void putInt(byte[] bytes, int offset, int value) {
        for (int b = 0; b < Integer.BYTES; b++) {
            bytes[offset + b] = (byte) (value >>> (b * Byte.SIZE));
        }
    }

    private void writable() {
        if (protectedOperations != 0) {
            throw new IllegalStateException("reentrant mutation of adaptive integer array");
        }
    }

    private static void checkCanceled(ProgressMonitor monitor) {
        if (monitor != null) {
            monitor.checkCanceled();
        }
    }

    private static void checkpoint(ProgressMonitor monitor, int completed) {
        if (monitor != null) {
            monitor.worked(completed);
            monitor.checkCanceled();
        }
    }

    private static void finishPreparation(ProgressMonitor monitor, int pending) {
        if (pending != 0) {
            checkpoint(monitor, pending);
        }
        checkCanceled(monitor);
    }

    private record Layout(int length, int blockSize, int count, boolean signed) { }
}
