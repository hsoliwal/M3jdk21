// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FileSystemStoreHeadGenerationPrecomputeTest {
  @TempDir Path directory;

  @Test
  void reusesDecodedHeadByContentIdAndInvalidatesAfterPromotion() throws Exception {
    M3FileSystemStore store = new M3FileSystemStore(directory);
    M3ObjectRef firstManifest = manifest(store, "first");
    M3GenerationRoot firstRoot =
        M3GenerationRoot.empty().put("/a/file", firstManifest.id());
    M3ObjectRef firstGeneration = store.publishGeneration(firstRoot);
    store.promote(Optional.empty(), firstGeneration);

    M3GenerationRoot firstRead = store.headGeneration().orElseThrow();
    assertSame(firstRead, store.headGeneration().orElseThrow());
    assertEquals(firstManifest.id(), store.headManifestRef("/a/./file").orElseThrow().id());
    assertEquals(
        firstManifest.id(),
        store.headManifest("/a/file").orElseThrow().id());
    M3GenerationRoot.DirectoryPage rootPage =
        store.headDirectoryPage("/", null, 8).orElseThrow();
    assertEquals("a", rootPage.entries().getFirst().name());
    assertTrue(rootPage.entries().getFirst().isDirectory());

    M3ObjectRef secondManifest = manifest(store, "second");
    M3GenerationRoot secondRoot = firstRoot.put("/b", secondManifest.id());
    M3ObjectRef secondGeneration = store.publishGeneration(secondRoot);
    store.promote(Optional.of(firstGeneration.id()), secondGeneration);

    M3GenerationRoot secondRead = store.headGeneration().orElseThrow();
    assertNotSame(firstRead, secondRead);
    assertEquals(secondRoot.id(), secondRead.id());
    assertEquals(secondManifest.id(), store.headManifestRef("/b").orElseThrow().id());
  }

  @Test
  void restartUsesPersistedHeadAndEmptyStoreStaysEmpty() throws Exception {
    Path emptyDirectory = directory.resolve("empty");
    M3FileSystemStore empty = new M3FileSystemStore(emptyDirectory);
    assertTrue(empty.headGeneration().isEmpty());
    assertTrue(empty.headManifest("/missing").isEmpty());
    assertTrue(empty.headDirectoryPage("/", null, 4).isEmpty());

    Path persistedDirectory = directory.resolve("persisted");
    M3FileSystemStore writer = new M3FileSystemStore(persistedDirectory);
    M3ObjectRef manifest = manifest(writer, "payload");
    M3GenerationRoot root = M3GenerationRoot.empty().put("/x/y", manifest.id());
    M3ObjectRef generation = writer.publishGeneration(root);
    writer.promote(Optional.empty(), generation);

    M3FileSystemStore reopened = new M3FileSystemStore(persistedDirectory);
    assertEquals(root.id(), reopened.headGeneration().orElseThrow().id());
    assertEquals(manifest.id(), reopened.headManifestRef("/x/y").orElseThrow().id());
    assertEquals("x", reopened.headDirectoryPage("/", null, 4)
        .orElseThrow().entries().getFirst().name());
  }

  private static M3ObjectRef manifest(M3FileSystemStore store, String text) throws Exception {
    M3ObjectRef raw = store.putRaw(text.getBytes(StandardCharsets.UTF_8));
    return store.createFileManifest(raw, Map.of(), Map.of(), Map.of());
  }
}
