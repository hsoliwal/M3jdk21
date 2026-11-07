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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Deterministic SHA-256-trailed immutable image for one exact typed M3MatPool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPoolImage.java}
 * blob {@code 6a5c8f9fa5f09b0dfd911ada5e35bf6e4d720ff0} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatPoolImage {
    static final byte[] MAGIC = {'S','N','X','M','A','T','P','1'};
    static final int VERSION = 1;
    static final int SHA256_BYTES = 32;

    private M3MatPoolImage() { }

    public static <T> byte[] encode(M3MatPool<T> pool) {
        Objects.requireNonNull(pool, "pool");
        byte[] type = pool.codec().typeId().getBytes(StandardCharsets.UTF_8);
        byte[] arena = pool.arenaCopy();
        int[] offsets = pool.offsetsCopy();
        long[] hashes = pool.hashesCopy();
        int[] slots = pool.slotsCopy();
        if (type.length == 0 || type.length > 65_535) throw new IllegalArgumentException("type ID byte length");

        long total = MAGIC.length + 4L + 4L + type.length + 4L + 4L + 4L
                + 4L * offsets.length + 8L * hashes.length + 4L * slots.length
                + arena.length + SHA256_BYTES;
        pool.limits().imageBytes(total);
        if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("pool image too large");

        ByteBuffer out = ByteBuffer.allocate((int) total).order(ByteOrder.BIG_ENDIAN);
        out.put(MAGIC).putInt(VERSION).putInt(type.length).put(type);
        out.putInt(hashes.length).putInt(arena.length).putInt(slots.length);
        for (int value : offsets) out.putInt(value);
        for (long value : hashes) out.putLong(value);
        for (int value : slots) out.putInt(value);
        out.put(arena);
        int trailer = out.position();
        out.put(digest(out, 0, trailer));
        return out.array();
    }

    public static <T> M3MatPool<T> decode(byte[] image, M3MatCodec<T> codec, M3MatLimits limits) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(limits, "limits");
        limits.imageBytes(image.length);
        ByteBuffer buffer = ByteBuffer.wrap(image).asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
        Layout layout = validate(buffer, codec, limits);
        byte[] arena = new byte[layout.arenaBytes()];
        ByteBuffer data = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
        data.position(layout.arenaStart()).limit(layout.trailerStart());
        data.get(arena);
        int[] offsets = new int[layout.count() + 1];
        long[] hashes = new long[layout.count()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = buffer.getInt(layout.offsetsStart() + i * Integer.BYTES);
        }
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = buffer.getLong(layout.hashesStart() + i * Long.BYTES);
        }
        return M3MatPool.fromCanonical(codec, limits, arena, offsets, hashes);
    }

    public static <T> M3MatPool<T> read(Path path, M3MatCodec<T> codec, M3MatLimits limits) throws IOException {
        Objects.requireNonNull(path, "path");
        long size = Files.size(path);
        limits.imageBytes(size);
        if (size > Integer.MAX_VALUE) throw new IOException("pool image exceeds Java byte[] range");
        return decode(Files.readAllBytes(path), codec, limits);
    }

    /** Create a new immutable artifact; existing targets are never overwritten. */
    public static <T> void writeNew(Path path, M3MatPool<T> pool) throws IOException {
        Objects.requireNonNull(path, "path");
        byte[] image = encode(pool);
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(image);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    public static String sha256(byte[] image) {
        return HexFormat.of().formatHex(sha256Bytes(Objects.requireNonNull(image, "image")));
    }

    static <T> Layout validate(ByteBuffer source, M3MatCodec<T> codec, M3MatLimits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(limits, "limits");
        ByteBuffer buffer = source.duplicate().order(ByteOrder.BIG_ENDIAN);
        limits.imageBytes(buffer.remaining());
        if (buffer.remaining() < MAGIC.length + 6 * Integer.BYTES + SHA256_BYTES) {
            throw new IllegalArgumentException("truncated MAT pool image");
        }
        int base = buffer.position();
        for (byte expected : MAGIC) {
            if (buffer.get() != expected) throw new IllegalArgumentException("MAT pool magic");
        }
        if (buffer.getInt() != VERSION) throw new IllegalArgumentException("MAT pool version");
        int typeBytes = buffer.getInt();
        if (typeBytes <= 0 || typeBytes > 65_535 || typeBytes > buffer.remaining()) {
            throw new IllegalArgumentException("MAT pool type length");
        }
        byte[] actualType = new byte[typeBytes];
        buffer.get(actualType);
        byte[] expectedType = codec.typeId().getBytes(StandardCharsets.UTF_8);
        if (!Arrays.equals(actualType, expectedType)) {
            throw new IllegalArgumentException("MAT pool codec/type mismatch");
        }
        if (buffer.remaining() < 3 * Integer.BYTES + SHA256_BYTES) {
            throw new IllegalArgumentException("truncated MAT pool header");
        }
        int count = buffer.getInt();
        int arenaBytes = buffer.getInt();
        int slotsCount = buffer.getInt();
        if (count < 0 || arenaBytes < 0 || slotsCount < 2
                || (slotsCount & (slotsCount - 1)) != 0) {
            throw new IllegalArgumentException("MAT pool dimensions");
        }
        limits.entries(count);
        limits.arenaBytes(arenaBytes);
        long expectedBody = (long) (count + 1) * Integer.BYTES
                + (long) count * Long.BYTES
                + (long) slotsCount * Integer.BYTES
                + arenaBytes + SHA256_BYTES;
        if (expectedBody != buffer.remaining()) {
            throw new IllegalArgumentException("MAT pool frame size mismatch");
        }
        int offsetsStart = buffer.position();
        int hashesStart = Math.addExact(offsetsStart, Math.multiplyExact(count + 1, Integer.BYTES));
        int slotsStart = Math.addExact(hashesStart, Math.multiplyExact(count, Long.BYTES));
        int arenaStart = Math.addExact(slotsStart, Math.multiplyExact(slotsCount, Integer.BYTES));
        int trailerStart = Math.addExact(arenaStart, arenaBytes);
        if ((long) trailerStart + SHA256_BYTES != source.limit()) {
            throw new IllegalArgumentException("MAT pool layout mismatch");
        }

        byte[] expectedDigest = digest(buffer, base, trailerStart - base);
        byte[] storedDigest = new byte[SHA256_BYTES];
        ByteBuffer digestView = buffer.duplicate();
        digestView.position(trailerStart).get(storedDigest);
        if (!MessageDigest.isEqual(expectedDigest, storedDigest)) {
            throw new IllegalArgumentException("MAT pool SHA-256 mismatch");
        }

        if (buffer.getInt(offsetsStart) != 0) {
            throw new IllegalArgumentException("MAT pool offsets must start at zero");
        }
        long[] hashes = new long[count];
        int previous = 0;
        for (int id = 0; id < count; id++) {
            int end = buffer.getInt(offsetsStart + (id + 1) * Integer.BYTES);
            if (end < previous || end > arenaBytes) {
                throw new IllegalArgumentException("MAT pool offsets");
            }
            int length = end - previous;
            limits.valueBytes(length);
            long hash = buffer.getLong(hashesStart + id * Long.BYTES);
            if (M3MatHash.fnv64(buffer, arenaStart + previous, length) != hash) {
                throw new IllegalArgumentException("MAT pool content hash mismatch");
            }
            hashes[id] = hash;
            previous = end;
        }
        if (previous != arenaBytes) throw new IllegalArgumentException("MAT pool arena tail");
        int[] expectedSlots = M3MatPool.buildSlots(hashes);
        if (expectedSlots.length != slotsCount) {
            throw new IllegalArgumentException("MAT pool slot count mismatch");
        }
        for (int i = 0; i < slotsCount; i++) {
            if (buffer.getInt(slotsStart + i * Integer.BYTES) != expectedSlots[i]) {
                throw new IllegalArgumentException("MAT pool slot index mismatch");
            }
        }
        return new Layout(
                count, arenaBytes, slotsCount, offsetsStart, hashesStart, slotsStart,
                arenaStart, trailerStart, HexFormat.of().formatHex(storedDigest));
    }

    private static byte[] digest(ByteBuffer source, int start, int byteCount) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer view = source.duplicate();
            view.position(start);
            view.limit(Math.addExact(start, byteCount));
            digest.update(view);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static byte[] sha256Bytes(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    record Layout(
            int count,
            int arenaBytes,
            int slotsCount,
            int offsetsStart,
            int hashesStart,
            int slotsStart,
            int arenaStart,
            int trailerStart,
            String sha256) { }
}
