/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/jdk/internal/m3/cpullm/facets/FacetExactCoverage.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Optional, bounded exact feasibility search over the existing source-backed facet groups.
 * It neither rewrites evidence nor changes FacetProjection's established greedy semantics.
 * Exhaustive failure proves infeasibility only inside the admitted immutable scope/budgets.
 */
public final class FacetExactCoverage {
    private FacetExactCoverage() { }

    public enum Status {
        FEASIBLE, PROVEN_INFEASIBLE, SEARCH_LIMIT_REACHED, TOO_MANY_GROUPS,
        MISSING_FACETS, INCOMPLETE_CONFLICT_SCOPE, MANDATORY_OVER_BUDGET
    }

    /** Limits are per explicit request; no exponential search runs implicitly. */
    public record SearchLimit(int maxOptionalGroups, int maxStates) {
        public SearchLimit {
            if (maxOptionalGroups < 1 || maxOptionalGroups > 32 || maxStates < 1 || maxStates > 1_000_000) {
                throw new IllegalArgumentException("exact coverage search bounds");
            }
        }
        public static SearchLimit defaults() { return new SearchLimit(20, 200_000); }
    }

    public record Result(Status status, String corpusRoot, String scopeRoot,
                         FacetProjection.Request request, SearchLimit limit,
                         List<String> evidenceIds, long coveredFacets,
                         int renderedUtf8Bytes, long visitedStates, String receipt) {
        public Result {
            Objects.requireNonNull(status, "status");
            evidenceIds = List.copyOf(evidenceIds);
        }
        public boolean proved() { return status == Status.FEASIBLE || status == Status.PROVEN_INFEASIBLE; }
    }

    public static Result solve(FacetNode node, FacetProjection.Request request,
                               FacetKernels kernels, SearchLimit limit) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(kernels, "kernels");
        Objects.requireNonNull(limit, "limit");
        FacetCorpus.cancelled();
        FacetCorpus corpus = node.corpus();
        corpus.requireMask(request.requiredFacets());
        int[] counts = new int[corpus.groups.length];
        for (int row : node.internalRows()) counts[corpus.rowGroup[row]]++;
        long present = 0L;
        long mandatoryFacets = 0L;
        int mandatoryBytes = 0;
        int mandatoryAtoms = 0;
        boolean[] selected = new boolean[counts.length];
        for (int group = 0; group < counts.length; group++) {
            if ((group & 255) == 0) FacetCorpus.cancelled();
            if (counts[group] != 0 && counts[group] != corpus.groups[group].length) {
                return result(node, request, limit, Status.INCOMPLETE_CONFLICT_SCOPE, null, 0, 0L);
            }
            if (counts[group] == 0) continue;
            present |= corpus.groupFacets[group];
            if (corpus.groupMandatory[group]) {
                selected[group] = true;
                mandatoryFacets |= corpus.groupFacets[group];
                mandatoryBytes = Math.addExact(mandatoryBytes, corpus.groupBytes[group]);
                mandatoryAtoms = Math.addExact(mandatoryAtoms, counts[group]);
            }
        }
        if ((request.requiredFacets() & ~present) != 0L) {
            return result(node, request, limit, Status.MISSING_FACETS, null, 0, 0L);
        }
        if (mandatoryBytes > request.maxUtf8Bytes() || mandatoryAtoms > request.maxAtoms()) {
            return result(node, request, limit, Status.MANDATORY_OVER_BUDGET, null, 0, 0L);
        }
        long missing = request.requiredFacets() & ~mandatoryFacets;
        if (missing == 0L) return result(node, request, limit, Status.FEASIBLE, selected, 1, mandatoryFacets);

        List<Integer> candidates = new ArrayList<>();
        int freeBytes = request.maxUtf8Bytes() - mandatoryBytes;
        int freeAtoms = request.maxAtoms() - mandatoryAtoms;
        for (int group = 0; group < counts.length; group++) {
            if (counts[group] > 0 && !selected[group]
                    && (corpus.groupFacets[group] & missing) != 0L
                    && corpus.groupBytes[group] <= freeBytes && counts[group] <= freeAtoms) {
                candidates.add(group);
            }
        }
        // Feasibility is independent of candidate priority. A group whose remaining
        // facet mask is contained in another group with no larger byte/atom cost
        // can never be needed in an existence proof. Drop such groups without
        // dropping their original evidence from FacetCorpus.
        List<Integer> pruned = removeDominated(candidates, corpus, counts, missing,
                Math.min(512, Math.max(16, limit.maxOptionalGroups() * 8)));
        if (pruned.size() > limit.maxOptionalGroups()) {
            return result(node, request, limit, Status.TOO_MANY_GROUPS, null, 0, 0L);
        }
        List<Integer> orderedCandidates = pruned;
        long[] masks = new long[orderedCandidates.size()];
        for (int i = 0; i < masks.length; i++) masks[i] = corpus.groupFacets[orderedCandidates.get(i)];
        int[] gains = kernels.gains(masks, missing);
        if (gains.length != masks.length) throw new IllegalStateException("facet kernel gain length mismatch");
        Integer[] ordering = new Integer[orderedCandidates.size()];
        for (int i = 0; i < ordering.length; i++) ordering[i] = i;
        Arrays.sort(ordering, Comparator.<Integer>comparingLong(
                i -> -(long) gains[i] * 1_000_000 / Math.max(1, corpus.groupBytes[orderedCandidates.get(i)]))
                .thenComparing((a, b) -> Integer.compare(corpus.groupPriority[orderedCandidates.get(b)],
                        corpus.groupPriority[orderedCandidates.get(a)]))
                .thenComparingInt(orderedCandidates::get));
        int[] groups = new int[ordering.length];
        long[] suffix = new long[groups.length + 1];
        for (int i = 0; i < groups.length; i++) groups[i] = orderedCandidates.get(ordering[i]);
        for (int i = groups.length - 1; i >= 0; i--) suffix[i] = suffix[i + 1] | corpus.groupFacets[groups[i]];
        Search search = new Search(corpus, counts, groups, suffix, request, limit, selected);
        boolean found = search.visit(0, mandatoryFacets, mandatoryBytes, mandatoryAtoms);
        if (found) return result(node, request, limit, Status.FEASIBLE, selected, search.visited, search.covered);
        return result(node, request, limit,
                search.exhausted ? Status.SEARCH_LIMIT_REACHED : Status.PROVEN_INFEASIBLE,
                null, search.visited, 0L);
    }

    private static List<Integer> removeDominated(List<Integer> eligible, FacetCorpus corpus,
                                                  int[] counts, long missing, int maximumFrontier) {
        // One streaming Pareto frontier: O(N * F), where F is bounded by the
        // explicit search limit plus a small headroom, not an O(N^2) scan over
        // the whole 65,536-atom corpus. If the frontier grows too large,
        // preserve the existing TOO_MANY_GROUPS refusal rather than speculating.
        List<Integer> frontier = new ArrayList<>();
        for (int candidate : eligible) {
            FacetCorpus.cancelled();
            long mask = corpus.groupFacets[candidate] & missing;
            int bytes = corpus.groupBytes[candidate];
            int atoms = counts[candidate];
            boolean dominated = false;
            for (int existing : frontier) {
                long other = corpus.groupFacets[existing] & missing;
                if ((mask & ~other) == 0L && corpus.groupBytes[existing] <= bytes
                        && counts[existing] <= atoms) {
                    dominated = true;
                    break;
                }
            }
            if (dominated) continue;
            for (int i = frontier.size() - 1; i >= 0; i--) {
                int existing = frontier.get(i);
                long other = corpus.groupFacets[existing] & missing;
                if ((other & ~mask) == 0L && bytes <= corpus.groupBytes[existing]
                        && atoms <= counts[existing]) {
                    frontier.remove(i);
                }
            }
            frontier.add(candidate);
            if (frontier.size() > maximumFrontier) return frontier; // explicit unproven refusal
        }
        return frontier;
    }

    /** Replays with Java bit counts, independently of an optional JNI witness. */
    public static void verify(FacetNode node, Result candidate) {
        Objects.requireNonNull(candidate, "candidate");
        Result expected = solve(node, candidate.request(), FacetKernels.javaOnly(), candidate.limit());
        if (!expected.equals(candidate)) throw new IllegalArgumentException("exact facet coverage receipt differs");
    }

    private static Result result(FacetNode node, FacetProjection.Request request,
                                 SearchLimit limit, Status status, boolean[] selected,
                                 long visited, long covered) {
        FacetCorpus corpus = node.corpus();
        List<String> ids = new ArrayList<>();
        int bytes = 0;
        if (selected != null) {
            for (int row : node.internalRows()) {
                if (selected[corpus.rowGroup[row]]) {
                    ids.add(corpus.evidenceId(row));
                    bytes = Math.addExact(bytes, corpus.renderedBytes(row));
                }
            }
        }
        var seal = new FacetCorpus.Seal("SX-FACET-EXACT-COVER-1")
                .text(status.name()).text(corpus.root()).text(node.semanticRoot())
                .number(request.requiredFacets()).number(request.maxUtf8Bytes()).number(request.maxAtoms())
                .number(limit.maxOptionalGroups()).number(limit.maxStates())
                .number(covered).number(bytes).number(visited).number(ids.size());
        ids.forEach(seal::text);
        return new Result(status, corpus.root(), node.semanticRoot(), request, limit,
                ids, covered, bytes, visited, seal.finish());
    }

    private static final class Search {
        private final FacetCorpus corpus;
        private final int[] counts, groups;
        private final long[] suffix;
        private final FacetProjection.Request request;
        private final SearchLimit limit;
        private final boolean[] picked;
        private long visited, covered;
        private boolean exhausted;

        Search(FacetCorpus corpus, int[] counts, int[] groups, long[] suffix,
               FacetProjection.Request request, SearchLimit limit, boolean[] picked) {
            this.corpus = corpus;
            this.counts = counts;
            this.groups = groups;
            this.suffix = suffix;
            this.request = request;
            this.limit = limit;
            this.picked = picked;
        }
        boolean visit(int index, long mask, int usedBytes, int usedAtoms) {
            if ((visited & 255L) == 0L) FacetCorpus.cancelled();
            if (visited >= limit.maxStates()) { exhausted = true; return false; }
            visited++;
            if ((request.requiredFacets() & ~mask) == 0L) { covered = mask; return true; }
            if (index == groups.length || (request.requiredFacets() & ~(mask | suffix[index])) != 0L) return false;
            int group = groups[index];
            int bytes = corpus.groupBytes[group];
            int atoms = counts[group];
            if (bytes <= request.maxUtf8Bytes() - usedBytes && atoms <= request.maxAtoms() - usedAtoms) {
                picked[group] = true;
                if (visit(index + 1, mask | corpus.groupFacets[group], usedBytes + bytes, usedAtoms + atoms)) return true;
                picked[group] = false;
            }
            return visit(index + 1, mask, usedBytes, usedAtoms);
        }
    }
}
