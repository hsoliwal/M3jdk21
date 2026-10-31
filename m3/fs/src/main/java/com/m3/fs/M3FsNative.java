// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/** Package-private checked JNI surface for the standalone native M3FS library. */
final class M3FsNative {
  static final int OPEN_CREATE = 1;
  static final int OPEN_READ_ONLY = 2;
  static final int OPEN_READ_SNAPSHOT = 4;
  static final int CAP_CLONE_FILE = 1;
  static final int CAP_LIST_PAGE = 2;
  static final int CAP_READ_SNAPSHOT = 4;
  static final int CAP_WRITEV = 8;
  static final int CAP_NATIVE_MINDEX = 16;
  static final int CAP_NATIVE_REGEX = 32;
  static final int CAP_READV = 64;
  static final int CAP_BLOCK_SCRUB = 128;
  static final int MAX_PAGE_ENTRIES = 4096;
  static final int MAX_WRITE_SEGMENTS = 4096;
  static final int MAX_READ_SEGMENTS = 4096;
  private static final int ABI_VERSION = 1;
  private static volatile Boolean available;
  private static volatile Integer capabilities;

  static final long FEATURE_EXACT_BLOCK_DEDUP = 1L << 0;
  static final long FEATURE_SPARSE_FILES = 1L << 1;
  static final long FEATURE_BLOCK_CLONE_COW = 1L << 2;
  static final long FEATURE_HARD_LINKS = 1L << 3;
  static final long FEATURE_SYMLINKS = 1L << 4;
  static final long FEATURE_RETAINED_READ_SNAPSHOT = 1L << 5;
  static final long FEATURE_DIRECTORY_PAGING = 1L << 6;
  static final long FEATURE_SCATTER_GATHER_WRITE = 1L << 7;
  static final long FEATURE_WAL_CHECKPOINT = 1L << 8;
  static final long FEATURE_INTEGRITY_VERIFY = 1L << 9;
  static final long FEATURE_ORPHAN_BLOCK_GC = 1L << 10;
  static final long FEATURE_HOLE_PUNCH = 1L << 11;
  static final long FEATURE_ALLOCATED_RANGE_QUERY = 1L << 12;
  static final long FEATURE_SCATTER_GATHER_READ = 1L << 13;
  static final long FEATURE_NATIVE_MINDEX = 1L << 14;
  static final long FEATURE_NATIVE_REGEX = 1L << 15;
  static final long FEATURE_BLOCK_SCRUB = 1L << 16;
  static final long FEATURE_SET_METADATA = 1L << 17;
  static final long FEATURE_ATOMIC_REPLACE = 1L << 18;
  static final long FEATURE_CHECKED_INODE_IO = 1L << 19;
  static final int CAP_PUNCH_HOLE = 256;
  static final int CAP_ALLOCATED_RANGES = 512;
  static final int CAP_SET_METADATA = 1024;
  static final int CAP_ATOMIC_REPLACE = 2048;
  static final int CAP_CHECKED_INODE_IO = 4096;
  static final int CAP_RENAME_NOREPLACE = 8192;
  static final long FEATURE_RENAME_NOREPLACE = 1L << 20;

  static void renameNoReplace(long handle, String from, String to) throws IOException {
    requireCapability(CAP_RENAME_NOREPLACE);
    nativeRenameNoReplace(handle, Objects.requireNonNull(from, "from"), Objects.requireNonNull(to, "to"));
  }

  private static native void nativeRenameNoReplace(long handle, String from, String to);

  static long featureBits() {
    if (!available()) return 0L;
    try { return nativeFeatureBits(); }
    catch (UnsatisfiedLinkError legacyAbi) { return 0L; }
  }

  static void punchHole(long handle, String path, long offset, long length) throws IOException {
    requireCapability(CAP_PUNCH_HOLE);
    nativePunchHole(handle, Objects.requireNonNull(path, "path"), offset, length);
  }
  static long[] allocatedRanges(long handle, String path, long offset, long length) throws IOException {
    requireCapability(CAP_ALLOCATED_RANGES);
    return nativeAllocatedRanges(handle, Objects.requireNonNull(path, "path"), offset, length);
  }
  static void setMetadata(long handle, String path, int mode, long time, boolean setMode, boolean setTime)
      throws IOException {
    requireCapability(CAP_SET_METADATA);
    nativeSetMetadata(handle, Objects.requireNonNull(path, "path"), mode, time, setMode, setTime);
  }
  static long replaceImage(long handle, String path, ByteBuffer image) throws IOException {
    requireCapability(CAP_ATOMIC_REPLACE);
    ByteBuffer checked = Objects.requireNonNull(image, "image");
    if (!checked.isDirect() || checked.position() != 0 || checked.limit() != checked.capacity())
      throw new IllegalArgumentException("exact direct-buffer slice required");
    return nativeReplaceImage(handle, Objects.requireNonNull(path, "path"), checked);
  }
  static int readIfInode(long handle, String path, long inode, long offset, byte[] bytes, int at, int length)
      throws IOException {
    requireCapability(CAP_CHECKED_INODE_IO);
    return nativeReadIfInode(handle, Objects.requireNonNull(path, "path"), inode, offset, bytes, at, length);
  }
  static long writeIfInode(long handle, String path, long inode, long offset, byte[] bytes, int at,
      int length, int flags) throws IOException {
    requireCapability(CAP_CHECKED_INODE_IO);
    return nativeWriteIfInode(handle, Objects.requireNonNull(path, "path"), inode, offset, bytes, at, length, flags);
  }
  private static native long nativeFeatureBits();
  private static native void nativePunchHole(long handle, String path, long offset, long length);
  private static native long[] nativeAllocatedRanges(long handle, String path, long offset, long length);
  private static native void nativeSetMetadata(long handle, String path, int mode, long time,
      boolean setMode, boolean setTime);
  private static native long nativeReplaceImage(long handle, String path, ByteBuffer image);
  private static native int nativeReadIfInode(long handle, String path, long inode, long offset,
      byte[] bytes, int at, int length);
  private static native long nativeWriteIfInode(long handle, String path, long inode, long offset,
      byte[] bytes, int at, int length, int flags);

  private M3FsNative() {}

  static boolean available() {
    Boolean current = available;
    if (current != null) return current;
    synchronized (M3FsNative.class) {
      current = available;
      if (current == null) {
        try {
          String explicit = System.getProperty("m3.fs.native.path");
          String pathFile = System.getProperty("m3.fs.native.pathFile");
          if ((explicit == null || explicit.isBlank()) && pathFile != null) {
            explicit = java.nio.file.Files.readString(java.nio.file.Path.of(pathFile)).strip();
          }
          if (explicit == null || explicit.isBlank()) System.loadLibrary("m3fs");
          else System.load(java.nio.file.Path.of(explicit).toAbsolutePath().normalize().toString());
          current = nativeAbiVersion() == ABI_VERSION;
        } catch (IOException | RuntimeException | LinkageError failure) {
          current = Boolean.FALSE;
        }
        available = current;
      }
      return current;
    }
  }

  static int capabilities() {
    if (!available()) return 0;
    Integer current = capabilities;
    if (current != null) return current;
    synchronized (M3FsNative.class) {
      current = capabilities;
      if (current == null) {
        try {
          current = nativeCapabilities();
        } catch (UnsatisfiedLinkError legacyAbi) {
          current = 0;
        }
        capabilities = current;
      }
      return current;
    }
  }

  private static void requireCapability(int capability) {
    requireAvailable();
    if ((capabilities() & capability) == 0) {
      throw new UnsupportedOperationException("native M3FS extension unavailable: " + capability);
    }
  }

  static long cloneFile(long handle, String from, String to) throws IOException {
    requireCapability(CAP_CLONE_FILE);
    return nativeCloneFile(handle, Objects.requireNonNull(from, "from"),
        Objects.requireNonNull(to, "to"));
  }

  static byte[] listPage(long handle, String path, String afterName, int limit) throws IOException {
    requireCapability(CAP_LIST_PAGE);
    if (limit < 1 || limit > MAX_PAGE_ENTRIES) throw new IllegalArgumentException("page limit");
    return nativeListPage(handle, Objects.requireNonNull(path, "path"), afterName, limit);
  }

  private static void requireAvailable() {
    if (!available()) {
      throw new IllegalStateException("native M3FS ABI is unavailable");
    }
  }

  static long open(String database, int flags) throws IOException {
    requireAvailable();
    if ((flags & OPEN_READ_SNAPSHOT) != 0) requireCapability(CAP_READ_SNAPSHOT);
    return nativeOpen(Objects.requireNonNull(database, "database"), flags);
  }

  static void close(long handle) {
    if (handle == 0L || !available()) return;
    nativeClose(handle);
  }

  static void mkdir(long handle, String path, int mode) throws IOException {
    requireAvailable();
    nativeMkdir(handle, Objects.requireNonNull(path, "path"), mode);
  }

  static void create(long handle, String path, int mode) throws IOException {
    requireAvailable();
    nativeCreate(handle, Objects.requireNonNull(path, "path"), mode);
  }

  static long write(
      long handle, String path, long offset, byte[] data, int dataOffset, int length) throws IOException {
    requireAvailable();
    return nativeWrite(
        handle,
        Objects.requireNonNull(path, "path"),
        offset,
        Objects.requireNonNull(data, "data"),
        dataOffset,
        length);
  }

  static int read(
      long handle, String path, long offset, byte[] output, int outputOffset, int length) throws IOException {
    requireAvailable();
    return nativeRead(
        handle,
        Objects.requireNonNull(path, "path"),
        offset,
        Objects.requireNonNull(output, "output"),
        outputOffset,
        length);
  }

  static long writeDirect(
      long handle, String path, long offset, ByteBuffer data, int bufferOffset, int length) throws IOException {
    requireAvailable();
    ByteBuffer checked = Objects.requireNonNull(data, "data");
    if (!checked.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
    return nativeWriteDirect(
        handle,
        Objects.requireNonNull(path, "path"),
        offset,
        checked,
        bufferOffset,
        length);
  }

  static int readDirect(
      long handle, String path, long offset, ByteBuffer output, int bufferOffset, int length) throws IOException {
    requireAvailable();
    ByteBuffer checked = Objects.requireNonNull(output, "output");
    if (!checked.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
    if (checked.isReadOnly()) throw new java.nio.ReadOnlyBufferException();
    return nativeReadDirect(
        handle,
        Objects.requireNonNull(path, "path"),
        offset,
        checked,
        bufferOffset,
        length);
  }

  /**
   * Write exact direct-buffer slices as one native transaction. Each buffer must already describe
   * its complete visible range with position zero and limit equal to capacity.
   */
  static long writeDirectSegments(
      long handle, String path, long offset, ByteBuffer[] segments) throws IOException {
    requireCapability(CAP_WRITEV);
    ByteBuffer[] checked = Objects.requireNonNull(segments, "segments");
    if (checked.length > MAX_WRITE_SEGMENTS) {
      throw new IllegalArgumentException("scatter/gather segment budget");
    }
    for (ByteBuffer segment : checked) {
      ByteBuffer value = Objects.requireNonNull(segment, "segment");
      if (!value.isDirect() || value.position() != 0 || value.limit() != value.capacity()) {
        throw new IllegalArgumentException("exact direct-buffer slice required");
      }
    }
    return nativeWriteDirectSegments(
        handle, Objects.requireNonNull(path, "path"), offset, checked);
  }

  /** Fill exact writable direct-buffer slices under one native read view. */
  static long readDirectSegments(
      long handle, String path, long offset, ByteBuffer[] segments) throws IOException {
    requireCapability(CAP_READV);
    if (offset < 0L) throw new IllegalArgumentException("offset");
    ByteBuffer[] checked = Objects.requireNonNull(segments, "segments");
    if (checked.length > MAX_READ_SEGMENTS) {
      throw new IllegalArgumentException("scatter/gather segment budget");
    }
    for (ByteBuffer segment : checked) {
      ByteBuffer value = Objects.requireNonNull(segment, "segment");
      if (!value.isDirect() || value.isReadOnly()
          || value.position() != 0 || value.limit() != value.capacity()) {
        throw new IllegalArgumentException("exact writable direct-buffer slice required");
      }
    }
    return nativeReadDirectSegments(
        handle, Objects.requireNonNull(path, "path"), offset, checked);
  }

  static long[] mindexInfo(long handle, String path) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    return nativeMIndexInfo(handle, Objects.requireNonNull(path, "path"));
  }

  static long[] mindexValueInfo(long handle, String path, int value) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    if (value < 0) throw new IndexOutOfBoundsException("value");
    return nativeMIndexValueInfo(handle, Objects.requireNonNull(path, "path"), value);
  }

  static long mindexToken(long handle, String path, int value, int ordinal) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    if (value < 0 || ordinal < 0) throw new IndexOutOfBoundsException();
    return nativeMIndexToken(handle, Objects.requireNonNull(path, "path"), value, ordinal);
  }

  static char mindexChar(long handle, String path, int value, int ordinal) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    if (value < 0 || ordinal < 0) throw new IndexOutOfBoundsException();
    return (char) nativeMIndexChar(handle, Objects.requireNonNull(path, "path"), value, ordinal);
  }

  static int mindexCompare(long handle, String path, int left, int right) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    if (left < 0 || right < 0) throw new IndexOutOfBoundsException();
    return nativeMIndexCompare(handle, Objects.requireNonNull(path, "path"), left, right);
  }

  static int mindexTokenIndexOf(
      long handle, String path, int value, long[] needle, int fromToken) throws IOException {
    requireCapability(CAP_NATIVE_MINDEX);
    if (value < 0 || fromToken < 0) throw new IndexOutOfBoundsException();
    return nativeMIndexTokenIndexOf(
        handle, Objects.requireNonNull(path, "path"), value,
        Objects.requireNonNull(needle, "needle"), fromToken);
  }

  static long[] regexMatchAll(
      long handle,
      String path,
      int mode,
      M3FsRegexImage.Image image)
      throws IOException {
    requireCapability(CAP_NATIVE_REGEX);
    var program = Objects.requireNonNull(image, "image");
    return nativeRegexMatchAll(
        handle,
        Objects.requireNonNull(path, "path"),
        mode,
        program.op(),
        program.out1(),
        program.out2(),
        program.classId(),
        program.asciiLow(),
        program.asciiHigh(),
        program.rangeOffset(),
        program.rangeCount(),
        program.rangeLow(),
        program.rangeHigh(),
        program.startState(),
        program.matchState(),
        program.minLength(),
        program.maxLength(),
        program.requiredPresence64(),
        program.mayConsumeSurrogateUnit());
  }

  static void truncate(long handle, String path, long size) throws IOException {
    requireAvailable();
    nativeTruncate(handle, Objects.requireNonNull(path, "path"), size);
  }

  static void unlink(long handle, String path) throws IOException {
    requireAvailable();
    nativeUnlink(handle, Objects.requireNonNull(path, "path"));
  }

  static void rmdir(long handle, String path) throws IOException {
    requireAvailable();
    nativeRmdir(handle, Objects.requireNonNull(path, "path"));
  }

  static void rename(long handle, String from, String to) throws IOException {
    requireAvailable();
    nativeRename(
        handle,
        Objects.requireNonNull(from, "from"),
        Objects.requireNonNull(to, "to"));
  }

  static void link(long handle, String existing, String newPath) throws IOException {
    requireAvailable();
    nativeLink(
        handle,
        Objects.requireNonNull(existing, "existing"),
        Objects.requireNonNull(newPath, "newPath"));
  }

  static void symlink(long handle, String target, String linkPath) throws IOException {
    requireAvailable();
    nativeSymlink(
        handle,
        Objects.requireNonNull(target, "target"),
        Objects.requireNonNull(linkPath, "linkPath"));
  }

  static byte[] readlink(long handle, String path) throws IOException {
    requireAvailable();
    return nativeReadlink(handle, Objects.requireNonNull(path, "path"));
  }

  static long[] stat(long handle, String path) throws IOException {
    requireAvailable();
    return nativeStat(handle, Objects.requireNonNull(path, "path"));
  }

  /** Native packed stat receipt; absent entrypoints on older ABI-1 libraries fail closed. */
  static long[] statBatch(long handle, String[] paths) throws IOException {
    requireAvailable();
    try {
      return nativeStatBatch(handle, Objects.requireNonNull(paths, "paths"));
    } catch (UnsatisfiedLinkError missingExtension) {
      throw new UnsupportedOperationException(
          "loaded M3FS native library has no stat-batch extension", missingExtension);
    }
  }

  static byte[] list(long handle, String path) throws IOException {
    requireAvailable();
    return nativeList(handle, Objects.requireNonNull(path, "path"));
  }

  static long[] stats(long handle) throws IOException {
    requireAvailable();
    return nativeStats(handle);
  }

  static long[] scrub(long handle, boolean backfillMissing) throws IOException {
    requireCapability(CAP_BLOCK_SCRUB);
    return nativeScrub(handle, backfillMissing);
  }

  static void verify(long handle) throws IOException {
    requireAvailable();
    nativeVerify(handle);
  }

  static void checkpoint(long handle) throws IOException {
    requireAvailable();
    nativeCheckpoint(handle);
  }

  private static native int nativeCapabilities();

  private static native long nativeCloneFile(long handle, String from, String to);

  private static native byte[] nativeListPage(long handle, String path, String afterName, int limit);

  private static native int nativeAbiVersion();

  private static native long nativeOpen(String database, int flags);

  private static native void nativeClose(long handle);

  private static native void nativeMkdir(long handle, String path, int mode);

  private static native void nativeCreate(long handle, String path, int mode);

  private static native long nativeWrite(
      long handle, String path, long offset, byte[] data, int dataOffset, int length);

  private static native int nativeRead(
      long handle, String path, long offset, byte[] output, int outputOffset, int length);

  private static native long nativeWriteDirect(
      long handle, String path, long offset, ByteBuffer data, int bufferOffset, int length);

  private static native int nativeReadDirect(
      long handle, String path, long offset, ByteBuffer output, int bufferOffset, int length);

  private static native long nativeWriteDirectSegments(
      long handle, String path, long offset, ByteBuffer[] segments);

  private static native long nativeReadDirectSegments(
      long handle, String path, long offset, ByteBuffer[] segments);

  private static native long[] nativeMIndexInfo(long handle, String path);

  private static native long[] nativeMIndexValueInfo(long handle, String path, int value);

  private static native long nativeMIndexToken(long handle, String path, int value, int ordinal);

  private static native int nativeMIndexChar(long handle, String path, int value, int ordinal);

  private static native int nativeMIndexCompare(long handle, String path, int left, int right);

  private static native int nativeMIndexTokenIndexOf(
      long handle, String path, int value, long[] needle, int fromToken);

  private static native long[] nativeRegexMatchAll(
      long handle,
      String path,
      int mode,
      byte[] op,
      int[] out1,
      int[] out2,
      int[] classId,
      long[] asciiLow,
      long[] asciiHigh,
      int[] rangeOffset,
      int[] rangeCount,
      char[] rangeLow,
      char[] rangeHigh,
      int startState,
      int matchState,
      int minLength,
      int maxLength,
      long requiredPresence64,
      boolean mayConsumeSurrogateUnit);

  private static native void nativeTruncate(long handle, String path, long size);

  private static native void nativeUnlink(long handle, String path);

  private static native void nativeRmdir(long handle, String path);

  private static native void nativeRename(long handle, String from, String to);

  private static native void nativeLink(long handle, String existing, String newPath);

  private static native void nativeSymlink(long handle, String target, String linkPath);

  private static native byte[] nativeReadlink(long handle, String path);

  private static native long[] nativeStat(long handle, String path);

  private static native long[] nativeStatBatch(long handle, String[] paths);

  private static native byte[] nativeList(long handle, String path);

  private static native long[] nativeStats(long handle);

  private static native long[] nativeScrub(long handle, boolean backfillMissing);

  private static native void nativeVerify(long handle);

  private static native void nativeCheckpoint(long handle);
}
