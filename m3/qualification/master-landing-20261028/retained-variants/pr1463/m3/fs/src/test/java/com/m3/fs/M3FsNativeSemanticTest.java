// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FsNativeSemanticTest {
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
    assertTrue(M3FS.supportsNativeMIndex(), "native MIndex capability");
    assertTrue(M3FS.supportsNativeRegex(), "native regex capability");
  }

  @Test
  void filesystemExecutesMIndexStringAndRegexSemanticsNatively() throws Exception {
    List<String> values = List.of("ab", "zabz", "A B", "😀", "");
    int[] starts = {0, 2, 6, 9, 11, 11};
    long[] tokenIds = {11, 12, 13, 11, 12, 13, 14, 15, 16, 17, 18};
    M3FsIndexImage image = M3FsIndexImage.of(values, starts, tokenIds);

    Path database = temp.resolve("semantic.sqlite");
    try (M3FS fs = M3FS.create(database)) {
      fs.createMIndexFile("/values.midx", 0644, image);

      M3FS.MIndexInfo info = fs.mindexInfo("/values.midx");
      assertEquals(values.size(), info.valueCount());
      assertEquals(image.tokenCount(), info.tokenCount());
      assertEquals(image.utf16Count(), info.utf16Count());
      assertEquals(image.byteSize(), info.imageBytes());

      M3FS.MIndexValueInfo second =
          fs.mindexValueInfo("/values.midx", 1);
      assertEquals(4, second.tokenCount());
      assertEquals(values.get(1).length(), second.utf16Length());
      assertEquals(values.get(1).hashCode(), second.javaHash());
      assertEquals(M3TestRegexImages.signal(values.get(1)), second.signal64());
      assertEquals('b', fs.mindexChar("/values.midx", 1, 2));
      assertTrue(fs.mindexCompare("/values.midx", 0, 1) < 0);

      long[] needle = {
        tokenIds[3],
        tokenIds[4]
      };
      assertEquals(1, fs.mindexTokenIndexOf("/values.midx", 1, needle, 0));

      M3FsRegexImage literal = M3TestRegexImages.compile("ab");
      M3FS.NativeRegexResult find =
          fs.nativeRegex("/values.midx", literal, M3FS.RegexMode.FIND);
      assertTrue(find.matches(0));
      assertTrue(find.matches(1));
      assertFalse(find.matches(2));
      assertEquals(0, find.fallbackCount());

      M3FsRegexImage dot = M3TestRegexImages.compile(".");
      M3FS.NativeRegexResult raw =
          fs.nativeRegex("/values.midx", dot, M3FS.RegexMode.FIND);
      assertTrue(raw.requiresFallback(3));

      M3FS.NativeRegexResult resolved =
          fs.nativeRegex("/values.midx", dot, M3FS.RegexMode.FIND, image);
      assertEquals(0, resolved.fallbackCount());
      assertTrue(resolved.matches(0));
      assertTrue(resolved.matches(1));
      assertTrue(resolved.matches(2));
      assertTrue(resolved.matches(3));
      assertFalse(resolved.matches(4));

      fs.verify();
    }

    try (M3FS snapshot = M3FS.openSnapshot(database)) {
      assertEquals(values.size(), snapshot.mindexInfo("/values.midx").valueCount());
      assertTrue(
          snapshot
              .nativeRegex(
                  "/values.midx",
                  M3TestRegexImages.compile("^ab$"),
                  M3FS.RegexMode.MATCHES)
              .matches(0));
    }
  }
}
