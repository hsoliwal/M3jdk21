// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3GpuGrepSegmentsRuntimeTest {
  @TempDir Path temp;

  @Test
  void mappedSegmentBatchMatchesExactRowsWithoutRepackingCorpus() throws Exception {
    int segments = 320;
    byte[][] rows = new byte[segments][];
    long[] offsets = new long[segments + 1];
    int[] lengths = new int[segments];
    int bytes = 0;
    for (int row = 0; row < segments; row++) {
      String value = row % 17 == 0 ? "row-" + row + "-needle" : "row-" + row + "-other";
      rows[row] = value.getBytes(StandardCharsets.UTF_8);
      offsets[row] = bytes;
      lengths[row] = rows[row].length;
      bytes += rows[row].length;
    }
    offsets[segments] = bytes;

    byte[] packed = new byte[bytes];
    int at = 0;
    for (byte[] row : rows) {
      System.arraycopy(row, 0, packed, at, row.length);
      at += row.length;
    }

    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(packed, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search",
            "gpu-grep-segments",
            1,
            List.of(source.id()),
            Map.of("segments", Integer.toString(segments)));

    new M3FsPrecomputeWriter(store, key, "gpu-grep-segments-v1")
        .addGpuGrepSegments(packed, offsets, lengths)
        .publish(cache);

    var mapped =
        cache.lookupBundle(key)
            .orElseThrow()
            .openMapped(store, 1 << 20, 4L << 20)
            .gpuGrepSegments();

    int[] output = new int[segments];
    int matches =
        mapped.grepSegments("needle".getBytes(StandardCharsets.UTF_8), output);

    int[] expected =
        java.util.stream.IntStream.range(0, segments)
            .filter(row -> row % 17 == 0)
            .toArray();
    assertEquals(expected.length, matches);
    assertArrayEquals(expected, Arrays.copyOf(output, matches));

    Arrays.fill(output, -1);
    assertEquals(
        0,
        mapped.grepSegments("not-present".getBytes(StandardCharsets.UTF_8), output));
    assertThrows(
        IllegalArgumentException.class,
        () -> mapped.grepSegments(new byte[0], output));
  }
}
