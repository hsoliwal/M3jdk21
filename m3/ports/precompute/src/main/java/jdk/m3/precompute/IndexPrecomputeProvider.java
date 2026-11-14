// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/**
 * Extension point for domain modules that already own immutable precompute state.
 *
 * <p>Providers register facades over their existing owners; they do not copy state into the
 * universal module.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeProvider.java blob 336833f7696b; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public interface IndexPrecomputeProvider {
  String id();
  boolean supports(Object source);
  void register(Object source, IndexPrecomputeSnapshotBuilder target);
}
