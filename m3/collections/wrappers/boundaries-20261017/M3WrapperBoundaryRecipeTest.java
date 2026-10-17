/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Uses the existing unchanged Synexia Java-LST engine, never a surrogate patcher. */
final class M3WrapperBoundaryRecipeTest {
    private static final String CRATE = "m3-wrapper-boundaries-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static String resource(String name) {
        try (InputStream input = M3WrapperBoundaryRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(s -> !s.isBlank())
                .map(s -> s.split("\t", -1)).toList();
    }
    private static Map<String, String> images(boolean before) {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : rows()) {
            if (!before || !row[1].equals("ABSENT")) {
                result.put(row[0], resource((before ? "pre/" : "") + row[3]));
            }
        }
        return result;
    }
    private static List<SourceFile> parse(Map<String, String> values) {
        List<Parser.Input> input = new ArrayList<>();
        new TreeMap<>(values).forEach((path, text) ->
                input.add(Parser.Input.fromString(Path.of(path), text)));
        return JavaParser.fromJavaVersion().build().parseInputs(input, null,
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); })).toList();
    }
    private static RecipeRun run(List<SourceFile> source) {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(source),
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); }), 1);
    }
    private static Map<String, String> changes(RecipeRun run) {
        Map<String, String> result = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            SourceFile after = change.getAfter(); assertNotNull(after, "no deletion");
            result.put(after.getSourcePath().toString().replace('\\', '/'), after.printAll());
        });
        return result;
    }
    @Test void exactTypedGenerationAndFixedPoint() throws IOException {
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst());
        assertEquals(3, rows().size());
        assertEquals(2, images(true).size());
        Map<String, String> output = changes(run(parse(images(true))));
        assertEquals(images(false), output);
        assertTrue(changes(run(parse(output))).isEmpty());
        // Export only actual SDK output to an explicitly selected scratch root.
        String destination = System.getProperty("m3.boundary.output");
        if (destination != null) {
            Path root = Path.of(destination).toAbsolutePath().normalize();
            for (var entry : output.entrySet()) {
                Path target = root.resolve(entry.getKey()).normalize();
                assertTrue(target.startsWith(root));
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            }
        }
    }
    @Test void eachMissingAndDriftingPreimageRefuses() {
        for (String path : images(true).keySet()) {
            Map<String, String> missing = new LinkedHashMap<>(images(true)); missing.remove(path);
            assertThrows(RuntimeException.class, () -> run(parse(missing)));
            Map<String, String> drift = new LinkedHashMap<>(images(true));
            drift.compute(path, (key, value) -> value + "\n// unauthorized drift\n");
            assertThrows(RuntimeException.class, () -> run(parse(drift)));
        }
    }
    @Test void partialResumeAndUnrelatedSourceRemainUnchanged() {
        Map<String, String> partial = new LinkedHashMap<>(images(true));
        String first = rows().getFirst()[0];
        partial.put(first, images(false).get(first));
        partial.put("example/Unrelated.java", "package example; class Unrelated {}\n");
        Map<String, String> output = changes(run(parse(partial)));
        assertEquals(2, output.size()); assertFalse(output.containsKey(first));
        assertFalse(output.containsKey("example/Unrelated.java"));
    }
    @Test void occupiedTargetNonJavaAndDuplicateInputRefuse() {
        Map<String, String> occupied = new LinkedHashMap<>(images(true));
        String added = rows().stream().filter(row -> row[1].equals("ABSENT"))
                .findFirst().orElseThrow()[0];
        occupied.put(added, "class Intruder {}\n");
        assertThrows(RuntimeException.class, () -> run(parse(occupied)));
        List<SourceFile> nonJava = new ArrayList<>(parse(images(true)));
        SourceFile first = nonJava.getFirst();
        nonJava.set(0, PlainText.builder().sourcePath(first.getSourcePath()).text(first.printAll()).build());
        assertThrows(RuntimeException.class, () -> run(nonJava));
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var state = recipe.getInitialValue(context);
        var scanner = recipe.getScanner(state);
        for (SourceFile source : parse(images(true))) scanner.visit(source, context);
        scanner.visit(first, context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(state, context));
    }
}
