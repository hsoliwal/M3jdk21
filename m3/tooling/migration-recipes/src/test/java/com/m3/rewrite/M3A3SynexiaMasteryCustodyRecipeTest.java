// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Exact FILE-atom proof that A3 consumes the Synexia-owned mastery recipes. */
final class M3A3SynexiaMasteryCustodyRecipeTest {
    private static final String JAVA_CRATE = "a3-synexia-mastery-custody";
    private static final String TEXT_CRATE = "a3-synexia-mastery-custody-text";
    private static final String A3_PATH = "src/main/java/com/m3/a3/A3Lab.java";
    private static final String CATALOGUE_PATH = "m3/tooling/recipe-catalogue.tsv";

    @Test
    void exactA3LabPreimageMovesToSynexiaOwnersAndSecondRunIsFixedPoint() {
        String before =
                resource(
                        "/com/m3/rewrite/a3-synexia-mastery-custody/A3Lab.java.before");
        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE);

        Map<String, String> changed = applyJava(recipe, Map.of(A3_PATH, before));
        assertEquals(1, changed.size());

        String after = changed.get(A3_PATH);
        assertTrue(
                after.contains(
                        "import com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe;"));
        assertTrue(
                after.contains(
                        "import com.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe;"));
        assertFalse(after.contains("import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;"));
        assertFalse(after.contains("import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;"));

        assertTrue(applyJava(recipe, changed).isEmpty());
    }

    @Test
    void catalogueAndOwnershipNoteConvergeThroughHashPinnedTextRecipe() {
        String beforeCatalogue =
                resource(
                        "/com/m3/rewrite/a3-synexia-mastery-custody/recipe-catalogue.before.tsv");
        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE);

        Map<String, String> first =
                applyText(recipe, Map.of(CATALOGUE_PATH, beforeCatalogue));
        assertEquals(2, first.size());

        String catalogue = first.get(CATALOGUE_PATH);
        assertTrue(
                catalogue.contains(
                        "m3-file-local-pure-int-atom\tcom.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe"));
        assertTrue(
                catalogue.contains(
                        "m3-file-local-pure-int-pattern\tcom.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe"));
        assertTrue(
                catalogue.contains(
                        "m3-file-local-pure-int-convergence\tcom.synexia.rewrite.atom.M3PureIntConvergenceRecipe"));
        assertFalse(catalogue.contains("\tcom.m3.rewrite.atom.M3AtomizePureIntReturnRecipe"));
        assertFalse(catalogue.contains("\tcom.m3.rewrite.atom.M3PatternizePureIntAtomRecipe"));

        String note = first.get("m3/docs/A3_SYNX_MASTERY_CUSTODY.md");
        assertTrue(note.contains("Reusable Atomize/Patternize/Document/Convergence recipes"));
        assertTrue(note.contains("OpenJDK `configure -> make -> jtreg -> runtime` remains"));

        assertTrue(applyText(recipe, first).isEmpty());
    }

    @Test
    void declarativeCompositionBindsBothExistingGenericRecipeOwners() {
        String yaml =
                resource("/META-INF/rewrite/m3-a3-synexia-mastery-custody.yml");
        assertTrue(yaml.contains("name: com.m3.a3.SynexiaMasteryCustodyDelivery"));
        assertTrue(yaml.contains("M3HashPinnedJavaSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-synexia-mastery-custody"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-synexia-mastery-custody-text"));
    }

    private static Map<String, String> applyJava(
            Recipe recipe, Map<String, String> sources) {
        var context = context();
        List<Parser.Input> inputs =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        Parser.Input.fromString(
                                                Path.of(entry.getKey()), entry.getValue()))
                        .toList();
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context)
                        .toList();
        return changes(recipe, parsed, context);
    }

    private static Map<String, String> applyText(
            Recipe recipe, Map<String, String> sources) {
        var context = context();
        List<SourceFile> parsed =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        PlainText.builder()
                                                .sourcePath(Path.of(entry.getKey()))
                                                .text(entry.getValue())
                                                .build())
                        .map(SourceFile.class::cast)
                        .toList();
        return changes(recipe, parsed, context);
    }

    private static Map<String, String> changes(
            Recipe recipe,
            List<SourceFile> parsed,
            InMemoryExecutionContext context) {
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(parsed),
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

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String resource(String path) {
        try (var input =
                M3A3SynexiaMasteryCustodyRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 Synexia custody resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 Synexia custody resource", failure);
        }
    }
}
