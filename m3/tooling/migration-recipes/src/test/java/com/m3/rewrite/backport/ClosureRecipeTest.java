// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.openrewrite.java.tree.J;

/** Materializes the four reviewed repairs through the existing source-sealed OpenRewrite owner. */
final class ClosureRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-recipe-closure/";

    @Test
    void exactParentReplayMatchesInstalledOwnersAndThenStops() throws IOException {
        List<String[]> rows = manifest();
        List<SourceFile> before = new ArrayList<>();
        for (String[] row : rows) before.add(parse(row[0], read("before-" + row[3])));
        Map<String, SourceFile> emitted = apply(recipe(), before, 4);
        Path module = Path.of("").toAbsolutePath().normalize();
        Path output = Path.of("target", "recipe-closure").toAbsolutePath().normalize();
        for (String[] row : rows) {
            String result = emitted.get(row[0]).printAll();
            assertEquals(read(row[3]), result, row[0]);
            assertEquals(Files.readString(module.resolve(row[0])), result, row[0]);
            Path destination = output.resolve(row[0]).normalize();
            if (!destination.startsWith(output)) throw new IOException("candidate path escape");
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, result, StandardCharsets.UTF_8);
        }
        apply(recipe(), new ArrayList<>(emitted.values()), 0);
    }

    @Test
    void anyDriftOrMissingRequiredOwnerRefusesTheWholeCrate() throws IOException {
        List<SourceFile> originals = new ArrayList<>();
        for (String[] row : manifest()) originals.add(parse(row[0], read("before-" + row[3])));
        assertThrows(RuntimeException.class, () -> apply(recipe(), List.of(), 0));
        List<SourceFile> drifted = new ArrayList<>(originals);
        String[] first = manifest().getFirst();
        drifted.set(0, parse(first[0], read("before-" + first[3]) + "\n// drift\n"));
        assertThrows(RuntimeException.class, () -> apply(recipe(), drifted, 0));
    }

    @Test
    void namedCompositionUsesTheSameCrateAndRetainsUnrelatedSource() throws IOException {
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.Closure");
        List<SourceFile> originals = new ArrayList<>();
        for (String[] row : manifest()) originals.add(parse(row[0], read("before-" + row[3])));
        SourceFile other = parse("Other.java", "class Other {}\n");
        originals.add(other);
        Map<String, SourceFile> emitted = apply(named, originals, 4);
        assertEquals(other, emitted.get("Other.java"));
    }

    private static Recipe recipe() {
        return new M3Jdk21HashPinnedSnapshotRecipe("jdk22-recipe-closure");
    }

    private static Map<String, SourceFile> apply(Recipe recipe, List<SourceFile> sources, int count) {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var results = recipe.run(new InMemoryLargeSourceSet(sources), context, 1)
                .getChangeset().getAllResults();
        assertEquals(count, results.size());
        Map<String, SourceFile> output = new TreeMap<>();
        sources.forEach(file -> output.put(file.getSourcePath().toString(), file));
        results.forEach(result -> {
            SourceFile after = result.getAfter();
            assertNotNull(after);
            assertInstanceOf(J.CompilationUnit.class, after);
            output.put(after.getSourcePath().toString(), after);
        });
        return output;
    }

    private static SourceFile parse(String path, String source) {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        List<SourceFile> files = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), source)), null, context).toList();
        assertEquals(1, files.size());
        assertInstanceOf(J.CompilationUnit.class, files.getFirst());
        assertEquals(source, files.getFirst().printAll());
        return files.getFirst();
    }

    private static List<String[]> manifest() throws IOException {
        return read("manifest.tsv").lines().map(line -> line.split("\t", -1)).toList();
    }

    private static String read(String name) throws IOException {
        try (var input = ClosureRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IOException("missing closure input: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
