/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

/** Complete admission happens before the first host callback is invoked. */
public final class PlanValidator {
  private PlanValidator() {}
  public static List<Node> validate(Plan plan, Registry registry, Limits limits,
                                   Set<Capability> allowed) {
    if (plan.nodes().isEmpty() || plan.nodes().size() > limits.maxNodes()) {
      throw new IllegalArgumentException("node budget exceeded or empty plan");
    }
    Map<String, Node> nodes = new TreeMap<>();
    long edges = 0;
    for (Node n : plan.nodes()) {
      if (nodes.putIfAbsent(n.id(), n) != null) throw new IllegalArgumentException("duplicate node");
      Binding b = registry.require(n.bindingId());
      if (!b.sha256().equals(n.bindingSha256())) throw new IllegalArgumentException("stale binding");
      if (!allowed.containsAll(b.capabilities())) throw new IllegalArgumentException("capability denied");
      edges += n.dependencies().size();
    }
    if (edges > limits.maxEdges()) throw new IllegalArgumentException("edge budget exceeded");
    if (!nodes.containsKey(plan.outputNode())) throw new IllegalArgumentException("unknown output");
    Map<String, Integer> degree = new HashMap<>();
    Map<String, List<String>> children = new HashMap<>();
    PriorityQueue<String> ready = new PriorityQueue<>();
    for (Node n : nodes.values()) {
      degree.put(n.id(), n.dependencies().size());
      if (n.dependencies().isEmpty()) ready.add(n.id());
      for (String parent : n.dependencies()) {
        if (!nodes.containsKey(parent)) throw new IllegalArgumentException("missing dependency");
        children.computeIfAbsent(parent, key -> new ArrayList<>()).add(n.id());
      }
    }
    List<Node> ordered = new ArrayList<>();
    while (!ready.isEmpty()) {
      String id = ready.remove();
      ordered.add(nodes.get(id));
      for (String child : children.getOrDefault(id, List.of())) {
        int remaining = degree.merge(child, -1, Integer::sum);
        if (remaining == 0) ready.add(child);
      }
    }
    if (ordered.size() != nodes.size()) throw new IllegalArgumentException("cycle");
    Set<String> ancestors = new HashSet<>();
    ArrayDeque<String> pending = new ArrayDeque<>();
    pending.add(plan.outputNode());
    while (!pending.isEmpty()) {
      String id = pending.remove();
      if (ancestors.add(id)) pending.addAll(nodes.get(id).dependencies());
    }
    if (ancestors.size() != nodes.size()) throw new IllegalArgumentException("unreachable work");
    return List.copyOf(ordered);
  }
}
