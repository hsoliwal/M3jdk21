// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import java.nio.ByteBuffer;

/** Common immutable storage ownership; views stay readable after their pool is closed. */
abstract class SharedArray {
  final ArrayHandle handle;
  final SharedSegments segments;
  final int language;
  private final int hash;
  SharedArray(ArrayHandle handle, SharedSegments segments, int language, int hash) {
    this.handle = handle; this.segments = segments; this.language = language; this.hash = hash;
  }
  public final ArrayHandle handle() { return handle; }
  public final int length() { return segments.length(); }
  public final int languageId() { return language; }
  public final int segmentCount() { return segments.data.length; }
  public final ByteBuffer[] asReadOnlyBuffers() { return segments.buffers(); }
  @Override public final int hashCode() { return hash; }
  @Override public final boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || other.getClass() != getClass()) return false;
    SharedArray that = (SharedArray) other;
    if (length() != that.length() || hash != that.hash) return false;
    for (int i = 0; i < length(); i++) {
      if (segments.unitAt(i) != that.segments.unitAt(i)) return false;
    }
    return true;
  }
  final ArrayHandle rangeHandle(int start, int end) {
    return new ArrayHandle(handle.namespace(), handle.recordOffset(), handle.start() + start, end - start);
  }
}
