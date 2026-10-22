/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Objects;

/**
 * Fail-closed, zero-payload-copy bridge from immutable typed M3 signal lanes to the
 * already-qualified progressive comparison/bounds engine.
 *
 * <p>A scalar double is admissible only when the typed coordinate is present, finite,
 * and its integer representation is exact. A verified-looking source SHA string does not
 * authenticate an arbitrary caller-created image; the producer must bind it to its
 * source-version lifecycle. Exact semantic ordering and metric certificates remain
 * explicit caller declarations, never inferred from hash, source rank or column type.
 */
public final class M3SignalEvidenceBridge {
    private M3SignalEvidenceBridge() { }

    /** A requested domain projection and its caller-supplied metric contract. */
    public record Lane(
            String name,
            int column,
            M3ProgressiveSignalEvidence.Family family,
            boolean ordered,
            boolean exactOrdinal,
            double metricWeight,
            double maximumGap) {
        public Lane {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) throw new IllegalArgumentException("blank signal name");
            Objects.requireNonNull(family, "family");
        }
    }

    /**
     * Source-time intake from the existing compiler-image owner, which has already
     * verified the SHA-256 trailer against the received S/T/E source bytes.
     *
     * <p>This is an integrity check, not a cryptographic signature or proof of
     * compiler producer identity. Exact ordering and metric policies remain
     * independent caller-supplied certificates.</p>
     */
    public static M3ProgressiveSignalEvidence.Plan compileVerified(
            M3SourceSignalImage verifiedImage,
            long sourceVersion,
            boolean completeExactSchema,
            Lane... lanes) {
        Objects.requireNonNull(verifiedImage, "verifiedImage");
        return compile(verifiedImage.columns(), sourceVersion,
                verifiedImage.sourceSha256(), completeExactSchema, lanes);
    }

    /**
     * Validate every projected source row *before* returning a reusable versioned plan.
     * This avoids converting missing facts or arbitrary long values into a false double
     * similarity/order coordinate while retaining the single original primitive payload.
     *
     * @param source original immutable typed M3 column owner
     * @param sourceVersion source-snapshot epoch supplied by the authoritative producer
     * @param sourceSha256 complete lowercase SHA-256 source identity supplied by the producer
     * @param completeExactSchema independent caller certificate for the whole domain order
     * @param lanes ordered source-coordinate definitions and certified metric gap declarations
     */
    public static M3ProgressiveSignalEvidence.Plan compile(
            M3SignalColumns source,
            long sourceVersion,
            String sourceSha256,
            boolean completeExactSchema,
            Lane... lanes) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceSha256, "sourceSha256");
        Objects.requireNonNull(lanes, "lanes");
        if (sourceVersion < 0) throw new IllegalArgumentException("negative source version");
        if (!sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("full lowercase SHA-256 source identity required");
        }
        if (lanes.length == 0) throw new IllegalArgumentException("empty projection");
        Lane[] selected = lanes.clone();
        int[] columns = new int[selected.length];
        boolean[] seen = new boolean[source.columnCount()];
        M3ProgressiveSignalEvidence.Dimension[] descriptors =
                new M3ProgressiveSignalEvidence.Dimension[selected.length];
        for (int position = 0; position < selected.length; position++) {
            Lane lane = Objects.requireNonNull(selected[position], "lane[" + position + "]");
            int column = Objects.checkIndex(lane.column(), source.columnCount());
            if (seen[column]) {
                throw new IllegalArgumentException("duplicate projected coordinate " + column);
            }
            seen[column] = true;
            columns[position] = column;
            validateDenseExactCoordinates(source, column);
            M3SignalColumns.Origin original = source.origin(column);
            String symbol = "sha256:" + sourceSha256 + ":origin:" + original
                    + ":evidence:" + Long.toUnsignedString(source.evidenceId(column), 16)
                    + ":column:" + column;
            descriptors[position] = new M3ProgressiveSignalEvidence.Dimension(
                    lane.name(), lane.family(), provenance(original), symbol,
                    lane.ordered(), lane.exactOrdinal(), false,
                    lane.metricWeight(), lane.maximumGap());
        }
        M3ProgressiveSignalEvidence.Reader reader =
                (dimension, row) -> source.exactFiniteDoubleAt(
                        columns[Objects.checkIndex(dimension, columns.length)], row);
        return M3ProgressiveSignalEvidence.Plan.of(
                source.rowCount(), sourceVersion, descriptors, reader, completeExactSchema);
    }

    private static void validateDenseExactCoordinates(M3SignalColumns source, int column) {
        for (int row = 0; row < source.rowCount(); row++) {
            source.exactFiniteDoubleAt(column, row);
        }
    }

    /**
     * Origin and provenance are distinct enums. PROFILED identifies runtime data,
     * not a guaranteed instrumented measurement; historical facts are source-time
     * provenance with the original HISTORY tag retained in the source symbol.
     */
    private static M3ProgressiveSignalEvidence.Provenance provenance(
            M3SignalColumns.Origin origin) {
        return switch (Objects.requireNonNull(origin, "origin")) {
            case STATIC, HISTORY -> M3ProgressiveSignalEvidence.Provenance.SOURCE_TIME;
            case PROFILED -> M3ProgressiveSignalEvidence.Provenance.RUNTIME;
            case DOMAIN -> M3ProgressiveSignalEvidence.Provenance.DOMAIN;
        };
    }
}
