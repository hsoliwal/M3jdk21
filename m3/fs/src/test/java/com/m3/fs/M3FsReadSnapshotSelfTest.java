// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Concurrent whole-file replacement must not produce a mixed-version read. */
public final class M3FsReadSnapshotSelfTest {
  private M3FsReadSnapshotSelfTest() {}
  public static void main(String[] args) throws Exception {
    Path temp = Files.createTempDirectory("mindex-read-snapshot-");
    Path db = temp.resolve("test.db");
    int length = 32 * 65536;
    byte[] a = new byte[length], b = new byte[length];
    Arrays.fill(a, (byte)17); Arrays.fill(b, (byte)88);
    AtomicBoolean stop = new AtomicBoolean(); AtomicInteger writes = new AtomicInteger();
    CountDownLatch ready = new CountDownLatch(1), go = new CountDownLatch(1);
    int torn = 0;
    try {
      try (var reader = M3FS.create(db); var writer = M3FS.open(db);
           var pool = Executors.newSingleThreadExecutor()) {
        reader.createFile("/x", 0600); reader.write("/x", 0, a);
        var task = pool.submit(() -> {
          ready.countDown(); go.await();
          for (int i = 0; i < 500 && !stop.get(); i++) { writer.write("/x", 0, (i & 1) == 0 ? b : a); writes.incrementAndGet(); }
          return true;
        });
        ready.await(); go.countDown();
        for (int read = 0; read < 1000; read++) {
          byte[] bytes = reader.read("/x", 0, length);
          if (!Arrays.equals(bytes, a) && !Arrays.equals(bytes, b)) torn++;
        }
        stop.set(true); task.get(30, TimeUnit.SECONDS);
      }
      System.out.println("snapshot-read reads=1000 torn=" + torn + " writes=" + writes.get());
      if (torn != 0) throw new AssertionError("mixed committed versions");
    } finally {
      try (var paths = Files.walk(temp)) { for (var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
    }
  }
}
