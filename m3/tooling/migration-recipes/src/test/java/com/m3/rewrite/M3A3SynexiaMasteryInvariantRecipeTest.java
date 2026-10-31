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

/** Exact text-recipe proof for the executable A3 Synexia mastery invariant. */
final class M3A3SynexiaMasteryInvariantRecipeTest {
    private static final String CRATE = "a3-synexia-mastery-invariant";
    private static final String ROOT =
            "/com/m3/rewrite/a3-synexia-mastery-invariant/";

    @Test
    void exactCurrentPolicyPreimagesAdvanceAndSecondRunIsFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "m3/compatibility/synexia-recipe-home-policy.tsv",
                        resource("policy.before.tsv"),
                        "m3/compatibility/test_synexia_recipe_home_policy.py",
                        resource("policy-test.before.py"));

        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        Map<String, String> first = apply(recipe, before);
        assertEquals(2, first.size());

        String policy = first.get("m3/compatibility/synexia-recipe-home-policy.tsv");
        assertTrue(
                policy.contains(
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3Lab.java\tTARGET_PROOF_CONSUMER\tcom.synexia.rewrite.atom"));

        String policyTest =
                first.get("m3/compatibility/test_synexia_recipe_home_policy.py");
        assertTrue(policyTest.contains("import xml.etree.ElementTree as ET"));
        assertTrue(policyTest.contains("PIN = ROOT / \"compatibility\" / \"synexia-recipe-home-pin.tsv\""));
        assertTrue(
                policyTest.contains(
                        "test_a3_mastery_and_catalogue_resolve_to_synexia_recipe_owners"));
        assertTrue(
                policyTest.contains(
                        "import com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe;"));

        assertTrue(apply(recipe, first).isEmpty());
    }

    @Test
    void declarativeRecipeBindsTheExistingHashPinnedTextOwner() {
        String yaml = resource("/META-INF/rewrite/m3-a3-synexia-mastery-invariant.yml");
        assertTrue(yaml.contains("name: com.m3.a3.SynexiaMasteryInvariantDelivery"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-synexia-mastery-invariant"));
    }

    private static Map<String, String> apply(
            Recipe recipe,
            Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> input =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        PlainText.builder()
                                                .sourcePath(Path.of(entry.getKey()))
                                                .text(entry.getValue())
                                                .build())
                        .map(SourceFile.class::cast)
                        .toList();
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(input),
                        context,
                        8);
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
        return Map.copyOf(output);
    }

    private static String resource(String name) {
        String path = name.startsWith("/") ? name : ROOT + name;
        try (var input =
                M3A3SynexiaMasteryInvariantRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 Synexia mastery invariant resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 Synexia mastery invariant resource", failure);
        }
    }
}
