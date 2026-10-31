// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted from the pinned Synexia owner; manifest references use primitive columns.
package com.m3.fs;

import java.io.*;
import java.util.*;

/** Immutable format-1 links between content, structures, relations and precomputes. */
public final class M3FileManifest {
  private static final int FORMAT = 1;
  private static final int MAX_ENTRIES = 1_000_000;
  private final byte[] rawId;
  private final long rawSize;
  private final M3RefTable structures;
  private final M3RefTable relations;
  private final M3IdPairs precomputes;
  private final byte[] canonical;
  private final byte[] id;

  public M3FileManifest(M3ObjectRef raw, Map<String, M3ObjectRef> structures,
      Map<String, M3ObjectRef> relations, Map<M3ContentId, M3ContentId> precomputes) {
    Objects.requireNonNull(raw, "raw");
    if (raw.kind() != M3ObjectKind.RAW_BYTES) throw new IllegalArgumentException("manifest raw reference must be RAW_BYTES");
    rawId = raw.id().bytes(); rawSize = raw.size();
    this.structures = new M3RefTable(structures, MAX_ENTRIES); this.relations = new M3RefTable(relations, MAX_ENTRIES);
    this.precomputes = new M3IdPairs(precomputes, MAX_ENTRIES); canonical = encode(); id = M3Columns.digest(canonical);
  }
  private M3FileManifest(byte[] rawId, long rawSize, M3RefTable structures, M3RefTable relations, M3IdPairs precomputes) {
    if (rawId.length != 32 || rawSize < 0 || structures.names.length > MAX_ENTRIES
        || relations.names.length > MAX_ENTRIES || precomputes.keys.length / 32 > MAX_ENTRIES) throw new IllegalArgumentException("invalid manifest geometry");
    this.rawId = rawId; this.rawSize = rawSize; this.structures = structures; this.relations = relations;
    this.precomputes = precomputes; canonical = encode(); id = M3Columns.digest(canonical);
  }
  public M3ObjectRef raw() { return new M3ObjectRef(M3ContentId.of(rawId), rawSize, M3ObjectKind.RAW_BYTES); }
  public Map<String, M3ObjectRef> structures() { return structures.view(); }
  public Map<String, M3ObjectRef> relations() { return relations.view(); }
  public Map<M3ContentId, M3ContentId> precomputes() { return precomputes.view(); }
  public M3ContentId id() { return M3ContentId.of(id); }
  public byte[] canonicalBytes() { return canonical.clone(); }
  void copyRawIdTo(byte[] out, int offset) { System.arraycopy(rawId, 0, out, offset, 32); }
  long rawSizeValue() { return rawSize; }
  int structureCount() { return structures.names.length; }
  int relationCount() { return relations.names.length; }
  int structureIndex(String name) { return Arrays.binarySearch(structures.names, name); }
  int relationIndex(String name) { return Arrays.binarySearch(relations.names, name); }
  String structureName(int row) { return structures.names[row]; }
  String relationName(int row) { return relations.names[row]; }
  long structureSize(int row) { return structures.sizes[row]; }
  long relationSize(int row) { return relations.sizes[row]; }
  int structureKindOrdinal(int row) { return structures.kinds[row]; }
  int relationKindOrdinal(int row) { return relations.kinds[row]; }
  void copyStructureIdTo(int row, byte[] out, int offset) { System.arraycopy(structures.ids, row * 32, out, offset, 32); }
  void copyRelationIdTo(int row, byte[] out, int offset) { System.arraycopy(relations.ids, row * 32, out, offset, 32); }
  public M3FileManifest withStructure(String name, M3ObjectRef ref) {
    return new M3FileManifest(rawId, rawSize, structures.put(name, ref), relations, precomputes);
  }
  public M3FileManifest withRelation(String name, M3ObjectRef ref) {
    return new M3FileManifest(rawId, rawSize, structures, relations.put(name, ref), precomputes);
  }
  public M3FileManifest withPrecompute(M3ContentId keyId, M3ContentId descriptorId) {
    return new M3FileManifest(rawId, rawSize, structures, relations,
        precomputes.put(Objects.requireNonNull(keyId, "keyId").bytes(), Objects.requireNonNull(descriptorId, "descriptorId").bytes()));
  }
  private byte[] encode() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT); out.write(rawId); out.writeLong(rawSize); out.writeInt(M3ObjectKind.RAW_BYTES.ordinal());
        structures.write(out); relations.write(out); out.writeInt(precomputes.keys.length / 32);
        for (int i = 0; i < precomputes.keys.length; i += 32) { out.write(precomputes.keys, i, 32); out.write(precomputes.values, i, 32); }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) { throw new AssertionError(impossible); }
  }
  public static M3FileManifest decode(byte[] encoded) throws IOException {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      if (in.readInt() != FORMAT) throw new IOException("unsupported M3 file manifest format");
      byte[] raw = M3Columns.readId(in); long size = in.readLong();
      if (in.readInt() != M3ObjectKind.RAW_BYTES.ordinal()) throw new IOException("invalid raw object kind");
      M3RefTable structures = M3RefTable.read(in, MAX_ENTRIES), relations = M3RefTable.read(in, MAX_ENTRIES);
      int count = M3Columns.readCount(in, MAX_ENTRIES, 64); byte[] keys = new byte[count * 32], values = new byte[count * 32];
      for (int i = 0; i < count; i++) { in.readFully(keys, i * 32, 32); in.readFully(values, i * 32, 32); }
      if (in.read() != -1) throw new IOException("trailing manifest bytes");
      M3FileManifest manifest = new M3FileManifest(raw, size, structures, relations, new M3IdPairs(keys, values));
      if (!Arrays.equals(manifest.canonical, bytes)) throw new IOException("non-canonical manifest bytes");
      return manifest;
    } catch (IllegalArgumentException invalid) { throw new IOException("invalid M3 file manifest", invalid); }
  }
}
