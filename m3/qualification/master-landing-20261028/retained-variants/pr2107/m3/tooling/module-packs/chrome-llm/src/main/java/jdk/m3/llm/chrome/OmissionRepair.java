/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/OmissionRepair.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Mechanical publication-floor repair: model omission becomes restoration, never deletion. */
public final class OmissionRepair {
    private OmissionRepair() {}

    public static RepairedSelection repair(List<MechanicalClaim> claims, Set<String> retainedClaimIds) {
        Set<String> all = new LinkedHashSet<>(claims.stream().map(MechanicalClaim::claimId).sorted().toList());
        if (!all.containsAll(retainedClaimIds)) {
            Set<String> unknown = new LinkedHashSet<>(retainedClaimIds);
            unknown.removeAll(all);
            throw new IllegalArgumentException("unknown retained claim ids: " + unknown);
        }
        Set<String> retained = new LinkedHashSet<>(retainedClaimIds);
        Set<String> restored = new LinkedHashSet<>(all);
        restored.removeAll(retained);
        Set<String> finalIds = new LinkedHashSet<>(retained);
        finalIds.addAll(restored);
        return new RepairedSelection(retained, restored, finalIds);
    }
}
