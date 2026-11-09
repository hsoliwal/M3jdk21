// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Oracle of m3/fs/recipe/precompute-joined-lanes-20261106 stage 01 (Synexia crate m3fs-joined-directory-history-20261102):
// the joined segment directory, its mapped payload owner and the transposed byte-presence bitmap, exercised through
// the public records (the Writer/Catalog profiles of that crate are a separate leaf, see DECISIONS.tsv).
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsJoinedContentSegmentsTest {
  @TempDir Path temp;

  /** Directory over the given payloads; segment i reads payload ids[i] at [starts[i], starts[i] + lengths[i]). */
  static M3FsPrecomputeBundle.JoinedContentSegments directory(
      M3ObjectRef[] refs, int[] payloadOf, int[] starts, int[] lengths) {
    int segments = lengths.length;
    ByteBuffer ids = ByteBuffer.allocate(segments * M3ContentId.BYTES);
    long[] logical = new long[segments + 1];
    for (int i = 0; i < segments; i++) {
      ids.put(refs[payloadOf[i]].id().bytes());
      logical[i + 1] = logical[i] + lengths[i];
    }
    ids.flip();
    return new M3FsPrecomputeBundle.JoinedContentSegments(
        ids, IntBuffer.wrap(starts.clone()), LongBuffer.wrap(logical), IntBuffer.wrap(lengths.clone()));
  }

  @Test
  void mappedSegmentsShareOneMappingPerContentIdAndReadTheLogicalJoin() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    Random random = new Random(0x4a4f494e4544L);
    int distinct = 48;
    M3ObjectRef[] refs = new M3ObjectRef[distinct];
    byte[][] payloads = new byte[distinct][];
    for (int p = 0; p < distinct; p++) {
      payloads[p] = ("payload-" + p + "-" + "x".repeat(p % 13)).getBytes(StandardCharsets.UTF_8);
      refs[p] = store.put(payloads[p], M3ObjectKind.RAW_BYTES);
    }
    int segments = 512;
    int[] payloadOf = new int[segments];
    int[] starts = new int[segments];
    int[] lengths = new int[segments];
    long distinctBytes = 0L;
    boolean[] seen = new boolean[distinct];
    for (int i = 0; i < segments; i++) {
      payloadOf[i] = random.nextInt(distinct);
      int size = payloads[payloadOf[i]].length;
      starts[i] = random.nextInt(size + 1);
      lengths[i] = random.nextInt(size - starts[i] + 1);
      if (!seen[payloadOf[i]]) {
        seen[payloadOf[i]] = true;
        distinctBytes += size;
      }
    }
    int used = 0;
    for (boolean s : seen) used += s ? 1 : 0;
    var directory = directory(refs, payloadOf, starts, lengths);
    assertEquals(segments, directory.segmentCount());
    var mapped = directory.openRawSegments(store, 1 << 20, 1L << 20);
    assertEquals(segments, mapped.segmentCount());
    assertEquals(used, mapped.uniquePayloadCount());
    assertEquals(distinctBytes, mapped.mappedPayloadBytes());
    assertEquals(directory.logicalLength(), mapped.logicalLength());

    byte[] joined = new byte[Math.toIntExact(directory.logicalLength())];
    int at = 0;
    for (int i = 0; i < segments; i++) {
      assertEquals(refs[payloadOf[i]].id(), directory.contentId(i));
      assertEquals(starts[i], directory.sourceOffset(i));
      assertEquals(lengths[i], directory.length(i));
      assertEquals(at, directory.logicalStart(i));
      assertEquals(at + lengths[i], directory.logicalEnd(i));
      ByteBuffer segment = mapped.segment(i);
      byte[] actual = new byte[segment.remaining()];
      segment.get(actual);
      assertArrayEquals(Arrays.copyOfRange(payloads[payloadOf[i]], starts[i], starts[i] + lengths[i]), actual);
      System.arraycopy(actual, 0, joined, at, actual.length);
      at += actual.length;
    }
    for (int logical = 0; logical < joined.length; logical++) {
      assertEquals(joined[logical], mapped.byteAt(logical));
    }
    for (int trial = 0; trial < 200; trial++) {
      int from = random.nextInt(joined.length + 1);
      int count = random.nextInt(joined.length - from + 1);
      byte[] target = new byte[count + 3];
      mapped.copyTo(from, target, 3, count);
      assertArrayEquals(Arrays.copyOfRange(joined, from, from + count), Arrays.copyOfRange(target, 3, 3 + count));
    }
    ByteBuffer[] views = mapped.asReadOnlyBuffers();
    assertEquals(segments, views.length);
    assertTrue(views[0].isReadOnly());
    assertThrows(ReadOnlyBufferException.class, () -> mapped.segment(1).put((byte) 0));
    ByteBuffer borrow = mapped.segment(0);
    int unchanged = borrow.remaining();
    borrow.position(borrow.limit());
    assertEquals(unchanged, mapped.segment(0).remaining());
    assertThrows(IndexOutOfBoundsException.class, () -> mapped.byteAt(joined.length));
    assertThrows(IndexOutOfBoundsException.class, () -> mapped.copyTo(1, new byte[joined.length], 0, joined.length));
  }

  @Test
  void mappingBudgetAndPayloadRangesRefuse() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3ObjectRef[] refs = {
      store.put("alpha".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES),
      store.put("beta-beta".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES)
    };
    var directory = directory(refs, new int[] {0, 1, 0, 1}, new int[] {0, 1, 2, 0}, new int[] {5, 8, 3, 9});
    assertEquals(14L, directory.openRawSegments(store, 64, 14L).mappedPayloadBytes());
    IOException budget = assertThrows(IOException.class, () -> directory.openRawSegments(store, 64, 13L));
    assertEquals("joined segment payload mapping budget exceeded", budget.getMessage());
    assertThrows(IllegalArgumentException.class, () -> directory.openRawSegments(store, -1, 14L));
    assertThrows(IllegalArgumentException.class, () -> directory.openRawSegments(store, 64, -1L));
    var overrun = directory(refs, new int[] {0}, new int[] {1}, new int[] {5});
    IOException range = assertThrows(IOException.class, () -> overrun.openRawSegments(store, 64, 64L));
    assertEquals("joined segment range exceeds payload", range.getMessage());
  }

  @Test
  void malformedDirectoryGeometryRefuses() {
    ByteBuffer ids = ByteBuffer.allocate(2 * M3ContentId.BYTES);
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedContentSegments(
        ids, IntBuffer.wrap(new int[] {0, 0}), LongBuffer.wrap(new long[] {0, 1}), IntBuffer.wrap(new int[] {1, 1})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedContentSegments(
        ids, IntBuffer.wrap(new int[] {0, 0}), LongBuffer.wrap(new long[] {1, 2, 3}), IntBuffer.wrap(new int[] {1, 1})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedContentSegments(
        ids, IntBuffer.wrap(new int[] {0, -1}), LongBuffer.wrap(new long[] {0, 1, 2}), IntBuffer.wrap(new int[] {1, 1})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedContentSegments(
        ids, IntBuffer.wrap(new int[] {0, 0}), LongBuffer.wrap(new long[] {0, 1, 3}), IntBuffer.wrap(new int[] {1, 1})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedContentSegments(
        ByteBuffer.allocate(M3ContentId.BYTES), IntBuffer.wrap(new int[] {0, 0}),
        LongBuffer.wrap(new long[] {0, 1, 2}), IntBuffer.wrap(new int[] {1, 1})));
    var empty = new M3FsPrecomputeBundle.JoinedContentSegments(
        ByteBuffer.allocate(0), IntBuffer.allocate(0), LongBuffer.wrap(new long[] {0}), IntBuffer.allocate(0));
    assertEquals(0, empty.segmentCount());
    assertEquals(0L, empty.logicalLength());
  }

  @Test
  void bytePresenceCrossesUnsigned64BitWordBoundariesWithoutMissingRows() {
    int segmentCount = 257;
    int words = (segmentCount + 63) >>> 6;
    long[] presence = new long[256 * words];
    int[] expected = {0, 63, 64, 65, 127, 128, 256};
    for (int row : expected) {
      add(presence, words, 'n', row);
      add(presence, words, 'e', row);
    }
    add(presence, words, 'n', 42); // a necessary condition only: no false rejection
    var index = new M3FsPrecomputeBundle.JoinedBytePresence(
        LongBuffer.wrap(presence), IntBuffer.wrap(new int[] {segmentCount, words}));
    int[] output = new int[segmentCount];
    Arrays.fill(output, -99);
    int count = index.candidateRows(new byte[] {'n', 'e'}, output);
    assertEquals(expected.length, count);
    assertArrayEquals(expected, Arrays.copyOf(output, count));
    for (int i = count; i < output.length; i++) assertEquals(-99, output[i]);
    assertEquals(0, index.candidateRows(new byte[] {'z'}, output));
    assertThrows(IllegalArgumentException.class, () -> index.candidateRows(new byte[0], output));
    assertThrows(IllegalArgumentException.class, () -> index.candidateRows(new byte[] {'n'}, new int[segmentCount - 1]));
    assertEquals(257, index.segmentCount());
    assertEquals(words, index.bitmapWords());
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedBytePresence(
        LongBuffer.wrap(new long[256]), IntBuffer.wrap(new int[] {257, 4})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedBytePresence(
        LongBuffer.wrap(new long[256 * 5 - 1]), IntBuffer.wrap(new int[] {257, 5})));
    assertThrows(IllegalArgumentException.class, () -> new M3FsPrecomputeBundle.JoinedBytePresence(
        LongBuffer.wrap(new long[0]), IntBuffer.wrap(new int[] {-1, 0})));
  }

  @Test
  void bytePresenceNeverRejectsAnExactSegmentMatch() {
    Random random = new Random(0x50524553454e43L);
    for (int trial = 0; trial < 300; trial++) {
      int segments = random.nextInt(300);
      int words = (segments + 63) >>> 6;
      byte[][] rows = new byte[segments][];
      long[] presence = new long[256 * words];
      for (int row = 0; row < segments; row++) {
        rows[row] = new byte[random.nextInt(24)];
        for (int i = 0; i < rows[row].length; i++) rows[row][i] = (byte) ('a' + random.nextInt(6));
        for (byte value : rows[row]) add(presence, words, Byte.toUnsignedInt(value), row);
      }
      byte[] needle = new byte[1 + random.nextInt(3)];
      for (int i = 0; i < needle.length; i++) needle[i] = (byte) ('a' + random.nextInt(7));
      var index = new M3FsPrecomputeBundle.JoinedBytePresence(
          LongBuffer.wrap(presence), IntBuffer.wrap(new int[] {segments, words}));
      int[] output = new int[segments];
      int count = index.candidateRows(needle, output);
      int expected = 0;
      for (int row = 0; row < segments; row++) {
        boolean all = true;
        for (byte value : needle) all &= contains(rows[row], new byte[] {value});
        if (all) assertEquals(row, output[expected++], "candidate rows are exactly the rows holding every byte");
        if (contains(rows[row], needle)) assertTrue(all, "exact match row " + row + " was rejected");
      }
      assertEquals(expected, count);
    }
  }

  static void add(long[] data, int words, int value, int row) {
    data[value * words + (row >>> 6)] |= 1L << (row & 63);
  }

  static boolean contains(byte[] haystack, byte[] needle) {
    outer:
    for (int start = 0; start + needle.length <= haystack.length; start++) {
      for (int i = 0; i < needle.length; i++) if (haystack[start + i] != needle[i]) continue outer;
      return true;
    }
    return false;
  }
}
