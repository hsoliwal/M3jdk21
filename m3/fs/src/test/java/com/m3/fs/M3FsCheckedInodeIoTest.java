// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsCheckedInodeIoTest {
  @TempDir Path temp;
  @BeforeAll static void nativeRequired() {
    org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
    assertTrue(M3FS.nativeAvailable());
    assertTrue(M3FS.supportsCheckedInodeIo());
    assertTrue(M3FS.supportsRenameNoReplace());
  }

  @Test void secondOwnerReplacementRejectsReadsWritesAppendsAndTruncation() throws Exception {
    Path database = temp.resolve("stale.sqlite");
    try (M3FS first = M3FS.create(database); M3FS second = M3FS.open(database)) {
      first.createFile("/file", 0644); first.write("/file", 0, new byte[] {1, 2});
      long old = first.stat("/file").inode();
      try (M3FS snapshot = M3FS.openSnapshot(database)) {
        second.unlink("/file"); second.createFile("/file", 0600); second.write("/file", 0, new byte[] {8, 9});
        long replacement = second.stat("/file").inode();
        assertNotEquals(old, replacement);
        IOException failure = assertThrows(IOException.class,
            () -> first.readIfInode("/file", old, 0, new byte[0], 0, 0));
        assertTrue(failure.getMessage().contains("stale inode"));
        assertThrows(IOException.class, () -> first.writeIfInode("/file", old, 0, new byte[] {3}, 0, 1));
        assertThrows(IOException.class, () -> first.appendIfInode("/file", old, new byte[] {3}, 0, 1));
        assertThrows(IOException.class, () -> first.truncateIfInode("/file", old, 0));
        assertArrayEquals(new byte[] {8, 9}, second.read("/file", 0, 4));
        byte[] retained = new byte[2];
        assertEquals(2, snapshot.readIfInode("/file", old, 0, retained, 0, 2));
        assertArrayEquals(new byte[] {1, 2}, retained);
      }
    }
  }

  @Test void appendChoosesEofInTheSameTransactionAcrossOwners() throws Exception {
    Path database = temp.resolve("append.sqlite");
    try (M3FS first = M3FS.create(database); M3FS second = M3FS.open(database)) {
      first.createFile("/file", 0644); long inode = first.stat("/file").inode();
      CountDownLatch start = new CountDownLatch(1);
      try (var executor = Executors.newFixedThreadPool(2)) {
        var a = executor.submit(() -> append(first, inode, 1, start));
        var b = executor.submit(() -> append(second, inode, 2, start));
        start.countDown(); a.get(); b.get();
      }
      byte[] bytes = first.read("/file", 0, 1601); assertEquals(1600, bytes.length);
      int[] counts = new int[3];
      ByteBuffer input = ByteBuffer.wrap(bytes);
      while (input.hasRemaining()) {
        int owner = input.getInt(), ordinal = input.getInt();
        assertTrue(owner == 1 || owner == 2); assertEquals(counts[owner]++, ordinal);
      }
      assertArrayEquals(new int[] {0, 100, 100}, counts);
    }
  }

  private static int append(M3FS fs, long inode, int owner, CountDownLatch start) throws Exception {
    start.await();
    for (int i = 0; i < 100; i++) {
      byte[] frame = ByteBuffer.allocate(8).putInt(owner).putInt(i).array();
      fs.appendIfInode("/file", inode, frame, 0, frame.length);
    }
    return 100;
  }

  @Test void checkedTruncationSupportsArbitrarySizesAndNoOpGeneration() throws Exception {
    Path database = temp.resolve("truncate.sqlite");
    try (M3FS fs = M3FS.create(database)) {
      fs.createFile("/file", 0644); long inode = fs.stat("/file").inode();
      fs.truncateIfInode("/file", inode, 8);
      assertArrayEquals(new byte[8], fs.read("/file", 0, 20));
      long generation = fs.stat("/file").generation();
      fs.truncateIfInode("/file", inode, 8);
      fs.writeIfInode("/file", inode, 0, new byte[0], 0, 0);
      assertEquals(generation, fs.stat("/file").generation());
      assertEquals(5, fs.writeIfInode("/file", inode, 4, new byte[] {7}, 0, 1, M3FS.WRITE_TRUNCATE));
      assertArrayEquals(new byte[] {0, 0, 0, 0, 7}, fs.read("/file", 0, 20));
      assertThrows(IllegalArgumentException.class,
          () -> fs.writeIfInode("/file", inode, 0, new byte[0], 0, 0, 3));
      assertThrows(IllegalArgumentException.class,
          () -> fs.readIfInode("/file", 0, 0, new byte[0], 0, 0));
      try (M3FS readOnly = M3FS.openReadOnly(database)) {
        assertThrows(IOException.class, () -> readOnly.truncateIfInode("/file", inode, 0));
      }
    }
  }

  @Test void noReplaceRenamePreservesDestinationAndOverwriteRenameIsAtomic() throws Exception {
    try (M3FS fs = M3FS.create(temp.resolve("rename.sqlite"))) {
      fs.createFile("/a", 0644); fs.write("/a", 0, new byte[] {1});
      fs.createFile("/b", 0600); fs.write("/b", 0, new byte[] {2});
      M3FS.Stat a = fs.stat("/a"), b = fs.stat("/b");
      assertThrows(FileAlreadyExistsException.class, () -> fs.renameNoReplace("/a", "/b"));
      assertEquals(a, fs.stat("/a")); assertEquals(b, fs.stat("/b"));
      fs.renameNoReplace("/a", "/c"); assertEquals(a.inode(), fs.stat("/c").inode());
      fs.rename("/c", "/b"); assertEquals(a.inode(), fs.stat("/b").inode());
      assertArrayEquals(new byte[] {1}, fs.read("/b", 0, 10));
    }
  }
}
