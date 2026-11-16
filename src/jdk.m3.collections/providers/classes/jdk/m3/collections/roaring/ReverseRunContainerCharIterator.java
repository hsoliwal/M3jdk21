/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package jdk.m3.collections.roaring;

import jdk.m3.collections.roaring.buffer.MappeableContainer;
import jdk.m3.collections.roaring.buffer.MappeableRunContainer;

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

final class ReverseRunContainerCharIterator implements PeekableCharIterator {
  int pos;
  private int le;
  private RunContainer parent;
  private int maxlength;
  private int base;

  ReverseRunContainerCharIterator() {}

  ReverseRunContainerCharIterator(RunContainer p) {
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
    return pos >= 0;
  }

  @Override
  public char next() {
    char ans = (char) (base + maxlength - le);
    le++;
    if (le > maxlength) {
      pos--;
      le = 0;
      if (pos >= 0) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      }
    }
    return ans;
  }

  @Override
  public int nextAsInt() {
    int ans = base + maxlength - le;
    le++;
    if (le > maxlength) {
      pos--;
      le = 0;
      if (pos >= 0) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      }
    }
    return ans;
  }

  @Override
  public void advanceIfNeeded(char maxval) {
    while (base > (maxval)) {
      pos--;
      le = 0;
      if (pos >= 0) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      } else {
        return;
      }
    }
    if (base + maxlength < (maxval)) {
      return;
    }
    le = Math.max(le, maxlength + base - maxval);
  }

  @Override
  public char peekNext() {
    return (char) (base + maxlength - le);
  }

  @Override
  public void remove() {
    throw new RuntimeException("Not implemented"); // TODO
  }

  void wrap(RunContainer p) {
    parent = p;
    pos = parent.nbrruns - 1;
    le = 0;
    if (pos >= 0) {
      maxlength = (parent.getLength(pos));
      base = (parent.getValue(pos));
    }
  }
}
