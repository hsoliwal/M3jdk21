// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

/** Primitive producer contract for the pinned prepared-segment storage format. */
public interface M3PreparedByteSegments {
  int size();
  int packedBytes();
  int bitmapWords();
  int pairNgramBuckets();
  int trigramNgramBuckets();
  int maxSegmentLength();
  int byteAt(int offset);
  int segmentStart(int segment);
  int segmentLength(int segment);
  long bytePresenceWord(int byteValue, int word);
  long pairNgramPresenceWord(int bucket, int word);
  long trigramNgramPresenceWord(int bucket, int word);
}
