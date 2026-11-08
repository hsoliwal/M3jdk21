// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Arrays;

//FLLDCIM: RELOCATE - donor blob c5ac13fda361 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/graph-trie-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/graph-trie-20261106/relocate.py v1
public final class GraphAlgorithms {
    private GraphAlgorithms() {
    }

    public record WeightedEdge<V>(V to, long weight) {
        public WeightedEdge {
            Objects.requireNonNull(to, "to");
            if (weight < 0L) {
                throw new IllegalArgumentException("Dijkstra weights must be non-negative");
            }
        }
    }

    public static <V> List<V> breadthFirst(
            Map<V, ? extends Collection<V>> graph,
            V start,
            ProgressMonitor monitor) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(start, "start");
        AlgorithmSupport.begin(monitor, "graph-bfs", graph.size());
        try {
            List<V> order = new ArrayList<>();
            Set<V> seen = new HashSet<>();
            ArrayDeque<V> queue = new ArrayDeque<>();
            seen.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                V vertex = queue.removeFirst();
                order.add(vertex);
                for (V next : neighbors(graph, vertex)) {
                    if (seen.add(next)) {
                        queue.addLast(next);
                    }
                }
            }
            return List.copyOf(order);
        } finally {
            monitor.done();
        }
    }

    public static <V> Map<V, Integer> shortestPaths(
            Map<V, ? extends Collection<V>> graph,
            V start,
            ProgressMonitor monitor) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(start, "start");
        AlgorithmSupport.begin(monitor, "unweighted-shortest-path", graph.size());
        try {
            Map<V, Integer> distance = new LinkedHashMap<>();
            ArrayDeque<V> queue = new ArrayDeque<>();
            distance.put(start, 0);
            queue.add(start);
            while (!queue.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                V vertex = queue.removeFirst();
                int nextDistance = distance.get(vertex) + 1;
                for (V next : neighbors(graph, vertex)) {
                    if (!distance.containsKey(next)) {
                        distance.put(next, nextDistance);
                        queue.addLast(next);
                    }
                }
            }
            return Map.copyOf(distance);
        } finally {
            monitor.done();
        }
    }

    public static <V> List<V> topologicalOrder(
            Map<V, ? extends Collection<V>> graph,
            Comparator<? super V> comparator,
            ProgressMonitor monitor) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(comparator, "comparator");
        AlgorithmSupport.begin(monitor, "topological-order", graph.size());
        try {
            Map<V, Integer> indegree = new HashMap<>();
            graph.forEach((vertex, edges) -> {
                indegree.putIfAbsent(vertex, 0);
                edges.forEach(next -> indegree.merge(next, 1, Integer::sum));
            });
            PriorityQueue<V> ready = new PriorityQueue<>(comparator);
            indegree.forEach((vertex, degree) -> {
                if (degree == 0) {
                    ready.add(vertex);
                }
            });
            List<V> order = new ArrayList<>(indegree.size());
            while (!ready.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                V vertex = ready.remove();
                order.add(vertex);
                for (V next : neighbors(graph, vertex)) {
                    int degree = indegree.compute(next, (ignored, value) -> value - 1);
                    if (degree == 0) {
                        ready.add(next);
                    }
                }
            }
            if (order.size() != indegree.size()) {
                throw new IllegalArgumentException("graph contains a directed cycle");
            }
            return List.copyOf(order);
        } finally {
            monitor.done();
        }
    }

    public static <V> Map<V, Long> dijkstra(
            Map<V, ? extends Collection<WeightedEdge<V>>> graph,
            V start,
            ProgressMonitor monitor) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(start, "start");
        AlgorithmSupport.begin(monitor, "dijkstra", graph.size());
        try {
            record QueueEntry<V>(V vertex, long distance) {}
            PriorityQueue<QueueEntry<V>> queue =
                    new PriorityQueue<>(Comparator.comparingLong(entry -> entry.distance()));
            Map<V, Long> distance = new HashMap<>();
            distance.put(start, 0L);
            queue.add(new QueueEntry<>(start, 0L));
            while (!queue.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                QueueEntry<V> current = queue.remove();
                if (current.distance() != distance.getOrDefault(current.vertex(), Long.MAX_VALUE)) {
                    continue;
                }
                for (WeightedEdge<V> edge : weightedNeighbors(graph, current.vertex())) {
                    // MAX is reachable, but any positive successor would exceed long range.
                    // The original sentinel route did not visit this successor.
                    if (current.distance() == Long.MAX_VALUE && edge.weight() > 0L) {
                        continue;
                    }
                    long candidate = Math.addExact(current.distance(), edge.weight());
                    Long previous = distance.get(edge.to());
                    if (previous == null || candidate < previous) {
                        distance.put(edge.to(), candidate);
                        queue.add(new QueueEntry<>(edge.to(), candidate));
                    }
                }
            }
            return Map.copyOf(distance);
        } finally {
            monitor.done();
        }
    }

    public static <V> List<List<V>> stronglyConnectedComponents(
            Map<V, ? extends Collection<V>> graph,
            Comparator<? super V> comparator,
            ProgressMonitor monitor) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(comparator, "comparator");
        Set<V> vertices = new HashSet<>(graph.keySet());
        graph.values().forEach(vertices::addAll);
        AlgorithmSupport.begin(monitor, "strongly-connected-components", vertices.size() * 2L);
        try {
            Map<V, List<V>> forward = normalized(graph, vertices, comparator);
            Map<V, List<V>> reverse = reverse(forward, vertices, comparator);
            Set<V> seen = new HashSet<>();
            List<V> finished = new ArrayList<>(vertices.size());

            List<V> starts = vertices.stream().sorted(comparator).toList();
            record Frame<V>(V vertex, boolean exit) {}
            for (V start : starts) {
                if (seen.contains(start)) {
                    continue;
                }
                ArrayDeque<Frame<V>> stack = new ArrayDeque<>();
                stack.push(new Frame<>(start, false));
                while (!stack.isEmpty()) {
                    monitor.checkCanceled();
                    Frame<V> frame = stack.pop();
                    if (frame.exit()) {
                        finished.add(frame.vertex());
                        continue;
                    }
                    if (!seen.add(frame.vertex())) {
                        continue;
                    }
                    monitor.worked(1);
                    stack.push(new Frame<>(frame.vertex(), true));
                    List<V> edges = forward.get(frame.vertex());
                    for (int index = edges.size() - 1; index >= 0; index--) {
                        V next = edges.get(index);
                        if (!seen.contains(next)) {
                            stack.push(new Frame<>(next, false));
                        }
                    }
                }
            }

            seen.clear();
            List<List<V>> components = new ArrayList<>();
            for (int index = finished.size() - 1; index >= 0; index--) {
                V start = finished.get(index);
                if (!seen.add(start)) {
                    continue;
                }
                List<V> members = new ArrayList<>();
                ArrayDeque<V> stack = new ArrayDeque<>();
                stack.push(start);
                while (!stack.isEmpty()) {
                    monitor.checkCanceled();
                    V vertex = stack.pop();
                    members.add(vertex);
                    monitor.worked(1);
                    for (V next : reverse.get(vertex)) {
                        if (seen.add(next)) {
                            stack.push(next);
                        }
                    }
                }
                members.sort(comparator);
                components.add(List.copyOf(members));
            }
            components.sort((left, right) -> comparator.compare(left.getFirst(), right.getFirst()));
            return List.copyOf(components);
        } finally {
            monitor.done();
        }
    }

    private static <V> Collection<V> neighbors(
            Map<V, ? extends Collection<V>> graph,
            V vertex) {
        Collection<V> edges = graph.get(vertex);
        return edges == null ? List.of() : edges;
    }

    private static <V> Collection<WeightedEdge<V>> weightedNeighbors(
            Map<V, ? extends Collection<WeightedEdge<V>>> graph,
            V vertex) {
        Collection<WeightedEdge<V>> edges = graph.get(vertex);
        return edges == null ? List.of() : edges;
    }

    private static <V> Map<V, List<V>> normalized(
            Map<V, ? extends Collection<V>> graph,
            Set<V> vertices,
            Comparator<? super V> comparator) {
        Map<V, List<V>> result = new HashMap<>();
        for (V vertex : vertices) {
            result.put(vertex, neighbors(graph, vertex).stream()
                    .sorted(comparator)
                    .toList());
        }
        return result;
    }

    private static <V> Map<V, List<V>> reverse(
            Map<V, List<V>> graph,
            Set<V> vertices,
            Comparator<? super V> comparator) {
        Map<V, List<V>> mutable = new HashMap<>();
        vertices.forEach(vertex -> mutable.put(vertex, new ArrayList<>()));
        graph.forEach((from, edges) -> edges.forEach(to -> mutable.get(to).add(from)));
        Map<V, List<V>> result = new HashMap<>();
        mutable.forEach((vertex, edges) -> {
            edges.sort(comparator);
            result.put(vertex, List.copyOf(edges));
        });
        return result;
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.CsrGraph from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public record CsrGraph(int vertices, int[] offsets, int[] edges) {
        public CsrGraph {
            validateCsr(vertices, offsets, edges);
            offsets = offsets.clone();
            edges = edges.clone();
        }

        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }

        int[] rawOffsets() { return offsets; }
        int[] rawEdges() { return edges; }

        public static CsrGraph directed(int vertices, int[] from, int[] to) {
            return build(vertices, from, to, false);
        }

        public static CsrGraph undirected(int vertices, int[] left, int[] right) {
            return build(vertices, left, right, true);
        }
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.WeightedCsrGraph from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public record WeightedCsrGraph(
            int vertices,
            int[] offsets,
            int[] edges,
            long[] weights) {
        public WeightedCsrGraph {
            validateCsr(vertices, offsets, edges);
            Objects.requireNonNull(weights, "weights");
            if (weights.length != edges.length) {
                throw new IllegalArgumentException("weights length mismatch");
            }
            for (long weight : weights) {
                if (weight < 0L) throw new IllegalArgumentException("Dijkstra weights must be non-negative");
            }
            offsets = offsets.clone();
            edges = edges.clone();
            weights = weights.clone();
        }

        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
        @Override public long[] weights() { return weights.clone(); }

        int[] rawOffsets() { return offsets; }
        int[] rawEdges() { return edges; }
        long[] rawWeights() { return weights; }

        public static WeightedCsrGraph directed(
                int vertices,
                int[] from,
                int[] to,
                long[] weights) {
            return buildWeighted(vertices, from, to, weights, false);
        }

        public static WeightedCsrGraph undirected(
                int vertices,
                int[] left,
                int[] right,
                long[] weights) {
            return buildWeighted(vertices, left, right, weights, true);
        }
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.EdgeList from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public record EdgeList(int vertices, int[] from, int[] to, long[] weights) {
        public EdgeList {
            if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(weights, "weights");
            if (from.length != to.length || from.length != weights.length) {
                throw new IllegalArgumentException("edge-list lengths differ");
            }
            for (int i = 0; i < from.length; i++) {
                checkVertex(vertices, from[i]);
                checkVertex(vertices, to[i]);
            }
            from = from.clone();
            to = to.clone();
            weights = weights.clone();
        }

        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
        @Override public long[] weights() { return weights.clone(); }

        int[] rawFrom() { return from; }
        int[] rawTo() { return to; }
        long[] rawWeights() { return weights; }
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.MstResult from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public record MstResult(long totalWeight, int edgesUsed, boolean connected) {}

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.dfsPreorder from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static int[] dfsPreorder(CsrGraph graph, int source) {
        Objects.requireNonNull(graph, "graph");
        checkVertex(graph.vertices(), source);
        boolean[] visited = new boolean[graph.vertices()];
        boolean[] scheduled = new boolean[graph.vertices()];
        int[] order = new int[graph.vertices()];
        int count = 0;
        int[] stack = new int[Math.max(1, graph.vertices())];
        int size = 0;
        stack[size++] = source;
        scheduled[source] = true;
        int[] offsets = graph.rawOffsets();
        int[] edges = graph.rawEdges();
        while (size > 0) {
            int vertex = stack[--size];
            if (visited[vertex]) continue;
            visited[vertex] = true;
            order[count++] = vertex;
            for (int edge = offsets[vertex + 1] - 1; edge >= offsets[vertex]; edge--) {
                int next = edges[edge];
                if (!scheduled[next]) {
                    scheduled[next] = true;
                    stack[size++] = next;
                }
            }
        }
        return Arrays.copyOf(order, count);
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.connectedComponents from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static int connectedComponents(CsrGraph undirectedGraph) {
        Objects.requireNonNull(undirectedGraph, "undirectedGraph");
        int n = undirectedGraph.vertices();
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int[] offsets = undirectedGraph.rawOffsets();
        int[] edges = undirectedGraph.rawEdges();
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            components++;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            visited[start] = true;
            while (head < tail) {
                int vertex = queue[head++];
                for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                    int next = edges[edge];
                    if (!visited[next]) {
                        visited[next] = true;
                        queue[tail++] = next;
                    }
                }
            }
        }
        return components;
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.isBipartite from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static boolean isBipartite(CsrGraph graph) {
        Objects.requireNonNull(graph, "graph");
        int n = graph.vertices();
        byte[] color = new byte[n];
        Arrays.fill(color, (byte) -1);
        int[] queue = new int[n];
        int[] offsets = graph.rawOffsets();
        int[] edges = graph.rawEdges();
        for (int start = 0; start < n; start++) {
            if (color[start] >= 0) continue;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            color[start] = 0;
            while (head < tail) {
                int vertex = queue[head++];
                for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                    int next = edges[edge];
                    if (color[next] < 0) {
                        color[next] = (byte) (color[vertex] ^ 1);
                        queue[tail++] = next;
                    } else if (color[next] == color[vertex]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.bellmanFord from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static long[] bellmanFord(EdgeList graph, int source) {
        Objects.requireNonNull(graph, "graph");
        checkVertex(graph.vertices(), source);
        long[] distance = new long[graph.vertices()];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0L;
        int[] from = graph.rawFrom();
        int[] to = graph.rawTo();
        long[] weights = graph.rawWeights();
        for (int pass = 1; pass < graph.vertices(); pass++) {
            boolean changed = false;
            for (int edge = 0; edge < from.length; edge++) {
                long base = distance[from[edge]];
                if (base == Long.MAX_VALUE) continue;
                long weight = weights[edge];
                long candidate = Math.addExact(base, weight);
                if (candidate < distance[to[edge]]) {
                    distance[to[edge]] = candidate;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        for (int edge = 0; edge < from.length; edge++) {
            long base = distance[from[edge]];
            if (base == Long.MAX_VALUE) continue;
            long weight = weights[edge];
            if (Math.addExact(base, weight) < distance[to[edge]]) {
                throw new IllegalArgumentException("reachable negative cycle");
            }
        }
        return distance;
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.unionFindComponents from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static int unionFindComponents(int vertices, int[] left, int[] right) {
        validateParallelEdges(vertices, left, right);
        PrimitiveAlgorithmSupport.DisjointSet set =
                new PrimitiveAlgorithmSupport.DisjointSet(vertices);
        for (int i = 0; i < left.length; i++) set.union(left[i], right[i]);
        return set.components();
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.hasUndirectedCycle from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static boolean hasUndirectedCycle(int vertices, int[] left, int[] right) {
        validateParallelEdges(vertices, left, right);
        PrimitiveAlgorithmSupport.DisjointSet set =
                new PrimitiveAlgorithmSupport.DisjointSet(vertices);
        for (int i = 0; i < left.length; i++) {
            if (!set.union(left[i], right[i])) return true;
        }
        return false;
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.kruskalMst from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    public static MstResult kruskalMst(
            int vertices,
            int[] left,
            int[] right,
            long[] weights) {
        validateParallelEdges(vertices, left, right);
        Objects.requireNonNull(weights, "weights");
        if (weights.length != left.length) throw new IllegalArgumentException("weights length mismatch");
        PrimitiveAlgorithmSupport.LongIntMinHeap heap =
                new PrimitiveAlgorithmSupport.LongIntMinHeap(weights.length);
        for (int edge = 0; edge < weights.length; edge++) {
            heap.add(weights[edge], edge);
        }
        PrimitiveAlgorithmSupport.DisjointSet set =
                new PrimitiveAlgorithmSupport.DisjointSet(vertices);
        long total = 0L;
        int used = 0;
        while (!heap.isEmpty() && used < Math.max(0, vertices - 1)) {
            long weight = heap.peekPriority();
            int edge = heap.peekValue();
            heap.removeMin();
            if (set.union(left[edge], right[edge])) {
                total = Math.addExact(total, weight);
                used++;
            }
        }
        boolean connected = vertices <= 1 || used == vertices - 1;
        return new MstResult(total, used, connected);
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.build from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static CsrGraph build(
            int vertices,
            int[] from,
            int[] to,
            boolean undirected) {
        validateParallelEdges(vertices, from, to);
        int multiplier = undirected ? 2 : 1;
        int edgeCount = Math.multiplyExact(from.length, multiplier);
        int[] offsets = new int[vertices + 1];
        for (int value : from) offsets[value + 1]++;
        if (undirected) for (int value : to) offsets[value + 1]++;
        prefix(offsets);
        int[] cursor = offsets.clone();
        int[] edges = new int[edgeCount];
        for (int i = 0; i < from.length; i++) {
            edges[cursor[from[i]]++] = to[i];
            if (undirected) edges[cursor[to[i]]++] = from[i];
        }
        return new CsrGraph(vertices, offsets, edges);
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.buildWeighted from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static WeightedCsrGraph buildWeighted(
            int vertices,
            int[] from,
            int[] to,
            long[] weights,
            boolean undirected) {
        validateParallelEdges(vertices, from, to);
        Objects.requireNonNull(weights, "weights");
        if (weights.length != from.length) throw new IllegalArgumentException("weights length mismatch");
        for (long weight : weights) {
            if (weight < 0L) throw new IllegalArgumentException("weights must be non-negative");
        }
        int multiplier = undirected ? 2 : 1;
        int edgeCount = Math.multiplyExact(from.length, multiplier);
        int[] offsets = new int[vertices + 1];
        for (int value : from) offsets[value + 1]++;
        if (undirected) for (int value : to) offsets[value + 1]++;
        prefix(offsets);
        int[] cursor = offsets.clone();
        int[] edges = new int[edgeCount];
        long[] csrWeights = new long[edgeCount];
        for (int i = 0; i < from.length; i++) {
            int slot = cursor[from[i]]++;
            edges[slot] = to[i];
            csrWeights[slot] = weights[i];
            if (undirected) {
                slot = cursor[to[i]]++;
                edges[slot] = from[i];
                csrWeights[slot] = weights[i];
            }
        }
        return new WeightedCsrGraph(vertices, offsets, edges, csrWeights);
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.validateCsr from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static void validateCsr(int vertices, int[] offsets, int[] edges) {
        if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(edges, "edges");
        if (offsets.length != vertices + 1) throw new IllegalArgumentException("offset length mismatch");
        if (offsets[0] != 0 || offsets[vertices] != edges.length) {
            throw new IllegalArgumentException("invalid CSR boundaries");
        }
        for (int i = 0; i < vertices; i++) {
            if (offsets[i] > offsets[i + 1]) throw new IllegalArgumentException("CSR offsets decrease");
        }
        for (int edge : edges) checkVertex(vertices, edge);
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.validateParallelEdges from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static void validateParallelEdges(int vertices, int[] left, int[] right) {
        if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) throw new IllegalArgumentException("edge arrays differ");
        for (int i = 0; i < left.length; i++) {
            checkVertex(vertices, left[i]);
            checkVertex(vertices, right[i]);
        }
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.checkVertex from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static void checkVertex(int vertices, int vertex) {
        if (vertex < 0 || vertex >= vertices) {
            throw new IllegalArgumentException("vertex out of range: " + vertex);
        }
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.prefix from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    private static void prefix(int[] offsets) {
        for (int i = 1; i < offsets.length; i++) offsets[i] += offsets[i - 1];
    }

    //FLLDCIM: FOLD - member PrimitiveGraphAlgorithms.zeroOneBfs from donor blob ebddcaa6767d (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveGraphAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-graph-fold/FOLD_MAP.tsv
    /** 0-1 BFS over a graph whose edge weights must be exactly 0 or 1. */
    public static long[] zeroOneBfs(GraphAlgorithms.WeightedCsrGraph graph, int source) {
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
