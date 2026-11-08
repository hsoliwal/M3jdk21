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
}
