// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class J485CurrentRecipeTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned/";

    @Test
    void currentProductGuardsAndFinalTestAtomsReachFixedPoint() throws Exception {
        int guards = 0;
        int mutations = 0;
        for (Row row : rows()) {
            String before = resource(row.crate() + "/" + row.beforeResource());
            String after = resource(row.crate() + "/" + row.afterResource());
            SourceFile input = parse(row.path(), before);
            var first = new M3Jdk21HashPinnedSnapshotRecipe(row.crate())
                    .run(new InMemoryLargeSourceSet(List.of(input)), context(), 1)
                    .getChangeset().getAllResults();

            SourceFile output;
            if ("GUARD".equals(row.mode())) {
                guards++;
                assertTrue(first.isEmpty(), row.crate());
                output = input;
            } else {
                mutations++;
                assertEquals(1, first.size(), row.crate());
                output = first.getFirst().getAfter();
                assertNotNull(output, row.crate());
                assertEquals(after, output.printAll(), row.crate());
            }

            assertTrue(new M3Jdk21HashPinnedSnapshotRecipe(row.crate())
                    .run(new InMemoryLargeSourceSet(List.of(output)), context(), 1)
                    .getChangeset().getAllResults().isEmpty(), row.crate());
        }
        assertEquals(8, guards);
        assertEquals(7, mutations);
    }

    @Test
    void catalogueIsFileAtomicAndShortNamed() throws Exception {
        List<Row> rows = rows();
        assertEquals(15, rows.size());
        assertEquals(15, rows.stream().map(Row::crate).distinct().count());
        assertEquals(15, rows.stream().map(Row::path).distinct().count());
        assertTrue(rows.stream().allMatch(row -> row.crate().startsWith("jdk24-j485-cur-")));
    }

    @Test
    void sourceDriftFailsClosed() throws Exception {
        Row row = rows().stream().filter(candidate -> "MUTATE".equals(candidate.mode()))
                .findFirst().orElseThrow();
        String before = resource(row.crate() + "/" + row.beforeResource()) + "\n// drift\n";
        SourceFile input = parse(row.path(), before);
        assertThrows(RuntimeException.class, () ->
                new M3Jdk21HashPinnedSnapshotRecipe(row.crate())
                        .run(new InMemoryLargeSourceSet(List.of(input)), context(), 1)
                        .getChangeset().getAllResults());
    }

    private static List<Row> rows() throws IOException {
        return resource("j485-current-catalogue.tsv").lines().skip(1)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    String[] cells = line.split("\t", -1);
                    assertEquals(7, cells.length);
                    return new Row(cells[0], cells[1], cells[2], cells[5], cells[6]);
                })
                .toList();
    }

    private static SourceFile parse(String path, String source) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), source)), null, context())
                .toList();
        assertEquals(1, parsed.size(), path);
        assertEquals(source, parsed.getFirst().printAll(), path);
        return parsed.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String name) throws IOException {
        try (var input = J485CurrentRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Row(String crate, String path, String mode, String beforeResource, String afterResource) {}
}
