/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/StructuredPromptPort.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.Set;

/** Strategy port for structured Prompt-API retention proposals. It never receives deletion authority. */
@FunctionalInterface
public interface StructuredPromptPort {
    Set<String> retainedClaimIds(SupersetBundle bundle) throws Exception;

    static StructuredPromptPort mechanicalFloor() {
        return bundle -> Set.copyOf(bundle.claims().stream().map(MechanicalClaim::claimId).toList());
    }
}
