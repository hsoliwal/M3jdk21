// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsPreparedSearchStateTest {
  @TempDir Path temp;

  @Test
  void preparedSegmentsAndPatternMachineRoundTripThroughMappedFs() throws Exception {
    List<byte[]> segments =
        List.of(bytes("alpha beta"), bytes("gamma alpha"), bytes("delta"));
    SegmentFixture preparedSegments =
        SegmentFixture.prepare(segments, 4096);
    MachineFixture machine =
        MachineFixture.compile(
            List.of(bytes("alpha"), bytes("gamma"), bytes("delta")), 4096);

    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put(
            "alpha beta\ngamma alpha\ndelta".getBytes(StandardCharsets.UTF_8),
            M3ObjectKind.RAW_BYTES);

    M3PrecomputeKey segmentKey =
        new M3PrecomputeKey(
            "search",
            "gpu-prepared-byte-segments",
            1,
            List.of(source.id()),
            Map.of("root", preparedSegments.rootSha256()));
    M3FsPrecomputeWriter segmentWriter =
        new M3FsPrecomputeWriter(
                store, segmentKey, "gpu-prepared-byte-segments-v1")
            .addPreparedByteSegments(preparedSegments);
    M3ObjectRef segmentDescriptor = segmentWriter.publish(cache);
    assertEquals(segmentDescriptor, segmentWriter.publish(cache));

    M3FsPrecomputeBundle.MappedView segmentBundle =
        cache.lookupMappedBundle(segmentKey, 8 * 1024 * 1024, 32L * 1024 * 1024)
            .orElseThrow();
    M3FsPrecomputeBundle.PreparedByteSegments mappedSegments =
        segmentBundle.preparedByteSegments();

    assertEquals(preparedSegments.size(), mappedSegments.segmentCount());
    assertEquals(preparedSegments.bitmapWords(), mappedSegments.bitmapWords());
    assertEquals(preparedSegments.pairNgramBuckets(), mappedSegments.pairBuckets());
    assertEquals(
        preparedSegments.trigramNgramBuckets(), mappedSegments.trigramBuckets());
    assertEquals(preparedSegments.maxSegmentLength(), mappedSegments.maxSegmentLength());

    for (int segment = 0; segment < preparedSegments.size(); segment++) {
      assertEquals(
          preparedSegments.segmentStart(segment),
          mappedSegments.starts().get(segment));
      assertEquals(
          preparedSegments.segmentLength(segment),
          mappedSegments.lengths().get(segment));
    }
    for (int index = 0; index < preparedSegments.packedBytes(); index++) {
      assertEquals(preparedSegments.byteAt(index), Byte.toUnsignedInt(mappedSegments.bytes().get(index)));
    }
    int words = preparedSegments.bitmapWords();
    for (int value : new int[] {'a', 'g', 'z'}) {
      for (int word = 0; word < words; word++) {
        assertEquals(
            preparedSegments.bytePresenceWord(value, word),
            mappedSegments.bytePresence().get(value * words + word));
      }
    }
    for (int bucket : new int[] {0, 17, preparedSegments.pairNgramBuckets() - 1}) {
      for (int word = 0; word < words; word++) {
        assertEquals(
            preparedSegments.pairNgramPresenceWord(bucket, word),
            mappedSegments.pairNgramPresence().get(bucket * words + word));
      }
    }
    for (int bucket : new int[] {0, 31, preparedSegments.trigramNgramBuckets() - 1}) {
      for (int word = 0; word < words; word++) {
        assertEquals(
            preparedSegments.trigramNgramPresenceWord(bucket, word),
            mappedSegments.trigramNgramPresence().get(bucket * words + word));
      }
    }

    M3PrecomputeKey machineKey =
        new M3PrecomputeKey(
            "search",
            "gpu-prepared-byte-machine",
            1,
            List.of(source.id()),
            Map.of("root", machine.rootSha256()));
    M3FsPrecomputeWriter machineWriter =
        new M3FsPrecomputeWriter(
                store, machineKey, "gpu-prepared-byte-machine-v1")
            .addPreparedBytePatternMachine(machine);
    M3ObjectRef machineDescriptor = machineWriter.publish(cache);
    assertEquals(machineDescriptor, machineWriter.publish(cache));

    M3FsPrecomputeBundle.PreparedBytePatternMachine mappedMachine =
        cache.lookupMappedBundle(machineKey, 8 * 1024 * 1024, 32L * 1024 * 1024)
            .orElseThrow()
            .preparedBytePatternMachine();
    assertEquals(machine.patternCount(), mappedMachine.patternCount());
    assertEquals(machine.stateCount(), mappedMachine.stateCount());
    assertEquals(0, mappedMachine.startState());

    for (int index = 0; index < machine.transitionCount(); index++) {
      assertEquals(machine.transitionAt(index), mappedMachine.transitions().get(index));
    }
    for (int state = 0; state < machine.stateCount(); state++) {
      assertEquals(machine.outputMaskAt(state), mappedMachine.outputMask(state));
    }

    long observed = run(mappedMachine, bytes("xx gamma alpha yy"));
    assertTrue((observed & 1L) != 0L);
    assertTrue((observed & (1L << 1)) != 0L);
    assertTrue((observed & (1L << 2)) == 0L);

    M3FsPrecomputeCatalog.PipelineManifest pipeline =
        new M3FsPrecomputeCatalog.PipelineManifest(
            "gpu-prepared-byte-search-v1",
            List.of(
                new M3FsPrecomputeCatalog.PipelineEntry(
                    "gpu-prepared-byte-segments-v1", segmentKey),
                new M3FsPrecomputeCatalog.PipelineEntry(
                    "gpu-prepared-byte-machine-v1", machineKey)));
    M3ObjectRef pipelineRef = pipeline.publish(store);
    M3FsPrecomputeCatalog.PipelineManifest loaded =
        M3FsPrecomputeCatalog.PipelineManifest.load(store, pipelineRef);
    assertEquals(pipeline.id(), loaded.id());
    assertTrue(loaded.runtimeOnlyState().contains("device-automaton"));
    assertTrue(loaded.runtimeOnlyState().contains("stream"));
    assertEquals(
        2,
        loaded.openMapped(cache, 8 * 1024 * 1024, 64L * 1024 * 1024)
            .orElseThrow()
            .bundles()
            .size());
  }

  private static long run(
      M3FsPrecomputeBundle.PreparedBytePatternMachine machine, byte[] input) {
    int state = machine.startState();
    long mask = 0L;
    for (byte value : input) {
      state = machine.nextState(state, Byte.toUnsignedInt(value));
      mask |= machine.outputMask(state);
    }
    return mask;
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /** Bounded test producer; exercises the public primitive contract without donating a GPU runtime. */
  private static final class SegmentFixture implements M3PreparedByteSegments {
    private final byte[] bytes;
    private final int[] starts;
    private final int[] lengths;
    private final long[] bytePresence;
    private final long[] pairPresence;
    private final long[] trigramPresence;
    private final int maximum;
    private final int words;
    private static final int BUCKETS = 64;
    private SegmentFixture(List<byte[]> input, int limit) {
      starts = new int[input.size()]; lengths = new int[input.size()];
      int total = 0, max = 0;
      for (int i = 0; i < input.size(); i++) {
        starts[i] = total; lengths[i] = input.get(i).length;
        total = Math.addExact(total, lengths[i]); max = Math.max(max, lengths[i]);
      }
      if (total > limit) throw new IllegalArgumentException("fixture byte budget");
      maximum = max; words = (input.size() + 63) >>> 6; bytes = new byte[total];
      bytePresence = new long[256 * words]; pairPresence = new long[BUCKETS * words]; trigramPresence = new long[BUCKETS * words];
      for (int row = 0; row < input.size(); row++) {
        byte[] value = input.get(row); System.arraycopy(value, 0, bytes, starts[row], value.length);
        int word = row >>> 6; long bit = 1L << row;
        for (int i = 0; i < value.length; i++) {
          int a = Byte.toUnsignedInt(value[i]); bytePresence[a * words + word] |= bit;
          if (i + 1 < value.length) {
            int pair = (a * 31 + Byte.toUnsignedInt(value[i + 1])) & (BUCKETS - 1);
            pairPresence[pair * words + word] |= bit;
          }
          if (i + 2 < value.length) {
            int tri = (a * 31 * 31 + Byte.toUnsignedInt(value[i + 1]) * 31 + Byte.toUnsignedInt(value[i + 2])) & (BUCKETS - 1);
            trigramPresence[tri * words + word] |= bit;
          }
        }
      }
    }
    static SegmentFixture prepare(List<byte[]> input, int limit) { return new SegmentFixture(input, limit); }
    String rootSha256() { return M3ContentId.digest(bytes).hex(); }
    public int size() { return starts.length; }
    public int packedBytes() { return bytes.length; }
    public int bitmapWords() { return words; }
    public int pairNgramBuckets() { return BUCKETS; }
    public int trigramNgramBuckets() { return BUCKETS; }
    public int maxSegmentLength() { return maximum; }
    public int byteAt(int offset) { return Byte.toUnsignedInt(bytes[offset]); }
    public int segmentStart(int segment) { return starts[segment]; }
    public int segmentLength(int segment) { return lengths[segment]; }
    public long bytePresenceWord(int value, int word) { return bytePresence[value * words + word]; }
    public long pairNgramPresenceWord(int bucket, int word) { return pairPresence[bucket * words + word]; }
    public long trigramNgramPresenceWord(int bucket, int word) { return trigramPresence[bucket * words + word]; }
  }

  /** Independent small longest-suffix machine; the mapped transitions must preserve real matches. */
  private static final class MachineFixture implements M3PreparedBytePatternMachine {
    private final byte[][] patterns;
    private final int[] offsets;
    private final int[] transitions;
    private final long[] outputs;
    private MachineFixture(List<byte[]> input, int maxStates) {
      if (input.isEmpty() || input.size() > Long.SIZE) throw new IllegalArgumentException("fixture patterns");
      patterns = new byte[input.size()][]; offsets = new int[patterns.length + 1];
      java.util.ArrayList<byte[]> prefixes = new java.util.ArrayList<>(); prefixes.add(new byte[0]);
      for (int pattern = 0; pattern < patterns.length; pattern++) {
        patterns[pattern] = input.get(pattern).clone();
        if (patterns[pattern].length == 0) throw new IllegalArgumentException("empty fixture pattern");
        offsets[pattern + 1] = Math.addExact(offsets[pattern], patterns[pattern].length);
        for (int length = 1; length <= patterns[pattern].length; length++) {
          byte[] candidate = java.util.Arrays.copyOf(patterns[pattern], length); boolean found = false;
          for (byte[] prefix : prefixes) if (java.util.Arrays.equals(prefix, candidate)) { found = true; break; }
          if (!found) prefixes.add(candidate);
        }
      }
      if (prefixes.size() > maxStates) throw new IllegalArgumentException("fixture states");
      transitions = new int[prefixes.size() * 256]; outputs = new long[prefixes.size()];
      for (int state = 0; state < prefixes.size(); state++) {
        byte[] prefix = prefixes.get(state);
        for (int pattern = 0; pattern < patterns.length; pattern++) if (suffix(prefix, patterns[pattern])) outputs[state] |= 1L << pattern;
        for (int value = 0; value < 256; value++) {
          byte[] next = java.util.Arrays.copyOf(prefix, prefix.length + 1); next[prefix.length] = (byte) value;
          int best = 0;
          for (int target = 1; target < prefixes.size(); target++) {
            if (prefixes.get(target).length > prefixes.get(best).length && suffix(next, prefixes.get(target))) best = target;
          }
          transitions[state * 256 + value] = best;
        }
      }
    }
    private static boolean suffix(byte[] bytes, byte[] ending) {
      return ending.length <= bytes.length && java.util.Arrays.equals(bytes, bytes.length - ending.length, bytes.length, ending, 0, ending.length);
    }
    static MachineFixture compile(List<byte[]> input, int limit) { return new MachineFixture(input, limit); }
    String rootSha256() {
      var digest = M3ContentId.newDigest(); for (byte[] pattern : patterns) digest.update(pattern);
      return java.util.HexFormat.of().formatHex(digest.digest());
    }
    public int totalPatternBytes() { return offsets[offsets.length - 1]; }
    public int patternCount() { return patterns.length; }
    public int stateCount() { return outputs.length; }
    public int transitionCount() { return transitions.length; }
    public int patternOffset(int pattern) { return offsets[pattern]; }
    public int patternByteAt(int pattern, int offset) { return Byte.toUnsignedInt(patterns[pattern][offset]); }
    public int transitionAt(int offset) { return transitions[offset]; }
    public long outputMaskAt(int state) { return outputs[state]; }
  }
}
