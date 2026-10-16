// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Compile runnable backport work and write Camel/Airflow/Drools projections. */
public final class M3BackportProgrammeExecutionPlanMain {
    private M3BackportProgrammeExecutionPlanMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: M3BackportProgrammeExecutionPlanMain <programme.tsv> <packet-index.tsv> <packet-dir> <output-dir>");
        }
        Path programme = Path.of(args[0]).normalize();
        Path packetIndex = Path.of(args[1]).normalize();
        Path packetDir = Path.of(args[2]).normalize();
        Path output = Path.of(args[3]).normalize();
        if (!Files.isRegularFile(programme) || !Files.isRegularFile(packetIndex) || !Files.isDirectory(packetDir)) {
            throw new IllegalArgumentException("missing execution-plan input");
        }

        Map<String, String> packets = new LinkedHashMap<>();
        try (var stream = Files.list(packetDir)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".tsv"))
                    .sorted()
                    .toList()) {
                packets.put(path.getFileName().toString(), Files.readString(path));
            }
        }

        M3BackportProgrammeExecutionPlan.Plan plan =
                M3BackportProgrammeExecutionPlan.compile(
                        Files.readString(programme), Files.readString(packetIndex), packets);
        Files.createDirectories(output);
        Files.writeString(output.resolve("execution-plan.tsv"), M3BackportProgrammeExecutionPlan.render(plan));

        for (M3BackportProgrammeExecutionPlan.Row row : plan.rows()) {
            if (!row.schedulerProjection()) continue;
            String packetText = packets.get(row.packet());
            M3BackportPacket packet = M3BackportPacketLoader.parse(packetText);
            M3RecipeDag dag = M3PacketDagComposer.compose(packet);
            Path target = output.resolve("projections").resolve(packet.packetId());
            Files.createDirectories(target);
            for (M3DagProjection projection : M3DagProjector.all(dag, packet.packetId()).values()) {
                Files.writeString(target.resolve(projection.fileName()), projection.content());
            }
            Files.writeString(
                    target.resolve("projection.tsv"),
                    "packet_id\tdag_root\tnodes\n"
                            + packet.packetId() + "\t" + row.dagRoot() + "\t" + dag.size() + "\n");
        }

        long runnable = plan.rows().stream()
                .filter(M3BackportProgrammeExecutionPlan.Row::schedulerProjection)
                .count();
        System.out.println(
                "M3_BACKPORT_PROGRAMME_EXECUTION_PLAN_OK rows="
                        + plan.rows().size()
                        + " runnable="
                        + runnable
                        + " root="
                        + plan.root()
                        + " productWriteAuthority=false promotionAuthority=false");
    }
}
