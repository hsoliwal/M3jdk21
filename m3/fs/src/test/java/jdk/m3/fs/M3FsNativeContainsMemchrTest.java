// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Oracle of m3/fs/recipe/precompute-joined-lanes-20261106 stage 02 (Synexia crate m3fs-memchr-bitmap-20261102), JNI
// side: GpuGrepSegments.grepSegments over direct buffers reaches nativeGrepSegments and its m3_contains (first-byte
// memchr skip + memcmp) under -Xcheck:jni; every row set must equal a plain Java scan, including one-byte needles,
// periodic runs, high bytes, needles longer than the segment and empty segments.
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class M3FsNativeContainsMemchrTest {
  @Test
  void nativeSegmentScanAgreesWithJavaScan() {
    Random random = new Random(0x4d454d434852L);
    for (int trial = 0; trial < 400; trial++) {
      int segments = 1 + random.nextInt(96);
      int mode = trial % 4;
      byte[][] rows = new byte[segments][];
      int total = 0;
      for (int row = 0; row < segments; row++) {
        rows[row] = new byte[random.nextInt(80)];
        for (int i = 0; i < rows[row].length; i++) {
          rows[row][i] = mode == 0 ? (byte) 'a' : mode == 1 ? (byte) (0xF0 | random.nextInt(4)) : (byte) random.nextInt(256);
        }
        total += rows[row].length;
      }
      byte[] needle = new byte[1 + random.nextInt(mode == 3 ? 3 : 12)];
      for (int i = 0; i < needle.length; i++) {
        needle[i] = mode == 0 ? (byte) 'a' : mode == 1 ? (byte) (0xF0 | random.nextInt(4)) : (byte) random.nextInt(256);
      }
      if (mode == 2 && segments > 1) {
        byte[] host = rows[random.nextInt(segments)];
        if (host.length >= needle.length) System.arraycopy(needle, 0, host, random.nextInt(host.length - needle.length + 1), needle.length);
      }
      ByteBuffer corpus = ByteBuffer.allocateDirect(total);
      LongBuffer offsets = ByteBuffer.allocateDirect(8 * (segments + 1)).order(ByteOrder.BIG_ENDIAN).asLongBuffer();
      IntBuffer lengths = ByteBuffer.allocateDirect(4 * segments).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
      int[] expected = new int[segments];
      int matches = 0;
      long at = 0;
      for (int row = 0; row < segments; row++) {
        corpus.put(rows[row]);
        offsets.put(at);
        lengths.put(rows[row].length);
        at += rows[row].length;
        if (M3FsJoinedContentSegmentsTest.contains(rows[row], needle)) expected[matches++] = row;
      }
      offsets.put(at);
      corpus.flip();
      offsets.flip();
      lengths.flip();
      var grep = new M3FsPrecomputeBundle.GpuGrepSegments(corpus, offsets, lengths);
      int[] output = new int[segments];
      int found = grep.grepSegments(needle, output);
      assertEquals(matches, found, "trial " + trial);
      assertArrayEquals(Arrays.copyOf(expected, matches), Arrays.copyOf(output, found), "trial " + trial);
    }
  }
}
