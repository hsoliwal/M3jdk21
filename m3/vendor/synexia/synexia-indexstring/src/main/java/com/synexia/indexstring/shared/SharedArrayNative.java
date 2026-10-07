// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import com.synexia.indexstring.IndexTextMetrics;
import com.synexia.job.IProgressMonitor;
import java.util.concurrent.CancellationException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/** Optional JNI kernels over read-only direct segment descriptors; no flattening or pointer escape. */
public final class SharedArrayNative {
  private static final int UTF8_REPLACEMENT_BYTES = StandardCharsets.UTF_8.newEncoder().replacement().length;
  private SharedArrayNative() {}
  /** Explicit opt-in only. The caller selects a trusted library; the pure Java API never loads one. */
  public static void load(Path library) {
    System.load(Objects.requireNonNull(library, "library").toAbsolutePath().toString());
  }
  public static int hash32(SharedChars chars) {
    return hash32Segments(Objects.requireNonNull(chars, "chars").segments.preparedBuffers(), 2);
  }
  public static int hash32(SharedBytes bytes) {
    return hash32Segments(Objects.requireNonNull(bytes, "bytes").segments.preparedBuffers(), 1);
  }
  /** Exact String.compareTo ordering over UTF-16 code units, without flattening either input. */
  public static int compare(SharedChars left, SharedChars right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    return compareUtf16Segments(left.segments.preparedBuffers(), right.segments.preparedBuffers());
  }
  /** Content comparison; neither namespace IDs nor hash equality is treated as proof. */
  public static boolean contentEquals(SharedChars left, SharedChars right) {
    return compare(left, right) == 0;
  }
  /** Inputs are private immutable descriptor arrays of sliced direct big-endian UTF-16 buffers. */
  static native int compareUtf16Segments(ByteBuffer[] left, ByteBuffer[] right);
  /** Each buffer is a sliced direct buffer: native traversal covers its complete capacity. */
  static native int hash32Segments(ByteBuffer[] segments, int width);
  /** Scan once in JNI and return the existing Java metric record, preserving surrogate seams. */
  public static IndexTextMetrics metrics(SharedChars chars) {
    int[] result = scanUtf16Segments(Objects.requireNonNull(chars, "chars").segments.preparedBuffers());
    return new IndexTextMetrics(result[0], result[1], result[2], result[3],
        result[4], result[5], result[6], result[7]);
  }
  static int[] scanUtf16Segments(ByteBuffer[] segments) {
    return scanUtf16Segments0(segments, UTF8_REPLACEMENT_BYTES);
  }
  private static native int[] scanUtf16Segments0(ByteBuffer[] segments, int replacementBytes);

  /**
   * Explicit contiguous-array export. The canonical source is unchanged; only this returned
   * mutable array owns new character payload. Uses the existing shared-array native library.
   */
  public static char[] copy(SharedChars source) { return copy(source, null); }

  public static char[] copy(SharedChars source, IProgressMonitor monitor) {
    Objects.requireNonNull(source, "source");
    copyCheckpoint(monitor);
    char[] result = new char[source.length()];
    copyTo(source, 0, result, 0, result.length, monitor);
    return result;
  }

  public static void copyTo(SharedChars source, int sourceStart,
      char[] target, int targetStart, int count) {
    copyTo(source, sourceStart, target, targetStart, count, null);
  }

  /**
   * Copy an exact UTF-16 range into caller-owned mutable storage without a joined intermediate.
   * Null/range checks precede mutation. No target-array pinning, boxed list or native pointer escapes.
   * Native byte loads decode canonical UTF-16BE regardless of host byte order/alignment.
   *
   * <p>Cancellation is cooperative before/during export. On cancellation or a native failure,
   * a prefix of the requested target range may have been written; outside that range stays
   * untouched. This is not a transaction or snapshot of concurrently modified native aliases.
   * The caller owns target synchronization and must keep source backing valid and immutable.
   * Zero-length export validates arguments/checkpoint but needs no loaded native library.
   */
  public static void copyTo(SharedChars source, int sourceStart, char[] target,
      int targetStart, int count, IProgressMonitor monitor) {
    Objects.requireNonNull(source, "source"); Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, source.length());
    Objects.checkFromIndexSize(targetStart, count, target.length);
    copyCheckpoint(monitor);
    if (count == 0) return;
    // Private exact-range descriptors, not a CharSequence export or retained character copy.
    SharedSegments.Utf16RangePlan plan = source.segments.utf16RangePlan(sourceStart, count);
    copyUtf16RangeTo0(plan.owners, plan.offsets, plan.lengths, plan.ends,
        plan.firstSegment, plan.lastSegmentExclusive, plan.start, target, targetStart,
        plan.count, monitor);
    copyCheckpoint(monitor);
  }

  // Called from JNI once per bounded transfer block; no JNI array critical region is held.
  private static void copyCheckpoint(IProgressMonitor monitor) {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("native export interrupted");
    if (monitor != null) monitor.checkCanceled();
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("native export interrupted");
  }

  private static native void copyUtf16SegmentsTo0(ByteBuffer[] segments,
      char[] target, int targetStart, int count, IProgressMonitor monitor);

  /** Native range export over existing owners; no sliced ByteBuffer descriptors are created. */
  private static native void copyUtf16RangeTo0(ByteBuffer[] owners, int[] offsets, int[] lengths,
      int[] ends, int firstSegment, int lastSegmentExclusive, int sourceStart, char[] target,
      int targetStart, int count, IProgressMonitor monitor);
}
