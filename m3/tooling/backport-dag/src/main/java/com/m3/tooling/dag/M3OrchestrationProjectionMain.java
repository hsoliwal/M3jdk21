// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes Camel/Airflow/Drools projections for one validated packet to an explicit output directory.
 *
 * <p>The command generates orchestration artifacts only. It does not execute recipes, mutate JDK
 * sources, widen edit scope, or promote a backport.</p>
 */
public final class M3OrchestrationProjectionMain {
    private M3OrchestrationProjectionMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: M3OrchestrationProjectionMain <packet.tsv> <output-dir>");
        }
        Path packetPath = Path.of(args[0]).normalize();
        Path output = Path.of(args[1]).normalize();
        if (!Files.isRegularFile(packetPath)) {
            throw new IllegalArgumentException("packet file not found: " + packetPath);
        }

        M3BackportPacket packet =
                M3BackportPacketLoader.parse(Files.readString(packetPath));
        M3RecipeDag dag = M3PacketDagComposer.compose(packet);
        Map<M3DagProjectionFormat, M3DagProjection> projections =
                M3DagProjector.all(dag, packet.packetId());

        Files.createDirectories(output);
        for (M3DagProjection projection : projections.values()) {
            Files.writeString(output.resolve(projection.fileName()), projection.content());
        }
        String root = M3DagSemanticRoot.of(dag);
        Files.writeString(
                output.resolve("projection.tsv"),
                "packet_id\tdag_root\tnodes\n"
                        + packet.packetId()
                        + "\t"
                        + root
                        + "\t"
                        + dag.size()
                        + "\n");

        System.out.println(
                "M3_ORCHESTRATION_PROJECTIONS_OK packet="
                        + packet.packetId()
                        + " dagRoot="
                        + root
                        + " output="
                        + output);
    }
}
