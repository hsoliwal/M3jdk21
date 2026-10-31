/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.HashSet;
import java.util.List;

/** Empty dependencies consume the request input; otherwise concatenate dependencies in listed order. */
public record Node(String id, String bindingId, String bindingSha256, List<String> dependencies) {
  public Node {
    id = Ids.id(id);
    bindingId = Ids.id(bindingId);
    bindingSha256 = Ids.sha256(bindingSha256);
    dependencies = List.copyOf(dependencies);
    dependencies.forEach(Ids::id);
    if (new HashSet<>(dependencies).size() != dependencies.size()) {
      throw new IllegalArgumentException("duplicate dependency");
    }
  }
  public static Node of(String id, Binding binding, String... dependencies) {
    return new Node(id, binding.id(), binding.sha256(), List.of(dependencies));
  }
}
