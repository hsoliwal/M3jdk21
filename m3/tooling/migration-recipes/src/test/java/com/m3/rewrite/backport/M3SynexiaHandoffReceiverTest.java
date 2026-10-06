// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3SynexiaHandoffReceiverTest {
    @Test
    void javaSnapshotAdmitsSynexiaRecipeAndM3OwnedRoots() {
        assertDoesNotThrow(
                () -> new M3Jdk21HashPinnedSnapshotRecipe("synexia-recipe-export-v1"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        ".m3/openrewrite-recipes/src/main/java/com/m3/rewrite/SampleRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        ".m3/openrewrite-recipes/src/test/java/com/m3/rewrite/SampleRecipeTest.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/SampleRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        "m3/ports/sample/src/main/java/sample/Sample.java"));
        assertFalse(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath("m3/docs/Sample.java"));
    }

    @Test
    void provenanceGuardResolvesAndValidatesASealedApache2Packet() {
        var guard = new M3SynexiaHandoffGuardRecipe("synexia-guard-test-v1");
        assertDoesNotThrow(guard::getVisitor);
        assertTrue(M3SynexiaHandoffGuardRecipe.m3OwnedTargetPath("m3/ports/A.java"));
        assertTrue(
                M3SynexiaHandoffGuardRecipe.m3OwnedTargetPath(
                        ".m3/openrewrite-recipes/src/main/java/A.java"));
        assertFalse(M3SynexiaHandoffGuardRecipe.m3OwnedTargetPath("src/java.base/A.java"));
    }

    @Test
    void provenanceGuardAcceptsCurrentV2AndHierarchyQualifiedV3Packets() {
        assertDoesNotThrow(
                () -> new M3SynexiaHandoffGuardRecipe("synexia-guard-test-v2").getVisitor());
        assertDoesNotThrow(
                () -> new M3SynexiaHandoffGuardRecipe("synexia-guard-test-v3").getVisitor());
    }

    @Test
    void hierarchyQualificationRootIsIndependentlyVerified() {
        String qualification =
                "version\tSYNEXIA_HIERARCHICAL_ATOM_PATTERN_EXPORT_V1\n"
                        + "sourceRevision\t0123456789abcdef0123456789abcdef01234567\n"
                        + "topologyRoot\t" + "1".repeat(64) + "\n"
                        + "FILE\t" + "2".repeat(64) + "\n"
                        + "PACKAGE\t" + "3".repeat(64) + "\n"
                        + "MODULE\t" + "4".repeat(64) + "\n"
                        + "PROJECT\t" + "5".repeat(64) + "\n"
                        + "REPOSITORY\t" + "6".repeat(64) + "\n"
                        + "hierarchyRoot\t" + "7".repeat(64) + "\n"
                        + "atomPatternRoot\t" + "8".repeat(64) + "\n"
                        + "portfolioRoot\t" + "9".repeat(64) + "\n"
                        + "sources\t3\n"
                        + "documentationSources\t1\n"
                        + "complete\ttrue\n"
                        + "root\taf5c833d7d3d0a87dc57520ca9a2bf8791133b145ad116eef53f1dec596eb75f\n";

        assertEquals(
                "af5c833d7d3d0a87dc57520ca9a2bf8791133b145ad116eef53f1dec596eb75f",
                M3SynexiaHandoffGuardRecipe.hierarchyQualificationRoot(qualification));

        assertThrows(
                IllegalStateException.class,
                () ->
                        M3SynexiaHandoffGuardRecipe.hierarchyQualificationRoot(
                                qualification.replace(
                                        "FILE\t" + "2".repeat(64),
                                        "FILE\t" + "0".repeat(64))));
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3SynexiaHandoffGuardRecipe.hierarchyQualificationRoot(
                                qualification.replace("complete\ttrue", "complete\tfalse")));
    }

    @Test
    void provenanceGuardRejectsNonApacheBytesForM3OwnedTargets() {
        var guard = new M3SynexiaHandoffGuardRecipe("synexia-guard-non-apache-v1");
        assertThrows(IllegalStateException.class, guard::getVisitor);
    }

    @Test
    void receiverRejectsUnscopedCrateNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3Jdk21HashPinnedSnapshotRecipe("arbitrary-crate"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3SynexiaHandoffGuardRecipe("jdk27-not-synexia"));
    }
}
