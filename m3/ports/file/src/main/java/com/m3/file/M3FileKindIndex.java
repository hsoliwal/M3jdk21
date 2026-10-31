// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.function.IntConsumer;

/** Primitive CSR postings from token kind to source-token positions. */
public final class M3FileKindIndex {
  private static final int KIND_COUNT = M3FileTokenKind.values().length;
  private final M3File file;
  private final int[] starts;
  private final int[] positions;

  M3FileKindIndex(M3File file) {
    this.file = Objects.requireNonNull(file, "file");
    int[] counts = new int[KIND_COUNT];
    for (int token = 0; token < file.tokenCount(); token++) {
      counts[file.tokenKindAt(token).ordinal()]++;
    }
    starts = new int[KIND_COUNT + 1];
    for (int k = 0; k < KIND_COUNT; k++) {
      starts[k + 1] = Math.addExact(starts[k], counts[k]);
    }
    positions = new int[file.tokenCount()];
    int[] cursors = starts.clone();
    for (int token = 0; token < file.tokenCount(); token++) {
      int k = file.tokenKindAt(token).ordinal();
      positions[cursors[k]++] = token;
    }
  }

  public M3File file() { return file; }

  public int count(M3FileTokenKind kind) {
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    return starts[k + 1] - starts[k];
  }

  public int count(M3FileTokenKind kind, int startToken, int endToken) {
    Objects.checkFromToIndex(startToken, endToken, file.tokenCount());
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    int from = lowerBound(positions, starts[k], starts[k + 1], startToken);
    int to = lowerBound(positions, from, starts[k + 1], endToken);
    return to - from;
  }

  public int first(M3FileTokenKind kind) {
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    return starts[k] == starts[k + 1] ? -1 : positions[starts[k]];
  }

  public int last(M3FileTokenKind kind) {
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    return starts[k] == starts[k + 1] ? -1 : positions[starts[k + 1] - 1];
  }

  public int firstAtOrAfter(M3FileTokenKind kind, int fromToken) {
    if (fromToken < 0 || fromToken > file.tokenCount()) {
      throw new IndexOutOfBoundsException("from token " + fromToken);
    }
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    int at = lowerBound(positions, starts[k], starts[k + 1], fromToken);
    return at == starts[k + 1] ? -1 : positions[at];
  }

  public int lastBefore(M3FileTokenKind kind, int beforeTokenExclusive) {
    if (beforeTokenExclusive < 0 || beforeTokenExclusive > file.tokenCount()) {
      throw new IndexOutOfBoundsException("before token " + beforeTokenExclusive);
    }
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    int at = lowerBound(positions, starts[k], starts[k + 1], beforeTokenExclusive) - 1;
    return at < starts[k] ? -1 : positions[at];
  }

  public void forEach(M3FileTokenKind kind, IntConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    int k = Objects.requireNonNull(kind, "kind").ordinal();
    for (int at = starts[k]; at < starts[k + 1]; at++) consumer.accept(positions[at]);
  }

  public long retainedPayloadBytes() {
    return Integer.BYTES * (long) (starts.length + positions.length);
  }

  private static int lowerBound(int[] values, int from, int to, int target) {
    int low = from;
    int high = to;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] < target) low = middle + 1;
      else high = middle;
    }
    return low;
  }
}
