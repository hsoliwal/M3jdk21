// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/** Typed assertions for running two pinned donor JUnit test bodies as dependency-free mains. */
final class M3FileProbeAssertions {
  private M3FileProbeAssertions() { }
  static void assertEquals(int expected, int actual) {
    if (expected != actual) throw new AssertionError("expected " + expected + ", got " + actual);
  }
  static void assertEquals(long expected, long actual) {
    if (expected != actual) throw new AssertionError("expected " + expected + ", got " + actual);
  }
  static void assertEquals(Object expected, Object actual) {
    if (!Objects.equals(expected, actual)) throw new AssertionError("expected " + expected + ", got " + actual);
  }
  static void assertArrayEquals(int[] expected, int[] actual) {
    if (!Arrays.equals(expected, actual)) throw new AssertionError("array mismatch");
  }
  static void assertTrue(boolean value) { if (!value) throw new AssertionError("expected true"); }
  static void assertFalse(boolean value) { if (value) throw new AssertionError("expected false"); }
  @FunctionalInterface interface ThrowingAction { void run() throws Throwable; }
  static <T extends Throwable> T assertThrows(Class<T> expected, ThrowingAction action) {
    try { action.run(); }
    catch (Throwable actual) {
      if (expected.isInstance(actual)) return expected.cast(actual);
      throw new AssertionError("expected " + expected + ", got " + actual, actual);
    }
    throw new AssertionError("expected " + expected);
  }
}
