// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

/**
 * Precomputed facts a cursor source can answer without reading its text.
 *
 * <p>Proving-ground twin of the M3String fact lanes ({@code M3StringFacts} unit presence,
 * {@code jdk.internal.mindex.M3TQ} trigram absence): the regex gates consult these facts before
 * any engine touches a chunk.</p>
 */
public interface M3CursorFacts {
    /** All bits set: nothing is known. */
    long UNKNOWN = -1L;

    /**
     * Unit-presence set: bit {@code unit & 63} is set when {@code unit} may occur in the source.
     * A clear bit proves absence; {@link #UNKNOWN} proves nothing.
     */
    long unitPresenceMask();

    /** Presence bit of one symbol. */
    static long bit(int unit) {
        return 1L << (unit & 63);
    }
}
