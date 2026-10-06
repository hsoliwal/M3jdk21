// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
