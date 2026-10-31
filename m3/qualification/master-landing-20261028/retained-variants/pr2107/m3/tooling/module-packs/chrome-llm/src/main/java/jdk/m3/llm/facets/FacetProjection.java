/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/jdk/internal/m3/cpullm/facets/FacetProjection.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** One root contraction with explicit obligations. Greedy coverage is not an optimal knapsack solver. */
public final class FacetProjection {
    private FacetProjection() { }
    public enum Status { PASS, MISSING_FACETS, MANDATORY_OVER_BUDGET, INCOMPLETE_CONFLICT_SCOPE, GREEDY_COVERAGE_SHORTFALL }
    public record Request(long requiredFacets, int maxUtf8Bytes, int maxAtoms) {
        public Request {
            if (maxUtf8Bytes < 0 || maxUtf8Bytes > FacetCorpus.MAX_BYTES || maxAtoms < 0 || maxAtoms > FacetCorpus.MAX_ATOMS) {
                throw new IllegalArgumentException("projection budget bounds");
            }
        }
    }
    public record Result(Status status, String corpusRoot, String scopeRoot, Request request,
                         List<String> evidenceIds, String text, long coveredFacets,
                         int eligibleAtoms, int selectedAtoms, int utf8Bytes, String receipt) {
        public Result { evidenceIds = List.copyOf(evidenceIds); }
    }
    public static Result select(FacetNode node, Request request, FacetKernels kernels) {
        Objects.requireNonNull(node, "node"); Objects.requireNonNull(request, "request"); Objects.requireNonNull(kernels, "kernels");
        FacetCorpus.cancelled(); FacetCorpus corpus = node.corpus(); corpus.requireMask(request.requiredFacets());
        int[] counts = new int[corpus.groups.length];
        for (int row : node.internalRows()) counts[corpus.rowGroup[row]]++;
        long present = 0; int mandatoryBytes = 0, mandatoryAtoms = 0;
        boolean[] picked = new boolean[counts.length];
        for (int group = 0; group < counts.length; group++) {
            if (counts[group] != 0 && counts[group] != corpus.groups[group].length) {
                return result(node, request, Status.INCOMPLETE_CONFLICT_SCOPE, picked, 0);
            }
            if (counts[group] == 0) continue;
            present |= corpus.groupFacets[group];
            if (corpus.groupMandatory[group]) {
                mandatoryBytes += corpus.groupBytes[group]; mandatoryAtoms += counts[group];
            }
        }
        if ((request.requiredFacets() & ~present) != 0) return result(node, request, Status.MISSING_FACETS, picked, 0);
        if (mandatoryBytes > request.maxUtf8Bytes() || mandatoryAtoms > request.maxAtoms()) {
            return result(node, request, Status.MANDATORY_OVER_BUDGET, picked, 0);
        }
        int bytes = 0, atoms = 0; long covered = 0;
        for (int group = 0; group < counts.length; group++) if (counts[group] != 0 && corpus.groupMandatory[group]) {
            picked[group] = true; bytes += corpus.groupBytes[group]; atoms += counts[group]; covered |= corpus.groupFacets[group];
        }
        while ((request.requiredFacets() & ~covered) != 0) {
            FacetCorpus.cancelled();
            int[] gains = kernels.gains(corpus.groupFacets, request.requiredFacets() & ~covered);
            int best = -1;
            for (int group = 0; group < counts.length; group++) {
                if (picked[group] || counts[group] == 0 || gains[group] == 0
                        || corpus.groupBytes[group] > request.maxUtf8Bytes() - bytes || counts[group] > request.maxAtoms() - atoms) continue;
                if (best < 0 || better(group, best, gains, corpus)) best = group;
            }
            if (best < 0) return result(node, request, Status.GREEDY_COVERAGE_SHORTFALL, picked, covered);
            picked[best] = true; bytes += corpus.groupBytes[best]; atoms += counts[best]; covered |= corpus.groupFacets[best];
        }
        // Sort once for budget filling; no repeated O(N^2) maximum search.
        Integer[] order = new Integer[counts.length]; for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            int c = Integer.compare(corpus.groupPriority[b], corpus.groupPriority[a]);
            if (c == 0) c = Integer.compare(corpus.groupBytes[a], corpus.groupBytes[b]);
            return c == 0 ? Integer.compare(corpus.groups[a][0], corpus.groups[b][0]) : c;
        });
        for (int group : order) {
            FacetCorpus.cancelled();
            if (!picked[group] && counts[group] != 0 && corpus.groupBytes[group] <= request.maxUtf8Bytes() - bytes
                    && counts[group] <= request.maxAtoms() - atoms) {
                picked[group] = true; bytes += corpus.groupBytes[group]; atoms += counts[group]; covered |= corpus.groupFacets[group];
            }
        }
        return result(node, request, Status.PASS, picked, covered);
    }
    private static boolean better(int a, int b, int[] gains, FacetCorpus corpus) {
        long lhs = (long) gains[a] * corpus.groupBytes[b], rhs = (long) gains[b] * corpus.groupBytes[a];
        if (lhs != rhs) return lhs > rhs;
        if (corpus.groupPriority[a] != corpus.groupPriority[b]) return corpus.groupPriority[a] > corpus.groupPriority[b];
        return corpus.groups[a][0] < corpus.groups[b][0];
    }
    private static Result result(FacetNode node, Request request, Status status, boolean[] selected, long covered) {
        FacetCorpus corpus = node.corpus(); List<String> ids = new ArrayList<>(); StringBuilder text = new StringBuilder();
        for (int row : node.internalRows()) if (selected[corpus.rowGroup[row]]) { ids.add(corpus.evidenceId(row)); text.append(corpus.rendered(row)); }
        String rendered = text.toString(); int bytes = FacetCorpus.utf8(rendered).length;
        var hash = new FacetCorpus.Seal("SX-FACET-PROJECTION-1").text(status.name()).text(corpus.root()).text(node.semanticRoot())
                .number(request.requiredFacets()).number(request.maxUtf8Bytes()).number(request.maxAtoms()).number(covered).number(ids.size());
        ids.forEach(hash::text); hash.text(rendered);
        return new Result(status, corpus.root(), node.semanticRoot(), request, ids, rendered, covered, node.internalRows().length, ids.size(), bytes, hash.finish());
    }
    /** Recomputes selection through the Java implementation; a rehashed fabricated output is insufficient. */
    public static void verify(FacetNode node, Result result) {
        Objects.requireNonNull(result, "result");
        if (!select(node, result.request(), FacetKernels.javaOnly()).equals(result)) throw new IllegalArgumentException("projection reconstruction differs");
    }
}
