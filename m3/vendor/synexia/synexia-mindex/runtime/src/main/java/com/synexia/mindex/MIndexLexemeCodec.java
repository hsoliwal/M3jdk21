// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable UTF-16-code-unit alphabet for the bounded packed-lexeme fast path.
 *
 * <p>Symbol zero is reserved for end/padding, so one codec may contain at most 255 distinct
 * UTF-16 code units. A lexeme is packable when it contains at most
 * {@link MIndexLexemeKey128#MAX_SYMBOLS} code units and every unit belongs to this alphabet.</p>
 *
 * <p>The forward unit-to-symbol map is a two-level sparse/direct table: the high UTF-16 byte
 * selects an allocated 256-cell page and the low byte addresses that page. Typical Latin or
 * single-script lexicons therefore keep O(1) direct addressing without retaining a 65,536-byte
 * table per codec.</p>
 *
 * <p>The codec is deliberately lexicon-scoped metadata. The resulting 128-bit coordinate is exact
 * only when interpreted with the same codec fingerprint.</p>
 */
public final class MIndexLexemeCodec {
    public static final int MAX_ALPHABET_SIZE = 255;

    private static final int PAGE_BITS = 8;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;
    private static final int PAGE_COUNT = 1 << (Character.SIZE - PAGE_BITS);
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final MIndexLexemeCodec ASCII = asciiCodec();

    private final String name;
    private final char[] pageBasePlusOne;
    private final byte[] symbolPages;
    private final char[] unitBySymbol;
    private final int alphabetSize;
    private final int directPageCount;
    private final long fingerprint64;

    private MIndexLexemeCodec(String name, char[] alphabet) {
        this.name = requireName(name);
        Objects.requireNonNull(alphabet, "alphabet");
        if (alphabet.length > MAX_ALPHABET_SIZE) {
            throw new IllegalArgumentException("alphabet exceeds 255 symbols");
        }

        boolean[] seenUnits = new boolean[Character.MAX_VALUE + 1];
        boolean[] usedPages = new boolean[PAGE_COUNT];
        for (char unit : alphabet) {
            if (seenUnits[unit]) {
                throw new IllegalArgumentException(
                        "duplicate UTF-16 unit U+" + String.format("%04X", (int) unit));
            }
            seenUnits[unit] = true;
            usedPages[unit >>> PAGE_BITS] = true;
        }

        pageBasePlusOne = new char[PAGE_COUNT];
        int pages = 0;
        for (int page = 0; page < usedPages.length; page++) {
            if (!usedPages[page]) continue;
            int base = Math.multiplyExact(pages++, PAGE_SIZE);
            int encodedBase = Math.addExact(base, 1);
            if (encodedBase > Character.MAX_VALUE) {
                throw new IllegalArgumentException("too many direct symbol pages");
            }
            pageBasePlusOne[page] = (char) encodedBase;
        }
        directPageCount = pages;
        symbolPages = new byte[Math.multiplyExact(directPageCount, PAGE_SIZE)];
        unitBySymbol = new char[MAX_ALPHABET_SIZE + 1];

        for (int index = 0; index < alphabet.length; index++) {
            char unit = alphabet[index];
            int symbol = index + 1;
            int base = pageBasePlusOne[unit >>> PAGE_BITS] - 1;
            symbolPages[base + (unit & PAGE_MASK)] = (byte) symbol;
            unitBySymbol[symbol] = unit;
        }

        alphabetSize = alphabet.length;
        fingerprint64 = fingerprint(alphabet);
    }

    /** ASCII code units U+0000..U+007F mapped to symbols 1..128. */
    public static MIndexLexemeCodec ascii() {
        return ASCII;
    }

    /**
     * Derive one lexicon-scoped alphabet from bounded candidate values.
     *
     * <p>When at most 255 distinct UTF-16 units occur, every candidate unit is retained in natural
     * code-unit order. For wider alphabets the 255 most frequent units are selected, with code-unit
     * order as the deterministic tie breaker, then sorted before symbol assignment. Strings
     * containing an unselected unit simply use the universal fallback path.</p>
     */
    public static MIndexLexemeCodec derive(
            String name, Iterable<? extends CharSequence> values) {
        Objects.requireNonNull(values, "values");
        int[] frequency = new int[Character.MAX_VALUE + 1];
        int distinct = 0;
        for (CharSequence value : values) {
            Objects.requireNonNull(value, "value");
            if (value.length() > MIndexLexemeKey128.MAX_SYMBOLS) continue;
            for (int index = 0; index < value.length(); index++) {
                int unit = value.charAt(index);
                if (frequency[unit] == 0) distinct++;
                if (frequency[unit] != Integer.MAX_VALUE) frequency[unit]++;
            }
        }

        if (distinct <= MAX_ALPHABET_SIZE) {
            char[] alphabet = new char[distinct];
            int at = 0;
            for (int unit = 0; unit < frequency.length; unit++) {
                if (frequency[unit] != 0) alphabet[at++] = (char) unit;
            }
            return new MIndexLexemeCodec(name, alphabet);
        }

        long[] ranked = new long[distinct];
        int rankedAt = 0;
        for (int unit = 0; unit < frequency.length; unit++) {
            if (frequency[unit] == 0) continue;
            long descendingFrequency =
                    Integer.toUnsignedLong(Integer.MAX_VALUE - frequency[unit]);
            ranked[rankedAt++] =
                    (descendingFrequency << 32) | Integer.toUnsignedLong(unit);
        }
        Arrays.sort(ranked);
        char[] alphabet = new char[MAX_ALPHABET_SIZE];
        for (int index = 0; index < alphabet.length; index++) {
            alphabet[index] = (char) ranked[index];
        }
        Arrays.sort(alphabet);
        return new MIndexLexemeCodec(name, alphabet);
    }

    /**
     * Create a deterministic codec. Alphabet order is part of the persisted coordinate contract.
     */
    public static MIndexLexemeCodec of(String name, char... alphabet) {
        return new MIndexLexemeCodec(
                name, Objects.requireNonNull(alphabet, "alphabet").clone());
    }

    public String name() {
        return name;
    }

    public int alphabetSize() {
        return alphabetSize;
    }

    /** Number of 256-code-unit forward-addressing pages retained by this codec. */
    public int directPageCount() {
        return directPageCount;
    }

    public long fingerprint64() {
        return fingerprint64;
    }

    /** Returns symbol 1..255, or zero when the code unit is outside this codec. */
    public int symbol(char unit) {
        int encodedBase = pageBasePlusOne[unit >>> PAGE_BITS];
        if (encodedBase == 0) return 0;
        return Byte.toUnsignedInt(
                symbolPages[encodedBase - 1 + (unit & PAGE_MASK)]);
    }

    /** Reverse the codec symbol. Symbol zero is the reserved terminator/padding value. */
    public char unit(int symbol) {
        if (symbol < 1 || symbol > alphabetSize) {
            throw new IllegalArgumentException("invalid lexeme symbol " + symbol);
        }
        return unitBySymbol[symbol];
    }

    public boolean canEncode(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (value.length() > MIndexLexemeKey128.MAX_SYMBOLS) return false;
        for (int index = 0; index < value.length(); index++) {
            if (symbol(value.charAt(index)) == 0) return false;
        }
        return true;
    }

    public MIndexLexemeKey128 encode(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (!canEncode(value)) {
            throw new IllegalArgumentException(
                    "lexeme is outside codec '" + name + "' or exceeds "
                            + MIndexLexemeKey128.MAX_SYMBOLS + " UTF-16 units");
        }
        return new MIndexLexemeKey128(packHigh(value), packLow(value));
    }

    /** Primitive payload retained by this codec, excluding ordinary object headers. */
    public long primitivePayloadBytes() {
        return Character.BYTES * (long) pageBasePlusOne.length
                + symbolPages.length
                + Character.BYTES * (long) unitBySymbol.length;
    }

    char[] alphabetCopy() {
        return Arrays.copyOfRange(unitBySymbol, 1, alphabetSize + 1);
    }

    long packHigh(CharSequence value) {
        int limit = Math.min(8, value.length());
        long high = 0L;
        for (int index = 0; index < limit; index++) {
            int symbol = symbol(value.charAt(index));
            if (symbol == 0) {
                throw new IllegalArgumentException("unencodable UTF-16 unit");
            }
            high |= (long) symbol << (56 - index * 8);
        }
        return high;
    }

    long packLow(CharSequence value) {
        long low = 0L;
        for (int index = 8; index < value.length(); index++) {
            int symbol = symbol(value.charAt(index));
            if (symbol == 0) {
                throw new IllegalArgumentException("unencodable UTF-16 unit");
            }
            low |= (long) symbol << (56 - (index - 8) * 8);
        }
        return low;
    }

    private static MIndexLexemeCodec asciiCodec() {
        char[] alphabet = new char[128];
        for (int index = 0; index < alphabet.length; index++) {
            alphabet[index] = (char) index;
        }
        return new MIndexLexemeCodec("ascii", alphabet);
    }

    private static String requireName(String value) {
        Objects.requireNonNull(value, "name");
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("codec name must not be blank");
        }
        return stripped;
    }

    private static long fingerprint(char[] alphabet) {
        long hash = FNV_OFFSET;
        hash ^= alphabet.length;
        hash *= FNV_PRIME;
        for (char unit : alphabet) {
            hash ^= unit & 0xff;
            hash *= FNV_PRIME;
            hash ^= unit >>> 8;
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
