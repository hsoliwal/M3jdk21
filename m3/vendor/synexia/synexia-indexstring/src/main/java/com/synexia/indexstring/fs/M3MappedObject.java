// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

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
  private final M3ObjectRef reference;
  private final ByteBuffer data;

  private M3MappedObject(M3ObjectRef reference, ByteBuffer data) {
    this.reference = reference;
    this.data = data.asReadOnlyBuffer();
  }

  static M3MappedObject open(Path path, M3ObjectRef ref, int maxBytes) throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(ref, "ref");
    if (maxBytes < 0) throw new IllegalArgumentException("negative mapping budget");
    if (ref.size() > maxBytes) throw new IOException("M3 object exceeds mapping budget");
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("M3 mapped object must be a non-symlink regular file");
    }
    try (FileChannel channel =
        FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      if (channel.size() != ref.size()) throw new IOException("M3 mapped object size mismatch");
      int size = Math.toIntExact(ref.size());
      ByteBuffer mapped = size == 0
          ? ByteBuffer.allocate(0).asReadOnlyBuffer()
          : channel.map(FileChannel.MapMode.READ_ONLY, 0L, size);
      MessageDigest digest = M3ContentId.newDigest();
      digest.update(mapped.asReadOnlyBuffer());
      if (channel.size() != size || !M3ContentId.of(digest.digest()).equals(ref.id())) {
        throw new IOException("M3 mapped object integrity mismatch");
      }
      return new M3MappedObject(ref, mapped);
    }
  }

  /** @return the existing full content identity, role and size, unchanged */
  public M3ObjectRef reference() {
    return reference;
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
