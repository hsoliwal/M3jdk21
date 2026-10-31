// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Real Java -> existing loader -> JNI -> SQLite integration; no database or loader substitutes. */
public final class M3FsIndexedSelfTest {
  private static final long SEED = 20260927L;
  private static final int BLOCK = 65536;
  private int checks;

  private M3FsIndexedSelfTest() {}

  public static void main(String[] args) throws Exception {
    var suite = new M3FsIndexedSelfTest();
    suite.check(M3FS.nativeAvailable(), "real native library must load");
    if (args.length > 0 && args[0].equals("legacy")) {
      suite.legacyLibrary();
    } else {
      suite.check(M3FS.supportsSharedBlockClone(), "clone capability");
      suite.check(M3FS.supportsDirectoryPaging(), "paging capability");
      suite.integration();
    }
    System.out.println("PASS indexed-java checks=" + suite.checks + " seed=" + SEED
        + " lane=" + (args.length == 0 ? "current" : args[0]) + " real-loader-jni-sqlite=YES");
  }

  private void integration() throws Exception {
    Path temp = Files.createTempDirectory("mindexfs-indexed-");
    Path db = temp.resolve("objects.sqlite");
    try {
      try (var fs = M3FS.create(db)) {
        boundaries(fs);
        cloneAndCow(fs);
        paging(fs);
        randomizedClones(fs);
        fs.verify();
        concurrentClones(fs, db);
        fs.checkpoint();
      }
      try (var fs = M3FS.openReadOnly(db)) {
        check(fs.isReadOnly(), "read-only mode");
        check(fs.listPage("/pages", null, 3).entries().size() == 3, "read-only page");
        reject(IOException.class, () -> fs.cloneFile("/source", "/read-only-copy"));
        check(Arrays.equals(fs.read("/source", 0, 7), new byte[] {1,2,3,4,5,6,7}), "reopen source");
        fs.verify();
      }
      var closed = M3FS.open(db);
      closed.close();
      check(!closed.isOpen(), "closed");
      reject(IllegalStateException.class, () -> closed.cloneFile("/source", "/after-close"));
      reject(IllegalStateException.class, () -> closed.listPage("/", null, 3));
      closed.close();
      try (var fs = M3FS.open(db)) { fs.verify(); }
    } finally { removeTree(temp); }
  }

  private void boundaries(M3FS fs) throws Exception {
    reject(IllegalArgumentException.class, () -> fs.createFile("/truncated\0suffix", 0600));
    reject(NoSuchFileException.class, () -> fs.stat("/truncated"));
    reject(IllegalArgumentException.class, () -> fs.symlink("target\0suffix", "/link"));
    reject(IllegalArgumentException.class, () -> fs.createFile("/bad\uD800", 0600));
    fs.createFile("/source", 0640);
    fs.write("/source", 0, new byte[] {1,2,3,4,5,6,7});
    ByteBuffer backing = ByteBuffer.allocateDirect(16);
    ByteBuffer readonly = backing.asReadOnlyBuffer();
    reject(ReadOnlyBufferException.class, () -> fs.read("/source", 0, readonly));
    check(readonly.position() == 0 && backing.get(0) == 0, "readonly buffer unchanged");
    // Call the real private native method to ensure Java admission is not the only guard.
    long handle = M3FsNative.open(fs.database().toString(), 0);
    try {
      var method = M3FsNative.class.getDeclaredMethod("nativeReadDirect", long.class,
          String.class, long.class, ByteBuffer.class, int.class, int.class);
      method.setAccessible(true);
      reject(ReadOnlyBufferException.class, () -> {
        try { method.invoke(null, handle, "/source", 0L, readonly, 0, 7); }
        catch (InvocationTargetException cause) {
          if (cause.getCause() instanceof Exception failure) throw failure;
          throw cause;
        }
      });
      check(backing.get(0) == 0, "native readonly guard");
    } finally { M3FsNative.close(handle); }
    ByteBuffer direct = ByteBuffer.allocateDirect(16).position(3).limit(10);
    check(fs.read("/source", 0, direct) == 7 && direct.position() == 10, "direct cursor");
    check(direct.get(3) == 1 && direct.get(9) == 7, "direct contents");
    fs.createFile("/😀-नमस्ते", 0600);
    check(fs.list("/").stream().anyMatch(row -> row.name().equals("😀-नमस्ते")), "UTF8 round trip");
  }

  private void cloneAndCow(M3FS fs) throws Exception {
    fs.write("/source", BLOCK * 5L + 3, new byte[] {9,8,7});
    var source = fs.stat("/source");
    var before = fs.stats();
    long clone = fs.cloneFile("/source", "/clone");
    var cloned = fs.stat("/clone");
    check(clone == cloned.inode() && clone != source.inode(), "independent inode");
    check(cloned.mode() == 0640 && cloned.size() == source.size(), "mode and size");
    check(cloned.linkCount() == 1 && cloned.generation() == 1, "fresh metadata");
    check(fs.stat("/source").equals(source), "source metadata unchanged");
    check(fs.stats().uniqueBlocks() == before.uniqueBlocks(), "no new block payloads");
    check(fs.stats().blockReferences() == before.blockReferences() + 2, "two reused references");
    check(Arrays.equals(fs.read("/clone", BLOCK * 2L, 64), new byte[64]), "sparse hole retained");
    ByteBuffer change = ByteBuffer.allocateDirect(8); change.put((byte)22).put((byte)23).flip();
    fs.write("/clone", 0, change.asReadOnlyBuffer());
    check(fs.read("/clone", 0, 1)[0] == 22 && fs.read("/source", 0, 1)[0] == 1, "clone COW");
    fs.link("/clone", "/clone-link");
    fs.write("/clone-link", 1, new byte[] {44});
    check(fs.read("/clone", 1, 1)[0] == 44, "hardlink still shared inode");
    check(fs.read("/source", 1, 1)[0] == 2, "hardlink does not affect source");
    fs.truncate("/clone", 2);
    fs.truncate("/clone", 10);
    check(Arrays.equals(fs.read("/clone", 2, 8), new byte[8]), "truncate zero tail");
    check(fs.stat("/source").size() == source.size(), "source size retained");
    reject(FileAlreadyExistsException.class, () -> fs.cloneFile("/source", "/clone"));
    reject(FileAlreadyExistsException.class, () -> fs.cloneFile("/source", "/source"));
    reject(IOException.class, () -> fs.cloneFile("/", "/directory-clone"));
    reject(NoSuchFileException.class, () -> fs.cloneFile("/missing", "/not-published"));
    reject(NoSuchFileException.class, () -> fs.stat("/not-published"));
    fs.symlink("source", "/source-link");
    reject(IllegalArgumentException.class, () -> fs.cloneFile("/source-link", "/symlink-copy"));
    reject(IllegalArgumentException.class, () -> fs.cloneFile("/source", "/bad\0tail"));
    fs.createFile("/empty", 0600);
    fs.cloneFile("/empty", "/empty-copy");
    check(fs.read("/empty-copy", 0, 1).length == 0, "empty clone");
    fs.unlink("/clone"); fs.unlink("/clone-link");
    check(fs.stats().uniqueBlocks() == before.uniqueBlocks(), "COW blocks collected");
    fs.verify();
  }

  private void paging(M3FS fs) throws Exception {
    fs.mkdir("/pages", 0700);
    List<String> names = new ArrayList<>(List.of("a", "aa", "a' OR 1=1 --", "é", "é", "汉字", "\uE000", "😀", "नमस्ते", "z"));
    for (int i = 0; i < 311; i++) names.add(String.format(java.util.Locale.ROOT, "n%04d", i));
    names.add("x".repeat(255));
    for (String name : names) fs.createFile("/pages/" + name, 0600);
    names.sort((a, b) -> Arrays.compareUnsigned(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)));
    check(names.equals(fs.list("/pages").stream().map(M3FS.DirEntry::name).toList()), "old list UTF8 order");
    for (int limit : new int[] {1,2,3,7,64,321,322,4096}) {
      List<String> actual = new ArrayList<>();
      String cursor = null;
      for (int pageNo = 0;; pageNo++) {
        check(pageNo <= names.size(), "bounded page progress");
        var page = fs.listPage("/pages", cursor, limit);
        check(page.entries().size() <= limit, "page size cap");
        if (page.hasMore()) check(page.entries().size() == limit, "more flag");
        for (var entry : page.entries()) { actual.add(entry.name()); check(entry.stat().inode() > 0, "page stat"); }
        if (!page.hasMore()) break;
        check(!page.nextAfter().equals(cursor), "cursor progresses");
        cursor = page.nextAfter();
      }
      check(actual.equals(names), "all entries exactly once, limit=" + limit);
    }
    var page = fs.listPage("/pages", null, 3);
    reject(UnsupportedOperationException.class, () -> page.entries().clear());
    String cursor = page.nextAfter();
    fs.unlink("/pages/" + cursor);
    check(fs.listPage("/pages", cursor, 1).entries().getFirst().name().equals(names.get(3)), "deleted cursor works");
    check(fs.listPage("/pages", names.getLast(), 4).entries().isEmpty(), "past last empty");
    fs.mkdir("/no-entries", 0700);
    var empty = fs.listPage("/no-entries", null, 10);
    check(empty.entries().isEmpty() && !empty.hasMore() && empty.nextAfter() == null, "empty page");
    for (String invalid : List.of("/", "a/b", ".", "..", "a\0b", "\uD800", "z".repeat(256))) {
      reject(IllegalArgumentException.class, () -> fs.listPage("/pages", invalid, 2));
    }
    for (int invalid : new int[] {0,-1,4097,Integer.MAX_VALUE}) {
      reject(IllegalArgumentException.class, () -> fs.listPage("/pages", null, invalid));
    }
    reject(IOException.class, () -> fs.listPage("/source", null, 2));
    reject(NoSuchFileException.class, () -> fs.listPage("/missing", null, 2));
    fs.verify();
  }

  private void randomizedClones(M3FS fs) throws Exception {
    Random random = new Random(SEED);
    fs.mkdir("/random", 0700);
    for (int sample = 0; sample < 80; sample++) {
      byte[] expected = new byte[random.nextInt(BLOCK * 3 + 17)]; random.nextBytes(expected);
      String a = "/random/a" + sample, b = "/random/b" + sample;
      fs.createFile(a, 0600); fs.write(a, 0, expected); fs.cloneFile(a, b);
      check(Arrays.equals(fs.read(b, 0, expected.length + 1), expected), "random clone " + sample);
      byte[] modified = expected.clone();
      if (modified.length > 0) {
        int at = random.nextInt(modified.length); modified[at] ^= 0x5a;
        fs.write(b, at, new byte[] {modified[at]});
      }
      check(Arrays.equals(fs.read(a, 0, expected.length + 1), expected), "random source independence");
      check(Arrays.equals(fs.read(b, 0, modified.length + 1), modified), "random clone mutation");
      fs.unlink(a);
      check(Arrays.equals(fs.read(b, 0, modified.length + 1), modified), "source removal retains clone");
      fs.unlink(b);
    }
    fs.verify();
  }

  private void concurrentClones(M3FS fs, Path db) throws Exception {
    fs.mkdir("/parallel", 0700);
    AtomicInteger winners = new AtomicInteger();
    CountDownLatch ready = new CountDownLatch(8), start = new CountDownLatch(1);
    List<Callable<Long>> work = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      int index = i;
      work.add(() -> {
        try (var other = M3FS.open(db)) {
          ready.countDown(); start.await();
          try { other.cloneFile("/source", "/parallel/winner"); winners.incrementAndGet(); }
          catch (FileAlreadyExistsException expected) { /* exactly one create-only winner */ }
          long inode = other.cloneFile("/source", "/parallel/" + index);
          other.write("/parallel/" + index, 0, new byte[] {(byte)(30 + index)});
          if (other.read("/source", 0, 1)[0] != 1) throw new AssertionError("concurrent source mutation");
          return inode;
        }
      });
    }
    try (var pool = Executors.newFixedThreadPool(8)) {
      var futures = work.stream().map(pool::submit).toList();
      if (!ready.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("worker startup");
      start.countDown();
      var ids = new HashSet<Long>();
      for (var future : futures) ids.add(future.get(30, java.util.concurrent.TimeUnit.SECONDS));
      check(ids.size() == 8, "eight distinct cloned inodes");
    }
    check(winners.get() == 1, "one atomic destination winner");
    check(fs.listPage("/parallel", null, 20).entries().size() == 9, "race no extra names");
    check(fs.read("/source", 0, 1)[0] == 1, "parallel source unchanged");
    fs.verify();
  }

  private void legacyLibrary() throws Exception {
    check(!M3FS.supportsSharedBlockClone(), "old library has no clone extension");
    check(!M3FS.supportsDirectoryPaging(), "old library has no page extension");
    Path temp = Files.createTempDirectory("mindexfs-legacy-");
    try (var fs = M3FS.create(temp.resolve("legacy.sqlite"))) {
      fs.createFile("/a", 0600); fs.write("/a", 0, new byte[] {1,2,3});
      check(Arrays.equals(fs.read("/a", 0, 5), new byte[] {1,2,3}), "old operations retained");
      reject(UnsupportedOperationException.class, () -> fs.cloneFile("/a", "/b"));
      reject(UnsupportedOperationException.class, () -> fs.listPage("/", null, 3));
      check(fs.list("/").size() == 1, "no hidden fallback mutations");
      fs.verify();
    } finally { removeTree(temp); }
  }

  private static void removeTree(Path root) throws IOException {
    try (var paths = Files.walk(root)) {
      for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(entry);
    }
  }
  private void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
  private void reject(Class<? extends Throwable> type, Checked action) throws Exception {
    try { action.run(); }
    catch (Throwable failure) { if (type.isInstance(failure)) { checks++; return; } throw failure; }
    throw new AssertionError("expected " + type.getName());
  }
  @FunctionalInterface private interface Checked { void run() throws Exception; }
}
