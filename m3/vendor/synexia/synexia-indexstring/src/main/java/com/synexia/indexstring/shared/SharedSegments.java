// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Immutable primitive segment directory. Arrays here contain metadata, never copied payloads. */
final class SharedSegments {
  static final int MAX_SEGMENTS = 4096;
  final int width;
  final ByteBuffer[] data;
  final long[] ids;
  final int[] offsets;
  final int[] lengths;
  private static final ThreadLocal<LookupCursor> LOOKUP_CURSOR =
      ThreadLocal.withInitial(LookupCursor::new);
  final int[] ends;

  SharedSegments(int width, ByteBuffer[] data, long[] ids, int[] offsets, int[] lengths) {
    this.width = width;
    this.data = data;
    this.ids = ids;
    this.offsets = offsets;
    this.lengths = lengths;
    this.ends = new int[lengths.length];
    int total = 0;
    for (int i = 0; i < lengths.length; i++) {
      if (lengths[i] <= 0 || offsets[i] < 0
          || ((long) offsets[i] + lengths[i]) * width > data[i].capacity()) {
        throw new IllegalArgumentException("invalid segment");
      }
      ends[i] = total = Math.addExact(total, lengths[i]);
    }
  }
  int length() { return ends.length == 0 ? 0 : ends[ends.length - 1]; }
  int unitAt(int at) {
    return unitAt(at, LOOKUP_CURSOR.get());
  }
  /** Thread-confined segment locality state; it owns no payload or strong segment reference. */
  static final class LookupCursor {
    private java.lang.ref.WeakReference<SharedSegments> owner =
        new java.lang.ref.WeakReference<>(null);
    private int segment = -1;
    private int segmentStart;
    private int segmentEnd;

    // Package-private discriminator for the focused locality proof.
    int binarySearches;

    int locate(SharedSegments candidate, int absolute, int[] cumulativeEnds) {
      if (owner.get() == candidate) {
        if (absolute >= segmentStart && absolute < segmentEnd) return segment;
        int next = segment + 1;
        if (next < cumulativeEnds.length
            && absolute >= segmentEnd && absolute < cumulativeEnds[next]) {
          install(candidate, next, cumulativeEnds);
          return next;
        }
        int previous = segment - 1;
        if (previous >= 0) {
          int previousStart = previous == 0 ? 0 : cumulativeEnds[previous - 1];
          if (absolute >= previousStart && absolute < segmentStart) {
            install(candidate, previous, cumulativeEnds);
            return previous;
          }
        }
      }
      binarySearches++;
      int found = Arrays.binarySearch(cumulativeEnds, absolute + 1);
      int resolved = found < 0 ? -found - 1 : found;
      install(candidate, resolved, cumulativeEnds);
      return resolved;
    }

    private void install(SharedSegments candidate, int resolved, int[] cumulativeEnds) {
      if (owner.get() != candidate) {
        owner = new java.lang.ref.WeakReference<>(candidate);
      }
      segment = resolved;
      segmentStart = resolved == 0 ? 0 : cumulativeEnds[resolved - 1];
      segmentEnd = cumulativeEnds[resolved];
    }
  }

  int unitAt(int at, LookupCursor cursor) {
    Objects.checkIndex(at, length());
    int s = cursor.locate(this, at, ends);
    int local = offsets[s] + at - (s == 0 ? 0 : ends[s - 1]);
    if (width == 1) return data[s].get(local) & 255;
    int p = local * 2;
    return ((data[s].get(p) & 255) << 8) | (data[s].get(p + 1) & 255);
  }
  SharedSegments slice(int start, int end) {
    Objects.checkFromToIndex(start, end, length());
    if (start == 0 && end == length()) return this;
    // ends is an existing strictly increasing prefix index. Retain only participating owners.
    // No parent-sized temporary arrays, extra index, new payload or retained parent directory.
    int first = start == end ? 0 : firstSegment(start, 1);
    int count = start == end ? 0 : firstSegment(end - 1, 1) - first + 1;
    ByteBuffer[] b = new ByteBuffer[count];
    long[] r = new long[count];
    int[] o = new int[count], n = new int[count];
    for (int at = 0; at < count; at++) {
      int i = first + at, previous = i == 0 ? 0 : ends[i - 1];
      int from = Math.max(previous, start), to = Math.min(ends[i], end);
      b[at] = data[i]; r[at] = ids[i];
      o[at] = offsets[i] + from - previous; n[at] = to - from;
    }
    return new SharedSegments(width, b, r, o, n);
  }

  static SharedSegments join(int width, SharedArray[] arrays) {
    int count = 0;
    for (SharedArray a : arrays) count = Math.addExact(count, a.segments.data.length);
    if (count > MAX_SEGMENTS) throw new IllegalArgumentException("segment budget exceeded");
    ByteBuffer[] b = new ByteBuffer[count]; long[] r = new long[count];
    int[] o = new int[count], n = new int[count];
    int k = 0;
    for (SharedArray a : arrays) {
      SharedSegments s = a.segments;
      for (int i = 0; i < s.data.length; i++) {
        if (k > 0 && r[k - 1] == s.ids[i] && (long) o[k - 1] + n[k - 1] == s.offsets[i]) {
          n[k - 1] = Math.addExact(n[k - 1], s.lengths[i]);
        } else {
          b[k] = s.data[i]; r[k] = s.ids[i]; o[k] = s.offsets[i]; n[k++] = s.lengths[i];
        }
      }
    }
    return new SharedSegments(width, Arrays.copyOf(b, k), Arrays.copyOf(r, k),
        Arrays.copyOf(o, k), Arrays.copyOf(n, k));
  }
  /**
   * Ephemeral UTF-16 descriptor plan for one native copy. It retains no payload and
   * deliberately reuses the owner and directory arrays of this immutable value.
   */
  static final class Utf16RangePlan {
    final ByteBuffer[] owners;
    final int[] offsets;
    final int[] lengths;
    final int[] ends;
    final int firstSegment;
    final int lastSegmentExclusive;
    final int start;
    final int count;

    private Utf16RangePlan(ByteBuffer[] owners, int[] offsets, int[] lengths, int[] ends,
        int firstSegment, int lastSegmentExclusive, int start, int count) {
      this.owners = owners;
      this.offsets = offsets;
      this.lengths = lengths;
      this.ends = ends;
      this.firstSegment = firstSegment;
      this.lastSegmentExclusive = lastSegmentExclusive;
      this.start = start;
      this.count = count;
    }
  }

  Utf16RangePlan utf16RangePlan(int start, int count) {
    if (width != 2) throw new IllegalStateException("UTF-16 range plan requires width 2");
    Objects.checkFromIndexSize(start, count, length());
    if (count == 0) return new Utf16RangePlan(data, offsets, lengths, ends, 0, 0, start, count);
    int first = firstSegment(start, 1);
    int last = firstSegment(start + count - 1, 1) + 1;
    return new Utf16RangePlan(data, offsets, lengths, ends, first, last, start, count);
  }

  private volatile ByteBuffer[] preparedBuffers;

  ByteBuffer[] buffers() {
    ByteBuffer[] result = new ByteBuffer[data.length];
    for (int i = 0; i < data.length; i++) {
      ByteBuffer view = data[i].asReadOnlyBuffer();
      view.position(offsets[i] * width).limit((offsets[i] + lengths[i]) * width);
      result[i] = view.slice().asReadOnlyBuffer();
    }
    return result;
  }
  /**
   * Private complete-value descriptors for synchronous native calls.
   * Publication occurs only after every view is constructed; a failed construction
   * leaves the cache empty so a later call can retry.
   */
  ByteBuffer[] preparedBuffers() {
    ByteBuffer[] result = preparedBuffers;
    if (result != null) return result;
    synchronized (this) {
      result = preparedBuffers;
      if (result == null) {
        result = buffers();
        preparedBuffers = result;
      }
      return result;
    }
  }

  void copyTo(int start, byte[] target, int targetStart, int count) {
    int logical = start, remaining = count;
    for (int s = firstSegment(start, count); remaining > 0; s++) {
      int previous = s == 0 ? 0 : ends[s - 1];
      int take = Math.min(remaining, ends[s] - logical);
      data[s].get(offsets[s] + logical - previous, target, targetStart, take);
      logical += take; targetStart += take; remaining -= take;
    }
  }
  void copyTo(int start, char[] target, int targetStart, int count) {
    int logical = start, remaining = count;
    for (int s = firstSegment(start, count); remaining > 0; s++) {
      int previous = s == 0 ? 0 : ends[s - 1];
      int take = Math.min(remaining, ends[s] - logical);
      data[s].asCharBuffer().get(offsets[s] + logical - previous, target, targetStart, take);
      logical += take; targetStart += take; remaining -= take;
    }
  }
  void copyTo(int start, ByteBuffer target, int count) {
    int logical = start, remaining = count;
    for (int s = firstSegment(start, count); remaining > 0; s++) {
      int previous = s == 0 ? 0 : ends[s - 1];
      int take = Math.min(remaining, ends[s] - logical);
      target.put(data[s].slice(offsets[s] + logical - previous, take));
      logical += take; remaining -= take;
    }
  }
  void copyTo(int start, java.nio.CharBuffer target, int count) {
    int logical = start, remaining = count;
    for (int s = firstSegment(start, count); remaining > 0; s++) {
      int previous = s == 0 ? 0 : ends[s - 1];
      int take = Math.min(remaining, ends[s] - logical);
      target.put(data[s].asCharBuffer().slice(offsets[s] + logical - previous, take));
      logical += take; remaining -= take;
    }
  }
  private int firstSegment(int start, int count) {
    if (count == 0) return 0;
    int found = Arrays.binarySearch(ends, start + 1);
    return found >= 0 ? found : -found - 1;
  }
  int hash32() {
    int hash = width == 1 ? 1 : 0;
    ByteBuffer previous = null;
    int previousOffset = 0, previousLength = 0;
    int beforeRange = 0, afterRange = 0, power = 0, contribution = 0;
    for (int s = 0; s < data.length; s++) {
      // Identity means this exact live buffer/range, not a possibly colliding canonical ID.
      if (data[s] == previous && offsets[s] == previousOffset && lengths[s] == previousLength) {
        if (power == 0) {
          // 31^n is odd, so zero is an unambiguous not-yet-prepared marker.
          power = hash31Power(previousLength);
          contribution = afterRange - beforeRange * power;
        }
        hash = hash * power + contribution;
        continue;
      }
      previous = data[s]; previousOffset = offsets[s]; previousLength = lengths[s];
      beforeRange = hash;
      int end = (offsets[s] + lengths[s]) * width;
      for (int p = offsets[s] * width; p < end; p += width) {
        int unit = width == 1 ? data[s].get(p)
            : ((data[s].get(p) & 255) << 8) | (data[s].get(p + 1) & 255);
        hash = 31 * hash + unit;
      }
      afterRange = hash; power = 0;
    }
    return hash;
  }

  /** Prepare only on an exact repeat; unique ranges retain the original per-unit loop. */
  private static int hash31Power(int length) {
    int power = 1, factor = 31;
    for (int remaining = length; remaining != 0; remaining >>>= 1) {
      if ((remaining & 1) != 0) power *= factor;
      factor *= factor;
    }
    return power;
  }
}
