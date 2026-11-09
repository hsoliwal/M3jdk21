// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-storage-sdk/src/test/java/com/synexia/storage/DurableExtentStoreTest.java blob 8477678bdd7e; recipe m3/file/recipe/durable-extent-store-20261107/relocate.py v1
class M3DurableExtentStoreTest {
  private static final String SOURCE = "1".repeat(64);
  @TempDir Path directory;

  private M3ContentExtent extent(long start, byte[] bytes) {
    return M3ContentExtent.fromBytes("src-id", SOURCE, start, bytes, "raw");
  }

  @Test void persistedRoundtripAndIndependentArraySnapshot() throws Exception {
    Path root = directory.resolve("roundtrip");
    byte[] bytes = "hello persistent extent".getBytes(StandardCharsets.UTF_8);
    M3ContentExtent expected = extent(4, bytes);
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      assertNull(store.read("missing"));
      assertEquals(expected, store.write("ns/z", expected, bytes));
      store.write("ns/a", extent(0, new byte[0]), new byte[0]);
      assertEquals(List.of("ns/a", "ns/z"), store.keys("ns/", 0));
      assertEquals(List.of("ns/a"), store.keys("ns/", 1));
      byte[] returned = store.read("ns/z");
      assertArrayEquals(bytes, returned);
      returned[0] ^= 0x40;
      assertArrayEquals(bytes, store.read("ns/z"));
      store.verifyAll();
      assertEquals(2, store.manifest().extents().size());
    }
    byte[] index = Files.readAllBytes(root.resolve("index.m3"));
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertEquals(expected, reopened.metadata("ns/z"));
      assertArrayEquals(bytes, reopened.read("ns/z"));
    }
    assertArrayEquals(index, Files.readAllBytes(root.resolve("index.m3")));
  }

  @Test void supersedeFailureDoesNotDropOldEntryEvenAfterReopen() throws Exception {
    Path root = directory.resolve("failed-supersede");
    byte[] old = "original".getBytes(StandardCharsets.UTF_8);
    byte[] replacement = "replacement".getBytes(StandardCharsets.UTF_8);
    M3ContentExtent first = extent(0, old);
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      store.write("key", first, old);
    }
    try (M3DurableExtentStore failed = M3DurableExtentStore.open(root, () -> {
      throw new IOException("synthetic failure immediately before commit");
    })) {
      assertThrows(IOException.class, () -> failed.supersede("key", extent(0, replacement), replacement));
      assertEquals(first, failed.metadata("key"));
      assertArrayEquals(old, failed.read("key"));
    }
    try (M3DurableExtentStore recovered = M3DurableExtentStore.open(root)) {
      assertEquals(first, recovered.metadata("key"));
      assertArrayEquals(old, recovered.read("key"));
      recovered.supersede("key", extent(0, replacement), replacement);
      assertArrayEquals(replacement, recovered.read("key"));
    }
  }

  @Test void invalidPayloadAndDuplicateKeysNeverPublish() throws Exception {
    Path root = directory.resolve("invalid");
    byte[] bytes = new byte[]{1, 2};
    M3ContentExtent original = extent(0, bytes);
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      assertThrows(M3ContentExtent.ContentTamperedException.class,
          () -> store.write("key", original, new byte[]{1, 3}));
      assertNull(store.metadata("key"));
      store.write("key", original, bytes);
      assertThrows(IllegalStateException.class,
          () -> store.write("key", original, bytes));
      assertThrows(IllegalStateException.class,
          () -> store.supersede("missing", original, bytes));
      assertThrows(IllegalArgumentException.class,
          () -> store.write("", original, bytes));
      assertEquals(List.of("key"), store.keys("", 0));
    }
  }

  @Test void corruptOrTruncatedContentFailsClosed() throws Exception {
    Path root = directory.resolve("corrupt-blob");
    byte[] bytes = new byte[]{1, 2, 3};
    M3ContentExtent e = extent(0, bytes);
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      store.write("key", e, bytes);
    }
    Path blob = root.resolve("blobs").resolve(e.contentSha256() + ".bin");
    Files.write(blob, new byte[]{1, 2});
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      assertThrows(IOException.class, () -> store.read("key"));
      assertThrows(IOException.class, store::verifyAll);
      assertThrows(IOException.class, () -> store.supersede("key", e, bytes));
    }
    Files.write(blob, new byte[]{1, 2, 4});
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      assertThrows(IOException.class, () -> store.read("key"));
    }
  }

  @Test void indexCorruptionAndConcurrentWriterAreRejected() throws Exception {
    Path root = directory.resolve("index-corrupt");
    try (M3DurableExtentStore first = M3DurableExtentStore.open(root)) {
      assertThrows(IOException.class, () -> M3DurableExtentStore.open(root));
      byte[] bytes = new byte[]{7};
      first.write("k", extent(0, bytes), bytes);
    }
    Files.writeString(root.resolve("index.m3"), "M3EXTIDX1\n#sha256:bad\n");
    assertThrows(IOException.class, () -> M3DurableExtentStore.open(root));
  }

  @Test void optionsAndPostCloseAccessAreChecked() throws Exception {
    Path root = directory.resolve("options");
    M3DurableExtentStore store = M3DurableExtentStore.open(root);
    assertNotNull(store);
    store.close();
    assertThrows(IOException.class, () -> store.read("k"));
    assertFalse(Files.size(root.resolve("index.m3")) == 0);
    assertTrue(Files.exists(root.resolve("writer.lock")));
  }
}
