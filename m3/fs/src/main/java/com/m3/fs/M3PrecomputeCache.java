// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Create-only index from deterministic precompute keys to persisted positive or negative results. */
public final class M3PrecomputeCache {
  private static final int MAX_DESCRIPTOR_BYTES = 256;
  private static final int MAX_BUNDLE_DESCRIPTOR_BYTES = 256 * 1024;
  private static final int KEY_OFFSET = 4;
  private static final int STATE_OFFSET = 36;
  private static final int OUTPUT_OFFSET = 37;
  private static final int SIZE_OFFSET = 69;
  private static final int KIND_OFFSET = 77;
  private static final int VALUE_BYTES = 81;
  private final M3DirectoryContentStore store;
  private final Path indexRoot;
  private final Path staging;

  public M3PrecomputeCache(M3DirectoryContentStore store) throws IOException {
    this.store = Objects.requireNonNull(store, "store");
    indexRoot = store.root().resolve("precompute-index"); staging = store.root().resolve("staging");
    M3DirectoryContentStore.durableCreateDirs(indexRoot);
  }

  public Optional<M3PrecomputeResult> lookup(M3PrecomputeKey key) throws IOException {
    byte[] id = keyBytes(key); byte[] encoded = readResultBytes(id, 0);
    return encoded == null ? Optional.empty() : Optional.of(M3PrecomputeResult.decode(encoded));
  }

  /** One bundle view inside a multi-profile mapped execution set. */
  public record MappedBundle(M3ContentId keyId, M3FsPrecomputeBundle bundle,
      List<M3FsPrecomputeBundle.MappedLane> lanes) {
    public MappedBundle {
      keyId = Objects.requireNonNull(keyId, "keyId"); bundle = Objects.requireNonNull(bundle, "bundle");
      if (!bundle.matchesKeyId(keyId.bytes(), 0)) throw new IllegalArgumentException("mapped bundle/key mismatch");
      lanes = M3FsPrecomputeBundle.normalizeMappedLanes(bundle, lanes);
    }
    public M3FsPrecomputeBundle.MappedLane requireLane(String name) {
      return ((M3FsPrecomputeBundle.MappedLaneList) lanes).require(name);
    }
  }

  private static final class MappedBundleList extends M3FsPrecomputeBundle.ArrayListView<MappedBundle> {
    private final M3FsPrecomputeBundle[] owners;
    private final M3MappedObject[][] mappings;
    MappedBundleList(M3FsPrecomputeBundle[] owners, M3MappedObject[][] mappings) {
      this.owners = owners.clone(); this.mappings = mappings.clone();
      if (owners.length == 0 || owners.length != mappings.length) throw new IllegalArgumentException("mapped bundle geometry");
      for (int i = 0; i < owners.length; i++) {
        Objects.requireNonNull(this.owners[i], "bundle"); this.mappings[i] = mappings[i].clone();
        M3FsPrecomputeBundle.mappedLanes(this.owners[i], this.mappings[i]);
      }
    }
    static MappedBundleList from(List<MappedBundle> supplied) {
      Objects.requireNonNull(supplied, "bundles");
      if (supplied instanceof MappedBundleList columns) return columns;
      M3FsPrecomputeBundle[] owners = new M3FsPrecomputeBundle[supplied.size()];
      M3MappedObject[][] mappings = new M3MappedObject[supplied.size()][];
      for (int i = 0; i < supplied.size(); i++) {
        MappedBundle row = Objects.requireNonNull(supplied.get(i), "bundle"); owners[i] = row.bundle();
        mappings[i] = ((M3FsPrecomputeBundle.MappedLaneList) row.lanes()).mappings();
      }
      return new MappedBundleList(owners, mappings);
    }
    @Override public int size() { return owners.length; }
    @Override public MappedBundle get(int index) {
      Objects.checkIndex(index, size());
      return new MappedBundle(owners[index].keyId(), owners[index], M3FsPrecomputeBundle.mappedLanes(owners[index], mappings[index]));
    }
  }

  /** Multiple bundles sharing one mapping per unique content ID. */
  public record MappedBundleSet(List<MappedBundle> bundles, long uniqueMappedBytes) {
    public MappedBundleSet {
      bundles = MappedBundleList.from(bundles);
      if (uniqueMappedBytes < 0) throw new IllegalArgumentException("negative mapped size");
    }
    int bundleCount() { return ((MappedBundleList) bundles).owners.length; }
    M3FsPrecomputeBundle bundleAt(int index) {
      MappedBundleList columns = (MappedBundleList) bundles;
      return columns.owners[Objects.checkIndex(index, columns.size())];
    }
    public List<MappedBundle> bundles(String profile) {
      String checked = Objects.requireNonNull(profile, "profile").strip();
      if (checked.isEmpty()) throw new IllegalArgumentException("profile");
      MappedBundleList columns = (MappedBundleList) bundles; int[] rows = new int[columns.size()]; int count = 0;
      for (int i = 0; i < rows.length; i++) if (columns.owners[i].profile().equals(checked)) rows[count++] = i;
      int[] found = Arrays.copyOf(rows, count);
      return M3FsPrecomputeBundle.apiList(found.length, index -> columns.get(found[index]));
    }
  }

  /** Flat SHA-256 keys and primitive open-addressed slots preserve insertion order. */
  private static final class IdIndex {
    private byte[] ids = new byte[8 * M3ContentId.BYTES];
    private int[] slots = new int[16];
    private int size;
    private static int hash(byte[] ids, int offset) {
      int value = 1;
      for (int i = 0; i < M3ContentId.BYTES; i++) value = value * 31 + ids[offset + i];
      return value ^ (value >>> 16);
    }
    int intern(byte[] supplied, int offset) {
      Objects.checkFromIndexSize(offset, M3ContentId.BYTES, supplied.length);
      int slot = hash(supplied, offset) & (slots.length - 1);
      while (slots[slot] != 0) {
        int row = slots[slot] - 1;
        if (Arrays.equals(ids, row * M3ContentId.BYTES, (row + 1) * M3ContentId.BYTES,
            supplied, offset, offset + M3ContentId.BYTES)) return row;
        slot = (slot + 1) & (slots.length - 1);
      }
      if (size == ids.length / M3ContentId.BYTES) ids = Arrays.copyOf(ids, Math.multiplyExact(ids.length, 2));
      if (size + 1 > slots.length / 2) {
        slots = new int[Math.multiplyExact(slots.length, 2)];
        for (int row = 0; row < size; row++) {
          int at = hash(ids, row * M3ContentId.BYTES) & (slots.length - 1);
          while (slots[at] != 0) at = (at + 1) & (slots.length - 1);
          slots[at] = row + 1;
        }
        slot = hash(supplied, offset) & (slots.length - 1);
        while (slots[slot] != 0) slot = (slot + 1) & (slots.length - 1);
      }
      int row = size++;
      System.arraycopy(supplied, offset, ids, row * M3ContentId.BYTES, M3ContentId.BYTES);
      slots[slot] = row + 1; return row;
    }
  }

  public Optional<MappedBundleSet> lookupMappedBundles(List<M3PrecomputeKey> keys,
      int maxLaneBytes, long maxTotalBytes) throws IOException {
    List<M3PrecomputeKey> checked = Objects.requireNonNull(keys, "keys");
    byte[] ids = new byte[Math.multiplyExact(checked.size(), M3ContentId.BYTES)];
    for (int i = 0; i < checked.size(); i++) Objects.requireNonNull(checked.get(i), "key").copyIdTo(ids, i * M3ContentId.BYTES);
    return lookupMappedBundleIds(ids, maxLaneBytes, maxTotalBytes);
  }

  Optional<MappedBundleSet> lookupMappedBundleIds(byte[] suppliedIds, int maxLaneBytes, long maxTotalBytes)
      throws IOException {
    byte[] keyIds = Objects.requireNonNull(suppliedIds, "keyIds").clone();
    if (keyIds.length == 0 || keyIds.length % M3ContentId.BYTES != 0 || maxLaneBytes < 0 || maxTotalBytes < 0) {
      throw new IllegalArgumentException("mapped bundle set arguments");
    }
    int count = keyIds.length / M3ContentId.BYTES; IdIndex seenKeys = new IdIndex();
    M3FsPrecomputeBundle[] owners = new M3FsPrecomputeBundle[count];
    for (int i = 0; i < count; i++) {
      if (seenKeys.intern(keyIds, i * M3ContentId.BYTES) != i) throw new IllegalArgumentException("duplicate precompute key");
      Optional<M3FsPrecomputeBundle> found = lookupBundleId(keyIds, i * M3ContentId.BYTES);
      if (found.isEmpty()) return Optional.empty(); owners[i] = found.get();
    }
    IdIndex unique = new IdIndex(); long[] sizes = new long[8]; long uniqueBytes = 0;
    int[][] mappingRows = new int[count][]; byte[] id = new byte[M3ContentId.BYTES];
    for (int i = 0; i < count; i++) {
      M3FsPrecomputeBundle bundle = owners[i]; mappingRows[i] = new int[bundle.laneCount()];
      for (int lane = 0; lane < bundle.laneCount(); lane++) {
        long size = bundle.laneBytes(lane);
        if (size > maxLaneBytes) throw new IOException("precompute lane exceeds mapping budget: " + bundle.laneName(lane));
        bundle.copyLaneId(lane, id, 0); int priorCount = unique.size; int row = unique.intern(id, 0);
        if (row == priorCount) {
          if (row == sizes.length) sizes = Arrays.copyOf(sizes, Math.multiplyExact(sizes.length, 2));
          if (size > maxTotalBytes - uniqueBytes) throw new IOException("precompute set exceeds mapping budget");
          sizes[row] = size; uniqueBytes += size;
        } else if (sizes[row] != size) throw new IOException("content ID has inconsistent lane reference");
        mappingRows[i][lane] = row;
      }
    }
    M3MappedObject[] mappings = new M3MappedObject[unique.size];
    for (int i = 0; i < unique.size; i++) {
      mappings[i] = store.openMappedId(unique.ids, i * M3ContentId.BYTES, sizes[i], M3ObjectKind.PRECOMPUTE_OUTPUT, maxLaneBytes);
    }
    M3MappedObject[][] lanes = new M3MappedObject[count][];
    for (int i = 0; i < count; i++) {
      lanes[i] = new M3MappedObject[mappingRows[i].length];
      for (int lane = 0; lane < lanes[i].length; lane++) lanes[i][lane] = mappings[mappingRows[i][lane]];
    }
    return Optional.of(new MappedBundleSet(new MappedBundleList(owners, lanes), uniqueBytes));
  }

  public Optional<M3FsPrecomputeBundle.MappedView> lookupMappedBundle(M3PrecomputeKey key,
      int maxLaneBytes, long maxTotalBytes) throws IOException {
    Optional<M3FsPrecomputeBundle> found = lookupBundle(key);
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get().openMapped(store, maxLaneBytes, maxTotalBytes));
  }
  public Optional<M3FsPrecomputeBundle> lookupBundle(M3PrecomputeKey key) throws IOException {
    return lookupBundleId(keyBytes(key), 0);
  }
  private Optional<M3FsPrecomputeBundle> lookupBundleId(byte[] keyIds, int offset) throws IOException {
    byte[] result = readResultBytes(keyIds, offset);
    if (result == null || result[STATE_OFFSET] == 1) return Optional.empty();
    ByteBuffer bytes = ByteBuffer.wrap(result);
    if (bytes.getInt(KIND_OFFSET) != M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR.ordinal()) throw new IOException("result is not a bundle descriptor");
    M3FsPrecomputeBundle bundle = M3FsPrecomputeBundle.decode(store.readSmallId(result, OUTPUT_OFFSET,
        bytes.getLong(SIZE_OFFSET), MAX_BUNDLE_DESCRIPTOR_BYTES));
    if (!bundle.matchesKeyId(keyIds, offset)) throw new IOException("precompute bundle/key mismatch");
    M3FsPrecomputeCatalog.requireCompatible(bundle, false); bundle.requireStore(store);
    return Optional.of(bundle);
  }

  public M3ObjectRef publishBundle(M3PrecomputeKey key, M3FsPrecomputeBundle bundle) throws IOException {
    byte[] keyId = keyBytes(key); Objects.requireNonNull(bundle, "bundle");
    if (!bundle.matchesKeyId(keyId, 0)) throw new IllegalArgumentException("precompute bundle/key mismatch");
    M3FsPrecomputeCatalog.requireCompatible(bundle, false); bundle.requireStore(store);
    byte[] id = new byte[M3ContentId.BYTES];
    long size = store.putId(new ByteArrayInputStream(bundle.canonicalBytes()), id, 0);
    ByteBuffer encoded = ByteBuffer.allocate(VALUE_BYTES);
    encoded.putInt(1).put(keyId).put((byte) 0).put(id).putLong(size).putInt(M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR.ordinal());
    publishEncoded(keyId, encoded.array());
    return new M3ObjectRef(M3ContentId.of(id), size, M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
  }
  public M3ObjectRef publish(M3PrecomputeResult result) throws IOException {
    byte[] encoded = Objects.requireNonNull(result, "result").canonicalBytes();
    byte[] keyId = Arrays.copyOfRange(encoded, KEY_OFFSET, KEY_OFFSET + M3ContentId.BYTES);
    byte[] id = publishEncoded(keyId, encoded);
    return new M3ObjectRef(M3ContentId.of(id), encoded.length, M3ObjectKind.PRECOMPUTE_DESCRIPTOR);
  }
  private byte[] publishEncoded(byte[] keyId, byte[] encoded) throws IOException {
    byte[] id = new byte[M3ContentId.BYTES]; store.putId(new ByteArrayInputStream(encoded), id, 0);
    Path pointer = pointerPath(keyId, 0); M3DirectoryContentStore.durableCreateDirs(pointer.getParent());
    byte[] bytes = (HexFormat.of().formatHex(id) + "\n").getBytes(StandardCharsets.US_ASCII);
    Path temp = staging.resolve("precompute-" + UUID.randomUUID() + ".tmp");
    try {
      Files.write(temp, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
      M3DirectoryContentStore.forceFile(temp);
      try { Files.createLink(pointer, temp); }
      catch (FileAlreadyExistsException race) {
        if (!Arrays.equals(bytes, readPointer(pointer))) throw new IOException("deterministic precompute key produced a conflicting result", race);
      } catch (UnsupportedOperationException unsupported) {
        throw new IOException("M3 create-only index requires hard-link support", unsupported);
      }
      M3DirectoryContentStore.forceFile(pointer); M3DirectoryContentStore.syncDirectory(pointer.getParent());
    } finally { Files.deleteIfExists(temp); }
    return id;
  }
  private byte[] readResultBytes(byte[] keyIds, int offset) throws IOException {
    Path pointer = pointerPath(keyIds, offset);
    if (!Files.exists(pointer, LinkOption.NOFOLLOW_LINKS)) return null;
    byte[] pointerBytes = readPointer(pointer); byte[] descriptorId = new byte[M3ContentId.BYTES];
    for (int i = 0; i < descriptorId.length; i++) descriptorId[i] = (byte) ((hex(pointerBytes[2 * i]) << 4) | hex(pointerBytes[2 * i + 1]));
    byte[] result = store.readSmallId(descriptorId, 0, -1, MAX_DESCRIPTOR_BYTES);
    if ((result.length != OUTPUT_OFFSET && result.length != VALUE_BYTES) || ByteBuffer.wrap(result).getInt() != 1
        || !Arrays.equals(result, KEY_OFFSET, KEY_OFFSET + M3ContentId.BYTES, keyIds, offset, offset + M3ContentId.BYTES)) {
      throw new IOException("invalid precompute descriptor/key");
    }
    int state = Byte.toUnsignedInt(result[STATE_OFFSET]);
    if ((state != 0 && state != 1) || result.length != (state == 1 ? OUTPUT_OFFSET : VALUE_BYTES)) throw new IOException("invalid precompute state/shape");
    if (state == 0) {
      ByteBuffer bytes = ByteBuffer.wrap(result); int kind = bytes.getInt(KIND_OFFSET);
      if (bytes.getLong(SIZE_OFFSET) < 0 || kind < 0 || kind >= M3ObjectKind.values().length) throw new IOException("invalid precompute output geometry");
    }
    return result;
  }
  private static byte[] readPointer(Path pointer) throws IOException {
    if (!Files.isRegularFile(pointer, LinkOption.NOFOLLOW_LINKS)) throw new IOException("precompute pointer is not a regular file");
    byte[] bytes = new byte[65];
    try (FileChannel channel = FileChannel.open(pointer, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      if (channel.size() != bytes.length) throw new IOException("invalid precompute pointer size");
      ByteBuffer output = ByteBuffer.wrap(bytes);
      while (output.hasRemaining()) if (channel.read(output) < 0) throw new IOException("truncated precompute pointer");
      if (channel.size() != bytes.length || bytes[64] != '\n') throw new IOException("invalid precompute pointer framing");
      for (int i = 0; i < 64; i++) hex(bytes[i]);
    }
    return bytes;
  }
  private static int hex(byte value) throws IOException {
    if (value >= '0' && value <= '9') return value - '0';
    if (value >= 'a' && value <= 'f') return value - 'a' + 10;
    throw new IOException("invalid lowercase precompute pointer hex");
  }
  private Path pointerPath(byte[] ids, int offset) {
    Objects.checkFromIndexSize(offset, M3ContentId.BYTES, ids.length);
    String hex = HexFormat.of().formatHex(ids, offset, offset + M3ContentId.BYTES);
    return indexRoot.resolve(hex.substring(0, 2)).resolve(hex.substring(2) + ".ref");
  }
  private static byte[] keyBytes(M3PrecomputeKey key) {
    byte[] id = new byte[M3ContentId.BYTES]; Objects.requireNonNull(key, "key").copyIdTo(id, 0); return id;
  }
}
