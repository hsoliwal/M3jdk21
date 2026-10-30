/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
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


    // Source-time evidence uses one primitive validity mask and one proved-true mask.
    // Missing/unknown facts are NOT false and are never treated as proof.
    public static final int SOURCE_BOUND = 1;
    public static final int ATTRIBUTED_CFG = 1 << 1;
    public static final int NORMAL_EXCEPTION_CLOSED = 1 << 2;
    public static final int ALIAS_SET_CLOSED = 1 << 3;
    public static final int ORIGIN_DEAD_AT_BOUNDARY = 1 << 4;
    public static final int NO_ESCAPE = 1 << 5;
    public static final int IDENTITY_UNOBSERVABLE = 1 << 6;
    public static final int NO_JNI_MONITOR_WEAK_OBSERVERS = 1 << 7;
    public static final int EXCLUSIVE_OWNER = 1 << 8;
    public static final int CANONICAL_PARENT_STATE = 1 << 9;
    public static final int RESET_COMPLETE = 1 << 10;
    public static final int COMPATIBLE_PROJECTION = 1 << 11;
    public static final int ELISION_EFFECTS_PRESERVED = 1 << 12;
    public static final int ALL_LIFETIME_FACTS = (1 << 13) - 1;
    public static final int BASE_LIFETIME_PROOF = (1 << 8) - 1;
    public static final int DIRECT_REBIND_PROOF = BASE_LIFETIME_PROOF
            | EXCLUSIVE_OWNER | CANONICAL_PARENT_STATE | RESET_COMPLETE | COMPATIBLE_PROJECTION;
    public static final int SCALAR_ELISION_PROOF =
            BASE_LIFETIME_PROOF | ELISION_EFFECTS_PRESERVED;

    /**
     * Pure source-time candidate ranking. Never use the result as a heap reclamation permit.
     * Only an exact method/site, SHA-bound attributed compiler analysis may supply true facts.
     * Dynamic borrows, pins, JNI handles and the parent residency budget must still be checked
     * at the actual operation boundary by M3ObjectResidency.
     *
     * <p>A source liveness boundary alone proves only a local reference became dead.
     * The other facts must establish the allocation origin, closed alias set and non-escape
     * across both normal and exceptional paths. Annotations alone cannot set these bits.
     */
    public enum LifetimeCandidate {
        PRESERVE_JVM_REACHABILITY, SCALAR_REPLACEMENT_CANDIDATE, DIRECT_PARENT_REBIND_CANDIDATE
    }

    public static LifetimeCandidate lifetimeCandidate(int validFacts, int provenFacts) {
        if ((validFacts & ~ALL_LIFETIME_FACTS) != 0 || (provenFacts & ~validFacts) != 0) {
            throw new IllegalArgumentException("invalid or unobserved lifetime fact bits");
        }
        if ((provenFacts & BASE_LIFETIME_PROOF) != BASE_LIFETIME_PROOF) {
            return LifetimeCandidate.PRESERVE_JVM_REACHABILITY;
        }
        // Allocation removal is preferable to recycling if constructor effects and identity
        // remain preserved; the product compiler/JIT, not this classifier, must prove it.
        if ((provenFacts & SCALAR_ELISION_PROOF) == SCALAR_ELISION_PROOF) {
            return LifetimeCandidate.SCALAR_REPLACEMENT_CANDIDATE;
        }
        if ((provenFacts & DIRECT_REBIND_PROOF) == DIRECT_REBIND_PROOF) {
            return LifetimeCandidate.DIRECT_PARENT_REBIND_CANDIDATE;
        }
        return LifetimeCandidate.PRESERVE_JVM_REACHABILITY;
    }

    private M3GcUsePlan() { }
}
