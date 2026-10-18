// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** Defensive export of one frozen M3File's primitive structure lanes. */
public final class M3FilePrimitiveSnapshot {
  private final int[] localHandles;
  private final byte[] tokenKinds;
  private final int[] utf16Offsets;
  private final int[] lineStarts;
  private final int[] frequencies;
  private final int[] firstOccurrences;
  private final int[] lastOccurrences;
  private final int[] postingStarts;
  private final int[] postings;
  private final long[] rollingPrefix;
  private final long[] rollingPowers;

  M3FilePrimitiveSnapshot(
      int[] localHandles,
      byte[] tokenKinds,
      int[] utf16Offsets,
      int[] lineStarts,
      int[] frequencies,
      int[] firstOccurrences,
      int[] lastOccurrences,
      int[] postingStarts,
      int[] postings,
      long[] rollingPrefix,
      long[] rollingPowers) {
    this.localHandles = copyRequired(localHandles, "localHandles");
    this.tokenKinds = copyRequired(tokenKinds, "tokenKinds");
    this.utf16Offsets = copyRequired(utf16Offsets, "utf16Offsets");
    this.lineStarts = copyRequired(lineStarts, "lineStarts");
    this.frequencies = copyRequired(frequencies, "frequencies");
    this.firstOccurrences = copyRequired(firstOccurrences, "firstOccurrences");
    this.lastOccurrences = copyRequired(lastOccurrences, "lastOccurrences");
    this.postingStarts = copyRequired(postingStarts, "postingStarts");
    this.postings = copyRequired(postings, "postings");
    if (tokenKinds.length != localHandles.length
        || utf16Offsets.length != localHandles.length + 1
        || lineStarts.length == 0
        || firstOccurrences.length != frequencies.length
        || lastOccurrences.length != frequencies.length
        || postingStarts.length != frequencies.length + 1
        || postings.length != localHandles.length
        || postingStarts[0] != 0
        || postingStarts[postingStarts.length - 1] != postings.length) {
      throw new IllegalArgumentException("invalid M3File primitive snapshot shape");
    }
    for (int handle : localHandles) {
      if (handle < 0 || handle >= frequencies.length) {
        throw new IllegalArgumentException("invalid M3File primitive snapshot handle");
      }
    }
    for (int handle = 0; handle < frequencies.length; handle++) {
      int start = postingStarts[handle];
      int end = postingStarts[handle + 1];
      if (frequencies[handle] < 0 || start < 0 || end < start || end > postings.length
          || frequencies[handle] != end - start) {
        throw new IllegalArgumentException("invalid M3File primitive snapshot posting row");
      }
      int previousPosition = -1;
      for (int at = start; at < end; at++) {
        int position = postings[at];
        if (position <= previousPosition || position < 0 || position >= localHandles.length
            || localHandles[position] != handle) {
          throw new IllegalArgumentException("invalid M3File primitive snapshot posting member");
        }
        previousPosition = position;
      }
    }
    if ((rollingPrefix == null) != (rollingPowers == null)
        || (rollingPrefix != null && rollingPrefix.length != localHandles.length + 1)
        || (rollingPowers != null && rollingPowers.length != localHandles.length + 1)) {
      throw new IllegalArgumentException("invalid M3File rolling snapshot shape");
    }
    this.rollingPrefix = rollingPrefix == null ? null : rollingPrefix.clone();
    this.rollingPowers = rollingPowers == null ? null : rollingPowers.clone();
  }

  public int tokenCount() { return localHandles.length; }

  public int uniqueTokenCount() { return frequencies.length; }

  /** Local lexicon handle at one token occurrence without cloning the full handle lane. */
  public int localHandleAt(int tokenIndex) {
    return localHandles[Objects.checkIndex(tokenIndex, localHandles.length)];
  }

  public int lineCount() { return lineStarts.length; }

  public int postingCount() { return postings.length; }

  public boolean hasRollingLanes() { return rollingPrefix != null; }

  public int[] localHandles() { return localHandles.clone(); }

  /** Token-kind enum codes in occurrence order. */
  public byte[] tokenKinds() { return tokenKinds.clone(); }

  public int[] utf16Offsets() { return utf16Offsets.clone(); }

  public int utf16StartAt(int tokenIndex) {
    return utf16Offsets[Objects.checkIndex(tokenIndex, localHandles.length)];
  }

  public int utf16EndAt(int tokenIndex) {
    return utf16Offsets[Objects.checkIndex(tokenIndex, localHandles.length) + 1];
  }

  public int[] lineStarts() { return lineStarts.clone(); }

  public int[] frequencies() { return frequencies.clone(); }

  public int[] firstOccurrences() { return firstOccurrences.clone(); }

  public int[] lastOccurrences() { return lastOccurrences.clone(); }

  /** CSR row starts indexed by local handle. */
  public int[] postingStarts() { return postingStarts.clone(); }

  /** CSR occurrence positions flattened by local handle. */
  public int[] postings() { return postings.clone(); }

  public long[] rollingPrefix() {
    return rollingPrefix == null ? null : rollingPrefix.clone();
  }

  public long[] rollingPowers() {
    return rollingPowers == null ? null : rollingPowers.clone();
  }

  public long retainedPayloadBytes() {
    long retained = localHandles.length * (long) Integer.BYTES
        + tokenKinds.length
        + (long) Integer.BYTES
            * (utf16Offsets.length
                + lineStarts.length
                + frequencies.length
                + firstOccurrences.length
                + lastOccurrences.length
                + postingStarts.length
                + postings.length);
    if (rollingPrefix != null) {
      retained = Math.addExact(
          retained, Long.BYTES * (long) (rollingPrefix.length + rollingPowers.length));
    }
    return retained;
  }

  private static int[] copyRequired(int[] values, String name) {
    return Objects.requireNonNull(values, name).clone();
  }

  private static byte[] copyRequired(byte[] values, String name) {
    return Objects.requireNonNull(values, name).clone();
  }
}
