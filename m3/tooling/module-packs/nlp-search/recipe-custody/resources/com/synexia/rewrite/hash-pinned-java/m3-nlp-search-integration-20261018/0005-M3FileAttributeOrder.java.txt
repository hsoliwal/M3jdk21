// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Comparator;
import java.util.Objects;

/**
 * Canonical total order for metadata-first Universe admission.
 *
 * <p>Creation time and last-modified time are the primary filesystem order. Normalized path is the
 * deterministic tie-breaker; the nullable provider file identity is the final tie-breaker so an
 * attribute snapshot never relies on provider-specific identity being present.</p>
 */
public final class M3FileAttributeOrder {
  /** Creation time, modification time, normalized path, then nullable file identity. */
  public static final Comparator<M3FileAttributes> CREATION_THEN_MODIFIED_THEN_PATH =
      Comparator.comparing(M3FileAttributes::creationTime)
          .thenComparing(M3FileAttributes::lastModifiedTime)
          .thenComparing(M3FileAttributes::normalizedPath)
          .thenComparing(
              M3FileAttributes::fileKey,
              Comparator.nullsFirst(Comparator.naturalOrder()));

  private M3FileAttributeOrder() {}

  /** Compare two already-captured immutable attribute snapshots. */
  public static int compare(M3FileAttributes left, M3FileAttributes right) {
    return CREATION_THEN_MODIFIED_THEN_PATH.compare(
        Objects.requireNonNull(left, "left"),
        Objects.requireNonNull(right, "right"));
  }
}
