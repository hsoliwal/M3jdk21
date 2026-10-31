// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/**
 * M3 primitive-lane ranking, Pareto and evidence-quorum atoms.
 * They are deterministic aggregation rules, not a distributed consensus protocol,
 * authorization engine, or calibrated probability model.
 */
public final class M3EvidenceMath {
    private M3EvidenceMath() { }

    /**
     * Reciprocal-rank fusion of distinct candidate IDs within each source list.
     * offsets: listCount+1; candidate IDs: packed list entries; rank is 1-based.
     * All population IDs are returned, ranked by descending score then ascending ID.
     */
    public static int[] reciprocalRankFusion(int population, int[] offsets,
                                             int[] candidates, int constant) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(candidates, "candidates");
        if (population < 0 || population > 1_000_000 || offsets.length == 0
                || offsets[0] != 0 || offsets[offsets.length - 1] != candidates.length
                || constant < 1) throw new IllegalArgumentException("rank geometry");
        double[] scores = new double[population];
        int[] lastSeen = new int[population];
        for (int list = 0; list + 1 < offsets.length; list++) {
            int from = offsets[list], to = offsets[list + 1];
            if (from < 0 || to < from || to > candidates.length)
                throw new IllegalArgumentException("rank offsets");
            for (int position = from; position < to; position++) {
                int candidate = candidates[position];
                if (candidate < 0 || candidate >= population || lastSeen[candidate] == list + 1)
                    throw new IllegalArgumentException("candidate or duplicate rank");
                lastSeen[candidate] = list + 1;
                scores[candidate] += 1d / ((double) constant + (position - from + 1));
            }
        }
        int[] order = new int[population], scratch = new int[population];
        for (int i = 0; i < population; i++) order[i] = i;
        // Stable primitive-only merge sort; ties compare canonical physical IDs.
        for (long width = 1; width < population; width <<= 1) {
            for (int first = 0; first < population;) {
                int middle = (int) Math.min((long) population, first + width);
                int last = (int) Math.min((long) population, middle + width);
                int a = first, b = middle, write = first;
                while (a < middle && b < last) {
                    int left = order[a], right = order[b];
                    int comparison = Double.compare(scores[right], scores[left]);
                    scratch[write++] = comparison < 0 || (comparison == 0 && left < right)
                            ? order[a++] : order[b++];
                }
                while (a < middle) scratch[write++] = order[a++];
                while (b < last) scratch[write++] = order[b++];
                first = last;
            }
            int[] swap = order; order = scratch; scratch = swap;
        }
        return order;
    }

    /**
     * One bit per outcome if at least required independent ballots selected it.
     * Ballots are packed contiguously by voter, each with ceil(entities/64) words.
     */
    public static long[] quorumMask(long[] ballots, int voters, int entities, int required) {
        Objects.requireNonNull(ballots, "ballots");
        if (voters < 1 || entities < 0 || required < 1 || required > voters)
            throw new IllegalArgumentException("quorum geometry");
        int words = (entities >>> 6) + ((entities & 63) == 0 ? 0 : 1);
        if (Math.multiplyExact(voters, words) != ballots.length)
            throw new IllegalArgumentException("quorum ballot length");
        long[] result = new long[words];
        for (int outcome = 0; outcome < entities; outcome++) {
            int votes = 0;
            long bit = 1L << (outcome & 63);
            for (int voter = 0; voter < voters; voter++) {
                if ((ballots[voter * words + (outcome >>> 6)] & bit) != 0) votes++;
            }
            if (votes >= required) result[outcome >>> 6] |= bit;
        }
        return result;
    }

    /**
     * Non-dominated candidate mask: each candidate has dimensions contiguous
     * finite double metrics, with larger being better in every dimension.
     * Identical candidates are retained; a strict improvement dominates.
     */
    public static long[] paretoFrontier(double[] packed, int candidates, int dimensions) {
        Objects.requireNonNull(packed, "packed");
        if (candidates < 0 || candidates > 4096 || dimensions < 1 || dimensions > 64
                || Math.multiplyExact(candidates, dimensions) != packed.length)
            throw new IllegalArgumentException("Pareto geometry");
        for (double value : packed) if (!Double.isFinite(value))
            throw new IllegalArgumentException("nonfinite Pareto score");
        long[] kept = new long[(candidates >>> 6) + ((candidates & 63) == 0 ? 0 : 1)];
        for (int i = 0; i < candidates; i++) {
            boolean dominated = false;
            for (int j = 0; j < candidates && !dominated; j++) {
                if (i == j) continue;
                boolean asGood = true, strictlyBetter = false;
                for (int d = 0; d < dimensions; d++) {
                    double left = packed[j * dimensions + d];
                    double right = packed[i * dimensions + d];
                    if (left < right) { asGood = false; break; }
                    if (left > right) strictlyBetter = true;
                }
                dominated = asGood && strictlyBetter;
            }
            if (!dominated) kept[i >>> 6] |= 1L << (i & 63);
        }
        return kept;
    }
}
