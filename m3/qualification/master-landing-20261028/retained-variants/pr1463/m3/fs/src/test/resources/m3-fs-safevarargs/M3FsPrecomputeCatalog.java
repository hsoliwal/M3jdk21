// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filesystem retention policy for reusable CPU/JNI/GPU primitive precomputes.
 *
 * <p>Profiles describe portable data geometry, not backend authority. A GPU provider may consume a
 * profile only after its own capability, parity and crossover gates pass.</p>
 */
public final class M3FsPrecomputeCatalog {
  public enum Retention {
    KEEP,
    KEEP_IF_REUSED
  }

  public record Profile(
      String id,
      Retention retention,
      Set<M3FsPrecomputeBundle.Role> requiredRoles,
      Set<M3FsPrecomputeBundle.Role> optionalRoles,
      List<String> acceleratorOperations) {
    public Profile {
      id = requireId(id);
      retention = Objects.requireNonNull(retention, "retention");
      requiredRoles = Set.copyOf(Objects.requireNonNull(requiredRoles, "requiredRoles"));
      optionalRoles = Set.copyOf(Objects.requireNonNull(optionalRoles, "optionalRoles"));
      acceleratorOperations =
          List.copyOf(Objects.requireNonNull(acceleratorOperations, "acceleratorOperations"));
      if (requiredRoles.isEmpty()) throw new IllegalArgumentException("requiredRoles");
      EnumSet<M3FsPrecomputeBundle.Role> overlap =
          EnumSet.noneOf(M3FsPrecomputeBundle.Role.class);
      overlap.addAll(requiredRoles);
      overlap.retainAll(optionalRoles);
      if (!overlap.isEmpty()) throw new IllegalArgumentException("role overlap");
    }

    public void requireCompatible(M3FsPrecomputeBundle bundle) {
      M3FsPrecomputeBundle checked = Objects.requireNonNull(bundle, "bundle");
      if (!checked.profile().equals(id)) {
        throw new IllegalArgumentException(
            "bundle profile " + checked.profile() + " != " + id);
      }
      EnumSet<M3FsPrecomputeBundle.Role> present =
          EnumSet.noneOf(M3FsPrecomputeBundle.Role.class);
      checked.lanes().forEach(lane -> present.add(lane.role()));
      if (!present.containsAll(requiredRoles)) {
        EnumSet<M3FsPrecomputeBundle.Role> missing = EnumSet.copyOf(requiredRoles);
        missing.removeAll(present);
        throw new IllegalArgumentException("missing required precompute roles: " + missing);
      }
    }
  }

  private static final List<Profile> PROFILES =
      List.of(
          profile(
              "candidate-stream-v1",
              Retention.KEEP_IF_REUSED,
              roles(
                  M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
                  M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32),
              roles(
                  M3FsPrecomputeBundle.Role.TOKEN_IDS_I32,
                  M3FsPrecomputeBundle.Role.TOKEN_IDS_I64),
              "GREP_I32_SUBSEQUENCE"),
          profile(
              "gpu-search-index-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.UTF16_CODE_UNITS_I16,
                  M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,
                  M3FsPrecomputeBundle.Role.NGRAM_IDS_I64,
                  M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I32,
                  M3FsPrecomputeBundle.Role.LOOKUP_DISPLACEMENTS_I32,
                  M3FsPrecomputeBundle.Role.LOOKUP_SLOTS_I32),
              roles(
                  M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64,
                  M3FsPrecomputeBundle.Role.RANK_SUPERBLOCKS_I32,
                  M3FsPrecomputeBundle.Role.SELECT_SAMPLES_I32,
                  M3FsPrecomputeBundle.Role.KEY_FREQUENCY_I64),
              "GREP_I32_SUBSEQUENCE",
              "SET_INTERSECTION_I32",
              "BITMAP_AND_I64",
              "LOWER_BOUND_I32_BATCH"),
          profile(
              "trigram-postings-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.TRIGRAM_IDS_I64,
                  M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I32),
              roles(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              "SET_INTERSECTION_I32",
              "BITMAP_AND_I64"),
          profile(
              "posting-set-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I32),
              roles(
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I64,
                  M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              "SET_INTERSECTION_I32",
              "SET_UNION_I32",
              "SET_DIFFERENCE_I32"),
          profile(
              "bitmap-set-v1",
              Retention.KEEP,
              roles(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              roles(),
              "BITMAP_AND_I64",
              "BITMAP_OR_I64",
              "BITMAP_AND_NOT_I64",
              "BITMAP_POPCOUNT_I64"),
          profile(
              "automaton-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.AUTOMATON_TRANSITIONS_I32,
                  M3FsPrecomputeBundle.Role.AUTOMATON_ACCEPT_BITMAP_I64),
              roles(M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I32),
              "AUTOMATON_I32_FINAL_STATE_BATCH"),
          profile(
              "sorted-bounds-v1",
              Retention.KEEP,
              roles(M3FsPrecomputeBundle.Role.SORTED_KEYS_I32),
              roles(M3FsPrecomputeBundle.Role.ROW_IDS_I32),
              "LOWER_BOUND_I32_BATCH",
              "UPPER_BOUND_I32_BATCH",
              "MEMBERSHIP_I32_SORTED"),
          profile(
              "csr-graph-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.CSR_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.CSR_EDGES_I32),
              roles(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              "CSR_FRONTIER_I32"),
          profile(
              "hash-signatures-v1",
              Retention.KEEP_IF_REUSED,
              roles(M3FsPrecomputeBundle.Role.HASH64_I64),
              roles(
                  M3FsPrecomputeBundle.Role.MINHASH64_I64,
                  M3FsPrecomputeBundle.Role.SIMHASH64_I64),
              "HASH64_BATCH",
              "MINHASH64_I32_ROWS",
              "SIMHASH64_I32_ROWS"),
          profile(
              "window-range-v1",
              Retention.KEEP_IF_REUSED,
              roles(
                  M3FsPrecomputeBundle.Role.RANGE_STARTS_I32,
                  M3FsPrecomputeBundle.Role.RANGE_ENDS_I32),
              roles(
                  M3FsPrecomputeBundle.Role.WINDOW_HASH64_I64,
                  M3FsPrecomputeBundle.Role.PREFIX_SCAN_I64),
              "WINDOW_HASH64_I32",
              "SCAN_I64_INCLUSIVE"),
          profile(
              "tuple-lane-v1",
              Retention.KEEP,
              roles(M3FsPrecomputeBundle.Role.TUPLE_VALUES_I64),
              roles(
                  M3FsPrecomputeBundle.Role.GROUP_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64)),
          profile(
              "candidate-stream-wide-v1",
              Retention.KEEP_IF_REUSED,
              roles(
                  M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,
                  M3FsPrecomputeBundle.Role.RECORD_OFFSETS_I64),
              roles(
                  M3FsPrecomputeBundle.Role.TOKEN_IDS_I32,
                  M3FsPrecomputeBundle.Role.TOKEN_IDS_I64),
              "GREP_I32_SUBSEQUENCE"),
          profile(
              "posting-set-wide-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I64,
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I64),
              roles(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64)),
          profile(
              "rank-select-bitmap-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64,
                  M3FsPrecomputeBundle.Role.RANK_SUPERBLOCKS_I32),
              roles(M3FsPrecomputeBundle.Role.SELECT_SAMPLES_I32),
              "BITMAP_AND_I64",
              "BITMAP_OR_I64",
              "BITMAP_AND_NOT_I64",
              "BITMAP_XOR_I64",
              "BITMAP_POPCOUNT_I64",
              "POPCOUNT_I64"),
          profile(
              "static-graph-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.CSR_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.CSR_EDGES_I32,
                  M3FsPrecomputeBundle.Role.REVERSE_CSR_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.REVERSE_CSR_EDGES_I32),
              roles(
                  M3FsPrecomputeBundle.Role.TOPOLOGICAL_ORDER_I32,
                  M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              "CSR_FRONTIER_I32"),
          profile(
              "join-index-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.SORTED_KEYS_I32,
                  M3FsPrecomputeBundle.Role.ROW_IDS_I32,
                  M3FsPrecomputeBundle.Role.GROUP_OFFSETS_I32),
              roles(
                  M3FsPrecomputeBundle.Role.RADIX_BUCKET_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.PARTITION_ROW_IDS_I32),
              "LOWER_BOUND_I32_BATCH",
              "UPPER_BOUND_I32_BATCH",
              "MEMBERSHIP_I32_SORTED",
              "GROUP_STARTS_I32_SORTED",
              "DIRECT_INNER_JOIN_I32_DISTINCT"),
          profile(
              "posting-index-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.SORTED_KEYS_I32,
                  M3FsPrecomputeBundle.Role.POSTING_OFFSETS_I32,
                  M3FsPrecomputeBundle.Role.POSTING_VALUES_I32),
              roles(M3FsPrecomputeBundle.Role.BITMAP_WORDS_I64),
              "LOWER_BOUND_I32_BATCH",
              "SET_INTERSECTION_I32"),
          profile(
              "interval-index-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.RANGE_STARTS_I32,
                  M3FsPrecomputeBundle.Role.RANGE_ENDS_I32),
              roles(),
              "LOWER_BOUND_I32_BATCH",
              "UPPER_BOUND_I32_BATCH"),
          profile(
              "dominator-tree-v1",
              Retention.KEEP,
              roles(
                  M3FsPrecomputeBundle.Role.DOMINATOR_PARENT_I32,
                  M3FsPrecomputeBundle.Role.REACHABLE_VERTICES_I32),
              roles()),
          profile(
              "search-selectivity-v1",
              Retention.KEEP_IF_REUSED,
              roles(
                  M3FsPrecomputeBundle.Role.TRIGRAM_IDS_I64,
                  M3FsPrecomputeBundle.Role.KEY_FREQUENCY_I64),
              roles(M3FsPrecomputeBundle.Role.HISTOGRAM_COUNTS_I64),
              "HISTOGRAM_I32_DENSE"));

  private M3FsPrecomputeCatalog() {}

  public static List<Profile> profiles() {
    return PROFILES;
  }

  public static Profile byId(String id) {
    String checked = requireId(id);
    return PROFILES.stream()
        .filter(profile -> profile.id().equals(checked))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown precompute profile: " + checked));
  }

  public static List<Profile> forOperation(String operation) {
    String checked = Objects.requireNonNull(operation, "operation").strip();
    if (checked.isEmpty()) throw new IllegalArgumentException("operation");
    return PROFILES.stream()
        .filter(profile -> profile.acceleratorOperations().contains(checked))
        .toList();
  }

  @SafeVarargs
  private static Set<M3FsPrecomputeBundle.Role> roles(M3FsPrecomputeBundle.Role... roles) {
    return Set.of(roles);
  }

  private static Profile profile(
      String id,
      Retention retention,
      Set<M3FsPrecomputeBundle.Role> required,
      Set<M3FsPrecomputeBundle.Role> optional,
      String... operations) {
    return new Profile(id, retention, required, optional, List.of(operations));
  }

  private static String requireId(String value) {
    String checked = Objects.requireNonNull(value, "id").strip();
    if (!checked.matches("[a-z0-9][a-z0-9._-]{0,127}")) {
      throw new IllegalArgumentException("profile id");
    }
    return checked;
  }
}
