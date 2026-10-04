// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import org.openrewrite.text.PlainText;

/** Recipe mechanics are separate from actual Java21 archive/JVM conformance. */
final class VersionRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/pack-version/";
    private static final String INSPECTOR = "src/main/java/com/m3/tools/modulepack/M3ModuleInspector.java";
    private static final String PROBE = "src/test/java/com/m3/tools/modulepack/VersionProbe.java";

    @Test
    void namedRecipeReplaysExactJavaOutputsAndFixedPoint() throws IOException {
        Recipe recipe = Environment.builder().scanRuntimeClasspath().build()
                .activateRecipes("com.m3.rewrite.packs.Version");
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile before = parse(INSPECTOR, resource("before-00-M3ModuleInspector.java.txt"));
        var untouched = PlainText.builder().sourcePath(Path.of("untouched.txt")).text("keep\n").build();
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(before, untouched)), context, 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(3, changes.size());
        var expected = new TreeMap<String, String>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            String[] cells = line.split("\t", -1);
            expected.put(cells[0], resource(cells[3]));
        }
        var actual = new TreeMap<String, String>();
        var after = new ArrayList<SourceFile>();
        for (var result : changes) {
            SourceFile file = result.getAfter();
            assertNotNull(file);
            assertTrue(file instanceof J.CompilationUnit);
            String path = file.getSourcePath().toString().replace('\\', '/');
            actual.put(path, file.printAll());
            if (path.equals(INSPECTOR)) assertEquals(before.getId(), file.getId());
            after.add(file);
        }
        assertEquals(expected, actual);
        after.add(untouched);
        var repeat = recipe.run(new InMemoryLargeSourceSet(after), context, 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertEquals("keep\n", untouched.printAll());
    }

    @Test
    void missingDriftDuplicateWrongTypeAndOccupiedTargetsRefuse() throws IOException {
        SourceFile before = parse(INSPECTOR, resource("before-00-M3ModuleInspector.java.txt"));
        reject(List.of());
        reject(List.of(parse(INSPECTOR, before.printAll() + "// drift\n")));
        reject(List.of(before, parse(INSPECTOR, before.printAll())));
        reject(List.of(PlainText.builder().sourcePath(Path.of(INSPECTOR)).text(before.printAll()).build()));
        reject(List.of(before, parse(PROBE, "package com.m3.tools.modulepack; final class VersionProbe {}\n")));
    }

    @Test
    void alreadyRepairedOwnerOnlyGeneratesMissingTests() throws IOException {
        var errors = new ArrayList<Throwable>();
        var result = new M3HashPinnedJavaSnapshotRecipe("pack-version")
                .run(new InMemoryLargeSourceSet(List.of(parse(INSPECTOR,
                        resource("00-M3ModuleInspector.java.txt")))),
                        new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(2, result.getChangeset().getAllResults().size());
    }

    private static void reject(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var result = new M3HashPinnedJavaSnapshotRecipe("pack-version")
                    .run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(result.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty());
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static SourceFile parse(String path, String content) {
        var errors = new ArrayList<Throwable>();
        var files = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), content)),
                        null, new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, files.size());
        assertTrue(files.getFirst() instanceof J.CompilationUnit);
        return files.getFirst();
    }

    private static String resource(String name) throws IOException {
        try (var input = VersionRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
