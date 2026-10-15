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

    // Relocatable effect data; this is NOT Java serialization or the Synexia image ABI.
    private static final long EFFECT_IMAGE_MAGIC = 0x31464f455852334dL;
    private static final int EFFECT_IMAGE_VERSION = 1;
    private static final int EFFECT_IMAGE_HEADER = 104;
    private static final int IMAGE_LEADING_LOW = 1;
    private static final int IMAGE_TRAILING_HIGH = 2;

    /**
     * Restore an effect into this exact compiled program from a qualified image receipt.
     * The caller must supply a TRUSTED SHA-256 of the whole image and the exact canonical
     * source identity bound by its producer. A checksum read from the same untrusted file
     * is not a trusted receipt. Neither a hash nor this decoder proves that a forged effect
     * was computed from text; the producer/receipt must have been qualified independently.
     *
     * <p>Reads the buffer's remaining region without changing its position, limit, mark or
     * byte order. Takes a bounded snapshot before validation; neither the buffer, a mapping,
     * nor any source identity array is retained. A mapping must remain readable during this
     * call, but may be closed after it. Concurrent mutation is not synchronization: the
     * snapshot is accepted only if it has exactly the supplied image digest.
     *
     * <p>All syntax, bounds, plan/source identity and digest errors throw
     * IllegalArgumentException, never NO_MATCH. Nulls throw NullPointerException.
     * Fingerprints must have exactly 32 bytes. Invalid outer sizes are refused before
     * allocation of an image snapshot. A non-null checkpoint runs before that snapshot
     * and before returning the result; its exceptions are propagated. Calls with null
     * checkpoints neither consume nor change thread interruption status.
     */
    public Effect restoreEffect(java.nio.ByteBuffer image, byte[] expectedSourceIdentity,
            byte[] expectedImageSha256, Runnable checkpoint) {
        Objects.requireNonNull(image, "image");
        byte[] sourceIdentity = copyImageFingerprint(expectedSourceIdentity);
        byte[] imageIdentity = copyImageFingerprint(expectedImageSha256);
        // Capture caller-selected geometry before user code can change the original view.
        java.nio.ByteBuffer region = image.slice();
        int size = region.remaining();
        if (size < EFFECT_IMAGE_HEADER + states * Long.BYTES
                || size > EFFECT_IMAGE_HEADER + 2 * states * Long.BYTES) {
            throw new IllegalArgumentException("regex effect image size");
        }
        check(checkpoint);
        byte[] snapshot = new byte[size];
        region.get(snapshot);
        if (!java.security.MessageDigest.isEqual(imageIdentity, imageDigest(snapshot))) {
            throw new IllegalArgumentException("regex effect image digest");
        }
        java.nio.ByteBuffer input = java.nio.ByteBuffer.wrap(snapshot)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        if (input.getLong() != EFFECT_IMAGE_MAGIC || input.getInt() != EFFECT_IMAGE_VERSION
                || input.getInt() != EFFECT_IMAGE_HEADER || input.getInt() != size
                || input.getInt() != states) {
            throw new IllegalArgumentException("regex effect image schema or program width");
        }
        int flags = input.getInt();
        if ((flags & ~(IMAGE_LEADING_LOW | IMAGE_TRAILING_HIGH)) != 0 || input.getInt() != 0) {
            throw new IllegalArgumentException("regex effect image flags or reserved field");
        }
        long length = input.getLong();
        int laneCount = (flags & IMAGE_LEADING_LOW) != 0 ? 2 : 1;
        if (length < 0 || length == 0 && flags != 0
                || length == 1 && flags == (IMAGE_LEADING_LOW | IMAGE_TRAILING_HIGH)
                || size != EFFECT_IMAGE_HEADER + laneCount * states * Long.BYTES) {
            throw new IllegalArgumentException("regex effect image geometry");
        }
        byte[] stored = new byte[32];
        input.get(stored);
        if (!java.security.MessageDigest.isEqual(stored, imageProgramFingerprint())) {
            throw new IllegalArgumentException("different regex image program");
        }
        input.get(stored);
        if (!java.security.MessageDigest.isEqual(stored, sourceIdentity)) {
            throw new IllegalArgumentException("different regex image source");
        }
        long[] rows = readImageRows(input);
        long[] skipped = laneCount == 2 ? readImageRows(input) : null;
        boolean high = (flags & IMAGE_TRAILING_HIGH) != 0;
        // Structural invariants are additional admission checks, not semantic proof.
        if (length == 0 || length == 1 && high) requireImageIdentity(rows);
        if (length == 1 && skipped != null) requireImageIdentity(skipped);
        if (skipped != null) {
            for (int i = 0; i < states; i++) {
                if (rows[i] != applyRows(skipped, advanceMatched(1L << i, dots))) {
                    throw new IllegalArgumentException("regex image leading-low relation");
                }
            }
        }
        Effect result = new Effect(rows, length, skipped, high);
        check(checkpoint);
        return result;
    }

    private long[] readImageRows(java.nio.ByteBuffer input) {
        long[] result = new long[states];
        for (int i = 0; i < states; i++) {
            result[i] = input.getLong();
            checkBits(result[i]);
        }
        return result;
    }

    private void requireImageIdentity(long[] rows) {
        for (int i = 0; i < states; i++) {
            if (rows[i] != (1L << i)) throw new IllegalArgumentException("regex image identity");
        }
    }

    private static byte[] copyImageFingerprint(byte[] value) {
        Objects.requireNonNull(value, "fingerprint");
        if (value.length != 32) throw new IllegalArgumentException("SHA-256 fingerprint length");
        return value.clone();
    }

    private static byte[] imageDigest(byte[] bytes) {
        try { return java.security.MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (java.security.NoSuchAlgorithmException missing) { throw new AssertionError(missing); }
    }

    /** Exact derived primitive program and semantic-version identity, not object identity. */
    private byte[] imageProgramFingerprint() {
        java.nio.ByteBuffer data = java.nio.ByteBuffer.allocate(
                16 + Long.BYTES * (5 + literalMasks.length + nullableBridges.length))
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        data.putLong(EFFECT_IMAGE_MAGIC).putInt(EFFECT_IMAGE_VERSION).putInt(states)
                .putLong(starred).putLong(dots).putLong(universe).putLong(initial).putLong(accepting);
        for (long value : literalMasks) data.putLong(value);
        for (long value : nullableBridges) data.putLong(value);
        return imageDigest(data.array());
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
        /**
         * Export deterministic primitive metadata bound to a producer-supplied canonical
         * source identity. The exact 32-byte identity is opaque here: use an immutable,
         * generation/range-bound source receipt, never a process address or similarity hash.
         * Return bytes are fresh and owned by the caller; mutating them cannot change this
         * effect. No text or source-identity array is retained by this effect or its plan.
         *
         * <p>Persist SHA-256(returned bytes) in a separately trusted build/cache receipt.
         * Restore requires that digest, the same source identity and an equivalent compiled
         * plan. The format is little-endian, versioned and at most 1128 bytes (64 states,
         * two relations). This method does not choose paths, perform disk I/O, attach an
         * effect to a String owner, or make an untrusted artifact authoritative.
         */
        public byte[] toImage(byte[] sourceIdentity, Runnable checkpoint) {
            byte[] source = copyImageFingerprint(sourceIdentity);
            check(checkpoint);
            int flags = (withoutLeadingLow != null ? IMAGE_LEADING_LOW : 0)
                    | (trailingHigh ? IMAGE_TRAILING_HIGH : 0);
            int size = EFFECT_IMAGE_HEADER + states * Long.BYTES
                    * (withoutLeadingLow == null ? 1 : 2);
            byte[] result = new byte[size];
            java.nio.ByteBuffer output = java.nio.ByteBuffer.wrap(result)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
            output.putLong(EFFECT_IMAGE_MAGIC).putInt(EFFECT_IMAGE_VERSION)
                    .putInt(EFFECT_IMAGE_HEADER).putInt(size).putInt(states)
                    .putInt(flags).putInt(0).putLong(length)
                    .put(imageProgramFingerprint()).put(source);
            for (long row : rows) output.putLong(row);
            if (withoutLeadingLow != null) {
                for (long row : withoutLeadingLow) output.putLong(row);
            }
            check(checkpoint);
            return result;
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
    static native long[] composeStructured0(long[] left, long[] right);
    static native long[] repeat0(long[] rows, long count, boolean structured);
}
