// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Dependency-free executable regressions, also called by the module's JUnit wrapper. */
public final class M3MappedStorageChecks {
  private static final long SEED = 0x4d334653L;
  private static final int CASES = 10_000;
  private int assertions;

  private M3MappedStorageChecks() {}

  /** Runs against an isolated directory; never touches the user's real store. */
  public static void main(String[] args) throws Exception {
    String mode = args.length == 0 ? "off" : args[0];
    Path root = Files.createTempDirectory("m3fs-mapped-check-");
    M3MappedStorageChecks checks = new M3MappedStorageChecks();
    try {
      if (mode.equals("missing")) {
        checks.unavailable();
      } else {
        checks.run(root, mode.equals("required"));
      }
      System.out.println("M3FS_MAPPED_PASS mode=" + mode + " assertions=" + checks.assertions
          + " randomizedByteCases=" + (mode.equals("missing") ? 0 : CASES)
          + " seed=" + SEED);
    } finally {
      // Mapped buffers are GC-owned. Windows may defer deleting mapped files.
      try (var paths = Files.walk(root)) {
        for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
          try {
            Files.deleteIfExists(path);
          } catch (IOException deferred) {
            System.err.println("CLEANUP_DEFERRED " + path + ": " + deferred.getMessage());
          }
        }
      }
    }
  }

  private void run(Path root, boolean nativeRequired) throws Exception {
    mappedViews(root);
    publication(root);
    geometryAndCorruption(root);
    exactByteSpans(nativeRequired);
    fileComparison(root);
    if (nativeRequired) nativeInvalidSpans();
  }

  private void mappedViews(Path root) throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(root.resolve("views"));
    Random random = new Random(SEED);
    byte[] payload = new byte[4099];
    random.nextBytes(payload);
    M3ObjectRef ref = store.put(payload, M3ObjectKind.PRECOMPUTE_OUTPUT);
    check(ref.id().equals(M3ContentId.digest(payload)), "existing content identity");
    check(store.verify(ref), "published digest");
    M3MappedObject first = store.openMapped(ref, payload.length);
    M3MappedObject second = store.openMapped(ref, payload.length);
    check(first.reference().equals(ref), "reference unchanged");
    check(first.size() == payload.length, "mapping size");
    check(first.contentEquals(0, second, 0, payload.length), "independent mappings");
    check(first.contentEquals(3, second, 3, 1999), "offset spans");
    byte[] changed = payload.clone();
    changed[0] ^= 1;
    M3MappedObject different = store.openMapped(
        store.put(changed, M3ObjectKind.PRECOMPUTE_OUTPUT), changed.length);
    check(!first.contentEquals(0, different, 0, payload.length), "unequal object payloads");
    check(first.contentEquals(1, different, 1, payload.length - 1),
        "equal spans across different content identities");
    expect(IndexOutOfBoundsException.class,
        () -> first.contentEquals(0, second, second.size(), 1));
    for (int i = 0; i < payload.length; i++) {
      check(first.byteAt(i) == payload[i], "byte value");
    }
    ByteBuffer view = first.bytes(3, 80);
    check(view.isReadOnly() && view.position() == 0 && view.limit() == 80, "view geometry");
    expect(ReadOnlyBufferException.class, () -> view.put(0, (byte) 7));
    view.position(12).mark().limit(20);
    check(first.bytes(3, 80).position() == 0, "independent view cursor");
    check(first.byteAt(3) == payload[3], "parent cursor unaffected");
    view.reset();
    check(view.position() == 12, "mark retained");
    for (ByteOrder order : List.of(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)) {
      ByteBuffer oracle = ByteBuffer.wrap(payload).order(order);
      IntBuffer ints = first.ints(1, 500, order);
      LongBuffer longs = first.longs(3, 500, order);
      check(ints.isReadOnly() && longs.isReadOnly(), "primitive lanes read-only");
      for (int i = 0; i < 500; i++) {
        check(ints.get(i) == oracle.getInt(1 + i * 4), "int endian and unaligned offset");
        check(longs.get(i) == oracle.getLong(3 + i * 8), "long endian and unaligned offset");
      }
      expect(ReadOnlyBufferException.class, () -> ints.put(0, 99));
      expect(ReadOnlyBufferException.class, () -> longs.put(0, 99L));
    }
    check(first.bytes(payload.length, 0).remaining() == 0, "empty tail");
    M3ObjectRef emptyRef = store.put(new byte[0], M3ObjectKind.RAW_BYTES);
    M3MappedObject empty = store.openMapped(emptyRef, 0);
    check(empty.size() == 0, "empty image");
    check(empty.ints(0, 0, ByteOrder.BIG_ENDIAN).remaining() == 0, "empty int lane");
    check(empty.longs(0, 0, ByteOrder.LITTLE_ENDIAN).remaining() == 0, "empty long lane");
    check(empty.contentEquals(0, empty, 0, 0), "empty equality");
    M3ObjectRef otherRole = store.put(payload, M3ObjectKind.RAW_BYTES);
    check(otherRole.id().equals(ref.id()) && otherRole.kind() != ref.kind(), "role not erased");
    check(Arrays.equals(payload, store.readSmall(ref, Integer.MAX_VALUE)), "max-int read limit");
    try (var readers = Executors.newFixedThreadPool(8)) {
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int thread = 0; thread < 8; thread++) {
        tasks.add(() -> {
          for (int i = 0; i < 500; i++) {
            if (first.longs(3, 500, ByteOrder.BIG_ENDIAN).get(i)
                != ByteBuffer.wrap(payload).getLong(3 + i * 8)) return false;
          }
          return true;
        });
      }
      for (Future<Boolean> result : readers.invokeAll(tasks)) check(result.get(), "shared reader");
    }
  }

  private void publication(Path root) throws Exception {
    Path storeRoot = root.resolve("publication");
    M3DirectoryContentStore store = new M3DirectoryContentStore(storeRoot);
    byte[] bytes = new byte[8_193];
    new Random(SEED + 1).nextBytes(bytes);
    M3ContentId expected = M3ContentId.digest(bytes);
    try (var writers = Executors.newFixedThreadPool(8)) {
      List<Callable<M3ObjectRef>> tasks = new ArrayList<>();
      for (int i = 0; i < 128; i++) {
        tasks.add(() -> new M3DirectoryContentStore(storeRoot).put(bytes, M3ObjectKind.RAW_BYTES));
      }
      for (Future<M3ObjectRef> result : writers.invokeAll(tasks)) {
        check(result.get().id().equals(expected), "concurrent publication identity");
      }
    }
    M3ObjectRef ref = store.ref(expected, M3ObjectKind.RAW_BYTES);
    check(store.verify(ref), "concurrent object verified");
    try (var staged = Files.list(storeRoot.resolve("staging"))) {
      check(staged.findAny().isEmpty(), "staging cleaned");
    }
    try (var objects = Files.walk(storeRoot.resolve("objects"))) {
      check(objects.filter(Files::isRegularFile).count() == 1L, "one physical object");
    }
    Path source = root.resolve("publication-source");
    Path target = root.resolve("publication-existing");
    Files.write(source, new byte[] {1, 2, 3});
    Files.write(target, new byte[] {9, 8, 7});
    Method publish = M3DirectoryContentStore.class.getDeclaredMethod(
        "moveCreateOnly", Path.class, Path.class);
    publish.setAccessible(true);
    expect(FileAlreadyExistsException.class, () -> invoke(publish, source, target));
    check(Arrays.equals(Files.readAllBytes(target), new byte[] {9, 8, 7}), "no replacement");
    check(Arrays.equals(Files.readAllBytes(source), new byte[] {1, 2, 3}), "race candidate retained");
    Files.write(store.objectPath(expected), new byte[bytes.length]);
    expect(IOException.class, () -> store.put(bytes, M3ObjectKind.RAW_BYTES));
    check(Arrays.equals(Files.readAllBytes(store.objectPath(expected)), new byte[bytes.length]),
        "collision/corrupt target not overwritten");
    expect(NullPointerException.class, () -> store.put(bytes, null));
    try (var staged = Files.list(storeRoot.resolve("staging"))) {
      check(staged.findAny().isEmpty(), "failure staging cleaned");
    }
  }

  private void geometryAndCorruption(Path root) throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(root.resolve("geometry"));
    byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7};
    M3ObjectRef ref = store.put(new ByteArrayInputStream(bytes), M3ObjectKind.MINDEX_FILE_IMAGE);
    M3MappedObject mapped = store.openMapped(ref, 8);
    expect(IllegalArgumentException.class, () -> store.openMapped(ref, -1));
    expect(IOException.class, () -> store.openMapped(ref, 7));
    expect(IndexOutOfBoundsException.class, () -> mapped.bytes(-1, 1));
    expect(IndexOutOfBoundsException.class, () -> mapped.bytes(0, -1));
    expect(IndexOutOfBoundsException.class, () -> mapped.byteAt(8));
    expect(IndexOutOfBoundsException.class, () -> mapped.bytes(Integer.MAX_VALUE, 1));
    expect(IndexOutOfBoundsException.class, () -> mapped.ints(0, Integer.MAX_VALUE, ByteOrder.BIG_ENDIAN));
    expect(IndexOutOfBoundsException.class, () -> mapped.longs(0, Integer.MAX_VALUE, ByteOrder.BIG_ENDIAN));
    expect(IndexOutOfBoundsException.class, () -> mapped.longs(0, -1, ByteOrder.BIG_ENDIAN));
    expect(NullPointerException.class, () -> mapped.ints(0, 1, null));
    expect(NullPointerException.class, () -> mapped.contentEquals(0, null, 0, 0));
    expect(IOException.class, () -> store.readSmall(ref, 7));
    expect(IllegalArgumentException.class, () -> store.readSmall(ref, -1));
    Path object = store.objectPath(ref.id());
    M3ObjectRef wrongSize = new M3ObjectRef(ref.id(), 7, ref.kind());
    expect(IOException.class, () -> store.openMapped(wrongSize, 8));
    Files.write(object, new byte[8]);
    expect(IOException.class, () -> store.openMapped(ref, 8));
    expect(IOException.class, () -> store.readSmall(ref, 8));
    Files.write(object, new byte[7]);
    expect(IOException.class, () -> store.openMapped(ref, 8));
    Path hugeMissing = root.resolve("budget-before-open");
    M3ObjectRef huge = new M3ObjectRef(ref.id(), (long) Integer.MAX_VALUE + 1L, ref.kind());
    try {
      M3MappedObject.open(hugeMissing, huge, Integer.MAX_VALUE);
      throw new AssertionError("oversize mapping admitted");
    } catch (IOException expected) {
      check(expected.getMessage().contains("budget"), "budget checked before file access");
    }
    Path outside = Files.createDirectory(root.resolve("symlink-target"));
    Path link = root.resolve("symlink-root");
    Files.createSymbolicLink(link, outside);
    expect(IOException.class, () -> new M3DirectoryContentStore(link));
    check(!Files.exists(outside.resolve("objects")), "no directory writes through rejected root");
    Path alias = root.resolve("object-alias");
    Files.createSymbolicLink(alias, object);
    expect(IOException.class, () -> M3MappedObject.open(alias, ref, 8));
  }

  private void exactByteSpans(boolean nativeRequired) throws Exception {
    Random random = new Random(SEED + 2);
    for (int iteration = 0; iteration < CASES; iteration++) {
      int capacity = random.nextInt(257);
      byte[] leftBytes = new byte[capacity];
      random.nextBytes(leftBytes);
      byte[] rightBytes = leftBytes.clone();
      if (capacity > 0 && (iteration & 1) == 0) {
        int at = random.nextInt(capacity);
        rightBytes[at] ^= (byte) 0x80;
      }
      boolean direct = nativeRequired || (iteration & 2) == 0;
      ByteBuffer a = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
      ByteBuffer b = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
      a.put(leftBytes).clear();
      b.put(rightBytes).clear();
      if ((iteration & 4) == 0) a = a.asReadOnlyBuffer();
      if ((iteration & 8) == 0) b = b.asReadOnlyBuffer();
      int offsetA = random.nextInt(capacity + 1);
      int offsetB = random.nextInt(capacity + 1);
      int length = random.nextInt(Math.min(capacity - offsetA, capacity - offsetB) + 1);
      int limitA = random.nextInt(capacity + 1);
      int limitB = random.nextInt(capacity + 1);
      int posA = random.nextInt(limitA + 1);
      int posB = random.nextInt(limitB + 1);
      a.limit(limitA).position(posA).mark();
      b.limit(limitB).position(posB).mark();
      boolean expected = Arrays.equals(leftBytes, offsetA, offsetA + length,
          rightBytes, offsetB, offsetB + length);
      check(M3NativeContent.equalDirect(a, offsetA, b, offsetB, length) == expected, "span oracle");
      check(a.limit() == limitA && b.limit() == limitB, "limits unchanged");
      check(a.position() == posA && b.position() == posB, "positions unchanged");
      a.reset();
      b.reset();
      check(a.position() == posA && b.position() == posB, "marks unchanged");
    }
    ByteBuffer backingA = ByteBuffer.allocateDirect(32);
    ByteBuffer backingB = ByteBuffer.allocateDirect(40);
    for (int i = 0; i < 16; i++) {
      backingA.put(7 + i, (byte) i);
      backingB.put(11 + i, (byte) i);
    }
    ByteBuffer sliceA = backingA.slice(7, 16).asReadOnlyBuffer();
    ByteBuffer sliceB = backingB.slice(11, 16).asReadOnlyBuffer();
    sliceA.limit(0);
    sliceB.limit(0);
    check(M3NativeContent.equalDirect(sliceA, 0, sliceB, 0, 16), "slice-relative bases");
    backingB.put(19, (byte) 99);
    check(!M3NativeContent.equalDirect(sliceA, 0, sliceB, 0, 16), "slice-relative mismatch");
    ByteBuffer direct = ByteBuffer.allocateDirect(8);
    expect(IndexOutOfBoundsException.class, () -> M3NativeContent.equalDirect(direct, -1, direct, 0, 0));
    expect(IndexOutOfBoundsException.class,
        () -> M3NativeContent.equalDirect(direct, Integer.MAX_VALUE, direct, 0, 8));
    expect(IndexOutOfBoundsException.class, () -> M3NativeContent.equalDirect(direct, 0, direct, 0, -1));
    expect(NullPointerException.class, () -> M3NativeContent.equalDirect(null, 0, direct, 0, 1));
    check(M3NativeContent.equalDirect(direct, 8, direct, 8, 0), "empty end span");
    if (nativeRequired) {
      expect(IllegalStateException.class,
          () -> M3NativeContent.equalDirect(ByteBuffer.allocate(8), 0, direct, 0, 1));
    }
  }

  private void fileComparison(Path root) throws Exception {
    Path a = root.resolve("compare-a");
    Path b = root.resolve("compare-b");
    Random random = new Random(SEED + 3);
    for (int size : new int[] {0, 1, 127, 131071, 131072, 131073, 1048579}) {
      byte[] bytes = new byte[size];
      random.nextBytes(bytes);
      Files.write(a, bytes);
      Files.write(b, bytes);
      check(M3NativeContent.filesEqual(a, b), "file equality");
      check(Files.mismatch(a, b) == -1L, "file oracle");
      if (size > 0) {
        bytes[size - 1] ^= 1;
        Files.write(b, bytes);
        check(!M3NativeContent.filesEqual(a, b), "tail mismatch");
      }
      Files.write(b, Arrays.copyOf(bytes, size + 1));
      check(!M3NativeContent.filesEqual(a, b), "size mismatch");
    }
  }

  private void nativeInvalidSpans() throws Exception {
    Method nativeMethod = M3NativeContent.class.getDeclaredMethod("nativeEqualDirect",
        ByteBuffer.class, int.class, ByteBuffer.class, int.class, int.class);
    nativeMethod.setAccessible(true);
    ByteBuffer direct = ByteBuffer.allocateDirect(16);
    check((int) invoke(nativeMethod, null, 0, direct, 0, 1) == -2, "JNI null");
    check((int) invoke(nativeMethod, direct, -1, direct, 0, 1) == -2, "JNI negative");
    check((int) invoke(nativeMethod, direct, 0, direct, 0, -1) == -2, "JNI negative length");
    check((int) invoke(nativeMethod, direct, 0, direct, 0, Integer.MAX_VALUE) == -2, "JNI overflow");
    check((int) invoke(nativeMethod, ByteBuffer.allocate(16), 0, direct, 0, 1) == -2, "JNI heap");
    check((int) invoke(nativeMethod, ByteBuffer.allocateDirect(0), 0,
        ByteBuffer.allocateDirect(0), 0, 0) == 1, "JNI empty direct");
    check((int) invoke(nativeMethod, direct, 16, direct, 16, 0) == 1, "JNI empty end");
  }

  private void unavailable() throws Exception {
    ByteBuffer direct = ByteBuffer.allocateDirect(16);
    expect(IllegalStateException.class, () -> M3NativeContent.equalDirect(direct, 0, direct, 0, 16));
  }

  private void check(boolean value, String label) {
    if (!value) throw new AssertionError(label);
    assertions++;
  }

  private void expect(Class<? extends Throwable> expected, Checked action) throws Exception {
    try {
      action.run();
    } catch (Throwable failure) {
      if (!expected.isInstance(failure)) throw new AssertionError("expected " + expected, failure);
      assertions++;
      return;
    }
    throw new AssertionError("expected " + expected.getName());
  }

  private static Object invoke(Method method, Object... arguments) throws Exception {
    try {
      return method.invoke(null, arguments);
    } catch (InvocationTargetException wrapped) {
      Throwable cause = wrapped.getCause();
      if (cause instanceof Exception exception) throw exception;
      if (cause instanceof Error error) throw error;
      throw new AssertionError(cause);
    }
  }

  @FunctionalInterface
  private interface Checked {
    void run() throws Exception;
  }
}
