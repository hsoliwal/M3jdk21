// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

final class M3PackageBoundaryRecipeTest {
    @Test
    void packageBoundaryRootIsDeterministicAcrossFileOrder() {
        SourceFile first = parse(
                "src/java.base/share/classes/p/A.java",
                """
                package p;
                class A {
                    int packageField;
                    protected int protectedField;
                    private int privateField;
                    public int publicField;

                    void overloaded(int value) {}
                    void overloaded(String value) {}
                    private void privateMethod() {}
                    public void publicMethod() {}
                }
                """);
        SourceFile second = parse(
                "src/java.base/share/classes/p/B.java",
                """
                package p;
                interface B {
                    int VALUE = 1;
                    int apply(int value);
                }
                """);

        var left = run(List.of(first, second));
        var right = run(List.of(second, first));

        assertEquals(1, left.size());
        assertEquals(left.getFirst().packageName(), right.getFirst().packageName());
        assertEquals(left.getFirst().fileCount(), right.getFirst().fileCount());
        assertEquals(left.getFirst().declarationCount(), right.getFirst().declarationCount());
        assertEquals(left.getFirst().boundaryRoot(), right.getFirst().boundaryRoot());

        var row = left.getFirst();
        assertEquals("p", row.packageName());
        assertEquals(2, row.fileCount());
        assertEquals(6, row.declarationCount());
        assertEquals(64, row.boundaryRoot().length());
        assertEquals("M3:PACKAGE_BOUNDARY", row.patternRole());
    }

    @Test
    void emitsDefaultAndEmptyPackageBoundaryRows() {
        SourceFile root = parse(
                "Root.java",
                """
                public final class Root {
                    private int value;
                    public void run() {}
                }
                """);

        var rows = run(List.of(root));
        assertEquals(1, rows.size());
        assertEquals("<default>", rows.getFirst().packageName());
        assertEquals(1, rows.getFirst().fileCount());
        assertEquals(0, rows.getFirst().declarationCount());
        assertEquals(64, rows.getFirst().boundaryRoot().length());
    }

    @Test
    void metadataDeclaresPackageScopeInventory() {
        var recipe = new M3PackageBoundaryRecipe();
        assertTrue(recipe.getDisplayName().contains("package"));
        assertTrue(recipe.getDescription().contains("deterministic package roots"));
        assertTrue(recipe.getTags().contains("package"));
        assertTrue(recipe.getTags().contains("non-mutating"));
    }

    private static List<M3PackageBoundaryRecipe.Row> run(List<SourceFile> sources) {
        var execution = new M3PackageBoundaryRecipe()
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(execution.getChangeset().getAllResults().isEmpty());
        return execution.getDataTableRows(M3PackageBoundaryRecipe.PackageTable.class);
    }

    private static SourceFile parse(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
