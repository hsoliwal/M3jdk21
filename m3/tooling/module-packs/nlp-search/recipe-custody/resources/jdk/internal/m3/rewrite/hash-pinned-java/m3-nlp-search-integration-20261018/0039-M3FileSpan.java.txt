// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import com.m3.file.M3Hash256;
import java.nio.charset.Charset;

import com.m3.file.M3FileText;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Zero-copy token range over one immutable M3File. */
public record M3FileSpan(M3File file, int startToken, int endToken) {
  public M3FileSpan {
    Objects.requireNonNull(file, "file");
    Objects.checkFromToIndex(startToken, endToken, file.tokenCount());
  }

  public int tokenCount() {
    return endToken - startToken;
  }

  public int utf16Start() {
    return file.utf16Offset(startToken);
  }

  public int utf16End() {
    return file.utf16Offset(endToken);
  }

  public int utf16Length() {
    return utf16End() - utf16Start();
  }

  /**
   * Detaches the already-precomputed source coordinates and candidate range identity.
   *
   * <p>Source facts are required because UTF-8/code-point coordinates and the composable range
   * fingerprint are admission-time facts. Exact token-range SHA-256 is exposed separately through
   * {@link #tokenRangeSha256()} and is computed from the retained resolver atoms.</p>
   */
  public M3FileSpanSnapshot snapshot() {
    M3FileSourceFacts facts = file.sourceFacts();
    return new M3FileSpanSnapshot(
        startToken,
        endToken,
        utf16Start(),
        utf16End(),
        facts.utf8Offset(startToken),
        facts.utf8Offset(endToken),
        facts.codePointOffset(startToken),
        facts.codePointOffset(endToken),
        facts.rangeFingerprint(startToken, endToken));
  }

  public M3FileSpan subSpan(int fromInclusive, int toExclusive) {
    Objects.checkFromToIndex(fromInclusive, toExclusive, tokenCount());
    return new M3FileSpan(file, startToken + fromInclusive, startToken + toExclusive);
  }

  /** Returns this token-boundary range as a resolver-owned indexed projection. */
  public M3FileText asM3String() {
    return file.asM3String().substring(utf16Start(), utf16End());
  }

  /** Computes the token-span digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(Charset charset) {
    return asM3String().sha256(Objects.requireNonNull(charset, "charset"));
  }

  /**
   * Computes exact SHA-256 for this token-boundary range from retained canonical atoms.
   *
   * <p>The source stream is not reopened and the logical range is not joined into one value. This
   * operation is available only for physical-source admissions because its byte coordinates come
   * from the admission-time source-facts owner.</p>
   */
  public M3FileTokenRangeSha256 tokenRangeSha256() {
    M3FileSourceFacts facts = file.sourceFacts();
    return new M3FileTokenRangeSha256(
        startToken,
        endToken,
        facts.utf8Offset(startToken),
        facts.utf8Offset(endToken),
        asM3String().sha256(StandardCharsets.UTF_8));
  }

  public M3FilePattern preparePattern() {
    int[] handles = new int[tokenCount()];
    for (int i = 0; i < handles.length; i++) {
      handles[i] = file.localHandleAt(startToken + i);
    }
    return M3FilePattern.fromLocalHandles(file, handles);
  }

  public String materialize() {
    StringBuilder out = new StringBuilder(utf16Length());
    file.appendRangeTo(startToken, endToken, out);
    return out.toString();
  }

  @Override
  public String toString() {
    return materialize();
  }
}
