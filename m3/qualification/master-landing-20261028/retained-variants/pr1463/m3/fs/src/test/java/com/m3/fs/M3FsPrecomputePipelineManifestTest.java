// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsPrecomputePipelineManifestTest {
  @TempDir java.nio.file.Path temp;

  @Test
  void publishesReloadsAndMapsGpuGrepPipelineByDerivationIdentity() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put(
            "alpha\nbeta\ngamma".getBytes(StandardCharsets.UTF_8),
            M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search",
            "gpu-grep-segments",
            1,
            List.of(source.id()),
            Map.of("encoding", "utf8", "geometry", "offset-length"));

    byte[] packed = "alphabetagamma".getBytes(StandardCharsets.UTF_8);
    new M3FsPrecomputeWriter(store, key, "gpu-grep-segments-v1")
        .addGpuGrepSegments(
            packed,
            new long[] {0L, 5L, 9L, 14L},
            new int[] {5, 4, 5})
        .publish(cache);

    M3FsPrecomputeCatalog.PipelineManifest manifest =
        new M3FsPrecomputeCatalog.PipelineManifest(
            "gpu-grep-segments-v1",
            List.of(
                new M3FsPrecomputeCatalog.PipelineEntry(
                    "gpu-grep-segments-v1", key)));

    M3ObjectRef ref = manifest.publish(store);
    assertEquals(M3ObjectKind.PRECOMPUTE_PIPELINE_DESCRIPTOR, ref.kind());
    assertEquals(manifest.id(), ref.id());

    M3FsPrecomputeCatalog.PipelineManifest cold =
        M3FsPrecomputeCatalog.PipelineManifest.load(store, ref);
    assertArrayEquals(manifest.canonicalBytes(), cold.canonicalBytes());
    assertEquals(key.id(), cold.entries().getFirst().key().id());
    assertTrue(cold.runtimeOnlyState().contains("device-buffer"));
    assertTrue(cold.runtimeOnlyState().contains("kernel-handle"));

    M3PrecomputeCache.MappedBundleSet mapped =
        cold.openMapped(cache, 4096, 16384).orElseThrow();
    assertEquals(1, mapped.bundles().size());
    var segments =
        mapped.bundles().getFirst().bundle().openMapped(store, 4096, 16384)
            .gpuGrepSegments();
    byte[] actual = new byte[segments.bytes().remaining()];
    segments.bytes().get(actual);
    assertArrayEquals(packed, actual);
    assertEquals(3, segments.segmentCount());
  }

  @Test
  void canonicalPrecomputeKeyRoundTripsExactly() throws Exception {
    M3ContentId input = M3ContentId.digest(new byte[] {1, 2, 3});
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "graph",
            "csr-forward-reverse",
            7,
            List.of(input),
            Map.of("vertex-width", "i32", "edge-order", "stable"));

    M3PrecomputeKey decoded = M3PrecomputeKey.decode(key.canonicalBytes());

    assertArrayEquals(key.canonicalBytes(), decoded.canonicalBytes());
    assertEquals(key.id(), decoded.id());
    assertEquals(key.algorithm(), decoded.algorithm());
    assertEquals(key.configuration(), decoded.configuration());
  }

  @Test
  void pipelineRequiresDeclaredProfilesAndUniqueKeys() {
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search",
            "gpu-grep-segments",
            1,
            List.of(M3ContentId.digest(new byte[] {1})),
            Map.of());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3FsPrecomputeCatalog.PipelineManifest(
                "gpu-grep-segments-v1", List.of()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3FsPrecomputeCatalog.PipelineManifest(
                "gpu-grep-segments-v1",
                List.of(
                    new M3FsPrecomputeCatalog.PipelineEntry(
                        "posting-set-v1", key))));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3FsPrecomputeCatalog.PipelineManifest(
                "gpu-grep-segments-v1",
                List.of(
                    new M3FsPrecomputeCatalog.PipelineEntry(
                        "gpu-grep-segments-v1", key),
                    new M3FsPrecomputeCatalog.PipelineEntry(
                        "hash-signatures-v1", key))));
  }

  @Test
  void strictPipelineAdmissionRejectsUndeclaredSemanticLaneButLegacyRemainsReadable()
      throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("strict-store"));
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "search",
            "gpu-grep-segments",
            1,
            List.of(M3ContentId.digest(new byte[] {9})),
            Map.of());

    M3FsPrecomputeBundle bundle =
        new M3FsPrecomputeBundle(
            key.id(),
            "gpu-grep-segments-v1",
            List.of(
                lane(
                    store,
                    "candidate-bytes",
                    M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
                    new byte[] {1, 2, 3}),
                lane(
                    store,
                    "segment-offsets",
                    M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64,
                    longs(0L, 3L)),
                lane(
                    store,
                    "segment-lengths",
                    M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,
                    ints(3)),
                lane(
                    store,
                    "unrelated-automaton",
                    M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I32,
                    ints(0))));

    var profile = M3FsPrecomputeCatalog.byId("gpu-grep-segments-v1");
    profile.requireCompatible(bundle);
    assertThrows(
        IllegalArgumentException.class,
        () -> profile.requireStrictCompatible(bundle));
  }

  private static M3FsPrecomputeBundle.Lane lane(
      M3DirectoryContentStore store,
      String name,
      M3FsPrecomputeBundle.Role role,
      byte[] bytes)
      throws java.io.IOException {
    M3ObjectRef object = store.put(bytes, M3ObjectKind.PRECOMPUTE_OUTPUT);
    return new M3FsPrecomputeBundle.Lane(
        name, role, bytes.length / role.elementBytes(), object);
  }

  private static byte[] ints(int... values) {
    java.nio.ByteBuffer buffer =
        java.nio.ByteBuffer.allocate(values.length * Integer.BYTES)
            .order(java.nio.ByteOrder.BIG_ENDIAN);
    for (int value : values) buffer.putInt(value);
    return buffer.array();
  }

  private static byte[] longs(long... values) {
    java.nio.ByteBuffer buffer =
        java.nio.ByteBuffer.allocate(values.length * Long.BYTES)
            .order(java.nio.ByteOrder.BIG_ENDIAN);
    for (long value : values) buffer.putLong(value);
    return buffer.array();
  }
}
