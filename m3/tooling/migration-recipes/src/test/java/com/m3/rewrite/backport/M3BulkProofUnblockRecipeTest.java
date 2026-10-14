// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkProofUnblockRecipeTest {

    @Test
    void receiverOwnsExactlyThreeReviewedCurrentTreeTargets() throws Exception {
        var recipe = new M3BulkProofUnblockRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        var targets = new M3Jdk21HashPinnedSnapshotRecipe(
                M3BulkProofUnblockRecipe.CRATE).targetPaths();
        assertEquals(
                List.of(
                        "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/semantic/M3SemanticHashRecipeTest.java",
                        "src/java.desktop/share/classes/javax/swing/JComponent.java",
                        "src/java.desktop/share/classes/javax/swing/RepaintManager.java"),
                targets);
        Path root = repoRoot();
        for (String target : targets) {
            assertTrue(Files.isRegularFile(root.resolve(target)), target);
        }
        String component = Files.readString(
                root.resolve("src/java.desktop/share/classes/javax/swing/JComponent.java"));
        String repaint = Files.readString(
                root.resolve("src/java.desktop/share/classes/javax/swing/RepaintManager.java"));
        assertTrue(component.chars().allMatch(c -> c <= 127));
        assertTrue(repaint.chars().allMatch(c -> c <= 127));
        String test = Files.readString(root.resolve(targets.getFirst()));
        assertTrue(test.contains("\\\"M3-IOP: PURE_INT_EXPRESSION\\\""));
        assertTrue(test.contains("\\\"ordinary\\\""));
    }

    private static Path repoRoot() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null) {
            if (Files.isRegularFile(p.resolve("src/java.base/share/classes/module-info.java"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("M3JDK root not found");
    }
}
