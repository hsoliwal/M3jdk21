/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Explicitly opened, read-only, versioned lookup image. No network or constructor hook.
 * Multiple processes may map the same immutable file through the OS page cache.
 * Published pieces COPY record bytes into a local arena: read-only file mappings
 * do not prevent another process from altering the file. Direct mapped String
 * backing is deliberately not implemented by this foundation.
 * The channel closes after mapping; the image keeps the mapping alive until GC.
 */
public final class SharedLexiconImage {
    private static final long MAGIC = 0x4d334c4558303031L; // M3LEX001
    private static final int HEADER = 64;
    public static final int MAX_IMAGE_BYTES = 64 * 1024 * 1024;
    public static final int MAX_RECORDS = 65536;
    private final MappedByteBuffer mapping;
    private final int[] offsets, lengths;
    private final byte[] recordDigests;
    private final int payloadOffset;
    private final String imageIdentity;

    private SharedLexiconImage(MappedByteBuffer data) throws IOException {
        mapping = data;
        data.order(ByteOrder.BIG_ENDIAN);
        if (data.limit() < HEADER || data.getLong(0) != MAGIC || data.getInt(8) != 1)
            throw new IOException("unsupported image header");
        int count = data.getInt(12);
        long payload = data.getLong(16), units = data.getLong(24);
        if (count < 0 || count > MAX_RECORDS || units < 0
                || units > MAX_IMAGE_BYTES / 2 || payload != HEADER + 8L * count
                || payload + 2L * units != data.limit()) throw new IOException("invalid image dimensions");
        payloadOffset = (int)payload;
        byte[] expected = new byte[32];
        data.get(32, expected);
        if (!MessageDigest.isEqual(expected, digestImage(data))) throw new IOException("image checksum mismatch");
        imageIdentity = HexFormat.of().formatHex(expected);
        offsets = new int[count]; lengths = new int[count]; recordDigests = new byte[count * 32];
        // Snapshot metadata and record checksums before publishing this image owner.
        for (int i = 0; i < count; i++) {
            int offset = data.getInt(HEADER + i * 8), length = data.getInt(HEADER + i * 8 + 4);
            if (offset < 0 || length < 0 || (long)offset + length > units) throw new IOException("invalid record range");
            offsets[i] = offset; lengths[i] = length;
            System.arraycopy(digest(recordBytes(i)), 0, recordDigests, i * 32, 32);
        }
        // Detect changes during initial metadata/hash capture. Source files must be
        // immutable after publication; this is corruption detection, not OS sealing.
        if (!MessageDigest.isEqual(expected, digestImage(data))) throw new IOException("image changed while opening");
    }

    public static SharedLexiconImage open(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size < HEADER || size > MAX_IMAGE_BYTES) throw new IOException("image size out of bounds");
            return new SharedLexiconImage(channel.map(FileChannel.MapMode.READ_ONLY, 0, size));
        }
    }
    /** Explicit warming after bootstrap; opening additional shards is caller-driven. */
    public void warm() { mapping.load(); }
    public String imageIdentity() { return imageIdentity; }
    public int size() { return offsets.length; }
    public int mappedBytes() { return mapping.limit(); }

    private ByteBuffer recordBytes(int index) {
        Objects.checkIndex(index, offsets.length);
        int begin = payloadOffset + offsets[index] * 2;
        return mapping.asReadOnlyBuffer().slice(begin, lengths[index] * 2);
    }
    /** Copies and checks a record before it can become immutable local backing. */
    public LocalM3StringPiece copyRecord(int index, LocalM3Arena arena) throws IOException {
        Objects.requireNonNull(arena);
        ByteBuffer record = recordBytes(index);
        byte[] owned = new byte[record.remaining()]; record.get(owned);
        byte[] actual = digest(ByteBuffer.wrap(owned));
        int difference = 0;
        for (int i = 0; i < 32; i++) difference |= recordDigests[index * 32 + i] ^ actual[i];
        if (difference != 0)
            throw new IOException("mapped record changed");
        return arena.owned(owned, LocalM3StringPiece.Encoding.UTF16_LE);
    }

    /** Writes a new image only; never truncates or overwrites a mapped file. */
    public static void create(Path target, List<String> words) throws IOException {
        if (words.size() > MAX_RECORDS) throw new IOException("too many records");
        List<String> snapshot = List.copyOf(words);
        if (snapshot.size() > MAX_RECORDS) throw new IOException("too many records");
        long units = 0;
        for (String word : snapshot) units = Math.addExact(units, word.length());
        long payload = HEADER + 8L * snapshot.size(), size = payload + 2L * units;
        if (size > MAX_IMAGE_BYTES) throw new IOException("image too large");
        ByteBuffer bytes = ByteBuffer.allocate((int)size).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(MAGIC).putInt(1).putInt(snapshot.size()).putLong(payload).putLong(units);
        int offset = 0;
        for (int i = 0; i < snapshot.size(); i++) {
            String word = snapshot.get(i);
            bytes.putInt(HEADER + i * 8, offset).putInt(HEADER + i * 8 + 4, word.length());
            for (int j = 0; j < word.length(); j++) {
                char c = word.charAt(j); int at = (int)payload + (offset + j) * 2;
                bytes.put(at, (byte)c).put(at + 1, (byte)(c >>> 8));
            }
            offset += word.length();
        }
        bytes.put(32, digestImage(bytes));bytes.position(0);
        try (FileChannel out = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            while (bytes.hasRemaining()) out.write(bytes);
            out.force(true);
        }
    }
    // Identity includes dimensions/version, not just bytes whose interpretation depends on them.
    private static byte[] digestImage(ByteBuffer bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes.asReadOnlyBuffer().slice(0, 32));
            digest.update(bytes.asReadOnlyBuffer().slice(HEADER, bytes.limit() - HEADER));
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static byte[] digest(ByteBuffer bytes) {
        try { MessageDigest digest = MessageDigest.getInstance("SHA-256");digest.update(bytes);return digest.digest(); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
