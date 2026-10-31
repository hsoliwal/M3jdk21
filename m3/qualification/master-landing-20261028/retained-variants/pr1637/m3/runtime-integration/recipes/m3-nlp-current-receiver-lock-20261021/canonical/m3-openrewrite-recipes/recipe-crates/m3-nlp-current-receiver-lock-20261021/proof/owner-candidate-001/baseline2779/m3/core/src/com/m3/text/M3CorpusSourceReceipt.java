// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.text;

import java.net.URI;
import java.util.Objects;

/** Immutable input custody. A valid checksum establishes bytes, not the truth of their claims. */
public record M3CorpusSourceReceipt(String provider, URI uri, String revision, String license, String sha256) {
  public M3CorpusSourceReceipt {
    provider = required(provider);
    revision = required(revision);
    license = required(license);
    Objects.requireNonNull(uri, "uri");
    if (!uri.isAbsolute() || uri.getUserInfo() != null) throw new IllegalArgumentException("source URI");
    if (!Objects.requireNonNull(sha256, "sha256").matches("[0-9a-f]{64}"))
      throw new IllegalArgumentException("lowercase SHA-256 required");
    if (provider.equals("HUGGING_FACE") && !revision.matches("[0-9a-f]{40}"))
      throw new IllegalArgumentException("Hugging Face requires a full commit revision");
    if (provider.equals("KAGGLE") && !revision.matches("[1-9][0-9]*"))
      throw new IllegalArgumentException("Kaggle requires an explicit dataset version number");
  }

  /** Reject oversized or substituted payloads before any parser sees them. */
  public void verify(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    if (bytes.length > 16 * 1024 * 1024) throw new IllegalArgumentException("input exceeds 16 MiB");
    if (!M3CorpusProjectionDigest.bytes(bytes).equals(sha256)) throw new IllegalArgumentException("source hash mismatch");
  }

  private static String required(String value) {
    if (Objects.requireNonNull(value, "value").isBlank() || value.indexOf('\n') >= 0
        || value.indexOf('\t') >= 0 || value.indexOf('\r') >= 0)
      throw new IllegalArgumentException("nonblank single-line metadata required");
    return value;
  }
}
