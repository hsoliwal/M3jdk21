// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** Defensive bulk view of one file-local lexicon's flattened resolver-ID rows. */
public final class M3FileLexiconSnapshot {
  private final long[] tokenIds;
  private final int[] tokenStarts;

  M3FileLexiconSnapshot(long[] tokenIds, int[] tokenStarts) {
    this.tokenIds = Objects.requireNonNull(tokenIds, "tokenIds");
    this.tokenStarts = Objects.requireNonNull(tokenStarts, "tokenStarts");
    if (tokenStarts.length == 0
        || tokenStarts[0] != 0
        || tokenStarts[tokenStarts.length - 1] != tokenIds.length) {
      throw new IllegalArgumentException("invalid M3File lexicon snapshot shape");
    }
    int previous = 0;
    for (int boundary = 1; boundary < tokenStarts.length; boundary++) {
      int next = tokenStarts[boundary];
      if (next < previous || next > tokenIds.length) {
        throw new IllegalArgumentException("invalid M3File lexicon snapshot boundary");
      }
      previous = next;
    }
  }

  public int uniqueTokenCount() {
    return tokenStarts.length - 1;
  }

  public int resolverTokenCount() {
    return tokenIds.length;
  }

  /** Flattened resolver IDs in immutable local-handle order. */
  public long[] tokenIds() {
    return tokenIds.clone();
  }

  /** CSR-style row boundaries indexed by local handle. */
  public int[] tokenStarts() {
    return tokenStarts.clone();
  }

  public int tokenCount(int localHandle) {
    int handle = Objects.checkIndex(localHandle, uniqueTokenCount());
    return tokenStarts[handle + 1] - tokenStarts[handle];
  }

  public long tokenIdAt(int localHandle, int ordinal) {
    int handle = Objects.checkIndex(localHandle, uniqueTokenCount());
    int start = tokenStarts[handle];
    return tokenIds[start + Objects.checkIndex(ordinal, tokenStarts[handle + 1] - start)];
  }
}
