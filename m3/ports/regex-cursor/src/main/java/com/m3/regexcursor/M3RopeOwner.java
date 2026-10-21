// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

/**
 * Proving-ground mirror of {@code java.lang.M3StringOwner}: a canonical owner node that is either
 * a scalar atom or a composition tuple. Text is never stored in {@link M3RopeString} itself.
 */
public abstract sealed class M3RopeOwner permits M3RopeAtom, M3RopeTuple {
    /** Number of UTF-16 units owned. */
    final int length;
    /** OR-composed unit-presence set (see {@link M3CursorFacts#unitPresenceMask()}). */
    final long presence;

    M3RopeOwner(int length, long presence) {
        this.length = length;
        this.presence = presence;
    }

    /** Owned length in UTF-16 units. */
    public final int length() {
        return length;
    }
}
