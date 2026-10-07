// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Validates and composes every generated M3 backport packet in one JVM. */
public final class M3BackportPacketBatchMain {
    private M3BackportPacketBatchMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "usage: M3BackportPacketBatchMain <packet-directory>");
        }
        Path directory = Path.of(args[0]).normalize();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("packet directory not found: " + directory);
        }

        List<Path> packets;
        try (var stream = Files.list(directory)) {
            packets =
                    stream.filter(path -> path.getFileName().toString().endsWith(".tsv"))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .toList();
        }
        if (packets.isEmpty()) {
            throw new IllegalArgumentException("packet directory is empty: " + directory);
        }

        int atoms = 0;
        int maximumDagNodes = 0;
        for (Path path : packets) {
            M3BackportPacket packet =
                    M3BackportPacketLoader.parse(Files.readString(path));
            M3RecipeDag dag = M3PacketDagComposer.compose(packet);
            atoms += packet.atoms().size();
            maximumDagNodes = Math.max(maximumDagNodes, dag.size());
            System.out.println(
                    "M3_BACKPORT_PACKET_OK packet="
                            + packet.packetId()
                            + " atoms="
                            + packet.atoms().size()
                            + " scope="
                            + packet.maximumScope()
                            + " dagNodes="
                            + dag.size());
        }

        System.out.println(
                "M3_BACKPORT_PACKET_BATCH_OK packets="
                        + packets.size()
                        + " atoms="
                        + atoms
                        + " maxDagNodes="
                        + maximumDagNodes);
    }
}
