/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package jdk.m3.collections.roaring.buffer;

import static jdk.m3.collections.roaring.buffer.MappeableBitmapContainer.MAX_CAPACITY;

import jdk.m3.collections.roaring.ArrayContainer;
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
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;

final class ReverseMappeableArrayContainerCharIterator implements PeekableCharIterator {

  int pos;

  private MappeableArrayContainer parent;

  ReverseMappeableArrayContainerCharIterator() {}

  ReverseMappeableArrayContainerCharIterator(MappeableArrayContainer p) {
    wrap(p);
  }

  @Override
  public void advanceIfNeeded(char maxval) {
    // reverseUntil returns 0 both when index 0 is the answer and when nothing qualifies, so
    // index 0 has to be re-checked: if it is still above maxval, this container is exhausted.
    int candidate = BufferUtil.reverseUntil(parent.content, pos + 1, maxval);
    pos = (candidate == 0 && parent.content.get(0) > maxval) ? -1 : candidate;
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
    return pos >= 0;
  }

  @Override
  public char next() {
    return parent.content.get(pos--);
  }

  @Override
  public int nextAsInt() {
    return (parent.content.get(pos--));
  }

  @Override
  public char peekNext() {
    return parent.content.get(pos);
  }

  @Override
  public void remove() {
    parent.removeAtIndex(pos + 1);
    pos++;
  }

  void wrap(MappeableArrayContainer p) {
    parent = p;
    pos = parent.cardinality - 1;
  }
}
