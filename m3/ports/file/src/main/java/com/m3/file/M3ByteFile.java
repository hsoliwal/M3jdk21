// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** Immutable raw-byte counterpart to token-native {@link M3File}. */
public final class M3ByteFile {
  private final M3FileAttributes before;
  private final M3FileAttributes after;
  private final M3FileByteFacts byteFacts;

  M3ByteFile(
      M3FileAttributes before,
      M3FileAttributes after,
      M3FileByteFacts byteFacts) {
    this.before = Objects.requireNonNull(before, "before");
    this.after = Objects.requireNonNull(after, "after");
    this.byteFacts = Objects.requireNonNull(byteFacts, "byteFacts");
  }

  public M3FileAttributes attributesBeforeRead() {
    return before;
  }

  public M3FileAttributes attributesAfterRead() {
    return after;
  }

  public M3FileByteFacts byteFacts() {
    return byteFacts;
  }

  /** Direct raw-byte facade for one defensive projection of all one-pass byte facts. */
  public M3FileByteFactsSnapshot byteFactsSnapshot() {
    return byteFacts.snapshot();
  }

  public boolean stableDuringRead() {
    return before.sameContentIdentity(after)
        && byteFacts.byteCount() == before.size()
        && byteFacts.byteCount() == after.size();
  }

  public long retainedPayloadBytes() {
    return byteFacts.retainedPayloadBytes();
  }
}
