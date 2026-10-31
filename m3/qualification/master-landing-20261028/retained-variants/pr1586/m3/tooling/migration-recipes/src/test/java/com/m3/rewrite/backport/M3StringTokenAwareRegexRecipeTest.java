// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3StringTokenAwareRegexRecipeTest {
    private static final String FORWARD = "m3-string-token-aware-regex-20261020";
    private static final String INVERSE = "m3-string-token-aware-regex-20261020-inverse";

    @Test
    void forwardInverseAndFixedPointsAreExact() {
        List<SourceFile> before = sources(INVERSE);
        List<SourceFile> after = sources(FORWARD);
        assertEquals(paths(after), paths(before));

        var forward = new M3Jdk21HashPinnedSnapshotRecipe(FORWARD);
        var applied = forward.run(new InMemoryLargeSourceSet(before), context(), 1);
        assertEquals(texts(after), texts(apply(before, applied.getChangeset().getAllResults())));
        assertTrue(forward.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());

        var inverse = new M3Jdk21HashPinnedSnapshotRecipe(INVERSE);
        var rolled = inverse.run(new InMemoryLargeSourceSet(after), context(), 1);
        assertEquals(texts(before), texts(apply(after, rolled.getChangeset().getAllResults())));
        assertTrue(inverse.run(new InMemoryLargeSourceSet(before), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftRefuses() {
        List<SourceFile> before = new ArrayList<>(sources(INVERSE));
        SourceFile first = before.getFirst();
        before.set(0, parse(first.getSourcePath(), first.printAll() + "\n// deliberate drift\n"));
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(FORWARD);
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(before), context(), 1));
    }

    private static List<SourceFile> sources(String crate) {
        List<SourceFile> result = new ArrayList<>();
        for (String line : resource(crate, "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\\t", -1);
            result.add(parse(Path.of(cells[0]), resource(crate, cells[3])));
        }
        return List.copyOf(result);
    }

    private static SourceFile parse(Path path, String text) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(path, text)), null, context())
                .findFirst().orElseThrow();
    }

    private static List<String> paths(List<SourceFile> values) {
        return values.stream().map(v -> v.getSourcePath().toString().replace('\\', '/')).toList();
    }

    private static List<String> texts(List<SourceFile> values) {
        return values.stream().map(SourceFile::printAll).toList();
    }

    private static List<SourceFile> apply(List<SourceFile> input,
            List<org.openrewrite.Result> changes) {
        var result = new java.util.TreeMap<String, SourceFile>();
        for (SourceFile source : input) {
            result.put(source.getSourcePath().toString().replace('\\', '/'), source);
        }
        for (var change : changes) {
            assertNotNull(change.getAfter());
            result.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter());
        }
        return List.copyOf(result.values());
    }

    private static String resource(String crate, String file) {
        String path = "/com/m3/rewrite/backport/jdk21-hash-pinned/" + crate + "/" + file;
        try (var stream = M3StringTokenAwareRegexRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}
