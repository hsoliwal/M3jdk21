// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Advanced graph shapes implemented with primitive arrays. */
//FLLDCIM: RELOCATE - donor blob 3f85f4199eca at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class AdvancedGraphAlgorithms {
    private AdvancedGraphAlgorithms() {}

    /** 0-1 BFS over a graph whose edge weights must be exactly 0 or 1. */
    public static long[] zeroOneBfs(PrimitiveGraphAlgorithms.WeightedCsrGraph graph, int source) {
        Objects.requireNonNull(graph, "graph");
        if (source < 0 || source >= graph.vertices()) throw new IllegalArgumentException("source out of range");
        int n = graph.vertices();
        long[] distance = new long[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0L;

        int capacity = Math.max(4, n * 2 + 1);
        int[] deque = new int[capacity];
        int head = capacity / 2;
        int tail = head;
        deque[tail++] = source;

        int[] offsets = graph.rawOffsets();
        int[] edges = graph.rawEdges();
        long[] weights = graph.rawWeights();
        while (head < tail) {
            int vertex = deque[head++];
            long base = distance[vertex];
            for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                long weight = weights[edge];
                if (weight != 0L && weight != 1L) {
                    throw new IllegalArgumentException("0-1 BFS requires weights 0 or 1");
                }
                int next = edges[edge];
                long candidate = base + weight;
                if (candidate >= distance[next]) continue;
                distance[next] = candidate;
                if (weight == 0L) {
                    if (head == 0) {
                        int live = tail - head;
                        int[] expanded = new int[deque.length << 1];
                        int newHead = expanded.length / 2;
                        System.arraycopy(deque, head, expanded, newHead, live);
                        deque = expanded;
                        head = newHead;
                        tail = newHead + live;
                    }
                    deque[--head] = next;
                } else {
                    if (tail == deque.length) deque = Arrays.copyOf(deque, deque.length << 1);
                    deque[tail++] = next;
                }
            }
            if (head == tail) {
                head = deque.length / 2;
                tail = head;
            }
        }
        return distance;
    }

}
