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
 * Patterns admit only ASCII alphanumeric/dot atoms and optional stars. The legacy
 * summarize/advance entries reject surrogate input; summarizeUtf16 resolves code points.
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
    private static volatile boolean structuredUnavailable;
    private static volatile boolean repeatUnavailable;

    private M3RegexDpPlan(long[] literals, long dots, long starred, int tokens) {
        this.literalMasks = literals;
        this.dots = dots;
        this.starred = starred;
        states = tokens + 1;
        universe = states == 64 ? -1L : (1L << states) - 1;
        accepting = 1L << tokens;
        nullableBridges = new long[nullableLevels(starred)];
        long bridge = starred;
        for (int k = 0; k < nullableBridges.length; k++) {
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
        long[] literals = new long[62];
        long dot = 0, star = 0;
        int token = 0;
        for (int i = 0; i < size; i++) {
            char c = pattern.charAt(i);
            if (c == '*') star |= 1L << token;
            else if (c == '.') dot |= 1L << ++token;
            else literals[literalIndex(c)] |= 1L << ++token;
        }
        return new M3RegexDpPlan(literals, dot, star, tokens);
    }

    public int stateCount() { return states; }
    public long initialState() { return initial; }
    public boolean accepts(long frontier) { checkBits(frontier); return (frontier & accepting) != 0; }
    public boolean supports(char unit) { return !Character.isSurrogate(unit); }

    /** One allocation-free DP step, with up to six precomputed nullable-prefix propagation masks. */
    public long advance(long frontier, char unit) {
        checkBits(frontier);
        if (!supports(unit)) throw new IllegalArgumentException("surrogate input is not admitted");
        return advanceMatched(frontier, matchingMask(unit));
    }

    /** Pure pattern predicate: compute once per consumed unit, not once per row. */
    private long matchingMask(char unit) {
        int literal = literalIndex(unit);
        long matched = literal < 0 ? 0 : literalMasks[literal];
        return lineTerminator(unit) ? matched : matched | dots;
    }

    /** Internal callers already hold validated states and a supported unit. */
    private long advanceMatched(long frontier, long matched) {
        long consumed = ((frontier << 1) & matched & ~starred)
                | (frontier & matched & starred);
        return close(consumed);
    }

    /** Compact perfect index for exactly the admitted 62 literal code units. */
    private static int literalIndex(char unit) {
        if (unit >= 'a' && unit <= 'z') return unit - 'a';
        if (unit >= 'A' && unit <= 'Z') return unit - 'A' + 26;
        if (unit >= '0' && unit <= '9') return unit - '0' + 52;
        return -1;
    }

    /** Keep only the nonzero doubling bridges needed by this immutable program. */
    private static int nullableLevels(long stars) {
        int levels = 0;
        for (long bridge = stars; bridge != 0 && levels < 6; levels++) {
            bridge &= bridge << (1 << levels);
        }
        return levels;
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
        long previousMask = 0;
        boolean fixedPoint = false;
        boolean seekFixedPoint = length >= 2 * states;
        for (int i = 0; i < length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            char c = text.charAt(i);
            if (!supports(c)) return null;
            long matched = matchingMask(c);
            if (!seekFixedPoint || i == 0 || matched != previousMask) {
                fixedPoint = false;
                for (int state = 0; state < states; state++) {
                    rows[state] = advanceMatched(rows[state], matched);
                }
            } else if (!fixedPoint) {
                fixedPoint = true;
                for (int state = 0; state < states; state++) {
                    long incoming = rows[state];
                    long next = advanceMatched(incoming, matched);
                    fixedPoint &= incoming == next;
                    rows[state] = next;
                }
            }
            previousMask = matched;
            // Reuse only a proven fixed point under the identical pattern predicate.
            // Keep reading even then: later surrogates, charAt failures and
            // checkpoint calls retain their original order and behavior.
        }
        check(checkpoint);
        return new Effect(rows, length);
    }

    /**
     * Code-point-correct summary for the same ASCII literal/dot/star program.
     * Unlike {@link #summarize}, this entry admits paired and unpaired surrogates.
     * A terminal high surrogate is deferred; a leading low has an optional skip relation.
     * Those two pieces of boundary metadata allow concatenation to resolve a split pair.
     * No character, source reference, text array or pointer is retained by the result.
     * Every UTF-16 unit is read once, in order; checkpoints occur at the same unit cadence.
     */
    public Effect summarizeUtf16(CharSequence text, Runnable checkpoint) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        long[] rows = identityRows();
        long[] withoutLeadingLow = null;
        boolean pendingHigh = false;
        for (int i = 0; i < length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            char unit = text.charAt(i);
            if (i == 0 && Character.isLowSurrogate(unit)) {
                withoutLeadingLow = identityRows();
            }
            if (pendingHigh) {
                // Both a complete supplementary code point and a lone high surrogate
                // have the dot predicate in this program's strictly ASCII literal domain.
                advanceRows(rows, dots);
                if (withoutLeadingLow != null) advanceRows(withoutLeadingLow, dots);
                pendingHigh = false;
                if (Character.isLowSurrogate(unit)) continue;
            }
            if (Character.isHighSurrogate(unit)) {
                pendingHigh = true;
            } else {
                long matched = matchingMask(unit);
                advanceRows(rows, matched);
                if (withoutLeadingLow != null && i != 0) {
                    advanceRows(withoutLeadingLow, matched);
                }
            }
        }
        check(checkpoint);
        return new Effect(rows, length, withoutLeadingLow, pendingHigh);
    }

    private void advanceRows(long[] rows, long matched) {
        for (int state = 0; state < states; state++) {
            rows[state] = advanceMatched(rows[state], matched);
        }
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

        // Null means the fragment does not start with a low surrogate. Otherwise this
        // is the open relation after omitting that first unit. Rows omit a terminal high.
        private final long[] withoutLeadingLow;
        private final boolean trailingHigh;

        private Effect(long[] rows, long length) { this(rows, length, null, false); }
        private Effect(long[] rows, long length, long[] withoutLeadingLow, boolean trailingHigh) {
            this.rows = rows;
            this.length = length;
            this.withoutLeadingLow = withoutLeadingLow;
            this.trailingHigh = trailingHigh;
        }
        public long length() { return length; }
        public long apply(long frontier) {
            checkBits(frontier);
            long result = applyRows(rows, frontier);
            // Complete-input observation flushes a remaining lone high surrogate.
            return trailingHigh ? advanceMatched(result, dots) : result;
        }

        public Effect then(Effect right, Runnable checkpoint) {
            Objects.requireNonNull(right, "right");
            if (right.plan != plan) throw new IllegalArgumentException("different regex plan");
            long total = Math.addExact(length, right.length);
            check(checkpoint);
            Effect result;
            if (length == 0) {
                result = new Effect(right.rows, total, right.withoutLeadingLow, right.trailingHigh);
            } else if (right.length == 0) {
                result = new Effect(rows, total, withoutLeadingLow, trailingHigh);
            } else {
                long[] suffix = trailingHigh && right.withoutLeadingLow != null
                        ? right.withoutLeadingLow : right.rows;
                long[] joined = appendOpen(rows, suffix, trailingHigh);
                long[] skipped = withoutLeadingLow == null ? null
                        : appendOpen(withoutLeadingLow, suffix, trailingHigh);
                result = new Effect(joined, total, skipped, right.trailingHigh);
            }
            check(checkpoint);
            return result;
        }

        /** Insert the pending-high predicate before the suffix, without completing it. */
        private long[] appendOpen(long[] prefix, long[] suffix, boolean pendingHigh) {
            if (!pendingHigh) return compose(prefix, suffix);
            long[] flushed = prefix.clone();
            advanceRows(flushed, dots);
            return compose(flushed, suffix);
        }

        /** Boundary-aware powers reuse the same ordered composition; callbacks stay external. */
        private Effect repeatedUtf16(long count, long total, Runnable checkpoint) {
            Effect result = identity(), power = this;
            for (long left = count; left != 0; left >>>= 1) {
                check(checkpoint);
                if ((left & 1L) != 0) result = result.then(power, null);
                if (left > 1) power = power.then(power, null);
            }
            check(checkpoint);
            return new Effect(result.rows, total, result.withoutLeadingLow, result.trailingHigh);
        }

        /**
         * Exact power without expanded text. Boundary-free Java retains its checkpoint schedule and three
         * rotating rows. Eligible wide/null-checkpoint powers may use one bounded JNI call,
         * returning one row array. Missing repeat JNI leaves prior Java execution available.
         * Boundary-bearing effects compose in O(states^2 log(count + 1)) work and
         * O(states) live row payload, with O(states log(count + 1)) allocated row payload.
         */
        public Effect repeated(long count, Runnable checkpoint) {
            if (count < 0) throw new IllegalArgumentException("negative repeat count");
            long total = Math.multiplyExact(length, count);
            check(checkpoint);
            // Preserve the original callback schedule and fresh Effect identity. Rows
            // are private immutable summaries, so count one can share them safely.
            // Keep both Unicode seam fields: a later join may complete a split pair.
            if (count == 0) {
                long[] empty = identityRows();
                check(checkpoint);
                return new Effect(empty, total);
            }
            if (count == 1) {
                check(checkpoint);
                check(checkpoint);
                return new Effect(rows, total, withoutLeadingLow, trailingHigh);
            }
            if (withoutLeadingLow != null || trailingHigh) {
                return repeatedUtf16(count, total, checkpoint);
            }
            // Preserve every supplied checkpoint on the original Java schedule.
            // A null checkpoint permits one bounded native call; it never owns text.
            if (checkpoint == null && count >= 256 && states >= 16
                    && starred == (universe & ~1L) && needsRowUnion(rows)
                    && !nativeUnavailable && !repeatUnavailable) {
                try {
                    return new Effect(repeat0(rows, count, starred == (universe & ~1L)), total);
                } catch (UnsatisfiedLinkError unavailable) {
                    // Older libjava may expose compose0 but not repeat0. Keep it usable.
                    repeatUnavailable = true;
                }
            }
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
        if (states >= 16 && needsRowUnion(left) && !nativeUnavailable) {
            if (starred == (universe & ~1L) && !structuredUnavailable) {
                try { return composeStructured0(left, right); }
                catch (UnsatisfiedLinkError unavailable) { structuredUnavailable = true; }
            }
            try { return compose0(left, right); }
            catch (UnsatisfiedLinkError unavailable) { nativeUnavailable = true; }
        }
        long[] result = new long[states];
        composeJava(left, right, result);
        return result;
    }

    /** Zero/singleton frontiers need direct row selection, not a JNI round trip. */
    private static boolean needsRowUnion(long[] rows) {
        for (long row : rows) if ((row & (row - 1)) != 0) return true;
        return false;
    }

    private static void composeJava(long[] left, long[] right, long[] result) {
        // Reuse the product's existing ordered primitive relation atom. All callers
        // supply this plan's dimensions/domain and a distinct destination row array.
        M3StateRelation.compose(left, 0, right, 0, result, 0, left.length);
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
    static native long[] composeStructured0(long[] left, long[] right);
    static native long[] repeat0(long[] rows, long count, boolean structured);
}
