// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

final class J485RecipeTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned/";

    @Test
    void everyFileAtomMaterializesExactlyAndReachesFixedPoint() throws Exception {
        for (String line : resource("j485-catalogue.tsv").lines().skip(1).toList()) {
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(6, cells.length);
            String crate = cells[0];
            String path = cells[1];
            String expected = resource(crate + "/" + cells[5]);
            List<SourceFile> input = "ABSENT".equals(cells[4])
                    ? List.of()
                    : parse(path, resource(crate + "/" + cells[4]));
            var changes = new M3Jdk21HashPinnedSnapshotRecipe(crate)
                    .run(new InMemoryLargeSourceSet(input), context(), 1)
                    .getChangeset().getAllResults();
            assertEquals(1, changes.size(), crate);
            SourceFile output = changes.getFirst().getAfter();
            assertNotNull(output, crate);
            assertEquals(path, output.getSourcePath().toString().replace('\\', '/'));
            assertEquals(expected, output.printAll(), crate);
            assertTrue(new M3Jdk21HashPinnedSnapshotRecipe(crate)
                    .run(new InMemoryLargeSourceSet(parse(path, output.printAll())), context(), 1)
                    .getChangeset().getAllResults().isEmpty(), crate);
        }
    }

    @Test
    void catalogueIsOneFilePerCrateAndUsesShortJ485Vocabulary() throws Exception {
        List<String> rows = resource("j485-catalogue.tsv").lines().skip(1)
                .filter(line -> !line.isBlank()).toList();
        assertEquals(15, rows.size());
        assertEquals(15, rows.stream().map(line -> line.split("\t", -1)[0]).distinct().count());
        assertTrue(rows.stream().allMatch(line -> line.startsWith("jdk24-j485-")));
    }

    private static List<SourceFile> parse(String path, String source) {
        var parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), source)), null, context())
                .toList();
        assertEquals(1, parsed.size());
        assertEquals(source, parsed.getFirst().printAll());
        return parsed;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String resource(String name) throws IOException {
        try (var input = J485RecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
