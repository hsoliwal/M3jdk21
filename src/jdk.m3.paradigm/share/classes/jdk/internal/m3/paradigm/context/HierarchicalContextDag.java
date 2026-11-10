/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable DAG-of-DAG descriptor and bounded recursive expander.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dag/HierarchicalContextDag.java}
 * blob {@code 13f775e0f52972add08dd04ab01790a58fb3990b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class HierarchicalContextDag {
    public enum TargetKind { SKILL, DAG }

    public record Node(String id, TargetKind kind, String targetId, Set<String> dependsOn) {
        public Node {
            id = required(id, "node id");
            kind = Objects.requireNonNull(kind, "kind");
            targetId = required(targetId, "targetId");
            dependsOn = dependsOn == null ? Set.of() : Set.copyOf(new TreeSet<>(dependsOn));
            if (dependsOn.contains(id)) throw new IllegalArgumentException("DAG_SELF_DEPENDENCY:" + id);
        }

        public static Node skill(String id, String skillId, Set<String> dependsOn) {
            return new Node(id, TargetKind.SKILL, skillId, dependsOn);
        }

        public static Node dag(String id, String dagId, Set<String> dependsOn) {
            return new Node(id, TargetKind.DAG, dagId, dependsOn);
        }
    }

    public record Dag(String id, Set<String> tags, List<Node> nodes) {
        public Dag {
            id = required(id, "dag id");
            tags = tags == null ? Set.of() : Set.copyOf(new TreeSet<>(tags));
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            if (nodes.isEmpty()) throw new IllegalArgumentException("DAG_EMPTY:" + id);
            validateLocalGraph(id, nodes);
        }
    }

    public record Step(String id, String skillId, Set<String> dependsOn) {
        public Step {
            id = required(id, "step id");
            skillId = required(skillId, "skillId");
            dependsOn = Set.copyOf(new TreeSet<>(Objects.requireNonNull(dependsOn, "dependsOn")));
        }
    }

    public record Plan(List<Step> steps, Set<String> roots, Set<String> sinks) {
        public Plan {
            steps = List.copyOf(steps);
            roots = Set.copyOf(new TreeSet<>(roots));
            sinks = Set.copyOf(new TreeSet<>(sinks));
        }
    }

    private record Fragment(Set<String> roots, Set<String> sinks) {}

    private final ContextSkillInventory inventory;
    private final Map<String, Dag> dags;

    public HierarchicalContextDag(ContextSkillInventory inventory, Collection<Dag> dags) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        TreeMap<String, Dag> map = new TreeMap<>();
        Objects.requireNonNull(dags, "dags").forEach(dag -> {
            Dag value = Objects.requireNonNull(dag, "dag");
            if (map.putIfAbsent(value.id(), value) != null) {
                throw new IllegalArgumentException("DUPLICATE_DAG:" + value.id());
            }
        });
        this.dags = Map.copyOf(map);
    }

    public Map<String, Dag> dags() {
        return dags;
    }

    public Plan expand(String dagId, int maxDepth, int maxNodes) {
        if (maxDepth < 1 || maxDepth > 64 || maxNodes < 1 || maxNodes > 8192) {
            throw new IllegalArgumentException("DAG_EXPANSION_BUDGET_INVALID");
        }
        List<Step> steps = new ArrayList<>();
        Fragment fragment = expand(required(dagId, "dagId"), dagId, Set.of(),
                new ArrayDeque<>(), steps, maxDepth, maxNodes);
        return new Plan(steps, fragment.roots(), fragment.sinks());
    }

    private Fragment expand(
            String dagId,
            String prefix,
            Set<String> externalDependencies,
            Deque<String> stack,
            List<Step> output,
            int maxDepth,
            int maxNodes) {
        if (stack.size() >= maxDepth) throw new IllegalArgumentException("DAG_MAX_DEPTH_EXCEEDED");
        if (stack.contains(dagId)) throw new IllegalArgumentException("DAG_RECURSION_CYCLE:" + dagId);
        Dag dag = requireDag(dagId);
        stack.addLast(dagId);
        try {
            Map<String, Fragment> fragments = new HashMap<>();
            Set<String> referenced = new HashSet<>();
            dag.nodes().forEach(node -> referenced.addAll(node.dependsOn()));
            for (Node node : topological(dag.nodes())) {
                TreeSet<String> dependencies = new TreeSet<>();
                if (node.dependsOn().isEmpty()) {
                    dependencies.addAll(externalDependencies);
                } else {
                    node.dependsOn().forEach(dep -> dependencies.addAll(
                            Objects.requireNonNull(fragments.get(dep), "dependency fragment").sinks()));
                }
                Fragment fragment;
                String expandedId = prefix + "." + node.id();
                if (node.kind() == TargetKind.SKILL) {
                    inventory.require(node.targetId());
                    if (output.size() >= maxNodes) {
                        throw new IllegalArgumentException("DAG_MAX_NODES_EXCEEDED");
                    }
                    output.add(new Step(expandedId, node.targetId(), dependencies));
                    fragment = new Fragment(Set.of(expandedId), Set.of(expandedId));
                } else {
                    fragment = expand(node.targetId(), expandedId, dependencies, stack, output,
                            maxDepth, maxNodes);
                }
                fragments.put(node.id(), fragment);
            }
            TreeSet<String> roots = new TreeSet<>();
            TreeSet<String> sinks = new TreeSet<>();
            dag.nodes().forEach(node -> {
                if (node.dependsOn().isEmpty()) roots.addAll(fragments.get(node.id()).roots());
                if (!referenced.contains(node.id())) sinks.addAll(fragments.get(node.id()).sinks());
            });
            return new Fragment(Collections.unmodifiableSet(roots), Collections.unmodifiableSet(sinks));
        } finally {
            stack.removeLast();
        }
    }

    private Dag requireDag(String id) {
        Dag dag = dags.get(id);
        if (dag == null) throw new IllegalArgumentException("UNKNOWN_DAG:" + id);
        return dag;
    }

    private static List<Node> topological(List<Node> nodes) {
        Map<String, Node> byId = new TreeMap<>();
        Map<String, Integer> indegree = new TreeMap<>();
        Map<String, Set<String>> outgoing = new TreeMap<>();
        nodes.forEach(node -> {
            byId.put(node.id(), node);
            indegree.put(node.id(), node.dependsOn().size());
            outgoing.put(node.id(), new TreeSet<>());
        });
        nodes.forEach(node -> node.dependsOn().forEach(dep -> outgoing.get(dep).add(node.id())));
        TreeSet<String> ready = new TreeSet<>();
        indegree.forEach((id, degree) -> { if (degree == 0) ready.add(id); });
        List<Node> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.pollFirst();
            ordered.add(byId.get(id));
            outgoing.get(id).forEach(next -> {
                int degree = indegree.compute(next, (ignored, value) -> value - 1);
                if (degree == 0) ready.add(next);
            });
        }
        if (ordered.size() != nodes.size()) throw new IllegalArgumentException("DAG_CYCLE");
        return List.copyOf(ordered);
    }

    private static void validateLocalGraph(String dagId, List<Node> nodes) {
        Set<String> ids = new HashSet<>();
        nodes.forEach(node -> {
            if (!ids.add(node.id())) throw new IllegalArgumentException("DAG_DUPLICATE_NODE:" + node.id());
        });
        nodes.forEach(node -> {
            if (!ids.containsAll(node.dependsOn())) {
                throw new IllegalArgumentException("DAG_UNKNOWN_DEPENDENCY:" + dagId + ":" + node.id());
            }
        });
        topological(nodes);
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > 192) {
            throw new IllegalArgumentException("DAG_" + field.replace(' ', '_').toUpperCase() + "_INVALID");
        }
        return normalized;
    }
}
