/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

/**
 * A monoid measure over UTF-16 text whose summaries live in fixed-width {@code long} lanes.
 *
 * <p>Every node of an {@link M3MeasuredRope} carries one summary per registered measure. A summary is
 * {@link #width()} consecutive {@code long} slots in an arena lane, so no summary object is stored.</p>
 *
 * <p>Laws, checked by {@link M3MeasuredRope#verifyInvariants()} for every node and by the measure tests
 * for every split point: {@link #identity} is neutral on both sides of {@link #combine}; {@code combine} is
 * associative; and the leaf measure is a homomorphism,
 * {@code leaf(t, a, c) == combine(leaf(t, a, b), leaf(t, b, c))} for every {@code a <= b <= c}, including a
 * split inside a surrogate pair or between CR and LF. A measure repairs such seams in {@code combine}.</p>
 *
 * <p>Implementations are stateless strategies and do not allocate per call. The contract shape follows
 * functionaljava {@code fj.data.fingertrees.Measured}/{@code Monoid} and Swift Collections
 * {@code RopeSummary}/{@code RopeElement.summary} (pattern only; no code copied).</p>
 */
public interface M3RopeMeasure {
    /** Number of {@code long} slots one summary occupies; at least one. */
    int width();

    /** Writes the identity summary at {@code out[at .. at + width())}. */
    void identity(long[] out, int at);

    /** Writes the summary of {@code text[from, to)} at {@code out[at .. at + width())}. */
    void leaf(CharSequence text, int from, int to, long[] out, int at);

    /**
     * Writes {@code left (+) right} at {@code out[at ..]}. The output range never overlaps either input
     * range, so an implementation may read and write in any order.
     */
    void combine(long[] left, int leftAt, long[] right, int rightAt, long[] out, int at);
}
