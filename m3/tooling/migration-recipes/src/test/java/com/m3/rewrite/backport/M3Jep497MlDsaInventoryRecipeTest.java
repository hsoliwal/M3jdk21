// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3Jep497MlDsaInventoryRecipeTest {
    private static final List<String> TARGETS = List.of(
            ".github/workflows/m3-jep497-mldsa-inventory.yml",
            "m3/backports/recipes/jep-497-mldsa/DEPENDENCY_CLOSURE.tsv",
            "m3/backports/recipes/jep-497-mldsa/GA_PATH_STATE.tsv",
            "m3/backports/recipes/jep-497-mldsa/MATERIALIZE_PATHS.txt",
            "m3/backports/recipes/jep-497-mldsa/README.md",
            "m3/backports/recipes/jep-497-mldsa/UPSTREAM_AUTHORITY.tsv");

    @Test void usesExistingHashPinnedTextOwnerWithoutProductAuthority() {
        var recipe = new M3Jep497MlDsaInventoryRecipe();
        assertEquals("8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7", M3Jep497MlDsaInventoryRecipe.IMPLEMENTATION_COMMIT);
        assertEquals("6705a9255d28f351950e7fbca9d05e73942a4e27", M3Jep497MlDsaInventoryRecipe.JDK24_GA_COMMIT);
        assertEquals("fb95a5394413dba7352a7ad2ebd39a3da42308a6", M3Jep497MlDsaInventoryRecipe.FIPS204_FINAL_COMMIT);
        var owner = assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, recipe.getRecipeList().getFirst());
        assertEquals("jep497-mldsa-inventory-20261007", owner.getCrateName());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test void generatesExactEvidenceThenStops() {
        var recipe = new M3Jep497MlDsaInventoryRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1).getChangeset().getAllResults();
        assertEquals(TARGETS.size(), first.size());
        List<SourceFile> after = new ArrayList<>();
        for (Result result : first) if (result.getAfter() != null) after.add(result.getAfter());
        assertEquals(TARGETS, after.stream().map(M3Jep497MlDsaInventoryRecipeTest::path).sorted().toList());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1).getChangeset().getAllResults().isEmpty());
    }

    @Test void releasedStateExcludesThreeStaleAcvpFilesAndPinsSevenSurvivors() {
        var recipe = new M3Jep497MlDsaInventoryRecipe();
        List<SourceFile> files = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults().stream().map(Result::getAfter).toList();
        String state = find(files, "m3/backports/recipes/jep-497-mldsa/GA_PATH_STATE.tsv");
        assertEquals(10, state.lines().skip(1).count());
        assertEquals(3, state.lines().filter(line -> line.contains("ABSENT_AT_GA")).count());
        String materialize = find(files, "m3/backports/recipes/jep-497-mldsa/MATERIALIZE_PATHS.txt");
        assertEquals(7, materialize.lines().filter(line -> !line.isBlank()).count());
        assertFalse(materialize.contains("internalProjection.json"));
        String deps = find(files, "m3/backports/recipes/jep-497-mldsa/DEPENDENCY_CLOSURE.tsv");
        assertTrue(deps.contains("JEP-496"));
        assertTrue(deps.contains("JDK-8345057"));
        assertTrue(deps.contains("JDK-8345533"));
    }

    private static String find(List<SourceFile> files, String wanted) {
        return files.stream().filter(file -> path(file).equals(wanted)).findFirst().orElseThrow().printAll();
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\','/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}
