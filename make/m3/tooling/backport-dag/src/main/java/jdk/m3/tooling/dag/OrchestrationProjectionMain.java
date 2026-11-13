// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes Camel/Airflow/Drools projections for one validated packet to an explicit output directory.
 *
 * <p>The command generates orchestration artifacts only. It does not execute recipes, mutate JDK
 * sources, widen edit scope, or promote a backport.</p>
 */
public final class OrchestrationProjectionMain {
    private OrchestrationProjectionMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "usage: OrchestrationProjectionMain <packet.tsv> <output-dir> [atom-evidence.tsv]");
        }
        Path packetPath = Path.of(args[0]).normalize();
        Path output = Path.of(args[1]).normalize();
        if (!Files.isRegularFile(packetPath)) {
            throw new IllegalArgumentException("packet file not found: " + packetPath);
        }

        BackportPacket packet =
                BackportPacketLoader.parse(Files.readString(packetPath));
        BackportPacketEvidence evidence = null;
        RecipeDag dag;
        if (args.length == 3) {
            Path evidencePath = Path.of(args[2]).normalize();
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
        Map<DagProjectionFormat, DagProjection> projections =
                DagProjector.all(dag, packet.packetId());

        Files.createDirectories(output);
        for (DagProjection projection : projections.values()) {
            Files.writeString(output.resolve(projection.fileName()), projection.content());
        }
        String root = DagSemanticRoot.of(dag);
        Files.writeString(
                output.resolve("projection.tsv"),
                "packet_id\tdag_root\tnodes\n"
                        + packet.packetId()
                        + "\t"
                        + root
                        + "\t"
                        + dag.size()
                        + "\n");
        if (evidence != null) {
            Files.writeString(
                    output.resolve("atom-evidence.tsv"),
                    "packet_id\tevidence_root\tatoms\n"
                            + packet.packetId()
                            + "\t"
                            + AtomEvidenceRoot.of(packet, evidence)
                            + "\t"
                            + packet.atoms().size()
                            + "\n");
        }

        System.out.println(
                "M3_ORCHESTRATION_PROJECTIONS_OK packet="
                        + packet.packetId()
                        + " dagRoot="
                        + root
                        + " output="
                        + output);
    }
}
