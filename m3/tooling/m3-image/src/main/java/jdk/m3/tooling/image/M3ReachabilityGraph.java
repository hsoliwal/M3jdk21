/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Immutable deterministic compressed-sparse-row graph for application-class reachability. */
public final class M3ReachabilityGraph {
    private final List<String> vertices;
    private final int[] offsets;
    private final int[] edges;
    private final int[] roots;

    private M3ReachabilityGraph(List<String> vertices, int[] offsets, int[] edges, int[] roots) {
        this.vertices = List.copyOf(vertices);
        this.offsets = offsets.clone();
        this.edges = edges.clone();
        this.roots = roots.clone();
        validate();
    }

    /** Builds a canonical graph from the sorted class universe and admitted roots. */
    public static M3ReachabilityGraph from(Map<String, M3ClassInfo> classes, Set<String> roots) {
        if (classes == null || roots == null) {
            throw new IllegalArgumentException("classes and roots must not be null");
        }
        List<String> vertices = new ArrayList<>(classes.keySet());
        Collections.sort(vertices);
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < vertices.size(); index++) {
            String name = vertices.get(index);
            if (name == null || name.isBlank() || indexes.put(name, index) != null) {
                throw new IllegalArgumentException("Invalid or duplicate graph vertex: " + name);
            }
        }

        int[] offsets = new int[vertices.size() + 1];
        for (int vertex = 0; vertex < vertices.size(); vertex++) {
            M3ClassInfo info = classes.get(vertices.get(vertex));
            if (info == null) {
                throw new IllegalArgumentException("Missing class facts for " + vertices.get(vertex));
            }
            int degree = 0;
            for (String reference : info.referencedClasses()) {
                if (indexes.containsKey(reference)) {
                    degree = Math.addExact(degree, 1);
                }
            }
            offsets[vertex + 1] = Math.addExact(offsets[vertex], degree);
        }
        int[] edges = new int[offsets[offsets.length - 1]];
        for (int vertex = 0; vertex < vertices.size(); vertex++) {
            int output = offsets[vertex];
            for (String reference : classes.get(vertices.get(vertex)).referencedClasses()) {
                Integer target = indexes.get(reference);
                if (target != null) {
                    edges[output++] = target;
                }
            }
            if (output != offsets[vertex + 1]) {
                throw new IllegalStateException("Graph degree drift for " + vertices.get(vertex));
            }
        }

        TreeSet<String> orderedRootNames = new TreeSet<>(roots);
        int[] rootIndexes = new int[orderedRootNames.size()];
        int rootOutput = 0;
        for (String root : orderedRootNames) {
            Integer index = indexes.get(root);
            if (index == null) {
                throw new IllegalArgumentException("Graph root is not in the class universe: " + root);
            }
            rootIndexes[rootOutput++] = index;
        }
        java.util.Arrays.sort(rootIndexes);
        return new M3ReachabilityGraph(vertices, offsets, edges, rootIndexes);
    }

    /** Returns the number of graph vertices. */
    public int vertexCount() {
        return vertices.size();
    }

    /** Returns the canonical vertex names. */
    public List<String> vertices() {
        return vertices;
    }

    /** Returns a defensive copy of the CSR offsets. */
    public int[] offsets() {
        return offsets.clone();
    }

    /** Returns a defensive copy of the CSR edges. */
    public int[] edges() {
        return edges.clone();
    }

    /** Returns a defensive copy of the root indexes. */
    public int[] roots() {
        return roots.clone();
    }

    int[] offsetsView() {
        return offsets;
    }

    int[] edgesView() {
        return edges;
    }

    int[] rootsView() {
        return roots;
    }

    /** Converts and validates a sorted closure into canonical class names. */
    public Set<String> namesFor(int[] closure) {
        int[] validated = validateClosure(closure);
        TreeSet<String> names = new TreeSet<>();
        for (int index : validated) {
            names.add(vertices.get(index));
        }
        return Collections.unmodifiableSet(names);
    }

    int[] validateClosure(int[] closure) {
        if (closure == null) {
            throw new IllegalArgumentException("Reachability closure must not be null");
        }
        int previous = -1;
        for (int index : closure) {
            if (index < 0 || index >= vertices.size()) {
                throw new IllegalArgumentException("Reachability index outside graph: " + index);
            }
            if (index <= previous) {
                throw new IllegalArgumentException("Reachability closure must be sorted and unique");
            }
            previous = index;
        }
        for (int root : roots) {
            if (java.util.Arrays.binarySearch(closure, root) < 0) {
                throw new IllegalArgumentException("Reachability closure omitted root index " + root);
            }
        }
        return closure.clone();
    }

    private void validate() {
        if (offsets.length != vertices.size() + 1 || offsets[0] != 0
                || offsets[offsets.length - 1] != edges.length) {
            throw new IllegalArgumentException("Invalid compressed-sparse-row shape");
        }
        for (int vertex = 0; vertex < vertices.size(); vertex++) {
            if (offsets[vertex] > offsets[vertex + 1]) {
                throw new IllegalArgumentException("CSR offsets must be monotone");
            }
            int previous = -1;
            for (int index = offsets[vertex]; index < offsets[vertex + 1]; index++) {
                int target = edges[index];
                if (target < 0 || target >= vertices.size() || target <= previous) {
                    throw new IllegalArgumentException("CSR edges must be in-range, sorted, and unique");
                }
                previous = target;
            }
        }
        int previousRoot = -1;
        for (int root : roots) {
            if (root < 0 || root >= vertices.size() || root <= previousRoot) {
                throw new IllegalArgumentException("Roots must be in-range, sorted, and unique");
            }
            previousRoot = root;
        }
    }
}
