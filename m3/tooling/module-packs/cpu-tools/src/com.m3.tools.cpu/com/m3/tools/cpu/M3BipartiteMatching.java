// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic maximum-cardinality bipartite matching over immutable CSR input. */
public final class M3BipartiteMatching {
    private static final int MAX_PARTITION = 4096;
    private static final int MAX_EDGES = 1_000_000;
    private M3BipartiteMatching() { }

    /** Detached cardinality and inverse mate lanes; -1 means unmatched. */
    public record Matching(int[] leftToRight, int[] rightToLeft, int size) {
        public Matching {
            leftToRight = leftToRight.clone();
            rightToLeft = rightToLeft.clone();
            if (size < 0 || size > Math.min(leftToRight.length, rightToLeft.length))
                throw new IllegalArgumentException("matching size");
        }
        @Override public int[] leftToRight() { return leftToRight.clone(); }
        @Override public int[] rightToLeft() { return rightToLeft.clone(); }
    }

    /**
     * Each left vertex i owns edges [offsets[i],offsets[i+1]) into right-side IDs.
     * Duplicate edges are permitted. Does not mutate caller storage or create Edge objects.
     * Uses layered augmenting paths, with an explicit primitive DFS stack.
     */
    public static Matching maximum(int left, int right, int[] offsets, int[] targets) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(targets, "targets");
        if (left < 0 || right < 0 || left > MAX_PARTITION || right > MAX_PARTITION
                || targets.length > MAX_EDGES || offsets.length != left + 1
                || offsets[0] != 0 || offsets[left] != targets.length)
            throw new IllegalArgumentException("invalid bipartite CSR dimensions");
        for (int i = 1; i <= left; i++)
            if (offsets[i] < offsets[i - 1]) throw new IllegalArgumentException("CSR offset order");
        for (int value : targets)
            if (value < 0 || value >= right) throw new IllegalArgumentException("right vertex bounds");
        int[] leftMate = new int[left], rightMate = new int[right];
        Arrays.fill(leftMate, -1);
        Arrays.fill(rightMate, -1);
        int[] dist = new int[left], cursor = new int[left], queue = new int[left];
        int[] stack = new int[left], choice = new int[left];
        int matches = 0;
        while (true) {
            Arrays.fill(dist, -1);
            int head = 0, tail = 0, shortest = Integer.MAX_VALUE;
            for (int l = 0; l < left; l++) if (leftMate[l] < 0) {
                dist[l] = 0;
                queue[tail++] = l;
            }
            while (head < tail) {
                int l = queue[head++];
                if (dist[l] + 1 > shortest) continue;
                for (int e = offsets[l]; e < offsets[l + 1]; e++) {
                    int matched = rightMate[targets[e]];
                    if (matched < 0) shortest = Math.min(shortest, dist[l] + 1);
                    else if (dist[matched] < 0 && dist[l] + 1 < shortest) {
                        dist[matched] = dist[l] + 1;
                        queue[tail++] = matched;
                    }
                }
            }
            if (shortest == Integer.MAX_VALUE) break;
            System.arraycopy(offsets, 0, cursor, 0, left);
            for (int l = 0; l < left; l++) if (leftMate[l] < 0 && augment(l,
                    offsets, targets, leftMate, rightMate, dist, cursor, stack, choice, shortest)) matches++;
        }
        return new Matching(leftMate, rightMate, matches);
    }

    private static boolean augment(int start, int[] offsets, int[] targets, int[] leftMate,
                                   int[] rightMate, int[] dist, int[] cursor,
                                   int[] stack, int[] choice, int shortest) {
        int depth = 0;
        stack[0] = start;
        while (depth >= 0) {
            int l = stack[depth];
            if (cursor[l] == offsets[l + 1]) {
                dist[l] = -1;
                depth--;
                continue;
            }
            int r = targets[cursor[l]++];
            int matched = rightMate[r];
            if (matched < 0 && dist[l] + 1 == shortest) {
                int leafDepth = depth;
                while (depth >= 0) {
                    int currentLeft = stack[depth];
                    int currentRight = depth == leafDepth ? r : choice[depth];
                    leftMate[currentLeft] = currentRight;
                    rightMate[currentRight] = currentLeft;
                    depth--;
                }
                return true;
            }
            if (matched >= 0 && dist[matched] == dist[l] + 1 && dist[matched] + 1 <= shortest) {
                choice[depth] = r;
                stack[++depth] = matched;
            }
        }
        return false;
    }

}
