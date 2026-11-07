// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/**
 * Optional coarse-batch accelerator.
 *
 * <p>Identity, dictionary semantics and exact correctness remain owned by Java/frozen data.
 * Accelerators only execute primitive batch kernels and must match the Java oracle exactly.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeAccelerator.java blob ffdda4685e38; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public interface M3IndexPrecomputeAccelerator {
  String id();
  int priority();
  int minimumRows();
  boolean available();
  boolean supports(M3IndexPrecomputeKernel kernel);

  default void lookupPairs(
      M3FrozenLongTupleIndex index,
      long[] left,
      long[] right,
      int defaultValue,
      int[] output) {
    throw new UnsupportedOperationException(id() + " does not support tuple pair lookup");
  }

  default void hash64Ranges(
      int[] tokens,
      int[] offsets,
      long[] output) {
    throw new UnsupportedOperationException(id() + " does not support hash64 ranges");
  }

  default void signature256Ranges(
      int[] tokens,
      int[] offsets,
      long[] output) {
    throw new UnsupportedOperationException(id() + " does not support signature256 ranges");
  }

  default void translateDense(
      int[] source,
      int[] translationTable,
      int[] output) {
    throw new UnsupportedOperationException(id() + " does not support dense translation");
  }
}
