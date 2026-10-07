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

/** Fixed-point proof for the A3 mastery documentation/workflow text crate. */
final class M3A3MasteryWorkflowRecipeTest {
    private static final String CRATE = "a3-mastery-workflow";
    private static final String BEFORE_ROOT =
            "/com/m3/rewrite/a3-mastery-workflow/before/";

    @Test
    void exactCurrentTextPreimagesAdvanceMasteryAndReplayAtFixedPoint() {
        Map<String, String> before =
                Map.of(
                        ".github/workflows/m3-a3.yml",
                        resource(BEFORE_ROOT + "m3-a3.yml"),
                        "m3/docs/a3-lab.md",
                        resource(BEFORE_ROOT + "a3-lab.md"));

        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        Map<String, String> changed = apply(recipe, before);

        assertEquals(3, changed.size());
        assertTrue(
                changed.get(".github/workflows/m3-a3.yml")
                        .contains("test \"$ROWS\" -eq 288"));
        assertTrue(
                changed.get(".github/workflows/m3-a3.yml")
                        .contains("M3A3MasteryWorkflowRecipeTest"));
        assertTrue(
                changed.get("m3/docs/a3-lab.md")
                        .contains("48 x 6 = 288 result rows"));
        assertTrue(
                changed.get("m3/docs/a3-mastery.md")
                        .contains("compiler and tests are authoritative"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(changed);
        assertTrue(apply(recipe, converged).isEmpty());
    }

    @Test
    void declarativeRecipeBindsExactTextCrate() {
        String yaml = resource("/META-INF/rewrite/m3-a3-mastery-workflow.yml");
        assertTrue(yaml.contains("name: com.m3.a3.MasteryWorkflowDelivery"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-mastery-workflow"));
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
                M3A3MasteryWorkflowRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 mastery workflow resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 mastery workflow resource", failure);
        }
    }
}
