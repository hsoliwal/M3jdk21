// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Array-backed canonical M3FileText pool.
 *
 * <p>Rows are flattened into one long token arena. Hash slots retain integer handles; no entry
 * object is retained per pooled value. Admission is O(tokens); handle equality is O(1).</p>
 */
public final class M3FileTextPool {
  private final M3FileResolver resolver;
  private long[] arena = new long[64];
  private int[] starts = new int[16];
  private int[] lengths = new int[16];
  private int[] hashes = new int[16];
  private int[] slots = new int[32];
  private int size;
  private int arenaSize;

  public M3FileTextPool(M3FileResolver resolver) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
  }

  public synchronized M3FileTextRef intern(M3FileText value) {
    return new M3FileTextRef(this, internHandle(value));
  }

  /**
   * Looks up a whole-string handle without admitting a new value.
   *
   * <p>This is the query-side counterpart of {@link #internHandle(M3FileText)}. A miss returns
   * {@code -1} and never grows the pool.</p>
   */
  public synchronized int findHandle(M3FileText value) {
    Objects.requireNonNull(value, "value");
    if (value.resolver() != resolver) throw new IllegalArgumentException("resolver mismatch");
    int hash = sequenceHash(value);
    int slot = findSlot(value, hash);
    return slots[slot] == 0 ? -1 : slots[slot] - 1;
  }

  /**
   * Canonicalizes a value and returns the primitive whole-string handle.
   *
   * <p>The hot path does not call tokenIds(), allocate a temporary long[], or allocate a ref
   * wrapper. Equality confirmation reads token IDs directly from the immutable M3FileText.
   */
  public synchronized int internHandle(M3FileText value) {
    Objects.requireNonNull(value, "value");
    if (value.resolver() != resolver) throw new IllegalArgumentException("resolver mismatch");

    int hash = sequenceHash(value);
    int slot = findSlot(value, hash);
    if (slots[slot] != 0) return slots[slot] - 1;

    int tokenCount = value.tokenCount();
    reserveRows(size + 1);
    reserveTable(size + 1);
    reserveArena(Math.addExact(arenaSize, tokenCount));

    int handle = size++;
    starts[handle] = arenaSize;
    lengths[handle] = tokenCount;
    hashes[handle] = hash;
    for (int index = 0; index < tokenCount; index++) {
      arena[arenaSize++] = value.tokenIdAt(index);
    }

    slot = findSlot(value, hash);
    slots[slot] = handle + 1;
    return handle;
  }

  /**
   * Finds a scalar text handle without constructing an M3FileText wrapper or admitting a miss.
   */
  public synchronized int findHandle(int languageId, CharSequence text) {
    Objects.requireNonNull(text, "text");
    OptionalLong found = resolver.lookup(languageId, text);
    if (found.isEmpty()) return -1;
    long id = found.getAsLong();
    int hash = scalarSequenceHash(id);
    int slot = findScalarSlot(id, hash);
    return slots[slot] == 0 ? -1 : slots[slot] - 1;
  }

  /**
   * Interns one scalar resolver value directly and returns its primitive whole-string handle.
   *
   * <p>This is the allocation-light ingress used by M3File tokenizers: token slices are
   * synchronously interned by the resolver and the resulting ID is admitted without allocating a
   * transient M3FileText wrapper.</p>
   */
  public synchronized int internHandle(int languageId, CharSequence text) {
    Objects.requireNonNull(text, "text");
    long id = resolver.intern(languageId, text);
    int hash = scalarSequenceHash(id);
    int slot = findScalarSlot(id, hash);
    if (slots[slot] != 0) return slots[slot] - 1;

    reserveRows(size + 1);
    reserveTable(size + 1);
    reserveArena(Math.addExact(arenaSize, 1));

    int handle = size++;
    starts[handle] = arenaSize;
    lengths[handle] = 1;
    hashes[handle] = hash;
    arena[arenaSize++] = id;

    slot = findScalarSlot(id, hash);
    slots[slot] = handle + 1;
    return handle;
  }

  public synchronized M3FileTextRef intern(int languageId, CharSequence text) {
    return new M3FileTextRef(this, internHandle(languageId, text));
  }

  public M3FileResolver resolver() { return resolver; }

  public synchronized int size() { return size; }

  public synchronized int tokenCount(int handle) {
    return lengths[Objects.checkIndex(handle, size)];
  }

  public synchronized long tokenIdAt(int handle, int ordinal) {
    int checked = Objects.checkIndex(handle, size);
    return arena[starts[checked] + Objects.checkIndex(ordinal, lengths[checked])];
  }

  /** Returns one defensive flattened snapshot of every admitted resolver ID in handle order. */
  public synchronized long[] tokenIdsSnapshot() {
    return Arrays.copyOf(arena, arenaSize);
  }

  /** Returns one defensive CSR-style row-boundary snapshot in handle order. */
  public synchronized int[] tokenStartsSnapshot() {
    int[] snapshot = Arrays.copyOf(starts, size + 1);
    snapshot[size] = arenaSize;
    return snapshot;
  }

  /** Returns an immutable value projection for an existing primitive handle. */
  public synchronized M3FileText valueAt(int handle) {
    return value(handle);
  }

  synchronized M3FileText value(int handle) {
    Objects.checkIndex(handle, size);
    return new M3FileText(
        resolver,
        Arrays.copyOfRange(arena, starts[handle], starts[handle] + lengths[handle]));
  }

  public synchronized long retainedPayloadBytes() {
    return 8L * arena.length
        + 4L * (starts.length + lengths.length + hashes.length + slots.length);
  }

  private int findSlot(M3FileText value, int hash) {
    int mask = slots.length - 1;
    int slot = mix(hash) & mask;
    while (slots[slot] != 0) {
      int row = slots[slot] - 1;
      if (hashes[row] == hash && lengths[row] == value.tokenCount()) {
        int start = starts[row];
        boolean equal = true;
        for (int index = 0; index < value.tokenCount(); index++) {
          if (arena[start + index] != value.tokenIdAt(index)) {
            equal = false;
            break;
          }
        }
        if (equal) return slot;
      }
      slot = (slot + 1) & mask;
    }
    return slot;
  }

  private int findScalarSlot(long id, int hash) {
    int mask = slots.length - 1;
    int slot = mix(hash) & mask;
    while (slots[slot] != 0) {
      int row = slots[slot] - 1;
      if (hashes[row] == hash && lengths[row] == 1 && arena[starts[row]] == id) {
        return slot;
      }
      slot = (slot + 1) & mask;
    }
    return slot;
  }

  private static int scalarSequenceHash(long id) {
    return 31 + (int) (id ^ (id >>> 32));
  }

  private static int sequenceHash(M3FileText value) {
    int hash = 1;
    for (int index = 0; index < value.tokenCount(); index++) {
      long token = value.tokenIdAt(index);
      hash = 31 * hash + (int) (token ^ (token >>> 32));
    }
    return hash;
  }

  private void reserveRows(int required) {
    if (required <= starts.length) return;
    int next = Math.max(required, starts.length + (starts.length >>> 1) + 1);
    starts = Arrays.copyOf(starts, next);
    lengths = Arrays.copyOf(lengths, next);
    hashes = Arrays.copyOf(hashes, next);
  }

  private void reserveArena(int required) {
    if (required <= arena.length) return;
    arena = Arrays.copyOf(arena, Math.max(required, arena.length + (arena.length >>> 1) + 1));
  }

  private void reserveTable(int expected) {
    if (expected * 2 < slots.length) return;
    int[] previous = slots;
    slots = new int[previous.length << 1];
    for (int row = 0; row < size; row++) {
      int mask = slots.length - 1;
      int slot = mix(hashes[row]) & mask;
      while (slots[slot] != 0) slot = (slot + 1) & mask;
      slots[slot] = row + 1;
    }
  }

  private static int mix(int value) {
    int x = value;
    x ^= x >>> 16;
    x *= 0x7feb352d;
    x ^= x >>> 15;
    x *= 0x846ca68b;
    return x ^ (x >>> 16);
  }
}
