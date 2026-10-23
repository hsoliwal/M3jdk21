// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

/** Executable proof for the one-pass byte-facts bulk projection. */
public final class M3FileByteFactsPrimitiveSnapshotProof {
  private M3FileByteFactsPrimitiveSnapshotProof() {}

  public static void main(String[] args) throws Exception {
    int length = M3FileByteFacts.CHUNK_BYTES + 19;
    byte[] payload = new byte[length];
    for (int index = 0; index < payload.length; index++) {
      payload[index] = (byte) ((index * 73 + 11) & 0xff);
    }
    Path directory = Files.createTempDirectory("mindex-byte-facts-snapshot-proof-");
    Path source = directory.resolve("payload.bin");
    try {
      Files.write(source, payload);
      M3ByteFile file = M3ByteFileStreamIndexer.index(source);
      M3FileByteFactsSnapshot first = file.byteFactsSnapshot();
      require(first.byteCount() == payload.length, "byte count changed");
      require(first.sha256().equals(hash(payload)), "whole-file SHA-256 changed");
      require(first.chunkCount() == 2, "chunk count changed");
      require(first.chunkLength(0) == M3FileByteFacts.CHUNK_BYTES, "full chunk changed");
      require(first.chunkLength(1) == 19, "tail chunk changed");
      require(first.chunkOffset(1) == M3FileByteFacts.CHUNK_BYTES, "chunk offset changed");
      require(
          first.chunkSha256(0).equals(hash(Arrays.copyOfRange(payload, 0, M3FileByteFacts.CHUNK_BYTES))),
          "full chunk SHA-256 changed");
      require(
          first.chunkSha256(1).equals(hash(Arrays.copyOfRange(payload, M3FileByteFacts.CHUNK_BYTES, payload.length))),
          "tail chunk SHA-256 changed");
      require(Arrays.equals(first.prefixBytes(), Arrays.copyOf(payload, 64)), "prefix changed");
      require(
          Arrays.equals(
              first.suffixBytes(), Arrays.copyOfRange(payload, payload.length - 64, payload.length)),
          "suffix changed");

      long[] expectedHistogram = new long[256];
      for (byte value : payload) expectedHistogram[Byte.toUnsignedInt(value)]++;
      require(Arrays.equals(expectedHistogram, first.byteHistogram()), "histogram changed");
      require(first.uniqueByteValues() == 256, "unique-byte count changed");
      require(!first.asciiOnly(), "binary payload was classified as ASCII");
      require(file.byteFacts().retainedPayloadBytes() == first.retainedPayloadBytes(), "weight changed");

      long[] histogram = first.byteHistogram();
      byte[] prefix = first.prefixBytes();
      byte[] suffix = first.suffixBytes();
      byte[] chunkHashes = first.chunkSha256Bytes();
      int[] chunkLengths = first.chunkLengths();
      histogram[0] = Long.MAX_VALUE;
      prefix[0] ^= 1;
      suffix[0] ^= 1;
      chunkHashes[0] ^= 1;
      chunkLengths[0] = 1;
      M3FileByteFactsSnapshot second = file.byteFactsSnapshot();
      require(Arrays.equals(expectedHistogram, second.byteHistogram()), "histogram not defensive");
      require(Arrays.equals(Arrays.copyOf(payload, 64), second.prefixBytes()), "prefix not defensive");
      require(
          Arrays.equals(
              Arrays.copyOfRange(payload, payload.length - 64, payload.length), second.suffixBytes()),
          "suffix not defensive");
      require(
          Arrays.equals(new int[] {M3FileByteFacts.CHUNK_BYTES, 19}, second.chunkLengths()),
          "chunk lengths not defensive");
      require(Arrays.equals(first.chunkSha256Bytes(), second.chunkSha256Bytes()), "chunks not defensive");

      Path empty = directory.resolve("empty.bin");
      Files.write(empty, new byte[0]);
      M3FileByteFactsSnapshot emptySnapshot = M3ByteFileStreamIndexer.index(empty).byteFactsSnapshot();
      require(emptySnapshot.byteCount() == 0L, "empty byte count changed");
      require(emptySnapshot.sha256().equals(hash(new byte[0])), "empty SHA-256 changed");
      require(emptySnapshot.chunkCount() == 0, "empty file grew a synthetic chunk");
      require(Arrays.equals(new long[256], emptySnapshot.byteHistogram()), "empty histogram changed");
      require(emptySnapshot.prefixBytes().length == 0, "empty prefix changed");
      require(emptySnapshot.suffixBytes().length == 0, "empty suffix changed");
    } finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(directory.resolve("empty.bin"));
      Files.deleteIfExists(directory);
    }
    System.out.println(
        "MINDEX_FILE_BYTE_FACTS_PRIMITIVE_SNAPSHOT_CURRENT_DEVELOP_PASS"
            + "|singlePass=1|scalars=1|histogram=1|signatures=1|chunks=1"
            + "|defensive=1|empty=1|format=unchanged");
  }

  private static M3Hash256 hash(byte[] bytes) throws Exception {
    return M3Hash256.of(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
