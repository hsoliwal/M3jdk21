// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/**
 * UTF-16 code-unit trie using primitive adjacency arrays instead of one object/map per node.
 *
 * <p>Edges are stored as linked primitive records. This favors compact deterministic storage
 * and simple serialization over hash-table lookup at each node.</p>
 */
//FLLDCIM: RELOCATE - donor blob 0a6ffb762d2e at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class PrimitiveTrie {
    private int[] firstEdge;
    private boolean[] terminal;
    private char[] edgeLabel;
    private int[] edgeTarget;
    private int[] edgeNext;
    private int nodes = 1;
    private int edges;

    public PrimitiveTrie() {
        firstEdge = new int[16];
        Arrays.fill(firstEdge, -1);
        terminal = new boolean[16];
        edgeLabel = new char[32];
        edgeTarget = new int[32];
        edgeNext = new int[32];
    }

    public int nodeCount() { return nodes; }
    public int edgeCount() { return edges; }

    public void add(String value) {
        Objects.requireNonNull(value, "value");
        int node = 0;
        for (int index = 0; index < value.length(); index++) {
            char label = value.charAt(index);
            int edge = findEdge(node, label);
            if (edge < 0) {
                int target = newNode();
                edge = newEdge(label, target, firstEdge[node]);
                firstEdge[node] = edge;
            }
            node = edgeTarget[edge];
        }
        terminal[node] = true;
    }

    public boolean contains(String value) {
        int node = findNode(value);
        return node >= 0 && terminal[node];
    }

    public boolean hasPrefix(String prefix) {
        return findNode(prefix) >= 0;
    }

    public int longestStoredPrefixLength(String value) {
        Objects.requireNonNull(value, "value");
        int node = 0;
        int best = terminal[0] ? 0 : -1;
        for (int index = 0; index < value.length(); index++) {
            int edge = findEdge(node, value.charAt(index));
            if (edge < 0) break;
            node = edgeTarget[edge];
            if (terminal[node]) best = index + 1;
        }
        return best;
    }

    private int findNode(String value) {
        Objects.requireNonNull(value, "value");
        int node = 0;
        for (int index = 0; index < value.length(); index++) {
            int edge = findEdge(node, value.charAt(index));
            if (edge < 0) return -1;
            node = edgeTarget[edge];
        }
        return node;
    }

    private int findEdge(int node, char label) {
        for (int edge = firstEdge[node]; edge >= 0; edge = edgeNext[edge]) {
            if (edgeLabel[edge] == label) return edge;
        }
        return -1;
    }

    private int newNode() {
        if (nodes == firstEdge.length) {
            int old = firstEdge.length;
            firstEdge = Arrays.copyOf(firstEdge, old << 1);
            Arrays.fill(firstEdge, old, firstEdge.length, -1);
            terminal = Arrays.copyOf(terminal, terminal.length << 1);
        }
        firstEdge[nodes] = -1;
        return nodes++;
    }

    private int newEdge(char label, int target, int next) {
        if (edges == edgeLabel.length) {
            int capacity = edgeLabel.length << 1;
            edgeLabel = Arrays.copyOf(edgeLabel, capacity);
            edgeTarget = Arrays.copyOf(edgeTarget, capacity);
            edgeNext = Arrays.copyOf(edgeNext, capacity);
        }
        int edge = edges++;
        edgeLabel[edge] = label;
        edgeTarget[edge] = target;
        edgeNext[edge] = next;
        return edge;
    }
}
