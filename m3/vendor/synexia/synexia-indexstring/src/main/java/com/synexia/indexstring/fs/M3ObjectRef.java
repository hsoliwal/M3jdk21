// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.util.Objects;

/** Typed immutable reference to one physical content-addressed object. */
public record M3ObjectRef(M3ContentId id, long size, M3ObjectKind kind) {
  public M3ObjectRef {
    id = Objects.requireNonNull(id, "id");
    kind = Objects.requireNonNull(kind, "kind");
    if (size < 0L) throw new IllegalArgumentException("negative object size");
  }
}
