// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** Read-only fixed-arity tuple index contract shared by heap and mmap storage. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/LongTupleIndexView.java blob cc075586c123; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public interface LongTupleIndexView extends IndexPrecomputeFacet {
  int arity();
  int size();

  int get(long k1, int defaultValue);
  int get(long k1, long k2, int defaultValue);
  int get(long k1, long k2, long k3, int defaultValue);
  int get(long k1, long k2, long k3, long k4, int defaultValue);
}
