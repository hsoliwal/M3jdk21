// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/**
 * Stable capability IDs for compile-once/index-many text and semantic operations.
 *
 * <p>The enum describes contracts, not one implementation. Existing IndexString/MatIndex owners
 * may satisfy a capability directly; generic sidecars fill only the remaining gaps.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeCapability.java blob 851b246a9f57; recipe m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py v3
public enum IndexPrecomputeCapability {
  EXACT_IDENTITY("exact-id", QueryCost.CONSTANT),
  REVERSE_LOOKUP("reverse-lookup", QueryCost.CONSTANT),
  UTF16_LENGTH("utf16-length", QueryCost.CONSTANT),
  UTF8_LENGTH("utf8-length", QueryCost.CONSTANT),
  CODE_POINT_LENGTH("codepoint-length", QueryCost.CONSTANT),
  JAVA_HASH("java-hash", QueryCost.CONSTANT),
  HASH64("hash64", QueryCost.CONSTANT),
  SHA256("sha256", QueryCost.CONSTANT),
  SIMHASH("simhash", QueryCost.CONSTANT),
  LEXICAL_RANK("lexical-rank", QueryCost.CONSTANT),
  PREFIX_RANGE("prefix-range", QueryCost.CONSTANT),
  SUFFIX_RANGE("suffix-range", QueryCost.CONSTANT),
  PREFIX_PARENT("prefix-parent", QueryCost.CONSTANT),
  SUFFIX_PARENT("suffix-parent", QueryCost.CONSTANT),
  CHARACTER_POSTINGS("character-postings", QueryCost.OUTPUT_SENSITIVE),
  SUBSTRING_POSTINGS("substring-postings", QueryCost.OUTPUT_SENSITIVE),
  NGRAM_FACTS("ngram-facts", QueryCost.EXPECTED_CONSTANT),
  PHRASE_AUTOMATON("phrase-automaton", QueryCost.INPUT_LINEAR),
  STEM_ID("stem-id", QueryCost.EXPECTED_CONSTANT),
  DISTANCE_INDEX("distance-index", QueryCost.EXPECTED_CONSTANT),
  DISTANCE_ORDER("distance-order", QueryCost.CONSTANT),
  RELATION("relation", QueryCost.EXPECTED_CONSTANT),
  CONCEPT_PROJECTION("concept-projection", QueryCost.EXPECTED_CONSTANT),
  TRANSLATION("translation", QueryCost.EXPECTED_CONSTANT),
  TUPLE_ID("tuple-id", QueryCost.EXPECTED_CONSTANT),
  TRANSITION("transition", QueryCost.EXPECTED_CONSTANT),
  PATTERN_PLAN("pattern-plan", QueryCost.INPUT_LINEAR),
  REGEX_SET("regex-set", QueryCost.INPUT_LINEAR),
  SEGMENT_LAYOUT("segment-layout", QueryCost.CONSTANT),
  SEGMENT_METADATA("segment-metadata", QueryCost.CONSTANT),
  SEGMENT_PATTERN_SET("segment-pattern-set", QueryCost.INPUT_LINEAR),
  TYPED_DECISIONS("typed-decisions", QueryCost.CONSTANT),
  TYPE_BITS("type-bits", QueryCost.CONSTANT),
  NORMALIZATION("normalization", QueryCost.CONSTANT),
  GUARD("guard", QueryCost.CONSTANT),
  CONCEPT_ADDRESS("concept-address", QueryCost.EXPECTED_CONSTANT),
  PROVENANCE("provenance", QueryCost.CONSTANT),
  DOMAIN_MEMBERSHIP("domain-membership", QueryCost.OUTPUT_SENSITIVE),
  FREQUENCY("frequency", QueryCost.CONSTANT),
  DOCUMENT_FREQUENCY("document-frequency", QueryCost.CONSTANT),
  STRUCTURAL_HASH("structural-hash", QueryCost.CONSTANT),
  LOGIC_HASH("logic-hash", QueryCost.CONSTANT),
  MERKLE_HASH("merkle-hash", QueryCost.CONSTANT),
  ARGUMENTATION("argumentation", QueryCost.EXPECTED_CONSTANT),
  BELIEF_DYNAMICS("belief-dynamics", QueryCost.OUTPUT_SENSITIVE),
  LOGICAL_CONSEQUENCE("logical-consequence", QueryCost.EXPECTED_CONSTANT),
  INCONSISTENCY("inconsistency", QueryCost.CONSTANT),
  PROBABILISTIC_REASONING("probabilistic-reasoning", QueryCost.EXPECTED_CONSTANT),
  REASONING_PROVENANCE("reasoning-provenance", QueryCost.CONSTANT),
  REPOSITORY_EVIDENCE("repository-evidence", QueryCost.OUTPUT_SENSITIVE),
  REPOSITORY_FILE_SCAN("repository-file-scan", QueryCost.CONSTANT),
  REPOSITORY_SOURCE_SIGNALS("repository-source-signals", QueryCost.OUTPUT_SENSITIVE),
  REPOSITORY_AUDIT("repository-audit", QueryCost.OUTPUT_SENSITIVE),
  REPOSITORY_MECHANICAL_PLAN("repository-mechanical-plan", QueryCost.OUTPUT_SENSITIVE),
  REPOSITORY_REACTOR_DEPENDENCIES("repository-reactor-dependencies", QueryCost.OUTPUT_SENSITIVE),
  SERVICE_TOPOLOGY("service-topology", QueryCost.OUTPUT_SENSITIVE),
  VERIFICATION_RECEIPTS("verification-receipts", QueryCost.CONSTANT),
  OPENREWRITE_CANDIDATES("openrewrite-candidates", QueryCost.CONSTANT),
  REPOSITORY_INCREMENTAL_VERIFICATION(
      "repository-incremental-verification", QueryCost.OUTPUT_SENSITIVE);

  private final String id;
  private final QueryCost queryCost;

  IndexPrecomputeCapability(String id, QueryCost queryCost) {
    this.id = id;
    this.queryCost = queryCost;
  }

  public String id() { return id; }
  public QueryCost queryCost() { return queryCost; }

  public enum QueryCost {
    CONSTANT,
    EXPECTED_CONSTANT,
    OUTPUT_SENSITIVE,
    INPUT_LINEAR
  }
}
