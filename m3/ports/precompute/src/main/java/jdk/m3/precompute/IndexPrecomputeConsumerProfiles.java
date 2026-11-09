// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional build-time demand profiles for the existing universal precompute planner.
 *
 * <p>Profiles nominate existing capability IDs; they never compile facets or build a second
 * cache, interner, regex engine, collection index, or runtime executor. Only the existing
 * {@link M3IndexPrecomputePlanner} and {@link M3IndexPrecomputeMultipassCompiler} can select and
 * materialize candidates under actual storage/dependency budgets. Scores are proposed
 * relative priorities, not measured latency/energy or proof of cost amortization.
 *
 * <p>Consumer shares range from 0 (not requested) to 1024 (bounded relative demand). The
 * maximum combined score is 7 * 1024 * 200, safely representable as an {@code int}.
 * Construct once per planning generation; do not run this profile builder in a hot query loop.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeConsumerProfiles.java blob b2ea8dcc26c7; recipe m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py v3
public final class IndexPrecomputeConsumerProfiles {
  public enum Consumer { REGEX, M3_STRING, COLLECTIONS, READER, WRITER, SWING, UTIL }

  private static final int MAX_SHARE = 1024;
  private static final EnumMap<Consumer, Map<IndexPrecomputeCapability, Integer>> PROFILES =
      createProfiles();

  private IndexPrecomputeConsumerProfiles() { }

  /** A single consumer's reviewed candidate weights; not an allocation budget. */
  public static IndexPrecomputeWorkload forConsumer(Consumer consumer) {
    return blend(Map.of(Objects.requireNonNull(consumer, "consumer"), 1), 1);
  }

  /** Immutable weights; capabilities absent from this profile have zero priority. */
  public static Map<IndexPrecomputeCapability, Integer> weights(Consumer consumer) {
    return PROFILES.get(Objects.requireNonNull(consumer, "consumer"));
  }

  /**
   * Combine relative consumer demand deterministically in enum order.
   * Caller-supplied shares do not bypass provider eligibility, byte budgets or exact
   * verification. Zero shares are ignored, but an all-zero workload is refused.
   */
  public static IndexPrecomputeWorkload blend(
      Map<Consumer, Integer> shares, int buildPenalty) {
    Objects.requireNonNull(shares, "shares");
    if (buildPenalty < 0) throw new IllegalArgumentException("negative buildPenalty");
    for (var entry : shares.entrySet()) {
      Objects.requireNonNull(entry.getKey(), "consumer");
      checkShare(Objects.requireNonNull(entry.getValue(), "share"));
    }
    EnumMap<IndexPrecomputeCapability, Integer> result =
        new EnumMap<>(IndexPrecomputeCapability.class);
    boolean requested = false;
    for (Consumer consumer : Consumer.values()) {
      int share = shares.getOrDefault(consumer, 0);
      if (share == 0) continue;
      requested = true;
      for (var entry : PROFILES.get(consumer).entrySet()) {
        result.merge(entry.getKey(), Math.multiplyExact(share, entry.getValue()), Math::addExact);
      }
    }
    if (!requested) throw new IllegalArgumentException("no positive consumer demand");
    IndexPrecomputeWorkload.Builder builder =
        IndexPrecomputeWorkload.builder().buildPenalty(buildPenalty);
    for (IndexPrecomputeCapability capability : IndexPrecomputeCapability.values()) {
      int score = result.getOrDefault(capability, 0);
      if (score != 0) builder.weight(capability, score);
    }
    return builder.build();
  }

  private static int checkShare(int share) {
    if (share < 0 || share > MAX_SHARE)
      throw new IllegalArgumentException("consumer share outside [0,1024]");
    return share;
  }

  private static EnumMap<Consumer, Map<IndexPrecomputeCapability, Integer>> createProfiles() {
    EnumMap<Consumer, Map<IndexPrecomputeCapability, Integer>> result =
        new EnumMap<>(Consumer.class);
    result.put(Consumer.REGEX, profile(
        IndexPrecomputeCapability.PATTERN_PLAN, 200,
        IndexPrecomputeCapability.REGEX_SET, 190,
        IndexPrecomputeCapability.SEGMENT_PATTERN_SET, 180,
        IndexPrecomputeCapability.SUBSTRING_POSTINGS, 150,
        IndexPrecomputeCapability.PHRASE_AUTOMATON, 140,
        IndexPrecomputeCapability.UTF16_LENGTH, 130,
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 120,
        IndexPrecomputeCapability.NGRAM_FACTS, 110));
    result.put(Consumer.M3_STRING, profile(
        IndexPrecomputeCapability.EXACT_IDENTITY, 200,
        IndexPrecomputeCapability.UTF16_LENGTH, 200,
        IndexPrecomputeCapability.JAVA_HASH, 190,
        IndexPrecomputeCapability.CODE_POINT_LENGTH, 180,
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 170,
        IndexPrecomputeCapability.SEGMENT_METADATA, 160,
        IndexPrecomputeCapability.PREFIX_RANGE, 130,
        IndexPrecomputeCapability.SUFFIX_RANGE, 130,
        IndexPrecomputeCapability.NGRAM_FACTS, 115));
    result.put(Consumer.COLLECTIONS, profile(
        IndexPrecomputeCapability.EXACT_IDENTITY, 200,
        IndexPrecomputeCapability.DOMAIN_MEMBERSHIP, 190,
        IndexPrecomputeCapability.TUPLE_ID, 170,
        IndexPrecomputeCapability.LEXICAL_RANK, 160,
        IndexPrecomputeCapability.RELATION, 150,
        IndexPrecomputeCapability.TYPE_BITS, 130,
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 90));
    result.put(Consumer.READER, profile(
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 200,
        IndexPrecomputeCapability.UTF16_LENGTH, 190,
        IndexPrecomputeCapability.UTF8_LENGTH, 170,
        IndexPrecomputeCapability.SEGMENT_METADATA, 160,
        IndexPrecomputeCapability.CODE_POINT_LENGTH, 150,
        IndexPrecomputeCapability.PHRASE_AUTOMATON, 130,
        IndexPrecomputeCapability.PATTERN_PLAN, 120));
    result.put(Consumer.WRITER, profile(
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 200,
        IndexPrecomputeCapability.UTF8_LENGTH, 195,
        IndexPrecomputeCapability.UTF16_LENGTH, 180,
        IndexPrecomputeCapability.CODE_POINT_LENGTH, 150,
        IndexPrecomputeCapability.SEGMENT_METADATA, 140,
        IndexPrecomputeCapability.JAVA_HASH, 120,
        IndexPrecomputeCapability.NORMALIZATION, 100));
    result.put(Consumer.SWING, profile(
        IndexPrecomputeCapability.SEGMENT_LAYOUT, 200,
        IndexPrecomputeCapability.SEGMENT_METADATA, 190,
        IndexPrecomputeCapability.DOMAIN_MEMBERSHIP, 180,
        IndexPrecomputeCapability.LEXICAL_RANK, 170,
        IndexPrecomputeCapability.CHARACTER_POSTINGS, 150,
        IndexPrecomputeCapability.PREFIX_RANGE, 110,
        IndexPrecomputeCapability.PATTERN_PLAN, 100));
    result.put(Consumer.UTIL, profile(
        IndexPrecomputeCapability.EXACT_IDENTITY, 200,
        IndexPrecomputeCapability.JAVA_HASH, 195,
        IndexPrecomputeCapability.HASH64, 180,
        IndexPrecomputeCapability.TYPE_BITS, 160,
        IndexPrecomputeCapability.NORMALIZATION, 140,
        IndexPrecomputeCapability.STRUCTURAL_HASH, 130,
        IndexPrecomputeCapability.PREFIX_RANGE, 100));
    if (result.size() != Consumer.values().length)
      throw new IllegalStateException("missing consumer profile");
    return result;
  }

  private static Map<IndexPrecomputeCapability, Integer> profile(Object... entries) {
    if ((entries.length & 1) != 0) throw new IllegalArgumentException("unpaired capability");
    EnumMap<IndexPrecomputeCapability, Integer> result =
        new EnumMap<>(IndexPrecomputeCapability.class);
    for (int i = 0; i < entries.length; i += 2) {
      IndexPrecomputeCapability capability = (IndexPrecomputeCapability) entries[i];
      int value = (Integer) entries[i + 1];
      if (value < 1 || value > 200 || result.putIfAbsent(capability, value) != null)
        throw new IllegalArgumentException("invalid/duplicate profile capability");
    }
    return Map.copyOf(result);
  }
}
