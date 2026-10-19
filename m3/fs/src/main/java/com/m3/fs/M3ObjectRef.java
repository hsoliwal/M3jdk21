// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.util.Objects;

/** Typed immutable reference to one physical content-addressed object. */
public record M3ObjectRef(M3ContentId id, long size, M3ObjectKind kind) {
  public M3ObjectRef {
    id = Objects.requireNonNull(id, "id");
    kind = Objects.requireNonNull(kind, "kind");
    if (size < 0L) throw new IllegalArgumentException("negative object size");
  }
}
