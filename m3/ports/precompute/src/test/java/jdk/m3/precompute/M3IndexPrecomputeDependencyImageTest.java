// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/test/java/com/synexia/precompute/IndexPrecomputeDependencyImageTest.java blob d2a3739a73b6; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
class M3IndexPrecomputeDependencyImageTest {
  private static final String ROOT_A = "a".repeat(64);
  private static final String ROOT_B = "b".repeat(64);
  private static final String ROOT_A2 = "c".repeat(64);
  private static final String ROOT_B2 = "d".repeat(64);
  private static final String FACET_EXACT = "1".repeat(64);
  private static final String FACET_HASH = "2".repeat(64);

  @Test
  void sourceRootChangesInvalidateOnlyProducerAndTransitiveDependents() {
    M3IndexPrecomputeMultipassCompiler.Result result =
        compiler().compile(
            "generation-invalidation",
            List.of(
                new M3IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new M3IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            candidates(),
            1_024,
            workload(),
            64,
            true);

    M3IndexPrecomputeDependencyImage image = result.dependencyImage();

    assertEquals(
        Set.of(),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));
    assertTrue(
        image.reusable(
            M3IndexPrecomputeCapability.EXACT_IDENTITY,
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));
    assertTrue(
        image.reusable(
            M3IndexPrecomputeCapability.JAVA_HASH,
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));

    assertEquals(
        Set.of(
            M3IndexPrecomputeCapability.EXACT_IDENTITY,
            M3IndexPrecomputeCapability.JAVA_HASH),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A2, "source-b", ROOT_B)));

    assertEquals(
        Set.of(M3IndexPrecomputeCapability.JAVA_HASH),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A, "source-b", ROOT_B2)));

    assertEquals(
        Set.of(
            M3IndexPrecomputeCapability.EXACT_IDENTITY,
            M3IndexPrecomputeCapability.JAVA_HASH),
        image.invalidatedBySourceRoots(Map.of("source-b", ROOT_B)));

    assertEquals(
        Set.of(),
        image.invalidatedBySourceRoots(
            Map.of(
                "source-a", ROOT_A,
                "source-b", ROOT_B,
                "unrelated-new-source", "e".repeat(64))));
  }

  @Test
  void manifestReplayRetainsProducerOwnershipAndDependencyIdentity() {
    M3IndexPrecomputeMultipassCompiler.Result result =
        compiler().compile(
            "generation-replay",
            List.of(
                new M3IndexPrecomputeSource("source-a", ROOT_A, new SourceA()),
                new M3IndexPrecomputeSource("source-b", ROOT_B, new SourceB())),
            candidates(),
            1_024,
            workload(),
            64,
            true);

    M3IndexPrecomputeManifest replayed =
        M3IndexPrecomputeManifest.parseCanonical(result.manifest().canonicalText());
    M3IndexPrecomputeDependencyImage before = result.dependencyImage();
    M3IndexPrecomputeDependencyImage after =
        M3IndexPrecomputeDependencyImage.from(replayed);

    assertEquals(result.manifest().rootHash(), replayed.rootHash());
    assertEquals(before.rootHash(), after.rootHash());
    assertEquals(before.sourceRoots(), after.sourceRoots());
    assertEquals(
        Set.of(M3IndexPrecomputeCapability.EXACT_IDENTITY),
        after.dependencies(M3IndexPrecomputeCapability.JAVA_HASH));
    assertEquals(
        "source-a",
        after.sourceFor(M3IndexPrecomputeCapability.EXACT_IDENTITY).orElseThrow());
    assertEquals(
        "source-b",
        after.sourceFor(M3IndexPrecomputeCapability.JAVA_HASH).orElseThrow());
    assertTrue(after.retainedPrimitiveBytes() > 0L);
  }

  @Test
  void sourceAndProviderOrderDoNotChangeOwnershipImage() {
    M3IndexPrecomputeMultipassCompiler firstCompiler =
        compiler();
    M3IndexPrecomputeMultipassCompiler secondCompiler =
        new M3IndexPrecomputeMultipassCompiler(
            M3IndexPrecomputeProviders.of(
                new FakeProvider(
                    "alpha",
                    SourceA.class,
                    new FakeFacet(
                        M3IndexPrecomputeCapability.EXACT_IDENTITY,
                        FACET_EXACT,
                        32,
                        "storage-exact")),
                new FakeProvider(
                    "beta",
                    SourceB.class,
                    new FakeFacet(
                        M3IndexPrecomputeCapability.JAVA_HASH,
                        FACET_HASH,
                        48,
                        "storage-hash"))),
            M3IndexPrecomputeAccelerators.of(),
            new M3IndexPrecomputePlanner());

    M3IndexPrecomputeMultipassCompiler.Result first =
        firstCompiler.compile(
            "generation-order",
            List.of(
                new M3IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new M3IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            candidates(),
            1_024,
            workload(),
            64,
            true);
    M3IndexPrecomputeMultipassCompiler.Result second =
        secondCompiler.compile(
            "generation-order",
            List.of(
                new M3IndexPrecomputeSource("source-a", ROOT_A, new SourceA()),
                new M3IndexPrecomputeSource("source-b", ROOT_B, new SourceB())),
            List.of(candidates().get(1), candidates().get(0)),
            1_024,
            workload(),
            64,
            true);

    assertEquals(first.manifest().canonicalText(), second.manifest().canonicalText());
    assertEquals(first.manifest().rootHash(), second.manifest().rootHash());
    assertEquals(first.dependencyImage().rootHash(), second.dependencyImage().rootHash());
  }

  @Test
  void oneCapabilityCanHaveMultipleSourceOwnersAndEitherSourceInvalidatesIt() {
    M3IndexPrecomputeMultipassCompiler compiler =
        new M3IndexPrecomputeMultipassCompiler(
            M3IndexPrecomputeProviders.of(
                new FakeProvider(
                    "alpha",
                    SourceA.class,
                    new FakeFacet(
                        M3IndexPrecomputeCapability.PROVENANCE,
                        FACET_EXACT,
                        32,
                        "storage-a")),
                new FakeProvider(
                    "beta",
                    SourceB.class,
                    new FakeFacet(
                        M3IndexPrecomputeCapability.PROVENANCE,
                        FACET_HASH,
                        48,
                        "storage-b"))),
            M3IndexPrecomputeAccelerators.of(),
            new M3IndexPrecomputePlanner());

    M3IndexPrecomputeMultipassCompiler.Result result =
        compiler.compile(
            "generation-multi-owner",
            List.of(
                new M3IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new M3IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            List.of(
                new M3IndexPrecomputeCandidate(
                    M3IndexPrecomputeCapability.PROVENANCE,
                    80,
                    1,
                    100,
                    Set.of(),
                    true)),
            1_024,
            M3IndexPrecomputeWorkload.builder()
                .weight(M3IndexPrecomputeCapability.PROVENANCE, 100)
                .buildPenalty(1)
                .build(),
            64,
            true);

    assertEquals(80L, result.snapshot().retainedBytes());
    assertEquals(2, result.manifest().sourceCapabilityBindings().size());

    M3IndexPrecomputeDependencyImage image = result.dependencyImage();
    assertEquals(
        Set.of("source-a", "source-b"),
        image.sourcesFor(M3IndexPrecomputeCapability.PROVENANCE));
    assertTrue(image.sourceFor(M3IndexPrecomputeCapability.PROVENANCE).isEmpty());
    assertEquals(
        Set.of(M3IndexPrecomputeCapability.PROVENANCE),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A2, "source-b", ROOT_B)));
    assertEquals(
        Set.of(M3IndexPrecomputeCapability.PROVENANCE),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A, "source-b", ROOT_B2)));

    M3IndexPrecomputeManifest replayed =
        M3IndexPrecomputeManifest.parseCanonical(result.manifest().canonicalText());
    assertEquals(result.manifest().rootHash(), replayed.rootHash());
    assertEquals(image.rootHash(), M3IndexPrecomputeDependencyImage.from(replayed).rootHash());
  }

  @Test
  void legacyManifestWithoutOwnershipRemainsReadableButCannotInventOwners() {
    M3IndexPrecomputeManifest legacy =
        new M3IndexPrecomputeManifest(
            "legacy-generation",
            1_024,
            64,
            true,
            80,
            80,
            10,
            true,
            List.of(
                new M3IndexPrecomputeManifest.SourceDescriptor(
                    "source-a", ROOT_A, SourceA.class.getName())),
            List.of(
                M3IndexPrecomputeManifest.CandidateDescriptor.from(
                    new M3IndexPrecomputeCandidate(
                        M3IndexPrecomputeCapability.EXACT_IDENTITY,
                        40,
                        1,
                        100,
                        Set.of(),
                        true))),
            Map.of(M3IndexPrecomputeCapability.EXACT_IDENTITY, 100),
            1,
            List.of("alpha"),
            Set.of(M3IndexPrecomputeCapability.EXACT_IDENTITY),
            Map.of(M3IndexPrecomputeCapability.EXACT_IDENTITY, FACET_EXACT),
            Map.of());

    M3IndexPrecomputeDependencyImage image =
        M3IndexPrecomputeDependencyImage.from(legacy);
    assertFalse(image.sourceFor(M3IndexPrecomputeCapability.EXACT_IDENTITY).isPresent());
    assertEquals(
        Set.of(M3IndexPrecomputeCapability.EXACT_IDENTITY),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A2)));
    assertFalse(
        image.reusable(
            M3IndexPrecomputeCapability.EXACT_IDENTITY,
            Map.of("source-a", ROOT_A2)));
  }

  private static M3IndexPrecomputeMultipassCompiler compiler() {
    return new M3IndexPrecomputeMultipassCompiler(
        M3IndexPrecomputeProviders.of(
            new FakeProvider(
                "beta",
                SourceB.class,
                new FakeFacet(
                    M3IndexPrecomputeCapability.JAVA_HASH,
                    FACET_HASH,
                    48,
                    "storage-hash")),
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

  private static List<M3IndexPrecomputeCandidate> candidates() {
    return List.of(
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.EXACT_IDENTITY,
            40,
            1,
            100,
            Set.of(),
            true),
        new M3IndexPrecomputeCandidate(
            M3IndexPrecomputeCapability.JAVA_HASH,
            50,
            1,
            100,
            Set.of(M3IndexPrecomputeCapability.EXACT_IDENTITY),
            true));
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
      String storageKey)
      implements M3IndexPrecomputeFacet {}

  private record FakeProvider(
      String id,
      Class<?> sourceType,
      M3IndexPrecomputeFacet facet)
      implements M3IndexPrecomputeProvider {
    @Override
    public boolean supports(Object source) {
      return sourceType.isInstance(source);
    }

    @Override
    public void register(Object source, M3IndexPrecomputeSnapshotBuilder target) {
      if (!supports(source)) {
        throw new IllegalArgumentException("unsupported fake source");
      }
      target.add(id, facet);
    }
  }
}
