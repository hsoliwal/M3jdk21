// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Packed immutable lexical coordinate.
 *
 * <pre>
 * 63       56 55          43 42                               0
 * +----------+--------------+----------------------------------+
 * | version  | language id  | lexical rank                     |
 * +----------+--------------+----------------------------------+
 *     8 bits      13 bits                 43 bits
 * </pre>
 */
public record LexicalRef(long bits)
        implements Comparable<LexicalRef>, MIndexSymbolReference {
    public static final int VERSION_BITS = 8;
    public static final int LANGUAGE_BITS = 13;
    public static final int RANK_BITS = 43;
    public static final int MAX_VERSION = (1 << VERSION_BITS) - 1;
    public static final int MAX_LANGUAGE_ID = (1 << LANGUAGE_BITS) - 1;
    public static final long MAX_RANK = (1L << RANK_BITS) - 1L;

    private static final int LANGUAGE_SHIFT = RANK_BITS;
    private static final int VERSION_SHIFT = RANK_BITS + LANGUAGE_BITS;
    private static final long RANK_MASK = MAX_RANK;
    private static final long LANGUAGE_MASK = ((1L << LANGUAGE_BITS) - 1L) << LANGUAGE_SHIFT;

    public static LexicalRef of(int dictionaryVersion, int languageId, long lexicalRank) {
        if (dictionaryVersion < 0 || dictionaryVersion > MAX_VERSION) {
            throw new IllegalArgumentException("dictionaryVersion must be 0.." + MAX_VERSION);
        }
        if (languageId < 0 || languageId > MAX_LANGUAGE_ID) {
            throw new IllegalArgumentException("languageId must be 0.." + MAX_LANGUAGE_ID);
        }
        if (lexicalRank < 0 || lexicalRank > MAX_RANK) {
            throw new IllegalArgumentException("lexicalRank must be 0.." + MAX_RANK);
        }
        return new LexicalRef(
                ((long) dictionaryVersion << VERSION_SHIFT)
                        | ((long) languageId << LANGUAGE_SHIFT)
                        | lexicalRank);
    }

    public int dictionaryVersion() {
        return (int) ((bits >>> VERSION_SHIFT) & 0xffL);
    }

    public int languageId() {
        return (int) ((bits >>> LANGUAGE_SHIFT) & ((1L << LANGUAGE_BITS) - 1L));
    }

    public long lexicalRank() {
        return bits & RANK_MASK;
    }

    @Override
    public MIndexSymbolRef symbolRef() {
        long scope =
                ((long) dictionaryVersion() << LANGUAGE_BITS)
                        | Integer.toUnsignedLong(languageId());
        return new MIndexSymbolRef(MIndexReferenceDomain.LEXICAL, scope, lexicalRank());
    }

    public boolean sameLanguage(LexicalRef other) {
        return other != null && ((bits ^ other.bits) & LANGUAGE_MASK) == 0L;
    }

    public boolean sameDictionary(LexicalRef other) {
        return other != null
                && dictionaryVersion() == other.dictionaryVersion()
                && languageId() == other.languageId();
    }

    @Override
    public int compareTo(LexicalRef other) {
        return Long.compareUnsigned(bits, other.bits);
    }
}
