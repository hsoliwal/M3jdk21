/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Internal bounded regex-effect algebra, not a parser or a text/pattern storage owner.
 * Receives validated M3 String dot/star instructions. Each fragment is a Boolean relation
 * on at most 64 epsilon-NFA positions. Relation composition preserves logical seams.
 *
 * <p>Target-owned adaptation of the Synexia M3RegexDp capability, not an Apache source
 * transplant or a public String API. No input, cursor, String or native address is retained.
 * Optional checkpoints belong to internal callers; ordinary String matching must not gain
 * a new interruption/cancellation contract.</p>
 */
public final class M3RX {
    public static final int UNKNOWN = -1;
    public static final int NO_MATCH = 0;
    public static final int MATCH = 1;
    public static final int STAR = 1 << 16;
    public static final int MAX_ATOMS = 63;
    private static final int MAX_STREAM_UNITS = 1_000_000;

    private M3RX() { }

    /** Admission is instruction-based; the existing target regex parser keeps syntax authority. */
    public static Plan compile(int[] atoms, int count, Runnable checkpoint) {
        Objects.requireNonNull(atoms, "atoms");
        if (count < 1 || count > MAX_ATOMS || count > atoms.length) {
            throw new IllegalArgumentException("M3 regex instruction geometry");
        }
        tick(checkpoint);
        int[] instructions = new int[count];
        for (int i = 0; i < count; i++) {
            int atom = atoms[i];
            int unit = atom & 0xffff;
            if ((atom & ~(STAR | 0xffff)) != 0 || !(unit == '.' || alphanumeric(unit))) {
                throw new IllegalArgumentException("M3 regex instruction");
            }
            instructions[i] = atom;
        }
        Plan result = new Plan(instructions);
        tick(checkpoint);
        return result;
    }

    public static final class Plan {
        private final int[] instructions;
        private final long[] ascii;
        private final long[] epsilonRuns;
        private final int closurePasses;
        private final long starred;
        private final long dots;
        private final long valid;
        private final long accepting;
        private final int states;

        private Plan(int[] instructions) {
            this.instructions = instructions;
            states = instructions.length + 1;
            valid = states == Long.SIZE ? -1L : (1L << states) - 1;
            accepting = 1L << instructions.length;
            ascii = new long[128];
            long stars = 0, wildcards = 0;
            for (int i = 0; i < instructions.length; i++) {
                long bit = 1L << i;
                int atom = instructions[i];
                if ((atom & STAR) != 0) stars |= bit;
                if ((atom & 0xffff) == '.') wildcards |= bit;
                else ascii[atom & 0xffff] |= bit;
            }
            starred = stars;
            dots = wildcards;
            for (int c = 0; c < ascii.length; c++) if (!lineTerminator(c)) ascii[c] |= dots;
            epsilonRuns = new long[6];
            epsilonRuns[0] = stars;
            for (int i = 1; i < epsilonRuns.length; i++) {
                long previous = epsilonRuns[i - 1];
                epsilonRuns[i] = previous & (previous >>> (1 << (i - 1)));
            }
            int used = 0;
            for (int i = 0; i < epsilonRuns.length; i++) if (epsilonRuns[i] != 0) used = i + 1;
            closurePasses = used;
        }

        public int states() { return states; }
        public long primitiveBytes() { return 4L * instructions.length + 8L * (ascii.length + epsilonRuns.length); }

        /** Exact instructions, not a hash-only semantic key. No candidate array is retained. */
        public boolean sameInstructions(int[] atoms, int count) {
            Objects.requireNonNull(atoms, "atoms");
            if (count != instructions.length || count > atoms.length) return false;
            for (int i = 0; i < count; i++) if (atoms[i] != instructions[i]) return false;
            return true;
        }

        /** Scalar cold path: bounded frontier work, without building all-entry fragment effects. */
        public int tryMatch(int length, IntUnaryOperator units, Runnable checkpoint) {
            Objects.requireNonNull(units, "units");
            if (length < 0) throw new IllegalArgumentException("negative length");
            tick(checkpoint);
            if (length > MAX_STREAM_UNITS) return UNKNOWN;
            long frontier = close(1L);
            for (int at = 0; at < length; at++) {
                if ((at & 255) == 0) tick(checkpoint);
                int unit = units.applyAsInt(at);
                // Even an empty frontier cannot disguise a surrogate as a proved non-match.
                if (!supported(unit)) return UNKNOWN;
                frontier = step(frontier, unit);
            }
            tick(checkpoint);
            return (frontier & accepting) != 0 ? MATCH : NO_MATCH;
        }

        /** Identity on epsilon-closed frontiers; NOT the raw diagonal Boolean matrix. */
        public Effect identity(Runnable checkpoint) {
            tick(checkpoint);
            long[] rows = new long[states];
            for (int i = 0; i < states; i++) rows[i] = close(1L << i);
            return new Effect(this, 0, rows);
        }

        /** Reads each input unit once; work refusal is null, not a negative match. */
        public Effect summarize(int length, IntUnaryOperator units, long maxStateSteps,
                                Runnable checkpoint) {
            Objects.requireNonNull(units, "units");
            if (length < 0 || maxStateSteps < 0) throw new IllegalArgumentException("negative limit");
            tick(checkpoint);
            if ((long) length * states > maxStateSteps) return null;
            long[] rows = identity(checkpoint).rows;
            for (int at = 0; at < length; at++) {
                if ((at & 63) == 0) tick(checkpoint);
                int unit = units.applyAsInt(at);
                if (!supported(unit)) return null;
                for (int entry = 0; entry < states; entry++) rows[entry] = step(rows[entry], unit);
            }
            tick(checkpoint);
            return new Effect(this, length, rows);
        }

        public Effect compose(Effect left, Effect right, Runnable checkpoint) {
            require(left); require(right); tick(checkpoint);
            long length = Math.addExact(left.length, right.length);
            long[] result = new long[states];
            for (int i = 0; i < states; i++) {
                tick(checkpoint);
                result[i] = applyRows(right.rows, left.rows[i]);
            }
            tick(checkpoint);
            return new Effect(this, length, result);
        }

        /** Explicit numeric JNI candidate. Absent native support falls back to the exact Java lane. */
        public Effect composeNative(Effect left, Effect right, Runnable checkpoint) {
            require(left); require(right); tick(checkpoint);
            long length = Math.addExact(left.length, right.length);
            final long[] rows;
            try {
                rows = composeRows0(left.rows, right.rows, states);
            } catch (UnsatisfiedLinkError absent) {
                return compose(left, right, checkpoint);
            }
            tick(checkpoint);
            if (rows == null || rows.length != states) throw new InternalError("M3 regex native geometry");
            for (int i = 0; i < rows.length; i++) {
                if ((rows[i] & ~valid) != 0 || (length == 0 && rows[i] != close(1L << i))) {
                    throw new InternalError("M3 regex native row");
                }
            }
            return new Effect(this, length, rows);
        }

        public Effect repeat(Effect block, long count, Runnable checkpoint) {
            require(block);
            if (count < 0) throw new IllegalArgumentException("negative repetitions");
            Math.multiplyExact(block.length, count);
            Effect output = identity(checkpoint), power = block;
            for (long n = count; n != 0; n >>>= 1) {
                tick(checkpoint);
                if ((n & 1) != 0) output = compose(output, power, checkpoint);
                if (n > 1) power = compose(power, power, checkpoint);
            }
            return output;
        }

        public long apply(Effect effect, long incoming, Runnable checkpoint) {
            require(effect); tick(checkpoint);
            if ((incoming & ~valid) != 0) throw new IllegalArgumentException("M3 regex frontier");
            long answer = applyRows(effect.rows, incoming);
            tick(checkpoint);
            return answer;
        }

        public boolean matches(Effect effect) {
            require(effect);
            return (effect.rows[0] & accepting) != 0;
        }

        private long close(long bits) {
            for (int i = 0; i < closurePasses; i++) bits |= (bits & epsilonRuns[i]) << (1 << i);
            return bits & valid;
        }

        private long step(long frontier, int unit) {
            long match = frontier & (unit < 128 ? ascii[unit] : lineTerminator(unit) ? 0 : dots);
            return close((match & starred) | ((match & ~starred) << 1));
        }

        private void require(Effect effect) {
            if (Objects.requireNonNull(effect, "effect").owner != this) {
                throw new IllegalArgumentException("different M3 regex plan");
            }
        }
    }

    public static final class Effect {
        private final Plan owner;
        private final long length;
        private final long[] rows;
        private Effect(Plan owner, long length, long[] rows) {
            this.owner = owner; this.length = length; this.rows = rows;
        }
        public long length() { return length; }
        public long primitiveBytes() { return (long) rows.length * Long.BYTES; }
    }

    private static long applyRows(long[] rows, long incoming) {
        long result = 0;
        while (incoming != 0) {
            result |= rows[Long.numberOfTrailingZeros(incoming)];
            incoming &= incoming - 1;
        }
        return result;
    }

    private static boolean alphanumeric(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }
    private static boolean supported(int c) { return c >= 0 && c <= 0xffff && (c < 0xd800 || c > 0xdfff); }
    private static boolean lineTerminator(int c) { return c == 10 || c == 13 || c == 0x85 || c == 0x2028 || c == 0x2029; }
    private static void tick(Runnable checkpoint) { if (checkpoint != null) checkpoint.run(); }
    private static native long[] composeRows0(long[] left, long[] right, int states);
}
