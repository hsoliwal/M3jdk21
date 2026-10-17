// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Actual nested Java/Text SDK replay; never writes the receiving runtime. */
final class M3GenericPurposeRecipeOrderRepairTest {
    private static final String JAVA = "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeCompositionRecipeTest.java";
    private static final String JAVA2 = "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java";
    private static final String TEXT = "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/manifest.tsv";
    private static final String JROOT = "/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/";
    private static final String TROOT = "/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/";
    private static final String NAME = "com.synexia.rewrite.M3GenericPurposeRecipeOrderRepair20261017";

    @Test void nestedRecipeGeneratesExactCanonicalRepairsAndReachesFixedPoint() throws Exception {
        var recipe = recipe();
        assertEquals(1, recipe.getRecipeList().size());
        var named = recipe.getRecipeList().getFirst();
        assertEquals(NAME, named.getName());
        assertEquals(2, named.getRecipeList().size());
        assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class, named.getRecipeList().get(0));
        assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class, named.getRecipeList().get(1));
        var changed = changes(before(), recipe);
        assertEquals(after(), changed);
        assertTrue(changes(changed, recipe).isEmpty());
        Path root = Path.of(System.getProperty("m3.fixture.root"));
        assertEquals(after().get(JAVA), Files.readString(root.resolve(
                "src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeCompositionRecipeTest.java")));
        assertEquals(after().get(JAVA2), Files.readString(root.resolve(
                "src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java")));
        assertEquals(after().get(TEXT), resource(
                "/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/manifest.tsv"));
        Path output = root.resolve("target/repair-emitted").toAbsolutePath().normalize();
        for (var entry : changed.entrySet()) {
            Path path = output.resolve(entry.getKey()).normalize();
            assertTrue(path.startsWith(output));
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    @Test void allEightPartialStatesConverge() {
        for (int state = 0; state < 8; state++) {
            Map<String, String> input = new TreeMap<>(before());
            if ((state & 1) != 0) input.put(JAVA, after().get(JAVA));
            if ((state & 2) != 0) input.put(JAVA2, after().get(JAVA2));
            if ((state & 4) != 0) input.put(TEXT, after().get(TEXT));
            var changed = changes(input, recipe());
            assertEquals(3 - Integer.bitCount(state), changed.size());
            input.putAll(changed);
            assertEquals(after(), input);
            assertTrue(changes(input, recipe()).isEmpty());
        }
    }

    @Test void driftMissingAndDuplicateTargetsRefuse() {
        for (String target : List.of(JAVA, JAVA2, TEXT)) {
            Map<String, String> missing = new TreeMap<>(before());
            missing.remove(target);
            assertThrows(IllegalStateException.class, () -> changes(missing, recipe()));
            Map<String, String> drift = new TreeMap<>(before());
            drift.put(target, drift.get(target) + "\n// drift\n");
            assertThrows(IllegalStateException.class, () -> changes(drift, recipe()));
            var context = context();
            var sources = new ArrayList<>(parse(before(), context));
            sources.add(sources.stream().filter(s ->
                    s.getSourcePath().toString().replace('\\', '/').equals(target))
                    .findFirst().orElseThrow());
            assertThrows(IllegalStateException.class, () ->
                    recipe().run(new InMemoryLargeSourceSet(sources), context, 2));
        }
    }

    private static Recipe recipe() {
        return Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(NAME);
    }
    private static Map<String, String> before() {
        return Map.of(JAVA, resource(JROOT + "before.java.txt"),
                JAVA2, resource(JROOT + "before-text-sdk.java.txt"),
                TEXT, resource(TROOT + "before.tsv.txt"));
    }
    private static Map<String, String> after() {
        return Map.of(JAVA, resource(JROOT + "M3Jdk21GenericPurposeCompositionRecipeTest.java.txt"),
                JAVA2, resource(JROOT + "M3Jdk21GenericPurposeSealSuccessorRecipeTest.java.txt"),
                TEXT, resource(TROOT + "manifest.tsv.txt"));
    }
    private static Map<String, String> changes(Map<String, String> input, Recipe recipe) {
        var context = context();
        Map<String, String> output = new TreeMap<>();
        recipe.run(new InMemoryLargeSourceSet(parse(input, context)), context, 2)
                .getChangeset().getAllResults().forEach(result -> {
                    assertNotNull(result.getAfter());
                    output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                            result.getAfter().printAll());
                });
        return output;
    }
    private static List<SourceFile> parse(
            Map<String, String> input, InMemoryExecutionContext context) {
        List<SourceFile> result = new ArrayList<>();
        List<Parser.Input> javaInputs = new ArrayList<>();
        for (String name : List.of(JAVA, JAVA2)) {
            if (input.containsKey(name)) javaInputs.add(
                    Parser.Input.fromString(Path.of(name), input.get(name)));
        }
        if (!javaInputs.isEmpty()) result.addAll(JavaParser.fromJavaVersion().build()
                .parseInputs(javaInputs, null, context).toList());
        if (input.containsKey(TEXT)) result.add(PlainText.builder()
                .sourcePath(Path.of(TEXT)).text(input.get(TEXT)).build());
        return result;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
    }
    private static String resource(String name) {
        try (var stream = M3GenericPurposeRecipeOrderRepairTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
