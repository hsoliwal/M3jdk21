/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

import java.nio.charset.StandardCharsets;

/**
 * Built-in text measures for {@link M3MeasuredRope}, each a monoid whose {@code combine} repairs the seam
 * between two adjacent pieces.
 *
 * <p>Lineage: the hash, code point and UTF-8 seams adapt the first-party Synexia
 * {@code com.synexia.indexstring.IndexTextMetrics.combine} (blob 043e68f6), whose value-level port in this
 * module is {@link M3TextMetrics} (these measures are its allocation-free node-lane form and are tested
 * equal to it), and the composed hash of {@code java.lang.M3StringTuple}; the CR|LF seam matches the
 * in-flight {@code M3BigString} summary; the
 * whitespace monoid and the DFA transfer element are REGEX_PRECOMPUTE_DESIGN 2d and 2b. Edge-aware
 * measures keep the first and last code unit (or -1 when empty) in their last two slots.</p>
 */
public final class M3RopeMeasures {
    private static final int UTF8_REPLACEMENT = StandardCharsets.UTF_8.newEncoder().replacement().length;

    /** {@code [String.hashCode, 31^length]} as ints; combine {@code h(L) * 31^len(R) + h(R)}. */
    public static final M3RopeMeasure HASH = new Hash();

    /** {@code [code points, first unit, last unit]}; a pair split across the seam counts once. */
    public static final M3RopeMetric CODE_POINTS = new CodePoints();

    /** {@code [UTF-8 bytes as String.getBytes(UTF_8), first unit, last unit]}; lone surrogates encode as '?'. */
    public static final M3RopeMeasure UTF8_LENGTH = new Utf8Length();

    /** {@code [line terminators, first unit, last unit]}; CR, LF and CRLF count once, also across the seam. */
    public static final M3RopeMetric LINES = new Lines();

    /** {@code [leading, trailing, length]} runs of {@link Character#isWhitespace}; blank iff leading == length. */
    public static final M3RopeMeasure WHITESPACE = new Whitespace();

    private M3RopeMeasures() { }

    /** All built-in text measures, in a fixed order. */
    public static M3RopeMeasure[] text() {
        return new M3RopeMeasure[] {HASH, CODE_POINTS, UTF8_LENGTH, LINES, WHITESPACE};
    }

    /**
     * The transfer element {@code T(w): Q -> Q} of a deterministic automaton with at most 16 states, packed as
     * one 4-bit target per state in a single {@code long}; {@code T(uv) = T(v) o T(u)} is the combine.
     * {@code next[state * classes + cls]} is the successor; units below 256 map through {@code latin1Class},
     * all other units map to {@code otherClass}.
     */
    public static Dfa dfa(int states, int start, int acceptMask, int classes, byte[] latin1Class,
            int otherClass, byte[] next) {
        return new Dfa(states, start, acceptMask, classes, latin1Class.clone(), otherClass, next.clone());
    }

    static boolean emptyEdge(long[] s, int at) { return s[at + 1] < 0; }

    private static boolean joinsPair(long[] l, int la, long[] r, int ra) {
        return Character.isHighSurrogate((char) l[la + 2]) && Character.isLowSurrogate((char) r[ra + 1]);
    }

    private static void edges(CharSequence t, int from, int to, long[] out, int at) {
        out[at + 1] = from < to ? t.charAt(from) : -1;
        out[at + 2] = from < to ? t.charAt(to - 1) : -1;
    }

    private abstract static class Edged implements M3RopeMetric {
        @Override
        public final int width() { return 3; }

        @Override
        public final void identity(long[] out, int at) {
            out[at] = 0;
            out[at + 1] = -1;
            out[at + 2] = -1;
        }

        @Override
        public final long size(long[] summary, int at) { return summary[at]; }

        @Override
        public final void combine(long[] l, int la, long[] r, int ra, long[] out, int at) {
            if (emptyEdge(l, la)) {
                System.arraycopy(r, ra, out, at, 3);
            } else if (emptyEdge(r, ra)) {
                System.arraycopy(l, la, out, at, 3);
            } else {
                out[at] = l[la] + r[ra] + seam(l, la, r, ra);
                out[at + 1] = l[la + 1];
                out[at + 2] = r[ra + 2];
            }
        }

        abstract long seam(long[] l, int la, long[] r, int ra);
    }

    private static final class CodePoints extends Edged {
        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            out[at] = Character.codePointCount(t, from, to);
            edges(t, from, to, out, at);
        }

        @Override
        long seam(long[] l, int la, long[] r, int ra) { return joinsPair(l, la, r, ra) ? -1 : 0; }
    }

    private static final class Utf8Length extends Edged {
        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            long bytes = 0;
            for (int i = from; i < to; i++) {
                char c = t.charAt(i);
                if (c < 0x80) {
                    bytes += 1;
                } else if (c < 0x800) {
                    bytes += 2;
                } else if (Character.isHighSurrogate(c) && i + 1 < to && Character.isLowSurrogate(t.charAt(i + 1))) {
                    bytes += 4;
                    i++;
                } else {
                    bytes += Character.isSurrogate(c) ? UTF8_REPLACEMENT : 3;
                }
            }
            out[at] = bytes;
            edges(t, from, to, out, at);
        }

        @Override
        long seam(long[] l, int la, long[] r, int ra) {
            return joinsPair(l, la, r, ra) ? 4 - 2 * UTF8_REPLACEMENT : 0;
        }
    }

    private static final class Lines extends Edged {
        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            long terminators = 0;
            for (int i = from; i < to; i++) {
                char c = t.charAt(i);
                if (c == '\r' || (c == '\n' && (i == from || t.charAt(i - 1) != '\r'))) terminators++;
            }
            out[at] = terminators;
            edges(t, from, to, out, at);
        }

        @Override
        long seam(long[] l, int la, long[] r, int ra) {
            return l[la + 2] == '\r' && r[ra + 1] == '\n' ? -1 : 0;
        }
    }

    private static final class Hash implements M3RopeMeasure {
        @Override
        public int width() { return 2; }

        @Override
        public void identity(long[] out, int at) {
            out[at] = 0;
            out[at + 1] = 1;
        }

        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            int h = 0;
            int power = 1;
            for (int i = from; i < to; i++) {
                h = 31 * h + t.charAt(i);
                power *= 31;
            }
            out[at] = h;
            out[at + 1] = power;
        }

        @Override
        public void combine(long[] l, int la, long[] r, int ra, long[] out, int at) {
            int rightPower = (int) r[ra + 1];
            out[at] = (int) l[la] * rightPower + (int) r[ra];
            out[at + 1] = (int) l[la + 1] * rightPower;
        }
    }

    private static final class Whitespace implements M3RopeMeasure {
        @Override
        public int width() { return 3; }

        @Override
        public void identity(long[] out, int at) {
            out[at] = 0;
            out[at + 1] = 0;
            out[at + 2] = 0;
        }

        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            int lead = from;
            while (lead < to && Character.isWhitespace(t.charAt(lead))) lead++;
            int trail = to;
            while (trail > lead && Character.isWhitespace(t.charAt(trail - 1))) trail--;
            out[at] = lead - from;
            out[at + 1] = lead == to ? to - from : to - trail;
            out[at + 2] = to - from;
        }

        @Override
        public void combine(long[] l, int la, long[] r, int ra, long[] out, int at) {
            long leftLength = l[la + 2];
            long rightLength = r[ra + 2];
            out[at] = l[la] == leftLength ? leftLength + r[ra] : l[la];
            out[at + 1] = r[ra + 1] == rightLength ? rightLength + l[la + 1] : r[ra + 1];
            out[at + 2] = leftLength + rightLength;
        }
    }

    /** A packed DFA transfer measure; see {@link M3RopeMeasures#dfa}. */
    public static final class Dfa implements M3RopeMeasure {
        private final int states;
        private final int start;
        private final int acceptMask;
        private final int classes;
        private final byte[] latin1Class;
        private final int otherClass;
        private final byte[] next;
        private final long identity;

        Dfa(int states, int start, int acceptMask, int classes, byte[] latin1Class, int otherClass, byte[] next) {
            if (states < 1 || states > 16 || start < 0 || start >= states || classes < 1
                    || latin1Class.length != 256 || otherClass < 0 || otherClass >= classes
                    || next.length != states * classes) {
                throw new IllegalArgumentException("invalid DFA shape");
            }
            for (byte cls : latin1Class) {
                if (cls < 0 || cls >= classes) throw new IllegalArgumentException("invalid character class");
            }
            for (byte target : next) {
                if (target < 0 || target >= states) throw new IllegalArgumentException("invalid DFA target");
            }
            this.states = states;
            this.start = start;
            this.acceptMask = acceptMask;
            this.classes = classes;
            this.latin1Class = latin1Class;
            this.otherClass = otherClass;
            this.next = next;
            long id = 0;
            for (int q = 0; q < states; q++) id |= (long) q << (4 * q);
            this.identity = id;
        }

        @Override
        public int width() { return 1; }

        @Override
        public void identity(long[] out, int at) { out[at] = identity; }

        @Override
        public void leaf(CharSequence t, int from, int to, long[] out, int at) {
            long transfer = identity;
            for (int i = from; i < to; i++) {
                char c = t.charAt(i);
                int cls = c < 256 ? latin1Class[c] : otherClass;
                long stepped = 0;
                for (int q = 0; q < states; q++) {
                    stepped |= (long) next[apply(transfer, q) * classes + cls] << (4 * q);
                }
                transfer = stepped;
            }
            out[at] = transfer;
        }

        @Override
        public void combine(long[] l, int la, long[] r, int ra, long[] out, int at) {
            long composed = 0;
            for (int q = 0; q < states; q++) composed |= (long) apply(r[ra], apply(l[la], q)) << (4 * q);
            out[at] = composed;
        }

        /** The state reached from {@code state} under {@code transfer}. */
        public int apply(long transfer, int state) { return (int) ((transfer >>> (4 * state)) & 15); }

        /** True when the text summarized at {@code summary[at]} leads the start state to an accepting state. */
        public boolean accepts(long[] summary, int at) {
            return ((acceptMask >>> apply(summary[at], start)) & 1) != 0;
        }
    }
}
