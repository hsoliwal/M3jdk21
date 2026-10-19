/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;
import java.util.Objects;

/**
 * Canonical immediate owner: the whole text of a short String carried in one {@code long}.
 *
 * <p>No native payload, no pool bucket and no byte reservation exist for an immediate owner; the
 * packed value is the text. Two profiles are admitted. The profile is chosen from the content in a
 * fixed order (LATIN1 first, then IDENT), so the packed value is a canonical function of the text:
 * two whole immediate owners hold equal text if and only if their packed values are equal.</p>
 *
 * <pre>
 *   bits 63..61  profile tag: 1 = LATIN1, 2 = IDENT (0 is never admitted)
 *   LATIN1  1..7 units, each &lt;= 0xFF
 *           bits 58..56 length; unit i at bits 8i..8i+7; bits 60..59 zero
 *   IDENT   8..10 units of [0-9A-Za-z_$] (shorter text is always LATIN1 or refused)
 *           bijective base-64 over bits 60..0, least significant digit first,
 *           digit = 1 + index in "0-9 A-Z a-z _ $"; value 0 is the empty string
 * </pre>
 *
 * <p>Coverage measured on the M3jdk21 java.lang and java.util sources (master 677437be):
 * LATIN1 x7 holds 42.1% of string-literal occurrences and 85.7% of identifier-word occurrences;
 * adding IDENT x10 raises identifier words to 97.7%. The packing technique (allocation-free
 * short text in primitive words behind explicit domain gates) follows tools4j shortstring
 * (MIT, hsoliwal/shortstring @383d2d17). Its 36-symbol codec spends the whole 64-bit space on
 * signed values and cannot share a word with an owner tag, so no tools4j code is used here.</p>
 *
 * <p>HotSpot decodes the same layout in {@code java_lang_M3String::immediate_char_at}
 * (javaClasses.cpp); both sides change together.</p>
 */
//FLLDCIM: HPCPOP — hot path; pack/unit/charAt allocate nothing
//FLLDCIM: IOP — third canonical owner kind beside atom and tuple; M3String stays owner + coordinate
final class M3StringImmediate extends M3StringOwner {
    static final int TAG_SHIFT = 61;
    static final long TAG_MASK = 7L << TAG_SHIFT;
    static final long TAG_LATIN1 = 1L << TAG_SHIFT;
    static final long TAG_IDENT = 2L << TAG_SHIFT;
    static final long PAYLOAD_MASK = (1L << TAG_SHIFT) - 1L;
    static final int LATIN1_MAX = 7;
    static final int LATIN1_LENGTH_SHIFT = 56;
    static final int IDENT_MAX = 10;
    /** Never a packed value: every admitted value carries a non-zero tag. */
    static final long NONE = 0L;

    private static final int KIND_LATIN1_BYTES = 0;
    private static final int KIND_UTF16_BYTES = 1;
    private static final int KIND_CHARS = 2;
    private static final int KIND_CODE_POINTS = 3;

    private static final char[] IDENT_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$".toCharArray();
    private static final byte[] IDENT_CODE = identCodes();

    final long packed;

    private M3StringImmediate(long packed, int length, int javaHash) {
        super(IMMEDIATE, length, String.LATIN1, javaHash, mix64(packed));
        this.packed = packed;
    }

    /** The owner for an admitted packed value; {@code packed} must come from a pack method. */
    static M3StringImmediate of(long packed) {
        int length = length(packed);
        int hash = 0;
        if ((packed & TAG_MASK) == TAG_LATIN1) {
            for (int index = 0; index < length; index++) {
                hash = 31 * hash + (int) ((packed >>> (index << 3)) & 0xff);
            }
        } else {
            long digits = packed & PAYLOAD_MASK;
            for (int index = 0; index < length; index++) {
                hash = 31 * hash + IDENT_CHARS[(int) ((digits - 1L) & 63L)];
                digits = (digits - 1L) >>> 6;
            }
        }
        return new M3StringImmediate(packed, length, hash);
    }

    static long packCompactBytes(byte[] source, int offset, int length, byte coder) {
        return pack(coder == String.LATIN1 ? KIND_LATIN1_BYTES : KIND_UTF16_BYTES,
                source, offset, length);
    }

    static long packChars(char[] source, int offset, int length) {
        return pack(KIND_CHARS, source, offset, length);
    }

    /** Code points map one-to-one to units here: any supplementary point refuses both profiles. */
    static long packCodePoints(int[] source, int offset, int count) {
        return pack(KIND_CODE_POINTS, source, offset, count);
    }

    /** Unit count of an admitted packed value. */
    static int length(long packed) {
        long tag = packed & TAG_MASK;
        if (tag == TAG_LATIN1) {
            return (int) ((packed >>> LATIN1_LENGTH_SHIFT) & 7L);
        }
        if (tag != TAG_IDENT) {
            throw new IllegalArgumentException("not an M3 immediate String value");
        }
        int length = 0;
        for (long digits = packed & PAYLOAD_MASK; digits != 0L; digits = (digits - 1L) >>> 6) {
            length++;
        }
        return length;
    }

    /** Unit {@code index} of an admitted packed value; the bounds are the caller's. */
    static char unit(long packed, int index) {
        if ((packed & TAG_MASK) == TAG_LATIN1) {
            return (char) ((packed >>> (index << 3)) & 0xff);
        }
        long digits = packed & PAYLOAD_MASK;
        for (int skip = 0; skip < index; skip++) {
            digits = (digits - 1L) >>> 6;
        }
        return IDENT_CHARS[(int) ((digits - 1L) & 63L)];
    }

    /** Equal text of two whole immediate owners is equal packed values (canonical profile). */
    static boolean sameText(M3StringOwner left, M3StringOwner right) {
        return left instanceof M3StringImmediate a
                && right instanceof M3StringImmediate b
                && a.packed == b.packed;
    }

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        return unit(packed, index);
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        if ((packed & TAG_MASK) == TAG_LATIN1) {
            for (int source = start, target = destinationStart; source < end; source++, target++) {
                destination[target] = (char) ((packed >>> (source << 3)) & 0xff);
            }
            return;
        }
        long digits = packed & PAYLOAD_MASK;
        for (int skip = 0; skip < start; skip++) {
            digits = (digits - 1L) >>> 6;
        }
        for (int target = destinationStart, source = start; source < end; source++, target++) {
            destination[target] = IDENT_CHARS[(int) ((digits - 1L) & 63L)];
            digits = (digits - 1L) >>> 6;
        }
    }

    @Override
    M3StringFacts computeFacts() {
        return M3StringFacts.scan(M3String.whole(this));
    }

    @Override
    M3StringFacts computeRangeFacts(int start, int length) {
        Objects.checkFromIndexSize(start, length, this.length);
        if (start == 0 && length == this.length) return facts();
        return M3StringFacts.scan(M3String.range(this, start, length));
    }

    /**
     * One forward scan decides both profiles. LATIN1 wins when eligible; IDENT is kept only for
     * lengths LATIN1 cannot hold, which makes the chosen packed value unique for the text.
     */
    private static long pack(int kind, Object source, int offset, int length) {
        if (length < 1 || length > IDENT_MAX) return NONE;
        boolean latin1 = length <= LATIN1_MAX;
        long bytes = 0L;
        long digits = 0L;
        for (int index = 0; index < length; index++) {
            int unit = unitOf(kind, source, offset + index);
            if (unit > 0xff) return NONE;
            if (latin1) {
                bytes |= (long) unit << (index << 3);
            } else {
                int code = unit < IDENT_CODE.length ? IDENT_CODE[unit] : -1;
                if (code < 0) return NONE;
                digits += (long) (code + 1) << (6 * index);
            }
        }
        if (latin1) {
            return TAG_LATIN1 | ((long) length << LATIN1_LENGTH_SHIFT) | bytes;
        }
        return TAG_IDENT | digits;
    }

    private static int unitOf(int kind, Object source, int at) {
        return switch (kind) {
            case KIND_LATIN1_BYTES -> ((byte[]) source)[at] & 0xff;
            case KIND_UTF16_BYTES -> StringUTF16.charAt((byte[]) source, at);
            case KIND_CHARS -> ((char[]) source)[at];
            default -> {
                int codePoint = ((int[]) source)[at];
                yield codePoint <= 0xffff ? codePoint : Integer.MAX_VALUE;
            }
        };
    }

    private static byte[] identCodes() {
        byte[] codes = new byte[128];
        Arrays.fill(codes, (byte) -1);
        for (int index = 0; index < IDENT_CHARS.length; index++) {
            codes[IDENT_CHARS[index]] = (byte) index;
        }
        return codes;
    }

    private static long mix64(long value) {
        long mixed = value ^ 0x9e3779b97f4a7c15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }
}
