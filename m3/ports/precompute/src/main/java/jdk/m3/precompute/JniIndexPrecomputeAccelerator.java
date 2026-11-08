// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** JNI adapter over the existing coarse pair-lookup native kernel. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/JniIndexPrecomputeAccelerator.java blob df4bb4269b40; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class JniIndexPrecomputeAccelerator implements IndexPrecomputeAccelerator {
  public static final JniIndexPrecomputeAccelerator INSTANCE =
      new JniIndexPrecomputeAccelerator();

  private JniIndexPrecomputeAccelerator() {}

  @Override public String id() { return "jni"; }
  @Override public int priority() { return 100; }
  @Override public int minimumRows() {
    return Integer.getInteger("m3.precompute.jni.minimumRows", 4096);
  }
  @Override public boolean available() { return NativePrecomputeOps.available(); }
  @Override public boolean supports(IndexPrecomputeKernel kernel) {
    return kernel == IndexPrecomputeKernel.TUPLE_PAIR_LOOKUP;
  }

  @Override
  public void lookupPairs(
      FrozenLongTupleIndex index,
      long[] left,
      long[] right,
      int defaultValue,
      int[] output) {
    int[] result = NativePrecomputeOps.lookupPairs(index, left, right, defaultValue, 0);
    if (result.length != output.length) throw new IllegalArgumentException("output shape");
    System.arraycopy(result, 0, output, 0, result.length);
  }
}
