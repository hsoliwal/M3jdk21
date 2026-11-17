// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Set;

/** Deterministic output of one budget/workload recommendation. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeRecommendation.java blob 1a37caad0946; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public record IndexPrecomputeRecommendation(
    Set<IndexPrecomputeCapability> capabilities,
    long retainedBytes,
    long score,
    boolean exactSearch) {

  public IndexPrecomputeRecommendation {
    capabilities = Set.copyOf(capabilities);
    if (retainedBytes < 0) throw new IllegalArgumentException("negative retainedBytes");
  }

  public IndexPrecomputePlan toPlan(int nativeBatchThreshold, boolean mmapPreferred) {
    return new IndexPrecomputePlan(
        capabilities, retainedBytes, nativeBatchThreshold, mmapPreferred);
  }

  /**
   * Convert the recommendation to a plan while preserving the caller's real byte budget.
   *
   * <p>The recommendation's retainedBytes is an estimate/selection result. Actual provider-backed
   * facets may share storage or report slightly different retained bytes, so the snapshot builder
   * should enforce the original budget rather than mistaking the estimate for the hard limit.
   */
  public IndexPrecomputePlan toPlan(
      long maxRetainedBytes,
      int nativeBatchThreshold,
      boolean mmapPreferred) {
    if (maxRetainedBytes < retainedBytes) {
      throw new IllegalArgumentException(
          "plan budget is smaller than the recommended retained bytes");
    }
    return new IndexPrecomputePlan(
        capabilities, maxRetainedBytes, nativeBatchThreshold, mmapPreferred);
  }
}
