// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

/** Exact-source, fixed-point and drift-refusal proof for the shared M3 catalog warning. */
final class M3SharedLexiconBuilderExplicitCtorRecipeTest {
    private static final String CRATE = "m3-shared-lexicon-builder-explicit-ctor-20261018";
    private static final String RESOURCE = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String FIXTURE =
            "/com/synexia/rewrite/m3-shared-lexicon-builder-explicit-ctor-20261018/SharedLexiconPrecomputeCatalog.java.before.txt";
    private static final String TARGET = "m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java";
    private static final String BEFORE_SHA256 = "8e6156376ea9548971b78bb876dc3968195266f04c17c5b061d7f5e4b52f367d";
    private static final String AFTER_SHA256 = "421beafcb9d81cd35363e21e06737f72f8c43fa566ab70bb7501ccf1a042e352";
    private static final String DECLARATION = "    public static final class Builder {\n";
    private static final String CONSTRUCTOR = "        public Builder() { }\n";

    @Test
    void appliesExactTargetPostimageAndReplaysToFixedPoint() throws Exception {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        String beforeText = sourceFixture();
        List<SourceFile> before = parse(context, beforeText);
        assertEquals(1, before.size());
        assertEquals(BEFORE_SHA256, sha256(before.get(0).printAll()));

        var first = recipe.run(new InMemoryLargeSourceSet(before), context, 1);
        var results = first.getChangeset().getAllResults();
        assertEquals(1, results.size());
        var result = results.iterator().next();
        assertNotNull(result.getBefore());
        assertNotNull(result.getAfter());
        assertEquals(beforeText, result.getBefore().printAll());
        assertEquals(template(), result.getAfter().printAll());
        assertEquals(AFTER_SHA256, sha256(result.getAfter().printAll()));
        assertEquals(beforeText.replace(DECLARATION, DECLARATION + CONSTRUCTOR),
                result.getAfter().printAll());

        var replay = recipe.run(new InMemoryLargeSourceSet(
                parse(context, result.getAfter().printAll())), context, 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesDriftBeforeCandidatePublication() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(context);
        String drifted = sourceFixture().replace(
                "dictlang.dictionary", "dictlang.dictionary.drift");
        var scanner = recipe.getScanner(inventory);
        for (SourceFile file : parse(context, drifted)) {
            scanner.visit(file, context);
        }
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> recipe.generate(inventory, context));
        assertTrue(failure.getMessage().contains("source drift"));
    }

    @Test
    void pinsExactPreimageTemplateAndManifest() throws Exception {
        assertEquals(1, sourceFixture().split(java.util.regex.Pattern.quote(DECLARATION), -1).length - 1);
        assertEquals(AFTER_SHA256, sha256(template()));
        String[] row = manifest().lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\\t", -1))
                .findFirst().orElseThrow();
        assertEquals(4, row.length);
        assertEquals(TARGET, row[0]);
        assertEquals(BEFORE_SHA256, row[1]);
        assertEquals(AFTER_SHA256, row[2]);
        assertEquals("SharedLexiconPrecomputeCatalog.java.txt", row[3]);
    }

    @Test
    void namedRecipeReusesTheExistingSnapshotEngine() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3SharedLexiconBuilderExplicitCtor20261018");
        assertTrue(containsSnapshot(recipe));
    }

    private static boolean containsSnapshot(Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot) {
            return CRATE.equals(snapshot.getCrateName());
        }
        return recipe.getRecipeList().stream().anyMatch(
                M3SharedLexiconBuilderExplicitCtorRecipeTest::containsSnapshot);
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context, String text) {
        List<Parser.Input> inputs = List.of(Parser.Input.fromString(Path.of(TARGET), text));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static String sourceFixture() {
        return resource(FIXTURE);
    }

    private static String template() {
        return resource(RESOURCE + "SharedLexiconPrecomputeCatalog.java.txt");
    }

    private static String manifest() {
        return resource(RESOURCE + "manifest.tsv");
    }

    private static String resource(String name) {
        try (var stream = M3SharedLexiconBuilderExplicitCtorRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
