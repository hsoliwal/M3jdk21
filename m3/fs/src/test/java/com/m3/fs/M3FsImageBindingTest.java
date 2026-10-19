// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsImageBindingTest {
  @TempDir Path temp;
  @BeforeAll static void nativeRequired() {
    org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
    assertTrue(M3FS.nativeAvailable());
  }

  @Test void atomicImageReplacementPreservesHardlinksAndRetainedSnapshot() throws Exception {
    Path database = temp.resolve("images.sqlite");
    M3FsIndexImage old = M3FsIndexImage.of("old", "😀");
    M3FsIndexImage next = M3FsIndexImage.of("new and longer", "x");
    try (M3FS fs = M3FS.create(database)) {
      fs.createMIndexFile("/image", 0644, old); fs.link("/image", "/alias");
      M3FS.Stat before = fs.stat("/image");
      try (M3FS snapshot = M3FS.openSnapshot(database)) {
        assertEquals(next.byteSize(), fs.writeMIndexImage("/image", next));
        assertEquals(before.inode(), fs.stat("/image").inode());
        assertEquals(before.generation() + 1, fs.stat("/image").generation());
        assertEquals(next.byteSize(), fs.stat("/alias").size());
        assertEquals('n', fs.mindexChar("/alias", 0, 0));
        assertEquals('o', snapshot.mindexChar("/image", 0, 0));
        assertThrows(IOException.class, () -> snapshot.writeMIndexImage("/image", next));
      }
    }
  }

  @Test void fallbackRequiresExactImageBytesEvenForMatchingCountsOrNativeOnlyValues() throws Exception {
    try (M3FS fs = M3FS.create(temp.resolve("binding.sqlite"))) {
      M3FsIndexImage source = M3FsIndexImage.of("😀", "ab");
      fs.createMIndexFile("/image", 0644, source);
      assertThrows(IllegalArgumentException.class, () -> fs.nativeRegex("/image",
          M3TestRegexImages.compile("."), M3FS.RegexMode.MATCHES, M3FsIndexImage.of("x", "ab")));
      assertThrows(IllegalArgumentException.class, () -> fs.nativeRegex("/image",
          M3TestRegexImages.compile("ab"), M3FS.RegexMode.FIND, M3FsIndexImage.of("😀", "xx")));
    }
  }

  @Test void supplementaryFallbackPrecedesUtf16LengthAndPresencePrefiltersForEveryMode() throws Exception {
    try (M3FS fs = M3FS.create(temp.resolve("surrogate.sqlite"))) {
      M3FsIndexImage image = M3FsIndexImage.of("😀", "\ud800", "A", "", "\n", "\u0085", "\u2028");
      fs.createMIndexFile("/image", 0644, image);
      for (M3FS.RegexMode mode : M3FS.RegexMode.values()) {
        M3FS.NativeRegexResult raw = fs.nativeRegex("/image", M3TestRegexImages.compile("."), mode);
        assertTrue(raw.requiresFallback(0));
        M3FS.NativeRegexResult resolved = fs.nativeRegex("/image", M3TestRegexImages.compile("."), mode, image);
        assertEquals(0, resolved.fallbackCount());
        for (int i = 0; i < image.valueCount(); i++) {
          var matcher = java.util.regex.Pattern.compile(".").matcher(image.value(i));
          boolean expected = switch (mode) {
            case MATCHES -> matcher.matches(); case FIND -> matcher.find(); case LOOKING_AT -> matcher.lookingAt();
          };
          assertEquals(expected, resolved.matches(i), "value " + i + " mode " + mode);
        }
      }
    }
  }

  @Test void fallbackRunsAgainstPinnedSourceWhileAnotherOwnerReplacesTheLiveImage() throws Exception {
    Path database = temp.resolve("pinned.sqlite");
    try (M3FS fs = M3FS.create(database); M3FS writer = M3FS.open(database)) {
      M3FsIndexImage image = M3FsIndexImage.of("😀", "😃"); fs.createMIndexFile("/image", 0644, image);
      int[] calls = {0};
      M3FS.NativeRegexResult result = fs.nativeRegex("/image", M3TestRegexImages.compile("."),
          M3FS.RegexMode.MATCHES, image, (value, mode) -> {
            if (calls[0]++ == 0) {
              try { writer.writeMIndexImage("/image", M3FsIndexImage.of("a", "b")); }
              catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
            assertEquals(2, value.length());
            return true;
          });
      assertEquals(2, calls[0]); assertEquals(2, result.matchCount()); assertEquals(0, result.fallbackCount());
      assertEquals('a', fs.mindexChar("/image", 0, 0));
    }
  }
}
