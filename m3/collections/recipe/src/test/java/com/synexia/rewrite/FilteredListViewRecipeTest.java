// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class FilteredListViewRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3-filtered-list-view-20261017/";

    @Test
    void exactPreimageProducesThreeReviewedPostimagesAndFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-filtered-list-view-20261017");
        Map<String, String> before = images(false);
        var run = recipe.run(new InMemoryLargeSourceSet(parse(before)), context(), 1);
        Map<String, String> actual = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(result ->
                actual.put(normalize(result.getAfter()), result.getAfter().printAll()));
        assertEquals(images(true), actual);

        var fixed = recipe.run(new InMemoryLargeSourceSet(parse(actual)), context(), 1);
        assertTrue(fixed.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void missingRequiredOwnerIsRejected() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-filtered-list-view-20261017");
        var state = recipe.getInitialValue(context());
        assertThrows(IllegalStateException.class, () -> recipe.generate(state, context()));
    }

    @Test
    void driftAndOccupiedAbsentTargetAreRejected() {
        Map<String, String> before = images(false);
        String owner = before.keySet().iterator().next();

        var driftRecipe = new M3HashPinnedJavaSnapshotRecipe("m3-filtered-list-view-20261017");
        var drift = driftRecipe.getInitialValue(context());
        driftRecipe.getScanner(drift).visit(
                parse(Map.of(owner, before.get(owner) + "// drift\n")).get(0), context());
        assertThrows(IllegalStateException.class, () -> driftRecipe.generate(drift, context()));

        String occupiedPath =
                "m3/collections/src/test/java/com/m3/collections/M3FilteredListViewTest.java";
        var occupiedRecipe = new M3HashPinnedJavaSnapshotRecipe("m3-filtered-list-view-20261017");
        var occupied = occupiedRecipe.getInitialValue(context());
        for (SourceFile file : parse(before)) {
            occupiedRecipe.getScanner(occupied).visit(file, context());
        }
        occupiedRecipe.getScanner(occupied).visit(
                JavaParser.fromJavaVersion().build()
                        .parse(context(), "package com.m3.collections; final class M3FilteredListViewTest {}")
                        .findFirst().orElseThrow()
                        .withSourcePath(Path.of(occupiedPath)), context());
        assertThrows(IllegalStateException.class, () -> occupiedRecipe.generate(occupied, context()));
    }

    @Test
    void unrelatedJavaSourceIsPreserved() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-filtered-list-view-20261017");
        Map<String, String> before = new TreeMap<>(images(false));
        before.put(
                "m3/collections/src/main/java/com/m3/collections/Unrelated.java",
                "package com.m3.collections; final class Unrelated {}\n");
        var run = recipe.run(new InMemoryLargeSourceSet(parse(before)), context(), 1);
        assertTrue(run.getChangeset().getAllResults().stream()
                .noneMatch(result -> normalize(result.getAfter()).endsWith("Unrelated.java")));
    }

    private static Map<String, String> images(boolean after) {
        Map<String, String> result = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (!after && "ABSENT".equals(cells[1])) continue;
            result.put(cells[0], resource(cells[3]));
        }
        return result;
    }

    private static List<SourceFile> parse(Map<String, String> sources) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                sources.entrySet().stream()
                        .map(entry -> Parser.Input.fromString(
                                Path.of(entry.getKey()), entry.getValue()))
                        .toList(),
                null,
                context()).toList();
    }

    private static String normalize(SourceFile file) {
        return file.getSourcePath().toString().replace('\\', '/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String name) {
        try (var input = FilteredListViewRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException(name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
