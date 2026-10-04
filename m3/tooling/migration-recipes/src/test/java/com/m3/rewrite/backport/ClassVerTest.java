// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Actual parser/scheduler tests for a configuration of the existing snapshot engine. */
final class ClassVerTest {
    private static final String CRATE = "jdk22-class-ver";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";
    private static final String OWNER =
            "src/main/java/com/m3/tools/modulepack/M3ModuleInspector.java";
    private static final String BEFORE = "before-00-inspector.java.txt";

    @Test
    void exactJavaReplayAndFixedPointPreserveUnrelatedSource() throws IOException {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes("com.m3.rewrite.packs.ClassVer");
        SourceFile before = parse(OWNER, resource(BEFORE));
        SourceFile unrelated = parse("src/Unrelated.java", "class Unrelated {}\n");
        List<Result> changes = run(recipe, List.of(before, unrelated));
        assertEquals(3, changes.size());
        Map<String, String> actual = new TreeMap<>();
        List<SourceFile> after = new ArrayList<>();
        for (Result change : changes) {
            SourceFile file = change.getAfter();
            assertNotNull(file);
            assertInstanceOf(J.CompilationUnit.class, file);
            String path = file.getSourcePath().toString().replace('\\', '/');
            actual.put(path, file.printAll());
            if (path.equals(OWNER)) assertEquals(before.getId(), file.getId());
            after.add(file);
        }
        Map<String, String> expected = new TreeMap<>();
        for (String[] row : rows()) expected.put(row[0], resource(row[3]));
        assertEquals(expected, actual);
        after.add(unrelated);
        assertTrue(run(recipe, after).isEmpty());
        assertEquals("class Unrelated {}\n", unrelated.printAll());
    }

    @Test
    void missingDriftDuplicateAndOccupiedTargetsAreRefused() throws IOException {
        reject(List.of());
        reject(List.of(parse(OWNER, "class Wrong {}\n")));
        SourceFile before = parse(OWNER, resource(BEFORE));
        SourceFile duplicate = before.withId(UUID.randomUUID());
        reject(List.of(before, duplicate));
        String occupiedPath = rows().getLast()[0];
        reject(List.of(before, parse(occupiedPath, "class Occupied {}\n")));
    }

    @Test
    void exactSealsAndKernelRelativeScopeRemainExplicit() throws IOException {
        List<String[]> rows = rows();
        assertEquals(3, rows.size());
        String previous = "";
        for (int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            assertEquals(4, row.length);
            assertTrue(row[0].compareTo(previous) > 0);
            assertTrue(row[0].startsWith(index == 0 ? "src/main/java/" : "src/test/java/"));
            assertEquals(row[2], M3Jdk21HashPinnedSnapshotRecipe.sha256(resource(row[3])));
            if (index == 0) {
                assertEquals(OWNER, row[0]);
                assertEquals(row[1], M3Jdk21HashPinnedSnapshotRecipe.sha256(resource(BEFORE)));
            } else {
                assertEquals("ABSENT", row[1]);
            }
            previous = row[0];
        }
    }

    private static List<String[]> rows() throws IOException {
        return resource("manifest.tsv").lines().map(line -> line.split("\t", -1)).toList();
    }

    private static List<Result> run(Recipe recipe, List<SourceFile> input) {
        List<Throwable> errors = new ArrayList<>();
        List<Result> changes = recipe.run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        return changes;
    }

    private static void reject(List<SourceFile> input) {
        List<Throwable> errors = new ArrayList<>();
        try {
            var result = new M3Jdk21HashPinnedSnapshotRecipe(CRATE).run(
                    new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(result.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty(), "refusal must be reported");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static SourceFile parse(String path, String text) {
        List<Throwable> errors = new ArrayList<>();
        var files = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, files.size());
        assertInstanceOf(J.CompilationUnit.class, files.getFirst());
        assertEquals(text, files.getFirst().printAll());
        return files.getFirst();
    }

    private static String resource(String name) throws IOException {
        try (var stream = ClassVerTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
