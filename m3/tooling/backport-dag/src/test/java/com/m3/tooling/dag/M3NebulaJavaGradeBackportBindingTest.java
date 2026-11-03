// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3NebulaJavaGradeBackportBindingTest {
    @Test
    void exactNebulaCandidateIsAdmittedAsEvidenceButNeverExecutableAuthority() {
        M3NebulaJavaGradeBackportBinding.requireExactCandidate(
                "hsoliwal/nebula",
                "m3/java-grade-transfer-qualification-v3-20261015",
                "7a5a3cb5ffd82bca671b20e1496db80369657578",
                208,
                "HOLD_PENDING_NEBULA_PROOF",
                "INVENTORY_AND_ADAPTATION_ONLY_UNTIL_JAVA_GRADE_PROVEN");

        assertTrue(M3NebulaJavaGradeBackportBinding.evidenceAdmitted());
        assertFalse(M3NebulaJavaGradeBackportBinding.executableBackportDependency());
        assertFalse(M3NebulaJavaGradeBackportBinding.sourceMutationAuthority());
        assertFalse(M3NebulaJavaGradeBackportBinding.semanticEquivalenceAuthority());
        assertFalse(M3NebulaJavaGradeBackportBinding.compatibilityAuthority());
        assertFalse(M3NebulaJavaGradeBackportBinding.promotionAuthority());
        assertTrue(M3NebulaJavaGradeBackportBinding.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void exactFourNebulaAssetsAreGitBlobPinned() {
        assertEquals(
                List.of(
                        "java-grade",
                        "backport-plan",
                        "transfer-contract",
                        "java-grade-doc"),
                M3NebulaJavaGradeBackportBinding.assets().stream()
                        .map(M3NebulaJavaGradeBackportBinding.Asset::id)
                        .toList());
        assertEquals(
                List.of(
                        "80f3a2a0ca443972f367b5636d3a897a27eff88b",
                        "eddba9c6546749169b1a038147e06c4d8c863b4d",
                        "368b947eacef8355607706c90991af5b55b08bc2",
                        "d74b4180bf6b36a16d9c2f1362c19fcc5d38daf0"),
                M3NebulaJavaGradeBackportBinding.assets().stream()
                        .map(M3NebulaJavaGradeBackportBinding.Asset::gitBlobSha1)
                        .toList());
        assertTrue(
                M3NebulaJavaGradeBackportBinding.assets().stream()
                        .allMatch(asset -> !asset.path().isBlank()));
    }

    @Test
    void phaseOrderMatchesNebulaBackportProgrammeExactly() {
        assertEquals(
                List.of(
                        M3NebulaJavaGradeBackportBinding.Phase.NEBULA_PROOF,
                        M3NebulaJavaGradeBackportBinding.Phase.SYNEXIA_RECIPE_ABSORPTION,
                        M3NebulaJavaGradeBackportBinding.Phase.SYNEXIA_RECIPE_PROOF,
                        M3NebulaJavaGradeBackportBinding.Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                        M3NebulaJavaGradeBackportBinding.Phase.M3JDK21_BACKPORT_INVENTORY,
                        M3NebulaJavaGradeBackportBinding.Phase.M3JDK21_FILE_REPLAY,
                        M3NebulaJavaGradeBackportBinding.Phase.M3JDK21_SCOPE_PROMOTION,
                        M3NebulaJavaGradeBackportBinding.Phase.TARGET_PROOF),
                M3NebulaJavaGradeBackportBinding.steps().stream()
                        .map(M3NebulaJavaGradeBackportBinding.Step::phase)
                        .toList());

        for (int index = 0; index < M3NebulaJavaGradeBackportBinding.steps().size(); index++) {
            var step = M3NebulaJavaGradeBackportBinding.steps().get(index);
            assertEquals(index, step.ordinal());
            assertEquals(index < 6 ? "FILE" : "LIBRARY_API", step.maximumScope());
            if (index == 0) {
                assertEquals("", step.dependsOn());
                assertEquals("hsoliwal/nebula", step.repository());
            } else {
                assertEquals(
                        M3NebulaJavaGradeBackportBinding.steps()
                                .get(index - 1)
                                .phase()
                                .name(),
                        step.dependsOn());
            }
        }
        assertEquals("hsoliwal/com.synexia", M3NebulaJavaGradeBackportBinding.steps().get(1).repository());
        assertEquals("hsoliwal/com.synexia", M3NebulaJavaGradeBackportBinding.steps().get(2).repository());
        assertTrue(
                M3NebulaJavaGradeBackportBinding.steps().subList(3, 8).stream()
                        .allMatch(step -> "hsoliwal/M3jdk21".equals(step.repository())));
    }

    @Test
    void candidateIdentityOrPolicyDriftFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaJavaGradeBackportBinding.requireExactCandidate(
                        "hsoliwal/nebula",
                        "master",
                        M3NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        208,
                        M3NebulaJavaGradeBackportBinding.STATE,
                        M3NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaJavaGradeBackportBinding.requireExactCandidate(
                        M3NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        M3NebulaJavaGradeBackportBinding.NEBULA_REF,
                        "0".repeat(40),
                        208,
                        M3NebulaJavaGradeBackportBinding.STATE,
                        M3NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaJavaGradeBackportBinding.requireExactCandidate(
                        M3NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        M3NebulaJavaGradeBackportBinding.NEBULA_REF,
                        M3NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        209,
                        M3NebulaJavaGradeBackportBinding.STATE,
                        M3NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3NebulaJavaGradeBackportBinding.requireExactCandidate(
                        M3NebulaJavaGradeBackportBinding.NEBULA_REPOSITORY,
                        M3NebulaJavaGradeBackportBinding.NEBULA_REF,
                        M3NebulaJavaGradeBackportBinding.NEBULA_COMMIT,
                        208,
                        "PROVEN",
                        M3NebulaJavaGradeBackportBinding.APPLICATION_POLICY));
    }

    @Test
    void recordInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3NebulaJavaGradeBackportBinding.Asset(" ", "x", "0".repeat(40)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3NebulaJavaGradeBackportBinding.Asset("x", "p", "bad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3NebulaJavaGradeBackportBinding.Step(
                        -1,
                        M3NebulaJavaGradeBackportBinding.Phase.NEBULA_PROOF,
                        "hsoliwal/nebula",
                        "FILE",
                        ""));
    }
}
