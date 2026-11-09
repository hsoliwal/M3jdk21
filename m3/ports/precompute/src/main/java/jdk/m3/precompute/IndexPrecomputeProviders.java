// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Deterministic provider discovery and explicit provider composition. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeProviders.java blob 457b012f712d; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class IndexPrecomputeProviders {
  private final List<IndexPrecomputeProvider> providers;

  private IndexPrecomputeProviders(List<IndexPrecomputeProvider> providers) {
    this.providers = providers.stream()
        .sorted(Comparator.comparing(IndexPrecomputeProvider::id))
        .toList();
    for (int i = 1; i < this.providers.size(); i++) {
      if (this.providers.get(i - 1).id().equals(this.providers.get(i).id())) {
        throw new IllegalArgumentException("duplicate precompute provider id " + this.providers.get(i).id());
      }
    }
  }

  public static IndexPrecomputeProviders of(IndexPrecomputeProvider... providers) {
    Objects.requireNonNull(providers, "providers");
    return new IndexPrecomputeProviders(List.of(providers.clone()));
  }

  public static IndexPrecomputeProviders load(ClassLoader loader) {
    Objects.requireNonNull(loader, "loader");
    List<IndexPrecomputeProvider> providers = new ArrayList<>();
    ServiceLoader.load(IndexPrecomputeProvider.class, loader).forEach(providers::add);
    return new IndexPrecomputeProviders(providers);
  }

  public List<IndexPrecomputeProvider> all() { return providers; }

  /** Register every provider that recognizes the source, in deterministic provider-id order. */
  public int register(Object source, IndexPrecomputeSnapshotBuilder target) {
    return registerMatched(source, target).size();
  }

  /**
   * Register every matching provider and return their deterministic IDs.
   *
   * <p>This is the proof-bearing sibling of {@link #register(Object,
   * M3IndexPrecomputeSnapshotBuilder)} used by the multipass manifest.</p>
   */
  public List<String> registerMatched(
      Object source, IndexPrecomputeSnapshotBuilder target) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
    ArrayList<String> matched = new ArrayList<>();
    for (IndexPrecomputeProvider provider : providers) {
      if (provider.supports(source)) {
        provider.register(source, target);
        matched.add(provider.id());
      }
    }
    return List.copyOf(matched);
  }
}
