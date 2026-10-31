// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.util.Arrays;
import java.util.Objects;

/**
 * One defensive bulk view of all source-side facts retained during one M3File admission.
 *
 * <p>This is a projection of an immutable source-facts owner. It does not reread source bytes,
 * rebuild token hashes, or change the source-facts sidecar format.</p>
 */
public final class M3FileSourceFactsSnapshot {
  private final M3FileAttributes before;
  private final M3FileAttributes after;
  private final M3FileByteFactsSnapshot byteFacts;
  private final long[] utf8Offsets;
  private final int[] codePointOffsets;
  private final byte[] tokenSha256ByLocalHandle;
  private final long[] prefixA;
  private final long[] prefixB;
  private final long[] powersA;
  private final long[] powersB;
  private final int tokenCount;
  private final int uniqueTokenCount;

  M3FileSourceFactsSnapshot(
      M3FileAttributes before,
      M3FileAttributes after,
      M3FileByteFactsSnapshot byteFacts,
      long[] utf8Offsets,
      int[] codePointOffsets,
      byte[] tokenSha256ByLocalHandle,
      long[] prefixA,
      long[] prefixB,
      long[] powersA,
      long[] powersB,
      int tokenCount,
      int uniqueTokenCount) {
    this.before = Objects.requireNonNull(before, "before");
    this.after = Objects.requireNonNull(after, "after");
    this.byteFacts = Objects.requireNonNull(byteFacts, "byteFacts");
    this.utf8Offsets = Objects.requireNonNull(utf8Offsets, "utf8Offsets").clone();
    this.codePointOffsets = Objects.requireNonNull(codePointOffsets, "codePointOffsets").clone();
    this.tokenSha256ByLocalHandle =
        Objects.requireNonNull(tokenSha256ByLocalHandle, "tokenSha256ByLocalHandle").clone();
    this.prefixA = Objects.requireNonNull(prefixA, "prefixA").clone();
    this.prefixB = Objects.requireNonNull(prefixB, "prefixB").clone();
    this.powersA = Objects.requireNonNull(powersA, "powersA").clone();
    this.powersB = Objects.requireNonNull(powersB, "powersB").clone();
    this.tokenCount = tokenCount;
    this.uniqueTokenCount = uniqueTokenCount;
  }

  public M3FileAttributes attributesBeforeRead() {
    return before;
  }

  public M3FileAttributes attributesAfterRead() {
    return after;
  }

  public boolean stableDuringRead() {
    return before.sameContentIdentity(after)
        && byteFacts.byteCount() == before.size()
        && byteFacts.byteCount() == after.size();
  }

  public M3FileByteFactsSnapshot byteFactsSnapshot() {
    return byteFacts;
  }

  public M3Hash256 contentSha256() {
    return byteFacts.sha256();
  }

  public int tokenCount() {
    return tokenCount;
  }

  public int uniqueTokenCount() {
    return uniqueTokenCount;
  }

  public long utf8Offset(int tokenBoundary) {
    return utf8Offsets[checkedBoundary(tokenBoundary)];
  }

  public long utf8Length() {
    return utf8Offsets[tokenCount];
  }

  public long[] utf8Offsets() {
    return utf8Offsets.clone();
  }

  public int codePointOffset(int tokenBoundary) {
    return codePointOffsets[checkedBoundary(tokenBoundary)];
  }

  public int codePointLength() {
    return codePointOffsets[tokenCount];
  }

  public int[] codePointOffsets() {
    return codePointOffsets.clone();
  }

  public M3Hash256 tokenSha256ForLocalHandle(int localHandle) {
    int handle = Objects.checkIndex(localHandle, uniqueTokenCount);
    int start = Math.multiplyExact(handle, M3Hash256.BYTES);
    return M3Hash256.of(
        Arrays.copyOfRange(tokenSha256ByLocalHandle, start, start + M3Hash256.BYTES));
  }

  public byte[] tokenSha256Bytes() {
    return tokenSha256ByLocalHandle.clone();
  }

  public long[] prefixALane() {
    return prefixA.clone();
  }

  public long[] prefixBLane() {
    return prefixB.clone();
  }

  public long[] powerALane() {
    return powersA.clone();
  }

  public long[] powerBLane() {
    return powersB.clone();
  }

  public M3FileRangeFingerprint rangeFingerprint(int startToken, int endToken) {
    Objects.checkFromToIndex(startToken, endToken, tokenCount);
    int length = endToken - startToken;
    long laneA = prefixA[endToken] - prefixA[startToken] * powersA[length];
    long laneB = prefixB[endToken] - prefixB[startToken] * powersB[length];
    return new M3FileRangeFingerprint(laneA, laneB, length);
  }

  public long retainedPayloadBytes() {
    return byteFacts.retainedPayloadBytes()
        + tokenSha256ByLocalHandle.length
        + Long.BYTES
            * (long)
                (utf8Offsets.length
                    + prefixA.length
                    + prefixB.length
                    + powersA.length
                    + powersB.length)
        + Integer.BYTES * (long) codePointOffsets.length;
  }

  private int checkedBoundary(int tokenBoundary) {
    return Objects.checkIndex(tokenBoundary, tokenCount + 1);
  }
}
