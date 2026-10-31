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

/** Fixed-point proof for the A3 regex-memory workflow/documentation text crate. */
final class M3A3RegexMemoryWorkflowRecipeTest {
    private static final String CRATE = "a3-regex-memory-workflow";
    private static final String BEFORE_ROOT =
            "/com/m3/rewrite/a3-regex-memory-workflow/before/";

    @Test
    void exactCurrentTextPreimagesAdvanceRegexMemoryGateAndReplayAtFixedPoint() {
        Map<String, String> before =
                Map.of(
                        ".github/workflows/m3-a3.yml",
                        resource(BEFORE_ROOT + "workflow.yml"),
                        "m3/docs/a3-lab.md",
                        resource(BEFORE_ROOT + "a3-lab.md"),
                        "m3/docs/a3-mastery.md",
                        resource(BEFORE_ROOT + "a3-mastery.md"));

        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        Map<String, String> changed = apply(recipe, before);

        assertEquals(3, changed.size());
        assertTrue(
                changed.get(".github/workflows/m3-a3.yml")
                        .contains("$13 == \"true\""));
        assertTrue(
                changed.get("m3/docs/a3-lab.md")
                        .contains("16 x 17 = 272 deterministic cells"));
        assertTrue(
                changed.get("m3/docs/a3-mastery.md")
                        .contains("Thicker regex/string precompute"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(changed);
        assertTrue(apply(recipe, converged).isEmpty());
    }

    @Test
    void declarativeRecipeBindsExactTextCrate() {
        String yaml = resource("/META-INF/rewrite/m3-a3-regex-memory-workflow.yml");
        assertTrue(yaml.contains("name: com.m3.a3.RegexMemoryWorkflowDelivery"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-regex-memory-workflow"));
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
                                            .replace('\', '/'),
                                    after.printAll());
                        });
        return output;
    }

    private static String resource(String path) {
        try (var input =
                M3A3RegexMemoryWorkflowRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 regex-memory workflow resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 regex-memory workflow resource", failure);
        }
    }
}
