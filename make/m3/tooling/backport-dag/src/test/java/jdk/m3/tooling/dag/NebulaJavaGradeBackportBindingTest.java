// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaJavaGradeBackportBindingTest {
    @Test
    void exactNebulaCandidateIsAdmittedAsEvidenceButNeverExecutableAuthority() {
        NebulaJavaGradeBackportBinding.requireExactCandidate(
                "hsoliwal/nebula",
                "m3/java-grade-transfer-qualification-v3-20261015",
                "7a5a3cb5ffd82bca671b20e1496db80369657578",
                208,
                "HOLD_PENDING_NEBULA_PROOF",
                "INVENTORY_AND_ADAPTATION_ONLY_UNTIL_JAVA_GRADE_PROVEN");

        assertTrue(NebulaJavaGradeBackportBinding.evidenceAdmitted());
        assertFalse(NebulaJavaGradeBackportBinding.executableBackportDependency());
        assertFalse(NebulaJavaGradeBackportBinding.sourceMutationAuthority());
        assertFalse(NebulaJavaGradeBackportBinding.semanticEquivalenceAuthority());
        assertFalse(NebulaJavaGradeBackportBinding.compatibilityAuthority());
        assertFalse(NebulaJavaGradeBackportBinding.promotionAuthority());
        assertTrue(NebulaJavaGradeBackportBinding.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void exactFourNebulaAssetsAreGitBlobPinned() {
        assertEquals(
                List.of(
                        "java-grade",
                        "backport-plan",
                        "transfer-contract",
                        "java-grade-doc"),
                NebulaJavaGradeBackportBinding.assets().stream()
                        .map(NebulaJavaGradeBackportBinding.Asset::id)
                        .toList());
        assertEquals(
                List.of(
                        "80f3a2a0ca443972f367b5636d3a897a27eff88b",
                        "eddba9c6546749169b1a038147e06c4d8c863b4d",
                        "368b947eacef8355607706c90991af5b55b08bc2",
                        "d74b4180bf6b36a16d9c2f1362c19fcc5d38daf0"),
                NebulaJavaGradeBackportBinding.assets().stream()
                        .map(NebulaJavaGradeBackportBinding.Asset::gitBlobSha1)
                        .toList());
        assertTrue(
                NebulaJavaGradeBackportBinding.assets().stream()
                        .allMatch(asset -> !asset.path().isBlank()));
    }

    @Test
    void phaseOrderMatchesNebulaBackportProgrammeExactly() {
        assertEquals(
                List.of(
                        NebulaJavaGradeBackportBinding.Phase.NEBULA_PROOF,
                        NebulaJavaGradeBackportBinding.Phase.M3_RECIPE_ABSORPTION,
                        NebulaJavaGradeBackportBinding.Phase.M3_RECIPE_PROOF,
                        NebulaJavaGradeBackportBinding.Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                        NebulaJavaGradeBackportBinding.Phase.M3JDK21_BACKPORT_INVENTORY,
                        NebulaJavaGradeBackportBinding.Phase.M3JDK21_FILE_REPLAY,
                        NebulaJavaGradeBackportBinding.Phase.M3JDK21_SCOPE_PROMOTION,
                        NebulaJavaGradeBackportBinding.Phase.TARGET_PROOF),
                NebulaJavaGradeBackportBinding.steps().stream()
                        .map(NebulaJavaGradeBackportBinding.Step::phase)
                        .toList());

        for (int index = 0; index < NebulaJavaGradeBackportBinding.steps().size(); index++) {
            var step = NebulaJavaGradeBackportBinding.steps().get(index);
            assertEquals(index, step.ordinal());
            assertEquals(index < 6 ? "FILE" : "LIBRARY_API", step.maximumScope());
            if (index == 0) {
                assertEquals("", step.dependsOn());
                assertEquals("hsoliwal/nebula", step.repository());
            } else {
                assertEquals(
                        NebulaJavaGradeBackportBinding.steps()
                                .get(index - 1)
                                .phase()
                                .name(),
                        step.dependsOn());
            }
        }
        assertEquals("hsoliwal/com.synexia", NebulaJavaGradeBackportBinding.steps().get(1).repository());
        assertEquals("hsoliwal/com.synexia", NebulaJavaGradeBackportBinding.steps().get(2).repository());
        assertTrue(
                NebulaJavaGradeBackportBinding.steps().subList(3, 8).stream()
                        .allMatch(step -> "hsoliwal/M3jdk21".equals(step.repository())));
    }

    @Test
    void candidateIdentityOrPolicyDriftFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaJavaGradeBackportBinding.requireExactCandidate(
                        "hsoliwal/nebula",
                        "master",
                        NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        208,
                        NebulaJavaGradeBackportBinding.STATE,
                        NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaJavaGradeBackportBinding.requireExactCandidate(
                        NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        NebulaJavaGradeBackportBinding.NEBULA_REF,
                        "0".repeat(40),
                        208,
                        NebulaJavaGradeBackportBinding.STATE,
                        NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaJavaGradeBackportBinding.requireExactCandidate(
                        NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        NebulaJavaGradeBackportBinding.NEBULA_REF,
                        NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        209,
                        NebulaJavaGradeBackportBinding.STATE,
                        NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaJavaGradeBackportBinding.requireExactCandidate(
                        NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        NebulaJavaGradeBackportBinding.NEBULA_REF,
                        NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        208,
                        "PROVEN",
                        NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
    }

    @Test
    void recordInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaJavaGradeBackportBinding.Asset(" ", "x", "0".repeat(40)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaJavaGradeBackportBinding.Asset("x", "p", "bad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaJavaGradeBackportBinding.Step(
                        -1,
                        NebulaJavaGradeBackportBinding.Phase.NEBULA_PROOF,
                        "hsoliwal/nebula",
                        "FILE",
                        ""));
    }
}
