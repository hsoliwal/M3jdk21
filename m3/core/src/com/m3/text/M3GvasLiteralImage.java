/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Read-only, entry-less literal index over the existing, strictly mapped M3-GVAS owner.
 *
 * <p>Rows and UTF-16 code units live in mapped shared memory. Java never recreates a
 * per-literal String, Map.Entry, wrapper or private payload array for lookup. A hit returns
 * the native address of the canonical UTF-16 payload; zero is an ordinary miss. No entry
 * can be published before full image geometry, UTF-16 facts and ordering are validated.</p>
 */
public final class M3GvasLiteralImage {
    public static final int FLAG_ASCII = 1;
    public static final int FLAG_LATIN1 = 2;
    public static final int FLAG_SURROGATE = 4;
    private static final String MAGIC = "M3GVAS01";
    private static final int HEADER_BYTES = 64;
    private static final int ROW_BYTES = 32;
    // Optional derived metadata only; rows/payload addresses remain in the mapped image.
    private static final int LOOKUP_SLOT_LIMIT = 1 << 22;
    private static final int LOOKUP_PROBE_LIMIT = 64;
    private static final int LOOKUP_MIN_ROWS = 32;
    private final M3FixedAddressMapping.Image mapped;
    private final ByteBuffer data;
    private final int count;
    private final int payloadStart;
<<<<<<< HEAD
    /** Immutable, optional primitive row+1 hash directory; never owns spelling payload. */
    private final int[] hashSlots;
||||||| merged common ancestors
=======
    private final int[] lookupRows;
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf

    private M3GvasLiteralImage(M3FixedAddressMapping.Image mapped) {
        this.mapped = mapped;
        this.data = mapped.readOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        this.count = data.getInt(16);
        this.payloadStart = data.getInt(28);
<<<<<<< HEAD
        // open() validates the complete immutable native image before building this index.
        this.hashSlots = buildHashSlots(data, count);
||||||| merged common ancestors
=======
        this.lookupRows = prepareLookup(data, count);
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    }

    /** Admit the exact backing image only after native exact-address admission succeeded. */
    public static M3GvasLiteralImage open(M3FixedAddressMapping.Image image) {
        Objects.requireNonNull(image, "image");
        ByteBuffer b = image.readOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int capacity = b.capacity();
        if (capacity < HEADER_BYTES + 2) throw new IllegalArgumentException("truncated M3-GVAS literal header");
        for (int i = 0; i < MAGIC.length(); i++) {
            if ((char) (b.get(i) & 0xff) != MAGIC.charAt(i)) throw new IllegalArgumentException("M3-GVAS literal magic");
        }
        int count = b.getInt(16);
        int offset = b.getInt(28);
        if (b.getInt(8) != 1 || b.getInt(12) != HEADER_BYTES || count < 0
                || b.getInt(20) != ROW_BYTES || b.getInt(24) != HEADER_BYTES
                || offset < HEADER_BYTES || b.getInt(32) != capacity
                || offset != align16(HEADER_BYTES + (long) count * ROW_BYTES)
                || offset > capacity - 2 || b.getChar(offset) != 0) {
            throw new IllegalArgumentException("M3-GVAS literal geometry");
        }
        for (int i = 36; i < HEADER_BYTES; i++) {
            if (b.get(i) != 0) throw new IllegalArgumentException("M3-GVAS unexpected header flags");
        }
        for (long i = HEADER_BYTES + (long) count * ROW_BYTES; i < offset; i++) {
            if (b.get((int) i) != 0) throw new IllegalArgumentException("nonzero alignment padding");
        }
        int expected = offset + 2;
        int previous = -1;
        for (int row = 0; row < count; row++) {
            int at = HEADER_BYTES + row * ROW_BYTES;
            int begin = b.getInt(at);
            int length = b.getInt(at + 4);
            if (length < 0 || length > (capacity - expected) / Character.BYTES
                    || begin != (length == 0 ? offset : expected)) {
                throw new IllegalArgumentException("M3-GVAS overlapping/out-of-range payload");
            }
            if (previous >= 0 && compareRows(b, previous, row) >= 0) {
                throw new IllegalArgumentException("M3-GVAS literal rows not strictly sorted");
            }
            int hash = 0;
            int codePoints = 0;
            int flags = FLAG_ASCII | FLAG_LATIN1;
            char prior = 0;
            for (int i = 0; i < length; i++) {
                char c = b.getChar(begin + (i << 1));
                hash = 31 * hash + c;
                if (c > 0x7f) flags &= ~FLAG_ASCII;
                if (c > 0xff) flags &= ~FLAG_LATIN1;
                if (Character.isSurrogate(c)) flags |= FLAG_SURROGATE;
                if (i == 0 || !Character.isLowSurrogate(c) || !Character.isHighSurrogate(prior)) codePoints++;
                prior = c;
            }
            if (hash != b.getInt(at + 8) || power31(length) != b.getInt(at + 12)
                    || codePoints != b.getInt(at + 16) || flags != b.getInt(at + 20)
                    || (length == 0 ? -1 : b.getChar(begin)) != b.getInt(at + 24)
                    || (length == 0 ? -1 : b.getChar(begin + ((length - 1) << 1))) != b.getInt(at + 28)) {
                throw new IllegalArgumentException("M3-GVAS literal precompute fact mismatch");
            }
            if (length != 0) expected += length << 1;
            previous = row;
        }
        if (expected != capacity) throw new IllegalArgumentException("trailing/omitted payload bytes");
        return new M3GvasLiteralImage(image);
    }

    private static int align16(long value) {
        if (value > Integer.MAX_VALUE - 15L) throw new IllegalArgumentException("M3-GVAS capacity overflow");
        return (int) ((value + 15L) & ~15L);
    }

    private static int power31(int exponent) {
        int p = 1, b = 31;
        for (int n = exponent; n != 0; n >>>= 1) {
            if ((n & 1) != 0) p *= b;
            b *= b;
        }
        return p;
    }

    private static int compareRows(ByteBuffer b, int left, int right) {
        int a = HEADER_BYTES + left * ROW_BYTES;
        int c = HEADER_BYTES + right * ROW_BYTES;
        int lenA = b.getInt(a + 4), lenB = b.getInt(c + 4);
        int pa = b.getInt(a), pb = b.getInt(c);
        for (int i = 0, end = Math.min(lenA, lenB); i < end; i++) {
            int d = b.getChar(pa + (i << 1)) - b.getChar(pb + (i << 1));
            if (d != 0) return d;
        }
        return Integer.compare(lenA, lenB);
    }

    private int compare(String needle, int row) {
        int at = HEADER_BYTES + row * ROW_BYTES;
        int length = data.getInt(at + 4);
        int begin = data.getInt(at);
        for (int i = 0, end = Math.min(length, needle.length()); i < end; i++) {
            int diff = needle.charAt(i) - data.getChar(begin + (i << 1));
            if (diff != 0) return diff;
        }
        return Integer.compare(needle.length(), length);
    }


    /*
     * An entry-less hash directory over existing mapped UTF-16 rows.
     * Zero means empty; row+1 retains row 0 without a sentinel collision.
     * Optional acceleration is bounded by a 50% load factor, probe length and
     * heap budget. Source image layout, mapped owners and JNI ABI are unchanged.
     */
    private static int[] buildHashSlots(ByteBuffer bytes, int rows) {
        if (rows < 64 || rows > (1 << 20)) return null;
        int capacity = 1;
        while (capacity < rows * 2) capacity <<= 1;
        Runtime runtime = Runtime.getRuntime();
        long headroom = runtime.maxMemory()
                - (runtime.totalMemory() - runtime.freeMemory());
        if ((long) capacity * Integer.BYTES > headroom / 8) return null;
        int[] slots;
        try {
            slots = new int[capacity];
        } catch (OutOfMemoryError optionalIndexPressure) {
            return null;
        }
        int mask = capacity - 1;
        for (int row = 0; row < rows; row++) {
            int at = HEADER_BYTES + row * ROW_BYTES;
            int length = bytes.getInt(at + 4);
            long hash = spellingHash(bytes, bytes.getInt(at), length);
            int slot = mixSpellingHash(hash) & mask;
            int probes = 0;
            while (slots[slot] != 0) {
                if (++probes >= 64) return null;
                slot = (slot + 1) & mask;
            }
            slots[slot] = row + 1;
        }
        return slots;
    }

    private static long spellingHash(ByteBuffer bytes, int offset, int length) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < length; i++) {
            hash = (hash ^ bytes.getChar(offset + (i << 1))) * 0x100000001b3L;
        }
        return hash ^ length;
    }

    private static long spellingHash(String text) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) {
            hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
        }
        return hash ^ text.length();
    }

    private static int mixSpellingHash(long hash) {
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return (int) (hash ^ (hash >>> 33));
    }

    /** Package-private receiver-test hook, not a String or JNI API. */
    boolean hashSlotIndexPrepared() { return hashSlots != null; }

    /** Returns a zero-allocation row index, or {@code -1}; Unicode comparison is UTF-16 exact. */
    public int findRow(String spelling) {
        Objects.requireNonNull(spelling, "spelling");
<<<<<<< HEAD
        int[] slots = hashSlots;
        if (slots != null) {
            int mask = slots.length - 1;
            int slot = mixSpellingHash(spellingHash(spelling)) & mask;
            for (int probes = 0; probes < 64; probes++) {
                int encoded = slots[slot];
                if (encoded == 0) return -1;
                int row = encoded - 1;
                if (data.getInt(HEADER_BYTES + row * ROW_BYTES + 4) == spelling.length()
                        && compare(spelling, row) == 0) return row;
                slot = (slot + 1) & mask;
            }
        }
        return findRowBinary(spelling);
    }

    private int findRowBinary(String spelling) {
||||||| merged common ancestors
=======
        if (lookupRows != null) {
            int hash = spelling.hashCode();
            int length = spelling.length();
            int mask = lookupRows.length - 1;
            int slot = lookupSlot(hash, length, mask);
            for (int probe = 0; probe < LOOKUP_PROBE_LIMIT; probe++) {
                int encoded = lookupRows[slot];
                if (encoded == 0) return -1;
                int row = encoded - 1;
                int at = HEADER_BYTES + row * ROW_BYTES;
                if (data.getInt(at + 4) == length && data.getInt(at + 8) == hash
                        && compare(spelling, row) == 0) return row;
                slot = (slot + 1) & mask;
            }
        }
        return findRowBinary(spelling);
    }

    private int findRowBinary(String spelling) {
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
        int lo = 0, hi = count;
        while (lo < hi) {
            int middle = (lo + hi) >>> 1;
            int delta = compare(spelling, middle);
            if (delta == 0) return middle;
            if (delta < 0) hi = middle;
            else lo = middle + 1;
        }
        return -1;
    }

    /** Build from validated facts; refusal affects lookup cost, never image admission. */
    private static int[] prepareLookup(ByteBuffer image, int rows) {
        if (rows < LOOKUP_MIN_ROWS || rows > LOOKUP_SLOT_LIMIT / 2) return null;
        int capacity = 1;
        while (capacity < rows * 2) capacity <<= 1;
        int[] slots;
        try {
            slots = new int[capacity];
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
        int mask = capacity - 1;
        for (int row = 0; row < rows; row++) {
            int at = HEADER_BYTES + row * ROW_BYTES;
            int slot = lookupSlot(image.getInt(at + 8), image.getInt(at + 4), mask);
            int probe = 0;
            while (slots[slot] != 0 && probe < LOOKUP_PROBE_LIMIT) {
                slot = (slot + 1) & mask;
                probe++;
            }
            if (probe == LOOKUP_PROBE_LIMIT) return null;
            slots[slot] = row + 1;
        }
        return slots;
    }

    private static int lookupSlot(int hash, int length, int mask) {
        // Integer mixing only chooses a candidate. Exact UTF-16 comparison proves identity.
        long mixed = (Integer.toUnsignedLong(hash) << 32) | Integer.toUnsignedLong(length);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (int) mixed & mask;
    }

    /** Exact numeric pointer to the canonical code-unit payload, or zero when absent. */
    public long addressOf(String spelling) {
        int row = findRow(spelling);
        return row < 0 ? 0L : addressAt(row);
    }

    /** No Java heap pointer is ever returned. The mapping is retained for the process lifetime. */
    public long addressAt(int row) {
        Objects.checkIndex(row, count);
        return mapped.addressOfOffset(data.getInt(HEADER_BYTES + row * ROW_BYTES));
    }

    public int utf16LengthAt(int row) { return fact(row, 4); }
    public int hashCodeAt(int row) { return fact(row, 8); }
    public int hash31PowerAt(int row) { return fact(row, 12); }
    public int codePointCountAt(int row) { return fact(row, 16); }
    public int flagsAt(int row) { return fact(row, 20); }
    public int size() { return count; }
    public long baseAddress() { return mapped.address(); }

    private int fact(int row, int column) {
        Objects.checkIndex(row, count);
        return data.getInt(HEADER_BYTES + row * ROW_BYTES + column);
    }
}
