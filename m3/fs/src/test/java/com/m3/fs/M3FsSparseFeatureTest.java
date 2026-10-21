// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FsSparseFeatureTest {
  private static final int BLOCK = 65_536;

  @TempDir Path temp;

  @BeforeAll
  static void configureNativeLibrary() throws IOException {
    Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
    String pathFile = System.getProperty("m3.fs.native.pathFile");
    if (pathFile != null && !pathFile.isBlank()) {
      String library = Files.readString(Path.of(pathFile)).trim();
      if (!library.isEmpty()) System.setProperty("m3.fs.native.path", library);
    }
    assertTrue(M3FS.nativeAvailable(), "native MIndexFS library");
  }

  @Test
  void holePunchPreservesSizeReportsAllocationAndKeepsCloneIsolation() throws Exception {
    Path database = temp.resolve("sparse.sqlite");
    byte[] block = new byte[BLOCK];

    try (M3FS fs = M3FS.create(database)) {
      assertTrue(M3FS.supportsHolePunch());
      assertTrue(M3FS.supportsAllocatedRangeQuery());
      assertTrue(M3FS.supports(M3FsFeature.HOLE_PUNCH));
      assertTrue(M3FS.supports(M3FsFeature.ALLOCATED_RANGE_QUERY));

      fs.createFile("/f", 0644);
      Arrays.fill(block, (byte) 'A');
      fs.write("/f", 0L, block);
      Arrays.fill(block, (byte) 'B');
      fs.write("/f", BLOCK, block);
      Arrays.fill(block, (byte) 'C');
      fs.write("/f", 2L * BLOCK, block);

      long logicalSize = 3L * BLOCK;
      assertEquals(
          List.of(new M3FS.AllocatedRange(0L, logicalSize)),
          fs.allocatedRanges("/f", 0L, logicalSize));

      long generation = fs.stat("/f").generation();
      fs.punchHole("/f", BLOCK, BLOCK);
      assertEquals(logicalSize, fs.stat("/f").size());
      assertEquals(generation + 1L, fs.stat("/f").generation());
      assertEquals(0, fs.read("/f", BLOCK, 1)[0]);
      assertEquals((byte) 'C', fs.read("/f", 2L * BLOCK, 1)[0]);
      assertEquals(
          List.of(
              new M3FS.AllocatedRange(0L, BLOCK),
              new M3FS.AllocatedRange(2L * BLOCK, BLOCK)),
          fs.allocatedRanges("/f", 0L, logicalSize));

      fs.punchHole("/f", 100L, 1000L);
      assertArrayEquals(new byte[] {'A'}, fs.read("/f", 99L, 1));
      assertArrayEquals(new byte[] {0}, fs.read("/f", 100L, 1));
      assertArrayEquals(new byte[] {0}, fs.read("/f", 1099L, 1));
      assertArrayEquals(new byte[] {'A'}, fs.read("/f", 1100L, 1));
      assertEquals(
          List.of(
              new M3FS.AllocatedRange(BLOCK / 2L, BLOCK / 2L),
              new M3FS.AllocatedRange(2L * BLOCK, BLOCK / 2L)),
          fs.allocatedRanges("/f", BLOCK / 2L, 2L * BLOCK));

      fs.cloneFile("/f", "/clone");
      fs.punchHole("/clone", 2L * BLOCK, BLOCK);
      assertArrayEquals(new byte[] {0}, fs.read("/clone", 2L * BLOCK, 1));
      assertArrayEquals(new byte[] {'C'}, fs.read("/f", 2L * BLOCK, 1));

      long stable = fs.stat("/f").generation();
      fs.punchHole("/f", logicalSize + 10L, 100L);
      assertEquals(stable, fs.stat("/f").generation());

      M3FS.Stats stats = fs.stats();
      assertTrue(stats.uniqueBlocks() <= stats.blockReferences());
      fs.verify();
    }

    try (M3FS readOnly = M3FS.openReadOnly(database)) {
      assertThrows(IOException.class, () -> readOnly.punchHole("/f", 0L, 1L));
      assertEquals(2, readOnly.allocatedRanges("/f", 0L, 3L * BLOCK).size());
    }
  }

  @Test
  void rangeGeometryFailsBeforeNativeMutation() throws Exception {
    Path database = temp.resolve("sparse-bounds.sqlite");
    try (M3FS fs = M3FS.create(database)) {
      fs.createFile("/f", 0644);
      assertThrows(IllegalArgumentException.class, () -> fs.punchHole("/f", -1L, 1L));
      assertThrows(IllegalArgumentException.class, () -> fs.punchHole("/f", 0L, -1L));
      assertThrows(
          IllegalArgumentException.class,
          () -> fs.allocatedRanges("/f", Long.MAX_VALUE, 1L));
      assertEquals(List.of(), fs.allocatedRanges("/f", 0L, 0L));
    }
  }
}
