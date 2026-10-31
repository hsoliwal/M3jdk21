// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, framework-neutral DAG of M3 backport work atoms.
 *
 * <p>The DAG is the semantic authority. Camel, Airflow, Drools, CI and other orchestrators are
 * projections over this graph and may not change dependency or scope semantics.</p>
 */
public final class M3RecipeDag {
    private final Map<String, M3DagNode> nodes;
    private final List<M3DagNode> topologicalOrder;
    private final List<List<M3DagNode>> layers;

    public M3RecipeDag(Collection<M3DagNode> source) {
        Objects.requireNonNull(source, "source");
        LinkedHashMap<String, M3DagNode> indexed = new LinkedHashMap<>();
        for (M3DagNode node : source) {
            if (indexed.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("duplicate DAG node: " + node.id());
            }
        }
        if (indexed.isEmpty()) {
            throw new IllegalArgumentException("empty DAG");
        }
        validateDependencies(indexed);
        this.nodes = Map.copyOf(indexed);
        this.layers = buildLayers(indexed);
        this.topologicalOrder = layers.stream().flatMap(List::stream).toList();
        validateScopePromotion(topologicalOrder);
        validatePromotion(topologicalOrder);
    }

    public static M3RecipeDag canonical() {
        return M3BackportDagLoader.load("m3-backport-dag.tsv");
    }

    public List<M3DagNode> topologicalOrder() {
        return topologicalOrder;
    }

    public List<List<M3DagNode>> layers() {
        return layers;
    }

    public M3DagNode require(String id) {
        M3DagNode node = nodes.get(Objects.requireNonNull(id, "id"));
        if (node == null) {
            throw new IllegalArgumentException("unknown DAG node: " + id);
        }
        return node;
    }

    public int size() {
        return nodes.size();
    }

    private static void validateDependencies(Map<String, M3DagNode> nodes) {
        for (M3DagNode node : nodes.values()) {
            for (String dependency : node.dependsOn()) {
                if (!nodes.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            "missing dependency " + dependency + " for " + node.id());
                }
            }
        }
    }

    private static List<List<M3DagNode>> buildLayers(Map<String, M3DagNode> nodes) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();
        nodes.values().forEach(node -> {
            indegree.put(node.id(), node.dependsOn().size());
            node.dependsOn().forEach(dependency ->
                    dependents.computeIfAbsent(dependency, ignored -> new LinkedHashSet<>())
                            .add(node.id()));
        });

        ArrayDeque<String> ready = new ArrayDeque<>(nodes.values().stream()
                .filter(node -> indegree.get(node.id()) == 0)
                .map(M3DagNode::id)
                .sorted()
                .toList());
        ArrayList<List<M3DagNode>> result = new ArrayList<>();
        int visited = 0;

        while (!ready.isEmpty()) {
            List<String> ids = new ArrayList<>();
            while (!ready.isEmpty()) {
                ids.add(ready.removeFirst());
            }
            ids.sort(Comparator.naturalOrder());
            result.add(List.copyOf(ids.stream().map(nodes::get).toList()));
            visited += ids.size();

            ArrayList<String> next = new ArrayList<>();
            for (String id : ids) {
                for (String dependent : dependents.getOrDefault(id, Set.of())) {
                    int remaining = indegree.compute(dependent, (key, value) -> value - 1);
                    if (remaining == 0) {
                        next.add(dependent);
                    }
                }
            }
            next.stream().sorted().forEach(ready::addLast);
        }

        if (visited != nodes.size()) {
            throw new IllegalArgumentException("DAG contains a dependency cycle");
        }
        return List.copyOf(result);
    }

    private static void validateScopePromotion(List<M3DagNode> order) {
        Map<String, M3EditScope> inherited = new HashMap<>();
        for (M3DagNode node : order) {
            M3EditScope ancestor = null;
            for (String dependency : node.dependsOn()) {
                M3EditScope scope = inherited.get(dependency);
                if (scope != null) {
                    ancestor = ancestor == null ? scope : ancestor.promote(scope);
                }
            }

            if (node.mutating()) {
                if (ancestor != null
                        && !ancestor.permits(node.scope())
                        && !node.scopePromotionApproved()) {
                    throw new IllegalArgumentException(
                            "silent scope escalation at " + node.id()
                                    + ": " + ancestor + " -> " + node.scope());
                }
                inherited.put(
                        node.id(),
                        ancestor == null ? node.scope() : ancestor.promote(node.scope()));
            } else if (ancestor != null) {
                inherited.put(node.id(), ancestor);
            }
        }
    }

    private static void validatePromotion(List<M3DagNode> order) {
        List<M3DagNode> promotions = order.stream()
                .filter(node -> node.kind() == M3DagKind.PROMOTION)
                .toList();
        if (promotions.size() != 1) {
            throw new IllegalArgumentException("DAG requires exactly one canonical promotion node");
        }
        M3DagNode promotion = promotions.getFirst();
        if (!promotion.serialPromotion() || order.getLast() != promotion) {
            throw new IllegalArgumentException("canonical promotion must be serial and last");
        }
    }
}
