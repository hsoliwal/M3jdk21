// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable workload weights used by the budget planner. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeWorkload.java blob 59f076375372; recipe m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py v3
public final class IndexPrecomputeWorkload {
  private final EnumMap<IndexPrecomputeCapability, Integer> weights;
  private final int buildPenalty;

  private IndexPrecomputeWorkload(
      Map<IndexPrecomputeCapability, Integer> weights,
      int buildPenalty) {
    this.weights = new EnumMap<>(IndexPrecomputeCapability.class);
    this.weights.putAll(weights);
    if (buildPenalty < 0) throw new IllegalArgumentException("negative buildPenalty");
    this.buildPenalty = buildPenalty;
  }

  public int weight(IndexPrecomputeCapability capability) {
    return weights.getOrDefault(Objects.requireNonNull(capability, "capability"), 0);
  }

  public int buildPenalty() { return buildPenalty; }

  /** Immutable explicit weights, suitable for deterministic manifesting/replay. */
  public Map<IndexPrecomputeCapability, Integer> weights() {
    return Map.copyOf(weights);
  }

  public static Builder builder() { return new Builder(); }

  /** Balanced indexed-language/search workload. */
  public static IndexPrecomputeWorkload languageRuntime() {
    Builder builder = builder().buildPenalty(1);
    for (IndexPrecomputeCapability capability : IndexPrecomputeCapability.values()) {
      int weight = switch (capability) {
        case EXACT_IDENTITY, REVERSE_LOOKUP, UTF16_LENGTH, JAVA_HASH, HASH64,
             LEXICAL_RANK, PREFIX_RANGE, SUFFIX_RANGE, PREFIX_PARENT, SUFFIX_PARENT,
             TYPE_BITS, NORMALIZATION, CONCEPT_PROJECTION, TRANSLATION -> 100;
        case NGRAM_FACTS, PHRASE_AUTOMATON, STEM_ID, RELATION, TUPLE_ID, TRANSITION,
             DOMAIN_MEMBERSHIP, FREQUENCY, DOCUMENT_FREQUENCY, ARGUMENTATION,
             BELIEF_DYNAMICS, LOGICAL_CONSEQUENCE, INCONSISTENCY,
             PROBABILISTIC_REASONING, REASONING_PROVENANCE -> 70;
        case CHARACTER_POSTINGS, SUBSTRING_POSTINGS, PATTERN_PLAN, REGEX_SET,
             DISTANCE_INDEX, DISTANCE_ORDER -> 45;
        default -> 25;
      };
      builder.weight(capability, weight);
    }
    return builder.build();
  }

  public static final class Builder {
    private final EnumMap<IndexPrecomputeCapability, Integer> weights =
        new EnumMap<>(IndexPrecomputeCapability.class);
    private int buildPenalty = 1;

    public Builder weight(IndexPrecomputeCapability capability, int value) {
      if (value < 0) throw new IllegalArgumentException("negative weight");
      weights.put(Objects.requireNonNull(capability, "capability"), value);
      return this;
    }

    public Builder buildPenalty(int value) {
      if (value < 0) throw new IllegalArgumentException("negative buildPenalty");
      buildPenalty = value;
      return this;
    }

    public IndexPrecomputeWorkload build() {
      return new IndexPrecomputeWorkload(weights, buildPenalty);
    }
  }
}
