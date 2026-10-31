// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3LongLookup;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/** Authenticated secondary-storage image for an already-frozen Universe token index. */
final class M3FileUniverseTokenIndexImage {
  static final String NAME = "token-index.midxut";
  private static final byte[] MAGIC = "MIDXUTI1".getBytes(StandardCharsets.US_ASCII);
  private static final int FORMAT = 1;
  private static final int DIGEST_BYTES = 32;
  private static final int MAX_FILE_COUNT = 1_000_000;
  private static final int MAX_LANE_COUNT = 100_000_000;
  private static final long MAX_IMAGE_BYTES = 512L << 20;

  private M3FileUniverseTokenIndexImage() {}

  static void write(M3FileUniverseTokenIndex index, Path target) throws IOException {
    M3FileUniverseTokenIndex checked = Objects.requireNonNull(index, "index");
    int fileCount = checked.fileCount();
    int tokenCount = checked.tokenBoundaryAt(fileCount);
    int uniqueCount = checked.uniqueResolverTokenCount();
    int postingCount = checked.postingCount();
    requireCounts(fileCount, tokenCount, uniqueCount, postingCount);
    try {
      ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
      try (DataOutputStream body = new DataOutputStream(bodyBytes)) {
        body.write(MAGIC);
        body.writeInt(FORMAT);
        body.writeInt(fileCount);
        body.writeInt(tokenCount);
        body.writeInt(uniqueCount);
        body.writeInt(postingCount);
        for (int file = 0; file < fileCount; file++) {
          body.writeLong(M3FileImage.contentFingerprint64(checked.snapshotAt(file), checked.resolver()));
        }
        for (int boundary = 0; boundary <= fileCount; boundary++) {
          body.writeInt(checked.tokenBoundaryAt(boundary));
        }
        for (int row = 0; row < uniqueCount; row++) {
          body.writeLong(checked.resolverTokenIdAt(row));
        }
        for (int boundary = 0; boundary <= uniqueCount; boundary++) {
          body.writeInt(checked.postingBoundaryAt(boundary));
        }
        for (int posting = 0; posting < postingCount; posting++) {
          body.writeInt(checked.globalTokenOrdinalAt(posting));
        }
        M3LongLookup.Geometry geometry = checked.lookupGeometry();
        body.writeInt(geometry.bucketMask());
        body.writeInt(geometry.slotMask());
        for (int displacement : geometry.displacements()) body.writeInt(displacement);
        for (int slot : geometry.slots()) body.writeInt(slot);
      }
      byte[] body = bodyBytes.toByteArray();
      ByteArrayOutputStream image = new ByteArrayOutputStream(body.length + DIGEST_BYTES);
      image.write(body);
      image.write(sha256(body));
      Files.write(Objects.requireNonNull(target, "target").toAbsolutePath().normalize(), image.toByteArray());
    } catch (RuntimeException invalid) {
      throw new IOException("cannot encode Universe token-index image", invalid);
    }
  }

  static Data read(Path source, int expectedFileCount, int expectedTokenCount) throws IOException {
    Path imagePath = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    if (Files.isSymbolicLink(imagePath)
        || !Files.isRegularFile(imagePath)
        || Files.size(imagePath) > MAX_IMAGE_BYTES) {
      throw new IOException("missing or oversized Universe token-index image");
    }
    byte[] image = Files.readAllBytes(imagePath);
    if (image.length < MAGIC.length + Integer.BYTES * 4 + DIGEST_BYTES) {
      throw new IOException("truncated Universe token-index image");
    }
    int bodyLength = image.length - DIGEST_BYTES;
    byte[] body = Arrays.copyOf(image, bodyLength);
    if (!MessageDigest.isEqual(
        Arrays.copyOfRange(image, bodyLength, image.length), sha256(body))) {
      throw new IOException("Universe token-index image checksum mismatch");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      byte[] magic = input.readNBytes(MAGIC.length);
      if (!Arrays.equals(MAGIC, magic)) throw new IOException("invalid Universe token-index magic");
      if (input.readInt() != FORMAT) throw new IOException("unsupported Universe token-index format");
      int fileCount = input.readInt();
      int tokenCount = input.readInt();
      int uniqueCount = input.readInt();
      int postingCount = input.readInt();
      requireCounts(fileCount, tokenCount, uniqueCount, postingCount);
      if (fileCount != expectedFileCount || tokenCount != expectedTokenCount) {
        throw new IOException("Universe token-index image shape does not match snapshot");
      }
      long[] fingerprints = readLongs(input, fileCount, "file fingerprints");
      int[] tokenStarts = readInts(input, fileCount + 1, "token boundaries");
      long[] resolverTokenIds = readLongs(input, uniqueCount, "resolver token IDs");
      int[] postingStarts = readInts(input, uniqueCount + 1, "posting boundaries");
      int[] globalTokenOrdinals = readInts(input, postingCount, "global token ordinals");
      int bucketMask = input.readInt();
      int slotMask = input.readInt();
      int displacementCount = maskLength(bucketMask, "bucket mask");
      int slotCount = maskLength(slotMask, "slot mask");
      int[] displacements = readInts(input, displacementCount, "lookup displacements");
      int[] slots = readInts(input, slotCount, "lookup slots");
      if (input.read() != -1) throw new IOException("trailing Universe token-index bytes");
      M3LongLookup.Geometry geometry =
          new M3LongLookup.Geometry(bucketMask, slotMask, displacements, slots);
      return new Data(
          fingerprints, tokenStarts, resolverTokenIds, postingStarts, globalTokenOrdinals, geometry);
    } catch (EOFException | RuntimeException malformed) {
      throw new IOException("malformed Universe token-index image", malformed);
    }
  }

  private static void requireCounts(int fileCount, int tokenCount, int uniqueCount, int postingCount) {
    if (fileCount < 0 || fileCount > MAX_FILE_COUNT
        || tokenCount < 0 || tokenCount > MAX_LANE_COUNT
        || uniqueCount < 0 || uniqueCount > MAX_LANE_COUNT
        || postingCount < 0 || postingCount > MAX_LANE_COUNT) {
      throw new IllegalArgumentException("invalid Universe token-index image counts");
    }
  }

  private static long[] readLongs(DataInputStream input, int count, String label) throws IOException {
    long[] values = new long[count];
    for (int index = 0; index < count; index++) values[index] = input.readLong();
    return values;
  }

  private static int[] readInts(DataInputStream input, int count, String label) throws IOException {
    int[] values = new int[count];
    for (int index = 0; index < count; index++) values[index] = input.readInt();
    return values;
  }

  private static int maskLength(int mask, String label) throws IOException {
    if (mask < 0 || mask == Integer.MAX_VALUE || Integer.bitCount(mask + 1) != 1
        || mask + 1 > MAX_LANE_COUNT) {
      throw new IOException("invalid serialized Universe lookup " + label);
    }
    return mask + 1;
  }

  private static byte[] sha256(byte[] value) throws IOException {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IOException("SHA-256 is unavailable", impossible);
    }
  }

  record Data(
      long[] fileFingerprints,
      int[] tokenStarts,
      long[] resolverTokenIds,
      int[] postingStarts,
      int[] globalTokenOrdinals,
      M3LongLookup.Geometry lookupGeometry) {
    Data {
      fileFingerprints = fileFingerprints.clone();
      tokenStarts = tokenStarts.clone();
      resolverTokenIds = resolverTokenIds.clone();
      postingStarts = postingStarts.clone();
      globalTokenOrdinals = globalTokenOrdinals.clone();
      lookupGeometry = Objects.requireNonNull(lookupGeometry, "lookupGeometry");
    }
  }
}
