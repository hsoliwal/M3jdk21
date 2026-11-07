// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;

/**
 * Immutable fixed-arity tuple -> int index.
 *
 * <p>Runtime state is primitive-only. Lookup is expected O(1) with exact tuple confirmation.
 * Arity 1..4 is deliberate: it covers tokens, pairs, triples, n-grams and most semantic relation
 * keys without per-query object allocation.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/FrozenLongTupleIndex.java blob 3b3da91caaec; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3FrozenLongTupleIndex implements M3LongTupleIndexView {
  private final M3IndexPrecomputeCapability capability;
  private final int arity;
  private final long[] keys;
  private final int[] values;
  private final int[] slots;
  private final String rootHash;

  private M3FrozenLongTupleIndex(
      M3IndexPrecomputeCapability capability,
      int arity,
      long[] keys,
      int[] values,
      int[] slots) {
    this.capability = capability;
    this.arity = arity;
    this.keys = keys;
    this.values = values;
    this.slots = slots;
    this.rootHash = M3PrecomputeHashes.sha256(
        "M3_FROZEN_LONG_TUPLE_V1:" + capability.id() + ":" + arity,
        keys,
        values);
  }

  public static M3FrozenLongTupleIndex freeze(
      M3IndexPrecomputeCapability capability,
      int arity,
      long[] flattenedKeys,
      int[] values) {
    Objects.requireNonNull(capability, "capability");
    Objects.requireNonNull(flattenedKeys, "flattenedKeys");
    Objects.requireNonNull(values, "values");
    if (arity < 1 || arity > 4) throw new IllegalArgumentException("arity must be 1..4");
    if (flattenedKeys.length != Math.multiplyExact(values.length, arity)) {
      throw new IllegalArgumentException("flattened key/value cardinality mismatch");
    }

    long[] keys = flattenedKeys.clone();
    int[] copiedValues = values.clone();
    int capacity = 2;
    long required = Math.max(2L, (long) values.length * 2L);
    if (required > (1L << 30)) throw new IllegalArgumentException("tuple index too large");
    while (capacity < required) capacity <<= 1;
    int[] slots = new int[capacity];

    for (int row = 0; row < values.length; row++) {
      int offset = row * arity;
      long rowHash = arity == 2
          ? hashPair(keys[offset], keys[offset + 1])
          : hash(keys, offset, arity);
      int slot = M3PrecomputeHashes.slot(rowHash, capacity - 1);
      while (slots[slot] != 0) {
        int existing = slots[slot] - 1;
        if (same(keys, existing * arity, keys, row * arity, arity)) {
          throw new IllegalArgumentException("duplicate tuple row " + row);
        }
        slot = (slot + 1) & (capacity - 1);
      }
      slots[slot] = row + 1;
    }
    return new M3FrozenLongTupleIndex(capability, arity, keys, copiedValues, slots);
  }

  @Override public M3IndexPrecomputeCapability capability() { return capability; }
  @Override public String rootHash() { return rootHash; }
  @Override public long retainedBytes() {
    return (long) keys.length * Long.BYTES
        + (long) values.length * Integer.BYTES
        + (long) slots.length * Integer.BYTES;
  }

  public int arity() { return arity; }
  public int size() { return values.length; }
  public int capacity() { return slots.length; }

  public int get(long k1, int defaultValue) {
    requireArity(1);
    return getScalars(k1, 0L, 0L, 0L, defaultValue);
  }

  public int get(long k1, long k2, int defaultValue) {
    requireArity(2);
    return getPair(k1, k2, defaultValue);
  }

  public int get(long k1, long k2, long k3, int defaultValue) {
    requireArity(3);
    return getScalars(k1, k2, k3, 0L, defaultValue);
  }

  public int get(long k1, long k2, long k3, long k4, int defaultValue) {
    requireArity(4);
    return getScalars(k1, k2, k3, k4, defaultValue);
  }

  /** Allocation-free arity-2 lookup used by Java/JNI batch paths. */
  public int getPair(long left, long right, int defaultValue) {
    requireArity(2);
    int mask = slots.length - 1;
    int slot = M3PrecomputeHashes.slot(hashPair(left, right), mask);
    for (int probes = 0; probes < slots.length; probes++) {
      int encoded = slots[slot];
      if (encoded == 0) return defaultValue;
      int row = encoded - 1;
      int base = row << 1;
      if (keys[base] == left && keys[base + 1] == right) return values[row];
      slot = (slot + 1) & mask;
    }
    return defaultValue;
  }

  public boolean containsPair(long left, long right) {
    requireArity(2);
    int mask = slots.length - 1;
    int slot = M3PrecomputeHashes.slot(hashPair(left, right), mask);
    for (int probes = 0; probes < slots.length; probes++) {
      int encoded = slots[slot];
      if (encoded == 0) return false;
      int row = encoded - 1;
      int base = row << 1;
      if (keys[base] == left && keys[base + 1] == right) return true;
      slot = (slot + 1) & mask;
    }
    return false;
  }

  long[] rawKeys() { return keys; }
  int[] rawValues() { return values; }
  int[] rawSlots() { return slots; }

  /** Allocation-free lookup from an existing primitive tuple tape. */
  public int get(long[] query, int offset, int defaultValue) {
    Objects.requireNonNull(query, "query");
    Objects.checkFromIndexSize(offset, arity, query.length);
    if (arity == 2) return getPair(query[offset], query[offset + 1], defaultValue);
    int mask = slots.length - 1;
    int slot = M3PrecomputeHashes.slot(hash(query, offset, arity), mask);
    for (int probes = 0; probes < slots.length; probes++) {
      int encoded = slots[slot];
      if (encoded == 0) return defaultValue;
      int row = encoded - 1;
      if (same(keys, row * arity, query, offset, arity)) return values[row];
      slot = (slot + 1) & mask;
    }
    return defaultValue;
  }

  private int getScalars(long k1, long k2, long k3, long k4, int defaultValue) {
    long hash = M3PrecomputeHashes.mix64((0x9e3779b97f4a7c15L ^ arity) ^ k1);
    if (arity > 1) hash = M3PrecomputeHashes.mix64(hash ^ k2 ^ (1L << 32));
    if (arity > 2) hash = M3PrecomputeHashes.mix64(hash ^ k3 ^ (2L << 32));
    if (arity > 3) hash = M3PrecomputeHashes.mix64(hash ^ k4 ^ (3L << 32));
    int mask = slots.length - 1;
    int slot = M3PrecomputeHashes.slot(hash, mask);
    for (int probes = 0; probes < slots.length; probes++) {
      int encoded = slots[slot];
      if (encoded == 0) return defaultValue;
      int row = encoded - 1;
      int base = row * arity;
      if (keys[base] == k1 && (arity < 2 || keys[base + 1] == k2)
          && (arity < 3 || keys[base + 2] == k3)
          && (arity < 4 || keys[base + 3] == k4)) return values[row];
      slot = (slot + 1) & mask;
    }
    return defaultValue;
  }

  private void requireArity(int expected) {
    if (arity != expected) throw new IllegalStateException("index arity is " + arity);
  }

  private static long hashPair(long left, long right) {
    return M3PrecomputeHashes.mix64(left)
        ^ Long.rotateLeft(M3PrecomputeHashes.mix64(right), 29);
  }

  private static long hash(long[] values, int offset, int length) {
    long hash = 0x9e3779b97f4a7c15L ^ length;
    for (int i = 0; i < length; i++) {
      hash = M3PrecomputeHashes.mix64(hash ^ values[offset + i] ^ ((long) i << 32));
    }
    return hash;
  }

  private static boolean same(
      long[] left, int leftOffset, long[] right, int rightOffset, int length) {
    for (int i = 0; i < length; i++) {
      if (left[leftOffset + i] != right[rightOffset + i]) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "M3FrozenLongTupleIndex[" + capability.id() + ",arity=" + arity
        + ",size=" + size() + "]";
  }
}
