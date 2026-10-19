// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Persisted format-1 result columns; key and output value wrappers are API projections. */
public final class M3PrecomputeResult {
  private static final int FORMAT = 1;
  public enum State { VALUE, EMPTY }
  private final byte[] ids;
  private final int state;
  private final long size;
  private final int kind;

  private M3PrecomputeResult(byte[] ids, int state, long size, int kind) {
    this.ids = ids;
    this.state = state;
    this.size = size;
    this.kind = kind;
  }

  public static M3PrecomputeResult value(M3PrecomputeKey key, M3ObjectRef output) {
    Objects.requireNonNull(output, "output");
    byte[] ids = new byte[64];
    Objects.requireNonNull(key, "key").copyIdTo(ids, 0);
    System.arraycopy(output.id().bytes(), 0, ids, 32, 32);
    return new M3PrecomputeResult(ids, 0, output.size(), output.kind().ordinal());
  }

  public static M3PrecomputeResult empty(M3PrecomputeKey key) {
    byte[] ids = new byte[32];
    Objects.requireNonNull(key, "key").copyIdTo(ids, 0);
    return new M3PrecomputeResult(ids, 1, 0, 0);
  }

  public M3ContentId keyId() { return M3ContentId.of(Arrays.copyOf(ids, 32)); }
  public State state() { return State.values()[state]; }
  public boolean isEmpty() { return state == 1; }
  public M3ObjectRef output() {
    if (isEmpty()) throw new IllegalStateException("negative precompute result has no output");
    return new M3ObjectRef(M3ContentId.of(Arrays.copyOfRange(ids, 32, 64)), size, M3ObjectKind.values()[kind]);
  }

  public byte[] canonicalBytes() {
    ByteBuffer out = ByteBuffer.allocate(isEmpty() ? 37 : 81);
    out.putInt(FORMAT).put(ids, 0, 32).put((byte) state);
    if (!isEmpty()) out.put(ids, 32, 32).putLong(size).putInt(kind);
    return out.array();
  }

  public static M3PrecomputeResult decode(byte[] encoded) throws IOException {
    byte[] copy = Objects.requireNonNull(encoded, "encoded").clone();
    if (copy.length != 37 && copy.length != 81) throw new IOException("invalid precompute result length");
    ByteBuffer in = ByteBuffer.wrap(copy);
    if (in.getInt() != FORMAT) throw new IOException("unsupported M3 precompute result format");
    int state = Byte.toUnsignedInt(copy[36]);
    if ((state != 0 && state != 1) || copy.length != (state == 1 ? 37 : 81)) {
      throw new IOException("invalid precompute state/shape");
    }
    byte[] ids = new byte[state == 1 ? 32 : 64];
    System.arraycopy(copy, 4, ids, 0, 32);
    long size = 0;
    int kind = 0;
    if (state == 0) {
      System.arraycopy(copy, 37, ids, 32, 32);
      size = in.getLong(69);
      kind = in.getInt(77);
      if (size < 0 || kind < 0 || kind >= M3ObjectKind.values().length) {
        throw new IOException("invalid precompute output reference");
      }
    }
    return new M3PrecomputeResult(ids, state, size, kind);
  }
}
