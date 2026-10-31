// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Shared fixed-candidate lookup for immutable 64-bit keys.
 *
 * <p>Runtime is one hash mix, one displacement read, one slot read, and exact key verification by
 * the owning structure. Collision placement is entirely freeze-time work; lookup never probes.</p>
 */
public final class M3LongLookup {
  private static final int TARGET_BUCKET_SIZE = 4;
  private static final int MAX_DISPLACEMENT_ATTEMPTS = 1 << 20;
  private static final int MAX_TABLE_EXPANSIONS = 3;
  private static final long BUCKET_SALT = 0xd1b54a32d192ed03L;
  private static final long SLOT_SALT = 0x94d049bb133111ebL;

  private M3LongLookup() {}

  public static Geometry empty() {
    return new Geometry(0, 1, new int[] {0}, new int[2]);
  }

  public static Geometry build(long[] keys) {
    Objects.requireNonNull(keys, "keys");
    if (keys.length == 0) return empty();

    int bucketCount =
        nextPowerOfTwo(
            Math.max(1, (keys.length + TARGET_BUCKET_SIZE - 1) / TARGET_BUCKET_SIZE));
    int initialSlots = nextPowerOfTwo(Math.max(2, Math.multiplyExact(keys.length, 2)));

    long[] hashes = new long[keys.length];
    int[] bucketCounts = new int[bucketCount];
    for (int row = 0; row < keys.length; row++) {
      long hash = mix64(keys[row]);
      hashes[row] = hash;
      bucketCounts[bucket(hash, bucketCount - 1)]++;
    }

    int[] bucketOffsets = prefixOffsets(bucketCounts);
    int[] rows = new int[keys.length];
    int[] cursor = Arrays.copyOf(bucketOffsets, bucketCount);
    for (int row = 0; row < keys.length; row++) {
      int bucket = bucket(hashes[row], bucketCount - 1);
      rows[cursor[bucket]++] = row;
    }

    int[] order = bucketOrder(bucketCounts);
    for (int expansion = 0; expansion <= MAX_TABLE_EXPANSIONS; expansion++) {
      int tableSize = initialSlots << expansion;
      if (tableSize <= 0) break;
      int[] slots = new int[tableSize];
      int[] displacement = new int[bucketCount];
      int[] scratch = new int[max(bucketCounts)];
      boolean complete = true;

      for (int bucket : order) {
        int count = bucketCounts[bucket];
        int from = bucketOffsets[bucket];
        boolean placed = false;
        for (int d = 0; d < MAX_DISPLACEMENT_ATTEMPTS; d++) {
          boolean collision = false;
          for (int ordinal = 0; ordinal < count; ordinal++) {
            int row = rows[from + ordinal];
            int slot = slot(hashes[row], d, tableSize - 1);
            if (slots[slot] != 0) {
              collision = true;
              break;
            }
            for (int prior = 0; prior < ordinal; prior++) {
              if (scratch[prior] == slot) {
                collision = true;
                break;
              }
            }
            if (collision) break;
            scratch[ordinal] = slot;
          }
          if (collision) continue;

          displacement[bucket] = d;
          for (int ordinal = 0; ordinal < count; ordinal++) {
            slots[scratch[ordinal]] = rows[from + ordinal] + 1;
          }
          placed = true;
          break;
        }
        if (!placed) {
          complete = false;
          break;
        }
      }

      if (complete) {
        return new Geometry(bucketCount - 1, tableSize - 1, displacement, slots);
      }
    }
    throw new IllegalStateException("unable to freeze long-key lookup");
  }

  /** Candidate row, or -1. The owner must verify its exact key lane. */
  public static int candidate(long key, Geometry geometry) {
    Geometry checked = Objects.requireNonNull(geometry, "geometry");
    long hash = mix64(key);
    int bucket = bucket(hash, checked.bucketMask());
    int encoded =
        checked.slots()[
            slot(hash, checked.displacements()[bucket], checked.slotMask())];
    return encoded == 0 ? -1 : encoded - 1;
  }

  /** One-candidate exact lookup; runtime never probes. */
  public static int find(long key, long[] ownerKeys, Geometry geometry) {
    Objects.requireNonNull(ownerKeys, "ownerKeys");
    int row = candidate(key, geometry);
    return row >= 0 && row < ownerKeys.length && ownerKeys[row] == key ? row : -1;
  }

  /** Mapped candidate row, or -1. The owner must verify its exact mapped key lane. */
  public static int candidateMapped(
      long key,
      ByteBuffer data,
      int displacementsBase,
      int bucketMask,
      int slotsBase,
      int slotMask) {
    Objects.requireNonNull(data, "data");
    long hash = mix64(key);
    int bucket = bucket(hash, bucketMask);
    int displacement = data.getInt(displacementsBase + bucket * Integer.BYTES);
    int encoded =
        data.getInt(
            slotsBase + slot(hash, displacement, slotMask) * Integer.BYTES);
    return encoded == 0 ? -1 : encoded - 1;
  }

  /**
   * Exact fixed-candidate lookup over separately mapped portable primitive lanes.
   */
  public static int findMapped(
      long key,
      LongBuffer ownerKeys,
      IntBuffer displacements,
      IntBuffer slots) {
    LongBuffer keys = Objects.requireNonNull(ownerKeys, "ownerKeys");
    IntBuffer displacement = Objects.requireNonNull(displacements, "displacements");
    IntBuffer slotLane = Objects.requireNonNull(slots, "slots");
    int bucketCount = displacement.limit();
    int slotCount = slotLane.limit();
    if (bucketCount < 1
        || Integer.bitCount(bucketCount) != 1
        || slotCount < 2
        || Integer.bitCount(slotCount) != 1) {
      throw new IllegalArgumentException("invalid mapped frozen lookup geometry");
    }
    long hash = mix64(key);
    int bucket = bucket(hash, bucketCount - 1);
    int encoded =
        slotLane.get(
            slot(hash, displacement.get(bucket), slotCount - 1));
    if (encoded == 0) return -1;
    int row = encoded - 1;
    return row >= 0 && row < keys.limit() && keys.get(row) == key ? row : -1;
  }

  private static int bucket(long hash, int mask) {
    return (int) mix64(hash ^ BUCKET_SALT) & mask;
  }

  private static int slot(long hash, int displacement, int mask) {
    long salted = hash + (Integer.toUnsignedLong(displacement) + 1L) * SLOT_SALT;
    return (int) mix64(salted) & mask;
  }

  private static int[] prefixOffsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int i = 0; i < counts.length; i++) {
      offsets[i + 1] = Math.addExact(offsets[i], counts[i]);
    }
    return offsets;
  }

  private static int[] bucketOrder(int[] counts) {
    int maximum = max(counts);
    int nonEmpty = 0;
    for (int count : counts) if (count != 0) nonEmpty++;
    int[] frequencies = new int[maximum + 1];
    for (int count : counts) if (count != 0) frequencies[count]++;
    int[] start = new int[maximum + 1];
    int cursor = 0;
    for (int count = maximum; count >= 1; count--) {
      start[count] = cursor;
      cursor += frequencies[count];
    }
    int[] next = start.clone();
    int[] order = new int[nonEmpty];
    for (int bucket = 0; bucket < counts.length; bucket++) {
      int count = counts[bucket];
      if (count != 0) order[next[count]++] = bucket;
    }
    return order;
  }

  private static int max(int[] values) {
    int result = 0;
    for (int value : values) result = Math.max(result, value);
    return result;
  }

  private static int nextPowerOfTwo(int value) {
    if (value <= 1) return 1;
    if (value > (1 << 30)) {
      throw new IllegalArgumentException("lookup is too large");
    }
    return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
  }

  static long mix64(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  public record Geometry(
      int bucketMask, int slotMask, int[] displacements, int[] slots) {
    public Geometry {
      if (bucketMask < 0
          || slotMask < 1
          || Integer.bitCount(bucketMask + 1) != 1
          || Integer.bitCount(slotMask + 1) != 1) {
        throw new IllegalArgumentException("invalid frozen lookup masks");
      }
      Objects.requireNonNull(displacements, "displacements");
      Objects.requireNonNull(slots, "slots");
      if (displacements.length != bucketMask + 1 || slots.length != slotMask + 1) {
        throw new IllegalArgumentException("frozen lookup geometry length mismatch");
      }
    }
  }
}
