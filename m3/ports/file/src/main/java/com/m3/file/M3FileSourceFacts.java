// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable source-side facts compiled while a physical file is admitted.
 *
 * <p>Exact SHA-256 is retained for the complete byte stream and for each unique file-local token.
 * Arbitrary token ranges use two precomputed composable fingerprint lanes. Those fingerprints are
 * candidate identities only and are intentionally not presented as SHA-256.</p>
 */
public final class M3FileSourceFacts {
  private static final long BASE_A = 0x9e3779b185ebca87L;
  private static final long BASE_B = 0xc2b2ae3d27d4eb4fL;

  private final M3FileAttributes before;
  private final M3FileAttributes after;
  private final M3FileByteFacts byteFacts;
  private final long[] utf8Offsets;
  private final int[] codePointOffsets;
  private final byte[] tokenSha256ByLocalHandle;
  private final long[] prefixA;
  private final long[] prefixB;
  private final long[] powersA;
  private final long[] powersB;
  private final int tokenCount;
  private final int uniqueTokenCount;

  private M3FileSourceFacts(
      M3FileAttributes before,
      M3FileAttributes after,
      M3FileByteFacts byteFacts,
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
    this.utf8Offsets = utf8Offsets.clone();
    this.codePointOffsets = codePointOffsets.clone();
    this.tokenSha256ByLocalHandle = tokenSha256ByLocalHandle.clone();
    this.prefixA = prefixA.clone();
    this.prefixB = prefixB.clone();
    this.powersA = powersA.clone();
    this.powersB = powersB.clone();
    this.tokenCount = tokenCount;
    this.uniqueTokenCount = uniqueTokenCount;
  }

  /** Validate and restore the existing format-1 source-facts lanes. */
  static M3FileSourceFacts restore(
      M3FileAttributes before,
      M3FileAttributes after,
      M3FileByteFacts byteFacts,
      long[] utf8Offsets,
      int[] codePointOffsets,
      byte[] tokenSha256ByLocalHandle,
      long[] prefixA,
      long[] prefixB,
      long[] powersA,
      long[] powersB,
      int tokenCount,
      int uniqueTokenCount) {
    if (tokenCount < 0
        || uniqueTokenCount < 0
        || utf8Offsets == null
        || codePointOffsets == null
        || prefixA == null
        || prefixB == null
        || powersA == null
        || powersB == null
        || tokenSha256ByLocalHandle == null
        || utf8Offsets.length != (long) tokenCount + 1L
        || codePointOffsets.length != (long) tokenCount + 1L
        || prefixA.length != (long) tokenCount + 1L
        || prefixB.length != (long) tokenCount + 1L
        || powersA.length != (long) tokenCount + 1L
        || powersB.length != (long) tokenCount + 1L
        || tokenSha256ByLocalHandle.length
            != Math.multiplyExact(uniqueTokenCount, M3Hash256.BYTES)) {
      throw new IllegalArgumentException("invalid serialized M3File source facts shape");
    }
    if (utf8Offsets[0] != 0L
        || codePointOffsets[0] != 0
        || prefixA[0] != 0L
        || prefixB[0] != 0L
        || powersA[0] != 1L
        || powersB[0] != 1L) {
      throw new IllegalArgumentException("serialized M3File source facts do not start at zero");
    }
    for (int token = 1; token <= tokenCount; token++) {
      if (utf8Offsets[token] < utf8Offsets[token - 1]
          || codePointOffsets[token] < codePointOffsets[token - 1]
          || codePointOffsets[token] > utf8Offsets[token]) {
        throw new IllegalArgumentException("serialized M3File source coordinates are not monotonic");
      }
      if (powersA[token] != powersA[token - 1] * BASE_A
          || powersB[token] != powersB[token - 1] * BASE_B) {
        throw new IllegalArgumentException("serialized M3File range powers are inconsistent");
      }
    }
    if (byteFacts == null
        || byteFacts.byteCount() != utf8Offsets[tokenCount]
        || before == null
        || after == null
        || !before.sameContentIdentity(after)
        || byteFacts.byteCount() != before.size()
        || byteFacts.byteCount() != after.size()) {
      throw new IllegalArgumentException("serialized M3File source facts are inconsistent");
    }
    return new M3FileSourceFacts(
        before,
        after,
        byteFacts,
        utf8Offsets,
        codePointOffsets,
        tokenSha256ByLocalHandle,
        prefixA,
        prefixB,
        powersA,
        powersB,
        tokenCount,
        uniqueTokenCount);
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

  public M3FileByteFacts byteFacts() {
    return byteFacts;
  }

  /** Return one defensive bulk projection of every source-side fact retained during admission. */
  public M3FileSourceFactsSnapshot snapshot() {
    return new M3FileSourceFactsSnapshot(
        before,
        after,
        byteFacts.snapshot(),
        utf8Offsets,
        codePointOffsets,
        tokenSha256ByLocalHandle,
        prefixA,
        prefixB,
        powersA,
        powersB,
        tokenCount,
        uniqueTokenCount);
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
    if (tokenBoundary < 0 || tokenBoundary > tokenCount) {
      throw new IndexOutOfBoundsException("token boundary " + tokenBoundary);
    }
    return utf8Offsets[tokenBoundary];
  }

  public long utf8Length() {
    return utf8Offsets[tokenCount];
  }

  public int codePointOffset(int tokenBoundary) {
    if (tokenBoundary < 0 || tokenBoundary > tokenCount) {
      throw new IndexOutOfBoundsException("token boundary " + tokenBoundary);
    }
    return codePointOffsets[tokenBoundary];
  }

  public int codePointLength() {
    return codePointOffsets[tokenCount];
  }

  long[] utf8OffsetsSnapshot() { return utf8Offsets.clone(); }

  int[] codePointOffsetsSnapshot() { return codePointOffsets.clone(); }

  byte[] tokenSha256Snapshot() { return tokenSha256ByLocalHandle.clone(); }

  long[] prefixASnapshot() { return prefixA.clone(); }

  long[] prefixBSnapshot() { return prefixB.clone(); }

  long[] powersASnapshot() { return powersA.clone(); }

  long[] powersBSnapshot() { return powersB.clone(); }

  public M3Hash256 tokenSha256ForLocalHandle(int localHandle) {
    int handle = Objects.checkIndex(localHandle, uniqueTokenCount);
    int start = Math.multiplyExact(handle, M3Hash256.BYTES);
    return M3Hash256.of(
        Arrays.copyOfRange(tokenSha256ByLocalHandle, start, start + M3Hash256.BYTES));
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

  static final class Builder {
    private long[] utf8Offsets = new long[65];
    private int[] codePointOffsets = new int[65];
    private long[] prefixA = new long[65];
    private long[] prefixB = new long[65];
    private long[] powersA = new long[65];
    private long[] powersB = new long[65];
    private byte[] tokenHashes = new byte[64 * M3Hash256.BYTES];
    private boolean[] tokenHashPresent = new boolean[64];
    private int tokenCount;
    private long utf8Length;
    private int codePointLength;

    Builder() {
      powersA[0] = 1L;
      powersB[0] = 1L;
    }

    void acceptToken(
        int localHandle,
        int utf8Bytes,
        int codePoints,
        byte[] sha256) {
      if (localHandle < 0) throw new IllegalArgumentException("negative local handle");
      if (utf8Bytes < 0) throw new IllegalArgumentException("negative UTF-8 byte length");
      if (codePoints < 0) throw new IllegalArgumentException("negative code-point length");
      Objects.requireNonNull(sha256, "sha256");
      if (sha256.length < M3Hash256.BYTES) {
        throw new IllegalArgumentException("SHA-256 input too short");
      }
      ensureTokenCapacity(tokenCount + 1);
      ensureHandleCapacity(localHandle + 1);
      utf8Offsets[tokenCount] = utf8Length;
      codePointOffsets[tokenCount] = codePointLength;

      int hashStart = Math.multiplyExact(localHandle, M3Hash256.BYTES);
      if (!tokenHashPresent[localHandle]) {
        System.arraycopy(sha256, 0, tokenHashes, hashStart, M3Hash256.BYTES);
        tokenHashPresent[localHandle] = true;
      } else {
        for (int i = 0; i < M3Hash256.BYTES; i++) {
          if (tokenHashes[hashStart + i] != sha256[i]) {
            throw new IllegalStateException("one local token handle produced two SHA-256 values");
          }
        }
      }

      long laneA = readLong(sha256, 0) ^ readLong(sha256, 16);
      long laneB = readLong(sha256, 8) ^ readLong(sha256, 24);
      prefixA[tokenCount + 1] = prefixA[tokenCount] * BASE_A + mix(laneA);
      prefixB[tokenCount + 1] = prefixB[tokenCount] * BASE_B + mix(laneB);
      powersA[tokenCount + 1] = powersA[tokenCount] * BASE_A;
      powersB[tokenCount + 1] = powersB[tokenCount] * BASE_B;

      utf8Length = Math.addExact(utf8Length, utf8Bytes);
      codePointLength = Math.addExact(codePointLength, codePoints);
      tokenCount++;
      utf8Offsets[tokenCount] = utf8Length;
      codePointOffsets[tokenCount] = codePointLength;
    }

    long utf8Length() {
      return utf8Length;
    }

    M3FileSourceFacts finish(
        M3FileAttributes before,
        M3FileAttributes after,
        M3FileByteFacts byteFacts,
        int expectedTokenCount,
        int expectedUniqueTokenCount) {
      if (expectedTokenCount != tokenCount) {
        throw new IllegalStateException(
            "token count mismatch: sourceFacts=" + tokenCount + " file=" + expectedTokenCount);
      }
      for (int handle = 0; handle < expectedUniqueTokenCount; handle++) {
        if (handle >= tokenHashPresent.length || !tokenHashPresent[handle]) {
          throw new IllegalStateException("missing token SHA-256 for local handle " + handle);
        }
      }
      return new M3FileSourceFacts(
          before,
          after,
          byteFacts,
          Arrays.copyOf(utf8Offsets, tokenCount + 1),
          Arrays.copyOf(codePointOffsets, tokenCount + 1),
          Arrays.copyOf(
              tokenHashes,
              Math.multiplyExact(expectedUniqueTokenCount, M3Hash256.BYTES)),
          Arrays.copyOf(prefixA, tokenCount + 1),
          Arrays.copyOf(prefixB, tokenCount + 1),
          Arrays.copyOf(powersA, tokenCount + 1),
          Arrays.copyOf(powersB, tokenCount + 1),
          tokenCount,
          expectedUniqueTokenCount);
    }

    private void ensureTokenCapacity(int requiredTokens) {
      int required = requiredTokens + 1;
      if (required <= utf8Offsets.length) return;
      int next = grownLength(utf8Offsets.length, required);
      utf8Offsets = Arrays.copyOf(utf8Offsets, next);
      codePointOffsets = Arrays.copyOf(codePointOffsets, next);
      prefixA = Arrays.copyOf(prefixA, next);
      prefixB = Arrays.copyOf(prefixB, next);
      powersA = Arrays.copyOf(powersA, next);
      powersB = Arrays.copyOf(powersB, next);
    }

    private void ensureHandleCapacity(int required) {
      if (required <= tokenHashPresent.length) return;
      int next = grownLength(tokenHashPresent.length, required);
      tokenHashPresent = Arrays.copyOf(tokenHashPresent, next);
      tokenHashes =
          Arrays.copyOf(tokenHashes, Math.multiplyExact(next, M3Hash256.BYTES));
    }

    private static int grownLength(int oldLength, int required) {
      int next = oldLength + (oldLength >>> 1) + 1;
      return Math.max(required, next);
    }

    private static long readLong(byte[] bytes, int offset) {
      long value = 0L;
      for (int i = 0; i < Long.BYTES; i++) {
        value = (value << 8) | Byte.toUnsignedLong(bytes[offset + i]);
      }
      return value;
    }

    private static long mix(long value) {
      long z = value + 0xd6e8feb86659fd93L;
      z = (z ^ (z >>> 32)) * 0xd6e8feb86659fd93L;
      z = (z ^ (z >>> 32)) * 0xd6e8feb86659fd93L;
      return z ^ (z >>> 32);
    }
  }
}
