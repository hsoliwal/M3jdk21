// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The extent-store scenarios of the private mllm repository (origin/main f616c321987b; the repository is named in
 * ORACLE_CLASSIFICATION.tsv) restated against {@link M3DurableExtentStore} wherever their semantics match the donor
 * contract. Every scenario of the Python tests
 * test_extent_store_append_rollback_reopen.py (0e0143c3936f) and test_extent_store_concurrency.py (e739ace09f2b) and
 * of the Java tests ExtentStoreTest (93405e13d88e) and NativeExtentStoreTest (9406dc6deae2) is classified in
 * recipe/durable-extent-store-20261107/ORACLE_CLASSIFICATION.tsv; the MATCH and PARTIAL rows are the tests below.
 * The oracle's commit point is an fsynced append-log commit marker; this store's commit point is the atomic rename of
 * a complete sealed index, so torn-tail quarantine and rollback counters have no counterpart here.
 */
//FLLDCIM: ORACLE - mllm extent-store scenarios as JUnit over the relocated store; recipe m3/file/recipe/durable-extent-store-20261107
class M3DurableExtentStoreOracleTest {
  private static final String SOURCE = "2".repeat(64);
  @TempDir Path directory;

  private static M3ContentExtent extent(byte[] bytes) {
    return M3ContentExtent.fromBytes("oracle-source", SOURCE, 0, bytes, "raw");
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private static M3ContentExtent put(M3DurableExtentStore store, String key, String text) throws IOException {
    byte[] bytes = utf8(text);
    return store.write(key, extent(bytes), bytes);
  }

  private static long pendingIndexFiles(Path root) throws IOException {
    try (Stream<Path> listing = Files.list(root)) {
      return listing.filter(path -> path.getFileName().toString().startsWith(".m3-index-")).count();
    }
  }

  private static Path blob(Path root, M3ContentExtent extent) {
    return root.resolve("blobs").resolve(extent.contentSha256() + ".bin");
  }

  /** py test_failed_append_with_failed_rollback_preserves_reopenable_committed_prefix: MATCH (visibility). */
  @Test void failedPublishKeepsTheCommittedPrefixAcrossReopen() throws Exception {
    Path root = directory.resolve("failed-append");
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      put(store, "committed", "stable");
    }
    byte[] committedIndex = Files.readAllBytes(root.resolve("index.m3"));
    byte[] payload = utf8("payload");
    try (M3DurableExtentStore failing = M3DurableExtentStore.open(root, () -> {
      throw new IOException("injected index publication failure");
    })) {
      assertThrows(IOException.class, () -> failing.write("uncommitted", extent(payload), payload));
      assertArrayEquals(utf8("stable"), failing.read("committed"));
      assertNull(failing.read("uncommitted"));
      assertNull(failing.metadata("uncommitted"));
    }
    assertArrayEquals(committedIndex, Files.readAllBytes(root.resolve("index.m3")));
    assertEquals(0, pendingIndexFiles(root));
    assertTrue(Files.isRegularFile(blob(root, extent(payload))), "an interrupted put may leave an unreachable blob");
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(utf8("stable"), reopened.read("committed"));
      assertNull(reopened.read("uncommitted"));
      assertEquals(List.of("committed"), reopened.keys("", 0));
      reopened.verifyAll();
    }
  }

  /** py test_complete_row_with_failed_fsync_and_rollback_is_not_replayed_as_committed: MATCH. */
  @Test void completeForcedButUnpublishedIndexIsNotReplayed() throws Exception {
    Path root = directory.resolve("complete-row");
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      put(store, "committed", "stable");
    }
    String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(utf8("uncertain"));
    List<String> staged = new ArrayList<>();
    byte[] payload = utf8("complete-row");
    try (M3DurableExtentStore failing = M3DurableExtentStore.open(root, () -> {
      try (Stream<Path> listing = Files.list(root)) {
        for (Path pending : listing.filter(path -> path.getFileName().toString().startsWith(".m3-index-")).toList()) {
          staged.add(Files.readString(pending, StandardCharsets.UTF_8));
        }
      }
      throw new IOException("injected failure after the complete index was forced");
    })) {
      assertThrows(IOException.class, () -> failing.write("uncertain", extent(payload), payload));
    }
    assertEquals(1, staged.size());
    assertTrue(staged.get(0).startsWith("M3EXTIDX1\n") && staged.get(0).contains(encodedKey + "\t"));
    assertTrue(staged.get(0).contains("\n#sha256:"), "the unpublished index was complete and sealed");
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(utf8("stable"), reopened.read("committed"));
      assertNull(reopened.read("uncertain"));
    }
  }

  /**
   * py test_failed_commit_marker_sync_with_failed_rollback_replays_as_open_tail: DIFFERENT by contract. The oracle
   * un-commits a row whose directory sync failed; here the rename is the commit point and a directory sync that the
   * platform refuses (always on Windows, where a directory cannot be opened as a FileChannel) is ignored, so the put
   * stays visible after reopen.
   */
  @Test void theRenameIsTheCommitPointNotTheDirectorySync() throws Exception {
    Path root = directory.resolve("commit-point");
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      put(store, "committed", "stable");
      put(store, "after-rename", "visible");
    }
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(utf8("visible"), reopened.read("after-rename"));
    }
  }

  /** py test_reopen_rejects_missing_committed_current_extent_without_recreating_it: PARTIAL (refused on read). */
  @Test void missingCommittedBlobIsRefusedOnReadAndNeverRecreated() throws Exception {
    Path root = directory.resolve("missing-current");
    M3ContentExtent committed;
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      committed = put(store, "committed", "original-payload");
    }
    Files.delete(blob(root, committed));
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      IOException missing = assertThrows(IOException.class, () -> reopened.read("committed"));
      assertTrue(missing.getMessage().startsWith("missing content blob"));
      assertThrows(IOException.class, reopened::verifyAll);
      assertEquals(committed, reopened.metadata("committed"));
    }
    assertFalse(Files.exists(blob(root, committed)));
  }

  /** py test_reopen_rejects_truncated_committed_extent_without_extending_it: MATCH on read. */
  @Test void truncatedCommittedBlobIsRefusedAndNotExtended() throws Exception {
    Path root = directory.resolve("truncated-current");
    M3ContentExtent committed;
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      committed = put(store, "committed", "original-payload");
    }
    byte[] whole = Files.readAllBytes(blob(root, committed));
    Files.write(blob(root, committed), java.util.Arrays.copyOf(whole, 8));
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      IOException truncated = assertThrows(IOException.class, () -> reopened.read("committed"));
      assertEquals("truncated or extended extent blob", truncated.getMessage());
    }
    assertEquals(8, Files.size(blob(root, committed)));
  }

  /** py test_read_rejects_missing_committed_historical_extent_without_recreating_it: MATCH. */
  @Test void missingHistoricalBlobIsRefusedWhileTheOthersStillRead() throws Exception {
    Path root = directory.resolve("missing-historical");
    M3ContentExtent first;
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      first = put(store, "first", "first-payload");
      put(store, "second", "second-payload");
      put(store, "third", "third-payload");
    }
    Files.delete(blob(root, first));
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertThrows(IOException.class, () -> reopened.read("first"));
      assertArrayEquals(utf8("second-payload"), reopened.read("second"));
      assertArrayEquals(utf8("third-payload"), reopened.read("third"));
    }
    assertFalse(Files.exists(blob(root, first)));
  }

  /** py test_extent_index_allows_only_one_live_writer: MATCH. */
  @Test void onlyOneLiveWriterAndARecoveredWriterAfterClose() throws Exception {
    Path root = directory.resolve("single-writer");
    M3DurableExtentStore first = M3DurableExtentStore.open(root);
    IOException second = assertThrows(IOException.class, () -> M3DurableExtentStore.open(root));
    assertEquals("extent store already has a writer", second.getMessage());
    first.close();
    try (M3DurableExtentStore recovered = M3DurableExtentStore.open(root)) {
      put(recovered, "after-close", "safe");
      assertArrayEquals(utf8("safe"), recovered.read("after-close"));
    }
  }

  /** py test_concurrent_writes_on_one_store_serialize_index_appends: MATCH (publication is serialized). */
  @Test void concurrentWritesSerializeIndexPublication() throws Exception {
    Path root = directory.resolve("threaded-writes");
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maxActive = new AtomicInteger();
    int writers = 8;
    CyclicBarrier start = new CyclicBarrier(writers);
    ExecutorService executor = Executors.newFixedThreadPool(writers);
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root, () -> {
      maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
      try {
        Thread.sleep(20);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IOException("interrupted", interrupted);
      } finally {
        active.decrementAndGet();
      }
    })) {
      List<Callable<M3ContentExtent>> tasks = new ArrayList<>();
      for (int i = 0; i < writers; i++) {
        String key = "item-" + i;
        String text = "payload-" + i;
        tasks.add(() -> {
          start.await(5, TimeUnit.SECONDS);
          return put(store, key, text);
        });
      }
      for (Future<M3ContentExtent> done : executor.invokeAll(tasks)) done.get();
    } finally {
      executor.shutdownNow();
    }
    assertEquals(1, maxActive.get());
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      for (int i = 0; i < writers; i++) assertArrayEquals(utf8("payload-" + i), reopened.read("item-" + i));
    }
  }

  /** Java ExtentStoreTest.invalidSupersedeRetainsOriginalRecordAndReplaysAfterReopen: MATCH. */
  @Test void invalidSupersedeRetainsTheOriginalAcrossReopen() throws Exception {
    Path root = directory.resolve("supersede-fail-closed");
    byte[] original = utf8("original");
    M3ContentExtent before;
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      before = store.write("k", extent(original), original);
      assertThrows(NullPointerException.class, () -> store.supersede("k", extent(original), null));
      assertThrows(M3ContentExtent.ContentTamperedException.class,
          () -> store.supersede("k", extent(utf8("other")), utf8("wrong")));
      assertArrayEquals(original, store.read("k"));
      assertEquals(before, store.metadata("k"));
    }
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(original, reopened.read("k"));
    }
  }

  /** Java ExtentStoreTest.committedSupersedeRemainsTheLatestValueAfterReopen: MATCH. */
  @Test void committedSupersedeIsTheLatestValueAfterReopen() throws Exception {
    Path root = directory.resolve("supersede-durable");
    byte[] replacement = utf8("replacement");
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      put(store, "k", "original");
      store.supersede("k", extent(replacement), replacement);
      assertArrayEquals(replacement, store.read("k"));
    }
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(replacement, reopened.read("k"));
      assertEquals(extent(replacement), reopened.metadata("k"));
    }
  }

  /** Java ExtentStoreTest.rejectsInvalidWritesAndMissingReads and emptyPayload...: MATCH (no page allocator). */
  @Test void rejectsInvalidWritesMissingReadsAndKeepsEmptyPayloads() throws Exception {
    Path root = directory.resolve("invalid");
    try (M3DurableExtentStore store = M3DurableExtentStore.open(root)) {
      assertThrows(IllegalArgumentException.class, () -> store.write("", extent(new byte[0]), new byte[0]));
      assertThrows(NullPointerException.class, () -> store.write("null", extent(new byte[0]), null));
      put(store, "dup", "first");
      IllegalStateException duplicate = assertThrows(IllegalStateException.class, () -> put(store, "dup", "second"));
      assertTrue(duplicate.getMessage().contains("immutable"));
      assertNull(store.read("missing"));
      assertNull(store.metadata("missing"));
      store.write("empty", extent(new byte[0]), new byte[0]);
      assertArrayEquals(new byte[0], store.read("empty"));
    }
    try (M3DurableExtentStore reopened = M3DurableExtentStore.open(root)) {
      assertArrayEquals(new byte[0], reopened.read("empty"));
      assertEquals(0L, reopened.metadata("empty").byteLength());
    }
  }

  /** Java ExtentStoreTest.keysAreSortedFilteredAndLimited: MATCH (limit 0 means unlimited). */
  @Test void keysAreSortedFilteredAndLimited() throws Exception {
    try (M3DurableExtentStore store = M3DurableExtentStore.open(directory.resolve("keys"))) {
      put(store, "c-key", "3");
      put(store, "a-key", "1");
      put(store, "b-key", "2");
      put(store, "ns/a", "4");
      put(store, "ns/b", "5");
      assertEquals(List.of("a-key", "b-key", "c-key", "ns/a", "ns/b"), store.keys("", 0));
      assertEquals(List.of("ns/a", "ns/b"), store.keys("ns/", 1000));
      assertEquals(List.of("a-key", "b-key"), store.keys("", 2));
      assertEquals(5, store.keys("", 0).size());
    }
  }

  /** Java ExtentStoreTest.reloadReplaysTheAppendOnlyIndexAndAllocator: MATCH (reopen replays the sealed index). */
  @Test void reopenReplaysEveryPublishedKey() throws Exception {
    Path root = directory.resolve("reload");
    try (M3DurableExtentStore first = M3DurableExtentStore.open(root)) {
      for (int i = 0; i < 50; i++) put(first, "persist/k" + i, "persistent data " + i);
    }
    try (M3DurableExtentStore second = M3DurableExtentStore.open(root)) {
      assertEquals(50, second.keys("persist/", 0).size());
      for (int i = 0; i < 50; i++) assertArrayEquals(utf8("persistent data " + i), second.read("persist/k" + i));
      second.verifyAll();
    }
  }

  /** Java ExtentStoreTest.indexHeaderCorruptionFailsClosed: MATCH (an IOException, not IllegalStateException). */
  @Test void indexHeaderCorruptionFailsClosed() throws Exception {
    Path root = directory.resolve("corrupt");
    M3DurableExtentStore.open(root).close();
    Files.write(root.resolve("index.m3"), "bad-header".getBytes(StandardCharsets.US_ASCII));
    IOException refused = assertThrows(IOException.class, () -> M3DurableExtentStore.open(root));
    assertEquals("malformed extent index header or seal", refused.getMessage());
  }
}
