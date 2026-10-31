/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/** One evidence fact for one atom and one gate. */
public interface M3AtomEvidence {
    /**
     * Returns the referenced atom identity.
     *
     * @return referenced atom identity
     */
    String atomId();

    /**
     * Returns the proof gate represented by this row.
     *
     * @return proof gate represented by this row
     */
    M3CoverageGate gate();

    /**
     * Returns the explicit result state.
     *
     * @return explicit result state
     */
    M3GateStatus status();

    /**
     * Returns the evidence artifact reference or {@code -} when permitted.
     *
     * @return evidence artifact reference or {@code -} when permitted
     */
    String evidenceRef();

    /**
     * Returns the lowercase evidence SHA-256 or {@code -} when permitted.
     *
     * @return lowercase evidence SHA-256 or {@code -} when permitted
     */
    String evidenceSha256();

    /**
     * Returns the bounded rationale, limitation or result note.
     *
     * @return bounded rationale, limitation or result note
     */
    String note();
}
