// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
//
// Lineage (TARGET_OWNED_PORT, leaf R6-AUTOMATON, run 20261017-synexia-to-public):
//   Apache Lucene 9.10.0 org.apache.lucene.util.automaton.RunAutomaton (git blob d4b2a2ec41e14c3e49b2aa92ddb815d013952754)
//   and CharacterRunAutomaton (git blob f65ccd29ac77560f5bba69135c1c963faef47f18), vendored unchanged in M3jdk21 at
//   m3/vendor/third_party/lucene-core (BLOB_EQUAL to upstream tag 695c0ac84508438302cd346a812cfa2fdc5a10df),
//   Apache License, Version 2.0. Modifications: package jdk.internal.mindex; no org.apache.lucene imports; UTF-16
//   code-unit alphabet instead of code points; state cap; FixedBitSet -> long[] accept lane; Accountable dropped; added
//   the per-chunk transfer element and its composition (design REGEX_PRECOMPUTE_DESIGN.md section 2b), presence masks for
//   single-class separators, and forward/backward runners over M3ChunkCursor (cursor contract learned from
//   helix-editor/regex-cursor fd4c51c7, MIT OR Apache-2.0, pattern only).
//
//   RunAutomaton derives from dk.brics.automaton and carries its notice:
//
//   dk.brics.automaton
//   Copyright (c) 2001-2009 Anders Moeller
//   All rights reserved.
//
//   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//   following conditions are met:
//   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
//      following disclaimer.
//   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//      following disclaimer in the documentation and/or other materials provided with the distribution.
//   3. The name of the author may not be used to endorse or promote products derived from this software without
//      specific prior written permission.
//
//   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//   THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
//   POSSIBILITY OF SUCH DAMAGE.
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Table-driven minimized DFA over UTF-16 code units: {@code transitions[state * classes + class(unit)]}, -1 = dead.
 * Classes come from the automaton's interval start points; units below 256 use a direct class map, others a binary
 * search. Results are candidates: {@link #TRUE}, {@link #FALSE} or {@link #UNSUPPORTED} (input holds an unpaired
 * surrogate, which java.util.regex reads as one code point and this table does not). Internal; not a public JDK API.
 */
public final class M3RunTable {
    /** Largest admitted state count (byte-packable transfer rows); larger DFAs descend the regex ladder. */
    public static final int MAX_STATES = 255;
    /** Candidate result: no match. */
    public static final int FALSE = 0;
    /** Candidate result: match. */
    public static final int TRUE = 1;
    /** Candidate result: this table cannot decide; descend the ladder. */
    public static final int UNSUPPORTED = -1;
    /** Shape: general DFA. */
    public static final int SHAPE_GENERAL = 0;
    /** Shape: exactly one unit from a class (",", "[,;]", "\p{Punct}"). */
    public static final int SHAPE_UNIT_CLASS = 1;
    /** Shape: one or more units from a class ("\s+"). */
    public static final int SHAPE_UNIT_CLASS_RUN = 2;

    static final int STARTS_LOW = 1;
    static final int ENDS_HIGH = 2;
    static final int UNPAIRED = 4;
    static final int HAS_SURROGATE = 8;

    private final int size;
    private final int classes;
    private final int[] points;
    private final int[] classmap;
    private final int[] transitions;
    private final long[] accept;
    private final int shape;

    private M3RunTable(M3Automaton a) {
        points = a.getStartPoints(M3AutomatonOps.MIN_LABEL, M3AutomatonOps.MAX_LABEL);
        classes = points.length;
        size = Math.max(1, a.getNumStates());
        accept = new long[(size + 63) >>> 6];
        transitions = new int[size * classes];
        Arrays.fill(transitions, -1);
        M3Automaton.Transition t = new M3Automaton.Transition();
        for (int n = 0; n < a.getNumStates(); n++) {
            if (a.isAccept(n)) {
                accept[n >>> 6] |= 1L << n;
            }
            t.source = n;
            t.transitionUpto = -1;
            for (int c = 0; c < classes; c++) {
                transitions[n * classes + c] = a.next(t, points[c]);
            }
        }
        classmap = new int[256];
        int i = 0;
        for (int j = 0; j < classmap.length; j++) {
            if (i + 1 < classes && j == points[i + 1]) {
                i++;
            }
            classmap[j] = i;
        }
        shape = detectShape();
    }

    /**
     * Builds the table from any automaton (determinized and minimized here). Throws {@link M3AutomatonOps.TooComplex}
     * past the work limit and {@link StateCap} above {@link #MAX_STATES} minimized states.
     */
    public static M3RunTable of(M3Automaton a, int workLimit) {
        M3Automaton min = M3AutomatonOps.minimize(a, workLimit);
        if (min.getNumStates() > MAX_STATES) {
            throw new StateCap(min.getNumStates());
        }
        return new M3RunTable(min);
    }

    /** Thrown when the minimized DFA exceeds {@link #MAX_STATES}; the regex ladder descends. */
    public static final class StateCap extends RuntimeException {
        private static final long serialVersionUID = 1L;

        StateCap(int states) {
            super("STATE_CAP " + states + " > " + MAX_STATES, null, false, false);
        }
    }

    /** Number of states (at least 1; state 0 is initial). */
    public int size() {
        return size;
    }

    /** Number of unit classes. */
    public int classCount() {
        return classes;
    }

    /** One of {@link #SHAPE_GENERAL}, {@link #SHAPE_UNIT_CLASS}, {@link #SHAPE_UNIT_CLASS_RUN}. */
    public int shape() {
        return shape;
    }

    /** True if the state accepts. */
    public boolean isAccept(int state) {
        return state >= 0 && (accept[state >>> 6] & (1L << state)) != 0;
    }

    /** Class of a UTF-16 unit. */
    public int classOf(char unit) {
        if (unit < 256) {
            return classmap[unit];
        }
        int lo = 0;
        int hi = classes;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (points[mid] > unit) {
                hi = mid;
            } else if (points[mid] < unit) {
                lo = mid;
            } else {
                return mid;
            }
        }
        return lo;
    }

    /** Next state for a unit, or -1 (dead). */
    public int step(int state, char unit) {
        return state < 0 ? -1 : transitions[state * classes + classOf(unit)];
    }

    /** Whole-text run from state 0. */
    public int matches(CharSequence text) {
        int flags = surrogateFlags(text, 0, text.length());
        if ((flags & (STARTS_LOW | ENDS_HIGH | UNPAIRED)) != 0) {
            return UNSUPPORTED;
        }
        int q = 0;
        for (int i = 0; i < text.length() && q >= 0; i++) {
            q = transitions[q * classes + classOf(text.charAt(i))];
        }
        return isAccept(q) ? TRUE : FALSE;
    }

    /**
     * Accept profile of a run from state 0: bit k of the result is set when the run is in an accept state after k
     * units (k = 0..length). Returns null when the text holds an unpaired surrogate.
     */
    public long[] acceptProfile(CharSequence text) {
        int n = text.length();
        int flags = surrogateFlags(text, 0, n);
        if ((flags & (STARTS_LOW | ENDS_HIGH | UNPAIRED)) != 0) {
            return null;
        }
        long[] out = new long[(n >>> 6) + 1];
        int q = 0;
        if (isAccept(q)) {
            out[0] |= 1L;
        }
        for (int i = 0; i < n && q >= 0; i++) {
            q = transitions[q * classes + classOf(text.charAt(i))];
            if (isAccept(q)) {
                out[(i + 1) >>> 6] |= 1L << (i + 1);
            }
        }
        return out;
    }

    /** Forward whole-text run over a cursor positioned at its first chunk. */
    public int matches(M3ChunkCursor cursor) {
        int q = 0;
        boolean pendingHigh = false;
        while (true) {
            int len = cursor.chunkLength();
            for (int i = 0; i < len; i++) {
                char c = cursor.unit(i);
                if (Character.isHighSurrogate(c)) {
                    if (pendingHigh) {
                        return UNSUPPORTED;
                    }
                    pendingHigh = true;
                } else if (Character.isLowSurrogate(c)) {
                    if (!pendingHigh) {
                        return UNSUPPORTED;
                    }
                    pendingHigh = false;
                } else if (pendingHigh) {
                    return UNSUPPORTED;
                }
                if (q >= 0) {
                    q = transitions[q * classes + classOf(c)];
                }
            }
            if (!cursor.advance()) {
                break;
            }
        }
        if (pendingHigh) {
            return UNSUPPORTED;
        }
        return isAccept(q) ? TRUE : FALSE;
    }

    /**
     * Backward whole-text run over a cursor positioned at its last chunk; use on a table built from the reversed
     * automaton ({@link M3AutomatonOps#reverse}), so the verdict equals the forward table's.
     */
    public int matchesBackward(M3ChunkCursor cursor) {
        int q = 0;
        boolean pendingLow = false;
        while (true) {
            for (int i = cursor.chunkLength() - 1; i >= 0; i--) {
                char c = cursor.unit(i);
                if (Character.isLowSurrogate(c)) {
                    if (pendingLow) {
                        return UNSUPPORTED;
                    }
                    pendingLow = true;
                } else if (Character.isHighSurrogate(c)) {
                    if (!pendingLow) {
                        return UNSUPPORTED;
                    }
                    pendingLow = false;
                } else if (pendingLow) {
                    return UNSUPPORTED;
                }
                if (q >= 0) {
                    q = transitions[q * classes + classOf(c)];
                }
            }
            if (!cursor.backtrack()) {
                break;
            }
        }
        if (pendingLow) {
            return UNSUPPORTED;
        }
        return isAccept(q) ? TRUE : FALSE;
    }

    /**
     * Presence mask of single-unit matches: bit i set when the unit at {@code from + i} alone is accepted from state 0.
     * For {@link #SHAPE_UNIT_CLASS} and {@link #SHAPE_UNIT_CLASS_RUN} tables this is the class-presence bitmask of the
     * span (the per-atom boundary mask of leaf LEXICONS).
     */
    public long[] presence(CharSequence text, int from, int to) {
        Objects.checkFromToIndex(from, to, text.length());
        long[] out = new long[((to - from) >>> 6) + 1];
        for (int i = from; i < to; i++) {
            if (isAccept(transitions[classOf(text.charAt(i))])) {
                int k = i - from;
                out[k >>> 6] |= 1L << k;
            }
        }
        return out;
    }

    /** Transfer element of the empty text. */
    public Element identity() {
        int[] next = new int[size];
        int[] first = new int[size];
        for (int q = 0; q < size; q++) {
            next[q] = q;
            first[q] = isAccept(q) ? 0 : -1;
        }
        return new Element(this, next, first, 0, 0);
    }

    /** Transfer element of {@code text[from, to)} (one atom payload window). */
    public Element transfer(CharSequence text, int from, int to) {
        Objects.checkFromToIndex(from, to, text.length());
        if (from == to) {
            return identity();
        }
        Lanes lanes = new Lanes(size);
        for (int q = 0; q < size; q++) {
            lanes.init(q, isAccept(q));
        }
        for (int i = from; i < to && lanes.count > 0; i++) {
            lanes.step(this, classOf(text.charAt(i)), i - from + 1);
        }
        return new Element(this, lanes.finish(), lanes.first, to - from, surrogateFlags(text, from, to));
    }

    /** Transfer element of the cursor's current chunk. */
    public Element transfer(M3ChunkCursor cursor) {
        int len = cursor.chunkLength();
        if (len == 0) {
            return identity();
        }
        Lanes lanes = new Lanes(size);
        for (int q = 0; q < size; q++) {
            lanes.init(q, isAccept(q));
        }
        int flags = 0;
        for (int i = 0; i < len; i++) {
            char c = cursor.unit(i);
            if (lanes.count > 0) {
                lanes.step(this, classOf(c), i + 1);
            }
            if (Character.isSurrogate(c)) {
                flags |= HAS_SURROGATE;
                if (Character.isHighSurrogate(c)) {
                    if (i + 1 == len) {
                        flags |= ENDS_HIGH;
                    } else if (!Character.isLowSurrogate(cursor.unit(i + 1))) {
                        flags |= UNPAIRED;
                    }
                } else if (i == 0) {
                    flags |= STARTS_LOW;
                } else if (!Character.isHighSurrogate(cursor.unit(i - 1))) {
                    flags |= UNPAIRED;
                }
            }
        }
        return new Element(this, lanes.finish(), lanes.first, len, flags);
    }

    /** Element of u followed by v (both from this table); associative, {@link #identity()} is neutral. */
    public Element compose(Element u, Element v) {
        if (u.table != this || v.table != this) {
            throw new IllegalArgumentException("element of another table");
        }
        if (u.length == 0) {
            return v;
        }
        if (v.length == 0) {
            return u;
        }
        int[] next = new int[size];
        int[] first = new int[size];
        for (int q = 0; q < size; q++) {
            int mid = u.next[q];
            next[q] = mid < 0 ? -1 : v.next[mid];
            if (u.first[q] >= 0) {
                first[q] = u.first[q];
            } else {
                first[q] = (mid < 0 || v.first[mid] < 0) ? -1 : u.length + v.first[mid];
            }
        }
        boolean seamBad = ((u.flags & ENDS_HIGH) != 0) != ((v.flags & STARTS_LOW) != 0);
        int flags = (u.flags & STARTS_LOW) | (v.flags & ENDS_HIGH)
                | ((u.flags | v.flags) & (UNPAIRED | HAS_SURROGATE)) | (seamBad ? UNPAIRED : 0);
        return new Element(this, next, first, u.length + v.length, flags);
    }

    /**
     * Word-id transfer rows for a lexicon vocabulary (R6_DESIGN section 4: sidecar M3LEXRX1 beside M3LEX001, rows deduplicated
     * by DFA class string, u16 row id per word id). Layout is owned jointly with leaf LEXICONS; not received yet.
     */
    public byte[] wordRowImage(CharSequence[] vocabulary) {
        throw new M3NotYetReceived("M3RunTable.wordRowImage R6_DESIGN section 4 (M3LEXRX1 sidecar, leaf LEXICONS layout)");
    }

    /** Whole-text verdict of an element: accept of T(0), or {@link #UNSUPPORTED} for unpaired surrogates. */
    public int verdict(Element e) {
        if ((e.flags & (STARTS_LOW | ENDS_HIGH | UNPAIRED)) != 0) {
            return UNSUPPORTED;
        }
        return isAccept(e.next[0]) ? TRUE : FALSE;
    }

    static int surrogateFlags(CharSequence text, int from, int to) {
        int flags = 0;
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            if (!Character.isSurrogate(c)) {
                continue;
            }
            flags |= HAS_SURROGATE;
            if (Character.isHighSurrogate(c)) {
                if (i + 1 == to) {
                    flags |= ENDS_HIGH;
                } else if (Character.isLowSurrogate(text.charAt(i + 1))) {
                    i++;
                } else {
                    flags |= UNPAIRED;
                }
            } else if (i == from) {
                flags |= STARTS_LOW;
            } else {
                flags |= UNPAIRED;
            }
        }
        return flags;
    }

    private int detectShape() {
        if (size != 2 || isAccept(0) || !isAccept(1)) {
            return SHAPE_GENERAL;
        }
        boolean run = true;
        boolean single = true;
        for (int c = 0; c < classes; c++) {
            int from0 = transitions[c];
            int from1 = transitions[classes + c];
            if (from0 == 0 || from1 == 0) {
                return SHAPE_GENERAL;
            }
            if (from1 != -1) {
                single = false;
            }
            if (from0 != from1) {
                run = false;
            }
        }
        return single ? SHAPE_UNIT_CLASS : run ? SHAPE_UNIT_CLASS_RUN : SHAPE_GENERAL;
    }

    /**
     * Per-(table, atom) transfer element (REGEX_PRECOMPUTE_DESIGN.md section 2b): {@code next[q]} is the state after the atom
     * from q (-1 dead), {@code first[q]} the first unit offset at which a run from q is accepting (-1 none). A state
     * table, never a spelling store.
     */
    public static final class Element {
        final M3RunTable table;
        final int[] next;
        final int[] first;
        final int length;
        final int flags;

        Element(M3RunTable table, int[] next, int[] first, int length, int flags) {
            this.table = table;
            this.next = next;
            this.first = first;
            this.length = length;
            this.flags = flags;
        }

        /** Units covered. */
        public int length() {
            return length;
        }

        /** State after the covered units from q, or -1. */
        public int next(int q) {
            return next[q];
        }

        /** First accepting offset of a run from q, or -1. */
        public int firstAccept(int q) {
            return first[q];
        }

        /** True when the covered units hold a surrogate unit. */
        public boolean hasSurrogate() {
            return (flags & HAS_SURROGATE) != 0;
        }
    }

    /**
     * Distinct-state lanes for transfer construction: origins whose runs reach the same state merge and stay merged, so
     * the cost is O(units x distinct live states). Each lane keeps its member origins and the origins still waiting for
     * their first accepting offset as linked int lists (no per-origin objects).
     */
    private static final class Lanes {
        int count;
        int[] state;
        int[] memHead;
        int[] memTail;
        int[] penHead;
        int[] penTail;
        int[] state2;
        int[] memHead2;
        int[] memTail2;
        int[] penHead2;
        int[] penTail2;
        final int[] memLink;
        final int[] penLink;
        final int[] first;
        final int[] stampVersion;
        final int[] stampLane;
        int version;

        Lanes(int n) {
            state = new int[n];
            memHead = new int[n];
            memTail = new int[n];
            penHead = new int[n];
            penTail = new int[n];
            state2 = new int[n];
            memHead2 = new int[n];
            memTail2 = new int[n];
            penHead2 = new int[n];
            penTail2 = new int[n];
            memLink = new int[n];
            penLink = new int[n];
            first = new int[n];
            stampVersion = new int[n];
            stampLane = new int[n];
        }

        void init(int q, boolean accepting) {
            state[count] = q;
            memHead[count] = q;
            memTail[count] = q;
            memLink[q] = -1;
            penLink[q] = -1;
            if (accepting) {
                first[q] = 0;
                penHead[count] = -1;
                penTail[count] = -1;
            } else {
                first[q] = -1;
                penHead[count] = q;
                penTail[count] = q;
            }
            count++;
        }

        void step(M3RunTable table, int cls, int offset) {
            version++;
            int n = 0;
            for (int lane = 0; lane < count; lane++) {
                int ns = table.transitions[state[lane] * table.classes + cls];
                if (ns < 0) {
                    continue;
                }
                if (stampVersion[ns] != version) {
                    stampVersion[ns] = version;
                    stampLane[ns] = n;
                    state2[n] = ns;
                    memHead2[n] = memHead[lane];
                    memTail2[n] = memTail[lane];
                    penHead2[n] = penHead[lane];
                    penTail2[n] = penTail[lane];
                    n++;
                    continue;
                }
                int target = stampLane[ns];
                memLink[memTail2[target]] = memHead[lane];
                memTail2[target] = memTail[lane];
                if (penHead[lane] >= 0) {
                    if (penHead2[target] < 0) {
                        penHead2[target] = penHead[lane];
                    } else {
                        penLink[penTail2[target]] = penHead[lane];
                    }
                    penTail2[target] = penTail[lane];
                }
            }
            swap();
            count = n;
            for (int lane = 0; lane < count; lane++) {
                if (penHead[lane] >= 0 && table.isAccept(state[lane])) {
                    for (int o = penHead[lane]; o >= 0; o = penLink[o]) {
                        first[o] = offset;
                    }
                    penHead[lane] = -1;
                    penTail[lane] = -1;
                }
            }
        }

        private void swap() {
            int[] t = state; state = state2; state2 = t;
            t = memHead; memHead = memHead2; memHead2 = t;
            t = memTail; memTail = memTail2; memTail2 = t;
            t = penHead; penHead = penHead2; penHead2 = t;
            t = penTail; penTail = penTail2; penTail2 = t;
        }

        int[] finish() {
            int[] next = new int[first.length];
            Arrays.fill(next, -1);
            for (int lane = 0; lane < count; lane++) {
                for (int o = memHead[lane]; o >= 0; o = memLink[o]) {
                    next[o] = state[lane];
                }
            }
            return next;
        }
    }
}
