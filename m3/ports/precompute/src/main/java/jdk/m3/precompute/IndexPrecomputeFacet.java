// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Map;

/** One immutable capability owner inside a precompute snapshot. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeFacet.java blob f0cd1c6f4dab; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public interface IndexPrecomputeFacet {
  IndexPrecomputeCapability capability();
  String rootHash();
  long retainedBytes();

  /**
   * Physical-storage identity. Multiple capability facades over the same frozen arrays return the
   * same key so snapshot budgets count those bytes once.
   */
  default String storageKey() { return getClass().getName() + ":" + rootHash(); }

  /**
   * Physical storage claims owned/retained by this facet.
   *
   * <p>The default preserves the historical one-key contract. Composite owner-scoped facets
   * override this with the union of their members so budgets charge every physical owner once
   * without forcing unrelated semantic owners into one storage identity.</p>
   */
  default Map<String, Long> storageClaims() {
    return Map.of(storageKey(), retainedBytes());
  }

  default boolean mmapBacked() { return false; }
}
