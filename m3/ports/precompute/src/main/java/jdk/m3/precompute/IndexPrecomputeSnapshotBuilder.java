// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Budget-enforcing builder for one immutable precompute generation. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeSnapshotBuilder.java blob 503ed8d95e15; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class IndexPrecomputeSnapshotBuilder {
  private final String generationId;
  private final IndexPrecomputePlan plan;
  private final EnumMap<IndexPrecomputeCapability, TreeMap<String, IndexPrecomputeFacet>> facets =
      new EnumMap<>(IndexPrecomputeCapability.class);
  private final Map<String, Long> storageBytes = new HashMap<>();
  private long retainedBytes;

  public IndexPrecomputeSnapshotBuilder(String generationId, IndexPrecomputePlan plan) {
    this.generationId = Objects.requireNonNull(generationId, "generationId");
    this.plan = Objects.requireNonNull(plan, "plan");
  }

  /** Historical single-owner API; owner identity defaults to the concrete facet class. */
  public IndexPrecomputeSnapshotBuilder add(IndexPrecomputeFacet facet) {
    Objects.requireNonNull(facet, "facet");
    return add(facet.getClass().getName(), facet);
  }

  /** Add one stable semantic owner for a capability. */
  public IndexPrecomputeSnapshotBuilder add(String ownerId, IndexPrecomputeFacet facet) {
    Objects.requireNonNull(facet, "facet");
    if (!plan.wants(facet.capability())) return this;
    String owner = IndexPrecomputeFacetGroup.requireOwnerId(ownerId);
    TreeMap<String, IndexPrecomputeFacet> capabilityOwners =
        facets.computeIfAbsent(facet.capability(), ignored -> new TreeMap<>());
    if (capabilityOwners.containsKey(owner)) {
      throw new IllegalArgumentException(
          "duplicate capability owner " + facet.capability() + ":" + owner);
    }

    long added = 0L;
    Map<String, Long> newClaims = new HashMap<>();
    for (Map.Entry<String, Long> claim : facet.storageClaims().entrySet()) {
      String key = IndexPrecomputeFacetGroup.requireStorageKey(claim.getKey());
      long bytes = IndexPrecomputeFacetGroup.requireBytes(claim.getValue());
      Long withinFacet = newClaims.putIfAbsent(key, bytes);
      if (withinFacet != null && withinFacet.longValue() != bytes) {
        throw new IllegalArgumentException("facet reports inconsistent storage claim " + key);
      }
      Long previous = storageBytes.get(key);
      if (previous == null) added = Math.addExact(added, bytes);
      else if (previous.longValue() != bytes) {
        throw new IllegalArgumentException(
            "same storageKey reported different retained bytes: " + key);
      }
    }

    long next = Math.addExact(retainedBytes, added);
    if (next > plan.maxRetainedBytes()) {
      throw new IllegalStateException(
          "precompute byte budget exceeded by " + facet.capability()
              + ": " + next + " > " + plan.maxRetainedBytes());
    }
    capabilityOwners.put(owner, facet);
    newClaims.forEach(storageBytes::putIfAbsent);
    retainedBytes = next;
    return this;
  }

  public boolean wants(IndexPrecomputeCapability capability) {
    return plan.wants(Objects.requireNonNull(capability, "capability"));
  }

  public long retainedBytes() { return retainedBytes; }

  /** Capabilities already published into this in-progress generation. */
  public Set<IndexPrecomputeCapability> capabilities() {
    return Set.copyOf(facets.keySet());
  }

  /** Stable semantic owner IDs currently registered for one capability. */
  public Set<String> ownerIds(IndexPrecomputeCapability capability) {
    TreeMap<String, IndexPrecomputeFacet> values =
        facets.get(Objects.requireNonNull(capability, "capability"));
    return values == null ? Set.of() : Set.copyOf(values.keySet());
  }

  /** Immutable owner map used by source/capability manifest binding. */
  public Map<IndexPrecomputeCapability, Set<String>> ownerIds() {
    EnumMap<IndexPrecomputeCapability, Set<String>> result =
        new EnumMap<>(IndexPrecomputeCapability.class);
    facets.forEach((capability, values) -> result.put(capability, Set.copyOf(values.keySet())));
    return Map.copyOf(result);
  }

  public IndexPrecomputeSnapshot build() {
    EnumMap<IndexPrecomputeCapability, IndexPrecomputeFacet> materialized =
        new EnumMap<>(IndexPrecomputeCapability.class);
    EnumMap<IndexPrecomputeCapability, Map<String, IndexPrecomputeFacet>> ownership =
        new EnumMap<>(IndexPrecomputeCapability.class);
    facets.forEach((capability, values) -> {
      Map<String, IndexPrecomputeFacet> frozen = Map.copyOf(values);
      ownership.put(capability, frozen);
      materialized.put(
          capability,
          values.size() == 1
              ? values.firstEntry().getValue()
              : new IndexPrecomputeFacetGroup(capability, values));
    });
    return new IndexPrecomputeSnapshot(generationId, materialized, ownership);
  }
}
