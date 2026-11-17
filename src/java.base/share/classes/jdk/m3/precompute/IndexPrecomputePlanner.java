// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Budget/workload planner for immutable precomputation.
 *
 * <p>Up to 20 candidates are searched exactly. Larger catalogs use a deterministic
 * dependency-aware benefit/byte greedy pass, because exhaustive 2^N enumeration is not a sensible
 * build-time default for a universal capability catalog.</p>
 *
 * <p>Dependencies may form cycles. Both passes treat a cycle as one dependency-closed group: the exact
 * search admits a capability only together with all of its dependencies, and the greedy pass closes a
 * capability's dependencies visiting each capability once.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputePlanner.java blob 18b16bda6e3b; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class IndexPrecomputePlanner {
  public static final int EXACT_LIMIT = 20;

  public IndexPrecomputeRecommendation recommend(
      List<IndexPrecomputeCandidate> candidates,
      long byteBudget,
      IndexPrecomputeWorkload workload) {
    Objects.requireNonNull(candidates, "candidates");
    Objects.requireNonNull(workload, "workload");
    if (byteBudget < 0) throw new IllegalArgumentException("negative byteBudget");

    List<IndexPrecomputeCandidate> normalized = normalize(candidates);
    validateDependencies(normalized);
    return normalized.size() <= EXACT_LIMIT
        ? exact(normalized, byteBudget, workload)
        : greedy(normalized, byteBudget, workload);
  }

  private IndexPrecomputeRecommendation exact(
      List<IndexPrecomputeCandidate> candidates,
      long budget,
      IndexPrecomputeWorkload workload) {
    Search search = new Search(candidates, budget, workload);
    search.visit(0, EnumSet.noneOf(IndexPrecomputeCapability.class), 0L, 0L);
    if (!search.found) {
      throw new IllegalStateException("no valid precompute subset fits the byte budget");
    }
    return new IndexPrecomputeRecommendation(
        search.bestCapabilities, search.bestBytes, search.bestScore, true);
  }

  private IndexPrecomputeRecommendation greedy(
      List<IndexPrecomputeCandidate> candidates,
      long budget,
      IndexPrecomputeWorkload workload) {
    EnumMap<IndexPrecomputeCapability, IndexPrecomputeCandidate> byCapability =
        byCapability(candidates);
    EnumSet<IndexPrecomputeCapability> chosen =
        EnumSet.noneOf(IndexPrecomputeCapability.class);
    long bytes = 0;
    long score = 0;

    for (IndexPrecomputeCandidate candidate : candidates) {
      if (!candidate.mandatory()) continue;
      Closure closure = closure(candidate.capability(), byCapability, chosen, workload);
      bytes = Math.addExact(bytes, closure.additionalBytes);
      if (bytes > budget) {
        throw new IllegalStateException("mandatory precompute capabilities exceed byte budget");
      }
      score = saturatingAdd(score, closure.additionalScore);
      chosen.addAll(closure.capabilities);
    }

    while (true) {
      Closure best = null;
      for (IndexPrecomputeCandidate candidate : candidates) {
        if (chosen.contains(candidate.capability())) continue;
        Closure closure = closure(candidate.capability(), byCapability, chosen, workload);
        if (Math.addExact(bytes, closure.additionalBytes) > budget) continue;
        if (closure.additionalScore <= 0) continue;
        if (best == null || betterDensity(closure, best)) best = closure;
      }
      if (best == null) break;
      chosen.addAll(best.capabilities);
      bytes = Math.addExact(bytes, best.additionalBytes);
      score = saturatingAdd(score, best.additionalScore);
    }

    return new IndexPrecomputeRecommendation(chosen, bytes, score, false);
  }

  private static boolean betterDensity(Closure left, Closure right) {
    BigInteger leftCross = BigInteger.valueOf(left.additionalScore)
        .multiply(BigInteger.valueOf(Math.max(1L, right.additionalBytes)));
    BigInteger rightCross = BigInteger.valueOf(right.additionalScore)
        .multiply(BigInteger.valueOf(Math.max(1L, left.additionalBytes)));
    int compared = leftCross.compareTo(rightCross);
    if (compared != 0) return compared > 0;
    if (left.additionalScore != right.additionalScore) {
      return left.additionalScore > right.additionalScore;
    }
    if (left.additionalBytes != right.additionalBytes) {
      return left.additionalBytes < right.additionalBytes;
    }
    return left.capabilities.toString().compareTo(right.capabilities.toString()) < 0;
  }

  private static Closure closure(
      IndexPrecomputeCapability capability,
      Map<IndexPrecomputeCapability, IndexPrecomputeCandidate> byCapability,
      Set<IndexPrecomputeCapability> already,
      IndexPrecomputeWorkload workload) {
    EnumSet<IndexPrecomputeCapability> result =
        EnumSet.noneOf(IndexPrecomputeCapability.class);
    collect(capability, byCapability, already, result);
    long bytes = 0;
    long score = 0;
    for (IndexPrecomputeCapability item : result) {
      IndexPrecomputeCandidate candidate = byCapability.get(item);
      bytes = Math.addExact(bytes, candidate.retainedBytes());
      score = saturatingAdd(score, score(candidate, workload));
    }
    return new Closure(result, bytes, score);
  }

  private static void collect(
      IndexPrecomputeCapability capability,
      Map<IndexPrecomputeCapability, IndexPrecomputeCandidate> byCapability,
      Set<IndexPrecomputeCapability> already,
      EnumSet<IndexPrecomputeCapability> result) {
    //FLLDCIM: MRL - recipe planner-cycle-fix-20261107: explicit stack, one visit per capability (a dependency cycle closes as one group)
    ArrayDeque<IndexPrecomputeCapability> pending = new ArrayDeque<>();
    pending.push(capability);
    while (!pending.isEmpty()) {
      IndexPrecomputeCapability next = pending.pop();
      if (already.contains(next) || result.contains(next)) continue;
      IndexPrecomputeCandidate candidate = byCapability.get(next);
      if (candidate == null) throw new IllegalArgumentException("missing dependency candidate " + next);
      result.add(next);
      for (IndexPrecomputeCapability dependency : candidate.dependencies()) {
        pending.push(dependency);
      }
    }
  }

  private static long score(
      IndexPrecomputeCandidate candidate,
      IndexPrecomputeWorkload workload) {
    long benefit = saturatingMultiply(
        workload.weight(candidate.capability()), candidate.expectedReuseCount());
    long penalty = saturatingMultiply(
        workload.buildPenalty(), candidate.buildCostUnits());
    return saturatingAdd(benefit, -penalty);
  }

  private static List<IndexPrecomputeCandidate> normalize(
      List<IndexPrecomputeCandidate> candidates) {
    EnumMap<IndexPrecomputeCapability, IndexPrecomputeCandidate> unique =
        new EnumMap<>(IndexPrecomputeCapability.class);
    for (IndexPrecomputeCandidate candidate : candidates) {
      Objects.requireNonNull(candidate, "candidate");
      if (unique.putIfAbsent(candidate.capability(), candidate) != null) {
        throw new IllegalArgumentException("duplicate candidate " + candidate.capability());
      }
    }
    return unique.values().stream()
        .sorted(Comparator.comparingInt(candidate -> candidate.capability().ordinal()))
        .toList();
  }

  private static EnumMap<IndexPrecomputeCapability, IndexPrecomputeCandidate> byCapability(
      List<IndexPrecomputeCandidate> candidates) {
    EnumMap<IndexPrecomputeCapability, IndexPrecomputeCandidate> result =
        new EnumMap<>(IndexPrecomputeCapability.class);
    for (IndexPrecomputeCandidate candidate : candidates) {
      result.put(candidate.capability(), candidate);
    }
    return result;
  }

  private static void validateDependencies(List<IndexPrecomputeCandidate> candidates) {
    EnumMap<IndexPrecomputeCapability, IndexPrecomputeCandidate> by = byCapability(candidates);
    for (IndexPrecomputeCandidate candidate : candidates) {
      for (IndexPrecomputeCapability dependency : candidate.dependencies()) {
        if (!by.containsKey(dependency)) {
          throw new IllegalArgumentException(
              candidate.capability() + " depends on missing " + dependency);
        }
      }
    }
  }

  private static long saturatingMultiply(long left, long right) {
    try {
      return Math.multiplyExact(left, right);
    } catch (ArithmeticException overflow) {
      return (left ^ right) < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
  }

  private static long saturatingAdd(long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException overflow) {
      return right < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
  }

  private record Closure(
      EnumSet<IndexPrecomputeCapability> capabilities,
      long additionalBytes,
      long additionalScore) {}

  private static final class Search {
    private final List<IndexPrecomputeCandidate> candidates;
    private final long budget;
    private final IndexPrecomputeWorkload workload;
    private EnumSet<IndexPrecomputeCapability> bestCapabilities =
        EnumSet.noneOf(IndexPrecomputeCapability.class);
    private long bestBytes;
    private long bestScore = Long.MIN_VALUE;
    private boolean found;

    private Search(
        List<IndexPrecomputeCandidate> candidates,
        long budget,
        IndexPrecomputeWorkload workload) {
      this.candidates = candidates;
      this.budget = budget;
      this.workload = workload;
    }

    private void visit(
        int index,
        EnumSet<IndexPrecomputeCapability> chosen,
        long bytes,
        long score) {
      if (bytes > budget) return;
      if (index == candidates.size()) {
        if (!valid(chosen)) return;
        if (better(score, bytes, chosen)) {
          bestScore = score;
          bestBytes = bytes;
          bestCapabilities = EnumSet.copyOf(chosen);
          found = true;
        }
        return;
      }

      IndexPrecomputeCandidate candidate = candidates.get(index);
      if (!candidate.mandatory()) {
        visit(index + 1, chosen, bytes, score);
      }

      chosen.add(candidate.capability());
      visit(
          index + 1,
          chosen,
          Math.addExact(bytes, candidate.retainedBytes()),
          saturatingAdd(score, score(candidate, workload)));
      chosen.remove(candidate.capability());
    }

    private boolean valid(Set<IndexPrecomputeCapability> chosen) {
      for (IndexPrecomputeCandidate candidate : candidates) {
        if (!chosen.contains(candidate.capability())) {
          if (candidate.mandatory()) return false;
          continue;
        }
        if (!chosen.containsAll(candidate.dependencies())) return false;
      }
      return true;
    }

    private boolean better(
        long score,
        long bytes,
        Set<IndexPrecomputeCapability> chosen) {
      if (score != bestScore) return score > bestScore;
      if (bytes != bestBytes) return bytes < bestBytes;
      return chosen.toString().compareTo(bestCapabilities.toString()) < 0;
    }
  }
}
