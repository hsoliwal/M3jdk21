// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsGpuPipelineMappedSetTest {
  @TempDir Path temp;

  @Test
  void repeatedContentLaneIsMappedOnceAcrossBundles() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);

    M3PrecomputeKey left =
        new M3PrecomputeKey(
            "bitmap",
            "shared-lane",
            1,
            List.of(),
            Map.of("variant", "left"));
    M3PrecomputeKey right =
        new M3PrecomputeKey(
            "bitmap",
            "shared-lane",
            1,
            List.of(),
            Map.of("variant", "right"));
    long[] words = {0x55aa55aa55aa55aaL, 0x0102030405060708L};

    new M3FsPrecomputeWriter(store, left, "bitmap-set-v1")
        .addLongs("bits", M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64, words)
        .publish(cache);
    new M3FsPrecomputeWriter(store, right, "bitmap-set-v1")
        .addLongs("bits", M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64, words)
        .publish(cache);

    M3PrecomputeCache.MappedBundleSet mapped =
        cache
            .lookupMappedBundles(
                List.of(left, right),
                1024,
                1024)
            .orElseThrow();

    assertEquals(2, mapped.bundles().size());
    assertEquals((long) words.length * Long.BYTES, mapped.uniqueMappedBytes());
    assertEquals(2, mapped.bundles("bitmap-set-v1").size());
    assertSame(
        mapped.bundles().get(0).requireLane("bits").mapped(),
        mapped.bundles().get(1).requireLane("bits").mapped());
  }

  @Test
  void missingKeyMakesTheWholeMappedSetAbsent() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("missing"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3PrecomputeKey absent =
        new M3PrecomputeKey("bitmap", "absent", 1, List.of(), Map.of());

    assertTrue(
        cache
            .lookupMappedBundles(List.of(absent), 1024, 1024)
            .isEmpty());
  }

  @Test
  void duplicateKeyAndUniqueByteBudgetFailClosed() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("budget"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3PrecomputeKey key =
        new M3PrecomputeKey("bitmap", "budget", 1, List.of(), Map.of());
    new M3FsPrecomputeWriter(store, key, "bitmap-set-v1")
        .addLongs(
            "bits",
            M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64,
            new long[] {1L, 2L})
        .publish(cache);

    assertThrows(
        IllegalArgumentException.class,
        () -> cache.lookupMappedBundles(List.of(key, key), 1024, 1024));
    assertThrows(
        java.io.IOException.class,
        () -> cache.lookupMappedBundles(List.of(key), 1024, 8));
  }

  @Test
  void gpuGrepPipelineSeparatesDurableMathFromRuntimeState() {
    M3FsPrecomputeCatalog.Pipeline pipeline =
        M3FsPrecomputeCatalog.pipelineById("gpu-grep-v1");

    assertEquals(List.of("gpu-search-index-v1"), pipeline.requiredProfiles());
    assertTrue(pipeline.optionalProfiles().contains("search-selectivity-v1"));
    assertTrue(pipeline.optionalProfiles().contains("rank-select-bitmap-v1"));
    assertTrue(pipeline.runtimeOnlyState().contains("device-buffer"));
    assertTrue(pipeline.runtimeOnlyState().contains("kernel-handle"));
    assertTrue(pipeline.runtimeOnlyState().contains("candidate-output"));
    assertEquals(
        M3FsPrecomputeCatalog.Retention.KEEP,
        pipeline.required().getFirst().retention());
  }
}
