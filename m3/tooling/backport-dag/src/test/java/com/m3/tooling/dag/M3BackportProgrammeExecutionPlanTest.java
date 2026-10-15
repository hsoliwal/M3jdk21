// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3BackportProgrammeExecutionPlanTest {
    @TempDir Path temp;

    @Test
    void fileReadyBecomesRunnableAndHoldsRemainTyped() {
        String programme =
                header()
                        + "\n"
                        + row(22, 423, "FILE_READY", "RUN_FILE_BACKPORT")
                        + "\n"
                        + row(24, 484, "AUTHOR_RECIPE", "AUTHOR_RECIPE")
                        + "\n"
                        + row(27, 534, "EVIDENCE_HOLD", "REPAIR_EVIDENCE")
                        + "\n"
                        + row(27, 538, "HOLD", "WAIT_FINAL")
                        + "\n"
                        + row(25, 511, "REJECT", "NO_LANGUAGE_BACKPORT")
                        + "\n";
        String packet = packet("jep-423", "FILE", false);
        String index = "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n";

        var plan =
                M3BackportProgrammeExecutionPlan.compile(
                        programme, index, Map.of("jep-423.tsv", packet));

        assertEquals(5, plan.rows().size());
        assertEquals(M3BackportProgrammeExecutionPlan.State.RUNNABLE, plan.rows().get(0).state());
        assertTrue(plan.rows().get(0).schedulerProjection());
        assertTrue(plan.rows().get(0).dagRoot().matches("[0-9a-f]{64}"));
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.AUTHOR_RECIPE,
                plan.rows().get(1).state());
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.EVIDENCE_HOLD,
                plan.rows().get(2).state());
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.POLICY_HOLD,
                plan.rows().get(3).state());
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.REJECT,
                plan.rows().get(4).state());
        assertTrue(plan.root().matches("[0-9a-f]{64}"));
        assertTrue(plan.rows().stream().noneMatch(M3BackportProgrammeExecutionPlan.Row::productWriteAuthority));
        assertTrue(plan.rows().stream().noneMatch(M3BackportProgrammeExecutionPlan.Row::promotionAuthority));
    }

    @Test
    void broaderScopeRequiresExplicitApproval() {
        String programme = header() + "\n" + row(24, 484, "SCOPE_READY", "RUN_MODULE_BACKPORT") + "\n";
        String index = "work_id\tstate\tpacket\nJEP-484\tMATERIALIZED_RECIPE\tjep-484.tsv\n";

        var held =
                M3BackportProgrammeExecutionPlan.compile(
                        programme,
                        index,
                        Map.of("jep-484.tsv", packet("jep-484", "MODULE", false)));
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.SCOPE_APPROVAL_HOLD,
                held.rows().getFirst().state());
        assertFalse(held.rows().getFirst().schedulerProjection());
        assertEquals("APPROVE_SCOPE_PROMOTION_THEN_REPLAN", held.rows().getFirst().nextAction());

        var approved =
                M3BackportProgrammeExecutionPlan.compile(
                        programme,
                        index,
                        Map.of("jep-484.tsv", packet("jep-484", "MODULE", true)));
        assertEquals(
                M3BackportProgrammeExecutionPlan.State.RUNNABLE,
                approved.rows().getFirst().state());
        assertTrue(approved.rows().getFirst().schedulerProjection());
    }

    @Test
    void fileReadyCannotHideBroaderScopeOrMissingPacket() {
        String programme = header() + "\n" + row(22, 423, "FILE_READY", "RUN") + "\n";
        String index = "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n";

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportProgrammeExecutionPlan.compile(
                                programme,
                                index,
                                Map.of("jep-423.tsv", packet("jep-423", "MODULE", true))));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportProgrammeExecutionPlan.compile(
                                programme,
                                index,
                                Map.of()));
    }

    @Test
    void cliWritesExecutionPlanAndAllSchedulerProjections() throws Exception {
        Path status = temp.resolve("status.tsv");
        Files.writeString(status, header() + "\n" + row(22, 423, "FILE_READY", "RUN") + "\n");
        Path index = temp.resolve("packets.tsv");
        Files.writeString(
                index,
                "work_id\tstate\tpacket\n"
                        + "JEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n");
        Path packets = temp.resolve("packets");
        Files.createDirectories(packets);
        Files.writeString(packets.resolve("jep-423.tsv"), packet("jep-423", "FILE", false));
        Path out = temp.resolve("out");

        M3BackportProgrammeExecutionPlanMain.main(
                new String[] {
                    status.toString(), index.toString(), packets.toString(), out.toString()
                });

        assertTrue(Files.isRegularFile(out.resolve("execution-plan.tsv")));
        Path projection = out.resolve("projections/jep-423");
        assertTrue(Files.isRegularFile(projection.resolve("M3BackportRoutes.java")));
        assertTrue(Files.isRegularFile(projection.resolve("m3_backport_dag.py")));
        assertTrue(Files.isRegularFile(projection.resolve("m3_backport_rules.drl")));
        String receipt = Files.readString(projection.resolve("projection.tsv"));
        assertTrue(receipt.contains("jep-423"));
        assertTrue(receipt.matches("(?s).*jep-423\\t[0-9a-f]{64}\\t[0-9]+\\n.*"));
    }

    private static String header() {
        return "release\tjep\ttitle\tdomain\tdisposition\trecipe_state\trecipe_refs\t"
                + "atom_count\tready_atoms\theld_atoms\tprogramme_state\tnext_action\treason";
    }

    private static String row(int release, int jep, String state, String action) {
        return release
                + "\t"
                + jep
                + "\tFeature "
                + jep
                + "\tlibrary\tcandidate\tMATERIALIZED_RECIPE\trecipe\t2\t2\t0\t"
                + state
                + "\t"
                + action
                + "\ttest";
    }

    private static String packet(String packetId, String scope, boolean approved) {
        return M3BackportPacketLoader.HEADER
                + "\n"
                + packetId
                + "\tjava\t"
                + scope
                + "\t"
                + approved
                + "\trecipe.Java\t\n";
    }
}
