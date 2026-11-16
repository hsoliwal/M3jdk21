/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package jdk.m3.collections.roaring;

import jdk.m3.collections.roaring.buffer.MappeableArrayContainer;
import jdk.m3.collections.roaring.buffer.MappeableContainer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;

final class ArrayContainerCharIterator implements PeekableCharRankIterator {
  int pos;
  private ArrayContainer parent;

  ArrayContainerCharIterator() {}

  ArrayContainerCharIterator(ArrayContainer p) {
    wrap(p);
  }

  @Override
  public void advanceIfNeeded(char minval) {
    pos = Util.advanceUntil(parent.content, pos - 1, parent.cardinality, minval);
  }

  @Override
  public int peekNextRank() {
    return pos + 1;
  }

  @Override
  public PeekableCharRankIterator clone() {
    try {
      return (PeekableCharRankIterator) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // will not happen
    }
  }

  @Override
  public boolean hasNext() {
    return pos < parent.cardinality;
  }

  @Override
  public char next() {
    return parent.content[pos++];
  }

  @Override
  public int nextAsInt() {
    return (parent.content[pos++]);
  }

  @Override
  public char peekNext() {
    return parent.content[pos];
  }

  @Override
  public void remove() {
    parent.removeAtIndex(pos - 1);
    pos--;
  }

  void wrap(ArrayContainer p) {
    parent = p;
    pos = 0;
  }
}
