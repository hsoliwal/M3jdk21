// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI validator/projection for one concrete packet TSV.
 *
 * <p>This command validates and composes the packet against the canonical lifecycle DAG. It does
 * not execute recipes or promote the packet.</p>
 */
public final class M3BackportPacketMain {
    private M3BackportPacketMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: M3BackportPacketMain <packet.tsv>");
        }
        Path path = Path.of(args[0]).normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("packet file not found: " + path);
        }

        M3BackportPacket packet =
                M3BackportPacketLoader.parse(Files.readString(path));
        M3RecipeDag dag = M3PacketDagComposer.compose(packet);

        System.out.println(
                "M3_BACKPORT_PACKET_OK packet="
                        + packet.packetId()
                        + " atoms="
                        + packet.atoms().size()
                        + " scope="
                        + packet.maximumScope()
                        + " dagNodes="
                        + dag.size());
        int ordinal = 0;
        for (List<M3DagNode> layer : dag.layers()) {
            System.out.println(
                    "layer="
                            + ordinal++
                            + " nodes="
                            + layer.stream().map(M3DagNode::id).toList());
        }
    }
}
