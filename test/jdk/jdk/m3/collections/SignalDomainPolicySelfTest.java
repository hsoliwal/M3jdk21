/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Independent typed-coordinate, source-freshness and progressive L1 evidence differential. */
public final class SignalDomainPolicySelfTest {
    private static long checks;
    private static final String DIGEST = "0123456789abcdef".repeat(4);
    private SignalDomainPolicySelfTest() { }

    public static void main(String[] arguments) throws Exception {
        strictAdmissionAndFreshness();
        weightedBoundsAndStableRank();
        seededCoordinates();
        if (arguments.length == 1) {
            SourceSignalImage fromJavac = SourceSignalImage.parse(
                    Files.readString(Path.of(arguments[0]), StandardCharsets.UTF_8));
            SignalDomainPolicy policy = SignalDomainPolicy.fromCompilerImage(
                    fromJavac, 1, new int[]{0},
                    new int[]{SourceSignalImage.Scalar.CALL_SITES.ordinal()},
                    new double[]{1.0}, new double[]{Double.NaN});
            check(policy.identityAuthority()
                    == SignalDomainPolicy.IdentityAuthority.COMPILER_SHA256_VERIFIED,
                    "real javac evidence has cryptographically verified identity");
            check(policy.sourceSha256().equals(fromJavac.sourceSha256()),
                    "preserves full SHA-256, not only 64-bit hint");
            check(policy.rowCount() == fromJavac.rowCount(), "matching javac row universe");
        }
        System.out.println("PASS M3SignalDomainPolicySelfTest checks=" + checks);
    }

    private static void strictAdmissionAndFreshness() {
        SignalColumns signals = sample();
        SignalDomainPolicy p = policy(signals);
        check(p.sourceVersion() == 7, "version retained");
        check(DIGEST.equals(p.sourceSha256()), "complete digest retained");
        check(p.identityAuthority() == SignalDomainPolicy.IdentityAuthority.CALLER_DECLARED,
                "general domain SHA is only a caller assertion");
        requireThrows(IllegalStateException.class, () -> p.requireCurrent(8, DIGEST));
        requireThrows(IllegalStateException.class, () -> p.requireCurrent(7, "f".repeat(64)));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, -1, DIGEST, new int[]{1}, new int[]{0},
                new double[]{1}, new double[]{Double.NaN}));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, 7, DIGEST.substring(1), new int[]{1}, new int[]{0},
                new double[]{1}, new double[]{Double.NaN}));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, 7, DIGEST, new int[]{1}, new int[]{0, 0},
                new double[]{1, 1}, new double[]{Double.NaN, Double.NaN}));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, 7, DIGEST, new int[]{1}, new int[]{0},
                new double[]{Double.NaN}, new double[]{Double.NaN}));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, 7, DIGEST, new int[]{1}, new int[]{0},
                new double[]{1}, new double[]{-1}));
        requireThrows(IndexOutOfBoundsException.class, () -> p.evaluate(0, 10, 1, 7, DIGEST));
        requireThrows(IllegalArgumentException.class, () -> p.evaluate(0, 1, 3, 7, DIGEST));
        requireThrows(IllegalStateException.class, () -> p.topK(new int[]{0}, 1, 8, DIGEST));
        requireThrows(IllegalStateException.class, () -> p.begin(0, 1, 7, "a".repeat(64)));
        // Exact 64-bit order remains legal, but double-only L1 metric admission refuses
        // any long that could lose numeric significance above 2^53.
        var highLong = new SignalColumns(2,
                new SignalColumns.Column(SignalColumns.Kind.LONG,
                        SignalColumns.Origin.STATIC, 0, false,
                        SignalColumns.allValid(2), null,
                        new long[]{9007199254740992L, 9007199254740993L}, null));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                highLong, 7, DIGEST, new int[]{0}, new int[]{0},
                new double[]{1}, new double[]{Double.NaN}));
        long[] invalid = SignalColumns.allValid(2);
        invalid[0] &= ~2L;
        var missing = new SignalColumns(2,
                new SignalColumns.Column(SignalColumns.Kind.INTEGER,
                        SignalColumns.Origin.STATIC, 0, false,
                        invalid, new int[]{1, 2}, null, null));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                missing, 7, DIGEST, new int[]{0}, new int[]{0},
                new double[]{1}, new double[]{Double.NaN}));
        var nonfinite = new SignalColumns(2,
                new SignalColumns.Column(SignalColumns.Kind.DOUBLE,
                        SignalColumns.Origin.PROFILED, 0, false,
                        SignalColumns.allValid(2), null, null,
                        new double[]{1, Double.NaN}));
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                nonfinite, 7, DIGEST, new int[]{0}, new int[]{0},
                new double[]{1}, new double[]{Double.NaN}));
    }

    private static void weightedBoundsAndStableRank() {
        SignalColumns signals = sample();
        SignalDomainPolicy p = policy(signals);
        int[] rows = {2, 0, 2, 3, 1};
        int[] actual = p.topK(rows, rows.length, 7, DIGEST);
        int[] expected = {1, 2, 0, 2, 3};
        check(Arrays.equals(actual, expected), "domain precedence and stable duplicate rows");
        int[] mask = {Integer.MIN_VALUE, Integer.MIN_VALUE, 0, Integer.MIN_VALUE};
        int[] admitted = {0, 1, 3};
        check(Arrays.equals(p.topK(admitted, admitted.length, 7, DIGEST),
                        p.topKByState(mask, Integer.MIN_VALUE, 0, 10, 7, DIGEST)),
                "state mask bit31 and streamed primitive topK parity");
        ProgressiveSignalEvidence.Workspace ws = p.begin(0, 3, 7, DIGEST);
        ProgressiveSignalEvidence.Bounds untouched = ws.metricBounds();
        check(untouched.lowerAvailable() && untouched.upperAvailable() && !untouched.complete(),
                "uncomputed L1 has zero lower and caller-certificate upper");
        ws.refine(0);
        ProgressiveSignalEvidence.Bounds partial = ws.metricBounds();
        check(partial.lower() <= 10 && partial.upper() >= 10 && !partial.complete(),
                "first L1 coordinate has conservative progressive interval");
        ws.refineMetric();
        ProgressiveSignalEvidence.Bounds full = ws.metricBounds();
        check(full.complete() && full.lower() <= 11.5 && full.upper() >= 11.5,
                "fully refined weighted L1 brackets arithmetic result");
        SignalDomainPolicy.Judgment judged = p.evaluate(0, 3, 1, 7, DIGEST);
        check(judged.order() < 0 && judged.decisiveColumn() == 1,
                "domain order comes from explicit policy, not metric scalar");
        check(!judged.objectEqualityCertified()
                        && judged.identityAuthority()
                                == SignalDomainPolicy.IdentityAuthority.CALLER_DECLARED,
                "similarity cannot certify domain equality or fake source authority");
        requireThrows(IllegalArgumentException.class, () -> SignalDomainPolicy.declared(
                signals, 7, DIGEST, new int[]{1, 0}, new int[]{0, 1},
                new double[]{2, 1.5}, new double[]{2, 7}));
        SignalDomainPolicy unknown = SignalDomainPolicy.declared(
                signals, 7, DIGEST, new int[]{1, 0}, new int[]{0, 1},
                new double[]{2, 1.5}, new double[]{Double.NaN, 7});
        check(!unknown.begin(0, 3, 7, DIGEST).metricBounds().upperAvailable(),
                "unknown upper bound must remain unavailable");
        check(!unknown.evaluate(0, 0, 2, 7, DIGEST).objectEqualityCertified(),
                "same row cannot silently certify whole object equality");
    }

    private static SignalColumns sample() {
        return new SignalColumns(4,
                new SignalColumns.Column(SignalColumns.Kind.LONG,
                        SignalColumns.Origin.STATIC, 11, false,
                        SignalColumns.allValid(4), null, new long[]{4, 7, 4, 9}, null),
                new SignalColumns.Column(SignalColumns.Kind.INTEGER,
                        SignalColumns.Origin.DOMAIN, 12, true,
                        SignalColumns.allValid(4), new int[]{2, 8, 2, 1}, null, null));
    }

    private static SignalDomainPolicy policy(SignalColumns columns) {
        return SignalDomainPolicy.declared(columns, 7, DIGEST, new int[]{1, 0},
                new int[]{0, 1}, new double[]{2.0, 1.5}, new double[]{6, 7});
    }

    private static void seededCoordinates() {
        Random random = new Random(0x4d33444f4d504f4cL);
        for (int trial = 0; trial < 500; trial++) {
            int rows = 1 + random.nextInt(50);
            int[] ints = new int[rows];
            long[] longs = new long[rows];
            for (int i = 0; i < rows; i++) {
                ints[i] = random.nextInt(41) - 20;
                longs[i] = random.nextInt(61) - 30;
            }
            SignalColumns columns = new SignalColumns(rows,
                    new SignalColumns.Column(SignalColumns.Kind.INTEGER,
                            SignalColumns.Origin.DOMAIN, 33, false,
                            SignalColumns.allValid(rows), ints, null, null),
                    new SignalColumns.Column(SignalColumns.Kind.LONG,
                            SignalColumns.Origin.STATIC, 34, false,
                            SignalColumns.allValid(rows), null, longs, null));
            SignalDomainPolicy policy = SignalDomainPolicy.declared(
                    columns, trial, DIGEST, new int[]{1, 0}, new int[]{0, 1},
                    new double[]{1, 2}, new double[]{40, 60});
            for (int repeat = 0; repeat < 80; repeat++) {
                int a = random.nextInt(rows), b = random.nextInt(rows);
                int expected = Long.compare(longs[a], longs[b]);
                if (expected == 0) expected = Integer.compare(ints[a], ints[b]);
                SignalDomainPolicy.Judgment result = policy.evaluate(a, b, 2, trial, DIGEST);
                check(result.order() == Integer.signum(expected), "exact typed rank over permuted domain");
                double exact = Math.abs(ints[a] - ints[b]) + 2.0 * Math.abs(longs[a] - longs[b]);
                check(result.metricBounds().complete()
                                && result.metricBounds().lower() <= exact
                                && exact <= result.metricBounds().upper(),
                        "source-certified L1 bounds contain independent arithmetic oracle");
            }
            Arrays.fill(ints, Integer.MAX_VALUE);
            Arrays.fill(longs, Long.MIN_VALUE);
            check(policy.evaluate(0, 0, 2, trial, DIGEST).order() == 0,
                    "original signal payload survives input-array mutation");
        }
    }

    private static void requireThrows(Class<? extends Throwable> expected, Runnable test) {
        checks++;
        try { test.run(); }
        catch (Throwable ex) {
            if (expected.isInstance(ex)) return;
            throw new AssertionError("wrong exception type: " + ex, ex);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void check(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason + " check=" + checks);
    }
}
