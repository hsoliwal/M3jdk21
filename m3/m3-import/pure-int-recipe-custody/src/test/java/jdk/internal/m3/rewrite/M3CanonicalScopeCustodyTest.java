// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3CanonicalScopeCustodyTest {
    @Test void restoredExactHistoricalSourceBlobs() throws Exception {
        Path root = Path.of(System.getProperty("basedir", "."))
                .toAbsolutePath().normalize().resolve("src/main/java/jdk/internal/m3/rewrite");
        assertEquals("c79d2e9798596dcf6001a5545b034b75b37d0dba",
                gitBlob(Files.readAllBytes(root.resolve("M3RecipeScopePolicy.java"))));
        Path currentPath = root.resolve("M3RecipeScopeRegistry.java");
        byte[] now = Files.readAllBytes(currentPath);
        // Preserve immutable historical source custody and its additive successor.
        assertEquals("eb379da6c1c8af1f9d80703ab3f8f71ec556679e",
                gitBlob(now), "expanded current registry");
        try (java.io.InputStream input = M3CanonicalScopeCustodyTest.class
                .getResourceAsStream("/jdk/internal/m3/rewrite/M3RecipeScopeRegistry.historical.java.txt")) {
            assertTrue(input != null, "historical registry fixture");
            byte[] historical = input.readAllBytes();
            assertEquals("136869bb06a0f62c7c3f3dbd91fb7c488b5846b5",
                    gitBlob(historical), "exact original registry blob");
            String[] previousLines = new String(historical, StandardCharsets.UTF_8)
                    .split("\\n", -1);
            String[] currentLines = new String(now, StandardCharsets.UTF_8)
                    .split("\\n", -1);
            assertEquals(previousLines.length + 21, currentLines.length,
                    "exactly the admitted additive registry lines");
            int cursor = 0;
            for (String expectedLine : previousLines) {
                while (cursor < currentLines.length
                        && !expectedLine.equals(currentLines[cursor])) {
                    cursor++;
                }
                assertTrue(cursor < currentLines.length,
                        "every historical line stays in its original order");
                cursor++;
            }
        }
    }

    @Test void scopePoliciesRemainCanonical() {
        var file = List.of("src/java.base/share/classes/java/lang/String.java");
        assertTrue(M3RecipeScopePolicy.inferred().fileLocalMechanical(file));
        assertEquals(M3EditScope.MODULE,
                M3RecipeScopePolicy.inferred().resolve(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/share/classes/java/util/Map.java")));
        assertFalse(M3RecipeScopePolicy.explicitContractChange().fileLocalMechanical(file));
        assertTrue(M3RecipeScopeRegistry.registered("org.openrewrite.java.RemoveUnusedImports"));
        assertThrows(IllegalArgumentException.class,
                () -> M3RecipeScopeRegistry.require("unregistered.m3.recipe"));
    }

    @Test void addedScopeRecipeNamesStayExplicitlyFileLocal() {
        List<String> admitted = List.of(
                "jdk.internal.m3.rewrite.M3PureIntAtomizeRecipe",
                "jdk.internal.m3.rewrite.M3PureIntInventoryRecipe",
                "jdk.internal.m3.rewrite.M3PureIntPatternizeRecipe",
                "jdk.internal.m3.rewrite.M3PureIntDocumentationRecipe",
                "jdk.internal.m3.rewrite.M3PureIntConvergenceRecipe",
                "jdk.internal.m3.rewrite.semantic.M3SemanticHashRecipe",
                "jdk.internal.m3.rewrite.M3Jdk21FileConvergenceRecipe");
        List<String> oneFile = List.of("src/java.base/share/classes/java/lang/String.java");
        for (String name : admitted) {
            assertTrue(M3RecipeScopeRegistry.registered(name), name);
            assertTrue(M3RecipeScopeRegistry.require(name).fileLocalMechanical(oneFile), name);
        }
    }

    private static String gitBlob(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }
}
