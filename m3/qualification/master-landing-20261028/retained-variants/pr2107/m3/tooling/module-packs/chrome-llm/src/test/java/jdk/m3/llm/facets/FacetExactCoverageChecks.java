/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/facet-release/tests/FacetExactCoverageChecks.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent exhaustive subset oracle over bounded, synthetic source-backed facet instances. */
public final class FacetExactCoverageChecks {
    private static int checks;
    private FacetExactCoverageChecks() { }
    private static void yes(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static FacetCorpus sample(int[] masks, int[] lengths, boolean[] mandatory,
                                      String[] conflicts) {
        var sources = new ArrayList<FacetCorpus.Source>();
        var slices = new ArrayList<FacetCorpus.Slice>();
        for (int i = 0; i < masks.length; i++) {
            String source = "s" + i;
            byte[] raw = ("x".repeat(lengths[i])).getBytes(StandardCharsets.UTF_8);
            sources.add(new FacetCorpus.Source(source, raw));
            slices.add(new FacetCorpus.Slice(source, 0, raw.length, masks[i], masks.length - i,
                    mandatory[i], conflicts[i]));
        }
        return new FacetCorpus(sources, List.of("a", "b", "c", "d"), slices, 262_144);
    }
    private static boolean oracle(FacetCorpus corpus, long wanted, int maxBytes, int maxAtoms) {
        int n = corpus.groups.length;
        for (int subset = 0; subset < (1 << n); subset++) {
            long mask = 0;
            int bytes = 0, atoms = 0;
            boolean valid = true;
            for (int g = 0; g < n; g++) {
                if ((subset & (1 << g)) != 0) {
                    mask |= corpus.groupFacets[g];
                    bytes += corpus.groupBytes[g];
                    atoms += corpus.groups[g].length;
                } else if (corpus.groupMandatory[g]) valid = false;
            }
            if (valid && bytes <= maxBytes && atoms <= maxAtoms && (mask & wanted) == wanted) return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) throw new IllegalArgumentException("usage: [absolute-jni-library sha256]");
        FacetKernels kernel = args.length == 2 ? FacetKernels.pinnedJni(Path.of(args[0]), args[1]) : FacetKernels.javaOnly();
        var limit = new FacetExactCoverage.SearchLimit(20, 200_000);
        // A high-gain greedy pick leaves an uncovered facet; two smaller groups fit.
        var trap = sample(new int[]{7, 3, 12}, new int[]{40, 10, 10},
                new boolean[3], new String[]{"", "", ""});
        int[] costs = trap.groupBytes.clone();
        java.util.Arrays.sort(costs);
        int budget = costs[0] + costs[1];
        var request = new FacetProjection.Request(15L, budget, 2);
        yes(FacetProjection.select(FacetNode.all(trap), request, kernel).status()
                == FacetProjection.Status.GREEDY_COVERAGE_SHORTFALL, "greedy counterexample");
        var better = FacetExactCoverage.solve(FacetNode.all(trap), request, kernel, limit);
        yes(better.status() == FacetExactCoverage.Status.FEASIBLE && better.evidenceIds().size() == 2,
                "exact solver finds alternate cover");
        FacetExactCoverage.verify(FacetNode.all(trap), better);
        yes(better.renderedUtf8Bytes() <= budget && better.coveredFacets() == 15L, "budget + all facet bits");

        var incomplete = sample(new int[]{1, 2}, new int[]{5, 5}, new boolean[2],
                new String[]{"contradiction", "contradiction"});
        var incompleteProof = FacetExactCoverage.solve(FacetNode.leaf(incomplete, 0),
                new FacetProjection.Request(1L, 1000, 2), kernel, limit);
        yes(incompleteProof.status() == FacetExactCoverage.Status.INCOMPLETE_CONFLICT_SCOPE,
                "cannot select half a declared conflict group");
        var empty = new FacetCorpus(List.of(), List.of(), List.of(), 0);
        yes(FacetExactCoverage.solve(FacetNode.all(empty),
                new FacetProjection.Request(0L, 0, 0), kernel, limit).status()
                == FacetExactCoverage.Status.FEASIBLE, "empty corpus");


        // Strict source ranges cover ASCII, a two-byte character and a four-byte
        // supplementary code point. A byte-cost cache must preserve every receipt.
        byte[] unicodeBytes = "aé🚀".getBytes(StandardCharsets.UTF_8);
        var unicodeCorpus = new FacetCorpus(
                List.of(new FacetCorpus.Source("unicode", unicodeBytes)),
                List.of("ascii", "accent", "astral"),
                List.of(new FacetCorpus.Slice("unicode", 0, 1, 1L, 9, false, ""),
                        new FacetCorpus.Slice("unicode", 1, 3, 2L, 8, false, ""),
                        new FacetCorpus.Slice("unicode", 3, 7, 4L, 7, false, "")), 4096);
        int unicodeRenderedBytes = 0;
        for (int row = 0; row < unicodeCorpus.size(); row++) {
            int encoded = unicodeCorpus.rendered(row).getBytes(StandardCharsets.UTF_8).length;
            yes(unicodeCorpus.renderedBytes(row) == encoded, "UTF-8 row parity " + row);
            for (int round = 0; round < 16; round++) {
                yes(unicodeCorpus.renderedBytes(row) == encoded, "UTF-8 cached cost stable");
            }
            unicodeRenderedBytes = Math.addExact(unicodeRenderedBytes, encoded);
        }
        for (int group = 0; group < unicodeCorpus.groups.length; group++) {
            int expected = 0;
            for (int row : unicodeCorpus.groups[group]) {
                expected = Math.addExact(expected,
                        unicodeCorpus.rendered(row).getBytes(StandardCharsets.UTF_8).length);
            }
            yes(unicodeCorpus.groupBytes[group] == expected, "UTF-8 group cost");
        }
        var unicodeNode = FacetNode.all(unicodeCorpus);
        var unicodeRequest = new FacetProjection.Request(7L, unicodeRenderedBytes, 3);
        var unicodeProjection = FacetProjection.select(unicodeNode, unicodeRequest, kernel);
        yes(unicodeProjection.status() == FacetProjection.Status.PASS
                && unicodeProjection.evidenceIds().size() == 3
                && unicodeProjection.utf8Bytes() == unicodeRenderedBytes,
                "source-preserving Unicode facet projection");
        yes(unicodeProjection.utf8Bytes()
                == unicodeProjection.text().getBytes(StandardCharsets.UTF_8).length,
                "joined UTF-8 text matches group budgets");
        var unicodeExact = FacetExactCoverage.solve(unicodeNode, unicodeRequest, kernel, limit);
        yes(unicodeExact.status() == FacetExactCoverage.Status.FEASIBLE
                && unicodeExact.renderedUtf8Bytes() == unicodeRenderedBytes,
                "exact Java/JNI Unicode full coverage");
        FacetExactCoverage.verify(unicodeNode, unicodeExact);
        var oneByteShort = FacetExactCoverage.solve(unicodeNode,
                new FacetProjection.Request(7L, unicodeRenderedBytes - 1, 3), kernel, limit);
        yes(oneByteShort.status() == FacetExactCoverage.Status.PROVEN_INFEASIBLE,
                "one rendered UTF-8 byte short is genuinely infeasible");
        FacetExactCoverage.verify(unicodeNode, oneByteShort);

        Random random = new Random(3317L);
        for (int iteration = 0; iteration < 300; iteration++) {
            int n = 1 + random.nextInt(8);
            int[] masks = new int[n], lengths = new int[n];
            boolean[] mandatory = new boolean[n];
            String[] groups = new String[n];
            for (int i = 0; i < n; i++) {
                masks[i] = 1 + random.nextInt(15);
                lengths[i] = 2 + random.nextInt(30);
                mandatory[i] = random.nextInt(9) == 0;
                groups[i] = "";
            }
            var corpus = sample(masks, lengths, mandatory, groups);
            long required = random.nextInt(16);
            int byteBudget = random.nextInt(corpus.retainedUtf8PayloadBytes() + 1);
            int atomBudget = random.nextInt(n + 1);
            var req = new FacetProjection.Request(required, byteBudget, atomBudget);
            var root = FacetNode.all(corpus);
            var got = FacetExactCoverage.solve(root, req, kernel, limit);
            boolean can = oracle(corpus, required, byteBudget, atomBudget);
            yes((got.status() == FacetExactCoverage.Status.FEASIBLE) == can,
                    "exact feasibility vs independent exhaustive oracle " + iteration);
            FacetExactCoverage.verify(root, got);
            if (can) {
                yes(got.renderedUtf8Bytes() <= byteBudget && got.evidenceIds().size() <= atomBudget,
                        "feasible witness within both budgets " + iteration);
                yes((got.coveredFacets() & required) == required,
                        "feasible witness covers all obligations " + iteration);
            } else {
                yes(got.status() != FacetExactCoverage.Status.SEARCH_LIMIT_REACHED,
                        "small exhaustive search cannot claim budget limit " + iteration);
            }
        }
        var dominated = sample(new int[]{1, 1, 1, 1}, new int[]{2, 3, 4, 5},
                new boolean[4], new String[]{"", "", "", ""});
        var dominatedProof = FacetExactCoverage.solve(FacetNode.all(dominated),
                new FacetProjection.Request(1, 4096, 4), kernel,
                new FacetExactCoverage.SearchLimit(2, 100));
        yes(dominatedProof.status() == FacetExactCoverage.Status.FEASIBLE
                && dominatedProof.evidenceIds().size() == 1, "dominated equivalent mask reduction");
        FacetExactCoverage.verify(FacetNode.all(dominated), dominatedProof);
        var many = sample(new int[]{1, 2, 4, 8}, new int[]{2, 2, 2, 2},
                new boolean[4], new String[]{"", "", "", ""});
        yes(FacetExactCoverage.solve(FacetNode.all(many),
                new FacetProjection.Request(15, 4096, 4), kernel,
                new FacetExactCoverage.SearchLimit(2, 100)).status()
                == FacetExactCoverage.Status.TOO_MANY_GROUPS, "bounded incomparable candidates");
        // Include sign bit: 64 named facets, bit 63 is valid even as a negative long.
        var names = new ArrayList<String>();
        for (int i = 0; i < 64; i++) names.add("bit" + i);
        var wide = new FacetCorpus(List.of(new FacetCorpus.Source("high", "high-bit".getBytes(StandardCharsets.UTF_8))),
                names, List.of(new FacetCorpus.Slice("high", 0, 8, Long.MIN_VALUE, 99, false, "")), 4096);
        var high = FacetExactCoverage.solve(FacetNode.all(wide),
                new FacetProjection.Request(Long.MIN_VALUE, 4096, 1), kernel, limit);
        yes(high.status() == FacetExactCoverage.Status.FEASIBLE
                && high.coveredFacets() == Long.MIN_VALUE, "highest legal facet bit");
        FacetExactCoverage.verify(FacetNode.all(wide), high);
        var missing = FacetExactCoverage.solve(FacetNode.all(wide),
                new FacetProjection.Request(1L, 4096, 1), kernel, limit);
        yes(missing.status() == FacetExactCoverage.Status.MISSING_FACETS, "missing required coverage");
        int[] repeatedMasks = new int[800], repeatedLengths = new int[800];
        String[] repeatedGroups = new String[800];
        java.util.Arrays.fill(repeatedMasks, 1);
        java.util.Arrays.fill(repeatedLengths, 3);
        java.util.Arrays.fill(repeatedGroups, "");
        var repeated = sample(repeatedMasks, repeatedLengths, new boolean[800], repeatedGroups);
        var repeatedProof = FacetExactCoverage.solve(FacetNode.all(repeated),
                new FacetProjection.Request(1, 4096, 1), kernel,
                new FacetExactCoverage.SearchLimit(2, 100));
        yes(repeatedProof.status() == FacetExactCoverage.Status.FEASIBLE
                && repeatedProof.evidenceIds().size() == 1,
                "large equivalent mask cohort pruned before exponential search");
        FacetExactCoverage.verify(FacetNode.all(repeated), repeatedProof);
        var mandatory = sample(new int[]{1, 2}, new int[]{5, 5},
                new boolean[]{true, false}, new String[]{"", ""});
        yes(FacetExactCoverage.solve(FacetNode.all(mandatory),
                new FacetProjection.Request(3, 4096, 0), kernel, limit).status()
                == FacetExactCoverage.Status.MANDATORY_OVER_BUDGET,
                "mandatory group may not be dropped to fit budget");
        var scarce = sample(new int[]{1, 2}, new int[]{7, 7},
                new boolean[2], new String[]{"", ""});
        var proven = FacetExactCoverage.solve(FacetNode.all(scarce),
                new FacetProjection.Request(3, 4096, 1), kernel, limit);
        yes(proven.status() == FacetExactCoverage.Status.PROVEN_INFEASIBLE && proven.proved(),
                "exhausted exact search certifies infeasible bounded scope");
        var slow = FacetExactCoverage.solve(FacetNode.all(scarce),
                new FacetProjection.Request(3, 4096, 1), kernel,
                new FacetExactCoverage.SearchLimit(2, 1));
        yes(slow.status() == FacetExactCoverage.Status.SEARCH_LIMIT_REACHED
                && !slow.proved(), "budget exhaustion never proves impossibility");
        var forged = new FacetExactCoverage.Result(better.status(), better.corpusRoot(),
                better.scopeRoot(), better.request(), better.limit(), List.of("forged"),
                better.coveredFacets(), better.renderedUtf8Bytes(), better.visitedStates(),
                better.receipt());
        boolean refused = false;
        try { FacetExactCoverage.verify(FacetNode.all(trap), forged); }
        catch (IllegalArgumentException expected) { refused = true; }
        yes(refused, "fabricated witness refused by independent Java replay");
        yes(kernel.calls() > 0, "actual gain kernel exercised");
        System.out.println("FACET_EXACT_COVER_PASS checks=" + checks + " kernel=" + kernel.implementation());
    }
}
