/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Target-owned Boolean dot/star execution plan; not a Pattern parser or public JDK API.
 *
 * <p>Maps the prepared-state and fragment-relation responsibilities of Synexia
 * MIndexRegexProgram/MIndexRegexAtomAlgebra into the existing M3 String decision path.
 * Only ASCII alphanumeric/dot atoms, optional stars, and non-surrogate input are admitted.
 * No captures, search history, assertions, spelling payload or donor runtime is retained.
 * Immutable effects belong to this exact plan; the effect of A followed by B is B(A(state)).
 */
public final class M3RegexDpPlan {
    private final long[] literalMasks;
    private final long[] nullableBridges;
    private final long starred;
    private final long dots;
    private final long universe;
    private final long initial;
    private final long accepting;
    private final int states;
    private static volatile boolean nativeUnavailable;

    private M3RegexDpPlan(long[] literals, long dots, long starred, int tokens) {
        this.literalMasks = literals;
        this.dots = dots;
        this.starred = starred;
        states = tokens + 1;
        universe = states == 64 ? -1L : (1L << states) - 1;
        accepting = 1L << tokens;
        nullableBridges = new long[6];
        long bridge = starred;
        for (int k = 0; k < 6; k++) {
            nullableBridges[k] = bridge;
            bridge &= bridge << (1 << k);
        }
        initial = close(1L);
    }

    /** Null means not admitted. The pattern is inspected but never retained. */
    public static M3RegexDpPlan prepare(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        int size = pattern.length();
        if (size < 2 || size > 128) return null;
        int tokens = 0, stars = 0;
        boolean previousStar = true;
        for (int i = 0; i < size; i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (previousStar) return null;
                stars++;
                previousStar = true;
            } else {
                if (!(c == '.' || c >= 'a' && c <= 'z'
                        || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) return null;
                if (++tokens > 63) return null;
                previousStar = false;
            }
        }
        if (stars == 0) return null;
        long[] literals = new long[128];
        long dot = 0, star = 0;
        int token = 0;
        for (int i = 0; i < size; i++) {
            char c = pattern.charAt(i);
            if (c == '*') star |= 1L << token;
            else if (c == '.') dot |= 1L << ++token;
            else literals[c] |= 1L << ++token;
        }
        return new M3RegexDpPlan(literals, dot, star, tokens);
    }

    public int stateCount() { return states; }
    public long initialState() { return initial; }
    public boolean accepts(long frontier) { checkBits(frontier); return (frontier & accepting) != 0; }
    public boolean supports(char unit) { return !Character.isSurrogate(unit); }

    /** One allocation-free DP step, with six precomputed nullable-prefix propagation masks. */
    public long advance(long frontier, char unit) {
        checkBits(frontier);
        if (!supports(unit)) throw new IllegalArgumentException("surrogate input is not admitted");
        long matched = unit < literalMasks.length ? literalMasks[unit] : 0;
        if (!lineTerminator(unit)) matched |= dots;
        long consumed = ((frontier << 1) & matched & ~starred)
                | (frontier & matched & starred);
        return close(consumed);
    }

    private long close(long frontier) {
        for (int k = 0; k < nullableBridges.length; k++) {
            frontier |= (frontier << (1 << k)) & nullableBridges[k];
        }
        return frontier & universe;
    }

    private static boolean lineTerminator(char c) {
        return c == '\n' || c == '\r' || c == '\u0085' || c == '\u2028' || c == '\u2029';
    }

    /**
     * Build an all-entry-state effect without retaining input. Null means unsupported input.
     * Work is O(length * states), with at most six nullable-mask steps per entry transition.
     * Caller supplies stable text and a bounded length; a null checkpoint changes no thread state.
     */
    public Effect summarize(CharSequence text, Runnable checkpoint) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        long[] rows = identityRows();
        for (int i = 0; i < length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            char c = text.charAt(i);
            if (!supports(c)) return null;
            for (int state = 0; state < states; state++) rows[state] = advance(rows[state], c);
        }
        check(checkpoint);
        return new Effect(rows, length);
    }

    /** Exact empty-fragment identity, not epsilon closure of arbitrary incoming states. */
    public Effect identity() { return new Effect(identityRows(), 0); }

    private long[] identityRows() {
        long[] rows = new long[states];
        for (int i = 0; i < states; i++) rows[i] = 1L << i;
        return rows;
    }

    public final class Effect {
        private final M3RegexDpPlan plan = M3RegexDpPlan.this;
        private final long[] rows;
        private final long length;

        private Effect(long[] rows, long length) { this.rows = rows; this.length = length; }
        public long length() { return length; }
        public long apply(long frontier) { checkBits(frontier); return applyRows(rows, frontier); }

        public Effect then(Effect right, Runnable checkpoint) {
            Objects.requireNonNull(right, "right");
            if (right.plan != plan) throw new IllegalArgumentException("different regex plan");
            long total = Math.addExact(length, right.length);
            check(checkpoint);
            long[] result = compose(rows, right.rows);
            check(checkpoint);
            return new Effect(result, total);
        }

        /** Three work rows, reused at every exponent step; no expanded text or tuple. */
        public Effect repeated(long count, Runnable checkpoint) {
            if (count < 0) throw new IllegalArgumentException("negative repeat count");
            long total = Math.multiplyExact(length, count);
            check(checkpoint);
            long[] result = identityRows(), power = rows.clone(), scratch = new long[states];
            for (long left = count; left != 0; left >>>= 1) {
                check(checkpoint);
                if ((left & 1) != 0) {
                    composeJava(result, power, scratch);
                    long[] swap = result; result = scratch; scratch = swap;
                }
                if (left > 1) {
                    composeJava(power, power, scratch);
                    long[] swap = power; power = scratch; scratch = swap;
                }
            }
            check(checkpoint);
            return new Effect(result, total);
        }
    }

    private long[] compose(long[] left, long[] right) {
        // Native absence is an optimization miss, never an answer of NO_MATCH.
        if (states >= 16 && !nativeUnavailable) {
            try { return compose0(left, right); }
            catch (UnsatisfiedLinkError unavailable) { nativeUnavailable = true; }
        }
        long[] result = new long[states];
        composeJava(left, right, result);
        return result;
    }

    private static void composeJava(long[] left, long[] right, long[] result) {
        for (int i = 0; i < left.length; i++) result[i] = applyRows(right, left[i]);
    }

    private static long applyRows(long[] rows, long frontier) {
        long answer = 0;
        while (frontier != 0) {
            answer |= rows[Long.numberOfTrailingZeros(frontier)];
            frontier &= frontier - 1;
        }
        return answer;
    }

    private void checkBits(long bits) {
        if ((bits & ~universe) != 0) throw new IllegalArgumentException("foreign state bits");
    }
    private static void check(Runnable checkpoint) { if (checkpoint != null) checkpoint.run(); }

    // Package visibility permits exact JNI boundary qualification without exposing a JDK API.
    static native long[] compose0(long[] left, long[] right);
}
