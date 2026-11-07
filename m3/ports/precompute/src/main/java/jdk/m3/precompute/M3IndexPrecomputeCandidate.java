// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;
import java.util.Set;

/** One planner candidate with explicit retained/build/reuse costs and dependencies. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeCandidate.java blob 5ddc7ee435a9; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public record M3IndexPrecomputeCandidate(
    M3IndexPrecomputeCapability capability,
    long retainedBytes,
    long buildCostUnits,
    long expectedReuseCount,
    Set<M3IndexPrecomputeCapability> dependencies,
    boolean mandatory) {

  public M3IndexPrecomputeCandidate {
    Objects.requireNonNull(capability, "capability");
    dependencies = Set.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    if (dependencies.contains(capability)) {
      throw new IllegalArgumentException("capability cannot depend on itself");
    }
    if (retainedBytes < 0 || buildCostUnits < 0 || expectedReuseCount < 0) {
      throw new IllegalArgumentException("candidate costs must be non-negative");
    }
  }

  public static M3IndexPrecomputeCandidate of(
      M3IndexPrecomputeCapability capability,
      long retainedBytes,
      long buildCostUnits,
      long expectedReuseCount) {
    return new M3IndexPrecomputeCandidate(
        capability, retainedBytes, buildCostUnits, expectedReuseCount, Set.of(), false);
  }
}
