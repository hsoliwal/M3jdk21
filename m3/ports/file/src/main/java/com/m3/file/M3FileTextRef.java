// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** O(1) canonical whole-string handle: pool identity plus integer row. */
public final class M3FileTextRef {
  private final M3FileTextPool pool;
  private final int handle;

  M3FileTextRef(M3FileTextPool pool, int handle) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.handle = handle;
  }

  public int handle() { return handle; }
  public M3FileText value() { return pool.value(handle); }
  public String materialize() { return value().materialize(); }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof M3FileTextRef that
        && pool == that.pool
        && handle == that.handle;
  }

  @Override public int hashCode() { return 31 * System.identityHashCode(pool) + handle; }
  @Override public String toString() { return materialize(); }
}
