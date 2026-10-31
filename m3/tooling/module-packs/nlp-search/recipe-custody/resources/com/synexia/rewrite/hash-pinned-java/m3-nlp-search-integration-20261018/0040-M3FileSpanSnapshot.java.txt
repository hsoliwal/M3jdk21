// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import java.nio.charset.Charset;
import com.m3.file.M3Hash256;
import com.m3.file.M3FileText;
import com.m3.file.M3FileResolver;

import java.util.Objects;

/**
 * Detached precomputed coordinates for one source-backed M3File token span.
 *
 * <p>The range fingerprint is a candidate identity only. It is useful for JNI, serializers and
 * exact-token candidate filtering, but it is deliberately not an exact raw-byte SHA-256.</p>
 */
public record M3FileSpanSnapshot(
    int startToken,
    int endToken,
    int utf16Start,
    int utf16End,
    long utf8Start,
    long utf8End,
    int codePointStart,
    int codePointEnd,
    M3FileRangeFingerprint rangeFingerprint) {
  public M3FileSpanSnapshot {
    if (startToken < 0 || endToken < startToken) {
      throw new IllegalArgumentException("invalid token span");
    }
    if (utf16Start < 0 || utf16End < utf16Start) {
      throw new IllegalArgumentException("invalid UTF-16 span");
    }
    if (utf8Start < 0L || utf8End < utf8Start) {
      throw new IllegalArgumentException("invalid UTF-8 span");
    }
    if (codePointStart < 0 || codePointEnd < codePointStart) {
      throw new IllegalArgumentException("invalid code-point span");
    }
    rangeFingerprint = Objects.requireNonNull(rangeFingerprint, "rangeFingerprint");
    if (rangeFingerprint.tokenCount() != endToken - startToken) {
      throw new IllegalArgumentException("range fingerprint length mismatch");
    }
  }

  public int tokenCount() {
    return endToken - startToken;
  }

  public int utf16Length() {
    return utf16End - utf16Start;
  }

  public long utf8Length() {
    return utf8End - utf8Start;
  }

  public int codePointLength() {
    return codePointEnd - codePointStart;
  }

  /**
   * Reconstructs this detached span from retained lexicon IDs and UTF-16 coordinates.
   *
   * <p>No source path or source bytes are consulted.</p>
   */
  public M3FileText asM3String(M3FileSnapshot snapshot, M3FileResolver resolver) {
    M3FileSnapshot checkedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
    Objects.checkFromToIndex(
        startToken, endToken, checkedSnapshot.primitiveSnapshot().tokenCount());
    return checkedSnapshot.indexedText(Objects.requireNonNull(resolver, "resolver"))
        .substring(utf16Start, utf16End);
  }

  /** Computes the detached span digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(
      M3FileSnapshot snapshot, M3FileResolver resolver, Charset charset) {
    return asM3String(snapshot, resolver)
        .sha256(Objects.requireNonNull(charset, "charset"));
  }
}
