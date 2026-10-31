/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Independent Java21 source-owner tests: zero-loss coordinates, version custody,
 * original progressive comparator and exact caller-controlled metric certificates.
 */
public final class M3SignalEvidenceBridgeSelfTest {
    private static final String SOURCE_SHA =
            "a7909698870bf89797427d9dd757dbf84bd002cf846c41b612fdcaa2c0aca1af";
    private static int checks;

    private M3SignalEvidenceBridgeSelfTest() { }

    public static void main(String[] args) {
        ordinalAndMetricEvidence();
        signedZeroAndRuntimeOrigin();
        originalColumnOwnershipAndProvenance();
        rejectMissingOrLossyFacts();
        rejectUncertifiedOrAmbiguousProjection();
        verifiedSourceImageIntegrity();
        System.out.println("PASS M3SignalEvidenceBridgeSelfTest checks=" + checks);
    }

    private static void ordinalAndMetricEvidence() {
        M3SignalColumns facts = base();
        var plan = M3SignalEvidenceBridge.compile(facts, 17L, SOURCE_SHA, true,
                lane("long", 0, M3ProgressiveSignalEvidence.Family.IDENTITY,
                        true, true, 1.0, 10.0),
                lane("int", 1, M3ProgressiveSignalEvidence.Family.STRUCTURAL,
                        true, true, 1.0, 10.0),
                lane("sample", 2, M3ProgressiveSignalEvidence.Family.RUNTIME,
                        true, true, 1.0, 3.0));
        check(plan.rows() == facts.rowCount(), "row count stays in original owner");
        check(plan.version() == 17L, "source generation version retained");
        check(plan.dimensions() == 3, "domain projection dimensions");
        check(plan.dimension(0).sourceSymbol().contains(SOURCE_SHA), "full source custody");
        check(plan.dimension(0).sourceSymbol().contains("evidence:2a"), "column evidence id");
        check(plan.dimension(0).provenance()
                == M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME,
                "static facts have source-time provenance");

        var workspace = plan.workspace().begin(0, 1);
        workspace.refineUntilRankKnown();
        check(workspace.rankKnown(), "rank decisively established");
        check(workspace.rankOrder() == facts.compare(0, 1).sign(), "typed sign parity");
        check(workspace.exactOrderCertified(), "explicit full exact domain certificate");
        check(workspace.evaluated(0) && !workspace.evaluated(1),
                "lazy demand-driven ordering stops at decisive signal");

        workspace.refineMetric();
        var bounds = workspace.metricBounds();
        check(bounds.lowerAvailable() && bounds.upperAvailable() && bounds.complete(),
                "all declared finite weighted metric coordinates evaluated");
        // |2-1| + |5-9| + |-0.0-0.0| = 5.
        check(bounds.lower() <= 5.0 && bounds.upper() >= 5.0,
                "certified bounds include independently computed declared L1 distance");
        check(workspace.snapshot().sourceVersion() == 17L, "snapshot retains version");

        AtomicInteger verified = new AtomicInteger();
        var heuristic = M3SignalEvidenceBridge.compile(facts, 18L, SOURCE_SHA, false,
                lane("long", 0, M3ProgressiveSignalEvidence.Family.IDENTITY,
                        true, true, 0.0, Double.NaN));
        var candidate = heuristic.workspace().begin(0, 1);
        int first = candidate.exactOrderOrVerify((a, b) -> {
            verified.incrementAndGet();
            return facts.compare(a, b).sign();
        });
        int second = candidate.exactOrderOrVerify((a, b) -> {
            throw new AssertionError("verifier invoked again");
        });
        check(first == second && first == 1 && verified.get() == 1,
                "canonical verifier called once on uncertified policy");
        check(candidate.exactOrderCertified(), "verifier certificate remains explicit");
    }

    private static void signedZeroAndRuntimeOrigin() {
        var facts = new M3SignalColumns(2,
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 91, false,
                        M3SignalColumns.allValid(2), null, null,
                        new double[]{-0.0, +0.0}));
        var plan = M3SignalEvidenceBridge.compile(facts, 0, SOURCE_SHA, false,
                lane("signed-zero", 0, M3ProgressiveSignalEvidence.Family.RUNTIME,
                        true, true, 0.0, Double.NaN));
        check(plan.dimension(0).provenance() == M3ProgressiveSignalEvidence.Provenance.RUNTIME,
                "profiled source is runtime, never automatically measured/certified");
        var workspace = plan.workspace().begin(0, 1);
        workspace.refineUntilRankKnown();
        check(workspace.rankOrder() == -1, "Double.compare distinguishes signed zero");
        check(workspace.separationAt(0) == 0.0,
                "zero numeric separation does not imply equality of ordered coordinates");
        check(!workspace.exactOrderCertified(),
                "heuristic ordered signs never certify full object equality");
    }

    private static void originalColumnOwnershipAndProvenance() {
        long[] physical = {0L, 1L};
        var facts = new M3SignalColumns(2,
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.HISTORY, 7, false,
                        M3SignalColumns.allValid(2), null, physical, null));
        physical[0] = Long.MAX_VALUE; // The original typed source already owns its detached lane.
        var plan = M3SignalEvidenceBridge.compile(facts, 2, SOURCE_SHA, false,
                lane("history", 0, M3ProgressiveSignalEvidence.Family.TEMPORAL,
                        true, true, 0.0, Double.NaN));
        check(plan.dimension(0).sourceSymbol().contains("origin:HISTORY"),
                "historical source intent is not lost when mapped to existing provenance enum");
        check(plan.dimension(0).provenance()
                        == M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME,
                "Git/history fact maps to source-time provenance, not measured runtime");
        check(plan.workspace().begin(0, 1).exactOrderOrVerify(
                (a, b) -> Long.compare(a, b)) == -1, "independent verifier still available");
        var all = facts.snapshotColumn(0).longs();
        check(Arrays.equals(all, new long[]{0, 1}), "caller cannot mutate canonical owned lane");
    }

    private static void rejectMissingOrLossyFacts() {
        reject(() -> M3SignalEvidenceBridge.compile(singleLong(1L << 54), 1,
                SOURCE_SHA, true, orderedLong()), IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(singleLong(Long.MIN_VALUE), 1,
                SOURCE_SHA, true, orderedLong()), IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(new M3SignalColumns(1,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 0, false,
                        new long[]{0L}, new int[]{0}, null, null)), 1,
                SOURCE_SHA, false, orderedLong()), IllegalArgumentException.class);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            var source = new M3SignalColumns(1,
                    new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                            M3SignalColumns.Origin.PROFILED, 0, false,
                            M3SignalColumns.allValid(1), null, null,
                            new double[]{invalid}));
            reject(() -> M3SignalEvidenceBridge.compile(source, 1, SOURCE_SHA, false,
                    lane("bad-double", 0, M3ProgressiveSignalEvidence.Family.RUNTIME,
                            true, true, 0.0, Double.NaN)), IllegalArgumentException.class);
        }
    }

    private static void rejectUncertifiedOrAmbiguousProjection() {
        var source = singleLong(2);
        reject(() -> M3SignalEvidenceBridge.compile(source, -1, SOURCE_SHA,
                false, orderedLong()), IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(source, 1, "short", false,
                orderedLong()), IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(source, 1, SOURCE_SHA, false),
                IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(source, 1, SOURCE_SHA, false,
                orderedLong(), orderedLong()), IllegalArgumentException.class);
        reject(() -> M3SignalEvidenceBridge.compile(source, 1, SOURCE_SHA, true,
                lane("not-exact", 0, M3ProgressiveSignalEvidence.Family.IDENTITY,
                        true, false, 0.0, Double.NaN)), IllegalArgumentException.class);
        var plan = M3SignalEvidenceBridge.compile(base(), 0, SOURCE_SHA, false,
                lane("wrong-gap", 0, M3ProgressiveSignalEvidence.Family.IDENTITY,
                        true, true, 1.0, 0.0));
        reject(() -> plan.workspace().begin(0, 1).refineMetric(),
                IllegalStateException.class);
    }

    /** SHA-256 is checked against actual compiler S/T/E bytes, not merely its text shape. */
    private static void verifiedSourceImageIntegrity() {
        String body = "# M3JDK21-SOURCE-SIGNALS/1\n"
                + "# S=scalar counts (exact nonnegative integers); T=hashed token; E=attributed candidate edge\n"
                + scalarRecord("demo.A", 1)
                + scalarRecord("demo.B", 2);
        String sealed = sealSource(body);
        M3SourceSignalImage image = M3SourceSignalImage.parse(sealed);
        check(image.rowCount() == 2, "compiler image contains two real source types");
        var plan = M3SignalEvidenceBridge.compileVerified(image, 27, false,
                lane("fields", 0, M3ProgressiveSignalEvidence.Family.FIELDS,
                        true, true, 0.0, Double.NaN));
        check(plan.version() == 27L, "actual compiler-source version preserved");
        check(plan.dimension(0).sourceSymbol().contains(image.sourceSha256()),
                "compiler-image digest reused after existing parser verified its trailer");
        var comparison = plan.workspace().begin(0, 1);
        comparison.refineUntilRankKnown();
        check(comparison.rankOrder() == -1, "compiler-observed field counts retain order");
        check(!comparison.exactOrderCertified(),
                "verified image bytes do not self-certify complete domain object ordering");
        String tampered = sealed.replace("S\tdemo.A\t1\t", "S\tdemo.A\t2\t");
        reject(() -> M3SourceSignalImage.parse(tampered), IllegalArgumentException.class);
    }

    private static String scalarRecord(String owner, int fields) {
        StringBuilder row = new StringBuilder("S\t").append(owner);
        for (int index = 0; index < 15; index++) {
            row.append('\t').append(index == 0 ? fields : 0);
        }
        return row.append('\n').toString();
    }

    private static String sealSource(String source) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return source + "# sha256\t" + java.util.HexFormat.of().formatHex(digest) + "\n";
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static M3SignalColumns singleLong(long value) {
        return new M3SignalColumns(1, new M3SignalColumns.Column(
                M3SignalColumns.Kind.LONG, M3SignalColumns.Origin.STATIC, 0, false,
                M3SignalColumns.allValid(1), null, new long[]{value}, null));
    }

    private static M3SignalEvidenceBridge.Lane orderedLong() {
        return lane("long", 0, M3ProgressiveSignalEvidence.Family.IDENTITY,
                true, true, 0.0, Double.NaN);
    }

    private static M3SignalColumns base() {
        long[] validity = M3SignalColumns.allValid(4);
        return new M3SignalColumns(4,
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.STATIC, 42L, false,
                        validity, null, new long[]{2, 1, 3, 1}, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.DOMAIN, 43L, false,
                        validity, new int[]{5, 9, 5, 9}, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 44L, false,
                        validity, null, null, new double[]{-0.0, +0.0, 2.5, 2.5}));
    }

    private static M3SignalEvidenceBridge.Lane lane(String name, int column,
            M3ProgressiveSignalEvidence.Family family, boolean ordered,
            boolean exactOrdinal, double metricWeight, double maximumGap) {
        return new M3SignalEvidenceBridge.Lane(name, column, family, ordered,
                exactOrdinal, metricWeight, maximumGap);
    }

    private static void check(boolean result, String explanation) {
        checks++;
        if (!result) throw new AssertionError("M3SignalEvidenceBridge " + explanation);
    }

    private static void reject(Runnable action, Class<? extends Throwable> expected) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("wrong signal refusal", error);
        }
        throw new AssertionError("did not refuse invalid signal source");
    }
}
