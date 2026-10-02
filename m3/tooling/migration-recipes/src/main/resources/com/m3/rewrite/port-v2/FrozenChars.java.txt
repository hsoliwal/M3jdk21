// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Immutable UTF-16 code units, including unpaired surrogates, on heap or mapped bytes. */
public final class FrozenChars implements CharSequence {
  private final char[] data;
  private final FrozenBytes utf16;
  private final int offset;
  private final int length;
  private final int hash32;

  private FrozenChars(char[] data, FrozenBytes utf16, int offset, int length, int hash32) {
    this.data = data; this.utf16 = utf16; this.offset = offset; this.length = length; this.hash32 = hash32;
  }
  public static FrozenChars copyOf(char[] chars) {
    return owned(Objects.requireNonNull(chars, "chars").clone());
  }
  static FrozenChars owned(char[] chars) {
    int hash = 0;
    for (char value : chars) hash = 31 * hash + value;
    return new FrozenChars(chars, null, 0, chars.length, hash);
  }
  /** The byte atom is immutable, canonical UTF-16BE code units, not a lossy charset encoding. */
  static FrozenChars mapped(FrozenBytes bytes, int verifiedHash) {
    if (!bytes.isDirect() || (bytes.length() & 1) != 0) {
      throw new IllegalArgumentException("expected even-sized mapped UTF-16 storage");
    }
    return new FrozenChars(null, bytes, 0, bytes.length() / 2, verifiedHash);
  }
  /** Retain an immutable raw UTF16BE atom; preserves every code unit, not charset decoding. */
  public static FrozenChars fromUtf16Bytes(FrozenBytes bytes) {
    return Objects.requireNonNull(bytes, "bytes").utf16View();
  }
  static FrozenChars projectUtf16Bytes(FrozenBytes bytes) {
    Objects.requireNonNull(bytes, "bytes");
    return new FrozenChars(null, bytes, 0, bytes.length() / 2, hashUtf16(bytes.asReadOnlyBuffer()));
  }
  /** Precomputed String-compatible hash, retained for existing join/interner callers. */
  public int hash32() { return hash32; }
  public boolean isDirect() { return utf16 != null && utf16.isDirect(); }
  /** Backing identity, independent of text/hash equality and range coordinates. */
  public boolean sharesStorageWith(FrozenChars other) {
    Objects.requireNonNull(other, "other");
    return data == other.data && utf16 == other.utf16;
  }
  /** Only truly contiguous ranges within the same immutable backing may coalesce. */
  public boolean adjacentTo(FrozenChars next) {
    return sharesStorageWith(next) && (long)offset + length == next.offset;
  }
  public FrozenChars coalesce(FrozenChars next) {
    if (!adjacentTo(next)) throw new IllegalArgumentException("non-adjacent owners/ranges");
    int combinedLength = Math.addExact(length, next.length);
    int combinedHash = hash32;
    for (int i = 0; i < next.length; i++) combinedHash = 31 * combinedHash + next.charAt(i);
    return new FrozenChars(data, utf16, offset, combinedLength, combinedHash);
  }
  /** Entire owner retention; slices may keep much more storage than their logical length. */
  public long retainedBytes() { return data != null ? 2L * data.length : utf16.retainedBytes(); }
  @Override public int length() { return length; }
  @Override public char charAt(int index) {
    int at = offset + Objects.checkIndex(index, length);
    return data != null ? data[at]
        : (char) (((utf16.byteAt(2 * at) & 255) << 8) | (utf16.byteAt(2 * at + 1) & 255));
  }
  @Override public FrozenChars subSequence(int start, int end) {
    Objects.checkFromToIndex(start, end, length);
    if (start == 0 && end == length) return this;
    int hash = 0;
    for (int i = start; i < end; i++) hash = 31 * hash + charAt(i);
    return new FrozenChars(data, utf16, offset + start, end - start, hash);
  }
  public char[] copy() {
    if (data != null) return Arrays.copyOfRange(data, offset, offset + length);
    char[] result = new char[length];
    asReadOnlyBuffer().get(result);
    return result;
  }
  public void copyTo(int sourceStart, char[] target, int targetStart, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length);
    Objects.checkFromIndexSize(targetStart, count, target.length);
    if (data != null) System.arraycopy(data, offset + sourceStart, target, targetStart, count);
    else asReadOnlyBuffer().get(sourceStart, target, targetStart, count);
  }
  public CharBuffer asReadOnlyBuffer() {
    return data != null ? CharBuffer.wrap(data, offset, length).slice().asReadOnlyBuffer()
        : utf16.asReadOnlyBuffer().slice(2 * offset, 2 * length).order(ByteOrder.BIG_ENDIAN).asCharBuffer().asReadOnlyBuffer();
  }
  /** JNI descriptor in BYTES, UTF-16BE, position zero. Never allocates a payload. */
  public ByteBuffer directUtf16Bytes() {
    if (!isDirect()) throw new IllegalStateException("heap chars need explicit encoding/copy");
    return utf16.asReadOnlyBuffer().slice(2 * offset, 2 * length).asReadOnlyBuffer();
  }
  static int hashUtf16(ByteBuffer bytes) {
    if ((bytes.remaining() & 1) != 0) throw new IllegalArgumentException("odd UTF-16 byte length");
    int hash = 0;
    for (int i = bytes.position(); i < bytes.limit(); i += 2) {
      int unit = ((bytes.get(i) & 255) << 8) | (bytes.get(i + 1) & 255);
      hash = 31 * hash + unit;
    }
    return hash;
  }
  @Override public String toString() {
    return data != null ? new String(data, offset, length) : new String(copy());
  }
}
