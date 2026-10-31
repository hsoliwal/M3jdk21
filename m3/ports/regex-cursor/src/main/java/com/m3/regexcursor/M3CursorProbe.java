// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide counters for the cursor regex path; tests assert on them.
 *
 * <p>{@link #materializedUnits()} counts every symbol copied out of a cursor into a flat
 * {@code String}/{@code char[]} (only {@link M3CursorCharSequence#toString()} does that); the
 * cursor path must keep it at zero.</p>
 */
public final class M3CursorProbe {
    private static final AtomicLong MATERIALIZED = new AtomicLong();
    private static final AtomicLong STOCK = new AtomicLong();
    private static final AtomicLong GATE = new AtomicLong();
    private static final AtomicLong DFA_GIVE_UP = new AtomicLong();

    private M3CursorProbe() {}

    static void materialized(int units) {
        MATERIALIZED.addAndGet(units);
    }

    static void stock() {
        STOCK.incrementAndGet();
    }

    static void gate() {
        GATE.incrementAndGet();
    }

    static void dfaGiveUp() {
        DFA_GIVE_UP.incrementAndGet();
    }

    /** Symbols copied into flat text since the last {@link #reset()}. */
    public static long materializedUnits() {
        return MATERIALIZED.get();
    }

    /** Searches answered by the stock {@code java.util.regex} engine over a cursor view. */
    public static long stockSearches() {
        return STOCK.get();
    }

    /** Searches decided by a precompute or literal gate without running an automaton. */
    public static long gateDecisions() {
        return GATE.get();
    }

    /** Lazy DFA searches abandoned because the state cache filled. */
    public static long dfaGiveUps() {
        return DFA_GIVE_UP.get();
    }

    /** Clears every counter. */
    public static void reset() {
        MATERIALIZED.set(0);
        STOCK.set(0);
        GATE.set(0);
        DFA_GIVE_UP.set(0);
    }
}
