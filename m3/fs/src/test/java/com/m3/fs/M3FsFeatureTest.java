// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class M3FsFeatureTest {
  @BeforeAll
  static void configureNativeLibrary() throws IOException {
    Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
    String pathFile = System.getProperty("m3.fs.native.pathFile");
    if (pathFile != null && !pathFile.isBlank()) {
      String library = Files.readString(Path.of(pathFile)).trim();
      if (!library.isEmpty()) System.setProperty("m3.fs.native.path", library);
    }
  }

  @Test
  void featureBitsAreUniqueAndKnownMaskContainsEveryFeatureExactlyOnce() {
    long mask = M3FsFeature.knownMask();
    assertEquals(M3FsFeature.values().length, Long.bitCount(mask));
    for (M3FsFeature feature : M3FsFeature.values()) {
      assertEquals(1, Long.bitCount(feature.bit()));
      assertTrue((mask & feature.bit()) != 0L);
    }
  }

  @Test
  void currentNativeEngineAdvertisesItsImplementedSemanticSuperset() {
    Assumptions.assumeTrue(M3FS.nativeAvailable(), "native MIndexFS unavailable");
    Set<M3FsFeature> expected =
        EnumSet.of(
            M3FsFeature.EXACT_BLOCK_DEDUP,
            M3FsFeature.SPARSE_FILES,
            M3FsFeature.BLOCK_CLONE_COW,
            M3FsFeature.HARD_LINKS,
            M3FsFeature.SYMLINKS,
            M3FsFeature.RETAINED_READ_SNAPSHOT,
            M3FsFeature.DIRECTORY_PAGING,
            M3FsFeature.SCATTER_GATHER_WRITE,
            M3FsFeature.WAL_CHECKPOINT,
            M3FsFeature.INTEGRITY_VERIFY,
            M3FsFeature.ORPHAN_BLOCK_GC,
            M3FsFeature.HOLE_PUNCH,
            M3FsFeature.ALLOCATED_RANGE_QUERY,
            M3FsFeature.SCATTER_GATHER_READ,
            M3FsFeature.NATIVE_MINDEX,
            M3FsFeature.NATIVE_REGEX,
            M3FsFeature.BLOCK_SCRUB,
            M3FsFeature.SET_METADATA,
            M3FsFeature.ATOMIC_REPLACE,
            M3FsFeature.CHECKED_INODE_IO,
            M3FsFeature.RENAME_NOREPLACE);
    assertEquals(expected, M3FS.supportedFeatures());
    assertEquals(16_383, M3FsNative.capabilities());
    assertEquals(2_097_151L, M3FS.featureBits());
    for (M3FsFeature feature : expected) {
      assertTrue(M3FS.supports(feature));
    }
  }
}
