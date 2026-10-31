// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Actual Java -> repository loader -> JNI -> SQLite retained-view and compatibility checks. */
public final class M3FsSnapshotViewSelfTest {
  private static final long SEED = 20260928L;
  private int checks;

  private M3FsSnapshotViewSelfTest() {}

  public static void main(String[] args) throws Exception {
    M3FsSnapshotViewSelfTest suite = new M3FsSnapshotViewSelfTest();
    Path temp = Files.createTempDirectory("mindexfs-view-proof-");
    boolean legacy = args.length != 0 && args[0].equals("legacy");
    try {
      if (legacy) {
        suite.legacy(temp.resolve("legacy.sqlite"));
      } else {
        suite.views(temp.resolve("views.sqlite"));
        suite.concurrent(temp.resolve("concurrent.sqlite"));
      }
      System.out.println("PASS retained-java checks=" + suite.checks + " seed=" + SEED
          + " lane=" + (legacy ? "legacy-native" : "current") + " real-loader-jni-sqlite=YES");
    } finally {
      try (var paths = Files.walk(temp)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
  }

  private void legacy(Path db) throws Exception {
    check(M3FS.nativeAvailable(), "legacy native load");
    check(!M3FS.supportsReadSnapshots(), "legacy snapshot bit absent");
    try (var writer = M3FS.create(db)) {
      check(!writer.isSnapshot() && !writer.isReadOnly(), "live writer");
      writer.createFile("/value", 0600);
      writer.write("/value", 0, new byte[] {1, 2, 3});
      check(Arrays.equals(writer.read("/value", 0, 9), new byte[] {1, 2, 3}), "legacy read");
      for (int i = 0; i < 80; i++) reject(UnsupportedOperationException.class,
          () -> { try (var ignored = M3FS.openSnapshot(db)) { ignored.isOpen(); } });
      try (var live = M3FS.openReadOnly(db)) {
        check(!live.isSnapshot() && live.isReadOnly(), "legacy live reader");
        writer.write("/value", 0, new byte[] {4});
        check(live.read("/value", 0, 1)[0] == 4, "legacy live refresh");
      }
      writer.cloneFile("/value", "/copy");
      check(writer.listPage("/", null, 1).hasMore(), "prior extensions preserved");
      writer.verify();
    }
  }

  private void views(Path db) throws Exception {
    check(M3FS.supportsReadSnapshots(), "retained capability");
    byte[] original = new byte[3 * 65536 + 19]; new Random(SEED).nextBytes(original);
    try (var writer = M3FS.create(db)) {
      check(!writer.isSnapshot(), "ordinary writer unchanged");
      writer.mkdir("/data", 0755); writer.createFile("/data/image", 0640);
      writer.write("/data/image", 0, original); writer.symlink("/data/image", "/data/link");
      for (int i = 0; i < 40; i++) writer.createFile("/data/n%03d".formatted(i), 0600);
      for (String name : List.of("é", "भारत", "\uE000", "😀")) writer.createFile("/data/" + name, 0600);
      var entries = writer.list("/data"); var stat = writer.stat("/data/image"); var stats = writer.stats();
      try (var first = M3FS.openSnapshot(db); var live = M3FS.openReadOnly(db)) {
        // A writer commits before any user read on first. The pinned schema read must suffice.
        writer.write("/data/image", 0, new byte[] {42, 43, 44});
        check(first.isSnapshot() && first.isReadOnly() && first.isOpen(), "view flags");
        check(!live.isSnapshot() && live.isReadOnly(), "live reader flags");
        check(first.stat("/data/image").equals(stat), "eager original metadata");
        check(first.stats().equals(stats), "eager original statistics");
        check(Arrays.equals(first.read("/data/image", 0, original.length + 11), original), "eager content");
        check(live.read("/data/image", 0, 1)[0] == 42, "live reader sees commit");
        try (var second = M3FS.openSnapshot(db)) {
          writer.unlink("/data/image"); writer.unlink("/data/link");
          check(writer.stats().uniqueBlocks() == 0, "payload logically garbage collected");
          check(first.read("/data/image", 0, 1)[0] == original[0], "first view survives GC");
          check(second.read("/data/image", 0, 1)[0] == 42, "second view has own admission");
          reject(IOException.class, () -> live.stat("/data/image"));
          check(first.readlink("/data/link").equals("/data/image"), "symlink survives unlink");
          byte[] middle = new byte[701];
          check(first.read("/data/image", 65530, middle, 5, 690) == 690, "array offset read");
          check(Arrays.equals(Arrays.copyOfRange(middle, 5, 695), Arrays.copyOfRange(original, 65530, 66220)),
              "cross-block bytes");
          ByteBuffer direct = ByteBuffer.allocateDirect(900); direct.position(17).limit(811);
          check(first.read("/data/image", 131060, direct) == 794 && direct.position() == 811, "direct extent");
          for (int i = 0; i < 794; i++) check(direct.get(i + 17) == original[i + 131060], "direct content");
          reject(ReadOnlyBufferException.class, () -> first.read("/data/image", 0, direct.asReadOnlyBuffer()));
          reject(IOException.class, () -> first.stat("/absent"));
          reject(IOException.class, () -> first.listPage("/absent", null, 7));
          reject(IllegalArgumentException.class, () -> first.listPage("/data", null, 0));
          rejectedMutations(first);
          check(first.stat("/data/image").equals(stat), "errors do not unpin");
          List<M3FS.DirEntry> paged = new ArrayList<>(); String cursor = null;
          boolean more;
          do {
            var page = first.listPage("/data", cursor, 5);
            check(page.entries().size() <= 5, "bounded result");
            paged.addAll(page.entries()); cursor = page.nextAfter(); more = page.hasMore();
            if (paged.size() == 5) {
              for (var entry : writer.list("/data")) writer.unlink("/data/" + entry.name());
              writer.rmdir("/data"); writer.mkdir("/replacement", 0700);
            }
          } while (more);
          check(paged.equals(entries), "all pages preserve original names, IDs and metadata");
          check(first.list("/data").equals(entries), "list shares page view");
          check(first.stats().equals(stats), "statistics remain stable");
          first.verify(); second.verify(); writer.verify();
        }
      }
      for (int i = 0; i < 128; i++) {
        var view = M3FS.openSnapshot(db);
        check(view.isSnapshot() && view.list("/").size() == 1, "fresh reusable registry slot");
        view.close(); view.close(); check(!view.isOpen(), "idempotent close");
        reject(IllegalStateException.class, () -> view.stat("/"));
      }
      writer.checkpoint();
    }
  }

  private void rejectedMutations(M3FS view) throws Exception {
    reject(IOException.class, () -> view.createFile("/new", 0600));
    reject(IOException.class, () -> view.mkdir("/new", 0700));
    reject(IOException.class, () -> view.write("/data/image", 0, new byte[] {1}));
    reject(IOException.class, () -> view.write("/data/image", 0, ByteBuffer.allocateDirect(1)));
    reject(IOException.class, () -> view.truncate("/data/image", 0));
    reject(IOException.class, () -> view.unlink("/data/image"));
    reject(IOException.class, () -> view.rmdir("/data"));
    reject(IOException.class, () -> view.rename("/data/image", "/other"));
    reject(IOException.class, () -> view.link("/data/image", "/other"));
    reject(IOException.class, () -> view.symlink("/data/image", "/other"));
    reject(IOException.class, () -> view.cloneFile("/data/image", "/other"));
    reject(IOException.class, view::checkpoint);
  }

  private void concurrent(Path db) throws Exception {
    byte[] payload = new byte[65536 + 73]; new Random(SEED + 1).nextBytes(payload);
    try (var writer = M3FS.create(db)) {
      writer.createFile("/value", 0600); writer.write("/value", 0, payload);
    }
    try (var view = M3FS.openSnapshot(db); var executor = Executors.newSingleThreadExecutor()) {
      var started = new CountDownLatch(1);
      var future = executor.submit(() -> {
        try (var writer = M3FS.open(db)) {
          byte[] changed = new byte[payload.length]; started.countDown();
          for (int i = 0; i < 80; i++) { Arrays.fill(changed, (byte) i); writer.write("/value", 0, changed); }
          writer.truncate("/value", 0); writer.unlink("/value");
        } finally { started.countDown(); }
        return 80;
      });
      check(started.await(10, TimeUnit.SECONDS), "writer started");
      for (int i = 0; i < 1000; i++) {
        int offset = (i * 997) % payload.length;
        int count = Math.min(4097, payload.length - offset);
        check(Arrays.equals(view.read("/value", offset, count), Arrays.copyOfRange(payload, offset, offset + count)),
            "retained bytes during concurrent writes");
      }
      check(future.get(30, TimeUnit.SECONDS) == 80, "independent writer finished");
      check(Arrays.equals(view.read("/value", 0, payload.length), payload), "retained bytes after writer unlink");
      view.verify();
    }
  }

  @FunctionalInterface
  private interface Checked { void run() throws Exception; }

  private void reject(Class<? extends Throwable> expected, Checked action) throws Exception {
    try { action.run(); }
    catch (Throwable failure) {
      if (expected.isInstance(failure)) { checks++; return; }
      throw failure;
    }
    throw new AssertionError("expected " + expected.getName());
  }

  private void check(boolean value, String message) {
    checks++; if (!value) throw new AssertionError(message);
  }
}
