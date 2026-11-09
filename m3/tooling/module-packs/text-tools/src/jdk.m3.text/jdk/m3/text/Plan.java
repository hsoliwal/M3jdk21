/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.List;

/** Candidate-only DAG, with no executable text, classes, or callbacks. */
public record Plan(List<Node> nodes, String outputNode) {
  public Plan { nodes = List.copyOf(nodes); outputNode = Ids.id(outputNode); }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-plan-v1").text(outputNode).number(nodes.size());
    nodes.stream().sorted(java.util.Comparator.comparing(Node::id)).forEach(n -> {
      hash.text(n.id()).text(n.bindingId()).text(n.bindingSha256()).number(n.dependencies().size());
      n.dependencies().forEach(hash::text);
    });
    return hash.finish();
  }
}
