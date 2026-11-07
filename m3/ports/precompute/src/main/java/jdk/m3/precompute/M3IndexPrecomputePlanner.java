// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.math.BigInteger;
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
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputePlanner.java blob 18b16bda6e3b; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputePlanner {
  public static final int EXACT_LIMIT = 20;

  public M3IndexPrecomputeRecommendation recommend(
      List<M3IndexPrecomputeCandidate> candidates,
      long byteBudget,
      M3IndexPrecomputeWorkload workload) {
    Objects.requireNonNull(candidates, "candidates");
    Objects.requireNonNull(workload, "workload");
    if (byteBudget < 0) throw new IllegalArgumentException("negative byteBudget");

    List<M3IndexPrecomputeCandidate> normalized = normalize(candidates);
    validateDependencies(normalized);
    return normalized.size() <= EXACT_LIMIT
        ? exact(normalized, byteBudget, workload)
        : greedy(normalized, byteBudget, workload);
  }

  private M3IndexPrecomputeRecommendation exact(
      List<M3IndexPrecomputeCandidate> candidates,
      long budget,
      M3IndexPrecomputeWorkload workload) {
    Search search = new Search(candidates, budget, workload);
    search.visit(0, EnumSet.noneOf(M3IndexPrecomputeCapability.class), 0L, 0L);
    if (!search.found) {
      throw new IllegalStateException("no valid precompute subset fits the byte budget");
    }
    return new M3IndexPrecomputeRecommendation(
        search.bestCapabilities, search.bestBytes, search.bestScore, true);
  }

  private M3IndexPrecomputeRecommendation greedy(
      List<M3IndexPrecomputeCandidate> candidates,
      long budget,
      M3IndexPrecomputeWorkload workload) {
    EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> byCapability =
        byCapability(candidates);
    EnumSet<M3IndexPrecomputeCapability> chosen =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);
    long bytes = 0;
    long score = 0;

    for (M3IndexPrecomputeCandidate candidate : candidates) {
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
      for (M3IndexPrecomputeCandidate candidate : candidates) {
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

    return new M3IndexPrecomputeRecommendation(chosen, bytes, score, false);
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
      M3IndexPrecomputeCapability capability,
      Map<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> byCapability,
      Set<M3IndexPrecomputeCapability> already,
      M3IndexPrecomputeWorkload workload) {
    EnumSet<M3IndexPrecomputeCapability> result =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);
    collect(capability, byCapability, already, result);
    long bytes = 0;
    long score = 0;
    for (M3IndexPrecomputeCapability item : result) {
      M3IndexPrecomputeCandidate candidate = byCapability.get(item);
      bytes = Math.addExact(bytes, candidate.retainedBytes());
      score = saturatingAdd(score, score(candidate, workload));
    }
    return new Closure(result, bytes, score);
  }

  private static void collect(
      M3IndexPrecomputeCapability capability,
      Map<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> byCapability,
      Set<M3IndexPrecomputeCapability> already,
      EnumSet<M3IndexPrecomputeCapability> result) {
    if (already.contains(capability) || result.contains(capability)) return;
    M3IndexPrecomputeCandidate candidate = byCapability.get(capability);
    if (candidate == null) throw new IllegalArgumentException("missing dependency candidate " + capability);
    for (M3IndexPrecomputeCapability dependency : candidate.dependencies()) {
      collect(dependency, byCapability, already, result);
    }
    result.add(capability);
  }

  private static long score(
      M3IndexPrecomputeCandidate candidate,
      M3IndexPrecomputeWorkload workload) {
    long benefit = saturatingMultiply(
        workload.weight(candidate.capability()), candidate.expectedReuseCount());
    long penalty = saturatingMultiply(
        workload.buildPenalty(), candidate.buildCostUnits());
    return saturatingAdd(benefit, -penalty);
  }

  private static List<M3IndexPrecomputeCandidate> normalize(
      List<M3IndexPrecomputeCandidate> candidates) {
    EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> unique =
        new EnumMap<>(M3IndexPrecomputeCapability.class);
    for (M3IndexPrecomputeCandidate candidate : candidates) {
      Objects.requireNonNull(candidate, "candidate");
      if (unique.putIfAbsent(candidate.capability(), candidate) != null) {
        throw new IllegalArgumentException("duplicate candidate " + candidate.capability());
      }
    }
    return unique.values().stream()
        .sorted(Comparator.comparingInt(candidate -> candidate.capability().ordinal()))
        .toList();
  }

  private static EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> byCapability(
      List<M3IndexPrecomputeCandidate> candidates) {
    EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> result =
        new EnumMap<>(M3IndexPrecomputeCapability.class);
    for (M3IndexPrecomputeCandidate candidate : candidates) {
      result.put(candidate.capability(), candidate);
    }
    return result;
  }

  private static void validateDependencies(List<M3IndexPrecomputeCandidate> candidates) {
    EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeCandidate> by = byCapability(candidates);
    for (M3IndexPrecomputeCandidate candidate : candidates) {
      for (M3IndexPrecomputeCapability dependency : candidate.dependencies()) {
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
      EnumSet<M3IndexPrecomputeCapability> capabilities,
      long additionalBytes,
      long additionalScore) {}

  private static final class Search {
    private final List<M3IndexPrecomputeCandidate> candidates;
    private final long budget;
    private final M3IndexPrecomputeWorkload workload;
    private EnumSet<M3IndexPrecomputeCapability> bestCapabilities =
        EnumSet.noneOf(M3IndexPrecomputeCapability.class);
    private long bestBytes;
    private long bestScore = Long.MIN_VALUE;
    private boolean found;

    private Search(
        List<M3IndexPrecomputeCandidate> candidates,
        long budget,
        M3IndexPrecomputeWorkload workload) {
      this.candidates = candidates;
      this.budget = budget;
      this.workload = workload;
    }

    private void visit(
        int index,
        EnumSet<M3IndexPrecomputeCapability> chosen,
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

      M3IndexPrecomputeCandidate candidate = candidates.get(index);
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

    private boolean valid(Set<M3IndexPrecomputeCapability> chosen) {
      for (M3IndexPrecomputeCandidate candidate : candidates) {
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
        Set<M3IndexPrecomputeCapability> chosen) {
      if (score != bestScore) return score > bestScore;
      if (bytes != bestBytes) return bytes < bestBytes;
      return chosen.toString().compareTo(bestCapabilities.toString()) < 0;
    }
  }
}
