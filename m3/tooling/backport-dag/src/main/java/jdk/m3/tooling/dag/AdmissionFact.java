// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import java.util.Objects;

/**
 * Framework-neutral promotion fact. Drools adapters may consume this record, but the Java validator
 * remains the canonical authority.
 */
public record AdmissionFact(
        String packetId,
        boolean compatibilityProven,
        boolean recipeTestsPassed,
        boolean compilePassed,
        boolean testsPassed,
        boolean benchmarkRequired,
        boolean benchmarkPassed,
        boolean scopeApproved,
        boolean promotionRequested) {

    public AdmissionFact {
        packetId = Objects.requireNonNull(packetId, "packetId").strip();
        if (packetId.isEmpty()) {
            throw new IllegalArgumentException("packetId");
        }
    }

    public boolean promotable() {
        return compatibilityProven
                && recipeTestsPassed
                && compilePassed
                && testsPassed
                && scopeApproved
                && (!benchmarkRequired || benchmarkPassed);
    }
}
