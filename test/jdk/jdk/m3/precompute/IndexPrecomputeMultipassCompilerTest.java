// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/test/java/com/synexia/precompute/IndexPrecomputeMultipassCompilerTest.java blob c9bdf49ad132; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
class IndexPrecomputeMultipassCompilerTest {
  private static final String ROOT_A =
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String ROOT_B =
      "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
  private static final String FACET_EXACT =
      "1111111111111111111111111111111111111111111111111111111111111111";
  private static final String FACET_HASH =
      "2222222222222222222222222222222222222222222222222222222222222222";

  @TempDir Path temp;

  @Test
  void sourceAndProviderOrderDoNotChangeSealedBuildIdentity() throws Exception {
    IndexPrecomputeProvider beta = new FakeProvider(
        "beta",
        SourceB.class,
        new FakeFacet(IndexPrecomputeCapability.JAVA_HASH, FACET_HASH, 48, "storage-hash"));
    IndexPrecomputeProvider alpha = new FakeProvider(
        "alpha",
        SourceA.class,
        new FakeFacet(IndexPrecomputeCapability.EXACT_IDENTITY, FACET_EXACT, 32, "storage-exact"));

    IndexPrecomputeMultipassCompiler compiler =
        new IndexPrecomputeMultipassCompiler(
            IndexPrecomputeProviders.of(beta, alpha),
            IndexPrecomputeAccelerators.of(),
            new IndexPrecomputePlanner());

    List<IndexPrecomputeCandidate> candidates = List.of(
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.JAVA_HASH, 50, 1, 100, Set.of(), true),
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.EXACT_IDENTITY, 40, 1, 100, Set.of(), true));
    IndexPrecomputeWorkload workload = IndexPrecomputeWorkload.builder()
        .weight(IndexPrecomputeCapability.EXACT_IDENTITY, 100)
        .weight(IndexPrecomputeCapability.JAVA_HASH, 100)
        .buildPenalty(1)
        .build();

    var first = compiler.compile(
        "generation-42",
        List.of(
            new IndexPrecomputeSource("b", ROOT_B, new SourceB()),
            new IndexPrecomputeSource("a", ROOT_A, new SourceA())),
        candidates,
        256,
        workload,
        128,
        true);

    var second = compiler.compile(
        "generation-42",
        List.of(
            new IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new IndexPrecomputeSource("b", ROOT_B, new SourceB())),
        List.of(candidates.get(1), candidates.get(0)),
        256,
        workload,
        128,
        true);

    assertEquals(first.manifest().canonicalText(), second.manifest().canonicalText());
    assertEquals(first.manifest().rootHash(), second.manifest().rootHash());
    assertEquals(first.receipt().rootHash(), second.receipt().rootHash());
    assertEquals(List.of("alpha", "beta"), first.manifest().providerIds());
    assertEquals(90, first.manifest().recommendationRetainedBytes());
    assertEquals(80, first.manifest().actualRetainedBytes());
    assertEquals(
        Set.of(IndexPrecomputeCapability.EXACT_IDENTITY, IndexPrecomputeCapability.JAVA_HASH),
        first.plan().capabilities());

    IndexPrecomputeReceipt timestampedA = IndexPrecomputeReceipt.snapshot(
        first.snapshot(), IndexPrecomputeAccelerators.of(), 128, Instant.parse("2026-01-01T00:00:00Z"));
    IndexPrecomputeReceipt timestampedB = IndexPrecomputeReceipt.snapshot(
        first.snapshot(), IndexPrecomputeAccelerators.of(), 128, Instant.parse("2026-09-26T00:00:00Z"));
    assertNotEquals(timestampedA.createdAt(), timestampedB.createdAt());
    assertEquals(timestampedA.rootHash(), timestampedB.rootHash());
  }

  @Test
  void manifestImageIsCreateOnlyMappedAndReplayable() throws Exception {
    var result = compiler().compile(
        "mapped-generation",
        List.of(
            new IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new IndexPrecomputeSource("b", ROOT_B, new SourceB())),
        mandatoryCandidates(),
        512,
        workload(),
        64,
        true);

    Path image = temp.resolve("generation.sypm");
    IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image);
    assertThrows(
        java.nio.file.FileAlreadyExistsException.class,
        () -> IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image));

    IndexPrecomputeManifestImage.Mapped mapped = IndexPrecomputeManifestImage.map(image);
    assertEquals(result.manifest().rootHash(), mapped.rootHash());
    assertEquals(result.manifest().canonicalText(), mapped.manifest().canonicalText());
    assertEquals(result.plan(), mapped.plan());
    assertTrue(mapped.payloadView().isReadOnly());
    assertTrue(mapped.mappedBytes() > mapped.payloadView().remaining());
  }

  @Test
  void checksumCorruptionIsRejected() throws Exception {
    var result = compiler().compile(
        "corrupt-generation",
        List.of(
            new IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new IndexPrecomputeSource("b", ROOT_B, new SourceB())),
        mandatoryCandidates(),
        512,
        workload(),
        64,
        true);

    Path image = temp.resolve("corrupt.sypm");
    IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image);
    byte[] bytes = Files.readAllBytes(image);
    bytes[48] ^= 0x01;
    Files.write(image, bytes);

    assertThrows(java.io.IOException.class, () -> IndexPrecomputeManifestImage.map(image));
  }

  @Test
  void residueAndIncompleteCapabilityProductionFailClosed() {
    IndexPrecomputeMultipassCompiler compiler = compiler();

    assertThrows(
        IllegalStateException.class,
        () -> compiler.compile(
            "unknown-source",
            List.of(new IndexPrecomputeSource("unknown", ROOT_A, new Object())),
            mandatoryCandidates(),
            512,
            workload(),
            64,
            true));

    List<IndexPrecomputeCandidate> missing = List.of(
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.HASH64, 1, 1, 100, Set.of(), true));
    assertThrows(
        IllegalStateException.class,
        () -> compiler.compile(
            "missing-capability",
            List.of(
                new IndexPrecomputeSource("a", ROOT_A, new SourceA()),
                new IndexPrecomputeSource("b", ROOT_B, new SourceB())),
            missing,
            512,
            IndexPrecomputeWorkload.builder()
                .weight(IndexPrecomputeCapability.HASH64, 100)
                .build(),
            64,
            true));
  }

  @Test
  void duplicateSourceIdsFailBeforeProviderMutation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> compiler().compile(
            "duplicate",
            List.of(
                new IndexPrecomputeSource("same", ROOT_A, new SourceA()),
                new IndexPrecomputeSource("same", ROOT_B, new SourceB())),
            mandatoryCandidates(),
            512,
            workload(),
            64,
            true));
  }

  private static IndexPrecomputeMultipassCompiler compiler() {
    return new IndexPrecomputeMultipassCompiler(
        IndexPrecomputeProviders.of(
            new FakeProvider(
                "beta",
                SourceB.class,
                new FakeFacet(IndexPrecomputeCapability.JAVA_HASH, FACET_HASH, 48, "storage-hash")),
            new FakeProvider(
                "alpha",
                SourceA.class,
                new FakeFacet(
                    IndexPrecomputeCapability.EXACT_IDENTITY,
                    FACET_EXACT,
                    32,
                    "storage-exact"))),
        IndexPrecomputeAccelerators.of(),
        new IndexPrecomputePlanner());
  }

  private static List<IndexPrecomputeCandidate> mandatoryCandidates() {
    return List.of(
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.EXACT_IDENTITY, 40, 1, 100, Set.of(), true),
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.JAVA_HASH, 50, 1, 100, Set.of(), true));
  }

  private static IndexPrecomputeWorkload workload() {
    return IndexPrecomputeWorkload.builder()
        .weight(IndexPrecomputeCapability.EXACT_IDENTITY, 100)
        .weight(IndexPrecomputeCapability.JAVA_HASH, 100)
        .buildPenalty(1)
        .build();
  }

  private static final class SourceA {}
  private static final class SourceB {}

  private record FakeFacet(
      IndexPrecomputeCapability capability,
      String rootHash,
      long retainedBytes,
      String storageKey) implements IndexPrecomputeFacet {}

  private record FakeProvider(
      String id,
      Class<?> sourceType,
      IndexPrecomputeFacet facet) implements IndexPrecomputeProvider {
    @Override public boolean supports(Object source) {
      return sourceType.isInstance(source);
    }

    @Override public void register(Object source, IndexPrecomputeSnapshotBuilder target) {
      if (!supports(source)) throw new IllegalArgumentException("unsupported fake source");
      target.add(facet);
    }
  }
}
