// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Oracle of m3/fs/recipe/precompute-joined-lanes-20261106 stage 02 (Synexia crate m3fs-memchr-bitmap-20261102),
// relocated from its qualification proof M3BytePresenceFastSearchProof: the same seed, segment counts and sparse
// forcing drive the real JoinedBytePresence.candidateRows; the proof's read-counting model (unoptimized and with the
// zero-mask early refusal) must return the real rows, and the early refusal must cut the reads 1,736,187 -> 503,582.
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class M3FsJoinedBytePresenceEarlyRefusalTest {
  private static final int[] DIMENSIONS = {0, 1, 2, 31, 63, 64, 65, 66, 127, 128, 129, 255, 256, 257, 512, 1024};

  private static int[] model(long[] bitmap, int segments, int words, byte[] needle, boolean early, long[] reads) {
    int[] output = new int[segments];
    int count = 0;
    for (int word = 0; word < words; word++) {
      long candidates = -1L;
      for (byte value : needle) {
        candidates &= bitmap[Byte.toUnsignedInt(value) * words + word];
        reads[0]++;
        if (early && candidates == 0L) break;
      }
      if (word == words - 1 && (segments & 63) != 0) candidates &= (1L << (segments & 63)) - 1L;
      while (candidates != 0L) {
        output[count++] = (word << 6) + Long.numberOfTrailingZeros(candidates);
        candidates &= candidates - 1L;
      }
    }
    return Arrays.copyOf(output, count);
  }

  @Test
  void earlyRefusalKeepsEveryCandidateRowAndCutsBitmapReads() {
    Random rng = new Random(0x4d334a4156413231L);
    long cases = 0, oldReads = 0, newReads = 0;
    for (int segments : DIMENSIONS) {
      int words = (segments + 63) >>> 6;
      for (int trial = 0; trial < 2000; trial++) {
        long[] bits = new long[256 * words];
        for (int i = 0; i < bits.length; i++) bits[i] = rng.nextLong();
        byte[] needle = new byte[1 + rng.nextInt(32)];
        rng.nextBytes(needle);
        if (trial % 3 == 0 && words > 0) {
          for (int w = 0; w < words; w++) bits[Byte.toUnsignedInt(needle[0]) * words + w] = 0L;
        }
        long[] a = {0}, b = {0};
        int[] unoptimized = model(bits, segments, words, needle, false, a);
        int[] early = model(bits, segments, words, needle, true, b);
        var index = new M3FsPrecomputeBundle.JoinedBytePresence(
            LongBuffer.wrap(bits), IntBuffer.wrap(new int[] {segments, words}));
        int[] output = new int[segments];
        int count = index.candidateRows(needle, output);
        assertArrayEquals(unoptimized, early);
        assertArrayEquals(unoptimized, Arrays.copyOf(output, count), "segments=" + segments + " trial=" + trial);
        assertTrue(b[0] <= a[0]);
        for (int i = 1; i < count; i++) assertTrue(output[i - 1] < output[i]);
        oldReads += a[0];
        newReads += b[0];
        cases++;
      }
    }
    assertEquals(32_000, cases);
    assertEquals(1_736_187L, oldReads);
    assertEquals(503_582L, newReads);
  }
}
