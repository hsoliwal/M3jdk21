// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/test/java/com/synexia/precompute/FrozenLongTupleIndexPairRegressionTest.java blob 083d5398bec3; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
class FrozenLongTupleIndexPairRegressionTest {
  @TempDir Path temp;

  @Test
  void pairSlotsUseTheSameHashAsJavaJniAndMappedLookup() throws Exception {
    int size = 512;
    long[] keys = new long[size * 2];
    int[] values = new int[size];
    for (int row = 0; row < size; row++) {
      keys[2 * row] = row * 37L - 9_000L;
      keys[2 * row + 1] = Long.rotateLeft(row * 997L, 17);
      values[row] = row + 1;
    }
    FrozenLongTupleIndex index = FrozenLongTupleIndex.freeze(
        IndexPrecomputeCapability.TUPLE_ID, 2, keys, values);
    long[] left = new long[size * 2];
    long[] right = new long[size * 2];
    int[] expected = new int[size * 2];
    for (int row = 0; row < size; row++) {
      left[row] = keys[2 * row];
      right[row] = keys[2 * row + 1];
      expected[row] = row + 1;
      left[size + row] = left[row];
      right[size + row] = right[row] ^ 1L;
      expected[size + row] = -1;
      assertTrue(index.containsPair(left[row], right[row]));
      assertFalse(index.containsPair(left[size + row], right[size + row]));
      assertEquals(expected[row], index.getPair(left[row], right[row], -1));
    }
    assertArrayEquals(expected, NativePrecomputeOps.lookupPairs(
        index, left, right, -1, Integer.MAX_VALUE));
    if (NativePrecomputeOps.available()) {
      assertArrayEquals(expected, NativePrecomputeOps.lookupPairs(index, left, right, -1, 0));
    }
    Path image = temp.resolve("pairs.syp");
    LongTupleIndexImage.write(index, image);
    LongTupleIndexView mapped = LongTupleIndexImage.map(image);
    for (int row = 0; row < size * 2; row++) {
      assertEquals(expected[row], mapped.get(left[row], right[row], -1));
    }
    assertThrows(IllegalArgumentException.class, () -> FrozenLongTupleIndex.freeze(
        IndexPrecomputeCapability.TUPLE_ID, 2,
        new long[] {keys[0], keys[1], keys[0], keys[1]}, new int[] {1, 2}));
  }
}
