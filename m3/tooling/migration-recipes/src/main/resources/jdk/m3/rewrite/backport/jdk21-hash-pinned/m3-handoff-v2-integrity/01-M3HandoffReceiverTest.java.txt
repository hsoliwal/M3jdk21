// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3HandoffReceiverTest {
    @Test
    void javaSnapshotAdmitsOnlyTargetReceiverProofRootsForM3Crates() {
        assertDoesNotThrow(
                () -> new M3Jdk21HashPinnedSnapshotRecipe("m3-recipe-export-v1"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        ".m3/openrewrite-recipes/src/main/java/jdk/m3/rewrite/SampleRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        ".m3/openrewrite-recipes/src/test/java/jdk/m3/rewrite/SampleRecipeTest.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/SampleRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                        "m3/ports/sample/src/main/java/sample/Sample.java"));
        assertFalse(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath("m3/docs/Sample.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/backport/"
                                + "M3HandoffGuardRecipe.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/a3/src/test/java/jdk/m3/a3/A3ApplyTest.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "src/java.base/share/classes/java/lang/String.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "test/jdk/java/lang/String/Test.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        ".m3/openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/"
                                + "M3HashPinnedJavaSnapshotRecipe.java"));
    }

    @Test
    void provenanceGuardResolvesAndValidatesASealedApache2Packet() {
        var guard = new M3HandoffGuardRecipe("m3-guard-test-v1");
        assertDoesNotThrow(guard::getVisitor);
        assertTrue(M3HandoffGuardRecipe.m3OwnedTargetPath("m3/ports/A.java"));
        assertTrue(
                M3HandoffGuardRecipe.m3OwnedTargetPath(
                        ".m3/openrewrite-recipes/src/main/java/A.java"));
        assertFalse(M3HandoffGuardRecipe.m3OwnedTargetPath("src/java.base/A.java"));
    }

    @Test
    void provenanceGuardRejectsNonApacheBytesForM3OwnedTargets() {
        var guard = new M3HandoffGuardRecipe("m3-guard-non-apache-v1");
        assertThrows(IllegalStateException.class, guard::getVisitor);
    }

    @Test
    void v2AcceptsOnlyExplicitTargetRecipeRolesBoundToCanonicalM3Recipe() {
        assertDoesNotThrow(
                () -> new M3HandoffGuardRecipe("m3-guard-v2-adapter-v1").getVisitor());

        assertThrows(
                IllegalStateException.class,
                () -> new M3HandoffGuardRecipe("m3-guard-v2-generic-v1").getVisitor());

        assertThrows(
                IllegalStateException.class,
                () -> new M3HandoffGuardRecipe("m3-guard-v2-copy-v1").getVisitor());
    }

    @Test
    void v2RejectsPayloadManifestAndAccountingDrift() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3HandoffGuardRecipe(
                                        "m3-guard-v2-payload-drift-v1")
                                .getVisitor());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3HandoffGuardRecipe(
                                        "m3-guard-v2-byte-count-v1")
                                .getVisitor());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3HandoffGuardRecipe(
                                        "m3-guard-v2-manifest-drift-v1")
                                .getVisitor());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3HandoffGuardRecipe(
                                        "m3-guard-v2-alias-drift-v1")
                                .getVisitor());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3HandoffGuardRecipe(
                                        "m3-guard-v2-properties-duplicate-v1")
                                .getVisitor());
    }

    @Test
    void receiverRejectsUnscopedCrateNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3Jdk21HashPinnedSnapshotRecipe("arbitrary-crate"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HandoffGuardRecipe("jdk27-not-m3"));
    }
}
