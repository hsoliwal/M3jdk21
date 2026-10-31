// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3ItemReferenceSafetyRecipeTest {
    private static final String CRATE = "m3-item-reference-safety-v2";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";

    @Test
    void namedNestedRecipeOwnsOnlyTheFourReviewedTargets() throws IOException {
        Recipe recipe = recipe();
        assertEquals(1, recipe.getRecipeList().size());
        var nested = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().getFirst());
        assertEquals(CRATE, nested.getCrateName());
        assertEquals(List.copyOf(images(false).keySet()), nested.targetPaths());
        assertEquals(4, nested.targetPaths().size());
        assertTrue(recipe.getDescription().contains("no lifetime or GC admission"));
    }

    @Test
    void exactPreimagesProduceAllPostimagesAndTwoZeroChangeReplays() throws IOException {
        Map<String, String> expected = images(false);
        assertEquals(expected, apply(parse(images(true))));
        assertTrue(apply(parse(expected)).isEmpty());
        assertTrue(apply(parse(expected)).isEmpty());
    }

    @Test
    void driftAtAnyTargetRefusesTheEntireCrate() throws IOException {
        Map<String, String> before = images(true);
        for (String path : before.keySet()) {
            Map<String, String> drift = new LinkedHashMap<>(before);
            drift.put(path, before.get(path) + "\n// admission drift\n");
            assertFalse(drift.equals(before));
            refusal(parse(drift), "source drift: " + path);
        }
    }

    @Test
    void missingAnyRequiredTargetRefusesTheEntireCrate() throws IOException {
        Map<String, String> before = images(true);
        for (String path : before.keySet()) {
            Map<String, String> missing = new LinkedHashMap<>(before);
            missing.remove(path);
            refusal(parse(missing), "required JDK21 source missing: " + path);
        }
    }

    @Test
    void duplicateTargetRefusesTheEntireCrate() throws IOException {
        Map<String, String> before = images(true);
        String path = before.keySet().iterator().next();
        List<SourceFile> duplicate = new ArrayList<>(parse(before));
        duplicate.addAll(parse(Map.of(path, before.get(path))));
        refusal(duplicate, "duplicate target: " + path);
    }

    @Test
    void exactTextAtTheTargetRequiresAJavaCompilationUnit() throws IOException {
        Map<String, String> before = images(true);
        String path = before.keySet().iterator().next();
        List<SourceFile> sources = new ArrayList<>(parse(before));
        sources.removeIf(file -> file.getSourcePath().equals(Path.of(path)));
        sources.add(PlainText.builder().sourcePath(Path.of(path)).text(before.get(path)).build());
        refusal(sources, "target is not a Java compilation unit: " + path);
    }

    private static Recipe recipe() {
        return Environment.builder().scanYamlResources().build().listRecipes().stream()
                .filter(value -> value.getName().equals(
                        "com.m3.rewrite.backport.M3ItemReferenceSafety"))
                .findFirst().orElseThrow();
    }

    private static Map<String, String> images(boolean before) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String row : resource(ROOT + "manifest.tsv").lines().toList()) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] cells = row.split("\t", -1);
            assertEquals(4, cells.length);
            String file = before ? cells[3].replace(".java.txt", ".before.java.txt") : cells[3];
            result.put(cells[0], resource(ROOT + file));
        }
        return result;
    }

    private static String resource(String path) throws IOException {
        try (var input = M3ItemReferenceSafetyRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing recipe fixture: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<SourceFile> parse(Map<String, String> sources) {
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach((path, text) -> inputs.add(Parser.Input.fromString(Path.of(path), text)));
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(inputs, null, context).toList();
        assertEquals(inputs.size(), parsed.size());
        parsed.forEach(file -> assertEquals(
                sources.get(file.getSourcePath().toString().replace('\\', '/')), file.printAll()));
        return parsed;
    }

    private static Map<String, String> apply(List<SourceFile> sources) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var run = recipe().run(new InMemoryLargeSourceSet(sources), context, 1);
        Map<String, String> changed = new LinkedHashMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            changed.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return changed;
    }

    private static void refusal(List<SourceFile> sources, String expected) {
        Throwable failure = assertThrows(Throwable.class, () -> apply(sources));
        while (failure.getCause() != null) failure = failure.getCause();
        assertInstanceOf(IllegalStateException.class, failure);
        assertTrue(failure.getMessage().contains(expected), failure.getMessage());
    }
}
