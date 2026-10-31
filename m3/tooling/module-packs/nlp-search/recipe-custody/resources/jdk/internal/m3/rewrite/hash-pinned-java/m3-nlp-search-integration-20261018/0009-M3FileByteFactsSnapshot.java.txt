// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.util.Arrays;
import java.util.Objects;

/**
 * One defensive bulk view of every byte-derived fact retained for a physical file.
 *
 * <p>This is a projection of an already-finished {@link M3FileByteFacts} owner. It never reads
 * the source again and never changes the source-facts sidecar format.</p>
 */
public final class M3FileByteFactsSnapshot {
  private final long byteCount;
  private final M3Hash256 sha256;
  private final long[] histogram;
  private final byte[] prefix;
  private final byte[] suffix;
  private final int uniqueByteValues;
  private final double shannonEntropyBitsPerByte;
  private final byte[] chunkSha256;
  private final int[] chunkLengths;

  M3FileByteFactsSnapshot(
      long byteCount,
      M3Hash256 sha256,
      long[] histogram,
      byte[] prefix,
      byte[] suffix,
      int uniqueByteValues,
      double shannonEntropyBitsPerByte,
      byte[] chunkSha256,
      int[] chunkLengths) {
    this.byteCount = byteCount;
    this.sha256 = Objects.requireNonNull(sha256, "sha256");
    this.histogram = Objects.requireNonNull(histogram, "histogram").clone();
    this.prefix = Objects.requireNonNull(prefix, "prefix").clone();
    this.suffix = Objects.requireNonNull(suffix, "suffix").clone();
    this.uniqueByteValues = uniqueByteValues;
    this.shannonEntropyBitsPerByte = shannonEntropyBitsPerByte;
    this.chunkSha256 = Objects.requireNonNull(chunkSha256, "chunkSha256").clone();
    this.chunkLengths = Objects.requireNonNull(chunkLengths, "chunkLengths").clone();
  }

  public long byteCount() { return byteCount; }

  public M3Hash256 sha256() { return sha256; }

  public long byteFrequency(int unsignedByte) {
    if (unsignedByte < 0 || unsignedByte > 255) {
      throw new IllegalArgumentException("unsigned byte must be in [0,255]");
    }
    return histogram[unsignedByte];
  }

  public long[] byteHistogram() { return histogram.clone(); }

  public byte[] prefixBytes() { return prefix.clone(); }

  public byte[] suffixBytes() { return suffix.clone(); }

  public int uniqueByteValues() { return uniqueByteValues; }

  public double shannonEntropyBitsPerByte() { return shannonEntropyBitsPerByte; }

  public boolean asciiOnly() {
    for (int value = 128; value < histogram.length; value++) {
      if (histogram[value] != 0L) return false;
    }
    return true;
  }

  public int chunkCount() { return chunkLengths.length; }

  public long chunkOffset(int chunkOrdinal) {
    Objects.checkIndex(chunkOrdinal, chunkLengths.length);
    return Math.multiplyExact((long) chunkOrdinal, M3FileByteFacts.CHUNK_BYTES);
  }

  public int chunkLength(int chunkOrdinal) {
    return chunkLengths[Objects.checkIndex(chunkOrdinal, chunkLengths.length)];
  }

  public M3Hash256 chunkSha256(int chunkOrdinal) {
    int ordinal = Objects.checkIndex(chunkOrdinal, chunkLengths.length);
    int start = Math.multiplyExact(ordinal, M3Hash256.BYTES);
    return M3Hash256.of(Arrays.copyOfRange(chunkSha256, start, start + M3Hash256.BYTES));
  }

  public byte[] chunkSha256Bytes() { return chunkSha256.clone(); }

  public int[] chunkLengths() { return chunkLengths.clone(); }

  public long retainedPayloadBytes() {
    return M3Hash256.BYTES
        + Long.BYTES * (long) histogram.length
        + prefix.length
        + suffix.length
        + chunkSha256.length
        + Integer.BYTES * (long) chunkLengths.length;
  }
}
