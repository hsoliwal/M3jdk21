// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical replay/audit manifest for the complete multipass precompute decision.
 *
 * <p>Every collection is canonicalized before hashing. The root therefore depends on source roots,
 * candidate costs/dependencies, workload weights, selected capabilities, facet roots, provider IDs,
 * accelerator choices and plan parameters, never source-object identity or insertion order.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeManifest.java blob 316fa02101aa; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class IndexPrecomputeManifest {
  public record SourceDescriptor(String id, String rootHash, String sourceType) {
    public SourceDescriptor {
      id = requireText(id, "source id");
      rootHash = IndexPrecomputeSource.requireSha256(rootHash, "source root");
      sourceType = requireText(sourceType, "source type");
    }
  }

  /** One selected capability and a content-addressed source that contributed an owner facet. */
  public record SourceCapabilityBinding(
      String sourceId, IndexPrecomputeCapability capability) {
    public SourceCapabilityBinding {
      sourceId = requireText(sourceId, "source capability id");
      Objects.requireNonNull(capability, "source capability");
    }
  }

  public record CandidateDescriptor(
      IndexPrecomputeCapability capability,
      long retainedBytes,
      long buildCostUnits,
      long expectedReuseCount,
      List<IndexPrecomputeCapability> dependencies,
      boolean mandatory) {
    public CandidateDescriptor {
      Objects.requireNonNull(capability, "capability");
      if (retainedBytes < 0 || buildCostUnits < 0 || expectedReuseCount < 0) {
        throw new IllegalArgumentException("negative candidate cost");
      }
      ArrayList<IndexPrecomputeCapability> ordered =
          new ArrayList<>(Objects.requireNonNull(dependencies, "dependencies"));
      ordered.sort(Comparator.comparingInt(Enum::ordinal));
      dependencies = List.copyOf(ordered);
    }

    static CandidateDescriptor from(IndexPrecomputeCandidate candidate) {
      return new CandidateDescriptor(
          candidate.capability(),
          candidate.retainedBytes(),
          candidate.buildCostUnits(),
          candidate.expectedReuseCount(),
          List.copyOf(candidate.dependencies()),
          candidate.mandatory());
    }
  }

  private final String generationId;
  private final long byteBudget;
  private final int nativeBatchThreshold;
  private final boolean mmapPreferred;
  private final long recommendationRetainedBytes;
  private final long actualRetainedBytes;
  private final long recommendationScore;
  private final boolean exactSearch;
  private final List<SourceDescriptor> sources;
  private final List<SourceCapabilityBinding> sourceCapabilityBindings;
  private final List<CandidateDescriptor> candidates;
  private final EnumMap<IndexPrecomputeCapability, Integer> workloadWeights;
  private final int buildPenalty;
  private final List<String> providerIds;
  private final List<IndexPrecomputeCapability> selectedCapabilities;
  private final EnumMap<IndexPrecomputeCapability, String> facetRoots;
  private final EnumMap<IndexPrecomputeKernel, String> selectedAccelerators;
  private final String canonicalText;
  private final String rootHash;

  public IndexPrecomputeManifest(
      String generationId,
      long byteBudget,
      int nativeBatchThreshold,
      boolean mmapPreferred,
      long recommendationRetainedBytes,
      long actualRetainedBytes,
      long recommendationScore,
      boolean exactSearch,
      List<SourceDescriptor> sources,
      List<CandidateDescriptor> candidates,
      Map<IndexPrecomputeCapability, Integer> workloadWeights,
      int buildPenalty,
      List<String> providerIds,
      Set<IndexPrecomputeCapability> selectedCapabilities,
      Map<IndexPrecomputeCapability, String> facetRoots,
      Map<IndexPrecomputeKernel, String> selectedAccelerators) {
    this(
        generationId,
        byteBudget,
        nativeBatchThreshold,
        mmapPreferred,
        recommendationRetainedBytes,
        actualRetainedBytes,
        recommendationScore,
        exactSearch,
        sources,
        List.of(),
        candidates,
        workloadWeights,
        buildPenalty,
        providerIds,
        selectedCapabilities,
        facetRoots,
        selectedAccelerators);
  }

  public IndexPrecomputeManifest(
      String generationId,
      long byteBudget,
      int nativeBatchThreshold,
      boolean mmapPreferred,
      long recommendationRetainedBytes,
      long actualRetainedBytes,
      long recommendationScore,
      boolean exactSearch,
      List<SourceDescriptor> sources,
      List<SourceCapabilityBinding> sourceCapabilityBindings,
      List<CandidateDescriptor> candidates,
      Map<IndexPrecomputeCapability, Integer> workloadWeights,
      int buildPenalty,
      List<String> providerIds,
      Set<IndexPrecomputeCapability> selectedCapabilities,
      Map<IndexPrecomputeCapability, String> facetRoots,
      Map<IndexPrecomputeKernel, String> selectedAccelerators) {
    this.generationId = requireText(generationId, "generationId");
    if (byteBudget < 0 || nativeBatchThreshold < 0 || recommendationRetainedBytes < 0
        || actualRetainedBytes < 0 || buildPenalty < 0) {
      throw new IllegalArgumentException("negative manifest scalar");
    }
    this.byteBudget = byteBudget;
    this.nativeBatchThreshold = nativeBatchThreshold;
    this.mmapPreferred = mmapPreferred;
    this.recommendationRetainedBytes = recommendationRetainedBytes;
    this.actualRetainedBytes = actualRetainedBytes;
    this.recommendationScore = recommendationScore;
    this.exactSearch = exactSearch;

    ArrayList<SourceDescriptor> sourceList =
        new ArrayList<>(Objects.requireNonNull(sources, "sources"));
    sourceList.sort(Comparator
        .comparing(SourceDescriptor::id)
        .thenComparing(SourceDescriptor::rootHash)
        .thenComparing(SourceDescriptor::sourceType));
    this.sources = List.copyOf(sourceList);

    ArrayList<CandidateDescriptor> candidateList =
        new ArrayList<>(Objects.requireNonNull(candidates, "candidates"));
    candidateList.sort(Comparator.comparingInt(value -> value.capability().ordinal()));
    this.candidates = List.copyOf(candidateList);

    this.workloadWeights = new EnumMap<>(IndexPrecomputeCapability.class);
    Objects.requireNonNull(workloadWeights, "workloadWeights").forEach((capability, weight) -> {
      Objects.requireNonNull(capability, "workload capability");
      if (weight == null || weight < 0) throw new IllegalArgumentException("negative workload weight");
      this.workloadWeights.put(capability, weight);
    });
    this.buildPenalty = buildPenalty;

    ArrayList<String> providers = new ArrayList<>(Objects.requireNonNull(providerIds, "providerIds"));
    providers.replaceAll(value -> requireText(value, "provider id"));
    providers.sort(String::compareTo);
    for (int i = 1; i < providers.size(); i++) {
      if (providers.get(i - 1).equals(providers.get(i))) {
        throw new IllegalArgumentException("duplicate provider id " + providers.get(i));
      }
    }
    this.providerIds = List.copyOf(providers);

    ArrayList<IndexPrecomputeCapability> capabilities =
        new ArrayList<>(Objects.requireNonNull(selectedCapabilities, "selectedCapabilities"));
    capabilities.sort(Comparator.comparingInt(Enum::ordinal));
    this.selectedCapabilities = List.copyOf(capabilities);

    java.util.HashSet<String> sourceIds = new java.util.HashSet<>();
    this.sources.forEach(source -> sourceIds.add(source.id()));
    if (sourceIds.size() != this.sources.size()) {
      for (int i = 1; i < this.sources.size(); i++) {
        if (this.sources.get(i - 1).id().equals(this.sources.get(i).id())) {
          throw new IllegalArgumentException(
              "duplicate precompute source id " + this.sources.get(i).id());
        }
      }
    }
    ArrayList<SourceCapabilityBinding> bindings =
        new ArrayList<>(
            Objects.requireNonNull(sourceCapabilityBindings, "sourceCapabilityBindings"));
    bindings.sort(
        Comparator.comparing(SourceCapabilityBinding::sourceId)
            .thenComparingInt(value -> value.capability().ordinal()));
    java.util.HashSet<String> ownedPairs = new java.util.HashSet<>();
    for (SourceCapabilityBinding binding : bindings) {
      if (!sourceIds.contains(binding.sourceId())) {
        throw new IllegalArgumentException(
            "source capability binding references unknown source " + binding.sourceId());
      }
      if (!this.selectedCapabilities.contains(binding.capability())) {
        throw new IllegalArgumentException(
            "source capability binding is not selected: " + binding.capability());
      }
      String pair = binding.sourceId() + "\u0000" + binding.capability().name();
      if (!ownedPairs.add(pair)) {
        throw new IllegalArgumentException(
            "duplicate source capability binding: " + binding.sourceId()
                + ":" + binding.capability());
      }
    }
    this.sourceCapabilityBindings = List.copyOf(bindings);

    this.facetRoots = new EnumMap<>(IndexPrecomputeCapability.class);
    Objects.requireNonNull(facetRoots, "facetRoots").forEach((capability, root) ->
        this.facetRoots.put(
            Objects.requireNonNull(capability, "facet capability"),
            IndexPrecomputeSource.requireSha256(root, "facet root")));

    this.selectedAccelerators = new EnumMap<>(IndexPrecomputeKernel.class);
    Objects.requireNonNull(selectedAccelerators, "selectedAccelerators").forEach((kernel, id) ->
        this.selectedAccelerators.put(
            Objects.requireNonNull(kernel, "kernel"),
            requireText(id, "accelerator id")));

    this.canonicalText = encodeCanonical();
    this.rootHash = sha256Hex(canonicalText.getBytes(StandardCharsets.UTF_8));
  }

  public String generationId() { return generationId; }
  public long byteBudget() { return byteBudget; }
  public int nativeBatchThreshold() { return nativeBatchThreshold; }
  public boolean mmapPreferred() { return mmapPreferred; }
  public long recommendationRetainedBytes() { return recommendationRetainedBytes; }
  public long actualRetainedBytes() { return actualRetainedBytes; }
  public long recommendationScore() { return recommendationScore; }
  public boolean exactSearch() { return exactSearch; }
  public List<SourceDescriptor> sources() { return sources; }
  public List<SourceCapabilityBinding> sourceCapabilityBindings() {
    return sourceCapabilityBindings;
  }
  public List<CandidateDescriptor> candidates() { return candidates; }
  public Map<IndexPrecomputeCapability, Integer> workloadWeights() {
    return Map.copyOf(workloadWeights);
  }
  public int buildPenalty() { return buildPenalty; }
  public List<String> providerIds() { return providerIds; }
  public List<IndexPrecomputeCapability> selectedCapabilities() { return selectedCapabilities; }
  public Map<IndexPrecomputeCapability, String> facetRoots() { return Map.copyOf(facetRoots); }
  public Map<IndexPrecomputeKernel, String> selectedAccelerators() {
    return Map.copyOf(selectedAccelerators);
  }
  public String canonicalText() { return canonicalText; }
  public String rootHash() { return rootHash; }

  public IndexPrecomputePlan toPlan() {
    return new IndexPrecomputePlan(
        Set.copyOf(selectedCapabilities), byteBudget, nativeBatchThreshold, mmapPreferred);
  }

  public static IndexPrecomputeManifest parseCanonical(String canonical) {
    Objects.requireNonNull(canonical, "canonical");
    String generationId = null;
    long byteBudget = -1;
    int nativeBatchThreshold = -1;
    boolean mmapPreferred = false;
    long recommendationBytes = -1;
    long actualBytes = -1;
    long recommendationScore = 0;
    boolean exactSearch = false;
    int buildPenalty = -1;
    ArrayList<SourceDescriptor> sources = new ArrayList<>();
    ArrayList<SourceCapabilityBinding> sourceCapabilityBindings = new ArrayList<>();
    ArrayList<CandidateDescriptor> candidates = new ArrayList<>();
    EnumMap<IndexPrecomputeCapability, Integer> weights =
        new EnumMap<>(IndexPrecomputeCapability.class);
    ArrayList<String> providers = new ArrayList<>();
    java.util.EnumSet<IndexPrecomputeCapability> capabilities =
        java.util.EnumSet.noneOf(IndexPrecomputeCapability.class);
    EnumMap<IndexPrecomputeCapability, String> roots =
        new EnumMap<>(IndexPrecomputeCapability.class);
    EnumMap<IndexPrecomputeKernel, String> accelerators =
        new EnumMap<>(IndexPrecomputeKernel.class);

    String[] lines = canonical.split("\n", -1);
    for (String line : lines) {
      if (line.isEmpty()) continue;
      String[] fields = line.split("\t", -1);
      switch (fields[0]) {
        case "G" -> generationId = decode(fields[1]);
        case "B" -> {
          byteBudget = Long.parseLong(fields[1]);
          nativeBatchThreshold = Integer.parseInt(fields[2]);
          mmapPreferred = "1".equals(fields[3]);
          recommendationBytes = Long.parseLong(fields[4]);
          actualBytes = Long.parseLong(fields[5]);
          recommendationScore = Long.parseLong(fields[6]);
          exactSearch = "1".equals(fields[7]);
          buildPenalty = Integer.parseInt(fields[8]);
        }
        case "S" -> sources.add(new SourceDescriptor(
            decode(fields[1]), fields[2], decode(fields[3])));
        case "O" -> sourceCapabilityBindings.add(
            new SourceCapabilityBinding(decode(fields[1]), capability(fields[2])));
        case "C" -> {
          IndexPrecomputeCapability capability = capability(fields[1]);
          List<IndexPrecomputeCapability> dependencies =
              fields[6].isEmpty()
                  ? List.of()
                  : java.util.Arrays.stream(fields[6].split(","))
                      .map(IndexPrecomputeManifest::capability)
                      .toList();
          candidates.add(new CandidateDescriptor(
              capability,
              Long.parseLong(fields[2]),
              Long.parseLong(fields[3]),
              Long.parseLong(fields[4]),
              dependencies,
              "1".equals(fields[5])));
        }
        case "W" -> weights.put(capability(fields[1]), Integer.parseInt(fields[2]));
        case "P" -> providers.add(decode(fields[1]));
        case "A" -> capabilities.add(capability(fields[1]));
        case "F" -> roots.put(capability(fields[1]), fields[2]);
        case "K" -> accelerators.put(IndexPrecomputeKernel.valueOf(fields[1]), decode(fields[2]));
        default -> throw new IllegalArgumentException("unknown manifest record " + fields[0]);
      }
    }
    if (generationId == null || byteBudget < 0 || nativeBatchThreshold < 0
        || recommendationBytes < 0 || actualBytes < 0 || buildPenalty < 0) {
      throw new IllegalArgumentException("incomplete canonical precompute manifest");
    }
    IndexPrecomputeManifest parsed = new IndexPrecomputeManifest(
        generationId,
        byteBudget,
        nativeBatchThreshold,
        mmapPreferred,
        recommendationBytes,
        actualBytes,
        recommendationScore,
        exactSearch,
        sources,
        sourceCapabilityBindings,
        candidates,
        weights,
        buildPenalty,
        providers,
        capabilities,
        roots,
        accelerators);
    if (!parsed.canonicalText.equals(canonical)) {
      throw new IllegalArgumentException("manifest is not in canonical order/encoding");
    }
    return parsed;
  }

  private String encodeCanonical() {
    StringBuilder out = new StringBuilder(1024);
    out.append("G\t").append(encode(generationId)).append('\n');
    out.append("B\t").append(byteBudget)
        .append('\t').append(nativeBatchThreshold)
        .append('\t').append(mmapPreferred ? 1 : 0)
        .append('\t').append(recommendationRetainedBytes)
        .append('\t').append(actualRetainedBytes)
        .append('\t').append(recommendationScore)
        .append('\t').append(exactSearch ? 1 : 0)
        .append('\t').append(buildPenalty).append('\n');
    for (SourceDescriptor source : sources) {
      out.append("S\t").append(encode(source.id()))
          .append('\t').append(source.rootHash())
          .append('\t').append(encode(source.sourceType())).append('\n');
    }
    for (SourceCapabilityBinding binding : sourceCapabilityBindings) {
      out.append("O\t").append(encode(binding.sourceId()))
          .append('\t').append(binding.capability().name()).append('\n');
    }
    for (CandidateDescriptor candidate : candidates) {
      out.append("C\t").append(candidate.capability().name())
          .append('\t').append(candidate.retainedBytes())
          .append('\t').append(candidate.buildCostUnits())
          .append('\t').append(candidate.expectedReuseCount())
          .append('\t').append(candidate.mandatory() ? 1 : 0)
          .append('\t');
      for (int i = 0; i < candidate.dependencies().size(); i++) {
        if (i > 0) out.append(',');
        out.append(candidate.dependencies().get(i).name());
      }
      out.append('\n');
    }
    for (IndexPrecomputeCapability capability : IndexPrecomputeCapability.values()) {
      Integer weight = workloadWeights.get(capability);
      if (weight != null) {
        out.append("W\t").append(capability.name()).append('\t').append(weight).append('\n');
      }
    }
    for (String provider : providerIds) out.append("P\t").append(encode(provider)).append('\n');
    for (IndexPrecomputeCapability capability : selectedCapabilities) {
      out.append("A\t").append(capability.name()).append('\n');
    }
    for (var entry : facetRoots.entrySet()) {
      out.append("F\t").append(entry.getKey().name()).append('\t').append(entry.getValue()).append('\n');
    }
    for (var entry : selectedAccelerators.entrySet()) {
      out.append("K\t").append(entry.getKey().name()).append('\t').append(encode(entry.getValue())).append('\n');
    }
    return out.toString();
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(String value) {
    return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
  }

  private static IndexPrecomputeCapability capability(String name) {
    return IndexPrecomputeCapability.valueOf(name.toUpperCase(Locale.ROOT));
  }

  private static String requireText(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException("blank " + field);
    return normalized;
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }
}
