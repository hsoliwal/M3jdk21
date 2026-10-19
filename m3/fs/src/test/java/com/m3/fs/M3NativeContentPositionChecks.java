// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Fresh-JVM probe so native OFF/REQUIRED policies are independently initialized. */
public final class M3NativeContentPositionChecks {
  private static int checks;
  private M3NativeContentPositionChecks() {}
  public static void main(String[] args) {
    boolean required = System.getProperty("m3fs.native").equals("required");
    if (required) check(M3FS.nativeAvailable());
    for (ByteOrder order : new ByteOrder[] {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
      ByteBuffer corpus = ByteBuffer.allocateDirect(17);
      corpus.put((byte) '?').put((byte) '?').put("ahellozhello!".getBytes(StandardCharsets.UTF_8));
      corpus.put((byte) '!').put((byte) '!').position(2).limit(15);
      LongBuffer offsets = ByteBuffer.allocateDirect(48).order(order).asLongBuffer();
      offsets.put(new long[] {999, 0, 1, 6, 13, 999}).position(1).limit(5);
      IntBuffer lengths = lane(order, 999, 1, 5, 7, 999);
      int[] rows = {99, 99, 99};
      check(M3NativeContent.grepSegments(corpus, offsets, lengths,
          "hello".getBytes(StandardCharsets.UTF_8), rows) == 2);
      check(Arrays.equals(rows, new int[] {1, 2, 99}));
      check(corpus.position() == 2 && corpus.limit() == 15);
      check(offsets.position() == 1 && offsets.limit() == 5);
      check(lengths.position() == 1 && lengths.limit() == 4);

      ByteBuffer first = ByteBuffer.allocateDirect(12).order(ByteOrder.BIG_ENDIAN);
      first.putShort((short) 0).putChar('A').putChar('\ud83d').putChar('\ude00').putChar('β').putShort((short) 0);
      first.position(2).limit(10).order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer second = ByteBuffer.allocateDirect(8).order(ByteOrder.BIG_ENDIAN);
      second.put((byte) 0).putChar('x').putChar('y').putChar('z').put((byte) 0).position(1).limit(7);
      IntBuffer indexes = lane(order, 999, 0, 1, 0, 999);
      IntBuffer starts = lane(order, 999, 1, 1, 0, 999);
      IntBuffer sizes = lane(order, 999, 2, 2, 1, 999);
      char[] value = M3NativeContent.materializeUtf16Segments(new ByteBuffer[] {first, second}, indexes, starts, sizes, 5);
      check(Arrays.equals(value, "😀yzA".toCharArray()));
      check(first.position() == 2 && first.limit() == 10 && first.order() == ByteOrder.LITTLE_ENDIAN);
      check(second.position() == 1 && second.limit() == 7);
      check(indexes.position() == 1 && starts.position() == 1 && sizes.position() == 1);
      reject(IllegalArgumentException.class, () -> M3NativeContent.materializeUtf16Segments(
          new ByteBuffer[] {first}, lane(order, 99, 2, 99), lane(order, 99, 0, 99), lane(order, 99, 1, 99), 1));
    }
    ByteBuffer a = ByteBuffer.allocateDirect(8), b = ByteBuffer.allocateDirect(8);
    for (int i = 0; i < 8; i++) { a.put(i, (byte) (i + 1)); b.put(i, (byte) (i + 1)); }
    a.position(1).limit(1); b.position(2).limit(2);
    check(M3NativeContent.equalDirect(a, 4, b, 4, 3));
    b.clear().put(6, (byte) 0).position(2).limit(2);
    check(!M3NativeContent.equalDirect(a, 4, b, 4, 3));
    check(a.position() == 1 && a.limit() == 1 && b.position() == 2 && b.limit() == 2);
    reject(IndexOutOfBoundsException.class, () -> M3NativeContent.equalDirect(a, 7, b, 7, 2));
    if (required) reject(IllegalStateException.class,
        () -> M3NativeContent.equalDirect(ByteBuffer.allocate(1), 0, ByteBuffer.allocate(1), 0, 1));
    else check(M3NativeContent.equalDirect(ByteBuffer.allocate(1), 0, ByteBuffer.allocate(1), 0, 1));
    System.out.println("CONTENT_POSITION_PASS mode=" + System.getProperty("m3fs.native") + " checks=" + checks);
  }
  private static IntBuffer lane(ByteOrder order, int... values) {
    return ByteBuffer.allocateDirect(values.length * 4).order(order).asIntBuffer()
        .put(values).position(1).limit(values.length - 1);
  }
  private static void check(boolean valid) { checks++; if (!valid) throw new AssertionError("check " + checks); }
  private static void reject(Class<? extends Throwable> expected, Runnable action) {
    try { action.run(); } catch (Throwable problem) {
      if (expected.isInstance(problem)) { checks++; return; } throw problem;
    }
    throw new AssertionError("expected " + expected.getName());
  }
}
