// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3WholeSemanticHashRecipeTest {
    @Test
    void emitsCanonicalHashesFromAtomsThroughRepositoryWithoutMutation() {
        List<SourceFile> sources = parse(
                source(
                        "src/java.base/share/classes/p/Contract.java",
                        """
                        package p;
                        interface Contract {
                            int apply(int value);
                        }
                        """),
                source(
                        "src/java.base/share/classes/p/Impl.java",
                        """
                        package p;
                        /** Implementation docs. */
                        final class Impl implements Contract {
                            private static final int SCALE = 31;
                            public int apply(int value) {
                                return (value + 1) * SCALE;
                            }
                        }
                        """));

        var run = new M3WholeSemanticHashRecipe()
                .run(new InMemoryLargeSourceSet(sources), context(), 1);

        assertTrue(run.getChangeset().getAllResults().isEmpty());

        List<M3SemanticHashTable.Row> hashes =
                run.getDataTableRows(M3SemanticHashTable.class);
        List<M3SemanticRootTable.Row> roots =
                run.getDataTableRows(M3SemanticRootTable.class);

        assertFalse(hashes.isEmpty());
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("ATOM")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("FIELD")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("METHOD")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("FILE")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("INTERFACE")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("IMPLEMENTATION")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("DOCUMENTATION")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("PACKAGE")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("MODULE")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("LIBRARY")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("PROJECT")));
        assertTrue(hashes.stream().anyMatch(row -> row.kind().equals("REPOSITORY")));

        assertTrue(hashes.stream().allMatch(row -> row.nodeId().length() == 64));
        assertTrue(hashes.stream().allMatch(row -> row.exactSha256().length() == 64));
        assertTrue(hashes.stream().allMatch(row -> row.structuralSha256().length() == 64));
        assertTrue(hashes.stream().allMatch(row -> row.logicSha256().length() == 64));
        assertTrue(hashes.stream().allMatch(row -> row.structuralHash64().length() == 16));
        assertTrue(hashes.stream().allMatch(row -> row.logicHash64().length() == 16));
        assertTrue(hashes.stream().allMatch(row -> row.simHash64().length() == 16));
        assertTrue(hashes.stream().allMatch(row -> row.patternRole().startsWith("M3:")));
        assertTrue(hashes.stream().anyMatch(row ->
                row.kind().equals("METHOD") && row.childCount() > 0));
        assertTrue(hashes.stream().anyMatch(row ->
                row.kind().equals("IMPLEMENTATION") && row.parentCount() > 0));

        assertEquals(1L, roots.stream().filter(row -> row.kind().equals("REPOSITORY")).count());
        assertEquals(1L, roots.stream().filter(row -> row.kind().equals("PROJECT")).count());
        assertEquals(1L, roots.stream().filter(row -> row.kind().equals("LIBRARY")).count());
        assertEquals(1L, roots.stream().filter(row -> row.kind().equals("MODULE")).count());
        assertEquals(1L, roots.stream().filter(row -> row.kind().equals("PACKAGE")).count());
        assertEquals(2L, roots.stream().filter(row -> row.kind().equals("FILE")).count());

        M3SemanticRootTable.Row repository = roots.stream()
                .filter(row -> row.kind().equals("REPOSITORY"))
                .findFirst()
                .orElseThrow();
        assertEquals(64, repository.logicSha256().length());
        assertEquals(64, repository.structuralSha256().length());
        assertEquals(16, repository.logicHash64().length());
        assertEquals(16, repository.structuralHash64().length());
        assertEquals(16, repository.simHash64().length());
    }

    @Test
    void fileInputOrderCannotChangeWholeRepositoryHash() {
        SourceFile alpha = parseOne(
                "src/java.base/share/classes/p/Alpha.java",
                """
                package p;
                final class Alpha {
                    int value(int a, int b) {
                        return a + b;
                    }
                }
                """);
        SourceFile beta = parseOne(
                "src/java.base/share/classes/p/Beta.java",
                """
                package p;
                final class Beta {
                    int value(int a, int b) {
                        return a * b;
                    }
                }
                """);

        M3SemanticRootTable.Row left = repositoryRoot(List.of(alpha, beta));
        M3SemanticRootTable.Row right = repositoryRoot(List.of(beta, alpha));

        assertEquals(left.nodeId(), right.nodeId());
        assertEquals(left.exactSha256(), right.exactSha256());
        assertEquals(left.structuralSha256(), right.structuralSha256());
        assertEquals(left.logicSha256(), right.logicSha256());
        assertEquals(left.structuralHash64(), right.structuralHash64());
        assertEquals(left.logicHash64(), right.logicHash64());
        assertEquals(left.simHash64(), right.simHash64());
    }

    @Test
    void logicChangePropagatesToRepositoryLogicButPreservesSameShapeStructure() {
        SourceFile addition = parseOne(
                "src/java.base/share/classes/p/Calc.java",
                """
                package p;
                final class Calc {
                    int value(int a, int b) {
                        return a + b;
                    }
                }
                """);
        SourceFile subtraction = parseOne(
                "src/java.base/share/classes/p/Calc.java",
                """
                package p;
                final class Calc {
                    int value(int a, int b) {
                        return a - b;
                    }
                }
                """);

        M3SemanticRootTable.Row plus = repositoryRoot(List.of(addition));
        M3SemanticRootTable.Row minus = repositoryRoot(List.of(subtraction));

        assertEquals(plus.nodeId(), minus.nodeId());
        assertEquals(plus.structuralSha256(), minus.structuralSha256());
        assertEquals(plus.structuralHash64(), minus.structuralHash64());
        assertNotEquals(plus.exactSha256(), minus.exactSha256());
        assertNotEquals(plus.logicSha256(), minus.logicSha256());
        assertNotEquals(plus.logicHash64(), minus.logicHash64());
    }

    @Test
    void metadataAndNamespaceValidationAreExplicit() {
        var recipe = new M3WholeSemanticHashRecipe("repo", "project", "library");
        assertEquals("repo", recipe.repository());
        assertEquals("project", recipe.project());
        assertEquals("library", recipe.library());
        assertTrue(recipe.getDisplayName().contains("whole"));
        assertTrue(recipe.getDescription().contains("ATOM through REPOSITORY"));
        assertTrue(recipe.getTags().contains("semantic-hash"));
        assertTrue(recipe.getTags().contains("multi-pass"));
        assertTrue(recipe.getTags().contains("non-mutating"));

        assertThrows(
                NullPointerException.class,
                () -> new M3WholeSemanticHashRecipe(null, "project", "library"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3WholeSemanticHashRecipe("", "project", "library"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3WholeSemanticHashRecipe("repo", " ", "library"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3WholeSemanticHashRecipe("repo", "project", "\0"));
    }

    private static M3SemanticRootTable.Row repositoryRoot(List<SourceFile> sources) {
        var run = new M3WholeSemanticHashRecipe()
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        return run.getDataTableRows(M3SemanticRootTable.class)
                .stream()
                .filter(row -> row.kind().equals("REPOSITORY"))
                .findFirst()
                .orElseThrow();
    }

    private static Parser.Input source(String path, String source) {
        return Parser.Input.fromString(Path.of(path), source);
    }

    private static SourceFile parseOne(String path, String source) {
        return parse(source(path, source)).getFirst();
    }

    private static List<SourceFile> parse(Parser.Input... inputs) {
        return new ArrayList<>(
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(List.of(inputs), null, context())
                        .toList());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
