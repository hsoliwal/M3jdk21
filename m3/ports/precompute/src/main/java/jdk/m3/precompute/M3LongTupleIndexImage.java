// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.Objects;

/** Deterministic OS-mappable image for {@link M3FrozenLongTupleIndex}. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/LongTupleIndexImage.java blob b47d2be30fc5; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3LongTupleIndexImage {
  private static final long MAGIC = 0x5359505455504c31L; // SYPTUPL1
  private static final int VERSION = 1;
  private static final int HEADER_BYTES = 64;

  private M3LongTupleIndexImage() {}

  public static void write(M3FrozenLongTupleIndex index, Path output) throws IOException {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(output, "output");

    byte[] root = HexFormat.of().parseHex(index.rootHash());
    if (root.length != 32) throw new IllegalArgumentException("root hash must be SHA-256");
    long[] keys = index.rawKeys();
    int[] values = index.rawValues();
    int[] slots = index.rawSlots();

    long total = HEADER_BYTES
        + (long) keys.length * Long.BYTES
        + (long) values.length * Integer.BYTES
        + (long) slots.length * Integer.BYTES;
    if (total > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("tuple image exceeds 2 GiB; shard it");
    }

    Path absolute = output.toAbsolutePath().normalize();
    Path parent = absolute.getParent();
    if (parent != null) Files.createDirectories(parent);
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
        Files.newOutputStream(absolute)))) {
      out.writeLong(MAGIC);
      out.writeInt(VERSION);
      out.writeInt(index.capability().ordinal());
      out.writeInt(index.arity());
      out.writeInt(index.size());
      out.writeInt(index.capacity());
      out.write(root);
      out.writeInt(0); // reserved: header is exactly 64 bytes

      for (long key : keys) out.writeLong(key);
      for (int value : values) out.writeInt(value);
      for (int slot : slots) out.writeInt(slot);
    }
  }

  public static M3LongTupleIndexView map(Path input) throws IOException {
    Objects.requireNonNull(input, "input");
    MappedByteBuffer mapped;
    try (FileChannel channel = FileChannel.open(input, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size < HEADER_BYTES || size > Integer.MAX_VALUE) {
        throw new IOException("invalid tuple image size " + size);
      }
      mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
    }
    mapped.order(ByteOrder.BIG_ENDIAN);

    require(mapped.getLong(0) == MAGIC, "not a Synexia tuple image");
    require(mapped.getInt(8) == VERSION, "unsupported tuple image version");
    int capabilityOrdinal = mapped.getInt(12);
    M3IndexPrecomputeCapability[] capabilities = M3IndexPrecomputeCapability.values();
    require(capabilityOrdinal >= 0 && capabilityOrdinal < capabilities.length, "bad capability");
    int arity = mapped.getInt(16);
    int rows = mapped.getInt(20);
    int capacity = mapped.getInt(24);
    require(arity >= 1 && arity <= 4, "bad tuple arity");
    require(rows >= 0 && capacity >= 2 && Integer.bitCount(capacity) == 1, "bad tuple dimensions");

    byte[] root = new byte[32];
    ByteBuffer rootView = mapped.duplicate();
    rootView.position(28).get(root);

    long keysBytes = Math.multiplyExact(Math.multiplyExact((long) rows, arity), Long.BYTES);
    long valuesBytes = Math.multiplyExact((long) rows, Integer.BYTES);
    long slotsBytes = Math.multiplyExact((long) capacity, Integer.BYTES);
    long expected = HEADER_BYTES + keysBytes + valuesBytes + slotsBytes;
    require(expected == mapped.capacity(), "tuple image length mismatch");

    int keysOffset = HEADER_BYTES;
    int valuesOffset = Math.addExact(keysOffset, Math.toIntExact(keysBytes));
    int slotsOffset = Math.addExact(valuesOffset, Math.toIntExact(valuesBytes));

    return new Mapped(
        mapped,
        capabilities[capabilityOrdinal],
        arity,
        rows,
        capacity,
        HexFormat.of().formatHex(root),
        keysOffset,
        valuesOffset,
        slotsOffset);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private static final class Mapped implements M3LongTupleIndexView {
    private final ByteBuffer image;
    private final M3IndexPrecomputeCapability capability;
    private final int arity;
    private final int rows;
    private final int capacity;
    private final String rootHash;
    private final int keysOffset;
    private final int valuesOffset;
    private final int slotsOffset;

    private Mapped(
        ByteBuffer image,
        M3IndexPrecomputeCapability capability,
        int arity,
        int rows,
        int capacity,
        String rootHash,
        int keysOffset,
        int valuesOffset,
        int slotsOffset) {
      this.image = image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
      this.capability = capability;
      this.arity = arity;
      this.rows = rows;
      this.capacity = capacity;
      this.rootHash = rootHash;
      this.keysOffset = keysOffset;
      this.valuesOffset = valuesOffset;
      this.slotsOffset = slotsOffset;
    }

    @Override public M3IndexPrecomputeCapability capability() { return capability; }
    @Override public String rootHash() { return rootHash; }
    @Override public long retainedBytes() { return image.capacity(); }
    @Override public boolean mmapBacked() { return true; }
    @Override public String storageKey() { return "mmap-tuple:" + rootHash; }
    @Override public int arity() { return arity; }
    @Override public int size() { return rows; }

    @Override public int get(long k1, int defaultValue) {
      requireArity(1);
      return lookup(new long[] {k1}, defaultValue);
    }

    @Override public int get(long k1, long k2, int defaultValue) {
      requireArity(2);
      int mask = capacity - 1;
      int slot = M3PrecomputeHashes.slot(hashPair(k1, k2), mask);
      for (int probes = 0; probes < capacity; probes++) {
        int encoded = slot(slot);
        if (encoded == 0) return defaultValue;
        int row = encoded - 1;
        int key = keysOffset + row * 2 * Long.BYTES;
        if (image.getLong(key) == k1 && image.getLong(key + Long.BYTES) == k2) {
          return value(row);
        }
        slot = (slot + 1) & mask;
      }
      return defaultValue;
    }

    @Override public int get(long k1, long k2, long k3, int defaultValue) {
      requireArity(3);
      return lookup(new long[] {k1, k2, k3}, defaultValue);
    }

    @Override public int get(long k1, long k2, long k3, long k4, int defaultValue) {
      requireArity(4);
      return lookup(new long[] {k1, k2, k3, k4}, defaultValue);
    }

    private int lookup(long[] tuple, int defaultValue) {
      int mask = capacity - 1;
      int slot = M3PrecomputeHashes.slot(hash(tuple), mask);
      for (int probes = 0; probes < capacity; probes++) {
        int encoded = slot(slot);
        if (encoded == 0) return defaultValue;
        int row = encoded - 1;
        if (same(row, tuple)) return value(row);
        slot = (slot + 1) & mask;
      }
      return defaultValue;
    }

    private boolean same(int row, long[] tuple) {
      int base = keysOffset + row * arity * Long.BYTES;
      for (int i = 0; i < arity; i++) {
        if (image.getLong(base + i * Long.BYTES) != tuple[i]) return false;
      }
      return true;
    }

    private int value(int row) {
      return image.getInt(valuesOffset + row * Integer.BYTES);
    }

    private int slot(int index) {
      return image.getInt(slotsOffset + index * Integer.BYTES);
    }

    private void requireArity(int expected) {
      if (arity != expected) throw new IllegalStateException("index arity is " + arity);
    }

    private static long hashPair(long left, long right) {
      return M3PrecomputeHashes.mix64(left)
          ^ Long.rotateLeft(M3PrecomputeHashes.mix64(right), 29);
    }

    private static long hash(long[] values) {
      long hash = 0x9e3779b97f4a7c15L ^ values.length;
      for (int i = 0; i < values.length; i++) {
        hash = M3PrecomputeHashes.mix64(hash ^ values[i] ^ ((long) i << 32));
      }
      return hash;
    }
  }
}
