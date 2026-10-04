// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** ContractProbe for the existing engines' exact gate-repair configurations. */
final class GateTest {
    private static final String JAVA =
            "/com/synexia/rewrite/hash-pinned-java/gate-java/";
    private static final String META =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/gate-meta/";

    private record Row(String path, String before, String resource) {}

    @Test
    void javaRepairReplaysExactSourcesAndReachesFixedPoint() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("gate-java");
        var rows = rows(JAVA);
        var inputs = beforeJava(rows);
        var results = recipe.run(new InMemoryLargeSourceSet(inputs), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(rows.size(), results.size());
        var after = new TreeMap<String, SourceFile>();
        for (var result : results) {
            var source = result.getAfter();
            assertNotNull(source);
            assertInstanceOf(J.CompilationUnit.class, source);
            after.put(path(source), source);
        }
        for (var row : rows) {
            assertEquals(read(JAVA + row.resource()), after.get(row.path()).printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void javaRepairRefusesMissingDuplicateAndWrongTreeTargets() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("gate-java");
        var baseline = beforeJava(rows(JAVA));
        var missing = new ArrayList<>(baseline);
        missing.removeFirst();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(missing), context(), 1).getChangeset().getAllResults());
        var duplicate = new ArrayList<>(baseline);
        duplicate.add(baseline.getFirst());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(duplicate), context(), 1).getChangeset().getAllResults());
        var wrong = new ArrayList<>(baseline);
        wrong.set(0, text(path(baseline.getFirst()), baseline.getFirst().printAll()));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(wrong), context(), 1).getChangeset().getAllResults());
    }

    @Test
    void metadataRepairReplaysAllPathsWithoutChangingTargetContentIdentity() throws Exception {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("gate-meta");
        var rows = rows(META);
        List<SourceFile> inputs = new ArrayList<>();
        for (var row : rows) {
            if (!row.before().equals("ABSENT")) {
                inputs.add(text(row.path(), read(META + "before-" + row.resource())));
            }
        }
        var results = recipe.run(new InMemoryLargeSourceSet(inputs), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(rows.size(), results.size());
        var after = new TreeMap<String, SourceFile>();
        for (var result : results) {
            var source = result.getAfter();
            assertNotNull(source);
            assertInstanceOf(PlainText.class, source);
            after.put(path(source), source);
        }
        for (var row : rows) {
            assertEquals(read(META + row.resource()), after.get(row.path()).printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        var drift = new ArrayList<>(inputs);
        drift.set(0, text(path(inputs.getFirst()), inputs.getFirst().printAll() + "# drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(drift), context(), 1).getChangeset().getAllResults());
    }

    @Test
    void namedConfigurationsUseExistingRegisteredEngines() {
        var environment = Environment.builder().scanYamlResources().build();
        assertNotNull(environment.activateRecipes("com.m3.rewrite.GateJava"));
        assertNotNull(environment.activateRecipes("com.m3.rewrite.GateMeta"));
    }

    private static List<SourceFile> beforeJava(List<Row> rows) throws IOException {
        var inputs = new ArrayList<Parser.Input>();
        for (var row : rows) {
            if (!row.before().equals("ABSENT")) {
                inputs.add(Parser.Input.fromString(Path.of(row.path()),
                        read(JAVA + "before-" + row.resource())));
            }
        }
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context()).toList();
        assertEquals(inputs.size(), parsed.size());
        parsed.forEach(source -> assertInstanceOf(J.CompilationUnit.class, source));
        return parsed;
    }

    private static List<Row> rows(String root) throws IOException {
        var result = new ArrayList<Row>();
        for (String line : read(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            assertEquals(4, fields.length);
            result.add(new Row(fields[0], fields[1], fields[3]));
        }
        return result;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static String read(String name) throws IOException {
        try (var stream = GateTest.class.getResourceAsStream(name)) {
            if (stream == null) throw new IOException("Missing gate fixture " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
