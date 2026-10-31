// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Lossless bridge between mapped M3 String canonical IDs and nonnegative M3 atom payload IDs.
 *
 * <p>M3StringPool lexicon atoms use bit 63 as the mapped-lexicon tag. M3AtomStore reserves
 * nonnegative payload coordinates. Removing only bit 63 preserves the complete generation tag and
 * CWID, so different lexicon generations cannot collide. The conversion is allocation-free and
 * exactly reversible.</p>
 */
public final class M3LexiconAtomCoordinates {
    private static final long RESERVED_BIT = 0x4000_0000_0000_0000L;
    private static final long LOW_WORD_MASK = 0xffff_ffffL;

    private M3LexiconAtomCoordinates() { }

    /**
     * Converts one M3StringPool lexicon canonical ID to its atom-store payload coordinate.
     */
    public static long payloadId(long canonicalId) {
        if (canonicalId >= 0L || (canonicalId & RESERVED_BIT) != 0L
                || (canonicalId & LOW_WORD_MASK) == 0L) {
            throw new IllegalArgumentException("not an M3 mapped lexicon canonical ID");
        }
        return canonicalId & Long.MAX_VALUE;
    }

    /**
     * Restores the exact M3StringPool lexicon canonical ID from an atom-store payload coordinate.
     */
    public static long canonicalId(long payloadId) {
        if (payloadId <= 0L || (payloadId & RESERVED_BIT) != 0L
                || (payloadId & LOW_WORD_MASK) == 0L) {
            throw new IllegalArgumentException("not an M3 lexicon atom payload coordinate");
        }
        return Long.MIN_VALUE | payloadId;
    }

    /**
     * Resolves a generation-qualified atom payload to the dense CWID of the supplied generation.
     * Returns zero for another generation.
     */
    public static int wordId(long payloadId, long generation64) {
        return M3LexiconWordIds.wordId(canonicalId(payloadId), generation64);
    }

    /** True when the payload coordinate is structurally valid for an M3 mapped lexicon atom. */
    public static boolean isLexiconPayload(long payloadId) {
        return payloadId > 0L
                && (payloadId & RESERVED_BIT) == 0L
                && (payloadId & LOW_WORD_MASK) != 0L;
    }
}
