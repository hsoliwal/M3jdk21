// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.Arrays;
import java.sql.DriverManager;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FSScrubTest {
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
    assertTrue(M3FS.supportsBlockScrub(), "block scrub capability");
  }

  @Test
  void scrubReportsDeduplicatedUniqueBlocksAndReadOnlyVerification() throws Exception {
    Path database = temp.resolve("scrub.sqlite");
    byte[] block = new byte[65_536];
    Arrays.fill(block, (byte) 0x5a);

    try (M3FS fs = M3FS.create(database)) {
      fs.createFile("/a", 0644);
      fs.createFile("/b", 0644);
      fs.write("/a", 0L, block);
      fs.write("/b", 0L, block);

      M3FS.ScrubReport report = fs.scrub();
      assertEquals(1L, report.blocksScanned());
      assertEquals(1L, report.checksumsVerified());
      assertEquals(0L, report.checksumsMissing());
      assertEquals(0L, report.checksumsMismatched());
      assertEquals(0L, report.checksumsBackfilled());
      assertTrue(report.cleanAndComplete());
      assertTrue(report.noMismatch());
      assertEquals(1L, report.checksumsCoveredAfterRun());
    }

    CRC32C expected = new CRC32C();
    expected.update(block, 0, block.length);
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var rows = statement.executeQuery("SELECT crc32c FROM block_integrity")) {
      assertTrue(rows.next());
      assertEquals(expected.getValue(), rows.getLong(1));
      assertFalse(rows.next());
    }

    try (M3FS readOnly = M3FS.openReadOnly(database)) {
      M3FS.ScrubReport report = readOnly.scrub();
      assertTrue(report.cleanAndComplete());
      assertThrows(ReadOnlyFileSystemException.class, () -> readOnly.scrub(true));
    }
  }

  @Test
  void scrubReportRejectsIncompleteNativeClassificationGeometry() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3FS.ScrubReport(2L, 1L, 0L, 0L, 0L));
  }
}
