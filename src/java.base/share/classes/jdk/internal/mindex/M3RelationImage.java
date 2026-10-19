// M3JDK21 adaptation: package/type/scope/progress boundary mapping; Synexia owns the source.
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.internal.mindex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Wire codec for already-qualified Boolean relation rows, not a DB, cache or proof gate.
 * Images contain numeric effects and a scope fingerprint, never text or process-local identity.
 * Reuse MIndexStateRelation.rows()/Domain.of(...) at the typed boundary and the existing
 * MIndexPrecomputeCache for optional residency. A supplied scope must identify the exact
 * automaton/state ordering, source/range/mask and versions; state count alone is insufficient.
 * The expected image digest MUST come from a separate trusted publication/receipt.
 */
final class M3RelationImage {
  private static final long MAGIC = 0x4d33524930303031L; // M3RI0001: format and semantics version.
  private static final int HEADER_BYTES = Long.BYTES + Integer.BYTES + 32;
  private static final int MAX_BYTES = HEADER_BYTES + Long.SIZE * Long.BYTES;

  private M3RelationImage() { }

  /** Canonical big-endian snapshot. Input rows must remain stable during this call. */
  static byte[] encode(long[] rows, String scope, Runnable checkpoint) {
    Objects.requireNonNull(rows, "rows");
    check(checkpoint);
    int states = rows.length;
    requireStates(states);
    byte[] scopeBytes = digestBytes(Objects.requireNonNull(scope, "scope"));
    long[] stable = rows.clone();
    validate(stable, states, checkpoint);
    ByteBuffer output = ByteBuffer.allocate(HEADER_BYTES + Long.BYTES * states)
        .order(ByteOrder.BIG_ENDIAN);
    output.putLong(MAGIC).putInt(states).put(scopeBytes);
    for (long row : stable) {
      check(checkpoint);
      output.putLong(row);
    }
    check(checkpoint);
    return output.array();
  }

  /**
   * Bounded snapshot before checking. Refuses incompatible context, state order/version identity,
   * geometry or content. Hash equality verifies receipt integrity, NOT original semantic truth.
   * A new numeric array is returned only after every field and row passes validation.
   */
  static long[] decode(byte[] image, String expectedImageSha256, String expectedScope,
      int expectedStates, Runnable checkpoint) {
    Objects.requireNonNull(image, "image");
    check(checkpoint);
    requireStates(expectedStates);
    byte[] expectedHash = digestBytes(expectedImageSha256);
    byte[] expectedScopeBytes = digestBytes(Objects.requireNonNull(expectedScope, "expectedScope"));
    if (image.length > MAX_BYTES || image.length != HEADER_BYTES + Long.BYTES * expectedStates) {
      throw new IllegalArgumentException("relation image byte geometry");
    }
    // Snapshot once: callbacks or later caller mutations cannot invalidate checked bytes.
    byte[] stable = image.clone();
    check(checkpoint);
    if (!MessageDigest.isEqual(expectedHash, sha256(stable))) {
      throw new IllegalArgumentException("relation image receipt mismatch");
    }
    ByteBuffer input = ByteBuffer.wrap(stable).order(ByteOrder.BIG_ENDIAN);
    if (input.getLong() != MAGIC || input.getInt() != expectedStates) {
      throw new IllegalArgumentException("relation image version/state mismatch");
    }
    byte[] scope = new byte[32];
    input.get(scope);
    if (!MessageDigest.isEqual(scope, expectedScopeBytes)) {
      throw new IllegalArgumentException("relation image semantic scope mismatch");
    }
    long[] rows = new long[expectedStates];
    for (int row = 0; row < expectedStates; row++) {
      check(checkpoint);
      rows[row] = input.getLong();
    }
    validate(rows, expectedStates, checkpoint);
    check(checkpoint);
    return rows;
  }

  private static void requireStates(int states) {
    if (states < 1 || states > Long.SIZE) {
      throw new IllegalArgumentException("relation image state count must be in [1,64]");
    }
  }

  private static void validate(long[] rows, int states, Runnable checkpoint) {
    long mask = states == Long.SIZE ? -1L : (1L << states) - 1L;
    for (long row : rows) {
      check(checkpoint);
      if ((row & ~mask) != 0L) throw new IllegalArgumentException("relation image foreign state bits");
    }
  }

  private static byte[] digestBytes(String value) {
    Objects.requireNonNull(value, "SHA-256 fingerprint");
    if (value.length() != 64) throw new IllegalArgumentException("expected SHA-256 fingerprint");
    return HexFormat.of().parseHex(value);
  }

  private static byte[] sha256(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void check(Runnable checkpoint) {
    if (checkpoint != null) checkpoint.run();
  }
}
