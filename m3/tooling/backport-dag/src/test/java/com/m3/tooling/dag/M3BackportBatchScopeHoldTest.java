// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Generated backport packets must never acquire scope-promotion authority from a domain heuristic.
 *
 * <p>Broad-scope packets remain useful planning records, but cannot compose into the executable
 * canonical DAG until a separately reviewed packet grants promotion explicitly.</p>
 */
final class M3BackportBatchScopeHoldTest {
    @TempDir Path directory;

    @Test
    void unapprovedModulePacketIsHeldWhileIndependentFilePacketStillValidates() throws Exception {
        String header = M3BackportPacketLoader.HEADER + "\n";
        Files.writeString(
                directory.resolve("a-file.tsv"),
                header + "file-candidate\tinventory\tFILE\tfalse\tPROOF:file\t\n");
        Files.writeString(
                directory.resolve("b-module.tsv"),
                header + "module-candidate\tinventory\tMODULE\tfalse\tPROOF:module\t\n");

        M3BackportPacket unapproved = M3BackportPacketLoader.parse(
                Files.readString(directory.resolve("b-module.tsv")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PacketDagComposer.compose(unapproved));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream captured = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            assertDoesNotThrow(() -> M3BackportPacketBatchMain.main(
                    new String[] {directory.toString()}));
        } finally {
            System.setOut(original);
        }

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("M3_BACKPORT_PACKET_OK packet=file-candidate"));
        assertTrue(output.contains("M3_BACKPORT_PACKET_SCOPE_HOLD packet=module-candidate"));
        assertTrue(output.contains("reason=EXPLICIT_SCOPE_PROMOTION_APPROVAL_REQUIRED"));
        assertTrue(output.contains("validatedDags=1 heldScopePackets=1"));
        assertTrue(output.contains("sourcePromotionAuthority=false"));
    }

    @Test
    void explicitReviewedFlagIsNotInferredButCanBeSuppliedBySeparatePacket() throws Exception {
        String approved = M3BackportPacketLoader.HEADER + "\n"
                + "explicit-packet\tinventory\tMODULE\ttrue\tREVIEWED:module\t\n";
        M3BackportPacket packet = M3BackportPacketLoader.parse(approved);
        assertTrue(packet.atoms().getFirst().scopePromotionApproved());
        assertDoesNotThrow(() -> M3PacketDagComposer.compose(packet));
    }
}
