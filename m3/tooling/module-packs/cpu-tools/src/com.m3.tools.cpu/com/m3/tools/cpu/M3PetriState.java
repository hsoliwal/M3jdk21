// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Reachability for bounded 1-safe Petri nets: places are bits; transitions are primitive masks.
 * Refuses state-space truncation rather than misreporting unreachability.
 * No timed/stochastic/colored-net or general temporal-logic equivalence is implied.
 */
public final class M3PetriState {
    private M3PetriState() { }
    public record Graph(long[] markings, int[] parent, int[] transition) {
        public Graph { markings = markings.clone(); parent = parent.clone(); transition = transition.clone(); }
        @Override public long[] markings() { return markings.clone(); }
        @Override public int[] parent() { return parent.clone(); }
        @Override public int[] transition() { return transition.clone(); }
        public int indexOf(long marking) {
            for (int i = 0; i < markings.length; i++) if (markings[i] == marking) return i;
            return -1;
        }
    }

    public static Graph explore(int places, long initial, long[] pre, long[] post,
                                int maxStates) {
        Objects.requireNonNull(pre); Objects.requireNonNull(post);
        if (places < 0 || places > 62 || pre.length != post.length || pre.length > 1024
                || maxStates < 1 || maxStates > 100000)
            throw new IllegalArgumentException("Petri geometry or budget");
        long domain = places == 0 ? 0 : (1L << places) - 1;
        if ((initial & ~domain) != 0) throw new IllegalArgumentException("initial marking");
        for (int i = 0; i < pre.length; i++)
            if (((pre[i] | post[i]) & ~domain) != 0) throw new IllegalArgumentException("transition marking");
        // Exact open-addressed marking directory: values are index+1 (zero is empty).
        int capacity = 1;
        while (capacity < (long) maxStates * 4) capacity <<= 1;
        long[] keys = new long[capacity];
        int[] values = new int[capacity];
        long[] markings = new long[maxStates];
        int[] parent = new int[maxStates], transition = new int[maxStates];
        markings[0] = initial; parent[0] = -1; transition[0] = -1;
        int first = slot(initial, capacity); values[first] = 1; keys[first] = initial;
        int write = 1;
        for (int read = 0; read < write; read++) {
            long current = markings[read];
            for (int t = 0; t < pre.length; t++) {
                if ((current & pre[t]) != pre[t]) continue;
                long remaining = current & ~pre[t];
                if ((remaining & post[t]) != 0) continue; // safe capacity of each place is 1.
                long next = remaining | post[t];
                int s = slot(next, capacity);
                while (values[s] != 0 && keys[s] != next) s = (s + 1) & (capacity - 1);
                if (values[s] != 0) continue;
                if (write == maxStates) throw new IllegalStateException("Petri state budget exceeded");
                values[s] = write + 1; keys[s] = next;
                markings[write] = next; parent[write] = read; transition[write] = t;
                write++;
            }
        }
        return new Graph(Arrays.copyOf(markings, write), Arrays.copyOf(parent, write),
                Arrays.copyOf(transition, write));
    }

    private static int slot(long value, int capacity) {
        long hash = value ^ (value >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        return (int) hash & (capacity - 1);
    }
}
