// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Deterministic owner-scoped composition for one capability with multiple semantic producers. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeFacetGroup.java blob 269418ced51b; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputeFacetGroup implements M3IndexPrecomputeFacet {
  private final M3IndexPrecomputeCapability capability;
  private final Map<String, M3IndexPrecomputeFacet> owners;
  private final Map<String, Long> storageClaims;
  private final String rootHash;
  private final long retainedBytes;
  private final boolean mmapBacked;

  M3IndexPrecomputeFacetGroup(
      M3IndexPrecomputeCapability capability,
      Map<String, ? extends M3IndexPrecomputeFacet> source) {
    this.capability = Objects.requireNonNull(capability, "capability");
    TreeMap<String, M3IndexPrecomputeFacet> ordered = new TreeMap<>();
    Objects.requireNonNull(source, "source").forEach((ownerId, facet) -> {
      String owner = requireOwnerId(ownerId);
      M3IndexPrecomputeFacet value = Objects.requireNonNull(facet, "facet");
      if (value.capability() != capability) {
        throw new IllegalArgumentException("facet capability mismatch: " + capability);
      }
      if (ordered.putIfAbsent(owner, value) != null) {
        throw new IllegalArgumentException("duplicate facet owner " + owner);
      }
    });
    if (ordered.size() < 2) {
      throw new IllegalArgumentException("facet group requires at least two owners");
    }
    owners = Map.copyOf(ordered);

    LinkedHashMap<String, Long> claims = new LinkedHashMap<>();
    long bytes = 0L;
    boolean mapped = true;
    java.util.ArrayList<String> rootFields = new java.util.ArrayList<>(owners.size() * 2);
    for (Map.Entry<String, M3IndexPrecomputeFacet> entry : ordered.entrySet()) {
      rootFields.add(entry.getKey());
      rootFields.add(entry.getValue().rootHash());
      mapped &= entry.getValue().mmapBacked();
      for (Map.Entry<String, Long> claim : entry.getValue().storageClaims().entrySet()) {
        String key = requireStorageKey(claim.getKey());
        long value = requireBytes(claim.getValue());
        Long previous = claims.putIfAbsent(key, value);
        if (previous == null) bytes = Math.addExact(bytes, value);
        else if (previous.longValue() != value) {
          throw new IllegalArgumentException("shared storage has inconsistent byte accounting: " + key);
        }
      }
    }
    storageClaims = Map.copyOf(claims);
    retainedBytes = bytes;
    mmapBacked = mapped;
    rootHash = M3PrecomputeHashes.sha256(
        "INDEX_PRECOMPUTE_FACET_GROUP_V1:" + capability.name(),
        rootFields.toArray(String[]::new));
  }

  @Override public M3IndexPrecomputeCapability capability() { return capability; }
  @Override public String rootHash() { return rootHash; }
  @Override public long retainedBytes() { return retainedBytes; }
  @Override public String storageKey() { return "facet-group:" + capability.name() + ":" + rootHash; }
  @Override public Map<String, Long> storageClaims() { return storageClaims; }
  @Override public boolean mmapBacked() { return mmapBacked; }

  public Map<String, M3IndexPrecomputeFacet> owners() { return owners; }

  public Optional<M3IndexPrecomputeFacet> facet(String ownerId) {
    return Optional.ofNullable(owners.get(requireOwnerId(ownerId)));
  }

  public <T extends M3IndexPrecomputeFacet> T require(String ownerId, Class<T> type) {
    M3IndexPrecomputeFacet facet = owners.get(requireOwnerId(ownerId));
    if (facet == null) throw new IllegalStateException("facet owner not compiled: " + ownerId);
    return Objects.requireNonNull(type, "type").cast(facet);
  }

  static String requireOwnerId(String raw) {
    String value = Objects.requireNonNull(raw, "ownerId").trim();
    if (value.isEmpty()) throw new IllegalArgumentException("blank facet owner id");
    return value;
  }

  static String requireStorageKey(String raw) {
    String value = Objects.requireNonNull(raw, "storageKey").trim();
    if (value.isEmpty()) throw new IllegalArgumentException("blank storageKey");
    return value;
  }

  static long requireBytes(Long raw) {
    long value = Objects.requireNonNull(raw, "retainedBytes");
    if (value < 0L) throw new IllegalArgumentException("negative retained bytes");
    return value;
  }
}
