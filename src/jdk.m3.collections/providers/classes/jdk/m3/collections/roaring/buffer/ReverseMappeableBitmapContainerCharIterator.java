/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package jdk.m3.collections.roaring.buffer;

import static java.lang.Long.numberOfLeadingZeros;
import static java.lang.Long.numberOfTrailingZeros;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import jdk.m3.collections.roaring.BitSetUtil;
import jdk.m3.collections.roaring.BitmapContainer;
import jdk.m3.collections.roaring.CharIterator;
import jdk.m3.collections.roaring.Container;
import jdk.m3.collections.roaring.ContainerBatchIterator;
import jdk.m3.collections.roaring.IntConsumer;
import jdk.m3.collections.roaring.PeekableCharIterator;
import jdk.m3.collections.roaring.Util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Iterator;

final class ReverseMappeableBitmapContainerCharIterator implements PeekableCharIterator {

  private static final int len =
      MappeableBitmapContainer.MAX_CAPACITY >>> 6; // hard coded for speed
  private long w;
  int x;

  private MappeableBitmapContainer parent;

  ReverseMappeableBitmapContainerCharIterator() {}

  ReverseMappeableBitmapContainerCharIterator(MappeableBitmapContainer p) {
    wrap(p);
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
    return x >= 0;
  }

  @Override
  public char next() {
    int shift = Long.numberOfLeadingZeros(w) + 1;
    char answer = (char) ((x + 1) * 64 - shift);
    w &= (0xFFFFFFFFFFFFFFFEL >>> shift);
    while (w == 0) {
      --x;
      if (x < 0) {
        break;
      }
      w = parent.bitmap.get(x);
    }
    return answer;
  }

  @Override
  public int nextAsInt() {
    return next();
  }

  @Override
  public void advanceIfNeeded(char maxval) {
    if (maxval < (x + 1) * 64) {
      if (maxval < x * 64) {
        x = maxval / 64;
      }
      long currentWord = parent.bitmap.get(x);
      currentWord &= ~0L >>> (63 - (maxval & 63));
      while (currentWord == 0 && x > 0) {
        x--;
        currentWord = parent.bitmap.get(x);
      }
      w = currentWord;
      if (currentWord == 0) {
        // no value at or below maxval in this container
        x = -1;
      }
    }
  }

  @Override
  public char peekNext() {
    int shift = Long.numberOfLeadingZeros(w) + 1;
    return (char) ((x + 1) * 64 - shift);
  }

  @Override
  public void remove() {
    // TODO: implement
    throw new RuntimeException("unsupported operation: remove");
  }

  public void wrap(MappeableBitmapContainer p) {
    parent = p;
    for (x = len - 1; x >= 0; --x) {
      if ((w = parent.bitmap.get(x)) != 0) {
        break;
      }
    }
  }
}
