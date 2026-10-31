// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

/** Persisted value or explicit negative result for one deterministic precompute key. */
public final class M3PrecomputeResult {
  private static final int FORMAT = 1;

  public enum State {
    VALUE,
    EMPTY
  }

  private final M3ContentId keyId;
  private final State state;
  private final M3ObjectRef output;

  private M3PrecomputeResult(M3ContentId keyId, State state, M3ObjectRef output) {
    this.keyId = Objects.requireNonNull(keyId, "keyId");
    this.state = Objects.requireNonNull(state, "state");
    this.output = output;
    if ((state == State.VALUE) != (output != null)) {
      throw new IllegalArgumentException("VALUE requires output and EMPTY forbids output");
    }
  }

  public static M3PrecomputeResult value(M3PrecomputeKey key, M3ObjectRef output) {
    return new M3PrecomputeResult(
        Objects.requireNonNull(key, "key").id(),
        State.VALUE,
        Objects.requireNonNull(output, "output"));
  }

  public static M3PrecomputeResult empty(M3PrecomputeKey key) {
    return new M3PrecomputeResult(Objects.requireNonNull(key, "key").id(), State.EMPTY, null);
  }

  public M3ContentId keyId() { return keyId; }
  public State state() { return state; }
  public boolean isEmpty() { return state == State.EMPTY; }

  public M3ObjectRef output() {
    if (output == null) {
      throw new IllegalStateException("negative precompute result has no output");
    }
    return output;
  }

  public byte[] canonicalBytes() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(96);
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT);
        out.write(keyId.bytes());
        out.writeByte(state.ordinal());
        if (output != null) {
          out.write(output.id().bytes());
          out.writeLong(output.size());
          out.writeInt(output.kind().ordinal());
        }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public static M3PrecomputeResult decode(byte[] encoded) throws IOException {
    Objects.requireNonNull(encoded, "encoded");
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
      if (in.readInt() != FORMAT) {
        throw new IOException("unsupported M3 precompute result format");
      }
      byte[] key = in.readNBytes(M3ContentId.BYTES);
      if (key.length != M3ContentId.BYTES) throw new IOException("truncated precompute key");
      int stateOrdinal = in.readUnsignedByte();
      if (stateOrdinal >= State.values().length) throw new IOException("invalid precompute state");
      State state = State.values()[stateOrdinal];
      M3ObjectRef output = null;
      if (state == State.VALUE) {
        byte[] object = in.readNBytes(M3ContentId.BYTES);
        if (object.length != M3ContentId.BYTES) throw new IOException("truncated precompute output");
        long size = in.readLong();
        int kindOrdinal = in.readInt();
        if (size < 0L || kindOrdinal < 0 || kindOrdinal >= M3ObjectKind.values().length) {
          throw new IOException("invalid precompute output reference");
        }
        output =
            new M3ObjectRef(
                M3ContentId.of(object), size, M3ObjectKind.values()[kindOrdinal]);
      }
      if (in.read() != -1) throw new IOException("trailing precompute result bytes");
      return new M3PrecomputeResult(M3ContentId.of(key), state, output);
    }
  }
}
