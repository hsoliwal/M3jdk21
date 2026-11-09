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
class IndexPrecomputeDependencyImageTest {
  private static final String ROOT_A = "a".repeat(64);
  private static final String ROOT_B = "b".repeat(64);
  private static final String ROOT_A2 = "c".repeat(64);
  private static final String ROOT_B2 = "d".repeat(64);
  private static final String FACET_EXACT = "1".repeat(64);
  private static final String FACET_HASH = "2".repeat(64);

  @Test
  void sourceRootChangesInvalidateOnlyProducerAndTransitiveDependents() {
    IndexPrecomputeMultipassCompiler.Result result =
        compiler().compile(
            "generation-invalidation",
            List.of(
                new IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            candidates(),
            1_024,
            workload(),
            64,
            true);

    IndexPrecomputeDependencyImage image = result.dependencyImage();

    assertEquals(
        Set.of(),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));
    assertTrue(
        image.reusable(
            IndexPrecomputeCapability.EXACT_IDENTITY,
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));
    assertTrue(
        image.reusable(
            IndexPrecomputeCapability.JAVA_HASH,
            Map.of("source-a", ROOT_A, "source-b", ROOT_B)));

    assertEquals(
        Set.of(
            IndexPrecomputeCapability.EXACT_IDENTITY,
            IndexPrecomputeCapability.JAVA_HASH),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A2, "source-b", ROOT_B)));

    assertEquals(
        Set.of(IndexPrecomputeCapability.JAVA_HASH),
        image.invalidatedBySourceRoots(
            Map.of("source-a", ROOT_A, "source-b", ROOT_B2)));

    assertEquals(
        Set.of(
            IndexPrecomputeCapability.EXACT_IDENTITY,
            IndexPrecomputeCapability.JAVA_HASH),
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
    IndexPrecomputeMultipassCompiler.Result result =
        compiler().compile(
            "generation-replay",
            List.of(
                new IndexPrecomputeSource("source-a", ROOT_A, new SourceA()),
                new IndexPrecomputeSource("source-b", ROOT_B, new SourceB())),
            candidates(),
            1_024,
            workload(),
            64,
            true);

    IndexPrecomputeManifest replayed =
        IndexPrecomputeManifest.parseCanonical(result.manifest().canonicalText());
    IndexPrecomputeDependencyImage before = result.dependencyImage();
    IndexPrecomputeDependencyImage after =
        IndexPrecomputeDependencyImage.from(replayed);

    assertEquals(result.manifest().rootHash(), replayed.rootHash());
    assertEquals(before.rootHash(), after.rootHash());
    assertEquals(before.sourceRoots(), after.sourceRoots());
    assertEquals(
        Set.of(IndexPrecomputeCapability.EXACT_IDENTITY),
        after.dependencies(IndexPrecomputeCapability.JAVA_HASH));
    assertEquals(
        "source-a",
        after.sourceFor(IndexPrecomputeCapability.EXACT_IDENTITY).orElseThrow());
    assertEquals(
        "source-b",
        after.sourceFor(IndexPrecomputeCapability.JAVA_HASH).orElseThrow());
    assertTrue(after.retainedPrimitiveBytes() > 0L);
  }

  @Test
  void sourceAndProviderOrderDoNotChangeOwnershipImage() {
    IndexPrecomputeMultipassCompiler firstCompiler =
        compiler();
    IndexPrecomputeMultipassCompiler secondCompiler =
        new IndexPrecomputeMultipassCompiler(
            IndexPrecomputeProviders.of(
                new FakeProvider(
                    "alpha",
                    SourceA.class,
                    new FakeFacet(
                        IndexPrecomputeCapability.EXACT_IDENTITY,
                        FACET_EXACT,
                        32,
                        "storage-exact")),
                new FakeProvider(
                    "beta",
                    SourceB.class,
                    new FakeFacet(
                        IndexPrecomputeCapability.JAVA_HASH,
                        FACET_HASH,
                        48,
                        "storage-hash"))),
            IndexPrecomputeAccelerators.of(),
            new IndexPrecomputePlanner());

    IndexPrecomputeMultipassCompiler.Result first =
        firstCompiler.compile(
            "generation-order",
            List.of(
                new IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            candidates(),
            1_024,
            workload(),
            64,
            true);
    IndexPrecomputeMultipassCompiler.Result second =
        secondCompiler.compile(
            "generation-order",
            List.of(
                new IndexPrecomputeSource("source-a", ROOT_A, new SourceA()),
                new IndexPrecomputeSource("source-b", ROOT_B, new SourceB())),
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
    IndexPrecomputeMultipassCompiler compiler =
        new IndexPrecomputeMultipassCompiler(
            IndexPrecomputeProviders.of(
                new FakeProvider(
                    "alpha",
                    SourceA.class,
                    new FakeFacet(
                        IndexPrecomputeCapability.PROVENANCE,
                        FACET_EXACT,
                        32,
                        "storage-a")),
                new FakeProvider(
                    "beta",
                    SourceB.class,
                    new FakeFacet(
                        IndexPrecomputeCapability.PROVENANCE,
                        FACET_HASH,
                        48,
                        "storage-b"))),
            IndexPrecomputeAccelerators.of(),
            new IndexPrecomputePlanner());

    IndexPrecomputeMultipassCompiler.Result result =
        compiler.compile(
            "generation-multi-owner",
            List.of(
                new IndexPrecomputeSource("source-b", ROOT_B, new SourceB()),
                new IndexPrecomputeSource("source-a", ROOT_A, new SourceA())),
            List.of(
                new IndexPrecomputeCandidate(
                    IndexPrecomputeCapability.PROVENANCE,
                    80,
                    1,
                    100,
                    Set.of(),
                    true)),
            1_024,
            IndexPrecomputeWorkload.builder()
                .weight(IndexPrecomputeCapability.PROVENANCE, 100)
                .buildPenalty(1)
                .build(),
            64,
            true);

    assertEquals(80L, result.snapshot().retainedBytes());
    assertEquals(2, result.manifest().sourceCapabilityBindings().size());

    IndexPrecomputeDependencyImage image = result.dependencyImage();
    assertEquals(
        Set.of("source-a", "source-b"),
        image.sourcesFor(IndexPrecomputeCapability.PROVENANCE));
    assertTrue(image.sourceFor(IndexPrecomputeCapability.PROVENANCE).isEmpty());
    assertEquals(
        Set.of(IndexPrecomputeCapability.PROVENANCE),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A2, "source-b", ROOT_B)));
    assertEquals(
        Set.of(IndexPrecomputeCapability.PROVENANCE),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A, "source-b", ROOT_B2)));

    IndexPrecomputeManifest replayed =
        IndexPrecomputeManifest.parseCanonical(result.manifest().canonicalText());
    assertEquals(result.manifest().rootHash(), replayed.rootHash());
    assertEquals(image.rootHash(), IndexPrecomputeDependencyImage.from(replayed).rootHash());
  }

  @Test
  void legacyManifestWithoutOwnershipRemainsReadableButCannotInventOwners() {
    IndexPrecomputeManifest legacy =
        new IndexPrecomputeManifest(
            "legacy-generation",
            1_024,
            64,
            true,
            80,
            80,
            10,
            true,
            List.of(
                new IndexPrecomputeManifest.SourceDescriptor(
                    "source-a", ROOT_A, SourceA.class.getName())),
            List.of(
                IndexPrecomputeManifest.CandidateDescriptor.from(
                    new IndexPrecomputeCandidate(
                        IndexPrecomputeCapability.EXACT_IDENTITY,
                        40,
                        1,
                        100,
                        Set.of(),
                        true))),
            Map.of(IndexPrecomputeCapability.EXACT_IDENTITY, 100),
            1,
            List.of("alpha"),
            Set.of(IndexPrecomputeCapability.EXACT_IDENTITY),
            Map.of(IndexPrecomputeCapability.EXACT_IDENTITY, FACET_EXACT),
            Map.of());

    IndexPrecomputeDependencyImage image =
        IndexPrecomputeDependencyImage.from(legacy);
    assertFalse(image.sourceFor(IndexPrecomputeCapability.EXACT_IDENTITY).isPresent());
    assertEquals(
        Set.of(IndexPrecomputeCapability.EXACT_IDENTITY),
        image.invalidatedBySourceRoots(Map.of("source-a", ROOT_A2)));
    assertFalse(
        image.reusable(
            IndexPrecomputeCapability.EXACT_IDENTITY,
            Map.of("source-a", ROOT_A2)));
  }

  private static IndexPrecomputeMultipassCompiler compiler() {
    return new IndexPrecomputeMultipassCompiler(
        IndexPrecomputeProviders.of(
            new FakeProvider(
                "beta",
                SourceB.class,
                new FakeFacet(
                    IndexPrecomputeCapability.JAVA_HASH,
                    FACET_HASH,
                    48,
                    "storage-hash")),
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

  private static List<IndexPrecomputeCandidate> candidates() {
    return List.of(
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.EXACT_IDENTITY,
            40,
            1,
            100,
            Set.of(),
            true),
        new IndexPrecomputeCandidate(
            IndexPrecomputeCapability.JAVA_HASH,
            50,
            1,
            100,
            Set.of(IndexPrecomputeCapability.EXACT_IDENTITY),
            true));
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
      String storageKey)
      implements IndexPrecomputeFacet {}

  private record FakeProvider(
      String id,
      Class<?> sourceType,
      IndexPrecomputeFacet facet)
      implements IndexPrecomputeProvider {
    @Override
    public boolean supports(Object source) {
      return sourceType.isInstance(source);
    }

    @Override
    public void register(Object source, IndexPrecomputeSnapshotBuilder target) {
      if (!supports(source)) {
        throw new IllegalArgumentException("unsupported fake source");
      }
      target.add(id, facet);
    }
  }
}
