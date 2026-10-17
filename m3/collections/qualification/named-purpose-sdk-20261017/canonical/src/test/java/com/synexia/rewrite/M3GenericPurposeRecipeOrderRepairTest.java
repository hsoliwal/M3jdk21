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

    @Test void namedPurposeRecipeExecutesAllPartialStatesAndExactFixedPoint() {
        String name = "com.synexia.rewrite.M3Jdk21GenericPurposeMapComposition20261017";
        String root = "/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/";
        String owner = "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java";
        String self = "m3/collections/src/test/java/com/m3/collections/M3FilteredWrapperSelfTest.java";
        String junit = "m3/collections/src/test/java/com/m3/collections/M3FilteredPurposeCompositionTest.java";
        var activated = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(name);
        assertEquals(1, activated.getRecipeList().size());
        var named = activated.getRecipeList().getFirst();
        assertEquals(name, named.getName());
        assertEquals(1, named.getRecipeList().size());
        var provider = assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,
                named.getRecipeList().getFirst());
        assertEquals("m3jdk21-generic-filtered-purpose-20261017", provider.getCrateName());
        Map<String, String> reviewed = Map.of(
                owner, resource(root + "M3FilteredMaps.java.txt"),
                self, resource(root + "M3FilteredWrapperSelfTest.java.txt"),
                junit, resource(root + "M3FilteredPurposeCompositionTest.java.txt"));
        var paths = List.of(owner, junit, self);
        for (int state = 0; state < 8; state++) {
            Map<String, String> input = new TreeMap<>();
            input.put(owner, resource(root + "pre/M3FilteredMaps.java.txt"));
            input.put("opaque/Unrelated.java", "class Unrelated {}\n");
            for (int index = 0; index < paths.size(); index++) {
                if ((state & (1 << index)) != 0) {
                    input.put(paths.get(index), reviewed.get(paths.get(index)));
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var context = context();
                var sources = new ArrayList<>(JavaParser.fromJavaVersion().build()
                        .parseInputs(input.entrySet().stream().map(entry -> Parser.Input.fromString(
                                Path.of(entry.getKey()), entry.getValue())).toList(),
                                null, context).toList());
                if (reverse) java.util.Collections.reverse(sources);
                var changed = activated.run(new InMemoryLargeSourceSet(sources), context, 2)
                        .getChangeset().getAllResults();
                assertEquals(3 - Integer.bitCount(state), changed.size());
                Map<String, String> actual = new TreeMap<>(input);
                for (var result : changed) {
                    assertNotNull(result.getAfter());
                    actual.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                            result.getAfter().printAll());
                }
                Map<String, String> expected = new TreeMap<>(reviewed);
                expected.put("opaque/Unrelated.java", input.get("opaque/Unrelated.java"));
                assertEquals(expected, actual);
                var replay = JavaParser.fromJavaVersion().build()
                        .parseInputs(actual.entrySet().stream().map(entry -> Parser.Input.fromString(
                                Path.of(entry.getKey()), entry.getValue())).toList(),
                                null, context).toList();
                assertTrue(activated.run(new InMemoryLargeSourceSet(replay), context, 2)
                        .getChangeset().getAllResults().isEmpty());
            }
        }
    }

    @Test void namedSealRecipeExecutesAllPartialStatesAndExactFixedPoint() {
        String name = "com.synexia.rewrite.M3Jdk21GenericPurposeSealSuccessor20261017";
        String root = "/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/";
        String owner = "m3/collections/qualification/verify_generic_filtered_wrappers.py";
        String self = "m3/collections/qualification/test_verify_generic_filtered_successor.py";
        String receipt = "m3/collections/qualification/generic-filtered-purpose-postimages-20261017.tsv";
        var activated = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(name);
        assertEquals(1, activated.getRecipeList().size());
        var named = activated.getRecipeList().getFirst();
        assertEquals(name, named.getName());
        assertEquals(1, named.getRecipeList().size());
        var provider = assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class,
                named.getRecipeList().getFirst());
        assertEquals("m3jdk21-generic-purpose-seal-successor-20261017", provider.getCrateName());
        Map<String, String> reviewed = Map.of(
                owner, resource(root + "verify_generic_filtered_wrappers.py.txt"),
                self, resource(root + "test_verify_generic_filtered_successor.py.txt"),
                receipt, resource(root + "generic-filtered-purpose-postimages-20261017.tsv.txt"));
        var paths = List.of(receipt, self, owner);
        for (int state = 0; state < 8; state++) {
            Map<String, String> input = new TreeMap<>();
            input.put(owner, resource(root + "pre/verify_generic_filtered_wrappers.py.txt"));
            input.put("opaque/Unrelated.txt", "unrelated source remains exact\n");
            for (int index = 0; index < paths.size(); index++) {
                if ((state & (1 << index)) != 0) {
                    input.put(paths.get(index), reviewed.get(paths.get(index)));
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var context = context();
                List<SourceFile> sources = new ArrayList<>();
                input.forEach((path, content) -> sources.add(PlainText.builder()
                        .sourcePath(Path.of(path)).text(content).build()));
                if (reverse) java.util.Collections.reverse(sources);
                var changed = activated.run(new InMemoryLargeSourceSet(sources), context, 2)
                        .getChangeset().getAllResults();
                assertEquals(3 - Integer.bitCount(state), changed.size());
                Map<String, String> actual = new TreeMap<>(input);
                for (var result : changed) {
                    assertNotNull(result.getAfter());
                    actual.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                            result.getAfter().printAll());
                }
                Map<String, String> expected = new TreeMap<>(reviewed);
                expected.put("opaque/Unrelated.txt", input.get("opaque/Unrelated.txt"));
                assertEquals(expected, actual);
                List<SourceFile> replay = new ArrayList<>();
                actual.forEach((path, content) -> replay.add(PlainText.builder()
                        .sourcePath(Path.of(path)).text(content).build()));
                assertTrue(activated.run(new InMemoryLargeSourceSet(replay), context, 2)
                        .getChangeset().getAllResults().isEmpty());
            }
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
