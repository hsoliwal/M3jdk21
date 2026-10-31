// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.Objects;

/**
 * Immutable semantic/proof evidence for one recipe atom.
 *
 * <p>The recipe work reference says what executes. This evidence says why the atom exists, which
 * contract it preserves or extends, which pattern/IOP role it participates in, where its
 * documentation lives, and which JUnit proof owns its fixed point.</p>
 */
public record M3RecipeAtomEvidence(
        String atomId,
        String contractRef,
        String documentationRef,
        String pattern,
        String iopRole,
        String junitProofRef,
        boolean fixedPointRequired) {

    public M3RecipeAtomEvidence {
        atomId = atomId(atomId);
        contractRef = required(contractRef, "contractRef");
        documentationRef = required(documentationRef, "documentationRef");
        pattern = required(pattern, "pattern");
        iopRole = required(iopRole, "iopRole");
        junitProofRef = required(junitProofRef, "junitProofRef");
        if (!fixedPointRequired) {
            throw new IllegalArgumentException("M3 recipe atoms require fixed-point proof: " + atomId);
        }
    }

    private static String atomId(String value) {
        String checked = required(value, "atomId");
        if (!checked.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid atom id: " + checked);
        }
        return checked;
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
