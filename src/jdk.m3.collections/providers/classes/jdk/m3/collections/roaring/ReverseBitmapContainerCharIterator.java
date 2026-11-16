/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package jdk.m3.collections.roaring;

import static java.lang.Long.bitCount;
import static java.lang.Long.numberOfTrailingZeros;

import jdk.m3.collections.roaring.buffer.MappeableBitmapContainer;
import jdk.m3.collections.roaring.buffer.MappeableContainer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Iterator;

final class ReverseBitmapContainerCharIterator implements PeekableCharIterator {

  long word;
  int position;

  long[] bitmap;

  ReverseBitmapContainerCharIterator() {}

  ReverseBitmapContainerCharIterator(long[] bitmap) {
    wrap(bitmap);
  }

  @Override
  public PeekableCharIterator clone() {
    try {
      return (PeekableCharIterator) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public boolean hasNext() {
    return position >= 0;
  }

  @Override
  public char next() {
    int shift = Long.numberOfLeadingZeros(word) + 1;
    char answer = (char) ((position + 1) * 64 - shift);
    word &= (0xFFFFFFFFFFFFFFFEL >>> shift);
    while (word == 0) {
      --position;
      if (position < 0) {
        break;
      }
      word = bitmap[position];
    }
    return answer;
  }

  @Override
  public int nextAsInt() {
    return next();
  }

  @Override
  public void advanceIfNeeded(char maxval) {
    if (maxval < (position + 1) * 64) {
      if (maxval < position * 64) {
        position = maxval / 64;
      }
      long currentWord = bitmap[position];
      currentWord &= ~0L >>> (63 - (maxval & 63));
      while (currentWord == 0 && position > 0) {
        position--;
        currentWord = bitmap[position];
      }
      word = currentWord;
      if (currentWord == 0) {
        // no value at or below maxval in this container
        position = -1;
      }
    }
  }

  @Override
  public char peekNext() {
    int shift = Long.numberOfLeadingZeros(word) + 1;
    return (char) ((position + 1) * 64 - shift);
  }

  @Override
  public void remove() {
    // TODO: implement
    throw new RuntimeException("unsupported operation: remove");
  }

  void wrap(long[] b) {
    bitmap = b;
    for (position = bitmap.length - 1; position >= 0; --position) {
      if ((word = bitmap[position]) != 0) {
        break;
      }
    }
  }
}
