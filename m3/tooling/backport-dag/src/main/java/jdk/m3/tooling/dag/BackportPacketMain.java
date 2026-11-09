// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI validator/projection for one concrete packet TSV.
 *
 * <p>This command validates and composes the packet against the canonical lifecycle DAG. It does
 * not execute recipes or promote the packet.</p>
 */
public final class BackportPacketMain {
    private BackportPacketMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException(
                    "usage: BackportPacketMain <packet.tsv> [atom-evidence.tsv]");
        }
        Path path = Path.of(args[0]).normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("packet file not found: " + path);
        }

        BackportPacket packet =
                BackportPacketLoader.parse(Files.readString(path));
        BackportPacketEvidence evidence = null;
        RecipeDag dag;
        if (args.length == 2) {
            Path evidencePath = Path.of(args[1]).normalize();
            if (!Files.isRegularFile(evidencePath)) {
                throw new IllegalArgumentException(
                        "atom evidence file not found: " + evidencePath);
            }
            evidence =
                    BackportPacketEvidenceLoader.parse(Files.readString(evidencePath));
            dag = PacketDagComposer.compose(packet, evidence);
        } else {
            dag = PacketDagComposer.compose(packet);
        }

        System.out.println(
                "M3_BACKPORT_PACKET_OK packet="
                        + packet.packetId()
                        + " atoms="
                        + packet.atoms().size()
                        + " scope="
                        + packet.maximumScope()
                        + " dagNodes="
                        + dag.size()
                        + (evidence == null
                                ? ""
                                : " evidenceRoot=" + AtomEvidenceRoot.of(packet, evidence)));
        int ordinal = 0;
        for (List<DagNode> layer : dag.layers()) {
            System.out.println(
                    "layer="
                            + ordinal++
                            + " nodes="
                            + layer.stream().map(DagNode::id).toList());
        }
    }
}
