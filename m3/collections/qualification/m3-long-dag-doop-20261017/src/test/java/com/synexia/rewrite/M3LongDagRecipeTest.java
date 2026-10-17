// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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

/** Actual incumbent OpenRewrite SDK cycle tests; never rewrite checked-in sources in place. */
final class M3LongDagRecipeTest {
    private static final String CRATE = "m3-long-dag-doop-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void admitsReviewedTargetsAndIsIdempotent() {
        Map<String, String> expected = images();
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(List.of(seed())), context(), 1);
        Map<String, String> actual = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change ->
                actual.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
        assertEquals(expected, actual);
        var again = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(parse(actual)), context(), 1);
        assertTrue(again.getChangeset().getAllResults().isEmpty());
    }

    @Test void everyPartialResumeProducesTheSameFinalPostimage() {
        List<Map.Entry<String, String>> all = new ArrayList<>(images().entrySet());
        assertEquals(5, all.size());
        for (int mask = 0; mask < (1 << all.size()); mask++) {
            Map<String, String> existing = new TreeMap<>();
            for (int i = 0; i < all.size(); i++) {
                if ((mask & (1 << i)) != 0) existing.put(all.get(i).getKey(), all.get(i).getValue());
            }
            List<SourceFile> inputs = new ArrayList<>(parse(existing));
            inputs.add(seed());
            var next = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                    new InMemoryLargeSourceSet(inputs), context(), 1);
            Map<String, String> reconstructed = new TreeMap<>(existing);
            next.getChangeset().getAllResults().forEach(change ->
                    reconstructed.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
            assertEquals(images(), reconstructed, "resume subset " + mask);
        }
    }

    @Test void driftAndPlainTextTargetsAreRejected() {
        var source = images().entrySet().iterator().next();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var ctx = context();
        var state = recipe.getInitialValue(ctx);
        recipe.getScanner(state).visit(parse(Map.of(source.getKey(), source.getValue() + "// drift\n")).get(0), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));

        var second = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var ctx2 = context();
        var state2 = second.getInitialValue(ctx2);
        second.getScanner(state2).visit(PlainText.builder()
                .sourcePath(Path.of(source.getKey())).text(source.getValue()).build(), ctx2);
        assertThrows(IllegalStateException.class, () -> second.generate(state2, ctx2));
    }

    private static List<SourceFile> parse(Map<String, String> values) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(values.entrySet().stream()
                                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(),
                        null, context())
                .toList();
    }

    private static String normalize(Path path) { return path.toString().replace('\\', '/'); }

    private static SourceFile seed() {
        return PlainText.builder().sourcePath(Path.of("README.md")).text("seed\n").build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(throwable -> { throw new AssertionError(throwable); });
    }

    private static Map<String, String> images() {
        Map<String, String> values = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            if (row.length != 4 || !row[1].equals("ABSENT")) {
                throw new IllegalStateException("invalid new-source row");
            }
            values.put(row[0], resource(row[3]));
        }
        return values;
    }

    private static String resource(String path) {
        try (var input = M3LongDagRecipeTest.class.getResourceAsStream(ROOT + path)) {
            if (input == null) throw new IllegalArgumentException("missing: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
