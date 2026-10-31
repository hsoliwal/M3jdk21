// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import java.nio.charset.Charset;
import com.m3.file.M3Hash256;
import com.m3.file.M3FileText;
import com.m3.file.M3FileResolver;

import java.util.Objects;

/** Detached immutable projection of one ordered M3File token-selection lane. */
public record M3FileProjectionSnapshot(int kindMask, int[] sourceTokenPositions) {
  public M3FileProjectionSnapshot {
    sourceTokenPositions = Objects.requireNonNull(sourceTokenPositions, "sourceTokenPositions").clone();
    int previous = -1;
    for (int position : sourceTokenPositions) {
      if (position < 0 || position <= previous) {
        throw new IllegalArgumentException("source token positions must be strictly increasing");
      }
      previous = position;
    }
  }

  @Override
  public int[] sourceTokenPositions() {
    return sourceTokenPositions.clone();
  }

  public int tokenCount() {
    return sourceTokenPositions.length;
  }

  public int sourceTokenIndexAt(int ordinal) {
    return sourceTokenPositions[Objects.checkIndex(ordinal, sourceTokenPositions.length)];
  }

  /** Reconstructs this detached selection through the supplied snapshot and resolver. */
  public M3FileText indexedText(M3FileSnapshot snapshot, M3FileResolver resolver) {
    return Objects.requireNonNull(snapshot, "snapshot")
        .indexedText(Objects.requireNonNull(resolver, "resolver"), sourceTokenPositions);
  }

  /** Computes the detached selection digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(
      M3FileSnapshot snapshot, M3FileResolver resolver, Charset charset) {
    return indexedText(snapshot, resolver)
        .sha256(Objects.requireNonNull(charset, "charset"));
  }
}
