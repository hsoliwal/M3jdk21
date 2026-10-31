// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Canonical word id (CWID) space of the M3Lexicons String plane.
 *
 * <p>{@code java.lang.M3StringPool.Lexicon.atom(row)} gives every lexicon atom
 * {@code canonicalId = Long.MIN_VALUE | (generation64 & 0x3fff_ffff_0000_0000L) | (row + 1)}. The CWID is
 * {@code row + 1}: dense {@code 1..V} in UTF-16 order, so every per-word sidecar is a flat array indexed by
 * {@code cwid - 1}. CWID 0 means "not a word of this lexicon generation". Other id spaces (SYNDICT1,
 * MIWROWS1 IndexId) stay distinct identities; mapping between them needs a verified remap sidecar.
 * All methods are allocation-free.</p>
 */
public final class M3LexiconWordIds {
    /** Sign bit: the atom is a mapped lexicon row. */
    public static final long LEXICON_FLAG = Long.MIN_VALUE;
    /** 30 generation-tag bits taken from the image generation. */
    public static final long TAG_MASK = 0x3fff_ffff_0000_0000L;
    private static final long RESERVED_BIT = 0x4000_0000_0000_0000L;
    private static final int SYNDICT_BITS = 25;
    private static final int SYNDICT_MASK = (1 << SYNDICT_BITS) - 1;
    private static final long INDEX_LOCAL_MASK = (1L << 47) - 1;

    private M3LexiconWordIds() {
    }

    /** Encodes the atom canonical id exactly as {@code M3StringPool.Lexicon.atom} does. */
    public static long canonicalId(long generation64, int cwid) {
        if (cwid <= 0) {
            throw new IllegalArgumentException("CWID must be positive");
        }
        return LEXICON_FLAG | (generation64 & TAG_MASK) | Integer.toUnsignedLong(cwid);
    }

    /** @return the CWID when {@code canonicalId} is a row of this generation, else 0 */
    public static int wordId(long canonicalId, long generation64) {
        boolean lexicon = canonicalId < 0 && (canonicalId & RESERVED_BIT) == 0;
        boolean sameTag = (canonicalId & TAG_MASK) == (generation64 & TAG_MASK);
        long low = canonicalId & 0xffff_ffffL;
        return lexicon && sameTag && low > 0 && low <= Integer.MAX_VALUE ? (int) low : 0;
    }

    /** M3LEX001 generation: the first 8 bytes of the image SHA-256, big-endian. */
    public static long generationOfDigest(byte[] sha256) {
        if (sha256.length < 8) {
            throw new IllegalArgumentException("digest too short");
        }
        long value = 0;
        for (int index = 0; index < 8; index++) {
            value = (value << 8) | (sha256[index] & 0xffL);
        }
        return value;
    }

    /** SYNARR01 generation as the String plane derives it. */
    public static long generationOfArena(long namespaceHigh, long namespaceLow) {
        return namespaceHigh ^ namespaceLow;
    }

    /** SYNDICT1 word id {@code ((language + 1) << 25) | (word + 1)}. */
    public static int syndictId(int language, int word) {
        if (language < 0 || language >= 100 || word < 0 || word >= SYNDICT_MASK) {
            throw new IllegalArgumentException("SYNDICT1 coordinate out of range");
        }
        return ((language + 1) << SYNDICT_BITS) | (word + 1);
    }

    /** @return zero-based language index of a SYNDICT1 id */
    public static int syndictLanguage(int id) {
        return (id >>> SYNDICT_BITS) - 1;
    }

    /** @return zero-based word index of a SYNDICT1 id */
    public static int syndictWord(int id) {
        return (id & SYNDICT_MASK) - 1;
    }

    /** @return MIWROWS1 IndexId language (upper 16 bits) */
    public static int indexIdLanguage(long indexId) {
        return (int) (indexId >>> 48);
    }

    /** @return MIWROWS1 IndexId overlay flag (bit 47) */
    public static boolean indexIdOverlay(long indexId) {
        return ((indexId >>> 47) & 1L) != 0;
    }

    /** @return MIWROWS1 IndexId 47-bit local coordinate */
    public static long indexIdLocal(long indexId) {
        return indexId & INDEX_LOCAL_MASK;
    }
}
