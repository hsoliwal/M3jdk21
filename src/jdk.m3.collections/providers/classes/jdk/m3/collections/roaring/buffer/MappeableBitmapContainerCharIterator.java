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

final class MappeableBitmapContainerCharIterator implements PeekableCharIterator {
  private static final int len =
      MappeableBitmapContainer.MAX_CAPACITY >>> 6; // hard coded for speed
  private long w;
  int x;

  private MappeableBitmapContainer parent;

  MappeableBitmapContainerCharIterator() {}

  MappeableBitmapContainerCharIterator(MappeableBitmapContainer p) {
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
    return x < len;
  }

  @Override
  public char next() {
    char answer = (char) (x * 64 + numberOfTrailingZeros(w));
    w &= (w - 1);
    while (w == 0) {
      ++x;
      if (x == len) {
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
  public void remove() {
    // TODO: implement
    throw new RuntimeException("unsupported operation: remove");
  }

  void wrap(MappeableBitmapContainer p) {
    parent = p;
    for (x = 0; x < len; ++x) {
      if ((w = parent.bitmap.get(x)) != 0) {
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
        w = parent.bitmap.get(x);
      }
      w &= ~0L << (minval & 63);
      while (w == 0) {
        x++;
        if (x == len) {
          return;
        }
        w = parent.bitmap.get(x);
      }
    }
  }

  @Override
  public char peekNext() {
    return (char) (x * 64 + numberOfTrailingZeros(w));
  }
}
