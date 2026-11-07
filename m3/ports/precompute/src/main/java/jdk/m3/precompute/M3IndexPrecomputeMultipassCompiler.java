// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic multi-pass compiler:
 *
 * <ol>
 *   <li>content-addressed source inventory,
 *   <li>budget/workload recommendation,
 *   <li>capability-plan construction,
 *   <li>provider registration in deterministic order,
 *   <li>immutable snapshot sealing,
 *   <li>accelerator selection,
 *   <li>manifest/receipt production.
 * </ol>
 *
 * <p>The compiler is intentionally fail-closed. A source with no registered provider, a duplicate
 * source ID, or a selected capability that was not actually produced aborts the build rather than
 * silently dropping residue.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeMultipassCompiler.java blob 913a9db8ad59; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputeMultipassCompiler {
  private final M3IndexPrecomputeProviders providers;
  private final M3IndexPrecomputeAccelerators accelerators;
  private final M3IndexPrecomputePlanner planner;

  public M3IndexPrecomputeMultipassCompiler(
      M3IndexPrecomputeProviders providers,
      M3IndexPrecomputeAccelerators accelerators,
      M3IndexPrecomputePlanner planner) {
    this.providers = Objects.requireNonNull(providers, "providers");
    this.accelerators = Objects.requireNonNull(accelerators, "accelerators");
    this.planner = Objects.requireNonNull(planner, "planner");
  }

  public static M3IndexPrecomputeMultipassCompiler system() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return new M3IndexPrecomputeMultipassCompiler(
        M3IndexPrecomputeProviders.load(loader),
        M3IndexPrecomputeAccelerators.load(loader),
        new M3IndexPrecomputePlanner());
  }

  public Result compile(
      String generationId,
      List<M3IndexPrecomputeSource> sources,
      List<M3IndexPrecomputeCandidate> candidates,
      long byteBudget,
      M3IndexPrecomputeWorkload workload,
      int nativeBatchThreshold,
      boolean mmapPreferred) {
    Objects.requireNonNull(generationId, "generationId");
    Objects.requireNonNull(sources, "sources");
    Objects.requireNonNull(candidates, "candidates");
    Objects.requireNonNull(workload, "workload");
    if (byteBudget < 0) throw new IllegalArgumentException("negative byteBudget");
    if (nativeBatchThreshold < 0) {
      throw new IllegalArgumentException("negative nativeBatchThreshold");
    }

    List<M3IndexPrecomputeSource> orderedSources = normalizeSources(sources);
    M3IndexPrecomputeRecommendation recommendation =
        planner.recommend(candidates, byteBudget, workload);
    M3IndexPrecomputePlan plan =
        recommendation.toPlan(byteBudget, nativeBatchThreshold, mmapPreferred);

    M3IndexPrecomputeSnapshotBuilder snapshotBuilder =
        new M3IndexPrecomputeSnapshotBuilder(generationId, plan);
    TreeSet<String> providerIds = new TreeSet<>();
    ArrayList<M3IndexPrecomputeManifest.SourceCapabilityBinding> sourceCapabilityBindings =
        new ArrayList<>();
    for (M3IndexPrecomputeSource source : orderedSources) {
      Map<M3IndexPrecomputeCapability, Set<String>> beforeOwners = snapshotBuilder.ownerIds();
      List<String> matched = providers.registerMatched(source.value(), snapshotBuilder);
      if (matched.isEmpty()) {
        throw new IllegalStateException(
            "no precompute provider accepted source "
                + source.id()
                + " ("
                + source.sourceType()
                + ")");
      }
      providerIds.addAll(matched);
      Map<M3IndexPrecomputeCapability, Set<String>> afterOwners = snapshotBuilder.ownerIds();
      for (M3IndexPrecomputeCapability capability : afterOwners.keySet()) {
        Set<String> before = beforeOwners.getOrDefault(capability, Set.of());
        Set<String> after = afterOwners.getOrDefault(capability, Set.of());
        if (!after.equals(before)) {
          sourceCapabilityBindings.add(
              new M3IndexPrecomputeManifest.SourceCapabilityBinding(
                  source.id(), capability));
        }
      }
    }

    M3IndexPrecomputeSnapshot snapshot = snapshotBuilder.build();
    if (!snapshot.capabilities().containsAll(recommendation.capabilities())) {
      java.util.EnumSet<M3IndexPrecomputeCapability> missing =
          java.util.EnumSet.copyOf(recommendation.capabilities());
      missing.removeAll(snapshot.capabilities());
      throw new IllegalStateException(
          "planner-selected capabilities were not produced by providers: " + missing);
    }

    int representativeBatchSize = Math.max(1, nativeBatchThreshold);
    M3IndexPrecomputeReceipt receipt =
        M3IndexPrecomputeReceipt.deterministic(
            snapshot, accelerators, representativeBatchSize);

    ArrayList<M3IndexPrecomputeManifest.SourceDescriptor> sourceDescriptors =
        new ArrayList<>(orderedSources.size());
    for (M3IndexPrecomputeSource source : orderedSources) {
      sourceDescriptors.add(new M3IndexPrecomputeManifest.SourceDescriptor(
          source.id(), source.rootHash(), source.sourceType()));
    }

    ArrayList<M3IndexPrecomputeManifest.CandidateDescriptor> candidateDescriptors =
        new ArrayList<>(candidates.size());
    for (M3IndexPrecomputeCandidate candidate : candidates) {
      candidateDescriptors.add(
          M3IndexPrecomputeManifest.CandidateDescriptor.from(
              Objects.requireNonNull(candidate, "candidate")));
    }

    M3IndexPrecomputeManifest manifest =
        new M3IndexPrecomputeManifest(
            generationId,
            byteBudget,
            nativeBatchThreshold,
            mmapPreferred,
            recommendation.retainedBytes(),
            snapshot.retainedBytes(),
            recommendation.score(),
            recommendation.exactSearch(),
            sourceDescriptors,
            sourceCapabilityBindings,
            candidateDescriptors,
            workload.weights(),
            workload.buildPenalty(),
            List.copyOf(providerIds),
            recommendation.capabilities(),
            receipt.rootHashes(),
            receipt.selectedAccelerators());

    return new Result(
        recommendation,
        plan,
        snapshot,
        receipt,
        manifest);
  }

  private static List<M3IndexPrecomputeSource> normalizeSources(
      List<M3IndexPrecomputeSource> sources) {
    ArrayList<M3IndexPrecomputeSource> ordered = new ArrayList<>(sources.size());
    for (M3IndexPrecomputeSource source : sources) {
      ordered.add(Objects.requireNonNull(source, "source"));
    }
    ordered.sort(
        Comparator.comparing(M3IndexPrecomputeSource::id)
            .thenComparing(M3IndexPrecomputeSource::rootHash)
            .thenComparing(M3IndexPrecomputeSource::sourceType));
    for (int i = 1; i < ordered.size(); i++) {
      if (ordered.get(i - 1).id().equals(ordered.get(i).id())) {
        throw new IllegalArgumentException(
            "duplicate precompute source id " + ordered.get(i).id());
      }
    }
    return List.copyOf(ordered);
  }

  public record Result(
      M3IndexPrecomputeRecommendation recommendation,
      M3IndexPrecomputePlan plan,
      M3IndexPrecomputeSnapshot snapshot,
      M3IndexPrecomputeReceipt receipt,
      M3IndexPrecomputeManifest manifest) {
    public Result {
      Objects.requireNonNull(recommendation, "recommendation");
      Objects.requireNonNull(plan, "plan");
      Objects.requireNonNull(snapshot, "snapshot");
      Objects.requireNonNull(receipt, "receipt");
      Objects.requireNonNull(manifest, "manifest");
      if (!snapshot.generationId().equals(manifest.generationId())) {
        throw new IllegalArgumentException("snapshot/manifest generation mismatch");
      }
      if (snapshot.retainedBytes() != manifest.actualRetainedBytes()) {
        throw new IllegalArgumentException("snapshot/manifest retained-byte mismatch");
      }
    }

    /** Frozen source/capability dependency image for incremental generation invalidation. */
    public M3IndexPrecomputeDependencyImage dependencyImage() {
      return M3IndexPrecomputeDependencyImage.from(manifest);
    }
  }
}
