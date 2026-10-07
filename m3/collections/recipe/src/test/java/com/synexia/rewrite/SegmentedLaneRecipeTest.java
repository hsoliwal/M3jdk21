/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class SegmentedLaneRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/m3-collection-lanes/";

    @Test void actualRecipeProducesEverySealedCandidateThenReachesFixedPoint() throws IOException {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes");
        var result = recipe.run(new InMemoryLargeSourceSet(List.of(PlainText.builder().sourcePath(Path.of("README.md")).text("seed\n").build())), context, 1);
        Map<String, String> after = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(r -> after.put(r.getAfter().getSourcePath().toString().replace('\\', '/'), r.getAfter().printAll()));
        assertEquals(images(true), after);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(after)), context(), 1).getChangeset().getAllResults().isEmpty());
        Path target = Path.of(System.getProperty("candidate.output"));
        for (var entry : after.entrySet()) {
            Path path = target.resolve(entry.getKey()); Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
        }
    }

    @Test void driftAndNonJavaConflictsRejectNewFileAdmission() {
        Map<String, String> after = images(true);
        String first = after.keySet().iterator().next();
        for (int mode = 0; mode < 2; mode++) {
            var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes");
            var context = context(); var state = recipe.getInitialValue(context);
            String text = after.get(first) + (mode == 0 ? "// drift\n" : "");
            SourceFile source = mode == 0 ? parse(Map.of(first, text)).get(0)
                    : PlainText.builder().sourcePath(Path.of(first)).text(text).build();
            recipe.getScanner(state).visit(source, context);
            assertThrows(IllegalStateException.class, () -> recipe.generate(state, context));
        }
    }

    @Test void nativeRecipeInstallsExactlyOnceAndRefusesDifferentOrDuplicateSource() throws IOException {
        var recipe = new M3SegmentedLaneNativeRecipe(); var context = context();
        var state = recipe.getInitialValue(context);
        var generated = recipe.generate(state, context).iterator().next();
        Path path = Path.of(System.getProperty("candidate.output")).resolve(M3SegmentedLaneNativeRecipe.TARGET);
        Files.createDirectories(path.getParent()); Files.writeString(path, generated.printAll());
        var already = recipe.getInitialValue(context);
        recipe.getScanner(already).visit(generated, context);
        assertTrue(recipe.generate(already, context).isEmpty());
        recipe.getScanner(already).visit(generated, context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(already, context));
        var drifted = recipe.getInitialValue(context);
        recipe.getScanner(drifted).visit(PlainText.builder().sourcePath(Path.of(M3SegmentedLaneNativeRecipe.TARGET)).text("different\n").build(), context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(drifted, context));
    }

    private static Map<String, String> images(boolean after) {
        Map<String, String> result = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t");
            if (!after && cells[1].equals("ABSENT")) continue;
            result.put(cells[0], resource((after ? "" : "pre/") + cells[3]));
        }
        return result;
    }

    private static List<SourceFile> parse(Map<String, String> sources) {
        return JavaParser.fromJavaVersion().build().parseInputs(sources.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(), null, context()).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String file) {
        try (var in = SegmentedLaneRecipeTest.class.getResourceAsStream(ROOT + file)) {
            if (in == null) throw new IllegalStateException(file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
}
