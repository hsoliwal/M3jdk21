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
package jdk.internal.m3.collections.mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Read-only memory-mapped view of a verified immutable MAT pool image.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MappedMatPool.java}
 * blob {@code fe153cd5917745de155e1154169a9f47d860d5fb} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MappedMatPool<T> {
    private final MatCodec<T> codec;
    private final MatLimits limits;
    private final MappedByteBuffer image;
    private final MatPoolImage.Layout layout;

    private MappedMatPool(
            MatCodec<T> codec,
            MatLimits limits,
            MappedByteBuffer image,
            MatPoolImage.Layout layout) {
        this.codec = codec;
        this.limits = limits;
        this.image = image;
        this.layout = layout;
    }

    public static <T> MappedMatPool<T> open(Path path, MatCodec<T> codec, MatLimits limits)
            throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(limits, "limits");
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = channel.size();
            limits.imageBytes(size);
            if (size > Integer.MAX_VALUE) {
                throw new IOException("MAT pool image exceeds mapped ByteBuffer range");
            }
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            mapped.order(ByteOrder.BIG_ENDIAN);
            MatPoolImage.Layout layout = MatPoolImage.validate(mapped, codec, limits);
            return new MappedMatPool<>(codec, limits, mapped, layout);
        }
    }

    public MatCodec<T> codec() { return codec; }
    public MatLimits limits() { return limits; }
    public int size() { return layout.count(); }
    public String imageSha256() { return layout.sha256(); }
    public long payloadBytes() { return image.capacity(); }

    public MappedMatRef<T> ref(int id) {
        checkId(id);
        return new MappedMatRef<>(this, id);
    }

    public MatPortableRef portableRef(int id) {
        checkId(id);
        return new MatPortableRef(codec.typeId(), layout.sha256(), id);
    }

    public T value(int id) { return codec.decode(bytes(id)); }

    public byte[] bytes(int id) {
        checkId(id);
        int start = offset(id);
        int end = offset(id + 1);
        byte[] result = new byte[end - start];
        ByteBuffer view = image.duplicate();
        view.position(layout.arenaStart() + start).limit(layout.arenaStart() + end);
        view.get(result);
        return result;
    }

    public int encodedLength(int id) {
        checkId(id);
        return offset(id + 1) - offset(id);
    }

    public long contentHash64(int id) {
        checkId(id);
        return image.getLong(layout.hashesStart() + id * Long.BYTES);
    }

    public int idOf(T value) { return idOfBytes(codec.encode(value)); }

    /** Expected O(1); exact mapped bytes confirm every hash match. */
    public int idOfBytes(byte[] canonical) {
        Objects.requireNonNull(canonical, "canonical");
        limits.valueBytes(canonical.length);
        long hash = MatHash.fnv64(canonical);
        int mask = layout.slotsCount() - 1;
        int slot = MatHash.mix(MatHash.fold32(hash)) & mask;
        for (int probe = 0; probe < layout.slotsCount(); probe++) {
            int stored = image.getInt(layout.slotsStart() + slot * Integer.BYTES);
            if (stored == 0) return -1;
            int id = stored - 1;
            if (contentHash64(id) == hash && equalBytes(id, canonical)) return id;
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    public boolean contains(T value) { return idOf(value) >= 0; }

    /** Materialize one heap pool with identical IDs from the already-verified image. */
    public MatPool<T> toHeap() {
        byte[] arena = new byte[layout.arenaBytes()];
        ByteBuffer view = image.duplicate();
        view.position(layout.arenaStart()).limit(layout.trailerStart());
        view.get(arena);
        int[] offsets = new int[size() + 1];
        long[] hashes = new long[size()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = image.getInt(layout.offsetsStart() + i * Integer.BYTES);
        }
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = image.getLong(layout.hashesStart() + i * Long.BYTES);
        }
        return MatPool.fromCanonical(codec, limits, arena, offsets, hashes);
    }

    private int offset(int index) {
        return image.getInt(layout.offsetsStart() + index * Integer.BYTES);
    }

    private boolean equalBytes(int id, byte[] candidate) {
        int start = offset(id);
        int end = offset(id + 1);
        if (end - start != candidate.length) return false;
        int absolute = layout.arenaStart() + start;
        for (int i = 0; i < candidate.length; i++) {
            if (image.get(absolute + i) != candidate[i]) return false;
        }
        return true;
    }

    private void checkId(int id) {
        if (id < 0 || id >= size()) throw new IndexOutOfBoundsException("pool id: " + id);
    }
}
