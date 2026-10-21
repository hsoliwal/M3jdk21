// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

/** Primitive producer contract for existing prepared byte-pattern machine lanes. */
public interface M3PreparedBytePatternMachine {
  int ALPHABET_SIZE = 256;
  int totalPatternBytes();
  int patternCount();
  int stateCount();
  int transitionCount();
  int patternOffset(int pattern);
  int patternByteAt(int pattern, int offset);
  int transitionAt(int offset);
  long outputMaskAt(int state);
}
