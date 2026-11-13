// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class M3A3M3ReceiverAuthorityTest {

    @Test
    void synexiaReceiverAdmitsOnlyToolPlaneA3AndReceiverOwners() {
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/a3/src/main/java/jdk/m3/a3/A3Apply.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/a3/src/test/java/jdk/m3/a3/A3ApplyTest.java"));
        assertTrue(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/backport/"
                                + "M3HandoffGuardRecipe.java"));

        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "src/java.base/share/classes/java/lang/String.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "test/jdk/java/lang/String/Basic.java"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "m3/tooling/a3/README.md"));
        assertFalse(
                M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                        "../m3/tooling/a3/src/main/java/jdk/m3/a3/A3Apply.java"));
    }
}
