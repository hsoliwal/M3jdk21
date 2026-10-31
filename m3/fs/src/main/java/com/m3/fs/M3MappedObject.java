// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * Verified, read-only, relocatable primitive views over one existing M3 content object.
 *
 * <p>Offsets address bytes inside {@link #reference()}, not process pointers or canonical value
 * identities. No format, string normalization, schema or dictionary is inferred. The caller's
 * existing codec supplies offsets, counts, byte order and the meaning of stored IDs.</p>
 *
 * <p>The backing store must remain immutable for the lifetime of this object and all its views.
 * Admission checks integrity, not authenticity, and cannot prevent subsequent external writes.
 * Views keep their mapping alive; Java 21's non-preview mapping API has no deterministic unmap.
 * The byte budget is per mapping, not a bound on all mappings or process resident memory.</p>
 */
public final class M3MappedObject {
  private static final int KIND_COUNT = M3ObjectKind.values().length;
  private final byte[] id;
  private final int kind;
  private final ByteBuffer data;

  private M3MappedObject(byte[] id, int kind, ByteBuffer data) {
    this.id = id;
    this.kind = kind;
    this.data = data.asReadOnlyBuffer();
  }

  static M3MappedObject open(Path path, M3ObjectRef ref, int maxBytes) throws IOException {
    Objects.requireNonNull(ref, "ref");
    return openColumns(path, ref.id().bytes(), 0, ref.size(), ref.kind().ordinal(), maxBytes);
  }

  static M3MappedObject openColumns(Path path, byte[] ids, int offset, long size,
      int kind, int maxBytes) throws IOException {
    Objects.requireNonNull(path, "path");
    validateColumns(ids, offset, size, kind);
    if (maxBytes < 0) throw new IllegalArgumentException("negative mapping budget");
    if (size > maxBytes) throw new IOException("M3 object exceeds mapping budget");
    byte[] id = Arrays.copyOfRange(ids, offset, offset + 32);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("M3 mapped object must be a non-symlink regular file");
    }
    try (FileChannel channel =
        FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      if (channel.size() != size) throw new IOException("M3 mapped object size mismatch");
      ByteBuffer mapped = size == 0
          ? ByteBuffer.allocate(0).asReadOnlyBuffer()
          : channel.map(FileChannel.MapMode.READ_ONLY, 0L, size);
      MessageDigest digest = M3ContentId.newDigest();
      digest.update(mapped.asReadOnlyBuffer());
      if (channel.size() != size || !MessageDigest.isEqual(digest.digest(), id)) {
        throw new IOException("M3 mapped object integrity mismatch");
      }
      return new M3MappedObject(id, kind, mapped);
    }
  }

  /** @return the existing full content identity, role and size, unchanged */
  public M3ObjectRef reference() {
    return new M3ObjectRef(M3ContentId.of(id), size(), M3ObjectKind.values()[kind]);
  }

  boolean matchesReference(byte[] ids, int offset, long size, int kind) {
    validateColumns(ids, offset, size, kind);
    return size == size() && kind == this.kind
        && Arrays.equals(id, 0, 32, ids, offset, offset + 32);
  }

  private static void validateColumns(byte[] ids, int offset, long size, int kind) {
    Objects.checkFromIndexSize(offset, 32, Objects.requireNonNull(ids, "ids").length);
    if (size < 0 || kind < 0 || kind >= KIND_COUNT) {
      throw new IllegalArgumentException("invalid mapped object columns");
    }
  }

  /** @return byte count of this admitted mapping */
  public int size() {
    return data.capacity();
  }

  /**
   * Reads one byte by its relocatable offset.
   * @param offset offset within this object
   * @return stored byte
   */
  public byte byteAt(int offset) {
    Objects.checkIndex(offset, size());
    return data.get(offset);
  }

  /**
   * Returns an independent read-only byte view with position zero and capacity equal to length.
   * @param offset byte offset within this object
   * @param length number of bytes
   * @return a view, not a payload copy
   */
  public ByteBuffer bytes(int offset, int length) {
    Objects.checkFromIndexSize(offset, length, size());
    return data.slice(offset, length).asReadOnlyBuffer();
  }

  /**
   * Returns a packed UTF-16 code-unit lane without copying its mapped bytes.
   */
  public CharBuffer chars(int offset, int count, ByteOrder order) {
    return lane(offset, count, Character.BYTES, order).asCharBuffer();
  }

  /** Returns a packed signed-short lane without copying mapped bytes. */
  public ShortBuffer shorts(int offset, int count, ByteOrder order) {
    return lane(offset, count, Short.BYTES, order).asShortBuffer();
  }

  /**
   * Returns a packed int lane without constructing one object per element.
   * @param offset byte offset, which need not be naturally aligned
   * @param count number of int elements
   * @param order existing codec's explicit byte order
   * @return independent read-only int view
   */
  public IntBuffer ints(int offset, int count, ByteOrder order) {
    return lane(offset, count, Integer.BYTES, order).asIntBuffer();
  }

  /**
   * Returns a packed long lane without exposing any native pointer.
   * @param offset byte offset, which need not be naturally aligned
   * @param count number of long elements
   * @param order existing codec's explicit byte order
   * @return independent read-only long view
   */
  public LongBuffer longs(int offset, int count, ByteOrder order) {
    return lane(offset, count, Long.BYTES, order).asLongBuffer();
  }

  /**
   * Compares exact bytes, including across different objects; equal IDs alone are not the test.
   * @param offset byte offset in this object
   * @param other second mapped object
   * @param otherOffset byte offset in the second object
   * @param length byte count
   * @return true only when the two checked byte spans match
   */
  public boolean contentEquals(int offset, M3MappedObject other, int otherOffset, int length) {
    Objects.requireNonNull(other, "other");
    return bytes(offset, length).mismatch(other.bytes(otherOffset, length)) == -1;
  }

  private ByteBuffer lane(int offset, int count, int width, ByteOrder order) {
    Objects.requireNonNull(order, "order");
    long length = (long) count * width;
    Objects.checkFromIndexSize((long) offset, length, (long) size());
    return bytes(offset, (int) length).order(order);
  }
}
