// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Bounded exact-byte JNI kernel with a behavior-identical Java fallback. */
public final class M3NativeContent {
  private static final int NATIVE_THRESHOLD =
      Integer.getInteger("m3fs.native.threshold", 64 * 1024);
  private static final int COMPARE_BUFFER_BYTES = 128 * 1024;
  private static final int GREP_NATIVE_SEGMENT_THRESHOLD =
      Integer.getInteger("m3fs.grep.native.threshold", 256);
  private static final int UTF16_NATIVE_SEGMENT_THRESHOLD =
      Integer.getInteger("m3fs.utf16.native.threshold", 256);
  private static final Mode MODE = Mode.parse(System.getProperty("m3fs.native", "auto"));

  private M3NativeContent() {}

  public static boolean filesEqual(Path left, Path right) throws IOException {
    Path first = Objects.requireNonNull(left, "left").toAbsolutePath().normalize();
    Path second = Objects.requireNonNull(right, "right").toAbsolutePath().normalize();
    try (FileChannel a = FileChannel.open(first, StandardOpenOption.READ);
        FileChannel b = FileChannel.open(second, StandardOpenOption.READ)) {
      long size = a.size();
      if (size != b.size()) return false;
      ByteBuffer am = ByteBuffer.allocateDirect((int) Math.min(size, COMPARE_BUFFER_BYTES));
      ByteBuffer bm = ByteBuffer.allocateDirect(am.capacity());
      for (long offset = 0L; offset < size; ) {
        int length = (int) Math.min(COMPARE_BUFFER_BYTES, size - offset);
        readFully(a, am, offset, length);
        readFully(b, bm, offset, length);
        if (!equalDirect(am, 0, bm, 0, length)) return false;
        offset += length;
      }
      return a.size() == size && b.size() == size;
    }
  }

  private static void readFully(FileChannel source, ByteBuffer target, long offset, int length)
      throws IOException {
    target.clear().limit(length);
    while (target.hasRemaining()) {
      int count = source.read(target, offset + target.position());
      if (count <= 0) {
        throw new IOException("M3 comparison source ended or made no progress");
      }
    }
  }

  /**
   * Compares capacity-relative byte spans without changing either caller's buffer state.
   *
   * <p>Position and limit are cursor metadata, not bounds for this explicit pointer/length API.
   * Pass a slice to restrict the accessible region. Inputs must not be concurrently mutated.</p>
   *
   * @param left first buffer
   * @param leftOffset byte offset from the first buffer's base
   * @param right second buffer
   * @param rightOffset byte offset from the second buffer's base
   * @param length byte count
   * @return whether the spans are exactly equal
   */
  public static boolean equalDirect(
      ByteBuffer left, int leftOffset, ByteBuffer right, int rightOffset, int length) {
    ByteBuffer a = Objects.requireNonNull(left, "left");
    ByteBuffer b = Objects.requireNonNull(right, "right");
    Objects.checkFromIndexSize(leftOffset, length, a.capacity());
    Objects.checkFromIndexSize(rightOffset, length, b.capacity());
    boolean direct = a.isDirect() && b.isDirect();
    boolean requireNative = MODE == Mode.REQUIRED;
    boolean tryNative = MODE != Mode.OFF && direct && (requireNative || length >= NATIVE_THRESHOLD);
    if (tryNative) {
      if (!M3FsNative.available()) {
        if (requireNative) {
          throw new IllegalStateException("M3FS native exact-compare is required but unavailable");
        }
      } else {
        try {
          int result = nativeEqualDirect(a, leftOffset, b, rightOffset, length);
          if (result >= 0) return result != 0;
          if (requireNative) {
            throw new IllegalStateException(
                "M3FS native exact-compare rejected validated geometry");
          }
        } catch (UnsatisfiedLinkError incompatibleLibrary) {
          if (requireNative) {
            throw new IllegalStateException(
                "M3FS native exact-compare symbol is unavailable", incompatibleLibrary);
          }
        }
      }
    } else if (requireNative) {
      throw new IllegalStateException("M3FS native exact-compare requires direct buffers");
    }
    ByteBuffer av = a.duplicate().clear().slice(leftOffset, length);
    ByteBuffer bv = b.duplicate().clear().slice(rightOffset, length);
    return av.mismatch(bv) == -1;
  }

  public static int grepSegments(
      ByteBuffer corpus,
      java.nio.LongBuffer offsets,
      java.nio.IntBuffer lengths,
      byte[] needle,
      int[] outputRows) {
    ByteBuffer bytes = Objects.requireNonNull(corpus, "corpus").slice();
    java.nio.LongBuffer starts = Objects.requireNonNull(offsets, "offsets").slice();
    java.nio.IntBuffer sizes = Objects.requireNonNull(lengths, "lengths").slice();
    byte[] pattern = Objects.requireNonNull(needle, "needle");
    int[] output = Objects.requireNonNull(outputRows, "outputRows");
    int segments = sizes.remaining();
    if (pattern.length == 0
        || starts.remaining() != segments + 1
        || output.length < segments) {
      throw new IllegalArgumentException("gpu-grep runtime geometry");
    }
    long terminal = starts.get(starts.position() + segments);
    if (starts.get(starts.position()) != 0L || terminal != bytes.remaining()) {
      throw new IllegalArgumentException("gpu-grep runtime bounds");
    }
    for (int segment = 0; segment < segments; segment++) {
      long from = starts.get(starts.position() + segment);
      long to = starts.get(starts.position() + segment + 1);
      int length = sizes.get(sizes.position() + segment);
      if (from < 0L || to < from || to - from != length) {
        throw new IllegalArgumentException("gpu-grep runtime segment geometry");
      }
    }

    if ((MODE == Mode.REQUIRED || segments >= GREP_NATIVE_SEGMENT_THRESHOLD)
        && MODE != Mode.OFF
        && bytes.isDirect()
        && starts.isDirect()
        && sizes.isDirect()
        && M3FsNative.available()) {
      try {
        int result =
            nativeGrepSegments(bytes, bigEndian(starts), bigEndian(sizes), segments, pattern, output);
        if (result >= 0) return result;
        if (MODE == Mode.REQUIRED) {
          throw new IllegalStateException("native gpu-grep segment scan rejected");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        if (MODE == Mode.REQUIRED) {
          throw new IllegalStateException(
              "native gpu-grep segment symbol unavailable", incompatibleLibrary);
        }
      }
    } else if (MODE == Mode.REQUIRED) {
      throw new IllegalStateException("native gpu-grep segment scan unavailable");
    }

    int matches = 0;
    for (int segment = 0; segment < segments; segment++) {
      int from = Math.toIntExact(starts.get(starts.position() + segment));
      int length = sizes.get(sizes.position() + segment);
      if (indexOf(bytes, from, length, pattern) >= 0) {
        output[matches++] = segment;
      }
    }
    return matches;
  }

  private static int indexOf(ByteBuffer bytes, int from, int length, byte[] needle) {
    if (needle.length > length) return -1;
    int limit = from + length - needle.length;
    for (int start = from; start <= limit; start++) {
      int offset = 0;
      while (offset < needle.length && bytes.get(start + offset) == needle[offset]) {
        offset++;
      }
      if (offset == needle.length) return start;
    }
    return -1;
  }

  /**
   * Explicit compatibility boundary: gather immutable mapped UTF-16 segments into one Java
   * char[]. Small sets remain in Java; large direct sets cross JNI once.
   */
  public static char[] materializeUtf16Segments(
      ByteBuffer[] parents,
      java.nio.IntBuffer parentIndexes,
      java.nio.IntBuffer parentStarts,
      java.nio.IntBuffer lengths,
      int totalChars) {
    Objects.requireNonNull(parents, "parents");
    ByteBuffer[] slices = new ByteBuffer[parents.length];
    for (int i = 0; i < parents.length; i++) slices[i] = Objects.requireNonNull(parents[i], "parent").slice();
    parents = slices;
    java.nio.IntBuffer indexes =
        Objects.requireNonNull(parentIndexes, "parentIndexes").slice();
    java.nio.IntBuffer starts =
        Objects.requireNonNull(parentStarts, "parentStarts").slice();
    java.nio.IntBuffer sizes =
        Objects.requireNonNull(lengths, "lengths").slice();
    int segments = sizes.remaining();
    if (totalChars < 0
        || indexes.remaining() != segments
        || starts.remaining() != segments) {
      throw new IllegalArgumentException("segmented UTF-16 materialization geometry");
    }

    int cumulative = 0;
    boolean direct = indexes.isDirect() && starts.isDirect() && sizes.isDirect();
    for (ByteBuffer parent : parents) {
      ByteBuffer checked = Objects.requireNonNull(parent, "parent");
      if ((checked.remaining() & 1) != 0) {
        throw new IllegalArgumentException("UTF-16 parent byte geometry");
      }
      direct &= checked.isDirect();
    }
    for (int segment = 0; segment < segments; segment++) {
      int parent = indexes.get(indexes.position() + segment);
      int start = starts.get(starts.position() + segment);
      int length = sizes.get(sizes.position() + segment);
      if (parent < 0
          || parent >= parents.length
          || start < 0
          || length < 0
          || start > parents[parent].remaining() / Character.BYTES - length) {
        throw new IllegalArgumentException("segmented UTF-16 materialization range");
      }
      cumulative = Math.addExact(cumulative, length);
    }
    if (cumulative != totalChars) {
      throw new IllegalArgumentException("segmented UTF-16 materialization length");
    }

    boolean requireNative = MODE == Mode.REQUIRED;
    boolean tryNative =
        MODE != Mode.OFF
            && direct
            && M3FsNative.available()
            && (requireNative || segments >= UTF16_NATIVE_SEGMENT_THRESHOLD);
    if (tryNative) {
      try {
        char[] result =
            nativeMaterializeUtf16Segments(
                parents, bigEndian(indexes), bigEndian(starts), bigEndian(sizes), segments, totalChars);
        if (result != null) return result;
        if (requireNative) {
          throw new IllegalStateException("native segmented UTF-16 gather rejected");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        if (requireNative) {
          throw new IllegalStateException(
              "native segmented UTF-16 symbol unavailable", incompatibleLibrary);
        }
      }
    } else if (requireNative) {
      throw new IllegalStateException("native segmented UTF-16 gather unavailable");
    }

    char[] result = new char[totalChars];
    int cursor = 0;
    for (int segment = 0; segment < segments; segment++) {
      int parent = indexes.get(indexes.position() + segment);
      int start = starts.get(starts.position() + segment);
      int length = sizes.get(sizes.position() + segment);
      ByteBuffer source =
          parents[parent].duplicate().order(java.nio.ByteOrder.BIG_ENDIAN);
      for (int offset = 0; offset < length; offset++) {
        result[cursor++] = source.getChar((start + offset) * Character.BYTES);
      }
    }
    return result;
  }

  private static java.nio.IntBuffer bigEndian(java.nio.IntBuffer values) {
    if (values.order() == java.nio.ByteOrder.BIG_ENDIAN) return values;
    java.nio.IntBuffer result = ByteBuffer.allocateDirect(Math.multiplyExact(values.remaining(), Integer.BYTES))
        .order(java.nio.ByteOrder.BIG_ENDIAN).asIntBuffer();
    result.put(values.duplicate()).flip(); return result;
  }
  private static java.nio.LongBuffer bigEndian(java.nio.LongBuffer values) {
    if (values.order() == java.nio.ByteOrder.BIG_ENDIAN) return values;
    java.nio.LongBuffer result = ByteBuffer.allocateDirect(Math.multiplyExact(values.remaining(), Long.BYTES))
        .order(java.nio.ByteOrder.BIG_ENDIAN).asLongBuffer();
    result.put(values.duplicate()).flip(); return result;
  }

  private static native char[] nativeMaterializeUtf16Segments(
      ByteBuffer[] parents,
      java.nio.IntBuffer parentIndexes,
      java.nio.IntBuffer parentStarts,
      java.nio.IntBuffer lengths,
      int segmentCount,
      int totalChars);

  private static native int nativeGrepSegments(
      ByteBuffer corpus,
      java.nio.LongBuffer offsets,
      java.nio.IntBuffer lengths,
      int segmentCount,
      byte[] needle,
      int[] outputRows);

  private static native int nativeEqualDirect(
      ByteBuffer left, int leftOffset, ByteBuffer right, int rightOffset, int length);

  private enum Mode {
    OFF,
    AUTO,
    REQUIRED;

    static Mode parse(String value) {
      return switch (
          Objects.requireNonNull(value, "value")
              .trim()
              .toLowerCase(java.util.Locale.ROOT)) {
        case "off" -> OFF;
        case "auto" -> AUTO;
        case "required" -> REQUIRED;
        default ->
            throw new IllegalArgumentException(
                "m3fs.native must be off, auto or required");
      };
    }
  }
}
