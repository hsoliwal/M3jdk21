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
class M3IndexPrecomputeMultipassCompilerTest {
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
    M3IndexPrecomputeProvider beta = new FakeProvider(
        "beta",
        SourceB.class,
        new FakeFacet(M3IndexPrecomputeCapability.JAVA_HASH, FACET_HASH, 48, "storage-hash"));
    M3IndexPrecomputeProvider alpha = new FakeProvider(
        "alpha",
        SourceA.class,
        new FakeFacet(M3IndexPrecomputeCapability.EXACT_IDENTITY, FACET_EXACT, 32, "storage-exact"));

    M3IndexPrecomputeMultipassCompiler compiler =
        new M3IndexPrecomputeMultipassCompiler(
            M3IndexPrecomputeProviders.of(beta, alpha),
            M3IndexPrecomputeAccelerators.of(),
            new M3IndexPrecomputePlanner());

    List<M3IndexPrecomputeCandidate> candidates = List.of(
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.JAVA_HASH, 50, 1, 100, Set.of(), true),
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.EXACT_IDENTITY, 40, 1, 100, Set.of(), true));
    M3IndexPrecomputeWorkload workload = M3IndexPrecomputeWorkload.builder()
        .weight(M3IndexPrecomputeCapability.EXACT_IDENTITY, 100)
        .weight(M3IndexPrecomputeCapability.JAVA_HASH, 100)
        .buildPenalty(1)
        .build();

    var first = compiler.compile(
        "generation-42",
        List.of(
            new M3IndexPrecomputeSource("b", ROOT_B, new SourceB()),
            new M3IndexPrecomputeSource("a", ROOT_A, new SourceA())),
        candidates,
        256,
        workload,
        128,
        true);

    var second = compiler.compile(
        "generation-42",
        List.of(
            new M3IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new M3IndexPrecomputeSource("b", ROOT_B, new SourceB())),
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
        Set.of(M3IndexPrecomputeCapability.EXACT_IDENTITY, M3IndexPrecomputeCapability.JAVA_HASH),
        first.plan().capabilities());

    M3IndexPrecomputeReceipt timestampedA = M3IndexPrecomputeReceipt.snapshot(
        first.snapshot(), M3IndexPrecomputeAccelerators.of(), 128, Instant.parse("2026-01-01T00:00:00Z"));
    M3IndexPrecomputeReceipt timestampedB = M3IndexPrecomputeReceipt.snapshot(
        first.snapshot(), M3IndexPrecomputeAccelerators.of(), 128, Instant.parse("2026-09-26T00:00:00Z"));
    assertNotEquals(timestampedA.createdAt(), timestampedB.createdAt());
    assertEquals(timestampedA.rootHash(), timestampedB.rootHash());
  }

  @Test
  void manifestImageIsCreateOnlyMappedAndReplayable() throws Exception {
    var result = compiler().compile(
        "mapped-generation",
        List.of(
            new M3IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new M3IndexPrecomputeSource("b", ROOT_B, new SourceB())),
        mandatoryCandidates(),
        512,
        workload(),
        64,
        true);

    Path image = temp.resolve("generation.sypm");
    M3IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image);
    assertThrows(
        java.nio.file.FileAlreadyExistsException.class,
        () -> M3IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image));

    M3IndexPrecomputeManifestImage.Mapped mapped = M3IndexPrecomputeManifestImage.map(image);
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
            new M3IndexPrecomputeSource("a", ROOT_A, new SourceA()),
            new M3IndexPrecomputeSource("b", ROOT_B, new SourceB())),
        mandatoryCandidates(),
        512,
        workload(),
        64,
        true);

    Path image = temp.resolve("corrupt.sypm");
    M3IndexPrecomputeManifestImage.writeCreateOnly(result.manifest(), image);
    byte[] bytes = Files.readAllBytes(image);
    bytes[48] ^= 0x01;
    Files.write(image, bytes);

    assertThrows(java.io.IOException.class, () -> M3IndexPrecomputeManifestImage.map(image));
  }

  @Test
  void residueAndIncompleteCapabilityProductionFailClosed() {
    M3IndexPrecomputeMultipassCompiler compiler = compiler();

    assertThrows(
        IllegalStateException.class,
        () -> compiler.compile(
            "unknown-source",
            List.of(new M3IndexPrecomputeSource("unknown", ROOT_A, new Object())),
            mandatoryCandidates(),
            512,
            workload(),
            64,
            true));

    List<M3IndexPrecomputeCandidate> missing = List.of(
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.HASH64, 1, 1, 100, Set.of(), true));
    assertThrows(
        IllegalStateException.class,
        () -> compiler.compile(
            "missing-capability",
            List.of(
                new M3IndexPrecomputeSource("a", ROOT_A, new SourceA()),
                new M3IndexPrecomputeSource("b", ROOT_B, new SourceB())),
            missing,
            512,
            M3IndexPrecomputeWorkload.builder()
                .weight(M3IndexPrecomputeCapability.HASH64, 100)
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
                new M3IndexPrecomputeSource("same", ROOT_A, new SourceA()),
                new M3IndexPrecomputeSource("same", ROOT_B, new SourceB())),
            mandatoryCandidates(),
            512,
            workload(),
            64,
            true));
  }

  private static M3IndexPrecomputeMultipassCompiler compiler() {
    return new M3IndexPrecomputeMultipassCompiler(
        M3IndexPrecomputeProviders.of(
            new FakeProvider(
                "beta",
                SourceB.class,
                new FakeFacet(M3IndexPrecomputeCapability.JAVA_HASH, FACET_HASH, 48, "storage-hash")),
            new FakeProvider(
                "alpha",
                SourceA.class,
                new FakeFacet(
                    M3IndexPrecomputeCapability.EXACT_IDENTITY,
                    FACET_EXACT,
                    32,
                    "storage-exact"))),
        M3IndexPrecomputeAccelerators.of(),
        new M3IndexPrecomputePlanner());
  }

  private static List<M3IndexPrecomputeCandidate> mandatoryCandidates() {
    return List.of(
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.EXACT_IDENTITY, 40, 1, 100, Set.of(), true),
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.JAVA_HASH, 50, 1, 100, Set.of(), true));
  }

  private static M3IndexPrecomputeWorkload workload() {
    return M3IndexPrecomputeWorkload.builder()
        .weight(M3IndexPrecomputeCapability.EXACT_IDENTITY, 100)
        .weight(M3IndexPrecomputeCapability.JAVA_HASH, 100)
        .buildPenalty(1)
        .build();
  }

  private static final class SourceA {}
  private static final class SourceB {}

  private record FakeFacet(
      M3IndexPrecomputeCapability capability,
      String rootHash,
      long retainedBytes,
      String storageKey) implements M3IndexPrecomputeFacet {}

  private record FakeProvider(
      String id,
      Class<?> sourceType,
      M3IndexPrecomputeFacet facet) implements M3IndexPrecomputeProvider {
    @Override public boolean supports(Object source) {
      return sourceType.isInstance(source);
    }

    @Override public void register(Object source, M3IndexPrecomputeSnapshotBuilder target) {
      if (!supports(source)) throw new IllegalArgumentException("unsupported fake source");
      target.add(facet);
    }
  }
}
