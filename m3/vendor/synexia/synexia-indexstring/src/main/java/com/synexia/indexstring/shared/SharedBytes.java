// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import com.synexia.indexstring.MIndexByteArrayView;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

/** Immutable, OS-backed logical byte array; copy() is an explicit mutable-array escape. */
public final class SharedBytes extends SharedArray implements MIndexByteArrayView {
  SharedBytes(ArrayHandle handle, SharedSegments segments, int hash) { super(handle, segments, 0, hash); }
  @Override public byte byteAt(int index) { return (byte) segments.unitAt(index); }
  public SharedBytes slice(int start, int end) {
    SharedSegments cut = segments.slice(start, end);
    return cut == segments ? this : new SharedBytes(rangeHandle(start, end), cut, cut.hash32());
  }
  @Override public byte[] copy() {
    byte[] result = new byte[length()]; copyTo(0, result, 0, result.length); return result;
  }
  public void copyTo(int sourceStart, byte[] target, int targetStart, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length());
    Objects.checkFromIndexSize(targetStart, count, target.length);
    segments.copyTo(sourceStart, target, targetStart, count);
  }
  /** Copy into a heap/direct buffer without an intermediate payload array; advance only target. */
  public void copyTo(int sourceStart, ByteBuffer target, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length());
    if (count > target.remaining()) throw new BufferOverflowException();
    if (target.isReadOnly()) throw new ReadOnlyBufferException();
    segments.copyTo(sourceStart, target, count);
  }
}
