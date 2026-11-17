/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package jdk.m3.collections.guava.escape;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;

import jdk.m3.collections.guava.annotations.GwtCompatible;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
@GwtCompatible
final class Platform {
  private Platform() {}

  /** Returns a thread-local 1024-char array. */
  static char[] charBufferFromThreadLocal() {
    // requireNonNull accommodates Android's @RecentlyNullable annotation on ThreadLocal.get
    return requireNonNull(DEST_TL.get());
  }

  /**
   * A thread-local destination buffer to keep us from creating new buffers. The starting size is
   * 1024 characters. If we grow past this we don't put it back in the threadlocal, we just keep
   * going and grow as needed.
   */
  private static final ThreadLocal<char[]> DEST_TL =
      new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
          return new char[1024];
        }
      };

  /** Largest length doubling asks for; a larger need takes the pre-change length instead. */
  private static final int SOFT_MAX_LENGTH = Integer.MAX_VALUE - 8;

  /**
   * Returns a buffer holding {@code dest[0, destIndex)} that takes the next escape: {@code
   * skipped} unescaped chars, then {@code rlen} replacement chars. {@code cap} is the capacity
   * lane, the length the pre-change escapeSlow holds here. Growth doubles, so escaping n chars
   * into m chars copies O(n + m) chars. When {@code cap} cannot take the escape, the pre-change
   * buffer is rebuilt at length {@code cap}, so the next copy throws what it threw before.
   */
  //FLLDCIM: DYNAMIC_ARRAY - amortised doubling over the pre-change capacity lane
  static char[] reserve(char[] dest, int destIndex, int skipped, int rlen, int cap) {
    long end = (long) destIndex + Math.max(skipped, 0) + rlen;
    if (end > cap) {
      boolean oldKept = destIndex + skipped + rlen <= cap; // the old code did not regrow here
      return oldKept && dest.length == cap ? dest : growBuffer(dest, destIndex, cap);
    }
    if (end > dest.length) {
      dest = growBuffer(dest, destIndex, grownLength(dest.length, cap));
    }
    return dest;
  }

  /** Doubles {@code length}, clamped to SOFT_MAX_LENGTH, and never less than {@code cap}. */
  //FLLDCIM: DYNAMIC_ARRAY - geometric growth step (one System.arraycopy per growth)
  private static int grownLength(int length, int cap) {
    return Math.max(cap, (int) Math.min((long) length << 1, SOFT_MAX_LENGTH));
  }

  /** Copies {@code s[from, to)} when it is not empty, then {@code r}; returns the new end. */
  //FLLDCIM: DIRECT_ACCESS - one bulk copy for the skipped run, one for the replacement
  static int append(String s, int from, int to, char[] r, char[] dest, int destIndex) {
    int skipped = to - from;
    if (skipped > 0) {
      s.getChars(from, to, dest, destIndex);
      destIndex += skipped;
    }
    if (r.length > 0) {
      arraycopy(r, 0, dest, destIndex, r.length);
      destIndex += r.length;
    }
    return destIndex;
  }

  /** Copies the tail {@code s[from, s.length())} at its exact size and returns the result. */
  //FLLDCIM: DIRECT_ACCESS - exact final copy, unchanged sizing
  static String finish(String s, int from, char[] dest, int destIndex, int cap) {
    int left = s.length() - from;
    if (left > 0) {
      int sizeNeeded = destIndex + left;
      dest = reserveExact(dest, destIndex, sizeNeeded, cap);
      s.getChars(from, s.length(), dest, destIndex);
      destIndex = sizeNeeded;
    }
    return new String(dest, 0, destIndex);
  }

  /** Tail buffer: exact size; on int overflow the pre-change buffer of length {@code cap}. */
  private static char[] reserveExact(char[] dest, int destIndex, int sizeNeeded, int cap) {
    if (sizeNeeded < 0) {
      return dest.length == cap ? dest : growBuffer(dest, destIndex, cap);
    }
    return dest.length < sizeNeeded ? growBuffer(dest, destIndex, sizeNeeded) : dest;
  }

  /**
   * Helper method to grow the character buffer as needed, this only happens once in a while so it's
   * ok if it's in a method call. If the index passed in is 0 then no copying will be done.
   */
  private static char[] growBuffer(char[] dest, int index, int size) {
    char[] copy = new char[checkedSize(size)];
    if (index > 0) {
      arraycopy(dest, 0, copy, 0, index);
    }
    return copy;
  }

  /** Returns {@code size}; a negative (overflowed) size is the escapers' AssertionError. */
  static int checkedSize(int size) {
    if (size < 0) { // overflow - should be OutOfMemoryError but GWT/j2cl don't support it
      throw new AssertionError("Cannot increase internal buffer any further");
    }
    return size;
  }
}
