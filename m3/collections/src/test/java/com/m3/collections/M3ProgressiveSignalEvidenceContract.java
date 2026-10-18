// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

/** Differential proof for progressive comparison and bounded caller-owned evidence reuse. */
public final class M3ProgressiveSignalEvidenceContract {
    private static int checks;
    private M3ProgressiveSignalEvidenceContract() {}

    public static void main(String[] args) {
        catalogAndGuards();
        firstDecisionAndFallback();
        reuseAndBitMasks();
        certifiedMetricBounds();
        invalidMetricCases();
        permutationOracle();
        System.out.println("M3_SIGNAL_EVIDENCE_ASSERTIONS=" + checks);
        System.out.println("RESULT=PASS");
    }

    private static M3ProgressiveSignalEvidence.Dimension dim(
            String name, M3ProgressiveSignalEvidence.Family family,
            boolean ordered, boolean exact, boolean descending,
            double weight, double maximumGap) {
        return new M3ProgressiveSignalEvidence.Dimension(name, family,
                M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME, "Example#" + name,
                ordered, exact, descending, weight, maximumGap);
    }

    private static void catalogAndGuards() {
        check(M3ProgressiveSignalEvidence.Family.values().length >= 18,
                "complete signal family catalogue");
        var desc = dim("package", M3ProgressiveSignalEvidence.Family.STRUCTURAL,
                true, true, false, 0.0, Double.NaN);
        check(desc.provenance() == M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME,
                "source-time provenance");
        illegal(() -> dim("", M3ProgressiveSignalEvidence.Family.DOMAIN,
                true, true, false, 0.0, Double.NaN), "blank name");
        illegal(() -> dim("hint", M3ProgressiveSignalEvidence.Family.RUNTIME,
                false, true, false, 0.0, Double.NaN), "exact must be ordered");
        illegal(() -> dim("negative", M3ProgressiveSignalEvidence.Family.DISTANCE,
                true, false, false, -1.0, Double.NaN), "negative metric weight");
        illegal(() -> dim("badGap", M3ProgressiveSignalEvidence.Family.DISTANCE,
                true, false, false, 2.0, Double.NEGATIVE_INFINITY), "invalid gap cap");
        illegal(() -> dim("nonmetric", M3ProgressiveSignalEvidence.Family.LEXICAL,
                true, true, false, 0.0, 1.0), "nonmetric maximum gap");
        illegal(() -> M3ProgressiveSignalEvidence.Plan.of(1, 0,
                new M3ProgressiveSignalEvidence.Dimension[] {desc, desc}, (d, r) -> 0, true),
                "duplicate dimension");
        illegal(() -> M3ProgressiveSignalEvidence.Plan.of(1, 0,
                new M3ProgressiveSignalEvidence.Dimension[] {dim("heuristic",
                        M3ProgressiveSignalEvidence.Family.SEMANTIC, true, false, false,
                        0.0, Double.NaN)}, (d, r) -> 0, true), "reject unproven exact schema");
        illegal(() -> M3ProgressiveSignalEvidence.Plan.of(-1, 0,
                new M3ProgressiveSignalEvidence.Dimension[] {desc}, (d, r) -> 0, false),
                "negative rows");
    }

    private static void firstDecisionAndFallback() {
        double[][] values = {{1, 2, 3}, {1, 7, 2}, {9, 9, 0}};
        var dimensions = new M3ProgressiveSignalEvidence.Dimension[] {
            dim("name", M3ProgressiveSignalEvidence.Family.LEXICAL,
                    true, true, false, 0, Double.NaN),
            dim("fields", M3ProgressiveSignalEvidence.Family.FIELDS,
                    true, true, true, 0, Double.NaN),
            dim("runtime", M3ProgressiveSignalEvidence.Family.RUNTIME,
                    false, false, false, 0, Double.NaN)
        };
        AtomicInteger reads = new AtomicInteger();
        var plan = M3ProgressiveSignalEvidence.Plan.of(3, 42, dimensions,
                (d, row) -> { reads.incrementAndGet(); return values[d][row]; }, true);
        var workspace = plan.workspace();
        workspace.begin(0, 1).refineUntilRankKnown();
        check(workspace.rankKnown(), "first decision determined");
        check(workspace.rankOrder() == -1, "first exact ordinal decides");
        check(workspace.exactOrderCertified(), "complete exact schema certifies rank");
        check(!workspace.evaluated(1) && !workspace.evaluated(2),
                "later dimensions not eagerly computed");
        check(reads.get() == 2, "read each operand exactly once");
        int exact = workspace.exactOrderOrVerify((left, right) -> {
            throw new AssertionError("canonical verifier should not run on complete exact schema");
        });
        check(exact == -1, "exact schema bypasses verifier");
        workspace.refine(2);
        check(reads.get() == 4 && workspace.evaluated(2), "later refinement preserves first fact");
        workspace.begin(0, 1).refine(2);
        check(reads.get() == 4, "same pair rebegin retains all dimensions");
        var snapshot = workspace.snapshot();
        check(snapshot.sourceVersion() == 42 && snapshot.exactOrderCertified(),
                "evidence bound to immutable source version");
        snapshot.evaluatedMask()[0] = 0;
        snapshot.separations()[0] = 99;
        check(snapshot.evaluated(0) && snapshot.separations()[0] != 99,
                "caller cannot mutate evidence snapshot");
        workspace.begin(2, 0);
        check(!workspace.evaluated(0), "explicit new pair invalidates old evidence");
        workspace.refine(2);
        check(!workspace.rankKnown(), "late fact cannot skip earlier lex dimensions");
        workspace.refineUntilRankKnown();
        check(workspace.rankKnown() && workspace.rankOrder() > 0,
                "early lex order overrides previously sampled late hint");

        var heuristic = new M3ProgressiveSignalEvidence.Dimension[] {
            dim("frequency", M3ProgressiveSignalEvidence.Family.STATISTICAL,
                    true, false, true, 0, Double.NaN)
        };
        var candidate = M3ProgressiveSignalEvidence.Plan.of(2, 1, heuristic,
                (d, r) -> r + 1.0, false).workspace().begin(0, 1);
        candidate.refineUntilRankKnown();
        check(candidate.rankKnown() && candidate.rankOrder() > 0,
                "heuristic rank can order candidate list");
        check(!candidate.exactOrderCertified(), "heuristic rank not authoritative");
        AtomicInteger verifies = new AtomicInteger();
        IntBinaryOperator real = (left, right) -> { verifies.incrementAndGet(); return -123; };
        check(candidate.exactOrderOrVerify(real) == -1, "canonical comparator wins");
        check(candidate.exactOrderOrVerify(real) == -1 && verifies.get() == 1,
                "verifier result reused for same pair");
        check(candidate.snapshot().order() == -1 && candidate.snapshot().canonicalVerifierUsed(),
                "snapshot records verified order instead of tentative rank");
    }

    private static void reuseAndBitMasks() {
        int dimensions = 141;
        var spec = new M3ProgressiveSignalEvidence.Dimension[dimensions];
        for (int d = 0; d < dimensions; d++) {
            spec[d] = dim("m" + d, M3ProgressiveSignalEvidence.Family.METHODS,
                    true, true, false, 0, Double.NaN);
        }
        AtomicInteger calls = new AtomicInteger();
        var w = M3ProgressiveSignalEvidence.Plan.of(2, 0, spec,
                (d, r) -> { calls.incrementAndGet(); return d < dimensions - 1 ? 2 : r; },
                true).workspace().begin(0, 1);
        w.refine(130);
        check(w.evaluated(130), "mask high word set");
        check(!w.rankKnown(), "out-of-order evaluation cannot claim lex proof");
        w.refine(130);
        check(calls.get() == 2, "repeated dimension is free");
        w.refineUntilRankKnown();
        check(w.rankKnown() && w.rankOrder() == -1,
                "140 tied dimensions then decisive dimension");
        check(calls.get() == dimensions * 2, "no redundant reads for cached dimension");
        var evidence = w.snapshot();
        check(evidence.evaluated(130) && evidence.evaluated(140) && evidence.evaluated(0),
                "multiword mask retains all facts");
        check(evidence.metricBounds().lowerAvailable() == false,
                "no invented domain metric");
    }

    private static void certifiedMetricBounds() {
        double[][] values = {{1, 5}, {4, -3}, {11, 8}};
        var spec = new M3ProgressiveSignalEvidence.Dimension[] {
            dim("name", M3ProgressiveSignalEvidence.Family.LEXICAL,
                    true, true, false, 0, Double.NaN),
            dim("x", M3ProgressiveSignalEvidence.Family.DISTANCE,
                    false, false, false, 2.0, 10.0),
            dim("y", M3ProgressiveSignalEvidence.Family.DISTANCE,
                    false, false, false, 3.0, 20.0)
        };
        var w = M3ProgressiveSignalEvidence.Plan.of(2, 11, spec,
                (d, r) -> values[d][r], false).workspace().begin(0, 1);
        var before = w.metricBounds();
        check(before.lowerAvailable() && before.upperAvailable() && !before.complete(),
                "declared maxima bound uncomputed metric lanes");
        check(before.lower() == 0.0 && before.upper() >= 80.0,
                "weighted bounds from declared coordinate ranges");
        w.refine(1);
        var part = w.metricBounds();
        check(part.lower() <= 14.0 && part.upper() >= 23.0,
                "partial bounds contain true weighted distance");
        check(!w.evaluated(2), "metric work remains lazy");
        w.refineMetric();
        var all = w.metricBounds();
        check(all.complete() && all.lower() <= 23.0 && all.upper() >= 23.0,
                "full exact weighted-L1 is enclosed");
        check(w.separationAt(1) == 7.0 && w.separationAt(2) == 3.0,
                "all magnitude evidence retained separately from order");
        check(!w.exactOrderCertified(), "incomplete schema does not certify exact object order");

        Random random = new Random(0x455A15EEDL);
        for (int round = 0; round < 1000; round++) {
            int dims = 1 + random.nextInt(12);
            double[][] lanes = new double[dims][2];
            var info = new M3ProgressiveSignalEvidence.Dimension[dims];
            BigDecimal precise = BigDecimal.ZERO;
            for (int d = 0; d < dims; d++) {
                lanes[d][0] = random.nextInt(10_000) - 5_000;
                lanes[d][1] = random.nextInt(10_000) - 5_000;
                double weight = 0.5 + random.nextInt(9) * 0.5;
                info[d] = dim("metric" + d, M3ProgressiveSignalEvidence.Family.DISTANCE,
                        false, false, false, weight, 10_000);
                BigDecimal gap = BigDecimal.valueOf(lanes[d][0])
                        .subtract(BigDecimal.valueOf(lanes[d][1])).abs();
                precise = precise.add(gap.multiply(BigDecimal.valueOf(weight)));
            }
            var tested = M3ProgressiveSignalEvidence.Plan.of(2, 8, info,
                    (d, row) -> lanes[d][row], false).workspace().begin(0, 1);
            int partialDimensions = random.nextInt(dims + 1);
            for (int d = 0; d < partialDimensions; d++) tested.refine(d);
            var partial = tested.metricBounds();
            check(BigDecimal.valueOf(partial.lower()).compareTo(precise) <= 0,
                    "conservative lower bound against exact decimal oracle");
            check(BigDecimal.valueOf(partial.upper()).compareTo(precise) >= 0,
                    "conservative upper bound against exact decimal oracle");
            tested.refineMetric();
            var complete = tested.metricBounds();
            check(complete.complete(), "all coordinate measurements complete");
            check(BigDecimal.valueOf(complete.lower()).compareTo(precise) <= 0,
                    "complete lower against decimal oracle");
            check(BigDecimal.valueOf(complete.upper()).compareTo(precise) >= 0,
                    "complete upper against decimal oracle");
        }
    }

    private static void invalidMetricCases() {
        var unknown = new M3ProgressiveSignalEvidence.Dimension[] {
            dim("unbounded", M3ProgressiveSignalEvidence.Family.DISTANCE,
                    false, false, false, 1.0, Double.NaN)
        };
        var w = M3ProgressiveSignalEvidence.Plan.of(2, 0, unknown,
                (d, r) -> r, false).workspace().begin(0, 1);
        check(w.metricBounds().lowerAvailable() && !w.metricBounds().upperAvailable(),
                "unknown maximum cannot give finite upper bound");
        w.refineMetric();
        check(w.metricBounds().complete() && w.metricBounds().upperAvailable(),
                "computed unbounded lane now has finite total");
        var bad = new M3ProgressiveSignalEvidence.Dimension[] {
            dim("falseCertificate", M3ProgressiveSignalEvidence.Family.DISTANCE,
                    false, false, false, 1.0, 0.5)
        };
        var malformed = M3ProgressiveSignalEvidence.Plan.of(2, 0, bad,
                (d, r) -> r * 2.0, false).workspace().begin(0, 1);
        stateError(() -> malformed.refine(0), "false distance certificate must fail closed");
        check(!malformed.evaluated(0), "failed certificate never commits a misleading fact");
        var nonfinite = M3ProgressiveSignalEvidence.Plan.of(2, 0, unknown,
                (d, r) -> r == 0 ? Double.MAX_VALUE : -Double.MAX_VALUE, false)
                .workspace().begin(0, 1);
        nonfinite.refineMetric();
        check(!nonfinite.metricBounds().upperAvailable()
                && !nonfinite.metricBounds().lowerAvailable(),
                "overflow distance cannot be fabricated");
        var signedZero = M3ProgressiveSignalEvidence.Plan.of(2, 0,
                new M3ProgressiveSignalEvidence.Dimension[] {
                    dim("zero", M3ProgressiveSignalEvidence.Family.DOMAIN,
                            true, true, false, 0, Double.NaN)
                }, (d, r) -> r == 0 ? -0.0 : +0.0, true).workspace().begin(0, 1);
        signedZero.refineUntilRankKnown();
        check(signedZero.rankOrder() == -1 && signedZero.separationAt(0) == 0.0,
                "signed zero order distinct from zero geometric separation");
        var nan = M3ProgressiveSignalEvidence.Plan.of(2, 0,
                new M3ProgressiveSignalEvidence.Dimension[] {
                    dim("nan", M3ProgressiveSignalEvidence.Family.DOMAIN,
                            true, true, false, 1, Double.NaN)
                }, (d, r) -> r == 0 ? Double.NaN : 1.0, true).workspace().begin(0, 1);
        nan.refineUntilRankKnown();
        check(nan.rankOrder() > 0 && !nan.metricBounds().lowerAvailable(),
                "NaN sorted using Double.compare but never treated as numeric distance");
    }

    private static void permutationOracle() {
        Random random = new Random(0xA11C1E5EL);
        for (int round = 0; round < 450; round++) {
            int rows = 2 + random.nextInt(32);
            int dims = 1 + random.nextInt(12);
            double[][] lanes = new double[dims][rows];
            var descriptor = new M3ProgressiveSignalEvidence.Dimension[dims];
            for (int d = 0; d < dims; d++) {
                descriptor[d] = dim("name" + d, M3ProgressiveSignalEvidence.Family.METHODS,
                        true, true, (d & 1) == 0, 0, Double.NaN);
                for (int row = 0; row < rows; row++) {
                    lanes[d][row] = row % 7 == 0 ? Double.longBitsToDouble(random.nextLong())
                            : random.nextInt(5) - 2;
                }
            }
            var plan = M3ProgressiveSignalEvidence.Plan.of(rows, round, descriptor,
                    (d, r) -> lanes[d][r], true);
            var w = plan.workspace();
            for (int trial = 0; trial < 30; trial++) {
                int left = random.nextInt(rows);
                int right = random.nextInt(rows);
                w.begin(left, right).refineUntilRankKnown();
                int expected = 0;
                for (int d = 0; d < dims; d++) {
                    int sign = descriptor[d].descending()
                            ? Double.compare(lanes[d][right], lanes[d][left])
                            : Double.compare(lanes[d][left], lanes[d][right]);
                    if (sign != 0) { expected = Integer.signum(sign); break; }
                }
                check(w.rankOrder() == expected && w.exactOrderCertified(),
                        "random lexicographic signal oracle");
            }
        }
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static void illegal(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }

    private static void stateError(Runnable action, String label) {
        try { action.run(); } catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError(label);
    }
}
