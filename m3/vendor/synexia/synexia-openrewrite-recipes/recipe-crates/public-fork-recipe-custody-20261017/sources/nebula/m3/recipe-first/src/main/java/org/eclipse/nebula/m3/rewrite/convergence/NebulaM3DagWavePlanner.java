// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded dependency-proof and deterministic scheduling waves for M3 OpenRewrite recipe atoms.
 *
 * <p>The existing manifest remains the sole recipe/phase/authority owner. This planner only
 * validates dependency evidence and identifies which nodes would be ready in the same wave.
 * Readiness is NOT permission to execute leaves in different JVMs, mutate a file, or promote
 * canonical source. In particular, same-wave nodes require a separately proven independent
 * file/contract fence before any scheduler may actually fan them out.</p>
 *
 * <p>Each phase forms a barrier: every node after INVENTORY transitively depends on every node
 * in the immediately preceding phase. Same-phase nodes may be independent, but phases cannot
 * overlap, disappear, or reorder. An ordinal/ID/authority/scope mismatch fails closed.</p>
 */
public final class NebulaM3DagWavePlanner {
    private static final int MAX_NODES = 4096;

    private NebulaM3DagWavePlanner() {}

    /**
     * Validates the complete FILE-scope plan and returns immutable readiness waves.
     *
     * <p>Source/target mutation remains exclusively owned by tested OpenRewrite recipes. The
     * planner neither reads nor writes source files and grants no execution/promotion authority.</p>
     */
    public static List<List<NebulaM3RecipeDagManifest.Node>> plan(
            List<NebulaM3RecipeDagManifest.Node> proposed) {
        List<NebulaM3RecipeDagManifest.Node> nodes =
                List.copyOf(Objects.requireNonNull(proposed, "proposed"));
        if (nodes.size() < NebulaM3FileConvergenceRecipeDag.Phase.values().length
                || nodes.size() > MAX_NODES) {
            throw new IllegalArgumentException("invalid recipe DAG node budget");
        }

        Map<String, Integer> positions = new HashMap<>();
        Map<NebulaM3FileConvergenceRecipeDag.Phase, List<Integer>> byPhase =
                new EnumMap<>(NebulaM3FileConvergenceRecipeDag.Phase.class);
        List<BitSet> ancestors = new ArrayList<>(nodes.size());
        int[] depth = new int[nodes.size()];
        int previousPhase = -1;
        int highestDepth = 0;

        for (int index = 0; index < nodes.size(); index++) {
            NebulaM3RecipeDagManifest.Node node = nodes.get(index);
            requireIdentity(node, index);
            int phaseIndex = node.phase().ordinal();
            if (phaseIndex < previousPhase || phaseIndex > previousPhase + 1) {
                throw new IllegalArgumentException("missing or reordered recipe phase: " + node.id());
            }
            previousPhase = phaseIndex;
            if (positions.containsKey(node.id())) {
                throw new IllegalArgumentException("duplicate recipe atom: " + node.id());
            }

            BitSet closure = new BitSet(nodes.size());
            Set<String> seenDependencies = new HashSet<>();
            int level = 0;
            for (String required : node.dependsOn()) {
                if (!seenDependencies.add(required)) {
                    throw new IllegalArgumentException(
                            "duplicate recipe dependency: " + node.id() + " -> " + required);
                }
                Integer predecessor = positions.get(required);
                if (predecessor == null) {
                    throw new IllegalArgumentException(
                            "missing, forward or cyclic dependency: " + node.id() + " -> " + required);
                }
                closure.set(predecessor);
                closure.or(ancestors.get(predecessor));
                level = Math.max(level, depth[predecessor] + 1);
            }

            if (phaseIndex > 0) {
                NebulaM3FileConvergenceRecipeDag.Phase precedingPhase =
                        NebulaM3FileConvergenceRecipeDag.Phase.values()[phaseIndex - 1];
                for (int predecessor : byPhase.get(precedingPhase)) {
                    if (!closure.get(predecessor)) {
                        throw new IllegalArgumentException(
                                "phase barrier bypassed: " + node.id() + " after "
                                        + nodes.get(predecessor).id());
                    }
                }
            } else if (!node.dependsOn().isEmpty()) {
                throw new IllegalArgumentException("inventory root cannot depend on other phases");
            }

            positions.put(node.id(), index);
            byPhase.computeIfAbsent(node.phase(), ignored -> new ArrayList<>()).add(index);
            ancestors.add(closure);
            depth[index] = level;
            highestDepth = Math.max(highestDepth, level);
        }

        if (byPhase.size() != NebulaM3FileConvergenceRecipeDag.Phase.values().length) {
            throw new IllegalArgumentException("incomplete recipe phase frontier");
        }

        List<List<NebulaM3RecipeDagManifest.Node>> mutableWaves = new ArrayList<>();
        for (int level = 0; level <= highestDepth; level++) {
            mutableWaves.add(new ArrayList<>());
        }
        for (int index = 0; index < nodes.size(); index++) {
            mutableWaves.get(depth[index]).add(nodes.get(index));
        }

        return mutableWaves.stream().map(List::copyOf).toList();
    }

    private static void requireIdentity(NebulaM3RecipeDagManifest.Node node, int index) {
        if (node.ordinal() != index
                || !node.id().matches("[a-z][a-z0-9-]{0,127}")
                || !"FILE".equals(node.scope())
                || !node.authority().equals(
                        node.phase().mutating() ? "CANDIDATE_ONLY" : "READ_ONLY")) {
            throw new IllegalArgumentException("invalid FILE recipe identity/authority: " + node.id());
        }
    }
}
