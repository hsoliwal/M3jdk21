/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Objects;

/**
 * Explicit typed-signal domain ordering coupled to the existing progressive metric evidence.
 * Sorting never replaces the exact coordinates with a lossy double. Weighted L1 bounds are
 * caller-declared coordinate facts, not proofs about Java object identity or semantic distance.
 * This owner stores no application objects, Java Entry nodes or duplicate primitive fact columns.
 */
public final class M3SignalDomainPolicy {
    public enum IdentityAuthority { CALLER_DECLARED, COMPILER_SHA256_VERIFIED }

    /** A policy result states the declared coordinate bounds, not domain equality. */
    public record Judgment(int order, int decisiveColumn,
            M3ProgressiveSignalEvidence.Bounds metricBounds,
            long sourceVersion, String sourceSha256,
            IdentityAuthority identityAuthority, boolean objectEqualityCertified) {
        public Judgment {
            if (order < -1 || order > 1 || decisiveColumn < -1
                    || (order == 0) != (decisiveColumn == -1)) {
                throw new IllegalArgumentException("inconsistent ordered result");
            }
            Objects.requireNonNull(metricBounds, "metricBounds");
            Objects.requireNonNull(sourceSha256, "sourceSha256");
            Objects.requireNonNull(identityAuthority, "identityAuthority");
            if (objectEqualityCertified) {
                throw new IllegalArgumentException("source signals cannot certify object equality");
            }
        }
    }

    private final M3SignalColumns signals;
    private final M3SignalTopK ranked;
    private final M3ProgressiveSignalEvidence.Plan metrics;
    private final int[] priority;
    private final int[] metricColumns;
    private final long sourceVersion;
    private final String sourceSha256;
    private final IdentityAuthority identityAuthority;

    private M3SignalDomainPolicy(M3SignalColumns signals, long version, String sha256,
            IdentityAuthority authority, int[] priority, int[] metricDimensions,
            double[] weights, double[] certifiedMaximumGaps) {
        this.signals = Objects.requireNonNull(signals, "signals");
        Objects.requireNonNull(authority, "authority");
        if (version < 0) throw new IllegalArgumentException("negative source version");
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("complete lowercase source SHA-256 required");
        }
        this.sourceVersion = version;
        this.sourceSha256 = sha256;
        this.identityAuthority = authority;
        this.ranked = new M3SignalTopK(signals, Objects.requireNonNull(priority, "priority"));
        this.priority = priority.clone();
        Objects.requireNonNull(metricDimensions, "metricDimensions");
        Objects.requireNonNull(weights, "weights");
        Objects.requireNonNull(certifiedMaximumGaps, "certifiedMaximumGaps");
        if (metricDimensions.length == 0 || metricDimensions.length != weights.length
                || metricDimensions.length != certifiedMaximumGaps.length) {
            throw new IllegalArgumentException("metric dimension/weight/certificate alignment");
        }
        this.metricColumns = metricDimensions.clone();
        boolean[] admitted = new boolean[signals.columnCount()];
        M3ProgressiveSignalEvidence.Dimension[] descriptions =
                new M3ProgressiveSignalEvidence.Dimension[metricColumns.length];
        for (int i = 0; i < metricColumns.length; i++) {
            int column = Objects.checkIndex(metricColumns[i], admitted.length);
            if (admitted[column]) throw new IllegalArgumentException("duplicate metric dimension");
            admitted[column] = true;
            double weight = weights[i], maximum = certifiedMaximumGaps[i];
            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException("metric weight must be positive and finite");
            }
            if (!Double.isNaN(maximum) && (!Double.isFinite(maximum) || maximum < 0.0)) {
                throw new IllegalArgumentException("invalid certified maximum gap");
            }
            // Admission checks must precede constructing any metric plan. No lossy long,
            // missing coordinate or runtime NaN can enter the declared numeric metric.
            for (int row = 0; row < signals.rowCount(); row++) {
                signals.finiteMetricAt(column, row);
            }
            M3SignalColumns.Origin origin = signals.origin(column);
            M3ProgressiveSignalEvidence.Family family = switch (origin) {
                case STATIC -> M3ProgressiveSignalEvidence.Family.COMPILER;
                case PROFILED -> M3ProgressiveSignalEvidence.Family.RUNTIME;
                case DOMAIN -> M3ProgressiveSignalEvidence.Family.DOMAIN;
                case HISTORY -> M3ProgressiveSignalEvidence.Family.TEMPORAL;
            };
            M3ProgressiveSignalEvidence.Provenance provenance = switch (origin) {
                case STATIC, HISTORY -> M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME;
                case PROFILED -> M3ProgressiveSignalEvidence.Provenance.RUNTIME;
                case DOMAIN -> M3ProgressiveSignalEvidence.Provenance.DOMAIN;
            };
            descriptions[i] = new M3ProgressiveSignalEvidence.Dimension(
                    "m3-coordinate-" + column, family, provenance,
                    sha256 + "/" + Long.toUnsignedString(signals.evidenceId(column), 16),
                    false, false, false, weight, maximum);
        }
        // Explicitly NOT a complete exact object schema. Numeric ties cannot establish
        // identity or even the caller's full ordinal policy across all source columns.
        this.metrics = M3ProgressiveSignalEvidence.Plan.of(signals.rowCount(), version,
                descriptions, (dimension, row) ->
                        signals.finiteMetricAt(metricColumns[dimension], row), false);
    }

    /** The provided digest is a caller assertion; no cryptographic source has been verified. */
    public static M3SignalDomainPolicy declared(M3SignalColumns signals, long sourceVersion,
            String sourceSha256, int[] priority, int[] metricDimensions,
            double[] weights, double[] certifiedMaximumGaps) {
        return new M3SignalDomainPolicy(signals, sourceVersion, sourceSha256,
                IdentityAuthority.CALLER_DECLARED, priority, metricDimensions,
                weights, certifiedMaximumGaps);
    }

    /** Reuse the original javac plugin image whose complete SHA-256 has already been verified. */
    public static M3SignalDomainPolicy fromCompilerImage(M3SourceSignalImage image,
            long sourceVersion, int[] priority, int[] metricDimensions,
            double[] weights, double[] certifiedMaximumGaps) {
        Objects.requireNonNull(image, "image");
        return new M3SignalDomainPolicy(image.columns(), sourceVersion,
                image.sourceSha256(), IdentityAuthority.COMPILER_SHA256_VERIFIED,
                priority, metricDimensions, weights, certifiedMaximumGaps);
    }

    public long sourceVersion() { return sourceVersion; }
    public String sourceSha256() { return sourceSha256; }
    public IdentityAuthority identityAuthority() { return identityAuthority; }
    public int rowCount() { return signals.rowCount(); }
    public int metricDimensionCount() { return metricColumns.length; }

    /** Fail closed if any owning source revision or strong content identity changed. */
    public void requireCurrent(long currentVersion, String currentSha256) {
        if (currentVersion != sourceVersion || !sourceSha256.equals(currentSha256)) {
            throw new IllegalStateException("M3 signal source version or SHA-256 changed");
        }
    }

    public int[] topK(int[] selectedRows, int limit, long version, String digest) {
        requireCurrent(version, digest);
        return ranked.topK(selectedRows, limit);
    }

    public int[] topKByState(int[] state, int required, int forbidden, int limit,
            long version, String digest) {
        requireCurrent(version, digest);
        return ranked.topKByState(state, required, forbidden, limit);
    }

    /** New pair-local workspace. Call refine()/refineMetric()/metricBounds() on the existing owner. */
    public M3ProgressiveSignalEvidence.Workspace begin(int leftRow, int rightRow,
            long version, String digest) {
        requireCurrent(version, digest);
        return metrics.workspace().begin(leftRow, rightRow);
    }

    /**
     * Return policy order and bounded progressive evidence after a bounded number of
     * metric dimensions. A scalar gap is never used as the ordering comparator.
     */
    public Judgment evaluate(int leftRow, int rightRow, int metricBudget,
            long version, String digest) {
        requireCurrent(version, digest);
        if (metricBudget < 0 || metricBudget > metricColumns.length) {
            throw new IllegalArgumentException("metric budget outside declared dimensions");
        }
        Objects.checkIndex(leftRow, signals.rowCount());
        Objects.checkIndex(rightRow, signals.rowCount());
        int sign = 0, decisive = -1;
        for (int column : priority) {
            sign = signals.orderAt(column, leftRow, rightRow);
            if (sign != 0) { decisive = column; break; }
        }
        var workspace = metrics.workspace().begin(leftRow, rightRow);
        for (int dimension = 0; dimension < metricBudget; dimension++) {
            workspace.refine(dimension);
        }
        return new Judgment(sign, decisive, workspace.metricBounds(), sourceVersion,
                sourceSha256, identityAuthority, false);
    }
}
