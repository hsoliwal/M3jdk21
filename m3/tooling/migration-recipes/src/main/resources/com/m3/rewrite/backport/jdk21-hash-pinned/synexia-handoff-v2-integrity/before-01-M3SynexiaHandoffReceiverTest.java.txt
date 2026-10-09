// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3SynexiaHandoffReceiverTest {
    @Test
    void javaSnapshotAdmitsOnlyTargetReceiverProofRootsForSynexiaCrates() {
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
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/"
                                + "M3SynexiaHandoffGuardRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3ApplyTest.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "src/java.base/share/classes/java/lang/String.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "test/jdk/java/lang/String/Test.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        ".m3/openrewrite-recipes/src/main/java/com/synexia/rewrite/"
                                + "M3HashPinnedJavaSnapshotRecipe.java"));
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
    void provenanceGuardRejectsNonApacheBytesForM3OwnedTargets() {
        var guard = new M3SynexiaHandoffGuardRecipe("synexia-guard-non-apache-v1");
        assertThrows(IllegalStateException.class, guard::getVisitor);
    }

    @Test
    void v2AcceptsOnlyExplicitTargetRecipeRolesBoundToCanonicalSynexiaRecipe() {
        assertDoesNotThrow(
                () -> new M3SynexiaHandoffGuardRecipe("synexia-guard-v2-adapter-v1").getVisitor());

        assertThrows(
                IllegalStateException.class,
                () -> new M3SynexiaHandoffGuardRecipe("synexia-guard-v2-generic-v1").getVisitor());

        assertThrows(
                IllegalStateException.class,
                () -> new M3SynexiaHandoffGuardRecipe("synexia-guard-v2-copy-v1").getVisitor());
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
