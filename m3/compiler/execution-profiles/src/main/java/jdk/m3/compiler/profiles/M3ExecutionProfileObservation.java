/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.util.Objects;

/**
 * One immutable, content-addressed execution-profile observation.
 *
 * @param observationId content-derived {@code m3o1:SHA-256} identity
 * @param kind portable observation kind
 * @param subject canonical observed subject
 * @param context canonical context descriptor or dash
 * @param phase workload phase
 * @param count positive observed count bound to this evidence
 * @param evidenceRef normalized relative evidence reference
 * @param evidenceSha256 lowercase SHA-256 of the referenced evidence
 */
public record M3ExecutionProfileObservation(
        String observationId,
        M3ExecutionProfileKind kind,
        String subject,
        String context,
        M3ExecutionProfilePhase phase,
        long count,
        String evidenceRef,
        String evidenceSha256) {

    /** Validates content and its derived identity. */
    public M3ExecutionProfileObservation {
        M3ExecutionProfileFormat.requireObservationId(observationId);
        Objects.requireNonNull(kind, "kind");
        M3ExecutionProfileFormat.requireDescriptor("subject", subject, false);
        M3ExecutionProfileFormat.requireDescriptor("context", context, true);
        Objects.requireNonNull(phase, "phase");
        M3ExecutionProfileFormat.requireCount(count);
        M3ExecutionProfileFormat.requireEvidenceRef(evidenceRef);
        M3ExecutionProfileFormat.requireSha256("evidence_sha256", evidenceSha256);
        String expected = M3ExecutionProfileFormat.observationId(
                kind, subject, context, phase, count, evidenceRef, evidenceSha256);
        if (!expected.equals(observationId)) {
            throw new IllegalArgumentException("observation_id does not match canonical observation content");
        }
    }

    /**
     * Creates one observation and derives its stable identity.
     *
     * @param kind portable observation kind
     * @param subject canonical observed subject
     * @param context canonical context descriptor or dash
     * @param phase workload phase
     * @param count positive observed count
     * @param evidenceRef normalized relative evidence reference
     * @param evidenceSha256 lowercase SHA-256 of evidence
     * @return validated content-addressed observation
     */
    public static M3ExecutionProfileObservation create(
            M3ExecutionProfileKind kind,
            String subject,
            String context,
            M3ExecutionProfilePhase phase,
            long count,
            String evidenceRef,
            String evidenceSha256) {
        String id = M3ExecutionProfileFormat.observationId(
                kind, subject, context, phase, count, evidenceRef, evidenceSha256);
        return new M3ExecutionProfileObservation(
                id, kind, subject, context, phase, count, evidenceRef, evidenceSha256);
    }

    String row() {
        return String.join("\t", observationId, kind.name(), subject, context, phase.name(), Long.toString(count),
                evidenceRef, evidenceSha256);
    }
}
