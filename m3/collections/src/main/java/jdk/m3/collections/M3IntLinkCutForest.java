/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Dynamic forest with int vertex values and primitive splay/aggregate lanes.
 * All valid operations reuse storage; O(log V) amortized time. Queries also mutate
 * preferred paths, so this owner is thread-confined. Int values make long path sums exact.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntLinkCutForest.java}
 * blob {@code 704c343383382b3b95dfc6b82d7f5088816e704c} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntLinkCutForest {
    private final int[] left;
    private final int[] right;
    private final int[] parent;
    private final int[] values;
    private final int[] sizes;
    private final int[] minimum;
    private final int[] maximum;
    private final long[] sums;
    private final boolean[] reversed;
    private final int[] ancestors;

    /** Starts with one isolated vertex per snapshotted input value. */
    public M3IntLinkCutForest(int[] initialValues) {
        values = Objects.requireNonNull(initialValues, "initialValues").clone();
        int n = values.length;
        left = new int[n]; right = new int[n]; parent = new int[n]; sizes = new int[n];
        minimum = values.clone(); maximum = values.clone(); sums = new long[n];
        reversed = new boolean[n]; ancestors = new int[n];
        Arrays.fill(left, -1); Arrays.fill(right, -1); Arrays.fill(parent, -1); Arrays.fill(sizes, 1);
        for (int vertex = 0; vertex < n; vertex++) { sums[vertex] = values[vertex]; }
    }

    public int vertices() { return values.length; }
    public int valueAt(int vertex) { return values[Objects.checkIndex(vertex, vertices())]; }
    public void setValue(int vertex, int value) {
        Objects.checkIndex(vertex, vertices()); access(vertex); values[vertex] = value; pull(vertex);
    }
    public boolean connected(int a, int b) {
        check(a, b); return a == b || root(a) == root(b);
    }
    /** Returns false for a self-link or cycle, preserving logical edges and values. */
    public boolean link(int a, int b) {
        check(a, b); makeRoot(a);
        if (root(b) == a) { return false; }
        parent[a] = b; return true;
    }
    /** Cuts only the named direct edge; false means that edge was absent. */
    public boolean cut(int a, int b) {
        check(a, b); makeRoot(a); access(b);
        if (left[b] != a || right[a] >= 0) { return false; }
        left[b] = -1; parent[a] = -1; pull(b); return true;
    }
    public long pathSum(int from, int to) { expose(from, to); return sums[to]; }
    public int pathMinimum(int from, int to) { expose(from, to); return minimum[to]; }
    public int pathMaximum(int from, int to) { expose(from, to); return maximum[to]; }
    /** Number of vertices on the path, including both endpoints. */
    public int pathSize(int from, int to) { expose(from, to); return sizes[to]; }

    /** Zero-based vertex ordinal in the orientation from -> to. */
    public int vertexAt(int from, int to, int ordinal) {
        expose(from, to); Objects.checkIndex(ordinal, sizes[to]);
        int vertex = to;
        while (true) {
            push(vertex); int before = left[vertex] < 0 ? 0 : sizes[left[vertex]];
            if (ordinal < before) { vertex = left[vertex]; }
            else if (ordinal == before) { splay(vertex); return vertex; }
            else { ordinal -= before + 1; vertex = right[vertex]; }
        }
    }

    private void check(int a, int b) { Objects.checkIndex(a, vertices()); Objects.checkIndex(b, vertices()); }
    private void expose(int from, int to) {
        check(from, to); makeRoot(from);
        if (root(to) != from) { throw new IllegalArgumentException("disconnected endpoints"); }
        access(to);
    }
    private void makeRoot(int vertex) { access(vertex); flip(vertex); }
    private int root(int vertex) {
        access(vertex); push(vertex);
        while (left[vertex] >= 0) { vertex = left[vertex]; push(vertex); }
        splay(vertex); return vertex;
    }
    private boolean auxiliaryRoot(int vertex) {
        int p = parent[vertex]; return p < 0 || left[p] != vertex && right[p] != vertex;
    }
    private void access(int vertex) {
        int previous = -1;
        for (int at = vertex; at >= 0; at = parent[at]) {
            splay(at); right[at] = previous;
            if (previous >= 0) { parent[previous] = at; }
            pull(at); previous = at;
        }
        splay(vertex);
    }
    private void flip(int vertex) {
        if (vertex < 0) { return; }
        int swap = left[vertex]; left[vertex] = right[vertex]; right[vertex] = swap;
        reversed[vertex] = !reversed[vertex];
    }
    private void push(int vertex) {
        if (reversed[vertex]) { flip(left[vertex]); flip(right[vertex]); reversed[vertex] = false; }
    }
    private void pull(int vertex) {
        sizes[vertex] = 1; sums[vertex] = values[vertex]; minimum[vertex] = values[vertex]; maximum[vertex] = values[vertex];
        int a = left[vertex], b = right[vertex];
        if (a >= 0) {
            sizes[vertex] += sizes[a]; sums[vertex] += sums[a];
            minimum[vertex] = Math.min(minimum[vertex], minimum[a]); maximum[vertex] = Math.max(maximum[vertex], maximum[a]);
        }
        if (b >= 0) {
            sizes[vertex] += sizes[b]; sums[vertex] += sums[b];
            minimum[vertex] = Math.min(minimum[vertex], minimum[b]); maximum[vertex] = Math.max(maximum[vertex], maximum[b]);
        }
    }
    private void rotate(int vertex) {
        int p = parent[vertex], g = parent[p]; boolean isRight = right[p] == vertex;
        int middle = isRight ? left[vertex] : right[vertex];
        if (!auxiliaryRoot(p)) { if (left[g] == p) { left[g] = vertex; } else { right[g] = vertex; } }
        parent[vertex] = g;
        if (isRight) { left[vertex] = p; right[p] = middle; }
        else { right[vertex] = p; left[p] = middle; }
        parent[p] = vertex;
        if (middle >= 0) { parent[middle] = p; }
        pull(p); pull(vertex);
    }
    private void splay(int vertex) {
        int count = 0, at = vertex; ancestors[count++] = at;
        while (!auxiliaryRoot(at)) { at = parent[at]; ancestors[count++] = at; }
        while (count > 0) { push(ancestors[--count]); }
        while (!auxiliaryRoot(vertex)) {
            int p = parent[vertex], g = parent[p];
            if (!auxiliaryRoot(p)) { rotate((left[p] == vertex) == (left[g] == p) ? p : vertex); }
            rotate(vertex);
        }
    }
}
