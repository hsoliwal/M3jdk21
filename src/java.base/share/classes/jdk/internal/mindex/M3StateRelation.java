// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Adapted from com.synexia.indexstring.MIndexStateRelation; numeric atoms only.
package jdk.internal.mindex;

/** Internal numeric receiver. No text, interner, external loader or Synexia runtime dependency. */
final class M3StateRelation {
  private M3StateRelation() { }

  // Callers ensure dimensions/domain validity and output does not overlap either input range.
  static void compose(long[] left, int lo, long[] right, int ro,
      long[] result, int out, int states) {
    if (states >= 16 && Long.bitCount(left[lo]) >= 8) {
      composeDense(left, lo, right, ro, result, out, states);
      return;
    }
    // Identical adjacent frontiers have the same exact result in this invocation.
    // Reuse two scalar locals, not a per-row object or retained result cache.
    long previous = 0L;
    long value = 0L;
    for (int state = 0; state < states; state++) {
      long active = left[lo + state];
      if (state == 0 || active != previous) {
        value = applyRows(right, ro, active);
        previous = active;
      }
      result[out + state] = value;
    }
  }

  private static long applyRows(long[] rows, int offset, long active) {
    long result = 0;
    while (active != 0) {
      result |= rows[offset + Long.numberOfTrailingZeros(active)];
      active &= active - 1;
    }
    return result;
  }

  private static void composeDense(long[] left, int lo, long[] right, int ro,
      long[] result, int out, int states) {
    long ceiling = 0L;
    for (int state = 0; state < states; state++) ceiling |= right[ro + state];
    long previous = 0L;
    long value = 0L;
    for (int state = 0; state < states; state++) {
      long active = left[lo + state];
      if (state == 0 || active != previous) {
        value = applyRowsUntil(right, ro, active, ceiling);
        previous = active;
      }
      result[out + state] = value;
    }
  }

  private static long applyRowsUntil(long[] rows, int offset, long active, long ceiling) {
    long result = 0L;
    while (active != 0L && result != ceiling) {
      result |= rows[offset + Long.numberOfTrailingZeros(active)];
      active &= active - 1L;
    }
    return result;
  }

  /** Explicit numeric JNI lane, built into libjava; the production regex adapter stays Java. */
  static native long[] composeNative(long[] left, long[] right);
  static native long[] repeatNative(long[] rows, long count);
}
