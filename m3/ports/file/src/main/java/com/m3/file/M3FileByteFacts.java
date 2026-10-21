// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/** Immutable facts derived directly from one raw file byte stream. */
public final class M3FileByteFacts {
  static final int SIGNATURE_BYTES = 64;

  /** Fixed raw-byte chunk geometry shared with the existing FileIndex streaming donor. */
  public static final int CHUNK_BYTES = 512 * 1024;

  private final long byteCount;
  private final M3Hash256 sha256;
  private final long[] histogram;
  private final byte[] prefix;
  private final byte[] suffix;
  private final int uniqueByteValues;
  private final double shannonEntropyBitsPerByte;
  private final byte[] chunkSha256;
  private final int[] chunkLengths;

  private M3FileByteFacts(
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
    this.histogram = histogram.clone();
    this.prefix = prefix.clone();
    this.suffix = suffix.clone();
    this.uniqueByteValues = uniqueByteValues;
    this.shannonEntropyBitsPerByte = shannonEntropyBitsPerByte;
    this.chunkSha256 = chunkSha256.clone();
    this.chunkLengths = chunkLengths.clone();
  }

  /** Validate and restore the existing format-1 sidecar's byte facts. */
  static M3FileByteFacts restore(
      long byteCount,
      M3Hash256 sha256,
      long[] histogram,
      byte[] prefix,
      byte[] suffix,
      int uniqueByteValues,
      double shannonEntropyBitsPerByte,
      byte[] chunkSha256,
      int[] chunkLengths) {
    if (byteCount < 0L
        || histogram == null
        || histogram.length != 256
        || prefix == null
        || prefix.length != Math.min(byteCount, SIGNATURE_BYTES)
        || suffix == null
        || suffix.length != Math.min(byteCount, SIGNATURE_BYTES)
        || uniqueByteValues < 0
        || uniqueByteValues > 256
        || !Double.isFinite(shannonEntropyBitsPerByte)
        || shannonEntropyBitsPerByte < 0.0d
        || chunkSha256 == null
        || chunkLengths == null
        || chunkSha256.length != Math.multiplyExact(chunkLengths.length, M3Hash256.BYTES)) {
      throw new IllegalArgumentException("invalid serialized M3File byte facts");
    }
    long total = 0L;
    int unique = 0;
    double entropy = 0.0d;
    for (long frequency : histogram) {
      if (frequency < 0L) throw new IllegalArgumentException("negative byte frequency");
      total = Math.addExact(total, frequency);
      if (frequency != 0L) {
        unique++;
        double probability = (double) frequency / (double) Math.max(1L, byteCount);
        entropy -= probability * (Math.log(probability) / Math.log(2.0d));
      }
    }
    // Math.log may differ by a few ulps across JVM implementations. The histogram is exact;
    // admit only insignificant rounding drift in its derived entropy, preserving exact zero.
    double tolerance =
        Math.max(1.0e-12d, 1024.0d * Math.ulp(Math.max(1.0d, entropy)));
    boolean entropyMatches = entropy == 0.0d
        ? Double.doubleToLongBits(shannonEntropyBitsPerByte) == Double.doubleToLongBits(0.0d)
        : Math.abs(entropy - shannonEntropyBitsPerByte) <= tolerance;
    if (total != byteCount || unique != uniqueByteValues || !entropyMatches) {
      throw new IllegalArgumentException("serialized M3File byte facts statistics mismatch");
    }
    long chunkTotal = 0L;
    for (int chunk = 0; chunk < chunkLengths.length; chunk++) {
      int length = chunkLengths[chunk];
      if (length < 1 || length > CHUNK_BYTES
          || (chunk < chunkLengths.length - 1 && length != CHUNK_BYTES)) {
        throw new IllegalArgumentException("invalid serialized M3File chunk length");
      }
      chunkTotal = Math.addExact(chunkTotal, length);
    }
    if (chunkTotal != byteCount) {
      throw new IllegalArgumentException("serialized M3File chunk total mismatch");
    }
    return new M3FileByteFacts(
        byteCount,
        Objects.requireNonNull(sha256, "sha256"),
        histogram,
        prefix,
        suffix,
        uniqueByteValues,
        shannonEntropyBitsPerByte,
        chunkSha256,
        chunkLengths);
  }

  public long byteCount() {
    return byteCount;
  }

  public M3Hash256 sha256() {
    return sha256;
  }

  /** Return one defensive bulk projection of every fact computed during the source traversal. */
  public M3FileByteFactsSnapshot snapshot() {
    return new M3FileByteFactsSnapshot(
        byteCount,
        sha256,
        histogram,
        prefix,
        suffix,
        uniqueByteValues,
        shannonEntropyBitsPerByte,
        chunkSha256,
        chunkLengths);
  }

  public long byteFrequency(int unsignedByte) {
    if (unsignedByte < 0 || unsignedByte > 255) {
      throw new IllegalArgumentException("unsigned byte must be in [0,255]");
    }
    return histogram[unsignedByte];
  }

  public long[] byteHistogram() {
    return histogram.clone();
  }

  public byte[] prefixBytes() {
    return prefix.clone();
  }

  public byte[] suffixBytes() {
    return suffix.clone();
  }

  public int uniqueByteValues() {
    return uniqueByteValues;
  }

  public double shannonEntropyBitsPerByte() {
    return shannonEntropyBitsPerByte;
  }

  public boolean asciiOnly() {
    for (int value = 128; value < histogram.length; value++) {
      if (histogram[value] != 0L) return false;
    }
    return true;
  }

  public int chunkCount() {
    return chunkLengths.length;
  }

  public long chunkOffset(int chunkOrdinal) {
    Objects.checkIndex(chunkOrdinal, chunkLengths.length);
    return Math.multiplyExact((long) chunkOrdinal, CHUNK_BYTES);
  }

  public int chunkLength(int chunkOrdinal) {
    return chunkLengths[Objects.checkIndex(chunkOrdinal, chunkLengths.length)];
  }

  public M3Hash256 chunkSha256(int chunkOrdinal) {
    int ordinal = Objects.checkIndex(chunkOrdinal, chunkLengths.length);
    int start = Math.multiplyExact(ordinal, M3Hash256.BYTES);
    return M3Hash256.of(
        Arrays.copyOfRange(chunkSha256, start, start + M3Hash256.BYTES));
  }

  public long retainedPayloadBytes() {
    return M3Hash256.BYTES
        + Long.BYTES * (long) histogram.length
        + prefix.length
        + suffix.length
        + chunkSha256.length
        + Integer.BYTES * (long) chunkLengths.length;
  }

  static final class Builder {
    private final MessageDigest sha256 = newSha256();
    private final MessageDigest chunkDigest = newSha256();
    private final long[] histogram = new long[256];
    private final byte[] prefix = new byte[SIGNATURE_BYTES];
    private final byte[] suffixRing = new byte[SIGNATURE_BYTES];
    private byte[] chunkHashes = new byte[8 * M3Hash256.BYTES];
    private int[] chunkLengths = new int[8];
    private long count;
    private int prefixLength;
    private int chunkLength;
    private int chunkCount;
    private boolean finished;

    void accept(int value) {
      requireOpen();
      if (value < 0 || value > 255) throw new IllegalArgumentException("not an unsigned byte");
      byte unit = (byte) value;
      sha256.update(unit);
      chunkDigest.update(unit);
      record(value);
      chunkLength++;
      if (chunkLength == CHUNK_BYTES) finishChunk();
    }

    void accept(byte[] bytes, int offset, int length) {
      requireOpen();
      Objects.requireNonNull(bytes, "bytes");
      Objects.checkFromIndexSize(offset, length, bytes.length);
      sha256.update(bytes, offset, length);

      for (int i = 0; i < length; i++) {
        record(Byte.toUnsignedInt(bytes[offset + i]));
      }

      int cursor = offset;
      int remaining = length;
      while (remaining > 0) {
        int admitted = Math.min(remaining, CHUNK_BYTES - chunkLength);
        chunkDigest.update(bytes, cursor, admitted);
        chunkLength += admitted;
        cursor += admitted;
        remaining -= admitted;
        if (chunkLength == CHUNK_BYTES) finishChunk();
      }
    }

    long byteCount() {
      return count;
    }

    M3FileByteFacts finish() {
      requireOpen();
      finished = true;
      if (chunkLength != 0) finishChunk();
      byte[] digest = sha256.digest();
      int unique = 0;
      double entropy = 0.0d;
      if (count != 0L) {
        for (long frequency : histogram) {
          if (frequency == 0L) continue;
          unique++;
          double probability = (double) frequency / (double) count;
          entropy -= probability * (Math.log(probability) / Math.log(2.0d));
        }
      }
      int suffixLength = (int) Math.min(count, SIGNATURE_BYTES);
      byte[] suffix = new byte[suffixLength];
      long firstOrdinal = count - suffixLength;
      for (int i = 0; i < suffixLength; i++) {
        suffix[i] = suffixRing[(int) ((firstOrdinal + i) % SIGNATURE_BYTES)];
      }
      return new M3FileByteFacts(
          count,
          M3Hash256.of(digest),
          histogram,
          Arrays.copyOf(prefix, prefixLength),
          suffix,
          unique,
          entropy,
          Arrays.copyOf(chunkHashes, Math.multiplyExact(chunkCount, M3Hash256.BYTES)),
          Arrays.copyOf(chunkLengths, chunkCount));
    }

    private void record(int value) {
      histogram[value]++;
      if (prefixLength < SIGNATURE_BYTES) prefix[prefixLength++] = (byte) value;
      suffixRing[(int) (count % SIGNATURE_BYTES)] = (byte) value;
      count = Math.incrementExact(count);
    }

    private void finishChunk() {
      ensureChunkCapacity(chunkCount + 1);
      byte[] digest = chunkDigest.digest();
      System.arraycopy(
          digest,
          0,
          chunkHashes,
          Math.multiplyExact(chunkCount, M3Hash256.BYTES),
          M3Hash256.BYTES);
      chunkLengths[chunkCount] = chunkLength;
      chunkCount++;
      chunkLength = 0;
    }

    private void ensureChunkCapacity(int required) {
      if (required <= chunkLengths.length) return;
      int next = Math.max(required, chunkLengths.length + (chunkLengths.length >>> 1) + 1);
      chunkLengths = Arrays.copyOf(chunkLengths, next);
      chunkHashes =
          Arrays.copyOf(chunkHashes, Math.multiplyExact(next, M3Hash256.BYTES));
    }

    private void requireOpen() {
      if (finished) throw new IllegalStateException("byte facts already finished");
    }

    private static MessageDigest newSha256() {
      try {
        return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException impossible) {
        throw new ExceptionInInitializerError(impossible);
      }
    }
  }
}
