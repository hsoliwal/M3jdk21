// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Device-neutral durable bundle of primitive precompute lanes.
 *
 * <p>The bundle stores content IDs and primitive geometry only. It never contains process/native
 * pointers, CUDA/OpenCL handles, kernel objects, launch state or temporary backend buffers.</p>
 */
public final class M3FsPrecomputeBundle {
  private static final int FORMAT = 1;
  private static final int MAX_LANES = 256;
  private static final int MAX_TEXT_BYTES = 256;

  /** Stable portable lane roles; names are serialized, so enum order is not an on-disk contract. */
  public enum Role {
    CANDIDATE_BYTES(1),
    UTF16_CODE_UNITS_I16(Short.BYTES),
    SEGMENT_PARENT_INDEX_I32(Integer.BYTES),
    SEGMENT_PARENT_START_I32(Integer.BYTES),
    SEGMENT_LENGTH_I32(Integer.BYTES),
    SEGMENT_LOGICAL_END_I32(Integer.BYTES),
    RECORD_OFFSETS_I32(Integer.BYTES),
    RECORD_LENGTHS_I32(Integer.BYTES),
    TOKEN_IDS_I32(Integer.BYTES),
    TOKEN_IDS_I64(Long.BYTES),
    TRIGRAM_IDS_I64(Long.BYTES),
    NGRAM_IDS_I64(Long.BYTES),
    LOOKUP_DISPLACEMENTS_I32(Integer.BYTES),
    LOOKUP_SLOTS_I32(Integer.BYTES),
    POSTING_OFFSETS_I32(Integer.BYTES),
    POSTING_VALUES_I32(Integer.BYTES),
    POSTING_VALUES_I64(Long.BYTES),
    BITMAP_WORDS_I64(Long.BYTES),
    AUTOMATON_TRANSITIONS_I32(Integer.BYTES),
    AUTOMATON_ACCEPT_BITMAP_I64(Long.BYTES),
    SORTED_KEYS_I32(Integer.BYTES),
    SORTED_KEYS_I64(Long.BYTES),
    ROW_IDS_I32(Integer.BYTES),
    CSR_OFFSETS_I32(Integer.BYTES),
    CSR_EDGES_I32(Integer.BYTES),
    HASH64_I64(Long.BYTES),
    MINHASH64_I64(Long.BYTES),
    SIMHASH64_I64(Long.BYTES),
    RANGE_STARTS_I32(Integer.BYTES),
    RANGE_ENDS_I32(Integer.BYTES),
    WINDOW_HASH64_I64(Long.BYTES),
    PREFIX_SCAN_I32(Integer.BYTES),
    PREFIX_SCAN_I64(Long.BYTES),
    HISTOGRAM_COUNTS_I64(Long.BYTES),
    GROUP_OFFSETS_I32(Integer.BYTES),
    RECORD_OFFSETS_I64(Long.BYTES),
    POSTING_OFFSETS_I64(Long.BYTES),
    RANK_SUPERBLOCKS_I32(Integer.BYTES),
    SELECT_SAMPLES_I32(Integer.BYTES),
    REVERSE_CSR_OFFSETS_I32(Integer.BYTES),
    REVERSE_CSR_EDGES_I32(Integer.BYTES),
    TOPOLOGICAL_ORDER_I32(Integer.BYTES),
    RADIX_BUCKET_OFFSETS_I32(Integer.BYTES),
    PARTITION_ROW_IDS_I32(Integer.BYTES),
    KEY_FREQUENCY_I64(Long.BYTES),
    DOMINATOR_PARENT_I32(Integer.BYTES),
    REACHABLE_VERTICES_I32(Integer.BYTES),
    AUTOMATON_ASCII_CLASS_MAP_U8(1),
    AUTOMATON_TRANSITIONS_I16(Short.BYTES),
    AUTOMATON_ACCEPT_U8(1),
    AUTOMATON_METADATA_I32(Integer.BYTES),
    SEARCH_METADATA_I32(Integer.BYTES),
    BYTE_PRESENCE_BITMAP_I64(Long.BYTES),
    PAIR_NGRAM_BITMAP_I64(Long.BYTES),
    TRIGRAM_NGRAM_BITMAP_I64(Long.BYTES),
    PATTERN_BYTES_U8(1),
    PATTERN_OFFSETS_I32(Integer.BYTES),
    AUTOMATON_OUTPUT_MASKS_I64(Long.BYTES),
    TUPLE_VALUES_I64(Long.BYTES);

    private final int elementBytes;

    Role(int elementBytes) {
      this.elementBytes = elementBytes;
    }

    public int elementBytes() {
      return elementBytes;
    }
  }

  /** One admitted mapped lane; no payload copy and no new content identity. */
  public record MappedLane(Lane lane, M3MappedObject mapped) {
    public MappedLane {
      lane = Objects.requireNonNull(lane, "lane");
      mapped = Objects.requireNonNull(mapped, "mapped");
      if (!lane.object().equals(mapped.reference())) {
        throw new IllegalArgumentException("mapped lane/reference mismatch");
      }
    }

    public ByteBuffer bytes() {
      return mapped.bytes(0, mapped.size());
    }

    public ShortBuffer shorts() {
      requireWidth(Short.BYTES);
      return mapped.shorts(0, elementCount(), ByteOrder.BIG_ENDIAN);
    }

    public IntBuffer ints() {
      requireWidth(Integer.BYTES);
      return mapped.ints(0, elementCount(), ByteOrder.BIG_ENDIAN);
    }

    public LongBuffer longs() {
      requireWidth(Long.BYTES);
      return mapped.longs(0, elementCount(), ByteOrder.BIG_ENDIAN);
    }

    public int elementCount() {
      return Math.toIntExact(lane.elements());
    }

    private void requireWidth(int expected) {
      if (lane.role().elementBytes() != expected) {
        throw new IllegalStateException(lane.role() + " element width != " + expected);
      }
    }
  }

  /** Verified mapped view over one durable precompute bundle. */
  public static final class MappedView {
    private final M3FsPrecomputeBundle bundle;
    private final M3MappedObject[] mappings;
    private final long mappedBytes;

    private MappedView(
        M3FsPrecomputeBundle bundle, M3MappedObject[] mappings, long mappedBytes) {
      this.bundle = Objects.requireNonNull(bundle, "bundle");
      this.mappings = mappings.clone();
      this.mappedBytes = mappedBytes;
    }

    public M3FsPrecomputeBundle bundle() {
      return bundle;
    }

    public String profile() {
      return bundle.profile();
    }

    public long mappedBytes() {
      return mappedBytes;
    }

    public List<MappedLane> lanes() {
      return mappedLanes(bundle, mappings);
    }

    public List<MappedLane> lanes(Role role) {
      Role checked = Objects.requireNonNull(role, "role");
      int[] rows = bundle.roleRows(checked);
      return apiList(rows.length, index -> new MappedLane(bundle.lanes.at(rows[index]), mappings[rows[index]]));
    }

    /** Logical UTF-16 composition over content-addressed parent objects. */
    public SegmentedUtf16String segmentedUtf16String() {
      if (!profile().equals("mindex-segmented-utf16-v1")) {
        throw new IllegalStateException("not a segmented UTF-16 bundle");
      }
      int[] parentRows = new int[bundle.laneCount()]; int count = 0;
      for (int i = 0; i < bundle.laneCount(); i++) {
        if (bundle.laneRole(i) == Role.UTF16_CODE_UNITS_I16 && bundle.laneName(i).startsWith("utf16-parent-")) parentRows[count++] = i;
      }
      if (count == 0) throw new IllegalArgumentException("segmented UTF-16 parents missing");
      ByteBuffer[] parents = new ByteBuffer[count];
      for (int i = 0; i < count; i++) parents[i] = mappings[parentRows[i]].bytes(0, mappings[parentRows[i]].size());
      return new SegmentedUtf16String(
          parents,
          laneInts("segment-parent-index"),
          laneInts("segment-parent-start"),
          laneInts("segment-length"),
          laneInts("segment-logical-end"));
    }

    /** Device-neutral gpu-grep segment geometry over existing mapped lanes. */
    public GpuGrepSegments gpuGrepSegments() {
      if (!profile().equals("gpu-grep-segments-v1")) {
        throw new IllegalStateException("not a gpu-grep segment bundle");
      }
      return new GpuGrepSegments(
          laneBytes("candidate-bytes"),
          laneLongs("segment-offsets"),
          laneInts("segment-lengths"));
    }

    /** Compact ASCII DFA over existing verified mapped lanes. */
    public CompactAsciiDfa compactAsciiDfa() {
      if (!profile().equals("compact-ascii-dfa-v1")) {
        throw new IllegalStateException("not a compact ASCII DFA bundle");
      }
      return new CompactAsciiDfa(
          laneBytes("ascii-class-map"),
          laneShorts("dfa-transitions"),
          laneBytes("dfa-accepting"),
          laneInts("dfa-metadata"));
    }

    public PreparedByteSegments preparedByteSegments() {
      if (!profile().equals("gpu-prepared-byte-segments-v1")) {
        throw new IllegalStateException("not a prepared byte-segment bundle");
      }
      return new PreparedByteSegments(
          laneBytes("candidate-bytes"),
          laneInts("segment-starts"),
          laneInts("segment-lengths"),
          laneLongs("byte-presence"),
          laneLongs("pair-ngram-presence"),
          laneLongs("trigram-ngram-presence"),
          laneInts("search-metadata"));
    }

    public PreparedBytePatternMachine preparedBytePatternMachine() {
      if (!profile().equals("gpu-prepared-byte-machine-v1")) {
        throw new IllegalStateException("not a prepared byte-pattern machine bundle");
      }
      return new PreparedBytePatternMachine(
          laneBytes("pattern-bytes"),
          laneInts("pattern-offsets"),
          laneInts("automaton-transitions"),
          laneLongs("automaton-output-masks"),
          laneInts("automaton-metadata"));
    }

    public MappedLane requireLane(String name) {
      int index = bundle.lanes.indexOf(name);
      return new MappedLane(bundle.lanes.at(index), mappings[index]);
    }
    private int laneIndex(String name, int width) {
      int index = bundle.lanes.indexOf(name);
      if (bundle.laneRole(index).elementBytes() != width) throw new IllegalStateException("lane width mismatch");
      return index;
    }
    private ByteBuffer laneBytes(String name) {
      int index = bundle.lanes.indexOf(name); return mappings[index].bytes(0, mappings[index].size());
    }
    private ShortBuffer laneShorts(String name) {
      int index = laneIndex(name, Short.BYTES); return mappings[index].shorts(0, Math.toIntExact(bundle.lanes.elements[index]), ByteOrder.BIG_ENDIAN);
    }
    private IntBuffer laneInts(String name) {
      int index = laneIndex(name, Integer.BYTES); return mappings[index].ints(0, Math.toIntExact(bundle.lanes.elements[index]), ByteOrder.BIG_ENDIAN);
    }
    private LongBuffer laneLongs(String name) {
      int index = laneIndex(name, Long.BYTES); return mappings[index].longs(0, Math.toIntExact(bundle.lanes.elements[index]), ByteOrder.BIG_ENDIAN);
    }
  }

  /**
   * Zero-copy logical String-compatible UTF-16 over separately mapped parent objects.
   *
   * <p>Segment boundaries are UTF-16 code-unit boundaries and may split surrogate pairs exactly
   * like java.lang.String coordinates. Flattening happens only through toCharArray()/toJavaString().
   * </p>
   */
  public static final class SegmentedUtf16String implements CharSequence {
    private final ByteBuffer[] parents;
    private final IntBuffer parentIndexes;
    private final IntBuffer parentStarts;
    private final IntBuffer lengths;
    private final IntBuffer logicalEnds;
    private final int length;

    private SegmentedUtf16String(
        ByteBuffer[] parents,
        IntBuffer parentIndexes,
        IntBuffer parentStarts,
        IntBuffer lengths,
        IntBuffer logicalEnds) {
      Objects.requireNonNull(parents, "parents");
      this.parents = new ByteBuffer[parents.length];
      for (int index = 0; index < parents.length; index++) {
        ByteBuffer parent =
            Objects.requireNonNull(parents[index], "parent")
                .slice()
                .asReadOnlyBuffer()
                .order(ByteOrder.BIG_ENDIAN);
        if ((parent.remaining() & 1) != 0) {
          throw new IllegalArgumentException("UTF-16 parent byte geometry");
        }
        this.parents[index] = parent;
      }
      this.parentIndexes =
          Objects.requireNonNull(parentIndexes, "parentIndexes").slice().asReadOnlyBuffer();
      this.parentStarts =
          Objects.requireNonNull(parentStarts, "parentStarts").slice().asReadOnlyBuffer();
      this.lengths =
          Objects.requireNonNull(lengths, "lengths").slice().asReadOnlyBuffer();
      this.logicalEnds =
          Objects.requireNonNull(logicalEnds, "logicalEnds").slice().asReadOnlyBuffer();

      int segments = this.lengths.remaining();
      if (this.parentIndexes.remaining() != segments
          || this.parentStarts.remaining() != segments
          || this.logicalEnds.remaining() != segments) {
        throw new IllegalArgumentException("segmented UTF-16 lane geometry");
      }
      int cumulative = 0;
      for (int segment = 0; segment < segments; segment++) {
        int parent = this.parentIndexes.get(segment);
        int start = this.parentStarts.get(segment);
        int count = this.lengths.get(segment);
        if (parent < 0
            || parent >= this.parents.length
            || start < 0
            || count < 0
            || start > this.parents[parent].remaining() / Character.BYTES - count) {
          throw new IllegalArgumentException("segmented UTF-16 range");
        }
        cumulative = Math.addExact(cumulative, count);
        if (this.logicalEnds.get(segment) != cumulative) {
          throw new IllegalArgumentException("segmented UTF-16 logical end");
        }
      }
      length = cumulative;
    }

    public int segmentCount() {
      return lengths.remaining();
    }

    public int parentCount() {
      return parents.length;
    }

    @Override
    public int length() {
      return length;
    }

    @Override
    public char charAt(int index) {
      int checked = Objects.checkIndex(index, length);
      int segment = segmentAt(checked);
      int previous = segment == 0 ? 0 : logicalEnds.get(segment - 1);
      int parent = parentIndexes.get(segment);
      int offset = parentStarts.get(segment) + checked - previous;
      return parents[parent].getChar(offset * Character.BYTES);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      Objects.checkFromToIndex(start, end, length);
      return start == 0 && end == length ? this : new Range(this, start, end);
    }

    public void copyChars(int sourceStart, char[] target, int targetStart, int count) {
      Objects.requireNonNull(target, "target");
      Objects.checkFromIndexSize(sourceStart, count, length);
      Objects.checkFromIndexSize(targetStart, count, target.length);
      if (count == 0) return;
      int absolute = sourceStart;
      int segment = segmentAt(absolute);
      int copied = 0;
      while (copied < count) {
        int previous = segment == 0 ? 0 : logicalEnds.get(segment - 1);
        int available = logicalEnds.get(segment) - absolute;
        int chunk = Math.min(count - copied, available);
        int parent = parentIndexes.get(segment);
        int parentStart = parentStarts.get(segment) + absolute - previous;
        ByteBuffer bytes = parents[parent];
        for (int offset = 0; offset < chunk; offset++) {
          target[targetStart + copied + offset] =
              bytes.getChar((parentStart + offset) * Character.BYTES);
        }
        copied += chunk;
        absolute += chunk;
        if (absolute == logicalEnds.get(segment)) segment++;
      }
    }

    public char[] toCharArray() {
      return M3NativeContent.materializeUtf16Segments(
          parents, parentIndexes, parentStarts, lengths, length);
    }

    public String toJavaString() {
      return new String(toCharArray());
    }

    public boolean contentEquals(CharSequence other) {
      Objects.requireNonNull(other, "other");
      if (other.length() != length) return false;
      for (int index = 0; index < length; index++) {
        if (charAt(index) != other.charAt(index)) return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return toJavaString();
    }

    private static final class Range implements CharSequence {
      private final SegmentedUtf16String source;
      private final int start;
      private final int length;

      private Range(SegmentedUtf16String source, int start, int end) {
        this.source = Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(start, end, source.length());
        this.start = start;
        this.length = end - start;
      }

      @Override
      public int length() {
        return length;
      }

      @Override
      public char charAt(int index) {
        return source.charAt(start + Objects.checkIndex(index, length));
      }

      @Override
      public CharSequence subSequence(int from, int to) {
        Objects.checkFromToIndex(from, to, length);
        return from == 0 && to == length
            ? this
            : new Range(source, start + from, start + to);
      }

      @Override
      public String toString() {
        char[] chars = new char[length];
        source.copyChars(start, chars, 0, length);
        return new String(chars);
      }
    }

    private int segmentAt(int logicalIndex) {
      int low = 0;
      int high = logicalEnds.remaining();
      while (low < high) {
        int middle = low + ((high - low) >>> 1);
        if (logicalEnds.get(middle) <= logicalIndex) low = middle + 1;
        else high = middle;
      }
      return low;
    }
  }

  public record PreparedByteSegments(
      ByteBuffer bytes,
      IntBuffer starts,
      IntBuffer lengths,
      LongBuffer bytePresence,
      LongBuffer pairNgramPresence,
      LongBuffer trigramNgramPresence,
      IntBuffer metadata) implements M3PreparedByteSegments {
    public PreparedByteSegments {
      bytes = Objects.requireNonNull(bytes, "bytes").asReadOnlyBuffer();
      starts = Objects.requireNonNull(starts, "starts").asReadOnlyBuffer();
      lengths = Objects.requireNonNull(lengths, "lengths").asReadOnlyBuffer();
      bytePresence = Objects.requireNonNull(bytePresence, "bytePresence").asReadOnlyBuffer();
      pairNgramPresence =
          Objects.requireNonNull(pairNgramPresence, "pairNgramPresence").asReadOnlyBuffer();
      trigramNgramPresence =
          Objects.requireNonNull(trigramNgramPresence, "trigramNgramPresence").asReadOnlyBuffer();
      metadata = Objects.requireNonNull(metadata, "metadata").asReadOnlyBuffer();
      if (metadata.remaining() != 5) throw new IllegalArgumentException("prepared segment metadata");
      int meta = metadata.position();
      int segments = metadata.get(meta);
      int words = metadata.get(meta + 1);
      int pairBuckets = metadata.get(meta + 2);
      int trigramBuckets = metadata.get(meta + 3);
      int maxLength = metadata.get(meta + 4);
      if (segments < 0
          || words != ((segments + Long.SIZE - 1) >>> 6)
          || pairBuckets < 1
          || trigramBuckets < 1
          || maxLength < 0
          || starts.remaining() != segments
          || lengths.remaining() != segments
          || bytePresence.remaining() != Math.multiplyExact(256, words)
          || pairNgramPresence.remaining() != Math.multiplyExact(pairBuckets, words)
          || trigramNgramPresence.remaining() != Math.multiplyExact(trigramBuckets, words)) {
        throw new IllegalArgumentException("prepared segment geometry");
      }
      int cursor = 0;
      for (int index = 0; index < segments; index++) {
        int start = starts.get(starts.position() + index);
        int length = lengths.get(lengths.position() + index);
        if (start != cursor || length < 0) throw new IllegalArgumentException("prepared segment packing");
        cursor = Math.addExact(cursor, length);
      }
      if (cursor != bytes.remaining()) throw new IllegalArgumentException("prepared segment terminal");
    }
    public int size() { return segmentCount(); }
    public int packedBytes() { return bytes.remaining(); }
    public int byteAt(int index) { return Byte.toUnsignedInt(bytes.get(bytes.position() + Objects.checkIndex(index, bytes.remaining()))); }
    public int segmentStart(int index) { return starts.get(starts.position() + Objects.checkIndex(index, size())); }
    public int segmentLength(int index) { return lengths.get(lengths.position() + Objects.checkIndex(index, size())); }
    public int pairNgramBuckets() { return pairBuckets(); }
    public int trigramNgramBuckets() { return trigramBuckets(); }
    public long bytePresenceWord(int bucket, int word) { return bytePresence.get(bytePresence.position() + Objects.checkIndex(bucket, 256) * bitmapWords() + Objects.checkIndex(word, bitmapWords())); }
    public long pairNgramPresenceWord(int bucket, int word) { return pairNgramPresence.get(pairNgramPresence.position() + Objects.checkIndex(bucket, pairBuckets()) * bitmapWords() + Objects.checkIndex(word, bitmapWords())); }
    public long trigramNgramPresenceWord(int bucket, int word) { return trigramNgramPresence.get(trigramNgramPresence.position() + Objects.checkIndex(bucket, trigramBuckets()) * bitmapWords() + Objects.checkIndex(word, bitmapWords())); }
    public int segmentCount() { return metadata.get(metadata.position()); }
    public int bitmapWords() { return metadata.get(metadata.position() + 1); }
    public int pairBuckets() { return metadata.get(metadata.position() + 2); }
    public int trigramBuckets() { return metadata.get(metadata.position() + 3); }
    public int maxSegmentLength() { return metadata.get(metadata.position() + 4); }
  }

  public record PreparedBytePatternMachine(
      ByteBuffer patternBytes,
      IntBuffer patternOffsets,
      IntBuffer transitions,
      LongBuffer outputMasks,
      IntBuffer metadata) implements M3PreparedBytePatternMachine {
    public PreparedBytePatternMachine {
      patternBytes = Objects.requireNonNull(patternBytes, "patternBytes").asReadOnlyBuffer();
      patternOffsets = Objects.requireNonNull(patternOffsets, "patternOffsets").asReadOnlyBuffer();
      transitions = Objects.requireNonNull(transitions, "transitions").asReadOnlyBuffer();
      outputMasks = Objects.requireNonNull(outputMasks, "outputMasks").asReadOnlyBuffer();
      metadata = Objects.requireNonNull(metadata, "metadata").asReadOnlyBuffer();
      if (metadata.remaining() != 4) throw new IllegalArgumentException("prepared machine metadata");
      int meta = metadata.position();
      int patterns = metadata.get(meta);
      int states = metadata.get(meta + 1);
      int alphabet = metadata.get(meta + 2);
      int start = metadata.get(meta + 3);
      if (patterns < 1 || patterns > Long.SIZE || states < 1 || alphabet != 256
          || start < 0 || start >= states || patternOffsets.remaining() != patterns + 1
          || transitions.remaining() != Math.multiplyExact(states, alphabet)
          || outputMasks.remaining() != states) {
        throw new IllegalArgumentException("prepared machine geometry");
      }
      int base = patternOffsets.position();
      int terminal = patternOffsets.get(base + patterns);
      if (patternOffsets.get(base) != 0 || terminal != patternBytes.remaining()) {
        throw new IllegalArgumentException("prepared pattern offsets");
      }
      for (int index = 0; index < patterns; index++) {
        int from = patternOffsets.get(base + index);
        int to = patternOffsets.get(base + index + 1);
        if (from < 0 || to <= from || to > terminal) {
          throw new IllegalArgumentException("prepared pattern geometry");
        }
      }
    }
    public int totalPatternBytes() { return patternBytes.remaining(); }
    public int patternOffset(int index) { return patternOffsets.get(patternOffsets.position() + Objects.checkIndex(index, patternCount() + 1)); }
    public int patternLength(int index) { Objects.checkIndex(index, patternCount()); return patternOffset(index + 1) - patternOffset(index); }
    public int patternByteAt(int pattern, int offset) { return Byte.toUnsignedInt(patternBytes.get(patternBytes.position() + patternOffset(pattern) + Objects.checkIndex(offset, patternLength(pattern)))); }
    public int transitionCount() { return transitions.remaining(); }
    public int transitionAt(int index) { return transitions.get(transitions.position() + Objects.checkIndex(index, transitionCount())); }
    public long outputMaskAt(int state) { return outputMask(state); }
    public int patternCount() { return metadata.get(metadata.position()); }
    public int stateCount() { return metadata.get(metadata.position() + 1); }
    public int startState() { return metadata.get(metadata.position() + 3); }
    public int nextState(int state, int unsignedByte) {
      Objects.checkIndex(state, stateCount());
      if (unsignedByte < 0 || unsignedByte > 255) throw new IndexOutOfBoundsException(unsignedByte);
      return transitions.get(transitions.position() + state * 256 + unsignedByte);
    }
    public long outputMask(int state) {
      return outputMasks.get(outputMasks.position() + Objects.checkIndex(state, stateCount()));
    }
  }

  /** Device-neutral compact ASCII DFA geometry. */
  public record CompactAsciiDfa(
      ByteBuffer asciiClassByValue,
      ShortBuffer transitions,
      ByteBuffer accepting,
      IntBuffer metadata) {
    public CompactAsciiDfa {
      asciiClassByValue =
          Objects.requireNonNull(asciiClassByValue, "asciiClassByValue").asReadOnlyBuffer();
      transitions = Objects.requireNonNull(transitions, "transitions").asReadOnlyBuffer();
      accepting = Objects.requireNonNull(accepting, "accepting").asReadOnlyBuffer();
      metadata = Objects.requireNonNull(metadata, "metadata").asReadOnlyBuffer();

      if (metadata.remaining() != 3 || asciiClassByValue.remaining() != 128) {
        throw new IllegalArgumentException("compact ASCII DFA metadata geometry");
      }
      int meta = metadata.position();
      int startState = metadata.get(meta);
      int classCount = metadata.get(meta + 1);
      int stateCount = metadata.get(meta + 2);
      if (stateCount < 1
          || startState < 0
          || startState >= stateCount
          || classCount < 1
          || classCount > 128
          || accepting.remaining() != stateCount
          || transitions.remaining() != Math.multiplyExact(stateCount, classCount)) {
        throw new IllegalArgumentException("compact ASCII DFA geometry");
      }

      boolean[] used = new boolean[classCount];
      int classes = asciiClassByValue.position();
      for (int value = 0; value < 128; value++) {
        int id = Byte.toUnsignedInt(asciiClassByValue.get(classes + value));
        if (id >= classCount) {
          throw new IllegalArgumentException("compact ASCII DFA class");
        }
        used[id] = true;
      }
      for (boolean present : used) {
        if (!present) {
          throw new IllegalArgumentException("unused compact ASCII DFA class");
        }
      }

      int accepts = accepting.position();
      for (int state = 0; state < stateCount; state++) {
        byte value = accepting.get(accepts + state);
        if (value != 0 && value != 1) {
          throw new IllegalArgumentException("compact ASCII DFA accepting lane");
        }
      }
      int transitionBase = transitions.position();
      for (int index = 0; index < transitions.remaining(); index++) {
        short target = transitions.get(transitionBase + index);
        if (target < -1 || target >= stateCount) {
          throw new IllegalArgumentException("compact ASCII DFA transition target");
        }
      }
    }

    public int startState() {
      return metadata.get(metadata.position());
    }

    public int classCount() {
      return metadata.get(metadata.position() + 1);
    }

    public int stateCount() {
      return metadata.get(metadata.position() + 2);
    }

    public int nextState(int state, int asciiValue) {
      Objects.checkIndex(state, stateCount());
      if (asciiValue < 0 || asciiValue >= 128) {
        throw new IndexOutOfBoundsException("asciiValue=" + asciiValue);
      }
      int symbolClass =
          Byte.toUnsignedInt(asciiClassByValue.get(asciiClassByValue.position() + asciiValue));
      return transitions.get(transitions.position() + state * classCount() + symbolClass);
    }
  }

  /**
   * Zero-copy mapped view of one gpu-grep-style packed segment corpus.
   *
   * <p>Offsets are segment starts plus one terminal end offset. Lengths duplicate the differences
   * intentionally so GPU/JNI kernels can consume flat geometry without recomputing it.</p>
   */
  public record GpuGrepSegments(
      ByteBuffer bytes, LongBuffer offsets, IntBuffer lengths) {
    public GpuGrepSegments {
      bytes = Objects.requireNonNull(bytes, "bytes").asReadOnlyBuffer();
      offsets = Objects.requireNonNull(offsets, "offsets").asReadOnlyBuffer();
      lengths = Objects.requireNonNull(lengths, "lengths").asReadOnlyBuffer();
      if (offsets.remaining() != lengths.remaining() + 1) {
        throw new IllegalArgumentException("gpu-grep segment geometry");
      }
      int segments = lengths.remaining();
      long first = offsets.get(offsets.position());
      long terminal = offsets.get(offsets.position() + segments);
      if (first != 0L || terminal != bytes.remaining()) {
        throw new IllegalArgumentException("gpu-grep segment bounds");
      }
      for (int index = 0; index < segments; index++) {
        long from = offsets.get(offsets.position() + index);
        long to = offsets.get(offsets.position() + index + 1);
        int length = lengths.get(lengths.position() + index);
        if (from < 0L || to < from || to - from != length) {
          throw new IllegalArgumentException("gpu-grep segment offset/length");
        }
      }
    }

    public int segmentCount() {
      return lengths.remaining();
    }

    /**
     * Exact per-segment byte search. One native call covers the full segment batch; Java is the
     * behavior-identical fallback.
     */
    public int grepSegments(byte[] needle, int[] outputRows) {
      return M3NativeContent.grepSegments(bytes, offsets, lengths, needle, outputRows);
    }
  }

  /** One content-addressed fixed-width primitive lane. */
  public record Lane(String name, Role role, long elements, M3ObjectRef object) {
    public Lane {
      name = requireText(name, "lane name");
      role = Objects.requireNonNull(role, "role");
      object = Objects.requireNonNull(object, "object");
      if (elements < 0L) throw new IllegalArgumentException("negative lane element count");
      if (object.kind() != M3ObjectKind.PRECOMPUTE_OUTPUT) {
        throw new IllegalArgumentException("precompute lane must use PRECOMPUTE_OUTPUT");
      }
      long expected = Math.multiplyExact(elements, role.elementBytes());
      if (expected != object.size()) {
        throw new IllegalArgumentException(
            "lane byte size " + object.size() + " != expected " + expected);
      }
    }
  }

  /** Fixed-width retained metadata; rows are constructed only by public list/getter access. */
  static final class LaneTable {
    private static final Role[] ROLES = Role.values();
    private String[] names = new String[8];
    private byte[] roles = new byte[8];
    private long[] elements = new long[8];
    private long[] sizes = new long[8];
    private byte[] objectIds = new byte[8 * M3ContentId.BYTES];
    private int size;

    void addEncoded(String name, Role role, long count, byte[] id, long bytes) {
      String checked = requireText(name, "lane name");
      Objects.requireNonNull(role, "role");
      Objects.checkFromIndexSize(0, M3ContentId.BYTES, Objects.requireNonNull(id, "id").length);
      if (size >= MAX_LANES || count < 0 || Math.multiplyExact(count, role.elementBytes()) != bytes) {
        throw new IllegalArgumentException("invalid precompute lane geometry");
      }
      if (size == names.length) {
        int capacity = Math.min(MAX_LANES, size * 2);
        names = java.util.Arrays.copyOf(names, capacity);
        roles = java.util.Arrays.copyOf(roles, capacity);
        elements = java.util.Arrays.copyOf(elements, capacity);
        sizes = java.util.Arrays.copyOf(sizes, capacity);
        objectIds = java.util.Arrays.copyOf(objectIds, capacity * M3ContentId.BYTES);
      }
      names[size] = checked; roles[size] = (byte) role.ordinal();
      elements[size] = count; sizes[size] = bytes;
      System.arraycopy(id, 0, objectIds, size * M3ContentId.BYTES, M3ContentId.BYTES);
      size++;
    }

    static LaneTable from(List<Lane> supplied) {
      LaneTable table = new LaneTable();
      for (Lane lane : Objects.requireNonNull(supplied, "lanes")) {
        Lane checked = Objects.requireNonNull(lane, "lane");
        table.addEncoded(checked.name(), checked.role(), checked.elements(),
            checked.object().id().bytes(), checked.object().size());
      }
      return table;
    }

    LaneTable orderedCopy() {
      if (size < 1) throw new IllegalArgumentException("bundle must contain lanes");
      int[] order = new int[size];
      for (int i = 0; i < size; i++) {
        int at = i;
        while (at > 0 && names[order[at - 1]].compareTo(names[i]) > 0) {
          order[at] = order[at - 1]; at--;
        }
        order[at] = i;
      }
      LaneTable result = new LaneTable();
      result.names = new String[size]; result.roles = new byte[size];
      result.elements = new long[size]; result.sizes = new long[size];
      result.objectIds = new byte[size * M3ContentId.BYTES]; result.size = size;
      for (int i = 0; i < size; i++) {
        int row = order[i];
        if (i > 0 && names[row].equals(result.names[i - 1])) {
          throw new IllegalArgumentException("duplicate lane name: " + names[row]);
        }
        result.names[i] = names[row]; result.roles[i] = roles[row];
        result.elements[i] = elements[row]; result.sizes[i] = sizes[row];
        System.arraycopy(objectIds, row * M3ContentId.BYTES,
            result.objectIds, i * M3ContentId.BYTES, M3ContentId.BYTES);
      }
      return result;
    }

    int indexOf(String name) {
      int index = java.util.Arrays.binarySearch(names, 0, size, requireText(name, "lane name"));
      if (index < 0) throw new IllegalArgumentException("missing precompute lane: " + name);
      return index;
    }

    Lane at(int index) {
      Objects.checkIndex(index, size);
      return new Lane(names[index], ROLES[Byte.toUnsignedInt(roles[index])], elements[index],
          new M3ObjectRef(M3ContentId.of(java.util.Arrays.copyOfRange(objectIds,
              index * M3ContentId.BYTES, (index + 1) * M3ContentId.BYTES)),
              sizes[index], M3ObjectKind.PRECOMPUTE_OUTPUT));
    }
  }

  abstract static class ArrayListView<E> extends java.util.AbstractList<E>
      implements java.util.RandomAccess {
    @Override public final E set(int index, E value) { throw new UnsupportedOperationException(); }
    @Override public final void add(int index, E value) { throw new UnsupportedOperationException(); }
    @Override public final boolean add(E value) { throw new UnsupportedOperationException(); }
    @Override public final E remove(int index) { throw new UnsupportedOperationException(); }
    @Override public final boolean remove(Object value) { throw new UnsupportedOperationException(); }
    @Override public final boolean addAll(java.util.Collection<? extends E> values) { throw new UnsupportedOperationException(); }
    @Override public final boolean addAll(int index, java.util.Collection<? extends E> values) { throw new UnsupportedOperationException(); }
    @Override public final boolean removeAll(java.util.Collection<?> values) { throw new UnsupportedOperationException(); }
    @Override public final boolean retainAll(java.util.Collection<?> values) { throw new UnsupportedOperationException(); }
    @Override public final boolean removeIf(java.util.function.Predicate<? super E> filter) { throw new UnsupportedOperationException(); }
    @Override public final void replaceAll(java.util.function.UnaryOperator<E> operator) { throw new UnsupportedOperationException(); }
    @Override public final void sort(java.util.Comparator<? super E> comparator) { throw new UnsupportedOperationException(); }
    @Override public final void clear() { throw new UnsupportedOperationException(); }
    @Override public final List<E> subList(int from, int to) {
      return java.util.Collections.unmodifiableList(super.subList(from, to));
    }
  }

  static <E> List<E> apiList(int count, java.util.function.IntFunction<E> at) {
    return new ArrayListView<>() {
      @Override public int size() { return count; }
      @Override public E get(int index) { return at.apply(Objects.checkIndex(index, count)); }
    };
  }

  static final class MappedLaneList extends ArrayListView<MappedLane> {
    private final M3FsPrecomputeBundle bundle;
    private final M3MappedObject[] mappings;
    MappedLaneList(M3FsPrecomputeBundle bundle, M3MappedObject[] mappings) {
      this.bundle = Objects.requireNonNull(bundle, "bundle");
      this.mappings = Objects.requireNonNull(mappings, "mappings").clone();
      if (mappings.length != bundle.laneCount()) throw new IllegalArgumentException("mapped lane count");
      for (int i = 0; i < mappings.length; i++) {
        if (!Objects.requireNonNull(mappings[i], "mapped").matchesReference(bundle.lanes.objectIds,
            i * M3ContentId.BYTES, bundle.lanes.sizes[i], M3ObjectKind.PRECOMPUTE_OUTPUT.ordinal())) {
          throw new IllegalArgumentException("mapped lane/reference mismatch");
        }
      }
    }
    @Override public int size() { return mappings.length; }
    @Override public MappedLane get(int index) {
      return new MappedLane(bundle.lanes.at(index), mappings[Objects.checkIndex(index, size())]);
    }
    MappedLane require(String name) { return get(bundle.lanes.indexOf(name)); }
    M3MappedObject[] mappings() { return mappings.clone(); }
  }

  static MappedLaneList mappedLanes(M3FsPrecomputeBundle bundle, M3MappedObject[] mappings) {
    return new MappedLaneList(bundle, mappings);
  }

  static MappedLaneList normalizeMappedLanes(M3FsPrecomputeBundle bundle, List<MappedLane> supplied) {
    Objects.requireNonNull(supplied, "lanes");
    if (supplied instanceof MappedLaneList array) {
      if (!java.util.Arrays.equals(bundle.canonical, array.bundle.canonical)) {
        throw new IllegalArgumentException("mapped lane bundle mismatch");
      }
      return array.bundle == bundle ? array : new MappedLaneList(bundle, array.mappings);
    }
    if (supplied.size() != bundle.laneCount()) throw new IllegalArgumentException("mapped lane count");
    M3MappedObject[] mappings = new M3MappedObject[supplied.size()];
    for (int i = 0; i < mappings.length; i++) {
      MappedLane mapped = Objects.requireNonNull(supplied.get(i), "mapped lane");
      Lane lane = mapped.lane();
      if (!lane.name().equals(bundle.lanes.names[i]) || lane.role() != bundle.laneRole(i)
          || lane.elements() != bundle.lanes.elements[i]) {
        throw new IllegalArgumentException("mapped lane metadata mismatch");
      }
      mappings[i] = mapped.mapped();
    }
    return new MappedLaneList(bundle, mappings);
  }

  private final byte[] keyId;
  private final String profile;
  private final LaneTable lanes;
  private final byte[] canonical;

  public M3FsPrecomputeBundle(M3ContentId keyId, String profile, List<Lane> lanes) {
    this(Objects.requireNonNull(keyId, "keyId").bytes(), profile, LaneTable.from(lanes));
  }

  private M3FsPrecomputeBundle(byte[] keyId, String profile, LaneTable lanes) {
    if (Objects.requireNonNull(keyId, "keyId").length != M3ContentId.BYTES) throw new IllegalArgumentException("key ID length");
    this.keyId = keyId.clone();
    this.profile = requireText(profile, "profile");
    this.lanes = Objects.requireNonNull(lanes, "lanes").orderedCopy();
    this.canonical = encode();
  }

  static M3FsPrecomputeBundle fromLanes(byte[] keyId, String profile, LaneTable lanes) {
    return new M3FsPrecomputeBundle(keyId, profile, lanes);
  }

  public M3ContentId keyId() { return M3ContentId.of(keyId); }
  boolean matchesKeyId(byte[] ids, int offset) {
    Objects.checkFromIndexSize(offset, M3ContentId.BYTES, ids.length);
    return java.util.Arrays.equals(keyId, 0, M3ContentId.BYTES, ids, offset, offset + M3ContentId.BYTES);
  }
  public String profile() { return profile; }
  public List<Lane> lanes() { return apiList(lanes.size, lanes::at); }
  public List<Lane> lanes(Role role) {
    int[] rows = roleRows(Objects.requireNonNull(role, "role"));
    return apiList(rows.length, index -> lanes.at(rows[index]));
  }
  private int[] roleRows(Role role) {
    int[] rows = new int[lanes.size]; int count = 0;
    for (int i = 0; i < lanes.size; i++) if (laneRole(i) == role) rows[count++] = i;
    return java.util.Arrays.copyOf(rows, count);
  }
  public Lane requireLane(String name) { return lanes.at(lanes.indexOf(name)); }
  int laneCount() { return lanes.size; }
  Role laneRole(int index) { return LaneTable.ROLES[Byte.toUnsignedInt(lanes.roles[Objects.checkIndex(index, lanes.size)])]; }
  String laneName(int index) { return lanes.names[Objects.checkIndex(index, lanes.size)]; }
  long laneBytes(int index) { return lanes.sizes[Objects.checkIndex(index, lanes.size)]; }
  void copyLaneId(int index, byte[] target, int offset) {
    Objects.checkIndex(index, lanes.size); Objects.checkFromIndexSize(offset, M3ContentId.BYTES, target.length);
    System.arraycopy(lanes.objectIds, index * M3ContentId.BYTES, target, offset, M3ContentId.BYTES);
  }
  long roleMask() {
    long bits = 0;
    for (int i = 0; i < lanes.size; i++) bits |= 1L << Byte.toUnsignedInt(lanes.roles[i]);
    return bits;
  }
  public long totalBytes() {
    long total = 0;
    for (int i = 0; i < lanes.size; i++) total = Math.addExact(total, lanes.sizes[i]);
    return total;
  }
  public MappedView openMapped(M3DirectoryContentStore store, int maxLaneBytes, long maxTotalBytes)
      throws IOException {
    Objects.requireNonNull(store, "store");
    if (maxLaneBytes < 0 || maxTotalBytes < 0) throw new IllegalArgumentException("negative mapping budget");
    M3FsPrecomputeCatalog.requireCompatible(this, false);
    long total = totalBytes();
    if (total > maxTotalBytes) throw new IOException("precompute bundle exceeds total mapping budget");
    for (int i = 0; i < lanes.size; i++) {
      if (lanes.sizes[i] > maxLaneBytes) throw new IOException("precompute lane exceeds mapping budget: " + lanes.names[i]);
    }
    M3MappedObject[] mappings = new M3MappedObject[lanes.size];
    for (int i = 0; i < lanes.size; i++) {
      mappings[i] = store.openMappedId(lanes.objectIds, i * M3ContentId.BYTES, lanes.sizes[i],
          M3ObjectKind.PRECOMPUTE_OUTPUT, maxLaneBytes);
    }
    return new MappedView(this, mappings, total);
  }
  public byte[] canonicalBytes() { return canonical.clone(); }
  public void requireStore(M3DirectoryContentStore store) throws IOException {
    Objects.requireNonNull(store, "store");
    for (int i = 0; i < lanes.size; i++) store.requireObject(lanes.objectIds, i * M3ContentId.BYTES, lanes.sizes[i]);
  }
  public static M3FsPrecomputeBundle decode(byte[] encoded) throws IOException {
    Objects.requireNonNull(encoded, "encoded");
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
      if (in.readInt() != FORMAT) throw new IOException("unsupported M3 FS precompute bundle format");
      byte[] key = new byte[M3ContentId.BYTES]; in.readFully(key);
      String profile = readString(in, "profile");
      int count = in.readInt();
      if (count < 1 || count > MAX_LANES) throw new IOException("invalid M3 FS lane count");
      LaneTable columns = new LaneTable(); byte[] id = new byte[M3ContentId.BYTES];
      for (int i = 0; i < count; i++) {
        String name = readString(in, "lane name");
        Role role = Role.valueOf(readString(in, "lane role"));
        long elements = in.readLong(); in.readFully(id); long size = in.readLong();
        columns.addEncoded(name, role, elements, id, size);
      }
      if (in.read() != -1) throw new IOException("trailing M3 FS bundle bytes");
      M3FsPrecomputeBundle result = new M3FsPrecomputeBundle(key, profile, columns);
      if (!java.util.Arrays.equals(encoded, result.canonical)) throw new IOException("non-canonical M3 FS bundle");
      return result;
    } catch (IllegalArgumentException | ArithmeticException invalid) {
      throw new IOException("invalid M3 FS precompute bundle", invalid);
    }
  }
  private byte[] encode() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT); out.write(keyId); writeString(out, profile); out.writeInt(lanes.size);
        for (int i = 0; i < lanes.size; i++) {
          writeString(out, lanes.names[i]); writeString(out, laneRole(i).name());
          out.writeLong(lanes.elements[i]); out.write(lanes.objectIds, i * M3ContentId.BYTES, M3ContentId.BYTES);
          out.writeLong(lanes.sizes[i]);
        }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) { throw new AssertionError(impossible); }
  }

  private static void writeString(DataOutputStream out, String value) throws IOException {
    byte[] bytes = requireText(value, "text").getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_TEXT_BYTES) {
      throw new IllegalArgumentException("precompute text exceeds byte limit");
    }
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String readString(DataInputStream in, String field) throws IOException {
    int length = in.readInt();
    if (length < 1 || length > MAX_TEXT_BYTES) {
      throw new IOException("invalid " + field + " byte length");
    }
    byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) throw new IOException("truncated " + field);
    try {
      String value =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
      return requireText(value, field);
    } catch (java.nio.charset.CharacterCodingException invalid) {
      throw new IOException("invalid UTF-8 in " + field, invalid);
    }
  }

  private static String requireText(String value, String field) {
    String checked = Objects.requireNonNull(value, field).strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }
}
