// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Dependency-free executable smoke oracle; Maven/JUnit remains separately required. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-storage-sdk/src/test/java/com/synexia/storage/ExtentStandaloneProof.java blob 6bedf87836d8; recipe m3/file/recipe/durable-extent-store-20261107/relocate.py v1
public final class M3ExtentStandaloneProof {
  private M3ExtentStandaloneProof() { }

  public static void main(String[] arguments) throws Exception {
    Path directory = Files.createTempDirectory("m3-extent-proof-");
    String source = "0".repeat(64);
    byte[] original = "the original".getBytes(StandardCharsets.UTF_8);
    byte[] update = "the replacement".getBytes(StandardCharsets.UTF_8);
    M3ContentExtent first = M3ContentExtent.fromBytes("source", source, 17, original, "text");
    M3ContentExtent next = M3ContentExtent.fromBytes("source", source, 17, update, "text");
    check(!first.extentId().equals(next.extentId()), "content must change identity");
    check(first.extentId().equals(M3ContentExtent.fromBytes("source", source, 17,
        original, "text").extentId()), "identity must be deterministic");
    check("VERIFIED".equals(M3ContentExtent.validateContent(first, original).get("status")),
        "content verification");
    expect(M3ContentExtent.ContentTamperedException.class,
        () -> M3ContentExtent.validateContent(first, update));

    try (M3DurableExtentStore store = M3DurableExtentStore.open(directory)) {
      check(store.read("a") == null, "missing reads must return null");
      store.write("b", first, original);
      store.write("a", M3ContentExtent.fromBytes("source", source, 0, new byte[0], "raw"), new byte[0]);
      check(store.keys("", 0).equals(List.of("a", "b")), "stable sorted keys");
      check(Arrays.equals(original, store.read("b")), "mapped read");
      byte[] returned = store.read("b");
      returned[0] = 0;
      check(Arrays.equals(original, store.read("b")), "mutating read may not change canonical blob");
      expect(IllegalStateException.class, () -> store.write("b", first, original));
      store.verifyAll();
      check(store.manifest().extents().size() == 2, "manifest size");
    }

    try (M3DurableExtentStore failure = M3DurableExtentStore.open(directory, () -> {
      throw new IOException("injected before index commit");
    })) {
      expect(IOException.class, () -> failure.supersede("b", next, update));
      check(Arrays.equals(original, failure.read("b")), "failed supersede must preserve old data");
    }
    try (M3DurableExtentStore recovered = M3DurableExtentStore.open(directory)) {
      check(Arrays.equals(original, recovered.read("b")), "reopen must preserve old data");
      recovered.supersede("b", next, update);
      check(Arrays.equals(update, recovered.read("b")), "successful supersede must become visible");
      expect(IOException.class, () -> M3DurableExtentStore.open(directory));
    }
    Path blob = directory.resolve("blobs").resolve(next.contentSha256() + ".bin");
    Files.write(blob, "short".getBytes(StandardCharsets.UTF_8));
    try (M3DurableExtentStore recovered = M3DurableExtentStore.open(directory)) {
      expect(IOException.class, () -> recovered.read("b"));
      expect(IOException.class, recovered::verifyAll);
    }
    System.out.println("PASS: extent identity, persistence, atomic failpoint, recovery, locking, corruption");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void expect(Class<? extends Throwable> expected, CheckedAction action) throws Exception {
    try {
      action.run();
    } catch (Throwable found) {
      if (expected.isInstance(found)) return;
      throw new AssertionError("expected " + expected.getName() + ", got " + found, found);
    }
    throw new AssertionError("expected " + expected.getName());
  }

  @FunctionalInterface private interface CheckedAction {
    void run() throws Exception;
  }
}
