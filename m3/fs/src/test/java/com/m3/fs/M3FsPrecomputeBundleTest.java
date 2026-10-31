// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsPrecomputeBundleTest {
  @TempDir Path temp;

  @Test
  void publishesAndReloadsPortablePostingBundle() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put("source".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES);

    M3ObjectRef offsets =
        store.put(ints(0, 2, 4), M3ObjectKind.PRECOMPUTE_OUTPUT);
    M3ObjectRef values =
        store.put(ints(10, 11, 20, 21), M3ObjectKind.PRECOMPUTE_OUTPUT);
    M3ObjectRef bitmap =
        store.put(longs(0b1111L), M3ObjectKind.PRECOMPUTE_OUTPUT);

    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search-index",
            "posting-set",
            1,
            List.of(source.id()),
            Map.of("id-width", "i32"));

    M3FsPrecomputeBundle bundle =
        new M3FsPrecomputeBundle(
            key.id(),
            "posting-set-v1",
            List.of(
                new M3FsPrecomputeBundle.Lane(
                    "posting-offsets",
                    M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                    3,
                    offsets),
                new M3FsPrecomputeBundle.Lane(
                    "posting-values",
                    M3FsPrecomputeBundle.Role.POSTING_VALUES_I32,
                    4,
                    values),
                new M3FsPrecomputeBundle.Lane(
                    "candidate-bitmap",
                    M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64,
                    1,
                    bitmap)));

    M3FsPrecomputeCatalog.byId("posting-set-v1").requireCompatible(bundle);
    M3ObjectRef descriptor = cache.publishBundle(key, bundle);
    assertEquals(M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR, descriptor.kind());

    M3FsPrecomputeBundle loaded = cache.lookupBundle(key).orElseThrow();
    assertArrayEquals(bundle.canonicalBytes(), loaded.canonicalBytes());
    assertEquals(bundle.totalBytes(), loaded.totalBytes());
    assertEquals(values.id(), loaded.requireLane("posting-values").object().id());

    M3MappedObject mapped = store.openMapped(values, 1024);
    assertEquals(20, mapped.ints(0, 4, ByteOrder.BIG_ENDIAN).get(2));

    assertEquals(descriptor, cache.publishBundle(key, bundle));
  }

  @Test
  void streamingWriterPersistsCanonicalBigEndianLanes() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("writer-store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(new byte[] {9}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey("bounds", "sorted-keys", 1, List.of(source.id()), Map.of());

    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "sorted-bounds-v1")
            .addInts(
                "sorted-keys",
                M3FsPrecomputeBundle.Role.SORTED_KEYS_I32,
                new int[] {1, 7, 0x01020304})
            .addInts(
                "row-ids",
                M3FsPrecomputeBundle.Role.ROW_IDS_I32,
                new int[] {10, 11, 12});

    M3ObjectRef descriptor = writer.publish(cache);
    assertEquals(M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR, descriptor.kind());

    M3FsPrecomputeBundle loaded = cache.lookupBundle(key).orElseThrow();
    M3ObjectRef keys = loaded.requireLane("sorted-keys").object();
    M3MappedObject mapped = store.openMapped(keys, 1024);
    var values = mapped.ints(0, 3, ByteOrder.BIG_ENDIAN);
    assertEquals(1, values.get(0));
    assertEquals(7, values.get(1));
    assertEquals(0x01020304, values.get(2));
  }

  @Test
  void sameKeyCannotPublishConflictingBundle() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("conflict-store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(new byte[] {1}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey("graph", "csr", 1, List.of(source.id()), Map.of());

    M3FsPrecomputeBundle first =
        bundle(
            key,
            "csr-graph-v1",
            lane(
                store,
                "offsets",
                M3FsPrecomputeBundle.Role.CSR_OFFSETS_I32,
                ints(0, 1)),
            lane(
                store,
                "edges",
                M3FsPrecomputeBundle.Role.CSR_EDGES_I32,
                ints(0)));
    cache.publishBundle(key, first);

    M3FsPrecomputeBundle second =
        bundle(
            key,
            "csr-graph-v1",
            lane(
                store,
                "offsets",
                M3FsPrecomputeBundle.Role.CSR_OFFSETS_I32,
                ints(0, 1)),
            lane(
                store,
                "edges",
                M3FsPrecomputeBundle.Role.CSR_EDGES_I32,
                ints(1)));

    assertThrows(IOException.class, () -> cache.publishBundle(key, second));
  }

  @Test
  void catalogueMakesGpuGrepStylePersistenceExplicit() {
    var stream = M3FsPrecomputeCatalog.byId("candidate-stream-v1");
    assertEquals(M3FsPrecomputeCatalog.Retention.KEEP_IF_REUSED, stream.retention());
    assertTrue(
        stream.requiredRoles().contains(M3FsPrecomputeBundle.Role.CANDIDATE_BYTES));
    assertTrue(
        stream.requiredRoles().contains(M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32));

    var automaton = M3FsPrecomputeCatalog.byId("automaton-v1");
    assertTrue(
        automaton.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I32));
    assertTrue(
        automaton.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_ACCEPT_BITMAP_I64));

    assertTrue(
        M3FsPrecomputeCatalog.forOperation("CSR_FRONTIER_I32").stream()
            .anyMatch(profile -> profile.id().equals("csr-graph-v1")));
  }

  @Test
  void profileRejectsMissingRequiredLane() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("missing-store"));
    M3ObjectRef source = store.put(new byte[] {1}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey("search", "postings", 1, List.of(source.id()), Map.of());
    M3FsPrecomputeBundle incomplete =
        bundle(
            key,
            "posting-set-v1",
            lane(
                store,
                "posting-values",
                M3FsPrecomputeBundle.Role.POSTING_VALUES_I32,
                ints(1, 2)));

    assertThrows(
        IllegalArgumentException.class,
        () -> M3FsPrecomputeCatalog.byId("posting-set-v1").requireCompatible(incomplete));

    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    assertThrows(IllegalArgumentException.class, () -> cache.publishBundle(key, incomplete));
  }

  @Test
  void gpuSearchProfilesRetainOnlyPortableReusableFacts() throws Exception {
    var wide = M3FsPrecomputeCatalog.byId("candidate-stream-wide-v1");
    assertEquals(M3FsPrecomputeCatalog.Retention.KEEP_IF_REUSED, wide.retention());
    assertTrue(wide.requiredRoles().contains(M3FsPrecomputeBundle.Role.CANDIDATE_BYTES));
    assertTrue(wide.requiredRoles().contains(M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64));

    var rank = M3FsPrecomputeCatalog.byId("rank-select-bitmap-v1");
    assertTrue(rank.requiredRoles().contains(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64));
    assertTrue(rank.requiredRoles().contains(M3FsPrecomputeBundle.Role.RANK_SUPERBLOCKS_I32));
    assertTrue(rank.optionalRoles().contains(M3FsPrecomputeBundle.Role.SELECT_SAMPLES_I32));

    var graph = M3FsPrecomputeCatalog.byId("static-graph-v1");
    assertTrue(graph.requiredRoles().contains(M3FsPrecomputeBundle.Role.CSR_OFFSETS_I32));
    assertTrue(
        graph.requiredRoles().contains(M3FsPrecomputeBundle.Role.REVERSE_CSR_OFFSETS_I32));
    assertTrue(
        graph.requiredRoles().contains(M3FsPrecomputeBundle.Role.REVERSE_CSR_EDGES_I32));

    var join = M3FsPrecomputeCatalog.byId("join-index-v1");
    assertTrue(join.requiredRoles().contains(M3FsPrecomputeBundle.Role.SORTED_KEYS_I32));
    assertTrue(join.requiredRoles().contains(M3FsPrecomputeBundle.Role.GROUP_OFFSETS_I32));
    assertTrue(
        join.optionalRoles().contains(M3FsPrecomputeBundle.Role.RADIX_BUCKET_OFFSETS_I32));

    assertTrue(
        M3FsPrecomputeCatalog.forOperation("DIRECT_INNER_JOIN_I32_DISTINCT").stream()
            .anyMatch(profile -> profile.id().equals("join-index-v1")));
    assertTrue(
        M3FsPrecomputeCatalog.forOperation("BITMAP_XOR_I64").stream()
            .anyMatch(profile -> profile.id().equals("rank-select-bitmap-v1")));

    var posting = M3FsPrecomputeCatalog.byId("posting-index-v1");
    assertTrue(posting.requiredRoles().contains(M3FsPrecomputeBundle.Role.SORTED_KEYS_I32));
    assertTrue(
        posting.requiredRoles().contains(M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32));

    var intervals = M3FsPrecomputeCatalog.byId("interval-index-v1");
    assertTrue(intervals.requiredRoles().contains(M3FsPrecomputeBundle.Role.RANGE_STARTS_I32));
    assertTrue(intervals.requiredRoles().contains(M3FsPrecomputeBundle.Role.RANGE_ENDS_I32));

    var dominators = M3FsPrecomputeCatalog.byId("dominator-tree-v1");
    assertTrue(
        dominators.requiredRoles().contains(M3FsPrecomputeBundle.Role.DOMINATOR_PARENT_I32));
    assertTrue(
        dominators.requiredRoles().contains(M3FsPrecomputeBundle.Role.REACHABLE_VERTICES_I32));
  }

  @Test
  void wideRecordOffsetsPersistAsCanonicalBigEndianLongs() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("wide-stream-store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put("abcdef".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey("search", "candidate-stream-wide", 1, List.of(source.id()), Map.of());

    new M3FsPrecomputeWriter(store, key, "candidate-stream-wide-v1")
        .addBytes(
            "candidate-bytes",
            M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
            "abcdef".getBytes(StandardCharsets.UTF_8))
        .addLongs(
            "record-offsets",
            M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64,
            new long[] {0L, 3L, 6L})
        .publish(cache);

    M3FsPrecomputeBundle loaded = cache.lookupBundle(key).orElseThrow();
    M3ObjectRef offsets = loaded.requireLane("record-offsets").object();
    M3MappedObject mapped = store.openMapped(offsets, 1024);
    assertEquals(3L, mapped.longs(0, 3, ByteOrder.BIG_ENDIAN).get(1));
  }

  @Test
  void gpuGrepSegmentsPersistPackedBytesAndZeroCopyGeometry() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("gpu-grep-segments-store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put("alpha\nbeta\ngamma".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search",
            "gpu-grep-segments",
            1,
            List.of(source.id()),
            Map.of("encoding", "utf8", "geometry", "offset-length"));

    byte[] packed = "alphabetagamma".getBytes(StandardCharsets.UTF_8);
    long[] offsets = {0L, 5L, 9L, 14L};
    int[] lengths = {5, 4, 5};

    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "gpu-grep-segments-v1")
            .addGpuGrepSegments(packed, offsets, lengths);
    M3ObjectRef descriptor = writer.publish(cache);

    assertEquals(M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR, descriptor.kind());
    var profile = M3FsPrecomputeCatalog.byId("gpu-grep-segments-v1");
    assertEquals(M3FsPrecomputeCatalog.Retention.KEEP, profile.retention());
    assertTrue(
        profile.requiredRoles().contains(M3FsPrecomputeBundle.Role.CANDIDATE_BYTES));
    assertTrue(
        profile.requiredRoles().contains(M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64));
    assertTrue(
        profile.requiredRoles().contains(M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32));

    var pipeline = M3FsPrecomputeCatalog.pipelineById("gpu-grep-segments-v1");
    assertEquals(List.of("gpu-grep-segments-v1"), pipeline.requiredProfiles());
    assertTrue(pipeline.runtimeOnlyState().contains("device-buffer"));
    assertTrue(pipeline.runtimeOnlyState().contains("kernel-handle"));
    assertTrue(pipeline.runtimeOnlyState().contains("transfer-staging"));

    M3FsPrecomputeBundle loaded = cache.lookupBundle(key).orElseThrow();
    var mapped = loaded.openMapped(store, 1024, 4096).gpuGrepSegments();
    assertEquals(3, mapped.segmentCount());

    ByteBuffer bytes = mapped.bytes();
    byte[] actual = new byte[bytes.remaining()];
    bytes.get(actual);
    assertArrayEquals(packed, actual);

    LongBuffer starts = mapped.offsets();
    assertEquals(0L, starts.get(0));
    assertEquals(5L, starts.get(1));
    assertEquals(9L, starts.get(2));
    assertEquals(14L, starts.get(3));

    IntBuffer sizes = mapped.lengths();
    assertEquals(5, sizes.get(0));
    assertEquals(4, sizes.get(1));
    assertEquals(5, sizes.get(2));

    assertEquals(descriptor, writer.publish(cache));
  }

  @Test
  void gpuGrepSegmentsRejectInconsistentGeometryBeforePublication() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("gpu-grep-invalid-store"));
    M3ObjectRef source = store.put(new byte[] {1}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey("search", "gpu-grep-segments", 1, List.of(source.id()), Map.of());

    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "gpu-grep-segments-v1");
    assertThrows(
        IllegalArgumentException.class,
        () -> writer.addGpuGrepSegments(
            "abc".getBytes(StandardCharsets.UTF_8),
            new long[] {0L, 2L, 3L},
            new int[] {1, 1}));
  }

  private static M3FsPrecomputeBundle bundle(
      M3PrecomputeKey key, String profile, M3FsPrecomputeBundle.Lane... lanes) {
    return new M3FsPrecomputeBundle(key.id(), profile, List.of(lanes));
  }

  private static M3FsPrecomputeBundle.Lane lane(
      M3DirectoryContentStore store,
      String name,
      M3FsPrecomputeBundle.Role role,
      byte[] bytes)
      throws IOException {
    M3ObjectRef object = store.put(bytes, M3ObjectKind.PRECOMPUTE_OUTPUT);
    return new M3FsPrecomputeBundle.Lane(
        name, role, bytes.length / role.elementBytes(), object);
  }

  private static byte[] ints(int... values) {
    ByteBuffer buffer =
        ByteBuffer.allocate(values.length * Integer.BYTES).order(ByteOrder.BIG_ENDIAN);
    for (int value : values) buffer.putInt(value);
    return buffer.array();
  }

  private static byte[] longs(long... values) {
    ByteBuffer buffer =
        ByteBuffer.allocate(values.length * Long.BYTES).order(ByteOrder.BIG_ENDIAN);
    for (long value : values) buffer.putLong(value);
    return buffer.array();
  }
}
