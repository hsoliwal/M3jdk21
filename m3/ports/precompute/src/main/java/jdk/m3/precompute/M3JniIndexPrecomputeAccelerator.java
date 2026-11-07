// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** JNI adapter over the existing coarse pair-lookup native kernel. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/JniIndexPrecomputeAccelerator.java blob df4bb4269b40; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3JniIndexPrecomputeAccelerator implements M3IndexPrecomputeAccelerator {
  public static final M3JniIndexPrecomputeAccelerator INSTANCE =
      new M3JniIndexPrecomputeAccelerator();

  private M3JniIndexPrecomputeAccelerator() {}

  @Override public String id() { return "jni"; }
  @Override public int priority() { return 100; }
  @Override public int minimumRows() {
    return Integer.getInteger("m3.precompute.jni.minimumRows", 4096);
  }
  @Override public boolean available() { return M3NativePrecomputeOps.available(); }
  @Override public boolean supports(M3IndexPrecomputeKernel kernel) {
    return kernel == M3IndexPrecomputeKernel.TUPLE_PAIR_LOOKUP;
  }

  @Override
  public void lookupPairs(
      M3FrozenLongTupleIndex index,
      long[] left,
      long[] right,
      int defaultValue,
      int[] output) {
    int[] result = M3NativePrecomputeOps.lookupPairs(index, left, right, defaultValue, 0);
    if (result.length != output.length) throw new IllegalArgumentException("output shape");
    System.arraycopy(result, 0, output, 0, result.length);
  }
}
