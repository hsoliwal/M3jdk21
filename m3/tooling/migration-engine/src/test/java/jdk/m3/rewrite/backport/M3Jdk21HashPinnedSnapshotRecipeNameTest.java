// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class M3Jdk21HashPinnedSnapshotRecipeNameTest {
    @Test
    void targetOwnedM3CratesCanOwnM3PortJavaWithoutWeakeningM3Fence() {
        assertDoesNotThrow(
                () -> new M3Jdk21HashPinnedSnapshotRecipe("m3-full-precompute-second-pass"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3Jdk21HashPinnedSnapshotRecipe("m4-full-precompute-second-pass"));

        String target = "m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java";
        assertTrue(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(target));
        assertFalse(M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(target));
    }
}
