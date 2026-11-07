// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable dependency/invalidation image reconstructed from a sealed precompute manifest.
 *
 * <p>The image answers incremental generation questions without loading source objects or rerunning
 * providers. Source roots identify changed producers; capability dependency closure identifies every
 * selected downstream facet that must be rebuilt.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeDependencyImage.java blob 91b2ed9fadd1; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputeDependencyImage {
  private final String manifestRoot;
  private final List<String> sourceIds;
  private final List<String> sourceRoots;
  private final long[] ownerSourceBits;
  private final int sourceWordsPerRow;
  private final long[] directDependencyBits;
  private final long[] transitiveDependentBits;
  private final int wordsPerRow;
  private final String rootHash;
  private final long retainedPrimitiveBytes;

  private M3IndexPrecomputeDependencyImage(
      String manifestRoot,
      List<String> sourceIds,
      List<String> sourceRoots,
      long[] ownerSourceBits,
      int sourceWordsPerRow,
      long[] directDependencyBits,
      long[] transitiveDependentBits,
      int wordsPerRow,
      String rootHash) {
    this.manifestRoot = manifestRoot;
    this.sourceIds = sourceIds;
    this.sourceRoots = sourceRoots;
    this.ownerSourceBits = ownerSourceBits;
    this.sourceWordsPerRow = sourceWordsPerRow;
    this.directDependencyBits = directDependencyBits;
    this.transitiveDependentBits = transitiveDependentBits;
    this.wordsPerRow = wordsPerRow;
    this.rootHash = rootHash;
    this.retainedPrimitiveBytes =
        Math.multiplyExact(
            Math.addExact(
                Math.addExact((long) ownerSourceBits.length, directDependencyBits.length),
                transitiveDependentBits.length),
            Long.BYTES);
  }

  public static M3IndexPrecomputeDependencyImage from(M3IndexPrecomputeManifest manifest) {
    Objects.requireNonNull(manifest, "manifest");
    List<M3IndexPrecomputeCapability> selected = manifest.selectedCapabilities();
    EnumSet<M3IndexPrecomputeCapability> selectedSet =
        selected.isEmpty()
            ? EnumSet.noneOf(M3IndexPrecomputeCapability.class)
            : EnumSet.copyOf(selected);

    ArrayList<String> sourceIds = new ArrayList<>(manifest.sources().size());
    ArrayList<String> sourceRoots = new ArrayList<>(manifest.sources().size());
    LinkedHashMap<String, Integer> sourceOrdinal = new LinkedHashMap<>();
    for (M3IndexPrecomputeManifest.SourceDescriptor source : manifest.sources()) {
      if (sourceOrdinal.putIfAbsent(source.id(), sourceOrdinal.size()) != null) {
        throw new IllegalArgumentException("duplicate source id " + source.id());
      }
      sourceIds.add(source.id());
      sourceRoots.add(source.rootHash());
    }

    int capabilityCount = M3IndexPrecomputeCapability.values().length;
    int words = Math.max(1, (capabilityCount + Long.SIZE - 1) / Long.SIZE);
    int sourceWords = Math.max(1, (sourceIds.size() + Long.SIZE - 1) / Long.SIZE);
    long[] owner = new long[Math.multiplyExact(capabilityCount, sourceWords)];
    int[] legacyOwner = new int[capabilityCount];
    java.util.Arrays.fill(legacyOwner, -1);
    boolean multipleOwners = false;

    for (M3IndexPrecomputeManifest.SourceCapabilityBinding binding
        : manifest.sourceCapabilityBindings()) {
      Integer source = sourceOrdinal.get(binding.sourceId());
      if (source == null) {
        throw new IllegalArgumentException(
            "dependency image binding references unknown source " + binding.sourceId());
      }
      int capability = binding.capability().ordinal();
      int previous = legacyOwner[capability];
      if (previous >= 0 && previous != source) multipleOwners = true;
      else legacyOwner[capability] = source;
      setOwner(owner, sourceWords, capability, source);
    }

    EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeManifest.CandidateDescriptor>
        candidateByCapability = new EnumMap<>(M3IndexPrecomputeCapability.class);
    for (M3IndexPrecomputeManifest.CandidateDescriptor candidate : manifest.candidates()) {
      M3IndexPrecomputeManifest.CandidateDescriptor previous =
          candidateByCapability.putIfAbsent(candidate.capability(), candidate);
      if (previous != null) {
        throw new IllegalArgumentException(
            "duplicate candidate descriptor " + candidate.capability());
      }
    }

    long[] direct = new long[Math.multiplyExact(capabilityCount, words)];
    long[] downstream = new long[Math.multiplyExact(capabilityCount, words)];

    for (M3IndexPrecomputeCapability capability : selectedSet) {
      set(downstream, words, capability.ordinal(), capability.ordinal());
      M3IndexPrecomputeManifest.CandidateDescriptor candidate =
          candidateByCapability.get(capability);
      if (candidate == null) continue;
      for (M3IndexPrecomputeCapability dependency : candidate.dependencies()) {
        if (!selectedSet.contains(dependency)) {
          throw new IllegalArgumentException(
              "selected capability "
                  + capability
                  + " depends on unselected capability "
                  + dependency);
        }
        set(direct, words, capability.ordinal(), dependency.ordinal());
        set(downstream, words, dependency.ordinal(), capability.ordinal());
      }
    }

    closeTransitiveDependents(downstream, words, capabilityCount, selectedSet);

    String ownerRoot =
        multipleOwners
            ? M3PrecomputeHashes.sha256(
                "INDEX_PRECOMPUTE_DEPENDENCY_OWNERS_V2",
                owner,
                new int[] {sourceWords, sourceIds.size()})
            : M3PrecomputeHashes.sha256(
                "INDEX_PRECOMPUTE_DEPENDENCY_OWNERS_V1",
                new long[0],
                legacyOwner);
    String directRoot =
        M3PrecomputeHashes.sha256(
            "INDEX_PRECOMPUTE_DEPENDENCY_DIRECT_V1",
            direct,
            new int[] {words});
    String downstreamRoot =
        M3PrecomputeHashes.sha256(
            "INDEX_PRECOMPUTE_DEPENDENCY_TRANSITIVE_V1",
            downstream,
            new int[] {words});
    String root =
        M3PrecomputeHashes.sha256(
            multipleOwners
                ? "INDEX_PRECOMPUTE_DEPENDENCY_IMAGE_V2"
                : "INDEX_PRECOMPUTE_DEPENDENCY_IMAGE_V1",
            manifest.rootHash(),
            ownerRoot,
            directRoot,
            downstreamRoot);

    return new M3IndexPrecomputeDependencyImage(
        manifest.rootHash(),
        List.copyOf(sourceIds),
        List.copyOf(sourceRoots),
        owner,
        sourceWords,
        direct,
        downstream,
        words,
        root);
  }

  public String manifestRoot() {
    return manifestRoot;
  }

  public String rootHash() {
    return rootHash;
  }

  public long retainedPrimitiveBytes() {
    return retainedPrimitiveBytes;
  }

  public int sourceCount() {
    return sourceIds.size();
  }

  public String sourceId(int sourceOrdinal) {
    return sourceIds.get(java.util.Objects.checkIndex(sourceOrdinal, sourceIds.size()));
  }

  public String sourceRoot(int sourceOrdinal) {
    return sourceRoots.get(java.util.Objects.checkIndex(sourceOrdinal, sourceRoots.size()));
  }

  public Map<String, String> sourceRoots() {
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (int index = 0; index < sourceIds.size(); index++) {
      result.put(sourceIds.get(index), sourceRoots.get(index));
    }
    return Map.copyOf(result);
  }

  /**
   * Historical single-owner lookup. Returns empty when there is no owner or more than one owner.
   */
  public Optional<String> sourceFor(M3IndexPrecomputeCapability capability) {
    Set<String> owners = sourcesFor(capability);
    return owners.size() == 1 ? Optional.of(owners.iterator().next()) : Optional.empty();
  }

  /** All content-addressed sources that contributed semantic owners for this capability. */
  public Set<String> sourcesFor(M3IndexPrecomputeCapability capability) {
    int row = Objects.requireNonNull(capability, "capability").ordinal();
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    int base = Math.multiplyExact(row, sourceWordsPerRow);
    for (int word = 0; word < sourceWordsPerRow; word++) {
      long bits = ownerSourceBits[base + word];
      while (bits != 0L) {
        int bit = Long.numberOfTrailingZeros(bits);
        int source = word * Long.SIZE + bit;
        if (source < sourceIds.size()) result.add(sourceIds.get(source));
        bits &= bits - 1L;
      }
    }
    return Set.copyOf(result);
  }

  public Set<M3IndexPrecomputeCapability> dependencies(
      M3IndexPrecomputeCapability capability) {
    Objects.requireNonNull(capability, "capability");
    return bits(directDependencyBits, capability.ordinal());
  }

  /**
   * Selected capabilities invalidated by a changed capability, including the changed capability.
   */
  public Set<M3IndexPrecomputeCapability> invalidatedByCapabilities(
      Set<M3IndexPrecomputeCapability> changed) {
    Objects.requireNonNull(changed, "changed");
    EnumSet<M3IndexPrecomputeCapability> result =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);
    for (M3IndexPrecomputeCapability capability : changed) {
      Objects.requireNonNull(capability, "changed capability");
      addBits(result, transitiveDependentBits, capability.ordinal());
    }
    return Set.copyOf(result);
  }

  /**
   * Compare current source roots to the sealed manifest and return the exact selected capability
   * closure that must be rebuilt.
   *
   * <p>A missing source root is treated as changed. Extra current sources are ignored because they
   * were not inputs to this sealed generation.</p>
   */
  public Set<M3IndexPrecomputeCapability> invalidatedBySourceRoots(
      Map<String, String> currentSourceRoots) {
    Objects.requireNonNull(currentSourceRoots, "currentSourceRoots");
    EnumSet<M3IndexPrecomputeCapability> directlyChanged =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);

    boolean anySourceChanged = false;
    for (int source = 0; source < sourceIds.size(); source++) {
      String id = sourceIds.get(source);
      String current = currentSourceRoots.get(id);
      boolean changed =
          current == null
              || !sourceRoots.get(source)
                  .equals(M3IndexPrecomputeSource.requireSha256(current, "current source root"));
      if (!changed) continue;
      anySourceChanged = true;

      for (M3IndexPrecomputeCapability capability : M3IndexPrecomputeCapability.values()) {
        if (containsOwner(ownerSourceBits, sourceWordsPerRow, capability.ordinal(), source)) {
          directlyChanged.add(capability);
        }
      }
    }

    if (anySourceChanged) {
      for (M3IndexPrecomputeCapability capability : M3IndexPrecomputeCapability.values()) {
        int ordinal = capability.ordinal();
        if (!hasAnyOwner(ownerSourceBits, sourceWordsPerRow, ordinal)
            && contains(transitiveDependentBits, wordsPerRow, ordinal, ordinal)) {
          directlyChanged.add(capability);
        }
      }
    }
    return invalidatedByCapabilities(directlyChanged);
  }

  public boolean reusable(
      M3IndexPrecomputeCapability capability,
      Map<String, String> currentSourceRoots) {
    return !invalidatedBySourceRoots(currentSourceRoots)
        .contains(Objects.requireNonNull(capability, "capability"));
  }

  private Set<M3IndexPrecomputeCapability> bits(long[] matrix, int row) {
    EnumSet<M3IndexPrecomputeCapability> result =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);
    addBits(result, matrix, row);
    return Set.copyOf(result);
  }

  private void addBits(
      EnumSet<M3IndexPrecomputeCapability> target, long[] matrix, int row) {
    int base = Math.multiplyExact(row, wordsPerRow);
    M3IndexPrecomputeCapability[] values = M3IndexPrecomputeCapability.values();
    for (int word = 0; word < wordsPerRow; word++) {
      long bits = matrix[base + word];
      while (bits != 0L) {
        int bit = Long.numberOfTrailingZeros(bits);
        int ordinal = word * Long.SIZE + bit;
        if (ordinal < values.length) target.add(values[ordinal]);
        bits &= bits - 1L;
      }
    }
  }

  private static void closeTransitiveDependents(
      long[] downstream,
      int words,
      int capabilityCount,
      Set<M3IndexPrecomputeCapability> selected) {
    for (int middle = 0; middle < capabilityCount; middle++) {
      M3IndexPrecomputeCapability capability = M3IndexPrecomputeCapability.values()[middle];
      if (!selected.contains(capability)) continue;
      for (int source = 0; source < capabilityCount; source++) {
        if (!selected.contains(M3IndexPrecomputeCapability.values()[source])
            || !contains(downstream, words, source, middle)) {
          continue;
        }
        int sourceBase = source * words;
        int middleBase = middle * words;
        for (int word = 0; word < words; word++) {
          downstream[sourceBase + word] |= downstream[middleBase + word];
        }
      }
    }
  }

  private static void setOwner(long[] matrix, int words, int row, int source) {
    int index = row * words + source / Long.SIZE;
    matrix[index] |= 1L << (source % Long.SIZE);
  }

  private static boolean containsOwner(
      long[] matrix, int words, int row, int source) {
    int index = row * words + source / Long.SIZE;
    return (matrix[index] & (1L << (source % Long.SIZE))) != 0L;
  }

  private static boolean hasAnyOwner(long[] matrix, int words, int row) {
    int base = row * words;
    for (int word = 0; word < words; word++) {
      if (matrix[base + word] != 0L) return true;
    }
    return false;
  }

  private static void set(long[] matrix, int words, int row, int column) {
    int index = row * words + column / Long.SIZE;
    matrix[index] |= 1L << (column % Long.SIZE);
  }

  private static boolean contains(
      long[] matrix, int words, int row, int column) {
    int index = row * words + column / Long.SIZE;
    return (matrix[index] & (1L << (column % Long.SIZE))) != 0L;
  }
}
