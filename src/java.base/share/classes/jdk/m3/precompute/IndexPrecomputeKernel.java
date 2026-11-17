// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** Coarse primitive kernels eligible for optional acceleration. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeKernel.java blob 9958cd64018f; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public enum IndexPrecomputeKernel {
  TUPLE_PAIR_LOOKUP,
  HASH64_RANGES,
  SIGNATURE256_RANGES,
  TRANSLATE_DENSE
}
