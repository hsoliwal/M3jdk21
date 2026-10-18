// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
//
// Lineage (TARGET_OWNED_PORT, leaf R6-AUTOMATON, run 20261017-synexia-to-public):
//   Apache Lucene 9.10.0 org.apache.lucene.util.automaton.Operations (git blob 9ebe5b998037040f349e26b384c364822a37bfea),
//   MinimizationOperations (ffd6edd44e1b7d84d53530c227da9dfbef3d43fb), Automata (cf1581fc9127c41c83b9541b15a2f1f38219c944),
//   StateSet (6d4a7ea3b83305cf61014f193d1aebbbf015bbec), IntSet (6c64629c64fcc9ca3ddf4b1077c7d4c118d19a3c),
//   FrozenIntSet (1e80171d510708572452bf6272394d1dd063fdfb) and TooComplexToDeterminizeException
//   (0a3da3c01e912077ed198bcbc415ee365e686ea6), vendored unchanged in M3jdk21 at m3/vendor/third_party/lucene-core
//   (BLOB_EQUAL to upstream tag 695c0ac84508438302cd346a812cfa2fdc5a10df), Apache License, Version 2.0.
//   Modifications: package jdk.internal.mindex; no org.apache.lucene imports; alphabet bounded to UTF-16 code units
//   [0, MAX_LABEL] instead of code points; hppc IntIntHashMap/BitMixer replaced by dense int count/position lanes and a
//   local fmix32; FixedBitSet -> java.util.BitSet; byte/BytesRef/IntsRef operations, intersection, minus, sameLanguage,
//   subsetOf, finite-string and topological operations dropped; reverse without the initial-state out parameter.
//
//   Operations, MinimizationOperations and Automata derive from dk.brics.automaton and carry its notice:
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Automaton algebra over the UTF-16 code-unit alphabet: construction, union/concatenation/repetition, powerset
 * determinization with a work limit, Hopcroft minimization, reversal and complement. Internal; not a public JDK API.
 * All operations return new automata and never mutate their inputs.
 */
public final class M3AutomatonOps {
    /** Smallest label of the UTF-16 code-unit alphabet. */
    public static final int MIN_LABEL = 0;
    /** Largest label of the UTF-16 code-unit alphabet. */
    public static final int MAX_LABEL = 0xFFFF;
    /** Default determinization effort (Lucene DEFAULT_DETERMINIZE_WORK_LIMIT). */
    public static final int DEFAULT_WORK_LIMIT = 10_000;

    private M3AutomatonOps() { }

    /** Thrown when determinization would exceed its work limit; the regex ladder descends. */
    public static final class TooComplex extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /** Creates the refusal for an automaton of the given size. */
        public TooComplex(int states, int transitions, int workLimit) {
            super("Determinizing automaton with " + states + " states and " + transitions
                    + " transitions would require more than " + workLimit + " effort.");
        }
    }

    // ---- construction (Automata) ----

    /** Automaton accepting nothing. */
    public static M3Automaton makeEmpty() {
        M3Automaton a = new M3Automaton();
        a.finishState();
        return a;
    }

    /** Automaton accepting only the empty string. */
    public static M3Automaton makeEmptyString() {
        M3Automaton a = new M3Automaton();
        a.createState();
        a.setAccept(0, true);
        return a;
    }

    /** Automaton accepting every unit string. */
    public static M3Automaton makeAnyString() {
        M3Automaton a = new M3Automaton();
        int s = a.createState();
        a.setAccept(s, true);
        a.addTransition(s, s, MIN_LABEL, MAX_LABEL);
        a.finishState();
        return a;
    }

    /** Automaton accepting one unit in [min, max]. */
    public static M3Automaton makeUnitRange(int min, int max) {
        if (min > max) {
            return makeEmpty();
        }
        M3Automaton a = new M3Automaton();
        int s1 = a.createState();
        int s2 = a.createState();
        a.setAccept(s2, true);
        a.addTransition(s1, s2, min, max);
        a.finishState();
        return a;
    }

    /** Automaton accepting exactly the given unit sequence. */
    public static M3Automaton makeUnits(CharSequence units) {
        M3Automaton a = new M3Automaton();
        int last = a.createState();
        for (int i = 0; i < units.length(); i++) {
            int state = a.createState();
            a.addTransition(last, state, units.charAt(i));
            last = state;
        }
        a.setAccept(last, true);
        a.finishState();
        return a;
    }

    // ---- algebra (Operations) ----

    /** Concatenation of the languages, in order. */
    public static M3Automaton concatenate(M3Automaton... l) {
        M3Automaton result = new M3Automaton();
        for (M3Automaton a : l) {
            if (a.getNumStates() == 0) {
                result.finishState();
                return result;
            }
            for (int s = 0; s < a.getNumStates(); s++) {
                result.createState();
            }
        }
        int stateOffset = 0;
        M3Automaton.Transition t = new M3Automaton.Transition();
        for (int i = 0; i < l.length; i++) {
            M3Automaton a = l[i];
            int numStates = a.getNumStates();
            M3Automaton nextA = (i == l.length - 1) ? null : l[i + 1];
            for (int s = 0; s < numStates; s++) {
                int numTransitions = a.initTransition(s, t);
                for (int j = 0; j < numTransitions; j++) {
                    a.getNextTransition(t);
                    result.addTransition(stateOffset + s, stateOffset + t.dest, t.min, t.max);
                }
                if (a.isAccept(s)) {
                    M3Automaton followA = nextA;
                    int followOffset = stateOffset;
                    int upto = i + 1;
                    while (true) {
                        if (followA != null) {
                            numTransitions = followA.initTransition(0, t);
                            for (int j = 0; j < numTransitions; j++) {
                                followA.getNextTransition(t);
                                result.addTransition(stateOffset + s, followOffset + numStates + t.dest, t.min, t.max);
                            }
                            if (followA.isAccept(0)) {
                                followOffset += followA.getNumStates();
                                followA = (upto == l.length - 1) ? null : l[upto + 1];
                                upto++;
                            } else {
                                break;
                            }
                        } else {
                            result.setAccept(stateOffset + s, true);
                            break;
                        }
                    }
                }
            }
            stateOffset += numStates;
        }
        if (result.getNumStates() == 0) {
            result.createState();
        }
        result.finishState();
        return result;
    }

    /** Union of the languages. */
    public static M3Automaton union(M3Automaton... l) {
        M3Automaton result = new M3Automaton();
        result.createState();
        for (M3Automaton a : l) {
            result.copy(a);
        }
        int stateOffset = 1;
        for (M3Automaton a : l) {
            if (a.getNumStates() == 0) {
                continue;
            }
            result.addEpsilon(0, stateOffset);
            stateOffset += a.getNumStates();
        }
        result.finishState();
        return removeDeadStates(result);
    }

    /** The language plus the empty string. */
    public static M3Automaton optional(M3Automaton a) {
        M3Automaton result = new M3Automaton();
        result.createState();
        result.setAccept(0, true);
        if (a.getNumStates() > 0) {
            result.copy(a);
            result.addEpsilon(0, 1);
        }
        result.finishState();
        return result;
    }

    /** Kleene star. */
    public static M3Automaton repeat(M3Automaton a) {
        if (a.getNumStates() == 0) {
            return a;
        }
        M3Automaton.Builder builder = new M3Automaton.Builder();
        builder.createState();
        builder.setAccept(0, true);
        builder.copy(a);
        M3Automaton.Transition t = new M3Automaton.Transition();
        int count = a.initTransition(0, t);
        for (int i = 0; i < count; i++) {
            a.getNextTransition(t);
            builder.addTransition(0, t.dest + 1, t.min, t.max);
        }
        int numStates = a.getNumStates();
        for (int s = 0; s < numStates; s++) {
            if (a.isAccept(s)) {
                count = a.initTransition(0, t);
                for (int i = 0; i < count; i++) {
                    a.getNextTransition(t);
                    builder.addTransition(s + 1, t.dest + 1, t.min, t.max);
                }
            }
        }
        return builder.finish();
    }

    /** {@code min} or more repetitions. */
    public static M3Automaton repeat(M3Automaton a, int min) {
        if (min == 0) {
            return repeat(a);
        }
        M3Automaton[] as = new M3Automaton[min + 1];
        Arrays.fill(as, 0, min, a);
        as[min] = repeat(a);
        return concatenate(as);
    }

    /** Between {@code min} and {@code max} repetitions, inclusive. */
    public static M3Automaton repeat(M3Automaton a, int min, int max) {
        if (min > max) {
            return makeEmpty();
        }
        M3Automaton b;
        if (min == 0) {
            b = makeEmptyString();
        } else if (min == 1) {
            b = new M3Automaton();
            b.copy(a);
        } else {
            M3Automaton[] as = new M3Automaton[min];
            Arrays.fill(as, a);
            b = concatenate(as);
        }
        int[] prevAccept = acceptList(b, 0);
        M3Automaton.Builder builder = new M3Automaton.Builder();
        builder.copy(b);
        for (int i = min; i < max; i++) {
            int numStates = builder.getNumStates();
            builder.copy(a);
            for (int s : prevAccept) {
                builder.addEpsilon(s, numStates);
            }
            prevAccept = acceptList(a, numStates);
        }
        return builder.finish();
    }

    private static int[] acceptList(M3Automaton a, int offset) {
        BitSet accept = a.acceptStates();
        int[] out = new int[accept.cardinality()];
        int n = 0;
        for (int s = accept.nextSetBit(0); s >= 0 && s < a.getNumStates(); s = accept.nextSetBit(s + 1)) {
            out[n++] = offset + s;
        }
        return Arrays.copyOf(out, n);
    }

    /** Deterministic complement over the unit alphabet. */
    public static M3Automaton complement(M3Automaton a, int workLimit) {
        a = totalize(determinize(a, workLimit));
        for (int p = 0; p < a.getNumStates(); p++) {
            a.setAccept(p, !a.isAccept(p));
        }
        return removeDeadStates(a);
    }

    /** True if the automaton accepts no string. */
    public static boolean isEmpty(M3Automaton a) {
        if (a.getNumStates() == 0) {
            return true;
        }
        if (!a.isAccept(0) && a.getNumTransitions(0) == 0) {
            return true;
        }
        if (a.isAccept(0)) {
            return false;
        }
        ArrayDeque<Integer> workList = new ArrayDeque<>();
        BitSet seen = new BitSet(a.getNumStates());
        workList.add(0);
        seen.set(0);
        M3Automaton.Transition t = new M3Automaton.Transition();
        while (!workList.isEmpty()) {
            int state = workList.removeFirst();
            if (a.isAccept(state)) {
                return false;
            }
            int count = a.initTransition(state, t);
            for (int i = 0; i < count; i++) {
                a.getNextTransition(t);
                if (!seen.get(t.dest)) {
                    workList.add(t.dest);
                    seen.set(t.dest);
                }
            }
        }
        return true;
    }

    /** True if the (minimized) automaton accepts every unit string. */
    public static boolean isTotal(M3Automaton a) {
        if (a.getNumStates() > 0 && a.isAccept(0) && a.getNumTransitions(0) == 1) {
            M3Automaton.Transition t = new M3Automaton.Transition();
            a.getTransition(0, 0, t);
            return t.dest == 0 && t.min == MIN_LABEL && t.max == MAX_LABEL;
        }
        return false;
    }

    /** Reversed language (not deterministic in general). */
    public static M3Automaton reverse(M3Automaton a) {
        if (isEmpty(a)) {
            return new M3Automaton();
        }
        int numStates = a.getNumStates();
        M3Automaton.Builder builder = new M3Automaton.Builder();
        builder.createState();
        for (int s = 0; s < numStates; s++) {
            builder.createState();
        }
        builder.setAccept(1, true);
        M3Automaton.Transition t = new M3Automaton.Transition();
        for (int s = 0; s < numStates; s++) {
            int numTransitions = a.initTransition(s, t);
            for (int i = 0; i < numTransitions; i++) {
                a.getNextTransition(t);
                builder.addTransition(t.dest + 1, s + 1, t.min, t.max);
            }
        }
        M3Automaton result = builder.finish();
        BitSet accept = a.acceptStates();
        for (int s = accept.nextSetBit(0); s >= 0 && s < numStates; s = accept.nextSetBit(s + 1)) {
            result.addEpsilon(0, s + 1);
        }
        result.finishState();
        return result;
    }

    /** Adds a dead state so every state has a transition on every unit. */
    static M3Automaton totalize(M3Automaton a) {
        M3Automaton result = new M3Automaton();
        int numStates = a.getNumStates();
        for (int i = 0; i < numStates; i++) {
            result.createState();
            result.setAccept(i, a.isAccept(i));
        }
        int deadState = result.createState();
        result.addTransition(deadState, deadState, MIN_LABEL, MAX_LABEL);
        M3Automaton.Transition t = new M3Automaton.Transition();
        for (int i = 0; i < numStates; i++) {
            int maxi = MIN_LABEL;
            int count = a.initTransition(i, t);
            for (int j = 0; j < count; j++) {
                a.getNextTransition(t);
                result.addTransition(i, t.dest, t.min, t.max);
                if (t.min > maxi) {
                    result.addTransition(i, deadState, maxi, t.min - 1);
                }
                if (t.max + 1 > maxi) {
                    maxi = t.max + 1;
                }
            }
            if (maxi <= MAX_LABEL) {
                result.addTransition(i, deadState, maxi, MAX_LABEL);
            }
        }
        result.finishState();
        return result;
    }

    /** Removes states not reachable from the initial state or unable to reach an accept state. */
    public static M3Automaton removeDeadStates(M3Automaton a) {
        int numStates = a.getNumStates();
        BitSet live = liveFromInitial(a);
        live.and(liveToAccept(a));
        int[] map = new int[numStates];
        M3Automaton result = new M3Automaton();
        for (int i = 0; i < numStates; i++) {
            if (live.get(i)) {
                map[i] = result.createState();
                result.setAccept(map[i], a.isAccept(i));
            }
        }
        M3Automaton.Transition t = new M3Automaton.Transition();
        for (int i = 0; i < numStates; i++) {
            if (live.get(i)) {
                int numTransitions = a.initTransition(i, t);
                for (int j = 0; j < numTransitions; j++) {
                    a.getNextTransition(t);
                    if (live.get(t.dest)) {
                        result.addTransition(map[i], map[t.dest], t.min, t.max);
                    }
                }
            }
        }
        result.finishState();
        return result;
    }

    private static BitSet liveFromInitial(M3Automaton a) {
        int numStates = a.getNumStates();
        BitSet live = new BitSet(numStates);
        if (numStates == 0) {
            return live;
        }
        ArrayDeque<Integer> workList = new ArrayDeque<>();
        live.set(0);
        workList.add(0);
        M3Automaton.Transition t = new M3Automaton.Transition();
        while (!workList.isEmpty()) {
            int s = workList.removeFirst();
            int count = a.initTransition(s, t);
            for (int i = 0; i < count; i++) {
                a.getNextTransition(t);
                if (!live.get(t.dest)) {
                    live.set(t.dest);
                    workList.add(t.dest);
                }
            }
        }
        return live;
    }

    private static BitSet liveToAccept(M3Automaton a) {
        M3Automaton.Builder builder = new M3Automaton.Builder();
        M3Automaton.Transition t = new M3Automaton.Transition();
        int numStates = a.getNumStates();
        for (int s = 0; s < numStates; s++) {
            builder.createState();
        }
        for (int s = 0; s < numStates; s++) {
            int count = a.initTransition(s, t);
            for (int i = 0; i < count; i++) {
                a.getNextTransition(t);
                builder.addTransition(t.dest, s, t.min, t.max);
            }
        }
        M3Automaton a2 = builder.finish();
        ArrayDeque<Integer> workList = new ArrayDeque<>();
        BitSet live = new BitSet(numStates);
        BitSet accept = a.acceptStates();
        for (int s = accept.nextSetBit(0); s >= 0 && s < numStates; s = accept.nextSetBit(s + 1)) {
            live.set(s);
            workList.add(s);
        }
        while (!workList.isEmpty()) {
            int s = workList.removeFirst();
            int count = a2.initTransition(s, t);
            for (int i = 0; i < count; i++) {
                a2.getNextTransition(t);
                if (!live.get(t.dest)) {
                    live.set(t.dest);
                    workList.add(t.dest);
                }
            }
        }
        return live;
    }

    // ---- determinization (Operations.determinize, StateSet, FrozenIntSet) ----

    /** Powerset determinization; throws {@link TooComplex} past {@code workLimit * 10} effort. */
    public static M3Automaton determinize(M3Automaton a, int workLimit) {
        if (a.isDeterministic() || a.getNumStates() <= 1) {
            return a;
        }
        M3Automaton.Builder b = new M3Automaton.Builder();
        FrozenSet initial = new FrozenSet(new int[] {0}, mix(0) + 1, 0);
        b.createState();
        ArrayDeque<FrozenSet> worklist = new ArrayDeque<>();
        Map<StateKey, Integer> newstate = new HashMap<>();
        worklist.add(initial);
        b.setAccept(0, a.isAccept(0));
        newstate.put(initial, 0);
        PointTransitionSet points = new PointTransitionSet();
        CountingSet statesSet = new CountingSet(a.getNumStates());
        M3Automaton.Transition t = new M3Automaton.Transition();
        long effortSpent = 0;
        long effortLimit = workLimit * 10L;
        while (!worklist.isEmpty()) {
            FrozenSet s = worklist.removeFirst();
            effortSpent += s.values.length;
            if (effortSpent >= effortLimit) {
                throw new TooComplex(a.getNumStates(), a.getNumTransitions(), workLimit);
            }
            for (int s0 : s.values) {
                int numTransitions = a.initTransition(s0, t);
                for (int j = 0; j < numTransitions; j++) {
                    a.getNextTransition(t);
                    points.add(t);
                }
            }
            if (points.count == 0) {
                continue;
            }
            points.sort();
            int lastPoint = -1;
            int accCount = 0;
            int r = s.state;
            for (int i = 0; i < points.count; i++) {
                int point = points.points[i].point;
                if (statesSet.size() > 0) {
                    Integer q = newstate.get(statesSet);
                    if (q == null) {
                        q = b.createState();
                        FrozenSet p = statesSet.freeze(q);
                        worklist.add(p);
                        b.setAccept(q, accCount > 0);
                        newstate.put(p, q);
                    }
                    b.addTransition(r, q, lastPoint, point - 1);
                }
                TransitionList ends = points.points[i].ends;
                for (int j = 0; j < ends.next; j += 3) {
                    int dest = ends.transitions[j];
                    statesSet.decr(dest);
                    accCount -= a.isAccept(dest) ? 1 : 0;
                }
                ends.next = 0;
                TransitionList starts = points.points[i].starts;
                for (int j = 0; j < starts.next; j += 3) {
                    int dest = starts.transitions[j];
                    statesSet.incr(dest);
                    accCount += a.isAccept(dest) ? 1 : 0;
                }
                lastPoint = point;
                starts.next = 0;
            }
            points.reset();
        }
        return b.finish();
    }

    static long mix(int key) {
        int h = key * 0x9E3779B9;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }

    /** Hash/equality over a sorted state set (Lucene IntSet). */
    private abstract static class StateKey {
        abstract int[] array();

        abstract int size();

        abstract long longHash();

        @Override
        public final int hashCode() {
            return Long.hashCode(longHash());
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StateKey that)) {
                return false;
            }
            return longHash() == that.longHash() && Arrays.equals(array(), 0, size(), that.array(), 0, that.size());
        }
    }

    /** Frozen powerset member bound to its DFA state (Lucene FrozenIntSet). */
    private static final class FrozenSet extends StateKey {
        final int[] values;
        final int state;
        final long hash;

        FrozenSet(int[] values, long hash, int state) {
            this.values = values;
            this.state = state;
            this.hash = hash;
        }

        @Override int[] array() { return values; }

        @Override int size() { return values.length; }

        @Override long longHash() { return hash; }
    }

    /** Reference-counted NFA state set over dense lanes (replaces Lucene StateSet over hppc IntIntHashMap). */
    private static final class CountingSet extends StateKey {
        private final int[] counts;
        private final int[] position;
        private final int[] members;
        private int size;
        private long hash;
        private boolean hashValid = true;
        private int[] sorted = new int[0];
        private boolean sortedValid = true;

        CountingSet(int numStates) {
            counts = new int[numStates];
            position = new int[numStates];
            members = new int[numStates];
        }

        void incr(int state) {
            if (counts[state]++ == 0) {
                position[state] = size;
                members[size++] = state;
                changed();
            }
        }

        void decr(int state) {
            if (--counts[state] == 0) {
                int at = position[state];
                int last = members[--size];
                members[at] = last;
                position[last] = at;
                changed();
            }
        }

        private void changed() {
            hashValid = false;
            sortedValid = false;
        }

        FrozenSet freeze(int state) {
            return new FrozenSet(array().clone(), longHash(), state);
        }

        @Override
        int[] array() {
            if (!sortedValid) {
                sorted = Arrays.copyOf(members, size);
                Arrays.sort(sorted);
                sortedValid = true;
            }
            return sorted;
        }

        @Override int size() { return size; }

        @Override
        long longHash() {
            if (!hashValid) {
                long h = size;
                for (int i = 0; i < size; i++) {
                    h += mix(members[i]);
                }
                hash = h;
                hashValid = true;
            }
            return hash;
        }
    }

    private static final class TransitionList {
        int[] transitions = new int[3];
        int next;

        void add(M3Automaton.Transition t) {
            if (transitions.length < next + 3) {
                transitions = Arrays.copyOf(transitions, Math.max(next + 3, transitions.length * 2));
            }
            transitions[next] = t.dest;
            transitions[next + 1] = t.min;
            transitions[next + 2] = t.max;
            next += 3;
        }
    }

    private static final class PointTransitions implements Comparable<PointTransitions> {
        int point;
        final TransitionList ends = new TransitionList();
        final TransitionList starts = new TransitionList();

        @Override
        public int compareTo(PointTransitions other) {
            return Integer.compare(point, other.point);
        }

        void reset(int point) {
            this.point = point;
            ends.next = 0;
            starts.next = 0;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof PointTransitions p && p.point == point;
        }

        @Override
        public int hashCode() {
            return point;
        }
    }

    private static final class PointTransitionSet {
        private static final int HASHMAP_CUTOVER = 30;
        int count;
        PointTransitions[] points = new PointTransitions[5];
        private final HashMap<Integer, PointTransitions> map = new HashMap<>();
        private boolean useHash;

        private PointTransitions next(int point) {
            if (count == points.length) {
                points = Arrays.copyOf(points, count + (count >> 1) + 1);
            }
            PointTransitions p = points[count];
            if (p == null) {
                p = points[count] = new PointTransitions();
            }
            p.reset(point);
            count++;
            return p;
        }

        private PointTransitions find(int point) {
            if (useHash) {
                PointTransitions p = map.get(point);
                if (p == null) {
                    p = next(point);
                    map.put(point, p);
                }
                return p;
            }
            for (int i = 0; i < count; i++) {
                if (points[i].point == point) {
                    return points[i];
                }
            }
            PointTransitions p = next(point);
            if (count == HASHMAP_CUTOVER) {
                for (int i = 0; i < count; i++) {
                    map.put(points[i].point, points[i]);
                }
                useHash = true;
            }
            return p;
        }

        void reset() {
            if (useHash) {
                map.clear();
                useHash = false;
            }
            count = 0;
        }

        void sort() {
            if (count > 1) {
                Arrays.sort(points, 0, count);
            }
        }

        void add(M3Automaton.Transition t) {
            find(t.min).starts.add(t);
            find(1 + t.max).ends.add(t);
        }
    }

    // ---- minimization (MinimizationOperations, Hopcroft) ----

    /** Minimizes (determinizing first) with Hopcroft's algorithm over the unit alphabet. */
    public static M3Automaton minimize(M3Automaton a, int workLimit) {
        if (a.getNumStates() == 0 || (!a.isAccept(0) && a.getNumTransitions(0) == 0)) {
            return new M3Automaton();
        }
        a = determinize(a, workLimit);
        if (a.getNumTransitions(0) == 1) {
            M3Automaton.Transition t = new M3Automaton.Transition();
            a.getTransition(0, 0, t);
            if (t.dest == 0 && t.min == MIN_LABEL && t.max == MAX_LABEL) {
                return a;
            }
        }
        a = totalize(a);
        final int[] sigma = a.getStartPoints(MIN_LABEL, MAX_LABEL);
        final int sigmaLen = sigma.length;
        final int statesLen = a.getNumStates();
        @SuppressWarnings({"rawtypes", "unchecked"})
        final ArrayList<Integer>[][] reverse = (ArrayList<Integer>[][]) new ArrayList[statesLen][sigmaLen];
        @SuppressWarnings({"rawtypes", "unchecked"})
        final HashSet<Integer>[] partition = (HashSet<Integer>[]) new HashSet[statesLen];
        @SuppressWarnings({"rawtypes", "unchecked"})
        final ArrayList<Integer>[] splitblock = (ArrayList<Integer>[]) new ArrayList[statesLen];
        final int[] block = new int[statesLen];
        final StateList[][] active = new StateList[statesLen][sigmaLen];
        final StateListNode[][] active2 = new StateListNode[statesLen][sigmaLen];
        final LinkedList<int[]> pending = new LinkedList<>();
        final BitSet pending2 = new BitSet(sigmaLen * statesLen);
        final BitSet split = new BitSet(statesLen);
        final BitSet refine = new BitSet(statesLen);
        final BitSet refine2 = new BitSet(statesLen);
        for (int q = 0; q < statesLen; q++) {
            splitblock[q] = new ArrayList<>();
            partition[q] = new HashSet<>();
            for (int x = 0; x < sigmaLen; x++) {
                active[q][x] = new StateList();
            }
        }
        M3Automaton.Transition transition = new M3Automaton.Transition();
        for (int q = 0; q < statesLen; q++) {
            final int j = a.isAccept(q) ? 0 : 1;
            partition[j].add(q);
            block[q] = j;
            transition.source = q;
            transition.transitionUpto = -1;
            for (int x = 0; x < sigmaLen; x++) {
                final ArrayList<Integer>[] r = reverse[a.next(transition, sigma[x])];
                if (r[x] == null) {
                    r[x] = new ArrayList<>();
                }
                r[x].add(q);
            }
        }
        for (int j = 0; j <= 1; j++) {
            for (int x = 0; x < sigmaLen; x++) {
                for (int q : partition[j]) {
                    if (reverse[q][x] != null) {
                        active2[q][x] = active[j][x].add(q);
                    }
                }
            }
        }
        for (int x = 0; x < sigmaLen; x++) {
            final int j = (active[0][x].size <= active[1][x].size) ? 0 : 1;
            pending.add(new int[] {j, x});
            pending2.set(x * statesLen + j);
        }
        int k = 2;
        while (!pending.isEmpty()) {
            final int[] ip = pending.removeFirst();
            final int p = ip[0];
            final int x = ip[1];
            pending2.clear(x * statesLen + p);
            for (StateListNode m = active[p][x].first; m != null; m = m.next) {
                final ArrayList<Integer> r = reverse[m.q][x];
                if (r != null) {
                    for (int i : r) {
                        if (!split.get(i)) {
                            split.set(i);
                            final int j = block[i];
                            splitblock[j].add(i);
                            if (!refine2.get(j)) {
                                refine2.set(j);
                                refine.set(j);
                            }
                        }
                    }
                }
            }
            for (int j = refine.nextSetBit(0); j >= 0; j = refine.nextSetBit(j + 1)) {
                final ArrayList<Integer> sb = splitblock[j];
                if (sb.size() < partition[j].size()) {
                    final HashSet<Integer> b1 = partition[j];
                    final HashSet<Integer> b2 = partition[k];
                    for (int s : sb) {
                        b1.remove(s);
                        b2.add(s);
                        block[s] = k;
                        for (int c = 0; c < sigmaLen; c++) {
                            final StateListNode sn = active2[s][c];
                            if (sn != null && sn.sl == active[j][c]) {
                                sn.remove();
                                active2[s][c] = active[k][c].add(s);
                            }
                        }
                    }
                    for (int c = 0; c < sigmaLen; c++) {
                        final int aj = active[j][c].size;
                        final int ak = active[k][c].size;
                        final int ofs = c * statesLen;
                        if (!pending2.get(ofs + j) && 0 < aj && aj <= ak) {
                            pending2.set(ofs + j);
                            pending.add(new int[] {j, c});
                        } else {
                            pending2.set(ofs + k);
                            pending.add(new int[] {k, c});
                        }
                    }
                    k++;
                }
                refine2.clear(j);
                for (int s : sb) {
                    split.clear(s);
                }
                sb.clear();
            }
            refine.clear();
        }
        M3Automaton result = new M3Automaton();
        M3Automaton.Transition t = new M3Automaton.Transition();
        int[] stateMap = new int[statesLen];
        int[] stateRep = new int[k];
        result.createState();
        for (int n = 0; n < k; n++) {
            boolean isInitial = partition[n].contains(0);
            int newState = isInitial ? 0 : result.createState();
            for (int q : partition[n]) {
                stateMap[q] = newState;
                result.setAccept(newState, a.isAccept(q));
                stateRep[newState] = q;
            }
        }
        for (int n = 0; n < k; n++) {
            int numTransitions = a.initTransition(stateRep[n], t);
            for (int i = 0; i < numTransitions; i++) {
                a.getNextTransition(t);
                result.addTransition(n, stateMap[t.dest], t.min, t.max);
            }
        }
        result.finishState();
        return removeDeadStates(result);
    }

    private static final class StateList {
        int size;
        StateListNode first;
        StateListNode last;

        StateListNode add(int q) {
            return new StateListNode(q, this);
        }
    }

    private static final class StateListNode {
        final int q;
        StateListNode next;
        StateListNode prev;
        final StateList sl;

        StateListNode(int q, StateList sl) {
            this.q = q;
            this.sl = sl;
            if (sl.size++ == 0) {
                sl.first = this;
                sl.last = this;
            } else {
                sl.last.next = this;
                prev = sl.last;
                sl.last = this;
            }
        }

        void remove() {
            sl.size--;
            if (sl.first == this) {
                sl.first = next;
            } else {
                prev.next = next;
            }
            if (sl.last == this) {
                sl.last = prev;
            } else {
                next.prev = prev;
            }
        }
    }
}
