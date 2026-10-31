// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
//
// Lineage (TARGET_OWNED_PORT, leaf R6-AUTOMATON, run 20261017-synexia-to-public):
//   Apache Lucene 9.10.0 org.apache.lucene.util.automaton.Automaton (git blob d6f0d3c4698a1b38828ede7628b0aecb2ffeafea)
//   and Transition (git blob a9e60b317607221e0e44089e424e19dc4caef99a), vendored unchanged in M3jdk21 at
//   m3/vendor/third_party/lucene-core (BLOB_EQUAL to upstream tag 695c0ac84508438302cd346a812cfa2fdc5a10df).
//   Original notice: Licensed to the Apache Software Foundation (ASF) under one or more contributor license
//   agreements; the ASF licenses that file under the Apache License, Version 2.0
//   (http://www.apache.org/licenses/LICENSE-2.0). See m3/vendor/third_party/lucene-core/THIRD_PARTY_NOTICES.md.
//   Modifications: package jdk.internal.mindex; no org.apache.lucene imports; Accountable/RAM accounting, toDot and
//   getSortedTransitions dropped; ArrayUtil.grow -> Arrays.copyOf; InPlaceMergeSorter -> stable index merge sort over
//   int tuples; getStartPoints collects into an int lane instead of a HashSet<Integer>; Transition is a nested reusable
//   cursor (never stored). Labels are plain ints: the UTF-16 unit alphabet is chosen by M3AutomatonOps, token-id
//   alphabets (leaf TOKEN-REGEX) reuse this class unchanged.
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * Finite automaton over int labels stored as packed primitive lanes (DOOP: no per-state or per-transition objects).
 *
 * <p>{@code states} holds two ints per state (offset into {@code transitions}, transition count); {@code transitions}
 * holds three ints per transition (dest, min, max). Transitions of one state are added contiguously; finishing a state
 * sorts them, merges adjacent ranges to the same destination and records whether the automaton is still
 * deterministic. State 0 is the initial state. Internal; not a public JDK API.
 */
public final class M3Automaton {
    private static final int[] DEST_MIN_MAX = {0, 1, 2};
    private static final int[] MIN_MAX_DEST = {1, 2, 0};
    private static final int[] SRC_MIN_MAX_DEST = {0, 2, 3, 1};

    private int nextState;
    private int nextTransition;
    private int curState = -1;
    private int[] states;
    private final BitSet isAccept;
    private int[] transitions;
    private boolean deterministic = true;

    /** Creates an automaton with no states. */
    public M3Automaton() {
        this(2, 2);
    }

    /** Creates an automaton with room for the given number of states and transitions. */
    public M3Automaton(int numStates, int numTransitions) {
        states = new int[Math.max(2, numStates * 2)];
        isAccept = new BitSet(numStates);
        transitions = new int[Math.max(3, numTransitions * 3)];
    }

    /** Creates a new state and returns its number. */
    public int createState() {
        growStates();
        int state = nextState / 2;
        states[nextState] = -1;
        states[nextState + 1] = 0;
        nextState += 2;
        return state;
    }

    /** Sets or clears the accept flag of a state. */
    public void setAccept(int state, boolean accept) {
        Objects.checkIndex(state, getNumStates());
        isAccept.set(state, accept);
    }

    BitSet acceptStates() {
        return isAccept;
    }

    /** Returns true if the state accepts. */
    public boolean isAccept(int state) {
        return isAccept.get(state);
    }

    /** Adds a transition on a single label. */
    public void addTransition(int source, int dest, int label) {
        addTransition(source, dest, label, label);
    }

    /** Adds a transition on the inclusive label range [min, max]; all transitions of a source are added together. */
    public void addTransition(int source, int dest, int min, int max) {
        int bounds = nextState / 2;
        Objects.checkIndex(source, bounds);
        Objects.checkIndex(dest, bounds);
        growTransitions();
        if (curState != source) {
            if (curState != -1) {
                finishCurrentState();
            }
            curState = source;
            if (states[2 * curState] != -1) {
                throw new IllegalStateException("from state (" + source + ") already had transitions added");
            }
            states[2 * curState] = nextTransition;
        }
        transitions[nextTransition++] = dest;
        transitions[nextTransition++] = min;
        transitions[nextTransition++] = max;
        states[2 * curState + 1]++;
    }

    /** Adds a virtual epsilon transition: copies the finished transitions and accept flag of dest onto source. */
    public void addEpsilon(int source, int dest) {
        Transition t = new Transition();
        int count = initTransition(dest, t);
        for (int i = 0; i < count; i++) {
            getNextTransition(t);
            addTransition(source, t.dest, t.min, t.max);
        }
        if (isAccept(dest)) {
            setAccept(source, true);
        }
    }

    /** Appends all states and transitions of another (finished) automaton, renumbering its states. */
    public void copy(M3Automaton other) {
        int stateOffset = getNumStates();
        states = Arrays.copyOf(states, Math.max(states.length, nextState + other.nextState));
        System.arraycopy(other.states, 0, states, nextState, other.nextState);
        for (int i = 0; i < other.nextState; i += 2) {
            if (states[nextState + i] != -1) {
                states[nextState + i] += nextTransition;
            }
        }
        nextState += other.nextState;
        int otherNumStates = other.getNumStates();
        BitSet otherAccept = other.acceptStates();
        for (int s = otherAccept.nextSetBit(0); s >= 0 && s < otherNumStates; s = otherAccept.nextSetBit(s + 1)) {
            setAccept(stateOffset + s, true);
        }
        transitions = Arrays.copyOf(transitions, Math.max(transitions.length, nextTransition + other.nextTransition));
        System.arraycopy(other.transitions, 0, transitions, nextTransition, other.nextTransition);
        for (int i = 0; i < other.nextTransition; i += 3) {
            transitions[nextTransition + i] += stateOffset;
        }
        nextTransition += other.nextTransition;
        if (!other.deterministic) {
            deterministic = false;
        }
    }

    private void finishCurrentState() {
        int numTransitions = states[2 * curState + 1];
        int offset = states[2 * curState];
        int start = offset / 3;
        sortTuples(transitions, 3, start, numTransitions, DEST_MIN_MAX);
        int upto = 0;
        int min = -1;
        int max = -1;
        int dest = -1;
        for (int i = 0; i < numTransitions; i++) {
            int tDest = transitions[offset + 3 * i];
            int tMin = transitions[offset + 3 * i + 1];
            int tMax = transitions[offset + 3 * i + 2];
            if (dest == tDest) {
                if (tMin <= max + 1) {
                    if (tMax > max) {
                        max = tMax;
                    }
                } else {
                    if (dest != -1) {
                        upto = put(offset, upto, dest, min, max);
                    }
                    min = tMin;
                    max = tMax;
                }
            } else {
                if (dest != -1) {
                    upto = put(offset, upto, dest, min, max);
                }
                dest = tDest;
                min = tMin;
                max = tMax;
            }
        }
        if (dest != -1) {
            upto = put(offset, upto, dest, min, max);
        }
        nextTransition -= (numTransitions - upto) * 3;
        states[2 * curState + 1] = upto;
        sortTuples(transitions, 3, start, upto, MIN_MAX_DEST);
        if (deterministic && upto > 1) {
            int lastMax = transitions[offset + 2];
            for (int i = 1; i < upto; i++) {
                if (transitions[offset + 3 * i + 1] <= lastMax) {
                    deterministic = false;
                    break;
                }
                lastMax = transitions[offset + 3 * i + 2];
            }
        }
    }

    private int put(int offset, int upto, int dest, int min, int max) {
        transitions[offset + 3 * upto] = dest;
        transitions[offset + 3 * upto + 1] = min;
        transitions[offset + 3 * upto + 2] = max;
        return upto + 1;
    }

    /** True if no state has two transitions with overlapping labels. */
    public boolean isDeterministic() {
        return deterministic;
    }

    /** Finishes the current state; required after adding the transitions of the last state. */
    public void finishState() {
        if (curState != -1) {
            finishCurrentState();
            curState = -1;
        }
    }

    /** Number of states. */
    public int getNumStates() {
        return nextState / 2;
    }

    /** Number of transitions. */
    public int getNumTransitions() {
        return nextTransition / 3;
    }

    /** Number of transitions leaving the state. */
    public int getNumTransitions(int state) {
        int count = states[2 * state + 1];
        return count == -1 ? 0 : count;
    }

    private void growStates() {
        if (nextState + 2 > states.length) {
            states = Arrays.copyOf(states, Math.max(nextState + 2, states.length + (states.length >> 1) + 2));
        }
    }

    private void growTransitions() {
        if (nextTransition + 3 > transitions.length) {
            transitions = Arrays.copyOf(transitions,
                    Math.max(nextTransition + 3, transitions.length + (transitions.length >> 1) + 3));
        }
    }

    /** Positions the reusable cursor at the first transition of the state; returns the transition count. */
    public int initTransition(int state, Transition t) {
        t.source = state;
        t.transitionUpto = states[2 * state];
        return getNumTransitions(state);
    }

    /** Advances the cursor to the next transition. */
    public void getNextTransition(Transition t) {
        t.dest = transitions[t.transitionUpto++];
        t.min = transitions[t.transitionUpto++];
        t.max = transitions[t.transitionUpto++];
    }

    /** Fills the cursor with the index'th transition of the state. */
    public void getTransition(int state, int index, Transition t) {
        int i = states[2 * state] + 3 * index;
        t.source = state;
        t.dest = transitions[i++];
        t.min = transitions[i++];
        t.max = transitions[i];
    }

    /** Sorted, distinct interval start points of all transitions, always including minLabel. */
    public int[] getStartPoints(int minLabel, int maxLabel) {
        int[] points = new int[2 + 2 * getNumTransitions()];
        int n = 0;
        points[n++] = minLabel;
        for (int s = 0; s < nextState; s += 2) {
            int trans = states[s];
            int limit = trans + 3 * states[s + 1];
            while (trans < limit) {
                points[n++] = transitions[trans + 1];
                int max = transitions[trans + 2];
                if (max < maxLabel) {
                    points[n++] = max + 1;
                }
                trans += 3;
            }
        }
        Arrays.sort(points, 0, n);
        int distinct = 0;
        for (int i = 0; i < n; i++) {
            if (distinct == 0 || points[distinct - 1] != points[i]) {
                points[distinct++] = points[i];
            }
        }
        return Arrays.copyOf(points, distinct);
    }

    /** Deterministic lookup: destination for the label, or -1. */
    public int step(int state, int label) {
        return next(state, 0, label, null);
    }

    /** Deterministic lookup continuing from the cursor position; updates the cursor. */
    public int next(Transition transition, int label) {
        return next(transition.source, transition.transitionUpto, label, transition);
    }

    private int next(int state, int fromTransitionIndex, int label, Transition transition) {
        int firstTransitionIndex = states[2 * state];
        int low = Math.max(fromTransitionIndex, 0);
        int high = getNumTransitions(state) - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int transitionIndex = firstTransitionIndex + 3 * mid;
            int minLabel = transitions[transitionIndex + 1];
            if (minLabel > label) {
                high = mid - 1;
            } else {
                int maxLabel = transitions[transitionIndex + 2];
                if (maxLabel < label) {
                    low = mid + 1;
                } else {
                    int destState = transitions[transitionIndex];
                    if (transition != null) {
                        transition.dest = destState;
                        transition.min = minLabel;
                        transition.max = maxLabel;
                        transition.transitionUpto = mid;
                    }
                    return destState;
                }
            }
        }
        if (transition != null) {
            transition.dest = -1;
            transition.transitionUpto = low;
        }
        return -1;
    }

    /** Stable merge sort of {@code count} int tuples of {@code width} starting at tuple index {@code from}. */
    static void sortTuples(int[] a, int width, int from, int count, int[] keys) {
        if (count < 2) {
            return;
        }
        int[] idx = new int[count];
        int[] tmp = new int[count];
        for (int i = 0; i < count; i++) {
            idx[i] = i;
        }
        int base = from * width;
        for (int run = 1; run < count; run <<= 1) {
            for (int lo = 0; lo < count - run; lo += run << 1) {
                int mid = lo + run;
                int hi = Math.min(lo + (run << 1), count);
                int i = lo;
                int j = mid;
                int k = lo;
                while (i < mid && j < hi) {
                    tmp[k++] = compareTuples(a, base, width, idx[j], idx[i], keys) < 0 ? idx[j++] : idx[i++];
                }
                while (i < mid) {
                    tmp[k++] = idx[i++];
                }
                while (j < hi) {
                    tmp[k++] = idx[j++];
                }
                System.arraycopy(tmp, lo, idx, lo, hi - lo);
            }
        }
        int[] copy = Arrays.copyOfRange(a, base, base + count * width);
        for (int i = 0; i < count; i++) {
            System.arraycopy(copy, idx[i] * width, a, base + i * width, width);
        }
    }

    private static int compareTuples(int[] a, int base, int width, int x, int y, int[] keys) {
        int bx = base + x * width;
        int by = base + y * width;
        for (int key : keys) {
            int cmp = Integer.compare(a[bx + key], a[by + key]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /** Reusable transition cursor (source, dest, min, max); never stored in an automaton. */
    public static final class Transition {
        /** Source state. */
        public int source;
        /** Destination state. */
        public int dest;
        /** Minimum label, inclusive. */
        public int min;
        /** Maximum label, inclusive. */
        public int max;
        int transitionUpto = -1;

        /** Creates an uninitialized cursor. */
        public Transition() { }
    }

    /**
     * Records states and transitions in any order, then {@link #finish} builds a sorted {@link M3Automaton}.
     * Transitions are packed four ints each (source, dest, min, max).
     */
    public static final class Builder {
        private int nextState;
        private final BitSet isAccept;
        private int[] transitions;
        private int nextTransition;

        /** Builder with room for 16 states and transitions. */
        public Builder() {
            this(16, 16);
        }

        /** Builder with room for the given number of states and transitions. */
        public Builder(int numStates, int numTransitions) {
            isAccept = new BitSet(numStates);
            transitions = new int[Math.max(4, numTransitions * 4)];
        }

        /** Adds a transition on a single label. */
        public void addTransition(int source, int dest, int label) {
            addTransition(source, dest, label, label);
        }

        /** Adds a transition on [min, max]. */
        public void addTransition(int source, int dest, int min, int max) {
            if (transitions.length < nextTransition + 4) {
                transitions = Arrays.copyOf(transitions,
                        Math.max(nextTransition + 4, transitions.length + (transitions.length >> 1) + 4));
            }
            transitions[nextTransition++] = source;
            transitions[nextTransition++] = dest;
            transitions[nextTransition++] = min;
            transitions[nextTransition++] = max;
        }

        /** Adds a virtual epsilon transition by copying the transitions recorded so far for dest. */
        public void addEpsilon(int source, int dest) {
            int limit = nextTransition;
            for (int upto = 0; upto < limit; upto += 4) {
                if (transitions[upto] == dest) {
                    addTransition(source, transitions[upto + 1], transitions[upto + 2], transitions[upto + 3]);
                }
            }
            if (isAccept(dest)) {
                setAccept(source, true);
            }
        }

        /** Builds the automaton. */
        public M3Automaton finish() {
            int numStates = nextState;
            int numTransitions = nextTransition / 4;
            M3Automaton a = new M3Automaton(numStates, numTransitions);
            for (int state = 0; state < numStates; state++) {
                a.createState();
                a.setAccept(state, isAccept(state));
            }
            sortTuples(transitions, 4, 0, numTransitions, SRC_MIN_MAX_DEST);
            for (int upto = 0; upto < nextTransition; upto += 4) {
                a.addTransition(transitions[upto], transitions[upto + 1], transitions[upto + 2], transitions[upto + 3]);
            }
            a.finishState();
            return a;
        }

        /** Creates a state. */
        public int createState() {
            return nextState++;
        }

        /** Sets or clears the accept flag. */
        public void setAccept(int state, boolean accept) {
            Objects.checkIndex(state, getNumStates());
            isAccept.set(state, accept);
        }

        /** Returns true if the state accepts. */
        public boolean isAccept(int state) {
            return isAccept.get(state);
        }

        /** Number of states. */
        public int getNumStates() {
            return nextState;
        }

        /** Copies all states and transitions of another automaton. */
        public void copy(M3Automaton other) {
            int offset = getNumStates();
            int otherNumStates = other.getNumStates();
            copyStates(other);
            Transition t = new Transition();
            for (int s = 0; s < otherNumStates; s++) {
                int count = other.initTransition(s, t);
                for (int i = 0; i < count; i++) {
                    other.getNextTransition(t);
                    addTransition(offset + s, offset + t.dest, t.min, t.max);
                }
            }
        }

        /** Copies the states (and accept flags) of another automaton. */
        public void copyStates(M3Automaton other) {
            int otherNumStates = other.getNumStates();
            for (int s = 0; s < otherNumStates; s++) {
                int newState = createState();
                setAccept(newState, other.isAccept(s));
            }
        }
    }
}
