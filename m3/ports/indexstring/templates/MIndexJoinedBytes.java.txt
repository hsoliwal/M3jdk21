// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Immutable byte concatenation view over frozen segments. */
public final class MIndexJoinedBytes {
  private final MIndexJoinedStorageIntern.ByteBody body;
  private final int start;
  private final int length;

  private MIndexJoinedBytes(MIndexJoinedStorageIntern.ByteBody body, int start, int length) {
    this.body = Objects.requireNonNull(body, "body"); this.start = start; this.length = length;
  }
  static MIndexJoinedBytes full(MIndexJoinedStorageIntern.ByteBody body) {
    return new MIndexJoinedBytes(body, 0, body.length);
  }
  public static MIndexJoinedBytes of(FrozenBytes... parts) {
    return MIndexJoinedStorageIntern.internBytes(Objects.requireNonNull(parts, "parts")).canonicalView();
  }
  /** Process-local identity of the canonical segment tuple, not an OS or sliced-value ID. */
  public long canonicalBackingId() { return body.canonicalId; }
  public boolean sharesBackingWith(MIndexJoinedBytes other) {
    return body == Objects.requireNonNull(other, "other").body;
  }

  public int length() { return length; }
  public byte byteAt(int index) {
    int absolute = start + Objects.checkIndex(index, length);
    int segment = segmentAt(absolute), previous = segment == 0 ? 0 : body.ends[segment - 1];
    return body.segments[segment].byteAt(absolute - previous);
  }
  public MIndexJoinedBytes slice(int from, int to) {
    Objects.checkFromToIndex(from, to, length);
    return from == 0 && to == length ? this
        : new MIndexJoinedBytes(body, start + from, to - from);
  }
  public byte[] copy() {
    byte[] result = new byte[length];
    int absolute = start, written = 0;
    while (written < length) {
      int segment = segmentAt(absolute), previous = segment == 0 ? 0 : body.ends[segment - 1];
      int take = Math.min(length - written, body.ends[segment] - absolute);
      body.segments[segment].copyTo(absolute - previous, result, written, take);
      absolute += take; written += take;
    }
    return result;
  }
  public FrozenBytes detach() { return FrozenBytes.owned(copy()); }
  /** Independent read-only cursors for gathering output/JNI; no payload copy or slice hashing. */
  public ByteBuffer[] asReadOnlyBuffers() {
    if (length == 0) return new ByteBuffer[0];
    int first = segmentAt(start), last = segmentAt(start + length - 1);
    ByteBuffer[] result = new ByteBuffer[last - first + 1];
    for (int segment = first; segment <= last; segment++) {
      int previous = segment == 0 ? 0 : body.ends[segment - 1];
      int from = Math.max(start, previous) - previous;
      int to = Math.min(start + length, body.ends[segment]) - previous;
      result[segment - first] = body.segments[segment].asReadOnlyBuffer().slice(from, to - from);
    }
    return result;
  }
  private int segmentAt(int absolute) {
    int result = Arrays.binarySearch(body.ends, absolute + 1);
    return result >= 0 ? result : -result - 1;
  }
  /** Independent ordinary Java I/O cursor; borrows immutable payloads without flattening. */
  public java.io.InputStream asInputStream() {
    return MIndexJoinedStreams.bytes(asReadOnlyBuffers(), length);
  }

}
