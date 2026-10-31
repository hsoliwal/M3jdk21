// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** ContractProbe for the existing scheduler/Java engine; not product-runtime proof. */
final class MapFactsRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-map-facts/";
    private static final String OWNER =
            "src/java.base/share/classes/jdk/internal/mindex/MIndexMappedStringBacking.java";
    private static final String API =
            "src/java.base/share/classes/jdk/internal/mindex/MIndexStringBacking.java";
    private static final String PRIOR =
            "test/jdk/java/lang/String/MIndexMappedStringBackingTest.java";
    private static final String TEST = "test/jdk/java/lang/String/MapFactsTest.java";

    @Test
    void exactCandidateUsesJavaTreesPreservesContextAndReachesFixedPoint() throws IOException {
        Map<String, SourceFile> result = run(recipe(), baseline(), 2);
        assertEquals(read("00-Backing.java.txt"), result.get(OWNER).printAll());
        assertEquals(read("01-MapFactsTest.java.txt"), result.get(TEST).printAll());
        assertEquals(read("context-Backing.java.txt"), result.get(API).printAll());
        assertEquals(read("context-Test.java.txt"), result.get(PRIOR).printAll());
        assertInstanceOf(J.CompilationUnit.class, result.get(OWNER));
        assertInstanceOf(J.CompilationUnit.class, result.get(TEST));
        run(recipe(), new ArrayList<>(result.values()), 0);
    }

    @Test
    void missingDriftedDuplicateAndOccupiedInputsAreRefused() throws IOException {
        List<SourceFile> original = baseline();
        List<SourceFile> missing = new ArrayList<>(original);
        missing.removeIf(file -> path(file).equals(OWNER));
        assertThrows(RuntimeException.class, () -> run(recipe(), missing, 0));

        List<SourceFile> duplicate = new ArrayList<>(original);
        duplicate.add(original.stream().filter(file -> path(file).equals(OWNER)).findFirst().orElseThrow());
        assertThrows(RuntimeException.class, () -> run(recipe(), duplicate, 0));

        List<SourceFile> occupied = new ArrayList<>(original);
        occupied.add(PlainText.builder().sourcePath(Path.of(TEST)).text("unreviewed").build());
        assertThrows(RuntimeException.class, () -> run(recipe(), occupied, 0));

        List<SourceFile> drifted = new ArrayList<>();
        for (SourceFile source : original) {
            if (path(source).equals(OWNER)) {
                source = parse(OWNER, read("before-00-Backing.java.txt") + "\n// drift\n");
            }
            drifted.add(source);
        }
        assertThrows(RuntimeException.class, () -> run(recipe(), drifted, 0));
    }

    @Test
    void completedOwnerResumesOnlyTheAbsentTestAndKeepsUnrelatedSource() throws IOException {
        List<SourceFile> inputs = new ArrayList<>(baseline());
        inputs.removeIf(file -> path(file).equals(OWNER));
        inputs.add(parse(OWNER, read("00-Backing.java.txt")));
        inputs.add(PlainText.builder().sourcePath(Path.of("notes.txt")).text("unchanged\n").build());
        Map<String, SourceFile> after = run(recipe(), inputs, 1);
        assertEquals("unchanged\n", after.get("notes.txt").printAll());
        run(recipe(), new ArrayList<>(after.values()), 0);
    }

    @Test
    void configuredNamedRecipeAndSerializationRetainExactCrate() throws IOException {
        var codec = new RecipeSerializer();
        var restored = assertInstanceOf(M3Jdk21HashPinnedSnapshotRecipe.class,
                codec.read(codec.write(recipe())));
        assertEquals("jdk22-map-facts", restored.getCrateName());
        assertThrows(IllegalArgumentException.class,
                () -> new M3Jdk21HashPinnedSnapshotRecipe(null));
        var named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.MapFacts");
        run(named, baseline(), 2);
    }

    @Test
    void workflowReplayRetainsTheOriginalTestAndAddsMapFacts() throws IOException {
        String root = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/map-facts/";
        String target = ".github/workflows/mindex-string-backing.yml";
        String prior = resource(root + "before-workflow.yml.txt");
        String next = resource(root + "workflow.yml.txt");
        var source = PlainText.builder().sourcePath(Path.of(target)).text(prior).build();
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("map-facts");
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(source)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile result = results.getFirst().getAfter();
        assertNotNull(result);
        assertEquals(next, result.printAll());
        assertTrue(next.contains("MIndexMappedStringBackingTest.java test/jdk/java/lang/String/MapGuardTest.java test/jdk/java/lang/String/MapFactsTest.java"));
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(result)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        var drift = source.withText(prior + "# drift\n");
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(drift)), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void priorGateRetainsItsOriginalPreimageAndAgreesOnTheFinalOwner() throws IOException {
        String oldRoot = ROOT.replace("jdk22-map-facts", "jdk22-map-guard");
        assertEquals(read("00-Backing.java.txt"), resource(oldRoot + "00-Backing.java.txt"));
        List<SourceFile> old = new ArrayList<>(baseline());
        old.removeIf(file -> path(file).equals(OWNER));
        old.add(parse(OWNER, resource(oldRoot + "before-00-Backing.java.txt")));
        var guard = new M3Jdk21HashPinnedSnapshotRecipe("jdk22-map-guard");
        var guarded = run(guard, old, 2);
        var after = run(recipe(), new ArrayList<>(guarded.values()), 1);
        run(guard, new ArrayList<>(after.values()), 0);
        run(recipe(), new ArrayList<>(after.values()), 0);
    }

    private static M3Jdk21HashPinnedSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedSnapshotRecipe("jdk22-map-facts");
    }

    private static List<SourceFile> baseline() throws IOException {
        var inputs = List.of(
                Parser.Input.fromString(Path.of(OWNER), read("before-00-Backing.java.txt")),
                Parser.Input.fromString(Path.of(API), read("context-Backing.java.txt")),
                Parser.Input.fromString(Path.of(PRIOR), read("context-Test.java.txt")));
        var files = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context()).toList();
        assertEquals(inputs.size(), files.size());
        files.forEach(file -> assertInstanceOf(J.CompilationUnit.class, file));
        return files;
    }

    private static SourceFile parse(String path, String text) {
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, context()).toList();
        assertEquals(1, parsed.size());
        assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
        assertEquals(text, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static Map<String, SourceFile> run(Recipe recipe, List<SourceFile> inputs, int changes) {
        var results = recipe.run(new InMemoryLargeSourceSet(inputs), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(changes, results.size());
        TreeMap<String, SourceFile> after = new TreeMap<>();
        inputs.forEach(file -> after.put(path(file), file));
        results.forEach(result -> {
            assertNotNull(result.getAfter());
            after.put(path(result.getAfter()), result.getAfter());
        });
        assertTrue(after.keySet().contains(OWNER));
        return after;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static String read(String name) throws IOException {
        String root = name.startsWith("context-")
                ? ROOT.replace("jdk22-map-facts", "jdk22-map-guard") : ROOT;
        return resource(root + name);
    }

    private static String resource(String name) throws IOException {
        try (var input = MapFactsRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("Missing MapFacts fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
