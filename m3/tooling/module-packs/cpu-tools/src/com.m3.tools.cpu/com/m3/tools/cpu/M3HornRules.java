// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Positive Horn/Datalog-style monotone fixed point over primitive bit masks and CSR rule lanes.
 * Not general Prolog, negation-as-failure, SMT, policy authorization, or a theorem prover.
 */
public final class M3HornRules {
    private M3HornRules() { }

    /** Each rule r: antecedents[offsets[r]..offsets[r+1]), derives head[r].
     * Derived facts receive the first firing rule index. Original facts receive -1;
     * unreached facts receive -2. All input arrays are caller-owned and never retained.
     */
    public record Closure(long[] facts, int[] firstRule, int firedRules) {
        public Closure {
            facts = facts.clone(); firstRule = firstRule.clone();
        }
        @Override public long[] facts() { return facts.clone(); }
        @Override public int[] firstRule() { return firstRule.clone(); }
        public boolean has(int fact) {
            Objects.checkIndex(fact, firstRule.length);
            return (facts[fact >>> 6] & (1L << (fact & 63))) != 0;
        }
    }

    public static Closure close(int factCount, long[] initial, int[] offsets,
                                int[] antecedents, int[] heads) {
        Objects.requireNonNull(initial); Objects.requireNonNull(offsets);
        Objects.requireNonNull(antecedents); Objects.requireNonNull(heads);
        if (factCount < 0 || factCount > 65536 || heads.length > 100000
                || antecedents.length > 2_000_000 || offsets.length != heads.length + 1
                || initial.length != ((long) factCount + 63) / 64
                || offsets[0] != 0 || offsets[heads.length] != antecedents.length)
            throw new IllegalArgumentException("Horn geometry");
        for (int i = 1; i < offsets.length; i++)
            if (offsets[i] < offsets[i - 1] || offsets[i] > antecedents.length)
                throw new IllegalArgumentException("nonmonotone Horn offsets");
        for (int head : heads) if (head < 0 || head >= factCount)
            throw new IllegalArgumentException("Horn conclusion");
        for (int prerequisite : antecedents) if (prerequisite < 0 || prerequisite >= factCount)
            throw new IllegalArgumentException("Horn prerequisite");
        long[] facts = initial.clone();
        if ((factCount & 63) != 0 && facts.length != 0
                && (facts[facts.length - 1] >>> (factCount & 63)) != 0)
            throw new IllegalArgumentException("bits outside fact domain");
        // Inverted index: each fact owns the rule-occurrences that depend on it.
        int[] starts = new int[factCount + 1];
        for (int prerequisite : antecedents) starts[prerequisite + 1]++;
        for (int i = 1; i < starts.length; i++) starts[i] = Math.addExact(starts[i], starts[i - 1]);
        int[] next = starts.clone(), ruleRefs = new int[antecedents.length];
        for (int r = 0; r < heads.length; r++)
            for (int p = offsets[r]; p < offsets[r + 1]; p++)
                ruleRefs[next[antecedents[p]]++] = r;
        int[] remaining = new int[heads.length], first = new int[factCount];
        Arrays.fill(first, -2);
        int[] queue = new int[factCount];
        int read = 0, write = 0;
        for (int f = 0; f < factCount; f++) if ((facts[f >>> 6] & (1L << (f & 63))) != 0) {
            first[f] = -1; queue[write++] = f;
        }
        // Fire empty-body rules immediately and exactly once.
        int fired = 0;
        for (int r = 0; r < heads.length; r++) {
            remaining[r] = offsets[r + 1] - offsets[r];
            if (remaining[r] == 0) {
                fired++;
                int f = heads[r];
                if (first[f] == -2) {
                    first[f] = r; facts[f >>> 6] |= 1L << (f & 63); queue[write++] = f;
                }
            }
        }
        while (read < write) {
            int fact = queue[read++];
            for (int at = starts[fact]; at < starts[fact + 1]; at++) {
                int rule = ruleRefs[at];
                if (--remaining[rule] == 0) {
                    fired++;
                    int derived = heads[rule];
                    if (first[derived] == -2) {
                        first[derived] = rule; facts[derived >>> 6] |= 1L << (derived & 63);
                        queue[write++] = derived;
                    }
                }
            }
        }
        return new Closure(facts, first, fired);
    }
}
