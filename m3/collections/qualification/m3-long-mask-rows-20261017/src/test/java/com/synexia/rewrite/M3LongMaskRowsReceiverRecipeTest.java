// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Executes the real existing OpenRewrite snapshot recipe, not a hand-built fake engine. */
final class M3LongMaskRowsReceiverRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/"
            + "m3jdk21-long-mask-rows-20261017" + "/";

    @Test void sourceAdmitsBothTargetsThenIsARealFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3jdk21-long-mask-rows-20261017");
        var result = recipe.run(new InMemoryLargeSourceSet(List.of(seed())), context(), 1);
        Map<String,String> actual = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change ->
                actual.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
        assertEquals(images(), actual);
        var again = new M3HashPinnedJavaSnapshotRecipe("m3jdk21-long-mask-rows-20261017").run(
                new InMemoryLargeSourceSet(parse(actual)), context(), 1);
        assertTrue(again.getChangeset().getAllResults().isEmpty());
    }

    @Test void allPartialResumesMatchExactly() {
        List<Map.Entry<String,String>> all = new ArrayList<>(images().entrySet());
        assertEquals(2, all.size());
        for (int mask = 0; mask < 4; mask++) {
            Map<String,String> already = new TreeMap<>();
            for (int i = 0; i < 2; i++) if ((mask & (1 << i)) != 0) {
                already.put(all.get(i).getKey(), all.get(i).getValue());
            }
            List<SourceFile> input = new ArrayList<>(parse(already));
            input.add(seed());
            var result = new M3HashPinnedJavaSnapshotRecipe("m3jdk21-long-mask-rows-20261017").run(
                    new InMemoryLargeSourceSet(input), context(), 1);
            result.getChangeset().getAllResults().forEach(change ->
                    already.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
            assertEquals(images(), already, "resume mask " + mask);
        }
    }

    @Test void driftAndForeignTypeRefuse() {
        var entry = images().entrySet().iterator().next();
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3jdk21-long-mask-rows-20261017");
        var ctx = context();
        var inventory = recipe.getInitialValue(ctx);
        M3HashPinnedJavaSnapshotRecipe inner = recipe;
        inner.getScanner(inventory).visit(parse(Map.of(entry.getKey(), entry.getValue() + "// drift\n")).getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> inner.generate(inventory, ctx));
        var other = recipe;
        var secondCtx = context();
        var second = other.getInitialValue(secondCtx);
        other.getScanner(second).visit(PlainText.builder()
                .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build(), secondCtx);
        assertThrows(IllegalStateException.class, () -> other.generate(second, secondCtx));
    }

    private static Map<String,String> images() {
        Map<String,String> entries = new TreeMap<>();
        for (String line : read("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length != 4 || !"ABSENT".equals(parts[1])) {
                throw new IllegalStateException("unexpected source target");
            }
            entries.put(parts[0], read(parts[3]));
        }
        return entries;
    }

    private static List<SourceFile> parse(Map<String,String> sources) {
        return JavaParser.fromJavaVersion().build().parseInputs(sources.entrySet().stream()
                        .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(),
                null, context()).toList();
    }

    private static String normalize(Path path) { return path.toString().replace('\\', '/'); }
    private static SourceFile seed() { return PlainText.builder()
            .sourcePath(Path.of("README.md")).text("seed\n").build(); }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String read(String name) {
        try (var stream = M3LongMaskRowsReceiverRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
}
