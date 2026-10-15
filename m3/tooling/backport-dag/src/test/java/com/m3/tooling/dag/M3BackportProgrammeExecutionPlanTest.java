// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void wholeProgrammeRowsAdmitNonJepJbsPacketsWithoutChangingLegacySchema() {
        String programme =
                "order\twork_id\tsource_kind\trelease\tjep\ttitle\trecipe_state\trecipe_refs\t"
                        + "atom_count\tready_atoms\theld_atoms\tprogramme_state\tnext_action\n"
                        + "0\tJDK-8357439\tUPSTREAM_CHANGE\t27\t0\tBash completion\t"
                        + "MATERIALIZED_RECIPE\tjdk-8357439-bash\t1\t1\t0\tFILE_READY\tRUN\n";
        String packet =
                M3BackportPacketLoader.HEADER
                        + "\n"
                        + "jdk-8357439\tjava\tFILE\tfalse\t"
                        + "m3/backports/recipes/jdk-8357439-bash\t\n";
        String index =
                "work_id\tstate\tpacket\n"
                        + "JDK-8357439\tMATERIALIZED_RECIPE\tjdk-8357439.tsv\n";

        var plan =
                M3BackportProgrammeExecutionPlan.compile(
                        programme, index, Map.of("jdk-8357439.tsv", packet));

        var row = plan.rows().getFirst();
        assertEquals("JDK-8357439", row.workId());
        assertEquals("UPSTREAM_CHANGE", row.sourceKind());
        assertEquals(0, row.jep());
        assertEquals(M3BackportProgrammeExecutionPlan.State.RUNNABLE, row.state());
        assertTrue(row.schedulerProjection());
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
        assertTrue(receipt.matches("(?s).*jep-423\t[0-9a-f]{64}\t[0-9]+\n.*"));
    }


    @Test
    void receiptRootBindsReleaseAndCannotBeForged() {
        String index = "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n";
        Map<String, String> packets =
                Map.of("jep-423.tsv", packet("jep-423", "FILE", false));
        var original = M3BackportProgrammeExecutionPlan.compile(
                header() + "\n" + row(22, 423, "FILE_READY", "RUN") + "\n",
                index,
                packets);
        var differentRelease = M3BackportProgrammeExecutionPlan.compile(
                header() + "\n" + row(23, 423, "FILE_READY", "RUN") + "\n",
                index,
                packets);

        assertFalse(original.root().equals(differentRelease.root()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportProgrammeExecutionPlan.Plan(
                        original.rows(), "0".repeat(64)));

        var row = original.rows().getFirst();
        var forged = new M3BackportProgrammeExecutionPlan.Row(
                row.order(), row.workId(), row.sourceKind(), row.release() + 1,
                row.jep(), row.programmeState(), row.state(), row.packet(),
                row.scope(), row.dagRoot(), row.nextAction(),
                row.schedulerProjection(), row.productWriteAuthority(), row.promotionAuthority());
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportProgrammeExecutionPlan.Plan(
                        List.of(forged), original.root()));
    }

    @Test
    void packetIdAndIndexFilenameAreBoundToTheExactWorkItem() {
        String programme =
                header() + "\n" + row(22, 423, "FILE_READY", "RUN") + "\n";
        String index =
                "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        programme, index,
                        Map.of("jep-423.tsv", packet("jep-999", "FILE", false))));

        String swappedName =
                "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-999.tsv\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        programme, swappedName,
                        Map.of("jep-999.tsv", packet("jep-999", "FILE", false))));
    }

    @Test
    void unmaterializedPacketIndexCannotAuthorizeRunnableWork() {
        String programme =
                header() + "\n" + row(22, 423, "FILE_READY", "RUN") + "\n";
        String pending =
                "work_id\tstate\tpacket\nJEP-423\tAUTHOR_RECIPE\tjep-423.tsv\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        programme, pending,
                        Map.of("jep-423.tsv", packet("jep-423", "FILE", false))));

        String materializedSet =
                "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE_SET\tjep-423.tsv\n";
        var admitted = M3BackportProgrammeExecutionPlan.compile(
                programme, materializedSet,
                Map.of("jep-423.tsv", packet("jep-423", "FILE", false)));
        assertTrue(admitted.rows().getFirst().schedulerProjection());
    }

    @Test
    void duplicateWorkRowsAndAmbiguousTsvHeadersFailClosed() {
        String hold = row(22, 423, "HOLD", "WAIT");
        String extraneousIndex =
                "work_id\tstate\tpacket\nJEP-999\tAUTHOR_RECIPE\tjep-999.tsv\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        header() + "\n" + hold + "\n" + hold + "\n",
                        extraneousIndex, Map.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        header().replace("release\tjep", "release\trelease")
                                + "\n" + hold + "\n",
                        extraneousIndex, Map.of()));
    }

    @Test
    void externallyReportedProgrammeOrderMustBeContiguous() {
        String programme =
                "order\twork_id\tsource_kind\trelease\tjep\tprogramme_state\tnext_action\n"
                        + "3\tJEP-423\tJEP\t22\t423\tFILE_READY\tRUN\n";
        String index =
                "work_id\tstate\tpacket\nJEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportProgrammeExecutionPlan.compile(
                        programme, index,
                        Map.of("jep-423.tsv", packet("jep-423", "FILE", false))));
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
