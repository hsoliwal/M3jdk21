/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3Automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reference semantics of M3Automaton over UTF-16 code units (AUTOMATON ORACLE v1): a set-of-states
 * simulation that reads only the public transition cursor, language comparison against
 * java.util.regex over every text of a small alphabet, and structural invariants (sorted, merged,
 * in-range transitions; determinism recomputed from the transitions).
 */
final class AutomatonOracle {
    /** Small alphabet of the language checks, plus one unit outside every literal. */
    static final List<String> ABC = List.of("a", "b", "c", "d");
    /** Alphabet with the extreme code units of the unit range. */
    static final List<String> EDGES = List.of("a", "b", "\u0000", "￿");
    /** Default exhaustive text length. */
    static final int MAX_LENGTH = 5;
    /** Largest UTF-16 code unit label. */
    static final int MAX_LABEL = 0xFFFF;
    //FLLDCIM: SFA - LITERAL GATE v1: the fields of a transition row are named, not indexed by literals
    /** Index of the destination state in a transition row {dest, min, max} of {@link #transitions}. */
    static final int DEST = 0;
    /** Index of the smallest label of the range in a transition row. */
    static final int MIN = 1;
    /** Index of the largest label of the range in a transition row. */
    static final int MAX = 2;

    private static final Map<String, Pattern> COMPILED = new HashMap<>();

    private AutomatonOracle() {
    }

    /** Every outgoing transition of the state as {dest, min, max}, in cursor order. */
    static int[][] transitions(M3Automaton a, int state) {
        M3Automaton.Transition t = new M3Automaton.Transition();
        int count = a.initTransition(state, t);
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) {
            a.getNextTransition(t);
            out[i] = new int[] {t.dest, t.min, t.max};
        }
        return out;
    }

    /** NFA acceptance by state-set simulation from state 0. */
    static boolean accepts(M3Automaton a, CharSequence text) {
        if (a.getNumStates() == 0) {
            return false;
        }
        BitSet states = new BitSet();
        states.set(0);
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            BitSet next = new BitSet();
            for (int s = states.nextSetBit(0); s >= 0; s = states.nextSetBit(s + 1)) {
                for (int[] t : transitions(a, s)) {
                    if (t[MIN] <= unit && unit <= t[MAX]) {
                        next.set(t[DEST]);
                    }
                }
            }
            states = next;
        }
        for (int s = states.nextSetBit(0); s >= 0; s = states.nextSetBit(s + 1)) {
            if (a.isAccept(s)) {
                return true;
            }
        }
        return false;
    }

    static boolean javaMatches(String regex, CharSequence text) {
        return COMPILED.computeIfAbsent(regex, r -> Pattern.compile(r, Pattern.DOTALL)).matcher(text).matches();
    }

    /** Every string over {@code alphabet} of length 0..maxLength. */
    static List<String> texts(List<String> alphabet, int maxLength) {
        List<String> out = new ArrayList<>();
        List<String> layer = List.of("");
        out.add("");
        for (int len = 1; len <= maxLength; len++) {
            List<String> next = new ArrayList<>();
            for (String prefix : layer) {
                for (String unit : alphabet) {
                    next.add(prefix + unit);
                }
            }
            out.addAll(next);
            layer = next;
        }
        return out;
    }

    /** Language of {@code a} equals the java.util.regex language of {@code regex} (DOTALL) on all texts. */
    static void assertLanguage(M3Automaton a, String regex, List<String> alphabet, int maxLength) {
        assertWellFormed(a);
        for (String text : texts(alphabet, maxLength)) {
            assertEquals(javaMatches(regex, text), accepts(a, text), "/" + regex + "/ on '" + text + "'");
        }
    }

    /** Two automata accept the same texts over the alphabet. */
    static void assertSameLanguage(M3Automaton expected, M3Automaton actual, List<String> alphabet, int maxLength) {
        assertWellFormed(actual);
        for (String text : texts(alphabet, maxLength)) {
            assertEquals(accepts(expected, text), accepts(actual, text), "'" + text + "'");
        }
    }

    /** True when no state has two transitions with overlapping label ranges. */
    static boolean computedDeterministic(M3Automaton a) {
        for (int s = 0; s < a.getNumStates(); s++) {
            int[][] ts = transitions(a, s);
            for (int i = 0; i < ts.length; i++) {
                for (int j = i + 1; j < ts.length; j++) {
                    if (ts[i][MIN] <= ts[j][MAX] && ts[j][MIN] <= ts[i][MAX]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Finished-state invariants: destinations in range, labels in [0, 0xFFFF] with min <= max, transitions
     * sorted by (min, max, dest), no two adjacent or overlapping ranges to the same destination, and the
     * determinism flag equal to the recomputed one whenever the flag claims determinism.
     */
    static void assertWellFormed(M3Automaton a) {
        int transitions = 0;
        for (int s = 0; s < a.getNumStates(); s++) {
            int[][] ts = transitions(a, s);
            transitions += ts.length;
            assertEquals(ts.length, a.getNumTransitions(s), "count of state " + s);
            for (int i = 0; i < ts.length; i++) {
                assertTrue(ts[i][DEST] >= 0 && ts[i][DEST] < a.getNumStates(), "dest in range");
                assertTrue(0 <= ts[i][MIN] && ts[i][MIN] <= ts[i][MAX] && ts[i][MAX] <= MAX_LABEL, "label range");
                if (i > 0) {
                    int[] p = ts[i - 1];
                    int[] q = ts[i];
                    boolean ordered = p[MIN] < q[MIN] || p[MIN] == q[MIN] && (p[MAX] < q[MAX] || p[MAX] == q[MAX] && p[DEST] <= q[DEST]);
                    assertTrue(ordered, "sorted by (min, max, dest) at state " + s);
                }
                for (int j = 0; j < ts.length; j++) {
                    if (i != j && ts[i][DEST] == ts[j][DEST]) {
                        assertTrue(ts[j][MIN] > ts[i][MAX] + 1 || ts[i][MIN] > ts[j][MAX] + 1,
                                "merged ranges to one destination at state " + s);
                    }
                }
            }
        }
        assertEquals(transitions, a.getNumTransitions(), "total transitions");
        if (a.isDeterministic()) {
            assertTrue(computedDeterministic(a), "deterministic flag matches the transitions");
        }
    }

    /** Builds a deterministic automaton accepting exactly {@code word} (one state per unit). */
    static M3Automaton word(String word) {
        M3Automaton a = new M3Automaton();
        int last = a.createState();
        for (int i = 0; i < word.length(); i++) {
            int next = a.createState();
            a.addTransition(last, next, word.charAt(i));
            last = next;
        }
        a.setAccept(last, true);
        a.finishState();
        return a;
    }
}
