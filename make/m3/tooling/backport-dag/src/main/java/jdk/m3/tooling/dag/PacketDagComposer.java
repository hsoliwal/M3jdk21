// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composes one concrete backport packet into the canonical M3 lifecycle DAG.
 *
 * <p>The generic canonical {@code recipe-crate} node is replaced by the packet's immutable recipe
 * atoms. Packet roots depend on {@code a3-preparation}, which itself follows exact file-delta
 * inventory; packet-local dependencies remain packet-local;
 * all terminal packet atoms rejoin at {@code recipe-junit}. Every later canonical verification and
 * the single serial promotion node are preserved.</p>
 */
public final class PacketDagComposer {
    private static final String GENERIC_RECIPE = "recipe-crate";
    private static final String FILE_DELTA = "file-delta";
    private static final String A3_PREPARATION = "a3-preparation";
    private static final String RECIPE_JUNIT = "recipe-junit";

    private PacketDagComposer() {}

    public static RecipeDag compose(BackportPacket packet) {
        return compose(RecipeDag.canonical(), packet);
    }

    /**
     * Composes only after every packet atom has complete contract/documentation/pattern/IOP/JUnit
     * evidence and an explicit fixed-point requirement.
     */
    public static RecipeDag compose(
            BackportPacket packet,
            BackportPacketEvidence evidence) {
        BackportPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(evidence, "evidence").requireComplete(checkedPacket);
        return compose(checkedPacket);
    }

    static RecipeDag compose(RecipeDag canonical, BackportPacket packet) {
        RecipeDag checkedDag = Objects.requireNonNull(canonical, "canonical");
        BackportPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        checkedDag.require(GENERIC_RECIPE);
        checkedDag.require(FILE_DELTA);
        checkedDag.require(A3_PREPARATION);
        checkedDag.require(RECIPE_JUNIT);

        ArrayList<DagNode> nodes = new ArrayList<>();
        for (DagNode node : checkedDag.topologicalOrder()) {
            if (GENERIC_RECIPE.equals(node.id())) {
                continue;
            }
            if (RECIPE_JUNIT.equals(node.id())) {
                nodes.add(
                        new DagNode(
                                node.id(),
                                node.kind(),
                                node.scope(),
                                node.mutating(),
                                node.serialPromotion(),
                                node.scopePromotionApproved(),
                                node.workRef(),
                                checkedPacket.terminalAtoms().stream()
                                        .map(PacketDagComposer::nodeId)
                                        .toList()));
                continue;
            }
            if ("promote".equals(node.id())) {
                nodes.add(
                        new DagNode(
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

        for (RecipeAtom atom : checkedPacket.atoms()) {
            nodes.add(
                    new DagNode(
                            nodeId(atom),
                            DagKind.RECIPE,
                            atom.scope(),
                            true,
                            false,
                            atom.scopePromotionApproved(),
                            atom.workRef(),
                            atom.dependsOn().isEmpty()
                                    ? List.of(A3_PREPARATION)
                                    : atom.dependsOn().stream()
                                            .map(PacketDagComposer::nodeId)
                                            .toList()));
        }

        return new RecipeDag(nodes);
    }

    public static String nodeId(RecipeAtom atom) {
        return "packet-" + Objects.requireNonNull(atom, "atom").id();
    }
}
