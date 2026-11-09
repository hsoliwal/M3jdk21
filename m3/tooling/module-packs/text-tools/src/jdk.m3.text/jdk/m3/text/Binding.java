/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.Objects;
import java.util.Set;

/** Exact approved definition and implementation identity bound to one host-owned atom. */
public record Binding(String id, String definition, String implementationSha256,
                      Set<Capability> capabilities, Atom atom) {
  public Binding {
    id = Ids.id(id);
    Canonical.validUnicode(definition);
    if (definition.isBlank() || definition.length() > 65536) {
      throw new IllegalArgumentException("invalid reviewed definition");
    }
    implementationSha256 = Ids.sha256(implementationSha256);
    capabilities = Set.copyOf(capabilities);
    Objects.requireNonNull(atom, "atom");
  }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-binding-v1").text(id).text(definition)
        .text(implementationSha256).number(capabilities.size());
    capabilities.stream().sorted().forEach(c -> hash.text(c.name()));
    return hash.finish();
  }
}
