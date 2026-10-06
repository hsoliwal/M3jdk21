// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3NebulaTransferAdmissionTest {
    @Test
    void completeAuthorityFreeBundleIsContentAddressedAndAdmitted() throws Exception {
        Path bundle = fixture(false, false, true, true);
        M3NebulaTransferAdmission.Receipt receipt =
                M3NebulaTransferAdmission.read(bundle);

        assertTrue(receipt.transferRoot().matches("[0-9a-f]{64}"));
        assertTrue(receipt.dagRoot().matches("[0-9a-f]{64}"));
        assertTrue(receipt.orchestratorPlansRoot().matches("[0-9a-f]{64}"));
        assertTrue(receipt.admissionTsv().contains(
                "targetLane	JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES"));
        assertFalse(M3NebulaTransferAdmission.sourceMutationAuthority());
        assertFalse(M3NebulaTransferAdmission.semanticEquivalenceAuthority());
        assertFalse(M3NebulaTransferAdmission.sourceCopyAuthority());
        assertFalse(M3NebulaTransferAdmission.nativeExecutionAuthority());
        assertFalse(M3NebulaTransferAdmission.promotionAuthority());
    }

    @Test
    void targetOrSchedulerAuthorityFailsClosed() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(fixture(true, false, true, true)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(fixture(false, true, true, true)));
    }

    @Test
    void missingM3JdkTargetOrUnboundSchedulerPlanFailsClosed() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(fixture(false, false, false, true)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(fixture(false, false, true, false)));
    }

    @Test
    void transferAndOrchestratorRootsAreRecomputedNotTrusted() throws Exception {
        Path transfer = fixture(false, false, true, true);
        Files.writeString(
                transfer.resolve("transfer.sha256"),
                "0".repeat(64) + "
",
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(transfer));

        Path plans = fixture(false, false, true, true);
        Files.writeString(
                plans.resolve("orchestrator-plans.sha256"),
                "f".repeat(64) + "
",
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(plans));
    }

    @Test
    void metadataVersionAndLaneDriftFailClosed() throws Exception {
        Path bundle = fixture(false, false, true, true);
        Path metadata = bundle.resolve("transfer-metadata.tsv");
        Files.writeString(
                metadata,
                Files.readString(metadata).replace(
                        "openRewriteVersion	8.90.4",
                        "openRewriteVersion	9.0.0"),
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(bundle));

        Path scope = fixture(false, false, true, true);
        Path scopeMetadata = scope.resolve("transfer-metadata.tsv");
        Files.writeString(
                scopeMetadata,
                Files.readString(scopeMetadata).replace(
                        "refactorScopeOrder\tFILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API",
                        "refactorScopeOrder\tWRONG_SCOPE_ORDER"),
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(scope));

        Path lane = fixture(false, false, true, true);
        Path targets = lane.resolve("transfer-targets.tsv");
        Files.writeString(
                targets,
                Files.readString(targets).replace(
                        M3NebulaTransferAdmission.TARGET_LANE,
                        "WRONG_LANE"),
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaTransferAdmission.read(lane));
    }

    static Path fixture(
            boolean targetAuthority,
            boolean schedulerAuthority,
            boolean includeM3Jdk,
            boolean plansBindDag)
            throws IOException {
        Path dir = Files.createTempDirectory("m3-nebula-transfer-");
        String dagRoot = "a".repeat(64);
        String dag = "ordinal	id	recipeClass	scope	authority
"
                + "0	inventory	example.Inventory	FILE	READ_ONLY
";
        String planRootMarker = plansBindDag ? dagRoot : "b".repeat(64);
        String camel = "# generated from DAG " + planRootMarker + "
- route: {}
";
        String airflow = "# generated from DAG " + planRootMarker + "
";
        String drools = "// generated from DAG " + planRootMarker + "
";
        String plansRoot =
                M3NebulaTransferAdmission.sha256(
                        framed(dagRoot)
                                + framed(camel)
                                + framed(airflow)
                                + framed(drools));
        String metadata =
                """
                key	value
                schema	NEBULA_M3_RECIPE_TRANSFER_V1
                javaRelease	21
                openRewriteVersion	8.90.4
                entrypoint	org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe
                refactorScopeOrder	FILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API
                dagRoot	%s
                orchestratorPlansRoot	%s
                directSourceMutationAuthority	false
                promotionAuthority	false
                """
                        .formatted(dagRoot, plansRoot);
        String targetFlag = Boolean.toString(targetAuthority);
        StringBuilder targets =
                new StringBuilder(
                        "repository	adapterLane	directSourceMutationAuthority	promotionAuthority
");
        if (includeM3Jdk) {
            targets.append("hsoliwal/M3jdk21	JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES	")
                    .append(targetFlag)
                    .append("	false
");
        }
        targets.append("hsoliwal/com.synexia	RECIPE_PACK_AND_MODULE_LANES	false	false
");

        String schedulerFlag = Boolean.toString(schedulerAuthority);
        String orchestrators =
                "orchestrator	mutationAuthority	promotionAuthority
"
                        + "AIRFLOW	" + schedulerFlag + "	false
"
                        + "CAMEL	false	false
"
                        + "DROOLS	false	false
"
                        + "MAVEN_OPENREWRITE	false	false
";
        String transferRoot =
                M3NebulaTransferAdmission.sha256(
                        framed(metadata)
                                + framed(dag)
                                + framed(targets.toString())
                                + framed(orchestrators)
                                + framed(camel)
                                + framed(airflow)
                                + framed(drools));

        write(dir, "transfer-metadata.tsv", metadata);
        write(dir, "transfer-dag.tsv", dag);
        write(dir, "transfer-targets.tsv", targets.toString());
        write(dir, "orchestrators.tsv", orchestrators);
        write(dir, "camel-route.yaml", camel);
        write(dir, "airflow-dag.py", airflow);
        write(dir, "drools-agenda.drl", drools);
        write(dir, "orchestrator-plans.sha256", plansRoot + "
");
        write(dir, "transfer.sha256", transferRoot + "
");
        return dir;
    }

    private static void write(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static String framed(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return bytes.length + ":" + value;
    }
}
