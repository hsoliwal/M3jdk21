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

final class ReverseArrayContainerCharIterator implements PeekableCharIterator {
  int pos;
  private ArrayContainer parent;

  ReverseArrayContainerCharIterator() {}

  ReverseArrayContainerCharIterator(ArrayContainer p) {
    wrap(p);
  }

  @Override
  public void advanceIfNeeded(char maxval) {
    // reverseUntil returns 0 both when index 0 is the answer and when nothing qualifies, so
    // index 0 has to be re-checked: if it is still above maxval, this container is exhausted.
    int candidate = Util.reverseUntil(parent.content, pos + 1, maxval);
    pos = (candidate == 0 && parent.content[0] > maxval) ? -1 : candidate;
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
    return parent.content[pos--];
  }

  @Override
  public int nextAsInt() {
    return (parent.content[pos--]);
  }

  @Override
  public char peekNext() {
    return parent.content[pos];
  }

  @Override
  public void remove() {
    parent.removeAtIndex(pos + 1);
    pos++;
  }

  void wrap(ArrayContainer p) {
    parent = p;
    pos = parent.cardinality - 1;
  }
}
