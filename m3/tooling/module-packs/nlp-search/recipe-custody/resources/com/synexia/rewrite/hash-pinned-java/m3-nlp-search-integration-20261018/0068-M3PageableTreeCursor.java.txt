// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.CRC32C;

/**
 * Snapshot-bound continuation cursor for {@link M3PageableTree}.
 *
 * <p>The cursor is an exclusive child/root ordinal, not a database offset detached from topology.
 * Reusing it against another tree snapshot fails closed through the structural fingerprint.
 *
 * <p>The checksum detects accidental corruption only; it is not authentication or authorization.
 */
public record M3PageableTreeCursor(
    long snapshotFingerprint64, int parentNode, int nextOrdinal) {
  private static final int MAGIC = 0x4d495443; // MITC
  private static final int VERSION = 1;
  private static final int BYTES = 32;

  public M3PageableTreeCursor {
    if (parentNode < M3PageableTree.NO_PARENT) {
      throw new IllegalArgumentException("parent node");
    }
    if (nextOrdinal < 0) {
      throw new IllegalArgumentException("next ordinal");
    }
  }

  public boolean roots() {
    return parentNode == M3PageableTree.NO_PARENT;
  }

  public byte[] encode() {
    ByteBuffer buffer = ByteBuffer.allocate(BYTES);
    buffer.putInt(MAGIC);
    buffer.putInt(VERSION);
    buffer.putLong(snapshotFingerprint64);
    buffer.putInt(parentNode);
    buffer.putInt(nextOrdinal);
    buffer.putInt(0);
    buffer.putInt(checksum(buffer.array(), 0, BYTES - Integer.BYTES));
    return buffer.array();
  }

  public String token() {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(encode());
  }

  public static M3PageableTreeCursor decode(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    if (bytes.length != BYTES) {
      throw new IllegalArgumentException("invalid pageable-tree cursor length");
    }
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    if (buffer.getInt() != MAGIC) {
      throw new IllegalArgumentException("invalid pageable-tree cursor magic");
    }
    if (buffer.getInt() != VERSION) {
      throw new IllegalArgumentException("unsupported pageable-tree cursor version");
    }
    long fingerprint = buffer.getLong();
    int parent = buffer.getInt();
    int next = buffer.getInt();
    int reserved = buffer.getInt();
    int expected = buffer.getInt();
    if (reserved != 0) {
      throw new IllegalArgumentException("invalid pageable-tree cursor reserved field");
    }
    int actual = checksum(bytes, 0, BYTES - Integer.BYTES);
    if (actual != expected) {
      throw new IllegalArgumentException("pageable-tree cursor checksum mismatch");
    }
    return new M3PageableTreeCursor(fingerprint, parent, next);
  }

  public static M3PageableTreeCursor parse(String token) {
    Objects.requireNonNull(token, "token");
    try {
      return decode(Base64.getUrlDecoder().decode(token));
    } catch (IllegalArgumentException malformed) {
      throw new IllegalArgumentException("invalid pageable-tree cursor token", malformed);
    }
  }

  private static int checksum(byte[] bytes, int offset, int length) {
    CRC32C crc = new CRC32C();
    crc.update(bytes, offset, length);
    return (int) crc.getValue();
  }
}
