// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable budget and capability request for one precompute generation. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputePlan.java blob 6e2181fbce4f; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public record IndexPrecomputePlan(
    Set<IndexPrecomputeCapability> capabilities,
    long maxRetainedBytes,
    int nativeBatchThreshold,
    boolean mmapPreferred) {

  public IndexPrecomputePlan {
    Objects.requireNonNull(capabilities, "capabilities");
    capabilities = Set.copyOf(capabilities);
    if (maxRetainedBytes < 0) throw new IllegalArgumentException("negative byte budget");
    if (nativeBatchThreshold < 0) throw new IllegalArgumentException("negative native threshold");
  }

  public static IndexPrecomputePlan everything(long maxRetainedBytes) {
    return new IndexPrecomputePlan(
        EnumSet.allOf(IndexPrecomputeCapability.class),
        maxRetainedBytes,
        4096,
        true);
  }

  public static Builder builder() { return new Builder(); }

  public boolean wants(IndexPrecomputeCapability capability) {
    return capabilities.contains(Objects.requireNonNull(capability, "capability"));
  }

  public static final class Builder {
    private final EnumSet<IndexPrecomputeCapability> capabilities =
        EnumSet.noneOf(IndexPrecomputeCapability.class);
    private long maxRetainedBytes = Long.MAX_VALUE;
    private int nativeBatchThreshold = 4096;
    private boolean mmapPreferred = true;

    public Builder enable(IndexPrecomputeCapability capability) {
      capabilities.add(Objects.requireNonNull(capability, "capability"));
      return this;
    }

    public Builder enableAll() {
      capabilities.addAll(EnumSet.allOf(IndexPrecomputeCapability.class));
      return this;
    }

    public Builder maxRetainedBytes(long value) {
      maxRetainedBytes = value;
      return this;
    }

    public Builder nativeBatchThreshold(int value) {
      nativeBatchThreshold = value;
      return this;
    }

    public Builder mmapPreferred(boolean value) {
      mmapPreferred = value;
      return this;
    }

    public IndexPrecomputePlan build() {
      return new IndexPrecomputePlan(
          capabilities, maxRetainedBytes, nativeBatchThreshold, mmapPreferred);
    }
  }
}
