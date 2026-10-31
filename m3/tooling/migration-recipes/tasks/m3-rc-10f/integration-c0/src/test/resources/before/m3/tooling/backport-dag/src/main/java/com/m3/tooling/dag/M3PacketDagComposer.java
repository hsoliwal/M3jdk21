// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composes one concrete backport packet into the canonical M3 lifecycle DAG.
 *
 * <p>The generic canonical {@code recipe-crate} node is replaced by the packet's immutable recipe
 * atoms. Packet roots depend on {@code file-delta}; packet-local dependencies remain packet-local;
 * all terminal packet atoms rejoin at {@code recipe-junit}. Every later canonical verification and
 * the single serial promotion node are preserved.</p>
 */
public final class M3PacketDagComposer {
    private static final String GENERIC_RECIPE = "recipe-crate";
    private static final String FILE_DELTA = "file-delta";
    private static final String RECIPE_JUNIT = "recipe-junit";

    private M3PacketDagComposer() {}

    public static M3RecipeDag compose(M3BackportPacket packet) {
        return compose(M3RecipeDag.canonical(), packet);
    }

    /**
     * Composes only after every packet atom has complete contract/documentation/pattern/IOP/JUnit
     * evidence and an explicit fixed-point requirement.
     */
    public static M3RecipeDag compose(
            M3BackportPacket packet,
            M3BackportPacketEvidence evidence) {
        M3BackportPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(evidence, "evidence").requireComplete(checkedPacket);
        return compose(checkedPacket);
    }

    static M3RecipeDag compose(M3RecipeDag canonical, M3BackportPacket packet) {
        M3RecipeDag checkedDag = Objects.requireNonNull(canonical, "canonical");
        M3BackportPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        checkedDag.require(GENERIC_RECIPE);
        checkedDag.require(FILE_DELTA);
        checkedDag.require(RECIPE_JUNIT);

        ArrayList<M3DagNode> nodes = new ArrayList<>();
        for (M3DagNode node : checkedDag.topologicalOrder()) {
            if (GENERIC_RECIPE.equals(node.id())) {
                continue;
            }
            if (RECIPE_JUNIT.equals(node.id())) {
                nodes.add(
                        new M3DagNode(
                                node.id(),
                                node.kind(),
                                node.scope(),
                                node.mutating(),
                                node.serialPromotion(),
                                node.scopePromotionApproved(),
                                node.workRef(),
                                checkedPacket.terminalAtoms().stream()
                                        .map(M3PacketDagComposer::nodeId)
                                        .toList()));
                continue;
            }
            if ("promote".equals(node.id())) {
                nodes.add(
                        new M3DagNode(
                                node.id(),
                                node.kind(),
                                checkedPacket.maximumScope(),
                                node.mutating(),
                                node.serialPromotion(),
                                node.scopePromotionApproved(),
                                node.workRef(),
                                node.dependsOn()));
                continue;
            }
            nodes.add(node);
        }

        for (M3RecipeAtom atom : checkedPacket.atoms()) {
            nodes.add(
                    new M3DagNode(
                            nodeId(atom),
                            M3DagKind.RECIPE,
                            atom.scope(),
                            true,
                            false,
                            atom.scopePromotionApproved(),
                            atom.workRef(),
                            atom.dependsOn().isEmpty()
                                    ? List.of(FILE_DELTA)
                                    : atom.dependsOn().stream()
                                            .map(M3PacketDagComposer::nodeId)
                                            .toList()));
        }

        return new M3RecipeDag(nodes);
    }

    public static String nodeId(M3RecipeAtom atom) {
        return "packet-" + Objects.requireNonNull(atom, "atom").id();
    }
}
