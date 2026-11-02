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
public record M3EvidenceRecord(
        String atomId,
        M3CoverageGate gate,
        M3GateStatus status,
        String evidenceRef,
        String evidenceSha256,
        String note) implements M3AtomEvidence {
}
