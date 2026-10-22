// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

class NebulaM3InventoryRecipeTest {

    @Test
    void analyzesPublicSurfaceAndFastSearchSignalsWithoutChangingSource() {
        String source =
                """
                package p;
                import java.util.List;
                public class Candidate {
                    private native int nativeCall();
                    public int find(List<String> values, String value) {
                        for (int i = 0; i < values.size(); i++) {
                            if (values.contains(value)) return values.indexOf(value);
                        }
                        return -1;
                    }
                }
                """;
        J.CompilationUnit unit = parse(Path.of("widgets/demo/src/p/Candidate.java"), source);
        NebulaM3InventoryRecipe.SourceFacts facts = NebulaM3InventoryRecipe.analyze(unit);

        assertEquals("widgets/demo", facts.module());
        assertEquals(1, facts.typeCount());
        assertEquals(1, facts.publicProtectedTypeCount());
        assertEquals(2, facts.methodCount());
        assertEquals(1, facts.publicProtectedMethodCount());
        assertEquals(1, facts.nativeMethodCount());
        assertEquals(1, facts.loopCount());
        assertEquals(1, facts.indexOfCalls());
        assertEquals(1, facts.containsCalls());
        assertTrue(facts.fastSearchSignal().contains("REPEATED_SCAN_CANDIDATE"));
        assertEquals(
                "REVIEW_LINEAR_SEARCH_FOR_PRECOMPUTED_INDEX",
                facts.recommendedNextPass());

        var run =
                new NebulaM3InventoryRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(unit)),
                                new InMemoryExecutionContext());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void wholeCheckoutCliWritesDeterministicEvidence(@TempDir Path temp) throws Exception {
        Path root = temp.resolve("nebula");
        Path source = root.resolve("widgets/demo/src/p/Candidate.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
                source,
                """
                package p;
                public class Candidate {
                    public int find(String text) {
                        return text.indexOf("x");
                    }
                }
                """,
                StandardCharsets.UTF_8);

        Path output = temp.resolve("out");
        NebulaM3InventoryCli.Summary summary = NebulaM3InventoryCli.run(root, output);

        assertEquals(1, summary.javaFiles());
        assertEquals(1, summary.parsedCompilationUnits());
        assertEquals(0, summary.failureRows());
        String inventory =
                Files.readString(output.resolve("nebula-m3-java-inventory.tsv"));
        assertTrue(inventory.contains("widgets/demo/src/p/Candidate.java"));
        assertTrue(inventory.contains("REVIEW_LINEAR_SEARCH_CONTRACT"));
        assertTrue(Files.readString(output.resolve("nebula-m3-parse-failures.tsv"))
                .equals("sourcePath\treason\n"));
    }

    private static J.CompilationUnit parse(Path path, String source) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(path, source)),
                                null,
                                new InMemoryExecutionContext())) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return (J.CompilationUnit) files.getFirst();
        }
    }
}
