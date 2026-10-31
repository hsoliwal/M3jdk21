/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/EntropyTrajectory.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.List;

/** Deterministic structural-entropy trajectory classifier. */
public final class EntropyTrajectory {
    private EntropyTrajectory() {}

    public static TrajectoryClass classify(EntropyPoint previous, EntropyPoint current, List<EntropyPoint> history) {
        if (previous.inputRoot().equals(current.inputRoot()) && previous.semanticRoot().equals(current.semanticRoot())) {
            return TrajectoryClass.NO_CHANGE;
        }
        if (previous.inputRoot().equals(current.inputRoot()) && !previous.semanticRoot().equals(current.semanticRoot())) {
            return TrajectoryClass.STRUCTURE_CHANGED_WITH_STABLE_INPUT;
        }
        if (!previous.inputRoot().equals(current.inputRoot()) && previous.semanticRoot().equals(current.semanticRoot())) {
            return TrajectoryClass.SURFACE_CHURN_STRUCTURAL_STALL;
        }
        boolean cycle = history.stream()
                .limit(Math.max(0, history.size() - 1L))
                .anyMatch(point -> point.semanticRoot().equals(current.semanticRoot()));
        return cycle ? TrajectoryClass.STRUCTURAL_CYCLE_RETURN : TrajectoryClass.STRUCTURAL_DELTA;
    }
}
