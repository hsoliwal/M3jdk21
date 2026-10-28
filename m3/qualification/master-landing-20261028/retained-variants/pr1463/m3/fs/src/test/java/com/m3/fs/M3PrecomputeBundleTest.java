// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3PrecomputeBundleTest {
  @TempDir Path temp;

  @Test
  void bundleRoundTripsAndMapsExactPrimitiveLanes() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("store"));
    M3ObjectRef offsets =
        store.putIntLane(
            new int[] {0, 2, 3, 5},
            M3DirectoryContentStore.LaneEncoding.INT32_BE);
    M3ObjectRef rows =
        store.putIntLane(
            new int[] {1, 4, 2, 0, 3},
            M3DirectoryContentStore.LaneEncoding.INT32_BE);

    var bundle =
        new M3DirectoryContentStore.PrecomputeBundle(
            digest("source"),
            digest("config"),
            List.of(
                new M3DirectoryContentStore.PrecomputeLane(
                    M3DirectoryContentStore.PrecomputeRole.POSTING_ROWS,
                    M3DirectoryContentStore.LaneEncoding.INT32_BE,
                    5,
                    rows),
                new M3DirectoryContentStore.PrecomputeLane(
                    M3DirectoryContentStore.PrecomputeRole.POSTING_OFFSETS,
                    M3DirectoryContentStore.LaneEncoding.INT32_BE,
                    4,
                    offsets)));

    M3ObjectRef first = store.putPrecomputeBundle(bundle);
    M3ObjectRef second = store.putPrecomputeBundle(bundle);
    assertEquals(first, second);
    assertEquals(M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR, first.kind());

    var restored = store.readPrecomputeBundle(first, 32 * 1024);
    assertEquals(bundle, restored);
    assertEquals(
        M3DirectoryContentStore.PrecomputeRole.POSTING_OFFSETS,
        restored.lanes().getFirst().role());

    M3MappedObject mapped =
        store.openPrecomputeLane(
            restored,
            M3DirectoryContentStore.PrecomputeRole.POSTING_ROWS,
            1024);
    int[] actual = new int[5];
    mapped.ints(0, 5, ByteOrder.BIG_ENDIAN).get(actual);
    assertArrayEquals(new int[] {1, 4, 2, 0, 3}, actual);

    M3ObjectRef hashes =
        store.putLongLane(
            new long[] {Long.MIN_VALUE, 0L, -1L},
            M3DirectoryContentStore.LaneEncoding.INT64_LE);
    long[] hashValues = new long[3];
    store.openMapped(hashes, 1024)
        .longs(0, 3, ByteOrder.LITTLE_ENDIAN)
        .get(hashValues);
    assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, -1L}, hashValues);
  }

  @Test
  void rejectsDuplicateRolesWrongGeometryAndNonPrecomputeObjects()
      throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("validation"));
    M3ObjectRef fourBytes =
        store.put(new byte[Integer.BYTES], M3ObjectKind.PRECOMPUTE_OUTPUT);
    M3ObjectRef raw =
        store.put(new byte[Integer.BYTES], M3ObjectKind.RAW_BYTES);
    String root = M3ContentId.digest(new byte[] {1}).hex();

    var lane =
        new M3DirectoryContentStore.PrecomputeLane(
            M3DirectoryContentStore.PrecomputeRole.SORTED_KEYS,
            M3DirectoryContentStore.LaneEncoding.INT32_BE,
            1,
            fourBytes);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3DirectoryContentStore.PrecomputeBundle(
                root, root, List.of(lane, lane)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3DirectoryContentStore.PrecomputeLane(
                M3DirectoryContentStore.PrecomputeRole.HASH64,
                M3DirectoryContentStore.LaneEncoding.INT64_BE,
                1,
                fourBytes));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new M3DirectoryContentStore.PrecomputeLane(
                M3DirectoryContentStore.PrecomputeRole.SORTED_KEYS,
                M3DirectoryContentStore.LaneEncoding.INT32_BE,
                1,
                raw));
  }

  @Test
  void filesystemRolesNeverTreatDeviceResidencyAsCanonicalState() {
    assertFalse(
        Arrays.stream(M3DirectoryContentStore.PrecomputeRole.values())
            .map(Enum::name)
            .anyMatch(name -> name.contains("GPU") || name.contains("DEVICE")));
  }

  @Test
  void indexedSuppliersStreamWithoutMaterializingASecondLane() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("indexed"));
    M3ObjectRef ints =
        store.putIntLane(
            4,
            M3DirectoryContentStore.LaneEncoding.INT32_LE,
            index -> index * 17 - 3);
    M3ObjectRef longs =
        store.putLongLane(
            3,
            M3DirectoryContentStore.LaneEncoding.INT64_BE,
            index -> index == 2 ? -1L : 1L << index);

    int[] intValues = new int[4];
    store.openMapped(ints, 1024)
        .ints(0, 4, ByteOrder.LITTLE_ENDIAN)
        .get(intValues);
    assertArrayEquals(new int[] {-3, 14, 31, 48}, intValues);

    long[] longValues = new long[3];
    store.openMapped(longs, 1024)
        .longs(0, 3, ByteOrder.BIG_ENDIAN)
        .get(longValues);
    assertArrayEquals(new long[] {1L, 2L, -1L}, longValues);
  }

  @Test
  void utf16LanePreservesCodeUnitsAndEndian() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("utf16"));
    char[] units = {'A', '\u00e9', '\ud83d', '\ude00'};
    M3ObjectRef lane =
        store.putUtf16Lane(
            units.length,
            M3DirectoryContentStore.LaneEncoding.UTF16_LE,
            index -> units[index]);

    var mapped =
        store.openMapped(lane, 1024)
            .chars(0, units.length, ByteOrder.LITTLE_ENDIAN);
    char[] actual = new char[units.length];
    mapped.get(actual);
    assertArrayEquals(units, actual);
  }

  @Test
  void reportsMissingExecutionProfileLanes() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("profiles"));
    M3ObjectRef rows =
        store.putIntLane(
            new int[] {1, 2, 3},
            M3DirectoryContentStore.LaneEncoding.INT32_BE);
    String root = M3ContentId.digest(new byte[] {9}).hex();
    var partial =
        new M3DirectoryContentStore.PrecomputeBundle(
            root,
            root,
            List.of(
                new M3DirectoryContentStore.PrecomputeLane(
                    M3DirectoryContentStore.PrecomputeRole.POSTING_ROWS,
                    M3DirectoryContentStore.LaneEncoding.INT32_BE,
                    3,
                    rows)));

    assertFalse(
        partial.supports(
            M3DirectoryContentStore.PrecomputeProfile.TOKEN_POSTINGS));
    org.junit.jupiter.api.Assertions.assertTrue(
        partial
            .missing(M3DirectoryContentStore.PrecomputeProfile.TOKEN_POSTINGS)
            .contains(
                M3DirectoryContentStore.PrecomputeRole.SORTED_KEYS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            partial.requireProfile(
                M3DirectoryContentStore.PrecomputeProfile.TOKEN_POSTINGS));
  }

  private static String digest(String value) {
    return M3ContentId.digest(value.getBytes(StandardCharsets.UTF_8)).hex();
  }
}
