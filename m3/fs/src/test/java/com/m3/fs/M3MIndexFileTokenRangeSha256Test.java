// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.function.IntToLongFunction;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3MIndexFileTokenRangeSha256Test {
  @TempDir Path temp;

  @Test
  void hashesExactMultibyteTokenRangesAfterOriginalSourceIsDeleted() throws Exception {
    IntToLongFunction resolver = resolver();
    Published published = publish("alpha β\nalpha", resolver);
    Files.delete(published.source());
    int beta = published.file().firstOccurrence(published.file().localHandle(7, "β"));
    long start = published.file().sourceFacts().utf8Offset(beta);
    long end = published.file().sourceFacts().utf8Offset(beta + 1);
    assertEquals(2L, end - start);
    M3FileTokenRangeSha256 result =
        published.store().sha256MIndexFileTokenRange(published.manifest(), resolver, beta, beta + 1);
    assertEquals(start, result.utf8Start());
    assertEquals(end, result.utf8End());
    assertEquals(M3ContentId.of(sha256("β".getBytes(StandardCharsets.UTF_8))), result.sha256());
    assertEquals(2L, result.byteCount());
    M3FileTokenRangeSha256 empty =
        published.store().sha256MIndexFileTokenRange(published.manifest(), resolver, beta, beta);
    assertEquals(0L, empty.byteCount());
    assertEquals(M3ContentId.of(sha256(new byte[0])), empty.sha256());
    M3FileTokenRangeSha256 atEnd = published.store().sha256MIndexFileTokenRange(
        published.manifest(), resolver, published.file().tokenCount(), published.file().tokenCount());
    assertEquals(published.raw().size(), atEnd.utf8Start());
    assertEquals(atEnd.utf8Start(), atEnd.utf8End());
    assertEquals(M3ContentId.of(sha256(new byte[0])), atEnd.sha256());
    assertThrows(IndexOutOfBoundsException.class,
        () -> published.store().sha256MIndexFileTokenRange(
            published.manifest(), resolver, -1, beta));
    assertThrows(IndexOutOfBoundsException.class,
        () -> published.store().sha256MIndexFileTokenRange(
            published.manifest(), resolver, 0, published.file().tokenCount() + 1));
  }

  @Test
  void batchPreservesOrderAndHashesOverlappingRangesAcrossStreamBuffer() throws Exception {
    IntToLongFunction resolver = resolver();
    Published published = publish("alpha ".repeat(12_000), resolver);
    byte[] raw = Files.readAllBytes(published.source());
    assertTrue(raw.length > 64 * 1024);
    int last = published.file().tokenCount();
    List<M3FileTokenRange> requests = List.of(
        new M3FileTokenRange(last / 2, last),
        new M3FileTokenRange(0, last),
        new M3FileTokenRange(last / 3, last / 3),
        new M3FileTokenRange(1, Math.min(100, last)));
    List<M3FileTokenRangeSha256> results = published.store()
        .sha256MIndexFileTokenRanges(published.manifest(), resolver, requests);
    assertEquals(requests.size(), results.size());
    for (int i = 0; i < requests.size(); i++) {
      M3FileTokenRange requested = requests.get(i);
      M3FileTokenRangeSha256 result = results.get(i);
      int from = Math.toIntExact(published.file().sourceFacts().utf8Offset(requested.startToken()));
      int to = Math.toIntExact(published.file().sourceFacts().utf8Offset(requested.endToken()));
      assertEquals(from, result.utf8Start());
      assertEquals(to, result.utf8End());
      assertArrayEquals(sha256(java.util.Arrays.copyOfRange(raw, from, to)), result.sha256().bytes());
    }
  }

  @Test
  void rejectsForeignManifestRawAndSidecarAndCorruptStoredRawBytes() throws Exception {
    IntToLongFunction resolver = resolver();
    Published published = publish("alpha β\nalpha", resolver);
    M3ObjectRef foreignRaw = published.store().putRaw("alpha γ\nalpha".getBytes(StandardCharsets.UTF_8));
    M3ObjectRef badRawManifest = published.store().createMIndexFileManifest(
        foreignRaw, published.semantic(), published.sidecar());
    assertThrows(IOException.class,
        () -> published.store().sha256MIndexFileTokenRange(badRawManifest, resolver, 0, 1));

    Path foreignSource = temp.resolve("foreign.txt");
    Files.writeString(foreignSource, "β alpha\nalpha", StandardCharsets.UTF_8);
    M3ColdFileFixture foreign = M3ColdFileFixture.fromSource(foreignSource);
    Path foreignFacts = temp.resolve("foreign.midxfacts");
    Files.write(foreignFacts, foreign.sourceFactsImage());
    M3ObjectRef badFacts = published.store().putMIndexFileSourceFactsImage(foreignFacts);
    M3ObjectRef badFactsManifest = published.store().createMIndexFileManifest(
        published.raw(), published.semantic(), badFacts);
    assertThrows(IOException.class,
        () -> published.store().sha256MIndexFileTokenRange(badFactsManifest, resolver, 0, 1));

    Path stored = published.store().objects().objectPath(published.raw().id());
    byte[] corrupt = Files.readAllBytes(stored);
    corrupt[0] ^= 1;
    Files.write(stored, corrupt);
    assertThrows(IOException.class,
        () -> published.store().sha256MIndexFileTokenRange(published.manifest(), resolver, 0, 1));
  }

  @Test
  void emptySourceUsesSha256OfEmptyBytes() throws Exception {
    IntToLongFunction resolver = resolver();
    Published published = publish("", resolver);
    M3FileTokenRangeSha256 result =
        published.store().sha256MIndexFileTokenRange(published.manifest(), resolver, 0, 0);
    assertEquals(0L, result.byteCount());
    assertEquals(M3ContentId.of(sha256(new byte[0])), result.sha256());
    assertEquals(List.of(),
        published.store().sha256MIndexFileTokenRanges(published.manifest(), resolver, List.of()));
  }

  @Test
  void rejectsForeignGenerationAndSemanticFingerprint() throws Exception {
    Published published = publish("alpha β", resolver());
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(
        published.manifest(), language -> M3ColdFileFixture.GENERATION + 1, 0, 1));
    byte[] semantic = published.file().semanticImage();
    semantic[40] ^= 1;
    M3ObjectRef image = published.store().objects().put(semantic, M3ObjectKind.MINDEX_FILE_IMAGE);
    M3ObjectRef manifest = published.store().createMIndexFileManifest(published.raw(), image, published.sidecar());
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(manifest, 0, 1));
  }

  @Test
  void rejectsInnerChecksumFailureAndTruncationDespiteValidOuterObjectIdentity() throws Exception {
    Published published = publish("alpha β", resolver());
    byte[] facts = published.file().sourceFactsImage();
    facts[facts.length - 1] ^= 1;
    M3ObjectRef invalid = withFacts(published, facts);
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(invalid, 0, 1));
    M3ObjectRef truncated = withFacts(published, java.util.Arrays.copyOf(facts, 35));
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(truncated, 0, 1));
  }

  @Test
  void rejectsRechecksummedShapeOffsetAndPowerViolations() throws Exception {
    Published published = publish("alpha β", resolver());
    byte[] facts = published.file().sourceFactsImage();
    ByteBuffer.wrap(facts).putInt(20, published.file().tokenCount() + 1);
    M3ObjectRef shape = withFacts(published, M3ColdFileFixture.resign(facts));
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(shape, 0, 1));
    facts = published.file().sourceFactsImage();
    ByteBuffer.wrap(facts).putLong(published.file().utf8LanePosition + 4, 1L);
    M3ObjectRef offsets = withFacts(published, M3ColdFileFixture.resign(facts));
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(offsets, 0, 1));
    facts = published.file().sourceFactsImage();
    facts[facts.length - 33] ^= 1;
    M3ObjectRef powers = withFacts(published, M3ColdFileFixture.resign(facts));
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(powers, 0, 1));
  }

  @Test
  void acceptsJavaUtf16MapOrderAndRejectsReversedSupplementaryKeys() throws Exception {
    Published published = publish("😀 β", resolver());
    assertEquals(4, published.store().sha256MIndexFileTokenRange(published.manifest(), 0, 1).byteCount());
    M3ObjectRef reversed = withFacts(published, published.file().sourceFactsImage(true));
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRange(reversed, 0, 1));
  }

  @Test
  void emptyRequestBatchStillVerifiesWholeRawContent() throws Exception {
    Published published = publish("alpha", resolver());
    byte[] raw = Files.readAllBytes(published.store().objects().objectPath(published.raw().id()));
    raw[raw.length - 1] ^= 1;
    Files.write(published.store().objects().objectPath(published.raw().id()), raw);
    assertThrows(IOException.class, () -> published.store().sha256MIndexFileTokenRanges(
        published.manifest(), new int[0], new int[0]));
  }

  @Test
  void primitiveBatchesAreBoundedAndResultsOwnTheirCoordinates() throws Exception {
    Published published = publish("abc", resolver());
    int[] starts = {2, 0, 1}, ends = {3, 3, 1};
    List<M3FileTokenRangeSha256> results = published.store().sha256MIndexFileTokenRanges(
        published.manifest(), starts, ends);
    starts[0] = 0; ends[0] = 0;
    assertEquals(2, results.getFirst().startToken());
    assertEquals(3, results.getFirst().endToken());
    assertArrayEquals(sha256(new byte[] {'c'}), results.getFirst().sha256().bytes());
    assertThrows(UnsupportedOperationException.class, results::clear);
    assertThrows(IllegalArgumentException.class, () -> published.store().sha256MIndexFileTokenRanges(
        published.manifest(), new int[65_537], new int[65_537]));
    assertThrows(IllegalArgumentException.class, () -> published.store().sha256MIndexFileTokenRanges(
        published.manifest(), new int[1], new int[0]));
    assertThrows(IndexOutOfBoundsException.class, () -> published.store().sha256MIndexFileTokenRanges(
        published.manifest(), new int[] {2}, new int[] {1}));
  }

  private M3ObjectRef withFacts(Published published, byte[] facts) throws IOException {
    M3ObjectRef ref = published.store().objects().put(facts, M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE);
    return published.store().createMIndexFileManifest(published.raw(), published.semantic(), ref);
  }

  private Published publish(String text, IntToLongFunction resolver) throws Exception {
    Path source = temp.resolve("source-" + System.nanoTime() + ".txt");
    Files.writeString(source, text, StandardCharsets.UTF_8);
    M3ColdFileFixture file = M3ColdFileFixture.fromSource(source);
    Path semanticImage = temp.resolve("semantic-" + System.nanoTime() + ".midxf");
    Path factsImage = temp.resolve("facts-" + System.nanoTime() + ".midxfacts");
    Files.write(semanticImage, file.semanticImage());
    Files.write(factsImage, file.sourceFactsImage());
    M3FileSystemStore store = new M3FileSystemStore(temp.resolve("m3-" + System.nanoTime()));
    M3ObjectRef raw = store.putRaw(source);
    M3ObjectRef semantic = store.putMIndexFileImage(semanticImage);
    M3ObjectRef sidecar = store.putMIndexFileSourceFactsImage(factsImage);
    M3ObjectRef manifest = store.createMIndexFileManifest(raw, semantic, sidecar);
    assertEquals(
        Map.of(
            M3FileSystemStore.MINDEX_SEMANTIC_STRUCTURE, semantic,
            M3FileSystemStore.MINDEX_SOURCE_FACTS_STRUCTURE, sidecar),
        store.readManifest(manifest).structures());
    return new Published(source, store, file, manifest, raw, semantic, sidecar);
  }

  private static IntToLongFunction resolver() {
    return language -> M3ColdFileFixture.GENERATION;
  }

  private static byte[] sha256(byte[] bytes) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(bytes);
  }

  private record Published(
      Path source,
      M3FileSystemStore store,
      M3ColdFileFixture file,
      M3ObjectRef manifest,
      M3ObjectRef raw,
      M3ObjectRef semantic,
      M3ObjectRef sidecar) {}
}
