/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;
import java.util.Objects;

/**
 * Canonical immediate owner: packed short text in a fixed-size, parent-owned {@code long[]} page.
 *
 * <p>No native text payload or byte reservation exists for an immediate owner; its word
 * resides in a fixed 32-slot array page. Two profiles are admitted, chosen from the content in a
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
//FLLDCIM: IOP - pattern=M3StringOwnerKinds role=ImmediateOwner contract=M3StringOwner(charAt,getChars,computeFacts,computeRangeFacts)
//FLLDCIM: HPCPOP - hot path; pack/unit/charAt allocate nothing
//FLLDCIM: E14 - no magic numbers: every width, mask and multiplier below is a named constant
=======
//FLLDCIM: IOP \u2014 pattern=M3StringOwnerKinds role=ImmediateOwner contract=M3StringOwner(charAt,getChars,computeFacts,computeRangeFacts)
//FLLDCIM: HPCPOP \u2014 hot path; pack/unit/charAt allocate nothing
//FLLDCIM: E14 \u2014 no magic numbers: every width, mask and multiplier below is a named constant
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
final class M3StringImmediate extends M3StringOwner {
    static final int TAG_BITS = 3;
    static final int TAG_SHIFT = Long.SIZE - TAG_BITS;
    static final long TAG_MASK = ((1L << TAG_BITS) - 1L) << TAG_SHIFT;
    static final long TAG_LATIN1 = 1L << TAG_SHIFT;
    static final long TAG_IDENT = 2L << TAG_SHIFT;
    static final long PAYLOAD_MASK = (1L << TAG_SHIFT) - 1L;
    /** Never a packed value: every admitted value carries a non-zero tag. */
    static final long NONE = 0L;

    static final int LATIN1_MAX = 7;
    static final int LATIN1_LENGTH_SHIFT = LATIN1_MAX * Byte.SIZE;
    static final int LATIN1_LENGTH_BITS = 3;
    static final long LATIN1_LENGTH_MASK = (1L << LATIN1_LENGTH_BITS) - 1L;
    static final int LATIN1_UNIT_MASK = 0xff;

    static final int IDENT_MAX = 10;
    static final int DIGIT_BITS = 6;
    static final long DIGIT_MASK = (1L << DIGIT_BITS) - 1L;
    /** Units below this bound are looked up in the identifier alphabet (ASCII). */
    private static final int IDENT_CODE_LIMIT = 128;
    private static final byte NO_CODE = -1;
    private static final String IDENT_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$";
    private static final char[] IDENT_CHARS = IDENT_ALPHABET.toCharArray();
    private static final byte[] IDENT_CODE = identCodes();

    /** {@link String#hashCode()} polynomial multiplier. */
    private static final int JAVA_HASH_MULTIPLIER = 31;
    /** splitmix64 finalizer (structural hash only; never content identity). */
    private static final long MIX_GAMMA = 0x9e3779b97f4a7c15L;
    private static final long MIX_MULTIPLIER_1 = 0xbf58476d1ce4e5b9L;
    private static final long MIX_MULTIPLIER_2 = 0x94d049bb133111ebL;
    private static final int MIX_SHIFT_1 = 30;
    private static final int MIX_SHIFT_2 = 27;
    private static final int MIX_SHIFT_3 = 31;

    private static final int KIND_LATIN1_BYTES = 0;
    private static final int KIND_UTF16_BYTES = 1;
    private static final int KIND_CHARS = 2;
    private static final int KIND_CODE_POINTS = 3;
    /** A code point that no profile admits (any supplementary point maps here). */
    private static final int REFUSED_UNIT = Integer.MAX_VALUE;

    /*
     * Every word is stored in an indexed slot of a fixed-size parent-owned long[] page.
     * The owner is a stable handle to a slot, not a second scalar payload.
     * Published slot words cannot change while an owner remains reachable.
     */
    private static final int INTERN_BUCKET_BITS = 12;
    private static final int INTERN_BUCKET_COUNT = 1 << INTERN_BUCKET_BITS;
    private static final int INTERN_PAGE_WORDS = 32;
    private static final int INTERN_LOCK_COUNT = 64;
    private static final Object[] INTERN_LOCKS = makeInternLocks();
    private static final PackedPage[] INTERN_BUCKETS = new PackedPage[INTERN_BUCKET_COUNT];

    final long[] words;
    final int slot;

    private M3StringImmediate(long[] words, int slot, long packed, int length, int javaHash) {
        super(IMMEDIATE, length, String.LATIN1, javaHash, mix64(packed));
        this.words = words;
        this.slot = slot;
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    private long packed() {
        return words[slot];
    }

    private static Object[] makeInternLocks() {
        Object[] locks = new Object[INTERN_LOCK_COUNT];
        for (int index = 0; index < locks.length; index++) locks[index] = new Object();
        return locks;
    }

    /** Fixed-capacity, weakly indexed interned words; only a new page adds physical capacity. */
    private static final class PackedPage {
        final long[] words = new long[INTERN_PAGE_WORDS];
        final java.lang.ref.WeakReference<?>[] owners =
                new java.lang.ref.WeakReference<?>[INTERN_PAGE_WORDS];
        final PackedPage next;
        int used;

        PackedPage(PackedPage next) {
            this.next = next;
        }
    }

    /**
     * Intern a validated packed value by its exact word.
     *
     * <p>The directory is a fixed array of buckets. Each bucket contains linked, fixed-size
     * pages, never resizable arrays. Weak owners permit a slot to be reused after its last
     * M3String owner becomes unreachable. A reachable owner always retains its word array;
     * no stale native address is cached or returned.</p>
     */
    //FLLDCIM: ATOM - fixed-page intern: owner reuse, no copied whole-text payload
=======
    /** The owner for an admitted packed value; {@code packed} must come from a pack method. */
    //FLLDCIM: ATOM \u2014 of: packed -> owner (length + String.hashCode parity)
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
    static M3StringImmediate of(long packed) {
        int bucket = (int) mix64(packed) & (INTERN_BUCKET_COUNT - 1);
        synchronized (INTERN_LOCKS[bucket & (INTERN_LOCK_COUNT - 1)]) {
            PackedPage available = null;
            int availableSlot = -1;
            for (PackedPage page = INTERN_BUCKETS[bucket]; page != null; page = page.next) {
                for (int index = 0; index < page.used; index++) {
                    java.lang.ref.WeakReference<?> reference = page.owners[index];
                    Object candidate = reference == null ? null : reference.get();
                    if (candidate instanceof M3StringImmediate live) {
                        if (page.words[index] == packed) return live;
                    } else if (available == null) {
                        available = page;
                        availableSlot = index;
                    }
                }
                if (available == null && page.used < INTERN_PAGE_WORDS) {
                    available = page;
                    availableSlot = page.used;
                }
            }

            if (available == null) {
                available = new PackedPage(INTERN_BUCKETS[bucket]);
                INTERN_BUCKETS[bucket] = available;
                availableSlot = 0;
            }
            if (availableSlot == available.used) available.used++;
            available.words[availableSlot] = packed;
            M3StringImmediate created = createOwner(available.words, availableSlot, packed);
            available.owners[availableSlot] = new java.lang.ref.WeakReference<>(created);
            return created;
        }
    }

    /** Exact per-text String.hashCode facts, created only on a new canonical slot. */
    private static M3StringImmediate createOwner(long[] words, int slot, long packed) {
        int length = length(packed);
        int hash = 0;
        if (isLatin1(packed)) {
            for (int index = 0; index < length; index++) {
                hash = JAVA_HASH_MULTIPLIER * hash + latin1Unit(packed, index);
            }
        } else {
            long digits = packed & PAYLOAD_MASK;
            for (int index = 0; index < length; index++) {
                hash = JAVA_HASH_MULTIPLIER * hash + digitChar(digits);
                digits = nextDigits(digits);
            }
        }
        return new M3StringImmediate(words, slot, packed, length, hash);
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    //FLLDCIM: ATOM - pack: four source kinds, one canonical packed value or NONE
=======
    //FLLDCIM: ATOM \u2014 pack: four source kinds, one canonical packed value or NONE
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    //FLLDCIM: ATOM - length: LATIN1 field, IDENT digit count
=======
    //FLLDCIM: ATOM \u2014 length: LATIN1 field, IDENT digit count
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
    static int length(long packed) {
        long tag = packed & TAG_MASK;
        if (tag == TAG_LATIN1) {
            return (int) ((packed >>> LATIN1_LENGTH_SHIFT) & LATIN1_LENGTH_MASK);
        }
        if (tag != TAG_IDENT) {
            throw new IllegalArgumentException("not an M3 immediate String value");
        }
        int length = 0;
        for (long digits = packed & PAYLOAD_MASK; digits != 0L; digits = nextDigits(digits)) {
            length++;
        }
        return length;
    }

    /** Unit {@code index} of an admitted packed value; the bounds are the caller's. */
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    //FLLDCIM: ATOM - unit: O(1) LATIN1 shift, O(index) IDENT digit walk (index below IDENT_MAX)
=======
    //FLLDCIM: ATOM \u2014 unit: O(1) LATIN1 shift, O(index) IDENT digit walk (index below IDENT_MAX)
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
    static char unit(long packed, int index) {
        if (isLatin1(packed)) {
            return latin1Unit(packed, index);
        }
        long digits = packed & PAYLOAD_MASK;
        for (int skip = 0; skip < index; skip++) {
            digits = nextDigits(digits);
        }
        return digitChar(digits);
    }

    /** Equal text of two whole immediate owners is equal packed values (canonical profile). */
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    //FLLDCIM: ATOM - sameText: identity of text = equality of packed words
=======
    //FLLDCIM: ATOM \u2014 sameText: identity of text = equality of packed words
>>>>>>> a8720d7d5c631cb7f24f116b96dab9d9b76467a6
    static boolean sameText(M3StringOwner left, M3StringOwner right) {
        return left instanceof M3StringImmediate a
                && right instanceof M3StringImmediate b
                && a.packed() == b.packed();
    }

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        return unit(packed(), index);
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        long packed = packed();
        if (isLatin1(packed)) {
            for (int source = start, target = destinationStart; source < end; source++, target++) {
                destination[target] = latin1Unit(packed, source);
            }
            return;
        }
        long digits = packed & PAYLOAD_MASK;
        for (int skip = 0; skip < start; skip++) {
            digits = nextDigits(digits);
        }
        for (int target = destinationStart, source = start; source < end; source++, target++) {
            destination[target] = digitChar(digits);
            digits = nextDigits(digits);
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

    private static boolean isLatin1(long packed) {
        return (packed & TAG_MASK) == TAG_LATIN1;
    }

    private static char latin1Unit(long packed, int index) {
        return (char) ((packed >>> (index * Byte.SIZE)) & LATIN1_UNIT_MASK);
    }

    /** Lowest bijective base-64 digit as its alphabet character. */
    private static char digitChar(long digits) {
        return IDENT_CHARS[(int) ((digits - 1L) & DIGIT_MASK)];
    }

    /** Drops the lowest bijective base-64 digit. */
    private static long nextDigits(long digits) {
        return (digits - 1L) >>> DIGIT_BITS;
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
            if (unit > LATIN1_UNIT_MASK) return NONE;
            if (latin1) {
                bytes |= (long) unit << (index * Byte.SIZE);
            } else {
                int code = unit < IDENT_CODE_LIMIT ? IDENT_CODE[unit] : NO_CODE;
                if (code == NO_CODE) return NONE;
                digits += (long) (code + 1) << (DIGIT_BITS * index);
            }
        }
        if (latin1) {
            return TAG_LATIN1 | ((long) length << LATIN1_LENGTH_SHIFT) | bytes;
        }
        return TAG_IDENT | digits;
    }

    private static int unitOf(int kind, Object source, int at) {
        return switch (kind) {
            case KIND_LATIN1_BYTES -> ((byte[]) source)[at] & LATIN1_UNIT_MASK;
            case KIND_UTF16_BYTES -> StringUTF16.charAt((byte[]) source, at);
            case KIND_CHARS -> ((char[]) source)[at];
            default -> {
                int codePoint = ((int[]) source)[at];
                yield codePoint <= Character.MAX_VALUE ? codePoint : REFUSED_UNIT;
            }
        };
    }

    private static byte[] identCodes() {
        byte[] codes = new byte[IDENT_CODE_LIMIT];
        Arrays.fill(codes, NO_CODE);
        for (int index = 0; index < IDENT_CHARS.length; index++) {
            codes[IDENT_CHARS[index]] = (byte) index;
        }
        return codes;
    }

    private static long mix64(long value) {
        long mixed = value ^ MIX_GAMMA;
        mixed = (mixed ^ (mixed >>> MIX_SHIFT_1)) * MIX_MULTIPLIER_1;
        mixed = (mixed ^ (mixed >>> MIX_SHIFT_2)) * MIX_MULTIPLIER_2;
        return mixed ^ (mixed >>> MIX_SHIFT_3);
    }
}
