// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * M3 array-only graph atoms: deterministic SCC components, reachability mask,
 * and directed integral max-flow. Adjacency uses CSR or parallel int/long lanes.
 * No per-edge Entry/Node objects, persistent native pointers or String copies.
 */
public final class M3ArrayGraph {
    private M3ArrayGraph() { }

    private static int vertices(int[] offsets, int[] targets) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(targets, "targets");
        if (offsets.length == 0 || offsets[0] != 0 || offsets[offsets.length - 1] != targets.length)
            throw new IllegalArgumentException("CSR geometry");
        int vertices = offsets.length - 1;
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1] || offsets[i] > targets.length)
                throw new IllegalArgumentException("CSR offsets");
        }
        for (int target : targets) {
            if (target < 0 || target >= vertices) throw new IllegalArgumentException("CSR target");
        }
        return vertices;
    }

    /**
     * One component id per original vertex. IDs are canonicalized by each
     * component's smallest original vertex (0,1,...); duplicate edges and
     * isolated vertices are valid. Iterative Kosaraju avoids recursive DFS.
     */
    public static int[] stronglyConnectedComponents(int[] offsets, int[] targets) {
        int n = vertices(offsets, targets);
        int[] order = new int[n], stack = new int[n], cursor = new int[n];
        boolean[] seen = new boolean[n];
        int count = 0;
        for (int root = 0; root < n; root++) {
            if (seen[root]) continue;
            int depth = 0;
            stack[depth++] = root;
            seen[root] = true;
            cursor[root] = offsets[root];
            while (depth > 0) {
                int node = stack[depth - 1];
                if (cursor[node] == offsets[node + 1]) {
                    order[count++] = node;
                    depth--;
                } else {
                    int next = targets[cursor[node]++];
                    if (!seen[next]) {
                        seen[next] = true;
                        cursor[next] = offsets[next];
                        stack[depth++] = next;
                    }
                }
            }
        }
        int[] reverseOffsets = new int[n + 1];
        for (int target : targets) reverseOffsets[target + 1] = Math.addExact(reverseOffsets[target + 1], 1);
        for (int i = 1; i <= n; i++) reverseOffsets[i] = Math.addExact(reverseOffsets[i], reverseOffsets[i - 1]);
        int[] positions = reverseOffsets.clone();
        int[] reversed = new int[targets.length];
        for (int source = 0; source < n; source++)
            for (int j = offsets[source]; j < offsets[source + 1]; j++)
                reversed[positions[targets[j]]++] = source;
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int groups = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = order[i];
            if (component[root] != -1) continue;
            int size = 0;
            stack[size++] = root;
            component[root] = groups;
            while (size != 0) {
                int node = stack[--size];
                for (int j = reverseOffsets[node]; j < reverseOffsets[node + 1]; j++) {
                    int other = reversed[j];
                    if (component[other] == -1) {
                        component[other] = groups;
                        stack[size++] = other;
                    }
                }
            }
            groups++;
        }
        int[] minimum = new int[groups], renumber = new int[groups];
        Arrays.fill(minimum, Integer.MAX_VALUE);
        for (int i = 0; i < n; i++) minimum[component[i]] = Math.min(minimum[component[i]], i);
        // Canonical component label by increasing minimum vertex, without boxed sorting.
        int[] belongs = new int[n]; Arrays.fill(belongs, -1);
        for (int i = 0; i < groups; i++) belongs[minimum[i]] = i;
        int nextId = 0;
        for (int i = 0; i < n; i++) if (belongs[i] != -1) renumber[belongs[i]] = nextId++;
        for (int i = 0; i < n; i++) component[i] = renumber[component[i]];
        return component;
    }

    /** Reachable vertex mask, including the source, over validated CSR geometry. */
    public static long[] reachableMask(int[] offsets, int[] targets, int source) {
        int n = vertices(offsets, targets);
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException("source");
        long[] bits = new long[(n >>> 6) + ((n & 63) == 0 ? 0 : 1)];
        int[] queue = new int[n];
        int first = 0, last = 0;
        bits[source >>> 6] |= 1L << (source & 63);
        queue[last++] = source;
        while (first < last) {
            int node = queue[first++];
            for (int j = offsets[node]; j < offsets[node + 1]; j++) {
                int other = targets[j];
                long bit = 1L << (other & 63);
                if ((bits[other >>> 6] & bit) == 0) {
                    bits[other >>> 6] |= bit;
                    queue[last++] = other;
                }
            }
        }
        return bits;
    }

    /**
     * Maximum s-t flow using Dinic and parallel primitive edge arrays. Each
     * capacity is nonnegative and flow overflow refuses rather than saturates.
     * Vertex count is capped to bound recursive blocking-flow stack depth.
     */
    public static long maximumFlow(int vertices, int[] from, int[] to, long[] capacity,
                                   int source, int sink) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(capacity, "capacity");
        if (vertices < 2 || vertices > 1024 || source < 0 || sink < 0
                || source >= vertices || sink >= vertices || source == sink
                || from.length != to.length || from.length != capacity.length
                || from.length > 1_000_000) throw new IllegalArgumentException("flow geometry");
        int[] head = new int[vertices]; Arrays.fill(head, -1);
        int[] targets = new int[Math.multiplyExact(from.length, 2)];
        int[] next = new int[targets.length];
        long[] residual = new long[targets.length];
        for (int edge = from.length - 1; edge >= 0; edge--) {
            if (from[edge] < 0 || to[edge] < 0 || from[edge] >= vertices || to[edge] >= vertices
                    || capacity[edge] < 0) throw new IllegalArgumentException("invalid flow edge");
            int forward = edge << 1, backward = forward + 1;
            targets[forward] = to[edge]; residual[forward] = capacity[edge];
            next[forward] = head[from[edge]]; head[from[edge]] = forward;
            targets[backward] = from[edge];
            next[backward] = head[to[edge]]; head[to[edge]] = backward;
        }
        long flow = 0;
        int[] level = new int[vertices], queue = new int[vertices], position = new int[vertices];
        while (levels(head, targets, next, residual, source, sink, level, queue)) {
            System.arraycopy(head, 0, position, 0, head.length);
            long pushed;
            while ((pushed = send(source, sink, Long.MAX_VALUE, targets, next, residual,
                                  position, level)) > 0) {
                flow = Math.addExact(flow, pushed);
            }
        }
        return flow;
    }

    /**
     * Reverse a validated CSR graph without per-edge objects. Each reversed row
     * is ordered by original source vertex; duplicate edges and self-edges remain.
     * Output owns only primitive structural metadata, not copies of String labels.
     */
    public static Csr transpose(int[] offsets, int[] targets) {
        int n = vertices(offsets, targets);
        int[] reversedOffsets = new int[n + 1];
        for (int target : targets) reversedOffsets[target + 1]++;
        for (int i = 1; i <= n; i++)
            reversedOffsets[i] = Math.addExact(reversedOffsets[i], reversedOffsets[i - 1]);
        int[] positions = reversedOffsets.clone();
        int[] reversedTargets = new int[targets.length];
        for (int source = 0; source < n; source++)
            for (int edge = offsets[source]; edge < offsets[source + 1]; edge++)
                reversedTargets[positions[targets[edge]]++] = source;
        return new Csr(reversedOffsets, reversedTargets);
    }

    /** Exact unweighted edge distances, -1 for unreachable vertices, 0 at source. */
    public static int[] shortestDistances(int[] offsets, int[] targets, int source) {
        int n = vertices(offsets, targets);
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException("source");
        int[] distances = new int[n], queue = new int[n];
        Arrays.fill(distances, -1);
        int first = 0, last = 0;
        distances[source] = 0;
        queue[last++] = source;
        while (first < last) {
            int node = queue[first++];
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                int next = targets[edge];
                if (distances[next] == -1) {
                    distances[next] = distances[node] + 1;
                    queue[last++] = next;
                }
            }
        }
        return distances;
    }

    /**
     * Undirected/weak components of directed CSR edges. Primitive union-by-rank
     * and path compression; IDs ordered by each component's smallest vertex.
     * The existing directed SCC operation remains distinct and unchanged.
     */
    public static int[] weaklyConnectedComponents(int[] offsets, int[] targets) {
        int n = vertices(offsets, targets);
        int[] parents = new int[n];
        byte[] ranks = new byte[n];
        for (int i = 0; i < n; i++) parents[i] = i;
        for (int source = 0; source < n; source++) {
            for (int edge = offsets[source]; edge < offsets[source + 1]; edge++) {
                int a = root(parents, source), b = root(parents, targets[edge]);
                if (a == b) continue;
                if (ranks[a] < ranks[b]) parents[a] = b;
                else {
                    parents[b] = a;
                    if (ranks[a] == ranks[b]) ranks[a]++;
                }
            }
        }
        int[] ids = new int[n], output = new int[n];
        Arrays.fill(ids, -1);
        int nextId = 0;
        for (int i = 0; i < n; i++) {
            int parent = root(parents, i);
            if (ids[parent] == -1) ids[parent] = nextId++;
            output[i] = ids[parent];
        }
        return output;
    }

    private static int root(int[] parents, int vertex) {
        int root = vertex;
        while (parents[root] != root) root = parents[root];
        while (vertex != root) {
            int next = parents[vertex];
            parents[vertex] = root;
            vertex = next;
        }
        return root;
    }

    /**
     * Reflexive reachability, row-major packed masks. Each row has ceil(V/64)
     * long words. Refuses before allocation if V*ceil(V/64) exceeds maxWords or
     * a Java array length. Padding bits stay zero. A single primitive queue is
     * reused for every source. The word budget bounds output, not execution time;
     * the worst-case scan cost is O(V*(V+E)), and no all-pairs cache is retained.
     */
    public static long[] transitiveClosure(int[] offsets, int[] targets, long maxWords) {
        int n = vertices(offsets, targets);
        if (maxWords < 0) throw new IllegalArgumentException("negative word budget");
        int words = (int) (((long) n + 63) >>> 6);
        long total = (long) n * words;
        if (total > maxWords || total > Integer.MAX_VALUE - 8L)
            throw new IllegalArgumentException("reachability output exceeds word budget");
        long[] output = new long[(int) total];
        int[] queue = new int[n];
        for (int source = 0; source < n; source++) {
            int base = source * words;
            int first = 0, last = 0;
            output[base + (source >>> 6)] |= 1L << (source & 63);
            queue[last++] = source;
            while (first < last) {
                int node = queue[first++];
                for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                    int next = targets[edge], slot = base + (next >>> 6);
                    long bit = 1L << (next & 63);
                    if ((output[slot] & bit) == 0) {
                        output[slot] |= bit;
                        queue[last++] = next;
                    }
                }
            }
        }
        return output;
    }

    /** Immutable CSR result; requested array exports are independent mutable copies. */
    public static final class Csr {
        private final int[] offsets;
        private final int[] targets;
        private Csr(int[] offsets, int[] targets) {
            this.offsets = offsets;
            this.targets = targets;
        }
        public int vertexCount() { return offsets.length - 1; }
        public int edgeCount() { return targets.length; }
        public int start(int vertex) { return offsets[Objects.checkIndex(vertex, vertexCount())]; }
        public int end(int vertex) { return offsets[Objects.checkIndex(vertex, vertexCount()) + 1]; }
        public int target(int edge) { return targets[Objects.checkIndex(edge, targets.length)]; }
        public int[] offsets() { return offsets.clone(); }
        public int[] targets() { return targets.clone(); }
    }

    /**
     * Contract strongly connected components into a simple directed acyclic CSR
     * graph. Reuses the existing canonical SCC labels (increasing minimum vertex).
     * Internal edges disappear; duplicate cross-component edges coalesce and rows
     * are sorted. maxEdges bounds admitted INPUT edges before derived allocation;
     * it is not a time budget. Scratch is O(V+E), sorting costs O(E log E).
     */
    public static Condensation condensation(int[] offsets, int[] targets, int maxEdges) {
        int n = vertices(offsets, targets);
        if (maxEdges < 0 || targets.length > maxEdges)
            throw new IllegalArgumentException("condensation input edge budget");
        int[] components = stronglyConnectedComponents(offsets, targets);
        int groups = 0;
        for (int component : components) groups = Math.max(groups, component + 1);
        long[] edges = new long[targets.length];
        int used = 0;
        for (int source = 0; source < n; source++) {
            int from = components[source];
            for (int edge = offsets[source]; edge < offsets[source + 1]; edge++) {
                int to = components[targets[edge]];
                if (from != to) edges[used++] = ((long) from << 32) | (to & 0xffffffffL);
            }
        }
        Arrays.sort(edges, 0, used);
        int unique = 0;
        long previous = -1;
        for (int i = 0; i < used; i++) {
            long edge = edges[i];
            if (edge != previous) edges[unique++] = edge;
            previous = edge;
        }
        int[] contractedOffsets = new int[groups + 1];
        int[] contractedTargets = new int[unique];
        for (int i = 0; i < unique; i++) {
            contractedOffsets[(int) (edges[i] >>> 32) + 1]++;
            contractedTargets[i] = (int) edges[i];
        }
        for (int i = 1; i <= groups; i++)
            contractedOffsets[i] = Math.addExact(contractedOffsets[i], contractedOffsets[i - 1]);
        return new Condensation(components, new Csr(contractedOffsets, contractedTargets));
    }

    /** Immutable primitive component map plus the existing CSR result owner. */
    public static final class Condensation {
        private final int[] components;
        private final Csr graph;
        private Condensation(int[] components, Csr graph) {
            this.components = components;
            this.graph = graph;
        }
        public int vertexCount() { return components.length; }
        public int componentCount() { return graph.vertexCount(); }
        public int component(int vertex) {
            return components[Objects.checkIndex(vertex, components.length)];
        }
        public int[] components() { return components.clone(); }
        public Csr graph() { return graph; }
    }

    private static boolean levels(int[] head, int[] to, int[] next, long[] residual,
                                  int source, int sink, int[] level, int[] queue) {
        Arrays.fill(level, -1);
        int first = 0, last = 0;
        queue[last++] = source; level[source] = 0;
        while (first < last) {
            int node = queue[first++];
            for (int e = head[node]; e != -1; e = next[e]) {
                if (residual[e] > 0 && level[to[e]] == -1) {
                    level[to[e]] = level[node] + 1;
                    queue[last++] = to[e];
                }
            }
        }
        return level[sink] != -1;
    }

    private static long send(int node, int sink, long remaining, int[] to, int[] next,
                             long[] residual, int[] position, int[] level) {
        if (node == sink) return remaining;
        for (int e = position[node]; e != -1; e = position[node] = next[e]) {
            int other = to[e];
            if (residual[e] <= 0 || level[other] != level[node] + 1) continue;
            long pushed = send(other, sink, Math.min(remaining, residual[e]),
                               to, next, residual, position, level);
            if (pushed > 0) {
                residual[e] -= pushed;
                residual[e ^ 1] = Math.addExact(residual[e ^ 1], pushed);
                return pushed;
            }
        }
        return 0;
    }
}
