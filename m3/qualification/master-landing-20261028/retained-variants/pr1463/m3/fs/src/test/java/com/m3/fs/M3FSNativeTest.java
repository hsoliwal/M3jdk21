// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FSNativeTest {
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
  void nativeCoreJniAndJavaFacadePreserveFilesystemSemantics() throws Exception {
    Path database = temp.resolve("mindexfs.sqlite");

    try (M3FS fs = M3FS.create(database)) {
      assertTrue(fs.isOpen());
      assertFalse(fs.isReadOnly());
      fs.mkdir("/src😀", 0755);
      fs.createFile("/src😀/a", 0644);
      fs.createFile("/src😀/b", 0644);

      byte[] block = new byte[65_536];
      Arrays.fill(block, (byte) 'Q');
      assertEquals(65_536L, fs.write("/src😀/a", 0L, block));
      assertEquals(65_536L, fs.write("/src😀/b", 0L, block));

      M3FS.Stats dedup = fs.stats();
      assertEquals(1L, dedup.uniqueBlocks());
      assertEquals(2L, dedup.blockReferences());
      assertEquals(65_536L, dedup.physicalBlockBytes());
      assertEquals(65_536L, dedup.deduplicatedBlockBytes());

      fs.createFile("/src😀/sparse", 0644);
      ByteBuffer directWrite = ByteBuffer.allocateDirect(3);
      directWrite.put(new byte[] {'X', 'Y', 'Z'}).flip();
      assertEquals(
          65_544L,
          fs.write("/src😀/sparse", 65_541L, directWrite));
      assertEquals(3, directWrite.position());

      ByteBuffer directRead = ByteBuffer.allocateDirect(16);
      assertEquals(8, fs.read("/src😀/sparse", 65_536L, directRead));
      assertEquals(8, directRead.position());
      directRead.flip();
      byte[] sparse = new byte[8];
      directRead.get(sparse);
      assertArrayEquals(new byte[] {0, 0, 0, 0, 0, 'X', 'Y', 'Z'}, sparse);


      fs.createFile("/src😀/vector", 0644);
      ByteBuffer first = ByteBuffer.allocateDirect(4);
      first.put(new byte[] {9, 'A', 'B', 9}).position(1).limit(3);
      ByteBuffer secondBacking = ByteBuffer.allocateDirect(3);
      secondBacking.put(new byte[] {'C', 'D', 'E'}).flip();
      ByteBuffer second = secondBacking.asReadOnlyBuffer();
      int firstPosition = first.position();
      int firstLimit = first.limit();
      int secondPosition = second.position();
      int secondLimit = second.limit();
      long initialGeneration = fs.stat("/src😀/vector").generation();
      assertTrue(M3FS.supportsScatterGatherWrite());
      assertEquals(
          65_539L,
          fs.writeSegments("/src😀/vector", 65_534L, first, ByteBuffer.allocateDirect(0), second));
      assertEquals(firstPosition, first.position());
      assertEquals(firstLimit, first.limit());
      assertEquals(secondPosition, second.position());
      assertEquals(secondLimit, second.limit());
      assertEquals(initialGeneration + 1L, fs.stat("/src😀/vector").generation());
      assertArrayEquals(
          new byte[] {0, 0, 'A', 'B', 'C', 'D', 'E'},
          fs.read("/src😀/vector", 65_532L, 16));

      assertTrue(M3FS.supportsScatterGatherRead());
      ByteBuffer readHead = ByteBuffer.allocateDirect(4);
      readHead.put(new byte[] {9, 9, 9, 9}).position(1).limit(3);
      ByteBuffer readTail = ByteBuffer.allocateDirect(7);
      readTail.put(new byte[] {9, 9, 9, 9, 9, 9, 9}).position(1).limit(6);
      int headPosition = readHead.position();
      int headLimit = readHead.limit();
      int tailPosition = readTail.position();
      int tailLimit = readTail.limit();
      assertEquals(
          7L,
          fs.readSegments(
              "/src😀/vector", 65_532L, readHead, ByteBuffer.allocateDirect(0), readTail));
      assertEquals(headPosition, readHead.position());
      assertEquals(headLimit, readHead.limit());
      assertEquals(tailPosition, readTail.position());
      assertEquals(tailLimit, readTail.limit());
      assertEquals(0, readHead.get(1));
      assertEquals(0, readHead.get(2));
      assertEquals('A', readTail.get(1));
      assertEquals('B', readTail.get(2));
      assertEquals('C', readTail.get(3));
      assertEquals('D', readTail.get(4));
      assertEquals('E', readTail.get(5));

      ByteBuffer eofHead = ByteBuffer.allocateDirect(1);
      ByteBuffer eofTail = ByteBuffer.allocateDirect(4);
      eofTail.put(new byte[] {9, 9, 9, 9}).clear();
      assertEquals(2L, fs.readSegments("/src😀/vector", 65_537L, eofHead, eofTail));
      assertEquals('D', eofHead.get(0));
      assertEquals('E', eofTail.get(0));
      assertEquals(9, eofTail.get(1));
      assertEquals(9, eofTail.get(2));
      assertEquals(9, eofTail.get(3));
      assertEquals(0L, fs.readSegments("/src😀/vector", 0L));
      assertThrows(
          java.nio.ReadOnlyBufferException.class,
          () -> fs.readSegments(
              "/src😀/vector", 0L, ByteBuffer.allocateDirect(1).asReadOnlyBuffer()));
      assertThrows(
          IllegalArgumentException.class,
          () -> fs.readSegments("/src😀/vector", 0L, ByteBuffer.wrap(new byte[1])));

      long vectorGeneration = fs.stat("/src😀/vector").generation();
      assertThrows(
          IllegalArgumentException.class,
          () -> fs.writeSegments(
              "/src😀/vector", 0L, ByteBuffer.allocateDirect(1), ByteBuffer.wrap(new byte[] {1})));
      assertEquals(vectorGeneration, fs.stat("/src😀/vector").generation());
      assertEquals(65_539L, fs.writeSegments("/src😀/vector", 0L));
      assertEquals(vectorGeneration, fs.stat("/src😀/vector").generation());

      fs.link("/src😀/a", "/src😀/a2");
      assertEquals(2, fs.stat("/src😀/a2").linkCount());
      fs.rename("/src😀/a", "/src😀/a2");
      assertEquals(2, fs.stat("/src😀/a").linkCount());
      assertEquals(2, fs.stat("/src😀/a2").linkCount());

      fs.symlink("../src😀/a", "/src😀/a-link");
      assertEquals("../src😀/a", fs.readlink("/src😀/a-link"));

      List<String> names = fs.list("/src😀").stream().map(M3FS.DirEntry::name).toList();
      assertTrue(names.containsAll(List.of("a", "a2", "a-link", "b", "sparse")));

      fs.rename("/src😀/b", "/src😀/c");
      assertThrows(NoSuchFileException.class, () -> fs.stat("/src😀/b"));
      assertEquals(M3FS.Kind.FILE, fs.stat("/src😀/c").kind());

      fs.truncate("/src😀/a", 3L);
      assertArrayEquals(new byte[] {'Q', 'Q', 'Q'}, fs.read("/src😀/a2", 0L, 16));
      fs.truncate("/src😀/a", 10L);
      assertArrayEquals(
          new byte[] {'Q', 'Q', 'Q', 0, 0, 0, 0, 0, 0, 0},
          fs.read("/src😀/a", 0L, 16));

      assertThrows(IOException.class, () -> fs.rmdir("/src😀"));
      assertThrows(IllegalArgumentException.class, () -> fs.createFile("/src😀/../bad", 0644));

      fs.verify();
      fs.checkpoint();
    }

    try (M3FS reopened = M3FS.open(database)) {
      reopened.verify();
      assertArrayEquals(
          new byte[] {'Q', 'Q', 'Q', 0, 0, 0, 0, 0, 0, 0},
          reopened.read("/src😀/a", 0L, 16));
    }

    try (M3FS readOnly = M3FS.openReadOnly(database)) {
      assertTrue(readOnly.isReadOnly());
      assertThrows(IOException.class, () -> readOnly.write("/src😀/a", 0L, new byte[] {1}));
    }
  }

  @Test
  void immutableDirectSegmentsWriteThroughScatterGather() throws Exception {
    Path database = temp.resolve("joined-native.sqlite");
    try (M3FS fs = M3FS.create(database)) {
      fs.createFile("/joined", 0644);
      ByteBuffer[] joined = {
        ByteBuffer.allocateDirect(2).put(new byte[] {'A', 'B'}).flip().asReadOnlyBuffer(),
        ByteBuffer.allocateDirect(1).put((byte) 'C').flip().asReadOnlyBuffer(),
        ByteBuffer.allocateDirect(2).put(new byte[] {'D', 'E'}).flip().asReadOnlyBuffer()
      };
      assertEquals(5L, fs.writeSegments("/joined", 0L, joined));
      assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E'}, fs.read("/joined", 0L, 16));
      assertEquals(3, joined.length);
      for (ByteBuffer segment : joined) assertEquals(0, segment.position());
    }
  }

  @Test
  void lifecycleAndBoundsFailClosed() throws Exception {
    Path database = temp.resolve("lifecycle.sqlite");
    M3FS fs = M3FS.create(database);
    fs.createFile("/x", 0644);

    assertThrows(IllegalArgumentException.class, () -> fs.read("/x", 0L, -1));
    assertThrows(IllegalArgumentException.class, () -> fs.write("/x", -1L, new byte[] {1}));

    fs.close();
    fs.close();
    assertFalse(fs.isOpen());
    assertThrows(IllegalStateException.class, () -> fs.stat("/x"));
  }
}
