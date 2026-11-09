// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Oracle of m3/fs/recipe/precompute-joined-lanes-20261106 stages 03-04 (Synexia crates m3fs-key-value-lanes-20261102
// and m3fs-hash-lane-cache-20261102): openRawSegments keeps its one-mapping-per-content-ID contract while its private
// table grows from 16 slots past 512; refusals keep first-occurrence order (budget before an oversized object and the
// reverse), and borrowed segment cursors stay independent. Relocated from M3HashLaneMappedPayloadTest without the
// Writer/Catalog profile and the joined-grep JNI call (see DECISIONS.tsv).
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsJoinedKeyValueLanesTest {
  @TempDir Path temp;

  @Test
  void growsPastFiveHundredDistinctIdsWithoutLosingAMapping() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    int distinct = 613;
    M3ObjectRef[] refs = new M3ObjectRef[distinct];
    byte[][] payloads = new byte[distinct][];
    long distinctBytes = 0;
    for (int p = 0; p < distinct; p++) {
      payloads[p] = ((p % 17 == 0 ? "prefix-needle-suffix-" : "prefix-others-") + p).getBytes(StandardCharsets.UTF_8);
      refs[p] = store.put(payloads[p], M3ObjectKind.RAW_BYTES);
      distinctBytes += refs[p].size();
    }
    int segments = 2 * distinct + 101;
    int[] payloadOf = new int[segments];
    int[] starts = new int[segments];
    int[] lengths = new int[segments];
    for (int i = 0; i < segments; i++) {
      payloadOf[i] = (int) ((i * 7919L) % distinct);
      starts[i] = i % 3;
      lengths[i] = payloads[payloadOf[i]].length - starts[i];
    }
    var directory = M3FsJoinedContentSegmentsTest.directory(refs, payloadOf, starts, lengths);
    var mapped = directory.openRawSegments(store, 1 << 20, 16L << 20);
    assertEquals(distinct, mapped.uniquePayloadCount());
    assertEquals(segments, mapped.segmentCount());
    assertEquals(distinctBytes, mapped.mappedPayloadBytes());
    for (int i = 0; i < segments; i++) {
      ByteBuffer segment = mapped.segment(i);
      byte[] actual = new byte[segment.remaining()];
      segment.get(actual);
      assertArrayEquals(Arrays.copyOfRange(payloads[payloadOf[i]], starts[i], starts[i] + lengths[i]), actual);
    }
    ByteBuffer borrow = mapped.segment(0);
    int unchanged = borrow.remaining();
    borrow.position(borrow.limit());
    assertEquals(unchanged, mapped.segment(0).remaining());
    assertEquals(distinctBytes, directory.openRawSegments(store, 1 << 20, distinctBytes).mappedPayloadBytes());
    long tooSmall = distinctBytes - 1;
    assertThrows(IOException.class, () -> directory.openRawSegments(store, 1 << 20, tooSmall));
  }

  @Test
  void refusalsKeepFirstOccurrenceOrder() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    int distinct = 80;
    M3ObjectRef[] refs = new M3ObjectRef[distinct];
    long[] prefix = new long[distinct + 1];
    for (int p = 0; p < distinct; p++) {
      int size = p == 60 ? 400 : 10;
      byte[] bytes = new byte[size];
      Arrays.fill(bytes, (byte) p);
      refs[p] = store.put(bytes, M3ObjectKind.RAW_BYTES);
      prefix[p + 1] = prefix[p] + size;
    }
    int segments = 3 * distinct;
    int[] payloadOf = new int[segments];
    for (int i = 0; i < segments; i++) payloadOf[i] = i < distinct ? i : (i * 31) % distinct;
    var directory = M3FsJoinedContentSegmentsTest.directory(refs, payloadOf, new int[segments], new int[segments]);
    // Budget runs out at distinct ID 50, before the oversized object at 60: the budget refusal wins.
    IOException budget = assertThrows(IOException.class, () -> directory.openRawSegments(store, 64, prefix[50]));
    assertEquals("joined segment payload mapping budget exceeded", budget.getMessage());
    // Budget lasts past 60: the object-size refusal of ID 60 wins.
    IOException size = assertThrows(IOException.class, () -> directory.openRawSegments(store, 64, prefix[distinct]));
    assertEquals(false, "joined segment payload mapping budget exceeded".equals(size.getMessage()));
    assertEquals(distinct, directory.openRawSegments(store, 400, prefix[distinct]).uniquePayloadCount());
  }
}
