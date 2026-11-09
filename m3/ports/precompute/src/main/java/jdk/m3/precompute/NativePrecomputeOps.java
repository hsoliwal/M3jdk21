// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;

/**
 * Optional JNI acceleration for coarse primitive batches.
 *
 * <p>Java remains the semantic oracle. JNI is never crossed per token/pair.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-tuple-fallback-scalar-java21-20261102/NativePrecomputeOps.java.txt blob 0fbc79832e98; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class NativePrecomputeOps {
  private static final boolean AVAILABLE;
  private static final boolean TUPLES_AVAILABLE;

  static {
    boolean loaded = false;
    if (!Boolean.getBoolean("m3.precompute.disableNative")) {
      try {
        System.loadLibrary("m3_precompute");
        loaded = nativeAbiVersion() == 1;
      } catch (UnsatisfiedLinkError ignored) {
        loaded = false;
      }
    }
    AVAILABLE = loaded;
    boolean tuples = false;
    if (loaded) {
      try { tuples = tupleLookupAbiVersionNative() == 1; }
      catch (UnsatisfiedLinkError oldLibrary) { tuples = false; }
    }
    TUPLES_AVAILABLE = tuples;
  }

  private NativePrecomputeOps() {}

  public static boolean available() { return AVAILABLE; }

  /**
   * Batch lookup over one arity-2 frozen tuple index.
   *
   * @return one int result per query pair.
   */
  public static int[] lookupPairs(
      FrozenLongTupleIndex index,
      long[] left,
      long[] right,
      int defaultValue,
      int nativeThreshold) {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    if (index.arity() != 2) throw new IllegalArgumentException("index must have arity 2");
    if (left.length != right.length) throw new IllegalArgumentException("query cardinality mismatch");
    if (nativeThreshold < 0) throw new IllegalArgumentException("negative native threshold");

    int[] output = new int[left.length];
    if (AVAILABLE && left.length >= nativeThreshold) {
      lookupPairsNative(
          left,
          right,
          index.rawKeys(),
          index.rawValues(),
          index.rawSlots(),
          defaultValue,
          output);
      return output;
    }

    for (int i = 0; i < left.length; i++) {
      output[i] = index.getPair(left[i], right[i], defaultValue);
    }
    return output;
  }

  /** Separate feature negotiation preserves compatibility with the original pair-only ABI. */
  public static boolean tuplesAvailable() { return TUPLES_AVAILABLE; }

  /**
   * Row-major batch lookup for arities 1..4, using exactly the existing frozen-index layout.
   * No JNI call is made per token. An absent or older library uses the Java oracle.
   */
  public static int[] lookupTuples(
      FrozenLongTupleIndex index, long[] queries, int defaultValue, int nativeThreshold) {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(queries, "queries");
    int arity = index.arity();
    if (queries.length % arity != 0) throw new IllegalArgumentException("query cardinality mismatch");
    if (nativeThreshold < 0) throw new IllegalArgumentException("negative native threshold");
    int[] output = new int[queries.length / arity];
    if (TUPLES_AVAILABLE && output.length >= nativeThreshold) {
      lookupTuplesNative(queries, arity, index.rawKeys(), index.rawValues(), index.rawSlots(), defaultValue, output);
    } else {
      // One arity decision per batch; the incumbent scalar getters preserve the
      // existing frozen-index hash, exact tuple comparison and allocation policy.
      switch (arity) {
        case 1 -> {
          for (int row = 0; row < output.length; row++) {
            output[row] = index.get(queries[row], defaultValue);
          }
        }
        case 2 -> {
          for (int row = 0, offset = 0; row < output.length; row++, offset += 2) {
            output[row] = index.getPair(queries[offset], queries[offset + 1], defaultValue);
          }
        }
        case 3 -> {
          for (int row = 0, offset = 0; row < output.length; row++, offset += 3) {
            output[row] = index.get(
                queries[offset], queries[offset + 1], queries[offset + 2], defaultValue);
          }
        }
        case 4 -> {
          for (int row = 0, offset = 0; row < output.length; row++, offset += 4) {
            output[row] = index.get(queries[offset], queries[offset + 1],
                queries[offset + 2], queries[offset + 3], defaultValue);
          }
        }
        default -> throw new IllegalStateException("unexpected frozen tuple arity " + arity);
      }
    }
    return output;
  }

  private static native int tupleLookupAbiVersionNative();
  private static native void lookupTuplesNative(
      long[] queries, int arity, long[] keys, int[] values, int[] slots, int defaultValue, int[] output);

  private static native int nativeAbiVersion();

  private static native void lookupPairsNative(
      long[] queryLeft,
      long[] queryRight,
      long[] keys,
      int[] values,
      int[] slots,
      int defaultValue,
      int[] output);
}
