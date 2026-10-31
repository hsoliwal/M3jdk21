// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

/**
 * Streaming builder for portable precompute bundles.
 *
 * <p>Integer lanes are persisted in canonical big-endian order without allocating a second
 * full-size byte array. The content store still owns hashing, create-only publication and exact
 * duplicate validation.</p>
 */
public final class M3FsPrecomputeWriter {
  private final M3DirectoryContentStore store;
  private final M3PrecomputeKey key;
  private final String profile;
  private final M3FsPrecomputeBundle.LaneTable lanes = new M3FsPrecomputeBundle.LaneTable();

  public M3FsPrecomputeWriter(
      M3DirectoryContentStore store, M3PrecomputeKey key, String profileId) {
    this.store = Objects.requireNonNull(store, "store");
    this.key = Objects.requireNonNull(key, "key");
    this.profile = Objects.requireNonNull(profileId, "profileId").strip();
    M3FsPrecomputeCatalog.profileIndex(this.profile);
  }

  public M3FsPrecomputeWriter addBytes(
      String name, M3FsPrecomputeBundle.Role role, byte[] values) throws IOException {
    Objects.requireNonNull(values, "values");
    requireWidth(role, 1);
    return addStreamLane(name, role, values.length, new java.io.ByteArrayInputStream(values));
  }

  public M3FsPrecomputeWriter addBytes(
      String name,
      M3FsPrecomputeBundle.Role role,
      int elements,
      IntUnaryOperator valueAt)
      throws IOException {
    requireElements(elements);
    requireWidth(role, 1);
    IntUnaryOperator values = Objects.requireNonNull(valueAt, "valueAt");
    return addFunctionalLane(
        name,
        role,
        elements,
        1,
        index -> {
          int value = values.applyAsInt(index);
          if (value < 0 || value > 0xff) throw new IllegalArgumentException("byte lane value");
          return value;
        });
  }

  public M3FsPrecomputeWriter addInts(
      String name, M3FsPrecomputeBundle.Role role, int[] values) throws IOException {
    Objects.requireNonNull(values, "values");
    requireWidth(role, Integer.BYTES);
    return addStreamLane(name, role, values.length, new IntLaneInputStream(values));
  }

  public M3FsPrecomputeWriter addLongs(
      String name, M3FsPrecomputeBundle.Role role, long[] values) throws IOException {
    Objects.requireNonNull(values, "values");
    requireWidth(role, Long.BYTES);
    return addStreamLane(name, role, values.length, new LongLaneInputStream(values));
  }

  public M3FsPrecomputeWriter addShorts(
      String name,
      M3FsPrecomputeBundle.Role role,
      int elements,
      IntUnaryOperator valueAt)
      throws IOException {
    requireElements(elements);
    requireWidth(role, Short.BYTES);
    IntUnaryOperator values = Objects.requireNonNull(valueAt, "valueAt");
    return addFunctionalLane(
        name,
        role,
        elements,
        Short.BYTES,
        index -> {
          int value = values.applyAsInt(index);
          if (value < 0 || value > 0xffff) {
            throw new IllegalArgumentException("short lane value");
          }
          return value;
        });
  }

  public M3FsPrecomputeWriter addInts(
      String name,
      M3FsPrecomputeBundle.Role role,
      int elements,
      IntUnaryOperator valueAt)
      throws IOException {
    requireElements(elements);
    requireWidth(role, Integer.BYTES);
    IntUnaryOperator values = Objects.requireNonNull(valueAt, "valueAt");
    return addFunctionalLane(
        name,
        role,
        elements,
        Integer.BYTES,
        index -> Integer.toUnsignedLong(values.applyAsInt(index)));
  }

  public M3FsPrecomputeWriter addLongs(
      String name,
      M3FsPrecomputeBundle.Role role,
      int elements,
      IntToLongFunction valueAt)
      throws IOException {
    requireElements(elements);
    requireWidth(role, Long.BYTES);
    return addFunctionalLane(
        name,
        role,
        elements,
        Long.BYTES,
        Objects.requireNonNull(valueAt, "valueAt"));
  }

  private M3FsPrecomputeWriter addFunctionalLane(
      String name,
      M3FsPrecomputeBundle.Role role,
      int elements,
      int width,
      IntToLongFunction valueAt)
      throws IOException {
    return addStreamLane(name, role, elements, new FunctionalLaneInputStream(elements, width, valueAt));
  }

  private M3FsPrecomputeWriter addStreamLane(String name, M3FsPrecomputeBundle.Role role,
      long elements, InputStream stream) throws IOException {
    byte[] id = new byte[M3ContentId.BYTES];
    long size = store.putId(stream, id, 0);
    lanes.addEncoded(name, role, elements, id, size);
    return this;
  }

  /** Adds a validated compact ASCII DFA without re-expanding equivalence classes. */
  public M3FsPrecomputeWriter addCompactAsciiDfa(
      byte[] asciiClassByValue,
      int classCount,
      short[] transitions,
      byte[] accepting,
      int startState)
      throws IOException {
    byte[] classes = Objects.requireNonNull(asciiClassByValue, "asciiClassByValue");
    short[] targets = Objects.requireNonNull(transitions, "transitions");
    byte[] accepts = Objects.requireNonNull(accepting, "accepting");
    if (classes.length != 128
        || classCount < 1
        || classCount > 128
        || accepts.length == 0
        || startState < 0
        || startState >= accepts.length
        || targets.length != Math.multiplyExact(accepts.length, classCount)) {
      throw new IllegalArgumentException("compact ASCII DFA geometry");
    }

    boolean[] used = new boolean[classCount];
    for (byte encoded : classes) {
      int id = Byte.toUnsignedInt(encoded);
      if (id >= classCount) throw new IllegalArgumentException("compact ASCII DFA class");
      used[id] = true;
    }
    for (boolean present : used) {
      if (!present) throw new IllegalArgumentException("unused compact ASCII DFA class");
    }
    for (short target : targets) {
      if (target < -1 || target >= accepts.length) {
        throw new IllegalArgumentException("compact ASCII DFA transition target");
      }
    }
    for (byte value : accepts) {
      if (value != 0 && value != 1) {
        throw new IllegalArgumentException("compact ASCII DFA accepting lane");
      }
    }

    return addBytes(
            "ascii-class-map",
            M3FsPrecomputeBundle.Role.AUTOMATON_ASCII_CLASS_MAP_U8,
            classes)
        .addShorts(
            "dfa-transitions",
            M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I16,
            targets.length,
            index -> Short.toUnsignedInt(targets[index]))
        .addBytes(
            "dfa-accepting",
            M3FsPrecomputeBundle.Role.AUTOMATON_ACCEPT_U8,
            accepts)
        .addInts(
            "dfa-metadata",
            M3FsPrecomputeBundle.Role.AUTOMATON_METADATA_I32,
            new int[] {startState, classCount, accepts.length});
  }

  /**
   * Adds canonical packed bytes plus prevalidated gpu-grep segment geometry.
   *
   * <p>Offsets contain segmentCount+1 values and the terminal offset equals the packed byte
   * length.</p>
   */
  /**
   * Persists separately-addressable UTF-16 parents plus logical segment ranges.
   *
   * <p>Equal parent payloads collapse through the content store. No joined UTF-16 payload is
   * published.</p>
   */
  public M3FsPrecomputeWriter addSegmentedUtf16(
      List<? extends CharSequence> parents,
      int[] parentIndex,
      int[] parentStart,
      int[] segmentLength)
      throws IOException {
    List<? extends CharSequence> sourceParents =
        List.copyOf(Objects.requireNonNull(parents, "parents"));
    int[] requestedParents = Objects.requireNonNull(parentIndex, "parentIndex");
    int[] starts = Objects.requireNonNull(parentStart, "parentStart");
    int[] lengths = Objects.requireNonNull(segmentLength, "segmentLength");
    if (sourceParents.isEmpty()
        || requestedParents.length != starts.length
        || requestedParents.length != lengths.length) {
      throw new IllegalArgumentException("segmented UTF-16 geometry");
    }

    for (int segment = 0; segment < lengths.length; segment++) {
      int inputParent = requestedParents[segment];
      if (inputParent < 0 || inputParent >= sourceParents.size()) {
        throw new IllegalArgumentException("segmented UTF-16 parent index");
      }
      CharSequence parent = sourceParents.get(inputParent);
      Objects.checkFromIndexSize(starts[segment], lengths[segment], parent.length());
    }

    byte[] uniqueIds = new byte[252 * M3ContentId.BYTES];
    int uniqueCount = 0;
    int[] compactParentByInput = new int[sourceParents.size()];
    byte[] id = new byte[M3ContentId.BYTES];
    for (int index = 0; index < sourceParents.size(); index++) {
      CharSequence parent = Objects.requireNonNull(sourceParents.get(index), "parent");
      long size = store.putId(new FunctionalLaneInputStream(parent.length(), Character.BYTES,
          position -> parent.charAt(position)), id, 0);
      int ordinal = 0;
      while (ordinal < uniqueCount && !java.util.Arrays.equals(id, 0, M3ContentId.BYTES,
          uniqueIds, ordinal * M3ContentId.BYTES, (ordinal + 1) * M3ContentId.BYTES)) ordinal++;
      if (ordinal == uniqueCount) {
        if (uniqueCount >= 252) throw new IllegalArgumentException("too many unique segmented UTF-16 parents");
        System.arraycopy(id, 0, uniqueIds, ordinal * M3ContentId.BYTES, M3ContentId.BYTES);
        uniqueCount++;
        lanes.addEncoded(String.format(java.util.Locale.ROOT, "utf16-parent-%06d", ordinal),
            M3FsPrecomputeBundle.Role.UTF16_CODE_UNITS_I16, size / Character.BYTES, id, size);
      }
      compactParentByInput[index] = ordinal;
    }

    int segments = lengths.length;
    int[] compactParents = new int[segments];
    int[] logicalEnds = new int[segments];
    int cumulative = 0;
    for (int segment = 0; segment < segments; segment++) {
      int inputParent = requestedParents[segment];
      int length = lengths[segment];
      compactParents[segment] = compactParentByInput[inputParent];
      cumulative = Math.addExact(cumulative, length);
      logicalEnds[segment] = cumulative;
    }

    return addInts(
            "segment-parent-index",
            M3FsPrecomputeBundle.Role.SEGMENT_PARENT_INDEX_I32,
            compactParents)
        .addInts(
            "segment-parent-start",
            M3FsPrecomputeBundle.Role.SEGMENT_PARENT_START_I32,
            starts)
        .addInts(
            "segment-length",
            M3FsPrecomputeBundle.Role.SEGMENT_LENGTH_I32,
            lengths)
        .addInts(
            "segment-logical-end",
            M3FsPrecomputeBundle.Role.SEGMENT_LOGICAL_END_I32,
            logicalEnds);
  }

  public M3FsPrecomputeWriter addGpuGrepSegments(
      byte[] packedBytes, long[] offsets, int[] lengths)
      throws IOException {
    byte[] bytes = Objects.requireNonNull(packedBytes, "packedBytes");
    long[] starts = Objects.requireNonNull(offsets, "offsets");
    int[] sizes = Objects.requireNonNull(lengths, "lengths");
    if (starts.length != sizes.length + 1
        || starts.length == 0
        || starts[0] != 0L
        || starts[starts.length - 1] != bytes.length) {
      throw new IllegalArgumentException("gpu-grep segment geometry");
    }
    for (int index = 0; index < sizes.length; index++) {
      long from = starts[index];
      long to = starts[index + 1];
      if (from < 0L || to < from || to - from != sizes[index]) {
        throw new IllegalArgumentException("gpu-grep segment offset/length");
      }
    }
    return addBytes(
            "candidate-bytes",
            M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
            bytes)
        .addLongs(
            "segment-offsets",
            M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64,
            starts)
        .addInts(
            "segment-lengths",
            M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,
            sizes);
  }

  public M3FsPrecomputeWriter addPreparedByteSegments(
      M3PreparedByteSegments prepared)
      throws IOException {
    M3PreparedByteSegments value = Objects.requireNonNull(prepared, "prepared");
    int segments = value.size();
    int words = value.bitmapWords();
    return addBytes("candidate-bytes", M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
            value.packedBytes(), value::byteAt)
        .addInts("segment-starts", M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32,
            segments, value::segmentStart)
        .addInts("segment-lengths", M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,
            segments, value::segmentLength)
        .addLongs("byte-presence", M3FsPrecomputeBundle.Role.BYTE_PRESENCE_BITMAP_I64,
            Math.multiplyExact(256, words),
            index -> value.bytePresenceWord(index / words, index % words))
        .addLongs("pair-ngram-presence", M3FsPrecomputeBundle.Role.PAIR_NGRAM_BITMAP_I64,
            Math.multiplyExact(value.pairNgramBuckets(), words),
            index -> value.pairNgramPresenceWord(index / words, index % words))
        .addLongs("trigram-ngram-presence", M3FsPrecomputeBundle.Role.TRIGRAM_NGRAM_BITMAP_I64,
            Math.multiplyExact(value.trigramNgramBuckets(), words),
            index -> value.trigramNgramPresenceWord(index / words, index % words))
        .addInts("search-metadata", M3FsPrecomputeBundle.Role.SEARCH_METADATA_I32,
            new int[] {segments, words, value.pairNgramBuckets(),
                value.trigramNgramBuckets(), value.maxSegmentLength()});
  }

  public M3FsPrecomputeWriter addPreparedBytePatternMachine(
      M3PreparedBytePatternMachine prepared)
      throws IOException {
    M3PreparedBytePatternMachine value = Objects.requireNonNull(prepared, "prepared");
    return addBytes("pattern-bytes", M3FsPrecomputeBundle.Role.PATTERN_BYTES_U8,
            value.totalPatternBytes(), index -> patternByteAt(value, index))
        .addInts("pattern-offsets", M3FsPrecomputeBundle.Role.PATTERN_OFFSETS_I32,
            value.patternCount() + 1, value::patternOffset)
        .addInts("automaton-transitions", M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I32,
            value.transitionCount(), value::transitionAt)
        .addLongs("automaton-output-masks", M3FsPrecomputeBundle.Role.AUTOMATON_OUTPUT_MASKS_I64,
            value.stateCount(), value::outputMaskAt)
        .addInts("automaton-metadata", M3FsPrecomputeBundle.Role.AUTOMATON_METADATA_I32,
            new int[] {value.patternCount(), value.stateCount(),
                M3PreparedBytePatternMachine.ALPHABET_SIZE, 0});
  }

  private static int patternByteAt(
      M3PreparedBytePatternMachine machine, int globalOffset) {
    int low = 0;
    int high = machine.patternCount();
    while (low < high) {
      int middle = low + ((high - low) >>> 1);
      if (machine.patternOffset(middle + 1) <= globalOffset) low = middle + 1;
      else high = middle;
    }
    if (low >= machine.patternCount()) throw new IndexOutOfBoundsException(globalOffset);
    return machine.patternByteAt(low, globalOffset - machine.patternOffset(low));
  }

  public M3FsPrecomputeWriter addPath(
      String name,
      M3FsPrecomputeBundle.Role role,
      long elements,
      Path source)
      throws IOException {
    byte[] id = new byte[M3ContentId.BYTES];
    long size = store.putId(Objects.requireNonNull(source, "source"), id, 0);
    lanes.addEncoded(name, role, elements, id, size);
    return this;
  }

  public M3FsPrecomputeBundle bundle() {
    byte[] keyId = new byte[M3ContentId.BYTES];
    key.copyIdTo(keyId, 0);
    M3FsPrecomputeBundle bundle =
        M3FsPrecomputeBundle.fromLanes(keyId, profile, lanes);
    M3FsPrecomputeCatalog.requireCompatible(bundle, false);
    return bundle;
  }

  public M3ObjectRef publish(M3PrecomputeCache cache) throws IOException {
    return Objects.requireNonNull(cache, "cache").publishBundle(key, bundle());
  }

  private static void requireElements(int elements) {
    if (elements < 0) throw new IllegalArgumentException("negative lane elements");
  }

  private static void requireWidth(M3FsPrecomputeBundle.Role role, int width) {
    M3FsPrecomputeBundle.Role checked = Objects.requireNonNull(role, "role");
    if (checked.elementBytes() != width) {
      throw new IllegalArgumentException(
          checked + " requires " + checked.elementBytes() + " bytes per element");
    }
  }

  private abstract static class PrimitiveLaneInputStream extends InputStream {
    private long bytePosition;

    abstract long byteLength();

    abstract int byteAt(long position);

    @Override
    public final int read() {
      if (bytePosition >= byteLength()) return -1;
      return byteAt(bytePosition++);
    }

    @Override
    public final int read(byte[] target, int offset, int length) {
      Objects.checkFromIndexSize(offset, length, target.length);
      if (length == 0) return 0;
      long remaining = byteLength() - bytePosition;
      if (remaining <= 0L) return -1;
      int count = (int) Math.min(remaining, length);
      long position = bytePosition;
      for (int index = 0; index < count; index++) {
        target[offset + index] = (byte) byteAt(position + index);
      }
      bytePosition += count;
      return count;
    }
  }

  private static final class FunctionalLaneInputStream
      extends PrimitiveLaneInputStream {
    private final int elements;
    private final int width;
    private final IntToLongFunction valueAt;

    private FunctionalLaneInputStream(
        int elements, int width, IntToLongFunction valueAt) {
      this.elements = elements;
      this.width = width;
      this.valueAt = Objects.requireNonNull(valueAt, "valueAt");
    }

    @Override
    long byteLength() {
      return Math.multiplyExact((long) elements, width);
    }

    @Override
    int byteAt(long position) {
      int element = Math.toIntExact(position / width);
      int within = (int) (position % width);
      long value = valueAt.applyAsLong(element);
      int shift = (width - 1 - within) * Byte.SIZE;
      return (int) ((value >>> shift) & 0xffL);
    }
  }

  private static final class IntLaneInputStream extends PrimitiveLaneInputStream {
    private final int[] values;

    private IntLaneInputStream(int[] values) {
      this.values = values;
    }

    @Override
    long byteLength() {
      return (long) values.length * Integer.BYTES;
    }

    @Override
    int byteAt(long position) {
      int value = values[(int) (position >>> 2)];
      int shift = (3 - ((int) position & 3)) << 3;
      return (value >>> shift) & 0xff;
    }
  }

  private static final class LongLaneInputStream extends PrimitiveLaneInputStream {
    private final long[] values;

    private LongLaneInputStream(long[] values) {
      this.values = values;
    }

    @Override
    long byteLength() {
      return (long) values.length * Long.BYTES;
    }

    @Override
    int byteAt(long position) {
      long value = values[(int) (position >>> 3)];
      int shift = (7 - ((int) position & 7)) << 3;
      return (int) ((value >>> shift) & 0xffL);
    }
  }
}
