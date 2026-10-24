/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable per-DIMENSION quality metadata over the original typed M3 signal owner.
 * Signal payload arrays, canonical objects, validity bits and ordering remain owned
 * by {@link M3SignalColumns}. This class retains only O(dimensions) primitive codes,
 * shared labels and one original dependency-generation owner; never per-row entries.
 *
 * <p>Claims about sound bounds are producer declarations, NOT machine-verified
 * certificates of semantic equivalence, metric validity or safe pruning.
 * Neither an SHA-256 string nor a Java annotation converts a heuristic into a fact.
 * Callers must qualify bound certificates independently before using them to
 * eliminate candidates or to change a public comparator's contract.</p>
 */
public final class M3SignalQualitySchema {
    public enum Family {
        IDENTITY, LEXICAL, TYPE, STRUCTURAL, DEPENDENCY, CALL_GRAPH, BEHAVIORAL,
        CONTROL_FLOW, DATAFLOW, ALLOCATION, MEMORY, LIFECYCLE, CONCURRENCY,
        SEMANTIC, ORDERING, DISTANCE, STATISTICAL, TEMPORAL, GRAPH, EXECUTION,
        CORRECTNESS, PROVENANCE, RELATIONAL, PREDICTIVE
    }
    public enum Phase { SOURCE, COMPILER, INDEX, RUNTIME }
    public enum Reliability {
        EXACT_FACT, SOUND_LOWER_BOUND, SOUND_UPPER_BOUND, ESTIMATE, UNKNOWN
    }
    public enum Invalidation {
        SOURCE_VERSION, RECORDED_DEPENDENCIES, PROFILE_REFRESH, EXTERNAL_UNKNOWN
    }
    public enum SourceAuthority { DECLARED, COMPILER_IMAGE_SHA256_VERIFIED }

    /** Transient schema declaration, not a retained object for each row or signal value. */
    public record Spec(String name, Family family, Phase phase,
            Reliability reliability, Invalidation invalidation,
            String unit, String producer, double confidence, String evidenceSha256) {
        public Spec {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(reliability, "reliability");
            Objects.requireNonNull(invalidation, "invalidation");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(producer, "producer");
            Objects.requireNonNull(evidenceSha256, "evidenceSha256");
            if (!name.matches("[A-Za-z][A-Za-z0-9_.:-]{0,127}")
                    || producer.isBlank() || producer.length() > 256
                    || unit.length() > 64 || unit.indexOf('\n') >= 0
                    || unit.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("invalid schema symbol, producer or unit");
            }
            if (reliability == Reliability.ESTIMATE) {
                if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
                    throw new IllegalArgumentException("estimate needs confidence in [0,1]");
                }
            } else if (!Double.isNaN(confidence)) {
                throw new IllegalArgumentException("non-estimate must not pretend statistical confidence");
            }
            if (!evidenceSha256.isEmpty() && !sha256(evidenceSha256)) {
                throw new IllegalArgumentException("invalid evidence digest");
            }
            if ((reliability == Reliability.SOUND_LOWER_BOUND
                    || reliability == Reliability.SOUND_UPPER_BOUND)
                    && evidenceSha256.isEmpty()) {
                throw new IllegalArgumentException("bound claim requires independently reviewable evidence");
            }
        }
    }

    /** Immutable operation-bound optimistic stamp; never used for source-object ownership. */
    public static final class Ticket {
        private final M3SignalQualitySchema owner;
        private final int[] rows;
        private final long[] stamps;

        private Ticket(M3SignalQualitySchema owner, int[] rows, long[] stamps) {
            this.owner = owner;
            this.rows = rows;
            this.stamps = stamps;
        }
        public int size() { return rows.length; }
        public int[] rows() { return rows.clone(); }
        public long[] stamps() { return stamps.clone(); }
    }

    private final M3SignalColumns signals;
    private final M3SignalDependencyEpochs epochs;
    private final long sourceVersion;
    private final String sourceSha256;
    private final SourceAuthority authority;
    private final String[] names;
    private final String[] units;
    private final String[] producers;
    private final String[] evidenceDigests;
    private final byte[] families;
    private final byte[] phases;
    private final byte[] reliabilities;
    private final byte[] invalidations;
    private final double[] confidences;

    private M3SignalQualitySchema(M3SignalColumns signals, M3SignalDependencyEpochs epochs,
            long sourceVersion, String sourceSha256, SourceAuthority authority, Spec[] specs) {
        this.signals = Objects.requireNonNull(signals, "signals");
        this.epochs = Objects.requireNonNull(epochs, "epochs");
        if (signals.rowCount() != epochs.rowCount() || sourceVersion < 0
                || !sha256(sourceSha256)) {
            throw new IllegalArgumentException("row count or source version/identity mismatch");
        }
        if (specs.length != signals.columnCount() || specs.length > 255) {
            throw new IllegalArgumentException("schema must describe every original typed signal column");
        }
        this.sourceVersion = sourceVersion;
        this.sourceSha256 = sourceSha256;
        this.authority = authority;
        names = new String[specs.length];
        units = new String[specs.length];
        producers = new String[specs.length];
        evidenceDigests = new String[specs.length];
        families = new byte[specs.length];
        phases = new byte[specs.length];
        reliabilities = new byte[specs.length];
        invalidations = new byte[specs.length];
        confidences = new double[specs.length];
        for (int col = 0; col < specs.length; col++) {
            Spec spec = Objects.requireNonNull(specs[col], "null schema dimension");
            for (int earlier = 0; earlier < col; earlier++) {
                if (spec.name.equals(names[earlier])) {
                    throw new IllegalArgumentException("duplicate signal dimension name");
                }
            }
            if (signals.origin(col) == M3SignalColumns.Origin.PROFILED
                    && spec.phase != Phase.RUNTIME) {
                throw new IllegalArgumentException("profiled value cannot claim static production");
            }
            if (signals.origin(col) == M3SignalColumns.Origin.STATIC
                    && spec.phase == Phase.RUNTIME) {
                throw new IllegalArgumentException("static source fact cannot claim runtime origin");
            }
            names[col] = spec.name;
            units[col] = spec.unit;
            producers[col] = spec.producer;
            evidenceDigests[col] = spec.evidenceSha256;
            families[col] = (byte) spec.family.ordinal();
            phases[col] = (byte) spec.phase.ordinal();
            reliabilities[col] = (byte) spec.reliability.ordinal();
            invalidations[col] = (byte) spec.invalidation.ordinal();
            confidences[col] = spec.confidence;
        }
    }

    /** The digest is caller-DECLARED, not cryptographically verified by this method. */
    public static M3SignalQualitySchema declared(M3SignalColumns signals,
            M3SignalDependencyEpochs epochs, long sourceVersion,
            String sourceSha256, Spec... specs) {
        Objects.requireNonNull(specs, "specs");
        return new M3SignalQualitySchema(signals, epochs, sourceVersion, sourceSha256,
                SourceAuthority.DECLARED, specs.clone());
    }

    /**
     * Reuse the original compiler receiver that checked its serialized source-image
     * SHA-256. It does not verify the independent quality descriptors or their proofs.
     */
    public static M3SignalQualitySchema fromCompilerImage(M3SourceSignalImage image,
            M3SignalDependencyEpochs epochs, long sourceVersion, Spec... specs) {
        Objects.requireNonNull(image, "verified compiler image");
        Objects.requireNonNull(specs, "specs");
        return new M3SignalQualitySchema(image.columns(), epochs, sourceVersion,
                image.sourceSha256(), SourceAuthority.COMPILER_IMAGE_SHA256_VERIFIED,
                specs.clone());
    }

    public int rowCount() { return signals.rowCount(); }
    public int dimensionCount() { return names.length; }
    public long sourceVersion() { return sourceVersion; }
    public String sourceSha256() { return sourceSha256; }
    public SourceAuthority sourceAuthority() { return authority; }
    public M3SignalColumns.Kind kind(int col) { return signals.kind(column(col)); }
    public M3SignalColumns.Origin origin(int col) { return signals.origin(column(col)); }
    public long evidenceId(int col) { return signals.evidenceId(column(col)); }
    public boolean validAt(int col, int row) {
        return signals.validAt(column(col), row);
    }

    /** Recreate a single transient dimension descriptor, never a per-value Entry. */
    public Spec descriptor(int col) {
        col = column(col);
        return new Spec(names[col], Family.values()[Byte.toUnsignedInt(families[col])],
                Phase.values()[Byte.toUnsignedInt(phases[col])],
                Reliability.values()[Byte.toUnsignedInt(reliabilities[col])],
                Invalidation.values()[Byte.toUnsignedInt(invalidations[col])],
                units[col], producers[col], confidences[col], evidenceDigests[col]);
    }

    /** An exact label is a source-claimed fact, not semantic equality certification. */
    public Reliability reliability(int col) {
        return Reliability.values()[Byte.toUnsignedInt(reliabilities[column(col)])];
    }

    /** Captures only requested physical row stamps under the original epoch owner's lock. */
    public Ticket capture(int[] selectedRows) {
        Objects.requireNonNull(selectedRows, "selectedRows");
        int[] owned = selectedRows.clone();
        return new Ticket(this, owned, epochs.capture(owned));
    }

    /**
     * Advisory optimism: external changes must invalidateAll(), source payload
     * must be immutable for this reader, and the caller must guard publication.
     * A succeeding call is not a long-lived lock or an object-equality proof.
     */
    public boolean unchanged(Ticket ticket, long actualSourceVersion, String actualSha256) {
        Objects.requireNonNull(ticket, "ticket");
        if (ticket.owner != this) throw new IllegalArgumentException("foreign signal ticket");
        if (actualSourceVersion != sourceVersion || !sourceSha256.equals(actualSha256)) {
            return false;
        }
        return epochs.unchanged(ticket.rows, ticket.stamps);
    }

    public void requireCurrent(long actualSourceVersion, String actualSha256) {
        if (actualSourceVersion != sourceVersion || !sourceSha256.equals(actualSha256)) {
            throw new IllegalStateException("source identity/version changed");
        }
    }

    private int column(int index) { return Objects.checkIndex(index, names.length); }
    private static boolean sha256(String digest) {
        return digest != null && digest.matches("[0-9a-f]{64}");
    }
}
