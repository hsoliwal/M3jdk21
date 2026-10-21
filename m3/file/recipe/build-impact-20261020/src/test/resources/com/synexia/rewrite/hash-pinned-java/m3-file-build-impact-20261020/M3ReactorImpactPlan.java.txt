// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * M3JDK21-native project/reactor impact closure over original M3File physical slots.
 * Maven 4, Gradle and JDT own their reactor metadata; this Java 21 primitive
 * kernel only uses complete, generation-matched project mappings and dependencies.
 * It never parses another POM or forms an independent cache, object graph or JNI
 * callback queue. Project IDs and file slots remain stable physical coordinates.
 */
public final class M3ReactorImpactPlan {
    public static final int FILE_IMPACT = 1;
    public static final int DEPENDENT_PROJECT = 1 << 1;
    public static final int CONSERVATIVE = 1 << 2;

    private final byte[] reasons;
    private final int[] orderedProjects;
    private final boolean conservative;

    private M3ReactorImpactPlan(byte[] reasons, int[] orderedProjects, boolean conservative) {
        this.reasons = reasons;
        this.orderedProjects = orderedProjects;
        this.conservative = conservative;
    }

    public int[] impactedProjects() { return orderedProjects.clone(); }
    public int size() { return orderedProjects.length; }
    public boolean requiresConservativeReactor() { return conservative; }
    public int reasonAt(int projectSlot) {
        return Byte.toUnsignedInt(reasons[Objects.checkIndex(projectSlot, reasons.length)]);
    }

    /**
     * Reverse graph edges are from a dependency project to its downstream
     * dependent project. Verified=true requires complete Maven/Gradle reactor,
     * plugin and generated-output edges; a cyclic graph is rejected. Verified=false
     * must supply no graph and selects every project when anything changed.
     * File ownership mapping covers ALL original M3File physical slots, with -1
     * reserved for non-compilation build inputs or unresolved ownership.
     * A changed/impacted source/class whose owner is unknown forces a full plan.
     */
    public static M3ReactorImpactPlan compute(M3FileDeltaLedger.Snapshot snapshot,
            M3BuildImpactPlan fileImpact, int[] fileProject, int projects,
            boolean verified, int[] dependentOffsets, int[] dependentTargets) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(fileImpact, "fileImpact");
        Objects.requireNonNull(fileProject, "fileProject");
        if (projects < 0 || projects > 1_000_000 || fileProject.length != snapshot.slots()) {
            throw new IllegalArgumentException("incomplete M3 physical file/project coordinates");
        }
        for (int owner : fileProject) {
            if (owner < -1 || owner >= projects) {
                throw new IllegalArgumentException("invalid reactor project coordinate");
            }
        }
        if (verified) validate(projects, dependentOffsets, dependentTargets);
        else if (dependentOffsets != null || dependentTargets != null) {
            throw new IllegalArgumentException("unverified reactor graph must be absent");
        }
        int[] topology = verified ? topological(projects, dependentOffsets, dependentTargets)
                : identity(projects);
        byte[] reasons = new byte[projects];
        int[] affectedFiles = fileImpact.impactedSlots();
        boolean conservative = !verified || fileImpact.requiresConservativeBuild();
        for (int slot = 0; slot < snapshot.slots(); slot++) {
            int reason = fileImpact.reasonAt(slot);
            boolean direct = (reason & (M3BuildImpactPlan.SOURCE_DELTA
                    | M3BuildImpactPlan.CLASS_DELTA | M3BuildImpactPlan.BUILD_INPUT_DELTA)) != 0;
            if (direct != snapshot.changedAt(slot)) {
                throw new IllegalArgumentException("file plan belongs to different delta generation");
            }
        }
        for (int slot : affectedFiles) {
            int owner = fileProject[slot];
            if (owner >= 0) reasons[owner] |= (byte) FILE_IMPACT;
            else conservative = true;
        }
        if (affectedFiles.length == 0) {
            return new M3ReactorImpactPlan(reasons, new int[0], false);
        }
        if (projects == 0) {
            throw new IllegalArgumentException("nonempty file impact without reactor projects");
        }
        if (conservative) {
            Arrays.fill(reasons, (byte) CONSERVATIVE);
            return new M3ReactorImpactPlan(reasons, topology, true);
        }
        int[] queue = new int[projects];
        byte[] queued = new byte[projects];
        int write = 0;
        for (int i = 0; i < projects; i++) {
            if ((reasons[i] & FILE_IMPACT) != 0) {
                queue[write++] = i;
                queued[i] = 1;
            }
        }
        for (int read = 0; read < write; read++) {
            int current = queue[read];
            for (int edge = dependentOffsets[current]; edge < dependentOffsets[current + 1]; edge++) {
                int next = dependentTargets[edge];
                if ((reasons[next] & DEPENDENT_PROJECT) == 0) {
                    reasons[next] |= (byte) DEPENDENT_PROJECT;
                    if (queued[next] == 0) {
                        queue[write++] = next;
                        queued[next] = 1;
                    }
                }
            }
        }
        int total = 0;
        for (int project : topology) if (reasons[project] != 0) total++;
        int[] selected = new int[total];
        for (int i = 0, at = 0; i < topology.length; i++) {
            if (reasons[topology[i]] != 0) selected[at++] = topology[i];
        }
        return new M3ReactorImpactPlan(reasons, selected, false);
    }

    private static int[] identity(int projects) {
        int[] indices = new int[projects];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        return indices;
    }

    private static void validate(int projects, int[] offsets, int[] targets) {
        Objects.requireNonNull(offsets, "dependentOffsets");
        Objects.requireNonNull(targets, "dependentTargets");
        if (offsets.length != projects + 1 || offsets[0] != 0
                || offsets[projects] != targets.length) {
            throw new IllegalArgumentException("invalid reactor CSR dimension");
        }
        for (int i = 0; i < projects; i++) {
            if (offsets[i] < 0 || offsets[i] > offsets[i + 1]
                    || offsets[i + 1] > targets.length) {
                throw new IllegalArgumentException("invalid reactor CSR offset");
            }
        }
        for (int target : targets) {
            if (target < 0 || target >= projects) {
                throw new IllegalArgumentException("invalid dependent project slot");
            }
        }
    }

    /** Bounded Kahn traversal; no boxed project objects and cycle refusal. */
    private static int[] topological(int projects, int[] offsets, int[] targets) {
        int[] indegree = new int[projects];
        for (int target : targets) indegree[target] = Math.addExact(indegree[target], 1);
        int[] queue = new int[projects];
        int write = 0;
        for (int p = 0; p < projects; p++) if (indegree[p] == 0) queue[write++] = p;
        for (int read = 0; read < write; read++) {
            int parent = queue[read];
            for (int edge = offsets[parent]; edge < offsets[parent + 1]; edge++) {
                int dependent = targets[edge];
                if (--indegree[dependent] == 0) queue[write++] = dependent;
            }
        }
        if (write != projects) {
            throw new IllegalArgumentException("cyclic Maven/Gradle reactor dependency graph");
        }
        return queue;
    }
}
