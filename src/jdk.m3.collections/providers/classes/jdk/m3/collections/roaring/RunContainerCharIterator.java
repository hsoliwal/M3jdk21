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

class RunContainerCharIterator implements PeekableCharIterator {
  int pos;
  int le = 0;
  int maxlength;
  int base;

  RunContainer parent;

  RunContainerCharIterator() {}

  RunContainerCharIterator(RunContainer p) {
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
    return pos < parent.nbrruns;
  }

  @Override
  public char next() {
    char ans = (char) (base + le);
    le++;
    if (le > maxlength) {
      pos++;
      le = 0;
      if (pos < parent.nbrruns) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      }
    }
    return ans;
  }

  @Override
  public int nextAsInt() {
    int ans = base + le;
    le++;
    if (le > maxlength) {
      pos++;
      le = 0;
      if (pos < parent.nbrruns) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      }
    }
    return ans;
  }

  @Override
  public void remove() {
    throw new RuntimeException("Not implemented"); // TODO
  }

  void wrap(RunContainer p) {
    parent = p;
    pos = 0;
    le = 0;
    if (pos < parent.nbrruns) {
      maxlength = (parent.getLength(pos));
      base = (parent.getValue(pos));
    }
  }

  @Override
  public void advanceIfNeeded(char minval) {
    while (base + maxlength < (minval)) {
      pos++;
      le = 0;
      if (pos < parent.nbrruns) {
        maxlength = (parent.getLength(pos));
        base = (parent.getValue(pos));
      } else {
        return;
      }
    }
    if (base > (minval)) {
      return;
    }
    le = Math.max(le, minval - base);
  }

  @Override
  public char peekNext() {
    return (char) (base + le);
  }
}
