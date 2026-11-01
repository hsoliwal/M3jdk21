// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Exact complete-DFA state equivalence by bounded primitive partition refinement.
 * Original unreachable states are also retained for state-equivalence queries.
 * Not an NFA, regex compiler, symbolic transducer or grammar parser.
 */
public final class M3DfaPartitions {
    private M3DfaPartitions() { }
    public record Minimized(int alphabet, int[] originalClass, int[] transitions,
                            long[] accepting, int classCount) {
        public Minimized {
            originalClass = originalClass.clone(); transitions = transitions.clone();
            accepting = accepting.clone();
        }
        @Override public int[] originalClass() { return originalClass.clone(); }
        @Override public int[] transitions() { return transitions.clone(); }
        @Override public long[] accepting() { return accepting.clone(); }
        public boolean accepts(int initialState, int[] word) {
            Objects.checkIndex(initialState, originalClass.length);
            Objects.requireNonNull(word, "word");
            int state = originalClass[initialState];
            for (int symbol : word) {
                if (symbol < 0 || symbol >= alphabet) throw new IllegalArgumentException("symbol");
                state = transitions[state * alphabet + symbol];
            }
            return (accepting[state >>> 6] & (1L << (state & 63))) != 0;
        }
    }

    /** Primitive transition rows are state-major: transitions[state*alphabet+symbol]. */
    public static Minimized minimize(int states, int alphabet, int[] transitions, long[] finalMask) {
        Objects.requireNonNull(transitions, "transitions");
        Objects.requireNonNull(finalMask, "finalMask");
        if (states < 1 || states > 128 || alphabet < 1 || alphabet > 32
                || transitions.length != states * alphabet || finalMask.length != (states + 63) >>> 6)
            throw new IllegalArgumentException("DFA geometry");
        if ((states & 63) != 0 && (finalMask[finalMask.length - 1] >>> (states & 63)) != 0)
            throw new IllegalArgumentException("unused acceptance bits");
        for (int target : transitions) if (target < 0 || target >= states)
            throw new IllegalArgumentException("invalid transition target");
        int[] classes = new int[states];
        for (int state = 0; state < states; state++)
            classes[state] = ((finalMask[state >>> 6] >>> (state & 63)) & 1L) != 0 ? 1 : 0;
        int distinct = -1;
        for (int pass = 0; pass <= states + 2; pass++) {
            int[] next = new int[states];
            distinct = 0;
            for (int state = 0; state < states; state++) {
                int representative = -1;
                for (int prior = 0; prior < state; prior++) {
                    if (classes[state] != classes[prior]) continue;
                    boolean same = true;
                    for (int symbol = 0; symbol < alphabet; symbol++) {
                        if (classes[transitions[state * alphabet + symbol]]
                                != classes[transitions[prior * alphabet + symbol]]) {
                            same = false; break;
                        }
                    }
                    if (same) { representative = next[prior]; break; }
                }
                next[state] = representative < 0 ? distinct++ : representative;
            }
            if (Arrays.equals(classes, next)) break;
            if (pass == states + 2) throw new IllegalStateException("nonconvergent partition");
            classes = next;
        }
        int[] collapsed = new int[Math.multiplyExact(distinct, alphabet)];
        long[] accepted = new long[(distinct + 63) >>> 6];
        boolean[] seen = new boolean[distinct];
        for (int state = 0; state < states; state++) {
            int group = classes[state];
            if (seen[group]) continue;
            seen[group] = true;
            if ((finalMask[state >>> 6] & (1L << (state & 63))) != 0)
                accepted[group >>> 6] |= 1L << (group & 63);
            for (int symbol = 0; symbol < alphabet; symbol++)
                collapsed[group * alphabet + symbol] = classes[transitions[state * alphabet + symbol]];
        }
        return new Minimized(alphabet, classes, collapsed, accepted, distinct);
    }
}
