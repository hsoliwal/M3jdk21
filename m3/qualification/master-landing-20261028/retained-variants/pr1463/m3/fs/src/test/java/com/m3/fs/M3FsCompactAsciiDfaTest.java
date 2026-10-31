// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FsCompactAsciiDfaTest {
  @TempDir java.nio.file.Path temp;

  @Test
  void persistsCompactDfaWithoutDenseExpansion() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source = store.put(new byte[] {1, 2, 3}, M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "automaton",
            "compact-ascii-dfa",
            1,
            List.of(source.id()),
            Map.of("alphabet", "ascii", "transition-width", "i16"));

    byte[] classes = classes();
    short[] transitions = {0, 1, -1, 1, 1, -1};
    byte[] accepting = {0, 1};

    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "compact-ascii-dfa-v1")
            .addCompactAsciiDfa(classes, 3, transitions, accepting, 0);
    writer.publish(cache);

    M3FsPrecomputeBundle bundle = cache.lookupBundle(key).orElseThrow();
    assertEquals("compact-ascii-dfa-v1", bundle.profile());
    assertEquals(154L, bundle.totalBytes());
    assertTrue(bundle.totalBytes() < 2L * 128L * Short.BYTES + 2L);

    M3FsPrecomputeBundle.CompactAsciiDfa mapped =
        bundle.openMapped(store, 4096, 16384).compactAsciiDfa();
    assertEquals(0, mapped.startState());
    assertEquals(3, mapped.classCount());
    assertEquals(2, mapped.stateCount());
    assertEquals(1, mapped.nextState(0, 70));
    assertEquals(-1, mapped.nextState(0, 100));
    assertEquals(1, mapped.nextState(1, 10));

    var pipeline =
        new M3FsPrecomputeCatalog.PipelineManifest(
            "compact-ascii-dfa-v1",
            List.of(new M3FsPrecomputeCatalog.PipelineEntry("compact-ascii-dfa-v1", key)));
    M3ObjectRef pipelineRef = pipeline.publish(store);
    var cold = M3FsPrecomputeCatalog.PipelineManifest.load(store, pipelineRef);
    assertEquals(pipeline.id(), cold.id());
    assertTrue(cold.runtimeOnlyState().contains("prepared-handle"));
    assertTrue(cold.runtimeOnlyState().contains("device-transition-buffer"));
    assertEquals(1, cold.openMapped(cache, 4096, 16384).orElseThrow().bundles().size());
  }

  @Test
  void rejectsInvalidClassAndTransitionGeometryBeforePublication() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("invalid-store"));
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "automaton",
            "compact-ascii-dfa",
            1,
            List.of(M3ContentId.digest(new byte[] {9})),
            Map.of());
    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "compact-ascii-dfa-v1");

    byte[] unusedClass = new byte[128];
    assertThrows(
        IllegalArgumentException.class,
        () ->
            writer.addCompactAsciiDfa(
                unusedClass,
                2,
                new short[] {0, -1},
                new byte[] {0},
                0));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            writer.addCompactAsciiDfa(
                classes(),
                3,
                new short[] {0, 2, -1, 1, 1, -1},
                new byte[] {0, 1},
                0));
  }

  @Test
  void catalogKeepsMathAndMarksDeviceStateRuntimeOnly() {
    var profile = M3FsPrecomputeCatalog.byId("compact-ascii-dfa-v1");
    assertEquals(M3FsPrecomputeCatalog.Retention.KEEP, profile.retention());
    assertTrue(
        profile.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_ASCII_CLASS_MAP_U8));
    assertTrue(
        profile.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I16));
    assertTrue(
        profile.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_ACCEPT_U8));
    assertTrue(
        profile.requiredRoles().contains(
            M3FsPrecomputeBundle.Role.AUTOMATON_METADATA_I32));

    var pipeline = M3FsPrecomputeCatalog.pipelineById("compact-ascii-dfa-v1");
    assertTrue(pipeline.runtimeOnlyState().contains("prepared-handle"));
    assertTrue(pipeline.runtimeOnlyState().contains("device-class-map"));
    assertTrue(pipeline.runtimeOnlyState().contains("batch-workspace"));
  }

  private static byte[] classes() {
    byte[] result = new byte[128];
    for (int value = 0; value < result.length; value++) {
      result[value] = (byte) (value < 64 ? 0 : value < 96 ? 1 : 2);
    }
    return result;
  }
}
