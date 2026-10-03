// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3OrchestrationProjectionMainTest {
    @TempDir Path temp;

    @Test
    void cliWritesAllFrameworkArtifactsAndOneRootReceipt() throws Exception {
        Path packet = temp.resolve("packet.tsv");
        Files.writeString(
                packet,
                M3BackportPacketLoader.HEADER
                        + "\n"
                        + "jdk-cli\tjava\tFILE\tfalse\trecipe.Java\t\n"
                        + "jdk-cli\ttext\tFILE\tfalse\trecipe.Text\t\n");
        Path output = temp.resolve("projection");

        M3OrchestrationProjectionMain.main(
                new String[] {packet.toString(), output.toString()});

        assertTrue(Files.isRegularFile(output.resolve("M3BackportRoutes.java")));
        assertTrue(Files.isRegularFile(output.resolve("m3_backport_dag.py")));
        assertTrue(Files.isRegularFile(output.resolve("m3_backport_rules.drl")));
        Path receipt = output.resolve("projection.tsv");
        assertTrue(Files.isRegularFile(receipt));

        String[] rows = Files.readString(receipt).lines().toArray(String[]::new);
        assertEquals("packet_id\tdag_root\tnodes", rows[0]);
        String[] fields = rows[1].split("\t");
        assertEquals("jdk-cli", fields[0]);
        assertTrue(fields[1].matches("[0-9a-f]{64}"));
        assertEquals(
                fields[1],
                M3DagProjector.project(
                                M3PacketDagComposer.compose(
                                        M3BackportPacketLoader.parse(Files.readString(packet))),
                                "jdk-cli",
                                M3DagProjectionFormat.AIRFLOW_PYTHON)
                        .dagRoot());
    }
}
