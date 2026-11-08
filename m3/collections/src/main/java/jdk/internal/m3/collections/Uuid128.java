// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

/**
 * UUID value as the actual 128 bits: two primitive longs and no retained String.
 *
 * <p>Unsigned comparison matches lexicographic ordering of the standard lowercase UUID text and
 * of the canonical 16-byte big-endian representation. This intentionally differs from
 * {@link UUID#compareTo(UUID)}, which compares signed long halves.</p>
 */
//FLLDCIM: RELOCATE - donor blob bb77b6d5b7a8 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public record Uuid128(long mostSignificantBits, long leastSignificantBits)
        implements Comparable<Uuid128> {
    public static final int BINARY_BYTES = 16;
    public static final int TEXT_CHARS = 36;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static Uuid128 fromUuid(UUID value) {
        Objects.requireNonNull(value, "value");
        return new Uuid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
    }

    public UUID toUuid() {
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /** Parse the canonical 8-4-4-4-12 form without substring or intermediate integer objects. */
    public static Uuid128 parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        if (text.length() != TEXT_CHARS
                || text.charAt(8) != '-'
                || text.charAt(13) != '-'
                || text.charAt(18) != '-'
                || text.charAt(23) != '-') {
            throw new IllegalArgumentException("UUID must use canonical 8-4-4-4-12 form");
        }

        long msb = 0L;
        long lsb = 0L;
        int nibble = 0;
        for (int i = 0; i < TEXT_CHARS; i++) {
            char ch = text.charAt(i);
            if (ch == '-') continue;
            int value = hex(ch);
            if (nibble < 16) {
                msb = (msb << 4) | value;
            } else {
                lsb = (lsb << 4) | value;
            }
            nibble++;
        }
        if (nibble != 32) throw new IllegalArgumentException("UUID must contain 32 hex digits");
        return new Uuid128(msb, lsb);
    }

    public static Uuid128 fromBigEndian(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return fromBigEndian(bytes, 0);
    }

    public static Uuid128 fromBigEndian(byte[] bytes, int offset) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || offset + BINARY_BYTES > bytes.length) {
            throw new IndexOutOfBoundsException("UUID requires 16 bytes");
        }
        return new Uuid128(readLong(bytes, offset), readLong(bytes, offset + Long.BYTES));
    }

    public static Uuid128 fromBigEndian(ByteBuffer source) {
        Objects.requireNonNull(source, "source");
        if (source.remaining() < BINARY_BYTES) throw new IllegalArgumentException("UUID requires 16 bytes");
        long high = 0L;
        long low = 0L;
        for (int i = 0; i < Long.BYTES; i++) high = (high << 8) | (source.get() & 0xffL);
        for (int i = 0; i < Long.BYTES; i++) low = (low << 8) | (source.get() & 0xffL);
        return new Uuid128(high, low);
    }

    public byte[] toBigEndianBytes() {
        byte[] out = new byte[BINARY_BYTES];
        writeBigEndian(out, 0);
        return out;
    }

    public void writeBigEndian(byte[] target, int offset) {
        Objects.requireNonNull(target, "target");
        if (offset < 0 || offset + BINARY_BYTES > target.length) {
            throw new IndexOutOfBoundsException("target does not have 16 UUID bytes");
        }
        writeLong(target, offset, mostSignificantBits);
        writeLong(target, offset + Long.BYTES, leastSignificantBits);
    }

    public void writeBigEndian(ByteBuffer target) {
        Objects.requireNonNull(target, "target");
        if (target.remaining() < BINARY_BYTES) throw new IllegalArgumentException("target requires 16 bytes");
        writeLong(target, mostSignificantBits);
        writeLong(target, leastSignificantBits);
    }

    public int version() {
        return (int) ((mostSignificantBits >>> 12) & 0x0fL);
    }

    public int variant() {
        long value = leastSignificantBits;
        if ((value & 0x8000_0000_0000_0000L) == 0L) return 0;
        if ((value & 0x4000_0000_0000_0000L) != 0L) {
            return (int) ((value >>> 61) & 0x07L);
        }
        return 2;
    }

    /**
     * Canonical textual boundary. Internal code should prefer the two longs or 16 binary bytes.
     */
    public String toCanonicalString() {
        char[] out = new char[TEXT_CHARS];
        int hexIndex = 0;
        for (int i = 0; i < out.length; i++) {
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                out[i] = '-';
            } else {
                int shift = hexIndex < 16 ? (15 - hexIndex) << 2 : (31 - hexIndex) << 2;
                long word = hexIndex < 16 ? mostSignificantBits : leastSignificantBits;
                out[i] = HEX[(int) ((word >>> shift) & 0x0fL)];
                hexIndex++;
            }
        }
        return new String(out);
    }

    @Override
    public String toString() {
        return toCanonicalString();
    }

    @Override
    public int compareTo(Uuid128 other) {
        Objects.requireNonNull(other, "other");
        int high = Long.compareUnsigned(mostSignificantBits, other.mostSignificantBits);
        return high != 0 ? high : Long.compareUnsigned(leastSignificantBits, other.leastSignificantBits);
    }

    private static int hex(char ch) {
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'a' && ch <= 'f') return ch - 'a' + 10;
        if (ch >= 'A' && ch <= 'F') return ch - 'A' + 10;
        throw new IllegalArgumentException("invalid UUID hex character: " + ch);
    }

    private static long readLong(byte[] bytes, int offset) {
        long value = 0L;
        for (int i = 0; i < Long.BYTES; i++) value = (value << 8) | (bytes[offset + i] & 0xffL);
        return value;
    }

    private static void writeLong(byte[] bytes, int offset, long value) {
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            bytes[offset + i] = (byte) value;
            value >>>= 8;
        }
    }

    private static void writeLong(ByteBuffer target, long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            target.put((byte) (value >>> shift));
        }
    }
}
