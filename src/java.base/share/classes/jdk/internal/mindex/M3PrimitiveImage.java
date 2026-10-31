/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package jdk.internal.mindex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Read-only, compile-time built primitive intern image shared through OS page cache.
 *
 * <p>Each canonical entry is (SHA-256 image namespace, 0-based slot), resolved at
 * byte offset 64 + 16 * slot. Mapping virtual addresses can differ among VMs.
 * There is no per-primitive native allocation, native pointer, or global mutable
 * interner. Never equate a slot from another image without checking namespace.</p>
 *
 * <p>Format v1: 64-byte header: 8-byte magic, 4-byte version, 4-byte count,
 * 8-byte total length, 32-byte SHA-256 of magic||version||count||rows,
 * 8 zero bytes. Every 16-byte sorted row has 1-byte kind, 7 reserved zero
 * bytes and 8-byte unsigned raw bits; all fields are big endian. Exact bytes,
 * not a hash lookup alone, determine the row's identity.</p>
 */
public final class M3PrimitiveImage {
    public static final long MAGIC = 0x4d335052494d3031L; // M3PRIM01
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 64;
    public static final int ROW_BYTES = 16;
    public static final int MAX_ROWS = 1 << 20;

    private final ByteBuffer view;
    private final byte[] namespace;
    private final int size;

    private M3PrimitiveImage(ByteBuffer view, byte[] namespace, int size) {
        this.view = view.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
        this.namespace = namespace;
        this.size = size;
    }

    public static M3PrimitiveImage open(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long length = channel.size();
            if (length < HEADER_BYTES || length > HEADER_BYTES + (long) MAX_ROWS * ROW_BYTES) {
                throw new IOException("invalid M3 primitive image length");
            }
            ByteBuffer bytes = channel.map(FileChannel.MapMode.READ_ONLY, 0L, length)
                    .order(ByteOrder.BIG_ENDIAN);
            if (bytes.getLong(0) != MAGIC || bytes.getInt(8) != VERSION) {
                throw new IOException("unknown M3 primitive image format");
            }
            int count = bytes.getInt(12);
            if (count < 0 || count > MAX_ROWS
                    || length != HEADER_BYTES + (long) count * ROW_BYTES
                    || bytes.getLong(16) != length || bytes.getLong(56) != 0L) {
                throw new IOException("invalid M3 primitive image geometry");
            }
            byte[] expected = new byte[32];
            bytes.position(24);
            bytes.get(expected);
            MessageDigest hash;
            try {
                hash = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new InternalError(impossible);
            }
            ByteBuffer prefix = bytes.duplicate();
            prefix.position(0).limit(16);
            hash.update(prefix);
            ByteBuffer rows = bytes.duplicate();
            rows.position(HEADER_BYTES).limit((int) length);
            hash.update(rows);
            if (!MessageDigest.isEqual(expected, hash.digest())) {
                throw new IOException("M3 primitive image SHA-256 mismatch");
            }
            int previousKind = 0;
            long previousBits = 0L;
            for (int slot = 0; slot < count; slot++) {
                int offset = HEADER_BYTES + ROW_BYTES * slot;
                int kind = Byte.toUnsignedInt(bytes.get(offset));
                long bits = bytes.getLong(offset + 8);
                if (!M3PrimitiveIdentity.valid(kind, bits) || bytes.get(offset + 1) != 0
                        || bytes.getShort(offset + 2) != 0 || bytes.getInt(offset + 4) != 0
                        || (slot != 0 && M3PrimitiveIdentity.compareRaw(
                                previousKind, previousBits, kind, bits) >= 0)) {
                    throw new IOException("invalid/noncanonical M3 primitive image entry " + slot);
                }
                previousKind = kind;
                previousBits = bits;
            }
            return new M3PrimitiveImage(bytes, expected, count);
        }
    }

    public int size() { return size; }
    public byte[] namespace() { return namespace.clone(); }
    public int kind(int slot) { return Byte.toUnsignedInt(view.get(rowOffset(slot))); }
    public long bits(int slot) { return view.getLong(rowOffset(slot) + 8); }
    public long offset(int slot) { return rowOffset(slot); }
    public long byteLength() { return HEADER_BYTES + (long) ROW_BYTES * size; }

    private int rowOffset(int slot) {
        Objects.checkIndex(slot, size);
        return HEADER_BYTES + ROW_BYTES * slot;
    }

    /** Deterministic lookup. The compiler emits the slot for O(1) resolution. */
    public int find(int kind, long bits) {
        if (!M3PrimitiveIdentity.valid(kind, bits)) return -1;
        int low = 0, high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int at = HEADER_BYTES + ROW_BYTES * mid;
            int cmp = M3PrimitiveIdentity.compareRaw(
                    Byte.toUnsignedInt(view.get(at)), view.getLong(at + 8), kind, bits);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -1;
    }

    /** A slot is only equal across images when its content-addressed namespace matches. */
    public boolean sameSlot(M3PrimitiveImage other, int leftSlot, int rightSlot) {
        Objects.requireNonNull(other, "other");
        rowOffset(leftSlot);
        other.rowOffset(rightSlot);
        return leftSlot == rightSlot && Arrays.equals(namespace, other.namespace);
    }
}
