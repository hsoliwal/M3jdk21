// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Objects;

/** Optional JNI executor for the frozen derivative-DFA run-power image. */
final class M3RegexDerivativeNative {
  private static final int NATIVE_BATCH_THRESHOLD =
      Math.max(1, Integer.getInteger("m3.precompute.derivative.nativeBatchThreshold", 32));
  private static volatile Boolean available;

  private M3RegexDerivativeNative() {}

  static boolean shouldUse(int rows) { return rows >= NATIVE_BATCH_THRESHOLD; }

  static boolean available() {
    Boolean current = available;
    if (current != null) return current;
    synchronized (M3RegexDerivativeNative.class) {
      current = available;
      if (current == null) {
        try {
          M3CodeTextSignalBatchNative.loadRequired();
          current = Boolean.TRUE;
        } catch (RuntimeException | LinkageError failure) {
          current = Boolean.FALSE;
        }
        available = current;
      }
      return current;
    }
  }

  static byte[] matchRunsBatch(
      int classes,
      int states,
      int startState,
      int[] powers,
      int powerBits,
      byte[] accepting,
      byte[] live,
      int[] runClasses,
      int[] runLengths,
      int[] offsets) {
    Objects.requireNonNull(powers, "powers");
    Objects.requireNonNull(accepting, "accepting");
    Objects.requireNonNull(live, "live");
    Objects.requireNonNull(runClasses, "runClasses");
    Objects.requireNonNull(runLengths, "runLengths");
    Objects.requireNonNull(offsets, "offsets");
    if (classes < 1
        || states < 1
        || startState < 0
        || startState >= states
        || powerBits < 1
        || accepting.length != states
        || live.length != states
        || runClasses.length != runLengths.length
        || offsets.length < 1
        || offsets[0] != 0
        || offsets[offsets.length - 1] != runClasses.length
        || powers.length
            != Math.multiplyExact(
                powerBits, Math.multiplyExact(classes, states))) {
      throw new IllegalArgumentException("invalid derivative regex native geometry");
    }
    for (int index = 1; index < offsets.length; index++) {
      if (offsets[index] < offsets[index - 1]
          || offsets[index] > runClasses.length) {
        throw new IllegalArgumentException("invalid derivative regex row offsets");
      }
    }
    for (int index = 0; index < runClasses.length; index++) {
      if (runClasses[index] < 0
          || runClasses[index] >= classes
          || runLengths[index] < 0) {
        throw new IllegalArgumentException("invalid derivative regex run");
      }
    }

    try {
      byte[] result =
          nativeMatchRunsBatch(
              classes,
              states,
              startState,
              powers,
              powerBits,
              accepting,
              live,
              runClasses,
              runLengths,
              offsets);
      if (result == null || result.length != offsets.length - 1) {
        throw new LinkageError("invalid derivative regex JNI result");
      }
      return result;
    } catch (UnsatisfiedLinkError incompatibleLibrary) {
      available = Boolean.FALSE;
      throw incompatibleLibrary;
    }
  }

  private static native byte[] nativeMatchRunsBatch(
      int classes,
      int states,
      int startState,
      int[] powers,
      int powerBits,
      byte[] accepting,
      byte[] live,
      int[] runClasses,
      int[] runLengths,
      int[] offsets);
}
