// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Pointer-free generic M3 structural image for native M3FS traversal.
 *
 * <p>This codec does not invent semantic identity. Callers supply canonical node coordinates,
 * primitive lanes, semantic hashes and child/text/binary arenas from the owning M3 subsystem.
 * Node IDs are 1-based in child/reference lanes; zero is never a legal node ID.</p>
 */
public final class M3FsStructureImage {
  public static final long MAGIC = 0x4d334d5354523031L;
  public static final int FORMAT = 1;
  public static final int HEADER_BYTES = 96;

  public enum NodeKind {
    NULL,
    BOOLEAN,
    INT32,
    INT64,
    FLOAT32,
    FLOAT64,
    DECIMAL,
    TEXT,
    BINARY,
    TEMPORAL,
    TUPLE,
    SEQUENCE,
    MAP,
    TABLE,
    EDGE,
    REF,
    EVENT;

    public boolean composition() {
      return this == TUPLE || this == SEQUENCE || this == MAP || this == TABLE;
    }
  }

  private final ByteBuffer image;
  private final int domainId;
  private final int nodeCount;
  private final int childCount;
  private final int textTokenCount;
  private final int binaryByteCount;

  private M3FsStructureImage(
      ByteBuffer image,
      int domainId,
      int nodeCount,
      int childCount,
      int textTokenCount,
      int binaryByteCount) {
    this.image = image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
    this.domainId = domainId;
    this.nodeCount = nodeCount;
    this.childCount = childCount;
    this.textTokenCount = textTokenCount;
    this.binaryByteCount = binaryByteCount;
  }

  /**
   * Freeze exact primitive lanes into the native image.
   *
   * <p>For TEXT/DECIMAL and BINARY nodes, first/second are [start,count] coordinates into the
   * corresponding arenas. REF first is a target node ID and second must be zero. EDGE has exactly
   * three children; EVENT exactly two; MAP has an even child count of key/value coordinates.</p>
   */
  public static M3FsStructureImage of(
      int domainId,
      NodeKind[] kinds,
      long[] first,
      long[] second,
      long[] semanticHashes,
      int[] childStarts,
      int[] childCounts,
      int[] children,
      long[] textTokens,
      byte[] binaryBytes) {
    if (domainId < 0) throw new IllegalArgumentException("negative domainId");
    NodeKind[] kindLane = Objects.requireNonNull(kinds, "kinds").clone();
    long[] firstLane = Objects.requireNonNull(first, "first").clone();
    long[] secondLane = Objects.requireNonNull(second, "second").clone();
    long[] hashLane = Objects.requireNonNull(semanticHashes, "semanticHashes").clone();
    int[] starts = Objects.requireNonNull(childStarts, "childStarts").clone();
    int[] counts = Objects.requireNonNull(childCounts, "childCounts").clone();
    int[] childLane = Objects.requireNonNull(children, "children").clone();
    long[] textLane = Objects.requireNonNull(textTokens, "textTokens").clone();
    byte[] byteLane = Objects.requireNonNull(binaryBytes, "binaryBytes").clone();

    int nodes = kindLane.length;
    if (firstLane.length != nodes
        || secondLane.length != nodes
        || hashLane.length != nodes
        || starts.length != nodes
        || counts.length != nodes) {
      throw new IllegalArgumentException("structure node lane length mismatch");
    }

    for (int index = 0; index < nodes; index++) {
      NodeKind kind = Objects.requireNonNull(kindLane[index], "kind");
      int start = starts[index];
      int count = counts[index];
      if (start < 0 || count < 0 || start > childLane.length || count > childLane.length - start) {
        throw new IllegalArgumentException("structure child range");
      }
      validateNode(kind, firstLane[index], secondLane[index], count, nodes,
          textLane.length, byteLane.length);
    }
    for (int child : childLane) {
      if (child <= 0 || child > nodes) throw new IllegalArgumentException("structure child id");
    }

    long kindsOffset = HEADER_BYTES;
    long firstOffset = align8(Math.addExact(kindsOffset, nodes));
    long secondOffset = Math.addExact(firstOffset, Math.multiplyExact((long) nodes, Long.BYTES));
    long hashesOffset = Math.addExact(secondOffset, Math.multiplyExact((long) nodes, Long.BYTES));
    long childStartsOffset = Math.addExact(hashesOffset, Math.multiplyExact((long) nodes, Long.BYTES));
    long childCountsOffset =
        Math.addExact(childStartsOffset, Math.multiplyExact((long) nodes, Integer.BYTES));
    long childrenOffset =
        Math.addExact(childCountsOffset, Math.multiplyExact((long) nodes, Integer.BYTES));
    long textOffset =
        align8(Math.addExact(childrenOffset, Math.multiplyExact((long) childLane.length, Integer.BYTES)));
    long bytesOffset =
        Math.addExact(textOffset, Math.multiplyExact((long) textLane.length, Long.BYTES));
    long end = Math.addExact(bytesOffset, byteLane.length);
    if (end > Integer.MAX_VALUE) throw new IllegalArgumentException("structure image exceeds 2 GiB");

    ByteBuffer out = ByteBuffer.allocateDirect((int) end).order(ByteOrder.BIG_ENDIAN);
    out.putLong(MAGIC).putInt(FORMAT).putInt(domainId).putInt(nodes)
        .putInt(childLane.length).putInt(textLane.length).putInt(byteLane.length)
        .putInt((int) kindsOffset).putInt((int) firstOffset).putInt((int) secondOffset)
        .putInt((int) hashesOffset).putInt((int) childStartsOffset).putInt((int) childCountsOffset)
        .putInt((int) childrenOffset).putInt((int) textOffset).putInt((int) bytesOffset)
        .putInt((int) end).putInt(0).putLong(0L).putLong(0L).putInt(0);
    if (out.position() != HEADER_BYTES) throw new IllegalStateException("structure header geometry");

    for (NodeKind kind : kindLane) out.put((byte) kind.ordinal());
    while (out.position() < firstOffset) out.put((byte) 0);
    for (long value : firstLane) out.putLong(value);
    for (long value : secondLane) out.putLong(value);
    for (long value : hashLane) out.putLong(value);
    for (int value : starts) out.putInt(value);
    for (int value : counts) out.putInt(value);
    for (int value : childLane) out.putInt(value);
    while (out.position() < textOffset) out.put((byte) 0);
    for (long value : textLane) out.putLong(value);
    out.put(byteLane);
    if (out.position() != end) throw new IllegalStateException("structure image geometry");
    out.flip();
    return new M3FsStructureImage(
        out, domainId, nodes, childLane.length, textLane.length, byteLane.length);
  }

  private static void validateNode(
      NodeKind kind,
      long first,
      long second,
      int childCount,
      int nodes,
      int textTokens,
      int binaryBytes) {
    switch (kind) {
      case NULL, BOOLEAN, INT32, INT64, FLOAT32, FLOAT64, TEMPORAL -> {
        if (childCount != 0) throw new IllegalArgumentException("scalar node has children");
      }
      case DECIMAL, TEXT -> {
        requireRange(first, second, textTokens, "text token");
        if (childCount != 0) throw new IllegalArgumentException("text node has children");
      }
      case BINARY -> {
        requireRange(first, second, binaryBytes, "binary");
        if (childCount != 0) throw new IllegalArgumentException("binary node has children");
      }
      case TUPLE, SEQUENCE, TABLE -> { }
      case MAP -> {
        if ((childCount & 1) != 0) throw new IllegalArgumentException("map requires key/value pairs");
      }
      case EDGE -> {
        if (childCount != 3) throw new IllegalArgumentException("edge requires 3 children");
      }
      case REF -> {
        if (first <= 0L || first > nodes || second != 0L || childCount != 0) {
          throw new IllegalArgumentException("invalid ref node");
        }
      }
      case EVENT -> {
        if (childCount != 2) throw new IllegalArgumentException("event requires source/payload");
      }
    }
  }

  private static void requireRange(long start, long count, int capacity, String name) {
    if (start < 0L || count < 0L || start > capacity || count > capacity - start) {
      throw new IllegalArgumentException(name + " range");
    }
  }

  private static long align8(long value) {
    return Math.addExact(value, 7L) & ~7L;
  }

  public int domainId() { return domainId; }
  public int nodeCount() { return nodeCount; }
  public int childCount() { return childCount; }
  public int textTokenCount() { return textTokenCount; }
  public int binaryByteCount() { return binaryByteCount; }
  public long byteSize() { return image.capacity(); }

  public ByteBuffer image() {
    return image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
  }
}
