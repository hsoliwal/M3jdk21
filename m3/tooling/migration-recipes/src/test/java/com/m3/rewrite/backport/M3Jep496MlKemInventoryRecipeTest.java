// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3Jep496MlKemInventoryRecipeTest {
    private static final List<String> TARGETS =
            List.of(
                    ".github/workflows/m3-jep496-mlkem-inventory.yml",
                    "m3/backports/recipes/jep-496-mlkem/DEPENDENCY_CLOSURE.tsv",
                    "m3/backports/recipes/jep-496-mlkem/GA_PATH_STATE.tsv",
                    "m3/backports/recipes/jep-496-mlkem/MATERIALIZE_PATHS.txt",
                    "m3/backports/recipes/jep-496-mlkem/README.md",
                    "m3/backports/recipes/jep-496-mlkem/UPSTREAM_AUTHORITY.tsv");

    @Test
    void inventoryRecipeUsesExistingHashPinnedTextOwner() {
        var recipe = new M3Jep496MlKemInventoryRecipe();

        assertEquals(
                "13987b4244614d594dc8f94c288eddb6239a066f",
                M3Jep496MlKemInventoryRecipe.IMPLEMENTATION_COMMIT);
        assertEquals(
                "6705a9255d28f351950e7fbca9d05e73942a4e27",
                M3Jep496MlKemInventoryRecipe.JDK24_GA_COMMIT);
        assertEquals(
                "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7",
                M3Jep496MlKemInventoryRecipe.JEP497_COMMIT);
        assertEquals(1, recipe.getRecipeList().size());
        var text =
                assertInstanceOf(
                        M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().getFirst());
        assertEquals("jep496-mlkem-inventory-20261007", text.getCrateName());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void absentInventoryTargetsGenerateThenReachFixedPoint() {
        var recipe = new M3Jep496MlKemInventoryRecipe();

        var first =
                recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                        .getChangeset()
                        .getAllResults();
        assertEquals(TARGETS.size(), first.size());

        List<SourceFile> after = new ArrayList<>();
        for (Result result : first) {
            SourceFile generated = result.getAfter();
            if (generated != null) after.add(generated);
        }
        assertEquals(TARGETS, after.stream().map(M3Jep496MlKemInventoryRecipeTest::path).sorted().toList());
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void generatedEvidencePinsReleasedStateAndExcludesTwoStaleAcvpFiles() {
        var recipe = new M3Jep496MlKemInventoryRecipe();
        List<SourceFile> generated =
                recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .stream()
                        .map(Result::getAfter)
                        .toList();

        String state = find(generated, "m3/backports/recipes/jep-496-mlkem/GA_PATH_STATE.tsv");
        assertEquals(10, state.lines().skip(1).count());
        assertEquals(2, state.lines().filter(line -> line.contains("ABSENT_AT_GA")).count());

        String materialize =
                find(generated, "m3/backports/recipes/jep-496-mlkem/MATERIALIZE_PATHS.txt");
        assertEquals(8, materialize.lines().filter(line -> !line.isBlank()).count());
        assertFalse(materialize.contains("internalProjection.json"));

        String dependencies =
                find(generated, "m3/backports/recipes/jep-496-mlkem/DEPENDENCY_CLOSURE.tsv");
        assertTrue(dependencies.contains("JEP-497"));
        assertTrue(dependencies.contains("JDK-8345057"));
        assertTrue(dependencies.contains("JDK24-GA"));
    }

    private static String find(List<SourceFile> files, String wanted) {
        return files.stream()
                .filter(file -> path(file).equals(wanted))
                .findFirst()
                .orElseThrow()
                .printAll();
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
