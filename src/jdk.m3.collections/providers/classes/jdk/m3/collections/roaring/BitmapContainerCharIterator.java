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

class BitmapContainerCharIterator implements PeekableCharIterator {

  long w;
  int x;

  long[] bitmap;

  BitmapContainerCharIterator() {}

  BitmapContainerCharIterator(long[] p) {
    wrap(p);
  }

  @Override
  public PeekableCharIterator clone() {
    try {
      return (PeekableCharIterator) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // will not happen
    }
  }

  @Override
  public boolean hasNext() {
    return x < bitmap.length;
  }

  @Override
  public char next() {
    char answer = (char) (x * 64 + numberOfTrailingZeros(w));
    w &= (w - 1);
    while (w == 0) {
      ++x;
      if (x == bitmap.length) {
        break;
      }
      w = bitmap[x];
    }
    return answer;
  }

  @Override
  public int nextAsInt() {
    return (next());
  }

  @Override
  public void remove() {
    // TODO: implement
    throw new RuntimeException("unsupported operation: remove");
  }

  public void wrap(long[] b) {
    bitmap = b;
    for (x = 0; x < bitmap.length; ++x) {
      if ((w = bitmap[x]) != 0) {
        break;
      }
    }
  }

  @Override
  public void advanceIfNeeded(char minval) {
    if (!hasNext()) {
      return;
    }
    if (minval >= x * 64) {
      if (minval >= (x + 1) * 64) {
        x = minval / 64;
        w = bitmap[x];
      }
      w &= ~0L << (minval & 63);
      while (w == 0) {
        x++;
        if (!hasNext()) {
          return;
        }
        w = bitmap[x];
      }
    }
  }

  @Override
  public char peekNext() {
    return (char) (x * 64 + numberOfTrailingZeros(w));
  }
}
