// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Exact recipe-first proof for the A3 Levenshtein tri-platform evidence closure. */
final class M3A3LevenshteinEvidenceClosureRecipeTest {
    private static final String CRATE = "a3-levenshtein-evidence-closure";
    private static final String BEFORE_ROOT =
            "/com/m3/rewrite/a3-levenshtein-evidence-closure/before/";

    @Test
    void exactCurrentTextPreimagesCloseEvidenceAndReplayAtFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "m3/backports/ALGORITHM_CATALOGUE.tsv",
                        resource(BEFORE_ROOT + "algorithm-catalogue.tsv"),
                        "m3/docs/a3-alg.md",
                        resource(BEFORE_ROOT + "a3-alg.md"));

        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        Map<String, String> changed = apply(recipe, before);

        assertEquals(2, changed.size());
        String catalogue = changed.get("m3/backports/ALGORITHM_CATALOGUE.tsv");
        assertTrue(
                catalogue.contains(
                        "ALG-LEV\tFUZZY_SEARCH\tLEVENSHTEIN_AUTOMATON\thold-contract"));
        assertTrue(
                catalogue.contains(
                        "https://www.geeksforgeeks.org/dsa/edit-distance-dp-5/"));
        assertTrue(catalogue.contains("\tREFERENCE_ONLY\t"));
        assertTrue(
                changed.get("m3/docs/a3-alg.md")
                        .contains("Fuzzy-search evidence closure"));
        assertTrue(
                changed.get("m3/docs/a3-alg.md")
                        .contains("real JDK fuzzy/string owner"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(changed);
        assertTrue(apply(recipe, converged).isEmpty());
    }

    @Test
    void declarativeRecipeBindsExactTextCrate() {
        String yaml =
                resource(
                        "/META-INF/rewrite/m3-a3-levenshtein-evidence-closure.yml");
        assertTrue(yaml.contains("name: com.m3.a3.LevenshteinEvidenceClosure"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-levenshtein-evidence-closure"));
    }

    private static Map<String, String> apply(
            Recipe recipe,
            Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> files =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        (SourceFile)
                                                PlainText.builder()
                                                        .sourcePath(Path.of(entry.getKey()))
                                                        .text(entry.getValue())
                                                        .build())
                        .toList();
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(files),
                        context,
                        4);
        Map<String, String> output = new LinkedHashMap<>();
        run.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile after =
                                    Objects.requireNonNull(
                                            result.getAfter(), "after");
                            output.put(
                                    after.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    after.printAll());
                        });
        return output;
    }

    private static String resource(String path) {
        try (var input =
                M3A3LevenshteinEvidenceClosureRecipeTest.class
                        .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 Levenshtein evidence resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 Levenshtein evidence resource", failure);
        }
    }
}
