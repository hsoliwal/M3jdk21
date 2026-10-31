// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.function.IntConsumer;

/** Primitive deterministic frequency rank over one immutable M3File local lexicon. */
public final class M3FileFrequencyIndex {
  private final M3File file;
  private final int[] handlesByRank;
  private final int[] rankByHandle;

  M3FileFrequencyIndex(M3File file) {
    this.file = Objects.requireNonNull(file, "file");
    int unique = file.uniqueTokenCount();
    handlesByRank = new int[unique];
    rankByHandle = new int[unique];
    if (unique == 0) return;

    int maxFrequency = 0;
    for (int handle = 0; handle < unique; handle++) {
      maxFrequency = Math.max(maxFrequency, file.occurrenceCount(handle));
    }
    int[] bucketCounts = new int[maxFrequency + 1];
    for (int handle = 0; handle < unique; handle++) {
      bucketCounts[file.occurrenceCount(handle)]++;
    }
    int[] next = new int[maxFrequency + 1];
    int rank = 0;
    for (int frequency = maxFrequency; frequency >= 1; frequency--) {
      next[frequency] = rank;
      rank = Math.addExact(rank, bucketCounts[frequency]);
    }
    for (int handle = 0; handle < unique; handle++) {
      int frequency = file.occurrenceCount(handle);
      handlesByRank[next[frequency]++] = handle;
    }
    for (int ordinal = 0; ordinal < unique; ordinal++) {
      rankByHandle[handlesByRank[ordinal]] = ordinal;
    }
  }

  public M3File file() { return file; }

  public int uniqueTokenCount() { return handlesByRank.length; }

  public int localHandleAtRank(int rank) {
    return handlesByRank[Objects.checkIndex(rank, handlesByRank.length)];
  }

  public int frequencyAtRank(int rank) {
    return file.occurrenceCount(localHandleAtRank(rank));
  }

  public int rankOfLocalHandle(int localHandle) {
    return rankByHandle[Objects.checkIndex(localHandle, rankByHandle.length)];
  }

  public int[] topLocalHandles(int limit) {
    if (limit < 0) throw new IllegalArgumentException("negative limit");
    int length = Math.min(limit, handlesByRank.length);
    int[] result = new int[length];
    System.arraycopy(handlesByRank, 0, result, 0, length);
    return result;
  }

  public void forEachTopLocalHandle(int limit, IntConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    if (limit < 0) throw new IllegalArgumentException("negative limit");
    int length = Math.min(limit, handlesByRank.length);
    for (int ordinal = 0; ordinal < length; ordinal++) {
      consumer.accept(handlesByRank[ordinal]);
    }
  }

  public int countAtLeast(int minimumFrequency) {
    if (minimumFrequency <= 0) return handlesByRank.length;
    int low = 0;
    int high = handlesByRank.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (frequencyAtRank(middle) >= minimumFrequency) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  public long retainedPayloadBytes() {
    return Integer.BYTES * (long) (handlesByRank.length + rankByHandle.length);
  }
}
