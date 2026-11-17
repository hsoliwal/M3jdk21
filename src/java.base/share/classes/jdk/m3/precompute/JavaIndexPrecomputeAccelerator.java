// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;

/** Always-available Java semantic oracle for every universal precompute kernel. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/JavaIndexPrecomputeAccelerator.java blob 925450db84bf; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class JavaIndexPrecomputeAccelerator implements IndexPrecomputeAccelerator {
  public static final JavaIndexPrecomputeAccelerator INSTANCE =
      new JavaIndexPrecomputeAccelerator();

  private JavaIndexPrecomputeAccelerator() {}

  @Override public String id() { return "java"; }
  @Override public int priority() { return Integer.MIN_VALUE; }
  @Override public int minimumRows() { return 0; }
  @Override public boolean available() { return true; }
  @Override public boolean supports(IndexPrecomputeKernel kernel) { return true; }

  @Override
  public void lookupPairs(
      FrozenLongTupleIndex index,
      long[] left,
      long[] right,
      int defaultValue,
      int[] output) {
    Objects.requireNonNull(index, "index");
    requireSame(left, right, output);
    for (int i = 0; i < output.length; i++) {
      output[i] = index.getPair(left[i], right[i], defaultValue);
    }
  }

  @Override
  public void hash64Ranges(int[] tokens, int[] offsets, long[] output) {
    validateRanges(tokens, offsets, output.length);
    for (int row = 0; row < output.length; row++) {
      long hash = 0xcbf29ce484222325L;
      int from = offsets[row];
      int to = offsets[row + 1];
      for (int i = from; i < to; i++) {
        int value = tokens[i];
        hash ^= value & 0xffL;
        hash *= 0x100000001b3L;
        hash ^= (value >>> 8) & 0xffL;
        hash *= 0x100000001b3L;
        hash ^= (value >>> 16) & 0xffL;
        hash *= 0x100000001b3L;
        hash ^= (value >>> 24) & 0xffL;
        hash *= 0x100000001b3L;
      }
      hash ^= to - from;
      hash *= 0x9e3779b97f4a7c15L;
      hash ^= hash >>> 32;
      output[row] = hash;
    }
  }

  @Override
  public void signature256Ranges(int[] tokens, int[] offsets, long[] output) {
    int rows = offsets.length - 1;
    if (output.length != rows * 4) throw new IllegalArgumentException("signature output shape");
    validateRanges(tokens, offsets, rows);

    for (int row = 0; row < rows; row++) {
      long w0 = 0L, w1 = 0L, w2 = 0L, w3 = 0L;
      for (int i = offsets[row]; i < offsets[row + 1]; i++) {
        long mixed = mix(tokens[i]);
        int bitA = (int) mixed & 255;
        int bitB = (int) Long.rotateLeft(mixed, 21) & 255;

        long maskA = 1L << (bitA & 63);
        if (bitA < 64) w0 |= maskA;
        else if (bitA < 128) w1 |= maskA;
        else if (bitA < 192) w2 |= maskA;
        else w3 |= maskA;

        long maskB = 1L << (bitB & 63);
        if (bitB < 64) w0 |= maskB;
        else if (bitB < 128) w1 |= maskB;
        else if (bitB < 192) w2 |= maskB;
        else w3 |= maskB;
      }
      int at = row * 4;
      output[at] = w0;
      output[at + 1] = w1;
      output[at + 2] = w2;
      output[at + 3] = w3;
    }
  }

  @Override
  public void translateDense(int[] source, int[] translationTable, int[] output) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(translationTable, "translationTable");
    Objects.requireNonNull(output, "output");
    if (source.length != output.length) throw new IllegalArgumentException("translation shape");
    for (int i = 0; i < source.length; i++) {
      int token = source[i];
      int mapped = token >= 0 && token < translationTable.length
          ? translationTable[token] : -1;
      output[i] = mapped >= 0 ? mapped : token;
    }
  }

  private static void requireSame(long[] left, long[] right, int[] output) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    Objects.requireNonNull(output, "output");
    if (left.length != right.length || left.length != output.length) {
      throw new IllegalArgumentException("batch shape");
    }
  }

  private static void validateRanges(int[] tokens, int[] offsets, int rows) {
    Objects.requireNonNull(tokens, "tokens");
    Objects.requireNonNull(offsets, "offsets");
    if (offsets.length != rows + 1 || offsets[0] != 0) {
      throw new IllegalArgumentException("offset shape");
    }
    int previous = 0;
    for (int value : offsets) {
      if (value < previous || value > tokens.length) {
        throw new IllegalArgumentException("offset ordering");
      }
      previous = value;
    }
    if (previous != tokens.length) throw new IllegalArgumentException("final offset");
  }

  private static long mix(int value) {
    long z = Integer.toUnsignedLong(value) + 0x9e3779b97f4a7c15L;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
