// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsMappedBundleViewTest {
  @TempDir Path temp;

  @Test
  void gpuSearchBundleMapsAsTypedPortablePrimitiveLanes() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(new byte[] {1, 2, 3}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "gpu-search-index",
            "mapped-view",
            1,
            List.of(source.id()),
            Map.of("format", "canonical-big-endian-v1"));

    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "gpu-search-index-v1")
            .addShorts(
                "utf16-code-units",
                M3FsPrecomputeBundle.Role.UTF16_CODE_UNITS_I16,
                3,
                index -> "abc".charAt(index))
            .addInts(
                "record-offsets",
                M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32,
                new int[] {0, 3})
            .addInts(
                "record-lengths",
                M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,
                new int[] {3, 0})
            .addLongs(
                "ngram-ids",
                M3FsPrecomputeBundle.Role.NGRAM_IDS_I64,
                new long[] {0x010203040506L})
            .addInts(
                "posting-offsets",
                M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                new int[] {0, 1})
            .addInts(
                "posting-values",
                M3FsPrecomputeBundle.Role.POSTING_VALUES_I32,
                new int[] {0})
            .addInts(
                "lookup-displacements",
                M3FsPrecomputeBundle.Role.LOOKUP_DISPLACEMENTS_I32,
                new int[] {0})
            .addInts(
                "lookup-slots",
                M3FsPrecomputeBundle.Role.LOOKUP_SLOTS_I32,
                new int[] {1, 0});
    writer.publish(cache);

    M3FsPrecomputeBundle.MappedView view =
        cache.lookupMappedBundle(key, 4096, 64 * 1024L).orElseThrow();

    assertEquals("gpu-search-index-v1", view.profile());
    assertEquals(writer.bundle().totalBytes(), view.mappedBytes());
    assertEquals(
        'b',
        view.requireLane("utf16-code-units").shorts().get(1));
    assertEquals(
        3,
        view.requireLane("record-offsets").ints().get(1));
    assertEquals(
        0x010203040506L,
        view.requireLane("ngram-ids").longs().get(0));
    assertEquals(
        0,
        view.requireLane("posting-values").ints().get(0));
    assertTrue(view.requireLane("posting-values").bytes().isReadOnly());
  }

  @Test
  void mappingBudgetsFailBeforeUnboundedAdmission() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("budget-store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(new byte[] {9}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "posting-index", "budget", 1, List.of(source.id()), Map.of());

    new M3FsPrecomputeWriter(store, key, "posting-index-v1")
        .addInts(
            "sorted-keys",
            M3FsPrecomputeBundle.Role.SORTED_KEYS_I32,
            new int[] {1, 2})
        .addInts(
            "posting-offsets",
            M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
            new int[] {0, 1, 2})
        .addInts(
            "posting-values",
            M3FsPrecomputeBundle.Role.POSTING_VALUES_I32,
            new int[] {7, 9})
        .publish(cache);

    M3FsPrecomputeBundle bundle = cache.lookupBundle(key).orElseThrow();
    assertThrows(IOException.class, () -> bundle.openMapped(store, 4, 1024));
    assertThrows(IOException.class, () -> bundle.openMapped(store, 1024, 4));
    assertThrows(
        IllegalArgumentException.class,
        () -> bundle.openMapped(store, -1, 1024));
  }

  @Test
  void typedLaneViewRejectsWrongPrimitiveWidth() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("width-store"));
    M3ObjectRef lane =
        store.putLongLane(
            new long[] {1L, 2L}, M3DirectoryContentStore.LaneEncoding.INT64_BE);
    M3ContentId keyId = M3ContentId.digest(new byte[] {7});
    M3FsPrecomputeBundle bundle =
        new M3FsPrecomputeBundle(
            keyId,
            "hash-signatures-v1",
            List.of(
                new M3FsPrecomputeBundle.Lane(
                    "hashes",
                    M3FsPrecomputeBundle.Role.HASH64_I64,
                    2,
                    lane)));
    M3FsPrecomputeBundle.MappedLane mapped =
        bundle.openMapped(store, 1024, 1024).requireLane("hashes");

    assertEquals(2L, mapped.longs().get(1));
    assertThrows(IllegalStateException.class, mapped::ints);
  }
}
