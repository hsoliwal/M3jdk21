// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Immutable heap or mapped byte storage. Mutable callers enter through copyOf. */
public final class FrozenBytes {
  private final byte[] heap;
  private final ByteBuffer mapped; // private read-only cursor, never mutated after construction
  private final int offset;
  private final int length;
  private final int retainedBytes;
  private final int hash32;

  private FrozenBytes(byte[] heap, ByteBuffer mapped, int offset, int length,
      int retainedBytes, int hash) {
    this.heap = heap; this.mapped = mapped; this.offset = offset; this.length = length;
    this.retainedBytes = retainedBytes; this.hash32 = hash;
  }

  /** Snapshot caller-owned bytes. Mutation after this call cannot affect the admitted value. */
  public static FrozenBytes copyOf(byte[] bytes) {
    return owned(Objects.requireNonNull(bytes, "bytes").clone());
  }

  /** Internal ownership transfer: no writable alias may escape. */
  static FrozenBytes owned(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    return new FrozenBytes(bytes, null, 0, bytes.length, bytes.length, hash(bytes));
  }

  /** Only validated, published, immutable images may enter this package-private boundary. */
  static FrozenBytes mapped(ByteBuffer bytes, int retainedBytes, int verifiedHash) {
    if (!bytes.isDirect() || !bytes.isReadOnly() || retainedBytes < bytes.remaining()) {
      throw new IllegalArgumentException("expected read-only mapped storage and valid retained size");
    }
    ByteBuffer data = bytes.slice().asReadOnlyBuffer();
    return new FrozenBytes(null, data, 0, data.capacity(), retainedBytes, verifiedHash);
  }

  public int length() { return length; }
  /** Backing retained by this view; shared slices/mappings must not be summed repeatedly. */
  public int retainedBytes() { return retainedBytes; }
  public boolean isDirect() { return mapped != null; }
  /** Explicit detachment of a partial heap view or a mapped-image view. */
  public FrozenBytes compact() { return length == retainedBytes ? this : owned(copy()); }
  public int hash32() { return hash32; }
  public byte byteAt(int index) {
    int at = offset + Objects.checkIndex(index, length);
    return heap != null ? heap[at] : mapped.get(at);
  }
  public byte[] copy() {
    if (heap != null) return Arrays.copyOfRange(heap, offset, offset + length);
    byte[] out = new byte[length];
    mapped.get(offset, out);
    return out;
  }
  public FrozenBytes slice(int start, int end) {
    Objects.checkFromToIndex(start, end, length);
    if (start == 0 && end == length) return this;
    int h = 0x811c9dc5;
    for (int i = start; i < end; i++) h = (h ^ (byteAt(i) & 255)) * 0x01000193;
    return new FrozenBytes(heap, mapped, offset + start, end - start, retainedBytes, h);
  }
  public void copyTo(int sourceStart, byte[] target, int targetStart, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length);
    Objects.checkFromIndexSize(targetStart, count, target.length);
    if (heap != null) System.arraycopy(heap, offset + sourceStart, target, targetStart, count);
    else mapped.get(offset + sourceStart, target, targetStart, count);
  }
  /** Fresh read-only cursor. Mapped storage remains direct for JNI and gathering I/O. */
  public ByteBuffer asReadOnlyBuffer() {
    return heap != null ? ByteBuffer.wrap(heap, offset, length).slice().asReadOnlyBuffer()
        : mapped.slice(offset, length).asReadOnlyBuffer();
  }

  // Existing IndexStringHash.hashUtf8 FNV-1a contract, generalized here for mapped backing.
  static int hash(byte[] bytes) {
    int h = 0x811c9dc5;
    for (byte b : bytes) h = (h ^ (b & 255)) * 0x01000193;
    return h;
  }
  static int hash(ByteBuffer bytes) {
    int h = 0x811c9dc5;
    for (int i = bytes.position(); i < bytes.limit(); i++) h = (h ^ (bytes.get(i) & 255)) * 0x01000193;
    return h;
  }
}
