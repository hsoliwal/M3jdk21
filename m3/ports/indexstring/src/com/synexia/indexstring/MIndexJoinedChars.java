// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Objects;

/** Immutable UTF-16 view over frozen segments, on heap or OS-shared mapped storage. */
public final class MIndexJoinedChars implements CharSequence {
  private final MIndexJoinedStorageIntern.CharBody body;
  private final int start;
  private final int length;

  private MIndexJoinedChars(MIndexJoinedStorageIntern.CharBody body, int start, int length) {
    this.body = Objects.requireNonNull(body, "body"); this.start = start; this.length = length;
  }
  static MIndexJoinedChars full(MIndexJoinedStorageIntern.CharBody body) {
    return new MIndexJoinedChars(body, 0, body.length);
  }
  public static MIndexJoinedChars of(FrozenChars... parts) {
    return MIndexJoinedStorageIntern.internChars(Objects.requireNonNull(parts, "parts")).canonicalView();
  }
  /** Additive M3 policy: canonicalize normalized ranges without replacing their owners. */
  public static MIndexJoinedChars ofRetained(FrozenChars... parts) {
    return MIndexJoinedStorageIntern.internRetainedChars(parts).canonicalView();
  }
  /** Fresh descriptor array; partial pieces retain the same frozen payload owners. */
  public FrozenChars[] retainedParts() {
    if (length == 0) return new FrozenChars[0];
    int first = segmentAt(start), last = segmentAt(start + length - 1);
    FrozenChars[] result = new FrozenChars[last - first + 1];
    for (int segment = first; segment <= last; segment++) {
      int previous = segment == 0 ? 0 : body.ends[segment - 1];
      int from = Math.max(start, previous) - previous;
      int to = Math.min(start + length, body.ends[segment]) - previous;
      result[segment - first] = body.segments[segment].subSequence(from, to);
    }
    return result;
  }
  /** Full atom hashes compose modulo 2^32; partial atom hashes require a scan. */
  public int stringHash() {
    int hash = 0;
    for (FrozenChars part : retainedParts()) hash = hash * FrozenChars.power31(part.length()) + part.hash32();
    return hash;
  }
  /** Copy directly to the requested mutable destination, without a flattened cache. */
  public void copyTo(int sourceStart, char[] target, int targetStart, int count) {
    Objects.requireNonNull(target, "target");
    Objects.checkFromIndexSize(sourceStart, count, length);
    Objects.checkFromIndexSize(targetStart, count, target.length);
    if (count == 0) return;
    int absolute = start + sourceStart, written = 0, segment = segmentAt(absolute);
    while (written < count) {
      int previous = segment == 0 ? 0 : body.ends[segment - 1];
      int take = Math.min(count - written, body.ends[segment] - absolute);
      body.segments[segment].copyTo(absolute - previous, target, targetStart + written, take);
      written += take; absolute += take; segment++;
    }
  }
  /** Process-local identity of the canonical segment tuple, not an OS or sliced-value ID. */
  public long canonicalBackingId() { return body.canonicalId; }
  public boolean sharesBackingWith(MIndexJoinedChars other) {
    return body == Objects.requireNonNull(other, "other").body;
  }

  @Override public int length() { return length; }
  @Override public char charAt(int index) {
    int absolute = start + Objects.checkIndex(index, length);
    int segment = segmentAt(absolute), previous = segment == 0 ? 0 : body.ends[segment - 1];
    return body.segments[segment].charAt(absolute - previous);
  }
  @Override public MIndexJoinedChars subSequence(int from, int to) {
    Objects.checkFromToIndex(from, to, length);
    return from == 0 && to == length ? this
        : new MIndexJoinedChars(body, start + from, to - from);
  }
  public int codePointAt(int index) {
    Objects.checkIndex(index, length); return Character.codePointAt(this, index);
  }
  public int codePointCount() { return Character.codePointCount(this, 0, length); }
  public char[] copy() {
    char[] result = new char[length];
    int absolute = start, written = 0;
    while (written < length) {
      int segment = segmentAt(absolute), previous = segment == 0 ? 0 : body.ends[segment - 1];
      int take = Math.min(length - written, body.ends[segment] - absolute);
      body.segments[segment].copyTo(absolute - previous, result, written, take);
      absolute += take; written += take;
    }
    return result;
  }
  public FrozenChars detach() { return FrozenChars.owned(copy()); }
  /** One logical charset operation preserves encoder state and cross-segment surrogate pairs. */
  public FrozenBytes encode(Charset charset) {
    var encoder = Objects.requireNonNull(charset, "charset").newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    try {
      var encoded = encoder.encode(CharBuffer.wrap(this));
      byte[] result = new byte[encoded.remaining()]; encoded.get(result);
      return FrozenBytes.owned(result);
    } catch (CharacterCodingException impossible) {
      throw new IllegalStateException("replacement encoder failed", impossible);
    }
  }
  /** Independent UTF-16 cursors; direct when their backing is mapped. */
  public CharBuffer[] asReadOnlyBuffers() {
    if (length == 0) return new CharBuffer[0];
    int first = segmentAt(start), last = segmentAt(start + length - 1);
    CharBuffer[] result = new CharBuffer[last - first + 1];
    for (int segment = first; segment <= last; segment++) {
      int previous = segment == 0 ? 0 : body.ends[segment - 1];
      int from = Math.max(start, previous) - previous;
      int to = Math.min(start + length, body.ends[segment]) - previous;
      result[segment - first] = body.segments[segment].asReadOnlyBuffer().slice(from, to - from);
    }
    return result;
  }
  /**
   * Zero-copy JNI descriptors: ByteBuffer capacities are BYTES, units are UTF-16BE.
   * Throws for heap-backed participating segments rather than silently flattening/copying.
   */
  public ByteBuffer[] directUtf16Buffers() {
    if (length == 0) return new ByteBuffer[0];
    int first = segmentAt(start), last = segmentAt(start + length - 1);
    ByteBuffer[] result = new ByteBuffer[last - first + 1];
    for (int segment = first; segment <= last; segment++) {
      int previous = segment == 0 ? 0 : body.ends[segment - 1];
      int from = Math.max(start, previous) - previous;
      int to = Math.min(start + length, body.ends[segment]) - previous;
      result[segment - first] = body.segments[segment].directUtf16Bytes().slice(2 * from, 2 * (to - from));
    }
    return result;
  }
  @Override public String toString() { return new String(copy()); }
  private int segmentAt(int absolute) {
    int result = Arrays.binarySearch(body.ends, absolute + 1);
    return result >= 0 ? result : -result - 1;
  }
  /** Independent ordinary Java I/O cursor; borrows immutable payloads without flattening. */
  public java.io.Reader asReader() {
    return MIndexJoinedStreams.chars(asReadOnlyBuffers(), length);
  }

}
