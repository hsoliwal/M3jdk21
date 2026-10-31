// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3HeadPromotionDurabilityTest {
  @TempDir Path temp;

  @Test
  void casSurvivesReopenAndStaleExpectedDoesNotChangeHead() throws Exception {
    Path root = temp.resolve("store");
    M3FileSystemStore store = new M3FileSystemStore(root);
    M3GenerationRoot first = M3GenerationRoot.empty();
    M3ObjectRef one = store.publishGeneration(first);
    store.promote(Optional.empty(), one);
    assertEquals(one, new M3FileSystemStore(root).head().orElseThrow());
    M3GenerationRoot next = first.put("/x", M3ContentId.digest(new byte[] {1}));
    M3ObjectRef two = store.publishGeneration(next);
    assertThrows(IOException.class, () -> store.promote(Optional.empty(), two));
    assertEquals(one, store.head().orElseThrow());
    store.promote(Optional.of(one.id()), two);
    assertEquals(two.id(), new M3FileSystemStore(root).headGeneration().orElseThrow().id());
  }

  @Test
  void unsupportedAtomicMoveFailsWithoutReplacingExistingHead() throws Exception {
    Path root = temp.resolve("store");
    M3FileSystemStore good = new M3FileSystemStore(root);
    M3GenerationRoot first = M3GenerationRoot.empty();
    M3ObjectRef one = good.publishGeneration(first);
    good.promote(Optional.empty(), one);
    M3ObjectRef two = good.publishGeneration(first.put("/x", M3ContentId.digest(new byte[] {2})));
    M3FileSystemStore refusing = new M3FileSystemStore(root, (from, to) -> {
      assertEquals(from.getParent(), to.getParent());
      assertTrue(Files.size(from) > 0);
      throw new AtomicMoveNotSupportedException(from.toString(), to.toString(), "injected refusal");
    });
    assertThrows(AtomicMoveNotSupportedException.class,
        () -> refusing.promote(Optional.of(one.id()), two));
    assertEquals(one, good.head().orElseThrow());
    try (var files = Files.list(root)) {
      assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".head-")));
    }
  }

  @Test
  void independentOwnersRaceThroughOneCas() throws Exception {
    Path root = temp.resolve("store");
    M3FileSystemStore left = new M3FileSystemStore(root), right = new M3FileSystemStore(root);
    M3GenerationRoot first = M3GenerationRoot.empty();
    M3ObjectRef one = left.publishGeneration(first);
    left.promote(Optional.empty(), one);
    M3ObjectRef a = left.publishGeneration(first.put("/a", M3ContentId.digest(new byte[] {3})));
    M3ObjectRef b = right.publishGeneration(first.put("/b", M3ContentId.digest(new byte[] {4})));
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var ar = executor.submit(() -> attempt(left, one.id(), a, start));
      var br = executor.submit(() -> attempt(right, one.id(), b, start));
      start.countDown();
      assertNotEquals(ar.get(), br.get());
    }
    M3ObjectRef actual = new M3FileSystemStore(root).head().orElseThrow();
    assertTrue(actual.equals(a) || actual.equals(b));
  }

  private static boolean attempt(M3FileSystemStore store, M3ContentId expected, M3ObjectRef next,
      CountDownLatch start) throws Exception {
    start.await();
    try { store.promote(Optional.of(expected), next); return true; }
    catch (IOException rejected) {
      assertTrue(rejected.getMessage().contains("compare-and-swap"), rejected.toString());
      return false;
    }
  }

  @Test
  void symlinkHeadAndLockFailClosed() throws Exception {
    Path root = temp.resolve("store");
    M3FileSystemStore store = new M3FileSystemStore(root);
    M3ObjectRef generation = store.publishGeneration(M3GenerationRoot.empty());
    Path outside = temp.resolve("outside"); Files.writeString(outside, generation.id().hex());
    Files.createSymbolicLink(root.resolve("HEAD"), outside);
    assertThrows(IOException.class, store::head);
    Files.delete(root.resolve("HEAD"));
    Files.createSymbolicLink(root.resolve("HEAD.lock"), outside);
    assertThrows(IOException.class, () -> store.promote(Optional.empty(), generation));
    assertEquals(generation.id().hex(), Files.readString(outside));
  }
}
