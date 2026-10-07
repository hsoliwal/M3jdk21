// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import com.synexia.indexstring.MIndexUtf16ArrayView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

/** Immutable UTF-16 CharSequence. Surrogate halves are preserved exactly, including at joins. */
public final class SharedChars extends SharedArray implements MIndexUtf16ArrayView {
  SharedChars(ArrayHandle handle, SharedSegments segments, int language, int hash) {
    super(handle, segments, language, hash);
  }
  @Override public char charAt(int index) { return (char) segments.unitAt(index); }
  @Override public SharedChars subSequence(int start, int end) {
    SharedSegments cut = segments.slice(start, end);
    return cut == segments ? this : new SharedChars(rangeHandle(start, end), cut, language, cut.hash32());
  }
  @Override public char[] copy() {
    char[] result = new char[length()]; copyTo(0, result, 0, result.length); return result;
  }
  public void copyTo(int sourceStart, char[] target, int targetStart, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length());
    Objects.checkFromIndexSize(targetStart, count, target.length);
    segments.copyTo(sourceStart, target, targetStart, count);
  }
  public boolean contentEquals(CharSequence other) {
    if (other == null || length() != other.length()) return false;
    for (int i = 0; i < length(); i++) if (charAt(i) != other.charAt(i)) return false;
    return true;
  }
  @Override public CharBuffer[] asReadOnlyCharBuffers() {
    ByteBuffer[] bytes = asReadOnlyBuffers();
    CharBuffer[] chars = new CharBuffer[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      chars[i] =
          bytes[i]
              .duplicate()
              .order(ByteOrder.BIG_ENDIAN)
              .asCharBuffer()
              .asReadOnlyBuffer();
    }
    return chars;
  }
  @Override public String toString() { return new String(copy()); }
  /** Copy into a heap/direct buffer without an intermediate payload array; advance only target. */
  public void copyTo(int sourceStart, CharBuffer target, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length());
    if (count > target.remaining()) throw new BufferOverflowException();
    if (target.isReadOnly()) throw new ReadOnlyBufferException();
    segments.copyTo(sourceStart, target, count);
  }
}
