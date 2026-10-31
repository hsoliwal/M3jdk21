/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CPU-first DAG execution. No process launch, reflection invocation, or model calls. */
public final class M3UnixRuntime {
  private final Registry registry;
  private final Limits limits;
  private final Set<Capability> allowed;
  public M3UnixRuntime(Registry registry, Limits limits, Set<Capability> allowed) {
    this.registry = java.util.Objects.requireNonNull(registry);
    this.limits = java.util.Objects.requireNonNull(limits);
    this.allowed = Set.copyOf(allowed);
  }
  public String policySha256() {
    Canonical hash = new Canonical("m3-unix4j-policy-v1").text(limits.sha256()).number(allowed.size());
    allowed.stream().sorted().forEach(c -> hash.text(c.name()));
    return hash.finish();
  }
  public ExecutionResult execute(Plan plan, TextFrame input) {
    limits.check(input);
    List<Node> order = PlanValidator.validate(plan, registry, limits, allowed);
    Map<String, TextFrame> outputs = new HashMap<>();
    Map<String, Integer> consumers = new HashMap<>();
    order.forEach(n -> n.dependencies().forEach(id -> consumers.merge(id, 1, Integer::sum)));
    List<StepReceipt> receipts = new ArrayList<>();
    for (Node n : order) {
      TextFrame frame;
      if (n.dependencies().isEmpty()) frame = input;
      else {
        FrameCollector joined = new FrameCollector(limits);
        for (String id : n.dependencies()) outputs.get(id).lines().forEach(joined);
        frame = joined.frame();
      }
      Binding binding = registry.require(n.bindingId());
      FrameCollector collected = new FrameCollector(limits);
      binding.atom().execute(frame, collected);
      TextFrame result = collected.frame();
      receipts.add(new StepReceipt(n.id(), binding.sha256(), frame.sha256(), result.sha256()));
      outputs.put(n.id(), result);
      for (String id : n.dependencies()) {
        if (consumers.merge(id, -1, Integer::sum) == 0) outputs.remove(id);
      }
    }
    TextFrame output = outputs.get(plan.outputNode());
    Receipt receipt = new Receipt(plan.sha256(), registry.sha256(), policySha256(),
        input.sha256(), output.sha256(), receipts);
    return new ExecutionResult(output, receipt);
  }
  /** Re-executes every binding; compares exact output frames and all receipt fields. */
  public ExecutionResult replay(Plan plan, TextFrame input, ExecutionResult expected) {
    if (!expected.receipt().planSha256().equals(plan.sha256())
        || !expected.receipt().registrySha256().equals(registry.sha256())
        || !expected.receipt().policySha256().equals(policySha256())
        || !expected.receipt().inputSha256().equals(input.sha256())) {
      throw new IllegalArgumentException("replay preimage mismatch");
    }
    ExecutionResult actual = execute(plan, input);
    if (!actual.equals(expected)) throw new IllegalStateException("replay mismatch");
    return actual;
  }
}
