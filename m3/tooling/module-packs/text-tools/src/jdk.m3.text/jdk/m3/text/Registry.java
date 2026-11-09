/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable registry: duplicate ownership is an error, never last-writer-wins. */
public final class Registry {
  private final Map<String, Binding> bindings;
  public Registry(Collection<Binding> input) {
    TreeMap<String, Binding> map = new TreeMap<>();
    for (Binding b : input) {
      if (map.putIfAbsent(b.id(), b) != null) throw new IllegalArgumentException("duplicate binding");
    }
    bindings = Collections.unmodifiableMap(map);
  }
  public Binding require(String id) {
    Binding binding = bindings.get(id);
    if (binding == null) throw new IllegalArgumentException("unknown binding: " + id);
    return binding;
  }
  public Collection<Binding> bindings() { return bindings.values(); }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-registry-v1").number(bindings.size());
    bindings.values().forEach(b -> hash.text(b.sha256()));
    return hash.finish();
  }
}
