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
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Independent actual SDK qualification for both canonical source-kind engines. */
final class M3GvasBoundaryRecipeTest {
    private static final String CRATE = "m3-gvas-boundaries-20261017";
    private static String resource(String kind, String name) {
        String path = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/" + name;
        try (InputStream input = M3GvasBoundaryRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    private static List<String[]> rows(String kind) {
        return resource(kind, "manifest.tsv").lines().filter(s -> !s.isBlank())
                .map(s -> s.split("\t", -1)).toList();
    }
    private static Map<String, String> images(String kind, boolean before) {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : rows(kind)) {
            if (!before || !row[1].equals("ABSENT")) {
                result.put(row[0], resource(kind, (before ? "pre/" : "") + row[3]));
            }
        }
        return result;
    }
    private static List<SourceFile> parse(String kind, Map<String, String> values) {
        if (kind.equals("text")) {
            List<SourceFile> output = new ArrayList<>();
            new TreeMap<>(values).forEach((path, text) -> output.add(
                    PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
            return output;
        }
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(values).forEach((path, text) ->
                inputs.add(Parser.Input.fromString(Path.of(path), text)));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null,
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); })).toList();
    }
    private static Recipe recipe(String kind) {
        return kind.equals("java") ? new M3HashPinnedJavaSnapshotRecipe(CRATE)
                : new M3HashPinnedTextSnapshotRecipe(CRATE);
    }
    private static Map<String, String> run(String kind, List<SourceFile> inputs) {
        var run = recipe(kind).run(new InMemoryLargeSourceSet(inputs),
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); }), 1);
        Map<String, String> output = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no deletion");
            SourceFile after = change.getAfter();
            output.put(after.getSourcePath().toString().replace('\\', '/'), after.printAll());
        });
        return output;
    }
    @Test void exactJavaAndNativeTextOutputsFixedPointAndExport() throws IOException {
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst());
        Map<String, String> all = new TreeMap<>();
        for (String kind : List.of("java", "text")) {
            assertEquals(2, rows(kind).size());
            Map<String, String> output = run(kind, parse(kind, images(kind, true)));
            assertEquals(images(kind, false), output);
            assertTrue(run(kind, parse(kind, output)).isEmpty());
            all.putAll(output);
        }
        assertEquals(4, all.size());
        String destination = System.getProperty("m3.gvas.output");
        if (destination != null) {
            Path root = Path.of(destination).toAbsolutePath().normalize();
            for (var entry : all.entrySet()) {
                Path target = root.resolve(entry.getKey()).normalize();
                assertTrue(target.startsWith(root));
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            }
        }
    }
    @Test void missingDriftingAndOccupiedTargetsRefuseForBothKinds() {
        for (String kind : List.of("java", "text")) {
            assertThrows(RuntimeException.class, () -> run(kind, parse(kind, Map.of())));
            for (String path : images(kind, true).keySet()) {
                Map<String, String> drift = new LinkedHashMap<>(images(kind, true));
                drift.compute(path, (key, value) -> value + "\n// drift\n");
                assertThrows(RuntimeException.class, () -> run(kind, parse(kind, drift)));
            }
            String addition = rows(kind).stream().filter(row -> row[1].equals("ABSENT"))
                    .findFirst().orElseThrow()[0];
            Map<String, String> occupied = new LinkedHashMap<>(images(kind, true));
            occupied.put(addition, kind.equals("java") ? "class Wrong {}\n" : "wrong\n");
            assertThrows(RuntimeException.class, () -> run(kind, parse(kind, occupied)));
        }
    }
    @Test void partialReplayUnrelatedContentAndInputOrderRemainSafe() {
        for (String kind : List.of("java", "text")) {
            Map<String, String> partial = new LinkedHashMap<>(images(kind, true));
            String first = images(kind, true).keySet().iterator().next();
            partial.put(first, images(kind, false).get(first));
            String unrelated = kind.equals("java") ? "unrelated/Example.java" : "unrelated/example.c";
            partial.put(unrelated, kind.equals("java") ? "class Example {}\n" : "/* opaque C */\n");
            Map<String, String> output = run(kind, parse(kind, partial));
            assertEquals(1, output.size());
            assertFalse(output.containsKey(first)); assertFalse(output.containsKey(unrelated));
            List<SourceFile> normal = parse(kind, images(kind, true));
            assertEquals(run(kind, normal), run(kind, normal.reversed()));
        }
    }
    @Test void duplicateAndNonJavaInputAtJavaOwnerRefuse() {
        for (String kind : List.of("java", "text")) {
            List<SourceFile> inputs = parse(kind, images(kind, true));
            var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
            if (kind.equals("java")) {
                var owner = new M3HashPinnedJavaSnapshotRecipe(CRATE);
                var state = owner.getInitialValue(context);
                var scanner = owner.getScanner(state);
                for (SourceFile input : inputs) scanner.visit(input, context);
                scanner.visit(inputs.getFirst(), context);
                assertThrows(IllegalStateException.class, () -> owner.generate(state, context));
            } else {
                var owner = new M3HashPinnedTextSnapshotRecipe(CRATE);
                var state = owner.getInitialValue(context);
                var scanner = owner.getScanner(state);
                for (SourceFile input : inputs) scanner.visit(input, context);
                scanner.visit(inputs.getFirst(), context);
                assertThrows(IllegalStateException.class, () -> owner.generate(state, context));
            }
        }
        SourceFile java = parse("java", images("java", true)).getFirst();
        SourceFile fake = PlainText.builder().sourcePath(java.getSourcePath()).text(java.printAll()).build();
        assertThrows(RuntimeException.class, () -> run("java", List.of(fake)));
    }
}
