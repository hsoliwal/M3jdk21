// SPDX-License-Identifier: Apache-2.0
package com.m3.gc;

import java.util.Objects;

/**
 * Conservative receiver for source-bound Synexia compiler facts.
 * This class does not infer escape safety from annotations or reference counts.
 * Plan selection alone never authorizes reclamation of a Java object.
 */
public final class M3GcUsePlan {
    public enum Mode { PRESERVE_JVM_REACHABILITY, COMPACT_PARENT_STATE, COOPERATIVE_LEASE }

    /**
     * Facts must be proved against the exact method/site by the canonical compiler.
     * observable covers identity, monitors, reflection, serialization, JNI/foreign
     * aliases, weak/phantom observers, unknown calls and exceptional escape paths.
     */
    public record Facts(boolean exactSiteProof, boolean noEscapeProven,
            boolean observableOrUnknown, boolean sharedBacking,
            boolean cooperativeLeaseProven, boolean deterministicReconstructionProven) { }

    public static Mode classify(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactSiteProof() || !facts.noEscapeProven() || facts.observableOrUnknown()) {
            return Mode.PRESERVE_JVM_REACHABILITY;
        }
        if (facts.sharedBacking()) {
            return facts.cooperativeLeaseProven()
                    ? Mode.COOPERATIVE_LEASE : Mode.PRESERVE_JVM_REACHABILITY;
        }
        return facts.deterministicReconstructionProven()
                ? Mode.COMPACT_PARENT_STATE : Mode.PRESERVE_JVM_REACHABILITY;
    }

    private M3GcUsePlan() { }
}
