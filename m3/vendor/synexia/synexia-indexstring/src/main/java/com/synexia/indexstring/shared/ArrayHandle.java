// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import java.util.Objects;
import java.util.UUID;

/** Persistent identity: namespace, immutable record, and UTF-16/byte range within that record. */
public record ArrayHandle(UUID namespace, long recordOffset, int start, int length) {
  public ArrayHandle {
    Objects.requireNonNull(namespace, "namespace");
    if (recordOffset < 64 || start < 0 || length < 0 || (long) start + length > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("invalid array handle");
    }
  }
}
