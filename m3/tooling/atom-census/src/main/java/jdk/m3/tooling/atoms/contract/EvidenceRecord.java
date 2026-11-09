/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/**
 * Immutable row from {@code EVIDENCE.tsv}.
 *
 * @param atomId referenced atom identity
 * @param gate proof gate
 * @param status explicit result state
 * @param evidenceRef evidence artifact reference or {@code -}
 * @param evidenceSha256 lowercase evidence SHA-256 or {@code -}
 * @param note rationale, limitation or result note
 */
public record EvidenceRecord(
        String atomId,
        CoverageGate gate,
        GateStatus status,
        String evidenceRef,
        String evidenceSha256,
        String note) implements AtomEvidence {
}
